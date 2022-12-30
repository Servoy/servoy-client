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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Procedure;
import com.servoy.j2db.persistence.QuerySet;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.query.ColumnType;
import com.servoy.j2db.query.ISQLQuery;
import com.servoy.j2db.query.ISQLSelect;
import com.servoy.j2db.query.ISQLUpdate;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.xmlxport.ColumnInfoDef;

/**
 * Proxy class around a {@link IDataServer} instance for switchServer support.
 *
 * @author jblok, rgansevles
 */
public class DataServerProxy implements IDataServer
{
	private final IDataServer ds;
	private final String clientId;

	private final Map<String, String> mappedServers = new HashMap<String, String>();
	private final Map<String, BroadcastFilter[]> clientBroadcastFilters = new HashMap<>(); // mapped server name -> broadcastFilters

	public DataServerProxy(IDataServer ds, String clientId)
	{
		this.ds = ds;
		this.clientId = clientId;
	}

	public void switchServer(String sourceName, String destinationName) throws RemoteException
	{
		// move broadcast filters, if any
		BroadcastFilter[] sourceBroadcastFilters = clientBroadcastFilters.get(sourceName);
		if (sourceBroadcastFilters != null)
		{
			ds.setBroadcastFilters(clientId, sourceName, null);
			ds.setBroadcastFilters(clientId, destinationName, sourceBroadcastFilters);
			clientBroadcastFilters.remove(sourceName);
			clientBroadcastFilters.put(destinationName, sourceBroadcastFilters);
		}

		if (sourceName.equals(mappedServers.get(destinationName)))
		{
			mappedServers.remove(destinationName);
		}
		else
		{
			mappedServers.put(sourceName, destinationName);
		}
	}

	public void clear()
	{
		mappedServers.clear();
		try
		{
			clearBroadcastFilters(clientId);
		}
		catch (RemoteException e)
		{
			Debug.error("Error clearing broadcast filters", e);
		}
	}

	public String getMappedServerName(String sourceName)
	{
		String retval = mappedServers.get(sourceName);
		if (retval == null) retval = sourceName;
		return retval;
	}

	/**
	 * Get all names of servers that map to the destName server.
	 *
	 * Will return a collection of at least 1 server.
	 *
	 * @param destName
	 * @return
	 */
	public Collection<String> getReverseMappedServerNames(String destName)
	{
		List<String> reverseMappedServerNames = null;
		for (Entry<String, String> entry : mappedServers.entrySet())
		{
			if (destName != null && destName.equals(entry.getValue()))
			{
				if (reverseMappedServerNames == null)
				{
					reverseMappedServerNames = new ArrayList<>(1);
				}
				reverseMappedServerNames.add(entry.getKey());
			}
		}

		if (reverseMappedServerNames != null)
		{
			return reverseMappedServerNames;
		}

		return Collections.singleton(destName);
	}

	public ISQLStatement createSQLStatement(int action, String serverName, String tableName, Object[] pkColumnData, String tid, String sql,
		Object[] questiondata) throws RemoteException, RepositoryException
	{
		return ds.createSQLStatement(action, getMappedServerName(serverName), tableName, pkColumnData, tid, sql, questiondata);
	}

	public ISQLStatement createSQLStatement(int action, String server_name, String tableName, Object[] pkColumnData, String tid, ISQLUpdate sqlUpdate,
		ArrayList<TableFilter> filters) throws RemoteException, RepositoryException
	{
		return ds.createSQLStatement(action, getMappedServerName(server_name), tableName, pkColumnData, tid, sqlUpdate, filters);
	}

	public Blob getBlob(String _ignoredClientId, String serverName, ISQLSelect blobSelect, ArrayList<TableFilter> filters, String tid)
		throws RepositoryException, RemoteException
	{
		return ds.getBlob(clientId, getMappedServerName(serverName), blobSelect, filters, tid);
	}

	public Object getNextSequence(String serverName, String tableName, String columnName, int columnInfoID, String columnInfoServer)
		throws RepositoryException, RemoteException
	{
		return ds.getNextSequence(getMappedServerName(serverName), tableName, columnName, columnInfoID, columnInfoServer);
	}

	public IDataSet performCustomQuery(String _ignoredClientId, String serverName, String driverTableName, String transaction_id, ISQLSelect sqlSelect,
		ArrayList<TableFilter> filters, int startRow, int rowsToRetrieve) throws ServoyException, RemoteException
	{
		return ds.performCustomQuery(clientId, getMappedServerName(serverName), driverTableName, transaction_id, sqlSelect, filters, startRow, rowsToRetrieve);
	}

	public IDataSet performQuery(String _ignoredClientId, String serverName, String transaction_id, ISQLSelect sqlSelect, ColumnType[] resultTypes,
		ArrayList<TableFilter> filters,
		boolean distinctInMemory, int startRow, int rowsToRetrieve, boolean updateIdleTimestamp) throws ServoyException, RemoteException
	{
		return ds.performQuery(clientId, getMappedServerName(serverName), transaction_id, sqlSelect, resultTypes, filters, distinctInMemory, startRow,
			rowsToRetrieve, updateIdleTimestamp);
	}

	public IDataSet performQuery(String _ignoredClientId, String serverName, String driverTableName, String transaction_id, String sql, Object[] questiondata,
		int startRow, int rowsToRetrieve, boolean updateIdleTimestamp) throws ServoyException, RemoteException
	{
		return ds.performQuery(clientId, getMappedServerName(serverName), driverTableName, transaction_id, sql, questiondata, startRow, rowsToRetrieve,
			updateIdleTimestamp);
	}

	public IDataSet performQuery(String _ignoredClientId, String serverName, String transaction_id, ISQLSelect sqlSelect, ColumnType[] resultTypes,
		ArrayList<TableFilter> filters,
		boolean distinctInMemory, int startRow, int rowsToRetrieve) throws ServoyException, RemoteException
	{
		return ds.performQuery(clientId, getMappedServerName(serverName), transaction_id, sqlSelect, resultTypes, filters, distinctInMemory, startRow,
			rowsToRetrieve);
	}

	public IDataSet performQuery(String _ignoredClientId, String serverName, String driverTableName, String transaction_id, String sql, Object[] questiondata,
		int startRow, int rowsToRetrieve) throws ServoyException, RemoteException
	{
		return ds.performQuery(clientId, getMappedServerName(serverName), driverTableName, transaction_id, sql, questiondata,
			startRow, rowsToRetrieve);
	}

	public IDataSet performQuery(String _ignoredClientId, String serverName, String transaction_id, ISQLSelect sqlSelect, ColumnType[] resultTypes,
		ArrayList<TableFilter> filters, boolean distinctInMemory, int startRow, int rowsToRetrieve, int type) throws ServoyException, RemoteException
	{
		return ds.performQuery(clientId, getMappedServerName(serverName), transaction_id, sqlSelect, resultTypes, filters, distinctInMemory, startRow,
			rowsToRetrieve, type);
	}

	public IDataSet performQuery(String _ignoredClientId, String server_name, String transaction_id, ISQLSelect sqlSelect, ColumnType[] resultTypes,
		ArrayList<TableFilter> filters,
		boolean distinctInMemory, int startRow, int rowsToRetrieve, int type, ITrackingSQLStatement trackingInfo) throws ServoyException, RemoteException
	{
		long time = System.currentTimeMillis();
		try
		{
			return ds.performQuery(clientId, getMappedServerName(server_name), transaction_id, sqlSelect, resultTypes, filters, distinctInMemory, startRow,
				rowsToRetrieve, type, trackingInfo);
		}
		finally
		{
			if (Debug.tracing())
			{
				Debug.trace(type + ", perform query time: " + (System.currentTimeMillis() - time) + " thread: " + Thread.currentThread().getName() + " SQL: " + //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
					sqlSelect);
			}
		}
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IDataServer#performQuery(com.servoy.j2db.dataprocessing.QueryData[])
	 */
	public IDataSet[] performQuery(String _ignoredClientId, String server_name, String transaction_id, QueryData[] array)
		throws ServoyException, RemoteException
	{
		return ds.performQuery(clientId, getMappedServerName(server_name), transaction_id, array);
	}

	public IDataSet performQuery(String _ignoredClientId, String serverName, String driverTableName, String transaction_id, String sql, Object[] questiondata,
		int startRow, int rowsToRetrieve, int type) throws ServoyException, RemoteException
	{
		return ds.performQuery(clientId, getMappedServerName(serverName), driverTableName, transaction_id, sql, questiondata, startRow, rowsToRetrieve, type);
	}

	public Object[] performUpdates(String _ignoredClientId, ISQLStatement[] statements) throws ServoyException, RemoteException
	{
		List<Runnable> resetServernames = new ArrayList<>(statements.length);
		for (ISQLStatement element : statements)
		{
			String sname = element.getServerName();
			element.setServerName(getMappedServerName(sname));
			resetServernames.add(() -> element.setServerName(sname));
		}
		try
		{
			return ds.performUpdates(clientId, statements);
		}
		finally
		{
			resetServernames.forEach(Runnable::run);
		}
	}

	public String startTransaction(String _ignoredClientId, String serverName) throws RepositoryException, RemoteException
	{
		return ds.startTransaction(clientId, getMappedServerName(serverName));
	}

	public boolean endTransactions(String _ignoredClientId, String[] transaction_id, boolean commit) throws RepositoryException, RemoteException
	{
		return ds.endTransactions(clientId, transaction_id, commit);
	}

	public IDataSet acquireLocks(String _ignoredClientId, String serverName, String table_name, Set<Object> pkhashkeys, QuerySelect lockSelect,
		String transaction_id,
		ArrayList<TableFilter> filters, int chunkSize) throws RemoteException, RepositoryException
	{
		return ds.acquireLocks(clientId, getMappedServerName(serverName), table_name, pkhashkeys, lockSelect, transaction_id, filters, chunkSize);
	}

	public boolean releaseLocks(String _ignoredClientId, String serverName, String table_name, Set<Object> pkhashkeys)
		throws RemoteException, RepositoryException
	{
		return ds.releaseLocks(clientId, getMappedServerName(serverName), table_name, pkhashkeys);
	}

	public void addClientAsTableUser(String _ignoredClientId, String serverName, String table_name) throws RemoteException, RepositoryException
	{
		ds.addClientAsTableUser(clientId, getMappedServerName(serverName), table_name);
	}

	public boolean notifyDataChange(String _ignoredClientId, String server_name, String tableName, IDataSet pks, int action, String transaction_id)
		throws RemoteException
	{
		// Note: do not use getMappedServerName() here, this call is for the rawSQL plugin, which is NOT transparent for switch-server
		return ds.notifyDataChange(clientId, server_name, tableName, pks, action, transaction_id);
	}

	@Override
	public void setBroadcastFilters(String _ignoredClientId, String serverName, BroadcastFilter[] broadcastFilters) throws RemoteException
	{
		String mappedServerName = getMappedServerName(serverName);
		if (broadcastFilters == null || broadcastFilters.length == 0)
		{
			clientBroadcastFilters.remove(mappedServerName);
		}
		else
		{
			clientBroadcastFilters.put(mappedServerName, broadcastFilters);
		}
		ds.setBroadcastFilters(clientId, mappedServerName, broadcastFilters);
	}

	@Override
	public void clearBroadcastFilters(String _ignoredClientId) throws RemoteException
	{
		clientBroadcastFilters.clear();
		ds.clearBroadcastFilters(clientId);
	}

	/**
	 * Log a message on the server
	 *
	 * @param msg
	 */
	public void logMessage(String msg) throws RemoteException
	{
		ds.logMessage(msg);
	}

	public InsertResult insertDataSet(String _ignoredClientId, IDataSet set, String dataSource, String serverName, String tableName, String tid,
		ColumnType[] columnTypes, String[] pkNames, HashMap<String, ColumnInfoDef> columnInfoDefinitions) throws ServoyException, RemoteException
	{
		return ds.insertDataSet(clientId, set, dataSource, getMappedServerName(serverName), tableName, tid, columnTypes, pkNames, columnInfoDefinitions);
	}

	public ITable insertQueryResult(String _ignoredClientId, String queryServerName, String queryTid, ISQLSelect sqlSelect, ArrayList<TableFilter> filters,
		boolean distinctInMemory, int startRow, int rowsToRetrieve, int type, String dataSource, String targetServerName, String targetTableName,
		String targetTid, ColumnType[] columnTypes, String[] pkNames) throws ServoyException, RemoteException
	{
		return ds.insertQueryResult(clientId, getMappedServerName(queryServerName), queryTid, sqlSelect, filters, distinctInMemory, startRow, rowsToRetrieve,
			type, dataSource, getMappedServerName(targetServerName), targetTableName, targetTid, columnTypes, pkNames);
	}

	public void dropTemporaryTable(String _ignoredClientId, String serverName, String tableName) throws RemoteException, RepositoryException
	{
		ds.dropTemporaryTable(clientId, getMappedServerName(serverName), tableName);
	}

	public boolean isInServerMaintenanceMode() throws RemoteException
	{
		return ds.isInServerMaintenanceMode();
	}

	public void setServerMaintenanceMode(boolean maintenanceMode) throws RemoteException
	{
		ds.setServerMaintenanceMode(maintenanceMode);
	}

	public QuerySet getSQLQuerySet(String serverName, ISQLQuery sqlQuery, ArrayList<TableFilter> filters, int startRow, int rowsToRetrieve,
		boolean forceQualifyColumns, boolean disableUseArrayForIn) throws RepositoryException, RemoteException
	{
		return ds.getSQLQuerySet(serverName, sqlQuery, filters, startRow, rowsToRetrieve, forceQualifyColumns, disableUseArrayForIn);
	}


	@Override
	public IDataSet[] executeProcedure(String _ignoredClientId, String serverName, String tid, Procedure procedure, Object[] arguments)
		throws RepositoryException, RemoteException
	{
		return ds.executeProcedure(clientId, getMappedServerName(serverName), tid, procedure, arguments);
	}
}
