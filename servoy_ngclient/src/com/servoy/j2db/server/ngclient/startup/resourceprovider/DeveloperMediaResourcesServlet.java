/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2018 Servoy BV

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

package com.servoy.j2db.server.ngclient.startup.resourceprovider;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.IDebugClientHandler;
import com.servoy.j2db.server.ngclient.MediaResourcesServlet;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;

/**
 * MediaServlet used only in the developer.
 * @author emera
 */
@WebServlet("/resources/*")
public class DeveloperMediaResourcesServlet extends MediaResourcesServlet
{
	@Override
	protected IApplication getClient(HttpServletRequest request, int clientnr)
	{
		IApplication client = super.getClient(request, clientnr);
		if (client == null)
		{
			IDebugClientHandler debugClientHandler = ApplicationServerRegistry.get().getDebugClientHandler();
			if (debugClientHandler != null)
			{
				client = debugClientHandler.getDebugNGClient();
			}
		}
		return client;

	}
}
