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
