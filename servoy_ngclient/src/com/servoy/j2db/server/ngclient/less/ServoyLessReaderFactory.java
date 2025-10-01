/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;

import com.inet.lib.less.ReaderFactory;
import com.servoy.j2db.FlattenedSolution;

/**
 * @author jcompagner
 *
 */
public final class ServoyLessReaderFactory extends ReaderFactory
{
	private final LessFileMananger fileMananger;

	/**
	 * @param fs
	 */
	public ServoyLessReaderFactory(FlattenedSolution fs, String name)
	{
		this.fileMananger = fs != null ? new FlattenedSolutionLessFileManager(fs, name) : new LessFileMananger();
		;
	}

	@Override
	public Reader create(URL url) throws IOException
	{
		String path = url.getFile();
		path = path.startsWith("/") ? path.substring(1) : path;
		Object less = fileMananger.readLess(path, "UTF-8");
		return less == null ? null : new StringReader(less.toString());
	}

	@Override
	public InputStream openStream(URL url) throws IOException
	{
		String path = url.getFile();
		path = path.startsWith("/") ? path.substring(1) : path;
		Object bytes = fileMananger.readLess(path, null);
		if (bytes instanceof byte[])
		{
			return new ByteArrayInputStream((byte[])bytes);
		}
		throw new FileNotFoundException(url.toString());
	}
}