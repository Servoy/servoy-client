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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.sablo.websocket.IWebsocketSession;

import com.servoy.j2db.util.Debug;

/**
 * Runnable of the ScriptThread that executes {@link Event} objects.
 *
 * @author rgansevles
 *
 */
public class EventDispatcher<E extends Event> implements Runnable, IEventDispatcher<E>
{
	private final ConcurrentMap<Object, Event> suspendedEvents = new ConcurrentHashMap<Object, Event>();

	private final List<Event> events = new ArrayList<Event>();
	private final LinkedList<Event> stack = new LinkedList<Event>();

	private volatile boolean exit = false;

	private volatile Thread scriptThread = null;

	private final IWebsocketSession session;

	public EventDispatcher(IWebsocketSession session)
	{
		this.session = session;
	}

	public void run()
	{
		scriptThread = Thread.currentThread();
		while (!exit)
		{
			dispatch();
		}
	}

	private void dispatch()
	{
		try
		{
			Event event = null;
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
		catch (Throwable t)
		{
			Debug.error(t);
		}
	}

	@Override
	public boolean isEventDispatchThread()
	{
		return scriptThread == Thread.currentThread();
	}

	/**
	 * @param event
	 */
	public void addEvent(Event event)
	{
		if (isEventDispatchThread())
		{
			event.execute();
		}
		else
		{
			synchronized (events)
			{
				events.add(event);
				events.notifyAll();
				// non-blocking
//				while (!(event.isExecuted() || event.isSuspended() || event.isExecutingInBackground()))
//				{
//					try
//					{
//						events.wait();
//					}
//					catch (InterruptedException e)
//					{
//						Debug.error(e);
//					}
//				}
			}
		}
	}

	/**
	 * @param object
	 */
	public void suspend(Object object)
	{
		// TODO should this one be called in the execute event thread, should an check be done??
		if (!isEventDispatchThread())
		{
			Debug.error("suspend called in another thread then the script thread: " + Thread.currentThread(), new RuntimeException());
			return;
		}
		Event event = stack.getLast();
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
		suspendedEvents.remove(object);
	}

	private void addEmptyEvent()
	{
		synchronized (events)
		{
			// add a nop event so that the dispatcher is triggered.
			events.add(new Event(session, null));
			events.notifyAll();
		}
	}

	public void destroy()
	{
		exit = true;
		addEmptyEvent();
	}
}