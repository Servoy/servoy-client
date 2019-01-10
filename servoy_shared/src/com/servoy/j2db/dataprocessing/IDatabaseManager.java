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

import java.util.Collection;
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
	 * Commit a transaction, will save all edited records before commit
	 */
	public boolean commitTransaction();

	/**
	 * Commit a transaction
	 *
	 * @param saveFirst boolean to configure if commit should be preceded by a save of all records
	 * @param revertSavedRecords boolean used as rollback option, if true and transaction fails will revert saved records to their previous (database) values
	 */
	public boolean commitTransaction(boolean saveFirst, boolean revertSavedRecords);

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
	 * @deprecated Use {@link #getOriginalServerNames(String)}
	 * @param switched_to_servername
	 */
	@Deprecated
	public String getOriginalServerName(String switched_to_servername);

	/**
	 * Get the orginal server names for the server after databaseManager.switchServer(original_servername, switched_to_servername).
	 * When the server was not used with databaseManager.switchServer() the input server name is returned in a single-element collection.
	 * <p>
	 * This call can be used to find transactions in the client (which are based on kept with original_servername) for real underlying servers.
	 * <pre>
	 * Collection<String> originalServerNames = plugin.getClientPluginAccess().getDatabaseManager().getOriginalServerNames(serverName);
	 * for (String originalServerName: originalServerNames) {
	 * 	String tid = plugin.getClientPluginAccess().getDatabaseManager().getTransactionID(originalServerName);
	 * 	...
	 * }
	 * </pre>
	 * @param switched_to_servername
	 */
	public Collection<String> getOriginalServerNames(String switched_to_servername);

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
	 * Notify the current client of data changes.
	 * @see ISQLActionTypes for action.
	 *
	 * @param dataSource
	 * @param pks when null, whole table is flushed
	 * @param action
	 * @param insertOrUpdateColumnData
	 */
	public void notifyDataChange(final String dataSource, IDataSet pks, final int action, Object[] insertOrUpdateColumnData);

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

	/**
	 * Performs a sql query with a query builder object.
	 * Will throw an exception if anything did go wrong when executing the query.
	 *
	 * @param query IQueryBuilder query.
	 * @param useTableFilters use table filters (default true).
	 * @param max_returned_rows The maximum number of rows returned by the query.
	 *
	 * @return The IDataSet containing the results of the query.
	 *
	 * @since 8.0
	 */
	public IDataSet getDataSetByQuery(IQueryBuilder query, boolean useTableFilters, int max_returned_rows) throws ServoyException;

	/**
	 * Gets the list of records that couldn't be saved.
	 *
	 * @return The records for which save failed.
	 */
	public IRecord[] getFailedRecords();

	/**
	 * Request lock(s) for a foundset, can be a normal or related foundset.
	 * The record_index can be -2 to lock all rows, -1 to lock the current row, or a specific row of >= 0
	 * Optionally name the lock(s) so that it can be referenced in releaseAllLocks()
	 *
	 * @param fs Foundset used for locking.
	 * @param index Record index to lock.
	 * @param lockName Lock name.
	 *
	 * @return true if the lock could be acquired.
	 */
	public boolean acquireLock(IFoundSet fs, int index, String lockName);

	/**
	 * Returns true if the current client has any or the specified lock(s) acquired.
	 *
	 * @param lockName The lock name to check. If null, it means any lock.
	 *
	 * @return true if the current client has locks or the lock.
	 */
	boolean hasLocks(String lockName);

	/**
	 * Release all current locks the client has (optionally limited to named locks). Returns true if the locks are released.
	 *
	 * @param lockName The lock name to release or null for all locks.
	 *
	 * @return true if all locks or the specified lock are released.
	 */
	boolean releaseAllLocks(String lockName);

	/**
	 * True if null column validator is enabled; false otherwise.
	 * <br>
	 * <pre>
	 * // test if it's enabled
	 * if (plugin.getClientPluginAccess().getDatabaseManager().isNullColumnValidatorEnabled()) System.out.println("null validation enabled");
	 * </pre>
	 * @return true if null column validator is enabled; false otherwise
	 *
	 * @see #setNullColumnValidatorEnabled(boolean)
	 */
	public boolean isNullColumnValidatorEnabled();

	/**
	 * Enable/disable the default null validator for non null columns, makes it possible todo the checks later on when saving, when for example autosave is disabled.
	 * <br>
	 * <pre>
	 * // disable it
	 * plugin.getClientPluginAccess().getDatabaseManager().setNullColumnValidatorEnabled(false);
	 * </pre>
	 * @param enable true to enable null column validator (default), false to disable it.
	 */
	public void setNullColumnValidatorEnabled(boolean enable);

}
