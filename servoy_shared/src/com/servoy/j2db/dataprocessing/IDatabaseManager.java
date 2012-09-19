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

import java.util.List;

import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.ITableAndRelationProvider;
import com.servoy.j2db.querybuilder.IQueryBuilder;
import com.servoy.j2db.querybuilder.IQueryBuilderFactory;
import com.servoy.j2db.util.ServoyException;

/**
 * The foundset manager interface for handling all kinds of database functions.
 * 
 * @author jblok
 */
public interface IDatabaseManager extends ISaveConstants, ITableAndRelationProvider
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
	 * @param serverName the server name for which a transaction id is requested
	 * @return String the transaction id, returns null if none present.
	 */
	public String getTransactionID(String serverName) throws ServoyException;

	/**
	 * Get a datasource for a table object interface
	 * 
	 * @param table the table
	 * @return the datasource
	 */
	public String getDataSource(ITable table);

	/**
	 * Save all data, not encouraged to be used by plugins, leave save to solution.
	 * 
	 * @see ISaveConstants
	 * @return a constant, if autosave is disabled AUTO_SAVE_BLOCKED will be return and nothing is saved
	 */
	public int saveData();

	/**
	 * Save specific record data, not encouraged to be used by plugins, leave save to solution.
	 * 
	 * @see ISaveConstants
	 * @return a constant, will allow to save individual record even when autosave is disabled 
	 */
	public int saveData(List<IRecord> recordsToSave);

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

	/**
	 * Get a query factory for building queries.
	 * 
	 * @return a query factory
	 * @since 6.1
	 */
	public IQueryBuilderFactory getQueryFactory();

	/**
	 * Get a new foundset for the data source.
	 * @since 6.1
	 */
	public IFoundSet getFoundSet(String dataSource) throws ServoyException;

	/**
	 * Get a new foundset for the query.
	 * @since 6.1
	 */
	public IFoundSet getFoundSet(IQueryBuilder query) throws ServoyException;


	/**
	 * Performs a sql query with a query builder object.
	 * Will throw an exception if anything did go wrong when executing the query.
	 *
	 * <br>Table filters on the involved tables in the query are applied.
	 * 
	 * @param query IQueryBuilder query.
	 * @param max_returned_rows The maximum number of rows returned by the query.  
	 * 
	 * @return The IDataSet containing the results of the query.
	 * 
	 * @since 6.1
	 */
	public IDataSet getDataSetByQuery(IQueryBuilder query, int max_returned_rows) throws ServoyException;
}
