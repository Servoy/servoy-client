/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

import org.apache.wicket.AbortException;
import org.apache.wicket.Page;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.Response;
import org.apache.wicket.RestartResponseAtInterceptPageException;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.protocol.http.ClientProperties;
import org.apache.wicket.protocol.http.PageExpiredException;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.apache.wicket.protocol.http.request.InvalidUrlException;
import org.apache.wicket.protocol.http.request.WebClientInfo;
import org.apache.wicket.request.ClientInfo;

import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

/**
 * The {@link WebRequestCycle} implementation that for setting up the right thread locals and handling errors.
 * 
 * @author jcompagner
 * 
 * @since 6.1
 */
public final class ServoyRequestCycle extends WebRequestCycle
{

	public static final void set(RequestCycle requestCycle)
	{
		RequestCycle.set(requestCycle);
	}

	/**
	 * @param application
	 * @param request
	 * @param response
	 */
	ServoyRequestCycle(WebApplication application, WebRequest request, Response response)
	{
		super(application, request, response);
	}

	/**
	 * @see wicket.RequestCycle#onBeginRequest()
	 */
	@Override
	protected void onBeginRequest()
	{
		WebClientSession webClientSession = (WebClientSession)getSession();
		WebClient webClient = webClientSession.getWebClient();
		if (webClient != null)
		{
			J2DBGlobals.setServiceProvider(webClient);
			webClient.onBeginRequest(webClientSession);
		}
	}

	/**
	 * @see wicket.RequestCycle#onEndRequest()
	 */
	@Override
	protected void onEndRequest()
	{
		J2DBGlobals.setServiceProvider(null);
		WebClientSession webClientSession = (WebClientSession)getSession();
		WebClient webClient = webClientSession.getWebClient();
		if (webClient != null)
		{
			webClient.onEndRequest(webClientSession);
		}
	}

	/**
	 * @see org.apache.wicket.protocol.http.WebRequestCycle#newClientInfo()
	 */
	@Override
	protected ClientInfo newClientInfo()
	{
		// We will always do a redirect here. The servoy browser info has to make one.
		WebClientInfo webClientInfo = new WebClientInfo(this);
		ClientProperties cp = webClientInfo.getProperties();
		if (cp.isBrowserInternetExplorer() || cp.isBrowserMozilla() || cp.isBrowserKonqueror() || cp.isBrowserOpera() || cp.isBrowserSafari() ||
			cp.isBrowserChrome())
		{
			if (cp.isBrowserInternetExplorer() && cp.getBrowserVersionMajor() != -1 && cp.getBrowserVersionMajor() < 7)
			{
				// IE6 is no longer supported when anchoring is enabled.
				boolean enableAnchoring = Utils.getAsBoolean(Settings.getInstance().getProperty("servoy.webclient.enableAnchors", Boolean.TRUE.toString())); //$NON-NLS-1$ 
				if (enableAnchoring)
				{
					throw new RestartResponseException(new UnsupportedBrowserPage("Internet Explorer 6")); //$NON-NLS-1$
				}
			}
			Page page = getResponsePage();
			if (page != null)
			{
				throw new RestartResponseAtInterceptPageException(new ServoyBrowserInfoPage(urlFor(page).toString().replaceAll("../", ""))); //$NON-NLS-1$ //$NON-NLS-2$
			}
			else
			{
				throw new RestartResponseAtInterceptPageException(new ServoyBrowserInfoPage(getRequest().getURL()));
			}
		}
		return webClientInfo;
	}

	/**
	 * @see org.apache.wicket.RequestCycle#onRuntimeException(org.apache.wicket.Page, java.lang.RuntimeException)
	 */
	@Override
	public Page onRuntimeException(Page page, RuntimeException e)
	{
		if (e instanceof PageExpiredException || e instanceof InvalidUrlException)
		{
			if (((WebRequest)RequestCycle.get().getRequest()).isAjax())
			{
				Debug.log("ajax request with exception aborted ", e); //$NON-NLS-1$
				throw new AbortException();
			}
		}
		if (page instanceof MainPage && ((MainPage)page).getController() != null)
		{
			Debug.error("Error rendering the page " + ((MainPage)page).getController().getName(), e); //$NON-NLS-1$
		}
		else
		{
			Debug.error("Error rendering the page " + page, e); //$NON-NLS-1$
		}
		return super.onRuntimeException(page, e);
	}
}