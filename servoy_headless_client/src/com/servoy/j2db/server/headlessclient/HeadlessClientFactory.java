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
package com.servoy.j2db.server.headlessclient;

import javax.servlet.ServletRequest;

/**
 * @author Jan Blok
 */
public class HeadlessClientFactory
{
	public static ISessionBean createSessionBean(ServletRequest req, String solutionname) throws Exception
	{
		return createSessionBean(req, solutionname, null, null, null);
	}

	public static ISessionBean createSessionBean(ServletRequest req, String solutionname, String username, String password) throws Exception
	{
		return createSessionBean(req, solutionname, username, password, null);
	}

	public static ISessionBean createSessionBean(ServletRequest req, String solutionname, Object[] solutionOpenMethodArgs) throws Exception
	{
		return createSessionBean(req, solutionname, null, null, solutionOpenMethodArgs);
	}

	public static ISessionBean createSessionBean(ServletRequest req, String solutionname, String username, String password, Object[] solutionOpenMethodArgs)
		throws Exception
	{
		return HeadlessClientFactoryInternal.createSessionBean(req, solutionname, username, password, solutionOpenMethodArgs, null);
	}

	public static IHeadlessClient createHeadlessClient(String solutionname) throws Exception
	{
		return createHeadlessClient(solutionname, null, null, null);
	}

	public static IHeadlessClient createHeadlessClient(String solutionname, String username, String password) throws Exception
	{
		return createHeadlessClient(solutionname, username, password, null);
	}

	public static IHeadlessClient createHeadlessClient(String solutionname, Object[] solutionOpenMethodArgs) throws Exception
	{
		return createHeadlessClient(solutionname, null, null, solutionOpenMethodArgs);
	}

	public static IHeadlessClient createHeadlessClient(String solutionname, String username, String password, Object[] solutionOpenMethodArgs) throws Exception
	{
		return HeadlessClientFactoryInternal.createHeadlessClient(solutionname, username, password, solutionOpenMethodArgs, null);
	}
}
