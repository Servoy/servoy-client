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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.servlet.ServletContext;

import org.apache.commons.io.FilenameUtils;
import org.json.JSONObject;
import org.sablo.specification.property.IPropertyType;

import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

/**
 * An abstraction of package that contains Servoy web-components.
 * @author acostescu
 */
@SuppressWarnings("nls")
public class WebComponentPackage
{

	private static final String GLOBAL_TYPES_MANIFEST_ATTR = "Global-Types";

	public interface IPackageReader
	{
		String getName();

		String getPackageName();

		Manifest getManifest() throws IOException;

		String readTextFile(String path, Charset charset) throws IOException;

		URL getUrlForPath(String path);

	}

	private IPackageReader reader;
	private List<WebComponentSpec> cachedDescriptions; // probably useful for developer inthe future

	public WebComponentPackage(IPackageReader reader)
	{
		if (reader == null) throw new NullPointerException();
		this.reader = reader;
	}

	public String getName()
	{
		return reader.getName();
	}

	public void appendGlobalTypesJSON(JSONObject allGlobalTypesFromAllPackages) throws IOException
	{
		Manifest mf = reader.getManifest();

		if (mf != null)
		{
			Attributes mainAttrs = mf.getMainAttributes();
			if (mainAttrs != null)
			{
				String globalTypesSpecPath = mainAttrs.getValue(GLOBAL_TYPES_MANIFEST_ATTR);
				if (globalTypesSpecPath != null)
				{
					try
					{
						String specfileContent = reader.readTextFile(globalTypesSpecPath, Charset.forName("UTF8")); // TODO: check encoding
						if (specfileContent != null)
						{
							JSONObject json = new JSONObject('{' + specfileContent + '}');
							Object types = json.get(WebComponentSpec.TYPES_KEY);
							if (types instanceof JSONObject)
							{
								Iterator<String> typesIt = ((JSONObject)types).keys();
								while (typesIt.hasNext())
								{
									String key = typesIt.next();
									allGlobalTypesFromAllPackages.put(key, ((JSONObject)types).get(key));
								}
							}
						}
					}
					catch (Exception e)
					{
						Debug.error("Cannot parse global spec file '" + globalTypesSpecPath + "' from package '" + reader.toString() + "'. ", e);
					}
				}
			}
		}
	}

	public List<WebComponentSpec> getWebComponentDescriptions(Map<String, IPropertyType> globalTypes) throws IOException
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
							WebComponentSpec parsed = WebComponentSpec.parseSpec(specfileContent, reader.getPackageName(), globalTypes, specpath);
							// add properties defined by us
							if (parsed.getProperty("size") == null) parsed.putProperty("size", new PropertyDescription("size",
								IPropertyType.Default.dimension.getType()));
							if (parsed.getProperty("location") == null) parsed.putProperty("location", new PropertyDescription("location",
								IPropertyType.Default.point.getType()));
							if (parsed.getProperty("anchors") == null) parsed.putProperty("anchors", new PropertyDescription("anchors",
								IPropertyType.Default.intnumber.getType()));
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

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.servoy.j2db.server.ngclient.component.WebComponentPackage.IPackageReader#getName()
		 */
		@Override
		public String getName()
		{
			return jarFile.getAbsolutePath();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.servoy.j2db.server.ngclient.component.WebComponentPackage.IPackageReader#getPackageName()
		 */
		@Override
		public String getPackageName()
		{
			return FilenameUtils.getBaseName(jarFile.getAbsolutePath());
		}

		@Override
		public Manifest getManifest() throws IOException
		{
			JarFile jar = null;
			try
			{
				jar = new JarFile(jarFile);
				return jar.getManifest();
			}
			finally
			{
				if (jar != null) jar.close();
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.servoy.j2db.server.ngclient.component.WebComponentPackage.IPackageReader#getUrlForPath(java.lang.String)
		 */
		@Override
		public URL getUrlForPath(String path)
		{
			JarFile jar = null;
			try
			{
				jar = new JarFile(jarFile);
				JarEntry entry = jar.getJarEntry(path.substring(1)); // strip /
				if (entry != null)
				{
					return new URL("jar:" + jarFile.toURI().toURL() + "!" + path);
				}
			}
			catch (IOException e)
			{
				Debug.error(e);
			}
			finally
			{
				if (jar != null) try
				{
					jar.close();
				}
				catch (IOException e)
				{
				}
			}
			return null;
		}

		@Override
		public String readTextFile(String path, Charset charset) throws IOException
		{
			JarFile jar = null;
			try
			{
				jar = new JarFile(jarFile);
				JarEntry entry = jar.getJarEntry(path);
				if (entry != null)
				{
					InputStream is = jar.getInputStream(entry);
					return Utils.getTXTFileContent(is, charset);
				}
			}
			finally
			{
				if (jar != null) jar.close();
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

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.servoy.j2db.server.ngclient.component.WebComponentPackage.IPackageReader#getName()
		 */
		@Override
		public String getName()
		{
			return dir.getAbsolutePath();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.servoy.j2db.server.ngclient.component.WebComponentPackage.IPackageReader#getPackageName()
		 */
		@Override
		public String getPackageName()
		{
			return dir.getName();
		}

		@Override
		public Manifest getManifest() throws IOException
		{
			InputStream is = null;
			try
			{
				is = new BufferedInputStream(new FileInputStream(new File(dir, "META-INF/MANIFEST.MF")));
				return new Manifest(is);
			}
			finally
			{
				if (is != null) is.close();
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.servoy.j2db.server.ngclient.component.WebComponentPackage.IPackageReader#getUrlForPath(java.lang.String)
		 */
		@Override
		public URL getUrlForPath(String path)
		{
			File file = new File(dir, path);
			if (file.exists())
			{
				try
				{
					return file.toURI().toURL();
				}
				catch (MalformedURLException e)
				{
					Debug.error(e);
				}
			}
			return null;
		}

		@Override
		public String readTextFile(String path, Charset charset) throws IOException
		{
			InputStream is = null;
			try
			{
				is = new BufferedInputStream(new FileInputStream(new File(dir, path)));
				return Utils.getTXTFileContent(is, charset);
			}
			finally
			{
				if (is != null) is.close();
			}
		}

		@Override
		public String toString()
		{
			return "DirPackage: " + dir.getAbsolutePath();
		}
	}

	public static class WarURLPackageReader implements WebComponentPackage.IPackageReader
	{
		private final URL urlOfManifest;
		private final String packageName;
		private final ServletContext servletContext;

		public WarURLPackageReader(ServletContext servletContext, String packageName) throws MalformedURLException
		{
			this.packageName = packageName.endsWith("/") ? packageName : packageName + "/";
			this.urlOfManifest = servletContext.getResource(this.packageName + "META-INF/MANIFEST.MF");
			this.servletContext = servletContext;
			if (urlOfManifest == null)
			{
				throw new IllegalArgumentException("Package " + this.packageName + "META-INF/MANIFEST.MF not found in this context");
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.servoy.j2db.server.ngclient.component.WebComponentPackage.IPackageReader#getName()
		 */
		@Override
		public String getName()
		{
			return urlOfManifest.toExternalForm();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.servoy.j2db.server.ngclient.component.WebComponentPackage.IPackageReader#getPackageName()
		 */
		@Override
		public String getPackageName()
		{
			return packageName.replaceAll("/", "");
		}

		@Override
		public Manifest getManifest() throws IOException
		{
			InputStream is = urlOfManifest.openStream();
			try
			{
				Manifest manifest = new Manifest();
				manifest.read(is);
				return manifest;
			}
			finally
			{
				is.close();
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.servoy.j2db.server.ngclient.component.WebComponentPackage.IPackageReader#getUrlForPath(java.lang.String)
		 */
		@Override
		public URL getUrlForPath(String path)
		{
			try
			{
				return servletContext.getResource(packageName + path);// path includes /
			}
			catch (MalformedURLException e)
			{
				Debug.error(e);
				return null;
			}
		}

		@SuppressWarnings("nls")
		@Override
		public String readTextFile(String path, Charset charset) throws IOException
		{
			URL url = getUrlForPath(path);
			if (url == null) return null;
			InputStream is = null;
			try
			{
				is = url.openStream();
				return Utils.getTXTFileContent(is, charset);
			}
			finally
			{
				if (is != null) is.close();
			}
		}
	}

}