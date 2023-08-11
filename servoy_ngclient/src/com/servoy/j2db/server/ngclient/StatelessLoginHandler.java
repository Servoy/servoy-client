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
import java.util.Date;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Settings;

/**
 * @author emera
 */
public class StatelessLoginHandler
{
	public static final String ID_TOKEN = "id_token"; //$NON-NLS-1$
	private static final String GROUPS = "groups"; //$NON-NLS-1$
	public static final String USER = "user"; //$NON-NLS-1$
	private static final String UID = "uid"; //$NON-NLS-1$
	private static final String JWT_Password = "jwt"; //$NON-NLS-1$
	private static final int TOKEN_AGE_IN_SECONDS = 24 * 3600;

	@SuppressWarnings("boxing")
	public static Pair<Boolean, String> mustAuthenticate(HttpServletRequest request, String solutionName)
		throws ServletException
	{
		Pair<Boolean, String> needToLogin = new Pair<>(Boolean.FALSE, null);
		String requestURI = request.getRequestURI();
		if (solutionName != null &&
			(!requestURI.contains("/designer") && (requestURI.endsWith("/") || requestURI.endsWith("/" + solutionName) ||
				requestURI.toLowerCase().endsWith("/index.html"))))
		{
			Pair<FlattenedSolution, Boolean> _fs = AngularIndexPageWriter.getFlattenedSolution(solutionName, null, request, null);
			FlattenedSolution fs = _fs.getLeft();

			try
			{
				needToLogin.setLeft(fs.getMainSolutionMetaData().getMustAuthenticate() && fs.getSolution().getLoginFormID() == 0 &&
					fs.getSolution().getLoginSolutionName() == null);
			}
			catch (RepositoryException e)
			{
				throw new ServletException(e);
			}
		}
		if (needToLogin.getLeft())
		{
			String user = request.getParameter(USER);
			String password = request.getParameter("password");
			if (user != null && password != null)
			{
				checkUser(user, password, needToLogin);
				if (!needToLogin.getLeft()) return needToLogin;
			}

			String id_token = request.getParameter(ID_TOKEN) != null ? request.getParameter(ID_TOKEN) : (String)request.getSession().getAttribute(ID_TOKEN);
			if (id_token != null)
			{
				Properties settings = ApplicationServerRegistry.get().getServerAccess().getSettings();
				JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(settings.getProperty(JWT_Password)))
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
						DecodedJWT decodedJWT = JWT.decode(id_token);
						if (decodedJWT.getClaims().containsKey(USER) && decodedJWT.getClaims().containsKey(UID) &&
							decodedJWT.getClaims().containsKey(GROUPS))
						{
							String _user = decodedJWT.getClaim(USER).toString();
							String _uid = decodedJWT.getClaim(UID).asString();
							String[] _groups = decodedJWT.getClaim(GROUPS).asArray(String.class);
							try
							{
								id_token = createToken(_user, _uid, _groups);
								needToLogin.setLeft(Boolean.FALSE);
								needToLogin.setRight(id_token);
								return needToLogin;
							}
							catch (Exception e)
							{
								throw new ServletException(e.getMessage());
							}
						}
					}
				}
			}
		}
		return needToLogin;
	}

	private static void checkUser(String user, String password, Pair<Boolean, String> needToLogin)
	{
		String uid;
		uid = ApplicationServerRegistry.get().checkDefaultServoyAuthorisation(user, password);
		if (uid != null)
		{
			String token = null;
			try
			{
				String clientid = ApplicationServerRegistry.get().getClientId();
				String[] groups = ApplicationServerRegistry.get().getUserManager().getUserGroups(clientid, uid);
				Properties settings = ApplicationServerRegistry.get().getServerAccess().getSettings();
				if (settings.getProperty(JWT_Password) == null)
				{
					settings.put(JWT_Password, "pwd" + Math.random());
				}
				token = createToken(user, uid, groups);
			}
			catch (Exception e)
			{
				Debug.error(e);
			}

			if (token != null)
			{
				needToLogin.setLeft(Boolean.FALSE);
				needToLogin.setRight(token);
				return;
			}
		}
		needToLogin.setLeft(Boolean.TRUE);
		needToLogin.setRight(null);
	}


	public static String createToken(String user, String uid, String[] groups)
		throws Exception
	{
		String token;
		Properties settings = ApplicationServerRegistry.get().getServerAccess().getSettings();
		Algorithm algorithm = Algorithm.HMAC256(settings.getProperty(JWT_Password));
		token = JWT.create()
			.withIssuer("svy")
			.withClaim(UID, uid)
			.withClaim(USER, user)
			.withArrayClaim(GROUPS, groups)
			.withExpiresAt(new Date(System.currentTimeMillis() + TOKEN_AGE_IN_SECONDS * 1000))
			.sign(algorithm);
		return token;
	}

	@SuppressWarnings("nls")
	public static void writeLoginPage(HttpServletRequest request, HttpServletResponse response, String solutionName)
		throws IOException
	{
		if (request.getCharacterEncoding() == null) request.setCharacterEncoding("UTF8");
		String loginHtml = null;
		try (InputStream rs = StatelessLoginHandler.class.getResourceAsStream("login.html"))
		{
			if (rs != null)
			{
				loginHtml = IOUtils.toString(rs, Charset.forName("UTF-8"));
			}
		}
		final String path = Settings.getInstance().getProperty("servoy.context.path", request.getContextPath() + '/');
		StringBuilder sb = new StringBuilder();
		sb.append("<base href=\"");
		sb.append(path);
		sb.append("\">");
		sb.append("\n  <title>Login</title>");
		if (request.getParameter("user") == null || request.getParameter("id_token") == null)
		{
			sb.append("\n  	 <script type='text/javascript'>");
			sb.append("\n    window.addEventListener('load', () => { ");
			if (request.getParameter("id_token") == null && request.getParameter("user") == null)
			{
				//we check the local storage for the token only once (if both are null)
				sb.append("\n     if (window.localStorage.getItem('servoy_token')) { ");
				sb.append("\n    	document.body.style.display = 'none'; ");
				sb.append("\n  	    document.login_form.id_token.value = JSON.parse(window.localStorage.getItem('servoy_token'));  ");
				sb.append("\n    	document.login_form.remember.checked = true;  ");
				sb.append("\n    	document.login_form.submit(); ");
				sb.append("\n     } ");
			}
			else
			{
				sb.append("\n     if (window.localStorage.getItem('servoy_username')) { ");
				sb.append("\n  	    document.login_form.user.value = JSON.parse(window.localStorage.getItem('servoy_username'));  ");
				sb.append("\n     } ");
			}
			sb.append("\n   }) ");
			sb.append("\n  </script> ");
		}
		loginHtml = loginHtml.replace("<base href=\"/\">", sb.toString());

		String requestLanguage = request.getHeader("accept-language");
		if (requestLanguage != null)
		{
			loginHtml = loginHtml.replace("lang=\"en\"", "lang=\"" + request.getLocale().getLanguage() + "\"");
		}

		loginHtml = loginHtml.replace("solutionName", solutionName);
		response.setCharacterEncoding("UTF-8");
		response.setContentType("text/html");
		response.setContentLengthLong(loginHtml.length());
		response.getWriter().write(loginHtml);
		return;
	}
}