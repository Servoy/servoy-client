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

import java.util.List;

import org.apache.wicket.Application;
import org.apache.wicket.IClusterable;
import org.apache.wicket.Page;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.Session;
import org.mozilla.javascript.Function;

import com.servoy.j2db.util.Debug;

/**
 * An implementation of {@link IEvent} that executes {@link Function} when {@link #execute()} is called.
 * Will set and reset all the wicket thread locals from the creation thread (the http thread) to the execution thread.
 * 
 * @author jcompagner
 * 
 * @since 6.1
 */
public abstract class WicketExecuteEvent implements IEvent
{
	private final RequestCycle requestCycle;
	private final Session session;
	private final Application application;

	private volatile boolean executed;
	private volatile boolean suspended;
	private volatile boolean resetThreadLocals = true;
	private volatile Exception exception;

	private final List<IClusterable> dirtyObjectsList;
	private final List<Page> touchedPages;
	private final Thread currentThread;

	/**
	 * @param f
	 * @param scope
	 * @param thisObject
	 * @param args
	 * @param focusEvent
	 * @param throwException
	 * @param scriptEngine TODO
	 */
	public WicketExecuteEvent()
	{
		requestCycle = RequestCycle.get();
		session = Session.get();
		application = Application.get();
		dirtyObjectsList = session.getDirtyObjectsList();
		touchedPages = session.getTouchedPages();
		currentThread = Thread.currentThread();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.server.headlessclient.IExecuteEvent#execute()
	 */
	public final void execute()
	{
		try
		{
//			resetThreadLocals = RequestCycle.get() == null;
//			if (resetThreadLocals)
//			{
			ServoyRequestCycle.set(requestCycle);
			Session.set(session);
			Application.set(application);

			session.moveUsedPage(currentThread, Thread.currentThread());
//			}

			run();
		}
		catch (Exception e)
		{
			Debug.error(e);
			exception = e;
		}
		finally
		{
			executed = true;
			cleanup();
		}
	}

	public abstract void run();

	/**
	 * 
	 */
	private void cleanup()
	{
		if (resetThreadLocals)
		{
			List<IClusterable> lst = session.getDirtyObjectsList();
			for (IClusterable dirtyObject : lst)
			{
				if (!dirtyObjectsList.contains(dirtyObject))
				{
					dirtyObjectsList.add(dirtyObject);
				}
			}

			List<Page> pages = session.getTouchedPages();
			for (Page page : pages)
			{
				if (!touchedPages.contains(page))
				{
					touchedPages.add(page);
				}
			}

			session.moveUsedPage(Thread.currentThread(), currentThread);

//			ServoyRequestCycle.set(null);
//			Session.unset();
//			Application.unset();
		}
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
	public boolean isExecuted()
	{
		return executed;
	}

	/**
	 * @return the suspended
	 */
	public boolean isSuspended()
	{
		return suspended;
	}

	/**
	 * 
	 */
	public void willSuspend()
	{
		cleanup();
		suspended = true;
	}

	/**
	 * 
	 */
	public void willResume()
	{
		suspended = false;
//		if (RequestCycle.get() == null) ServoyRequestCycle.set(requestCycle);
//		if (Session.exists()) Session.set(session);
//		if (Application.exists()) Application.set(application);
	}

}