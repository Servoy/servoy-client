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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Websocket user session management
 * @author jblok, rgansevles
 */
public class WebsocketSessionManager
{
	private static IWebsocketSessionFactory websocketSessionFactory;

	//maps form uuid to session
	private static Map<String, IWebsocketSession> wsSessions = new HashMap<>();

	private static Map<String, Long> nonActiveWsSessions = new HashMap<>();

	private static final long SESSION_TIMEOUT = 1 * 60 * 1000;

	private static String getSessionKey(String endpointType, String uuid)
	{
		return endpointType + ':' + uuid;
	}

	public static void addWebSocketSession(String endpointType, String uuid, IWebsocketSession wsSession)
	{
		synchronized (wsSessions)
		{
			wsSessions.put(getSessionKey(endpointType, uuid), wsSession);
			wsSession.setUuid(uuid);
		}
	}

	public static IWebsocketSession removeWebSocketSession(String endpointType, String uuid)
	{
		synchronized (wsSessions)
		{
			String key = getSessionKey(endpointType, uuid);
			nonActiveWsSessions.remove(key);
			return wsSessions.remove(key);
		}
	}

	public static IWebsocketSession getSession(String endpointType, String prevUuid)
	{
		return getOrCreateSession(endpointType, prevUuid, false);
	}

	public static IWebsocketSession getOrCreateSession(String endpointType, String prevUuid, boolean create)
	{
		String uuid = prevUuid;
		IWebsocketSession wsSession = null;
		synchronized (wsSessions)
		{
			String key;
			if (uuid != null && uuid.length() > 0)
			{
				key = getSessionKey(endpointType, uuid);
				wsSession = wsSessions.get(key);
				nonActiveWsSessions.remove(key);
			}
			else
			{
				uuid = UUID.randomUUID().toString();
				key = getSessionKey(endpointType, uuid);
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
				nonActiveWsSessions.put(getSessionKey(endpointType, uuid), new Long(currentTime));
			}
		}
	}

	public static void setWebsocketSessionFactory(IWebsocketSessionFactory factory)
	{
		websocketSessionFactory = factory;
	}
}
