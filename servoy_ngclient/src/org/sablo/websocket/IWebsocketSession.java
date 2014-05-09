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

import org.json.JSONObject;

/**
 * Interface for classes handling a websocket session.
 * @author rgansevles
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
	 * @param argument
	 */
	void onOpen(String argument);

	/**
	 * Set the uuid
	 * @param uuid
	 */
	void setUuid(String uuid);

	String getUuid();

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
