/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

package com.servoy.j2db.server.ngclient.auth;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.servoy.j2db.server.ngclient.StatelessLoginHandler;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * @author emera
 */
public class HTMLWriter
{
	public static final Logger log = LoggerFactory.getLogger("stateless.login");

	/**
	 * Check if there is an id_token as a request parameter or session attribute (without creating a new session).
	 * @param request
	 * @return the existing id_token or null if not present or in case of empty string
	 */
	public static String getExistingIdToken(HttpServletRequest request)
	{
		String id_token = request.getParameter(StatelessLoginHandler.ID_TOKEN);
		if (id_token == null)
		{
			HttpSession session = request.getSession(false);
			if (session != null)
			{
				id_token = (String)session.getAttribute(StatelessLoginHandler.ID_TOKEN);
			}
		}
		return !Utils.stringIsEmpty(id_token) ? id_token : null;
	}

	public static String getPath(HttpServletRequest request)
	{
		String path = Settings.getInstance().getProperty("servoy.context.path", request.getContextPath() + '/');
		Path p = Paths.get(request.getServletPath()).normalize();
		int i = 0;
		while (i < p.getNameCount() - 1 && !StatelessLoginUtils.SVYLOGIN_PATH.equals(p.getName(i).toString()))
		{
			path += p.getName(i) + "/";
			i++;
		}
		return path;
	}

	public static void writeHTML(HttpServletRequest request, HttpServletResponse response, String html) throws UnsupportedEncodingException, IOException
	{
		if (html == null)
		{
			log.error("writeHTML error: html is null");
			return;
		}
		if (request.getCharacterEncoding() == null) request.setCharacterEncoding("UTF8");
		StringBuilder sb = new StringBuilder();
		sb.append("<base href=\"");
		sb.append(getPath(request));
		sb.append("\">");
		html = html.replace("<base href=\"/\">", sb.toString());

		String requestLanguage = request.getHeader("accept-language");
		if (requestLanguage != null)
		{
			html = html.replace("lang=\"en\"", "lang=\"" + request.getLocale().getLanguage() + "\"");
		}

		response.setCharacterEncoding("UTF-8");
		response.setContentType("text/html");
		response.setContentLengthLong(html.length());
		response.getWriter().write(html);

	}
}
