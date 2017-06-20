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
import java.util.HashSet;
import java.util.Set;

import com.servoy.base.query.BaseQueryTable;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.QuerySet;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.query.ColumnType;
import com.servoy.j2db.query.ISQLQuery;
import com.servoy.j2db.query.ISQLSelect;
import com.servoy.j2db.query.ISQLUpdate;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.visitor.IVisitable;
import com.servoy.j2db.util.visitor.IVisitor;

/**
 * @author rgansevles
 *
 */
public class ValidatingDelegateDataServer extends AbstractDelegateDataServer
{
	private final IServiceProvider application;

	/**
	 * @param dataserver
	 */
	public ValidatingDelegateDataServer(IDataServer dataserver, IServiceProvider application)
	{
		super(dataserver);
		this.application = application;
	}

	protected void validateQuery(IVisitable[] queries)
	{
		if (queries != null)
		{
			for (IVisitable query : queries)
			{
				validateQuery(query);
			}
		}
	}

	protected void validateQuery(ISQLStatement[] statements)
	{
		if (statements != null)
		{
			for (ISQLStatement statement : statements)
			{
				validateQuery(statement.getUpdate());
				validateQuery(statement.getRequerySelect());
			}
		}
	}

	protected void validateQuery(IVisitable query)
	{
		if (query != null)
		{
			final Set<String> datasources = new HashSet<>();
			query.acceptVisitor(new IVisitor()
			{
				@Override
				public Object visit(Object o)
				{
					if (o instanceof BaseQueryTable)
					{
						String dataSource = ((BaseQueryTable)o).getDataSource();
						if (dataSource != null)
						{
							datasources.add(dataSource);
						}
					}
					return o;
				}
			});
			if (datasources.size() > 1)
			{
				String singleServer = null;
				for (String datasource : datasources)
				{
					String serverName = DataSourceUtils.getDataSourceServerName(datasource);
					if (singleServer == null)
					{
						singleServer = serverName;
					}
					else if (serverName != null && !singleServer.equals(serverName))
					{
						String msg = "Cannot perform multiserver-query '" + serverName + "'/'" + singleServer + "'";
						application.reportJSError(msg, query);
					}
				}
			}
		}
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
	@Override
	public IDataSet acquireLocks(String client_id, String server_name, String table_name, Set<Object> pkhashkeys, QuerySelect lockSelect, String transaction_id,
		ArrayList<TableFilter> filters, int chunkSize) throws RemoteException, RepositoryException
	{
		validateQuery(lockSelect);
		return super.acquireLocks(client_id, server_name, table_name, pkhashkeys, lockSelect, transaction_id, filters, chunkSize);
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
	@Override
	public IDataSet performQuery(String client_id, String server_name, String transaction_id, ISQLSelect sqlSelect, ArrayList<TableFilter> filters,
		boolean distinctInMemory, int startRow, int rowsToRetrieve) throws ServoyException, RemoteException
	{
		validateQuery(sqlSelect);
		return super.performQuery(client_id, server_name, transaction_id, sqlSelect, filters, distinctInMemory, startRow, rowsToRetrieve);
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
	@Override
	public IDataSet performQuery(String client_id, String server_name, String transaction_id, ISQLSelect sqlSelect, ArrayList<TableFilter> filters,
		boolean distinctInMemory, int startRow, int rowsToRetrieve, int type) throws ServoyException, RemoteException
	{
		validateQuery(sqlSelect);
		return super.performQuery(client_id, server_name, transaction_id, sqlSelect, filters, distinctInMemory, startRow, rowsToRetrieve, type);
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
	@Override
	public IDataSet performQuery(String client_id, String server_name, String transaction_id, ISQLSelect sqlSelect, ArrayList<TableFilter> filters,
		boolean distinctInMemory, int startRow, int rowsToRetrieve, int type, ITrackingSQLStatement trackingInfo) throws ServoyException, RemoteException
	{
		validateQuery(sqlSelect);
		return super.performQuery(client_id, server_name, transaction_id, sqlSelect, filters, distinctInMemory, startRow, rowsToRetrieve, type, trackingInfo);
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
	@Override
	public IDataSet performQuery(String client_id, String server_name, String transaction_id, ISQLSelect sqlSelect, ArrayList<TableFilter> filters,
		boolean distinctInMemory, int startRow, int rowsToRetrieve, boolean updateIdleTimestamp) throws ServoyException, RemoteException
	{
		validateQuery(sqlSelect);
		return super.performQuery(client_id, server_name, transaction_id, sqlSelect, filters, distinctInMemory, startRow, rowsToRetrieve, updateIdleTimestamp);
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
	@Override
	public IDataSet performCustomQuery(String client_id, String server_name, String driverTableName, String transaction_id, ISQLSelect sqlSelect,
		ArrayList<TableFilter> filters, int startRow, int rowsToRetrieve) throws ServoyException, RemoteException
	{
		validateQuery(sqlSelect);
		return super.performCustomQuery(client_id, server_name, driverTableName, transaction_id, sqlSelect, filters, startRow, rowsToRetrieve);
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
	@Override
	public IDataSet[] performQuery(String client_id, String server_name, String transaction_id, QueryData[] array) throws ServoyException, RemoteException
	{
		validateQuery(array);
		return super.performQuery(client_id, server_name, transaction_id, array);
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
	@Override
	public ISQLStatement createSQLStatement(int action, String server_name, String tableName, Object[] pkColumnData, String tid, ISQLUpdate sqlUpdate,
		ArrayList<TableFilter> filters) throws RemoteException, RepositoryException
	{
		validateQuery(sqlUpdate);
		return super.createSQLStatement(action, server_name, tableName, pkColumnData, tid, sqlUpdate, filters);
	}

	/**
	 * @param clientId
	 * @param statements
	 * @return
	 * @throws ServoyException
	 * @throws RemoteException
	 * @see com.servoy.j2db.dataprocessing.IDataServer#performUpdates(java.lang.String, com.servoy.j2db.dataprocessing.ISQLStatement[])
	 */
	@Override
	public Object[] performUpdates(String clientId, ISQLStatement[] statements) throws ServoyException, RemoteException
	{
		validateQuery(statements);
		return super.performUpdates(clientId, statements);
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
	@Override
	public Blob getBlob(String clientId, String serverName, ISQLSelect blobSelect, ArrayList<TableFilter> filters, String tid)
		throws RepositoryException, RemoteException
	{
		validateQuery(blobSelect);
		return super.getBlob(clientId, serverName, blobSelect, filters, tid);
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
	@Override
	public ITable insertQueryResult(String client_id, String queryServerName, String queryTid, ISQLSelect sqlSelect, ArrayList<TableFilter> filters,
		boolean distinctInMemory, int startRow, int rowsToRetrieve, int type, String dataSource, String targetServerName, String targetTableName,
		String targetTid, ColumnType[] columnTypes, String[] pkNames) throws ServoyException, RemoteException
	{
		validateQuery(sqlSelect);
		return super.insertQueryResult(client_id, queryServerName, queryTid, sqlSelect, filters, distinctInMemory, startRow, rowsToRetrieve, type, dataSource,
			targetServerName, targetTableName, targetTid, columnTypes, pkNames);
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
	@Override
	public QuerySet getSQLQuerySet(String serverName, ISQLQuery sqlQuery, ArrayList<TableFilter> filters, int startRow, int rowsToRetrieve,
		boolean forceQualifyColumns) throws RepositoryException, RemoteException
	{
		validateQuery(sqlQuery);
		return super.getSQLQuerySet(serverName, sqlQuery, filters, startRow, rowsToRetrieve, forceQualifyColumns);
	}
}
