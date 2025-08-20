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

import java.rmi.Remote;
import java.rmi.RemoteException;

import com.servoy.j2db.dataprocessing.IDataServer;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.util.UUID;

public interface IApplicationServerAccess extends Remote
{
	public IRepository getRepository() throws RemoteException;

	public IDataServer getDataServer() throws RemoteException;

	public IPerformanceRegistry getFunctionPerfomanceRegistry() throws RemoteException;

	public void logout(String clientId) throws RemoteException, RepositoryException;

	public IUserManager getUserManager(String clientId) throws RemoteException;

	public int getActiveClientCount(UUID solution_uuid) throws RemoteException;

	public int getClientCountForInfo(String info) throws RemoteException;

	/**
	 * Get the license names
	 *
	 * @return
	 */
	public String[] getLicenseNames() throws RemoteException;
}
