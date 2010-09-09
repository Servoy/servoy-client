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

import org.mozilla.javascript.Context;

import com.servoy.j2db.ISessionClient;
import com.servoy.j2db.persistence.InfoChannel;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;
import com.servoy.j2db.server.shared.IApplicationServerSingleton;

public class HeadlessClientFactoryInternal
{
	public static IHeadlessClient createHeadlessClient(String solutionname, String username, String password, Object[] solutionOpenMethodArgs,
		InfoChannel channel) throws Exception
	{
		return createSessionBean(null, solutionname, username, password, solutionOpenMethodArgs, channel);
	}

	public static ISessionClient createSessionBean(final ServletRequest req, final String solutionname, final String username, final String password,
		final Object[] solutionOpenMethodArgs, final InfoChannel channel) throws Exception
	{
		final ISessionClient[] sc = { null };
		final Exception[] exception = { null };
		Runnable createSessionBeanRunner = new Runnable()
		{
			public void run()
			{
				try
				{
					IApplicationServerSingleton as = ApplicationServerSingleton.get();
					if (as.isDeveloperStartup())
					{
						sc[0] = as.getDebugClientHandler().createDebugHeadlessClient(req, username, password, null, solutionOpenMethodArgs);
					}
					else
					{
						sc[0] = new SessionClient(req, username, password, null, solutionOpenMethodArgs, solutionname);
					}
					sc[0].setOutputChannel(channel);
					sc[0].loadSolution(solutionname);

				}
				catch (Exception e)
				{
					exception[0] = e;
				}
			}
		};

		if (Context.getCurrentContext() == null)
		{
			createSessionBeanRunner.run();
		}
		else
		{
			// we are running from a javascript thread, probably developer or webclient, create the bean in a
			// separate thread so that a new context will be created
			Thread createSessionBeanThread = new Thread(createSessionBeanRunner, "createSessionBean"); //$NON-NLS-1$
			createSessionBeanThread.start();
			createSessionBeanThread.join();
		}

		if (exception[0] != null)
		{
			throw exception[0];
		}
		return sc[0];
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
			sc = new SessionClient(null, null, null, method, solutionOpenMethodArgs, authenticatorName)
			{
				@Override
				protected int getSolutionTypeFilter()
				{
					return SolutionMetaData.AUTHENTICATOR;
				}
			};
		}
		sc.loadSolution(authenticatorName);
		return sc;
	}
}
