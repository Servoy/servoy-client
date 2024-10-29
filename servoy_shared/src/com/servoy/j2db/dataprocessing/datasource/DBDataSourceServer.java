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

import org.eclipse.dltk.rhino.dbgp.LazyInitScope;
import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.ITableListener;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.scripting.DefaultJavaScope;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;

/**
 * <pre data-puremarkdown>
`DBDataSourceServer` provides runtime access to a database server within Servoy applications, accessible via `datasources.db.<server_name>`. This object enables interaction with database servers, including the ability to define client-specific connections, retrieve server names, and access tables within the server.

For further details on configuring connections, refer to the [JSConnectionDefinition](./jsconnectiondefinition.md) documentation.
 * <pre>
 *
 * @author rgansevles
 *
 * @since 7.4
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class DBDataSourceServer extends DefaultJavaScope implements LazyInitScope
{
	private static final Map<String, NativeJavaMethod> jsFunctions = DefaultJavaScope.getJsFunctions(DBDataSourceServer.class);
	private volatile IApplication application;

	private final String serverName;
	private ITableListener tableListener;

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
			if (server instanceof IServerInternal)
			{
				tableListener = new ITableListener.TableListener()
				{
					@Override
					public void tablesRemoved(IServerInternal server, ITable[] tables, boolean deleted)
					{
						for (ITable table : tables)
						{
							DBDataSourceServer.this.remove(table.getName());
						}
					}

					@Override
					public void tablesAdded(IServerInternal server, String[] tableNames)
					{
						for (String tableName : tableNames)
						{
							DBDataSourceServer.this.put(tableName, DBDataSourceServer.this,
								new JSDataSource(application, DataSourceUtils.createDBTableDataSource(serverName, tableName)));
						}
					}
				};
				((IServerInternal)server).addTableListener(tableListener);
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

	@Override
	public Object[] getInitializedIds()
	{
		return getRealIds();
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
	 * Returns an array with the names of all tables of this server.
	 *
	 * @sample
	 * datasources.db.example_data.getTableNames()
	 *
	 * @return String[] server table names;
	 */
	@JSFunction
	public String[] getTableNames()
	{
		try
		{
			IServerInternal server = (IServerInternal)application.getRepository().getServer(serverName);
			if (server != null)
			{
				List<String> tableNames = server.getTableNames(false);
				return tableNames != null ? tableNames.toArray(new String[tableNames.size()]) : new String[] { };
			}
		}
		catch (RepositoryException e)
		{
			Debug.error(e);
		}
		return new String[] { };
	}

	/**
	 * Get the server where this server is a data model clone from.
	 *
	 * @sample
	 * datasources.db.example_data.getDataModelCloneFrom().getServerName()
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
		catch (RepositoryException e)
		{
			Debug.error(e);
		}
		return null;
	}

	/**
	 * Define a client connection for this server, you can configure the Database Server to create connections for current client using properties of this JSConnectionDefinition.
	 * All interaction with the database will go over a connection coming from a specific client pool with that is created for this client.
	 * Things like username, password or connection properties can be adjusted.
	 *
	 * @sample
	 * var conncetionDefinition = datasources.db.example_data.defineClientConnection().setProperty('key', 'value').create();
	 *
	 * @since 2021.06
	 *
	 * @return DBDataSourceServer server
	 */
	@JSFunction
	public JSConnectionDefinition defineClientConnection()
	{
		try
		{
			IServer server = application.getRepository().getServer(serverName);
			if (server != null)
			{
				return new JSConnectionDefinition(server, application.getClientID());
			}
		}
		catch (RepositoryException e)
		{
			Debug.error("Can't define a client connection definiton for " + serverName, e); //$NON-NLS-1$
		}
		return null;
	}

	@Override
	public void destroy()
	{
		if (tableListener != null && application.getRepository() != null)
		{
			try
			{
				IServer server = application.getRepository().getServer(serverName);
				if (server instanceof IServerInternal)
				{
					((IServerInternal)server).removeTableListener(tableListener);
				}
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
		application = null;
		tableListener = null;
		super.destroy();
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + '(' + serverName + ')';
	}
}
