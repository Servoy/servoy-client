/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2024 Servoy BV

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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.json.JSONObject;

import com.github.scribejava.apis.GoogleApi20;
import com.github.scribejava.apis.LinkedInApi20;
import com.github.scribejava.apis.MicrosoftAzureActiveDirectory20Api;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.servoy.j2db.util.Debug;

/**
 * Constants and helper methods for oauth.
 * @author emera
 */
public class OAuthUtils
{
	private static final String OAUTHCONFIGREQUEST = "oauthconfigrequest";

	public enum Provider
	{
		Google(
			"https://www.googleapis.com/oauth2/v3/certs",
			"openid email",
			"https://developers.google.com/identity/protocols/oauth2/javascript-implicit-flow#oauth-2.0-endpoints",
			false),
		Microsoft(
			"https://login.microsoftonline.com/{tenant}/discovery/v2.0/keys",
			"openid email",
			"https://learn.microsoft.com/en-us/entra/identity-platform/v2-oauth2-implicit-grant-flow#send-the-sign-in-request",
			false),
		Apple(
			"https://appleid.apple.com/auth/keys",
			"name email",
			"https://developer.apple.com/documentation/sign_in_with_apple/sign_in_with_apple_rest_api",
			true),
		Okta(
			"https://{domain}/oauth2/default/v1/keys",
			"openid profile email",
			"https://developer.okta.com/docs/api/openapi/okta-oauth/guides/overview",
			true),
		LinkedIn(
			"https://www.linkedin.com/oauth/v2/certs",
			"openid email",
			"https://learn.microsoft.com/en-us/linkedin/shared/authentication/authorization-code-flow?tabs=HTTPS1",
			false),
		Custom(null, null, null, false);

		private final String jwksUri;
		private final String defaultScope;
		private final String docs;
		private final boolean addState;

		Provider(String jwksUri, String defaultScope, String docs, boolean addState)
		{
			this.jwksUri = jwksUri;
			this.defaultScope = defaultScope;
			this.docs = docs;
			this.addState = addState;
		}

		public String getJwksUri()
		{
			return jwksUri;
		}

		public String getDefaultScope()
		{
			return defaultScope;
		}

		public String getDocs()
		{
			return docs;
		}

		public boolean shouldSendStateParam(String responseType)
		{
			return addState || responseType != null && responseType.contains("code");
		}
	}

	public enum OAuthParameters
	{
		nonce,
		jwks_uri,
		accessTokenEndpoint,
		refreshTokenEndpoint,
		revokeTokenEndpoint,
		authorizationBaseUrl,
		api,
		defaultScope,
		apiSecret,
		clientId,
		state;
	}

	private static String getResponseType(Provider provider, JSONObject auth, Map<String, String> additionalParameters)
	{
		switch (provider)
		{
			case Google :
			case Microsoft :
				String accessType = additionalParameters.containsKey("access_type") ? //
					additionalParameters.get("access_type") : auth.optString("access_type");
				return "offline".equals(accessType) ? "code" : "id_token";
			case Okta :
				return auth.optString(OAuthParameters.defaultScope.name(), "")
					.contains("offline_access") ? "code" : "id_token";
			case Apple :
				return "code id_token";
			default :
				return "code";
		}
	}

	static DefaultApi20 getApiInstance(Provider provider, JSONObject auth) throws Exception
	{
		JSONObject customParameters = auth.optJSONObject("customParameters");
		switch (provider)
		{
			case Microsoft :
				String tenant = customParameters != null ? customParameters.optString("tenant", null) : null;
				return tenant != null ? MicrosoftAzureActiveDirectory20Api.custom(tenant) : MicrosoftAzureActiveDirectory20Api.instance();
			case Google :
				return GoogleApi20.instance();
			case LinkedIn :
				return LinkedInApi20.instance();
			case Apple :
				return AppleIDApi.instance();
			case Okta :
				return OktaApi.custom(customParameters.optString("domain"));
			default :
				throw new Exception("Could not create an OAuth API instance.");
		}
	}

	public static OAuth20Service createOauthService(HttpServletRequest request, JSONObject auth,
		Map<String, String> additionalParameters)
	{
		String nonce = OAuthUtils.generateNonce(request.getServletContext(), auth);
		additionalParameters.put(OAuthParameters.nonce.name(), nonce);
		HttpSession session = request.getSession(false);
		if (session != null && session.getAttribute("logout") != null)
		{
			session.removeAttribute("logout");
			additionalParameters.put("prompt", "consent");
		}

		if (!request.getParameterMap().isEmpty())
		{
			StringBuilder state = new StringBuilder();
			request.getParameterMap().forEach((key, values) -> {
				String value = String.join(",", values);
				try
				{
					String encodedKey = URLEncoder.encode(key, "UTF-8");
					if (OAUTHCONFIGREQUEST.equals(encodedKey))
					{
						return; // skip oauthconfigrequest
					}
					String encodedValue = URLEncoder.encode(value, "UTF-8");
					state.append(encodedKey).append("=").append(encodedValue).append("&");
				}
				catch (UnsupportedEncodingException e)
				{
					Debug.error("Error encoding key or value", e);
				}
			});

			if (state.length() > 0 && state.charAt(state.length() - 1) == '&')
			{
				state.append("svyuuid=").append(nonce);
			}

			if (state.length() > 0)
			{
				additionalParameters.put("state", state.toString());
				Map<String, JSONObject> cache = (Map<String, JSONObject>)request.getServletContext().getAttribute(OAuthParameters.nonce.name());
				cache.put(additionalParameters.get("state"), auth);
			}
		}
		return createOauthService(auth, additionalParameters, StatelessLoginUtils.getServerURL(request));
	}

	public static OAuth20Service createOauthService(JSONObject auth, Map<String, String> additionalParameters, String serverURL)
	{
		ServiceBuilder builder = new ServiceBuilder(auth.optString(OAuthParameters.clientId.name()));
		String api = null;
		for (String key : auth.keySet())
		{
			OAuthParameters param;
			try
			{
				param = OAuthParameters.valueOf(key);
			}
			catch (IllegalArgumentException e)
			{
				param = null;
			}

			if (param != null)
			{
				switch (param)
				{
					case apiSecret :
						builder.apiSecret(auth.getString(key));
						break;
					case defaultScope :
						builder.defaultScope(auth.getString(key));
						break;
					case api :
						api = auth.getString(key);
						break;
					default :
						// Skip known parameters that do not require processing here
						break;
				}
			}
			else if (!"customParameters".equals(key))
			{
				additionalParameters.put(key, auth.getString(key));
			}
		}

		Provider provider = Provider.valueOf(api);
		String responseType = getResponseType(provider, auth, additionalParameters);
		builder.responseType(responseType);
		if (additionalParameters.containsKey(OAuthParameters.state.name()) || provider.shouldSendStateParam(responseType))
		{
			additionalParameters.put(OAuthParameters.state.name(),
				additionalParameters.getOrDefault(OAuthParameters.state.name(), OAuthParameters.nonce.name()));
		}
		if (serverURL != null)
		{
			String oauthPath = serverURL.contains("/svy_oauth/") ? "" : "svy_oauth/";
			builder.callback(serverURL + oauthPath + "index.html");
		}

		try
		{
			DefaultApi20 apiInstance = getApiInstance(provider, auth);
			return builder.build(apiInstance);
		}
		catch (Exception e)
		{
			Debug.error("Cannot create the OAuth service.", e);
		}
		return null;
	}

	public static String generateNonce(ServletContext context, JSONObject oauth)
	{
		Map<String, JSONObject> cache = (Map<String, JSONObject>)context.getAttribute(OAuthParameters.nonce.name());
		String nonce = UUID.randomUUID().toString();
		cache.put(nonce, oauth);
		return nonce;
	}
}