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
package com.servoy.j2db.persistence;


import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

/**
 * Class for client side cache to prevent recurring network calls
 * 
 * @author jblok
 */
public class ServerProxy implements IServer, Serializable
{
	private final IServer server;

	//local cache
	private String serverName;
	private String databaseProductName;
	private final Map<String, ITable> tables = new HashMap<String, ITable>();

	public ServerProxy(IServer a_server)
	{
		server = a_server;
//we cannot load all tables with columns from database, it to slow on big databases (1000+ tables)		
//		try
//		{
//			Iterator it = server.getTableNames().iterator();
//			while (it.hasNext())
//			{
//				String tname = (String) it.next();
//				getTable(tname); //make sure it loaded, so it can be serialized to client
//			}
//		}
//		catch (Exception e)
//		{
//			Debug.error(e);
//		}
	}

	public ITable getTable(String tableName) throws RepositoryException, RemoteException
	{
		if (tableName == null) return null;

		ITable table = tables.get(Utils.toEnglishLocaleLowerCase(tableName));
		if (table == null)
		{
			if (server != null)
			{
				table = server.getTable(tableName);
				tables.put(Utils.toEnglishLocaleLowerCase(tableName), table);
			}
		}
		return table;
	}

	public List<String> getTableAndViewNames() throws RepositoryException, RemoteException
	{
		return server.getTableAndViewNames();
	}

	public List<String> getViewNames() throws RepositoryException, RemoteException
	{
		return server.getViewNames();
	}

	public List<String> getTableNames() throws RepositoryException, RemoteException
	{
		return server.getTableNames();
	}

	public String getName() throws RemoteException
	{
		if (serverName == null)
		{
			serverName = server.getName();
		}
		return serverName;
	}

	public boolean isValid() throws RemoteException
	{
		// Can i proxy this? Or can a valid test always change back to in or valid? 
		return server.isValid();
	}

	void addTable(Table t)
	{
		tables.put(t.getName(), t);
	}

	public void combineTables(ServerProxy sp)
	{
		if (sp == null || sp == this) return;

		Iterator<ITable> it = sp.tables.values().iterator();
		while (it.hasNext())
		{
			Table t = (Table)it.next();
			if (t != null && !tables.containsKey(t.getName()))
			{
				addTable(t);
			}
		}
	}

	@Override
	public String toString()
	{
		try
		{
			return getName();
		}
		catch (RemoteException e)
		{
			Debug.error(e);
			return super.toString();
		}
	}

	public String getDatabaseProductName() throws RepositoryException, RemoteException
	{
		if (databaseProductName == null)
		{
			databaseProductName = server.getDatabaseProductName();
		}
		return databaseProductName;
	}
	
	public String getQuotedIdentifier(String tableSqlName, String columnSqlName) throws RemoteException, RepositoryException
	{
		return server.getQuotedIdentifier(tableSqlName, columnSqlName);
	}
	
	public int getTableType(String tableName) throws RepositoryException, RemoteException
	{
		ITable t = getTable(tableName);
		if (t != null)
		{
			return t.getTableType();
		}
		return 0;
	}
}
