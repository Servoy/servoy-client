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
import java.util.Map;


/**
 * The websocket endpoint interface.
 * @author rgansevles
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
	 * @param forJsonConverter
	 * @return remote response (when not async)
	 * @throws IOException
	 */
	Object sendMessage(Map<String, ? > data, boolean async, IForJsonConverter forJsonConverter) throws IOException;

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
	void sendResponse(Object msgId, Object object, boolean success, IForJsonConverter forJsonConverter) throws IOException;

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
