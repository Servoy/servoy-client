/*
 * Copyright (C) 2014 Servoy BV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sablo.websocket;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.servoy.j2db.util.Debug;


/**
 * Base class for handling a websocket session.
 * @author rgansevles
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
			Debug.warn("Unknown service called: " + serviceName);
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
				getActiveWebsocketEndpoint().sendResponse(msgId, error == null ? result : error, error == null, getForJsonConverter());
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

	@Override
	public IForJsonConverter getForJsonConverter()
	{
		// by default no conversion, only support basic types
		return null;
	}

}
