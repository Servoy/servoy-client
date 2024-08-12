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
package com.servoy.j2db.dataprocessing;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.mozilla.javascript.Wrapper;
import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.scripting.IJavaScriptType;
import com.servoy.j2db.scripting.IReturnedTypesProvider;
import com.servoy.j2db.util.Debug;

/**
 * JSTable represents a DB Table object - when using the maintenance plugin or databaseManager.
 *
 * @author jblok
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class JSTable implements IReturnedTypesProvider, Wrapper, IJavaScriptType
{
	private ITable table;
	private IServer server;

	public JSTable()
	{
	} //only for use JS engine

	public JSTable(ITable table, IServer server)
	{
		this.table = table;
		this.server = server;
	}

	/**
	 * Returns the table name as defined in the database.
	 *
	 * @sample
	 * var jsTable = databaseManager.getTable('udm', 'campaigns')
	 * var tableNameForQuery = jsTable.getSQLName()
	 *
	 * @return String table sql name.
	 */
	public String js_getSQLName()
	{
		return table.getSQLName();
	}

	/**
	 * Returns a quoted version of the table name, if necessary, as defined by the actual database used.
	 *
	 * @sample
	 * //use with the raw SQL plugin:
	 * //if the table name contains characters that are illegal in sql, the table name will be quoted
	 * var jsTable = databaseManager.getTable('udm', 'campaigns')
	 * var quotedTableName = jsTable.getQuotedSQLName()
	 * plugins.rawSQL.executeSQL('udm',  quotedTableName,  'select * from ' + quotedTableName + ' where is_active = ?', [1])
	 *
	 * @return String table name, quoted if needed.
	 */
	public String js_getQuotedSQLName()
	{
		try
		{
			return server.getQuotedIdentifier(table.getSQLName(), null);
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return null;
	}

	/**
	 * Returns the Servoy server name.
	 *
	 * @sample
	 * var jsTable = databaseManager.getTable('udm', 'campaigns')
	 * var serverName = jsTable.getServerName()
	 *
	 * @return String server name.
	 */
	public String js_getServerName()
	{
		return table.getServerName();
	}

	/**
	 * Returns the table data source uri.
	 *
	 * @sample
	 * var jsTable = databaseManager.getTable('udm', 'campaigns')
	 * var dataSource = jsTable.getDataSource()
	 *
	 * @return String datasource uri.
	 */
	public String js_getDataSource()
	{
		return table.getDataSource();
	}

	/**
	 * Returns an array containing the names of all table columns.
	 * If the table is in mem, then the internal rowid column name is not returned.
	 *
	 * @sample
	 * var jsTable = databaseManager.getTable('udm', 'campaigns')
	 * var columnNames = jsTable.getColumnNames()
	 *
	 * @return String array of column names.
	 */
	public String[] js_getColumnNames()
	{
		if (table.getServerName().equals(IServer.INMEM_SERVER))
		{
			return Arrays.stream(table.getDataProviderIDs()) //
				.filter(name -> !Column._SV_ROWID.equals(name)) //
				.toArray(String[]::new);
		}
		return table.getDataProviderIDs();
	}

	/**
	 * Returns an array containing the names of the identifier (PK) column(s).
	 *  Please note that if the table is in mem, then the internal rowid column name is also returned.
	 *
	 * @sample
	 * var jsTable = databaseManager.getTable('udm', 'campaigns')
	 * var identifierColumnNames = jsTable.getRowIdentifierColumnNames()
	 *
	 * @return String array of row identifier column names.
	 */
	public String[] js_getRowIdentifierColumnNames()
	{
		List<String> list = new ArrayList<String>();
		for (Column c : ((Table)table).getRowIdentColumns())
		{
			list.add(c.getName());
		}
		return list.toArray(new String[list.size()]);
	}

	/**
	 * Returns a JSColumn for the named column (or column dataproviderID).
	 *
	 * @sample
	 * var jsTable = databaseManager.getTable('udm', 'campaigns')
	 * var jsColumn = jsTable.getColumn('campaign_name')
	 *
	 * @param name The name of the column to return the value from.
	 *
	 * @return JSColumn column.
	 */
	public JSColumn js_getColumn(String name)
	{
		Column c = ((Table)table).getColumn(name);
		if (c != null)
		{
			return new JSColumn(c, server, table);
		}
		return null;
	}

	/**
	 * Returns whether table was flagged as metadata table.
	 *
	 * @sample
	 * var jsTable = databaseManager.getTable('udm', 'campaigns')
	 * var isMetaDataTable = jsTable.isMetadataTable()
	 *
	 * @return boolean is metadata
	 */
	@JSFunction
	public boolean isMetadataTable()
	{
		return ((Table)table).isMarkedAsMetaData();
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("JSTable[name:"); //$NON-NLS-1$
		sb.append(table.getName());
		sb.append(",columns:"); //$NON-NLS-1$
		String[] columnNames = table.getDataProviderIDs();
		for (String columnName : columnNames)
		{
			sb.append(columnName);
			sb.append(',');
		}
		sb.setLength(sb.length() - 1);
		sb.append(']');
		return sb.toString();
	}

	public Class< ? >[] getAllReturnedTypes()
	{
		return null;
	}

	/**
	 * @see org.mozilla.javascript.Wrapper#unwrap()
	 */
	public Object unwrap()
	{
		return table;
	}

	public ITable getTable()
	{
		return table;
	}

	public IServer getServer()
	{
		return server;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof JSTable)
		{
			return table.equals(((JSTable)obj).table);

		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		return table.hashCode();
	}
}
