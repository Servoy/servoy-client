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

package com.servoy.j2db.server.ngclient;

import static com.servoy.j2db.server.ngclient.AngularIndexPageWriter.addcontentSecurityPolicyHeader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.sablo.security.ContentSecurityPolicyConfig;
import org.sablo.websocket.WebsocketSessionManager;

import com.servoy.j2db.server.ngclient.auth.CloudStatelessAccessManager;
import com.servoy.j2db.server.ngclient.auth.OAuthHandler;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;

/**
 * @author jcompagner
 * @since 2021.03
 *
 */
@SuppressWarnings("nls")
@WebFilter(urlPatterns = { "/solution/*" }, dispatcherTypes = { DispatcherType.REQUEST, DispatcherType.FORWARD })
public class AngularIndexPageFilter implements Filter
{
	public static final String SOLUTIONS_PATH = "solution/";
	public static final String CLIENT_ENDPOINT = "client";
	private String indexPage = null;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
		try (InputStream rs = filterConfig.getServletContext().getResourceAsStream("WEB-INF/angular-index.html"))
		{
			if (rs != null)
			{
				indexPage = IOUtils.toString(rs, Charset.forName("UTF-8"));
			}
		}
		catch (IOException e)
		{
			throw new ServletException(e);
		}
		StatelessLoginHandler.init(filterConfig.getServletContext());
//		if (indexPage == null) throw new ServletException("Couldn't read 'WEB-INF/angular-index.html' from the context to get the angular index page");
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException
	{
		HttpServletRequest request = (HttpServletRequest)servletRequest;
		HttpServletResponse response = (HttpServletResponse)servletResponse;
		String requestURI = request.getRequestURI();
		String solutionName = getSolutionNameFromURI(requestURI);
		if (("GET".equalsIgnoreCase(request.getMethod()) || "POST".equalsIgnoreCase(request.getMethod())) && solutionName != null)
		{

			if ((requestURI.endsWith("/") || requestURI.endsWith("/" + solutionName) || requestURI.toLowerCase().endsWith("/index.html")) ||
				requestURI.contains("/svy_oauth/"))
			{
				String clientnr = AngularIndexPageWriter.getClientNr(requestURI, request);
				INGClientWebsocketSession wsSession = null;
				HttpSession httpSession = request.getSession(false);
				if (clientnr != null && httpSession != null)
				{
					wsSession = (INGClientWebsocketSession)WebsocketSessionManager.getSession(CLIENT_ENDPOINT, httpSession, Integer.parseInt(clientnr));
				}
				if (AngularIndexPageWriter.handleMaintenanceMode(request, response, wsSession))
				{
					return;
				}
				try
				{
					Pair<Boolean, String> showLogin = null;
					if (requestURI.contains("/svy_oauth/"))
					{
						showLogin = OAuthHandler.handleOauth(request, response);
						if (Boolean.FALSE.equals(showLogin.getLeft()) && showLogin.getRight() == null) return;
					}
					else
					{
						showLogin = StatelessLoginHandler.mustAuthenticate(request, response, solutionName);
					}

					if (showLogin.getLeft().booleanValue())
					{
						StatelessLoginHandler.writeLoginPage(request, response, solutionName, showLogin.getRight());
						return;
					}
					if (showLogin.getRight() != null)
					{
						HttpSession session = request.getSession(); // we know we are logged in so we can make a session now
						session.setAttribute(StatelessLoginHandler.ID_TOKEN, showLogin.getRight());
					}
				}
				catch (Exception e)
				{
					Debug.error(e);
					return;
				}


				ContentSecurityPolicyConfig contentSecurityPolicyConfig = addcontentSecurityPolicyHeader(request, response, false); // for NG2 remove the unsafe-eval
				if (this.indexPage != null)
				{
					request.getSession(); // now really make a session, we know we are going to render the index page to start a client.
					AngularIndexPageWriter.writeIndexPage(this.indexPage, request, response, solutionName,
						contentSecurityPolicyConfig == null ? null : contentSecurityPolicyConfig.getNonce());
				}
				else
				{
					response.setCharacterEncoding("UTF-8");
					response.setContentType("text/html");
					String indexHtml = "<html><body>No NGClient2 resources exported</body></html>";
					response.setContentLengthLong(indexHtml.length());
					response.getWriter().write(indexHtml);
					Debug.error(
						"Trying to service NGClient2, but no resouces are generatd for that in the exporter (-NG2 flag) or an error happend when exporting");

				}
				return;
			}
			else if (solutionName != null && CloudStatelessAccessManager.handlePossibleCloudRequest(request, response, solutionName, this.indexPage))
			{
				return;
			}
			else if (requestURI.toLowerCase().endsWith("/startup.js"))
			{
				AngularIndexPageWriter.writeStartupJs(request, (HttpServletResponse)servletResponse, solutionName);
				return;
			}
			else if (AngularIndexPageWriter.handleDeeplink(request, (HttpServletResponse)servletResponse))
			{
				return;
			}
		}
		chain.doFilter(servletRequest, servletResponse);
	}

	@Override
	public void destroy()
	{
	}

	private String getSolutionNameFromURI(String uri)
	{
		int solutionIndex = uri.indexOf(SOLUTIONS_PATH);
		int solutionEndIndex = uri.indexOf("/", solutionIndex + SOLUTIONS_PATH.length() + 1);
		if (solutionEndIndex == -1) solutionEndIndex = uri.length();
		if (solutionIndex >= 0 && solutionEndIndex > solutionIndex)
		{
			String possibleSolutionName = uri.substring(solutionIndex + SOLUTIONS_PATH.length(), solutionEndIndex);
			// skip all names that have a . in them
			if (possibleSolutionName.contains(".")) return null;
			return StringEscapeUtils.escapeHtml4(possibleSolutionName);
		}
		return null;
	}

}
