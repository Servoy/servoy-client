/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2017 Servoy BV

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.QuerySet;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.query.ColumnType;
import com.servoy.j2db.query.ISQLQuery;
import com.servoy.j2db.query.ISQLSelect;
import com.servoy.j2db.query.ISQLUpdate;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.util.IDelegate;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.xmlxport.ColumnInfoDef;

/**
 * Base delegate wrapper for {@link IDataServer}.
 *
 * @author rgansevles
 *
 */

public abstract class AbstractDelegateDataServer implements IDataServer, IDelegate<IDataServer>
{
	private final IDataServer dataserver;

	public AbstractDelegateDataServer(IDataServer dataserver)
	{
		this.dataserver = dataserver;
	}

	@Override
	public IDataServer getDelegate()
	{
		return dataserver;
	}

	/**
	 * @param msg
	 * @throws RemoteException
	 * @see com.servoy.j2db.dataprocessing.IMaintenanceServer#logMessage(java.lang.String)
	 */
	public void logMessage(String msg) throws RemoteException
	{
		dataserver.logMessage(msg);
	}

	/**
	 * @return
	 * @throws RemoteException
	 * @see com.servoy.j2db.dataprocessing.IMaintenanceServer#isInGlobalMaintenanceMode()
	 */
	public boolean isInGlobalMaintenanceMode() throws RemoteException
	{
		return dataserver.isInGlobalMaintenanceMode();
	}

	/**
	 * @param client_id
	 * @param server_name
	 * @param table_name
	 * @param pkhashkeys
	 * @param lockSelect
	 * @param transaction_id
	 * @param filters
	 * @param chunkSize
	 * @return
	 * @throws RemoteException
	 * @throws RepositoryException
	 * @see com.servoy.j2db.dataprocessing.ILockServer#acquireLocks(java.lang.String, java.lang.String, java.lang.String, java.util.Set, com.servoy.j2db.query.QuerySelect, java.lang.String, java.util.ArrayList, int)
	 */
	public IDataSet acquireLocks(String client_id, String server_name, String table_name, Set<Object> pkhashkeys, QuerySelect lockSelect, String transaction_id,
		ArrayList<TableFilter> filters, int chunkSize) throws RemoteException, RepositoryException
	{
		return dataserver.acquireLocks(client_id, server_name, table_name, pkhashkeys, lockSelect, transaction_id, filters, chunkSize);
	}

	/**
	 * @param maintenanceMode
	 * @throws RemoteException
	 * @see com.servoy.j2db.dataprocessing.IMaintenanceServer#setGlobalMaintenanceMode(boolean)
	 */
	public void setGlobalMaintenanceMode(boolean maintenanceMode) throws RemoteException
	{
		dataserver.setGlobalMaintenanceMode(maintenanceMode);
	}

	/**
	 * @return
	 * @throws RemoteException
	 * @see com.servoy.j2db.dataprocessing.IMaintenanceServer#isInServerMaintenanceMode()
	 */
	public boolean isInServerMaintenanceMode() throws RemoteException
	{
		return dataserver.isInServerMaintenanceMode();
	}

	/**
	 * @param maintenanceMode
	 * @throws RemoteException
	 * @see com.servoy.j2db.dataprocessing.IMaintenanceServer#setServerMaintenanceMode(boolean)
	 */
	public void setServerMaintenanceMode(boolean maintenanceMode) throws RemoteException
	{
		dataserver.setServerMaintenanceMode(maintenanceMode);
	}

	/**
	 * @param client_id
	 * @param server_name
	 * @param table_name
	 * @param pkhashkeys
	 * @return
	 * @throws RemoteException
	 * @throws RepositoryException
	 * @see com.servoy.j2db.dataprocessing.ILockServer#releaseLocks(java.lang.String, java.lang.String, java.lang.String, java.util.Set)
	 */
	public boolean releaseLocks(String client_id, String server_name, String table_name, Set<Object> pkhashkeys) throws RemoteException, RepositoryException
	{
		return dataserver.releaseLocks(client_id, server_name, table_name, pkhashkeys);
	}

	/**
	 * @param client_id
	 * @param server_name
	 * @param transaction_id
	 * @param sqlSelect
	 * @param filters
	 * @param distinctInMemory
	 * @param startRow
	 * @param rowsToRetrieve
	 * @return
	 * @throws ServoyException
	 * @throws RemoteException
	 * @see com.servoy.j2db.dataprocessing.IDataServer#performQuery(java.lang.String, java.lang.String, java.lang.String, com.servoy.j2db.query.ISQLSelect, java.util.ArrayList, boolean, int, int)
	 */
	public IDataSet performQuery(String client_id, String server_name, String transaction_id, ISQLSelect sqlSelect, ArrayList<TableFilter> filters,
		boolean distinctInMemory, int startRow, int rowsToRetrieve) throws ServoyException, RemoteException
	{
		return dataserver.performQuery(client_id, server_name, transaction_id, sqlSelect, filters, distinctInMemory, startRow, rowsToRetrieve);
	}

	/**
	 * @param client_id
	 * @param server_name
	 * @param driverTableName
	 * @param transaction_id
	 * @param sql
	 * @param questiondata
	 * @param startRow
	 * @param rowsToRetrieve
	 * @return
	 * @throws ServoyException
	 * @throws RemoteException
	 * @see com.servoy.j2db.dataprocessing.IDataServer#performQuery(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.Object[], int, int)
	 */
	public IDataSet performQuery(String client_id, String server_name, String driverTableName, String transaction_id, String sql, Object[] questiondata,
		int startRow, int rowsToRetrieve) throws ServoyException, RemoteException
	{
		return dataserver.performQuery(client_id, server_name, driverTableName, transaction_id, sql, questiondata, startRow, rowsToRetrieve);
	}

	/**
	 * @param client_id
	 * @param server_name
	 * @param transaction_id
	 * @param sqlSelect
	 * @param filters
	 * @param distinctInMemory
	 * @param startRow
	 * @param rowsToRetrieve
	 * @param type
	 * @return
	 * @throws ServoyException
	 * @throws RemoteException
	 * @see com.servoy.j2db.dataprocessing.IDataServer#performQuery(java.lang.String, java.lang.String, java.lang.String, com.servoy.j2db.query.ISQLSelect, java.util.ArrayList, boolean, int, int, int)
	 */
	public IDataSet performQuery(String client_id, String server_name, String transaction_id, ISQLSelect sqlSelect, ArrayList<TableFilter> filters,
		boolean distinctInMemory, int startRow, int rowsToRetrieve, int type) throws ServoyException, RemoteException
	{
		return dataserver.performQuery(client_id, server_name, transaction_id, sqlSelect, filters, distinctInMemory, startRow, rowsToRetrieve, type);
	}

	/**
	 * @param client_id
	 * @param server_name
	 * @param transaction_id
	 * @param sqlSelect
	 * @param filters
	 * @param distinctInMemory
	 * @param startRow
	 * @param rowsToRetrieve
	 * @param type
	 * @param trackingInfo
	 * @return
	 * @throws ServoyException
	 * @throws RemoteException
	 * @see com.servoy.j2db.dataprocessing.IDataServer#performQuery(java.lang.String, java.lang.String, java.lang.String, com.servoy.j2db.query.ISQLSelect, java.util.ArrayList, boolean, int, int, int, com.servoy.j2db.dataprocessing.ITrackingSQLStatement)
	 */
	public IDataSet performQuery(String client_id, String server_name, String transaction_id, ISQLSelect sqlSelect, ArrayList<TableFilter> filters,
		boolean distinctInMemory, int startRow, int rowsToRetrieve, int type, ITrackingSQLStatement trackingInfo) throws ServoyException, RemoteException
	{
		return dataserver.performQuery(client_id, server_name, transaction_id, sqlSelect, filters, distinctInMemory, startRow, rowsToRetrieve, type,
			trackingInfo);
	}

	/**
	 * @param client_id
	 * @param server_name
	 * @param driverTableName
	 * @param transaction_id
	 * @param sql
	 * @param questiondata
	 * @param startRow
	 * @param rowsToRetrieve
	 * @param type
	 * @return
	 * @throws ServoyException
	 * @throws RemoteException
	 * @see com.servoy.j2db.dataprocessing.IDataServer#performQuery(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.Object[], int, int, int)
	 */
	public IDataSet performQuery(String client_id, String server_name, String driverTableName, String transaction_id, String sql, Object[] questiondata,
		int startRow, int rowsToRetrieve, int type) throws ServoyException, RemoteException
	{
		return dataserver.performQuery(client_id, server_name, driverTableName, transaction_id, sql, questiondata, startRow, rowsToRetrieve, type);
	}

	/**
	 * @param client_id
	 * @param server_name
	 * @param transaction_id
	 * @param sqlSelect
	 * @param filters
	 * @param distinctInMemory
	 * @param startRow
	 * @param rowsToRetrieve
	 * @param updateIdleTimestamp
	 * @return
	 * @throws ServoyException
	 * @throws RemoteException
	 * @see com.servoy.j2db.dataprocessing.IDataServer#performQuery(java.lang.String, java.lang.String, java.lang.String, com.servoy.j2db.query.ISQLSelect, java.util.ArrayList, boolean, int, int, boolean)
	 */
	public IDataSet performQuery(String client_id, String server_name, String transaction_id, ISQLSelect sqlSelect, ArrayList<TableFilter> filters,
		boolean distinctInMemory, int startRow, int rowsToRetrieve, boolean updateIdleTimestamp) throws ServoyException, RemoteException
	{
		return dataserver.performQuery(client_id, server_name, transaction_id, sqlSelect, filters, distinctInMemory, startRow, rowsToRetrieve,
			updateIdleTimestamp);
	}

	/**
	 * @param client_id
	 * @param server_name
	 * @param driverTableName
	 * @param transaction_id
	 * @param sql
	 * @param questiondata
	 * @param startRow
	 * @param rowsToRetrieve
	 * @param updateIdleTimestamp
	 * @return
	 * @throws ServoyException
	 * @throws RemoteException
	 * @see com.servoy.j2db.dataprocessing.IDataServer#performQuery(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.Object[], int, int, boolean)
	 */
	public IDataSet performQuery(String client_id, String server_name, String driverTableName, String transaction_id, String sql, Object[] questiondata,
		int startRow, int rowsToRetrieve, boolean updateIdleTimestamp) throws ServoyException, RemoteException
	{
		return dataserver.performQuery(client_id, server_name, driverTableName, transaction_id, sql, questiondata, startRow, rowsToRetrieve,
			updateIdleTimestamp);
	}

	/**
	 * @param client_id
	 * @param server_name
	 * @param driverTableName
	 * @param transaction_id
	 * @param sqlSelect
	 * @param filters
	 * @param startRow
	 * @param rowsToRetrieve
	 * @return
	 * @throws ServoyException
	 * @throws RemoteException
	 * @see com.servoy.j2db.dataprocessing.IDataServer#performCustomQuery(java.lang.String, java.lang.String, java.lang.String, java.lang.String, com.servoy.j2db.query.ISQLSelect, java.util.ArrayList, int, int)
	 */
	public IDataSet performCustomQuery(String client_id, String server_name, String driverTableName, String transaction_id, ISQLSelect sqlSelect,
		ArrayList<TableFilter> filters, int startRow, int rowsToRetrieve) throws ServoyException, RemoteException
	{
		return dataserver.performCustomQuery(client_id, server_name, driverTableName, transaction_id, sqlSelect, filters, startRow, rowsToRetrieve);
	}

	/**
	 * @param client_id
	 * @param server_name
	 * @param transaction_id
	 * @param array
	 * @return
	 * @throws ServoyException
	 * @throws RemoteException
	 * @see com.servoy.j2db.dataprocessing.IDataServer#performQuery(java.lang.String, java.lang.String, java.lang.String, com.servoy.j2db.dataprocessing.QueryData[])
	 */
	public IDataSet[] performQuery(String client_id, String server_name, String transaction_id, QueryData[] array) throws ServoyException, RemoteException
	{
		return dataserver.performQuery(client_id, server_name, transaction_id, array);
	}

	/**
	 * @param action
	 * @param server_name
	 * @param tableName
	 * @param pkColumnData
	 * @param tid
	 * @param sql
	 * @param questiondata
	 * @return
	 * @throws RemoteException
	 * @throws RepositoryException
	 * @see com.servoy.j2db.dataprocessing.IDataServer#createSQLStatement(int, java.lang.String, java.lang.String, java.lang.Object[], java.lang.String, java.lang.String, java.lang.Object[])
	 */
	public ISQLStatement createSQLStatement(int action, String server_name, String tableName, Object[] pkColumnData, String tid, String sql,
		Object[] questiondata) throws RemoteException, RepositoryException
	{
		return dataserver.createSQLStatement(action, server_name, tableName, pkColumnData, tid, sql, questiondata);
	}

	/**
	 * @param action
	 * @param server_name
	 * @param tableName
	 * @param pkColumnData
	 * @param tid
	 * @param sqlUpdate
	 * @param filters
	 * @return
	 * @throws RemoteException
	 * @throws RepositoryException
	 * @see com.servoy.j2db.dataprocessing.IDataServer#createSQLStatement(int, java.lang.String, java.lang.String, java.lang.Object[], java.lang.String, com.servoy.j2db.query.ISQLUpdate, java.util.ArrayList)
	 */
	public ISQLStatement createSQLStatement(int action, String server_name, String tableName, Object[] pkColumnData, String tid, ISQLUpdate sqlUpdate,
		ArrayList<TableFilter> filters) throws RemoteException, RepositoryException
	{
		return dataserver.createSQLStatement(action, server_name, tableName, pkColumnData, tid, sqlUpdate, filters);
	}

	/**
	 * @param client_id
	 * @param server_name
	 * @param tableName
	 * @param pks
	 * @param action
	 * @param transaction_id
	 * @return
	 * @throws RemoteException
	 * @see com.servoy.j2db.dataprocessing.IDataServer#notifyDataChange(java.lang.String, java.lang.String, java.lang.String, com.servoy.j2db.dataprocessing.IDataSet, int, java.lang.String)
	 */
	public boolean notifyDataChange(String client_id, String server_name, String tableName, IDataSet pks, int action, String transaction_id)
		throws RemoteException
	{
		return dataserver.notifyDataChange(client_id, server_name, tableName, pks, action, transaction_id);
	}

	/**
	 * @param clientId
	 * @param statements
	 * @return
	 * @throws ServoyException
	 * @throws RemoteException
	 * @see com.servoy.j2db.dataprocessing.IDataServer#performUpdates(java.lang.String, com.servoy.j2db.dataprocessing.ISQLStatement[])
	 */
	public Object[] performUpdates(String clientId, ISQLStatement[] statements) throws ServoyException, RemoteException
	{
		return dataserver.performUpdates(clientId, statements);
	}

	/**
	 * @param clientId
	 * @param serverName
	 * @param blobSelect
	 * @param filters
	 * @param tid
	 * @return
	 * @throws RepositoryException
	 * @throws RemoteException
	 * @see com.servoy.j2db.dataprocessing.IDataServer#getBlob(java.lang.String, java.lang.String, com.servoy.j2db.query.ISQLSelect, java.util.ArrayList, java.lang.String)
	 */
	public Blob getBlob(String clientId, String serverName, ISQLSelect blobSelect, ArrayList<TableFilter> filters, String tid)
		throws RepositoryException, RemoteException
	{
		return dataserver.getBlob(clientId, serverName, blobSelect, filters, tid);
	}

	/**
	 * @param clientId
	 * @param server_name
	 * @return
	 * @throws RepositoryException
	 * @throws RemoteException
	 * @see com.servoy.j2db.dataprocessing.IDataServer#startTransaction(java.lang.String, java.lang.String)
	 */
	public String startTransaction(String clientId, String server_name) throws RepositoryException, RemoteException
	{
		return dataserver.startTransaction(clientId, server_name);
	}

	/**
	 * @param client_id
	 * @param transaction_id
	 * @param commit
	 * @return
	 * @throws RepositoryException
	 * @throws RemoteException
	 * @see com.servoy.j2db.dataprocessing.IDataServer#endTransactions(java.lang.String, java.lang.String[], boolean)
	 */
	public boolean endTransactions(String client_id, String[] transaction_id, boolean commit) throws RepositoryException, RemoteException
	{
		return dataserver.endTransactions(client_id, transaction_id, commit);
	}

	/**
	 * @param serverName
	 * @param tableName
	 * @param columnName
	 * @param columnInfoID
	 * @param columnInfoServer
	 * @return
	 * @throws RepositoryException
	 * @throws RemoteException
	 * @see com.servoy.j2db.dataprocessing.IDataServer#getNextSequence(java.lang.String, java.lang.String, java.lang.String, int, java.lang.String)
	 */
	public Object getNextSequence(String serverName, String tableName, String columnName, int columnInfoID, String columnInfoServer)
		throws RepositoryException, RemoteException
	{
		return dataserver.getNextSequence(serverName, tableName, columnName, columnInfoID, columnInfoServer);
	}

	/**
	 * @param client_id
	 * @param set
	 * @param dataSource
	 * @param serverName
	 * @param tableName
	 * @param tid
	 * @param columnTypes
	 * @param pkNames
	 * @param columnInfoDefinitions
	 * @return
	 * @throws ServoyException
	 * @throws RemoteException
	 * @see com.servoy.j2db.dataprocessing.IDataServer#insertDataSet(java.lang.String, com.servoy.j2db.dataprocessing.IDataSet, java.lang.String, java.lang.String, java.lang.String, java.lang.String, com.servoy.j2db.query.ColumnType[], java.lang.String[], java.util.HashMap)
	 */
	public ITable insertDataSet(String client_id, IDataSet set, String dataSource, String serverName, String tableName, String tid, ColumnType[] columnTypes,
		String[] pkNames, HashMap<String, ColumnInfoDef> columnInfoDefinitions) throws ServoyException, RemoteException
	{
		return dataserver.insertDataSet(client_id, set, dataSource, serverName, tableName, tid, columnTypes, pkNames, columnInfoDefinitions);
	}

	/**
	 * @param client_id
	 * @param queryServerName
	 * @param queryTid
	 * @param sqlSelect
	 * @param filters
	 * @param distinctInMemory
	 * @param startRow
	 * @param rowsToRetrieve
	 * @param type
	 * @param dataSource
	 * @param targetServerName
	 * @param targetTableName
	 * @param targetTid
	 * @param columnTypes
	 * @param pkNames
	 * @return
	 * @throws ServoyException
	 * @throws RemoteException
	 * @see com.servoy.j2db.dataprocessing.IDataServer#insertQueryResult(java.lang.String, java.lang.String, java.lang.String, com.servoy.j2db.query.ISQLSelect, java.util.ArrayList, boolean, int, int, int, java.lang.String, java.lang.String, java.lang.String, java.lang.String, com.servoy.j2db.query.ColumnType[], java.lang.String[])
	 */
	public ITable insertQueryResult(String client_id, String queryServerName, String queryTid, ISQLSelect sqlSelect, ArrayList<TableFilter> filters,
		boolean distinctInMemory, int startRow, int rowsToRetrieve, int type, String dataSource, String targetServerName, String targetTableName,
		String targetTid, ColumnType[] columnTypes, String[] pkNames) throws ServoyException, RemoteException
	{
		return dataserver.insertQueryResult(client_id, queryServerName, queryTid, sqlSelect, filters, distinctInMemory, startRow, rowsToRetrieve, type,
			dataSource, targetServerName, targetTableName, targetTid, columnTypes, pkNames);
	}

	/**
	 * @param client_id
	 * @param serverName
	 * @param tableName
	 * @throws RemoteException
	 * @throws RepositoryException
	 * @see com.servoy.j2db.dataprocessing.IDataServer#dropTemporaryTable(java.lang.String, java.lang.String, java.lang.String)
	 */
	public void dropTemporaryTable(String client_id, String serverName, String tableName) throws RemoteException, RepositoryException
	{
		dataserver.dropTemporaryTable(client_id, serverName, tableName);
	}

	/**
	 * @param client_id
	 * @param serverName
	 * @param tableName
	 * @throws RemoteException
	 * @throws RepositoryException
	 * @see com.servoy.j2db.dataprocessing.IDataServer#addClientAsTableUser(java.lang.String, java.lang.String, java.lang.String)
	 */
	public void addClientAsTableUser(String client_id, String serverName, String tableName) throws RemoteException, RepositoryException
	{
		dataserver.addClientAsTableUser(client_id, serverName, tableName);
	}

	/**
	 * @param serverName
	 * @param sqlQuery
	 * @param filters
	 * @param startRow
	 * @param rowsToRetrieve
	 * @param forceQualifyColumns
	 * @return
	 * @throws RepositoryException
	 * @throws RemoteException
	 * @see com.servoy.j2db.dataprocessing.IDataServer#getSQLQuerySet(java.lang.String, com.servoy.j2db.query.ISQLQuery, java.util.ArrayList, int, int, boolean)
	 */
	public QuerySet getSQLQuerySet(String serverName, ISQLQuery sqlQuery, ArrayList<TableFilter> filters, int startRow, int rowsToRetrieve,
		boolean forceQualifyColumns) throws RepositoryException, RemoteException
	{
		return dataserver.getSQLQuerySet(serverName, sqlQuery, filters, startRow, rowsToRetrieve, forceQualifyColumns);
	}


}
