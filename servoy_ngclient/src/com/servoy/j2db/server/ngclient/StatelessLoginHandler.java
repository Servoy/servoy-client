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
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collections;
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
import com.servoy.j2db.server.ngclient.auth.AuthenticatorManagerCreator;
import com.servoy.j2db.server.ngclient.auth.HTMLWriter;
import com.servoy.j2db.server.ngclient.auth.IAuthenticatorManager;
import com.servoy.j2db.server.ngclient.auth.ITokenRevocable;
import com.servoy.j2db.server.ngclient.auth.LoginResult;
import com.servoy.j2db.server.ngclient.auth.OAuthUtils.OAuthParameters;
import com.servoy.j2db.server.ngclient.auth.StatelessLoginUtils;
import com.servoy.j2db.server.ngclient.auth.SvyID;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
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

	private static final SecureRandom secureRandom = new SecureRandom();

	@SuppressWarnings({ "boxing" })
	public static LoginResult mustAuthenticate(HttpServletRequest request, HttpServletResponse reponse, String solutionName)
		throws ServletException
	{
		LoginResult result = LoginResult.authenticated(null);
		String requestURI = request.getRequestURI();
		if (requestURI.contains("/designer")) return result;

		if (solutionName != null && (requestURI.endsWith("/") ||
			requestURI.endsWith("/" + solutionName) || requestURI.toLowerCase().endsWith("/index.html")))
		{
			Pair<FlattenedSolution, Boolean> _fs = AngularIndexPageWriter.getFlattenedSolution(solutionName, null, request, reponse);
			FlattenedSolution fs = _fs.getLeft();
			if (fs == null) return result;
			try
			{
				AUTHENTICATOR_TYPE authenticator = fs.getSolution().getAuthenticator();
				boolean needsLogin = authenticator != AUTHENTICATOR_TYPE.NONE && fs.getSolution().getLoginFormID() == null &&
					fs.getSolution().getLoginSolutionName() == null;
				if (needsLogin)
				{
					result = LoginResult.needsLogin();
					String user = request.getParameter(USERNAME);
					String password = request.getParameter(PASSWORD);
					if (!Utils.stringIsEmpty(user) && !Utils.stringIsEmpty(password))
					{
						checkUser(user, password, "on".equals(request.getParameter("remember")), null, result, fs.getSolution(), request, reponse);
						if (result.isAuthenticated()) return result;
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
									checkPermissions(user, password, true, svyID, result, fs.getSolution(), request);
								}
								else
								{
									// the id_token was in the session, so we already have a client and the token is not expired
									// => no need to check the permissions again
									result.setAuthenticated(true);
									result.setToken(id_token);
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
											checkUser(user, password, true, svyID, result, fs.getSolution(), request, reponse);
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
							result.setAuthenticated(false);
						}
					}
				}
			}
			catch (RepositoryException e)
			{
				throw new ServletException(e);
			}
		}
		return result;
	}

	private static void checkUser(String username, String password, boolean remember, SvyID oldToken, LoginResult result, Solution solution,
		HttpServletRequest request, HttpServletResponse response)
	{
		boolean verified = false;
		IAuthenticatorManager authenticatorManager = AuthenticatorManagerCreator.getAuthenticatorManager(solution);
		if (!authenticatorManager.requiresCSRFForCheckUser() || StatelessLoginUtils.checkCSRFToken(request))
		{
			verified = authenticatorManager.checkUser(username, password, remember, oldToken, result, request, response);
		}
		if (!verified)
		{
			String ip = request.getRemoteAddr();
			log.atWarn().log(() -> "Authentication failed for user '" + username + "' from " + ip);
			result.setAuthenticated(false);
			if (result.getToken() != null && !result.getToken().startsWith("<"))
			{
				result.setToken(null);
			}
		}
	}

	/**
	 * This method is similar to checkUser, except for the OAUTH authenticator when it only calls the authenticator (does not refresh the oauth provider token)
	 */
	private static void checkPermissions(String username, String password, boolean remember, SvyID oldToken, LoginResult result,
		Solution solution, HttpServletRequest request) throws ServletException
	{
		log.atInfo().log(() -> "Checking permissions for user " + username + " with authenticator " + solution.getAuthenticator().name());
		boolean verified = false;
		if (StatelessLoginUtils.checkCSRFToken(request))
		{
			IAuthenticatorManager authenticatorManager = AuthenticatorManagerCreator.getAuthenticatorManager(solution);
			verified = authenticatorManager.checkPermissions(username, password, remember, oldToken, result, request);
		}
		else
		{
			throw new ServletException("Access forbidden due to failed security validation");
		}
		if (!verified)
		{
			String ip = request.getRemoteAddr();
			log.atWarn().log(() -> "Authentication failed for user '" + username + "' from " + ip);
			result.setAuthenticated(false);
			if (result.getToken() != null && !result.getToken().startsWith("<"))
			{
				result.setToken(null);
			}
		}
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
			byte[] keyBytes = new byte[32];
			secureRandom.nextBytes(keyBytes);
			settings.put(StatelessLoginUtils.JWT_Password, Base64.getEncoder().encodeToString(keyBytes));
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
				IAuthenticatorManager authenticatorManager = AuthenticatorManagerCreator.getAuthenticatorManager(solution);
				if (authenticatorManager instanceof ITokenRevocable tokenRevocable)
				{
					tokenRevocable.logoutAndRevokeToken(solution, jwt);
				}
			}
		}
	}

	public static void writeLoginPage(HttpServletRequest request, HttpServletResponse response, String solutionName, LoginResult loginResult)
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
		authenticatorManager.writeLoginPage(request, response, loginResult);
	}
}
