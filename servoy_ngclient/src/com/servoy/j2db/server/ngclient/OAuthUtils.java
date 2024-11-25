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

import com.github.scribejava.apis.GoogleApi20;
import com.github.scribejava.apis.LinkedInApi20;
import com.github.scribejava.apis.MicrosoftAzureActiveDirectory20Api;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.servoy.j2db.server.ngclient.auth.AppleIDApi;

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

	public static final String GOOGLE_JWKS = "https://www.googleapis.com/oauth2/v3/certs";
	public static final String MICROSOFT_JWKS = "https://login.microsoftonline.com/{tenant}/discovery/v2.0/keys";
	public static final String APPLE_JWKS = "https://appleid.apple.com/auth/keys";
	public static final String LINKEDIN_JWKS = "https://www.linkedin.com/oauth/v2/certs";


	public static String getResponseType(String api, Map<String, String> additionalParameters)
	{
		if (GOOGLE.equals(api) || MICROSOFT_AD.equals(api) || "Microsoft".equals(api))
		{
			return "offline".equals(additionalParameters.get("access_type")) ? "code" : "id_token";
		}
		else if (APPLE.equals(api))
		{
			return "code id_token";
		}
		return "code";
	}

	static DefaultApi20 getApiInstance(String provider, String tenant) throws Exception
	{
		switch (provider)
		{
			case "Microsoft" :
			case MICROSOFT_AD :
				return tenant != null ? MicrosoftAzureActiveDirectory20Api.custom(tenant) : MicrosoftAzureActiveDirectory20Api.instance();
			case GOOGLE :
				return GoogleApi20.instance();
			case LINKED_IN :
				return LinkedInApi20.instance();
			case APPLE :
				return AppleIDApi.instance();
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
			default :
				return null;
		}
	}
}
