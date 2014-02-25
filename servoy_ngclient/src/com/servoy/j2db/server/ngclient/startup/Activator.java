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

import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.servoy.j2db.server.ngclient.MediaResourcesServlet;
import com.servoy.j2db.server.ngclient.TemplateGeneratorFilter;

/**
 * @author jblok
 */
public class Activator implements BundleActivator
{
	@Override
	public void start(BundleContext context) throws Exception
	{
		Dictionary properties = new Hashtable();
		properties.put("alias", "/resources/*");
		context.registerService(Servlet.class.getName(), new MediaResourcesServlet(), properties);

		properties = new Hashtable();
		properties.put("urlPatterns", new String[] { "/solutions/*" });
		context.registerService(Filter.class.getName(), new TemplateGeneratorFilter(), properties);

//		properties = new Hashtable();
//		properties.put("url", "/websocket");
//		context.registerService(Endpoint.class.getName(), new NGClientEndpoint(), properties);
	}

	@Override
	public void stop(BundleContext context) throws Exception
	{
	}
}
