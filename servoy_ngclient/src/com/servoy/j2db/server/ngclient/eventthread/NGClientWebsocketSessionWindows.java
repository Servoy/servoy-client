/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

package com.servoy.j2db.server.ngclient.eventthread;

import org.sablo.eventthread.WebsocketSessionWindows;
import org.sablo.websocket.IWebsocketSession;
import org.sablo.websocket.IWindow;

import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.server.ngclient.INGClientWindow;

/**
 * A {@link INGClientWindow} implementation that redirects all the calls on it to the current registered,
 * {@link IWebsocketSession#getWindows()} windows.
 *
 * @author jcompagner, rgansevles
 *
 */
public class NGClientWebsocketSessionWindows extends WebsocketSessionWindows implements INGClientWindow
{

	/**
	 * @param session
	 */
	public NGClientWebsocketSessionWindows(IWebsocketSession session)
	{
		super(session);
	}


	@Override
	public void updateForm(Form form, String name)
	{
		// ignore
	}

	@Override
	public void formCreated(String formName)
	{
		// ignore
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.server.ngclient.INGClientWindow#destroyForm(java.lang.String)
	 */
	@Override
	public void destroyForm(String name)
	{
		for (IWindow window : getSession().getWindows())
		{
			((INGClientWindow)window).destroyForm(name);
		}
	}

	@Override
	public void touchForm(Form flattenedForm, String realInstanceName, boolean async)
	{
		// ignore
	}

}
