/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package org.sablo.websocket;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.servoy.j2db.util.Debug;


/**
 * Base class for handling a websocket session.
 * 
 * @author rgansevles
 *
 */
public abstract class BaseWebsocketSession implements IWebsocketSession
{
	private IWebsocketEndpoint wsEndpoint;

	private final Map<String, IService> services = new HashMap<>();

	private String uuid;

	public BaseWebsocketSession()
	{
	}

	@Override
	public void setActiveWebsocketEndpoint(IWebsocketEndpoint wsEndpoint)
	{
		this.wsEndpoint = wsEndpoint;
	}

	public IWebsocketEndpoint getActiveWebsocketEndpoint()
	{
		return wsEndpoint;
	}

	public void onOpen(String argument)
	{
	}

	@Override
	public void closeSession()
	{
		getActiveWebsocketEndpoint().closeSession();
	}

	/**
	 * @return the uuid
	 */
	public String getUuid()
	{
		return uuid;
	}

	/**
	 * @param uuid the uuid to set
	 */
	public void setUuid(String uuid)
	{
		this.uuid = uuid;
	}

	public void registerService(String name, IService service)
	{
		services.put(name, service);
	}

	public IService getService(String name)
	{
		return services.get(name);
	}

	public void callService(String serviceName, final String methodName, final JSONObject args, final Object msgId)
	{
		final IService service = getService(serviceName);
		if (service != null)
		{
			doCallService(service, methodName, args, msgId);
		}
		else
		{
			Debug.warn("Unknown servie called: " + serviceName);
		}
	}

	/**
	 * @param service
	 * @param methodName
	 * @param args
	 * @param msgId
	 */
	protected void doCallService(IService service, String methodName, JSONObject args, Object msgId)
	{
		Object result = null;
		String error = null;
		try
		{
			result = service.executeMethod(methodName, args);
		}
		catch (Exception e)
		{
			Debug.error(e);
			error = "Error: " + e.getMessage();
		}

		if (msgId != null) // client wants response
		{
			try
			{
				getActiveWebsocketEndpoint().sendResponse(msgId, error == null ? result : error, error == null);
			}
			catch (IOException e)
			{
				Debug.error(e);
			}
		}
	}

	@Override
	public Object executeServiceCall(String serviceName, String functionName, Object[] arguments) throws IOException
	{
		return getActiveWebsocketEndpoint().executeServiceCall(serviceName, functionName, arguments);
	}

	@Override
	public void executeAsyncServiceCall(String serviceName, String functionName, Object[] arguments)
	{
		getActiveWebsocketEndpoint().executeAsyncServiceCall(serviceName, functionName, arguments);
	}

}
