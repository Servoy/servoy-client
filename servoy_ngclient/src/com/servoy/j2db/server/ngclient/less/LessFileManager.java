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

package com.servoy.j2db.server.ngclient.less;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.scripting.solutionmodel.JSMedia;
import com.servoy.j2db.server.ngclient.less.resources.ThemeResourceLoader;
import com.servoy.j2db.solutionmodel.ISMMedia;
import com.servoy.j2db.util.Debug;

/**
 * @author jcompagner
 * @since 2019.03
 */
@SuppressWarnings("nls")
public class LessFileManager
{
	private final IMediaProvider mediaProvider;
	private final String startFolder;
	private final String lessFile;
	private final List<Media> imports = new ArrayList<>();

	public LessFileManager(IMediaProvider mediaProvider, String lessFile)
	{
		this.mediaProvider = mediaProvider;
		this.lessFile = lessFile;
		int index = lessFile.lastIndexOf('/') + 1;
		this.startFolder = index > 0 ? lessFile.substring(0, index) : "";

		ISMMedia parent = mediaProvider.getMedia(lessFile);
		if (parent instanceof JSMedia)
		{
			((JSMedia)parent).getMedia().setRuntimeProperty(Media.REFERENCES, imports);
		}

	}

	public String load(String path, @SuppressWarnings("unused") String directory)
	{
		if (mediaProvider != null)
		{
			String filename = path;
			// test for arguments (like last modified)
			int questionMark = filename.lastIndexOf('?');
			if (questionMark > 0)
			{
				filename = filename.substring(0, questionMark);
			}
			ISMMedia media = null;
			try
			{
				URI uri = new URI(startFolder + filename);
				media = mediaProvider.getMedia(uri.normalize().toString());
			}
			catch (URISyntaxException e1)
			{
				media = mediaProvider.getMedia(startFolder + filename);
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
			if (media == null && path.startsWith(ThemeResourceLoader.PROPERTIES_LESS))
			{
				int index = path.indexOf("version=");
				if (index == -1)
				{
					return ThemeResourceLoader.getLatestThemeProperties();
				}
				else
				{
					String version = path.substring(index + "version=".length());
					return ThemeResourceLoader.getThemeProperties(version);
				}
			}
			if (media != null)
			{
				if (media instanceof JSMedia) imports.add(((JSMedia)media).getMedia());
				try
				{
					return new String(media.getBytes(), "UTF-8");
				}
				catch (UnsupportedEncodingException e)
				{
					return new String(media.getBytes());
				}
			}
		}
		Debug.error("Couldn't resolve the media for " + startFolder + path + " when parsing the @import statement of the less file: " + lessFile);
		return "";
	}
}
