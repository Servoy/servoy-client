/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2019 Servoy BV

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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.server.ngclient.startup.resourceprovider.resources.ThemeResourceLoader;
import com.servoy.j2db.util.Debug;

/**
 * @author jcompagner
 * @since 2019.03
 */
@SuppressWarnings("nls")
public class LessFileManager
{
	private final FlattenedSolution fs;
	private final String startFolder;
	private final String lessFile;
	private final List<Media> imports = new ArrayList<>();

	public LessFileManager(FlattenedSolution fs, String lessFile)
	{
		this.fs = fs;
		this.lessFile = lessFile;
		int index = lessFile.lastIndexOf('/') + 1;
		this.startFolder = index > 0 ? lessFile.substring(0, index) : "";

		Media parent = fs.getMedia(lessFile);
		if (parent != null)
		{
			parent.setRuntimeProperty(Media.REFERENCES, imports);
		}

	}

	public String load(String path, @SuppressWarnings("unused") String directory)
	{
		if (fs != null)
		{
			String filename = path;
			// test for arguments (like last modified)
			int questionMark = filename.lastIndexOf('?');
			if (questionMark > 0)
			{
				filename = filename.substring(0, questionMark);
			}
			Media media = null;
			try
			{
				URI uri = new URI(startFolder + filename);
				media = fs.getMedia(uri.normalize().toString());
			}
			catch (URISyntaxException e1)
			{
				media = fs.getMedia(startFolder + filename);
			}
			if (media == null && path.startsWith(ThemeResourceLoader.THEME_LESS))
			{
				int index = path.indexOf("version=");
				if (index == -1)
				{
					return ThemeResourceLoader.getLatestTheme();
				}
				else
				{
					String version = path.substring(index + "version=".length());
					return ThemeResourceLoader.getTheme(version);
				}
			}
			if (media != null)
			{
				imports.add(media);
				try
				{
					return new String(media.getMediaData(), "UTF-8");
				}
				catch (UnsupportedEncodingException e)
				{
					return new String(media.getMediaData());
				}
			}
		}
		Debug.error("Couldn't resolve the media for " + startFolder + path + " when parsing the @import statement of the less file: " + lessFile);
		return "";
	}
}
