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

import java.util.Date;
import java.util.Properties;

/**
 * @author emera
 */
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator.Builder;
import com.auth0.jwt.algorithms.Algorithm;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;

/**
 *
 * @author emera
 */
public class SvyTokenBuilder
{
	private static final String ISSUER = "svy";
	private final Builder builder;
	private final Algorithm algorithm;
	private boolean lastLoginSet = false;

	public SvyTokenBuilder(String username, String uid, String[] permissions)
	{
		Properties settings = ApplicationServerRegistry.get().getServerAccess().getSettings();
		this.algorithm = Algorithm.HMAC256(settings.getProperty(StatelessLoginHandler.JWT_Password));
		this.builder = JWT.create().withIssuer(ISSUER)//
			.withClaim(StatelessLoginHandler.UID, uid)
			.withClaim(StatelessLoginHandler.USERNAME, username)
			.withArrayClaim(StatelessLoginHandler.PERMISSIONS, permissions);
	}

	public SvyTokenBuilder withClaim(String claim, String value)
	{
		if (value != null && !"".equals(value))
		{
			builder.withClaim(claim, value);
		}
		return this;
	}


	public SvyTokenBuilder withLastLogin(String lastLogin)
	{
		if (lastLogin != null)
		{
			lastLoginSet = true;
			builder.withClaim(StatelessLoginHandler.LAST_LOGIN, lastLogin);
		}
		return this;
	}

	public SvyTokenBuilder withRememberUser(Boolean rememberUser)
	{
		if (Boolean.TRUE.equals(rememberUser))
		{
			builder.withClaim(StatelessLoginHandler.REMEMBER, rememberUser);
		}
		return this;
	}

	public SvyTokenBuilder withRefreshToken(String refreshToken)
	{
		if (refreshToken != null)
		{
			builder.withClaim(StatelessLoginHandler.REFRESH_TOKEN, refreshToken);
		}
		return this;
	}

	public SvyTokenBuilder withTenants(String[] tenantsValue)
	{
		if (tenantsValue != null)
		{
			builder.withArrayClaim(StatelessLoginHandler.TENANTS, tenantsValue);
		}
		return this;
	}

	public String sign()
	{
		if (!lastLoginSet) builder.withClaim(StatelessLoginHandler.LAST_LOGIN, Long.valueOf(System.currentTimeMillis()));
		return builder
			.withExpiresAt(new Date(System.currentTimeMillis() + StatelessLoginHandler.TOKEN_AGE_IN_SECONDS * 1000))
			.sign(algorithm);
	}
}