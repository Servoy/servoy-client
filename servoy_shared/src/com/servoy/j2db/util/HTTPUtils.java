/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.j2db.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Class that provides static utility methods related to HTTP.
 * @author acostescu
 */
public class HTTPUtils
{
	public static final String IF_MODIFIED_SINCE = "If-Modified-Since"; //$NON-NLS-1$
	public static final String LAST_MODIFIED = "Last-Modified"; //$NON-NLS-1$

	/**
	 * This method tries to make peace between different browsers, versions and browser bugs for no-caching response headers.<br>
	 * It will set response headers to prevent the response from being cached.
	 * @param response the HTTP response on which header fields will be set.
	 */
	public static void setNoCacheHeaders(HttpServletResponse response)
	{
		setNoCacheHeaders(response, null);
	}

	/**
	 * This method tries to make peace between different browsers, versions and browser bugs for no-caching response headers.<br>
	 * It will set response headers to prevent the response from being cached.
	 * @param response the HTTP response on which header fields will be set.
	 * @param extraCacheControl some extra cache control additions.
	 */
	@SuppressWarnings("nls")
	public static void setNoCacheHeaders(HttpServletResponse response, String extraCacheControl)
	{
		String cacheControl = "max-age=0, must-revalidate, proxy-revalidate";
		if (extraCacheControl != null)
		{
			cacheControl += "," + extraCacheControl;
		}
		response.setHeader("Cache-Control", cacheControl); //HTTP 1.1
		response.setHeader("Expires", "0");//$NON-NLS-1$ // mentioned as an invalid (but used anyway in HTTP 1.0) value which MUST be interpreted correctly in HTTP 1.1 specs. (this means interpreted as no-cache); we can use this to avoid incorrectly synced clocks that would cause problems if expires clause would be used as described in specs.

//		if (request.isSecure())
//		{
//			response.setDateHeader("Expires", System.currentTimeMillis() + 5000);//$NON-NLS-1$
//		}
//		else
//		{
//		    ... do normal headers
//		}
//		//prevents save to disk in https, Expires should be enough
//		response.setHeader("Proxy", "no-cache"); //$NON-NLS-1$//$NON-NLS-2$
//		response.setHeader("Pragma", "no-cache"); //HTTP 1.0 //$NON-NLS-1$ //$NON-NLS-2$ // Pragma: no-cache is meant to be used in requests, not responses; in responses it's only taken into consideration by some browsers, such as IE in https, but this also results in a bug for IE 6
//		//seems not to work in IE
//		response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
//		response.setHeader("Cache-Control","no-store"); //HTTP 1.1
//
		//let cache for 5 seconds in browser, workarround for IE browser (which has a known bug with https and expires); see http://support.microsoft.com/kb/323308
//		response.setDateHeader("Expires", System.currentTimeMillis() + 5000);//$NON-NLS-1$ // works except for when system clocks are out of sync or when you do the operation in less then 5 secs.
	}

	/**
	 * Checks if a requested resource has been modified or not. It will also set the "Last-Modified" header in response.<br><br>
	 * If it has not been modified, it will return true and set response status HttpServletResponse.SC_NOT_MODIFIED.
	 * If it has been modified or request didn't want to use this check it will just return false.
	 * @param lastModifiedTime the last modification time (millis) of the requested resource.
	 */
	@SuppressWarnings("nls")
	public static boolean checkAndSetUnmodified(HttpServletRequest servletRequest, HttpServletResponse servletResponse, long lastModifiedTime)
	{
		long l = lastModifiedTime / 1000 * 1000;
		servletResponse.setDateHeader(LAST_MODIFIED, l);
		servletResponse.setHeader("Cache-Control", "max-age=0, must-revalidate, proxy-revalidate"); //HTTP 1.1
		long lm = servletRequest.getDateHeader(IF_MODIFIED_SINCE);
		if (lm != -1 && lm == l)
		{
			servletResponse.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
			return true;
		}
		return false;
	}
}