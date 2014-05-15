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

package org.sablo.eventthread;

import java.io.IOException;
import java.util.Map;

import org.sablo.websocket.IForJsonConverter;
import org.sablo.websocket.IWebsocketEndpoint;
import org.sablo.websocket.IWebsocketSession;

/**
 * A {@link IWebsocketEndpoint} implementation that redirects all the calls on it to the current registered,
 * {@link IWebsocketSession#getRegisteredEnpoints()}, endpoints.
 *
 * @author jcompagner
 *
 */
public class WebsocketSessionEndpoints implements IWebsocketEndpoint
{
	private final IWebsocketSession session;

	/**
	 * @param session
	 */
	public WebsocketSessionEndpoints(IWebsocketSession session)
	{
		this.session = session;
	}

	@Override
	public boolean hasSession()
	{
		return true;
	}

	@Override
	public void closeSession()
	{
		for (IWebsocketEndpoint endpoint : session.getRegisteredEnpoints())
		{
			endpoint.closeSession();
		}
	}

	@Override
	public void cancelSession(String reason)
	{
		for (IWebsocketEndpoint endpoint : session.getRegisteredEnpoints())
		{
			endpoint.cancelSession(reason);
		}
	}

	@Override
	public Object sendMessage(Map<String, ? > data, boolean async, IForJsonConverter forJsonConverter) throws IOException
	{
		// TODO should this throw an illegal call exception? Because this kind of call shouildn't be used in this class?
		// returns the first none null value.
		Object retValue = null;
		for (IWebsocketEndpoint endpoint : session.getRegisteredEnpoints())
		{
			Object reply = endpoint.sendMessage(data, async, forJsonConverter);
			retValue = retValue == null ? reply : retValue;
		}
		return retValue;
	}

	@Override
	public void sendMessage(String txt) throws IOException
	{
		for (IWebsocketEndpoint endpoint : session.getRegisteredEnpoints())
		{
			endpoint.sendMessage(txt);
		}
	}

	@Override
	public void sendResponse(Object msgId, Object object, boolean success, IForJsonConverter forJsonConverter) throws IOException
	{
		for (IWebsocketEndpoint endpoint : session.getRegisteredEnpoints())
		{
			endpoint.sendResponse(msgId, object, success, forJsonConverter);
		}
	}

	@Override
	public void executeAsyncServiceCall(String serviceName, String functionName, Object[] arguments)
	{
		for (IWebsocketEndpoint endpoint : session.getRegisteredEnpoints())
		{
			endpoint.executeAsyncServiceCall(serviceName, functionName, arguments);
		}

	}

	@Override
	public Object executeServiceCall(String serviceName, String functionName, Object[] arguments) throws IOException
	{
		// TODO should this throw an illegal call exception? Because this kind of call shouildn't be used in this class?
		// returns the first none null value.
		Object retValue = null;
		for (IWebsocketEndpoint endpoint : session.getRegisteredEnpoints())
		{
			Object reply = endpoint.executeServiceCall(serviceName, functionName, arguments);
			retValue = retValue == null ? reply : retValue;
		}
		return retValue;
	}

}
