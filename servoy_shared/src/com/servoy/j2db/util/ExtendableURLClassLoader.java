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
package com.servoy.j2db.util;


import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;

import com.tc.object.bytecode.hook.impl.ClassProcessorHelper;
import com.tc.object.loaders.NamedClassLoader;

public class ExtendableURLClassLoader extends URLClassLoader
{
	public ExtendableURLClassLoader(URL[] urls, ClassLoader parent, String nameSuffix)
	{
		super(urls, parent);
		registerClassLoaderInClusterIfNeeded(nameSuffix);
	}

	// TODO a better idea I think would be to create a Servoy TIM for this kind of things (if possible)
	private void registerClassLoaderInClusterIfNeeded(String nameSuffix)
	{
		try
		{
			// if this class can be found it means application server was started under Terracotta
			Class.forName("com.tc.object.loaders.NamedClassLoader"); //$NON-NLS-1$


			// although this class is not actually an instance of NamedClassLoader, when running clustered it is (URLClassLoader is injected by terracotta);
			// we need to register this classloader with terracotta so that plugins/beans/lafs/drivers can add clustering behavior;
			// the class loader name decided which named classloaders can share terracotta roots between JVM; each name should be unique within that JVM
			// (currently you are not allowed to have multiple instances of the same terracotta root in the same JVM if loaded by different classloaders - thus the names
			// have to be unique (also to avoid ambiguity))
			((NamedClassLoader)this).__tc_setClassLoaderName(getCurrentClassLoaderName() + nameSuffix);
			ClassProcessorHelper.registerGlobalLoader(((NamedClassLoader)this), null); // null web app. name (war deployment) for now - we build the name based on parent classloaders
		}
		catch (ClassNotFoundException e)
		{
			// we are not running inside a Terracotta cluster; np, nothing to register
		}
		catch (ClassCastException e)
		{
			// we are not running inside a Terracotta cluster; np, nothing to register
		}
	}

	private String getCurrentClassLoaderName()
	{
		// try to find the named classloader name of the classloader loading this classloader's class :) (when running under Terracotta)
		// - not the parent's that could be another (in case of beans for example)
		ClassLoader currentClassLoader = getClass().getClassLoader();
		String currentClassLoaderName = ""; //$NON-NLS-1$
		if (currentClassLoader instanceof NamedClassLoader) currentClassLoaderName = ((NamedClassLoader)currentClassLoader).__tc_getClassLoaderName();
		return currentClassLoaderName;
	}

	@Override
	public void addURL(URL url)
	{
		super.addURL(url);
	}

	private boolean urlsVisible = false;

	public void setURLsVisible(boolean b)
	{
		urlsVisible = b;
	}

	/**
	 * if disabled ,we never want that RMI starts loading classes from the developer
	 */
	@Override
	public URL[] getURLs()
	{
		Debug.trace("ExtendableURLClassLoader.getURLs is invoked!"); //$NON-NLS-1$
		if (urlsVisible)
		{
			return super.getURLs();
		}
		else
		{
			return new URL[0];
		}
	}

	public boolean hasURL(URL url)
	{
		List<URL> urls = Arrays.asList(super.getURLs());
		return urls.contains(url);
	}

	@Override
	public String toString()
	{
		return "ExtendableURLClassLoader: " + getURLs();
	}
}