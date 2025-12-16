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

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.scribejava.apis.openid.OpenIdOAuth2AccessToken;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.oauth.AccessTokenRequestParams;
import com.github.scribejava.core.oauth.AuthorizationUrlBuilder;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.github.scribejava.core.pkce.PKCE;
import com.github.scribejava.core.pkce.PKCECodeChallengeMethod;
import com.github.scribejava.core.revoke.TokenTypeHint;
import com.servoy.base.util.ITagResolver;
import com.servoy.base.util.TagParser;
import com.servoy.j2db.ClientLogin;
import com.servoy.j2db.Credentials;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Solution.AUTHENTICATOR_TYPE;
import com.servoy.j2db.server.ngclient.AngularIndexPageWriter;
import com.servoy.j2db.server.ngclient.StatelessLoginHandler;
import com.servoy.j2db.server.ngclient.auth.OAuthUtils.OAuthParameters;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServer;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author emera
 */
public class OAuthHandler
{
	private static final String CODE = "code";
	private static final String GET_OAUTH_CONFIG = "getOAuthConfig";

	static final Logger log = LoggerFactory.getLogger("stateless.login");

	public static Pair<Boolean, String> handleOauth(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		String reqUrl = req.getRequestURL().toString();
		Pair<Boolean, String> showLogin = new Pair<>(Boolean.TRUE, null);
		if (req.getParameter("id_token") != null || req.getParameter(CODE) != null)
		{
			checkToken(req, resp, reqUrl, showLogin);
		}
		else if (reqUrl.contains("/svy_oauth/"))
		{
			//could be that the id token is in the fragment
			showLogin.setLeft(Boolean.FALSE);
			extractFromFragment(req, resp, reqUrl);
		}
		return showLogin;
	}

	private static Pair<Boolean, String> checkToken(HttpServletRequest req, HttpServletResponse resp, String reqUrl, Pair<Boolean, String> showLogin)
	{
		String solutionName = getSolutionNameFromURI(reqUrl);
		Pair<FlattenedSolution, Boolean> _fs = AngularIndexPageWriter.getFlattenedSolution(solutionName, null, req, resp);
		FlattenedSolution fs = _fs.getLeft();
		if (fs == null)
		{
			log.error("The solution could not be found.");
			return showLogin;
		}

		String id_token = req.getParameter("id_token");
		String refreshToken = null;
		JSONObject auth = null;
		if (req.getParameter(CODE) != null)
		{
			String nonceState = req.getParameter(OAuthParameters.state.name());
			auth = getNonce(req.getServletContext(), nonceState);
			if (auth == null)
			{
				auth = getNonce(req.getServletContext(), req.getParameter(OAuthParameters.nonce.name()));
				if (auth == null)
				{
					log.error("Cannot get the oauth config. The nonce/state is not valid.");
					return showLogin;
				}
			}
			OAuth20Service service = OAuthUtils.createOauthService(req, auth, new HashMap<>());
			try
			{
				AccessTokenRequestParams accessTokenRequestParams = AccessTokenRequestParams.create(req.getParameter(CODE));
				if (auth.has(OAuthParameters.code_verifier.name()))
				{
					accessTokenRequestParams.pkceCodeVerifier(auth.getString(OAuthParameters.code_verifier.name()));
				}
				OAuth2AccessToken access = service.getAccessToken(accessTokenRequestParams);
				if (access instanceof OpenIdOAuth2AccessToken accessToken)
				{
					refreshToken = accessToken.getRefreshToken();
					id_token = accessToken.getOpenIdToken();
				}
				else
				{
					refreshToken = access.getRefreshToken();
					if (id_token == null)
					{
						log.error("The id_token is not retrieved.");
						return showLogin;
					}
				}
			}
			catch (Exception e)
			{
				log.error("Could not get the id and refresh tokens.");
				return showLogin;
			}
		}
		else
		{
			id_token = req.getParameter("id_token");
		}

		if (!Utils.stringIsEmpty(id_token))
		{
			DecodedJWT decodedJWT = JWT.decode(id_token);
			if (refreshToken == null)
			{
				refreshToken = decodedJWT.getClaim(StatelessLoginHandler.REFRESH_TOKEN).asString();
			}
			if (JWTValidator.checkOauthIdToken(showLogin, fs.getSolution(), fs.getSolution().getAuthenticator(), decodedJWT, req, resp, refreshToken,
				true))
			{
				return showLogin;
			}
			else if (fs.getSolution().getAuthenticator() != AUTHENTICATOR_TYPE.SERVOY_CLOUD)
			{
				handleLoginFailed(req, resp, _fs, auth);
			}
		}
		return showLogin;
	}

	private static void handleLoginFailed(HttpServletRequest req, HttpServletResponse resp, Pair<FlattenedSolution, Boolean> _fs, JSONObject auth)
	{
		if (auth == null) return;
		String loginFailedUrl = auth.optString(OAuthParameters.login_failed_url.name(), null);
		if (loginFailedUrl != null && !loginFailedUrl.isBlank())
		{
			try
			{
				resp.sendRedirect(loginFailedUrl);
			}
			catch (IOException e)
			{
				log.error("Could not redirect to the login failed url.", e);
			}
		}
		else
		{
			String html = null;
			try (InputStream rs = OAuthHandler.class.getResourceAsStream("error.html"))
			{
				html = IOUtils.toString(rs, Charset.forName("UTF-8"));
				if (_fs != null)
				{
					Solution sol = _fs.getLeft().getSolution();
					I18NTagResolver i18nProvider = new I18NTagResolver(req.getLocale(), sol);
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
								return "Your account is not properly setup. Please contact your admin";
							}
							return name;
						}
					}, null);

				}
				HTMLWriter.writeHTML(req, resp, html);
			}
			catch (IOException e)
			{
				log.error("Could not show an error page.", e);
			}
		}
	}

	private static void extractFromFragment(HttpServletRequest req, HttpServletResponse resp, String reqUrl) throws IOException
	{
		String scheme = req.getScheme();
		String serverName = req.getServerName();
		int serverPort = req.getServerPort();
		String contextPath = req.getContextPath();
		String queryString = req.getQueryString();

		StringBuilder url = new StringBuilder();
		url.append(scheme).append("://").append(serverName);
		if (serverPort != 80 && serverPort != 443)
		{
			url.append(":").append(serverPort);
		}
		url.append(contextPath);
		Path path = Paths.get(reqUrl.substring(reqUrl.indexOf(AngularIndexPageWriter.SOLUTIONS_PATH))).normalize();
		for (int i = 0; i < path.getNameCount(); i++)
		{
			String pathInfo = path.getName(i).toString();
			if ("svy_oauth".equals(pathInfo))
			{
				continue;
			}
			url.append("/" + pathInfo);
		}
		if (queryString != null && queryString.contains("svy_remove_id_token"))
		{
			//do not redirect again and again to extract the id_token, most likely it's not there
			throw new IOException("The id_token could not be retrieved." + req.getParameter("error_description"));
		}

		url.append("?");
		url.append("svy_remove_id_token=true&");

		if (queryString != null)
		{
			url.append(queryString).append('&');
		}

		resp.setContentType("text/html");
		PrintWriter out = resp.getWriter();
		out.println("<html>");
		out.println("<head>");
		out.println("<script type=\"text/javascript\">");
		out.println("function redirectToSolution() {");
		out.println(" var url = '" + StringEscapeUtils.escapeEcmaScript(url.toString()) + "'+window.location.hash.substring(1);");
		out.println("  window.location.href = url;");
		out.println("  }");
		out.println(" window.onload = redirectToSolution;");
		out.println(" </script>");
		out.println("</head>");
		out.println("<body>");
		out.println("</body>");
		out.println("</html>");
	}

	//TODO refactor (Angular)IndexPageFilter?
	public static final String SOLUTIONS_PATH = "/solution/";

	private static String getSolutionNameFromURI(String uri)
	{
		int solutionIndex = uri.indexOf(SOLUTIONS_PATH);
		int solutionEndIndex = uri.indexOf("/", solutionIndex + SOLUTIONS_PATH.length() + 1);
		if (solutionEndIndex == -1) solutionEndIndex = uri.length();
		if (solutionIndex >= 0 && solutionEndIndex > solutionIndex)
		{
			String possibleSolutionName = uri.substring(solutionIndex + SOLUTIONS_PATH.length(), solutionEndIndex);
			// skip all names that have a . in them
			if (possibleSolutionName.contains(".")) return null;
			return StringEscapeUtils.escapeHtml4(possibleSolutionName);
		}
		return null;
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
			Map<String, JSONObject> cache = (Map<String, JSONObject>)context.getAttribute(OAuthParameters.nonce.name());
			return cache.get(nonceString);
		}
		return null;
	}

	public static boolean refreshOAuthTokenIfPossible(Pair<Boolean, String> needToLogin, Solution solution, SvyID oldToken, HttpServletRequest request,
		HttpServletResponse response)
	{
		String refresh_token = oldToken.getStringClaim(StatelessLoginHandler.REFRESH_TOKEN);
		if (refresh_token != null)
		{
			JSONObject properties = solution.getCustomProperties();
			if (properties != null && properties.has(StatelessLoginHandler.OAUTH_CUSTOM_PROPERTIES))
			{
				JSONObject auth = properties.getJSONObject(StatelessLoginHandler.OAUTH_CUSTOM_PROPERTIES);
				OAuth20Service service = OAuthUtils.createOauthService(request, auth, new HashMap<>());
				if (service != null)
				{
					try
					{
						OpenIdOAuth2AccessToken token = (OpenIdOAuth2AccessToken)service.refreshAccessToken(refresh_token);
						String id_token = token.getOpenIdToken();
						DecodedJWT decodedJWT = JWT.decode(id_token);
						return JWTValidator.checkOauthIdToken(needToLogin, solution, solution.getAuthenticator(), decodedJWT, request, response, refresh_token,
							false);
					}
					catch (Exception e)
					{
						log.error("Could not refresh the token", e);
					}
				}
			}
			else
			{
				log.error("Could not create the oauth service");
			}
		}
		return false;
	}

	/**
	 * @param request
	 * @param response
	 * @param solution
	 */
	public static void generateOauthCall(HttpServletRequest request, HttpServletResponse response, JSONObject auth)
	{
		StatelessLoginHandler.log.atInfo().log(() -> "Generate oauth call " + auth.optString(OAuthParameters.api.name(), "Custom"));
		String id_token = HTMLWriter.getExistingIdToken(request);
		Map<String, String> additionalParameters = new HashMap<>();
		if (!Utils.stringIsEmpty(id_token))
		{
			try
			{
				DecodedJWT decodedJWT = JWT.decode(id_token);
				if (!"svy".equals(decodedJWT.getIssuer()))
				{
					//id token which is rejected by the authenticator, show the prompt
					additionalParameters.put("prompt", "consent"); // should this be select_account ?
					StatelessLoginHandler.log.info("The id_token could not be verified with the authenticator, show consent screen.");
				}
			}
			catch (Exception e)
			{
				log.error("The existing id_token is not valid, show consent screen.", e);
			}
		}
		OAuth20Service service = OAuthUtils.createOauthService(request, auth, additionalParameters);
		if (service != null)
		{
			try
			{
				AuthorizationUrlBuilder authorizationUrlBuilder = service.createAuthorizationUrlBuilder();
				if (auth.has(OAuthParameters.code_challenge_method.name()) && service.getResponseType().contains(CODE))
				{
					setPKCE(authorizationUrlBuilder, auth);
				}
				final String authorizationUrl = authorizationUrlBuilder//
					.additionalParams(additionalParameters).build();
				StatelessLoginHandler.log.atInfo().log(() -> "authorization url " + authorizationUrl);
				StatelessLoginHandler.log.atInfo().log(() -> "Writing the auto login page.");
				StringBuilder sb = new StringBuilder();
				sb.append("<!DOCTYPE html>").append("\n")
					.append("<html lang=\"en\">").append("\n")
					.append("<head>").append("\n")
					.append("    <meta charset=\"UTF-8\">").append("\n")
					.append("<base href=\"").append("\n")
					.append(HTMLWriter.getPath(request)).append("\n")
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
				log.error("Error writing the auto login page.", e);
			}
		}
		else
		{
			log.error("Could not create the oauth service for config " + auth);
		}
	}

	private static void setPKCE(AuthorizationUrlBuilder authorizationUrlBuilder, JSONObject auth)
	{
		String codeChallengeMethod = auth.optString(OAuthParameters.code_challenge_method.name(), "S256");
		String codeVerifier = null;
		if ("S256".equalsIgnoreCase(codeChallengeMethod))
		{
			authorizationUrlBuilder = authorizationUrlBuilder.initPKCE();
			PKCE pkce = authorizationUrlBuilder.getPkce();
			codeVerifier = pkce.getCodeVerifier();
		}
		else
		{
			//plain, but is not recommended
			byte[] randomBytes = new byte[32];
			new SecureRandom().nextBytes(randomBytes);
			codeVerifier = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

			// plain = challenge == verifier
			PKCE pkce = new PKCE();
			pkce.setCodeVerifier(codeVerifier);
			pkce.setCodeChallengeMethod(PKCECodeChallengeMethod.PLAIN);
			authorizationUrlBuilder = authorizationUrlBuilder.pkce(pkce);
		}
		auth.put(OAuthParameters.code_verifier.name(), codeVerifier);
	}

	public static void redirectToOAuthLogin(HttpServletRequest request, HttpServletResponse response, Solution solution)
	{
		Object properties = solution.getCustomProperty(new String[] { StatelessLoginHandler.OAUTH_CUSTOM_PROPERTIES });
		if (properties instanceof String s)
		{
			JSONObject auth = new JSONObject(s);
			generateOauthCall(request, response, auth);
		}
		else
		{
			log.error("The oauth configuration is missing for solution " + solution.getName() +
				". Please create it using the button from the authenticator type property in the properties view.");
		}
	}

	public static void revokeToken(Solution solution, DecodedJWT jwt)
	{
		JSONObject properties = solution.getCustomProperties();
		if (properties != null && properties.has(StatelessLoginHandler.OAUTH_CUSTOM_PROPERTIES))
		{
			JSONObject auth = properties.getJSONObject(StatelessLoginHandler.OAUTH_CUSTOM_PROPERTIES);
			OAuth20Service service = OAuthUtils.createOauthService(auth, new HashMap<>(), null);
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

	public static void redirectToAuthenticator(HttpServletRequest request, HttpServletResponse response, Solution solution) throws ServletException
	{
		Solution authenticatorModule = AuthenticatorManager.findAuthenticator(solution);
		if (authenticatorModule != null)
		{
			JSONObject json = new JSONObject();
			Map<String, String[]> parameters = request.getParameterMap();
			for (Map.Entry<String, String[]> entry : parameters.entrySet())
			{
				String[] values = entry.getValue();
				for (String value : values)
				{
					json.put(entry.getKey(), StringEscapeUtils.escapeHtml4(value));
				}
			}

			JSONArray args = new JSONArray();
			args.put(json);
			JSONObject config = getConfig(solution, authenticatorModule, args);
			if (config != null)
			{
				generateOauthCall(request, response, config);
			}
			else
			{
				throw new ServletException("Incorrect settings for oauth, missing config.");
			}
		}
	}

	private static JSONObject getConfig(Solution solution, Solution authenticatorModule, JSONArray args)
	{
		String method = GET_OAUTH_CONFIG;
		JSONObject properties = solution.getCustomProperties();
		ScriptMethod sm = null;
		if (properties != null && properties.has(GET_OAUTH_CONFIG))
		{
			UUID uuid = Utils.getAsUUID(properties.get(GET_OAUTH_CONFIG), false);
			sm = (ScriptMethod)authenticatorModule.getAllObjectsAsList().stream().filter(persist -> persist.getUUID().equals(uuid))
				.findFirst().orElse(null);
		}
		else
		{
			sm = authenticatorModule.getScriptMethod("globals", GET_OAUTH_CONFIG);
		}
		if (sm == null)
		{
			log.error("The authenticator does not have a method for getting the oauth config " + GET_OAUTH_CONFIG +
				". Please select it from the properties view for the authenticator OAUTH_AUTHENTICATOR property.");
			return null;
		}

		Credentials credentials = new Credentials(null, authenticatorModule.getName(), ScopesUtils.getScopeString(sm), args.toString());
		IApplicationServer applicationServer = ApplicationServerRegistry.getService(IApplicationServer.class);
		try
		{
			ClientLogin login = applicationServer.login(credentials);
			if (login != null && login.getJsReturn() != null)
			{
				JSONObject config = new JSONObject(login.getJsReturn());
				return config;
			}
			else
			{
				log.error("The authenticator did not return an oauth config.");
			}
		}
		catch (Exception e)
		{
			log.error("Could not call the authenticator.", e);
		}
		return null;
	}

	public static boolean isOAuthRequest(HttpServletRequest request)
	{
		return !request.getRequestURI().contains("/designer") &&
			(request.getParameter("svy_remove_id_token") != null || request.getRequestURI().contains("/svy_oauth/"));
	}
}