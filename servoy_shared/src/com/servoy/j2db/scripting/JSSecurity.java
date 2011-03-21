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
package com.servoy.j2db.scripting;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.servoy.j2db.ApplicationException;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.BufferedDataSet;
import com.servoy.j2db.dataprocessing.ClientInfo;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.dataprocessing.JSDataSet;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.scripting.info.FORMSECURITY;
import com.servoy.j2db.scripting.info.TABLESECURITY;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * JavaScript Object to handle security inside servoy
 * 
 * @author jcompagner,seb,jblok
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "Security", scriptingName = "security")
public class JSSecurity implements IReturnedTypesProvider, IConstantsObject
{
	static
	{
		ScriptObjectRegistry.registerReturnedTypesProviderForClass(JSSecurity.class, new IReturnedTypesProvider()
		{
			public Class< ? >[] getAllReturnedTypes()
			{
				return new Class< ? >[] { TABLESECURITY.class, FORMSECURITY.class };
			}
		});
	}

	/**
	 * Constant representing the read flag for table security.
	 *
	 * @sampleas js_setSecuritySettings(Object)
	 */
	public static final int READ = IRepository.READ;
	/**
	 * Constant representing the insert flag for table security.
	 *
	 * @sampleas js_setSecuritySettings(Object)
	 */
	public static final int INSERT = IRepository.INSERT;
	/**
	 * Constant representing the update flag for table security.
	 *
	 * @sampleas js_setSecuritySettings(Object)
	 */
	public static final int UPDATE = IRepository.UPDATE;
	/**
	 * Constant representing the delete flag for table security.
	 *
	 * @sampleas js_setSecuritySettings(Object)
	 */
	public static final int DELETE = IRepository.DELETE;
	/**
	 * Constant representing the tracking flag for table security.
	 *
	 * @sampleas js_setSecuritySettings(Object)
	 */
	public static final int TRACKING = IRepository.TRACKING;

	/**
	 * Constant representing the viewable flag for form security.
	 *
	 * @sampleas js_setSecuritySettings(Object)
	 */
	public static final int VIEWABLE = IRepository.VIEWABLE;

	/**
	 * Constant representing the accessible flag for form security.
	 *
	 * @sampleas js_setSecuritySettings(Object)
	 */
	public static final int ACCESSIBLE = IRepository.ACCESSIBLE;

	private volatile IApplication application;

	public JSSecurity(IApplication application)
	{
		this.application = application;
	}

	/**
	 * Returns the client ID.
	 *
	 * @sample var clientId = security.getClientID()
	 * @return the clientId as seen on the server admin page 
	 */
	public String js_getClientID()
	{
		return application.getClientID();
	}

	/**
	 * Get the current user UID (null if not logged in), finds the userUID for given user_name if passed as parameter.
	 *
	 * @sample
	 * //gets the current loggedIn username
	 * var userName = security.getUserName(); 
	 * //gets the uid of the given username
	 * var userUID = security.getUserUID(userName);
	 * //is the same as above 
	 * //var my_userUID = security.getUserUID(); 
	 *
	 * @param username optional the username to find the userUID for
	 * @return the userUID 
	 */
	public String js_getUserUID(Object[] args) throws ServoyException
	{
		checkAuthorized();
		try
		{
			if (args == null || args.length != 1)
			{
				// No user name specified, try logged in user.
				return application.getUserUID();
			}
			else
			{
				// Get the user uid for the sepcified user name.
				String userUID = normalizeUID(args[0]);
				return application.getUserManager().getUserUID(application.getClientID(), userUID);
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return null;
	}

	/**
	 * @see com.servoy.j2db.scripting.JSSecurity#js_getUserUID(Object[])
	 */
	@Deprecated
	public Object js_getUserId(Object[] args) throws ServoyException
	{
		return js_getUserUID(args);
	}

	//group id's are meaningless pk's (not stable across repositories); don't expose
	/**
	 * Returns the Group ID for the specified group (name)
	 *
	 * @sample 
	 * //returns the groupid for the admin group in the variable
	 * //var groupIDForAdmin = security.getGroupId('admin')
	 */
	@Deprecated
	public Object js_getGroupId(String groupName) throws ServoyException
	{
		checkAuthorized();
		int gid = getGroupId(groupName);
		if (gid != -1)
		{
			return new Integer(gid);
		}
		return null;
	}

	private int getGroupId(String groupName)
	{
		try
		{
			int groupId = application.getUserManager().getGroupId(application.getClientID(), groupName);
			return groupId;
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return -1;
	}

	/**
	 * Retrieves the username of the currently logged in user on operating system level.
	 *
	 * @sample
	 * //gets the current os username
	 * var osUserName = security.getSystemUserName();
	 * 
	 * @return the os user name
	 */
	public String js_getSystemUserName()
	{
		return System.getProperty("user.name"); //$NON-NLS-1$
	}

	/**
	 * Get the current user name (null if not logged in), finds the user name for given user UID if passed as parameter.
	 *
	 * @sample
	 * //gets the current loggedIn username
	 * var userName = security.getUserName(); 
	 *
	 * @param userUID optional to retrieve the name
	 * @return the user name
	 */
	public String js_getUserName(Object[] args) throws ServoyException
	{
		checkAuthorized();
		try
		{
			if (args == null || args.length != 1)
			{
				if (application.getClientInfo().getUserName() == null && application.getUserUID() != null)
				{
					String userName = application.getUserManager().getUserName(application.getClientID(), application.getUserUID());
					if (userName != null && userName.length() != 0)
					{
						application.getClientInfo().setUserName(userName);
					}
				}
				return application.getClientInfo().getUserName();
			}
			else
			{
				String userUID = normalizeUID(args[0]);
				// A user uid was specified; look up the user name.
				return application.getUserManager().getUserName(application.getClientID(), userUID);
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return null;
	}

	/**
	 * Check whatever the current user is part of the specified group
	 *
	 * @sample
	 * //check whatever user is part of the Administrators group
	 * if(security.isUserMemberOfGroup('Administrators', security.getUserUID('admin')))
	 * {
	 * 		// do administration stuff
	 * }
	 * 
	 * @param groupName name of the group to check
	 * 
	 * @return dataset with groupnames
	 */
	public boolean js_isUserMemberOfGroup(String groupName) throws ServoyException
	{
		return application.getUserUID() != null ? js_isUserMemberOfGroup(groupName, application.getUserUID()) : false;
	}


	/**
	 * Check whatever the user specified as parameter is part of the specified group.
	 *
	 * @sample
	 * //check whatever user is part of the Administrators group
	 * if(security.isUserMemberOfGroup('Administrators', security.getUserUID('admin')))
	 * {
	 * 		// do administration stuff
	 * }
	 * 
	 * @param groupName name of the group to check
	 * @param userUID UID of the user to check
	 * 
	 * @return dataset with groupnames
	 */
	public boolean js_isUserMemberOfGroup(String groupName, Object userUID) throws ServoyException
	{
		JSDataSet userGroups = js_getUserGroups(userUID);
		return userGroups != null ? Arrays.asList(userGroups.js_getColumnAsArray(2)).indexOf(groupName) > -1 : false;
	}

	/**
	 * Get all the groups of the current user.
	 *
	 * @sample
	 * //get all the users in the security settings (Returns a JSDataset)
	 * var dsUsers = security.getUsers()
	 * 
	 * //loop through each user to get their group
	 * //The getValue call is (row,column) where column 1 == id and 2 == name
	 * for(var i=1 ; i<=dsUsers.getMaxRowIndex() ; i++)
	 * {
	 * 	//print to the output debugger tab: "user: " and the username
	 * 	application.output("user:" + dsUsers.getValue(i,2));
	 * 
	 * 	//set p to the user group for the current user
	 * 	var p = security.getUserGroups(dsUsers.getValue(i,1));
	 * 
	 * 	for(k=1;k<=p.getMaxRowIndex();k++)
	 * 	{
	 * 		//print to the output debugger tab: "group" and the group(s)
	 * 		//the user belongs to
	 * 		application.output("group: " + p.getValue(k,2));
	 * 	}
	 * }
	 *
	 * @return dataset with groupnames
	 */
	public JSDataSet js_getUserGroups() throws ServoyException
	{
		JSDataSet groups = null;
		if (application.getUserUID() != null)
		{
			groups = js_getUserGroups(application.getUserUID());
		}
		return (groups == null ? new JSDataSet(new ApplicationException(ServoyException.INCORRECT_LOGIN)) : groups);
	}

	/**
	 * Get all the groups for given user UID.
	 *
	 * @sample
	 * //get all the users in the security settings (Returns a JSDataset)
	 * var dsUsers = security.getUsers()
	 * 
	 * //loop through each user to get their group
	 * //The getValue call is (row,column) where column 1 == id and 2 == name
	 * for(var i=1 ; i<=dsUsers.getMaxRowIndex() ; i++)
	 * {
	 * 	//print to the output debugger tab: "user: " and the username
	 * 	application.output("user:" + dsUsers.getValue(i,2));
	 * 
	 * 	//set p to the user group for the current user
	 * 	var p = security.getUserGroups(dsUsers.getValue(i,1));
	 * 
	 * 	for(k=1;k<=p.getMaxRowIndex();k++)
	 * 	{
	 * 		//print to the output debugger tab: "group" and the group(s)
	 * 		//the user belongs to
	 * 		application.output("group: " + p.getValue(k,2));
	 * 	}
	 * }
	 *
	 * @param userUID to retrieve the user groups
	 * 
	 * @return dataset with groupnames
	 */
	public JSDataSet js_getUserGroups(Object userUID) throws ServoyException
	{
		checkAuthorized();
		if (userUID == null) return null;
		String n_userUID = normalizeUID(userUID);

		try
		{
			String[] groups = null;
			if (n_userUID.equals(application.getUserUID()))
			{
				groups = application.getClientInfo().getUserGroups();
			}
			if (groups == null)
			{
				groups = application.getUserManager().getUserGroups(application.getClientID(), n_userUID.toString());
			}
			if (groups != null)
			{
				List rows = new ArrayList();
				for (String element : groups)
				{
					// fetch the id.
					int groupId = getGroupId(element);
					rows.add(new Object[] { new Integer(groupId), element });
				}
				return new JSDataSet(application, new BufferedDataSet(new String[] { "group_id", "group_name" }, rows)); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return new JSDataSet();
	}

	/**
	 * Set a new password for the given userUID.
	 *
	 * @sample
	 * if(security.checkPassword(security.getUserUID(), 'password1'))
	 * {
	 * 	security.setPassword(security.getUserUID(), 'password2')
	 * }
	 * else
	 * {
	 * 	application.output('wrong password')
	 * }
	 *
	 * @param a_userUID the userUID to set the new password for 
	 * @param password the new password
	 * 
	 * @return true if changed
	 */
	public boolean js_setPassword(Object a_userUID, String password) throws ServoyException
	{
		checkAuthorized();
		if (a_userUID == null) return false;
		String userUID = normalizeUID(a_userUID);
		try
		{
			return application.getUserManager().setPassword(application.getClientID(), userUID.toString(), password, true);
		}
		catch (Exception e)
		{
			Debug.error(e);
			return false;
		}
	}

	/**
	 * @see com.servoy.j2db.scripting.JSSecurity#js_setUserUID(Object, String)
	 */
	@Deprecated
	public void js_setUserId(Object userUID, String newUserUID) throws ServoyException
	{
		js_setUserUID(userUID, newUserUID);
	}

	/**
	 * Set a new userUID for the given userUID.
	 *
	 * @sampleas js_createUser(Object[])
	 *
	 * @param a_userUID the userUID to set the new user UID for 
	 * @param newUserUID the new user UID
	 * @return true if changed
	 */
	public boolean js_setUserUID(Object a_userUID, String newUserUID) throws ServoyException
	{
		checkAuthorized();
		if (a_userUID == null) return false;
		String userUID = normalizeUID(a_userUID);
		try
		{
			return application.getUserManager().setUserUID(application.getClientID(), userUID.toString(), newUserUID);
		}
		catch (Exception e)
		{
			Debug.error(e);
			return false;
		}
	}

	/**
	 * Returns true if the password for that userUID is correct, else false.
	 *
	 * @sample
	 * if(security.checkPassword(security.getUserUID(), 'password1'))
	 * {
	 * 	security.setPassword(security.getUserUID(), 'password2')
	 * }
	 * else
	 * {
	 * 	application.output('wrong password')
	 * }
	 *
	 * @param a_userUID the userUID to check the password for 
	 * @param password the new password
	 * @return true if password oke
	 */
	public boolean js_checkPassword(Object a_userUID, String password) throws ServoyException
	{
		checkAuthorized();
		if (a_userUID == null) return false;

		String userUID = normalizeUID(a_userUID);

		try
		{
			return application.getUserManager().checkPasswordForUserUID(application.getClientID(), userUID.toString(), password, false);
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return false;
	}

	/**
	 * Creates a group, returns the groupname (or null when group couldn't be created).
	 *
	 * @sampleas js_createUser(Object[])
	 *
	 * @param groupName the group name to create
	 * 
	 * @return the created groupname
	 */
	public String js_createGroup(String groupName) throws ServoyException
	{
		checkAuthorized();
		try
		{
			int groupId = application.getUserManager().createGroup(application.getClientID(), groupName);
			if (groupId != -1) return groupName;
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return null;
	}

	/**
	 * Creates a new user, returns new uid (or null when group couldn't be created or user alreay exist).
	 *
	 * @sample
	 * var removeUser = true;
	 * //create a user
	 * var uid = security.createUser('myusername', 'mypassword');
	 * if (uid) //test if user was created
	 * {
	 * 	// Get all the groups
	 * 	var set = security.getGroups();
	 * 	for(var p = 1 ; p <= set.getMaxRowIndex() ; p++)
	 * 	{
	 * 		// output name of the group
	 * 		application.output(set.getValue(p, 2));
	 * 		// add user to group
	 * 		security.addUserToGroup(uid, set.getValue(p,2));
	 * 	}
	 * 	// if not remove user, remove user from all the groups
	 * 	if(!removeUser)
	 * 	{
	 * 		// get now all the groups that that users has (all if above did go well)
	 * 		var set =security.getUserGroups(uid);
	 * 		for(var p = 1;p<=set.getMaxRowIndex();p++)
	 * 		{
	 * 			// output name of the group
	 * 			application.output(set.getValue(p, 2));
	 * 			// remove the user from the group
	 * 			security.removeUserFromGroup(uid, set.getValue(p,2));
	 * 		}
	 * 	}
	 * 	else
	 * 	{
	 * 		// delete the user (the user will be removed from the groups)
	 * 		security.deleteUser(uid);
	 * 	}
	 * }
	 *
	 * @param username the username
	 * @param password the user password
	 * @param userUID optional the userUID to use
	 * @return the userUID the created userUID, will be same if provided
	 */
	public Object js_createUser(Object[] args) throws ServoyException
	{
		checkAuthorized();
		if (args == null || args.length < 2 || args[0] == null || args[1] == null) return null;

		String username = args[0].toString();
		String password = args[1].toString();
		String a_userUID = (args.length > 2) ? args[2].toString() : null;

		if (username == null || username.length() == 0 || password == null || password.length() == 0) return null;

		try
		{
			// Check if the user name is free.
			int userId = application.getUserManager().getUserIdByUserName(application.getClientID(), username);
			if (userId != -1) return null;

			String userUID = normalizeUID(a_userUID);

			// If the user uid is specified, check if the UID is free.
			if (userUID != null)
			{

				String userName = application.getUserManager().getUserName(application.getClientID(), userUID);
				if (userName != null) return null;
			}

			userId = application.getUserManager().createUser(application.getClientID(), username, password, userUID, false);
			if (userId != -1) return application.getUserManager().getUserUID(application.getClientID(), userId);
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return null;
	}

	/**
	 * Deletes an user. returns true if no error was reported.
	 *
	 * @sampleas js_createUser(Object[])
	 *
	 * @param userUID The UID of the user to be deleted.
	 * 
	 * @return true if the user is successfully deleted.
	 */
	public boolean js_deleteUser(Object userUID) throws ServoyException
	{
		checkAuthorized();
		if (userUID == null) return false;

		String n_userUID = normalizeUID(userUID);
		try
		{
			return application.getUserManager().deleteUser(application.getClientID(), n_userUID.toString());
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return false;
	}

	/**
	 * Deletes a group, returns true if no error was reported.
	 *
	 * @sampleas js_createUser(Object[])
	 *
	 * @param groupName the name of the group to delete
	 * @return true if deleted
	 */
	public boolean js_deleteGroup(Object groupName) throws ServoyException
	{
		checkAuthorized();
		if (groupName == null) return false;

		try
		{
			if (groupName instanceof Number)
			{
				return application.getUserManager().deleteGroup(application.getClientID(), ((Number)groupName).intValue());
			}
			else
			{
				int gid = getGroupId(groupName.toString());
				return application.getUserManager().deleteGroup(application.getClientID(), gid);
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return false;
	}

	/**
	 * Changes the username of the specified userUID.
	 *
	 * @sample
	 * if(security.changeUserName(security.getUserUID('name1'), 'name2'))
	 * {
	 * 	application.output('Username changed');
	 * }
	 *
	 * @param a_userUID the userUID to work on
	 * @param username the new username
	 * @return true if changed
	 */
	public boolean js_changeUserName(Object a_userUID, String username) throws ServoyException
	{
		checkAuthorized();
		if (a_userUID == null || username == null || username.length() == 0) return false;

		String userUID = normalizeUID(a_userUID);

		try
		{
			return application.getUserManager().changeUserName(application.getClientID(), userUID.toString(), username);
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return false;
	}

	/**
	 * Changes the groupname of a group.
	 *
	 * @sample security.changeGroupName('oldGroup', 'newGroup');
	 *
	 * @param oldGroupName the old name 
	 * @param newGroupName the new name
	 * @return true if changed
	 */
	public boolean js_changeGroupName(Object oldGroupName, String newGroupName) throws ServoyException
	{
		checkAuthorized();
		if (oldGroupName == null || newGroupName == null || newGroupName.length() == 0 || newGroupName.equals(IRepository.ADMIN_GROUP) ||
			oldGroupName.equals(IRepository.ADMIN_GROUP)) return false;

		try
		{
			if (oldGroupName instanceof Number)
			{
				return application.getUserManager().changeGroupName(application.getClientID(), ((Number)oldGroupName).intValue(), newGroupName);
			}
			else
			{
				return application.getUserManager().changeGroupName(application.getClientID(), getGroupId(oldGroupName.toString()), newGroupName);
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return false;
	}

	/**
	 * Get all the users in the security settings (returns a dataset).
	 *
	 * @sampleas js_getUserGroups(Object)
	 * @param groupName optional the group to filter on
	 * @return dataset with all the users
	 */
	public JSDataSet js_getUsers(Object[] vargs) throws ServoyException
	{
		checkAuthorized();
		try
		{
			IDataSet users = null;
			if (vargs.length == 1)
			{
				users = application.getUserManager().getUsersByGroup(application.getClientID(), vargs[0].toString());
			}
			else
			{
				users = application.getUserManager().getUsers(application.getClientID());
			}
			return users == null ? new JSDataSet() : new JSDataSet(application, users);
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return null;
	}

	/**
	 * Get all the groups (returns a dataset).
	 * first id column is depricated!, use only the group name column.
	 *
	 * @sampleas js_createUser(Object[])
	 * @return dataset with all the groups
	 */
	public JSDataSet js_getGroups() throws ServoyException
	{
		checkAuthorized();
		try
		{
			IDataSet groups = application.getUserManager().getGroups(application.getClientID());
			return groups == null ? new JSDataSet() : new JSDataSet(application, groups);
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return null;
	}

	/**
	 * Adds an user to a named group.
	 *
	 * @sample
	 * var userUID = security.getUserUID();
	 * security.addUserToGroup(userUID, 'groupname');
	 *
	 * @param a_userUID the user UID to be added
	 * @param groupName the group to add to
	 * @return true if added
	 */
	public boolean js_addUserToGroup(Object a_userUID, Object groupName) throws ServoyException
	{
		checkAuthorized();
		if (a_userUID == null || groupName == null) return false;

		String userUID = normalizeUID(a_userUID);
		try
		{
			int userId = application.getUserManager().getUserIdByUID(application.getClientID(), userUID.toString());
			if (userId != -1)
			{
				if (groupName instanceof Number)
				{
					return application.getUserManager().addUserToGroup(application.getClientID(), userId, ((Number)groupName).intValue());
				}
				else
				{
					return application.getUserManager().addUserToGroup(application.getClientID(), userId, getGroupId(groupName.toString()));
				}
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return false;
	}

	/**
	 * Removes an user from a group.
	 *
	 * @sampleas js_createUser(Object[])
	 *
	 * @param a_userUID the user UID to be removed
	 * @param groupName the group to remove from
	 * @return true if removed
	 */
	public boolean js_removeUserFromGroup(Object a_userUID, Object groupName) throws ServoyException
	{
		checkAuthorized();
		if (a_userUID == null || groupName == null) return false;
		String userUID = normalizeUID(a_userUID);
		try
		{
			int userId = application.getUserManager().getUserIdByUID(application.getClientID(), userUID.toString());
			if (userId != -1)
			{
				if (groupName instanceof Number)
				{
					return application.getUserManager().removeUserFromGroup(application.getClientID(), userId, ((Number)groupName).intValue());
				}
				else
				{
					return application.getUserManager().removeUserFromGroup(application.getClientID(), userId, getGroupId(groupName.toString()));
				}
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return false;
	}

	/**
	 * Logout the current user. You can redirect to another solution if needed, if you want to go to a different url, 
	 * you need to call application.showURL(url) before calling security.logout()
	 *
	 * @sample
	 * var groups = new Array();
	 * 	groups[0] = 'testgroup';
	 * 	var ok =  security.login('user1', security.getUserUID('user1') , groups)
	 * 	if (!ok) 
	 * 	{
	 * 		plugins.dialogs.showErrorDialog('Login failure',  'Already logged in? or no user_uid/groups specified?', 'OK')
	 *  	}
	 * 	else 
	 * 	{ 
	 *  		plugins.dialogs.showInfoDialog('Logged in','Logged in','OK') 
	 * 	}
	 *  //Set the url to go to after logout.
	 *  //application.showURL('http://www.servoy.com', '_self'); 
	 * 	security.logout();
	 * 	//security.logout('solution_name');//log out and open solution 'solution_name'
	 * 	//security.logout('solution_name','global_method_name','my_argument');//log out, open solution 'solution_name', call global method 'global_method_name' with argument 'my_argument'
	 * 	//note: specifying a solution will not work in developer due to debugger dependencies
	 *
	 * @param solutionToLoad optional the solution to load after logout
	 * @param method optional the method to run in the solution to load
	 * @param argument optional the argument to pass to the method to run
	 */
	public void js_logout(final Object[] solution_to_open_args)
	{
		application.logout(solution_to_open_args);
	}

	/**
	 * Returns a boolean value for security rights.
	 *
	 * @sampleas js_canUpdate(String)
	 *
	 * @param dataSource the datasource
	 * @return true if allowed
	 */
	public boolean js_canDelete(String dataSource)
	{
		String[] serverAndTableNames = DataSourceUtils.getDBServernameTablename(dataSource);
		if (serverAndTableNames != null) return js_canDelete(serverAndTableNames[0], serverAndTableNames[1]);
		return false;
	}

	@Deprecated
	public boolean js_canDelete(Object serverName, Object tableName)
	{
		Table table = getTable(serverName, tableName);
		if (table != null)
		{
			return application.getFoundSetManager().getEditRecordList().hasAccess(table, IRepository.DELETE);
		}
		else
		{
			return false;
		}
	}

	/**
	 * Returns a boolean value for security rights.
	 *
	 * @sample
	 * var dataSource = controller.getDataSource();
	 * var canDelete = security.canDelete(dataSource);
	 * var canInsert = security.canInsert(dataSource);
	 * var canUpdate = security.canUpdate(dataSource);
	 * var canRead = security.canRead(dataSource);
	 * application.output("Can delete? " + canDelete);
	 * application.output("Can insert? " + canInsert);
	 * application.output("Can update? " + canUpdate);
	 * application.output("Can read? " + canRead);
	 *
	 * @param dataSource the datasource
	 * @return true if allowed
	 */
	public boolean js_canUpdate(String dataSource)
	{
		String[] serverAndTableNames = DataSourceUtils.getDBServernameTablename(dataSource);
		if (serverAndTableNames != null) return js_canUpdate(serverAndTableNames[0], serverAndTableNames[1]);
		return false;
	}

	@Deprecated
	public boolean js_canUpdate(Object serverName, Object tableName)
	{
		Table table = getTable(serverName, tableName);
		if (table != null)
		{
			return application.getFoundSetManager().getEditRecordList().hasAccess(table, IRepository.UPDATE);
		}
		else
		{
			return false;
		}
	}

	/**
	 * Returns a boolean value for security rights.
	 *
	 * @sampleas js_canUpdate(String)
	 *
	 * @param dataSource the datasource
	 * @return true if allowed
	 */
	public boolean js_canInsert(String dataSource)
	{
		String[] serverAndTableNames = DataSourceUtils.getDBServernameTablename(dataSource);
		if (serverAndTableNames != null) return js_canInsert(serverAndTableNames[0], serverAndTableNames[1]);
		return false;
	}

	@Deprecated
	public boolean js_canInsert(Object serverName, Object tableName)
	{
		Table table = getTable(serverName, tableName);
		if (table != null)
		{
			return application.getFoundSetManager().getEditRecordList().hasAccess(table, IRepository.INSERT);
		}
		else
		{
			return false;
		}
	}

	/**
	 * Returns a boolean value for security rights.
	 *
	 * @sampleas js_canUpdate(String)
	 *
	 * @param dataSource the datasource
	 * @return true if allowed
	 */
	public boolean js_canRead(String dataSource)
	{
		String[] serverAndTableNames = DataSourceUtils.getDBServernameTablename(dataSource);
		if (serverAndTableNames != null) return js_canRead(serverAndTableNames[0], serverAndTableNames[1]);
		return false;
	}

	@Deprecated
	public boolean js_canRead(Object serverName, Object tableName)
	{
		Table table = getTable(serverName, tableName);
		if (table != null)
		{
			return application.getFoundSetManager().getEditRecordList().hasAccess(table, IRepository.READ);
		}
		else
		{
			return false;
		}
	}

	private Table getTable(Object serverName, Object tableName)
	{
		if (serverName != null && tableName != null)
		{
			try
			{
				IServer server = application.getSolution().getServer(serverName.toString());
				if (server != null)
				{
					return (Table)server.getTable(tableName.toString());
				}
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
		return null;
	}

	//made easier for developers to work with old ids
	private String normalizeUID(Object a_userUID)
	{
		String userUID = null;
		if (a_userUID instanceof Number)
		{
			userUID = "" + ((Number)a_userUID).longValue(); //prevent '23.0' formatting for numbers (the old ids where numbers)  //$NON-NLS-1$
		}
		else if (a_userUID != null)
		{
			userUID = a_userUID.toString();
		}
		return userUID;
	}

	/**
	 * Login to be able to leave the solution loginForm.
	 * 
	 * Example: Group names may be received from LDAP (Lightweight Directory Access Protocol) - a standard protocol used in web browsers and email applications to enable lookup queries that access a directory listing. 
	 *
	 * @sample
	 * var groups = new Array()
	 * groups[0] = 'Administrators'; //normally these groups are for example received from LDAP
	 * var user_uid = globals.email; //also this uid might be received from external authentication method
	 * var ok =  security.login(globals.username, user_uid , groups)
	 * if (!ok)
	 * {
	 * 	plugins.dialogs.showErrorDialog('Login failure',  'Already logged in? or no user_uid/groups specified?', 'OK')
	 * }
	 *
	 * @param display_username the user display name, like 'James Webb'
	 * @param a_userUID the user UID to process login for
	 * @param groups the groups array 
	 * @return true if loggedin
	 */
	public boolean js_login(String display_username, Object a_userUID, String[] groups)
	{
		if (application.getUserManager() == null)
		{
			// cannot login locally in client because client has to be authenticated at the server first
			return false;
		}

		String userUID = normalizeUID(a_userUID);

		if (groups == null || groups.length == 0 || display_username == null || display_username.length() == 0 || userUID == null || userUID.length() == 0) return false;


		// check if the groups all exist
		IDataSet groupsDataSet;
		try
		{
			groupsDataSet = application.getUserManager().getGroups(application.getClientID());
		}
		catch (Exception e)
		{
			Debug.error(e);
			return false;
		}

		for (int g = 0; g < groups.length; g++)
		{
			int i;
			for (i = 0; i < groupsDataSet.getRowCount() && !groupsDataSet.getRow(i)[1].equals(groups[g]); i++)
			{
			}
			if (i == groupsDataSet.getRowCount())
			{
				Debug.log("Could not log in user for unknown group '" + groups[g] + "'"); //$NON-NLS-1$//$NON-NLS-2$
				return false;
			}
		}

		ClientInfo ci = application.getClientInfo();
		if (ci.getUserUid() != null && !ci.getUserUid().equalsIgnoreCase(userUID))
		{
			// already logged in
			return false;
		}

		ci.setUserName(display_username);
		ci.setUserUid(userUID);
		ci.setUserGroups(groups);
		if (application.getSolution().getSolutionType() != SolutionMetaData.AUTHENTICATOR)
		{
			application.clearLoginForm();
		}
		return true;
	}

	private void checkAuthorized() throws ServoyException
	{
		if (!application.haveRepositoryAccess())
		{
			// no access to repository yet, have to log in first
			throw new ServoyException(ServoyException.CLIENT_NOT_AUTHORIZED);
		}
	}

	/**
	 * Authenticate to the Servoy Server using one of the installed authenticators or the Servoy default authenticator.
	 * 
	 * Note: this method should be called from a login solution.
	 * 
	 * @sample
	 * // create the credentials object as expected by the authenticator solution
	 * var ok =  security.authenticate('myldap_authenticator', 'login', [globals.userName, globals.passWord])
	 * if (!ok)
	 * {
	 * 	plugins.dialogs.showErrorDialog('Login failed', 'OK')
	 * }
	 * 
	 * // if no authenticator name is used, the credentials are checked using the Servoy built-in user management
	 * ok = security.authenticate(null, null, [globals.userName, globals.passWord])
	 *
	 * @param authenticator_solution authenticator solution installed on the Servoy Server, null for servoy built-in authentication
	 * @param method authenticator method, null for servoy built-in authentication
	 * @param credentials array whose elements are passed as arguments to the authenticator method, in case of servoy built-in authentication this should be [username, password]
	 * 
	 * @return authentication result from authenticator solution or boolean in case of servoy built-in authentication
	 */
	public Object js_authenticate(String authenticator_solution, String method, Object[] credentials)
	{
		try
		{
			return application.authenticate(authenticator_solution, method, credentials);
		}
		catch (ServoyException e)
		{
			Debug.error(e);
			return null;
		}
	}

	/**
	 * Authenticate to the Servoy Server using one of the installed authenticators or the Servoy default authenticator.
	 * 
	 * Note: this method should be called from a login solution.
	 * 
	 * @sample
	 * // create the credentials object as expected by the authenticator solution
	 * var ok =  security.authenticate('myldap_authenticator', 'login', [globals.userName, globals.passWord])
	 * if (!ok)
	 * {
	 * 	plugins.dialogs.showErrorDialog('Login failed', 'OK')
	 * }
	 * 	
	 * // if no authenticator name is used, the credentials are checked using the Servoy built-in user management
	 * ok = security.authenticate(null, null, [globals.userName, globals.passWord])
	 *
	 * @param authenticator_solution authenticator solution installed on the Servoy Server, null for servoy built-in authentication
	 * @param method authenticator method, null for servoy built-in authentication
	 * 
	 * @return authentication result from authenticator solution or boolean in case of servoy built-in authentication
	 */
	public Object js_authenticate(String authenticator_solution, String method)
	{
		return js_authenticate(authenticator_solution, method, null);
	}

	/**
	 * Sets the security settings; the entries contained in the given dataset will override those contained in the current security settings.
	 * 
	 * NOTE: The security.getElementUUIDs and security.setSecuritySettings functions can be used to define custom security that overrides Servoy security. 
	 * For additional information see the function security.getElementUUIDs. 
	 *
	 * @sample
	 * var colNames = new Array();
	 * colNames[0] = 'uuid';
	 * colNames[1] = 'flags';
	 * var dataset = databaseManager.createEmptyDataSet(0,colNames);
	 * 
	 * var row = new Array();
	 * row[0] = '413a4d69-becb-4ae4-8fdd-980755d6a7fb';//normally retreived via security.getElementUUIDs(...)
	 * row[1] = JSSecurity.VIEWABLE|JSSecurity.ACCESSIBLE; // use bitwise 'or' for both
	 * dataset.addRow(row);//setting element security
	 * 
	 * row = new Array();
	 * row[0] = 'example_data.orders';
	 * row[1] = JSSecurity.READ|JSSecurity.INSERT|JSSecurity.UPDATE|JSSecurity.DELETE|JSSecurity.TRACKING; //use bitwise 'or' for multiple flags
	 * dataset.addRow(row);//setting table security
	 * 
	 * security.setSecuritySettings(dataset);//to be called in solution startup method
	 *
	 * @param dataset the dataset with security settings
	 */
	public void js_setSecuritySettings(Object dataset) // uuid/server.tablename , integer(flags)
	{
		if (dataset instanceof JSDataSet)
		{
			dataset = ((JSDataSet)dataset).getDataSet();
		}
		if (dataset instanceof IDataSet)
		{
			Map<Object, Integer> sp = new HashMap<Object, Integer>();

			IDataSet ds = (IDataSet)dataset;
			if (ds.getColumnCount() < 2) return;

			for (int i = 0; i < ds.getRowCount(); i++)
			{
				Object[] row = ds.getRow(i);
				if (row[0] != null && row[1] != null)
				{
					Integer val = new Integer(Utils.getAsInteger(row[1]));
					try
					{
						if (row[0] instanceof UUID)
						{
							sp.put(row[0], val);
						}
						else if (row[0].toString().indexOf('-') > 0)
						{
							UUID uuid = UUID.fromString(row[0].toString());
							sp.put(uuid, val);
						}
						else
						{
							String datasource = row[0].toString();
							if (datasource.indexOf('.') != -1)
							{
								String[] server_table = datasource.split("\\.");
								IServer server = application.getSolution().getServer(server_table[0]);
								if (server != null)
								{
									ITable table = server.getTable(server_table[1]);
									if (table != null)
									{
										Iterator it = table.getRowIdentColumnNames();
										if (it.hasNext())
										{
											sp.put(Utils.getDotQualitfied(table.getServerName(), table.getName(), it.next()), val);
										}
									}
								}
							}
						}
					}
					catch (Exception e)
					{
						Debug.error(e);
					}
				}
			}
			application.getFlattenedSolution().overrideSecurityAccess(sp);
		}
	}

	/**
	 * Returns the form elements UUID's as dataset, the one with no name is the form itself.
	 *
	 * @sample var formElementsUUIDDataSet = security.getElementUUIDs('orders_form');
	 *
	 * @param formname the formname to retieve the dataset for
	 * @return dataset with element info
	 */
	public JSDataSet js_getElementUUIDs(String formname)// return dataset with name, uuid (note: null name is form uuid)
	{
		Form f = application.getFlattenedSolution().getForm(formname);
		if (f == null) f = ((FormManager)application.getFormManager()).getPossibleForm(formname);
		if (f != null)
		{
			List elements = new ArrayList();
			elements.add(new Object[] { null, f.getUUID() });
			Iterator it = f.getAllObjects();
			while (it.hasNext())
			{
				IPersist elem = (IPersist)it.next();
				int type = elem.getTypeID();
				if (type == IRepository.GRAPHICALCOMPONENTS || type == IRepository.FIELDS || type == IRepository.PORTALS || type == IRepository.RECTSHAPES ||
					type == IRepository.SHAPES || type == IRepository.BEANS || type == IRepository.TABPANELS)
				{
					if (elem instanceof ISupportName && ((ISupportName)elem).getName() != null)
					{
						elements.add(new Object[] { ((ISupportName)elem).getName(), elem.getUUID() });
					}
				}
			}
			IDataSet set = new BufferedDataSet(new String[] { "name", "uuid" }, elements);
			return new JSDataSet(application, set);
		}
		return new JSDataSet(application);
	}

	public Class< ? >[] getAllReturnedTypes()
	{
		return new Class[] { TABLESECURITY.class, FORMSECURITY.class };
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "security"; //$NON-NLS-1$
	}

	public void destroy()
	{
		application = null;
	}
}
