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

import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.util.ServoyException;

/**
 * The foundset manager interface for handling all kinds of database functions.
 * 
 * @author jblok
 */
public interface IDatabaseManager extends ISaveConstants
{
	/**
	 * Start a transaction
	 */
	public void startTransaction();

	/**
	 * Commit a transaction
	 */
	public boolean commitTransaction();

	/**
	 * rollback a transaction
	 */
	public void rollbackTransaction();

	/**
	 * Check if a transaction is present
	 */
	public boolean hasTransaction();

	/**
	 * Get the transaction id, the client may have.
	 * 
	 * @see #getOriginalServerName(String)
	 * 
	 * @param serverName the server name for which a transaction id is requested
	 * @return String the transaction id, returns null if none present.
	 */
	public String getTransactionID(String serverName) throws ServoyException;

	/**
	 * Get a table object interface for a datasource
	 * 
	 * @param dataSource the datasource
	 * @return the table interface
	 */
	public ITable getTable(String dataSource) throws RepositoryException;

	/**
	 * Get a datasource for a table object interface
	 * 
	 * @param table the table
	 * @return the datasource
	 */
	public String getDataSource(ITable table);

	/**
	 * Save data
	 * 
	 * @see ISaveConstants
	 * @return a constant
	 */
	public int saveData();

	/**
	 * Get the orginal server name for the server after databaseManager.switchServer(original_servername, switched_to_servername).
	 * When the server was not used with databaseManager.switchServer() the input server name is returned.
	 * <p>
	 * This call can be used to find transactions in the client (which are based on kept with original_servername) for real underlying servers.
	 * <pre>
	 * String originalServerName = plugin.getClientPluginAccess().getDatabaseManager().getOriginalServerName(serverName);
	 * String tid = plugin.getClientPluginAccess().getDatabaseManager().getTransactionID(originalServerName);
	 * </pre>
	 * @param switched_to_servername
	 */
	public String getOriginalServerName(String switched_to_servername);

	/**
	 * Get the orginal server name for the server after databaseManager.switchServer(original_servername, switched_to_servername).
	 * When the server was not used with databaseManager.switchServer() the input server name is returned.
	 * @param original_servername
	 */
	public String getSwitchedToServerName(String original_servername);

	/**
	 * Notify the current client of data changes.
	 * @see ISQLActionTypes for action.
	 * 
	 * @param dataSource
	 * @param pks when null, whole table is flushed
	 * @param action
	 */
	public boolean notifyDataChange(String dataSource, IDataSet pks, int action);
}
