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

package com.servoy.j2db.util;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.servoy.j2db.server.shared.IServiceRegistry;

/**
 * @author jblok
 */
public class OSGIServiceRegistry implements IServiceRegistry
{
	private final BundleContext context;

	/**
	 * @param context
	 */
	public OSGIServiceRegistry(BundleContext context)
	{
		this.context = context;
	}

	@Override
	public <S> S getService(Class<S> clazz)
	{
		ServiceReference<S> ref = context.getServiceReference(clazz);
		if (ref == null) return null;
		return context.getService(ref);
	}

	@Override
	public <S> void registerService(Class<S> clazz, S service)
	{
		context.registerService(clazz, service, null);
	}

	@Override
	public <S> void ungetService(Class<S> clazz)
	{
		ServiceReference<S> ref = context.getServiceReference(clazz);
		if (ref == null) return;
		context.ungetService(ref);
	}
}
