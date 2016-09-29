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

import org.sablo.websocket.WebsocketEndpoint;

import com.servoy.j2db.util.Pair;

/**
 * Base endpoint for NGclients, keeps track of loaded forms on the client.
 *
 * @author rgansevles
 *
 */

public class BaseNGClientEndpoint extends WebsocketEndpoint implements INGClientWebsocketEndpoint
{

	/**
	 * So basically forms can be on client/browser and have their state 'attachedToDOM' not.<br/>
	 * For example a form could show, then hide so it was attached to DOM then it was detached.<br/><br/>
	 *
	 * The boolean in the right of each value in this map represents the attached/detached to/from DOM status.
	 * The string in the left of each value is the URL for the form with name given by the key. A form is present in this map only
	 */
	private final ConcurrentMap<String, Pair<String, Boolean>> formsOnClientForThisEndpoint = new ConcurrentHashMap<String, Pair<String, Boolean>>();

	public BaseNGClientEndpoint(String endpointType)
	{
		super(endpointType);
	}

	@Override
	public boolean addFormIfAbsent(String formName, String formUrl)
	{
		return formsOnClientForThisEndpoint.putIfAbsent(formName, new Pair<String, Boolean>(formUrl, Boolean.FALSE)) == null;
	}

	@Override
	public void formDestroyed(String formName)
	{
		formsOnClientForThisEndpoint.remove(formName);
	}

	@Override
	public String getFormUrl(String formName)
	{
		return formsOnClientForThisEndpoint.containsKey(formName) ? formsOnClientForThisEndpoint.get(formName).getLeft() : null;
	}

	@Override
	public void setAttachedToDOM(String formName, boolean attached)
	{
		if (formsOnClientForThisEndpoint.containsKey(formName))
		{
			formsOnClientForThisEndpoint.get(formName).setRight(Boolean.valueOf(attached));
		}
	}

	@Override
	public boolean isFormAttachedToDOM(String formName)
	{
		return formsOnClientForThisEndpoint.containsKey(formName) && formsOnClientForThisEndpoint.get(formName).getRight().booleanValue();
	}

}
