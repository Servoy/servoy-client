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
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;

import com.servoy.extension.ExtensionUtils;
import com.servoy.extension.FileBasedExtensionProvider;
import com.servoy.extension.IMessageProvider;
import com.servoy.extension.Message;
import com.servoy.extension.MessageKeeper;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.SortedList;
import com.servoy.j2db.util.Utils;

/**
 * Copies entries from exp file to a Servoy install folder.
 * 
 * @author lvostinar
 *
 */
public class CopyZipEntryImporter implements IMessageProvider
{
	public final static String EXPFILES_FOLDER = "application_server/.extensions"; //$NON-NLS-1$
	private final static String BACKUP_FOLDER = EXPFILES_FOLDER + "/.backup"; //$NON-NLS-1$
	final static String WEBTEMPLATES_SOURCE_FOLDER = "application_server/webtemplates"; //$NON-NLS-1$
	final static String WEBTEMPLATES_DESTINATION_FOLDER = "application_server/server/webapps/ROOT/servoy-webclient/templates"; //$NON-NLS-1$

	protected final File expFile;
	protected final File installDir;
	protected final String extensionID;
	protected final String version;

	private final File screenshotsFolder;
	private final File developerFolder;
	private final File docsFolder;

	protected final MessageKeeper messages = new MessageKeeper();

	public CopyZipEntryImporter(File expFile, File installDir, String extensionID, String version)
	{
		this.expFile = expFile;
		this.installDir = installDir;
		this.version = version;
		this.extensionID = extensionID;
		screenshotsFolder = new File(installDir, "screenshots"); //$NON-NLS-1$
		developerFolder = new File(installDir, "developer"); //$NON-NLS-1$
		docsFolder = new File(installDir, "application_server/docs/" + extensionID); //$NON-NLS-1$
	}

	public void handleFile()
	{
		if (expFile != null && expFile.exists() && expFile.isFile() && expFile.canRead() &&
			expFile.getName().endsWith(FileBasedExtensionProvider.EXTENSION_PACKAGE_FILE_EXTENSION) && installDir != null && installDir.exists() &&
			installDir.isDirectory() && installDir.canWrite())
		{
			ZipFile zipFile = null;
			try
			{
				zipFile = new ZipFile(expFile);
				Enumeration< ? extends ZipEntry> entries = zipFile.entries();
				while (entries.hasMoreElements())
				{
					ZipEntry entry = entries.nextElement();
					if (!entry.isDirectory())
					{
						String fileName = entry.getName().replace('\\', '/');
						fileName = fileName.replace(WEBTEMPLATES_SOURCE_FOLDER, WEBTEMPLATES_DESTINATION_FOLDER);
						File outputFile = new File(installDir, fileName);
						handleZipEntry(outputFile, zipFile, entry);
					}
				}
			}
			catch (IOException ex)
			{
				String tmp = "Exception while handling entries of expFile: " + expFile; //$NON-NLS-1$
				messages.addError(tmp);
				Debug.error(tmp, ex);
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

				enforceBackUpFolderLimit();
			}
			handleExpFile();
		}
		else
		{
			// shouldn't happen
			String tmp = "Invalid install/uninstall file/destination: " + expFile + ", " + installDir; //$NON-NLS-1$//$NON-NLS-2$
			messages.addError(tmp);
			Debug.error(tmp);
		}
	}

	protected void handleZipEntry(File outputFile, ZipFile zipFile, ZipEntry entry) throws IOException
	{
		copyFile(outputFile, new BufferedInputStream(zipFile.getInputStream(entry)), false);
	}

	protected void handleExpFile()
	{
		String generatedName = extensionID + "_" + version + ".exp"; //$NON-NLS-1$ //$NON-NLS-2$
		File expCopy = new File(installDir + File.separator + EXPFILES_FOLDER, generatedName);
		InputStream stream = null;
		try
		{
			stream = new BufferedInputStream(new FileInputStream(expFile));
			copyFile(expCopy, stream, false);
		}
		catch (IOException ex)
		{
			String tmp = "Exception while handling expFile: " + expFile; //$NON-NLS-1$
			messages.addError(tmp);
			Debug.error(tmp, ex);
		}
		finally
		{
			Utils.closeInputStream(stream);
		}
	}

	protected void enforceBackUpFolderLimit()
	{
		// limit backup folder size to 1 GB; although it's hudge, it's there just not to cause HDD problems because of un-called for backups
		final int MAX = 1024 * 1024 * 1024;

		File backUpFolder = new File(installDir + File.separator + BACKUP_FOLDER);
		if (backUpFolder.exists() && backUpFolder.isDirectory())
		{
			long size = FileUtils.sizeOfDirectory(backUpFolder);
			if (size > MAX)
			{
				// delete oldest files first
				long sizeOverflow = size - MAX;
				List<File> sortedByDate = new SortedList<File>(new Comparator<File>()
				{
					public int compare(File o1, File o2)
					{
						long result = o1.lastModified() - o2.lastModified();
						return (result < 0) ? -1 : (result == 0 ? 0 : 1);
					}
				});
				sortedByDate.addAll(Arrays.asList(backUpFolder.listFiles()));
				for (File f : sortedByDate)
				{
					sizeOverflow -= FileUtils.sizeOf(f);
					FileUtils.deleteQuietly(f);
					if (f.exists()) sizeOverflow += FileUtils.sizeOf(f);

					if (sizeOverflow < 0) break;
				}
			}
		}
	}

	private void copyFile(File outputFile, InputStream inputStream, boolean skipBackup)
	{
		try
		{
			if (skipFile(outputFile, true))
			{
				return;
			}
			if (outputFile.exists())
			{
				if (!skipBackup)
				{
					messages.addWarning("Destination file is already there; old file will be backed up and overwritten: " + outputFile); //$NON-NLS-1$
					backUpReplacedFile(outputFile);
				}
			}
			else
			{
				if (!outputFile.getParentFile().exists())
				{
					outputFile.getParentFile().mkdirs();
				}
				outputFile.createNewFile();
			}
			BufferedOutputStream out = null;
			try
			{
				out = new BufferedOutputStream(new FileOutputStream(outputFile));
				Utils.streamCopy(inputStream, out);
			}
			finally
			{
				Utils.closeOutputStream(out);
			}
		}
		catch (Exception ex)
		{
			String tmp = "Cannot copy file: " + outputFile; //$NON-NLS-1$
			messages.addError(tmp);
			Debug.error(tmp, ex);
		}
		finally
		{
			Utils.closeInputStream(inputStream);
		}
	}

	private void backUpReplacedFile(File sourceFile)
	{
		File backUpFolder = new File(installDir + File.separator + BACKUP_FOLDER);
		boolean ok = true;
		if (!backUpFolder.exists())
		{
			ok = backUpFolder.mkdirs();
		}
		else if (!backUpFolder.isDirectory())
		{
			ok = backUpFolder.delete();
			if (ok) ok = backUpFolder.mkdirs();
		}

		if (ok)
		{
			int i = 0;
			File backUpDestFile;
			do
			{
				backUpDestFile = new File(backUpFolder, sourceFile.getName() + ".bk" + i++); //$NON-NLS-1$
			}
			while (backUpDestFile.exists());

			// do actual copy
			BufferedOutputStream out = null;
			BufferedInputStream in = null;
			try
			{
				out = new BufferedOutputStream(new FileOutputStream(backUpDestFile));
				in = new BufferedInputStream(new FileInputStream(sourceFile));
				Utils.streamCopy(in, out);
			}
			catch (IOException e)
			{
				String tmp = "Cannot back-up replaced file at extension install: " + sourceFile; //$NON-NLS-1$
				messages.addError(tmp);
				Debug.error(tmp, e);
			}
			finally
			{
				Utils.closeInputStream(in);
				Utils.closeOutputStream(out);
			}
		}
		else
		{
			String tmp = "Cannot back-up replaced file at extension install; backup folder not accessible: " + sourceFile; //$NON-NLS-1$
			messages.addError(tmp);
			Debug.error(tmp);
		}
	}

	protected boolean skipFile(File outputFile, boolean logReasons)
	{
		if (!ExtensionUtils.isInParentDir(installDir, outputFile))
		{
			if (logReasons) messages.addWarning("Cannot affect files outside of the install dir; this file will be skipped: " + outputFile); //$NON-NLS-1$
			return true;
		}
		if (outputFile.getName().equals("package.xml")) return true; //$NON-NLS-1$
		if (ExtensionUtils.isInParentDir(screenshotsFolder, outputFile)) return true;
		if (!developerFolder.exists() && ExtensionUtils.isInParentDir(developerFolder, outputFile))
		{
			if (logReasons) messages.addWarning("Skipping file because developer folder does not exist: " + outputFile); //$NON-NLS-1$
			return true;
		}
		if (ExtensionUtils.isInParentDir(docsFolder.getParentFile(), outputFile) && !ExtensionUtils.isInParentDir(docsFolder, outputFile))
		{
			if (logReasons) messages.addWarning("Skipping documentation file (incorrect extension id folder): " + outputFile); //$NON-NLS-1$
			return true;
		}
		return false;
	}

	public Message[] getMessages()
	{
		return messages.getMessages();
	}

	public void clearMessages()
	{
		messages.clear();
	}

}
