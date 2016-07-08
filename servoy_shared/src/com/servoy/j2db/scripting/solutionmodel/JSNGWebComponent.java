/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

package com.servoy.j2db.scripting.solutionmodel;

import java.util.ArrayList;
import java.util.Arrays;

import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.Context;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectSpecification;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IRhinoDesignConverter;
import com.servoy.j2db.util.ServoyJSONObject;

/**
 * @author gboros
 *
 */
public class JSNGWebComponent extends JSWebComponent
{

	protected JSNGWebComponent(IJSParent< ? > parent, WebComponent baseComponent, IApplication application, boolean isNew)
	{
		super(parent, baseComponent, application, isNew);
	}

	@Override
	public void setJSONProperty(String propertyName, Object value)
	{
		try
		{
			String name = propertyName;
			WebComponent webComponent = getBaseComponent(true);
			WebObjectSpecification spec = WebComponentSpecProvider.getInstance().getWebComponentSpecification(webComponent.getTypeName());
			PropertyDescription pd = spec.getProperty(name);
			if (pd == null && spec.getHandler(name) != null) pd = spec.getHandler(name).getAsPropertyDescription();
			if (pd == null)
			{
				// now try it if it is a more legacy name where the id is stripped from
				pd = spec.getProperty(name + "ID");
				if (pd != null) name = name + "ID";
			}
			Object convertedValue = fromRhinoToDesignValue(value, pd, application, this);
			webComponent.setProperty(name, convertedValue);
		}
		catch (JSONException e)
		{
			Debug.error(e);
		}
	}

	@Override
	public void resetJSONProperty(String propertyName)
	{
		try
		{
			WebComponent webComponent = getBaseComponent(true);
			WebObjectSpecification spec = WebComponentSpecProvider.getInstance().getWebComponentSpecification(webComponent.getTypeName());
			if (spec.getProperty(propertyName) != null) webComponent.clearProperty(propertyName);
		}
		catch (JSONException e)
		{
			Debug.error(e);
		}
	}

	@Override
	public Object getJSONProperty(String propertyName)
	{
		WebComponent webComponent = getBaseComponent(false);
		JSONObject json = webComponent.getFlattenedJson();
		if (json == null) return Context.getUndefinedValue();

		//TODO for now this works because it is stored as a json;
		//this needs to be changed to getProperty when SVY-9365 is done
		//then we will also need special conversions for rhino
		WebObjectSpecification spec = WebComponentSpecProvider.getInstance().getWebComponentSpecification(webComponent.getTypeName());
		if (spec != null)
		{
			String name = propertyName;
			PropertyDescription pd = spec.getProperty(name);
			if (pd == null && spec.getHandler(name) != null) pd = spec.getHandler(name).getAsPropertyDescription();
			if (pd == null)
			{
				pd = spec.getProperty(name + "ID");
				if (pd != null) name = name + "ID";
			}
			return fromDesignToRhinoValue(json.opt(name), pd, application, this);
			// JSONArray and JSONObject are automatically wrapped when going to Rhino through ServoyWrapFactory, so no need to treat them specially here
		}
		Object value = json.opt(propertyName);
		return value == null ? Context.getUndefinedValue() : ServoyJSONObject.jsonNullToNull(value);
	}

	@Override
	public void setHandler(String handlerName, JSMethod value)
	{
		WebComponent webComponent = getBaseComponent(false);
		WebObjectSpecification spec = WebComponentSpecProvider.getInstance().getWebComponentSpecification(webComponent.getTypeName());
		if (spec != null)
		{
			String name = handlerName;
			if (spec.getHandler(name) == null)
			{
				name = name + "MethodID";
			}
			if (spec.getHandler(name) != null)
			{
				setJSONProperty(name, value);
				getBaseComponent(true).putMethodParameters(name, new ArrayList(),
					value instanceof JSMethodWithArguments ? Arrays.asList(((JSMethodWithArguments)value).getArguments()) : null);
			}
			else Debug.log("Error: component " + webComponent.getTypeName() + " does not declare a handler named " + handlerName + ".");
		}
	}

	@Override
	public void resetHandler(String handlerName)
	{
		WebComponent webComponent = getBaseComponent(false);
		WebObjectSpecification spec = WebComponentSpecProvider.getInstance().getWebComponentSpecification(webComponent.getTypeName());
		if (spec != null)
		{
			String name = handlerName;
			if (spec.getHandler(name) == null)
			{
				name = name + "MethodID";
			}
			if (spec.getHandler(name) != null)
			{
				webComponent.clearProperty(name);
				getBaseComponent(true).putMethodParameters(name, null, null);
			}
			else Debug.log("Error: component " + webComponent.getTypeName() + " does not declare a handler named " + handlerName + ".");
		}
	}

	@Override
	public JSMethod getHandler(String handlerName)
	{
		Object jsonProperty = getJSONProperty(handlerName);
		if (jsonProperty == null)
		{
			jsonProperty = getJSONProperty(handlerName + "MethodID");
		}
		if (jsonProperty instanceof JSMethod) return (JSMethod)jsonProperty;
		else return null;
	}

	public static Object fromRhinoToDesignValue(Object value, PropertyDescription pd, IApplication application, JSWebComponent webComponent)
	{
		Object result = null;
		if (pd != null && pd.getType() instanceof IRhinoDesignConverter)
		{
			result = ((IRhinoDesignConverter)pd.getType()).fromRhinoToDesignValue(value, pd, application, webComponent);
		}
		else
		{
			result = JSWebComponent.defaultRhinoToDesignValue(value, application);
		}
		return result;
	}

	public static Object fromDesignToRhinoValue(Object value, PropertyDescription pd, IApplication application, JSWebComponent webComponent)
	{
		Object result = value;
		if (pd != null && pd.getType() instanceof IRhinoDesignConverter)
		{
			result = ((IRhinoDesignConverter)pd.getType()).fromDesignToRhinoValue(value, pd, application, webComponent);
		}
		return result == null ? Context.getUndefinedValue() : ServoyJSONObject.jsonNullToNull(result);
	}

}
