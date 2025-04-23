/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2023 Servoy BV

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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.sablo.security.ContentSecurityPolicyConfig;
import org.sablo.util.HTTPUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.servoy.base.util.ITagResolver;
import com.servoy.base.util.TagParser;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Solution.AUTHENTICATOR_TYPE;
import com.servoy.j2db.server.ngclient.auth.AuthenticatorManager;
import com.servoy.j2db.server.ngclient.auth.CloudStatelessAccessManager;
import com.servoy.j2db.server.ngclient.auth.DefaultLoginManager;
import com.servoy.j2db.server.ngclient.auth.HTMLWriter;
import com.servoy.j2db.server.ngclient.auth.I18NTagResolver;
import com.servoy.j2db.server.ngclient.auth.OAuthHandler;
import com.servoy.j2db.server.ngclient.auth.OAuthUtils.OAuthParameters;
import com.servoy.j2db.server.ngclient.auth.StatelessLoginUtils;
import com.servoy.j2db.server.ngclient.auth.SvyID;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

/**
 * @author emera
 */
@SuppressWarnings("nls")
public class StatelessLoginHandler
{
	public static final Logger log = LoggerFactory.getLogger("stateless.login");

	public static final String REFRESH_TOKEN = "refresh_token";
	public static final String OAUTH_CUSTOM_PROPERTIES = "oauth";
	public static final String USERNAME = "username";
	public static final String PASSWORD = "password";
	public static final String ID_TOKEN = "id_token";

	private static final SecureRandom secureRandom = new SecureRandom();

	@SuppressWarnings({ "boxing" })
	public static Pair<Boolean, String> mustAuthenticate(HttpServletRequest request, HttpServletResponse reponse, String solutionName)
		throws ServletException
	{
		Pair<Boolean, String> needToLogin = new Pair<>(Boolean.FALSE, null);
		String requestURI = request.getRequestURI();
		if (requestURI.contains("/designer")) return needToLogin;

		if (solutionName != null && (requestURI.endsWith("/") ||
			requestURI.endsWith("/" + solutionName) || requestURI.toLowerCase().endsWith("/index.html")))
		{
			Pair<FlattenedSolution, Boolean> _fs = AngularIndexPageWriter.getFlattenedSolution(solutionName, null, request, reponse);
			FlattenedSolution fs = _fs.getLeft();
			if (fs == null) return needToLogin;
			try
			{
				AUTHENTICATOR_TYPE authenticator = fs.getSolution().getAuthenticator();
				needToLogin.setLeft(authenticator != AUTHENTICATOR_TYPE.NONE && fs.getSolution().getLoginFormID() <= 0 &&
					fs.getSolution().getLoginSolutionName() == null);
				if (needToLogin.getLeft())
				{
					String user = request.getParameter(USERNAME);
					String password = request.getParameter(PASSWORD);
					if (!Utils.stringIsEmpty(user) && !Utils.stringIsEmpty(password))
					{
						checkUser(user, password, "on".equals(request.getParameter("remember")), null, needToLogin, fs.getSolution(), request);
						if (!needToLogin.getLeft()) return needToLogin;
					}

					String id_token = HTMLWriter.getExistingIdToken(request);
					if (id_token != null)
					{
						SvyID svyID = new SvyID(id_token);
						Properties settings = ApplicationServerRegistry.get().getServerAccess().getSettings();
						JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(settings.getProperty(StatelessLoginUtils.JWT_Password)))
							.build();

						try
						{
							jwtVerifier.verify(id_token);
							needToLogin.setLeft(Boolean.FALSE);
							needToLogin.setRight(id_token);
						}
						catch (JWTVerificationException ex)
						{
							if (ex instanceof TokenExpiredException)
							{
								if (svyID.getUsername() != null && svyID.getUserID() != null && svyID.getPermissions() != null)
								{
									try
									{
										checkUser(user, password, true, svyID, needToLogin, fs.getSolution(), request);
									}
									catch (Exception e)
									{
										log.atInfo().setCause(e).log(() -> "Exception thrown when checking the user");
										throw new ServletException(e.getMessage(), e);
									}
								}
							}
						}
					}
				}
			}
			catch (RepositoryException e)
			{
				throw new ServletException(e);
			}
		}
		return needToLogin;
	}

	private static void checkUser(String username, String password, boolean remember, SvyID oldToken, Pair<Boolean, String> needToLogin, Solution solution,
		HttpServletRequest request)
	{
		boolean verified = false;
		if (solution.getAuthenticator() == AUTHENTICATOR_TYPE.SERVOY_CLOUD)
		{
			if (checkCSRFToken(request))
				verified = CloudStatelessAccessManager.checkCloudPermissions(username, password, remember, oldToken, needToLogin, solution, request);
		}
		else if (solution.getAuthenticator() == AUTHENTICATOR_TYPE.OAUTH)
		{
			verified = OAuthHandler.refreshOAuthTokenIfPossible(needToLogin, solution, oldToken, request);
		}
		else if (solution.getAuthenticator() == AUTHENTICATOR_TYPE.AUTHENTICATOR)
		{
			if (checkCSRFToken(request))
				verified = AuthenticatorManager.checkAuthenticatorPermissions(username, password, remember, oldToken, needToLogin, solution, request);
		}
		else
		{
			if (checkCSRFToken(request))
				verified = DefaultLoginManager.checkDefaultLoginPermissions(username, password, remember, oldToken, needToLogin);
		}
		if (!verified)
		{
			needToLogin.setLeft(Boolean.TRUE);
			if (needToLogin.getRight() != null && !needToLogin.getRight().startsWith("<"))
			{
				needToLogin.setRight(null);
			}
		}
	}

	/**
	 * @param request
	 */
	private static boolean checkCSRFToken(HttpServletRequest request)
	{
		String fieldToken = request.getParameter("csrf_token");
		Cookie[] cookies = request.getCookies();
		if (cookies == null || fieldToken == null)
		{
			// just return false here don't allow the login
			log.warn("no CSRF token (cookie or hidden field) in the request");
			return false;
		}

		Optional<Cookie> first = Arrays.asList(cookies).stream().filter(cookie -> "csrf_token".equals(cookie.getName())).findFirst();
		if (!first.isPresent())
		{
			log.warn("no CSRF cookie in the request");
			return false;
		}
		Cookie cookie = first.get();
		boolean match = fieldToken.equals(cookie.getValue());
		if (!match) log.warn("CSRF token mismatch, cookie: " + cookie.getValue() + " field: " + fieldToken);
		return match;
	}

	/**
	 *
	 */
	public static void init(ServletContext context)
	{
		Settings settings = Settings.getInstance();
		if (settings.getProperty(StatelessLoginUtils.JWT_Password) == null)
		{
			log.warn("A servoy property '" + StatelessLoginUtils.JWT_Password + //$NON-NLS-1$
				"' is added the the servoy properties file, this needs to be the same over redeploys, so make sure to add this in the servoy.properties that is used to deploy the WAR"); //$NON-NLS-1$
			settings.put(StatelessLoginUtils.JWT_Password, "pwd" + Math.random());
			try
			{
				settings.save();
			}
			catch (Exception e)
			{
				log.error("Error saving the settings class to store the JWT_Password", e); //$NON-NLS-1$
			}
		}
		context.setAttribute(OAuthParameters.nonce.name(), Collections.synchronizedMap(new PassiveExpiringMap<String, String>(30, TimeUnit.MINUTES)));
	}


	public static void logoutAndRevokeToken(HttpSession httpSession, Solution solution)
	{
		if (httpSession == null) return;
		httpSession.setAttribute("logout", true);
		String id_token = (String)httpSession.getAttribute(StatelessLoginHandler.ID_TOKEN);
		if (id_token != null)
		{
			DecodedJWT jwt = JWT.decode(id_token);
			if (jwt.getClaim(REFRESH_TOKEN).asString() != null)
			{
				AUTHENTICATOR_TYPE authenticator = solution.getAuthenticator();
				if (authenticator == AUTHENTICATOR_TYPE.OAUTH || authenticator == AUTHENTICATOR_TYPE.OAUTH_AUTHENTICATOR)
				{
					OAuthHandler.revokeToken(solution, jwt);
				}
				else if (authenticator == AUTHENTICATOR_TYPE.SERVOY_CLOUD)
				{
					CloudStatelessAccessManager.revokeToken(solution, jwt);
				}
			}
		}
	}

	public static void writeLoginPage(HttpServletRequest request, HttpServletResponse response, String solutionName, String customHTML)
		throws IOException, ServletException
	{
		if (request.getCharacterEncoding() == null) request.setCharacterEncoding("UTF8");
		HTTPUtils.setNoCacheHeaders(response);

		String id_token = HTMLWriter.getExistingIdToken(request);
		Solution solution = null;
		try
		{
			solution = (Solution)ApplicationServerRegistry.get().getLocalRepository().getActiveRootObject(solutionName, IRepository.SOLUTIONS);
		}
		catch (RepositoryException e)
		{
			log.error("Can't load solution " + solutionName, e);
			return;
		}

		if (solution == null)
		{
			log.error("The solution is null " + solutionName);
			return;
		}
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
		if (solution != null && loginHtml == null)
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

		StringBuilder sb = new StringBuilder();
		sb.append("<base href=\"");
		sb.append(HTMLWriter.getPath(request));
		sb.append("\">");
		if (request.getParameter(ID_TOKEN) == null && request.getParameter(USERNAME) == null)
		{
			//we check the local storage for the token or username only once (if both are null)
			sb.append("\n  	 <script type='text/javascript'>");
			sb.append("\n    window.addEventListener('load', () => { ");
			sb.append("\n     if (window.localStorage.getItem('servoy_id_token')) { ");
			sb.append("\n    	document.body.style.display = 'none'; ");
			sb.append("\n    	document.login_form.action = 'index.html'; ");
			sb.append("\n  	    document.login_form.id_token.value = JSON.parse(window.localStorage.getItem('servoy_id_token'));  ");
			sb.append("\n    	document.login_form.remember.checked = true;  ");
			sb.append("\n    	document.login_form.submit(); ");
			sb.append("\n     } ");
			sb.append("\n     if (window.localStorage.getItem('servoy_username')) { ");
			sb.append("\n  	    document.login_form.username.value = JSON.parse(window.localStorage.getItem('servoy_username'));  ");
			sb.append("\n     } ");
			sb.append("\n   }) ");
			sb.append("\n  </script> ");
		}
		else if (id_token != null)
		{
			sb.append("\n  	 <script type='text/javascript'>");
			sb.append("\n    window.addEventListener('load', () => { ");
			sb.append("\n     window.localStorage.removeItem('servoy_id_token');");
			sb.append("\n   }) ");
			sb.append("\n  </script> ");
		}
		else if (request.getParameter(USERNAME) != null)
		{
			sb.append("\n  	 <script type='text/javascript'>");
			sb.append("\n    window.addEventListener('load', () => { ");
			sb.append("\n  	    document.login_form.username.value = '");
			sb.append(StringEscapeUtils.escapeEcmaScript(request.getParameter(USERNAME)));
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

		long nextLong = secureRandom.nextLong();
		Cookie csrfCooke = new Cookie("csrf_token", Long.toString(nextLong));
		csrfCooke.setHttpOnly(true);
		response.addCookie(csrfCooke);

		loginHtml = loginHtml.replaceAll("(?i)</form>", "<input type='hidden' name='csrf_token' value='" + nextLong + "'></form>");

		response.setCharacterEncoding("UTF-8");
		response.setContentType("text/html");
		response.setContentLengthLong(loginHtml.length());
		response.getWriter().write(loginHtml);
		return;
	}
}