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

import java.rmi.RemoteException;
import java.util.List;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.util.Debug;

/**
 * JavaScript wrapper object around {@link IServer}
 *
 * @author jblok
 * @see IServer
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class JSServer
{
	private final IServer server;
	private String serverName; // For logging exceptions.

	public JSServer(IServer server)
	{
		this.server = server;
		try
		{
			this.serverName = server.getName();
		}
		catch (RemoteException e)
		{
			Debug.error("Exception while retrieving server name.", e); //$NON-NLS-1$
			this.serverName = "N/A"; //$NON-NLS-1$
		}
	}

	/**
	 * Creates in this server a new table with the specified name.
	 *
	 * @sample
	 * var server = plugins.maintenance.getServer("example_data");
	 * if (server)
	 * {
	 * 	var table = server.createNewTable("new_table");
	 * 	if (table) {
	 * 		var pk = table.createNewColumn("new_table_id", JSColumn.INTEGER, 0);
	 *		pk.rowIdentifierType = JSColumn.PK_COLUMN;
	 * 		if (server.synchronizeWithDB(table))
	 * 			application.output("New table created in the database.");
	 * 		else
	 * 			application.output("New table not created in database.");
	 * 	}
	 * 	else application.output("New table not created at all.");
	 * }
	 *
	 * @param tableName The name of the table to create.
	 *
	 * @return JSTableObject created table.
	 */
	public JSTableObject js_createNewTable(String tableName)
	{
		try
		{
			ITable t = ((IServerInternal)server).createNewTable(null, tableName, false);
			if (t != null)
			{
				return new JSTableObject(t, server);
			}
		}
		catch (Exception e)
		{
			Debug.error("Exception while creating table '" + tableName + "' in server '" + serverName + "'.", e); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		}
		return null;
	}

	/**
	 * Returns a JSTable instance corresponding to the table with the specified name from this server.
	 *
	 * @sample
	 * var server = plugins.maintenance.getServer("example_data");
	 * if (server) {
	 * 	var table = server.getTable("employees");
	 * 	if (table) {
	 * 		var colNames = table.getColumnNames()
	 * 		application.output("Table has " + colNames.length + " columns.");
	 * 		for (var i=0; i<colNames.length; i++)
	 * 			application.output("Column " + i + ": " + colNames[i]);
	 * 	}
	 * }
	 *
	 * @param tableName The name of the table to retrieve.
	 *
	 * @return JSTableObject table.
	 */
	public JSTableObject js_getTable(String tableName)
	{
		try
		{
			ITable t = ((IServerInternal)server).getTable(tableName);
			if (t != null) return new JSTableObject(t, server);
		}
		catch (Exception e)
		{
			Debug.error("Exception while retrieving table '" + tableName + "' from server '" + serverName + "'.", e); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		return null;
	}

	/**
	 * Synchronizes a JSTable instance with the database. If columns were added to or removed from the JSTable instance, all these changes will now be persisted to the database.
	 *
	 * @sampleas js_createNewTable(String)
	 *
	 * @param table A JSTableObject instance that should be synchronized.
	 *
	 * @return boolean success.
	 */
	public boolean js_synchronizeWithDB(JSTableObject table)
	{
		String tableName = "N/A"; //$NON-NLS-1$ // for logging exceptions
		try
		{
			if (table != null)
			{
				tableName = table.getTable().getName();
				((IServerInternal)server).syncTableObjWithDB(table.getTable(), true, true);
				return true;
			}
		}
		catch (Exception e)
		{
			Debug.error("Exception while synchronizing with DB table '" + tableName + "' in server '" + serverName + "'.", e); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		return false;
	}

	/**
	 * Reloads the datamodel from the database, if changed externally or via rawSQL plugin.
	 *
	 * This call is not needed after a call to synchronizeWithDB().
	 *
	 * @sample
	 * var server = plugins.maintenance.getServer("example_data");
	 * var result = plugins.rawSQL.executeSQL("example_data", null, 'CREATE TABLE raw_table (raw_table_id INTEGER)');
	 * if (result) {
	 * 	application.output("Table created through rawSQL plugin.");
	 * 	if (server) {
	 * 		server.reloadDataModel();
	 * 		// All existing JSTableObject/JSColumn object references are invalid now! Use getTable to get new ones.
	 * 		var table = server.getTable("raw_table");
	 * 		if (table) {
	 * 			var colNames = table.getColumnNames()
	 * 			application.output("Table has " + colNames.length + " columns.");
	 * 			for (var i=0; i<colNames.length; i++)
	 * 				application.output("Column " + i + ": " + colNames[i]);
	 * 		}
	 * 	}
	 * }
	 * else {
	 * 	application.output("Raw table creation failed: " + plugins.rawSQL.getException());
	 * }
	 */
	public void js_reloadDataModel()
	{
		try
		{
			((IServerInternal)server).reloadTables();
		}
		catch (Exception e)
		{
			Debug.error("Exception while reloading datamodel from server '" + serverName + "'.", e); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * Drops the table with the specified name from this server.
	 *
	 * @sample
	 * var server = plugins.maintenance.getServer("example_data");
	 * if (server) {
	 * 	var result = server.dropTable("new_table");
	 * 	if (result)
	 * 		application.output("Table dropped.");
	 * 	else
	 * 		application.output("Table not dropped.");
	 * }
	 *
	 * @param tableName The name of the table to drop.
	 *
	 * @return boolean success.
	 */
	public boolean js_dropTable(String tableName)
	{
		try
		{
			ITable t = ((IServerInternal)server).getTable(tableName);
			if (t != null)
			{
				((IServerInternal)server).removeTable(t);
				return true;
			}
		}
		catch (Exception e)
		{
			Debug.error("Exception while removing table '" + tableName + "' from server '" + serverName + "'.", e); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		return false;
	}

	/**
	 * Returns an array with the names of all tables in this server.
	 *
	 * @sample
	 * var server = plugins.maintenance.getServer("example_data");
	 * if (server) {
	 * 	var tableNames = server.getTableNames();
	 * 	application.output("There are " + tableNames.length + " tables.");
	 * 	for (var i=0; i<tableNames.length; i++)
	 * 		application.output("Table " + i + ": " + tableNames[i]);
	 * }
	 * else {
	 * 	plugins.dialogs.showInfoDialog("Attention","Server 'example_data' cannot be found.","OK");
	 * }
	 *
	 * @return Array of String table names.
	 */
	public String[] js_getTableNames()
	{
		try
		{
			List<String> tableNames = ((IServerInternal)server).getTableNames(false);
			String[] arr = new String[tableNames.size()];
			tableNames.toArray(arr);
			return arr;
		}
		catch (Exception e)
		{
			Debug.error("Exception while retrieving table names from server '" + serverName + "'.", e); //$NON-NLS-1$ //$NON-NLS-2$
			return new String[] { };
		}
	}

	/**
	 * Get valid state for the server.
	 *
	 * @sample
	 * var server = plugins.maintenance.getServer("example_data");
	 * if (!server.isValid()) {
	 * 	application.output("Server not valid!");
	 * }
	 *
	 * @return boolean valid state.
	 */
	public boolean js_isValid()
	{
		try
		{
			return server.isValid();
		}
		catch (RemoteException e)
		{
			Debug.error(e);
			return false;
		}
	}

	public String getDataModelCloneFrom()
	{
		if (server instanceof IServerInternal)
		{
			return ((IServerInternal)server).getConfig().getDataModelCloneFrom();
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof JSServer)
		{
			return server.equals(((JSServer)obj).server);

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
		return server.hashCode();
	}
}
