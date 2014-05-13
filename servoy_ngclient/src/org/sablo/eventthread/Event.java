/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

package org.sablo.eventthread;

import org.sablo.websocket.IWebsocketEndpoint;
import org.sablo.websocket.IWebsocketSession;
import org.sablo.websocket.WebsocketEndpoint;

import com.servoy.j2db.util.Debug;


/**
 * The default Event class used by the {@link IEventDispatcher}
 *
 * @author jcompagner
 *
 */
public class Event
{
	private volatile boolean runInBackground;
	private volatile boolean suspended;

	private final Runnable runnable;
	private volatile boolean executed;
	private volatile Exception exception;

	private final IWebsocketEndpoint currentEndpoint;
	private final IWebsocketSession session;


	public Event(IWebsocketSession session, Runnable runnable)
	{
		this.session = session;
		this.runnable = runnable;
		if (WebsocketEndpoint.exists())
		{
			currentEndpoint = WebsocketEndpoint.get();
		}
		else
		{
			// this is a runnable added to the event thread from a none request thread
			currentEndpoint = null;
		}
	}

	/**
	 * Called by the script thread to execute itself.
	 */
	public final void execute()
	{
		IWebsocketEndpoint set = currentEndpoint;
		if (set == null)
		{
			// this was an event from not triggered by a specific endpoint, just relay it to all the endpoints
			// could be changes in the data model that must be pushed to all endpoints, or a close/shutdown/logout
			set = new WebsocketSessionEndpoints(session);

		}
		IWebsocketEndpoint previous = WebsocketEndpoint.set(set);
		try
		{
			beforeExecute();
			if (runnable != null)
			{
				runnable.run();
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
			exception = e;
		}
		finally
		{
			executed = true;
			afterExecute();
			WebsocketEndpoint.set(previous);
		}
	}

	/**
	 *  called right before the runnable gets executed
	 */
	protected void afterExecute()
	{
	}

	/**
	 * called after the runnable is executed (normal or exception result)
	 */
	protected void beforeExecute()
	{
	}

	/**
	 * @return the exception
	 */
	public Exception getException()
	{
		return exception;
	}

	/**
	 * Called when this event will be suspended, will set this event in a suspended state.
	 */
	public void willSuspend()
	{
		suspended = true;
	}

	/**
	 * Called when this event will be resumed will set this event in a resumed state.
	 */
	public void willResume()
	{
		suspended = false;
	}

	/**
	 * @return the executed
	 */
	public boolean isExecuted()
	{
		return executed;
	}

	/**
	 * @return true This will return true if this event is in a suspended state.
	 */
	public boolean isSuspended()
	{
		return suspended;
	}

	/**
	 * Must be called when this event will be executed in the background (the ui will be painted)
	 */
	public void executeInBackground()
	{
		runInBackground = true;
	}

	/**
	 * @return true when {@link #executeInBackground()} was called.
	 */
	public boolean isExecutingInBackground()
	{
		return runInBackground;
	}
}