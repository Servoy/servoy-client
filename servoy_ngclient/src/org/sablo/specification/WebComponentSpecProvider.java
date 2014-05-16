/*
 * Copyright (C) 2014 Servoy BV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sablo.specification;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.json.JSONException;
import org.json.JSONObject;
import org.sablo.specification.WebComponentPackage.IPackageReader;
import org.sablo.specification.property.IComplexTypeImpl;
import org.sablo.specification.property.IPropertyType;

import com.servoy.j2db.util.Debug;

/**
 * Class responsible for loading a set of web component packages and specs.
 * @author acostescu
 */
@SuppressWarnings("nls")
public class WebComponentSpecProvider
{

	private final Map<String, WebComponentSpec> cachedDescriptions = new HashMap<>();
	private final Map<String, IPropertyType> globalTypes = new HashMap<>();

	private final IPackageReader[] packageReaders;

	public WebComponentSpecProvider(File[] packages)
	{
		this(getReades(packages));
	}

	public WebComponentSpecProvider(IPackageReader[] packageReaders)
	{
		this.packageReaders = packageReaders;
		List<WebComponentPackage> packages = new ArrayList<>();
		for (IPackageReader packageReader : packageReaders)
		{
			packages.add(new WebComponentPackage(packageReader));
		}
		try
		{
			readGloballyDefinedTypes(packages);
			cacheComponentSpecs(packages);
		}
		finally
		{
			for (WebComponentPackage p : packages)
			{
				p.dispose();
			}
		}
		instance = this;
	}

	protected void cacheComponentSpecs(List<WebComponentPackage> packages)
	{
		for (WebComponentPackage p : packages)
		{
			try
			{
				cache(p.getWebComponentDescriptions(globalTypes));
			}
			catch (IOException e)
			{
				Debug.error("Cannot read web component specs from package: " + p.getName(), e); //$NON-NLS-1$
			}
		}
	}

	protected void readGloballyDefinedTypes(List<WebComponentPackage> packages)
	{
		// populate default types
		for (IPropertyType.Default e : IPropertyType.Default.values())
		{
			IPropertyType type = e.getType();
			globalTypes.put(type.getName(), type);
		}

		try
		{
			JSONObject typeContainer = new JSONObject();
			JSONObject allGlobalTypesFromAllPackages = new JSONObject();
			typeContainer.put(WebComponentSpec.TYPES_KEY, allGlobalTypesFromAllPackages);

			for (WebComponentPackage p : packages)
			{
				try
				{
					p.appendGlobalTypesJSON(allGlobalTypesFromAllPackages);
				}
				catch (IOException e)
				{
					Debug.error("Cannot read globally defined types from package: " + p.getName(), e); //$NON-NLS-1$
				}
			}

			try
			{
				Map<String, IPropertyType> parsedTypes = WebComponentSpec.parseTypes(typeContainer, globalTypes,
					"flattened global types - from all web component packages");
				globalTypes.putAll(parsedTypes);
			}
			catch (Exception e)
			{
				Debug.error("Cannot parse flattened global types - from all web component packages.", e);
			}
		}
		catch (JSONException e)
		{
			// should never happen
			Debug.error("Error Creating a simple JSON object hierarchy while reading globally defined types...");
		}
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

	public IComplexTypeImpl getGlobalType(String typeName)
	{
		return globalTypes.get(typeName);
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
	public static synchronized void init(IPackageReader[] locations)
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
