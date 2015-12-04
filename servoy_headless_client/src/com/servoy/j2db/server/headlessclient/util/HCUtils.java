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

package com.servoy.j2db.server.headlessclient.util;

import javax.servlet.http.HttpServletRequest;

/**
 * @author rgansevles
 *
 */
public class HCUtils
{

	/**
	 * Replace absolute url with an url that works against the original (proxy) host, using standard request headers
	 * for proxy information.
	 *
	 * @param absoluteUrl
	 * @param request
	 *
	 * @return modified absolute url
	 */
	public static String replaceForwardedHost(String absoluteUrl, HttpServletRequest request)
	{
		// headers X-Forwarded-XXX
		String forwardedHost = request.getHeader("X-Forwarded-Host");

		String forwardedScheme = request.getHeader("X-Forwarded-Proto");
		if (forwardedScheme == null)
		{
			forwardedScheme = request.getHeader("X-Forwarded-Scheme");
		}

		// Header Forwarded (RFC 7239)
		String forwardedHeader = request.getHeader("Forwarded");
		if (forwardedHeader != null)
		{
			for (String s : forwardedHeader.split(";"))
			{
				if (s.startsWith("host="))
				{
					forwardedHost = s.substring(5);
				}
				else if (s.startsWith("proto="))
				{
					forwardedScheme = s.substring(6);
				}
			}
		}

		// Can be multiple values (separated by comma) in case of chained proxies, use first (original proxy)
		if (forwardedHost != null)
		{
			forwardedHost = forwardedHost.split(",")[0].trim();
		}
		if (forwardedScheme != null)
		{
			forwardedScheme = forwardedScheme.split(",")[0].trim();
		}

		String url = absoluteUrl;

		// replace scheme with forwarded
		String scheme = request.getScheme();
		if (scheme != null && forwardedScheme != null && url.startsWith(scheme))
		{
			url = forwardedScheme + url.substring(scheme.length());
		}

		// replace host (includes port) with forwarded
		String hostHeader = request.getHeader("Host");
		if (hostHeader != null && forwardedHost != null)
		{
			int index = url.indexOf(hostHeader);
			if (index >= 0)
			{
				url = url.substring(0, index) + forwardedHost + url.substring(index + hostHeader.length());
			}
		}

		return url;
	}
}
