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


import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Procedure;
import com.servoy.j2db.persistence.QuerySet;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.query.ColumnType;
import com.servoy.j2db.query.ISQLQuery;
import com.servoy.j2db.query.ISQLSelect;
import com.servoy.j2db.query.ISQLUpdate;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.xmlxport.ColumnInfoDef;

/**
 * Interface for manipulation database data
 *
 * @author jblok
 */
public interface IDataServer
{
	public static final int CUSTOM_QUERY = 1;
	public static final int RELATION_QUERY = 2;
	public static final int FIND_BROWSER_QUERY = 3;
	public static final int REFRESH_ROLLBACK_QUERY = 4;
	public static final int INSERT_QUERY = 5;
	public static final int UPDATE_QUERY = 6;
	public static final int DELETE_QUERY = 7;
	public static final int RAW_QUERY = 8;
	public static final int AGGREGATE_QUERY = 9;
	public static final int REPOSITORY_QUERY = 10;
	public static final int FOUNDSET_LOAD_QUERY = 11;
	public static final int LOCKS_QUERY = 12;
	public static final int MESSAGES_QUERY = 13;
	public static final int VALUELIST_QUERY = 14;
	public static final int PRINT_QUERY = 15;
	public static final int USERMANAGEMENT_QUERY = 16;
	public static final int META_DATA_QUERY = 17;
	public static final int METHOD_CALL = 18;
	public static final int METHOD_CALL_WAITING_FOR_USER_INPUT = 19;

	public static final String BLOB_MARKER_COLUMN_ALIAS = "SV_BLOB_M"; //$NON-NLS-1$

	/**
	 * Main query method on databases.
	 *
	 * @param client_id client ID
	 * @param server_name the server to use
	 * @param transaction_id id or null if none
	 * @param sqlSelect the sql statement
	 * @param distinctInMemory require distinct values but query is not distinct
	 * @param startRow start row normally 0
	 * @param rowsToRetrieve rowsToRetrieve number of rows to retrieve
	 * @return IDataSet the dataset
	 * @throws RepositoryException
	 * @throws RemoteException
	 */
	public IDataSet performQuery(String client_id, String server_name, String transaction_id, ISQLSelect sqlSelect, ColumnType[] resultTypes,
		ArrayList<TableFilter> filters,
		boolean distinctInMemory, int startRow, int rowsToRetrieve) throws ServoyException;

	public IDataSet performQuery(String client_id, String server_name, String driverTableName, String transaction_id, String sql, Object[] questiondata,
		int startRow, int rowsToRetrieve) throws ServoyException;

	public IDataSet performQuery(String client_id, String server_name, String transaction_id, ISQLSelect sqlSelect, ColumnType[] resultTypes,
		ArrayList<TableFilter> filters,
		boolean distinctInMemory, int startRow, int rowsToRetrieve, int type) throws ServoyException;

	public IDataSet performQuery(String client_id, String server_name, String transaction_id, ISQLSelect sqlSelect, ColumnType[] resultTypes,
		ArrayList<TableFilter> filters,
		boolean distinctInMemory, int startRow, int rowsToRetrieve, int type, ITrackingSQLStatement trackingInfo) throws ServoyException;

	public IDataSet performQuery(String client_id, String server_name, String driverTableName, String transaction_id, String sql, Object[] questiondata,
		int startRow, int rowsToRetrieve, int type) throws ServoyException;

	public IDataSet performQuery(String client_id, String server_name, String transaction_id, ISQLSelect sqlSelect, ColumnType[] resultTypes,
		ArrayList<TableFilter> filters, boolean distinctInMemory, int startRow, int rowsToRetrieve, boolean updateIdleTimestamp)
		throws ServoyException;

	public IDataSet performQuery(String client_id, String server_name, String driverTableName, String transaction_id, String sql, Object[] questiondata,
		int startRow, int rowsToRetrieve, boolean updateIdleTimestamp) throws ServoyException;

	//special case only used by user query, does add column info to resultset
	public IDataSet performCustomQuery(String client_id, String server_name, String driverTableName, String transaction_id, ISQLSelect sqlSelect,
		ArrayList<TableFilter> filters, int startRow, int rowsToRetrieve) throws ServoyException;

	public IDataSet[] performQuery(String client_id, String server_name, String transaction_id, QueryData[] array) throws ServoyException;


	/**
	 * @param action from ISQLStatement static fields
	 * @param server_name server name in lower case to work on
	 * @param tableName table name in lowercase to work on
	 * @param pkColumnData must be db compatible types and columns ordered a-z when having mulitple columns
	 * @param tid transaction id, can be null if not present
	 * @param sql the SQL to execute
	 * @param questiondata the data for the question marks (must be db compatible types)
	 * @return the statement
	 * @throws RemoteException
	 */
	public ISQLStatement createSQLStatement(int action, String server_name, String tableName, Object[] pkColumnData, String tid, String sql,
		Object[] questiondata) throws RepositoryException;

	public ISQLStatement createSQLStatement(int action, String server_name, String tableName, Object[] pkColumnData, String tid, ISQLUpdate sqlUpdate,
		ArrayList<TableFilter> filters) throws RepositoryException;

	public boolean notifyDataChange(String client_id, String server_name, String tableName, IDataSet pks, int action, String transaction_id)
		throws RemoteException;

	public boolean notifyDataChange(String client_id, boolean notifySelf, String server_name, String tableName, IDataSet pks, int action, String transaction_id)
		throws RemoteException;

	/**
	 * Change or Add data in database
	 *
	 * @param client_id the client ID
	 * @param statements the changes
	 * @return the values from the dbsequences for each statement
	 * @throws RepositoryException
	 * @throws RemoteException
	 */
	public Object[] performUpdates(String clientId, ISQLStatement[] statements) throws ServoyException;

	public Blob getBlob(String clientId, String serverName, ISQLSelect blobSelect, ArrayList<TableFilter> filters, String tid)
		throws RepositoryException;

	/**
	 * Start a transaction in backend DB.
	 *
	 * @param client_id the client ID
	 * @param server_name start transaction for specified server
	 * @return String the id
	 * @throws RepositoryException
	 * @throws RemoteException
	 */
	public String startTransaction(String clientId, String server_name) throws RepositoryException;//also starts transaction

	/**
	 * End a started transaction in backend DB.
	 *
	 * @param transaction_id id to end
	 * @param commit true for commit or false for rollback
	 * @return boolean true if success full
	 * @throws RepositoryException
	 * @throws RemoteException
	 */
	public boolean endTransactions(String client_id, String[] transaction_id, boolean commit) throws RepositoryException;

	/**
	 * Get a sequence
	 */
	public Object getNextSequence(String serverName, String tableName, String columnName, UUID columnInfoUUID, String columnInfoServer)
		throws RepositoryException;

	/**
	 * Insert a data set in a table. When tableName is null a temporary table will be created
	 *
	 * @param client_id
	 * @param set
	 * @param dataSource
	 * @param serverName
	 * @param tableName when null a temporary table will be created
	 * @param tid transaction id
	 * @param columnTypes column types
	 * @param pkNames
	 * @param columnInfoDefinitions
	 * @return the table where the set was inserted into
	 * @return
	 * @throws ServoyException
	 * @throws RemoteException
	 */
	public InsertResult insertDataSet(String client_id, IDataSet set, String dataSource, String serverName, String tableName, String tid,
		ColumnType[] columnTypes, String[] pkNames, HashMap<String, ColumnInfoDef> columnInfoDefinitions) throws ServoyException;

	/**
	 * Insert a data from a query in a table. When tableName is null a temporary table will be created
	 *
	 * @param client_id
	 * @param queryServerName
	 * @param queryTid
	 * @param sqlSelect the sql statement
	 * @param filters filters to apply
	 * @param distinctInMemory require distinct values but query is not distinct
	 * @param startRow start row normally 0
	 * @param rowsToRetrieve rowsToRetrieve number of rows to retrieve
	 * @param type query type
	 * @param dataSource
	 * @param targetServerName
	 * @param targetTableName when null a temporary table will be created
	 * @param targetTid transaction id
	 * @param types the column types
	 * @param pkNames
	 * @return the table where the set was inserted into
	 * @throws ServoyException
	 * @throws RemoteException
	 */
	public ITable insertQueryResult(String client_id, String queryServerName, String queryTid, ISQLSelect sqlSelect, ArrayList<TableFilter> filters,
		boolean distinctInMemory, int startRow, int rowsToRetrieve, int type, String dataSource, String targetServerName, String targetTableName,
		String targetTid, ColumnType[] columnTypes, String[] pkNames) throws ServoyException;

	public void dropTemporaryTable(String client_id, String serverName, String tableName) throws RepositoryException;

	public void addClientAsTableUser(String client_id, String serverName, String tableName) throws RepositoryException;

	/**
	 * Get the sql from the remote server, needed for databasemanager.getSQL() and databasemanager.getSQLParameters() in scripting.
	 *
	 * @param serverName
	 * @param sqlQuery
	 * @param startRow
	 * @param rowsToRetrieve
	 * @return
	 * @throws RepositoryException
	 * @throws RemoteException
	 */
	public QuerySet getSQLQuerySet(String serverName, ISQLQuery sqlQuery, ArrayList<TableFilter> filters, int startRow, int rowsToRetrieve,
		boolean forceQualifyColumns, boolean disableUseArrayForIn) throws RepositoryException;


	public IDataSet[] executeProcedure(String clientId, String server_name, String tid, Procedure procedure, Object[] arguments)
		throws RepositoryException;

	public IDataSet acquireLocks(String client_id, String server_name, String table_name, Set<Object> pkhashkeys, QuerySelect lockSelect, String transaction_id,
		ArrayList<TableFilter> filters, int chunkSize) throws RepositoryException;//returns the data for acquired locks

	public boolean releaseLocks(String client_id, String server_name, String table_name, Set<Object> pkhashkeys) throws RepositoryException;


	/**
	 * Set broadcast filters for this client in a server.
	 *
	 * @param clientId
	 * @param serverName
	 * @param broadcastFilters
	 */
	public void setBroadcastFilters(String clientId, String serverName, BroadcastFilter[] broadcastFilters);

	/**
	 * Get broadcast filters for this client in a server.
	 *
	 * @param clientId
	 * @param serverName
	 */
	public BroadcastFilter[] getBroadcastFilters(String clientId, String serverName);

	/**
	 * Clear all broadcast filters for this client.
	 *
	 * @param clientId
	 */
	public void clearBroadcastFilters(String clientId);

	/**
	 * Log a message on the server
	 *
	 * @param msg
	 */
	public void logMessage(String msg);

	boolean isInServerMaintenanceMode();

	void setServerMaintenanceMode(boolean maintenanceMode);

	public class InsertResult implements Serializable
	{
		private final Table table;
		private final Object[] generatedPks;

		public InsertResult(Table table, Object[] generatedPks)
		{
			this.table = table;
			this.generatedPks = generatedPks;
		}

		public Table getTable()
		{
			return table;
		}

		public Object[] getGeneratedPks()
		{
			return generatedPks;
		}
	}

}
