/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.j2db.server.ngclient.property.types;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mozilla.javascript.NativeFunction;
import org.mozilla.javascript.Scriptable;
import org.sablo.specification.property.IConvertedPropertyType;
import org.sablo.specification.property.IDataConverterContext;
import org.sablo.specification.property.types.FunctionPropertyType;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.scripting.GlobalScope;
import com.servoy.j2db.scripting.ScriptVariableScope;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.SecuritySupport;
import com.servoy.j2db.util.Settings;

/**
 * @author lvostinar
 *
 */
public class ServoyFunctionPropertyType extends FunctionPropertyType implements IConvertedPropertyType<Object>
{
	public static final ServoyFunctionPropertyType INSTANCE = new ServoyFunctionPropertyType();

	private ServoyFunctionPropertyType()
	{
	}

	@Override
	public Object fromJSON(Object newValue, Object previousValue, IDataConverterContext dataConverterContext)
	{
		if (newValue instanceof JSONObject)
		{
			Map<String, Object> value = new HashMap<String, Object>();
			Iterator<String> it = ((JSONObject)newValue).keys();
			while (it.hasNext())
			{
				String key = it.next();
				try
				{
					value.put(key, ((JSONObject)newValue).get(key));
				}
				catch (JSONException ex)
				{
					Debug.error(ex);
				}
			}
			return value;
		}
		return newValue;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, Object object, DataConversion clientConversion, IDataConverterContext dataConverterContext)
		throws JSONException
	{
		Map<String, Object> map = new HashMap<>();
		try
		{
			if (object instanceof String)
			{
				String script = (String)object;
				if (script.contains(ScriptVariable.SCOPES_DOT_PREFIX) || script.contains(ScriptVariable.GLOBALS_DOT_PREFIX) || !script.contains("."))
				{
					// scope method
					map.put("script", SecuritySupport.encrypt(Settings.getInstance(), script + "()"));
				}
				else
				{
					// form method: formname.formmethod
					String formName = script.substring(0, script.indexOf('.'));
					map.put("script", SecuritySupport.encrypt(Settings.getInstance(), "forms." + script + "()"));
					map.put("formname", SecuritySupport.encrypt(Settings.getInstance(), formName));
				}
			}
			else if (object instanceof NativeFunction)
			{
				NativeFunction function = (NativeFunction)object;
				String functionName = function.getFunctionName();
				Scriptable parentScope = function.getParentScope();
				while (parentScope != null && !(parentScope instanceof ScriptVariableScope))
				{
					parentScope = parentScope.getParentScope();
				}
				if (parentScope instanceof FormScope && ((FormScope)parentScope).getFormController() != null)
				{
					String formName = ((FormScope)parentScope).getFormController().getName();
					map.put("script", SecuritySupport.encrypt(Settings.getInstance(), "forms." + formName + "." + functionName + "()"));
					map.put("formname", SecuritySupport.encrypt(Settings.getInstance(), formName));
				}
				else if (parentScope instanceof GlobalScope)
				{
					map.put("script",
						SecuritySupport.encrypt(Settings.getInstance(), "scopes." + ((GlobalScope)parentScope).getScopeName() + "." + functionName + "()"));
				}

			}
			else if (object instanceof Map)
			{
				map = (Map<String, Object>)object;
			}
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
		return JSONUtils.toBrowserJSONFullValue(writer, key, map, null, clientConversion, null);
	}
}
