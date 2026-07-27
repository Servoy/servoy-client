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
import com.servoy.j2db.server.ngclient.AngularIndexPageWriter;
import com.servoy.j2db.server.ngclient.StatelessLoginHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Abstract base class for authenticator managers. Provides common login page rendering logic
 * including HTML loading, i18n tag processing, CSP nonce injection, CSRF token handling,
 * and login scripts generation.
 *
 * Subclasses override getLoginHTML(), getLoginScripts(), and addContentSecurityPolicyHeader()
 * to customize behavior for their authenticator type.
 *
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

	@Override
	public void writeLoginPage(HttpServletRequest request, HttpServletResponse response, String customHTML)
		throws ServletException, UnsupportedEncodingException, IOException
	{
		if (request.getCharacterEncoding() == null) request.setCharacterEncoding("UTF8");
		HTTPUtils.setNoCacheHeaders(response);

		String loginHtml = getLoginHTML(request, customHTML);
		if (loginHtml == null)
		{
			return;
		}

		long csrfToken = secureRandom.nextLong();

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

		String scripts = getLoginScripts(request, csrfToken);
		loginHtml = loginHtml.replace("<base href=\"/\">", scripts);
		String loaderHtml = "<div id='servoy_loader'><div class='spinner'></div></div>";
		loginHtml = loginHtml.replaceFirst("(?i)<body[^>]*+>", "$0" + loaderHtml);

		String requestLanguage = request.getHeader("accept-language");
		if (requestLanguage != null)
		{
			loginHtml = loginHtml.replace("lang=\"en\"", "lang=\"" + request.getLocale().getLanguage() + "\"");
		}

		ContentSecurityPolicyConfig contentSecurityPolicyConfig = addContentSecurityPolicyHeader(request, response);
		loginHtml = loginHtml.replaceAll("(?i)</form>", "<input type='hidden' name='csrf_token' value='" + csrfToken + "'></form>");

		writeSecuredHtmlResponse(request, response, loginHtml, csrfToken, contentSecurityPolicyConfig);
	}

	protected String getLoginHTML(HttpServletRequest request, String customHTML) throws IOException
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

	/**
	 * Get the login scripts to inject into the login page.
	 * Subclasses can override to provide different scripts (e.g. OAuth auto-redirect).
	 */
	@Override
	public String getLoginScripts(HttpServletRequest request, long csrfToken)
	{
		String id_token = HTMLWriter.getExistingIdToken(request);

		StringBuilder sb = new StringBuilder();
		sb.append("<base href=\"");
		sb.append(HTMLWriter.getPath(request));
		sb.append("\">");
		sb.append("\n    <style>");
		sb.append(
			"\n      #servoy_loader { position: fixed; top: 0; left: 0; width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; background: white; z-index: 9999; }");
		sb.append(
			"\n      .spinner { border: 4px solid #f3f3f3; border-top: 4px solid #3498db; border-radius: 50%; width: 40px; height: 40px; animation: spin 1s linear infinite; }");
		sb.append("\n      @keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }");
		sb.append("\n      form { display: none !important; }");
		sb.append("\n    </style>");

		String scriptInit = "\n  <script type='text/javascript'>" +
			"\n    window.addEventListener('load', () => { " +
			"\n      const forms = document.querySelectorAll('form');" +
			"\n      const loader = document.getElementById('servoy_loader');" +
			"\n      const show = () => { if(loader) loader.style.display='none'; forms.forEach(f => f.style.setProperty('display', 'block', 'important')); };";

		sb.append(scriptInit);

		if (request.getParameter(StatelessLoginHandler.ID_TOKEN) == null && request.getParameter(StatelessLoginHandler.USERNAME) == null)
		{
			//we check the local storage for the token or username only once (if both are null)
			sb.append("\n     if (window.localStorage.getItem('servoy_id_token')) { ");
			sb.append("\n    	document.login_form.action = 'index.html'; ");
			sb.append("\n  	    document.login_form.id_token.value = JSON.parse(window.localStorage.getItem('servoy_id_token'));  ");
			sb.append("\n  	    document.login_form.elements['csrf_token'].value = '" + Long.toString(csrfToken) + "';");
			sb.append("\n    	document.login_form.remember.checked = true;  ");
			sb.append("\n    	document.login_form.submit(); ");
			sb.append("\n     } else { ");
			sb.append("\n        if(loader) loader.style.display = 'none';");
			sb.append("\n        forms.forEach(f => f.style.setProperty('display', 'block', 'important')); ");
			sb.append("\n        if (window.localStorage.getItem('servoy_username')) { ");
			sb.append("\n  	       document.login_form.username.value = JSON.parse(window.localStorage.getItem('servoy_username'));  ");
			sb.append("\n        } ");
			sb.append("\n     } ");
			sb.append("\n   }) ");
			sb.append("\n  </script> ");
		}
		else if (!StringUtils.isBlank(id_token))
		{
			sb.append("\n     window.localStorage.removeItem('servoy_id_token');");
			sb.append("\n     if(loader) loader.style.display = 'none';");
			sb.append("\n     forms.forEach(f => f.style.setProperty('display', 'block', 'important')); ");
			sb.append("\n   }) ");
			sb.append("\n  </script> ");
		}
		else if (!StringUtils.isBlank(request.getParameter(StatelessLoginHandler.USERNAME)))
		{
			sb.append("\n     document.login_form.username.value = '" + StringEscapeUtils.escapeEcmaScript(request.getParameter(StatelessLoginHandler.USERNAME)) + "';");
			sb.append("\n     if (document.getElementById('errorlabel')) document.getElementById('errorlabel').style.display='block';");
			sb.append("\n     if(loader) loader.style.display = 'none';");
			sb.append("\n     forms.forEach(f => f.style.setProperty('display', 'block', 'important')); ");
			sb.append("\n   }) ");
			sb.append("\n  </script> ");
		}
		else
		{
			sb.append("\n      show();");
			sb.append("\n   }) ");
			sb.append("\n  </script> ");
		}

		return sb.toString();
	}

	/**
	 * Add the Content-Security-Policy header. Subclasses can override to customize CSP directives
	 * (e.g. CloudStatelessAccessManager adds the cloud URL).
	 */
	protected ContentSecurityPolicyConfig addContentSecurityPolicyHeader(HttpServletRequest request, HttpServletResponse response)
	{
		return AngularIndexPageWriter.addcontentSecurityPolicyHeader(request, response, false);
	}

	protected static void writeSecuredHtmlResponse(HttpServletRequest request, HttpServletResponse response, String html, long csrfToken,
		ContentSecurityPolicyConfig contentSecurityPolicyConfig) throws IOException
	{
		String result = html;
		String contentSecurityPolicyNonce = contentSecurityPolicyConfig != null ? contentSecurityPolicyConfig.getNonce() : null;
		if (contentSecurityPolicyNonce != null)
		{
			result = result.replaceAll("<script ", "<script nonce='" + contentSecurityPolicyNonce + "' ");
			result = result.replaceAll("<style", "<style nonce='" + contentSecurityPolicyNonce + "' ");
		}

		Cookie csrfCookie = new Cookie("csrf_token", Long.toString(csrfToken));
		csrfCookie.setPath("/");
		csrfCookie.setHttpOnly(true);
		csrfCookie.setSecure(request.isSecure());
		response.addCookie(csrfCookie);

		response.setCharacterEncoding("UTF-8");
		response.setContentType("text/html");
		response.setContentLengthLong(result.length());
		response.getWriter().write(result);
	}
}
