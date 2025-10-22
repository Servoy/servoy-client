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

import java.util.List;

import org.sablo.eventthread.Event;
import org.sablo.eventthread.WebsocketSessionWindows;
import org.sablo.websocket.CurrentWindow;

import com.servoy.j2db.server.ngclient.INGApplication;
import com.servoy.j2db.server.ngclient.INGClientWebsocketSession;
import com.servoy.j2db.util.Debug;


/**
 * Event used in {@link NGEventDispatcher} used for {@link INGApplication}
 *
 * @author rgansevles
 *
 */
public class NGEvent extends Event
{
	private final INGApplication client;
	private String suspendedWindowName;
	private String previous;

	public NGEvent(INGApplication client, Runnable runnable, int eventLevel)
	{
		super(client.getWebsocketSession(), runnable, eventLevel);
		this.client = client;
	}

	@Override
	protected INGClientWebsocketSession getSession()
	{
		return (INGClientWebsocketSession)super.getSession();
	}

	@Override
	protected void beforeExecute()
	{
		super.beforeExecute();
		previous = client.getRuntimeWindowManager().getCurrentWindowName();
		client.getRuntimeWindowManager().setCurrentWindowName(String.valueOf(CurrentWindow.get().getNr()));
	}

	@Override
	protected WebsocketSessionWindows createWebsocketSessionWindows()
	{
		return new NGClientWebsocketSessionWindows(getSession());
	}

	@Override
	protected void afterExecute()
	{
		List<Runnable> executeImmediateRunnables = client.getWebsocketSession().getEventDispatcher().getExecuteImmediateRunnablesAndClearList();
		while (executeImmediateRunnables != null && executeImmediateRunnables.size() > 0)
		{
			for (Runnable r : executeImmediateRunnables)
			{
				try
				{
					r.run();
				}
				catch (Exception e)
				{
					exception = e;
					Debug.error("Exception in after execute", e);
				}
			}
			executeImmediateRunnables = client.getWebsocketSession().getEventDispatcher().getExecuteImmediateRunnablesAndClearList();
		}
		client.getRuntimeWindowManager().setCurrentWindowName(previous);
		super.afterExecute();
	}

	@Override
	public void willSuspend()
	{
		suspendedWindowName = client.getRuntimeWindowManager().getCurrentWindowName();
		client.getRuntimeWindowManager().setCurrentWindowName(previous);
		super.willSuspend();
	}

	@Override
	public void willResume()
	{
		super.willResume();
		previous = client.getRuntimeWindowManager().getCurrentWindowName();
		if (client.getRuntimeWindowManager().getWindow(suspendedWindowName) != null)
		{
			client.getRuntimeWindowManager().setCurrentWindowName(suspendedWindowName);
		}
	}
}
