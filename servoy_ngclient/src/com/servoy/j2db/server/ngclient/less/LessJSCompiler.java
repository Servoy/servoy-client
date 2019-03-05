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
import java.util.HashMap;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.util.Debug;

/**
 * @author jcompagner
 * @since 2019.3
 */
@SuppressWarnings("nls")
public class LessJSCompiler
{
	private static ScriptEngine engine;
	private static CompiledScript script;
	private static Bindings bindings;

	public static String compileLessWithNashorn(String text, FlattenedSolution fs, String name)
	{

		try
		{
			createEngine();
			synchronized (engine)
			{
				bindings.put("lessStr", text);
				bindings.put("lessc4j", fs != null ? new FlattenedSolutionLessFileManager(fs, name) : new LessFileMananger());
				HashMap<String, String> _result = new HashMap<>();
				bindings.put("_result", _result);
				script.eval(bindings);
				String result = _result.get("css");
				if (result == null)
				{
					result = _result.get("err");
				}
				return result;
			}
		}
		catch (ScriptException e)
		{
			Debug.log(e);
		}

		catch (Exception e)
		{
			Debug.log(e);
		}
		return "";
	}

	private static synchronized ScriptEngine createEngine() throws IOException, ScriptException
	{
		if (engine == null)
		{
			//we have to pass in null as classloader if we want to acess the java 8 nashorn
			engine = new ScriptEngineManager(null).getEngineByName("nashorn");
			if (engine != null)
			{
				LessFileMananger jsReader = new LessFileMananger();
				bindings = engine.createBindings();
				bindings.put("lessc4j", jsReader);
				engine.eval(jsReader.readJs("/lessc4j/entry-point.js"), bindings);
				script = ((Compilable)engine).compile(
					"var options = {rewriteUrls:'all',ieCompat:false}; less.render(lessStr,options).then(function (output) {_result.put('css', output.css);},function (err) {_result.put('err', err);})");
			}
		}
		return engine;
	}
}
