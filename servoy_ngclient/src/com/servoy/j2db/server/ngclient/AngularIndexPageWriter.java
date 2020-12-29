/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2020 Servoy BV

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

import static com.servoy.j2db.persistence.IRepository.SOLUTIONS;
import static com.servoy.j2db.server.ngclient.MediaResourcesServlet.FLATTENED_SOLUTION_ACCESS;
import static com.servoy.j2db.server.ngclient.WebsocketSessionFactory.CLIENT_ENDPOINT;
import static com.servoy.j2db.util.Utils.getAsBoolean;
import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.sablo.security.ContentSecurityPolicyConfig;
import org.sablo.util.HTTPUtils;
import org.sablo.websocket.WebsocketSessionManager;

import com.servoy.base.util.TagParser;
import com.servoy.j2db.AbstractActiveSolutionHandler;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.headlessclient.util.HCUtils;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServer;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Settings;

/**
 * @author jcompagner
 * @since 2021.03
 */
@SuppressWarnings("nls")
public class AngularIndexPageWriter
{
	public static final String SOLUTIONS_PATH = "/solution/";

	public static void writeIndexPage(String page, HttpServletRequest request, HttpServletResponse response, String solutionName) throws IOException
	{
		FlattenedSolution fs = getFlattenedSolution(solutionName, request, response);
		if (fs != null)
		{
			StringBuilder sb = new StringBuilder();

			String indexHtml = page;
			final String path = Settings.getInstance().getProperty("servoy.context.path", request.getContextPath() + '/');
			sb.append("<base href=\"");
			sb.append(path);
			sb.append("\">");

			ContentSecurityPolicyConfig contentSecurityPolicyConfig = getContentSecurityPolicyConfig(request);
			if (contentSecurityPolicyConfig != null)
			{
				sb.append("\n  <meta http-equiv=\"Content-Security-Policy\" content=\"");
				contentSecurityPolicyConfig.getDirectives().forEach(sb::append);
				sb.append("\">");

				indexHtml = indexHtml.replace("<script ", "<script nonce='" + contentSecurityPolicyConfig.getNonce() + '\'');
			}

			String titleText = fs.getSolution().getTitleText();
			if (StringUtils.isBlank(titleText))
			{
				titleText = fs.getSolution().getName();
			}
			else if (titleText.equals("<empty>") || titleText.contains("i18n:") || titleText.contains(TagParser.TAGCHAR))
			{
				titleText = "";
			}
			sb.append("\n  <title>");
			sb.append(titleText);
			sb.append("</title>");

			if (fs.getMedia("manifest.json") != null)
			{
				String url = "resources/" + FLATTENED_SOLUTION_ACCESS + "/" + fs.getName() + "/manifest.json";
				sb.append("\n  <link rel=\"manifest\" href=\"");
				sb.append(url);
				sb.append("\">");
			}
			Media headExtension = fs.getMedia("head-index-contributions.html");
			if (headExtension != null)
			{
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(headExtension.getMediaData()), "UTF8")))
				{
					String line;
					for (int count = 0; count < 1000 && (line = reader.readLine()) != null; count++)
					{
						if (line.trim().startsWith("<meta") || line.trim().startsWith("<link"))
						{
							sb.append("\n  ");
							sb.append(line);
						}
					}
				}
				catch (Exception e)
				{
					Debug.error(e);
				}
			}


			indexHtml = indexHtml.replace("<base href=\"/\">", sb.toString());

			String requestLanguage = request.getHeader("accept-language");
			if (requestLanguage != null)
			{
				indexHtml = indexHtml.replace("lang=\"en\"", "lang=\"" + request.getLocale().getLanguage() + "\"");
			}

			response.setCharacterEncoding("UTF-8");
			response.setContentType("text/html");
			response.setContentLengthLong(indexHtml.length());
			response.getWriter().write(indexHtml);
		}
		return;
	}

	/**
	 * @return
	 */
	private static FlattenedSolution getFlattenedSolution(String name, HttpServletRequest request, HttpServletResponse response)
	{
		String uri = request.getRequestURI();
		String clientnr = getClientNr(uri, request);
		INGClientWebsocketSession wsSession = null;
		HttpSession httpSession = request.getSession(false);
		if (clientnr != null && httpSession != null)
		{
			wsSession = (INGClientWebsocketSession)WebsocketSessionManager.getSession(CLIENT_ENDPOINT, httpSession, Integer.parseInt(clientnr));
		}
		FlattenedSolution fs = null;
		boolean closeFS = false;
		if (wsSession != null)
		{
			fs = wsSession.getClient().getFlattenedSolution();
		}
		if (fs == null)
		{
			try
			{
				closeFS = true;
				IApplicationServer as = ApplicationServerRegistry.getService(IApplicationServer.class);
				if (applicationServerUnavailable(response, as))
				{
					return null;
				}

				SolutionMetaData solutionMetaData = (SolutionMetaData)ApplicationServerRegistry.get().getLocalRepository()
					.getRootObjectMetaData(
						name, SOLUTIONS);
				if (solutionMissing(response, name, solutionMetaData))
				{
					return null;
				}

				fs = new FlattenedSolution(solutionMetaData, new AbstractActiveSolutionHandler(as)
				{
					@Override
					public IRepository getRepository()
					{
						return ApplicationServerRegistry.get().getLocalRepository();
					}
				});
			}
			catch (Exception e)
			{
				Debug.error("error loading solution: " + name + " for clientnr: " + clientnr, e);
			}
		}
		return fs;
	}

	/**
	 * @param response
	 * @param solutionName
	 * @param solutionMetaData
	 * @throws IOException
	 */
	public static boolean solutionMissing(HttpServletResponse response, String solutionName, SolutionMetaData solutionMetaData) throws IOException
	{
		if (solutionMetaData == null)
		{
			Debug.error("Solution '" + solutionName + "' was not found.");
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			Writer w = response.getWriter();
			w.write(
				"<html><head><link rel=\"stylesheet\" href=\"/css/bootstrap/css/bootstrap.css\"/><link rel=\"stylesheet\" href=\"/css/servoy.css\"/></head><body><div style='padding:40px;'><div class=\"bs-callout bs-callout-danger\" ><h1>Page cannot be displayed</h1><p>Requested solution was not found.</p></div></div></body></html>");
			w.close();
			return true;
		}
		return false;
	}

	public static boolean applicationServerUnavailable(HttpServletResponse response, IApplicationServer as) throws IOException
	{
		if (as == null)
		{
			response.setStatus(SC_SERVICE_UNAVAILABLE);
			Writer w = response.getWriter();
			w.write(
				"<html><head><link rel=\"stylesheet\" href=\"/css/bootstrap/css/bootstrap.css\"/><link rel=\"stylesheet\" href=\"/css/servoy.css\"/></head><body><div style='padding:20px;color:#fd7100'><div class=\"bs-callout bs-callout-danger\"><p>System is inaccessible. Please contact your system administrator.</p></div></div></body></html>");
			w.close();
			return true;
		}

		return false;
	}

	/**
	 * Get the clientnr from parameter or an url /solutions/<solutionname>/<clientnr>/
	 *
	 */
	public static String getClientNr(String uri, ServletRequest request)
	{
		String clientnr = request.getParameter("clientnr");
		if (clientnr != null)
		{
			return clientnr;
		}


		int solutionIndex = uri.indexOf(SOLUTIONS_PATH);
		if (solutionIndex >= 0)
		{
			String[] parts = uri.substring(solutionIndex + SOLUTIONS_PATH.length()).split("/");
			if (parts.length >= 2 && parts[1].matches("[0-9]+"))
			{
				return parts[1];
			}
		}
		return null;
	}

	/**
	 * Get the ContentSecurityPolicyConfig is it should be applied, otherwise return null;
	 *
	 * Only when configured and when the browser is a modern browser that supports Content-Security-Policy level 3.
	 */
	public static ContentSecurityPolicyConfig getContentSecurityPolicyConfig(HttpServletRequest request)
	{
		Settings settings = Settings.getInstance();
		if (!getAsBoolean(settings.getProperty("servoy.ngclient.setContentSecurityPolicyHeader", "true")))
		{
			Debug.trace("ContentSecurityPolicyHeader is disabled by configuration");
			return null;
		}

		String userAgentHeader = request.getHeader("user-agent");

		if (!HCUtils.supportsContentSecurityPolicyLevel3(userAgentHeader))
		{
			if (Debug.tracing())
			{
				Debug.trace("ContentSecurityPolicyHeader is disabled, user agent '" + userAgentHeader + "' does not support ContentSecurityPolicy level 3");
			}
			return null;
		}

		ContentSecurityPolicyConfig contentSecurityPolicyConfig = new ContentSecurityPolicyConfig(HTTPUtils.getNonce(request));

		// Overridable directives
		setDirectiveOverride(contentSecurityPolicyConfig, "frame-src", settings);
		setDirectiveOverride(contentSecurityPolicyConfig, "style-src", settings);
		setDirectiveOverride(contentSecurityPolicyConfig, "img-src", settings);
		setDirectiveOverride(contentSecurityPolicyConfig, "font-src", settings);

		return contentSecurityPolicyConfig;

	}

	private static void setDirectiveOverride(ContentSecurityPolicyConfig contentSecurityPolicyConfig, String directive, Settings settings)
	{
		String override = settings.getProperty("servoy.ngclient.contentSecurityPolicy." + directive);
		if (override != null && override.trim().length() > 0 && override.indexOf(';') < 0)
		{
			contentSecurityPolicyConfig.setDirective(directive, override);
		}
	}
}
