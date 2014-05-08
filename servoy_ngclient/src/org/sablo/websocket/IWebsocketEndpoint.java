/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

import java.io.IOException;
import java.util.Map;


/**
 * The websocket endpoint interface.
 * 
 * @author rgansevles
 *
 */
public interface IWebsocketEndpoint
{
	/**
	 * It there an active session to the browser?
	 */
	boolean hasSession();

	/**
	 * Close the browser session.
	 */
	void closeSession();

	/**
	 * Close the browser session with a cancel reason.
	 */
	void cancelSession(String reason);

	/**
	 * Send a message to the browser, add conversion
	 * @param data
	 * @param async when false, wait for response
	 * @return remote response (when not async)
	 * @throws IOException
	 */
	Object sendMessage(Map<String, ? > data, boolean async) throws IOException;

	/**
	 * Just send this text as message, no conversion, no waiting for response.
	 * @param txt
	 * @throws IOException
	 */
	void sendMessage(String txt) throws IOException;

	/**
	 * Send a response for a previous request.
	 * @see IWebsocketSession#callService(String, String, org.json.JSONObject, Object).
	 * 
	 * @param msgId id of previous request
	 * @param object value to respond
	 * @param success is this a normal or an error response?
	 * @throws IOException
	 */
	void sendResponse(Object msgId, Object object, boolean success) throws IOException;

	/** Execute a service call asynchronously.
	 * 
	 * @param serviceName
	 * @param functionName
	 * @param arguments
	 */
	void executeAsyncServiceCall(String serviceName, String functionName, Object[] arguments);

	/** Execute a service call synchronously.
	 * 
	 * @param serviceName
	 * @param functionName
	 * @param arguments
	 * @return remote result
	 * @throws IOException
	 */
	Object executeServiceCall(String serviceName, String functionName, Object[] arguments) throws IOException;
}
