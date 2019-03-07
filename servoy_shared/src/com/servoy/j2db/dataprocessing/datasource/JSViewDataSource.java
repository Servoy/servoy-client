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
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.querybuilder.impl.QBSelect;
import com.servoy.j2db.scripting.IJavaScriptType;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IDestroyable;
import com.servoy.j2db.util.ServoyException;

/**
 * @author emera
 */
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
	 * Returns a foundset object for a specified datasource or server and tablename.
	 * It is important to note that this is a FACTORY method, it constantly creates new foundsets.
	 *
	 * @sample
	 * var fs = datasources.view.x.orders.getFoundSet()
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
	 * Creates a view foundset with the provided query and automatically registers it.
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
		ViewFoundSet viewFoundSet = application.getFoundSetManager().getViewFoundSet(DataSourceUtils.getViewDataSourceName(datasource), query);
		if (register && viewFoundSet != null)
		{
			application.getFoundSetManager().registerViewFoundSet(viewFoundSet);
		}
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
