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

/**
 * @author emera
 */
@SuppressWarnings("nls")
public class LoginResult
{
	private boolean authenticated;
	private String token;
	private String customHtml;
	private String returnValue;
	private boolean responseHandled;

	private LoginResult()
	{
	}

	public static LoginResult needsLogin()
	{
		LoginResult result = new LoginResult();
		result.authenticated = false;
		return result;
	}

	public static LoginResult needsLogin(String customHtml)
	{
		LoginResult result = new LoginResult();
		result.authenticated = false;
		result.customHtml = customHtml;
		return result;
	}

	public static LoginResult authenticated(String token)
	{
		LoginResult result = new LoginResult();
		result.authenticated = true;
		result.token = token;
		return result;
	}

	public static LoginResult authenticated(String token, String returnValue)
	{
		LoginResult result = new LoginResult();
		result.authenticated = true;
		result.token = token;
		result.returnValue = returnValue;
		return result;
	}

	public static LoginResult handled()
	{
		LoginResult result = new LoginResult();
		result.authenticated = false;
		result.responseHandled = true;
		return result;
	}

	public boolean isAuthenticated()
	{
		return authenticated;
	}

	public void setAuthenticated(boolean authenticated)
	{
		this.authenticated = authenticated;
	}

	public String getToken()
	{
		return token;
	}

	public void setToken(String token)
	{
		this.token = token;
	}

	public String getCustomHtml()
	{
		return customHtml;
	}

	public void setCustomHtml(String customHtml)
	{
		this.customHtml = customHtml;
	}

	public String getReturnValue()
	{
		return returnValue;
	}

	public void setReturnValue(String returnValue)
	{
		this.returnValue = returnValue;
	}

	public boolean isResponseHandled()
	{
		return responseHandled;
	}

	public void setResponseHandled(boolean responseHandled)
	{
		this.responseHandled = responseHandled;
	}
}
