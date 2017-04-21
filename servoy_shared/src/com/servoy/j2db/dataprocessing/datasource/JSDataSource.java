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

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.IFoundSet;
import com.servoy.j2db.dataprocessing.JSTable;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.querybuilder.impl.QBSelect;
import com.servoy.j2db.scripting.IJavaScriptType;
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
	 * Returns a named foundset object for a specified name. If named foundset datasource does not match current datasource will not be returned.
	 * If named foundset is not created will return null.
	 *
	 * @sample
	 * var fs = datasources.db.example_data.orders.getFoundSet('myname')
	 * var ridx = fs.newRecord()
	 * var record = fs.getRecord(ridx)
	 * record.emp_name = 'John'
	 * databaseManager.saveData()
	 *
	 * @return An existing named foundset for the datasource.
	 */
	@JSFunction
	public IFoundSet getFoundSet(String name) throws ServoyException
	{
		IFoundSet foundset = application.getFoundSetManager().getNamedFoundSet(name);
		if (Utils.equalObjects(foundset.getDataSource(), datasource))
		{
			return foundset;
		}
		return null;
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
