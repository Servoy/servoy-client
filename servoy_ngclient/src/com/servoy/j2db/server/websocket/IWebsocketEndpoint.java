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
	boolean hasSession();

	void closeSession();

	void cancelSession(String reason);

	Object sendMessage(Map<String, ? > data, boolean async) throws IOException;

	void sendMessage(String txt) throws IOException;

	void sendResponse(Object msgId, Object object, boolean success) throws IOException;

	/**
	 * @param serviceName
	 * @param functionName
	 * @param arguments
	 */
	void executeServiceCall(String serviceName, String functionName, Object[] arguments) throws IOException;

	Object executeDirectServiceCall(String serviceName, String functionName, Object[] arguments) throws IOException;
}
