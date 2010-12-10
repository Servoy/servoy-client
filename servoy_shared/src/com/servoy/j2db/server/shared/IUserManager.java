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

import com.servoy.j2db.ISolutionSecurityManager;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.UUID;


/**
 * This is a security manager that not only allows solutions the use of security/user functionality, but also to modify users/groups & security rights.<BR>
 * Inspired by UserManager.
 * 
 * @author acostescu
 */
public interface IUserManager extends ISolutionSecurityManager, Remote
{

	public boolean checkIfUserIsAdministrator(String clientId, String userUid) throws RemoteException;


	public boolean checkIfAdministratorsAreAvailable(String clientId) throws RemoteException;

	public String getUserUID(String clientId, String username) throws ServoyException, RemoteException;

	public int getGroupId(String clientId, String adminGroup) throws ServoyException, RemoteException;

	public IDataSet getUserGroups(String clientId, int userId) throws ServoyException, RemoteException;

	public IDataSet getGroups(String clientId) throws ServoyException, RemoteException;


	public String getUserUID(String clientId, int userId) throws ServoyException, RemoteException;

	public String getUserName(String clientId, int userId) throws ServoyException, RemoteException;

	public String getUserName(String clientId, String userUID) throws ServoyException, RemoteException;


	public String getUserPasswordHash(String clientId, String userUID) throws ServoyException, RemoteException;

	public int getUserIdByUserName(String clientId, String userName) throws ServoyException, RemoteException;

	public int getUserIdByUID(String clientId, String userUID) throws ServoyException, RemoteException;

	public IDataSet getUsers(String clientId) throws ServoyException, RemoteException;

	public IDataSet getUsersByGroup(String clientId, String group_name) throws ServoyException, RemoteException;

	public boolean addUserToGroup(String clientId, int userId, int groupId) throws ServoyException, RemoteException;


	public int createGroup(String clientId, String adminGroup) throws ServoyException, RemoteException;

	//this err constants are related to the createUser method;
	public static int ERR_RESOURCE_PROJECT_MISSING = -2;
	public static int ERR_EMPTY_USERNAME_OR_PASSWORD = -3;
	public static int ERR_USERNAME_EXISTS = -4;


	public int createUser(String clientId, String userName, String password, String userUID, boolean alreadyHashed) throws ServoyException, RemoteException;

	public boolean setPassword(String clientId, String userUID, String password, boolean hashPassword) throws ServoyException, RemoteException;

	public boolean setUserUID(String clientId, String oldUserUID, String newUserUID) throws ServoyException, RemoteException;

	public boolean deleteUser(String clientId, String userUID) throws ServoyException, RemoteException;

	public boolean deleteUser(String clientId, int userId) throws ServoyException, RemoteException;

	public boolean deleteGroup(String clientId, int groupId) throws ServoyException, RemoteException;

	public boolean removeUserFromGroup(String clientId, int userId, int groupId) throws ServoyException, RemoteException;

	public boolean changeGroupName(String clientId, int groupId, String groupName) throws ServoyException, RemoteException;

	public boolean changeUserName(String clientId, String userUID, String newUserName) throws ServoyException, RemoteException;

	public void setFormSecurityAccess(String clientId, String groupName, Integer accessMask, UUID elementUUID, String solutionName) throws ServoyException,
		RemoteException;

	public void setTableSecurityAccess(String clientId, String groupName, Integer accessMask, String connectionName, String tableName, String columnName)
		throws ServoyException, RemoteException;

	public void dispose() throws RemoteException;
}