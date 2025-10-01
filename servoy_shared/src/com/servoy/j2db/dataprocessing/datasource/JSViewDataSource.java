/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2019 Servoy BV

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
import com.servoy.j2db.dataprocessing.ViewFoundSet;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.querybuilder.impl.QBSelect;
import com.servoy.j2db.scripting.IJavaScriptType;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IDestroyable;
import com.servoy.j2db.util.ServoyException;

/**
 * <p>A <code>JSViewDataSource</code> provides methods for managing and manipulating view-based
 * data sources in Servoy. It enables the retrieval of column names, creation and management of
 * view foundsets, and dynamic querying capabilities. Methods include obtaining column names
 * through <code>getColumnNames()</code>, fetching the data source string with
 * <code>getDataSource()</code>, and accessing the <code>ViewFoundSet</code> with
 * <code>getFoundSet()</code>.</p>
 *
 * <p>Additionally, <code>getViewFoundSet(query)</code> creates and optionally registers view
 * foundsets from a specified query object. The system retains registered view foundsets in memory
 * until explicitly disposed of using <code>dispose()</code>. The <code>unregister()</code> method
 * facilitates removing view foundsets associated with the datasource, optimizing memory management.</p>
 *
 * <p>For more details, refer to
 * <a href="https://docs.servoy.com/reference/servoycore/dev-api/datasources/viewdatasource">ViewDataSource Documentation</a>.</p>
 *
 * @author emera
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class JSViewDataSource implements IJavaScriptType, IDestroyable
{
	private volatile IApplication application;
	private final String datasource;

	public JSViewDataSource(IApplication application, String datasource)
	{
		this.application = application;
		this.datasource = datasource;
	}

	/**
	 * Get the datasource string.
	 *
	 * @sample
	 * datasources.view.orders.getDataSource() // returns 'view:orders'
	 *
	 * @return String datasource
	 */
	@JSFunction
	public String getDataSource()
	{
		return datasource;
	}

	/**
	 * Returns the ViewFoundSet that was previously created and registered by a call t o {@link #getViewFoundSet(QBSelect)} or {@link #getViewFoundSet(QBSelect, boolean)} with the register boolean to true.
	 * It will return null when it can't find a ViewFoundSet for this datasource.
	 *
	 * @sample
	 * var fs = datasources.view.x.orders.getFoundSet()
	 * var record = fs.getRecord(1)
	 * // changes to records can only be done for ViewFoundSets that also do have the pk selected for the table of the column you change.
	 * record.emp_name = 'John'
	 * fs.save(record);
	 *
	 * @return A new ViewFoundSet  for the datasource.
	 */
	@JSFunction
	public ViewFoundSet getFoundSet() throws ServoyException
	{
		IFoundSet foundSet = application.getFoundSetManager().getFoundSet(datasource);
		if (foundSet instanceof ViewFoundSet) return (ViewFoundSet)foundSet;
		return null;
	}

	/**
	 * Creates a view foundset with the provided query and automatically registers it.
	 * Registered ViewFoundSets will be kept in memory by the system until ViewFoundSet.dispose() is called.
	 *
	 * @param query a QBSelect query object
	 *
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.result.add(query.columns.shipname);
	 * query.result.add(query.columns.orderid);
	 * query.where.add(query.columns.orderid.le(1280));
	 *
	 * var viewfs = datasources.view.a.getViewFoundSet(query);
	 *
	 * @return the registered view foundset, or null if the datasource columns do not match with the query columns
	 */
	@JSFunction
	public ViewFoundSet getViewFoundSet(QBSelect query)
	{
		return getViewFoundSet(query, true);
	}

	/**
	 * Creates a view foundset with the provided query and the option to register it.
	 * A registered ViewFoundSets will be kept in memory by the system until ViewFoundSet.dispose() is called.
	 *
	 * @param query a QBSelect query object
	 * @param register boolean
	 *
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.result.add(query.columns.shipname);
	 * query.result.add(query.columns.orderid);
	 * query.where.add(query.columns.orderid.le(1280));
	 *
	 * var viewfs = datasources.view.a.getViewFoundSet(query, false);
	 *
	 * @return the registered view foundset, or null if the datasource columns do not match with the query columns
	 */
	@JSFunction
	public ViewFoundSet getViewFoundSet(QBSelect query, boolean register)
	{
		ViewFoundSet viewFoundSet = application.getFoundSetManager().getViewFoundSet(DataSourceUtils.getViewDataSourceName(datasource), query, register);
		return viewFoundSet;
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
	 * Unregisters a view foundset associated to this view datasource.
	 *
	 * @sample
	 * datasources.view.a.unregister();
	 *
	 * @return true if the view foundset was unregistered, false otherwise
	 */
	@JSFunction
	public boolean unregister()
	{
		return application.getFoundSetManager().unregisterViewFoundSet(datasource);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.util.IDestroyable#destroy()
	 */
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
