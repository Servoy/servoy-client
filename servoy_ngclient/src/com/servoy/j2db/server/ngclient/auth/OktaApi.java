/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2021 Servoy BV

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

import com.github.scribejava.apis.openid.OpenIdJsonTokenExtractor;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.model.Verb;

/**
 * @author emera
 */
public class OktaApi extends DefaultApi20
{

	private final String tokenEndpoint;
	private final String authorizationBaseUrl;
	private final String revokeTokenEndpoint;

	public static OktaApi custom(String domain)
	{
		return new OktaApi(domain);
	}

	protected OktaApi(String domain)
	{
		if (domain == null) throw new IllegalArgumentException("Must specify the domain for the OKTA api.");
		tokenEndpoint = "https://" + domain + "/oauth2/default/v1/token";
		authorizationBaseUrl = "https://" + domain + "/oauth2/default/v1/authorize";
		revokeTokenEndpoint = "https://" + domain + "/oauth2/v1/revoke";
	}

	@Override
	public Verb getAccessTokenVerb()
	{
		return Verb.POST;
	}

	@Override
	public String getAccessTokenEndpoint()
	{
		return tokenEndpoint;
	}


	@Override
	protected String getAuthorizationBaseUrl()
	{
		return authorizationBaseUrl;
	}

	@Override
	public String getRevokeTokenEndpoint()
	{
		return revokeTokenEndpoint;
	}

	@Override
	public OpenIdJsonTokenExtractor getAccessTokenExtractor()
	{
		return OpenIdJsonTokenExtractor.instance();
	}
}