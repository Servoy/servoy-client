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

package com.servoy.j2db.debug;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.servoy.base.query.BaseAbstractBaseQuery;
import com.servoy.j2db.dataprocessing.Blob;
import com.servoy.j2db.dataprocessing.IDataServer;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.dataprocessing.ISQLStatement;
import com.servoy.j2db.dataprocessing.ITrackingSQLStatement;
import com.servoy.j2db.dataprocessing.QueryData;
import com.servoy.j2db.dataprocessing.TableFilter;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.QuerySet;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.query.ISQLQuery;
import com.servoy.j2db.query.ISQLSelect;
import com.servoy.j2db.query.ISQLUpdate;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.server.shared.PerformanceTiming;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyException;

/**
 * A wrapper/proxy around the actual {@link IDataServer} to profile queries to the database.
 *
 * @author jcompagner
 * @since 6.0
 *
 */

public class ProfileDataServer implements IDataServer
{
	private final IDataServer dataserver;
	private final List<IDataCallListener> listeners = new ArrayList<IDataCallListener>();


	public ProfileDataServer(IDataServer dataserver)
	{
		this.dataserver = dataserver;
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
		long startTime = System.currentTimeMillis();
		try
		{
			return dataserver.acquireLocks(client_id, server_name, table_name, pkhashkeys, lockSelect, transaction_id, filters, chunkSize);
		}
		finally
		{
			informListeners("Lock Aquired", server_name + '.' + table_name, lockSelect != null ? lockSelect.toString() : null, transaction_id, startTime,
				pkhashkeys.toArray());
		}
	}

	/**
	 * @param server_name
	 * @param table_name
	 * @param lockSelect
	 * @param transaction_id
	 * @param startTime
	 */
	private void informListeners(String name, String ds, String sql, String transaction_id, long startTime, Object[] arguments)
	{
		if (listeners.size() > 0)
		{
			long endTime = System.currentTimeMillis();

			String argumentString = null;
			if (arguments != null && arguments.length > 0)
			{
				if (arguments[0] instanceof Object[] && arguments.length == 1)
				{
					argumentString = BaseAbstractBaseQuery.toString(arguments[0]);
				}
				else
				{
					argumentString = BaseAbstractBaseQuery.toString(arguments);
				}

			}
			DataCallProfileData pd = new DataCallProfileData(name, ds, transaction_id, startTime, endTime, sql, argumentString, 1);

			listeners.get(listeners.size() - 1).addDataCallProfileData(pd);
		}
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
		long startTime = System.currentTimeMillis();
		try
		{
			return dataserver.releaseLocks(client_id, server_name, table_name, pkhashkeys);
		}
		finally
		{
			informListeners("Lock Released", server_name + '.' + table_name, null, null, startTime, pkhashkeys.toArray());
		}
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
		long startTime = System.currentTimeMillis();
		try
		{
			return dataserver.performQuery(client_id, server_name, transaction_id, sqlSelect, filters, distinctInMemory, startRow, rowsToRetrieve);
		}
		finally
		{
			QuerySet set = getSQLQuerySet(server_name, sqlSelect, filters, startRow, rowsToRetrieve, false);
			informListeners("Query", server_name, set.getSelect().getSql(), transaction_id, startTime, set.getSelect().getParameters());
		}
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
		long startTime = System.currentTimeMillis();
		try
		{
			return dataserver.performQuery(client_id, server_name, driverTableName, transaction_id, sql, questiondata, startRow, rowsToRetrieve);
		}
		finally
		{
			informListeners("Query", server_name, sql, transaction_id, startTime, questiondata);
		}

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
		return performQuery(client_id, server_name, transaction_id, sqlSelect, filters, distinctInMemory, startRow, rowsToRetrieve, type, null);
	}

	public IDataSet performQuery(String client_id, String server_name, String transaction_id, ISQLSelect sqlSelect, ArrayList<TableFilter> filters,
		boolean distinctInMemory, int startRow, int rowsToRetrieve, int type, ITrackingSQLStatement trackingInfo) throws ServoyException, RemoteException
	{
		long startTime = System.currentTimeMillis();
		try
		{
			return dataserver.performQuery(client_id, server_name, transaction_id, sqlSelect, filters, distinctInMemory, startRow, rowsToRetrieve, type,
				trackingInfo);
		}
		finally
		{
			QuerySet set = getSQLQuerySet(server_name, sqlSelect, filters, startRow, rowsToRetrieve, false);
			informListeners("Query[" + PerformanceTiming.getTypeString(type) + ']', server_name, set.getSelect().getSql(), transaction_id, startTime,
				set.getSelect().getParameters());
		}
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
		long startTime = System.currentTimeMillis();
		try
		{
			return dataserver.performQuery(client_id, server_name, driverTableName, transaction_id, sql, questiondata, startRow, rowsToRetrieve, type);
		}
		finally
		{
			informListeners("Query[" + PerformanceTiming.getTypeString(type) + ']', server_name, sql, transaction_id, startTime, questiondata);
		}
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
		long startTime = System.currentTimeMillis();
		try
		{
			return dataserver.performQuery(client_id, server_name, transaction_id, sqlSelect, filters, distinctInMemory, startRow, rowsToRetrieve,
				updateIdleTimestamp);
		}
		finally
		{
			QuerySet set = getSQLQuerySet(server_name, sqlSelect, filters, startRow, rowsToRetrieve, false);
			informListeners("Query", server_name, set.getSelect().getSql(), transaction_id, startTime, set.getSelect().getParameters());
		}
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
		long startTime = System.currentTimeMillis();
		try
		{
			return dataserver.performQuery(client_id, server_name, driverTableName, transaction_id, sql, questiondata, startRow, rowsToRetrieve,
				updateIdleTimestamp);
		}
		finally
		{
			informListeners("Query", server_name, sql, transaction_id, startTime, questiondata);
		}
	}

	/**
	 * @param client_id
	 * @param server_name
	 * @param driverTableName
	 * @param transaction_id
	 * @param startRow
	 * @param rowsToRetrieve
	 * @return
	 * @throws ServoyException
	 * @throws RemoteException
	 * @see com.servoy.j2db.dataprocessing.IDataServer#performCustomQuery(java.lang.String, java.lang.String, java.lang.String, java.lang.String, ISQLSelect, int, int)
	 */
	public IDataSet performCustomQuery(String client_id, String server_name, String driverTableName, String transaction_id, ISQLSelect sqlSelect,
		ArrayList<TableFilter> filters, int startRow, int rowsToRetrieve) throws ServoyException, RemoteException
	{
		long startTime = System.currentTimeMillis();
		try
		{
			return dataserver.performCustomQuery(client_id, server_name, driverTableName, transaction_id, sqlSelect, filters, startRow, rowsToRetrieve);
		}
		finally
		{
			QuerySet set = getSQLQuerySet(server_name, sqlSelect, null, 0, 1, false);
			informListeners("Query", server_name, set.getSelect().getSql(), transaction_id, startTime, set.getSelect().getParameters());
		}
	}

	public IDataSet[] performQuery(String client_id, String server_name, String transaction_id, QueryData[] array) throws ServoyException, RemoteException
	{
		long startTime = System.currentTimeMillis();
		try
		{
			return dataserver.performQuery(client_id, server_name, transaction_id, array);
		}
		finally
		{
			int counter = 0;
			for (QueryData queryData : array)
			{
				QuerySet set = getSQLQuerySet(server_name, queryData.getSqlSelect(), queryData.getFilters(), queryData.getStartRow(),
					queryData.getRowsToRetrieve(), false);
				informListeners(PerformanceTiming.getTypeString(queryData.getType()) + " Combined Query[" + (counter++) + '/' + array.length + ']', server_name,
					set.getSelect().getSql(), transaction_id, startTime, set.getSelect().getParameters());
			}
		}
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
		long startTime = System.currentTimeMillis();
		try
		{
			return dataserver.createSQLStatement(action, server_name, tableName, pkColumnData, tid, sql, questiondata);
		}
		finally
		{
			informListeners("Query", server_name, sql, tid, startTime, questiondata);
		}
	}

	public ISQLStatement createSQLStatement(int action, String server_name, String tableName, Object[] pkColumnData, String tid, ISQLUpdate sqlUpdate,
		ArrayList<TableFilter> filters) throws RemoteException
	{
		long startTime = System.currentTimeMillis();
		try
		{
			return dataserver.createSQLStatement(action, server_name, tableName, pkColumnData, tid, sqlUpdate, filters);
		}
		finally
		{
			QuerySet set;
			try
			{
				set = getSQLQuerySet(server_name, sqlUpdate, null, -1, -1, false);
				informListeners("Query", server_name, set.getUpdate().getSql(), tid, startTime, set.getUpdate().getParameters());
			}
			catch (RepositoryException e)
			{
				Debug.error(e);
			}
		}
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
		long startTime = System.currentTimeMillis();
		try
		{
			return dataserver.performUpdates(clientId, statements);
		}
		finally
		{
			for (ISQLStatement statement : statements)
			{
				QuerySet set = getSQLQuerySet(statement.getServerName(), statement.getUpdate(), null, -1, -1, false);

				informListeners("Update", statement.getServerName() + '.' + statement.getTableName(), set.getUpdate().getSql(), statement.getTransactionID(),
					startTime, set.getUpdate().getParameters());
			}
		}

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
		long startTime = System.currentTimeMillis();
		try
		{
			return dataserver.getBlob(clientId, serverName, blobSelect, filters, tid);
		}
		finally
		{
			QuerySet set = getSQLQuerySet(serverName, blobSelect, filters, 0, 1, false);
			informListeners("BlobLoad", serverName, set.getSelect().getSql(), tid, startTime, set.getSelect().getParameters());
		}
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
		long startTime = System.currentTimeMillis();
		try
		{
			return dataserver.startTransaction(clientId, server_name);
		}
		finally
		{
			informListeners("StartTransaction", server_name, null, null, startTime, null);
		}
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
		long startTime = System.currentTimeMillis();
		try
		{
			return dataserver.endTransactions(client_id, transaction_id, commit);
		}
		finally
		{
			informListeners("EndTransactions", null, null, Arrays.toString(transaction_id), startTime, null);
		}
	}

	/**
	 * @param serverName
	 * @param tableName
	 * @param columnName
	 * @param columnInfoID
	 * @return
	 * @throws RepositoryException
	 * @throws RemoteException
	 * @see com.servoy.j2db.dataprocessing.IDataServer#getNextSequence(java.lang.String, java.lang.String, java.lang.String, int)
	 */
	public Object getNextSequence(String serverName, String tableName, String columnName, int columnInfoID) throws RepositoryException, RemoteException
	{
		long startTime = System.currentTimeMillis();
		try
		{
			return dataserver.getNextSequence(serverName, tableName, columnName, columnInfoID);
		}
		finally
		{
			informListeners("GetSequence", serverName + '.' + tableName, columnName, null, startTime, null);
		}
	}

	/**
	 * @param client_id
	 * @param set
	 * @param dataSource
	 * @param serverName
	 * @param tableName
	 * @param tid
	 * @param types
	 * @return
	 * @throws ServoyException
	 * @throws RemoteException
	 * @see com.servoy.j2db.dataprocessing.IDataServer#insertDataSet(java.lang.String, com.servoy.j2db.dataprocessing.IDataSet, java.lang.String, java.lang.String, java.lang.String, java.lang.String, int[], String[])
	 */
	public ITable insertDataSet(String client_id, IDataSet set, String dataSource, String serverName, String tableName, String tid, int[] types,
		String[] pkNames) throws ServoyException, RemoteException
	{
		long startTime = System.currentTimeMillis();
		try
		{
			return dataserver.insertDataSet(client_id, set, dataSource, serverName, tableName, tid, types, pkNames);
		}
		finally
		{
			informListeners("FillDataSource", dataSource, null, null, startTime, null);
		}
	}

	/**
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
	 * @return the table where the set was inserted into
	 * @throws ServoyException
	 * @throws RemoteException
	 */
	public ITable insertQueryResult(String client_id, String queryServerName, String queryTid, ISQLSelect sqlSelect, ArrayList<TableFilter> filters,
		boolean distinctInMemory, int startRow, int rowsToRetrieve, int type, String dataSource, String targetServerName, String targetTableName,
		String targetTid, int[] types, String[] pkNames) throws ServoyException, RemoteException
	{
		long startTime = System.currentTimeMillis();
		try
		{
			return dataserver.insertQueryResult(client_id, queryServerName, queryTid, sqlSelect, filters, distinctInMemory, startRow, rowsToRetrieve, type,
				dataSource, targetServerName, targetTableName, targetTid, types, pkNames);
		}
		finally
		{
			QuerySet set = getSQLQuerySet(queryServerName, sqlSelect, filters, startRow, rowsToRetrieve, false);
			informListeners("FillDataSource", queryServerName, set.getSelect().getSql(), queryTid, startTime, set.getSelect().getParameters());
		}
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
		long startTime = System.currentTimeMillis();
		try
		{
			dataserver.dropTemporaryTable(client_id, serverName, tableName);
		}
		finally
		{
			informListeners("DropTempTable", serverName + '.' + tableName, null, null, startTime, null);
		}
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


	public void addDataCallListener(IDataCallListener listener)
	{
		listeners.add(listener);
	}

	public void removeDataCallListener(IDataCallListener listener)
	{
		listeners.remove(listener);
	}
}
