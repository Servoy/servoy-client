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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Settings;

/**
 * @author jcompagner
 * @since 2019.3
 */
@SuppressWarnings("nls")
public class LessCompiler
{
	private static final Logger LOG = LoggerFactory.getLogger("com.servoy.less.Compiler");
	private static ScriptEngine engine;
	private static CompiledScript script;
	private static Bindings bindings;

	public static String compileSolutionLessFile(Media media, FlattenedSolution fs)
	{
		String cssAsString = compileLess(new String(media.getMediaData(), Charset.forName("UTF-8")), fs, media.getName());
		cssAsString = cssAsString.replaceAll("##last-changed-timestamp##",
			Long.toHexString(media.getLastModifiedTime() != -1 ? media.getLastModifiedTime() : fs.getSolution().getLastModifiedTime()));
		return cssAsString;
	}

	public static String compileLess(InputStream is)
	{
		try
		{
			return compileLess(getText(is), null, null);
		}
		catch (IOException e)
		{
			Debug.log(e);
		}
		return null;
	}

	public static String compileLess(String text, FlattenedSolution fs, String name)
	{
		long time = System.currentTimeMillis();
		String lesscompiler = Settings.getInstance().getProperty("servoy.less.compiler", "jlessc");
		try
		{
			switch (lesscompiler)
			{
				case "lessjs" :
					return LessJSCompiler.compileLessWithNashorn(text, fs, name);
				default :
					return JLessCompiler.compileLess(text, fs, name);
			}
		}
		finally
		{
			LOG.info("Less '" + name + "' compiled in " + (System.currentTimeMillis() - time) + "ms with compiler " + lesscompiler +
				", 2 lesscompilers available: lessjs, jlessc (default)");
		}

	}

	private static String getText(InputStream is) throws IOException
	{
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));
		StringBuilder result = new StringBuilder();
		String inputLine;
		while ((inputLine = bufferedReader.readLine()) != null)
			result.append(inputLine);
		bufferedReader.close();
		return result.toString();
	}
}
