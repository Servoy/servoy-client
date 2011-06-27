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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.servoy.j2db.util.keyword.Ident;


/**
 * <br>
 * <br>
 * Normal Use: static methods <br>
 * <br>
 * 
 * @author jblok
 */
public class DataSourceUtils
{
	public static final String DB_DATASOURCE_SCHEME = "db"; //$NON-NLS-1$
	public static final String DB_DATASOURCE_SCHEME_COLON_SLASH = DB_DATASOURCE_SCHEME + ":/"; //$NON-NLS-1$
	public static final int DB_DATASOURCE_SCHEME_COLON_SLASH_LENGTH = DB_DATASOURCE_SCHEME_COLON_SLASH.length();
	public static final String INMEM_DATASOURCE_SCHEME = "mem"; //$NON-NLS-1$

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
		// db:/srv/tab
		if (dataSource != null)
		{
			if (dataSource.startsWith(DataSourceUtils.DB_DATASOURCE_SCHEME_COLON_SLASH))
			{
				int slash = dataSource.indexOf('/', DataSourceUtils.DB_DATASOURCE_SCHEME_COLON_SLASH_LENGTH);
				return new String[] {
				// serverName
				(slash <= DataSourceUtils.DB_DATASOURCE_SCHEME_COLON_SLASH_LENGTH) ? null : dataSource.substring(
					DataSourceUtils.DB_DATASOURCE_SCHEME_COLON_SLASH_LENGTH, slash),
				// tableName
				(slash < 0 || slash == dataSource.length() - 1) ? null : dataSource.substring(slash + 1) };
			}
		}
		return null;
	}

	/**
	 * Create the a database data source string from server and table
	 * 
	 * @param serverName the serverName
	 * @param tableName the tableName
	 * @return the table name
	 */
	public static String createDBTableDataSource(String serverName, String tableName)
	{
		if (serverName == null && tableName == null)
		{
			return null;
		}
		StringBuilder sb = new StringBuilder();
		sb.append(DataSourceUtils.DB_DATASOURCE_SCHEME_COLON_SLASH);
		if (serverName != null)
		{
			sb.append(serverName);
		}
		sb.append('/');
		if (tableName != null)
		{
			// when importing from some 3.5 solutions through introspection, name might not be the lower-case name (not sure if it also happens with newer exports);
			// it will be normalized here - as data sources should point to table name not to table SQL name
			sb.append(Ident.generateNormalizedName(tableName));
		}
		return sb.toString();
	}

	/**
	 * Create the a data source string for an inmemory table
	 * 
	 * @param name
	 * @return
	 */
	public static String createInmemDataSource(String name)
	{
		if (name == null) return null;
		return new StringBuilder().append(DataSourceUtils.INMEM_DATASOURCE_SCHEME).append(':').append(name).toString();
	}

	/**
	 * Get the sorted set of server names used server names in the data sources list
	 * 
	 * @param server names
	 * @return
	 */
	public static SortedSet<String> getServerNames(Set<String> dataSources)
	{
		SortedSet<String> serverNames = new TreeSet<String>();
		for (String ds : dataSources)
		{
			String[] stn = DataSourceUtils.getDBServernameTablename(ds);
			if (stn != null && stn[0] != null)
			{
				serverNames.add(stn[0]);
			}
		}
		return serverNames;
	}

	/**
	 * Get the table names with the given server name in the data sources list
	 * 
	 * @param table names
	 * @return
	 */
	public static List<String> getServerTablenames(Set<String> dataSources, String serverName)
	{
		List<String> tableNames = new ArrayList<String>();
		for (String ds : dataSources)
		{
			String[] stn = DataSourceUtils.getDBServernameTablename(ds);
			if (stn != null && serverName.equals(stn[0]))
			{
				tableNames.add(stn[1]);
			}
		}
		return tableNames;
	}
}
