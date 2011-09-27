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

package com.servoy.j2db.server.headlessclient;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.servoy.j2db.util.Debug;

/**
 * Runnable of the ScriptThread that executes {@link IExecuteEvent} objects.
 * 
 * @author jcompagner
 * 
 * @since 6.1
 */
public final class ScriptExecutor implements Runnable
{
	private final ConcurrentMap<Object, IExecuteEvent> suspendedEvents = new ConcurrentHashMap<Object, IExecuteEvent>();

	private final List<IExecuteEvent> events = new ArrayList<IExecuteEvent>();
	private final LinkedList<IExecuteEvent> stack = new LinkedList<IExecuteEvent>();

	private volatile boolean exit = false;

	public void run()
	{
		while (!exit)
		{
			dispatch();
		}
	}

	/**
	 * 
	 */
	private void dispatch()
	{
		try
		{
			IExecuteEvent event = null;
			synchronized (events)
			{
				while (event == null)
				{
					if (events.size() > 0)
					{
						event = events.remove(0);
					}
					else
					{
						events.wait();
					}
				}
			}
			stack.add(event);
			event.execute();
			if (stack.getLast() != event)
			{
				throw new Exception("State not expected");
			}
			stack.remove(event);
			synchronized (events)
			{
				events.notifyAll();
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
	}

	/**
	 * @param event
	 */
	public void addEvent(FunctionEvent event)
	{
		synchronized (events)
		{
			events.add(event);
			events.notifyAll();
			while (!event.isExecuted())
			{
				try
				{
					events.wait();
				}
				catch (InterruptedException e)
				{
					Debug.error(e);
				}
			}
		}
	}

	/**
	 * @param object
	 */
	public void suspend(Object object)
	{
		// TODO should this one be called in the execute event thread, should an check be done??

		IExecuteEvent event = stack.getLast();
		if (event != null)
		{
			suspendedEvents.put(object, event);
			event.willSuspend();
			synchronized (events)
			{
				events.notifyAll();

			}
			while (suspendedEvents.containsKey(object) && !exit)
			{
				dispatch();
			}
			event.willResume();
		}
	}

	public void resume(Object object)
	{
		IExecuteEvent event = suspendedEvents.remove(object);
		if (event != null)
		{
			addEmptyEvent();
		}

	}

	/**
	 * 
	 */
	private void addEmptyEvent()
	{
		synchronized (events)
		{
			// add a nop event so that the dispatcher is triggered.
			events.add(new IExecuteEvent()
			{
				public void willSuspend()
				{
				}

				public void willResume()
				{
				}

				public void execute()
				{
				}
			});
			events.notifyAll();
		}
	}

	/**
	 * 
	 */
	public void destroy()
	{
		exit = true;
		addEmptyEvent();
	}
}