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

package com.servoy.j2db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.scripting.JSEvent;
import com.servoy.j2db.scripting.info.EVENTS_AGGREGATION_TYPE;
import com.servoy.j2db.scripting.info.EventType;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Utils;

/**
 * @author lvostinar
 *
 */
public class EventsManager implements IEventsManager, Scriptable
{
	private final ClientState application;
	private final Map<String, List<Pair<String, Function>>> callbacks = new HashMap<String, List<Pair<String, Function>>>();

	public EventsManager(ClientState clientState)
	{
		this.application = clientState;
	}


	@Override
	public void addListener(String eventType, Function callback, String context)
	{
		if (eventType != null && callback != null)
		{
			List<Pair<String, Function>> eventTypeCallbacks = callbacks.get(eventType);
			if (eventTypeCallbacks == null)
			{
				eventTypeCallbacks = new ArrayList<Pair<String, Function>>();
				callbacks.put(eventType, eventTypeCallbacks);
			}
			eventTypeCallbacks.add(new Pair<String, Function>(context, callback));
		}

	}

	@Override
	public void removeListener(String eventType, Function callback, String context)
	{
		if (eventType != null)
		{
			if (callback == null && context == null)
			{
				callbacks.remove(eventType);
			}
			else
			{
				List<Pair<String, Function>> eventTypeCallbacks = callbacks.get(eventType);
				eventTypeCallbacks.removeIf(pair -> (callback == null || pair.getRight() == callback) && (context == null || context.equals(pair.getLeft())));
				if (eventTypeCallbacks.size() == 0)
				{
					callbacks.remove(eventType);
				}
			}
		}
	}


	private List<Function> getListeners(String eventType, String context)
	{
		if (eventType != null)
		{
			List<Pair<String, Function>> eventTypeCallbacks = callbacks.get(eventType);
			if (eventTypeCallbacks != null)
			{
				return eventTypeCallbacks.stream().filter(pair -> context == null || pair.getLeft() == null || context.equals(pair.getLeft()))
					.map(pair -> pair.getRight())
					.collect(Collectors.toList());
			}
		}
		return null;
	}

	@Override
	public boolean hasListeners(String eventType, String context)
	{
		if (eventType != null)
		{
			List<Pair<String, Function>> eventTypeCallbacks = callbacks.get(eventType);
			if (eventTypeCallbacks != null)
			{
				if (context == null)
				{
					return eventTypeCallbacks.size() > 0;
				}
				return eventTypeCallbacks.stream().anyMatch(pair -> context.equals(pair.getLeft()));
			}
		}
		return false;
	}

	@Override
	public Object fireListeners(String eventType, String context, Object[] callbackArguments, EVENTS_AGGREGATION_TYPE returnValueAggregationType)
	{
		List<Function> functions = getListeners(eventType, context);
		if (functions != null)
		{
			Boolean retAsBoolean = null;
			List<Object> retAsList = new ArrayList<Object>();
			for (Function function : functions)
			{
				JSEvent event = new JSEvent();
				event.setType(eventType);
				event.setName(eventType);
				event.setSource(context);
				if (context != null && context.startsWith("forms.")) //$NON-NLS-1$
				{
					event.setFormName(context.substring("forms.".length())); //$NON-NLS-1$
				}
				event.setData(callbackArguments);
				Object[] args = null;
				if (callbackArguments != null && callbackArguments.length > 0)
				{
					args = new Object[callbackArguments.length + 1];
					args[0] = event;
					System.arraycopy(callbackArguments, 0, args, 1, callbackArguments.length);
				}
				else args = new Object[] { event };
				try
				{
					Object retValue = application.getScriptEngine().executeFunction(function, function.getParentScope(), function.getParentScope(),
						args, false, false);
					if (returnValueAggregationType == EVENTS_AGGREGATION_TYPE.RETURN_VALUE_BOOLEAN)
					{
						if (retAsBoolean == null)
						{
							retAsBoolean = Boolean.valueOf(Utils.getAsBoolean(retValue));
						}
						else retAsBoolean = Boolean.valueOf(retAsBoolean.booleanValue() && Utils.getAsBoolean(retValue));
					}
					else
					{
						retAsList.add(retValue);
					}

				}
				catch (Exception e)
				{
					Debug.error(e);
				}
				if (event.isPropagationStopped())
				{
					break;
				}
			}
			if (returnValueAggregationType == EVENTS_AGGREGATION_TYPE.RETURN_VALUE_BOOLEAN)
			{
				return retAsBoolean;
			}
			else
			{
				return retAsList;
			}
		}
		return null;
	}


	@Override
	public String getClassName()
	{
		return null;
	}


	@Override
	public Object get(String name, Scriptable start)
	{
		EventType eventType = EventType.getDefaultEvents().get(name);
		return eventType == null ? application.getFlattenedSolution().getEventType(name) : eventType;
	}


	@Override
	public Object get(int index, Scriptable start)
	{
		return null;
	}


	@Override
	public boolean has(String name, Scriptable start)
	{
		return application.getFlattenedSolution().getEventType(name) != null || EventType.getDefaultEvents().containsKey(name);
	}


	@Override
	public boolean has(int index, Scriptable start)
	{
		return false;
	}


	@Override
	public void put(String name, Scriptable start, Object value)
	{

	}


	@Override
	public void put(int index, Scriptable start, Object value)
	{

	}


	@Override
	public void delete(String name)
	{
	}


	@Override
	public void delete(int index)
	{

	}


	@Override
	public Scriptable getPrototype()
	{
		return null;
	}


	@Override
	public void setPrototype(Scriptable prototype)
	{

	}


	@Override
	public Scriptable getParentScope()
	{
		return null;
	}


	@Override
	public void setParentScope(Scriptable parent)
	{

	}


	@Override
	public Object[] getIds()
	{
		List<String> names = new ArrayList<String>();
		// take all default
		EventType.getDefaultEvents().values().stream().map(eventType -> eventType.getName()).forEach(names::add);
		// take all custom declared in the solution and modules
		application.getFlattenedSolution().getEventTypes().stream().map(eventType -> eventType.getName()).forEach(names::add);
		return names.toArray();
	}


	@Override
	public Object getDefaultValue(Class< ? > hint)
	{
		return null;
	}


	@Override
	public boolean hasInstance(Scriptable instance)
	{
		return false;
	}
}
