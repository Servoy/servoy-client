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

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.application.IClassResolver;

import com.servoy.j2db.ClientState;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;

/**
 * @author jcompagner
 *
 */
public class ServoyClassResolver implements IClassResolver
{
	private final ConcurrentMap<String, Class> loadedClasses = new ConcurrentHashMap<String, Class>();

	/**
	 * @see org.apache.wicket.application.IClassResolver#resolveClass(java.lang.String)
	 */
	public Class resolveClass(String classname)
	{
		Class< ? > cls = loadedClasses.get(classname);
		if (cls != null) return cls;

		ClassLoader classLoader = ApplicationServerRegistry.get().getBeanManager().getClassLoader();
		try
		{
			cls = classLoader.loadClass(classname);
		}
		catch (ClassNotFoundException ex)
		{
			if (ClientState.class.getClassLoader() != classLoader)
			{
				try
				{
					cls = ClientState.class.getClassLoader().loadClass(classname);
				}
				catch (ClassNotFoundException e)
				{
					// ignore, runtime exception below will be thrown.
				}
			}
			if (cls == null)
			{
				throw new RuntimeException("Class " + classname + " couldn't be loaded through the bean/plugin classloader", ex); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		loadedClasses.put(classname, cls);
		return cls;
	}

	/**
	 * @see org.apache.wicket.application.IClassResolver#getResources(java.lang.String)
	 */
	public Iterator<URL> getResources(String name)
	{
		HashSet<URL> loadedFiles = new HashSet<URL>();
		try
		{
			// Try the classloader for the wicket jar/bundle
			ClassLoader classLoader = ApplicationServerRegistry.get().getBeanManager().getClassLoader();
			Enumeration<URL> resources = classLoader.getResources(name);
			loadResources(resources, loadedFiles);
			if (classLoader != ClientState.class.getClassLoader())
			{
				resources = ClientState.class.getClassLoader().getResources(name);
				loadResources(resources, loadedFiles);
			}


		}
		catch (IOException e)
		{
			throw new WicketRuntimeException(e);
		}

		return loadedFiles.iterator();
	}

	private void loadResources(Enumeration<URL> resources, Set<URL> loadedFiles)
	{
		if (resources != null)
		{
			while (resources.hasMoreElements())
			{
				final URL url = resources.nextElement();
				if (!loadedFiles.contains(url))
				{
					loadedFiles.add(url);
				}
			}
		}
	}

}
