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
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.oauth.AccessTokenRequestParams;
import com.github.scribejava.core.oauth.OAuth20Service;

/**
 * @author emera
 */
public class AppleIDService extends OAuth20Service
{
	public AppleIDService(DefaultApi20 api, String apiKey, String apiSecret, String callback, String defaultScope, String responseType,
		OutputStream debugStream, String userAgent, HttpClientConfig httpClientConfig, HttpClient httpClient)
	{
		super(api, apiKey, apiSecret, callback, defaultScope, responseType, debugStream, userAgent, httpClientConfig, httpClient);
	}

	@Override
	protected OAuthRequest createAccessTokenRequest(AccessTokenRequestParams params)
	{
		OAuthRequest req = super.createAccessTokenRequest(params);
		req.addHeader("Content-Type", "application/x-www-form-urlencoded");
		return req;
	}
}
