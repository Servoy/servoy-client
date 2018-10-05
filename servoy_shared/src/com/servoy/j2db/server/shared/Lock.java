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

package com.servoy.j2db.server.shared;

import java.util.Set;
import java.util.TreeSet;

import net.jcip.annotations.ThreadSafe;

/**
 * This class represents a lock held by a client on the server.
 *
 * @author svanerk
 */

@ThreadSafe
public class Lock implements Comparable<Lock>, Cloneable
{
	private final String serverName; // The server name of the server in which the records live.
	private final String tableName; // The table name of the table in which the records live.
	private final Set<Object> pkHashKeySet; // The set of primary key hashes (bad name, suggests that hashes can collide) of the records.
	private final String clientId; // The client id of the client which owns the lock.
	private final long acquiredTimestamp; // The timestamp when the lock was acquired.

	public Lock(final String serverName, final String tableName, final Set<Object> pkHashKeySet, final String clientId, final long acquiredTimestamp)
	{
		this.serverName = serverName;
		this.tableName = tableName;
		this.pkHashKeySet = new TreeSet<Object>(pkHashKeySet);
		this.clientId = clientId;
		this.acquiredTimestamp = acquiredTimestamp;
	}

	public int compareTo(final Lock lock)
	{
		int i;
		i = serverName.compareTo(lock.serverName);
		if (i != 0) return i;
		i = tableName.compareTo(lock.tableName);
		if (i != 0) return i;
		i = clientId.compareTo(lock.clientId);
		if (i != 0) return i;
		i = Long.valueOf(acquiredTimestamp).compareTo(Long.valueOf(lock.acquiredTimestamp));
		if (i != 0) return i;

		// This is to ensure that two Lock objects are not considered to be the same unless all fields are equal.
		return pkHashKeySet.hashCode() - lock.pkHashKeySet.hashCode();
	}

	@Override
	public Object clone()
	{
		return new Lock(serverName, tableName, new TreeSet<Object>(pkHashKeySet), clientId, acquiredTimestamp);
	}

	public String getServerName()
	{
		return serverName;
	}

	public String getTableName()
	{
		return tableName;
	}

	public Set<Object> getPkHashKeys()
	{
		return new TreeSet<Object>(pkHashKeySet);
	}

	public boolean removePkHashKey(Object pkHashKey)
	{
		return pkHashKeySet.remove(pkHashKey);
	}

	public int getPkHashKeySize()
	{
		return pkHashKeySet.size();
	}

	public String getClientId()
	{
		return clientId;
	}

	public long getAcquired()
	{
		return acquiredTimestamp;
	}
}