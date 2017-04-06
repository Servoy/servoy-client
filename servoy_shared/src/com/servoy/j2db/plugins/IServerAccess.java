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
package com.servoy.j2db.plugins;


import java.io.InputStream;
import java.net.URL;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.sql.Connection;

import javax.servlet.http.HttpServlet;

import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.server.shared.IClientInformation;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.UUID;

/**
 * Api to use by server plugins.
 *
 * @author jblok
 */
public interface IServerAccess extends IPluginAccess
{
	/**
	 * Get the broadcast interface where you can listen to b
	 * @return
	 */
	public IDataNotifyService getDataNotifyService();

	/**
	 * Register a webservice in the application web server Note on the server 'init(servletConfig)' is never called, only 'init()'. The service comes available
	 * as http://<host>[:port]/servoy-service/<webServiceAlias>
	 *
	 * @param webServiceAlias do start your webServiceAlias with your pluginname, like 'my_pluginName_webService'.
	 * @param service_servlet
	 */
	public void registerWebService(String webServiceAlias, HttpServlet service_servlet);

	/**
	 * Register a RMI remote object in the application server.
	 *
	 * @param interfaceClassName
	 * @param obj
	 * @deprecated
	 */
	@Deprecated
	public void registerRMIService(String interfaceClassName, Remote obj) throws RemoteException;

	/**
	 * Register a RMI remote object in the application server.
	 *
	 * @param interfaceClassName
	 * @param obj
	 */
	public void registerRemoteService(String interfaceClassName, Remote obj) throws RemoteException;

	/**
	 * Get a remote server service, will not work in the Servoy Runtime product!
	 */
	public Remote getRemoteService(String rmiLookupName);

	/**
	 * Get the clientID which is local to the server, should only be used when NO client is present!
	 *
	 * @return String the server local client id.
	 */
	public String getServerLocalClientID();

	/**
	 * Get a pooled and reserved jdbc connection, do not forget to close() if done with it (leave commit/rollback to starter of transaction).
	 *
	 * @param server_name
	 * @param transaction_id null if not present/used
	 * @return the connection, null if server or transaction not present.
	 * @since 3.1
	 */
	public Connection getDBServerConnection(String server_name, String transaction_id);

	/**
	 * Get a pooled raw jdbc connection, do not forget to close() if done with it.
	 *
	 * @param server_name
	 * @return the connection, null if server not present.
	 * @since 2.1
	 */
	public Connection getDBServerConnection(String server_name);//keep to not break existing plugins

	/**
	 * To retrieve servoy sequence values.
	 *
	 * @param server_name
	 * @param table_name
	 * @param column_name
	 * @return the next sequence obj, or null if not found or dbidentity is used.
	 * @since 2.2
	 */
	public Object getNextSequence(String server_name, String table_name, String column_name);

	/**
	 * Flush cached database data, use with extreme care, its effecting the performance of clients!
	 *
	 * @param server_name
	 * @param table_name
	 * @since 2.2
	 * @deprecated use the {@link #flushAllClientsCache(String, String, String)}
	 */
	@Deprecated
	public void flushAllClientsCache(String server_name, String table_name);

	/**
	 * Flush cached database data. Use with extreme care as this affects the performance of clients! This is a shortcut for notifyDataChange of an entire table
	 *
	 * @param server_name
	 * @param table_name
	 * @param transaction_id The transaction id to use, null if there isn't one or shouldn't be used.
	 * @since 3.5
	 */
	public boolean flushAllClientsCache(String server_name, String table_name, String transaction_id);

	/**
	 * notifyDataChange to clients based on pk(s) data, use with extreme care, its effecting the performance of clients!
	 *
	 * @param server_name
	 * @param table_name
	 * @since 3.5
	 */
	public boolean notifyDataChange(String server_name, String table_name, IDataSet pks, int action, String transaction_id);

	/**
	 * Add performance timing data for an action. This is shown on the server admin page, performance section.
	 * returns a UUID that should be give to the {@link #endPerformanceTiming(UUID)} to end the timing.
	 *
	 * @param context group
	 * @param action
	 * @param time_ms
	 * @since 8.0
	 */
	public UUID addPerformanceTiming(String context, String action, long time_ms, String clientId);

	/**
	 * End the timing that was started with {@link #addPerformanceTiming(String, String, long, String)}
	 *
	 * @param uuid
	 * @since 8.0
	 */
	public void endPerformanceTiming(String context, UUID uuid);

	/**
	 * Add performance timing data for an action. This is shown on the server admin page, performance section.
	 *
	 * @param context group
	 * @param action
	 * @param time_ms
	 * @since 3.1
	 */
	@Deprecated
	public void addPerformanceTiming(String context, String action, long time_ms);

	@Deprecated
	public void addPerformanceTiming(String action, long time_ms);

	/**
	 * Returns an array of IClientInformation instances that correspond to clients that are currently
	 * connected to this server.
	 *
	 * @return An array of IClientInformation instances.
	 *
	 * @since 5.0
	 */
	public IClientInformation[] getConnectedClients();

	/**
	 * Sends a message to all clients currently connected to this server.
	 *
	 * @param message The message that is sent to the clients.
	 *
	 * @since 5.0
	 */
	public void sendMessageToAllClients(String message);

	/**
	 * Sends a message to a specific client connected to this server. The client is identified by its
	 * client ID, which can be retrieved from an IClientInformation instance.
	 *
	 * @param clientId The ID of the client whom the message is sent to.
	 * @param message The message that is sent to the specified client.
	 *
	 * @since 5.0
	 */
	public void sendMessageToClient(String clientId, String message);

	/**
	 * Disconnects all clients that are currently connected to this server.
	 * @param skipClientId
	 *
	 * @since 8.0
	 */
	public void shutDownAllClients(String skipClientId);

	/**
	 * Disconnects all clients that are currently connected to this server.
	 *
	 * @since 5.0
	 *
	 * @deprecated use the {@link #shutDownAllClients(String)}
	 */
	@Deprecated
	public void shutDownAllClients();


	/**
	 * Disconnects a specific client that is currently connected to this server.
	 * The client is identified through its client ID, which can be retrieved from an IClientInformation
	 * instance.
	 *
	 * @param clientId The ID of the client that will be disconnected.
	 *
	 * @since 5.0
	 */
	public void shutDownClient(String clientId);

	/**
	 * Returns true if the server is in maintenance mode, false otherwise. While the server is in
	 * maintenance mode, no new clients can connect to it.
	 *
	 * @return true if the server is in maintenance mode, false otherwise.
	 *
	 * @since 5.0
	 */
	public boolean isInMaintenanceMode();

	/**
	 * Puts the server into maintenance mode, or takes it out of maintenance mode. While in maintenance
	 * mode, no new clients can connect to the server.
	 *
	 * @param inMaintenanceMode If true, then the server is put into maintenance mode. If false, then the
	 * 			server is taken out of maintenance mode.
	 *
	 * @since 5.0
	 */
	public void setInMaintenanceMode(boolean inMaintenanceMode);

	/**
	 * Returns an IServer instance that corresponds to the database server with a specified name.
	 * Two additional boolean flags can be used to make sure that the server is enabled and valid.
	 *
	 * @param serverName The name of the database server to retrieve.
	 * @param mustBeEnabled If true, then the database server must be enabled in order to be returned.
	 * @param mustBeValid If true, then the database server must be valid in order to be returned.
	 *
	 * @return An IServer instance that corresponds to the requested server. If the server with the given
	 * 		name is not found, or if it is found but it does not match the enabled and valid restrictions,
	 * 		then null is returned.
	 *
	 * @since 5.0
	 */
	public IServer getDBServer(String serverName, boolean mustBeEnabled, boolean mustBeValid);

	/**
	 * Returns an array with the names of existing database servers. Two boolean flags can be used
	 * for retrieving only the names of enabled and/or valid servers. The returned list of names can be
	 * sorted on demand, and it is possible to exclude duplicate servers from the list.
	 *
	 * @param mustBeEnabled If set to true, then only names of enabled servers are returned.
	 * @param mustBeValid If set to true, then only names of valid servers are returned.
	 * @param sort If set to true, then the returned list of names is sorted alphabetically.
	 * @param includeDuplicates If set to false, then duplicate servers are not included in the returned list.
	 *
	 * @return An array of Strings holding the names of the servers that satisfy the imposed criteria.
	 *
	 * @since 5.0
	 */
	public String[] getDBServerNames(boolean mustBeEnabled, boolean mustBeValid, boolean sort, boolean includeDuplicates);

	/**
	 * Check if the client runs inside the server (Web Client, Headless Client, ...) as opposed to Smart Client which runs in a jvm on the client.
	 *
	 * @return true if the client runs inside the server.
	 *
	 * @since 5.2
	 */
	public boolean isServerProcess(String clientId);

	/**
	 * Check if the client exists and is authenticated with the server.
	 *
	 * @return true if authenticated
	 *
	 * @since 5.2
	 */
	public boolean isAuthenticated(String clientId);


	/**
	 * Check the password for a username using Servoy built-in user management.
	 *
	 * @return user-UID or null if not valid user/password combination
	 * @throws ServoyException
	 *
	 * @since 5.2
	 */
	public String checkPasswordForUserName(String user, String password) throws ServoyException;

	/**
	 * Returns an array of user groups the user with userUid is member of (using Servoy built-in user management).
	 *
	 * @return String array
	 * @throws ServoyException
	 *
	 * @since 5.2
	 */
	public String[] getUserGroups(String userUid) throws ServoyException;

	/**
	 * @param filename The filename to get an {@link URL} reference to, filename must start with a /
	 */
	public URL getResource(String filename);

	/**
	 * @param filename The filename to get an {@link InputStream} to, filename must start with a /
	 */
	public InputStream getResourceAsStream(String filename);

	/** Executes a stored procedure
	 * @param clientId
	 * @param serverName
	 * @param transaction_id
	 * @param procedureDeclaration
	 * @param questiondata
	 * @param inOutType
	 * @param startRow
	 * @param maxNumberOfRowsToRetrieve
	 *
	 * @return last result set of procedure or result of inOutType
	 *
	 * @since 6.1
	 *
	 */

	public IDataSet executeStoredProcedure(String clientId, String serverName, String transaction_id, String procedureDeclaration, Object[] questiondata,
		int[] inOutType, int startRow, int maxNumberOfRowsToRetrieve) throws ServoyException;

	/** Executes a stored procedure, return all result sets
	 * @param clientId
	 * @param serverName
	 * @param transaction_id
	 * @param procedureDeclaration
	 * @param questiondata
	 * @param startRow
	 * @param maxNumberOfRowsToRetrieve
	 *
	 * @return all result sets of procedure
	 *
	 * @since 7.4
	 */
	public IDataSet[] executeStoredProcedure(String clientId, String serverName, String transaction_id, String procedureDeclaration, Object[] questiondata,
		int startRow, int maxNumberOfRowsToRetrieve) throws ServoyException;
}
