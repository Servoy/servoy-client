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

package com.servoy.j2db.server.ngclient.eventthread;

import org.sablo.eventthread.Event;
import org.sablo.eventthread.EventDispatcher;
import org.sablo.websocket.CurrentWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.server.ngclient.INGApplication;
import com.servoy.j2db.server.ngclient.NGClientWebsocketSession;

/**
 * Runnable of the ScriptThread that executes {@link Event} objects.
 *
 * @author rgansevles
 *
 */
public class NGEventDispatcher extends EventDispatcher
{
	protected static final Logger SHUTDOWNLOGGER = LoggerFactory.getLogger("SHUTDOWNLOGGER"); //$NON-NLS-1$

	private final INGApplication client;

	public NGEventDispatcher(INGApplication client)
	{
		super(client.getWebsocketSession());
		this.client = client;
	}

	@Override
	protected Event createEvent(Runnable event, int eventLevel)
	{
		return new NGEvent(client, event, eventLevel);
	}

	@Override
	public void run()
	{
		J2DBGlobals.setServiceProvider(client);
		try
		{
			super.run();
		}
		finally
		{
			J2DBGlobals.setServiceProvider(null);

			SHUTDOWNLOGGER.debug("Event dispatcher terminted for client: " + client.getWebsocketSession().getSessionKey()); //$NON-NLS-1$

			if (!client.isShutDown())
			{
				SHUTDOWNLOGGER.debug("Client was not shutdown, calling it now: " + client.getWebsocketSession().getSessionKey()); //$NON-NLS-1$
				client.shutDown(true);
			}
		}
	}

	@Override
	protected void handleException(Event event, Throwable t)
	{
		super.handleException(event, t);
		boolean restoreWindow = false;
		if (CurrentWindow.safeGet() == null && event != null)
		{
			restoreWindow = true;
			CurrentWindow.set(event.getWindow());
		}
		try
		{
			NGClientWebsocketSession.sendInternalError(t);
		}
		finally
		{
			if (restoreWindow) CurrentWindow.set(null);
		}
	}

}