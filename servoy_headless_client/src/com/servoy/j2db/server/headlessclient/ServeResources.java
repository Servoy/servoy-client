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

import org.apache.wicket.Session;
import org.apache.wicket.markup.html.DynamicWebResource;
import org.apache.wicket.markup.html.WebResource;
import org.apache.wicket.protocol.http.WebResponse;

import com.servoy.j2db.util.HTTPUtils;

/**
 * A {@link WebResource} that serves a resource that is get from {@link WebClientSession#getResourceState()}
 * which is set for example by printing at form.
 * 
 * @author jcompagner
 */
public class ServeResources extends DynamicWebResource
{
	private static final long serialVersionUID = 1L;

	@Override
	protected ResourceState getResourceState()
	{
		WebClientSession session = (WebClientSession)Session.get();
		if (session != null)
		{
			return session.getResourceState();
		}
		return new ResourceState()
		{

			@Override
			public byte[] getData()
			{
				return new byte[0];
			}

			@Override
			public String getContentType()
			{
				return null;
			}
		};
	}

	@Override
	protected void setHeaders(WebResponse response)
	{
		super.setHeaders(response);
		HTTPUtils.setNoCacheHeaders(response.getHttpServletResponse());
		response.getHttpServletResponse().setHeader("Pragma", "public");//$NON-NLS-1$//$NON-NLS-2$
	}
}
