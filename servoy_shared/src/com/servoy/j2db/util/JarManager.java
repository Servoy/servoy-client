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
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * @author jblok
 */
public abstract class JarManager
{
	public static void addCommonPackageToDefinitions(Extension[] exts, Map<String, Object> _loadedBeanDefs)
	{
		String lastJarFileName = null;
		List<String> workingClassNames = new ArrayList<String>();

		//group by jarFileName
		for (int i = 0; i < exts.length; i++)
		{
			if (lastJarFileName != null && !exts[i].jarFileName.equals(lastJarFileName))
			{
				addCommonPackageToDefinitions(lastJarFileName, workingClassNames, _loadedBeanDefs);
				workingClassNames = new ArrayList<String>();
			}
			workingClassNames.add(exts[i].instanceClass.getName());
			lastJarFileName = exts[i].jarFileName;
		}
		if (workingClassNames.size() != 0)
		{
			addCommonPackageToDefinitions(lastJarFileName, workingClassNames, _loadedBeanDefs);
		}
	}

	@SuppressWarnings("unchecked")
	protected static void addCommonPackageToDefinitions(String fileName, List<String> workingClassNames, Map<String, Object> _loadedBeanDefs)
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
//					Debug.trace("nameToCheck "+nameToCheck.toString());
					count++;
				}
				if (commonPart == null)
				{
					commonPart = nameToCheck.toString();
//					Debug.trace("commonPart "+commonPart);
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
				Debug.trace("Final commonPart " + commonPart); //$NON-NLS-1$
				Object prev = _loadedBeanDefs.get(commonPart);
				if (prev != null)
				{
					if (prev instanceof String)
					{
						ArrayList<String> al = new ArrayList<String>();
						al.add((String)prev);
						al.add(fileName);
						_loadedBeanDefs.put(commonPart, al);
					}
					else
					{
						((List<String>)prev).add(fileName);
					}
				}
				else
				{
					_loadedBeanDefs.put(commonPart, fileName);
				}
				break;
			}
		}
		while (!matched && wantedLength > 0);
	}

	//searchURLs can be subset of all urls in classloader to speedup loading
	@SuppressWarnings("unchecked")
	public <C> Class<C>[] getAssignableClasses(ExtendableURLClassLoader loader, Class<C> type, Map<URL, Pair<String, Long>> searchURLs)
	{
		List<Class< ? >> classes = new ArrayList<Class< ? >>();
		Extension[] exts = getExtensions(loader, type, searchURLs);
		for (Extension element : exts)
		{
			classes.add(element.instanceClass);
		}
		return classes.toArray(new Class[classes.size()]);
	}

	public static class Extension
	{
		public Class< ? > searchType;
		public Class< ? > instanceClass;
//		public Object instance;
//		public String name;
		public URL jarUrl;
		public String jarFileName;
		public long jarFileModTime;

//		public boolean enabled = true;

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString()
		{
			return "Extension[" + jarFileName + '=' + jarUrl + ']'; //$NON-NLS-1$
		}
	}

	//searchURLs: url->pair(jarname,jarmodtime)
	//loader must know all the urls already
	public Extension[] getExtensions(ExtendableURLClassLoader loader, Class< ? > searchType, Map<URL, Pair<String, Long>> searchURLs)
	{
		List<Extension> extensions = new ArrayList<Extension>();
		Iterator<Map.Entry<URL, Pair<String, Long>>> it = searchURLs.entrySet().iterator();
		while (it.hasNext())
		{
			Map.Entry<URL, Pair<String, Long>> entry = it.next();

			URL url = entry.getKey();
			InputStream is = null;
			boolean tryWithFiles = false;
			try
			{
				is = url.openStream();
				if (is != null)
				{
					final ZipInputStream zip = new ZipInputStream(is);
					readZipEntries(loader, searchType, extensions, entry, url, new Enumeration<ZipEntry>()
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
					readZipEntries(loader, searchType, extensions, entry, url, enumeration);
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
	private void readZipEntries(ExtendableURLClassLoader loader, Class< ? > searchType, List<Extension> extensions, Map.Entry<URL, Pair<String, Long>> entry,
		URL url, Enumeration< ? extends ZipEntry> enumeration)
	{
		while (enumeration.hasMoreElements())
		{
			Class< ? > cls = null;
			String entryName = ((ZipEntry)enumeration.nextElement()).getName();
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
						Extension ext = new Extension();
						ext.jarFileName = entry.getValue().getLeft();
						ext.jarFileModTime = entry.getValue().getRight().longValue();
						ext.jarUrl = url;
						ext.instanceClass = cls;
						ext.searchType = searchType;
						extensions.add(ext);
					}
				}
			}
		}
	}

	public static Map<URL, Pair<String, Long>> loadLibs(File dir)
	{
		Map<URL, Pair<String, Long>> retval = new HashMap<URL, Pair<String, Long>>();
		readDir(dir, retval, null, null, false);
		return retval;
	}

	public static List<String> readDir(File dir, Map<URL, Pair<String, Long>> baseRetval, Map<URL, Pair<String, Long>> subDirRetval,
		Map<String, Object> packageJarMapping, boolean isSubDir)
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
							subDirRetval.put(jarFile.toURI().toURL(), new Pair<String, Long>(fileName, new Long(jarFile.lastModified())));
						}
						else
						{
							baseRetval.put(jarFile.toURI().toURL(), new Pair<String, Long>(fileName, new Long(jarFile.lastModified())));

							if (packageJarMapping != null)
							{
								JarFile file = new JarFile(jarFile);
								Manifest mf = file.getManifest();
								if (mf != null)
								{
									List<String> beanClassNames = getBeanClassNames(mf);
									if (beanClassNames.size() > 0)
									{
										addCommonPackageToDefinitions(fileName, beanClassNames, packageJarMapping);
										foundBeanClassNames.addAll(beanClassNames);

										Map<String, File> classPathReferences = getManifestClassPath(jarFile, dir);
										if (classPathReferences != null && classPathReferences.size() > 0)
										{
											for (String reference : classPathReferences.keySet())
											{
												addCommonPackageToDefinitions(reference, beanClassNames, packageJarMapping);
												subDirRetval.put(classPathReferences.get(reference).toURI().toURL(), new Pair(reference, new Long(
													classPathReferences.get(reference).lastModified())));
											}
										}
									}
									else
									{
										packageJarMapping.put("#" + packageJarMapping.size(), fileName);//is likly jar for applet //$NON-NLS-1$
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

	public static List<String> getBeanClassNames(Manifest mf)
	{
		HashMap<String, Boolean> beans = new HashMap<String, Boolean>();
		Map<String, Attributes> entries = mf.getEntries();
		Iterator<String> it = entries.keySet().iterator();
		if (!it.hasNext())
		{
			checkIfBeanAttribute(mf.getMainAttributes(), null, beans);
		}
		while (it.hasNext())
		{
			String key = it.next();
			Attributes attr = entries.get(key);
			checkIfBeanAttribute(attr, key, beans);
		}

		ArrayList<String> beanNames = new ArrayList<String>();
		int i = 0;
		Iterator<String> it2 = beans.keySet().iterator();
		while (it2.hasNext())
		{
			i++;
			String key = it2.next();
			beanNames.add(key);
		}

		return beanNames;
	}

	private static void checkIfBeanAttribute(Attributes attr, String key, Map<String, Boolean> beans)
	{
		if (attr == null) return;

		String name = key;
		if (name == null) name = (String)attr.get(new Attributes.Name("Name")); //$NON-NLS-1$
		String isBean = (String)attr.get(new Attributes.Name("Java-Bean")); //$NON-NLS-1$
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

}
