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

import javax.servlet.http.HttpServletResponse;

import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.link.ExternalLink;

import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

/**
 * Page indication server is out of licenses
 * @author jblok
 */
public class ServoyServerToBusyPage extends WebPage
{
	private static final long serialVersionUID = 1L;

	public ServoyServerToBusyPage()
	{
		String url = Settings.getInstance().getProperty("servoy.webclient.pageexpired.url");
		WebMarkupContainer container = new WebMarkupContainer("meta");
		add(container);
		if (url != null)
		{
			int redirect = -1;
			String redirectTimeout = Settings.getInstance().getProperty("servoy.webclient.pageexpired.redirectTimeout");
			if (redirectTimeout != null)
			{
				redirect = Utils.getAsInteger(redirectTimeout);
			}
			if (redirect > -1)
			{
				container.add(new SimpleAttributeModifier("content", redirect + ";url=" + url));
			}
			else
			{
				container.setVisible(false);
			}

			ExternalLink link = new ExternalLink("homePageLink", url);
			add(link);
		}
		else
		{
			container.setVisible(false);
			add(homePageLink("homePageLink"));
		}
	}

	/**
	 * @see wicket.markup.html.WebPage#configureResponse()
	 */
	@Override
	protected void configureResponse()
	{
		super.configureResponse();
		getWebRequestCycle().getWebResponse().getHttpServletResponse().setStatus(HttpServletResponse.SC_NOT_FOUND);
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
