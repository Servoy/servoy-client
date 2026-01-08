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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;

/**
 * @author emera
 */
public class StatelessLoginUtils
{
	public static final String JWT_Password = "servoy.jwt.logintoken.password";
	public static final String SVYLOGIN_PATH = "svylogin";
	static final Logger log = LoggerFactory.getLogger("stateless.login");

	public static String getServerURL(HttpServletRequest req)
	{
		String scheme = req.getScheme();
		String serverName = req.getServerName();
		int serverPort = req.getServerPort();
		StringBuilder url = new StringBuilder();
		url.append(scheme).append("://").append(serverName);
		if (serverPort != 80 && serverPort != 443)
		{
			url.append(":").append(serverPort);
		}
		url.append(HTMLWriter.getPath(req));
		return url.toString();
	}

	public static String checkForPossibleSavedDeeplink(HttpServletRequest request)
	{
		String state = request.getParameter("state");
		if (state == null)
		{
			return null;
		}

		// remove svyuuid=... and state=...
		String queryString = state.replaceAll("(?:(?<=^)|(?<=&))(state|svyuuid)=[^&]*&?", "");
		queryString = queryString.replaceAll("^&|&$", "");
		return queryString;
	}
}
