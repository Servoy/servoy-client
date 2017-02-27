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

import java.util.Collection;

import org.sablo.Container;
import org.sablo.eventthread.WebsocketSessionWindows;
import org.sablo.websocket.IWebsocketSession;
import org.sablo.websocket.IWindow;

import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.server.ngclient.INGClientWebsocketSession;
import com.servoy.j2db.server.ngclient.INGClientWindow;
import com.servoy.j2db.server.ngclient.NGRuntimeWindow;

/**
 * A {@link INGClientWindow} implementation that redirects all the calls on it to the current registered,
 * {@link IWebsocketSession#getWindows()} windows.
 *
 * @author jcompagner, rgansevles
 *
 */
public class NGClientWebsocketSessionWindows extends WebsocketSessionWindows implements INGClientWindow
{

	private IWindow lastKnownWindow;

	/**
	 * @param session
	 */
	public NGClientWebsocketSessionWindows(INGClientWebsocketSession session)
	{
		super(session);
		Collection<INGClientWindow> windows = getSession().getWindows();
		if (windows.size() == 1)
		{
			// just a shortcut if there is one 1 window (tab in browser) then just always take that one
			lastKnownWindow = windows.iterator().next();
		}
		else if (windows.size() > 0)
		{
			NGRuntimeWindow currentWindow = getSession().getClient().getRuntimeWindowManager().getCurrentWindow();
			if (currentWindow != null)
			{
				for (INGClientWindow window : windows)
				{
					if (window.getUuid().equals(currentWindow.getName()))
					{
						lastKnownWindow = window;
						break;
					}
				}
			}
		}
	}

	@Override
	public String getCurrentFormUrl()
	{
		if (lastKnownWindow != null) return lastKnownWindow.getCurrentFormUrl();
		return super.getCurrentFormUrl();
	}

	@Override
	public Container getForm(String formName)
	{
		if (lastKnownWindow != null) return lastKnownWindow.getForm(formName);
		return super.getForm(formName);
	}

	@Override
	public String getName()
	{
		if (lastKnownWindow != null) return lastKnownWindow.getName();
		return super.getName();
	}

	@Override
	public String getUuid()
	{
		if (lastKnownWindow != null) return lastKnownWindow.getUuid();
		return super.getUuid();
	}


	@Override
	public void updateForm(Form form, String name, IFormHTMLAndJSGenerator formTemplateGenerator)
	{
		for (INGClientWindow window : getSession().getWindows())
		{
			window.updateForm(form, name, formTemplateGenerator);
		}
	}

	@Override
	public void setFormResolved(String formName, boolean resolved)
	{
		for (INGClientWindow window : getSession().getWindows())
		{
			window.setFormResolved(formName, resolved);
		}
	}

	@Override
	public INGClientWebsocketSession getSession()
	{
		return (INGClientWebsocketSession)super.getSession();
	}

	@Override
	public void destroyForm(String name)
	{
		for (INGClientWindow window : getSession().getWindows())
		{
			window.destroyForm(name);
		}
	}

	@Override
	public void touchForm(Form flattenedForm, String realInstanceName, boolean async, boolean testForValidForm)
	{
		for (INGClientWindow window : getSession().getWindows())
		{
			window.touchForm(flattenedForm, realInstanceName, async, testForValidForm);
		}
	}

	@Override
	public void registerAllowedForm(String formName)
	{
		for (INGClientWindow window : getSession().getWindows())
		{
			window.registerAllowedForm(formName);
		}
	}

	@Override
	public boolean hasFormChangedSinceLastSendToClient(Form flattenedForm, String realName)
	{
		for (INGClientWindow window : getSession().getWindows())
		{
			if (window.hasFormChangedSinceLastSendToClient(flattenedForm, realName)) return true;
		}
		return false;
	}

	@Override
	public boolean hasForm(String realName)
	{
		for (INGClientWindow window : getSession().getWindows())
		{
			if (window.hasForm(realName)) return true;
		}
		return false;
	}

}
