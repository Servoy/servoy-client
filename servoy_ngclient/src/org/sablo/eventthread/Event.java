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