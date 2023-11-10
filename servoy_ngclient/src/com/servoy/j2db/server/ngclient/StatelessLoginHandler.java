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
import java.rmi.RemoteException;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.servoy.base.util.I18NProvider;
import com.servoy.base.util.ITagResolver;
import com.servoy.base.util.TagParser;
import com.servoy.j2db.ClientLogin;
import com.servoy.j2db.Credentials;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Solution.AUTHENTICATOR_TYPE;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServer;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

/**
 * @author emera
 */
@SuppressWarnings("nls")
public class StatelessLoginHandler
{
	public static final String ID_TOKEN = "id_token";
	public static final String GROUPS = "groups";
	public static final String USERNAME = "username";
	public static final String UID = "uid";
	private static final String JWT_Password = "servoy.jwt.logintoken.password";
	private static final int TOKEN_AGE_IN_SECONDS = 24 * 3600;

	public static final String CLOUD_URL = "https://middleware-dev.unifiedui.servoy-cloud.eu/servoy-service/rest_ws/api/login_auth/validateAuthUser";

	@SuppressWarnings({ "boxing" })
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
				AUTHENTICATOR_TYPE authenticator = fs.getSolution().getAuthenticator();
				needToLogin.setLeft(authenticator != AUTHENTICATOR_TYPE.NONE && fs.getSolution().getLoginFormID() == 0 &&
					fs.getSolution().getLoginSolutionName() == null);
				if (needToLogin.getLeft())
				{
					String user = request.getParameter(USERNAME);
					String password = request.getParameter("password");
					if (user != null && password != null)
					{
						checkUser(user, password, needToLogin, fs.getSolution());
						if (!needToLogin.getLeft()) return needToLogin;
					}

					String id_token = request.getParameter(ID_TOKEN) != null ? request.getParameter(ID_TOKEN)
						: (String)request.getSession().getAttribute(ID_TOKEN);
					if (!Utils.stringIsEmpty(id_token))
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
								if (decodedJWT.getClaims().containsKey(USERNAME) && decodedJWT.getClaims().containsKey(UID) &&
									decodedJWT.getClaims().containsKey(GROUPS))
								{
									String _user = decodedJWT.getClaim(USERNAME).toString();
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
			}
			catch (RepositoryException e)
			{
				throw new ServletException(e);
			}
		}
		return needToLogin;
	}

	private static void checkUser(String username, String password, Pair<Boolean, String> needToLogin, Solution solution)
	{
		if (solution.getAuthenticator() == AUTHENTICATOR_TYPE.SERVOY_CLOUD)
		{
			CloseableHttpClient httpclient = HttpClients.createDefault();
			HttpGet httpget = new HttpGet(CLOUD_URL);

			String auth = username + ":" + password;
			byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("ISO-8859-1")));
			String authHeader = "Basic " + new String(encodedAuth);
			httpget.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
			httpget.addHeader(HttpHeaders.ACCEPT, "application/json");
			httpget.addHeader("uuid", solution.getUUID().toString());

			try
			{
				String[] permissions = httpclient.execute(httpget, new HttpClientResponseHandler<String[]>()
				{

					@Override
					public String[] handleResponse(ClassicHttpResponse response) throws HttpException, IOException
					{
						HttpEntity responseEntity = response.getEntity();
						String responseString = EntityUtils.toString(responseEntity);
						if (response.getCode() == HttpStatus.SC_OK)
						{

							JSONObject loginTokenJSON = new JSONObject(responseString);
							JSONArray permissionsArray = loginTokenJSON.getJSONArray("permissions");
							if (permissionsArray != null)
							{
								String[] prmsns = new String[permissionsArray.length()];
								for (int i = 0; i < prmsns.length; i++)
								{
									prmsns[i] = permissionsArray.getString(i);
								}
								return prmsns;
							}
							return null;
						}
						else
						{
							Debug.error("could not login the user because the response to servoycloud had an error: " + response.getCode() + " " +
								response.getReasonPhrase());
							return null;
						}
					}
				});
				if (permissions != null)
				{
					String token = createToken(username, username, permissions);
					needToLogin.setLeft(Boolean.FALSE);
					needToLogin.setRight(token);
					return;
				}
			}
			catch (IOException e)
			{
				Debug.error("Can't validate user with the Servoy Cloud", e);
			}

		}
		else if (solution.getAuthenticator() == AUTHENTICATOR_TYPE.AUTHENTICATOR)
		{
			String modulesNames = solution.getModulesNames();
			IRepository localRepository = ApplicationServerRegistry.get().getLocalRepository();
			Solution authenticator = null;
			for (String moduleName : Utils.getTokenElements(modulesNames, ",", true))
			{
				try
				{
					Solution module = (Solution)localRepository.getActiveRootObject(moduleName, IRepository.SOLUTIONS);
					if (module.getSolutionType() == SolutionMetaData.AUTHENTICATOR)
					{
						authenticator = module;
						break;
					}
				}
				catch (RemoteException | RepositoryException e)
				{
					Debug.error(e);
				}
			}
			if (authenticator != null)
			{
				JSONObject json = new JSONObject();
				json.put("username", username);
				json.put("password", password);

				Credentials credentials = new Credentials(null, authenticator.getName(), null, json.toString());

				IApplicationServer applicationServer = ApplicationServerRegistry.getService(IApplicationServer.class);
				try
				{
					ClientLogin login = applicationServer.login(credentials);
					if (login != null)
					{
						String token = createToken(login.getUserName(), login.getUserUid(), login.getUserGroups());
						needToLogin.setLeft(Boolean.FALSE);
						needToLogin.setRight(token);
						return;
					}
				}
				catch (RemoteException | RepositoryException e)
				{
					Debug.error(e);
				}

			}
			else
			{
				Debug.error("Trying to login in solution " + solution.getName() +
					" with using an AUTHENCATOR solution, but the main solution doesn't have that as a module");
			}
		}
		else
		{
			String uid = ApplicationServerRegistry.get().checkDefaultServoyAuthorisation(username, password);
			if (uid != null)
			{
				try
				{
					String clientid = ApplicationServerRegistry.get().getClientId();
					String[] permissions = ApplicationServerRegistry.get().getUserManager().getUserGroups(clientid, uid);
					String token = createToken(username, uid, permissions);
					needToLogin.setLeft(Boolean.FALSE);
					needToLogin.setRight(token);
					return;
				}
				catch (Exception e)
				{
					Debug.error(e);
				}

			}
		}
		needToLogin.setLeft(Boolean.TRUE);
		needToLogin.setRight(null);
	}


	public static String createToken(String username, String uid, String[] groups)
	{
		Properties settings = ApplicationServerRegistry.get().getServerAccess().getSettings();
		Algorithm algorithm = Algorithm.HMAC256(settings.getProperty(JWT_Password));
		return JWT.create()
			.withIssuer("svy")
			.withClaim(UID, uid)
			.withClaim(USERNAME, username)
			.withArrayClaim(GROUPS, groups)
			.withExpiresAt(new Date(System.currentTimeMillis() + TOKEN_AGE_IN_SECONDS * 1000))
			.sign(algorithm);
	}

	public static void writeLoginPage(HttpServletRequest request, HttpServletResponse response, String solutionName)
		throws IOException
	{
		if (request.getCharacterEncoding() == null) request.setCharacterEncoding("UTF8");
		Solution solution = null;
		try
		{
			solution = (Solution)ApplicationServerRegistry.get().getLocalRepository().getActiveRootObject(solutionName, IRepository.SOLUTIONS);
		}
		catch (RemoteException | RepositoryException e)
		{
			Debug.error("Can't load solution " + solutionName, e);
		}
		String loginHtml = null;
		if (solution != null)
		{
			Media media = solution.getMedia("login.html");
			if (media != null) loginHtml = new String(media.getMediaData(), Charset.forName("UTF-8"));
		}
		if (loginHtml == null)
		{
			try (InputStream rs = StatelessLoginHandler.class.getResourceAsStream("login.html"))
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

		final String path = Settings.getInstance().getProperty("servoy.context.path", request.getContextPath() + '/');
		StringBuilder sb = new StringBuilder();
		sb.append("<base href=\"");
		sb.append(path);
		sb.append("\">");
		sb.append("\n  <title>Login</title>");
		if (request.getParameter(USERNAME) == null || request.getParameter(ID_TOKEN) == null)
		{
			sb.append("\n  	 <script type='text/javascript'>");
			sb.append("\n    window.addEventListener('load', () => { ");
			if (request.getParameter(ID_TOKEN) == null && request.getParameter(USERNAME) == null)
			{
				//we check the local storage for the token only once (if both are null)
				sb.append("\n     if (window.localStorage.getItem('servoy_id_token')) { ");
				sb.append("\n    	document.body.style.display = 'none'; ");
				sb.append("\n  	    document.login_form.id_token.value = JSON.parse(window.localStorage.getItem('servoy_id_token'));  ");
				sb.append("\n    	document.login_form.remember.checked = true;  ");
				sb.append("\n    	document.login_form.submit(); ");
				sb.append("\n     } ");
			}
			else
			{
				if (request.getParameter(USERNAME) != null)
				{
					sb.append("\n  	    document.login_form.username.value = '");
					sb.append(request.getParameter(USERNAME));
					sb.append("'");
					sb.append("\n  	    if (document.getElementById('errorlabel')) document.getElementById('errorlabel').style.display='block';");
				}
				else
				{
					sb.append("\n     if (window.localStorage.getItem('servoy_username')) { ");
					sb.append("\n  	    document.login_form.username.value = JSON.parse(window.localStorage.getItem('servoy_username'));  ");
					sb.append("\n     } ");
				}
			}
			sb.append("\n   }) ");
			sb.append("\n  </script> ");
		}
		else if (request.getParameter(USERNAME) != null)
		{
			sb.append("\n  	    document.login_form.username.value = '");
			sb.append(request.getParameter(USERNAME));
			sb.append("'");
		}


		loginHtml = loginHtml.replace("<base href=\"/\">", sb.toString());

		String requestLanguage = request.getHeader("accept-language");
		if (requestLanguage != null)
		{
			loginHtml = loginHtml.replace("lang=\"en\"", "lang=\"" + request.getLocale().getLanguage() + "\"");
		}

		response.setCharacterEncoding("UTF-8");
		response.setContentType("text/html");
		response.setContentLengthLong(loginHtml.length());
		response.getWriter().write(loginHtml);
		return;
	}

	/**
	 *
	 */
	public static void init()
	{
		Settings settings = Settings.getInstance();
		if (settings.getProperty(JWT_Password) == null)
		{
			Debug.warn("A servoy property '" + JWT_Password + //$NON-NLS-1$
				"' is added the the servoy properties file, this needs to be the same over redeploys, so make sure to add this in the servoy.properties that is used to deploy the WAR"); //$NON-NLS-1$
			settings.put(JWT_Password, "pwd" + Math.random());
			try
			{
				settings.save();
			}
			catch (Exception e)
			{
				Debug.error("Error saving the settings class to store the JWT_Password", e); //$NON-NLS-1$
			}
		}
	}


	/**
	 * @author jcompagner
	 *
	 */
	private static final class I18NTagResolver implements I18NProvider
	{
		private final Locale locale;
		private final Solution sol;

		/**
		 * @param request
		 * @param sol
		 */
		private I18NTagResolver(Locale locale, Solution sol)
		{
			this.locale = locale;
			this.sol = sol;
		}

		@Override
		public String getI18NMessage(String i18nKey)
		{
			return AngularIndexPageWriter.getSolutionDefaultMessage(sol, locale, i18nKey);
		}

		@Override
		public String getI18NMessage(String i18nKey, String language, String country)
		{
			return getI18NMessage(i18nKey);
		}

		@Override
		public String getI18NMessage(String i18nKey, Object[] array)
		{
			return getI18NMessage(i18nKey);
		}

		@Override
		public String getI18NMessage(String i18nKey, Object[] array, String language, String country)
		{
			return getI18NMessage(i18nKey);
		}

		@Override
		public String getI18NMessageIfPrefixed(String key)
		{
			if (key != null && key.startsWith("i18n:")) //$NON-NLS-1$
			{
				return getI18NMessage(key.substring(5), null);
			}
			return key;
		}

		@Override
		public void setI18NMessage(String i18nKey, String value)
		{
		}
	}

}