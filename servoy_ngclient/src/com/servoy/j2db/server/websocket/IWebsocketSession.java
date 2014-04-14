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

import org.json.JSONObject;

/**
 * Interface for classes handling a websocket session.
 * 
 * @author rgansevles
 *
 */
public interface IWebsocketSession
{
	/**
	 * Can it still be used?
	 */
	boolean isValid();

	/**
	 * Rebind this websocket session to a new endpoint (browser reconnect)
	 */
	void setActiveWebsocketEndpoint(IWebsocketEndpoint websocketEndpoint);

	/**
	 * Called when a new connection is started (also on reconnect)
	 * @param uuid
	 * @param argument argument from browser url
	 */
	void onOpen(String uuid, String argument);

	/**
	 * Request to close the websocket session.
	 */
	void closeSession();

	/**
	 * Handle an incoming message.
	 * @param obj
	 */
	void handleMessage(JSONObject obj);

	/**
	 * Register a handler for a named service.
	 * @param name
	 * @param service handler
	 */
	void registerService(String name, IService service);

	/**
	 * Call a named service from the browser.
	 * @param serviceName
	 * @param methodName
	 * @param args
	 * @param msgId
	 */
	void callService(String serviceName, String methodName, JSONObject args, Object msgId);

}
