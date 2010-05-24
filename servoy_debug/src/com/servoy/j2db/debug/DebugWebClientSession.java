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
package com.servoy.j2db.debug;

import javax.servlet.http.HttpServletRequest;

import org.apache.wicket.Request;
import org.apache.wicket.RestartResponseException;

import com.servoy.j2db.IWebClientApplication;
import com.servoy.j2db.server.ApplicationServerSingleton;
import com.servoy.j2db.server.headlessclient.DebuggerNotConnectedErrorPage;
import com.servoy.j2db.server.headlessclient.WebClientSession;

/**
 * WebClientSession for running in developer.
 * @author rob
 *
 */
public class DebugWebClientSession extends WebClientSession
{

	public DebugWebClientSession(Request request)
	{
		super(request);
	}

	@Override
	protected IWebClientApplication createWebClient(HttpServletRequest req, String name, String pass, String method, Object[] methodArgs, String solution)
		throws Exception
	{
		if (RemoteDebugScriptEngine.isConnected())
		{
			return ApplicationServerSingleton.get().getDebugClientHandler().createDebugWebClient(this, req, name, pass, method, methodArgs);
		}
		throw new RestartResponseException(DebuggerNotConnectedErrorPage.class);
	}
}
