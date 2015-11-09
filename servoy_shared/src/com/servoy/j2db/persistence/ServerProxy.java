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


import java.io.IOException;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

/**
 * Class for client side cache to prevent recurring network calls
 *
 * @author jblok
 */
public class ServerProxy implements IServer, Serializable
{
	protected final IServer server;

	//local cache
	private String serverName;
	private String databaseProductName;
	// handle serialization of ConcurrentHashMap in writeObject/readObject (tables transient) because in a terracotta environment serialization of
	// ConcurrentHashMap is broken, See http://jira.terracotta.org/jira/browse/CDV-1377
	private volatile transient Map<String, ITable> tables = new ConcurrentHashMap<String, ITable>();

	public ServerProxy(IServer a_server)
	{
		server = a_server;
		// we cannot load all tables with columns from database, it to slow on big databases (1000+ tables)
	}

	private void writeObject(java.io.ObjectOutputStream s) throws IOException
	{
		s.defaultWriteObject();
		s.writeObject(new HashMap<String, ITable>(tables));
	}

	@SuppressWarnings("unchecked")
	private void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException
	{
		s.defaultReadObject();

		tables = new ConcurrentHashMap<String, ITable>();
		tables.putAll((Map< ? extends String, ? extends ITable>)s.readObject());
	}

	@SuppressWarnings("nls")
	public ITable getTable(String tableName) throws RepositoryException, RemoteException
	{
		if (tableName == null) return null;

		String lcname = Utils.toEnglishLocaleLowerCase(tableName);
		ITable table = tables.get(lcname);
		if (table == null)
		{
			// table is not found, now just first load all initialized tables from the server
			// so that it doesn't go one by one. The first client will not find anything, but it will slowly be filled.
			tables.putAll(getInitializedTables());
			table = tables.get(lcname);
			if (table == null)
			{
				// table still not found, now try to get it and initialize it (so that it will be found the next time in getInitializedTables())
				table = server.getTable(tableName);
				if (table != null)
				{
					tables.put(lcname, table);
				}
				else
				{
					Debug.trace("Table " + tableName + " name not found on server: " + getName());
				}
			}
		}
		return table;
	}

	public ITable getTableBySqlname(String tableSQLName) throws RepositoryException, RemoteException
	{
		if (tableSQLName == null) return null;
		for (ITable table : tables.values())
		{
			if (tableSQLName.equals(table.getSQLName()))
			{
				return table;
			}
		}

		tables.putAll(getInitializedTables());
		// try again
		for (ITable table : tables.values())
		{
			if (tableSQLName.equals(table.getSQLName()))
			{
				return table;
			}
		}

		// still not loaded, do remote call
		ITable table = server.getTableBySqlname(tableSQLName);
		if (table != null)
		{
			tables.put(Utils.toEnglishLocaleLowerCase(table.getName()), table);
		}

		return table;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServer#getTables(boolean)
	 */
	public Map<String, ITable> getInitializedTables() throws RepositoryException, RemoteException
	{
		return server.getInitializedTables();
	}

	public List<String> getTableAndViewNames(boolean hideTemporary) throws RepositoryException, RemoteException
	{
		return server.getTableAndViewNames(hideTemporary);
	}

	public List<String> getViewNames(boolean hideTempViews) throws RepositoryException, RemoteException
	{
		return server.getViewNames(hideTempViews);
	}

	public List<String> getTableNames(boolean hideTempTables) throws RepositoryException, RemoteException
	{
		return server.getTableNames(hideTempTables);
	}

	public String getName() throws RemoteException
	{
		if (serverName == null)
		{
			serverName = server.getName();
		}
		return serverName;
	}

	private boolean valid = false;

	public boolean isValid() throws RemoteException
	{
		// cache the valid state when the server was valid on the servoy server.
		// If this toggles for a client then we have other problems anyway.
		if (!valid)
		{
			valid = server.isValid();
		}
		return valid;
	}

	public void combineTables(ServerProxy sp)
	{
		if (sp == null || sp == this) return;

		for (java.util.Map.Entry<String, ITable> entry : sp.tables.entrySet())
		{
			if (entry.getValue() != null && !tables.containsKey(entry.getKey()))
			{
				tables.put(entry.getKey(), entry.getValue());
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
		return ITable.UNKNOWN;
	}

	@Override
	public String[] getDataModelClonesFrom() throws RemoteException
	{
		return server.getDataModelClonesFrom();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.persistence.IServer#getSequenceProvider()
	 */
	@Override
	public ISequenceProvider getSequenceProvider()
	{
		// TODO Auto-generated method stub
		return null;
	}
}
