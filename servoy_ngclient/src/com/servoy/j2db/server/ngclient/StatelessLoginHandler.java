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
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.net.URIBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sablo.util.HTTPUtils;

import com.auth0.jwk.InvalidPublicKeyException;
import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator.Builder;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.github.scribejava.apis.GoogleApi20;
import com.github.scribejava.apis.MicrosoftAzureActiveDirectory20Api;
import com.github.scribejava.apis.openid.OpenIdOAuth2AccessToken;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.oauth.OAuth20Service;
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
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

/**
 * @author emera
 */
@SuppressWarnings("nls")
public class StatelessLoginHandler
{
	private static final String REFRESH_TOKEN = "refresh_token";
	public static final String OAUTH_CUSTOM_PROPERTIES = "oauth";
	private static final String SVYLOGIN_PATH = "svylogin";
	public static final String PASSWORD = "password";
	public static final String ID_TOKEN = "id_token";
	public static final String PERMISSIONS = "permissions";
	public static final String USERNAME = "username";
	public static final String REMEMBER = "remember";
	public static final String UID = "uid";
	public static final String LAST_LOGIN = "last_login";
	private static final String JWT_Password = "servoy.jwt.logintoken.password";
	private static final int TOKEN_AGE_IN_SECONDS = 2 * 3600;

	private static final String BASE_CLOUD_URL = System.getProperty("servoy.api.url", "https://middleware-prod.unifiedui.servoy-cloud.eu");
	private static final String CLOUD_REST_API_GET = BASE_CLOUD_URL + "/servoy-service/rest_ws/api/auth_endpoint/getEndpointUI/";
	private static final String CLOUD_REST_API_POST = BASE_CLOUD_URL + "/servoy-service/rest_ws/api/auth_endpoint/submitForm/";
	public static final String CLOUD_URL = BASE_CLOUD_URL +
		"/servoy-service/rest_ws/api/login_auth/validateAuthUser";
	public static final String REFRESH_TOKEN_CLOUD_URL = BASE_CLOUD_URL +
		"/servoy-service/rest_ws/api/login_auth/refreshPermissions";
	public static final String CLOUD_OAUTH_URL = BASE_CLOUD_URL +
		"/servoy-service/rest_ws/api/login_auth/validateOAuthUser";

	//oauth constants
	private static final String NONCE = "nonce";
	public static final String JWKS_URI = "jwks_uri";
	public static final String ACCESS_TOKEN_ENDPOINT = "accessTokenEndpoint";
	public static final String REFRESH_TOKEN_ENDPOINT = "refreshTokenEndpoint";
	public static final String AUTHORIZATION_BASE_URL = "authorizationBaseUrl";
	public static final String TENANT = "tenant";
	public static final String OAUTH_API = "api";
	public static final String DEFAULT_SCOPE = "defaultScope";
	public static final String API_SECRET = "apiSecret";
	public static final String CLIENT_ID = "clientId";

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
						checkUser(user, password, needToLogin, fs.getSolution(), null, "on".equals(request.getParameter(REMEMBER)), request);
						if (!needToLogin.getLeft()) return needToLogin;
					}
					if ((request.getParameter("id_token") != null || request.getParameter("code") != null) &&
						(authenticator == AUTHENTICATOR_TYPE.OAUTH || authenticator == AUTHENTICATOR_TYPE.SERVOY_CLOUD))
					{
						String id_token = null;
						String refreshToken = null;
						if (request.getParameter("code") != null)
						{
							String nonceState = request.getParameter("state");
							JSONObject auth = getNonce(request.getServletContext(), nonceState);
							OAuth20Service service = createOauthService(request, auth, new HashMap<>());
							try
							{
								OpenIdOAuth2AccessToken accessToken = (OpenIdOAuth2AccessToken)service.getAccessToken(request.getParameter("code"));
								refreshToken = accessToken.getRefreshToken();
								id_token = accessToken.getOpenIdToken();
							}
							catch (Exception e)
							{
								Debug.error("Could not get the id and refresh tokens.");
							}
						}
						else
						{
							id_token = request.getParameter("id_token");
						}

						if (!Utils.stringIsEmpty(id_token))
						{
							DecodedJWT decodedJWT = JWT.decode(id_token);
							if (refreshToken == null)
							{
								refreshToken = decodedJWT.getClaim(REFRESH_TOKEN).asString();
							}
							if (checkOauthIdToken(needToLogin, fs.getSolution(), authenticator, decodedJWT, request, refreshToken, true))
							{
								return needToLogin;
							}
						}
					}

					String id_token = getExistingIdToken(request);
					if (id_token != null)
					{
						DecodedJWT decodedJWT = JWT.decode(id_token);
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
								if (decodedJWT.getClaims().containsKey(USERNAME) && decodedJWT.getClaims().containsKey(UID) &&
									decodedJWT.getClaims().containsKey(PERMISSIONS))
								{
									String _user = decodedJWT.getClaim(USERNAME).asString();
									Boolean rememberUser = decodedJWT.getClaims().containsKey(REMEMBER) ? //
										decodedJWT.getClaim(REMEMBER).asBoolean() : Boolean.FALSE;
									try
									{
										checkUser(_user, null, needToLogin, fs.getSolution(), decodedJWT, rememberUser, request);
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
			catch (MalformedURLException e)
			{
				throw new ServletException(e);
			}
		}
		return needToLogin;
	}

	private static boolean checkOauthIdToken(Pair<Boolean, String> needToLogin, Solution solution, AUTHENTICATOR_TYPE authenticator,
		DecodedJWT decodedJWT, HttpServletRequest request, String refreshToken, boolean checkNonce) throws MalformedURLException
	{
		if (!"svy".equals(decodedJWT.getIssuer()))
		{
			JSONObject auth = null;
			if (checkNonce)
			{
				//if token was refreshed it does not have nonce // TODO check
				String tokenNonce = decodedJWT.getClaim(NONCE).asString();
				auth = checkNonce(request.getServletContext(), tokenNonce);
				if (auth == null)
				{
					Debug.error("The token was replayed or tampered with.");
					return false;
				}
			}
			if (auth.has(JWKS_URI))
			{
				try
				{
					String jwks_uri = auth.getString(JWKS_URI);
					final JwkProvider jwkStore = new UrlJwkProvider(new URL(jwks_uri));
					if (decodedJWT.getKeyId() == null)
					{
						Debug.error("Cannot verify the token with jwks '" + jwks_uri //
							+ "' because the key id is missing in the token header.");
					}
					Algorithm algorithm = getAlgo(decodedJWT, jwkStore);
					JWTVerifier verifier = JWT.require(algorithm).build();
					try
					{
						verifier.verify(decodedJWT);
						Boolean remember = Boolean.valueOf("offline".equals(auth.optString("access_type")));
						String payload = new String(java.util.Base64.getUrlDecoder().decode(decodedJWT.getPayload()));

						if (authenticator == AUTHENTICATOR_TYPE.OAUTH)
						{
							JSONObject token = new JSONObject();
							JSONObject jsonObject = new JSONObject(payload);
							token.put(LAST_LOGIN, jsonObject);
							Solution authenticatorModule = findAuthenticator(solution);
							if (authenticatorModule != null)
							{
								return callAuthenticator(needToLogin, remember, authenticatorModule, token, refreshToken);
							}
							else
							{
								Debug.error("Trying to login in solution " + solution.getName() +
									" with using an AUTHENTICATOR solution, but the main solution doesn't have that as a module");
							}
						}
						else if (authenticator == AUTHENTICATOR_TYPE.SERVOY_CLOUD)
						{
							return checkCloudOAuthPermissions(needToLogin, solution, payload, remember);
						}
					}
					catch (Exception e)
					{
						Debug.error(e);
					}
				}
				catch (JwkException e)
				{
					Debug.error("Cannot verify the id_token", e);
				}
			}
		}

		return false;

	}

	private static Algorithm getAlgo(DecodedJWT decodedJWT, final JwkProvider jwkStore) throws JwkException, InvalidPublicKeyException
	{
		Jwk jwk = jwkStore.get(decodedJWT.getKeyId());
		String algo = decodedJWT.getAlgorithm();
		PublicKey publicKey = jwk.getPublicKey();
		switch (algo)
		{
			case "RS256" :
				return Algorithm.RSA256((RSAPublicKey)publicKey, null);
			case "RS384" :
				return Algorithm.RSA384((RSAPublicKey)publicKey, null);
			case "RS512" :
				return Algorithm.RSA512((RSAPublicKey)publicKey, null);
			case "ES256" :
				return Algorithm.ECDSA256((ECPublicKey)publicKey, null);
			case "ES384" :
				return Algorithm.ECDSA384((ECPublicKey)publicKey, null);
			case "ES512" :
				return Algorithm.ECDSA512((ECPublicKey)publicKey, null);
		}
		return null;
	}

	public static boolean handlePossibleCloudRequest(HttpServletRequest request, HttpServletResponse response, String solutionName) throws ServletException
	{
		Path path = Paths.get(request.getRequestURI()).normalize();
		if (solutionName != null && path.getNameCount() > 2 && SVYLOGIN_PATH.equals(path.getName(2).toString()))
		{
			Pair<FlattenedSolution, Boolean> _fs = AngularIndexPageWriter.getFlattenedSolution(solutionName, null, request, response);
			FlattenedSolution fs = _fs.getLeft();
			try
			{
				if (fs.getSolution().getAuthenticator() == AUTHENTICATOR_TYPE.SERVOY_CLOUD)
				{
					try (CloseableHttpClient httpclient = HttpClients.createDefault())
					{
						Solution solution = fs.getSolution();
						Pair<Integer, JSONObject> res = null;
						String[] endpoints = getCloudRestApiEndpoints(request.getServletContext(), httpclient, solution);
						if (endpoints != null)
						{
							String endpoint = path.getName(path.getNameCount() - 1).toString().replace(".html", "");
							if (Arrays.asList(endpoints).contains(endpoint))
							{
								if ("POST".equalsIgnoreCase(request.getMethod()))
								{
									res = executeCloudPostRequest(httpclient, solution, endpoint, request);
								}
								else
								{
									res = executeCloudGetRequest(httpclient, solution, endpoint, request);
								}

								if (res != null)
								{
									writeResponse(request, response, solution, res);
									return true;
								}
							}
						}
					}
					catch (IOException e)
					{
						Debug.error("Can't access the Servoy Cloud api", e);
					}
				}
			}
			catch (Exception e)
			{
				throw new ServletException(e.getMessage());
			}
		}
		return false;

	}

	private static void writeResponse(HttpServletRequest request, HttpServletResponse response, Solution solution, Pair<Integer, JSONObject> res)
		throws IOException, UnsupportedEncodingException
	{
		String html = null;
		int status = res.getLeft().intValue();
		JSONObject json = res.getRight();
		if (json != null)
		{
			if (status == HttpStatus.SC_OK && json.has("html"))
			{
				html = json.getString("html");
			}
			else if (json.has("error"))
			{
				String error = json.optString("error", "");
				if (error.startsWith("<html>"))
				{
					html = error;
				}
				else
				{
					try (InputStream rs = StatelessLoginHandler.class.getResourceAsStream("error.html"))
					{
						html = IOUtils.toString(rs, Charset.forName("UTF-8"));
					}
					if (solution != null)
					{
						Solution sol = solution;
						I18NTagResolver i18nProvider = new I18NTagResolver(request.getLocale(), sol);
						html = TagParser.processTags(html, new ITagResolver()
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
								if ("error".equals(name))
								{
									return i18nProvider.getI18NMessageIfPrefixed(json.getString("error"));
								}
								return name;
							}
						}, null);
					}
				}
			}
			else if (json.has("oauth"))
			{
				// this is an oauth request
				JSONObject oauth = json.getJSONObject("oauth");
				generateOauthCall(request, response, oauth);
			}
			if (html != null)
			{
				if (request.getCharacterEncoding() == null) request.setCharacterEncoding("UTF8");
				StringBuilder sb = new StringBuilder();
				sb.append("<base href=\"");
				sb.append(getPath(request));
				sb.append("\">");
				html = html.replace("<base href=\"/\">", sb.toString());

				String requestLanguage = request.getHeader("accept-language");
				if (requestLanguage != null)
				{
					html = html.replace("lang=\"en\"", "lang=\"" + request.getLocale().getLanguage() + "\"");
				}

				response.setCharacterEncoding("UTF-8");
				response.setContentType("text/html");
				response.setContentLengthLong(html.length());
				response.getWriter().write(html);
			}
		}
	}

	private static Pair<Integer, JSONObject> executeCloudPostRequest(CloseableHttpClient httpclient, Solution solution, String endpoint,
		HttpServletRequest request)
	{
		HttpPost httppost = new HttpPost(CLOUD_REST_API_POST + endpoint);
		httppost.addHeader(HttpHeaders.ACCEPT, "application/json");
		httppost.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
		httppost.addHeader("uuid", sanitizeHeader(solution.getUUID().toString()));
		JSONObject postParameters = new JSONObject();
		Map<String, String[]> parameters = request.getParameterMap();
		for (Map.Entry<String, String[]> entry : parameters.entrySet())
		{
			String[] values = entry.getValue();
			for (String value : values)
			{
				postParameters.put(entry.getKey(), value);
			}
		}
		postParameters.put("serverUrl", getServerURL(request));
		httppost.setEntity(new StringEntity(postParameters.toString()));

		try
		{
			return httpclient.execute(httppost, new ResponseHandler(endpoint));
		}
		catch (IOException e)
		{
			Debug.error("Can't get the rest api endpoints", e);
		}
		return null;
	}

	private static String getServerURL(HttpServletRequest req)
	{
		String scheme = req.getScheme();
		String serverName = req.getServerName();
		int serverPort = req.getServerPort();
		StringBuilder url = new StringBuilder();
		url.append(scheme).append("://").append(serverName);
		if (serverPort != 80 && serverPort != 443)
		{
			url.append(":").append(serverPort);
		}
		url.append(getPath(req));
		return url.toString();
	}

	private static String[] getCloudRestApiEndpoints(ServletContext servletContext, CloseableHttpClient httpclient, Solution solution)
	{
		String[] endpoints = (String[])servletContext.getAttribute("endpoints");
		if (endpoints != null)
		{
			long expire = Utils.getAsLong(servletContext.getAttribute("endpoints_expire"));
			if (expire < System.currentTimeMillis())
			{
				endpoints = null;
			}
		}
		if (endpoints == null)
		{
			HttpGet httpget = new HttpGet(CLOUD_REST_API_GET);
			httpget.addHeader(HttpHeaders.ACCEPT, "application/json");
			httpget.addHeader("uuid", sanitizeHeader(solution.getUUID().toString()));
			try
			{
				endpoints = getArrayProperty(httpclient, httpget, "endpoints",
					"Error when getting the endpoints from the servoycloud: ");
				if (endpoints != null)
				{
					servletContext.setAttribute("endpoints", endpoints);
					servletContext.setAttribute("endpoints_expire", Long.valueOf(System.currentTimeMillis() + 10 * 60 * 1000));
				}
			}
			catch (IOException e)
			{
				Debug.error("Can't get the rest api endpoints", e);
				servletContext.setAttribute("endpoints", null);
			}
		}

		return endpoints;
	}

	private static Pair<Integer, JSONObject> executeCloudGetRequest(CloseableHttpClient httpclient, Solution solution, String endpoint,
		HttpServletRequest request)
	{
		try
		{
			URIBuilder uriBuilder = new URIBuilder(CLOUD_REST_API_GET + endpoint);
			if (request != null)
			{
				Map<String, String[]> parameters = request.getParameterMap();
				for (Map.Entry<String, String[]> entry : parameters.entrySet())
				{
					String[] values = entry.getValue();
					for (String value : values)
					{
						uriBuilder.setParameter(entry.getKey(), value);
					}
				}
			}
			HttpGet httpget = new HttpGet(uriBuilder.build());
			httpget.addHeader(HttpHeaders.ACCEPT, "application/json");
			httpget.addHeader("uuid", sanitizeHeader(solution.getUUID().toString()));

			return httpclient.execute(httpget, new ResponseHandler(endpoint));
		}
		catch (Exception e)
		{
			Debug.error("Can't execute cloud get request", e);
		}
		return null;
	}

	private static void checkUser(String username, String password, Pair<Boolean, String> needToLogin, Solution solution, DecodedJWT oldToken,
		Boolean rememberUser, HttpServletRequest request)
	{
		boolean verified = false;
		if (solution.getAuthenticator() == AUTHENTICATOR_TYPE.SERVOY_CLOUD)
		{
			verified = checkCloudPermissions(username, password, needToLogin, solution, oldToken, rememberUser);
		}
		else if (solution.getAuthenticator() == AUTHENTICATOR_TYPE.OAUTH)
		{
			verified = refreshOAuthTokenIfPossible(needToLogin, solution, oldToken, request);
		}
		else if (solution.getAuthenticator() == AUTHENTICATOR_TYPE.AUTHENTICATOR)
		{
			verified = checkAuthenticatorPermissions(username, password, needToLogin, solution, oldToken, rememberUser);
		}
		else
		{
			verified = checkDefaultLoginPermissions(username, password, needToLogin, oldToken, rememberUser);
		}
		if (!verified)
		{
			needToLogin.setLeft(Boolean.TRUE);
			needToLogin.setRight(null);
		}
	}

	private static boolean refreshOAuthTokenIfPossible(Pair<Boolean, String> needToLogin, Solution solution, DecodedJWT oldToken, HttpServletRequest request)
	{
		String refresh_token = oldToken.getClaim(REFRESH_TOKEN).asString();
		if (refresh_token != null)
		{
			JSONObject properties = new ServoyJSONObject(solution.getCustomProperties(), true);
			if (properties.has(OAUTH_CUSTOM_PROPERTIES))
			{
				JSONObject auth = properties.getJSONObject(OAUTH_CUSTOM_PROPERTIES);
				OAuth20Service service = createOauthService(request, auth, new HashMap<>());
				if (service != null)
				{
					try
					{
						OpenIdOAuth2AccessToken token = (OpenIdOAuth2AccessToken)service.refreshAccessToken(refresh_token);
						String id_token = token.getOpenIdToken();
						DecodedJWT decodedJWT = JWT.decode(id_token);
						return checkOauthIdToken(needToLogin, solution, solution.getAuthenticator(), decodedJWT, request, refresh_token, false);
					}
					catch (Exception e)
					{
						Debug.error("Could not refresh the token", e);
					}
				}
			}
			else
			{
				Debug.error("Could not create the oauth service");
			}
		}
		return false;
	}

	private static boolean checkDefaultLoginPermissions(String username, String password, Pair<Boolean, String> needToLogin, DecodedJWT oldToken,
		Boolean rememberUser)
	{
		try
		{
			String clientid = ApplicationServerRegistry.get().getClientId();
			if (oldToken != null)
			{
				long passwordLastChagedTime = ApplicationServerRegistry.get().getUserManager().getPasswordLastSet(clientid,
					oldToken.getClaim(UID).asString());
				if (passwordLastChagedTime > oldToken.getClaim(LAST_LOGIN).asLong().longValue())
				{
					needToLogin.setLeft(Boolean.TRUE);
					needToLogin.setRight(null);
					return false;
				}
			}

			String uid = oldToken != null ? oldToken.getClaim(UID).asString()
				: ApplicationServerRegistry.get().checkDefaultServoyAuthorisation(username, password);
			if (uid != null)
			{

				String[] permissions = ApplicationServerRegistry.get().getUserManager().getUserGroups(clientid, uid);
				if (permissions.length > 0 && (oldToken == null || Arrays.equals(oldToken.getClaim(PERMISSIONS).asArray(String.class), permissions)))
				{
					String token = createToken(username, uid, permissions, Long.valueOf(System.currentTimeMillis()), rememberUser, null);
					needToLogin.setLeft(Boolean.FALSE);
					needToLogin.setRight(token);
					return true;
				}
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return false;
	}

	private static boolean checkAuthenticatorPermissions(String username, String password, Pair<Boolean, String> needToLogin, Solution solution,
		DecodedJWT oldToken, Boolean rememberUser)
	{
		Solution authenticator = findAuthenticator(solution);
		if (authenticator != null)
		{
			JSONObject json = new JSONObject();
			json.put(USERNAME, username);
			json.put(PASSWORD, password);
			String refreshToken = null;
			if (oldToken != null)
			{
				String payload = new String(java.util.Base64.getUrlDecoder().decode(oldToken.getPayload()));
				JSONObject token = new JSONObject(payload);
				json.put(LAST_LOGIN, token);
				refreshToken = oldToken.getClaim(REFRESH_TOKEN).asString();
			}

			return callAuthenticator(needToLogin, rememberUser, authenticator, json, refreshToken);
		}
		else
		{
			Debug.error("Trying to login in solution " + solution.getName() +
				" with using an AUTHENTICATOR solution, but the main solution doesn't have that as a module");
		}
		return false;
	}

	private static boolean callAuthenticator(Pair<Boolean, String> needToLogin, Boolean rememberUser, Solution authenticator, JSONObject json,
		String refreshToken)
	{
		Credentials credentials = new Credentials(null, authenticator.getName(), null, json.toString());
		IApplicationServer applicationServer = ApplicationServerRegistry.getService(IApplicationServer.class);
		try
		{
			ClientLogin login = applicationServer.login(credentials);
			if (login != null)
			{
				String token = createToken(login.getUserName(), login.getUserUid(), login.getUserGroups(), //
					Long.valueOf(System.currentTimeMillis()), rememberUser, refreshToken);
				needToLogin.setLeft(Boolean.FALSE);
				needToLogin.setRight(token);
				return true;
			}
		}
		catch (RemoteException | RepositoryException e)
		{
			Debug.error(e);
		}
		return false;
	}

	private static Solution findAuthenticator(Solution solution)
	{
		Solution authenticator = null;
		String modulesNames = solution.getModulesNames();
		IRepository localRepository = ApplicationServerRegistry.get().getLocalRepository();
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
			catch (RepositoryException e)
			{
				Debug.error(e);
			}
		}
		return authenticator;
	}

	private static boolean checkCloudPermissions(String username, String password, Pair<Boolean, String> needToLogin, Solution solution, DecodedJWT oldToken,
		Boolean rememberUser)
	{
		HttpGet httpget = new HttpGet(oldToken != null ? REFRESH_TOKEN_CLOUD_URL : CLOUD_URL);

		if (oldToken == null)
		{
			String auth = username + ":" + password;
			byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("ISO-8859-1")));
			String authHeader = "Basic " + new String(encodedAuth);
			httpget.setHeader(HttpHeaders.AUTHORIZATION, sanitizeHeader(authHeader));
		}
		else
		{
			httpget.addHeader(USERNAME, sanitizeHeader(oldToken.getClaim(USERNAME).asString()));
			httpget.addHeader(LAST_LOGIN, sanitizeHeader(oldToken.getClaim(LAST_LOGIN).asString()));
		}
		httpget.addHeader(HttpHeaders.ACCEPT, "application/json");
		httpget.addHeader("uuid", sanitizeHeader(solution.getUUID().toString()));

		try (CloseableHttpClient httpclient = HttpClients.createDefault())
		{
			Pair<Integer, JSONObject> res = httpclient.execute(httpget, new ResponseHandler("login_auth"));
			if (res.getLeft().intValue() == HttpStatus.SC_OK)
			{
				return extractPermissionFromResponse(needToLogin, rememberUser, res, username);
			}
		}
		catch (IOException e)
		{
			Debug.error("Can't validate user with the Servoy Cloud", e);
		}
		return false;
	}

	private static boolean checkCloudOAuthPermissions(Pair<Boolean, String> needToLogin, Solution solution, String payload, Boolean rememberUser)
	{
		HttpPost post = new HttpPost(CLOUD_OAUTH_URL);
		post.setEntity(new StringEntity(payload));

		post.addHeader(HttpHeaders.ACCEPT, "application/json");
		post.addHeader("uuid", sanitizeHeader(solution.getUUID().toString()));

		try (CloseableHttpClient httpclient = HttpClients.createDefault())
		{
			Pair<Integer, JSONObject> res = httpclient.execute(post, new ResponseHandler("validateOAuthUser"));
			if (res.getLeft().intValue() == HttpStatus.SC_OK)
			{
				return extractPermissionFromResponse(needToLogin, rememberUser, res, null);
			}
		}
		catch (IOException e)
		{
			Debug.error("Can't validate user with the Servoy Cloud", e);
		}
		return false;
	}

	/**
	 * @param needToLogin
	 * @param rememberUser
	 * @param res
	 */
	private static boolean extractPermissionFromResponse(Pair<Boolean, String> needToLogin, Boolean rememberUser, Pair<Integer, JSONObject> res, String user)
	{
		JSONObject loginTokenJSON = res.getRight();
		if (loginTokenJSON != null && loginTokenJSON.has("permissions"))
		{
			String[] permissions = null;
			JSONArray permissionsArray = loginTokenJSON.getJSONArray("permissions");
			if (permissionsArray != null)
			{
				permissions = new String[permissionsArray.length()];
				for (int i = 0; i < permissions.length; i++)
				{
					permissions[i] = permissionsArray.getString(i);
				}
			}
			if (permissions != null)
			{
				String username = user;

				if (username == null || loginTokenJSON.has("username")) username = loginTokenJSON.getString("username");

				String token = createToken(username, username, permissions, loginTokenJSON.optString("lastLogin"), rememberUser, null);
				needToLogin.setLeft(Boolean.FALSE);
				needToLogin.setRight(token);
				return true;
			}
		}
		return false;
	}

	private static String[] getArrayProperty(CloseableHttpClient httpclient, HttpGet httpget, String property, String error) throws IOException
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
					JSONArray permissionsArray = loginTokenJSON.getJSONArray(property);
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
					Debug.error(error + response.getCode() + " " +
						response.getReasonPhrase());
					return null;
				}
			}
		});
		return permissions;
	}


	public static String createToken(String username, String uid, String[] groups, Object lastLogin, Boolean rememberUser, String refresh_token)
	{
		Properties settings = ApplicationServerRegistry.get().getServerAccess().getSettings();
		Algorithm algorithm = Algorithm.HMAC256(settings.getProperty(JWT_Password));
		Builder builder = JWT.create()
			.withIssuer("svy")
			.withClaim(UID, uid)
			.withClaim(USERNAME, username)
			.withArrayClaim(PERMISSIONS, groups)
			.withExpiresAt(new Date(System.currentTimeMillis() + TOKEN_AGE_IN_SECONDS * 1000));
		if (lastLogin instanceof String)
		{
			builder = builder.withClaim(LAST_LOGIN, (String)lastLogin);
		}
		if (lastLogin instanceof Long)
		{
			builder = builder.withClaim(LAST_LOGIN, (Long)lastLogin);
		}
		if (Boolean.TRUE.equals(rememberUser))
		{
			builder = builder.withClaim(REMEMBER, rememberUser);
		}
		if (refresh_token != null)
		{
			builder = builder.withClaim(REFRESH_TOKEN, refresh_token);
		}
		return builder.sign(algorithm);
	}

	private static OAuth20Service createOauthService(HttpServletRequest request, JSONObject auth,
		Map<String, String> additionalParameters)
	{
		String nonce = generateNonce(request.getServletContext(), auth);
		additionalParameters.put(NONCE, nonce);
		return createOauthService(auth, additionalParameters, getServerURL(request));
	}


	public static void writeLoginPage(HttpServletRequest request, HttpServletResponse response, String solutionName)
		throws IOException
	{
		if (request.getCharacterEncoding() == null) request.setCharacterEncoding("UTF8");
		HTTPUtils.setNoCacheHeaders(response);

		String id_token = getExistingIdToken(request);
		Solution solution = null;
		try
		{
			solution = (Solution)ApplicationServerRegistry.get().getLocalRepository().getActiveRootObject(solutionName, IRepository.SOLUTIONS);
		}
		catch (RepositoryException e)
		{
			Debug.error("Can't load solution " + solutionName, e);
		}
		if (solution != null && solution.getAuthenticator() == AUTHENTICATOR_TYPE.OAUTH)
		{
			JSONObject properties = new ServoyJSONObject(solution.getCustomProperties(), true);
			if (properties.has(OAUTH_CUSTOM_PROPERTIES))
			{
				JSONObject auth = properties.getJSONObject(OAUTH_CUSTOM_PROPERTIES);
				generateOauthCall(request, response, auth);
			}
			else
			{
				Debug.error("The oauth configuration is missing for solution " + solution.getName() +
					". Please create it using the button from the authenticator type property in the properties view.");
			}

			return;
		}

		String loginHtml = null;
		if (solution != null && solution.getAuthenticator() == AUTHENTICATOR_TYPE.SERVOY_CLOUD)
		{
			try (CloseableHttpClient httpClient = HttpClients.createDefault())
			{
				Pair<Integer, JSONObject> result = executeCloudGetRequest(httpClient, solution, "login", null);
				if (result != null)
				{
					int status = result.getLeft().intValue();
					JSONObject res = result.getRight();
					if (status == HttpStatus.SC_OK && res != null)
					{
						loginHtml = res.optString("html", null);
					}
				}
			}
		}
		if (solution != null && loginHtml == null)
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

		StringBuilder sb = new StringBuilder();
		sb.append("<base href=\"");
		sb.append(getPath(request));
		sb.append("\">");
		if (request.getParameter(ID_TOKEN) == null && request.getParameter(USERNAME) == null)
		{
			//we check the local storage for the token or username only once (if both are null)
			sb.append("\n  	 <script type='text/javascript'>");
			sb.append("\n    window.addEventListener('load', () => { ");
			sb.append("\n     if (window.localStorage.getItem('servoy_id_token')) { ");
			sb.append("\n    	document.body.style.display = 'none'; ");
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

		response.setCharacterEncoding("UTF-8");
		response.setContentType("text/html");
		response.setContentLengthLong(loginHtml.length());
		response.getWriter().write(loginHtml);
		return;
	}

	/**
	 * Check if there is an id_token as a request parameter or session attribute (without creating a new session).
	 * @param request
	 * @return the existing id_token or null if not present or in case of empty string
	 */
	private static String getExistingIdToken(HttpServletRequest request)
	{
		String id_token = request.getParameter(ID_TOKEN);
		if (id_token == null)
		{
			HttpSession session = request.getSession(false);
			if (session != null)
			{
				id_token = (String)session.getAttribute(ID_TOKEN);
			}
		}
		return !Utils.stringIsEmpty(id_token) ? id_token : null;
	}

	/**
	 * @param request
	 * @param response
	 * @param solution
	 */
	private static void generateOauthCall(HttpServletRequest request, HttpServletResponse response, JSONObject auth)
	{
		String id_token = getExistingIdToken(request);
		Map<String, String> additionalParameters = new HashMap<>();
		if (!Utils.stringIsEmpty(id_token))
		{
			DecodedJWT decodedJWT = JWT.decode(id_token);
			if (!"svy".equals(decodedJWT.getIssuer()))
			{
				//id token which is rejected by the authenticator, show the prompt
				additionalParameters.put("prompt", "consent"); // should this be select_account ?
			}
		}
		OAuth20Service service = createOauthService(request, auth, additionalParameters);
		if (service != null)
		{
			try
			{
				final String authorizationUrl = service.createAuthorizationUrlBuilder()//
					.additionalParams(additionalParameters).build();
				StringBuilder sb = new StringBuilder();
				sb.append("<!DOCTYPE html>").append("\n")
					.append("<html lang=\"en\">").append("\n")
					.append("<head>").append("\n")
					.append("    <meta charset=\"UTF-8\">").append("\n")
					.append("<base href=\"").append("\n")
					.append(getPath(request)).append("\n")
					.append("\">").append("\n")
					.append("<script type='text/javascript'>").append("\n")
					.append("    window.addEventListener('load', () => { ").append("\n");
				if (!Utils.stringIsEmpty(id_token))
				{
					//we have an id token (svy or oauth provider) which is not valid or cannot be refreshed
					sb.append("     window.localStorage.removeItem('servoy_id_token');").append("\n")
						.append("   window.location.href = '").append(authorizationUrl).append("';").append("\n");
				}
				else
				{
					sb.append("        const servoyIdToken = window.localStorage.getItem('servoy_id_token');").append("\n")
						.append("        if (servoyIdToken) {").append("\n")
						.append("            document.login_form.id_token.value = JSON.parse(servoyIdToken);").append("\n")
						.append("            document.login_form.submit();").append("\n")
						.append("        } else {").append("\n")
						.append("            window.location.href = '").append(authorizationUrl).append("';").append("\n")
						.append("        }").append("\n");
				}
				sb.append("   }) ").append("\n")
					.append("  </script> ").append("\n")
					.append("    <title>Auto Login</title>").append("\n")
					.append("</head>").append("\n")
					.append("<body>").append("\n")
					.append("   <form accept-charset=\"UTF-8\" role=\"form\" name=\"login_form\" method=\"post\" style=\"display: none;\">")
					.append("\n")
					.append("        <input type=\"hidden\" name=\"id_token\" id=\"id_token\">").append("\n")
					.append("   </form>").append("\n")
					.append("\n")
					.append("</body>").append("\n")
					.append("</html>").append("\n");

				response.setCharacterEncoding("UTF-8");
				response.setContentType("text/html");
				response.setContentLengthLong(sb.length());
				response.getWriter().write(sb.toString());
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
	}


	public static OAuth20Service createOauthService(JSONObject auth, Map<String, String> additionalParameters, String serverURL)
	{
		ServiceBuilder builder = new ServiceBuilder(auth.optString(CLIENT_ID));
		String api = null, tenant = null;
		for (String key : auth.keySet())
		{
			switch (key)
			{
				case API_SECRET :
					builder.apiSecret(auth.getString(key));
					break;
				case DEFAULT_SCOPE :
					builder.defaultScope(auth.getString(key));
					break;
				case OAUTH_API :
					api = auth.getString(key);
					break;
				case TENANT :
					tenant = auth.getString(key);
					break;
				case AUTHORIZATION_BASE_URL :
				case ACCESS_TOKEN_ENDPOINT :
				case CLIENT_ID :
				case JWKS_URI :
					//skip
					break;
				default :
					additionalParameters.put(key, auth.getString(key));
			}
		}
		String responseType = "offline".equals(additionalParameters.get("access_type")) ? "code" : "id_token";
		builder.responseType(responseType); //TODO check other apis
		if (responseType.equals("code"))
		{
			additionalParameters.put("state", additionalParameters.get(NONCE));
		}
		builder.callback(serverURL + "svy_oauth/" + "index.html");
		try
		{
			DefaultApi20 apiInstance = null;
			if (api != null)
			{
				apiInstance = getApiInstance(api, tenant);
			}
			else
			{
				if (!auth.has(AUTHORIZATION_BASE_URL))
				{
					throw new Exception("Cannot create the custom oauth api, authorizationBaseUrl is null.");
				}
				if (!auth.has(ACCESS_TOKEN_ENDPOINT))
				{
					throw new Exception("Cannot create the custom oauth api, accessTokenEndpoint is null.");
				}

				apiInstance = new DefaultApi20()
				{
					@Override
					protected String getAuthorizationBaseUrl()
					{
						return auth.getString(AUTHORIZATION_BASE_URL);
					}

					@Override
					public String getAccessTokenEndpoint()
					{
						return auth.getString(ACCESS_TOKEN_ENDPOINT);
					}

					@Override
					public String getRefreshTokenEndpoint()
					{
						return auth.getString(REFRESH_TOKEN_ENDPOINT);
					}
				};
			}
			return builder.build(apiInstance);
		}
		catch (Exception e)
		{
			Debug.error("Cannot create the oauth service.", e);
		}
		return null;
	}

	private static String getPath(HttpServletRequest request)
	{
		String path = Settings.getInstance().getProperty("servoy.context.path", request.getContextPath() + '/');
		Path p = Paths.get(request.getServletPath()).normalize();
		int i = 0;
		while (i < p.getNameCount() - 1 && !SVYLOGIN_PATH.equals(p.getName(i).toString()))
		{
			path += p.getName(i) + "/";
			i++;
		}
		return path;
	}

	/**
	 *
	 */
	public static void init(ServletContext context)
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
		context.setAttribute(NONCE, Collections.synchronizedMap(new PassiveExpiringMap<String, String>(30, TimeUnit.MINUTES)));
	}


	/**
	 * this wil generate a nonce and put it in the context cache.
	 *
	 * @param context
	 * @param oauth
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private static String generateNonce(ServletContext context, JSONObject oauth)
	{
		Map<String, JSONObject> cache = (Map<String, JSONObject>)context.getAttribute(NONCE);
		String nonce = UUID.randomUUID().toString();
		cache.put(nonce, oauth);
		return nonce;
	}

	/**
	 * this will just return the value from the nonce context cache.
	 *
	 * @param context
	 * @param nonceString
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private static JSONObject getNonce(ServletContext context, String nonceString)
	{
		if (nonceString != null)
		{
			Map<String, JSONObject> cache = (Map<String, JSONObject>)context.getAttribute(NONCE);
			return cache.get(nonceString);
		}
		return null;
	}

	/**
	 * This will return the value from the nonce context cache and remove it. (so return null means it was not a valid nonce)
	 * @param context
	 * @param nonceString
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private static JSONObject checkNonce(ServletContext context, String nonceString)
	{
		if (nonceString != null)
		{
			Map<String, JSONObject> cache = (Map<String, JSONObject>)context.getAttribute(NONCE);
			return cache.remove(nonceString);
		}
		return null;
	}


	/**
	 * @author emera
	 */
	public static class ResponseHandler implements HttpClientResponseHandler<Pair<Integer, JSONObject>>
	{
		private final String endpoint;

		public ResponseHandler(String endpoint)
		{
			this.endpoint = endpoint;
		}

		@Override
		public Pair<Integer, JSONObject> handleResponse(ClassicHttpResponse response) throws HttpException, IOException
		{
			HttpEntity responseEntity = response.getEntity();
			if (responseEntity != null)
			{
				Pair<Integer, JSONObject> pair = new Pair<>(Integer.valueOf(response.getCode()), null);
				String responseString = EntityUtils.toString(responseEntity);
				if (responseString.startsWith("{"))
				{
					pair.setRight(new JSONObject(responseString));
				}
				return pair;
			}
			else
			{
				Debug.error("Could not access rest api endpoint " + endpoint + " " + response.getCode() + " " +
					response.getReasonPhrase());
			}
			return null;
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

	private static String sanitizeHeader(String headerValue)
	{
		return headerValue.replaceAll("[\n\r]+", " ");
	}

	static DefaultApi20 getApiInstance(String provider, String tenant) throws Exception
	{
		switch (provider)
		{
			case "Microsoft" :
				return tenant != null ? MicrosoftAzureActiveDirectory20Api.custom(tenant) : MicrosoftAzureActiveDirectory20Api.instance();
			case "Google" :
				return GoogleApi20.instance();
			default :
				throw new Exception("Could not create an OAuth Api.");
		}
	}
}