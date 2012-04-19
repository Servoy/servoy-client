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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.servoy.extension.parser.EXPParser;

/**
 * This class provides extension info & data taken from all extension packages within an OS directory or from a single file.
 * 
 * @author acostescu
 */
@SuppressWarnings("nls")
public class FileBasedExtensionProvider extends CachingExtensionProvider
{

	public static final String EXTENSION_PACKAGE_FILE_EXTENSION = ".exp";

	protected final File file;
	protected boolean thinkDir;

	protected boolean extensionXMLsParsed = false;
	protected Map<String, Map<String, File>> extensionVersionToFile; // <extensionid, <version, File>>
	protected List<String> warnings;

	/**
	 * Creates a new file base extension provider. It can use a directory of .exp files or a single .exp file.
	 * @param file the file or folder.
	 * @param thinkDir if it should be considered a directory; false for file.
	 */
	public FileBasedExtensionProvider(File file, boolean thinkDir)
	{
		if (!file.exists() || !file.canRead() || (thinkDir && !file.isDirectory()) || (!thinkDir && file.isFile()))
		{
			throw new IllegalArgumentException("'" + file + "' is not a valid/accessible directory/file.");
		}
		this.file = file;
		this.thinkDir = thinkDir;
	}

	@Override
	protected DependencyMetadata[] getDependencyMetadataImpl(ExtensionDependencyDeclaration extensionDependency)
	{
		// so this is a cache miss in super
		DependencyMetadata[] result;
		addCachedDependencyMetadataVersionInterval(extensionDependency.id, new VersionInterval(VersionStringUtils.UNBOUNDED, VersionStringUtils.UNBOUNDED));
		if (extensionXMLsParsed)
		{
			result = null; // all available extension packages were already parsed/cached, so if there was a cache miss, the dependency is just not there
		}
		else
		{
			parseExtensionXMLs(); // parses and caches all available dependencies
			result = getDependencyMetadata(extensionDependency); // from cache
		}
		return result;
	}

	/**
	 * Parses all available extension packages and caches the dependency meta-data.
	 */
	protected void parseExtensionXMLs()
	{
		extensionXMLsParsed = true;
		File[] fileList = thinkDir ? file.listFiles() : new File[] { file };
		int count = 0;

		if (fileList != null)
		{
			warnings = new ArrayList<String>();
			for (File f : fileList)
			{
				if (f.exists() && f.isFile() && f.getName().endsWith(EXTENSION_PACKAGE_FILE_EXTENSION))
				{
					count++;
					parseExtensionXML(f);
				}
			}
		}
		if (count == 0)
		{
			warnings.add((thinkDir ? "Cannot find any extension package in directory '" : "The file is not an extension package: '") + file.getAbsolutePath() +
				"'.");
		}
	}

	/**
	 * Parses one extension package and caches dependency meta-data.
	 * @param f the .exp file.
	 */
	protected void parseExtensionXML(File f)
	{
		EXPParser parser = new EXPParser(f);
		DependencyMetadata dependencyMetadata = parser.parseDependencyInfo();
		// cache dependency info about this version of the extension
		if (dependencyMetadata != null)
		{
			boolean added = cacheDependencyMetadataVersion(dependencyMetadata);

			if (added)
			{
				// tell cache that any version of this extension is already cached (because all available packages will be cached)
				addCachedDependencyMetadataVersionInterval(dependencyMetadata.id, new VersionInterval(VersionStringUtils.UNBOUNDED,
					VersionStringUtils.UNBOUNDED));
				associateExtensionVersionWithFile(dependencyMetadata.id, dependencyMetadata.version, f);
			}
			else
			{
				warnings.add("More then one package contains extension ('" + dependencyMetadata.id + "', " + dependencyMetadata.version +
					"). Ignoring package: " + f.getName());
			}
		}
		String[] parserWarnings = parser.getWarnings();
		if (parserWarnings != null)
		{
			warnings.addAll(Arrays.asList(parser.getWarnings()));
		}
	}

	protected void associateExtensionVersionWithFile(String extensionId, String version, File zipFile)
	{
		if (extensionVersionToFile == null) extensionVersionToFile = new HashMap<String, Map<String, File>>();
		Map<String, File> versions = extensionVersionToFile.get(extensionId);
		if (versions == null)
		{
			versions = new HashMap<String, File>();
			extensionVersionToFile.put(extensionId, versions);
		}
		versions.put(version, zipFile);
	}

	public File getEXPFile(String extensionId, String version)
	{
		if (!extensionXMLsParsed) parseExtensionXMLs();
		return extensionVersionToFile != null ? extensionVersionToFile.get(extensionId).get(version) : null;
	}

	/**
	 * If problems were encountered while reading contents of given directory/file, they will be remembered and returned by this method.
	 * @return any problems encountered that might be of interest to the user.
	 */
	public String[] getWarnings()
	{
		return (warnings == null || warnings.size() == 0) ? null : warnings.toArray(new String[warnings.size()]);
	}

	@Override
	public void flushCache()
	{
		extensionXMLsParsed = false;
		extensionVersionToFile = null;
		warnings = null;
		super.flushCache();
	}

}
