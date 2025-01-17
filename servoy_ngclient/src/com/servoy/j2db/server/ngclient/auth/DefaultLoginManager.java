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

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Pair;

/**
 * @author emera
 */
public class DefaultLoginManager
{
	private static final Logger log = LoggerFactory.getLogger("stateless.login");

	public static boolean checkDefaultLoginPermissions(String username, String password, boolean rememberUser, SvyID oldToken,
		Pair<Boolean, String> needToLogin)
	{
		try
		{
			String clientid = ApplicationServerRegistry.get().getClientId();
			if (oldToken != null)
			{
				long passwordLastChagedTime = ApplicationServerRegistry.get().getUserManager().getPasswordLastSet(clientid,
					oldToken.getUserID());
				if (passwordLastChagedTime > oldToken.getLongClaim(SvyID.LAST_LOGIN))
				{
					needToLogin.setLeft(Boolean.TRUE);
					needToLogin.setRight(null);
					return false;
				}
			}

			String uid = oldToken != null ? oldToken.getUserID()
				: ApplicationServerRegistry.get().checkDefaultServoyAuthorisation(oldToken.getUsername(), password);
			if (uid != null)
			{
				String[] permissions = ApplicationServerRegistry.get().getUserManager().getUserGroups(clientid, uid);
				if (permissions.length > 0 && (oldToken == null || Arrays.equals(oldToken.getPermissions(), permissions)))
				{
					SvyTokenBuilder builder = new SvyTokenBuilder(username, uid, permissions)//
						.withRememberUser(Boolean.valueOf(rememberUser));
					String token = builder.sign();
					needToLogin.setLeft(Boolean.FALSE);
					needToLogin.setRight(token);
					return true;
				}
			}
		}
		catch (Exception e)
		{
			log.error("Default login problem.", e);
		}
		return false;
	}

}
