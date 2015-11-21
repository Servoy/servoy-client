/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.j2db.util;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.servoy.base.util.DataSourceUtilsBase;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerManager;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServerSingleton;
import com.servoy.j2db.util.keyword.Ident;


/**
 * <br>
 * <br>
 * Normal Use: static methods <br>
 * <br>
 *
 * @author jblok
 */
public class DataSourceUtils extends DataSourceUtilsBase
{
	public static final String INMEM_DATASOURCE = "mem"; //$NON-NLS-1$
	public static final String INMEM_DATASOURCE_SCHEME_COLON = INMEM_DATASOURCE + ':';

	private DataSourceUtils()
	{
	}

	/**
	 * Get the server and table name from the datasource (when is is a db datasource)
	 *
	 * @param dataSource the dataSource
	 * @return the server and table name (or null if not a db datasource)
	 */
	public static String[] getDBServernameTablename(String dataSource)
	{
		return DataSourceUtilsBase.getDBServernameTablename(dataSource);
	}

	/**
	 * Create the a database data source string from server and table. Normalizes tableName if needed.
	 *
	 * @param serverName the serverName
	 * @param tableName the tableName
	 * @return the datasource
	 */
	public static String createDBTableDataSource(String serverName, String tableName)
	{
		// when importing from some 3.5 solutions through introspection, name might not be the lower-case name (not sure if it also happens with newer exports);
		// it will be normalized here - as data sources should point to table name not to table SQL name
		if (tableName != null) tableName = Ident.generateNormalizedName(tableName);
		return DataSourceUtilsBase.createDBTableDataSource(serverName, tableName);
	}

	/**
	 * Create the a data source string for an inmemory table
	 *
	 * @param name
	 * @return the datasource string
	 */
	public static String createInmemDataSource(String name)
	{
		if (name == null) return null;
		return new StringBuilder().append(INMEM_DATASOURCE_SCHEME_COLON).append(name).toString();
	}

	/**
	 * Get the datasource name from the in-mem datasource uri.
	 *
	 * @param dataSource the dataSource
	 * @return the server and table name (or null if not a db datasource)
	 */
	public static String getInmemDataSourceName(String dataSource)
	{
		// mem:name
		if (dataSource != null && dataSource.startsWith(INMEM_DATASOURCE_SCHEME_COLON))
		{
			return dataSource.substring(INMEM_DATASOURCE_SCHEME_COLON.length());
		}
		return null;
	}

	public static String getDataSourceServerName(String dataSource)
	{
		if (dataSource == null) return null;
		String[] stn = getDBServernameTablename(dataSource);
		if (stn != null && stn[0] != null)
		{
			return stn[0];
		}
		if (dataSource.startsWith(INMEM_DATASOURCE_SCHEME_COLON))
		{
			return IServer.INMEM_SERVER;
		}
		return null;
	}

	public static String getDataSourceTableName(String dataSource)
	{
		if (dataSource == null) return null;
		String[] stn = getDBServernameTablename(dataSource);
		if (stn != null && stn[1] != null)
		{
			return stn[1];
		}
		if (dataSource.startsWith(INMEM_DATASOURCE_SCHEME_COLON))
		{
			return dataSource.substring(INMEM_DATASOURCE_SCHEME_COLON.length());
		}
		return null;
	}

	/**
	 * Get the sorted set of server names used server names in the data sources list
	 *
	 * @param dataSources server names
	 * @return the sorted set of server names
	 */
	public static SortedSet<String> getServerNames(Set<String> dataSources)
	{
		SortedSet<String> serverNames = new TreeSet<String>();
		for (String ds : dataSources)
		{
			String serverName = getDataSourceServerName(ds);
			if (serverName != null)
			{
				serverNames.add(serverName);
			}
		}
		return serverNames;
	}

	/**
	 * Get the table names with the given server name in the data sources list
	 *
	 * @param dataSources
	 * @param serverName
	 * @return the sorted set of table names
	 */
	public static List<String> getServerTablenames(Set<String> dataSources, String serverName)
	{
		List<String> tableNames = new ArrayList<String>();
		for (String ds : dataSources)
		{
			String[] stn = getDBServernameTablename(ds);
			if (stn != null && serverName.equals(stn[0]))
			{
				tableNames.add(stn[1]);
			}
		}
		return tableNames;
	}


	/**
	 * @param dataSource1
	 * @param dataSource2
	 * @return true if the datasources desginate the same server, false otherwise
	 */
	public static boolean isSameServer(String dataSource1, String dataSource2)
	{
		if (dataSource1 == null || dataSource2 == null)
		{
			return false;
		}
		String server1 = getDataSourceServerName(dataSource1);
		return server1 != null && server1.equals(getDataSourceServerName(dataSource2));
	}

	/**
	 * Silently tries to get a table from the given datasource. If either server or table do not exist or something unexpected happens
	 * it will return null.<br>
	 *
	 * It will try to get the server from the following sources in this order: rootObject, serverManager, app. server singleton's server manager, current client's foundset manager (in case it's not a standard server/table combo).
	 *
	 * @param rootObject if a solution should be used to get the server.
	 * @param serverManager if this serverManager should be used to get the server.
	 */
	public static ITable getTable(String dataSource, IRootObject rootObject, IServerManager serverManager)
	{
		ITable t = null;
		if (dataSource != null)
		{
			String[] stn = DataSourceUtilsBase.getDBServernameTablename(dataSource);
			if (stn != null)
			{
				try
				{
					IServer server;
					if (rootObject != null)
					{
						server = rootObject.getServer(stn[0]);
					}
					else
					{
						IServerManager sm = serverManager;
						if (sm == null)
						{
							// use default
							IApplicationServerSingleton as = ApplicationServerRegistry.get();
							sm = as != null ? as.getServerManager() : null;
						}
						server = sm != null ? sm.getServer(stn[0], true, true) : null;
					}
					if (server != null)
					{
						t = server.getTable(stn[1]);
					}
				}
				catch (RepositoryException e)
				{
					Debug.error(e);
				}
				catch (RemoteException e)
				{
					Debug.error(e);
				}
			}
			else
			{
				// not a server/table combi, ask the current clients foundset manager
				if (J2DBGlobals.getServiceProvider() != null)
				{
					try
					{
						t = J2DBGlobals.getServiceProvider().getFoundSetManager().getTable(dataSource);
					}
					catch (RepositoryException e)
					{
						Debug.error(e);
					}
				}
			}
		}

		return t;
	}

	public static boolean isDatasourceUri(String str)
	{
		return str != null && (str.startsWith(DB_DATASOURCE_SCHEME_COLON_SLASH) || str.startsWith(INMEM_DATASOURCE_SCHEME_COLON));
	}

	public static String getI18NDataSource(Solution solution, Properties settings)
	{
		if (solution != null)
		{
			if (solution.getI18nDataSource() != null)
			{
				return solution.getI18nDataSource();
			}
			String serverName = settings.getProperty("defaultMessagesServer"); //$NON-NLS-1$
			String tableName = settings.getProperty("defaultMessagesTable"); //$NON-NLS-1$
			if (serverName != null && serverName.length() > 0 && tableName != null && tableName.length() > 0)
			{
				return createDBTableDataSource(serverName, tableName);
			}
		}
		return null;
	}
}
