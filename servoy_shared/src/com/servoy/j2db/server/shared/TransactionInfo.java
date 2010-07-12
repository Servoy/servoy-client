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

import java.util.Date;

/**
 * This class represents a transaction held by a client on the server.
 *  
 * @author svanerk
 */

public class TransactionInfo
{
	private final String uuid;
	private final String clientId;
	private final String serverName;
	private final int transactionId;
	private final long created;
	private long accessed;

	public TransactionInfo(String uuid, String clientId, String serverName, int transactionId, long created, long accesed)
	{
		this.uuid = uuid;
		this.clientId = clientId;
		this.serverName = serverName;
		this.transactionId = transactionId;
		this.created = created;
		this.accessed = accesed;
	}

	public String getClientId()
	{
		return clientId;
	}

	public String getServerName()
	{
		return serverName;
	}

	public int getTransactionId()
	{
		return transactionId;
	}

	public String getUuid()
	{
		return uuid;
	}

	public long getAccessed()
	{
		return accessed;
	}

	public long getCreated()
	{
		return created;
	}

	public void setAccessedToNow()
	{
		accessed = System.currentTimeMillis();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "TransactionInfo[client:" + clientId + ", transactionid:" + transactionId + ", server:" + serverName + ", accessed:" + new Date(accessed) + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	}
}