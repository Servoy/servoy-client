/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2026 Servoy BV

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
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.SecureRandom;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.sablo.security.ContentSecurityPolicyConfig;
import org.sablo.util.HTTPUtils;

import com.servoy.base.util.ITagResolver;
import com.servoy.base.util.TagParser;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Solution.AUTHENTICATOR_TYPE;
import com.servoy.j2db.server.ngclient.AngularIndexPageWriter;
import com.servoy.j2db.server.ngclient.StatelessLoginHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author emera
 */
public abstract class AbstractAuthenticatorManager implements IAuthenticatorManager
{
	private static final SecureRandom secureRandom = new SecureRandom();

	protected final Solution solution;

	public AbstractAuthenticatorManager(Solution solution)
	{
		this.solution = solution;
	}

	public void writeLoginPage(HttpServletRequest request, HttpServletResponse response, String customHTML)
		throws ServletException, UnsupportedEncodingException, IOException
	{
		String id_token = HTMLWriter.getExistingIdToken(request);
		long nextLong = secureRandom.nextLong();

		if (request.getCharacterEncoding() == null) request.setCharacterEncoding("UTF8");
		HTTPUtils.setNoCacheHeaders(response);

		if (solution.getAuthenticator() == AUTHENTICATOR_TYPE.OAUTH)
		{
			OAuthHandler.redirectToOAuthLogin(request, response, solution);
			return;
		}

		if (solution.getAuthenticator() == AUTHENTICATOR_TYPE.OAUTH_AUTHENTICATOR)
		{
			OAuthHandler.redirectToAuthenticator(request, response, solution);
			return;
		}

		ContentSecurityPolicyConfig contentSecurityPolicyConfig = null;
		String loginHtml = null;
		if (solution.getAuthenticator() == AUTHENTICATOR_TYPE.SERVOY_CLOUD)
		{
			if (customHTML != null && customHTML.startsWith("<"))
			{
				HTMLWriter.writeHTML(request, response, customHTML);
				return;
			}
			else
			{
				loginHtml = CloudStatelessAccessManager.getCloudLoginPage(request, solution, loginHtml);
				contentSecurityPolicyConfig = CloudStatelessAccessManager.addcontentSecurityPolicyHeader(request, response);
			}
		}
		else
		{
			contentSecurityPolicyConfig = AngularIndexPageWriter.addcontentSecurityPolicyHeader(request, response, false);
		}
		loginHtml = getLoginHTML();
		if (solution != null)
		{
			Solution sol = solution;
			I18NTagResolver i18nProvider = new I18NTagResolver(request.getLocale(), sol);
			loginHtml = TagParser.processTags(loginHtml, new ITagResolver()
			{
				@Override
				public String getStringValue(String name)
				{
					if ("solutionTitle".equals(name))
					{
						String titleText = sol.getTitleText();
						if (titleText == null) titleText = sol.getName();
						return i18nProvider.getI18NMessageIfPrefixed(titleText);
					}
					return name;
				}
			}, i18nProvider);
		}

		//TODO this should be a separate method getScript that can be overridden by different authenticators
		StringBuilder sb = new StringBuilder();
		sb.append("<base href=\"");
		sb.append(HTMLWriter.getPath(request));
		sb.append("\">");
		if (request.getParameter(StatelessLoginHandler.ID_TOKEN) == null && request.getParameter(StatelessLoginHandler.USERNAME) == null)
		{
			//we check the local storage for the token or username only once (if both are null)
			sb.append("\n  	 <script type='text/javascript'>");
			sb.append("\n    window.addEventListener('load', () => { ");
			sb.append("\n     if (window.localStorage.getItem('servoy_id_token')) { ");

			//TODO autologin
			sb.append("\n    	document.body.style.display = 'none'; ");
			sb.append("\n    	document.login_form.action = 'index.html'; ");
			sb.append("\n  	    document.login_form.id_token.value = JSON.parse(window.localStorage.getItem('servoy_id_token'));  ");
			sb.append("\n  	    document.login_form.elements['csrf_token'].value = '" + Long.toString(nextLong) + "';");

			sb.append("\n    	document.login_form.remember.checked = true;  "); //TODO this is not needed for oauth types

			sb.append("\n    	document.login_form.submit(); ");
			sb.append("\n     } ");
			sb.append("\n     if (window.localStorage.getItem('servoy_username')) { ");
			sb.append("\n  	    document.login_form.username.value = JSON.parse(window.localStorage.getItem('servoy_username'));  ");
			sb.append("\n     } ");
			sb.append("\n   }) ");
			sb.append("\n  </script> ");
		}
		else if (!StringUtils.isBlank(id_token))
		{
			sb.append("\n  	 <script type='text/javascript'>");
			sb.append("\n    window.addEventListener('load', () => { ");
			sb.append("\n     window.localStorage.removeItem('servoy_id_token');");
			sb.append("\n   }) ");
			sb.append("\n  </script> ");
		}
		else if (!StringUtils.isBlank(request.getParameter(StatelessLoginHandler.USERNAME)))
		{
			sb.append("\n  	 <script type='text/javascript'>");
			sb.append("\n    window.addEventListener('load', () => { ");
			sb.append("\n  	    document.login_form.username.value = '");
			sb.append(StringEscapeUtils.escapeEcmaScript(request.getParameter(StatelessLoginHandler.USERNAME)));
			sb.append("'");
			sb.append("\n  	    if (document.getElementById('errorlabel')) document.getElementById('errorlabel').style.display='block';");
			sb.append("\n   }) ");
			sb.append("\n  </script> ");
		}

		loginHtml = loginHtml.replace("<base href=\"/\">", sb.toString());

		String requestLanguage = request.getHeader("accept-language");
		if (requestLanguage != null)
		{
			loginHtml = loginHtml.replace("lang=\"en\"", "lang=\"" + request.getLocale().getLanguage() + "\"");
		}

		String contentSecurityPolicyNonce = contentSecurityPolicyConfig != null ? contentSecurityPolicyConfig.getNonce() : null;
		if (contentSecurityPolicyNonce != null)
		{
			loginHtml = loginHtml.replaceAll("<script ", "<script nonce='" + contentSecurityPolicyNonce + "\' ");
			loginHtml = loginHtml.replaceAll("<style", "<style nonce='" + contentSecurityPolicyNonce + "\' ");
		}

		Cookie csrfCookie = new Cookie("csrf_token", Long.toString(nextLong));
		csrfCookie.setPath("/");
		csrfCookie.setHttpOnly(true);
		response.addCookie(csrfCookie);

		loginHtml = loginHtml.replaceAll("(?i)</form>", "<input type='hidden' name='csrf_token' value='" + nextLong + "'></form>");

		response.setCharacterEncoding("UTF-8");
		response.setContentType("text/html");
		response.setContentLengthLong(loginHtml.length());
		response.getWriter().write(loginHtml);
	}

	public String getLoginHTML() throws IOException
	{
		String loginHtml = null;
		if (solution != null)
		{
			Media media = solution.getMedia("login.html");
			if (media != null) loginHtml = new String(media.getMediaData(), Charset.forName("UTF-8"));
		}
		if (loginHtml == null)
		{
			try (InputStream rs = HTMLWriter.class.getResourceAsStream("login.html"))
			{
				loginHtml = IOUtils.toString(rs, Charset.forName("UTF-8"));
			}
		}
		return loginHtml;
	}
}
