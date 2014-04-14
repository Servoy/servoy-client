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

import com.servoy.j2db.server.headlessclient.eventthread.Event;
import com.servoy.j2db.server.ngclient.INGApplication;


/**
 * Event used in {@link EventDispatcher} used for {@link INGApplication}
 * 
 * @author rgansevles
 *
 */
public class NGEvent extends Event
{
	private final INGApplication client;
	private final String currentWindowName;
	private String previous;

	public NGEvent(INGApplication client, Runnable runnable)
	{
		super(runnable);
		this.client = client;
		// if this is the event dispatch thread where this event is made, then just use the current set window name
		if (client.isEventDispatchThread())
		{
			currentWindowName = client.getRuntimeWindowManager().getCurrentWindowName();
		}
		else
		{
			// else take it from the current active endpoint
			currentWindowName = client.getWebsocketSession().getCurrentWindowName();
		}
	}

	@Override
	public void execute()
	{
		client.getWebsocketSession().startHandlingEvent();
		try
		{
			previous = client.getRuntimeWindowManager().getCurrentWindowName();
			client.getRuntimeWindowManager().setCurrentWindowName(currentWindowName);
			try
			{
				super.execute();
			}
			finally
			{
				client.getRuntimeWindowManager().setCurrentWindowName(previous);
			}
		}
		finally
		{
			client.getWebsocketSession().stopHandlingEvent();
		}
	}

	@Override
	public void willSuspend()
	{
		super.willSuspend();
		client.getRuntimeWindowManager().setCurrentWindowName(previous);
		client.getWebsocketSession().stopHandlingEvent();

	}

	@Override
	public void willResume()
	{
		super.willResume();
		client.getWebsocketSession().startHandlingEvent();
		client.getRuntimeWindowManager().setCurrentWindowName(currentWindowName);
	}

}
