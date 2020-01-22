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

package com.servoy.j2db.server.ngclient.endpoint;


import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.sablo.websocket.ClientSideWindowState;
import org.sablo.websocket.IWindow;

import com.servoy.j2db.util.Pair;

/**
 * ClientSideWindowState for NGclients, keeps track of loaded forms on the client as well.
 *
 * @author rgansevles, acostescu
 */

public class NGClientSideWindowState extends ClientSideWindowState
{

	/**
	 * So basically forms can be on client/browser and have their state 'attachedToDOM' not.<br/>
	 * For example a form could show, then hide so it was attached to DOM then it was detached.<br/><br/>
	 *
	 * The boolean in the right of each value in this map represents the attached/detached to/from DOM status.
	 * The string in the left of each value is the URL for the form with name given by the key. A form is present in this map only
	 */
	private final ConcurrentMap<String, Pair<String, Boolean>> formsOnClientForThisEndpoint = new ConcurrentHashMap<String, Pair<String, Boolean>>();

	public NGClientSideWindowState(IWindow window)
	{
		super(window);
	}

	public boolean addFormIfAbsent(String formName, String formUrl)
	{
		return formsOnClientForThisEndpoint.putIfAbsent(formName, new Pair<String, Boolean>(formUrl, Boolean.FALSE)) == null;
	}

	public void formDestroyed(String formName)
	{
		formsOnClientForThisEndpoint.remove(formName);
	}

	public String getFormUrl(String formName)
	{
		Pair<String, Boolean> pair = formsOnClientForThisEndpoint.get(formName);
		return pair != null ? pair.getLeft() : null;
	}

	public void setAttachedToDOM(String formName, boolean attached)
	{
		Pair<String, Boolean> pair = formsOnClientForThisEndpoint.get(formName);
		if (pair != null)
		{
			pair.setRight(Boolean.valueOf(attached));
		}
	}

	public boolean isFormAttachedToDOM(String formName)
	{
		Pair<String, Boolean> pair = formsOnClientForThisEndpoint.get(formName);
		return pair != null ? pair.getRight().booleanValue() : false;
	}

	@Override
	public void dispose()
	{
		super.dispose();
		formsOnClientForThisEndpoint.clear();
	}

}
