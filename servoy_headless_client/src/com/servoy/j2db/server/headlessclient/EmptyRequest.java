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
package com.servoy.j2db.server.headlessclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.util.lang.Bytes;


/**
 * @author jcompagner
 * 
 */
public class EmptyRequest extends WebRequest
{

	/**
	 * @see wicket.protocol.http.WebRequest#getContextPath()
	 */
	public String getContextPath()
	{
		return "";
	}

	/**
	 * @see wicket.protocol.http.WebRequest#getHttpServletRequest()
	 */
	@Override
	public HttpServletRequest getHttpServletRequest()
	{
		return new HttpServletRequest()
		{

			public void setCharacterEncoding(String arg0) throws UnsupportedEncodingException
			{
			}

			public void setAttribute(String arg0, Object arg1)
			{
			}

			public void removeAttribute(String arg0)
			{
			}

			public boolean isSecure()
			{
				return false;
			}

			public int getServerPort()
			{
				return 0;
			}

			public String getServerName()
			{
				return null;
			}

			public String getScheme()
			{
				return null;
			}

			public RequestDispatcher getRequestDispatcher(String arg0)
			{
				return null;
			}

			public int getRemotePort()
			{
				return 0;
			}

			public String getRemoteHost()
			{
				return null;
			}

			public String getRemoteAddr()
			{
				return null;
			}

			public String getRealPath(String arg0)
			{
				return null;
			}

			public BufferedReader getReader() throws IOException
			{
				return null;
			}

			public String getProtocol()
			{
				return null;
			}

			public String[] getParameterValues(String arg0)
			{
				return null;
			}

			public Enumeration getParameterNames()
			{
				return null;
			}

			public Map getParameterMap()
			{
				return null;
			}

			public String getParameter(String arg0)
			{
				return null;
			}

			public Enumeration getLocales()
			{
				return null;
			}

			public Locale getLocale()
			{
				return null;
			}

			public int getLocalPort()
			{
				return 0;
			}

			public String getLocalName()
			{
				return null;
			}

			public String getLocalAddr()
			{
				return null;
			}

			public ServletInputStream getInputStream() throws IOException
			{
				return null;
			}

			public String getContentType()
			{
				return null;
			}

			public int getContentLength()
			{
				return 0;
			}

			public String getCharacterEncoding()
			{
				return null;
			}

			public Enumeration getAttributeNames()
			{
				return null;
			}

			public Object getAttribute(String arg0)
			{
				return null;
			}

			public boolean isUserInRole(String arg0)
			{
				return false;
			}

			public boolean isRequestedSessionIdValid()
			{
				return false;
			}

			public boolean isRequestedSessionIdFromUrl()
			{
				return false;
			}

			public boolean isRequestedSessionIdFromURL()
			{
				return false;
			}

			public boolean isRequestedSessionIdFromCookie()
			{
				return false;
			}

			public Principal getUserPrincipal()
			{
				return null;
			}

			public HttpSession getSession(boolean arg0)
			{
				return null;
			}

			public HttpSession getSession()
			{
				return null;
			}

			public String getServletPath()
			{
				return null;
			}

			public String getRequestedSessionId()
			{
				return null;
			}

			public StringBuffer getRequestURL()
			{
				return null;
			}

			public String getRequestURI()
			{
				return null;
			}

			public String getRemoteUser()
			{
				return null;
			}

			public String getQueryString()
			{
				return null;
			}

			public String getPathTranslated()
			{
				return null;
			}

			public String getPathInfo()
			{
				return null;
			}

			public String getMethod()
			{
				return null;
			}

			public int getIntHeader(String arg0)
			{
				return 0;
			}

			public Enumeration getHeaders(String arg0)
			{
				return null;
			}

			public Enumeration getHeaderNames()
			{
				return null;
			}

			public String getHeader(String arg0)
			{
				return null;
			}

			public long getDateHeader(String arg0)
			{
				return 0;
			}

			public Cookie[] getCookies()
			{
				return null;
			}

			public String getContextPath()
			{
				return null;
			}

			public String getAuthType()
			{
				return null;
			}
		};
	}

	/**
	 * @see wicket.protocol.http.WebRequest#getLocale()
	 */
	@Override
	public Locale getLocale()
	{
		return Locale.getDefault();
	}

	/**
	 * @see wicket.protocol.http.WebRequest#getParameter(java.lang.String)
	 */
	@Override
	public String getParameter(String key)
	{
		return null;
	}

	/**
	 * @see wicket.protocol.http.WebRequest#getParameterMap()
	 */
	@Override
	public Map getParameterMap()
	{
		return Collections.EMPTY_MAP;
	}

	/**
	 * @see wicket.protocol.http.WebRequest#getParameters(java.lang.String)
	 */
	@Override
	public String[] getParameters(String key)
	{
		return null;
	}

	/**
	 * @see wicket.protocol.http.WebRequest#getServletPath()
	 */
	@Override
	public String getServletPath()
	{
		return "";
	}

	/**
	 * @see wicket.protocol.http.WebRequest#isAjax()
	 */
	@Override
	public boolean isAjax()
	{
		return false;
	}

	/**
	 * @see wicket.protocol.http.WebRequest#newMultipartWebRequest(wicket.util.lang.Bytes)
	 */
	@Override
	public WebRequest newMultipartWebRequest(Bytes maxSize)
	{
		return null;
	}

	/**
	 * @see wicket.Request#getPath()
	 */
	@Override
	public String getPath()
	{
		return null;
	}

	/**
	 * @see wicket.Request#getRelativeURL()
	 */
	@Override
	public String getRelativeURL()
	{
		return null;
	}

	/**
	 * @see org.apache.wicket.Request#getRelativePathPrefixToContextRoot()
	 */
	@Override
	public String getRelativePathPrefixToContextRoot()
	{
		return null;
	}

	/**
	 * @see org.apache.wicket.Request#getRelativePathPrefixToWicketHandler()
	 */
	@Override
	public String getRelativePathPrefixToWicketHandler()
	{
		return null;
	}

	/**
	 * @see org.apache.wicket.Request#getURL()
	 */
	@Override
	public String getURL()
	{
		return null;
	}

	/**
	 * @see org.apache.wicket.Request#getQueryString()
	 */
	@Override
	public String getQueryString()
	{
		return null;
	}

}
