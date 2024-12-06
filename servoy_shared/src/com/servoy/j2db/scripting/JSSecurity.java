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

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.base.scripting.api.IJSSecurity;
import com.servoy.j2db.ApplicationException;
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
import com.servoy.j2db.persistence.NameComparator;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.scripting.info.FORMSECURITY;
import com.servoy.j2db.scripting.info.TABLESECURITY;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ILogLevel;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * <p>The <code>security</code> object provides a comprehensive API for managing users, groups, and permissions
 * in a solution. It includes constants for form and table security and methods to control user access and
 * permissions programmatically.</p>
 *
 * <p>Security constants such as <code>ACCESSIBLE</code>, <code>DELETE</code>, <code>INSERT</code>,
 * <code>READ</code>, and <code>UPDATE</code> define flags for controlling access to forms and tables. These
 * constants allow developers to set permissions using datasets and apply them at runtime.</p>
 *
 * <p>The API includes methods for managing users, such as <code>createUser</code>, <code>deleteUser</code>,
 * and <code>changeUserName</code>. Permissions can be assigned or removed with <code>addPermissionToUser</code>
 * and <code>removePermissionFromUser</code>. Developers can query permissions using methods like
 * <code>hasPermission</code> or retrieve user-related information with <code>getUserName</code> and
 * <code>getUserUID</code>.</p>
 *
 * <p>Authentication is supported via the <code>authenticate</code> method, which integrates with custom
 * authenticators or Servoy's built-in system. The API also allows setting and managing tenant values to
 * filter data access by tenant.</p>
 *
 * <p>The <code>security</code> object facilitates dynamic security configurations and provides control
 * over application access at a granular level.</p>
 *
 * <p>For more information, please refer to the overall
 * <a href="../../../guides/develop/security/README.md">Security</a> documentation.</p>
 *
 * @author jcompagner,seb,jblok
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "Security", scriptingName = "security")
public class JSSecurity implements IReturnedTypesProvider, IConstantsObject, IJSSecurity
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
	 * Constant representing the tracking flag for table security (tracks sql insert/update/delete).
	 *
	 * @sampleas js_setSecuritySettings(Object)
	 */
	public static final int TRACKING = IRepository.TRACKING;

	/**
	 * Constant representing the tracking flag for table security (tracks sql select).
	 *
	 * @sampleas js_setSecuritySettings(Object)
	 */
	public static final int TRACKING_VIEWS = IRepository.TRACKING_VIEWS;

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
	 * @clonedesc js_getUserUID(String)
	 * @sampleas js_getUserUID(String)
	 *
	 * @return the userUID
	 */
	public String js_getUserUID() throws ServoyException
	{
		return js_getUserUID(null);
	}

	/**
	 * Set the tenant value for this Client, this value will be used as the value for all tables that have a column marked as a tenant column.
	 * This results in adding a table filter for that table based on that column and the given value, using JSTableFilter.dataBroadcast(true).
	 *<p>
	 * When creating a new record, this value will be auto filled in for all the columns that are marked as a tenant column. If you give an array of values then the first array value is used for this.
	 *</p>
	 *<p>
	 *  When a tenant value is set the client will only receive databroadcasts from other clients that have no or a common tenant value set.
	 *  If the tenant value is a list then the broadcast will be filtered only if there is single element match between the 2 list, so ['a','b'] will match ['a','c'] but not ['c','d'], the actual data of a recod is ignored for this.
	 *  Be sure to not access or depend on records having different tenant values, as no databroadcasts will be received for those
	 *</p>
	 * @param value a single tenant value or an array of tenant values to filter tables having a column flagged as Tenant column by.
	 */
	@JSFunction
	public void setTenantValue(Object value)
	{
		application.getFoundSetManager().setTenantValue(application.getFlattenedSolution().getSolution(), value);
	}

	/**
	 * Retrieve the tenant value for this Client, this value will be used as the value for all tables that have a column marked as a tenant column.
	 * This results in adding a table filter for that table based on that column and the this value.
	 *<p>
	 *  A client with tenant value will only receive databroadcasts from other clients that have no or a common tenant value set
	 *  Be sure to not access or depend on records having different tenant values, as no databroadcasts will be received for those
	 *</p>
	 * @return An array of tenant values for this client.
	 */
	@JSFunction
	public Object[] getTenantValue()
	{
		return application.getFoundSetManager().getTenantValue();
	}

	/**
	 * Get the current user UID (null if not logged in); finds the userUID for given user_name if passed as parameter.
	 *
	 * @sample
	 * //gets the current loggedIn username
	 * var userName = security.getUserName();
	 * //gets the uid of the given username
	 * var userUID = security.getUserUID(userName);
	 * //is the same as above
	 * //var my_userUID = security.getUserUID();
	 *
	 * @param username the username to find the userUID for
	 *
	 * @return the userUID
	 */
	public String js_getUserUID(String username) throws ServoyException
	{
		application.checkAuthorized();
		try
		{
			if (username == null || username.length() == 0)
			{
				// No user name specified, try logged in user.
				return application.getUserUID();
			}
			else
			{
				return application.getUserManager().getUserUID(application.getClientID(), username);
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return null;
	}

	/**
	 * @deprecated  As of release 3.0, replaced by {@link #getUserUID(String)}.
	 */
	@Deprecated
	public Object js_getUserId(Object[] args) throws ServoyException
	{
		if (args == null || args.length != 1) return js_getUserUID();
		else if (args[0] instanceof String) return js_getUserUID((String)args[0]);
		return null;
	}

	//group id's are meaningless pk's (not stable across repositories); don't expose
	/**
	 * Returns the Group ID for the specified group (name)
	 *
	 * @deprecated Deprecated as of release 3.0, not supported anymore.
	 *
	 * @sample
	 * //returns the groupid for the admin group in the variable
	 * //var groupIDForAdmin = security.getGroupId('admin')
	 */
	@Deprecated
	public Object js_getGroupId(String groupName) throws ServoyException
	{
		application.checkAuthorized();
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
	 * @clonedesc js_getUserName(Object)
	 * @sampleas js_getUserName(Object)
	 *
	 * @return the user name
	 */
	@JSFunction
	public String getUserName() throws ServoyException
	{
		return js_getUserName(null);
	}

	/**
	 * Get the current user name (null if not logged in), finds the user name for given user UID if passed as parameter.
	 *
	 * @sample
	 * //gets the current loggedIn username
	 * var userName = security.getUserName();
	 *
	 * @description-mc
	 * returns the current logged in username of that is used when doing a sync.
	 *
	 * @sample-mc
	 * var username = security.getUserName();
	 * if (username != null) {
	 *   // user is logged in
	 * }
	 *
	 * @param userUID the user UID used to retrieve the name
	 *
	 * @return the user name
	 */
	public String js_getUserName(Object userUID) throws ServoyException
	{
		application.checkAuthorized();
		try
		{
			if (userUID == null)
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
				String user_uid = normalizeUID(userUID);
				// A user uid was specified; look up the user name.
				return application.getUserManager().getUserName(application.getClientID(), user_uid);
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
	 * 	// do administration stuff
	 * }
	 *
	 * @param groupName name of the group to check
	 *
	 * @return dataset with groupnames
	 *
	 * @deprecated use hasPermission(permission);
	 */
	@Deprecated
	public boolean js_isUserMemberOfGroup(String groupName) throws ServoyException
	{
		return js_hasPermission(groupName);
	}

	/**
	 * Check if the current user has the given permission
	 *
	 * @sample
	 * //check whatever user is part of the Administrators permission
	 * if(security.hasPermission('Administrators'))
	 * {
	 * 	// do administration stuff
	 * }
	 *
	 * @param permisson name of the permission
	 *
	 * @return true if it has the given permission
	 */
	public boolean js_hasPermission(String permisson) throws ServoyException
	{
		return application.getUserUID() != null ? js_isUserMemberOfGroup(permisson, application.getUserUID()) : false;
	}

	/**
	 * Check whatever the user specified as parameter is part of the specified group.
	 *
	 * @sample
	 * //check whatever user is part of the Administrators group
	 * if(security.isUserMemberOfGroup('Administrators', security.getUserUID('admin')))
	 * {
	 * 	// do administration stuff
	 * }
	 *
	 * @param groupName name of the group to check
	 * @param userUID UID of the user to check
	 *
	 * @return dataset with groupnames
	 *
	 * @deprecated use hasPermission(permission, user);
	 */
	@Deprecated
	public boolean js_isUserMemberOfGroup(String groupName, Object userUID) throws ServoyException
	{
		return js_hasPermission(groupName, userUID);
	}

	/**
	 * Check if the given user has the given permission
	 *
	 * @sample
	 * //check whatever user is part of the Administrators permission
	 * if(security.hasPermission('Administrators', security.getUserUID('admin')))
	 * {
	 * 	// do administration stuff
	 * }
	 *
	 * @param permission name of the permission to check
	 * @param userUID UID of the user to check
	 *
	 * @return true if it has that given permission
	 */
	public boolean js_hasPermission(String permission, Object userUID) throws ServoyException
	{
		JSDataSet userGroups = js_getUserGroups(userUID);
		return userGroups != null ? Arrays.asList(userGroups.js_getColumnAsArray(2)).indexOf(permission) > -1 : false;
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
	 * 	/** @type {JSDataSet} *&#47;
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
	 *
	 * @deprecated use getUserPermissions()
	 */
	@Deprecated
	public JSDataSet js_getUserGroups() throws ServoyException
	{
		return js_getUserPermissions();
	}

	/**
	 * Get all the permissions of the current user.
	 *
	 * @sample
	 * 	//set p to the user permissions for the current user
	 * 	/** @type {JSDataSet} *&#47;
	 * 	var p = security.getUserPermissions();
	 *
	 * 	for(k=1;k<=p.getMaxRowIndex();k++)
	 * 	{
	 * 		//print to the output debugger tab: "permission" and the permissons(s)
	 * 		//the user has
	 * 		application.output("permission: " + p.getValue(k,2));
	 * 	}
	 *
	 * @return dataset with permissions
	 */
	public JSDataSet js_getUserPermissions() throws ServoyException
	{
		JSDataSet groups = null;
		if (application.getUserUID() != null)
		{
			groups = js_getPermissions(application.getUserUID());
		}
		return (groups == null ? new JSDataSet(new ApplicationException(ServoyException.INCORRECT_LOGIN)) : groups);
	}

	/**
	 *  @deprecated use getPermissions(Object)
	 * @param userUID
	 * @return
	 * @throws ServoyException
	 */
	@Deprecated
	public JSDataSet js_getUserGroups(Object userUID) throws ServoyException
	{
		return js_getPermissions(userUID);
	}

	/**
	 * Get all the permissions for given user UID.
	 *
	 * @sample
	 * //get all the users in the security settings (Returns a JSDataset)
	 * var dsUsers = security.getUsers()
	 *
	 * //loop through each user to get their permissions
	 * //The getValue call is (row,column) where column 1 == id and 2 == name
	 * for(var i=1 ; i<=dsUsers.getMaxRowIndex() ; i++)
	 * {
	 * 	//print to the output debugger tab: "user: " and the username
	 * 	application.output("user:" + dsUsers.getValue(i,2));
	 *
	 * 	//set p to the user permissions for the current user
	 * 	/** @type {JSDataSet} *&#47;
	 * 	var p = security.getPermissions(dsUsers.getValue(i,1));
	 *
	 * 	for(k=1;k<=p.getMaxRowIndex();k++)
	 * 	{
	 * 		//print to the output debugger tab: "permission" and the permission(s)
	 * 		//the user has
	 * 		application.output("permission: " + p.getValue(k,2));
	 * 	}
	 * }
	 *
	 * @param userUID to retrieve the user permissions
	 *
	 * @return dataset with permissions names
	 */
	public JSDataSet js_getPermissions(Object userUID) throws ServoyException
	{
		application.checkAuthorized();
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
				List<Object[]> rows = new ArrayList<>();
				for (String element : groups)
				{
					// fetch the id.
					int groupId = getGroupId(element);
					rows.add(new Object[] { Integer.valueOf(groupId), element });
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
	 * Note: this method can only be called by an admin user or a normal logged in user changing its own password.
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
	@SuppressWarnings("nls")
	public boolean js_setPassword(Object a_userUID, String password) throws ServoyException
	{
		application.checkAuthorized();
		if (a_userUID == null) return false;
		String userUID = normalizeUID(a_userUID);
		try
		{
			return application.getUserManager().setPassword(application.getClientID(), userUID.toString(), password, true);
		}
		catch (Exception e)
		{
			application.reportJSError("Can't change password of user  " + a_userUID + ", only admin users can create/change security stuff", e);
			return false;
		}
	}

	/**
	 * @deprecated  As of release 3.0, replaced by {@link #setUserUID(Object,String)}.
	 */
	@Deprecated
	public void js_setUserId(Object userUID, String newUserUID) throws ServoyException
	{
		js_setUserUID(userUID, newUserUID);
	}

	/**
	 * Set a new userUID for the given userUID.
	 * Note: this method can only be called by an admin.
	 *
	 * @param a_userUID the userUID to set the new user UID for
	 * @param newUserUID the new user UID
	 * @return true if changed
	 */
	public boolean js_setUserUID(Object a_userUID, String newUserUID) throws ServoyException
	{
		application.checkAuthorized();
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
		application.checkAuthorized();
		if (a_userUID == null) return false;

		String userUID = normalizeUID(a_userUID);

		try
		{
			return application.getUserManager().checkPasswordForUserUID(application.getClientID(), userUID.toString(), password);
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return false;
	}

	/**
	 * Creates a group, returns the groupname (or null when group couldn't be created).
	 * Note: this method can only be called by an admin.
	 *
	 * @sample
	 * var deleteGroup = true;
	 * //ceate a group
	 * var groupName = security.createGroup('myGroup');
	 * if (groupName)
	 * {
	 *	//create a user
	 *	var uid = security.createUser('myusername', 'mypassword');
	 *	if (uid) //test if user was created
	 *	{
	 *		//set a newUID for the user
	 *		var isChanged = security.setUserUID(uid,'myUserUID')
	 *		// add user to group
	 *		security.addUserToGroup(uid, groupName);
	 *		// if not delete group, do delete group
	 *		if (deleteGroup)
	 *		{
	 *			security.deleteGroup(groupName);
	 *		}
	 *	}
	 *}
	 *
	 * @param groupName the group name to create
	 *
	 * @return the created groupname
	 *
	 * @deprecated creating groups is not supported at runtime
	 */
	@SuppressWarnings("nls")
	@Deprecated
	public String js_createGroup(String groupName) throws ServoyException
	{
		application.checkAuthorized();
		try
		{
			int groupId = application.getUserManager().createGroup(application.getClientID(), groupName);
			if (groupId != -1) return groupName;
		}
		catch (Exception e)
		{
			application.reportJSError("Can't create group: " + groupName + ", only admin users can create/change security stuff", e);
		}
		return null;
	}

	/**
	 * @clonedesc js_createUser(String, String, Object)
	 * @sampleas js_createUser(String, String, Object)
	 *
	 * @param username the username
	 * @param password the user password
	 *
	 * @return the userUID the created userUID, will be same if provided
	 */
	public Object js_createUser(String username, String password) throws ServoyException
	{
		return js_createUser(username, password, null);
	}

	/**
	 * Creates a new user, returns new uid (or null when permission couldn't be created or user alreay exist).
	 * Note: this method can only be called by an admin.
	 *
	 * @sample
	 * var removeUser = true;
	 * //create a user
	 * var uid = security.createUser('myusername', 'mypassword');
	 * if (uid) //test if user was created
	 * {
	 * 	// Get all the permissions
	 * 	var set = security.getPermissions();
	 * 	for(var p = 1 ; p <= set.getMaxRowIndex() ; p++)
	 * 	{
	 * 		// output name of the permission
	 * 		application.output(set.getValue(p, 2));
	 * 		// add permission to user
	 * 		security.addPermissionToUser(uid, set.getValue(p,2));
	 * 	}
	 * 	// if not remove user, remove user from all the permissions
	 * 	if(!removeUser)
	 * 	{
	 * 		// get now all the permissions that that users has (all if above did go well)
	 * 		var set =security.getPermissions(uid);
	 * 		for(var p = 1;p<=set.getMaxRowIndex();p++)
	 * 		{
	 * 			// output name of the permission
	 * 			application.output(set.getValue(p, 2));
	 * 			// remove permission from user
	 * 			security.removePermissionFromUser(uid, set.getValue(p,2));
	 * 		}
	 * 	}
	 * 	else
	 * 	{
	 * 		// delete the user (the user will be removed from the permissions)
	 * 		security.deleteUser(uid);
	 * 	}
	 * }
	 *
	 * @param username the username
	 * @param password the user password
	 * @param userUID the user UID to use
	 *
	 * @return the userUID the created userUID, will be same if provided
	 */
	@SuppressWarnings("nls")
	public Object js_createUser(String username, String password, Object userUID) throws ServoyException
	{
		application.checkAuthorized();
		if (username == null || username.length() == 0 || password == null || password.length() == 0) return null;

		try
		{
			// Check if the user name is free.
			int userId = application.getUserManager().getUserIdByUserName(application.getClientID(), username);
			if (userId != -1) return null;
			String user_uid = normalizeUID(userUID);

			// If the user uid is specified, check if the UID is free.
			if (user_uid != null)
			{
				String userName = application.getUserManager().getUserName(application.getClientID(), user_uid);
				if (userName != null) return null;
			}

			userId = application.getUserManager().createUser(application.getClientID(), username, password, user_uid, false);
			if (userId != -1) return application.getUserManager().getUserUID(application.getClientID(), userId);
		}
		catch (Exception e)
		{
			application.reportJSError("Can't create user: " + username + ", only admin users can create/change security stuff", e);
		}
		return null;
	}

	/**
	 * Deletes an user. returns true if no error was reported.
	 * Note: this method can only be called by an admin.
	 *
	 * @sampleas js_createUser(String, String, Object)
	 *
	 * @param userUID The UID of the user to be deleted.
	 *
	 * @return true if the user is successfully deleted.
	 */
	@SuppressWarnings("nls")
	public boolean js_deleteUser(Object userUID) throws ServoyException
	{
		application.checkAuthorized();
		if (userUID == null) return false;

		String n_userUID = normalizeUID(userUID);
		try
		{
			return application.getUserManager().deleteUser(application.getClientID(), n_userUID.toString());
		}
		catch (Exception e)
		{
			application.reportJSError("Can't delete user: " + userUID + ", only admin users can create/change security stuff", e);
		}
		return false;
	}

	/**
	 * Deletes a group, returns true if no error was reported.
	 * Note: this method can only be called by an admin.
	 *
	 * @sampleas js_createGroup(String)
	 *
	 * @param groupName the name of the group to delete
	 * @return true if deleted
	 * @deprecated adjusting groups is not supported  at runtime.
	 */
	@SuppressWarnings("nls")
	@Deprecated
	public boolean js_deleteGroup(Object groupName) throws ServoyException
	{
		application.checkAuthorized();
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
			application.reportJSError("Can't delete group: " + groupName + ", only admin users can create/change security stuff", e);
		}
		return false;
	}

	/**
	 * Changes the username of the specified userUID.
	 * Note: this method can only be called by an admin user or a normal logged in user changing its own userName.
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
	@SuppressWarnings("nls")
	public boolean js_changeUserName(Object a_userUID, String username) throws ServoyException
	{
		application.checkAuthorized();
		if (a_userUID == null || username == null || username.length() == 0) return false;

		String userUID = normalizeUID(a_userUID);

		try
		{
			return application.getUserManager().changeUserName(application.getClientID(), userUID.toString(), username);
		}
		catch (Exception e)
		{
			application.reportJSError("Can't change username of " + a_userUID + ", only admin users can create/change security stuff", e);
		}
		return false;
	}

	/**
	 * Changes the groupname of a group.
	 * Note: this method can only be called by an admin.
	 *
	 * @sample security.changeGroupName('oldGroup', 'newGroup');
	 *
	 * @param oldGroupName the old name
	 * @param newGroupName the new name
	 * @return true if changed
	 *
	 * @deprecated adjusting groups is not supported  at runtime.
	 */
	@SuppressWarnings("nls")
	@Deprecated
	public boolean js_changeGroupName(Object oldGroupName, String newGroupName) throws ServoyException
	{
		application.checkAuthorized();
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
			application.reportJSError("Can't change groupname of " + oldGroupName + ", only admin users can create/change security stuff", e);
		}
		return false;
	}

	/**
	 * @clonedesc js_getUsers(String)
	 * @sampleas js_getUsers(String)
	 *
	 * @return dataset with all the users
	 */
	public JSDataSet js_getUsers() throws ServoyException
	{
		return js_getUsers(null);
	}

	/**
	 * Get all the users in the security settings (returns a dataset).
	 *
	 * @sampleas js_getUserGroups(Object)
	 * @param groupName the group to filter on
	 * @return dataset with all the users
	 */
	public JSDataSet js_getUsers(String groupName) throws ServoyException
	{
		application.checkAuthorized();
		try
		{
			IDataSet users = null;
			if (groupName != null && groupName.length() > 0)
			{
				users = application.getUserManager().getUsersByGroup(application.getClientID(), groupName);
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
	 * first id column is deprecated!, use only the group name column.
	 *
	 * @sampleas js_createUser(String, String, Object)
	 * @return dataset with all the groups
	 *
	 * @deprecated use getPermissions();
	 */
	@Deprecated
	public JSDataSet js_getGroups() throws ServoyException
	{
		return js_getPermissions();
	}

	/**
	 * Get all the permissions of the solution (returns a dataset).
	 * first id column is deprecated!, use only the permission name column.
	 *
	 * @sampleas js_createUser(String, String, Object)
	 * @return dataset with all the groups
	 */
	public JSDataSet js_getPermissions() throws ServoyException
	{
		application.checkAuthorized();
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
	 * Note: this method can only be called by an admin.
	 *
	 * @sample
	 * var userUID = security.getUserUID();
	 * security.addUserToGroup(userUID, 'groupname');
	 *
	 * @param a_userUID the user UID to be added
	 * @param groupName the group to add to
	 * @return true if added
	 *
	 * @deprecated use addPermissinToUser(Object, Object)
	 */
	@Deprecated
	public boolean js_addUserToGroup(Object a_userUID, Object groupName) throws ServoyException
	{
		return js_addPermissionToUser(a_userUID, groupName);
	}

	/**
	 * Gives a user a permission
	 * Note: this method can only be called by an admin.
	 *
	 * @sample
	 * var userUID = security.getUserUID();
	 * security.addPermissionToUser(userUID, 'permission');
	 *
	 * @param a_userUID the user UID to be added
	 * @param permission the permission to add to
	 * @return true if added
	 *
	 */
	@SuppressWarnings("nls")
	public boolean js_addPermissionToUser(Object a_userUID, Object permission) throws ServoyException
	{
		application.checkAuthorized();
		if (a_userUID == null || permission == null) return false;

		String userUID = normalizeUID(a_userUID);
		try
		{
			int userId = application.getUserManager().getUserIdByUID(application.getClientID(), userUID.toString());
			if (userId != -1)
			{
				if (permission instanceof Number)
				{
					return application.getUserManager().addUserToGroup(application.getClientID(), userId, ((Number)permission).intValue());
				}
				else
				{
					return application.getUserManager().addUserToGroup(application.getClientID(), userId, getGroupId(permission.toString()));
				}
			}
		}
		catch (Exception e)
		{
			application
				.reportJSError("Can't give the user " + a_userUID + " a permission: " + permission + ", only admin users can create/change security stuff", e);
		}
		return false;
	}

	/**
	 * Removes an user from a group.
	 * Note: this method can only be called by an admin.
	 *
	 * @sampleas js_createUser(String, String, Object)
	 *
	 * @param a_userUID the user UID to be removed
	 * @param groupName the group to remove from
	 * @return true if removed
	 *
	 * @deprecated use removePermissionFromUser(Object, Object);
	 */
	@Deprecated
	public boolean js_removeUserFromGroup(Object a_userUID, Object groupName) throws ServoyException
	{
		return js_removePermissionFromUser(a_userUID, groupName);
	}

	/**
	 * Removes an permission from a user.
	 * Note: this method can only be called by an admin.
	 *
	 * @sampleas js_createUser(String, String, Object)
	 *
	 * @param a_userUID the user UID to be removed
	 * @param permission the permission to remove from
	 * @return true if removed
	 */
	@SuppressWarnings("nls")
	public boolean js_removePermissionFromUser(Object a_userUID, Object permission) throws ServoyException
	{
		application.checkAuthorized();
		if (a_userUID == null || permission == null) return false;
		String userUID = normalizeUID(a_userUID);
		try
		{
			int userId = application.getUserManager().getUserIdByUID(application.getClientID(), userUID.toString());
			if (userId != -1)
			{
				if (permission instanceof Number)
				{
					return application.getUserManager().removeUserFromGroup(application.getClientID(), userId, ((Number)permission).intValue());
				}
				else
				{
					return application.getUserManager().removeUserFromGroup(application.getClientID(), userId, getGroupId(permission.toString()));
				}
			}
		}
		catch (Exception e)
		{
			application.reportJSError(
				"Can't remove from the user  " + a_userUID + " the permission " + permission + ", only admin users can create/change security stuff",
				e);
		}
		return false;
	}

	/**
	 * @clonedesc js_logout(String,String,Object)
	 * @sampleas js_logout(String,String,Object)
	 *
	 */
	@JSFunction
	public void logout()
	{
		js_logout(null, null, null);
	}

	/**
	 * @clonedesc js_logout(String,String,Object)
	 * @sampleas js_logout(String,String,Object)
	 *
	 * @param solutionToLoad the solution to load after logout
	 */
	public void js_logout(String solutionToLoad)
	{
		js_logout(solutionToLoad, null, null);
	}

	/**
	 * @clonedesc js_logout(String,String,Object)
	 * @sampleas js_logout(String,String,Object)
	 *
	 * @param solutionToLoad the solution to load after logout
	 * @param method the method to run in the solution to load
	 */
	public void js_logout(String solutionToLoad, String method)
	{
		js_logout(solutionToLoad, method, null);
	}

	/**
	 * @clonedesc js_logout(String,String,Object)
	 * @sampleas js_logout(String,String,Object)
	 *
	 * @param solutionToLoad the solution to load after logout
	 * @param argument the argument to pass to the (login) solution onOpen
	 */
	public void js_logout(String solutionToLoad, Object argument)
	{
		js_logout(solutionToLoad, null, argument);
	}

	/**
	 * Logout the current user and close the solution, if the solution requires authentication and user is logged in.
	 * You can redirect to another solution if needed; if you want to go to a different url, you need to call application.showURL(url) before calling security.logout() (this is only applicable for Web Client).
	 * An alternative option to close a solution and to open another solution, while keeping the user logged in, is application.closeSolution().
	 *
	 * @sample
	 * //Set the url to go to after logout.
	 * //application.showURL('http://www.servoy.com', '_self');  //Web Client only
	 * security.logout();
	 * //security.logout('solution_name');//log out and close current solution and open solution 'solution_name'
	 * //security.logout('solution_name','global_method_name');//log out, close current solution, open solution 'solution_name' and call global method 'global_method_name' of the newly opened solution
	 * //security.logout('solution_name','global_method_name','my_string_argument');//log out, close current solution, open solution 'solution_name', call global method 'global_method_name' with argument 'my_argument'
	 * //security.logout('solution_name','global_second_method_name',2);
	 * //security.logout('solution_name', {a: 'my_string_argument', p1: 'param1', p2: 'param2'});//log out, close current solution, open solution 'solution_name', call (login) solution's onOpen with argument 'my_argument' and queryParams p1,p2
	 * //Note: specifying a solution will not work in the Developer due to debugger dependencies
	 * //specified solution should be of compatible type with client (normal type or client specific(Smart client only/Web client only) type )
	 *
	 * @description-mc
	 * Clears the current credentials that the user specified when doing a sync. When the next sync happens the login form will be shown.
	 *
	 * @sample-mc
	 * security.logout();
	 * plugins.mobile.sync();
	 *
	 * @param solutionToLoad the solution to load after logout
	 * @param method the method to run in the solution to load
	 * @param argument the argument to pass to the method to run
	 */
	public void js_logout(String solutionToLoad, String method, Object argument)
	{
		application.logout(new Object[] { solutionToLoad, method, argument });
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
		return hasAccess(dataSource, IRepository.DELETE);
	}

	/**
	 * @param serverName the server name
	 * @param tableName the table name
	 *
	 * @deprecated replaced by canDelete(String)
	 */
	@Deprecated
	public boolean js_canDelete(Object serverName, Object tableName)
	{
		if (serverName != null && tableName != null)
		{
			return js_canDelete(DataSourceUtils.createDBTableDataSource(serverName.toString(), tableName.toString()));
		}
		return false;
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
		return hasAccess(dataSource, IRepository.UPDATE);
	}

	/**
	 * @param serverName the server name
	 * @param tableName the parameter name
	 *
	 * @deprecated replaced by canUpdate(String)
	 */
	@Deprecated
	public boolean js_canUpdate(Object serverName, Object tableName)
	{
		if (serverName != null && tableName != null)
		{
			return js_canUpdate(DataSourceUtils.createDBTableDataSource(serverName.toString(), tableName.toString()));
		}
		return false;
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
		return hasAccess(dataSource, IRepository.INSERT);
	}

	/**
	 * @param serverName the server name
	 * @param tableName the table name
	 *
	 * @deprecated replaced by canInsert(String)
	 */
	@Deprecated
	public boolean js_canInsert(Object serverName, Object tableName)
	{
		if (serverName != null && tableName != null)
		{
			return js_canInsert(DataSourceUtils.createDBTableDataSource(serverName.toString(), tableName.toString()));
		}
		return false;
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
		return hasAccess(dataSource, IRepository.READ);
	}

	/**
	 * @param serverName the server name
	 * @param tableName the table name
	 *
	 * @deprecated replaced by canRead(String)
	 */
	@Deprecated
	public boolean js_canRead(Object serverName, Object tableName)
	{
		if (serverName != null && tableName != null)
		{
			return js_canRead(DataSourceUtils.createDBTableDataSource(serverName.toString(), tableName.toString()));
		}
		return false;
	}

	private boolean hasAccess(String dataSource, int accessType)
	{
		try
		{
			Table table = (Table)application.getFoundSetManager().getTable(dataSource);
			if (table != null)
			{
				return application.getFoundSetManager().getEditRecordList().hasAccess(table, accessType);
			}
		}
		catch (RepositoryException ex)
		{
			Debug.error(ex);
		}

		return false;
	}

	/**
	 * Returns whether form is viewable.
	 *
	 * security.canView(formName)
	 *
	 * @param formName form name
	 * @return true if viewable
	 */
	@JSFunction
	public boolean canView(String formName)
	{
		return hasFormAccess(formName, null, IRepository.VIEWABLE);
	}


	/**
	 * Returns whether form is accessible.
	 *
	 * security.canAccess(formName)
	 *
	 * @param formName form name
	 * @return true if accessible
	 */
	@JSFunction
	public boolean canAccess(String formName)
	{
		return hasFormAccess(formName, null, IRepository.ACCESSIBLE);
	}

	/**
	 * Returns whether element from form is viewable.
	 *
	 * security.canView(formName,elementName)
	 *
	 * @param formName form name
	 * @param elementName element name from specified form
	 * @return true if viewable
	 */
	@JSFunction
	public boolean canView(String formName, String elementName)
	{
		return hasFormAccess(formName, elementName, IRepository.VIEWABLE);
	}


	/**
	 * Returns whether element from form is accessible.
	 *
	 * security.canAccess(formName,elementName)
	 *
	 * @param formName form name
	 * @param elementName element name from specified form
	 * @return true if accessible
	 */
	@JSFunction
	public boolean canAccess(String formName, String elementName)
	{
		return hasFormAccess(formName, elementName, IRepository.ACCESSIBLE);
	}

	private boolean hasFormAccess(String formName, String elementName, int accessType)
	{
		Form form = application.getFlattenedSolution().getForm(formName);
		int access = 0;
		if (form != null)
		{
			UUID accesUUID = null;
			if (elementName != null)
			{
				for (IPersist persist : form.getFlattenedFormElementsAndLayoutContainers())
				{
					if (persist instanceof ISupportName && Utils.equalObjects(elementName, ((ISupportName)persist).getName()))
					{
						accesUUID = persist.getUUID();
						break;
					}
				}
			}
			else
			{
				accesUUID = form.getUUID();
			}
			if (accesUUID != null)
			{
				access = application.getFlattenedSolution().getSecurityAccess(accesUUID,
					form.getImplicitSecurityNoRights() ? IRepository.IMPLICIT_FORM_NO_ACCESS : IRepository.IMPLICIT_FORM_ACCESS);
			}
		}
		return ((access & accessType) != 0);
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
	 * Example: Permissions names may be received from LDAP (Lightweight Directory Access Protocol) - a standard protocol used in web browsers and email applications to enable lookup queries that access a directory listing.
	 *
	 * @sample
	 * var permissions = ['Administrators']; //normally these groups are for example received from LDAP
	 * var user_uid = scopes.globals.email; //also this uid might be received from external authentication method
	 * var ok =  security.login(scopes.globals.username, user_uid , permissions)
	 * if (!ok)
	 * {
	 * 	plugins.dialogs.showErrorDialog('Login failure',  'Already logged in? or no user_uid/permissions specified?', 'OK')
	 * }
	 *
	 * @param username the username, like 'JamesWebb'
	 * @param a_userUID the user UID to process login for
	 * @param permissions the permissions array
	 * @return true if loggedin
	 */
	public boolean js_login(String username, Object a_userUID, String[] permissions)
	{
		if (application.getUserManager() == null)
		{
			// cannot login locally in client because client has to be authenticated at the server first
			return false;
		}

		String userUID = normalizeUID(a_userUID);

		if (permissions == null || permissions.length == 0 || username == null || username.length() == 0 || userUID == null || userUID.length() == 0)
			return false;


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

		for (String permission : permissions)
		{
			int i;
			for (i = 0; i < groupsDataSet.getRowCount() && !groupsDataSet.getRow(i)[1].equals(permission); i++)
			{
			}
			if (i == groupsDataSet.getRowCount())
			{
				Debug.log("Could not log in user for unknown permission '" + permission + "'", ILogLevel.WARNING); //$NON-NLS-1$//$NON-NLS-2$
				return false;
			}
		}

		ClientInfo ci = application.getClientInfo();
		if (ci.getUserUid() != null && !ci.getUserUid().equalsIgnoreCase(userUID))
		{
			// already logged in
			return false;
		}

		ci.setUserName(username);
		ci.setUserUid(userUID);
		ci.setUserGroups(permissions);
		if (application.getSolution().getSolutionType() != SolutionMetaData.AUTHENTICATOR)
		{
			application.clearLoginForm();
		}
		return true;
	}

	/**
	 * Authenticate the given credentials against the mobile service solution.
	 * It will set the credentials and then do a sync call to the server.
	 *
	 * @sample
	 * // method will return null in mobile client, the same flow as for default login page will happen after calling this method
	 * security.authenticate(['myusername', 'mypassword']);
	 *
	 * @param credentials array whose elements are passed as arguments to the authenticator method, in case of servoy built-in authentication this should be [username, password]
	 *
	 * @return authentication result from authenticator solution or boolean in case of servoy built-in authentication
	 */
	@JSFunction
	public Object authenticate(Object[] credentials)
	{
		return authenticate(null, null, credentials);
	}

	/**
	 * Authenticate to the Servoy Server using one of the installed authenticators or the Servoy default authenticator.
	 *
	 * Note: this method should be called from a login solution, once logged in, the authenticate method has no effect.
	 *
	 * @sample
	 * // create the credentials object as expected by the authenticator solution
	 * var ok =  security.authenticate('myldap_authenticator', 'login', [scopes.globals.userName, scopes.globals.passWord])
	 * if (!ok)
	 * {
	 * 	plugins.dialogs.showErrorDialog('Login failed', 'OK')
	 * }
	 *
	 * // if no authenticator name is used, the credentials are checked using the Servoy built-in user management
	 * ok = security.authenticate(null, null, [scopes.globals.userName, scopes.globals.passWord])
	 *
	 * @description-mc
	 * Authenticate the given credentials against the mobile service solution. First two parameters are not used in mobile solution, just the credentials.
	 * It will set the credentials and then do a sync call to the server.
	 *
	 * @sample-mc
	 * // method will return null in mobile client, the same flow as for default login page will happen after calling this method
	 * security.authenticate(null, null, ['myusername', 'mypassword']);
	 *
	 * @param authenticator_solution authenticator solution installed on the Servoy Server, null for servoy built-in authentication
	 * @param method authenticator method, null for servoy built-in authentication
	 * @param credentials array whose elements are passed as arguments to the authenticator method, in case of servoy built-in authentication this should be [username, password]
	 *
	 * @return authentication result from authenticator solution or boolean in case of servoy built-in authentication
	 */
	@JSFunction
	public Object authenticate(String authenticator_solution, String method, Object[] credentials)
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
	 * var ok =  security.authenticate('myldap_authenticator', 'login', [scopes.globals.userName, scopes.globals.passWord])
	 * if (!ok)
	 * {
	 * 	plugins.dialogs.showErrorDialog('Login failed', 'OK')
	 * }
	 *
	 * // if no authenticator name is used, the credentials are checked using the Servoy built-in user management
	 * ok = security.authenticate(null, null, [scopes.globals.userName, scopes.globals.passWord])
	 *
	 * @param authenticator_solution authenticator solution installed on the Servoy Server, null for servoy built-in authentication
	 * @param method authenticator method, null for servoy built-in authentication
	 *
	 * @return authentication result from authenticator solution or boolean in case of servoy built-in authentication
	 */
	public Object js_authenticate(String authenticator_solution, String method)
	{
		return authenticate(authenticator_solution, method, null);
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
	@SuppressWarnings("nls")
	public void js_setSecuritySettings(Object dataset) // uuid/server.tablename , integer(flags)
	{
		if (dataset instanceof JSDataSet)
		{
			dataset = ((JSDataSet)dataset).getDataSet();
		}
		if (dataset instanceof IDataSet)
		{
			application.getFoundSetManager().getEditRecordList().clearSecuritySettings();

			Map<Object, Integer> sp = new HashMap<Object, Integer>();

			IDataSet ds = (IDataSet)dataset;
			if (ds.getColumnCount() < 2) return;

			for (int i = 0; i < ds.getRowCount(); i++)
			{
				Object[] row = ds.getRow(i);
				if (row[0] != null && row[1] != null)
				{
					Integer val = Integer.valueOf(Utils.getAsInteger(row[1]));
					try
					{
						boolean matched = false;
						if (row[0] instanceof UUID)
						{
							sp.put(row[0], val);
							matched = true;
						}
						else if (row[0].toString().indexOf('-') > 0)
						{
							UUID uuid = UUID.fromString(row[0].toString());
							sp.put(uuid, val);
							matched = true;
						}
						else
						{
							String datasource = row[0].toString();
							if (datasource.indexOf('.') != -1)
							{
								String[] server_table = datasource.split("\\.");
								if (server_table.length == 2)
								{
									IServer server = application.getSolution().getServer(server_table[0]);
									if (server != null)
									{
										ITable table = server.getTable(server_table[1]);
										if (table != null)
										{
											Iterator<String> it = table.getRowIdentColumnNames();
											if (it.hasNext())
											{
												sp.put(Utils.getDotQualitfied(table.getServerName(), table.getName(), it.next()), val);
												matched = true;
											}
										}
									}
								}
							}
						}
						if (!matched)
						{
							Debug.error("security.setSecuritySettings: could not apply security settings for '" + row[0] + "'");
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
	@SuppressWarnings("nls")
	public JSDataSet js_getElementUUIDs(String formname)// return dataset with name, uuid (note: null name is form uuid)
	{
		Form f = application.getFlattenedSolution().getForm(formname);
		if (f == null) f = application.getFormManager().getPossibleForm(formname);
		if (f != null)
		{
			List<Object[]> elements = new ArrayList<>();
			elements.add(new Object[] { null, f.getUUID() });
			Iterator< ? extends IPersist> it = f.isResponsiveLayout() ? f.getFlattenedObjects(NameComparator.INSTANCE).iterator() : f.getAllObjects();
			while (it.hasNext())
			{
				IPersist elem = it.next();
				int type = elem.getTypeID();
				if (type == IRepository.GRAPHICALCOMPONENTS || type == IRepository.FIELDS || type == IRepository.PORTALS || type == IRepository.RECTSHAPES ||
					type == IRepository.SHAPES || type == IRepository.BEANS || type == IRepository.TABPANELS || type == IRepository.WEBCOMPONENTS)
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
