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

package com.servoy.extension;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Utils;

/**
 * Utility methods of interest only to extension classes.
 * @author acostescu
 */
@SuppressWarnings("nls")
public class ExtensionUtils
{

	public final static String EXPFILES_FOLDER = "application_server/.extensions"; //$NON-NLS-1$

	/**
	 * Valid id's are simple or qualified java names.
	 * @param id the id to check.
	 * @throws IllegalArgumentException when the id does not adhere to these rules.
	 */
	public static void assertValidId(String id) throws IllegalArgumentException
	{
		if (!Utils.isValidJavaSimpleOrQualifiedName(id)) throw new IllegalArgumentException("Unsupported id string format.");
	}

	/**
	 * Runs the given runner on the given zip file entry (prepares the input stream for use in the runner).
	 * @param file the zip file.
	 * @param entry an entry in the zip file.
	 * @param runner the runner.
	 * @return first item in the pair is Boolean.TRUE if the entry was found and Boolean.FALSE otherwise.
	 */
	public static <T> Pair<Boolean, T> runOnEntry(File file, String entry, EntryInputStreamRunner<T> runner) throws IOException, ZipException
	{
		Pair<Boolean, T> result; // not initialized; should never be null and if it's not initialized and no compiler warnings, then all branches are covered
		ZipFile zipFile = null;
		try
		{
			zipFile = new ZipFile(file);
			ZipEntry zipEntry = zipFile.getEntry(entry);
			if (zipEntry != null)
			{
				InputStream is = null;
				BufferedInputStream bis = null;
				try
				{
					is = zipFile.getInputStream(zipEntry);
					bis = new BufferedInputStream(is);

					result = new Pair<Boolean, T>(Boolean.TRUE, runner.runOnEntryInputStream(bis));
				}
				finally
				{
					Utils.closeInputStream(bis);
				}
			}
			else
			{
				result = new Pair<Boolean, T>(Boolean.FALSE, null);
			}
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

		return result;
	}

	public static interface EntryInputStreamRunner<T>
	{

		T runOnEntryInputStream(InputStream is) throws IOException;

	}

	/**
	 * Extracts a zip file entry to a destination file.
	 * @param zipFile the zip file.
	 * @param entryPath path of the entry in the zip file.
	 * @param destinationFile the file to be created/copied to.
	 * @return true if a zip entry with the given path was found and false otherwise.
	 * @throws IOException if problems occurred in the process.
	 * @throws ZipException if problems occurred in the process.
	 */
	public static boolean extractZipEntryToFile(File zipFile, String entryPath, final File destinationFile) throws IOException, ZipException
	{
		return ExtensionUtils.runOnEntry(zipFile, entryPath, new EntryInputStreamRunner<Object>()
		{

			public Object runOnEntryInputStream(InputStream is) throws IOException
			{
				BufferedOutputStream os = null;
				try
				{
					destinationFile.getParentFile().mkdirs();
					os = new BufferedOutputStream(new FileOutputStream(destinationFile));
					Utils.streamCopy(is, os);
				}
				finally
				{
					Utils.closeOutputStream(os);
				}
				return null;
			}

		}).getLeft().booleanValue();
	}

	public static String[] getZipEntryNames(File zipFile) throws IOException
	{
		ArrayList<String> zipEntryNames = new ArrayList<String>();

		ZipFile zip = new ZipFile(zipFile);
		Enumeration< ? extends ZipEntry> zipEntries = zip.entries();
		while (zipEntries.hasMoreElements())
			zipEntryNames.add(zipEntries.nextElement().getName());

		return zipEntryNames.toArray(new String[zipEntryNames.size()]);
	}

	// FIXME I think this might not work well for paths like [...]/../[...]
	// moved out of CopyZipEntryImporter for reuse
	public static boolean isInParentDir(File parentDir, File file)
	{
		File current = file;
		if (current != null && parentDir != null)
		{
			while (current.getParentFile() != null)
			{
				if (parentDir.equals(current.getParentFile()))
				{
					return true;
				}
				current = current.getParentFile();
			}
		}
		return false;
	}

}
