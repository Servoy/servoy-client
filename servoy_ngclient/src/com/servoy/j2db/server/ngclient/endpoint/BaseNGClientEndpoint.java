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

	private final ConcurrentMap<String, Pair<String, Boolean>> formsOnClient = new ConcurrentHashMap<String, Pair<String, Boolean>>();

	public BaseNGClientEndpoint(String endpointType)
	{
		super(endpointType);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.server.ngclient.endpoint.INGClientWebsocketEndpoint#addFormIfAbsent(java.lang.String, java.lang.String)
	 */
	@Override
	public boolean addFormIfAbsent(String formName, String formUrl)
	{
		return formsOnClient.putIfAbsent(formName, new Pair<String, Boolean>(formUrl, Boolean.FALSE)) == null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.server.ngclient.endpoint.INGClientWebsocketEndpoint#getFormUrl(java.lang.String)
	 */
	@Override
	public String getFormUrl(String formName)
	{
		return formsOnClient.containsKey(formName) ? formsOnClient.get(formName).getLeft() : null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.server.ngclient.endpoint.INGClientWebsocketEndpoint#markFormCreated(java.lang.String)
	 */
	@Override
	public void markFormCreated(String formName)
	{
		if (formsOnClient.containsKey(formName))
		{
			formsOnClient.get(formName).setRight(Boolean.TRUE);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.server.ngclient.endpoint.INGClientWebsocketEndpoint#isFormCreated(java.lang.String)
	 */
	@Override
	public boolean isFormCreated(String formName)
	{
		return formsOnClient.containsKey(formName) && formsOnClient.get(formName).getRight().booleanValue();
	}

}
