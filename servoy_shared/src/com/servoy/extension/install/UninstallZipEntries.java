/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.extension.install;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.servoy.extension.ExtensionUtils;
import com.servoy.j2db.util.Debug;

/**
 * Deletes installed zip entries.
 * 
 * @author lvostinar
 *
 */
public class UninstallZipEntries extends CopyZipEntryImporter
{

	public UninstallZipEntries(String expfileName, File installDir, String extensionID, String version)
	{
		super(new File(installDir + File.separator + ExtensionUtils.EXPFILES_FOLDER, expfileName), installDir, extensionID, version);
	}

	public UninstallZipEntries(File expFile, File installDir, String extensionID, String version)
	{
		super(expFile, installDir, extensionID, version);
	}

	@Override
	protected void handleZipEntry(File outputFile, ZipFile zipFile, ZipEntry entry) throws IOException
	{
		deleteFile(outputFile);
	}

	@Override
	protected void handleExpFile()
	{
		deleteFile(expFile);
	}

	private void deleteFile(File file)
	{
		if (skipFile(file, false))
		{
			return;
		}
		if (file.exists())
		{
			if (!file.canWrite() || !file.delete())
			{
				messages.addError("Cannot delete file: " + file); //$NON-NLS-1$
			}
		}
		else
		{
			Debug.trace("A file was already removed (before uninstall): " + file); //$NON-NLS-1$
		}
	}

}
