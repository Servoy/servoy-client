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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;

/**
 * @author emera
 */
public class SvyID
{
	private final DecodedJWT decoded;
	public static final String REMEMBER = "remember";
	public static final String UID = "uid";
	public static final String LAST_LOGIN = "last_login";
	public static final String USERNAME = "username";
	public static final String TENANTS = "tenants";
	public static final String PERMISSIONS = "permissions";

	private static final Logger log = LoggerFactory.getLogger("stateless.login");

	public SvyID(String id_token)
	{
		super();
		try
		{
			decoded = JWT.decode(id_token);
		}
		catch (JWTDecodeException e)
		{
			log.error("Not a valid JWT format", e);
			throw e;
		}
	}

	public String getUserID()
	{
		return decoded.getClaim(UID).asString();
	}

	public String getUsername()
	{
		return decoded.getClaim(USERNAME).asString();
	}

	public String[] getPermissions()
	{
		if (decoded.getClaims().containsKey(SvyID.PERMISSIONS))
		{
			return decoded.getClaim(PERMISSIONS).asArray(String.class);
		}
		return null;
	}

	public Object[] getTenants()
	{
		Object[] tenants = null;
		if (decoded.getClaims().containsKey(TENANTS))
		{
			return decoded.getClaim(TENANTS).asArray(Object.class);
		}
		return tenants;
	}

	public boolean rememberUser()
	{
		return decoded.getClaims().containsKey(REMEMBER) ? //
			decoded.getClaim(REMEMBER).asBoolean().booleanValue() : false;
	}

	public String getStringClaim(String claim)
	{
		return decoded.getClaim(claim).asString();
	}

	public String getPayload()
	{
		return decoded.getPayload();
	}

	public long getLongClaim(String name)
	{
		return decoded.getClaim(name).asLong().longValue();
	}
}
