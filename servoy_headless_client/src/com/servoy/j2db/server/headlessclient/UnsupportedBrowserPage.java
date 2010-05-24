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

import java.text.MessageFormat;
import java.util.ResourceBundle;

import org.apache.wicket.Session;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;

import com.servoy.j2db.Messages;
import com.servoy.j2db.util.Utils;

public class UnsupportedBrowserPage extends WebPage
{
	@SuppressWarnings("nls")
	public UnsupportedBrowserPage(String browserString)
	{
		String message = "Unsupported browser encountered '" + browserString + "', please upgrade";
		if (Session.exists())
		{
			WebClientSession session = (WebClientSession)Session.get();
			WebClient client = session.getWebClient();
			if (client != null)
			{
				message = client.getI18NMessage("servoy.client.message.unsupportedBrowser", new Object[] { browserString });
			}
			else
			{
				// client is not there yet. fall back to only use the bundled messages..
				// this happens probably all the time, because the client info is get early in the solution loader.
				ResourceBundle resources = ResourceBundle.getBundle(Messages.BUNDLE_NAME, session.getLocale());
				if (resources != null)
				{
					String rbMessage = resources.getString("servoy.client.message.unsupportedBrowser");
					if (rbMessage != null)
					{
						rbMessage = Utils.stringReplace(rbMessage, "'", "''"); //$NON-NLS-1$ //$NON-NLS-2$
						MessageFormat mf = new MessageFormat(rbMessage);
						mf.setLocale(session.getLocale());
						message = mf.format(new Object[] { browserString });

					}
				}
			}
		}
		add(new Label("title", message));
		add(new Label("message", message));
	}
}
