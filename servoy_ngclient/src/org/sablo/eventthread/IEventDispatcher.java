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

import org.sablo.websocket.IWebsocketSession;


/**
 * A {@link IWebsocketSession} returns a instance of this that should start up a thread
 * that will serve then as the dispatching thread for al the events coming from the browser.
 *
 * @author jcompagner
 *
 */
public interface IEventDispatcher<E extends Event> extends Runnable
{
	/**
	 * @param object The Object that is the suspend 'lock'
	 */
	void suspend(Object object);

	/**
	 * @param object The Object that holds an suspend 'lock' to resume it.
	 */
	void resume(Object object);

	public IEventProgressMonitor getEventProgressMonitor();

	boolean isEventDispatchThread();

	/**
	 * @param event
	 */
	void addEvent(E event);

	/**
	 * destroys this event dispatcher thread.
	 */
	public void destroy();

}
