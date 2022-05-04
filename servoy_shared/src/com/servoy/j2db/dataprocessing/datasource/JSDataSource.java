/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 */

package com.servoy.j2db.dataprocessing.datasource;

import java.util.Arrays;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.base.scripting.api.IJSRecord;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.BufferedDataSet;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.dataprocessing.IFoundSet;
import com.servoy.j2db.dataprocessing.JSTable;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.querybuilder.impl.QBSelect;
import com.servoy.j2db.scripting.IJavaScriptType;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IDestroyable;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Utils;

/**
 * Scope for datasources.db.myserver.mytable or datasources.mem['dsname']
 *
 * @author rgansevles
 *
 * @since 7.4
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class JSDataSource implements IJavaScriptType, IDestroyable
{
	private volatile IApplication application;
	private final String datasource;
	private FoundSet singleRecordFoundsetCache;

	public JSDataSource(IApplication application, String datasource)
	{
		this.application = application;
		this.datasource = datasource;
	}

	/**
	 * Get the datasource string.
	 *
	 * @sample
	 * datasources.db.example_data.orders.getDatasource() // returns 'db:/example_data/orders'
	 *
	 * @deprecated As of release 8.1.2 , replaced by {@link #getDataSource()}.
	 *
	 * @return String datasource
	 */
	@Deprecated
	@JSFunction
	public String getDatasource()
	{
		return datasource;
	}

	/**
	 * Get the datasource string.
	 *
	 * @sample
	 * datasources.db.example_data.orders.getDataSource() // returns 'db:/example_data/orders'
	 *
	 * @return String datasource
	 */
	@JSFunction
	public String getDataSource()
	{
		return datasource;
	}

	/**
	 * Returns a foundset object for a specified datasource or server and tablename.
	 * It is important to note that this is a FACTORY method, it constantly creates new foundsets.
	 *
	 * @sample
	 * var fs = datasources.db.example_data.orders.getFoundSet()
	 * var ridx = fs.newRecord()
	 * var record = fs.getRecord(ridx)
	 * record.emp_name = 'John'
	 * databaseManager.saveData()
	 *
	 * @return A new JSFoundset for the datasource.
	 */
	@JSFunction
	public IFoundSet getFoundSet() throws ServoyException
	{
		return application.getFoundSetManager().getFoundSet(datasource);
	}

	/**
	 * An existing foundset under that name will be returned, or created if there is a definition (there is a form with a named foundset property with that name).
	 * If named foundset datasource does not match current datasource will not be returned (will return null instead).
	 *
	 * @sample
	 * var fs = datasources.db.example_data.orders.getFoundSet('myname')
	 * var ridx = fs.newRecord()
	 * var record = fs.getRecord(ridx)
	 * record.emp_name = 'John'
	 * databaseManager.saveData()
	 *
	 *  @param name The named foundset to get  for this datasource.
	 *
	 * @return An existing named foundset for the datasource.
	 */
	@JSFunction
	public IFoundSet getFoundSet(String name) throws ServoyException
	{
		IFoundSet foundset = application.getFoundSetManager().getNamedFoundSet(name);
		return checkDataSourceEquality(foundset) ? foundset : null;
	}

	/**
	 * Get a single record from a datasource.
	 * For the sake of performance, if more records are needed,
	 * don't call this method in a loop but try using other methods instead.
	 *
	 * @sample
	 * var detailsRecord = datasources.db.example_data.order_details.getRecord([10248, 11])
	 * var orderRecord = datasources.db.example_data.orders.getRecord(10248)
	 * var customerRecord = datasources.db.example_data.customers.getRecord('ANATR')
	 *
	 * @param pk The primary key of the record to be retrieved. Can be an array, in case of a composite pk.
	 * @return a record
	 * @throws ServoyException
	 */
	@JSFunction
	public IJSRecord getRecord(Object pk) throws ServoyException
	{
		if (singleRecordFoundsetCache == null)
		{
			singleRecordFoundsetCache = (FoundSet)application.getFoundSetManager().getFoundSet(datasource);
		}

		IDataSet dataSet = new BufferedDataSet();

		if ((pk instanceof Object[]))
			dataSet.addRow((Object[])pk);
		else
			dataSet.addRow(new Object[] { pk });

		if (!singleRecordFoundsetCache.js_loadRecords(dataSet))
		{
			throw new ServoyException(ServoyException.INVALID_INPUT);
		}

		return singleRecordFoundsetCache.js_getRecord(1);
	}

	/**
	 * get a new foundset containing records based on a QBSelect query.
	 *
	 * @sample
	 * var qb = datasources.db.example_data.orders.createSelect();
	 * qb.result.add(qb.columns.orderid);
	 * var fs = datasources.db.example_data.orders.loadRecords(qb);
	 *
	 * @param qbSelect a query builder object
	 * @return a new JSFoundset
	 * @throws ServoyException
	 */
	@JSFunction
	public IFoundSet loadRecords(QBSelect qbSelect) throws ServoyException
	{
		IFoundSet foundset = application.getFoundSetManager().getFoundSet(datasource);
		foundset.loadByQuery(qbSelect);

		return checkDataSourceEquality(foundset) ? foundset : null;
	}

	/**
	 * get a new foundset containing records based on an SQL query string with parameters.
	 *
	 * @sample
	 * var query = "SELECT * FROM public.orders WHERE customerid = ? OR customerid = ? order by orderid asc";
	 * var args = ['ROMEY', 'BERGS'];
	 * var fs = datasources.db.example_data.orders.loadRecords(query, args);
	 *
	 * @param query an SQL query string with parameter placeholders
	 * @param args an array of arguments for the query string
	 * @return a new JSFoundset
	 * @throws ServoyException
	 */
	@JSFunction
	public IFoundSet loadRecords(String query, Object[] args) throws ServoyException
	{
		IFoundSet foundset = application.getFoundSetManager().getFoundSet(datasource);
		((FoundSet)foundset).loadByQuery(query, args);

		return checkDataSourceEquality(foundset) ? foundset : null;
	}

	/**
	 * get a new foundset containing records based on an SQL query string.
	 *
	 * @sample
	 * var query = "SELECT * FROM public.orders WHERE customerid = 'ROMEY' ORDER BY orderid ASC";
	 * var fs = datasources.db.example_data.orders.loadRecords(query);
	 *
	 * @param query an SQL query
	 * @return a new JSFoundset
	 * @throws ServoyException
	 */
	@JSFunction
	public IFoundSet loadRecords(String query) throws ServoyException
	{
		return loadRecords(query, null);
	}

	/**
	 * get a new foundset containing records based on a dataset of pks.
	 *
	 * @sample var fs = datasources.db.example_data.customers.loadRecords(pkDataSet)
	 *
	 * @param dataSet
	 * @return a new JSFoundset
	 * @throws ServoyException
	 */
	@JSFunction
	public IFoundSet loadRecords(IDataSet dataSet) throws ServoyException
	{
		IFoundSet foundset = application.getFoundSetManager().getFoundSet(datasource);
		((FoundSet)foundset).js_loadRecords(dataSet);

		return checkDataSourceEquality(foundset) ? foundset : null;
	}

	/**
	 * check whether a foundset is not null and comes from this datasource
	 *
	 * @param foundset
	 * @return true if not null and datasource matches
	 */
	private boolean checkDataSourceEquality(IFoundSet foundset)
	{
		return (foundset != null && Utils.equalObjects(foundset.getDataSource(), datasource));
	}

	/**
	 * Get the column names of a datasource.
	 *
	 * @return String[] column names
	 */
	@JSFunction
	public String[] getColumnNames()
	{
		try
		{
			if (datasource.startsWith(DataSourceUtils.INMEM_DATASOURCE_SCHEME_COLON))
			{
				return Arrays.stream(application.getFoundSetManager().getTable(datasource).getDataProviderIDs()) //
					.filter(name -> !Column._SV_ROWID.equals(name)) //
					.toArray(String[]::new);
			}
			return application.getFoundSetManager().getTable(datasource).getDataProviderIDs();
		}
		catch (RepositoryException e)
		{
			Debug.log(e);
		}
		return null;
	}

	/**
	 * Get the table of a datasource.
	 *
	 * @return JSTable table
	 */
	@JSFunction
	public JSTable getTable()
	{
		try
		{
			ITable table = application.getFoundSetManager().getTable(datasource);
			IServer server = application.getSolution().getServer(table.getServerName());
			if (server != null)
			{
				return new JSTable(table, server);
			}
		}
		catch (RepositoryException e)
		{
			Debug.log(e);
		}
		return null;
	}

	/**
	 *  Create a query builder for a data source.
	 *
	 *  @sample
	 *  var q = datasources.db.example_data.book_nodes.createSelect()
	 *  q.result.addPk()
	 *  q.where.add(q.columns.label_text.not.isin(null))
	 *  datasources.db.example_data.book_nodes.getFoundSet().loadRecords(q)
	 *
	 *  @return query builder
	 *
	 */
	@JSFunction
	public QBSelect createSelect() throws RepositoryException
	{
		return (QBSelect)application.getFoundSetManager().getQueryFactory().createSelect(datasource);
	}

	/**
	 *  Create a query builder for a data source with given table alias.
	 *  The alias can be used inside custom queries to bind to the outer table.
	 *
	 *  @sample
	 *  var q = datasources.db.example_data.book_nodes.createSelect('b')
	 *  q.result.addPk()
	 *  q.where.add(q.columns.label_text.isin('select comment_text from book_text t where t.note_text = ? and t.node_id = b.node_id', ['test']))
	 *  datasources.db.example_data.book_nodes.getFoundSet().loadRecords(q)
	 *
	 *  @param tableAlias the table alias to use
	 *
	 *  @return query builder
	 *
	 */
	@JSFunction
	public QBSelect createSelect(String tableAlias) throws RepositoryException
	{
		return (QBSelect)application.getFoundSetManager().getQueryFactory().createSelect(datasource, tableAlias);
	}

	@Override
	public void destroy()
	{
		application = null;
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + '(' + datasource + ')';
	}
}
