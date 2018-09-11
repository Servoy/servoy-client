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

public class ExtendableURLClassLoader extends URLClassLoader
{
	public ExtendableURLClassLoader(URL[] urls, ClassLoader parent, String nameSuffix)
	{
		super(urls, parent);
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