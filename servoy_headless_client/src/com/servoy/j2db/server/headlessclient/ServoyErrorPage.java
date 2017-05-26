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

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.wicket.Application;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.locator.IResourceStreamLocator;

import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 *
 */
public class ServoyErrorPage extends WebPage implements IMarkupResourceStreamProvider
{
	private static final long serialVersionUID = 1L;

	/**
	 *
	 */
	public ServoyErrorPage()
	{
	}

	/**
	 * @see org.apache.wicket.markup.IMarkupResourceStreamProvider#getMarkupResourceStream(org.apache.wicket.MarkupContainer, java.lang.Class)
	 */
	public IResourceStream getMarkupResourceStream(MarkupContainer container, Class containerClass)
	{
		String htmlfile = Settings.getInstance().getProperty("servoy.webclient.error.page");
		Cookie[] cookies = ((WebRequest)RequestCycle.get().getRequest()).getCookies();
		if (cookies != null && cookies.length > 0)
		{
			for (Cookie element : cookies)
			{
				if ("servoy.webclient.error.page".equals(element.getName()))
				{
					htmlfile = Utils.decodeCookieValue(element.getValue());
					break;
				}
			}
		}
		final IResourceStreamLocator locator = Application.get().getResourceSettings().getResourceStreamLocator();
		return locator.locate(ServoyErrorPage.class, htmlfile);
	}

	/**
	 * @see wicket.markup.html.WebPage#configureResponse()
	 */
	@Override
	protected void configureResponse()
	{
		super.configureResponse();
		getWebRequestCycle().getWebResponse().getHttpServletResponse().setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	}


	/**
	 * @see wicket.Component#isVersioned()
	 */
	@Override
	public boolean isVersioned()
	{
		return false;
	}

	/**
	 * @see wicket.Page#isErrorPage()
	 */
	@Override
	public boolean isErrorPage()
	{
		return true;
	}

}
