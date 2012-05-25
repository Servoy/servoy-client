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
import java.util.HashMap;
import java.util.Map;

import com.servoy.extension.parser.EXPParser;
import com.servoy.extension.parser.IEXPParserPool;

/**
 * Helps to reuse parsed content instead of re-parsing each time.
 * @author acostescu
 */
public class EXPParserPool implements IEXPParserPool
{

	protected Map<File, EXPParser> expParsers = new HashMap<File, EXPParser>();

	public EXPParser getOrCreateParser(File f)
	{
		EXPParser parser = expParsers.get(f);
		if (parser == null)
		{
			if (f != null)
			{
				parser = new EXPParser(f);
				expParsers.put(f, parser);
			}
		}
		return parser;
	}

	public void flushCache()
	{
		expParsers.clear();
	}

}
