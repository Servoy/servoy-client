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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.servoy.extension.parser.EXPParser;
import com.servoy.extension.parser.IEXPParserPool;

/**
 * This class provides extension info & data taken from all extension packages within an OS directory or from a single file.
 * 
 * @author acostescu
 */
@SuppressWarnings("nls")
public class FileBasedExtensionProvider extends CachingExtensionProvider implements IFileBasedExtensionProvider
{

	public static final String EXTENSION_PACKAGE_FILE_EXTENSION = ".exp";

	protected final File file;
	protected boolean thinkDir;
	protected IEXPParserPool parserSource;

	protected boolean extensionXMLsParsed = false;
	protected Map<String, Map<String, File>> extensionVersionToFile; // <extensionid, <version, File>>
	protected MessageKeeper messages = new MessageKeeper();


	/**
	 * Creates a new file based extension provider. It can use a directory of .exp files or a single .exp file.
	 * @param file the file or folder.
	 * @param thinkDir if it should be considered a directory; false for file.
	 */
	public FileBasedExtensionProvider(File file, boolean thinkDir, IEXPParserPool parserSource)
	{
		if (!file.exists() || !file.canRead() || (thinkDir && !file.isDirectory()) || (!thinkDir && !file.isFile()))
		{
			throw new IllegalArgumentException("'" + file + "' is not a valid/accessible directory/file.");
		}
		this.file = file;
		this.thinkDir = thinkDir;
		this.parserSource = parserSource;
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

	public DependencyMetadata[] getAllAvailableExtensions()
	{
		if (!extensionXMLsParsed)
		{
			parseExtensionXMLs(); // fill up cache
		}
		List<DependencyMetadata> allAvailable = new ArrayList<DependencyMetadata>();

		Iterator<List<DependencyMetadata>> it = cachedDependencyMetadata.values().iterator();
		while (it.hasNext())
		{
			allAvailable.addAll(it.next());
		}
		return allAvailable.toArray(new DependencyMetadata[allAvailable.size()]);
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
			for (File f : fileList)
			{
				if (f.exists() && f.isFile() && f.getName().endsWith(EXTENSION_PACKAGE_FILE_EXTENSION))
				{
					count++;
					parseExtensionXML(f);
				}
			}
		}
		if (count == 0 && !thinkDir)
		{
			messages.addWarning("The file is not an extension package: '" + file.getAbsolutePath() + "'.");
		}
	}

	/**
	 * Parses one extension package and caches dependency meta-data.
	 * @param f the .exp file.
	 */
	protected void parseExtensionXML(File f)
	{
		EXPParser parser = ((parserSource == null) ? new EXPParser(f) : parserSource.getOrCreateParser(f));
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
				messages.addWarning("More then one package contains extension ('" + dependencyMetadata.id + "', " + dependencyMetadata.version +
					"). Ignoring package: " + f.getName() + ".");
			}
		}
		messages.addAll(parser.getMessages());
		parser.clearMessages();
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

	public File getEXPFile(String extensionId, String version, IProgress progressMonitor)
	{
		if (!extensionXMLsParsed) parseExtensionXMLs();
		File f = null;
		if (extensionVersionToFile != null)
		{
			Map<String, File> tmp = extensionVersionToFile.get(extensionId);
			if (tmp != null)
			{
				f = tmp.get(version);
			}
		}

		return f;
	}

	/**
	 * If problems were encountered while reading contents of given directory/file, they will be remembered and returned by this method.
	 * @return any problems encountered that might be of interest to the user.
	 */
	public Message[] getMessages()
	{
		return messages.getMessages();
	}

	public void clearMessages()
	{
		messages.clearMessages();
	}

	@Override
	public void flushCache()
	{
		extensionXMLsParsed = false;
		extensionVersionToFile = null;
		clearMessages();
		super.flushCache();
	}

	public void dispose()
	{
		// not much to do here as we don't keep resources allocated
		flushCache();
	}

}
