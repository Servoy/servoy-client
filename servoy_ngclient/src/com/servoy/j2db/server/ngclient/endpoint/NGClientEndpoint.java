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

package com.servoy.j2db.server.ngclient.endpoint;


import javax.servlet.http.HttpSession;
import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import com.servoy.j2db.server.ngclient.GetHttpSessionConfigurator;
import com.servoy.j2db.server.ngclient.WebsocketSessionFactory;

/**
 * WebsocketEndpoint for NGClient.
 *
 * @author rgansevles
 *
 */

@ServerEndpoint(value = "/websocket/{sessionid}/{windowname}/{windowid}", configurator = GetHttpSessionConfigurator.class)
public class NGClientEndpoint extends BaseNGClientEndpoint
{
	public NGClientEndpoint()
	{
		super(WebsocketSessionFactory.CLIENT_ENDPOINT);
	}

	@OnOpen
	public void start(Session newSession, @PathParam("sessionid") String sessionid, @PathParam("windowname") String windowname,
		@PathParam("windowid") String windowid, EndpointConfig config) throws Exception
	{
		super.start(newSession, sessionid, windowname, windowid);
		HttpSession httpSession = (HttpSession)config.getUserProperties().get(HttpSession.class.getName());
		if (httpSession != null)
		{
			newSession.getUserProperties().put(HttpSession.class.getName(), httpSession);
		}
	}

	@Override
	@OnMessage
	public void incoming(String msg, boolean lastPart)
	{
		super.incoming(msg, lastPart);
	}

	@Override
	@OnClose
	public void onClose(CloseReason closeReason)
	{
		super.onClose(closeReason);
	}

	@Override
	@OnError
	public void onError(Throwable t)
	{
		super.onError(t);
	}

}
