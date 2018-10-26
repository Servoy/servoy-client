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
import com.servoy.j2db.persistence.IRemoteRepository;
import com.servoy.j2db.persistence.ITeamRepository;
import com.servoy.j2db.persistence.RepositoryException;

public interface IApplicationServerAccess extends Remote
{
	public IRemoteRepository getRepository() throws RemoteException;

	public IDataServer getDataServer() throws RemoteException;

	public IPerformanceRegistry getFunctionPerfomanceRegistry() throws RemoteException;

	public void logout(String clientId) throws RemoteException, RepositoryException;

	public ITeamRepository getTeamRepository() throws RemoteException;

	public IUserManager getUserManager(String clientId) throws RemoteException;

	public int getActiveClientCount(int solution_id) throws RemoteException;

	public int getClientCountForInfo(String info) throws RemoteException;

	/**
	 * Get the license names
	 * 
	 * @return
	 */
	public String[] getLicenseNames() throws RemoteException;
}
