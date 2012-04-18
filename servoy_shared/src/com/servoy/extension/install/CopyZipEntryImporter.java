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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.servoy.extension.FileBasedExtensionProvider;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

/**
 * Copies entries from exp file to a Servoy install folder.
 * 
 * @author lvostinar
 *
 */
public class CopyZipEntryImporter
{
	private final static String EXPFILES_FOLDER = "application_server/.extensions";

	private final File expFile;
	private final File installDir;
	private final File screenshotsFolder;
	private final File developerFolder;
	private final File docsFolfer;

	private final List<String> warnings = new ArrayList<String>();
	private final String extensionID;

	public CopyZipEntryImporter(File expFile, File installDir, String extensionID)
	{
		this.expFile = expFile;
		this.installDir = installDir;
		this.extensionID = extensionID;
		screenshotsFolder = new File(installDir, "screenshots"); //$NON-NLS-1$
		developerFolder = new File(installDir, "developer"); //$NON-NLS-1$
		docsFolfer = new File(installDir, "application_server/docs/" + extensionID); //$NON-NLS-1$
	}

	public void importFile()
	{
		if (expFile != null && expFile.exists() && expFile.isFile() && expFile.canRead() &&
			expFile.getName().endsWith(FileBasedExtensionProvider.EXTENSION_PACKAGE_FILE_EXTENSION) && installDir != null && installDir.exists() &&
			installDir.isDirectory() && installDir.canWrite())
		{
			ZipFile zipFile = null;
			try
			{
				zipFile = new ZipFile(expFile);
				Enumeration entries = zipFile.entries();
				while (entries.hasMoreElements())
				{
					ZipEntry entry = (ZipEntry)entries.nextElement();
					if (!entry.isDirectory())
					{
						String fileName = entry.getName().replace('\\', '/');
						fileName = fileName.replace("application_server/webtemplates", "application_server/server/webapps/ROOT/servoy-webclient/templates");
						File outputFile = new File(installDir, fileName);
						copyFile(outputFile, zipFile.getInputStream(entry));
					}
				}
				File expCopy = new File(installDir + File.separator + EXPFILES_FOLDER, expFile.getName());
				InputStream stream = new FileInputStream(expFile);
				copyFile(expCopy, new BufferedInputStream(stream));
				Utils.closeInputStream(stream);

			}
			catch (IOException ex)
			{
				Debug.error("Exception while reading expFile: " + expFile, ex); //$NON-NLS-1$
			}
			finally
			{
				if (zipFile != null)
				{
					try
					{
						zipFile.close();
					}
					catch (IOException e)
					{
						// ignore
					}
				}
			}

		}
		else
		{
			// shouldn't happen
			Debug.error("Invalid import file/destination: " + expFile + " " + installDir); //$NON-NLS-1$
		}
	}

	private void copyFile(File outputFile, InputStream inputStream)
	{
		try
		{
			if (!isInParentDir(installDir, outputFile))
			{
				warnings.add("Cannot copy file outside install dir, will be skipped: " + outputFile); //$NON-NLS-1$
				return;
			}
			if (skipFile(outputFile))
			{
				return;
			}
			if (outputFile.exists())
			{
				warnings.add("Duplicate file found, will be overwritten: " + outputFile); //$NON-NLS-1$
			}
			else
			{
				if (!outputFile.getParentFile().exists())
				{
					outputFile.getParentFile().mkdirs();
				}
				outputFile.createNewFile();
			}
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outputFile));
			Utils.streamCopy(inputStream, out);
			Utils.closeOutputStream(out);
			Utils.closeInputStream(inputStream);
		}
		catch (IOException ex)
		{
			Debug.error("Exception while writing file: " + outputFile, ex); //$NON-NLS-1$
		}
	}

	private boolean isInParentDir(File parentDir, File outputFile)
	{
		if (outputFile != null && parentDir != null)
		{
			while (outputFile.getParentFile() != null)
			{
				if (parentDir.equals(outputFile.getParentFile()))
				{
					return true;
				}
				outputFile = outputFile.getParentFile();
			}
		}
		return false;
	}

	private boolean skipFile(File outputFile)
	{
		if (outputFile.getName().equals("extension.xml")) return true; //$NON-NLS-1$
		if (isInParentDir(screenshotsFolder, outputFile)) return true;
		if (!developerFolder.exists() && isInParentDir(developerFolder, outputFile))
		{
			warnings.add("Skipping file because developer folder does not exist: " + outputFile); //$NON-NLS-1$
			return true;
		}
		if (isInParentDir(docsFolfer.getParentFile(), outputFile) && !isInParentDir(docsFolfer, outputFile))
		{
			warnings.add("Skipping file because is incorrect extension id folder: " + outputFile); //$NON-NLS-1$
			return true;
		}
		return false;
	}

	public static void main(String[] args)
	{
		// for testing only
		File expFile = new File("E:\\trunk\\j2db_test\\src\\com\\servoy\\extension\\expfiles\\Aver1.exp");
		File targetDir = new File("E:\\temp\\test_extensions");
		CopyZipEntryImporter importer = new CopyZipEntryImporter(expFile, targetDir, "test");
		importer.importFile();
		System.out.println(importer.warnings);
	}
}
