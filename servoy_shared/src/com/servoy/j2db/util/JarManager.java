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


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * @author jblok
 */
@SuppressWarnings("nls")
public abstract class JarManager
{
	public static final String JAVA_BEAN_ATTRIBUTE = "Java-Bean";
	public static final String SERVOY_PLUGIN_ATTRIBUTE = "Servoy-Plugin";

	public static void addCommonPackageToDefinitions(Extension[] extensions, Map<String, List<Extension>> packageJarMapping)
	{
		Extension lastJarFileName = null;
		List<String> workingClassNames = new ArrayList<String>();

		//group by jarFileName
		for (Extension ext : extensions)
		{
			if (lastJarFileName != null && !ext.jarFileName.equals(lastJarFileName))
			{
				addCommonPackageToDefinitions(lastJarFileName, workingClassNames, packageJarMapping);
				workingClassNames = new ArrayList<String>();
			}
			workingClassNames.add(ext.instanceClass.getName());
			lastJarFileName = ext;
		}
		if (workingClassNames.size() != 0)
		{
			addCommonPackageToDefinitions(lastJarFileName, workingClassNames, packageJarMapping);
		}
	}

	protected static void addCommonPackageToDefinitions(Extension ext, List<String> workingClassNames, Map<String, List<Extension>> packageJarMapping)
	{
		boolean matched = true;
		int wantedLength = 3;
		do
		{
			matched = true;
			String commonPart = null;
			Iterator<String> it = workingClassNames.iterator();
			while (it.hasNext())
			{
				String className = it.next();
				int count = 0;
				StringBuffer nameToCheck = new StringBuffer();
				StringTokenizer tokenizer = new StringTokenizer(className, "."); //$NON-NLS-1$
				while (tokenizer.hasMoreTokens() && count != wantedLength)
				{
					if (count != 0) nameToCheck.append("."); //$NON-NLS-1$
					nameToCheck.append(tokenizer.nextToken());
					count++;
				}
				if (commonPart == null)
				{
					commonPart = nameToCheck.toString();
				}
				else if (!nameToCheck.toString().equals(commonPart))
				{
					matched = false;
				}
			}
			if (!matched)
			{
				wantedLength--;
			}
			else
			{
				List<Extension> prev = packageJarMapping.get(commonPart);
				if (prev != null)
				{
					prev.add(ext);
				}
				else
				{
					List<Extension> exts = new ArrayList<Extension>(1);
					exts.add(ext);
					packageJarMapping.put(commonPart, exts);
				}
				break;
			}
		}
		while (!matched && wantedLength > 0);
	}

	//searchURLs can be subset of all urls in classloader to speedup loading
	@SuppressWarnings("unchecked")
	public <C> Class<C>[] getAssignableClasses(ExtendableURLClassLoader loader, Class<C> type, List<Extension> searchURLs)
	{
		List<Class< ? >> classes = new ArrayList<Class< ? >>();
		Extension[] exts = getExtensions(loader, type, searchURLs);
		for (Extension element : exts)
		{
			classes.add(element.instanceClass);
		}
		return classes.toArray(new Class[classes.size()]);
	}

	/**
	 * An extension represents one jar, optionally contains class type info
	 */
	public static class Extension
	{
		public final URL jarUrl;
		public final String jarFileName;
		public final long jarFileModTime;

		//optional when null, it may indicate a support jar
		public Class< ? > searchType;
		public Class< ? > instanceClass;
		public boolean hasClasses = true;

		public Extension(URL url, String fileName, long lastModified)
		{
			jarUrl = url;
			jarFileName = fileName;
			jarFileModTime = lastModified;
		}

		@Override
		public String toString()
		{
			return "Extension[" + jarFileName + '=' + jarUrl + ']'; //$NON-NLS-1$
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + (int)(jarFileModTime ^ (jarFileModTime >>> 32));
			result = prime * result + ((jarUrl == null) ? 0 : jarUrl.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			Extension other = (Extension)obj;
			if (jarFileModTime != other.jarFileModTime) return false;
			if (jarUrl == null)
			{
				if (other.jarUrl != null) return false;
			}
			else if (!jarUrl.equals(other.jarUrl)) return false;
			return true;
		}

		public boolean hasClasses()
		{
			return hasClasses;
		}
	}

	//searchURLs: url->pair(jarname,jarmodtime)
	//loader must know all the urls already
	public Extension[] getExtensions(ExtendableURLClassLoader loader, Class< ? > searchType, List<Extension> searchURLs)
	{
		List<Extension> extensions = new ArrayList<Extension>();
		Iterator<Extension> it = searchURLs.iterator();
		while (it.hasNext())
		{
			Extension entry = it.next();
			URL url = entry.jarUrl;
			InputStream is = null;
			boolean tryWithFiles = false;
			try
			{
				is = url.openStream();
				if (is != null)
				{
					final ZipInputStream zip = new ZipInputStream(is);
					readZipEntries(loader, searchType, extensions, entry, new Enumeration<ZipEntry>()
					{
						private ZipEntry nextEntry = zip.getNextEntry();

						public boolean hasMoreElements()
						{
							return nextEntry != null;
						}

						public ZipEntry nextElement()
						{
							ZipEntry retValue = nextEntry;
							try
							{
								nextEntry = zip.getNextEntry();
							}
							catch (IOException e)
							{
								nextEntry = null;
							}
							return retValue;
						}
					});
					zip.close();
				}
				else tryWithFiles = true;


			}
			catch (IOException e1)
			{
				Debug.trace(e1);
				tryWithFiles = true;
			}
			finally
			{
				if (is != null) try
				{
					is.close();
				}
				catch (IOException e)
				{
					// ignore
				}
			}
			if (tryWithFiles)
			{
				File file = null;
				try
				{
					file = new File(new URI(url.toExternalForm()));
				}
				catch (Exception e)
				{
					Debug.error("Error occured trying to load: " + url + ", error: " + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
					continue;
				}
				if (!file.isDirectory() && file.exists() && file.canRead())
				{
					ZipFile zipFile = null;
					try
					{
						zipFile = new ZipFile(file);
					}
					catch (Exception ex)
					{
						Debug.error("Error occured trying to load: " + file.getAbsolutePath() + ", error: " + ex.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
						continue;
					}

					Enumeration< ? extends ZipEntry> enumeration = zipFile.entries();
					readZipEntries(loader, searchType, extensions, entry, enumeration);
				}
			}
		}
		return extensions.toArray(new Extension[extensions.size()]);
	}

	/**
	 * @param loader
	 * @param searchType
	 * @param extensions
	 * @param entry
	 * @param url
	 * @param enumeration
	 */
	private void readZipEntries(ExtendableURLClassLoader loader, Class< ? > searchType, List<Extension> extensions, Extension entry,
		Enumeration< ? extends ZipEntry> enumeration)
	{
		boolean seenClass = false;
		Class< ? > cls = null;
		while (enumeration.hasMoreElements())
		{
			ZipEntry ze = enumeration.nextElement();
			if (!ze.isDirectory())
			{
				String entryName = ze.getName();
				if (!seenClass && entryName.endsWith(".class")) seenClass = true;

				String className = Utils.changeFileNameToClassName(entryName);
				if (className != null)
				{
					try
					{
						cls = loader.loadClass(className);
					}
					catch (Throwable th)
					{
						Debug.trace(th.toString());
					}
					if (cls != null)
					{
						if (searchType.isAssignableFrom(cls) && !Modifier.isAbstract(cls.getModifiers()))
						{
							entry.instanceClass = cls;
							entry.searchType = searchType;
							extensions.add(entry);
						}
					}
				}
			}
		}
		entry.hasClasses = seenClass;
	}

	public static List<Extension> loadLibs(File dir)
	{
		List<Extension> retval = new ArrayList<Extension>();
		readDir(dir, retval, null, null, false);
		return retval;
	}

	public static List<String> readDir(File dir, List<Extension> baseRetval, List<Extension> subDirRetval, Map<String, List<Extension>> packageJarMapping,
		boolean isSubDir)
	{
		List<String> foundBeanClassNames = new ArrayList<String>();
		if (dir != null && dir.isDirectory())
		{
			String[] filesa = dir.list();
			List<String> files = Arrays.asList(filesa);

			//make sure we have a predefined load order (== alphabetically)
			Collections.sort(files);

			Iterator<String> it = files.iterator();
			while (it.hasNext())
			{
				String fileName = it.next();
				if (fileName.toLowerCase().endsWith(".zip") || fileName.toLowerCase().endsWith(".jar")) //$NON-NLS-1$ //$NON-NLS-2$
				{
					try
					{
						File jarFile = new File(dir, fileName);
						if (isSubDir)
						{
							subDirRetval.add(new Extension(jarFile.toURI().toURL(), fileName, jarFile.lastModified()));
						}
						else
						{
							Extension ext = new Extension(jarFile.toURI().toURL(), fileName, jarFile.lastModified());
							baseRetval.add(ext);

							if (packageJarMapping != null)
							{
								JarFile file = new JarFile(jarFile);
								Manifest mf = file.getManifest();
								if (mf != null)
								{
									List<String> beanClassNames = getClassNamesForKey(mf, JAVA_BEAN_ATTRIBUTE);
									if (beanClassNames.size() > 0)
									{
										addCommonPackageToDefinitions(ext, beanClassNames, packageJarMapping);
										foundBeanClassNames.addAll(beanClassNames);

										Map<String, File> classPathReferences = getManifestClassPath(jarFile, dir);
										if (classPathReferences != null && classPathReferences.size() > 0)
										{
											for (String reference : classPathReferences.keySet())
											{
												Extension ref = new Extension(classPathReferences.get(reference).toURI().toURL(), reference,
													classPathReferences.get(reference).lastModified());
												addCommonPackageToDefinitions(ref, beanClassNames, packageJarMapping);
												subDirRetval.add(ref);
											}
										}
									}
									else
									{
										List<Extension> exts = new ArrayList<Extension>(1);
										exts.add(ext);
										packageJarMapping.put("#" + packageJarMapping.size(), exts);//is likly jar for applet //$NON-NLS-1$
									}
								}
							}
						}
					}
					catch (IOException ex)
					{
						Debug.error("Unable to load lib: " + fileName + "  error: " + ex.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
						Debug.error(ex);
					}
				}
				else
				{
					if (subDirRetval != null)
					{
						File test = new File(dir, fileName);
						if (test.isDirectory())
						{
							foundBeanClassNames.addAll(readDir(test, baseRetval, subDirRetval, packageJarMapping, true));//we don't add subdirs plugins to the jnlp list to prevent clutter
						}
					}
				}
			}
		}
		return foundBeanClassNames;
	}

	public static List<String> getManifestClassPath(URL jarUrl)
	{
		ArrayList<String> lst = new ArrayList<String>();
		JarInputStream jis = null;
		try
		{
			InputStream is = (InputStream)jarUrl.getContent(new Class[] { InputStream.class });
			if (is != null)
			{
				jis = new JarInputStream(is, false);
				Manifest mf = jis.getManifest();
				String classpath = mf.getMainAttributes().getValue(Attributes.Name.CLASS_PATH);
				if (classpath != null)
				{
					StringTokenizer st = new StringTokenizer(classpath, " "); //$NON-NLS-1$
					while (st.hasMoreTokens())
					{
						lst.add(st.nextToken());
					}
				}
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		finally
		{
			Utils.closeInputStream(jis);
		}
		return lst;
	}

	public static Map<String, File> getManifestClassPath(File jarFile, File contextDir)
	{
		Map<String, File> references = new HashMap<String, File>();
		try
		{
			JarFile file = new JarFile(jarFile);
			Manifest mf = file.getManifest();
			if (mf != null)
			{
				String classpath = (String)mf.getMainAttributes().get(Attributes.Name.CLASS_PATH);
				if (classpath != null)
				{
					StringTokenizer st = new StringTokenizer(classpath, " "); //$NON-NLS-1$
					while (st.hasMoreTokens())
					{
						String classPathJar = st.nextToken();
						File classPathFile = null;
						if (classPathJar.startsWith("/")) //$NON-NLS-1$
						{
							classPathFile = new File(contextDir.getParentFile(), classPathJar);
						}
						else
						{
							classPathFile = new File(contextDir, classPathJar);
						}
						if (classPathFile.exists())
						{
							references.put(classPathJar, classPathFile);
						}
						else
						{
							Debug.log("Classpath entry: " + classPathJar + " of jar: " + jarFile.getAbsolutePath() + " not found"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						}
					}
				}
			}
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
		return references;

	}

	public static List<String> getClassNamesForKey(Manifest mf, String attributeName)
	{
		HashMap<String, Boolean> beans = new HashMap<String, Boolean>();
		Map<String, Attributes> entries = mf.getEntries();
		Iterator<String> it = entries.keySet().iterator();
		if (!it.hasNext())
		{
			checkIfHasAttribute(mf.getMainAttributes(), attributeName, null, beans);
		}
		while (it.hasNext())
		{
			String key = it.next();
			Attributes attr = entries.get(key);
			checkIfHasAttribute(attr, attributeName, key, beans);
		}

		ArrayList<String> beanNames = new ArrayList<String>();
		Iterator<String> it2 = beans.keySet().iterator();
		while (it2.hasNext())
		{
			String key = it2.next();
			beanNames.add(key);
		}

		return beanNames;
	}

	private static void checkIfHasAttribute(Attributes attr, String attributeName, String key, Map<String, Boolean> beans)
	{
		if (attr == null) return;

		String name = key;
		if (name == null) name = (String)attr.get(new Attributes.Name("Name")); //$NON-NLS-1$
		String isBean = (String)attr.get(new Attributes.Name(attributeName));
		if (name != null && isBean != null && isBean.equalsIgnoreCase("True")) //$NON-NLS-1$
		{
			String beanName;
			boolean fromPrototype = true;
			if (name.endsWith(".class")) //$NON-NLS-1$
			{
				fromPrototype = false;
				beanName = name.substring(0, name.length() - 6);
			}
			else if (name.endsWith(".ser")) //$NON-NLS-1$
			{
				beanName = name.substring(0, name.length() - 4);
			}
			else
			{
				beanName = name;
			}
			beanName = beanName.replace('/', '.');
			beans.put(beanName, new Boolean(fromPrototype));
		}
	}

	public static URL[] getUrls(List<Extension> exts)
	{
		List<URL> allUrls = new ArrayList<URL>(exts.size());
		for (Extension ext : exts)
		{
			allUrls.add(ext.jarUrl);
		}
		return allUrls.toArray(new URL[allUrls.size()]);
	}

	public static List<Extension> getExtensions(Map<String, List<Extension>> definitions, String filename)
	{
		String jarFileName = filename;
		int index = jarFileName.lastIndexOf('/');
		if (index != -1)
		{
			jarFileName = jarFileName.substring(index + 1);
		}

		Iterator<List<Extension>> it = definitions.values().iterator();
		while (it.hasNext())
		{
			List<Extension> exts = it.next();
			for (Extension ext : exts)
			{
				String name = ext.jarFileName;
				index = name.lastIndexOf('/');
				if (index == -1) index = 0;
				else index++;
				name = name.substring(index);
				if (jarFileName.equals(name))
				{
					return exts;
				}
			}
		}
		return null;
	}
}
