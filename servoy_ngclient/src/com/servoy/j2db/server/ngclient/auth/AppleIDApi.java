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

import java.io.OutputStream;

import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.httpclient.HttpClient;
import com.github.scribejava.core.httpclient.HttpClientConfig;
import com.github.scribejava.core.oauth2.clientauthentication.ClientAuthentication;
import com.github.scribejava.core.oauth2.clientauthentication.RequestBodyAuthenticationScheme;

/**
 * @author emera
 */
public class AppleIDApi extends DefaultApi20
{

	protected AppleIDApi()
	{
	}

	private static class InstanceHolder
	{
		private static final AppleIDApi INSTANCE = new AppleIDApi();
	}

	public static AppleIDApi instance()
	{
		return InstanceHolder.INSTANCE;
	}

	@Override
	public String getAccessTokenEndpoint()
	{
		return "https://appleid.apple.com/auth/token";
	}

	@Override
	protected String getAuthorizationBaseUrl()
	{
		return "https://appleid.apple.com/auth/authorize";
	}

	@Override
	public String getRevokeTokenEndpoint()
	{
		return "https://appleid.apple.com/auth/revoke";
	}

	@Override
	public ClientAuthentication getClientAuthentication()
	{
		return RequestBodyAuthenticationScheme.instance();
	}

	@Override
	public AppleIDService createService(String apiKey, String apiSecret, String callback, String defaultScope,
		String responseType, OutputStream debugStream, String userAgent, HttpClientConfig httpClientConfig,
		HttpClient httpClient)
	{
		return new AppleIDService(this, apiKey, apiSecret, callback, defaultScope, responseType, debugStream,
			userAgent, httpClientConfig, httpClient);
	}
}
