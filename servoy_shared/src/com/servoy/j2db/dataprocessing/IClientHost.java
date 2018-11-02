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

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Date;

/**
 * Client host interface
 * @author jblok
 */
public interface IClientHost extends Remote
{
	public void pushClientInfo(String clientId, ClientInfo clientInfo) throws RemoteException;

	// returns generated { clientId, isLicenced }
	public Object[] register(IUserClient c, ClientInfo clientInfo) throws RemoteException;

	public void unregister(String client_id) throws RemoteException;

	public boolean isRegistered(String client_id) throws RemoteException;

	/**
	 * Helper method
	 *
	 * @param client_id The clients id
	 *
	 * @return server time
	 * @throws RemoteException
	 */
	public Date getServerTime(String client_id) throws RemoteException;
}
