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

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.scripting.solutionmodel.JSMedia;
import com.servoy.j2db.util.Debug;

/**
 * @author jcompagner
 * @since 2019.3
 */
@SuppressWarnings("nls")
public class LessCompiler
{
	private static Invocable invocable;

	public static String compileSolutionLessFile(Media media, FlattenedSolution fs)
	{
		String cssAsString = compileLessWithNashorn(new String(media.getMediaData(), Charset.forName("UTF-8")),
			(name) -> fs.getMedia(name) != null ? new JSMedia(fs.getMedia(name), fs, false) : null, media.getName());
		cssAsString = cssAsString.replaceAll("##last-changed-timestamp##",
			Long.toHexString(media.getLastModifiedTime() != -1 ? media.getLastModifiedTime() : fs.getSolution().getLastModifiedTime()));
		return cssAsString;
	}

	public static String compileLessWithNashorn(InputStream is)
	{
		try
		{
			return compileLessWithNashorn(getText(is), null, null);
		}
		catch (IOException e)
		{
			Debug.log(e);
		}
		return null;
	}

	public static String compileLessWithNashorn(String text, IMediaProvider mediaProvider, String name)
	{

		try
		{
			Invocable engine = getInvocable();
			synchronized (engine)
			{
				Object result = engine.invokeFunction("convert", text, new LessFileManager(mediaProvider, name));
				return result.toString();
			}
		}
		catch (ScriptException e)
		{
			Debug.log(e);
		}
		catch (NoSuchMethodException e)
		{
			Debug.log(e);
		}
		catch (Exception e)
		{
			Debug.log(e);
		}
		return "";
	}

	private static Invocable getInvocable() throws NoSuchMethodException, ScriptException
	{
		if (invocable == null)
		{
			//we have to pass in null as classloader if we want to acess the java 8 nashorn
			ScriptEngine engine = new ScriptEngineManager(null).getEngineByName("nashorn");
			if (engine != null)
			{
				invocable = (Invocable)engine;
				invocable.invokeFunction("load", LessCompiler.class.getResource("js/less-2.5.1.js"));
				invocable.invokeFunction("load", LessCompiler.class.getResource("js/less-env-2.5.1.js"));
				invocable.invokeFunction("load", LessCompiler.class.getResource("js/lessrunner.js"));
			}
		}
		return invocable;
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
