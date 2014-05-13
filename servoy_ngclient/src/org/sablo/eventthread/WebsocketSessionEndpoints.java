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

package org.sablo.eventthread;

import java.io.IOException;
import java.util.Map;

import org.sablo.websocket.IForJsonConverter;
import org.sablo.websocket.IWebsocketEndpoint;
import org.sablo.websocket.IWebsocketSession;

/**
 * @author Johan
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
