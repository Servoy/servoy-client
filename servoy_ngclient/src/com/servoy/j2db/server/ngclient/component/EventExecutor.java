/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.j2db.server.ngclient.component;

import java.awt.Point;
import java.sql.Timestamp;

import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.sablo.WebComponent;

import com.servoy.j2db.IFormController;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.scripting.ElementScope;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.scripting.GlobalScope;
import com.servoy.j2db.scripting.JSEvent;
import com.servoy.j2db.util.Debug;


/**
 * @author lvostinar
 *
 */
public class EventExecutor
{
	private final IFormController formController;

	public EventExecutor(IFormController formController)
	{
		this.formController = formController;
	}

	public Object executeEvent(WebComponent component, String eventType, int eventId, Object[] args)
	{
		Scriptable scope = null;
		Function f = null;

		if (eventId > 0)
		{
			FormScope formScope = formController.getFormScope();
			String name = formScope.getFunctionName(new Integer(eventId));
			if (name != null)
			{
				f = formScope.getFunctionByName(name);
				if (f != null && f != Scriptable.NOT_FOUND)
				{
					scope = formScope;
				}
			}

			if (scope == null)
			{
				ScriptMethod scriptMethod = formController.getApplication().getFlattenedSolution().getScriptMethod(eventId);
				if (scriptMethod != null)
				{
					scope = formController.getApplication().getScriptEngine().getScopesScope().getGlobalScope(scriptMethod.getScopeName());
				}
				if (scope != null)
				{
					name = ((GlobalScope)scope).getFunctionName(new Integer(eventId));
					f = ((GlobalScope)scope).getFunctionByName(name);
				}
			}
		}

		if (args != null)
		{
			for (int i = 0; i < args.length; i++)
			{
				if (args[i] instanceof JSONObject && "event".equals(((JSONObject)args[i]).optString("type")))
				{
					JSONObject json = (JSONObject)args[i];
					JSEvent event = new JSEvent();
					event.setType(eventType);
					event.setFormName(formController.getName());
					event.setElementName(component.getName());
					FormScope formScope = formController.getFormScope();
					if (formScope != null)
					{
						ElementScope elementsScope = (ElementScope)formScope.get("elements", null);
						if (elementsScope != null)
						{
							Object scriptableElement = elementsScope.get(component.getName(), null);
							if (scriptableElement != null && scriptableElement != Scriptable.NOT_FOUND)
							{
								event.setSource(scriptableElement);
							}
						}
					}
					try
					{
						event.setTimestamp(new Timestamp(json.getLong("timestamp")));
						if (json.has("x")) event.setLocation(new Point(json.getInt("x"), json.getInt("y")));
						if (json.has("modifiers")) event.setModifiers(json.getInt("modifiers"));
					}
					catch (Exception ex)
					{
						Debug.error(ex);
					}
					args[i] = event;
				}
			}
		}

		try
		{
			return formController.getApplication().getScriptEngine().executeFunction(f, scope, scope, args, false, true);
		}
		catch (Exception ex)
		{
			formController.getApplication().reportJSError(ex.getMessage(), ex);
			return null;
		}
	}

	public static JSONObject createEvent(String type)
	{
		JSONObject event = new JSONObject();
		try
		{
			event.put("type", "event");
			event.put("eventName", type);
			event.put("timestamp", System.currentTimeMillis());
			event.put("modifiers", 0);
			event.put("x", 0);
			event.put("y", 0);
		}
		catch (JSONException ex)
		{
			Debug.error(ex);
		}

		return event;
	}
}
