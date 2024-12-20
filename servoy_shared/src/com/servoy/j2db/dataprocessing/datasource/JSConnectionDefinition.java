/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2021 Servoy BV

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

package com.servoy.j2db.dataprocessing.datasource;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.scripting.IJavaScriptType;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IDestroyable;

/**
 * <p>
 * The <code>JSConnectionDefinition</code> provides a mechanism for creating and managing client-specific
 * database server connections at runtime in the Servoy environment. It is accessed through
 * <code>datasources.db.myserver.defineClientConnection()</code> and allows configuration of database
 * connections tailored to the current client session.
 * </p>
 *
 * <p>
 * This class enables setting properties such as <code>username</code> and <code>password</code> for authentication
 * and allows additional connection properties to be defined as key-value pairs. These configurations can be
 * dynamically registered to a server, ensuring that all subsequent connections to the database for that client
 * use the specified settings.
 * </p>
 *
 * <p>
 * Key functionalities include the ability to retrieve or modify connection details like the username, password,
 * and custom properties. The connection definition can be registered with the server using the <code>create</code>
 * method, or unregistered and destroyed with the <code>destroy</code> method, ensuring flexibility in managing
 * runtime database interactions.
 * </p>
 *
 * @author jcompagner
 * @since 2021.06
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class JSConnectionDefinition implements IJavaScriptType, IDestroyable, Serializable
{
	private final IServer server;
	private final Map<String, String> properties = new HashMap<>();
	private final String clientId;
	private String _username;
	private String _password;

	/**
	 * @param server
	 * @param clientId
	 */
	public JSConnectionDefinition(IServer server, String clientId)
	{
		this.server = server;
		this.clientId = clientId;
	}

	/**
	 * Sets the username to use for this client connection.
	 *
	 * @param username
	 *
	 * @return this
	 */
	@JSFunction
	public JSConnectionDefinition username(String username)
	{
		this._username = username;
		return this;
	}

	/**
	 * returns the username that was set by username(string)
	 *
	 * @return the username for this connection.
	 */
	@JSFunction
	public String username()
	{
		return _username;
	}

	/**
	 * Sets the password to use for this client connection.
	 *
	 * @param password
	 *
	 * @return this
	 */
	@JSFunction
	public JSConnectionDefinition password(String password)
	{
		this._password = password;
		return this;
	}

	/**
	 * returns the password that was set by password(string)
	 *
	 * @return the password for this connection.
	 */
	@JSFunction
	public String password()
	{
		return _password;
	}

	/**
	 * Set a key value pair that is used as a connection property for this connection definition
	 *
	 * @param key The propertie key
	 * @param value The property value
	 *
	 * @return this
	 */
	@JSFunction
	public JSConnectionDefinition setProperty(String key, String value)
	{
		properties.put(key, value);
		return this;
	}

	/**
	 * Returns  value for the given key that was set by setProperty(key,value)
	 *
	 * @param key
	 *
	 * @return the value for the key (null if no value was found for the given key)
	 *
	 */
	public String getProperty(String key)
	{
		return properties.get(key);
	}

	/**
	 *  Registers this JSConnectionDefinition to the server with the current configuration.
	 *  After this call all connections to that database will use the configuration of this definition.
	 *
	 * @return The this if it could be created, this will return null if there was a creating this definition (check logs)
	 */
	@JSFunction
	public JSConnectionDefinition create()
	{
		try
		{
			if (server.createClientDatasource(this))
				return this;
		}
		catch (RemoteException e)
		{
			Debug.error("Can't creating JSConncetionDefinition for server " + server, e); //$NON-NLS-1$
		}
		return null;
	}

	/**
	 * Destoyes this JSConnectonDefintion, this unregisteres this on the server so it will not use this configuration anymore to create connections.
	 *
	 */
	@Override
	@JSFunction
	public void destroy()
	{
		try
		{
			server.dropClientDatasource(clientId);
		}
		catch (RemoteException e)
		{
			Debug.error("Error destroying JSConncetionDefinition for server " + server, e); //$NON-NLS-1$
		}
	}

	/**
	 * @return the clientId
	 */
	public String getClientId()
	{
		return clientId;
	}

	/**
	 * @return
	 */
	public Map<String, String> getProperties()
	{
		return Collections.unmodifiableMap(properties);
	}
}
