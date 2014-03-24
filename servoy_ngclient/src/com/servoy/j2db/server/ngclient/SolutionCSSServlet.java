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

import java.io.IOException;
import java.nio.charset.Charset;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.IDebugClientHandler;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;

/**
 * Provides the solution level CSS content to clients.
 * @author acostescu
 * 
 * solution CSS url:
 * 	/solution-css/[clientuuid]
 */
@WebServlet("/solution-css/*")
public class SolutionCSSServlet extends HttpServlet
{

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		boolean solutionCSSServed = false;

		String path = req.getPathInfo();
		if (path.startsWith("/")) path = path.substring(1);
		String[] paths = path.split("/");

		if (paths.length == 1)
		{
			String clientUUID = paths[0];
			// try to look it up as clientId. (solution model)
			IApplication client = NGClientEndpoint.getClient(clientUUID);
			if (client == null)
			{
				IDebugClientHandler debugClientHandler = ApplicationServerRegistry.get().getDebugClientHandler();
				if (debugClientHandler != null)
				{
					client = debugClientHandler.getDebugNGClient();
				}
			}
			if (client != null)
			{
				solutionCSSServed = true;

				// TODO get that solution CSS from the solution itself
				String cssContent = "/* p { background: yellow } /**/";
				byte[] data = cssContent.getBytes(Charset.forName("UTF-8")); // this is currently hardcoded; we could in the future look for something like this in the content first '@charset "UTF-8";'

				resp.setContentType("text/css");
				resp.setContentLength(data.length);
				ServletOutputStream outputStream = resp.getOutputStream();
				outputStream.write(data);
				outputStream.flush();
			}
		}

		if (!solutionCSSServed) resp.sendError(HttpServletResponse.SC_NOT_FOUND);
	}

}
