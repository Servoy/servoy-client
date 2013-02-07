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

package com.servoy.j2db.server.headlessclient.eventthread;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.Application;
import org.apache.wicket.IClusterable;
import org.apache.wicket.Page;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.Session;
import org.mozilla.javascript.Function;

import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.server.headlessclient.ServoyRequestCycle;
import com.servoy.j2db.server.headlessclient.WebClient;
import com.servoy.j2db.util.Debug;

/**
 * An implementation of {@link Event} that executes {@link Function} when {@link #execute()} is called.
 * Will set and reset all the wicket thread locals from the creation thread (the http thread) to the execution thread.
 * 
 * @author jcompagner
 * 
 * @since 6.1
 */
final class WicketEvent extends Event
{
	private final RequestCycle requestCycle;
	private final Session session;
	private final Application application;

	private volatile boolean executed;
	private volatile boolean wasSuspended;
	private volatile Exception exception;

	private final List<IClusterable> dirtyObjectsList;
	private final List<Page> touchedPages;
	private volatile Thread httpThread;
	private final Runnable runable;
	private final IServiceProvider serviceProvider;
	private volatile List<Runnable> events;
	private final WebClient client;

	/**
	 * @param f
	 * @param scope
	 * @param thisObject
	 * @param args
	 * @param focusEvent
	 * @param throwException
	 * @param scriptEngine TODO
	 */
	public WicketEvent(WebClient client, Runnable runable)
	{
		this.client = client;
		this.runable = runable;
		requestCycle = RequestCycle.get();
		session = Session.get();
		serviceProvider = J2DBGlobals.getServiceProvider();
		application = Application.get();
		dirtyObjectsList = session.getDirtyObjectsList();
		touchedPages = session.getTouchedPages();
		httpThread = Thread.currentThread();
	}


	/**
	 * @param last
	 */
	public void updateHttpThread(Event last)
	{
		if (last instanceof WicketEvent)
		{
			httpThread = ((WicketEvent)last).httpThread;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.server.headlessclient.IExecuteEvent#execute()
	 */
	@Override
	public final void execute()
	{
		try
		{
			ServoyRequestCycle.set(requestCycle);
			Session.set(session);
			Application.set(application);
			J2DBGlobals.setServiceProvider(serviceProvider);

			session.moveUsedPage(httpThread, Thread.currentThread());

			runable.run();
		}
		catch (Exception e)
		{
			Debug.error(e);
			exception = e;
		}
		finally
		{
			// store the current request events of the client that are 
			// created in this thread on this event object. (see addEvent)
			// but only if it was never suspended. if it was suspended then on this event no http thread is waiting any more on.
			if (!wasSuspended)
			{
				setEvents(client.getRequestEvents());
			}
			executed = true;
			cleanup();
		}
	}

	/**
	 * 
	 */
	private void cleanup()
	{
		List<IClusterable> lst = session.getDirtyObjectsList();
		for (IClusterable dirtyObject : lst)
		{
			if (!dirtyObjectsList.contains(dirtyObject))
			{
				dirtyObjectsList.add(dirtyObject);
			}
		}
		lst.clear();

		List<Page> pages = session.getTouchedPages();
		for (Page page : pages)
		{
			if (!touchedPages.contains(page))
			{
				touchedPages.add(page);
			}
		}
		pages.clear();

		session.moveUsedPage(Thread.currentThread(), httpThread);

	}

	/**
	 * @return the exception
	 */
	public Exception getException()
	{
		return exception;
	}

	/**
	 * @return the executed
	 */
	@Override
	public boolean isExecuted()
	{
		return executed;
	}

	/**
	 * 
	 */
	@Override
	public void willSuspend()
	{
		// store the current request events of the client that are 
		// created in this thread on this event object. (see addEvent)
		// but only if it was never suspended. if it was suspended then on this event no http thread is waiting any more on.
		if (!wasSuspended)
		{
			setEvents(client.getRequestEvents());
		}
		cleanup();
		super.willSuspend();
		wasSuspended = true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.server.headlessclient.eventthread.Event#willResume()
	 */
	@Override
	public void willResume()
	{
		super.willResume();
		// when resuming move all used pages back to this thread.
		// the httpThread should be updated 
		session.moveUsedPage(httpThread, Thread.currentThread());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.server.headlessclient.eventthread.Event#executeInBackground()
	 */
	@Override
	public void executeInBackground()
	{
		cleanup();
		super.executeInBackground();
	}

	/**
	 * @return
	 */
	public List<Runnable> getEvents()
	{
		List<Runnable> retval = events;
		events = null;
		return retval == null ? Collections.<Runnable> emptyList() : retval;
	}

	public void setEvents(List<Runnable> requestEvents)
	{
		if (events != null)
		{
			Debug.error(new IllegalStateException("events set twice!"));
			events = new ArrayList<Runnable>(events);
			events.addAll(requestEvents);
		}
		else
		{
			events = requestEvents;
		}
	}
}