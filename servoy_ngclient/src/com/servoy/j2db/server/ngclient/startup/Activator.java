/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.j2db.server.ngclient.startup;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.sablo.eventthread.IEventDispatcher;
import org.sablo.websocket.IWebsocketSession;
import org.sablo.websocket.IWebsocketSessionFactory;
import org.sablo.websocket.WebsocketSessionManager;

import com.servoy.j2db.IDebugClientHandler;
import com.servoy.j2db.server.ngclient.NGClient;
import com.servoy.j2db.server.ngclient.NGClientWebsocketSession;
import com.servoy.j2db.server.ngclient.WebsocketSessionFactory;
import com.servoy.j2db.server.ngclient.eventthread.NGEventDispatcher;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;

/**
 * @author jblok
 */
public class Activator implements BundleActivator
{

	private static BundleContext context;

	public static BundleContext getContext()
	{
		return context;
	}

	@Override
	public void start(BundleContext ctx) throws Exception
	{
		Activator.context = ctx;
		if (ApplicationServerRegistry.getServiceRegistry() != null)
		{
			WebsocketSessionManager.setWebsocketSessionFactory(WebsocketSessionFactory.CLIENT_ENDPOINT, new IWebsocketSessionFactory()
			{
				@Override
				public IWebsocketSession createSession(String uuid) throws Exception
				{
					NGClientWebsocketSession wsSession = new NGClientWebsocketSession(uuid)
					{
						@Override
						public void init() throws Exception
						{
							if (getClient() == null)
							{
								final IDebugClientHandler service = ApplicationServerRegistry.getServiceRegistry().getService(IDebugClientHandler.class);
								if (service != null)
								{
									NGClient debugNGClient = (NGClient)service.getDebugNGClient();
									if (debugNGClient != null && !debugNGClient.isShutDown() && debugNGClient.getWebsocketSession().getUuid().equals(getUuid()))
										setClient(debugNGClient);
									else setClient((NGClient)service.createDebugNGClient(this));
								}
								else
								{
									setClient(new NGClient(this));
								}
							}
						}

						@Override
						protected IEventDispatcher createEventDispatcher()
						{
							// make sure that the command console thread is seen as the dispatch thread
							// so it can executed command, that are api calls to the browser
							return new NGEventDispatcher(getClient())
							{
								@Override
								public boolean isEventDispatchThread()
								{
									return super.isEventDispatchThread() || Thread.currentThread().getName().equals("Debug command reader"); //$NON-NLS-1$
								}
							};
						}
					};
					return wsSession;
				}
			});
		}
	}

	@Override
	public void stop(BundleContext ctx) throws Exception
	{
	}
}
