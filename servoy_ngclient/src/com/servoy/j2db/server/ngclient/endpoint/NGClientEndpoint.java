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

package com.servoy.j2db.server.ngclient.endpoint;


import java.util.HashMap;
import java.util.Map;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import org.sablo.websocket.CurrentWindow;
import org.sablo.websocket.GetHttpSessionConfigurator;
import org.sablo.websocket.IWebsocketSession;

import com.servoy.j2db.ApplicationException;
import com.servoy.j2db.server.ngclient.INGClientWebsocketSession;
import com.servoy.j2db.server.ngclient.NGClient;
import com.servoy.j2db.server.ngclient.WebsocketSessionFactory;
import com.servoy.j2db.server.ngclient.eventthread.NGClientWebsocketSessionWindows;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

/**
 * WebsocketEndpoint for NGClient.
 *
 * @author rgansevles
 *
 */

@ServerEndpoint(value = "/websocket/{windowname}/{windowid}", configurator = GetHttpSessionConfigurator.class)
public class NGClientEndpoint extends BaseNGClientEndpoint
{
	public NGClientEndpoint()
	{
		super(WebsocketSessionFactory.CLIENT_ENDPOINT);
	}

	@OnOpen
	public void start(Session newSession, @PathParam("windowname") String windowname, @PathParam("windowid") String windowid, EndpointConfig config)
		throws Exception
	{
		super.start(newSession, windowname, windowid);
	}

	@Override
	@OnMessage
	public void incoming(String msg, boolean lastPart)
	{
		super.incoming(msg, lastPart);
	}

	@Override
	@OnClose
	public void onClose(CloseReason closeReason)
	{
		super.onClose(closeReason);
	}

	@Override
	@OnError
	public void onError(Throwable t)
	{
		super.onError(t);
	}

	@Override
	protected void handleException(final Exception e, final IWebsocketSession session)
	{
		if (e instanceof ApplicationException)
		{
			final ApplicationException ae = (ApplicationException)e;
			// if the current window has no endpoint then quickly set it to this instance.
			if (CurrentWindow.exists() && !CurrentWindow.get().hasEndpoint()) CurrentWindow.get().setEndpoint(this);
			CurrentWindow.runForWindow(new NGClientWebsocketSessionWindows((INGClientWebsocketSession)session), new Runnable()
			{
				@Override
				public void run()
				{
					if (ae.getErrorCode() == ServoyException.NO_LICENSE)
					{
						session.getClientService("$sessionService").executeAsyncServiceCall("setNoLicense", new Object[] { getLicenseAndMaintenanceDetail() });
					}
					else if (ae.getErrorCode() == ServoyException.MAINTENANCE_MODE)
					{
						session.getClientService("$sessionService").executeAsyncServiceCall("setMaintenanceMode",
							new Object[] { getLicenseAndMaintenanceDetail() });
					}
				}
			});
		}
		try
		{
			((NGClient)((INGClientWebsocketSession)session).getClient()).shutDown(true);
		}
		catch (Exception e1)
		{
			Debug.error("Error calling shutdown on client that had an exception when starting up: " + e.getMessage(), e1);
		}
	}


	private Map<String, Object> getLicenseAndMaintenanceDetail()
	{
		Map<String, Object> detail = new HashMap<>();
		String url = Settings.getInstance().getProperty("servoy.webclient.pageexpired.url");
		if (url != null)
		{
			detail.put("redirectUrl", url);
			String redirectTimeout = Settings.getInstance().getProperty("servoy.webclient.pageexpired.redirectTimeout");
			detail.put("redirectTimeout", Utils.getAsInteger(redirectTimeout));
		}
		return detail;
	}

}
