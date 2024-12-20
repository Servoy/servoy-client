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

package com.servoy.j2db.server.ngclient;

import java.util.Map;

import org.json.JSONObject;

import com.github.scribejava.apis.GoogleApi20;
import com.github.scribejava.apis.LinkedInApi20;
import com.github.scribejava.apis.MicrosoftAzureActiveDirectory20Api;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.servoy.j2db.server.ngclient.auth.AppleIDApi;
import com.servoy.j2db.server.ngclient.auth.OktaApi;
import com.servoy.j2db.util.Debug;

/**
 * Constants and helper methods for oauth.
 * @author emera
 */
public class OAuthUtils
{
	public static final String APPLE = "Apple";
	public static final String LINKED_IN = "LinkedIn";
	public static final String GOOGLE = "Google";
	public static final String MICROSOFT_AD = "Microsoft AD";
	public static final String OKTA = "Okta";

	public static final String GOOGLE_JWKS = "https://www.googleapis.com/oauth2/v3/certs";
	public static final String MICROSOFT_JWKS = "https://login.microsoftonline.com/{tenant}/discovery/v2.0/keys";
	public static final String APPLE_JWKS = "https://appleid.apple.com/auth/keys";
	public static final String LINKEDIN_JWKS = "https://www.linkedin.com/oauth/v2/certs";
	public static final String OKTA_JWKS = "https://{domain}/oauth2/default/v1/keys";

	static final String NONCE = "nonce";
	public static final String JWKS_URI = "jwks_uri";
	public static final String ACCESS_TOKEN_ENDPOINT = "accessTokenEndpoint";
	public static final String REFRESH_TOKEN_ENDPOINT = "refreshTokenEndpoint";
	public static final String REVOKE_TOKEN_ENDPOINT = "revokeTokenEndpoint";
	public static final String AUTHORIZATION_BASE_URL = "authorizationBaseUrl";
	public static final String OAUTH_API = "api";
	public static final String DEFAULT_SCOPE = "defaultScope";
	public static final String API_SECRET = "apiSecret";
	public static final String CLIENT_ID = "clientId";


	public static String getResponseType(String api, JSONObject auth)
	{
		if (GOOGLE.equals(api) || MICROSOFT_AD.equals(api) || "Microsoft".equals(api))
		{
			return "offline".equals(auth.optString("access_type")) ? "code" : "id_token";
		}
		else if (OKTA.equals(api))
		{
			return auth.optString(DEFAULT_SCOPE, "").contains("offline_access") ? "code" : "id_token";
		}
		else if (APPLE.equals(api))
		{
			return "code id_token";
		}
		return "code";
	}

	static DefaultApi20 getApiInstance(String provider, JSONObject auth) throws Exception
	{
		JSONObject customParameters = auth.optJSONObject("customParameters");
		switch (provider)
		{
			case "Microsoft" :
			case MICROSOFT_AD :
				String tenant = customParameters != null ? customParameters.optString("tenant", null) : null;
				return tenant != null ? MicrosoftAzureActiveDirectory20Api.custom(tenant) : MicrosoftAzureActiveDirectory20Api.instance();
			case GOOGLE :
				return GoogleApi20.instance();
			case LINKED_IN :
				return LinkedInApi20.instance();
			case APPLE :
				return AppleIDApi.instance();
			case OKTA :
				return OktaApi.custom(customParameters.optString("domain"));
			default :
				throw new Exception("Could not create an OAuth Api.");
		}
	}

	public static String getJWKS_URI(String api)
	{
		switch (api)
		{
			case "Microsoft" :
			case MICROSOFT_AD :
				return MICROSOFT_JWKS;
			case GOOGLE :
				return GOOGLE_JWKS;
			case LINKED_IN :
				return LINKEDIN_JWKS;
			case APPLE :
				return APPLE_JWKS;
			case OKTA :
				return OKTA_JWKS;
			default :
				return null;
		}
	}

	public static OAuth20Service createOauthService(JSONObject auth, Map<String, String> additionalParameters, String serverURL)
	{
		ServiceBuilder builder = new ServiceBuilder(auth.optString(CLIENT_ID));
		String api = null;
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
				case AUTHORIZATION_BASE_URL :
				case ACCESS_TOKEN_ENDPOINT :
				case CLIENT_ID :
				case JWKS_URI :
				case "customParameters" :
					//skip
					break;
				default :
					additionalParameters.put(key, auth.getString(key));
			}
		}
		String responseType = getResponseType(api, auth);
		builder.responseType(responseType);
		if (responseType.contains("code"))
		{
			additionalParameters.put("state", additionalParameters.get(NONCE));
		}
		String oauth_path = responseType.equals("id_token") ? "svy_oauth/" : "";
		builder.callback(serverURL + oauth_path + "index.html");
		try
		{
			DefaultApi20 apiInstance = null;
			if (api != null)
			{
				apiInstance = getApiInstance(api, auth);
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
						return auth.optString(REFRESH_TOKEN_ENDPOINT, null);
					}

					@Override
					public String getRevokeTokenEndpoint()
					{
						return auth.optString(REVOKE_TOKEN_ENDPOINT, null);
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
}
