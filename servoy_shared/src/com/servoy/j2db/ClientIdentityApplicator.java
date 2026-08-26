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
package com.servoy.j2db;

import com.servoy.j2db.dataprocessing.ClientInfo;

/**
 * Shared utility for applying user identity (user, permissions, tenants) to any Servoy client.
 * Used by NGClientWebsocketSession (stateless NG login), REST WS, and MCP clients.
 *
 * @author Servoy
 */
public class ClientIdentityApplicator
{
	private ClientIdentityApplicator()
	{
	}

	/**
	 * Applies user identity to the given application client. Sets userUid, userName, permissions
	 * on the ClientInfo and the tenant value on the form manager.
	 *
	 * @param client the application client (NGClient, SessionClient, etc.)
	 * @param userUid the user UID
	 * @param userName the user name
	 * @param permissions the user's permissions/groups (may be null)
	 * @param tenants the tenant values (may be null)
	 */
	public static void applyIdentity(IApplication client, String userUid, String userName, String[] permissions, Object[] tenants)
	{
		ClientInfo ci = client.getClientInfo();
		ci.setUserUid(userUid);
		ci.setUserName(userName);
		if (permissions != null) ci.setUserGroups(permissions);
		client.getFoundSetManager().setRawTenantValue(tenants);
	}
}
