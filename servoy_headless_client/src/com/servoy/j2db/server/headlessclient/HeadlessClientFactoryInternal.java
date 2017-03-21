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

import java.rmi.RemoteException;

import javax.servlet.ServletRequest;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.ISessionClient;
import com.servoy.j2db.LocalActiveSolutionHandler;
import com.servoy.j2db.persistence.IActiveSolutionHandler;
import com.servoy.j2db.persistence.IDeveloperRepository;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.scripting.ScriptEngine;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServer;
import com.servoy.j2db.server.shared.IApplicationServerSingleton;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.xmlxport.IXMLImportUserChannel;

/**
 * Factory for headless clients
 * @author rgansevles
 */
@SuppressWarnings("nls")
public class HeadlessClientFactoryInternal
{
	public static ISessionClient createHeadlessClient(String solutionname, String username, String password, Object[] solutionOpenMethodArgs) throws Exception
	{
		return createSessionBean(null, solutionname, username, password, solutionOpenMethodArgs);
	}

	public static ISessionClient createSessionBean(final ServletRequest req, final String solutionname, final String username, final String password,
		final Object[] solutionOpenMethodArgs) throws Exception
	{
		final ISessionClient[] sc = { null };
		final Exception[] exception = { null };
		Runnable createSessionBeanRunner = new Runnable()
		{
			public void run()
			{
				try
				{
					IApplicationServerSingleton as = ApplicationServerRegistry.get();

					boolean nodebug = false;
					Object[] openArgs = solutionOpenMethodArgs;
					if (solutionOpenMethodArgs != null && solutionOpenMethodArgs.length != 0 &&
						"nodebug".equals(solutionOpenMethodArgs[solutionOpenMethodArgs.length - 1]))
					{
						nodebug = true;
						openArgs = Utils.arraySub(solutionOpenMethodArgs, 0, solutionOpenMethodArgs.length - 1);
					}
					// When last entry in solutionOpenMethodArgs in "nodebug" a non-debugging client is created.
					if (as.isDeveloperStartup() && !nodebug)
					{
						sc[0] = as.getDebugClientHandler().createDebugHeadlessClient(req, username, password, null, openArgs, solutionname);
					}
					else
					{
						sc[0] = new SessionClient(req, username, password, null, openArgs, solutionname);
					}
					sc[0].setUseLoginSolution(false);
					sc[0].loadSolution(solutionname);

				}
				catch (Exception e)
				{
					exception[0] = e;
					if (sc[0] != null)
					{
						try
						{
							sc[0].shutDown(true);
						}
						catch (Exception ex)
						{
							Debug.error(ex);
						}
					}
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

	public static ISessionClient createImportHookClient(final Solution importHookModule, final IXMLImportUserChannel channel) throws Exception
	{
		final String[] loadException = new String[1];
		// assuming no login and no method args for import hooks
		SessionClient sc = new SessionClient(null, null, null, null, null, importHookModule.getName())
		{
			@Override
			protected IActiveSolutionHandler createActiveSolutionHandler()
			{
				IApplicationServer as = ApplicationServerRegistry.getService(IApplicationServer.class);
				return new LocalActiveSolutionHandler(as, this)
				{
					@Override
					protected Solution loadSolution(RootObjectMetaData solutionDef) throws RemoteException, RepositoryException
					{
						// grab the latest version (-1) not the active one, because the hook was not yet activated.
						return (Solution)((IDeveloperRepository)getRepository()).getRootObject(solutionDef.getRootObjectId(), -1);
					}
				};
			}

			@Override
			protected IExecutingEnviroment createScriptEngine()
			{
				return new ScriptEngine(this)
				{
					@Override
					public Object executeFunction(Function f, Scriptable scope, Scriptable thisObject, Object[] args, boolean focusEvent, boolean throwException)
							throws Exception
					{
						// always throw exception
						return super.executeFunction(f, scope, thisObject, args, focusEvent, true);
					}
				};
			}

			@Override
			public void reportError(String msg, Object detail)
			{
				super.reportError(msg, detail);
				loadException[0] = msg;
				if (detail instanceof JavaScriptException && ((JavaScriptException)detail).getValue() instanceof Scriptable)
				{
					loadException[0] += " " + Utils.getScriptableString((Scriptable)((JavaScriptException)detail).getValue());
				}
				if (detail instanceof Exception)
				{
					loadException[0] += " " + ((Exception)detail).getMessage();
				}
			}
		};
		sc.setUseLoginSolution(false);
		String userName = channel.getImporterUsername();
		if (userName != null)
		{
			// let the import hook client run with credentials from the logged in user from the admin page.
			sc.getClientInfo().setUserUid(ApplicationServerRegistry.get().getUserManager().getUserUID(sc.getClientID(), userName));
			sc.getClientInfo().setUserName(userName);
		}
		sc.setOutputChannel(channel);
		sc.loadSolution(importHookModule.getName());
		if (loadException[0] != null)
		{
			sc.shutDown(true);
			throw new RepositoryException(loadException[0]);
		}
		return sc;
	}
}
