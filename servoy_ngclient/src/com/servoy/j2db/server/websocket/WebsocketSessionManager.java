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

package com.servoy.j2db.server.websocket;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Websocket session management
 * @author jblok, rgansevles
 */
public class WebsocketSessionManager
{
	private static IWebsocketSessionFactory websocketSessionFactory;

	//maps form uuid to session
	private static Map<String, IWebsocketSession> wsSessions = new HashMap<>();

	private static Map<String, Long> nonActiveWsSessions = new HashMap<>();

	private static final long SESSION_TIMEOUT = 1 * 60 * 1000;

	private static String getWsSessionKey(String endpointType, String uuid)
	{
		return endpointType + ':' + uuid;
	}

	public static void addWebSocketSession(String endpointType, String uuid, IWebsocketSession wsSession)
	{
		synchronized (wsSessions)
		{
			wsSessions.put(getWsSessionKey(endpointType, uuid), wsSession);
			wsSession.setUuid(uuid);
		}
	}

	public static IWebsocketSession removeWebSocketSession(String endpointType, String uuid)
	{
		synchronized (wsSessions)
		{
			String key = getWsSessionKey(endpointType, uuid);
			nonActiveWsSessions.remove(key);
			return wsSessions.remove(key);
		}
	}

	public static IWebsocketSession getWsSession(String endpointType, String prevUuid)
	{
		return getOrCreateWsSession(endpointType, prevUuid, false);
	}

	public static IWebsocketSession getOrCreateWsSession(String endpointType, String prevUuid, boolean create)
	{
		String uuid = prevUuid;
		IWebsocketSession wsSession = null;
		synchronized (wsSessions)
		{
			String key;
			if (uuid != null && uuid.length() > 0)
			{
				key = getWsSessionKey(endpointType, uuid);
				wsSession = wsSessions.get(key);
				nonActiveWsSessions.remove(key);
			}
			else
			{
				uuid = UUID.randomUUID().toString();
				key = getWsSessionKey(endpointType, uuid);
			}
			if (wsSession == null || !wsSession.isValid())
			{
				wsSessions.remove(key);
				wsSession = null;
				if (create && websocketSessionFactory != null)
				{
					wsSession = websocketSessionFactory.createSession(endpointType);
				}
				if (wsSession != null)
				{
					wsSessions.put(key, wsSession);
					wsSession.setUuid(uuid);
				}
			}
		}
		return wsSession;
	}

	/**
	 * @param endpointType
	 * @param uuid
	 */
	public static void close(String endpointType, String uuid)
	{
		synchronized (wsSessions)
		{
			long currentTime = System.currentTimeMillis();
			Iterator<Long> iterator = nonActiveWsSessions.values().iterator();
			while (iterator.hasNext())
			{
				Long entry = iterator.next();
				if (currentTime - entry.longValue() > SESSION_TIMEOUT)
				{
					iterator.remove();
				}
			}
			if (uuid != null)
			{
				nonActiveWsSessions.put(getWsSessionKey(endpointType, uuid), new Long(currentTime));
			}
		}
	}

	public static void setWebsocketSessionFactory(IWebsocketSessionFactory factory)
	{
		websocketSessionFactory = factory;
	}
}
