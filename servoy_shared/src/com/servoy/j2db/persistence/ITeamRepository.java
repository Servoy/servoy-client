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

import java.rmi.RemoteException;
import java.util.Date;

import com.servoy.j2db.util.UUID;

/**
 * @author jblok
 */
public interface ITeamRepository extends IRemoteRepository
{
	/**
	 * Load an IRootObject
	 * 
	 * @param name
	 * @param objectTypeId (SOLUTION/STYLE see IRepository)
	 * @param release (0 = active release)
	 * @return object implementing IRootObject such as style or solution
	 * @throws RepositoryException
	 */
	public IRootObject getRootObject(String name, int objectTypeId, int release) throws RepositoryException, RemoteException;

	/**
	 * Save an IRootObject (all child's are checked for changed flag, if changed they are stored)
	 * 
	 * @param rootObject style or solution
	 * @throws RepositoryException
	 */
	public void updateTeamRootObject(IRootObject rootObject) throws RepositoryException, RemoteException;

	/**
	 * To be able to check if you are still talking with same repository as checkout did originate from
	 * 
	 * @throws RepositoryException
	 * @throws RemoteException
	 */
	public UUID getRepositoryUUID() throws RepositoryException, RemoteException;

	/**
	 * To be able to check if team operation can be performed on the remote repository version
	 * 
	 * @throws RepositoryException
	 * @throws RemoteException
	 */
	public int getRepositoryVersion() throws RemoteException;

	/**
	 * push a eclipse configured table to the server to be synced with the actual database (this will also change the servoy_columninfo)
	 * 
	 * @param serverName synch table's server name
	 * @param tableName synch table name
	 * @param dm the table to be synced or null if it was removed
	 * @throws RepositoryException
	 * @throws RemoteException
	 */
	public void updateDataModel(String serverName, String tableName, Table dm) throws RepositoryException, RemoteException;


	public void deleteStringResource(String name, int objectTypeId) throws RepositoryException, RemoteException;

	public Date[] getRootObjectReleaseDates(int root_element_id) throws RemoteException, RepositoryException;

	/**
	 * Get i18n tables binded to at least one solution
	 * 
	 * @return all i18n tables binded to at least one solution
	 * @throws RemoteException
	 */
	public String[] getI18NDatasources() throws RemoteException, RepositoryException;
}
