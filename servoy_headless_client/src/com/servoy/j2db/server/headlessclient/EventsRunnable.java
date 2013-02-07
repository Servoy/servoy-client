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

package com.servoy.j2db.server.headlessclient;

import java.util.List;

import com.servoy.j2db.util.Debug;

/**
 * @author jcompagner
 *
 */
public final class EventsRunnable implements Runnable
{
	private final List<Runnable> toExecute;
	private final WebClient client;

	/**
	 * @param toExecute list of runnabels to execute
	 */
	public EventsRunnable(List<Runnable> toExecute)
	{
		this.client = null;
		this.toExecute = toExecute;
	}

	/**
	 * @param client Client to use to check for more events.
	 * @param toExecute List of Runnables to execute
	 */
	public EventsRunnable(WebClient client, List<Runnable> toExecute)
	{
		this.client = client;
		this.toExecute = toExecute;
	}

	public void run()
	{
		for (Runnable runnable : toExecute)
		{
			try
			{
				runnable.run();
			}
			catch (Throwable e)
			{
				Debug.error("error executing event " + runnable, e);
			}
			synchronized (runnable)
			{
				runnable.notifyAll();
			}
		}
		// look if those did add new events in the mean time.
		if (client != null) client.executeEvents();
	}
}