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

package com.servoy.j2db.server.ngclient.property;

import org.sablo.eventthread.IEventDispatcher;
import org.sablo.websocket.CurrentWindow;
import org.sablo.websocket.IWindow;

import com.servoy.j2db.server.ngclient.endpoint.NGClientEndpoint;

/**
 * @author acostescu
 *
 */
public class TestNGEventDispatcher implements IEventDispatcher
{

	private String valueToBeReturnedFromClient;

	private final NGClientEndpoint endpoint;

	public TestNGEventDispatcher(NGClientEndpoint endpoint)
	{
		this.endpoint = endpoint;
	}

	public void setResponseAwaitedFromClientForId(String valueToBeReturned)
	{
		valueToBeReturnedFromClient = valueToBeReturned;
	}

	@Override
	public void run()
	{
	}

	@Override
	public void suspend(Object object)
	{
		suspend(object, EVENT_LEVEL_DEFAULT, org.sablo.eventthread.EventDispatcher.CONFIGURED_TIMEOUT);

	}

	@Override
	public void resume(Object object)
	{
	}

	@Override
	public boolean isEventDispatchThread()
	{
		return true;
	}

	@Override
	public void destroy()
	{
	}

	@Override
	public void addEvent(Runnable event)
	{
		event.run();
	}

	@Override
	public void addEvent(Runnable event, int eventLevel)
	{
		event.run();
	}

	@Override
	public void suspend(Object suspendID, int minEventLevelToDispatch, long timeout)
	{
		IWindow tmp = CurrentWindow.safeGet();
		try
		{
			endpoint.incoming("{smsgid: '" + String.valueOf(suspendID) + "', ret: " + valueToBeReturnedFromClient + " }", true);
		}
		finally
		{
			CurrentWindow.set(tmp);
		}
	}

	@Override
	public void cancelSuspend(Integer suspendID, String reason)
	{
	}

	@Override
	public void postEvent(Runnable event)
	{
		event.run();
	}

}