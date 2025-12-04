/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
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
import org.owasp.encoder.Encode;
import org.sablo.security.ContentSecurityPolicyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.scribejava.apis.openid.OpenIdOAuth2AccessToken;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.github.scribejava.core.revoke.TokenTypeHint;
import com.servoy.base.util.ITagResolver;
import com.servoy.base.util.TagParser;
import com.servoy.j2db.ClientVersion;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Solution.AUTHENTICATOR_TYPE;
import com.servoy.j2db.server.ngclient.AngularIndexPageWriter;
import com.servoy.j2db.server.ngclient.StatelessLoginHandler;
import com.servoy.j2db.server.ngclient.auth.OAuthUtils.OAuthParameters;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Utils;

/**
 * @author emera
 */
public class CloudStatelessAccessManager
{
	private static final String SVY_REDIRECT = "svyRedirect";

	private static final Logger log = LoggerFactory.getLogger("stateless.login");

	private static final String BASE_CLOUD_URL = System.getProperty("servoy.api.url", "https://middleware-prod.unifiedui.servoy-cloud.eu");
	public static final String CLOUD_REST_API_GET = BASE_CLOUD_URL + "/servoy-service/rest_ws/api/auth_endpoint/getEndpointUI/";
	public static final String CLOUD_REST_API_POST = BASE_CLOUD_URL + "/servoy-service/rest_ws/api/auth_endpoint/submitForm/";
	public static final String CLOUD_URL = BASE_CLOUD_URL +
		"/servoy-service/rest_ws/api/login_auth/validateAuthUser";
	public static final String REFRESH_TOKEN_CLOUD_URL = BASE_CLOUD_URL +
		"/servoy-service/rest_ws/api/login_auth/refreshPermissions";
	public static final String CLOUD_OAUTH_URL = BASE_CLOUD_URL +
		"/servoy-service/rest_ws/api/login_auth/validateOAuthUser";
	public static final String CLOUD_OAUTH_ENDPOINT = "endpoint";


	public static boolean checkCloudOAuthPermissions(Pair<Boolean, String> needToLogin, Solution solution, String payload, Boolean rememberUser,
		String refresh_token, String provider)
	{
		HttpPost post = new HttpPost(CLOUD_OAUTH_URL);
		post.setEntity(new StringEntity(payload));

		post.addHeader(HttpHeaders.ACCEPT, "application/json");
		post.addHeader("uuid", sanitizeHeader(solution.getUUID().toString()));

		try (CloseableHttpClient httpclient = HttpClients.createDefault())
		{
			Pair<Integer, JSONObject> res = httpclient.execute(post, new CloudResponseHandler("validateOAuthUser"));
			if (res.getLeft().intValue() == HttpStatus.SC_OK)
			{
				SvyTokenBuilder tokenBuilder = extractPermissionFromResponse(needToLogin, res, null);
				if (tokenBuilder == null) return false;

				tokenBuilder.withRememberUser(rememberUser);
				tokenBuilder.withRefreshToken(refresh_token);
				tokenBuilder.withClaim(CLOUD_OAUTH_ENDPOINT, provider);
				String svyToken = tokenBuilder.sign();
				needToLogin.setLeft(Boolean.FALSE);
				needToLogin.setRight(svyToken);
				return true;
			}
		}
		catch (IOException e)
		{
			log.error("Can't validate user with the Servoy Cloud", e);
		}
		return false;
	}

	public static boolean checkCloudPermissions(String username, String password, boolean remember, SvyID oldToken, Pair<Boolean, String> needToLogin,
		Solution solution,
		HttpServletRequest request)
	{
		HttpGet httpget = new HttpGet(oldToken != null ? REFRESH_TOKEN_CLOUD_URL : CLOUD_URL);

		String provider = null;
		if (oldToken == null)
		{
			String auth = username + ":" + password;
			byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("ISO-8859-1")));
			String authHeader = "Basic " + new String(encodedAuth);
			httpget.setHeader(HttpHeaders.AUTHORIZATION, sanitizeHeader(authHeader));
			httpget.addHeader(SvyID.REMEMBER, remember); //this is needed until the validateAuthUser endpoint is deprecated
		}
		else
		{
			if (oldToken.getStringClaim(CLOUD_OAUTH_ENDPOINT) != null &&
				oldToken.getStringClaim(StatelessLoginHandler.REFRESH_TOKEN) != null)
			{
				provider = oldToken.getStringClaim(CLOUD_OAUTH_ENDPOINT);
				JSONObject oauth = getOAuthConfigFromTheCloud(solution, request, provider);
				OAuth20Service service = OAuthUtils.createOauthService(request, oauth, new HashMap<>());
				if (service != null)
				{
					try
					{
						String refresh_token = oldToken.getStringClaim(StatelessLoginHandler.REFRESH_TOKEN);
						OpenIdOAuth2AccessToken token = (OpenIdOAuth2AccessToken)service.refreshAccessToken(refresh_token);
						String id_token = token.getOpenIdToken();
						DecodedJWT decodedJWT = JWT.decode(id_token);
						if (!JWTValidator.verifyJWT(decodedJWT, oauth.getString(OAuthParameters.jwks_uri.name())))
						{
							return false;
						}
					}
					catch (Exception e)
					{
						log.error("Could not refresh the token", e);
						return false;
					}
				}
				else
				{
					log.error("Could not refresh the token, because the oauth service is null");
					return false;
				}
			}
			httpget.addHeader(SvyID.USERNAME, sanitizeHeader(oldToken.getUsername()));
			httpget.addHeader(SvyID.LAST_LOGIN, sanitizeHeader(oldToken.getStringClaim(SvyID.LAST_LOGIN)));
		}
		httpget.addHeader(HttpHeaders.ACCEPT, "application/json");
		httpget.addHeader("uuid", sanitizeHeader(solution.getUUID().toString()));

		try (CloseableHttpClient httpclient = HttpClients.createDefault())
		{
			Pair<Integer, JSONObject> res = httpclient.execute(httpget, new CloudResponseHandler("login_auth"));
			if (res.getLeft().intValue() == HttpStatus.SC_OK)
			{
				SvyTokenBuilder tokenBuilder = extractPermissionFromResponse(needToLogin, res, oldToken != null ? oldToken.getUsername() : username);
				if (tokenBuilder == null) return false;
				tokenBuilder.withRememberUser(oldToken != null ? Boolean.valueOf(oldToken.rememberUser()) : Boolean.FALSE);
				if (oldToken != null) tokenBuilder.withRefreshToken(oldToken.getStringClaim(StatelessLoginHandler.REFRESH_TOKEN));
				tokenBuilder.withClaim(CLOUD_OAUTH_ENDPOINT, provider);
				String svyToken = tokenBuilder.sign();
				needToLogin.setLeft(Boolean.FALSE);
				needToLogin.setRight(svyToken);
				return true;
			}
		}
		catch (IOException e)
		{
			log.error("Can't validate user with the Servoy Cloud", e);
		}
		return false;
	}

	private static JSONObject getOAuthConfigFromTheCloud(Solution solution, HttpServletRequest request, String provider)
	{
		JSONObject oauth = null;
		try (CloseableHttpClient httpclient = HttpClients.createDefault())
		{
			String[] parts = provider.split("\\?");
			String endpoint = parts[0];
			Map<String, String[]> parameters = parts.length > 1 ? parseQueryString(parts[1]) : new HashMap<>();
			Pair<Integer, JSONObject> providerRequest = executeCloudPostRequest(httpclient, solution, endpoint, request, parameters);
			if (providerRequest.getRight().has("oauth"))
			{
				oauth = providerRequest.getRight().getJSONObject("oauth");
			}
		}
		catch (IOException e)
		{
			log.error("Can't validate user with the Servoy Cloud", e);
		}
		return oauth;
	}

	public static boolean handlePossibleCloudRequest(HttpServletRequest request, HttpServletResponse response, String solutionName, Object index)
		throws ServletException
	{
		Path path = Paths.get(request.getRequestURI()).normalize();
		if (solutionName != null && path.getNameCount() > 2 && StatelessLoginUtils.SVYLOGIN_PATH.equals(path.getName(2).toString()))
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
								String svyRedirect = Encode.forHtmlAttribute(request.getParameter(SVY_REDIRECT));
								if ("POST".equalsIgnoreCase(request.getMethod()))
								{
									res = executeCloudPostRequest(httpclient, solution, endpoint, request,
										request.getParameterMap());
								}
								else
								{
									res = executeCloudGetRequest(httpclient, solution, endpoint, request);
								}

								if (res != null)
								{
									writeResponse(request, response, solution, res, index, svyRedirect);
									return true;
								}
								else
								{
									log.atInfo().log(() -> "The endpoint " + endpoint + " returned no result.");
								}
							}
							else
							{
								log.atInfo()
									.log(() -> "The endpoint " + endpoint + " is not available for the solution " + solution.getUUID());
							}
						}
					}
					catch (IOException e)
					{
						log.error("Can't access the Servoy Cloud api", e);
					}
				}
			}
			catch (Exception e)
			{
				log.atInfo().setCause(e).log(() -> "Exception thrown when handling a possible cloud request");
				throw new ServletException(e.getMessage(), e);
			}
		}
		return false;

	}

	private static Pair<Integer, JSONObject> executeCloudPostRequest(CloseableHttpClient httpclient, Solution solution, String endpoint,
		HttpServletRequest request, Map<String, String[]> parameters)
	{
		HttpPost httppost = new HttpPost(CLOUD_REST_API_POST + endpoint);
		httppost.addHeader(HttpHeaders.ACCEPT, "application/json");
		httppost.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
		httppost.addHeader("build-number", String.valueOf(ClientVersion.getReleaseNumber()));
		httppost.addHeader("uuid", sanitizeHeader(solution.getUUID().toString()));
		JSONObject postParameters = new JSONObject();
		for (Map.Entry<String, String[]> entry : parameters.entrySet())
		{
			String[] values = entry.getValue();
			for (String value : values)
			{
				if (SVY_REDIRECT.equals(entry.getKey())) continue;
				postParameters.put(entry.getKey(), value);
			}
		}
		//the request can be null on logout, but the cloud needs the server url
		postParameters.put("serverUrl", request != null ? StatelessLoginUtils.getServerURL(request) : "https://");
		httppost.setEntity(new StringEntity(postParameters.toString()));

		try
		{
			return httpclient.execute(httppost, new CloudResponseHandler(endpoint));
		}
		catch (IOException e)
		{
			log.error("Can't execute cloud post request", e);
		}
		return null;
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
			httpget.addHeader(HttpHeaders.ACCEPT_LANGUAGE, request.getHeader(HttpHeaders.ACCEPT_LANGUAGE));
			httpget.addHeader("build-number", String.valueOf(ClientVersion.getReleaseNumber()));
			httpget.addHeader("uuid", sanitizeHeader(solution.getUUID().toString()));

			return httpclient.execute(httpget, new CloudResponseHandler(endpoint));
		}
		catch (Exception e)
		{
			log.error("Can't execute cloud get request", e);
		}
		return null;
	}

	public static void revokeToken(Solution solution, DecodedJWT jwt)
	{
		String provider = jwt.getClaim(CLOUD_OAUTH_ENDPOINT).asString();
		if (provider != null)
		{
			JSONObject oauth = getOAuthConfigFromTheCloud(solution, null, provider);
			if (oauth != null)
			{
				OAuth20Service service = OAuthUtils.createOauthService(oauth, new HashMap<>(), null);
				if (service != null)
				{
					try
					{
						if (service != null && service.getApi().getRevokeTokenEndpoint() != null)
						{
							service.revokeToken(jwt.getClaim(StatelessLoginHandler.REFRESH_TOKEN).asString(), TokenTypeHint.REFRESH_TOKEN);
						}
					}
					catch (IOException | InterruptedException | ExecutionException | UnsupportedOperationException e)
					{
						log.error("Could not revoke the refresh token.", e);
					}
				}
			}
			else
			{
				log.error("Could not revoke the refresh token, the cloud did not return an oauth config for " + provider);
			}
		}
	}

	private static void writeResponse(HttpServletRequest request, HttpServletResponse response, Solution solution, Pair<Integer, JSONObject> res,
		Object index, String initialURL)
		throws IOException, UnsupportedEncodingException, ServletException
	{
		String html = null;
		int status = res.getLeft().intValue();
		JSONObject json = res.getRight();
		if (json != null)
		{
			if (status == HttpStatus.SC_OK && json.has("html"))
			{
				log.atInfo().log(() -> "The cloud returned html: " + json.get("html"));
				html = json.getString("html");
			}
			else if (json.has("error"))
			{
				log.atInfo().log(() -> "The cloud sent an error response, http status " + res.getLeft());
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
			else if (json.has("oauth"))
			{
				// this is an oauth request
				JSONObject oauth = json.getJSONObject("oauth");
				Path path = Paths.get(request.getRequestURI()).normalize();
				StringBuilder endpointBuilder = new StringBuilder(path.getName(path.getNameCount() - 1).toString().replace(".html", ""));
				endpointBuilder.append("?");
				endpointBuilder.append(requestParamsToString(request.getParameterMap()));
				oauth.put(CLOUD_OAUTH_ENDPOINT, endpointBuilder.toString());
				log.atInfo().log(() -> "The cloud returned an oauth config: " + oauth);
				OAuthHandler.generateOauthCall(request, response, oauth); //TODO move to OAuthHandler?
			}
			else if (json.has("permissions"))
			{
				log.atInfo().log(() -> "The cloud returned permissions: " + json.getJSONArray("permissions").toString(2));
				Pair<Boolean, String> showLogin = new Pair<>(Boolean.TRUE, null);
				SvyTokenBuilder tokenBuilder = extractPermissionFromResponse(showLogin, res,
					json.optString(SvyID.USERNAME, ""));
				if (tokenBuilder != null)
				{
					tokenBuilder
						.withRememberUser(
							json.has(SvyID.REMEMBER) ? Boolean.valueOf(json.getBoolean(SvyID.REMEMBER)) : Boolean.FALSE) //
						.withRefreshToken(json.optString(StatelessLoginHandler.REFRESH_TOKEN, ""))//
						//TODO we can't have the provider here unless it is returned from the cloud
						.withClaim(CLOUD_OAUTH_ENDPOINT, json.optString(CLOUD_OAUTH_ENDPOINT, ""));
					String svyToken = tokenBuilder.sign();
					showLogin.setLeft(Boolean.FALSE);
					showLogin.setRight(svyToken);
				}
				if (!showLogin.getLeft().booleanValue() && (index instanceof File || index instanceof String))
				{
					if (showLogin.getRight() != null)
					{
						request.getSession().setAttribute(StatelessLoginHandler.ID_TOKEN, showLogin.getRight());
					}
					response.sendRedirect(initialURL != null ? StringEscapeUtils.unescapeHtml4(initialURL) : request.getContextPath() + "/index.html");
					return;
				}
				else
				{
					if (showLogin.getLeft().booleanValue())
					{
						if (showLogin.getRight() != null && showLogin.getRight().startsWith("<"))
						{
							log.atInfo().log(() -> "Display html result from the cloud." + showLogin.getRight());
							html = showLogin.getRight();
						}
						else
						{
							log.error("There was a problem when extracting the permissions and creating a svy token.");
						}
					}
					else
					{
						log.error("Cannot redirect to the index page.");
					}
				}
			}
			else
			{
				log.atInfo()
					.log(() -> "Showing the login page. The cloud returned " + status + " http status and unknown response format:" + json);
				StatelessLoginHandler.writeLoginPage(request, response, solution.getName(), html); //TODO refactor
				return;
			}
			if (html != null)
			{
				ContentSecurityPolicyConfig contentSecurityPolicyConfig = CloudStatelessAccessManager.addcontentSecurityPolicyHeader(request, response);
				String contentSecurityPolicyNonce = contentSecurityPolicyConfig != null ? contentSecurityPolicyConfig.getNonce() : null;
				if (contentSecurityPolicyNonce != null)
				{
					html = html.replace("<script ", "<script nonce='" + contentSecurityPolicyNonce + '\'');
					html = html.replace("<style", "<style nonce='" + contentSecurityPolicyNonce + '\'');
				}
				if (initialURL != null) html = html.replace("</form>", "<input type='hidden' name='" + SVY_REDIRECT + "' value='" + initialURL + "'></form>");
				HTMLWriter.writeHTML(request, response, html);
			}
			else
			{
				log.error("The cloud did not return html.");
			}
		}

	}

	private static String requestParamsToString(Map<String, String[]> parameterMap)
	{
		StringBuilder params = new StringBuilder();
		parameterMap.forEach((key, values) -> Arrays.asList(values).stream().forEach(val -> params.append("&").append(key).append("=").append(val)));
		return params.toString().replaceFirst("&", "");
	}

	private static Map<String, String[]> parseQueryString(String query)
	{
		Map<String, List<String>> tempMap = new HashMap<>();
		String[] pairs = query.split("&");
		for (String pair : pairs)
		{
			String[] keyValue = pair.split("=", 2);
			String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
			String value = keyValue.length > 1 ? URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8) : null;
			tempMap.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
		}

		Map<String, String[]> result = new HashMap<>();
		tempMap.forEach((key, values) -> result.put(key, values.toArray(new String[0])));

		return result;
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
				else
				{
					log.atInfo().log(() -> "No endpoints were returned for solution " + solution.getUUID());
				}
			}
			catch (IOException e)
			{
				log.error("Can't get the rest api endpoints", e);
				servletContext.setAttribute("endpoints", null);
			}
		}

		return endpoints;
	}

	private static SvyTokenBuilder extractPermissionFromResponse(Pair<Boolean, String> needToLogin, Pair<Integer, JSONObject> res, String user)
	{
		JSONObject loginTokenJSON = res.getRight();
		if (loginTokenJSON != null)
		{
			if (res.getRight().has("html"))
			{
				//TODO should this be moved somehow to writeResponse?
				needToLogin.setLeft(Boolean.TRUE);
				needToLogin.setRight(res.getRight().getString("html"));
				return null;
			}
			if (loginTokenJSON.has("permissions"))
			{
				String[] permissions = null, tenants = null;
				JSONArray permissionsArray = loginTokenJSON.getJSONArray("permissions");
				if (permissionsArray != null)
				{
					permissions = new String[permissionsArray.length()];
					for (int i = 0; i < permissions.length; i++)
					{
						permissions[i] = permissionsArray.getString(i);
					}
				}
				if (loginTokenJSON.optJSONArray("tenantValues") != null)
				{
					JSONArray tenantValues = loginTokenJSON.getJSONArray("tenantValues");
					if (tenantValues.length() > 0)
					{
						tenants = new String[tenantValues.length()];
						for (int i = 0; i < tenants.length; i++)
						{
							tenants[i] = tenantValues.getString(i);
						}
					}
				}

				if (permissions != null)
				{
					String username = user;

					if (username == null || loginTokenJSON.has("username")) username = loginTokenJSON.getString("username");

					SvyTokenBuilder builder = new SvyTokenBuilder(username, username, permissions)//
						.withLastLogin(loginTokenJSON.optString("lastLogin")) //
						.withTenants(tenants);
					return builder;
				}
			}
		}
		return null;
	}

	private static String sanitizeHeader(String headerValue)
	{
		return headerValue.replaceAll("[\n\r]+", " ");
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
					log.error(error + response.getCode() + " " +
						response.getReasonPhrase());
					return null;
				}
			}
		});
		return permissions;
	}

	public static String getCloudLoginPage(HttpServletRequest request, Solution solution, String loginHtml) throws IOException
	{
		try (CloseableHttpClient httpClient = HttpClients.createDefault())
		{
			Pair<Integer, JSONObject> result = executeCloudGetRequest(httpClient, solution, "login", request);
			if (result != null)
			{
				int status = result.getLeft().intValue();
				JSONObject res = result.getRight();
				if (status == HttpStatus.SC_OK && res != null)
				{
					loginHtml = res.optString("html", null);
					if (loginHtml != null && loginHtml.contains("</form>"))
					{
						String queryString = request.getQueryString() != null ? "?" + Encode.forHtmlAttribute(request.getQueryString()) : "";
						loginHtml = loginHtml.replace("</form>", "<input type='hidden' name='" + SVY_REDIRECT + "' value='" +
							request.getRequestURL() + queryString + "'></form>");
					}
				}
			}
		}
		return loginHtml;
	}

	public static ContentSecurityPolicyConfig addcontentSecurityPolicyHeader(HttpServletRequest request, HttpServletResponse response)
	{
		ContentSecurityPolicyConfig contentSecurityPolicyConfig = AngularIndexPageWriter.getContentSecurityPolicyConfig(request);
		if (contentSecurityPolicyConfig != null)
		{
			String val = contentSecurityPolicyConfig.getDirectives().entrySet().stream()
				.map(entry -> {
					String key = entry.getKey();
					String value = entry.getValue();
					if ("script-src".equals(key) || "style-src".equals(key))
					{
						value += " " + BASE_CLOUD_URL;
					}
					return key + ' ' + value;
				})
				.collect(Collectors.joining("; "));
			response.addHeader("Content-Security-Policy", val);
		}
		return contentSecurityPolicyConfig;
	}
}
