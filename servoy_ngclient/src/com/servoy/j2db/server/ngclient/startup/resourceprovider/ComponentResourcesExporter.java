/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.j2db.server.ngclient.startup.resourceprovider;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import com.servoy.j2db.server.ngclient.startup.Activator;

/**
 * Used in war Exporter to copy resources.
 * @author emera
 */
public class ComponentResourcesExporter
{

	/**
	 * Copy the default component packages to war.
	 * @param path
	 * @throws IOException
	 * @throws Exception
	 */
	public static void copyDefaultComponentsAndServices(File tmpWarDir, List<String> excludedComponentPackages, List<String> excludedServicePackages,
		Map<String, File> allTemplates) throws IOException
	{
		List<String> excludedPackages = new ArrayList<String>();
		if (excludedComponentPackages != null) excludedPackages.addAll(excludedComponentPackages);
		if (excludedServicePackages != null) excludedPackages.addAll(excludedServicePackages);
		copy(Activator.getContext().getBundle().getEntryPaths("/war/"), tmpWarDir, excludedPackages, allTemplates);
	}

	/**
	 * Used in war export to create a components.properties file which is needed to load the components specs in war.
	 * @return the locations of components folders relative to the war dir.
	 */
	public static String getDefaultComponentDirectoryNames(List<String> excludedComponentPackages)
	{
		StringBuilder locations = new StringBuilder();
		Enumeration<String> paths = Activator.getContext().getBundle().getEntryPaths("/war/");
		while (paths.hasMoreElements())
		{
			String name = paths.nextElement().replace("war/", "");
			if (name.endsWith("/") && !name.equals("js/") && !name.equals("css/") && !name.equals("templates/") && !name.endsWith("services/"))
			{
				String packageName = name.substring(0, name.length() - 1);
				if (excludedComponentPackages == null || excludedComponentPackages.indexOf(packageName) == -1)
				{
					locations.append("/" + name + ";");
				}
			}
		}
		return locations.toString();
	}

	/**
	 * Used in war export to create a services.properties file, which is needed to load services specs in the war.
	 * @return the locations of services folders relative to the war dir.
	 */
	public static String getDefaultServicesDirectoryNames(List<String> excludedServicePackages)
	{
		StringBuilder locations = new StringBuilder();
		Enumeration<String> paths = Activator.getContext().getBundle().getEntryPaths("/war/");
		while (paths.hasMoreElements())
		{
			String name = paths.nextElement().replace("war/", "");
			if (name.endsWith("services/"))
			{
				String packageName = name.substring(0, name.length() - 1);
				if (excludedServicePackages == null || excludedServicePackages.indexOf(packageName) == -1)
				{
					locations.append("/" + name + ";");
				}
			}
		}
		return locations.toString();
	}

	/**
	 * @param path
	 * @param tmpWarDir
	 * @throws IOException
	 */
	private static void copy(Enumeration<String> paths, File destDir, List<String> excludedPackages, Map<String, File> allTemplates) throws IOException
	{
		if (paths != null)
		{
			while (paths.hasMoreElements())
			{
				String path = paths.nextElement();
				if (path.endsWith("/"))
				{
					String packageName = path.substring("war/".length(), path.length() - 1);
					if (excludedPackages == null || excludedPackages.indexOf(packageName) == -1)
					{
						File targetDir = new File(destDir, FilenameUtils.getName(path.substring(0, path.lastIndexOf("/"))));
						copy(Activator.getContext().getBundle().getEntryPaths(path), targetDir, null, allTemplates);
					}
				}
				else
				{
					URL entry = Activator.getContext().getBundle().getEntry(path);
					File newFile = new File(destDir, FilenameUtils.getName(path));
					FileUtils.copyInputStreamToFile(entry.openStream(), newFile);
					if (newFile.getName().endsWith(".html"))
					{
						allTemplates.put(path.substring("war/".length()), newFile);
					}
				}
			}
		}
	}
}
