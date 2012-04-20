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

package com.servoy.extension.parser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import com.servoy.extension.ExtensionUtils;
import com.servoy.extension.ExtensionUtils.EntryInputStreamRunner;
import com.servoy.j2db.util.Debug;


/**
 * Stores the 'content' (as declared in the extension.xml).<br>
 * This content is used to do special developer install actions. (things to do when installing an extension in developer)
 * @author acostescu
 */
public class Content
{
	/** Relative zip paths pointing to .servoy files that should be imported in developer. */
	public final String[] solutionToImportPaths;
	/** Relative zip paths pointing to .psf files that should be imported in developer. */
	public final String[] teamProjectSetPaths;
	/** Eclipse update sites to be added to developer. */
	public final String[] eclipseUpdateSiteURLs;

	private final File zipFile;

	public Content(File zipFile, String[] solutionToImportPaths, String[] teamProjectSetPaths, String[] eclipseUpdateSiteURLs)
	{
		this.zipFile = zipFile;
		this.solutionToImportPaths = solutionToImportPaths;
		this.teamProjectSetPaths = teamProjectSetPaths;
		this.eclipseUpdateSiteURLs = eclipseUpdateSiteURLs;
	}

	public File[] getTeamProjectSets(String installDir)
	{
		return getFilesForImportPaths(installDir, teamProjectSetPaths);
	}

	public File[] getSolutionFiles(String installDir)
	{
		return getFilesForImportPaths(installDir, solutionToImportPaths);
	}

	public File[] getStyleFiles(String installDir)
	{
		ArrayList<String> stylePaths = new ArrayList<String>();

		try
		{
			String[] zipEntryNames = ExtensionUtils.getZipEntryNames(zipFile);

			for (String zipEntry : zipEntryNames)
			{
				if (zipEntry.startsWith("application_server/styles/")) stylePaths.add(zipEntry); //$NON-NLS-1$
			}
		}
		catch (IOException ex)
		{
			Debug.error(ex);
		}

		String[] styleToImportPaths = stylePaths.size() > 0 ? stylePaths.toArray(new String[stylePaths.size()]) : null;

		return getFilesForImportPaths(installDir, styleToImportPaths);
	}

	private File[] getFilesForImportPaths(String installDir, String[] paths)
	{
		File[] files = null;

		if (paths != null)
		{
			files = new File[paths.length];
			for (int i = 0; i < paths.length; i++)
			{
				final File file = new File(installDir + paths[i]);
				if (!file.exists())
				{
					try
					{
						file.getParentFile().mkdirs();
						ExtensionUtils.runOnEntry(zipFile, paths[i], new EntryInputStreamRunner<File>()
						{
							private FileOutputStream fos;

							public File runOnEntryInputStream(InputStream is)
							{
								try
								{
									fos = new FileOutputStream(file);
									byte[] buffer = new byte[1024];
									int len;

									while ((len = is.read(buffer)) != -1)
									{
										fos.write(buffer, 0, len);
									}
									fos.flush();
								}
								catch (Exception ex)
								{
									Debug.error(ex);
								}
								finally
								{
									if (fos != null) try
									{
										fos.close();
									}
									catch (IOException ex)
									{
										Debug.error(ex);
									}
								}

								return file;
							}
						});
					}
					catch (Exception ex)
					{
						Debug.error(ex);
					}
				}
				files[i] = file;
			}
		}

		return files;
	}
}
