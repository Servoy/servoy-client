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

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.scripting.DefaultJavaScope;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;

/**
 * Scope for datasources.db.myserver.
 * 
 * @author rgansevles
 * 
 * @since 7.4
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class DBDataSourceServer extends DefaultJavaScope
{
	private static Map<String, NativeJavaMethod> jsFunctions = DefaultJavaScope.getJsFunctions(DBDataSourceServer.class);
	private volatile IApplication application;

	private final String serverName;

	DBDataSourceServer(IApplication application, String serverName)
	{
		super(application.getScriptEngine().getSolutionScope(), jsFunctions);
		this.application = application;
		this.serverName = serverName;
	}

	@Override
	protected boolean fill()
	{
		// table and view names
		try
		{
			IServer server = application.getRepository().getServer(serverName);
			if (server != null)
			{
				for (String tableName : server.getTableAndViewNames(false))
				{
					put(tableName, this, new JSDataSource(application, DataSourceUtils.createDBTableDataSource(serverName, tableName)));
				}
			}
		}
		catch (RemoteException e)
		{
			Debug.error(e);
		}
		catch (RepositoryException e)
		{
			Debug.error(e);
		}

		return true;
	}

	/**
	 * Get the server name.
	 * 
	 * @sample
	 * datasources.db.example_data.getServerName() // returns 'example_data' 
	 * 
	 * @return String server name
	 */
	@JSFunction
	public String getServerName()
	{
		return serverName;
	}

	/**
	 * Get server table names.
	 *
	 * @sample
	 * datasources.db.example_data.getTableNames()
	 *
	 * @return List<String> server  table names;
	 */
	@JSFunction
	public List<String> getTableNames()
	{
		try
		{
			IServerInternal server = (IServerInternal)application.getRepository().getServer(serverName);
			if (server != null)
			{
				return server.getTableNames(false);
			}
		}
		catch (RemoteException e)
		{
			Debug.error(e);
		}
		catch (RepositoryException e)
		{
			Debug.error(e);
		}
		return null;
	}

	/**
	 * Get the server where this server is a data model clone from.
	 * 
	 * @sample
	 * datasources.db.example_data99.getDataModelCloneFrom().getServerName()
	 * 
	 * @return DBDataSourceServer server
	 */
	@JSFunction
	public DBDataSourceServer getDataModelCloneFrom()
	{
		try
		{
			IServerInternal server = (IServerInternal)application.getRepository().getServer(serverName);
			if (server != null)
			{
				return new DBDataSourceServer(application, server.getConfig().getDataModelCloneFrom());
			}
		}
		catch (RemoteException e)
		{
			Debug.error(e);
		}
		catch (RepositoryException e)
		{
			Debug.error(e);
		}
		return null;
	}

	@Override
	public void destroy()
	{
		application = null;
		super.destroy();
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + '(' + serverName + ')';
	}
}
