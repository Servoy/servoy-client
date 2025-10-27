/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

package com.servoy.j2db.server.extensions;

import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentMap;

/**
 * @author jcompagner
 *
 * @since 2025.12
 *
 */
public class ServoyServiceLoader
{
	private static final ConcurrentMap<Class< ? >, ServiceLoader< ? >> serviceLoaders = new java.util.concurrent.ConcurrentHashMap<>();
	private static ClassLoader classLoader = ServoyServiceLoader.class.getClassLoader();

	@SuppressWarnings("unchecked")
	public static <S> ServiceLoader<S> load(Class<S> serviceClass)
	{
		ServiceLoader< ? > serviceLoader = serviceLoaders.get(serviceClass);
		if (serviceLoader == null)
		{
			serviceLoader = java.util.ServiceLoader.load(serviceClass, classLoader);
			if (serviceLoader != null)
			{
				serviceLoaders.putIfAbsent(serviceClass, serviceLoader);
			}
		}
		return (ServiceLoader<S>)serviceLoader;
	}

	public static void setClassLoader(ClassLoader classLoader)
	{
		ServoyServiceLoader.classLoader = classLoader != null ? classLoader : ServoyServiceLoader.class.getClassLoader();
	}
}
