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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;

import com.inet.lib.less.Less;
import com.inet.lib.less.LessException;
import com.inet.lib.less.ReaderFactory;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.util.Debug;

/**
 * @author jcompgner
 * @since 2019.03
 *
 */
public class JLessCompiler
{
	public static String compileLess(String text, FlattenedSolution fs, String name)
	{
		final LessFileMananger fileMananger = fs != null ? new FlattenedSolutionLessFileManager(fs, name) : new LessFileMananger();
		String css = null;
		try
		{
			css = Less.compile(new URL("http://localhost"), text, false, new ReaderFactory()
			{
				@Override
				public Reader create(URL url) throws IOException
				{
					String path = url.getFile();
					path = path.startsWith("/") ? path.substring(1) : path;
					Object less = fileMananger.readLess(path, "UTF-8");
					return less == null ? null : new StringReader(less.toString());
				}
			});
		}
		catch (LessException e)
		{
			Debug.error(e);
			css = e.getOriginalMessage();
		}
		catch (MalformedURLException e)
		{
			Debug.error(e);
		}
		return css;
	}
}
