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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.json.JSONException;

import com.servoy.j2db.server.ngclient.property.PropertyDescription;
import com.servoy.j2db.server.ngclient.property.PropertyType;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

/**
 * An abstraction of package that contains Servoy web-components.
 * @author acostescu
 */
@SuppressWarnings("nls")
public class WebComponentPackage
{

	public interface IPackageReader
	{

		Manifest getManifest() throws IOException;

		String readTextFile(String path, Charset charset) throws IOException;

	}

	private IPackageReader reader;
	private List<WebComponentSpec> cachedDescriptions; // probably useful for developer inthe future

	public WebComponentPackage(IPackageReader reader)
	{
		if (reader == null) throw new NullPointerException();
		this.reader = reader;
	}

	public List<WebComponentSpec> getWebComponentDescriptions() throws IOException, JSONException
	{
		if (cachedDescriptions == null)
		{
			ArrayList<WebComponentSpec> descriptions = new ArrayList<>();
			Manifest mf = reader.getManifest();

			if (mf != null)
			{
				for (String specpath : getWebComponentSpecNames(mf))
				{
					String specfileContent = reader.readTextFile(specpath, Charset.forName("UTF8")); // TODO: check encoding
					if (specfileContent != null)
					{
						try
						{
							WebComponentSpec parsed = WebComponentSpec.parseSpec(specfileContent, specpath);
							// add/overwrite properties defined by us
							parsed.addProperty(new PropertyDescription("location", PropertyType.point));
							parsed.addProperty(new PropertyDescription("size", PropertyType.dimension));
							parsed.addProperty(new PropertyDescription("anchors", PropertyType.intnumber));
							descriptions.add(parsed);
						}
						catch (Exception e)
						{
							Debug.error("Cannot parse spec file '" + specpath + "' from package '" + reader.toString() + "'. ", e);
						}
					}
				}
			}
			cachedDescriptions = descriptions;
			reader = null;
		}
		return cachedDescriptions;
	}

	private static List<String> getWebComponentSpecNames(Manifest mf)
	{
		List<String> names = new ArrayList<String>();
		for (Entry<String, Attributes> entry : mf.getEntries().entrySet())
		{
			if ("true".equalsIgnoreCase((String)entry.getValue().get(new Attributes.Name("Web-Component"))))
			{
				names.add(entry.getKey());
			}
		}

		return names;
	}

	public void dispose()
	{
		reader = null;
		cachedDescriptions = null;
	}

	public static class JarPackageReader implements IPackageReader
	{

		private final File jarFile;

		public JarPackageReader(File jarFile)
		{
			this.jarFile = jarFile;
		}

		@Override
		public Manifest getManifest() throws IOException
		{
			try (JarFile jar = new JarFile(jarFile))
			{
				return jar.getManifest();
			}
		}

		@Override
		public String readTextFile(String path, Charset charset) throws IOException
		{
			try (JarFile jar = new JarFile(jarFile))
			{
				JarEntry entry = jar.getJarEntry(path);
				if (entry != null)
				{
					try (InputStream is = jar.getInputStream(entry))
					{
						return Utils.getTXTFileContent(is, charset);
					}
				}
			}
			return null;
		}

		@Override
		public String toString()
		{
			return "JarPackage: " + jarFile.getAbsolutePath();
		}

	}

	public static class DirPackageReader implements IPackageReader
	{

		private final File dir;

		public DirPackageReader(File dir)
		{
			if (!dir.isDirectory()) throw new IllegalArgumentException("Non-directory package cannot be read by directory reader: " + dir.getAbsolutePath());
			this.dir = dir;
		}

		@Override
		public Manifest getManifest() throws IOException
		{
			try (InputStream x = new FileInputStream(new File(dir, "META-INF/MANIFEST.MF")); InputStream is = new BufferedInputStream(x))
			{
				return new Manifest(is);
			}
		}

		@Override
		public String readTextFile(String path, Charset charset) throws IOException
		{
			try (InputStream x = new FileInputStream(new File(dir, path)); InputStream is = new BufferedInputStream(x))
			{
				return Utils.getTXTFileContent(is, charset);
			}
		}

		@Override
		public String toString()
		{
			return "DirPackage: " + dir.getAbsolutePath();
		}

	}

}