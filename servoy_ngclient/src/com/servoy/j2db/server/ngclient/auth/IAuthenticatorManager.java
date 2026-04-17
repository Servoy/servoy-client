/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2026 Servoy BV

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

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import com.servoy.j2db.util.Pair;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author emera
 */
public interface IAuthenticatorManager
{

	/**
	 * @param request
	 * @param response
	 * @param customHTML
	 */
	void writeLoginPage(HttpServletRequest request, HttpServletResponse response, String customHTML)
		throws ServletException, UnsupportedEncodingException, IOException;

	public String getLoginHTML() throws IOException;

//	public String getLoginScripts()


	public boolean checkPermissions(String username, String password, boolean remember, SvyID oldToken, Pair<Boolean, String> needToLogin,
		HttpServletRequest request);

//	public void checkUser(String username, String password, boolean remember, SvyID oldToken, Pair<Boolean, String> needToLogin,
//		HttpServletRequest request, HttpServletResponse response);
//
//	public void logoutAndRevokeToken(HttpSession httpSession);
}
