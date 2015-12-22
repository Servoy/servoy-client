/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.base.util;

/**
 * @author acostescu
 */
public class DataSourceUtilsBase
{

	public static final String DB_DATASOURCE_SCHEME = "db"; //$NON-NLS-1$
	public static final String DB_DATASOURCE_SCHEME_COLON_SLASH = DB_DATASOURCE_SCHEME + ":/"; //$NON-NLS-1$

	/**
	 * Get the server and table name from the datasource (when is is a db datasource)
	 *
	 * @param dataSource the dataSource
	 * @return the server and table name (or null if not a db datasource)
	 */
	public static String[] getDBServernameTablename(String dataSource)
	{
		// db:/srv/tab
		if (dataSource != null && dataSource.startsWith(DB_DATASOURCE_SCHEME_COLON_SLASH))
		{
			int slash = dataSource.indexOf('/', DB_DATASOURCE_SCHEME_COLON_SLASH.length());
			return new String[] {
				// serverName
				(slash <= DB_DATASOURCE_SCHEME_COLON_SLASH.length()) ? null : dataSource.substring(DB_DATASOURCE_SCHEME_COLON_SLASH.length(), slash),
				// tableName
				(slash < 0 || slash == dataSource.length() - 1) ? null : dataSource.substring(slash + 1) };
		}
		return null;
	}

	/**
	 * Create the a database data source string from server and table
	 *
	 * @param serverName the serverName
	 * @param tableName the tableName
	 * @return the datasource
	 */
	public static String createDBTableDataSource(String serverName, String tableName)
	{
		if (serverName == null && tableName == null)
		{
			return null;
		}
		StringBuilder sb = new StringBuilder();
		sb.append(DB_DATASOURCE_SCHEME_COLON_SLASH);
		if (serverName != null)
		{
			sb.append(serverName);
		}
		sb.append('/');
		if (tableName != null)
		{
			sb.append(tableName);
		}
		return sb.toString();
	}

}
