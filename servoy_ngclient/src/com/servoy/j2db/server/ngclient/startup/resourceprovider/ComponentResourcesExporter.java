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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.channels.FileChannel;
import java.util.Enumeration;

import org.eclipse.core.runtime.FileLocator;

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
	 * @return the list of 
	 * @throws Exception 
	 * @throws URISyntaxException 
	 */
	public static String copyComponents(File tmpWarDir) throws IOException
	{
		StringBuilder locations = new StringBuilder();
		Enumeration<String> paths = Activator.getContext().getBundle().getEntryPaths("/war/");
		while (paths.hasMoreElements())
		{
			String path = paths.nextElement();
			String name = path.replace("war/", "");
			File targetDir = new File(tmpWarDir, name);
			copyDir(new File(FileLocator.resolve(Activator.getContext().getBundle().getEntry(path)).getPath()), targetDir, true);
			if (!name.equals("js/") && !name.equals("css/") && !name.equals("templates/"))
			{
				locations.append("/" + name + ";");
			}
		}
		locations.deleteCharAt(locations.length() - 1);
		return locations.toString();
	}

	public static void copyLibs(File libDir) throws IOException
	{
		copyDir(new File(FileLocator.resolve(Activator.getContext().getBundle().getEntry("/lib")).getPath()), libDir, true); //$NON-NLS-1$
	}

	private static void copyDir(File sourceDir, File destDir, boolean recusive) throws IOException
	{
		if (!destDir.exists() && !destDir.mkdirs()) throw new IOException("Can't create destination dir: " + destDir); //$NON-NLS-1$
		File[] listFiles = sourceDir.listFiles();
		for (File file : listFiles)
		{
			if (file.isDirectory())
			{
				if (recusive) copyDir(file, new File(destDir, file.getName()), recusive);
			}
			else
			{
				copyFile(file, new File(destDir, file.getName()));
			}
		}
	}

	private static void copyFile(File sourceFile, File destFile) throws IOException
	{
		if (!sourceFile.exists())
		{
			return;
		}
		try
		{
			if (!destFile.getParentFile().exists())
			{
				destFile.getParentFile().mkdirs();
			}
			if (!destFile.exists())
			{
				destFile.createNewFile();
			}
			FileChannel source = null;
			FileChannel destination = null;
			source = new FileInputStream(sourceFile).getChannel();
			destination = new FileOutputStream(destFile).getChannel();
			if (destination != null && source != null)
			{
				destination.transferFrom(source, 0, source.size());
			}
			if (source != null)
			{
				source.close();
			}
			if (destination != null)
			{
				destination.close();
			}
		}
		catch (IOException e)
		{
			throw new IOException("Cant'copy file from " + sourceFile + " to " + destFile, e);
		}
	}
}
