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
import java.util.Enumeration;

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
	public static void copyComponents(File tmpWarDir) throws IOException
	{
		copy(Activator.getContext().getBundle().getEntryPaths("/war/"), tmpWarDir, null);
	}

	public static String getComponentDirectoryNames()
	{
		StringBuilder locations = new StringBuilder();
		Enumeration<String> paths = Activator.getContext().getBundle().getEntryPaths("/war/");
		while (paths.hasMoreElements())
		{
			String name = paths.nextElement().replace("war/", "");
			if (name.endsWith("/") && !name.equals("js/") && !name.equals("css/") && !name.equals("templates/"))
			{
				locations.append("/" + name + ";");
			}
		}
		locations.deleteCharAt(locations.length() - 1);
		return locations.toString();
	}

	/**
	 * @param path
	 * @param tmpWarDir
	 * @throws IOException 
	 */
	private static void copy(Enumeration<String> paths, File destDir, String exclude) throws IOException
	{
		if (paths != null)
		{
			while (paths.hasMoreElements())
			{
				String path = paths.nextElement();
				if (path.endsWith("/"))
				{
					File targetDir = new File(destDir, FilenameUtils.getName(path.substring(0, path.lastIndexOf("/"))));
					copy(Activator.getContext().getBundle().getEntryPaths(path), targetDir, exclude);
				}
				else
				{
					URL entry = Activator.getContext().getBundle().getEntry(path);
					if (exclude == null || (exclude != null && !path.contains(exclude))) FileUtils.copyInputStreamToFile(entry.openStream(), new File(destDir,
						FilenameUtils.getName(path)));
				}
			}
		}
	}

	public static void copyClassFiles(File libDir) throws IOException
	{
		Enumeration<String> entryPaths = Activator.getContext().getBundle().getEntryPaths("/bin/"); //$NON-NLS-1$
		if (entryPaths != null) // servoy_ngclient plugin launched from eclipse workspace
		{
			copy(Activator.getContext().getBundle().getEntryPaths("/bin/com/"), new File(libDir, "com"), "/com/servoy/j2db/server/ngclient/startup/");
			copy(Activator.getContext().getBundle().getEntryPaths("/bin/org/"), new File(libDir, "org"), null);
		}
		else
		{
			copy(Activator.getContext().getBundle().getEntryPaths("/com/"), new File(libDir, "com"), "/com/servoy/j2db/server/ngclient/startup/");
			copy(Activator.getContext().getBundle().getEntryPaths("/org/"), new File(libDir, "org"), null);
		}


	}

	public static void copyLibs(File libDir) throws IOException
	{
		copy(Activator.getContext().getBundle().getEntryPaths("/lib/"), libDir, null);
	}
}
