/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2021 Servoy BV

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

import static com.servoy.j2db.server.ngclient.WebsocketSessionFactory.CLIENT_ENDPOINT;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.sablo.websocket.IWebsocketSession;
import org.sablo.websocket.WebsocketSessionManager;

/**
 * @author jcompagner
 * @since 2021.03
 */
@SuppressWarnings("nls")
@WebServlet("/clientfunctions.js")
public class ClientFunctionsServlet extends HttpServlet
{
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		String clientNr = AngularIndexPageWriter.getClientNr(req.getRequestURI(), req);
		HttpSession httpSession = req.getSession(false);
		if (clientNr != null && httpSession != null)
		{
			IWebsocketSession wsSession = WebsocketSessionManager.getSession(CLIENT_ENDPOINT, httpSession, Integer.parseInt(clientNr));
			if (wsSession instanceof INGClientWebsocketSession)
			{
				INGApplication client = ((INGClientWebsocketSession)wsSession).getClient();
				StringBuilder sb = new StringBuilder(512);
				sb.append("(function(){\n");
				sb.append("var fns = {};\n");
				sb.append("window.svyClientSideFunctions = fns;\n");
				Map<String, String> functions = client.getClientFunctions();
				for (Entry<String, String> entry : functions.entrySet())
				{
					sb.append("fns['");
					sb.append(entry.getValue());
					sb.append("']=");
					sb.append(entry.getKey());
					sb.append(";\n");
				}
				sb.append("})();\n");
				resp.setContentLength(sb.length());
				resp.setContentType("text/javascript");
				resp.getWriter().write(sb.toString());
				return;
			}

		}
		resp.sendError(HttpServletResponse.SC_NOT_FOUND);
	}
}
