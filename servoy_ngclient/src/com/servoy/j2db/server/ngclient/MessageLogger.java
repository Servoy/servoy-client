/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2018 Servoy BV

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

package com.servoy.j2db.server.ngclient;

import javax.websocket.Session;

import org.sablo.websocket.IMessageLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.servoy.j2db.dataprocessing.ClientInfo;

/**
 * @author jcompagner
 *
 */
@SuppressWarnings("nls")
public class MessageLogger implements IMessageLogger
{
	public static final Logger messageLogger = LoggerFactory.getLogger("com.servoy.j2db.server.ngclient.MessageLogger");
	public static boolean doLog = messageLogger.isDebugEnabled();

	private final INGClientWebsocketSession session;
	private final int windowNr;

	/**
	 * @param window
	 */
	public MessageLogger(INGClientWebsocketSession session, int windowNr)
	{
		this.session = session;
		this.windowNr = windowNr;
	}

	private void logMessage(String message)
	{
		StringBuilder sb = new StringBuilder(message.length() + 50);
		sb.append(session.getSessionKey()).append('|').append(windowNr).append('|');
		INGApplication client = session.getClient();
		if (client != null)
		{
			sb.append(client.getClientID());
			sb.append('|');
			ClientInfo clientInfo = client.getClientInfo();
			if (clientInfo != null)
			{
				sb.append(clientInfo.getUserName() != null ? clientInfo.getUserName() : "<no-user>");
			}
			else
			{
				sb.append("<no-client-info>");
			}
		}
		else
		{
			sb.append("<no-client>|<no-user>");
		}
		sb.append('|');
		sb.append(message);
		// it logs to error so it will always log when this line is hit. (but with the logMessages boolea this can be toggled)
		messageLogger.error(sb.toString());
	}

	@Override
	public void messageSend(String message)
	{
		logMessage("S|" + (message.length() > 400 ? message.substring(0, 400) : message));
	}

	@Override
	public void messageReceived(String message)
	{
		logMessage("R|" + message);
	}

	@Override
	public void endPointStarted(Session websocketSession)
	{
		logMessage("Websocket started with parameters: " + websocketSession.getRequestParameterMap());
	}

	@Override
	public void endPointClosed()
	{
		logMessage("Websocket closed");

	}
}
