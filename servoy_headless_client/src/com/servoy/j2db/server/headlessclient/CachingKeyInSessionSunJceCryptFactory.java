/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

import java.security.GeneralSecurityException;
import java.util.UUID;

import javax.servlet.http.HttpSession;

import org.apache.wicket.RequestCycle;
import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.apache.wicket.util.crypt.ICrypt;
import org.apache.wicket.util.crypt.ICryptFactory;
import org.apache.wicket.util.crypt.NoCrypt;

import com.servoy.j2db.util.Debug;

/**
 * @author jcompagner
 *
 */
public class CachingKeyInSessionSunJceCryptFactory implements ICryptFactory
{
	public ICrypt newCrypt()
	{
		ICrypt crypt = null;
		WebClientSession webClientSession = null;
		if (Session.exists())
		{
			webClientSession = WebClientSession.get();
			crypt = webClientSession.getCrypt();
		}

		if (crypt == null)
		{

			WebRequestCycle rc = (WebRequestCycle)RequestCycle.get();

			// get http session, create if necessary
			HttpSession session = rc.getWebRequest().getHttpServletRequest().getSession(true);

			// retrieve or generate encryption key from session
			final String keyAttr = rc.getApplication().getApplicationKey() + "." + getClass().getName();
			String key = (String)session.getAttribute(keyAttr);
			if (key == null)
			{
				// generate new key
				key = session.getId() + "." + UUID.randomUUID().toString();
				session.setAttribute(keyAttr, key);
			}

			// build the crypt based on session key
			try
			{
				crypt = new CachingSunJceCrypt(key);
			}
			catch (GeneralSecurityException e)
			{

				Debug.error("couldn't generate the crypt class", e);
				return new NoCrypt();
			}

			if (webClientSession != null)
			{
				webClientSession.setCrypt(crypt);
			}
		}
		return crypt;
	}
}
