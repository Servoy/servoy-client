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


import java.io.IOException;
import java.util.ArrayList;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import org.sablo.websocket.WebsocketEndpoint;

import com.servoy.j2db.server.ngclient.WebsocketSessionFactory;

/**
 * WebsocketEndpoint for NGClient.
 *
 * @author rgansevles
 *
 */

@ServerEndpoint(value = "/websocket/{sessionid}/{windowid}/{solutionName}/{queryParams}")
public class NGClientEndpoint extends WebsocketEndpoint
{
	public NGClientEndpoint()
	{
		super(WebsocketSessionFactory.CLIENT_ENDPOINT);
	}

	@OnOpen
	public void start(Session newSession, @PathParam("sessionid") String sessionid, @PathParam("windowid")
	final String windowid, @PathParam("solutionName")
	final String solutionName, @PathParam("queryParams")
	final String queryParams) throws Exception
	{
		ArrayList<String> arguments = new ArrayList<String>();
		arguments.add("null".equalsIgnoreCase(solutionName) ? null : solutionName);
		if (queryParams != null && !"null".equalsIgnoreCase(queryParams))
		{
			String[] args = queryParams.split("&");
			for (String arg : args)
			{
				String[] pair = null;
				if ((pair = arg.split("=")).length > 1)
				{
					arguments.add(pair[0] + ":" + pair[1]);
				}
				else
				{
					arguments.add(arg);
				}
			}
		}

		super.start(newSession, sessionid, windowid, arguments.toArray(new String[arguments.size()]));
	}

	@Override
	@OnMessage
	public void incoming(String msg, boolean lastPart)
	{
		super.incoming(msg, lastPart);
	}

	@Override
	@OnClose
	public void onClose()
	{
		super.onClose();
	}

	@OnError
	public void onError(Throwable t)
	{
		if (t instanceof IOException)
		{
			log.error("IOException happened", t.getMessage()); // TODO if it has no message but has a 'cause' it will not print anything useful
		}
		else
		{
			log.error("IOException happened", t);
		}
	}

}
