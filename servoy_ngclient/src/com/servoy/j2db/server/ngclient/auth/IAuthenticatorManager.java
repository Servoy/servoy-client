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
 * Interface for authenticator managers that handle stateless login.
 * Each authenticator type (Default, OAuth, Cloud, Authenticator module) implements this interface
 * to provide its own login page rendering, credential checking, and user verification logic.
 *
 * @author emera
 */
public interface IAuthenticatorManager
{
	/**
	 * Write the login page HTML to the response (or redirect to an OAuth provider).
	 *
	 * @param request the HTTP request
	 * @param response the HTTP response
	 * @param customHTML optional custom HTML to render instead of the default login page
	 */
	void writeLoginPage(HttpServletRequest request, HttpServletResponse response, String customHTML)
		throws ServletException, UnsupportedEncodingException, IOException;

	/**
	 * Get the login scripts to inject into the login page.
	 *
	 * @param request the HTTP request
	 * @param csrfToken the CSRF token to embed in the scripts
	 * @return the scripts HTML string
	 */
	String getLoginScripts(HttpServletRequest request, long csrfToken);

	/**
	 * Check permissions for a user (e.g. when the token is submitted from local storage or credentials are provided).
	 *
	 * @param username the username
	 * @param password the password
	 * @param remember whether to remember the user
	 * @param oldToken the existing token (if any, e.g. for refresh)
	 * @param needToLogin pair indicating whether login is needed and the token
	 * @param request the HTTP request
	 * @return true if the permissions were verified successfully
	 */
	boolean checkPermissions(String username, String password, boolean remember, SvyID oldToken,
		Pair<Boolean, String> needToLogin, HttpServletRequest request);

	/**
	 * Check user credentials. This is called when the user submits the login form
	 * or when a token needs to be refreshed (e.g. OAuth token refresh).
	 *
	 * @param username the username
	 * @param password the password
	 * @param remember whether to remember the user
	 * @param oldToken the existing token (if any, e.g. for refresh)
	 * @param needToLogin pair indicating whether login is needed and the token
	 * @param request the HTTP request
	 * @param response the HTTP response
	 * @return true if the user was verified successfully
	 */
	boolean checkUser(String username, String password, boolean remember, SvyID oldToken,
		Pair<Boolean, String> needToLogin, HttpServletRequest request, HttpServletResponse response);

	default boolean requiresCSRFForCheckUser()
	{
		return true;
	}
}
