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
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Solution.AUTHENTICATOR_TYPE;
import com.servoy.j2db.server.ngclient.auth.AbstractAuthenticatorManager;
import com.servoy.j2db.server.ngclient.auth.AuthenticatorManagerCreator;
import com.servoy.j2db.server.ngclient.auth.CloudStatelessAccessManager;
import com.servoy.j2db.server.ngclient.auth.HTMLWriter;
import com.servoy.j2db.server.ngclient.auth.IAuthenticatorManager;
import com.servoy.j2db.server.ngclient.auth.OAuthHandler;
import com.servoy.j2db.server.ngclient.auth.OAuthUtils.OAuthParameters;
import com.servoy.j2db.server.ngclient.auth.StatelessLoginUtils;
import com.servoy.j2db.server.ngclient.auth.SvyID;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

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
				needToLogin.setLeft(authenticator != AUTHENTICATOR_TYPE.NONE && fs.getSolution().getLoginFormID() == null &&
					fs.getSolution().getLoginSolutionName() == null);
				if (needToLogin.getLeft())
				{
					String user = request.getParameter(USERNAME);
					String password = request.getParameter(PASSWORD);
					if (!Utils.stringIsEmpty(user) && !Utils.stringIsEmpty(password))
					{
						checkUser(user, password, "on".equals(request.getParameter("remember")), null, needToLogin, fs.getSolution(), request, reponse);
						if (!needToLogin.getLeft()) return needToLogin;
					}

					String id_token = HTMLWriter.getExistingIdToken(request);
					if (id_token != null)
					{
						try
						{
							SvyID svyID = new SvyID(id_token);
							Properties settings = ApplicationServerRegistry.get().getServerAccess().getSettings();
							JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(settings.getProperty(StatelessLoginUtils.JWT_Password)))
								.build();

							try
							{
								jwtVerifier.verify(id_token);
								if (request.getParameter(ID_TOKEN) != null)
								{
									checkPermissions(user, password, true, svyID, needToLogin, fs.getSolution(), request);
								}
								else
								{
									// the id_token was in the session, so we already have a client and the token is not expired
									// => no need to check the permissions again
									needToLogin.setLeft(Boolean.FALSE);
									needToLogin.setRight(id_token);
								}
							}
							catch (JWTVerificationException ex)
							{
								if (ex instanceof TokenExpiredException)
								{
									if (svyID.getUsername() != null && svyID.getUserID() != null && svyID.getPermissions() != null)
									{
										try
										{
											checkUser(user, password, true, svyID, needToLogin, fs.getSolution(), request, reponse);
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
						catch (JWTDecodeException e)
						{
							log.atError().setCause(e).log(() -> "Not a valid JWT format");
							needToLogin.setLeft(Boolean.TRUE);
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
		HttpServletRequest request, HttpServletResponse response)
	{
		boolean verified = false;
		if (solution.getAuthenticator() == AUTHENTICATOR_TYPE.OAUTH)
		{
			verified = OAuthHandler.refreshOAuthTokenIfPossible(needToLogin, solution, oldToken, request, response);
		}
		else if (checkCSRFToken(request))
		{
			AbstractAuthenticatorManager authenticatorManager = (AbstractAuthenticatorManager)AuthenticatorManagerCreator.getAuthenticatorManager(solution);
			verified = authenticatorManager.checkPermissions(username, password, remember, oldToken, needToLogin, request);
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
	 * This method is similar to checkUser, except for the OAUTH authenticator when it only calls the authenticator (does not refresh the oauth provider token)
	 * @param user
	 * @param password2
	 * @param b
	 * @param svyID
	 * @param needToLogin
	 * @param solution
	 * @param request
	 * @param reponse
	 * @throws ServletException
	 */
	private static void checkPermissions(String username, String password, boolean remember, SvyID oldToken, Pair<Boolean, String> needToLogin,
		Solution solution, HttpServletRequest request) throws ServletException
	{
		log.atInfo().log(() -> "Checking permissions for user " + username + " with authenticator " + solution.getAuthenticator().name());
		boolean verified = false;
		if (checkCSRFToken(request))
		{
			AbstractAuthenticatorManager authenticatorManager = (AbstractAuthenticatorManager)AuthenticatorManagerCreator.getAuthenticatorManager(solution);
			verified = authenticatorManager.checkPermissions(username, password, remember, oldToken, needToLogin, request);
		}
		else
		{
			throw new ServletException("Access forbidden due to failed security validation");
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
		log.atInfo().log(() -> "Checking CSRF token " + request.getParameter("csrf_token"));
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
		if (!match) log.atWarn().log(() -> "CSRF token mismatch, cookie: " + cookie.getValue() + " field: " + fieldToken);
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
			log.atWarn().log(() -> "A servoy property '" + StatelessLoginUtils.JWT_Password + //$NON-NLS-1$
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

		IAuthenticatorManager authenticatorManager = AuthenticatorManagerCreator.getAuthenticatorManager(solution);
		authenticatorManager.writeLoginPage(request, response, customHTML);
	}
}