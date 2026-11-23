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

/**
 * Read-only view of a lock held by a client on the server. Only the lock manager can modify the underlying lock.
 * <p>
 * An ILock is a live view, not a snapshot: the lock manager hands out the lock itself (one shared instance for all keys of one acquire),
 * it does not clone it. The server name, table name, client id and acquired timestamp never change. The set of primary key hashes can
 * only shrink: rows are released from an existing lock, keys are never added to it (a new acquire always creates a new lock).
 * <p>
 * So one {@link #getPkHashKeys()} call returns a consistent copy of the keys still locked at the moment of that call. Two calls (or
 * {@link #getPkHashKeySize()} after {@link #getPkHashKeys()}) can return different results when rows were released in between; a later
 * result is always a subset of an earlier one. If you need one consistent view, call {@link #getPkHashKeys()} once and work on that copy.
 */
public interface ILock
{
	/**
	 * @return The server name of the server in which the records live.
	 */
	String getServerName();

	/**
	 * @return The table name of the table in which the records live.
	 */
	String getTableName();

	/**
	 * Returns a copy of the primary key hashes that are locked at the moment of this call. The copy is independent of the lock: changing it
	 * does not change the lock, and later releases do not change the copy. A later call can return fewer keys, never more.
	 *
	 * @return a copy of the set of primary key hashes of the locked records.
	 */
	Set<Object> getPkHashKeys();

	/**
	 * Returns the number of primary key hashes that are locked at the moment of this call. This is not necessarily the size of an earlier
	 * {@link #getPkHashKeys()} result, rows can be released in between.
	 *
	 * @return the current number of primary key hashes of the locked records.
	 */
	int getPkHashKeySize();

	/**
	 * @return The client id of the client which owns the lock.
	 */
	String getClientId();

	/**
	 * @return The timestamp when the lock was acquired.
	 */
	long getAcquired();
}
