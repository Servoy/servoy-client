/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2022 Servoy BV

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

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.scripting.IJavaScriptType;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;

/**
 * A table filter that can be created and used via a JSFoundset.
 *
 * @author rgansevles
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, scriptingName = "JSTableFilter")
public class JSTableFilter implements IJavaScriptType
{
	private final ITable table;
	private final TableFilterdefinition tableFilterdefinition;
	private final String serverName;
	private boolean dataBroadcast;

	public JSTableFilter(String serverName, ITable table, TableFilterdefinition tableFilterdefinition)
	{
		this.serverName = serverName;
		this.table = table;
		this.tableFilterdefinition = tableFilterdefinition;
	}

	/**
	 * Returns the server name.
	 *
	 * @sample
	 * var filter = databaseManager.createTableFilterParam('admin', 'messages', 'messagesid', '>', 10)
	 * var serverName = filter.serverName // admin
	 *
	 * @see com.servoy.j2db.dataprocessing.JSDatabaseManager#js_setTableFilters(String, JSTableFilter[])
	 *
	 * @return String server name.
	 */
	@JSReadonlyProperty
	public String getServerName()
	{
		return serverName;
	}

	/**
	 * @return the table
	 */
	public ITable getTable()
	{
		return table;
	}

	/**
	 * Returns the table name.
	 *
	 * @sample
	 * var filter = databaseManager.createTableFilterParam('admin', 'messages', 'messagesid', '>', 10)
	 * var tableName = filter.tableName // messages
	 *
	 * @see com.servoy.j2db.dataprocessing.JSDatabaseManager#js_setTableFilters(String, JSTableFilter[])
	 *
	 * @return String table name.
	 */
	@JSReadonlyProperty
	public String getTableName()
	{
		return table == null ? null : table.getName();
	}

	/** Set the dataBroadcast flag.
	 * <p>
	 * When the dataBroadcast flag is set, this filter will be used server-side to reduce databroadcast events
	 * for clients having a databroadcast filter set for the same column with a different value.
	 * <p>
	 * Note that the dataBroadcast flag is *only* supported for simple filters, only for operator 'in' or '='.
	 *
	 * @sample
	 * var filter = databaseManager.createTableFilterParam('example', 'orders', 'clusterid', '=', 10).dataBroadcast(true)
	 *
	 * @see com.servoy.j2db.dataprocessing.JSDatabaseManager#js_setTableFilters(String, JSTableFilter[])
	 *
	 * @param broadcast Boolean set to true this is a databroadcast filtering filter. (default false)
	 *
	 * @return filter.
	 */
	@JSFunction
	public JSTableFilter dataBroadcast(boolean broadcast)
	{
		this.dataBroadcast = broadcast;
		return this;
	}

	/**
	 * @return the tableFilterdefinition
	 */
	public TableFilterdefinition getTableFilterdefinition()
	{
		return tableFilterdefinition;
	}

	/**
	 * @return the dataBroadcast
	 */
	public boolean getDataBroadcast()
	{
		return dataBroadcast;
	}

}
