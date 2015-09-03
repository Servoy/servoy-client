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

package com.servoy.j2db.server.ngclient.design;

import java.util.List;
import java.util.Map;

import org.sablo.specification.WebComponentSpecification;
import org.sablo.websocket.IClientService;
import org.sablo.websocket.impl.ClientService;

import com.servoy.j2db.IFormController;
import com.servoy.j2db.server.ngclient.INGClientWindow;
import com.servoy.j2db.server.ngclient.NGClientWebsocketSession;

/**
 * @author jcompagner
 */
public class DesignNGClientWebsocketSession extends NGClientWebsocketSession
{
	public static final String EDITOR_CONTENT_SERVICE = "$editorContentService";

	private static final WebComponentSpecification EDITOR_CONTENT_SERVICE_SPECIFICATION = new WebComponentSpecification(EDITOR_CONTENT_SERVICE, "",
		EDITOR_CONTENT_SERVICE, null, null, null, "", null);

	/**
	 * @param uuid
	 */
	public DesignNGClientWebsocketSession(String uuid)
	{
		super(uuid);
	}

	@Override
	protected IClientService createClientService(String name)
	{
		if (EDITOR_CONTENT_SERVICE.equals(name))
		{
			return new ClientService(EDITOR_CONTENT_SERVICE, EDITOR_CONTENT_SERVICE_SPECIFICATION);
		}
		return super.createClientService(name);
	}

	@Override
	public INGClientWindow createWindow(String windowName)
	{
		return new DesignNGClientWindow(this, windowName);
	}


	@Override
	public void onOpen(Map<String, List<String>> requestParams)
	{
		super.onOpen(requestParams);
		String form = requestParams.get("f").get(0);
		IFormController controller = getClient().getFormManager().leaseFormPanel(form);
		getClient().getRuntimeWindowManager().getCurrentWindow().setController(controller);
		if (getClient().getSolution() != null)
		{
			sendSolutionCSSURL(getClient().getSolution());
		}
	}

}
