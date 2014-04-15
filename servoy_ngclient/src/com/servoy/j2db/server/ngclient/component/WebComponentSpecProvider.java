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

package com.servoy.j2db.server.ngclient.component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;

import com.servoy.j2db.server.ngclient.component.WebComponentPackage.IPackageReader;
import com.servoy.j2db.util.Debug;

/**
 * Class responsible for loading a set of web component packages and specs.
 * @author acostescu
 */
@SuppressWarnings("nls")
public class WebComponentSpecProvider
{

	private final Map<String, WebComponentSpec> cachedDescriptions = new HashMap<>();

	private final IPackageReader[] packageReaders;

	public WebComponentSpecProvider(IPackageReader[] packageReaders)
	{
		this.packageReaders = packageReaders;
		for (IPackageReader packageReader : packageReaders)
		{
			WebComponentPackage p = new WebComponentPackage(packageReader);

			try
			{
				cache(p.getWebComponentDescriptions());
			}
			catch (IOException e)
			{
				Debug.error("Cannot read web component package: " + packageReader.getName(), e); //$NON-NLS-1$
			}
			finally
			{
				p.dispose();
			}
		}
		instance = this;
	}

	public WebComponentSpecProvider(File[] packages)
	{
		this(getReades(packages));
	}

	private static IPackageReader[] getReades(File[] packages)
	{
		ArrayList<IPackageReader> readers = new ArrayList<>();
		for (File f : packages)
		{
			if (f.exists())
			{
				if (f.isDirectory()) readers.add(new WebComponentPackage.DirPackageReader(f));
				else readers.add(new WebComponentPackage.JarPackageReader(f));
			}
			else
			{
				Debug.error("A web component package location does not exist: " + f.getAbsolutePath()); //$NON-NLS-1$
			}
		}
		return readers.toArray(new IPackageReader[readers.size()]);
	}

	private void cache(List<WebComponentSpec> webComponentDescriptions)
	{
		for (WebComponentSpec desc : webComponentDescriptions)
		{
			WebComponentSpec old = cachedDescriptions.put(desc.getName(), desc);
			if (old != null) Debug.error("Conflict found! Duplicate web component definition name: " + old.getName());
		}
	}

	public WebComponentSpec getWebComponentDescription(String componentTypeName)
	{
		return cachedDescriptions.get(componentTypeName);
	}

	public WebComponentSpec[] getWebComponentDescriptions()
	{
		return cachedDescriptions.values().toArray(new WebComponentSpec[cachedDescriptions.size()]);
	}

	private static volatile WebComponentSpecProvider instance;

	public static WebComponentSpecProvider getInstance()
	{
		return instance;
	}

	/**
	 * @param array
	 */
	public static void init(IPackageReader[] locations)
	{
		instance = new WebComponentSpecProvider(locations);
	}


	public static WebComponentSpecProvider init(ServletContext servletContext)
	{
		if (instance == null)
		{
			synchronized (WebComponentSpecProvider.class)
			{
				if (instance == null)
				{
					try
					{
						ArrayList<IPackageReader> readers = new ArrayList<IPackageReader>();
						InputStream is = servletContext.getResourceAsStream("/WEB-INF/components.properties");
						Properties properties = new Properties();
						properties.load(is);
						String[] locations = properties.getProperty("locations").split(";");
						for (String location : locations)
						{
							readers.add(new WebComponentPackage.WarURLPackageReader(servletContext, location));
						}

						instance = new WebComponentSpecProvider(readers.toArray(new IPackageReader[readers.size()]));
					}
					catch (Exception e)
					{
						Debug.error(e);
					}
				}
			}
		}
		return instance;
	}

	public static void reload()
	{
		synchronized (WebComponentSpecProvider.class)
		{
			instance = new WebComponentSpecProvider(instance.packageReaders);
		}
	}

}
