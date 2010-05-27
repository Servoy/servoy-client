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

import com.servoy.j2db.ISessionClient;
import com.servoy.j2db.persistence.InfoChannel;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;
import com.servoy.j2db.server.shared.IApplicationServerSingleton;

public class HeadlessClientFactoryInternal
{
	public static IHeadlessClient createHeadlessClient(String solutionname, String username, String password, Object[] solutionOpenMethodArgs,
		InfoChannel channel) throws Exception
	{
		return createSessionBean(null, solutionname, username, password, solutionOpenMethodArgs, channel);
	}

	public static ISessionClient createSessionBean(ServletRequest req, String solutionname, String username, String password, Object[] solutionOpenMethodArgs,
		InfoChannel channel) throws Exception
	{
		ISessionClient sc;
		IApplicationServerSingleton as = ApplicationServerSingleton.get();
		if (as.isDeveloperStartup())
		{
			sc = as.getDebugClientHandler().createDebugHeadlessClient(req, username, password, null, solutionOpenMethodArgs);
		}
		else
		{
			sc = new SessionClient(req, username, password, null, solutionOpenMethodArgs, solutionname);
		}
		sc.setOutputChannel(channel);
		sc.loadSolution(solutionname);
		return sc;
	}

	public static ISessionClient createAuthenticator(String authenticatorName, String method, Object[] solutionOpenMethodArgs) throws Exception
	{
		ISessionClient sc;
		IApplicationServerSingleton as = ApplicationServerSingleton.get();
		if (as.isDeveloperStartup())
		{
			sc = as.getDebugClientHandler().createDebugAuthenticator(authenticatorName, method, solutionOpenMethodArgs);
		}
		else
		{
			sc = new SessionClient(null, null, null, method, solutionOpenMethodArgs, authenticatorName);
		}
		sc.loadSolution(authenticatorName);
		return sc;
	}
}
