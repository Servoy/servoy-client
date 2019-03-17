/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2016 Servoy BV

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

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.server.ServerEndpoint;

import com.servoy.j2db.server.ngclient.IMessagesRecorder;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;

/**
 * @author jcompagner
 *
 */
// RAGTEST testen??
@ServerEndpoint(value = "/recording/websocket/{clientnr}/{windowname}/{windowid}")
public class RecordingEndpoint extends NGClientEndpoint
{
	private final StringBuilder incomingPartialMessage = new StringBuilder();
	private final IMessagesRecorder recorder;
	private String sessionKeyString;

	/**
	 * @param recorder
	 * @param endpoint
	 */
	public RecordingEndpoint()
	{
		recorder = ApplicationServerRegistry.get().getService(IMessagesRecorder.class);
	}


	@Override
	@OnMessage
	public void incoming(String msg, boolean lastPart)
	{
		super.incoming(msg, lastPart);
		String message = msg;
		if (!lastPart)
		{
			incomingPartialMessage.append(message);
			return;
		}
		if (incomingPartialMessage.length() > 0)
		{
			incomingPartialMessage.append(message);
			message = incomingPartialMessage.toString();
			incomingPartialMessage.setLength(0);
		}
		if (!"P".equals(message)) recorder.addMessage(getSessionKeyString(), '>' + message);
	}


	/**
	 * @return
	 */
	private String getSessionKeyString()
	{
		if (sessionKeyString == null)
		{
			sessionKeyString = getWindow().getSession().getSessionKey().toString();
		}
		return sessionKeyString;
	}

	@Override
	public synchronized void sendText(String message) throws IOException
	{
		super.sendText(message);
		if (!"p".equals(message)) recorder.addMessage(getSessionKeyString(), message);
	}

	@Override
	@OnClose
	public void onClose(CloseReason closeReason)
	{
		// TODO should we support refresh of the browser in recording mode?
		// then we can only clear it when the session is really disposed..
		recorder.clear(getSessionKeyString());
		super.onClose(closeReason);
	}

	@Override
	@OnError
	public void onError(Throwable t)
	{
		super.onError(t);
		recorder.clear(getSessionKeyString());
	}
}
