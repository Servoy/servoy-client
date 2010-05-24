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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.util.Debug;

/**
 * @author jblok
 */
public class GlobalTransaction
{
	private WeakHashMap<Row, GlobalTransaction> rows;//all rows to prevent they are gc'ed
	private Map<String, String> serverTransactions;//serverName->transactionID
	private final IDataServer dataServer;
	private final String clientID;

	GlobalTransaction(IDataServer ds, String cid)
	{
		serverTransactions = new HashMap<String, String>();
		rows = new WeakHashMap<Row, GlobalTransaction>();
		dataServer = ds;
		clientID = cid;
	}

	Collection<String> rollback(boolean queryForNewData)
	{
		if (serverTransactions != null)
		{
			String[] tids = new String[serverTransactions.size()];
			serverTransactions.values().toArray(tids);
			try
			{
				dataServer.endTransactions(clientID, tids, false);
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}

		if (queryForNewData && !rows.isEmpty())
		{
			Set<String> set = new HashSet<String>();
			try
			{
				Iterator<Row> it2 = rows.keySet().iterator();
				while (it2.hasNext())
				{
					// TODO this can be optimized.
					// Search for all the rows with the same table and do a in query (per 200 rows..)
					Row row = it2.next();
					row.rollbackFromDB();
					set.add(row.getRowManager().getFoundsetManager().getDataSource(row.getRowManager().getSQLSheet().getTable()));
				}
				rows = null;
			}
			catch (Exception e)
			{
				Debug.error(e); //TODO: add to application
			}
			return set;
		}
		return Collections.<String> emptyList();
	}

	Collection<String> commit()
	{
		String[] tids = new String[serverTransactions.size()];
		serverTransactions.values().toArray(tids);
		try
		{
			dataServer.endTransactions(clientID, tids, true);
			rows = null;
		}
		catch (Exception e)
		{
			Debug.error(e);//TODO: add to application
		}
		if (rows != null)
		{
			serverTransactions = new HashMap<String, String>();//clear, so they are not processes on IDataService
			return rollback(true);//all others
		}
		return null;
	}

	String addRow(String serverName, Row r) throws RepositoryException
	{
		if (!rows.containsKey(r))
		{
			rows.put(r, this);
		}
		return getTransactionID(serverName);
	}

	public synchronized String getTransactionID(String serverName) throws RepositoryException
	{
		try
		{
			//check if transaction exist for a server
			String tid = serverTransactions.get(serverName);
			if (tid == null)
			{
				//create
				tid = dataServer.startTransaction(clientID, serverName);
				//store
				serverTransactions.put(serverName, tid);
			}
			return tid;
		}
		catch (RemoteException e)
		{
			throw new RepositoryException(e);
		}
	}
}
