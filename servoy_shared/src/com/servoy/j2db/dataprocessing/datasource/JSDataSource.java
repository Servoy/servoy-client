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

/**
 * Scope for datasources.db.myserver.mytable or datasources.mem['dsname'].
 *
 * @author rgansevles
 *
 * @since 7.4
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class JSDataSource implements IJavaScriptType, IDestroyable
{
	private static final String SINGLE_RECORD_NAMED_FOUNDSET = "__sv_singleRecordFoundset";

	private volatile IApplication application;
	private final String datasource;

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
	 * An existing foundset under that name will be returned, or created.
	 * If there is a definition (there is a form with a named foundset property with that name), the initial sort from that form will be used.
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
		return application.getFoundSetManager().getNamedFoundSet(name, datasource);
	}

	/**
	 * Returns a foundset object for a specified pk base query. This creates a filtered "view" on top of the database table based on that query.
	 *
	 * This foundset is different then when doing foundset.loadRecords(query) or datasources.db.server.table.loadRecords(query) because this is generated as a "view"
	 * Which means that the foundset will always have this query as its  base, even when doing foundset.loadAllRecords() afterwards. Because this query is set as its "creation query"
	 * JSFoundset.loadRecords(query) does set that query on the current foundset as a a "search" condition. which will be removed when doing a loadAllRecords().
	 *
	 *  So doing a clear() on a foundse created by this call will just add a "search" condition that results in no records found ( 1 = 2) and then loadAllRecords() will go back to this query.
	 *  But in a foundset.loadRecord(query) then clear() will overwrite the "search" condition which is the given query so the query will be lost after that so loadAllRecords() will go back to all records in the table)
	 *
	 * @sample
	 * var qb = datasources.db.example_data.orders.createSelect();
	 * qb.result.addPk();
	 * qb.where.add(qb.columns.product_id.eq(1))
	 * %%prefix%%foundset.loadRecords(qb);
	 *
	 * @param select The query to get the JSFoundset for.
	 *
	 * @return A new JSFoundset with that query as its base query.
	 *
	 * @since 2023.09
	 */
	@JSFunction
	public IFoundSet getFoundSet(QBSelect select) throws ServoyException
	{
		return application.getFoundSetManager().getFoundSet(select);
	}

	/** Get all currently foundsets for this datasource.
	 * <br></br>
	 * This method can be used to loop over foundset and programatically dispose them to clean up resources quickly.
	 *
	 * @sample
	 * var fslist = datasources.db.example_data.orders.getLoadedFoundSets()
	 * fslist.forEach(function(fs) {
	 *   if (shouldDispose(fs)) {
	 * 		fs.dispose()
	 *   }
	 * })
	 *
	 * @return An array of foundsets loaded for this datasource.
	 */
	@JSFunction
	public IFoundSet[] getLoadedFoundSets()
	{
		return application.getFoundSetManager().getAllLoadedFoundsets(datasource, false);
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
		FoundSet foundset = (FoundSet)application.getFoundSetManager().getNamedFoundSet(SINGLE_RECORD_NAMED_FOUNDSET, datasource);

		IDataSet dataSet = new BufferedDataSet();

		if ((pk instanceof Object[]))
		{
			dataSet.addRow((Object[])pk);
		}
		else
		{
			dataSet.addRow(new Object[] { pk });
		}

		if (!foundset.js_loadRecords(dataSet))
		{
			throw new ServoyException(ServoyException.INVALID_INPUT);
		}

		return foundset.js_getRecord(1);
	}

	/**
	 * get a new foundset containing records based on a QBSelect query that is given.
	 *
	 * This is just a shotcut for datasources.db.server.table.getFoundset() and then calling loadRecords(qbSelect) on the resulting foundset.
	 * So it has the same behavior as JSFoundset.loadRecords(qbselect) that is that the given query is set as a "search" condition on the existing query of the foundset.
	 * This means that if you do loadAllRecords() or calling clear() on it the qbselect conditon will also be removed.
	 * loadAllRecords() will revert back to the foundsets original query (see {@link #getFoundSet(QBSelect)}
	 * clear() will revert back to the original foundset query and add a "clear" condition to the query ( resulting in 1 = 2)
	 *
	 * @sample
	 * var qb = datasources.db.example_data.orders.createSelect();
	 * qb.result.addPk();
	 * qb.where.add(q.columns.product_id.eq(1))
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

		return foundset;
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

		return foundset;
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

		return foundset;
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
	public int hashCode()
	{
		return datasource.hashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof JSDataSource ds)
		{
			return ds.datasource.equals(datasource);
		}
		return false;
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + '(' + datasource + ')';
	}
}
