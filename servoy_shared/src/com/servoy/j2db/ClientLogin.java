/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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

import java.io.Serializable;

/**
 * This class represents the result from a login call to the application server.
 * @author rob
 *
 */
public class ClientLogin implements Serializable
{

	private final String clientId;
	private final String userUid;
	private final String userName;
	private final String[] userGroups;
	private final String jsReturn;

	public ClientLogin(String clientId, String userUid, String userName, String[] userGroups, String jsReturn)
	{
		this.clientId = clientId;
		this.userUid = userUid;
		this.userName = userName;
		this.userGroups = userGroups;
		this.jsReturn = jsReturn;
	}

	public String getClientId()
	{
		return clientId;
	}

	public String getUserUid()
	{
		return userUid;
	}

	public String getUserName()
	{
		return userName;
	}

	public String[] getUserGroups()
	{
		return userGroups;
	}

	public String getJsReturn()
	{
		return jsReturn;
	}

}
