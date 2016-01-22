/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.j2db.server.ngclient;

import java.util.Collection;

import org.sablo.IChangeListener;
import org.sablo.websocket.IWebsocketSession;
import org.sablo.websocket.IWindow;
import org.sablo.websocket.utils.ObjectReference;

import com.servoy.j2db.persistence.Solution;

/**
 * Interface for classes handling a websocket session based on a client.
 * @author rgansevles
 */
public interface INGClientWebsocketSession extends IWebsocketSession, IChangeListener
{
	INGApplication getClient();

	void solutionLoaded(Solution flattenedSolution);

	void sendRedirect(String redirectUrl);

	void sendStyleSheet();

	/**
	 * Will return the window in which the form with given name is already loaded. It will return null if it's not loaded in any window yet.
	 */
	INGClientWindow getWindowWithForm(String formName);

	@Override
	Collection<INGClientWindow> getWindows();

	Collection<ObjectReference< ? extends IWindow>> getWindowsRefs();
}
