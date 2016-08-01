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
import com.servoy.j2db.util.IFormComponentRhinoConverter;
import com.servoy.j2db.util.IFormComponentType;
import com.servoy.j2db.util.IRhinoDesignConverter;
import com.servoy.j2db.util.Pair;
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
			WebComponent webComponent = getBaseComponent(true);
			WebObjectSpecification spec = WebComponentSpecProvider.getInstance().getWebComponentSpecification(webComponent.getTypeName());

			Pair<PropertyDescription, String> propAndName = getPropertyDescriptionAndName(propertyName, spec);

			Object convertedValue = fromRhinoToDesignValue(value, propAndName.getLeft(), application, this, propertyName);
			webComponent.setProperty(propAndName.getRight(), convertedValue);
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
			Pair<PropertyDescription, String> propAndName = getPropertyDescriptionAndName(propertyName, spec);
			PropertyDescription pd = propAndName.getLeft();
			if (pd != null && pd.getType() instanceof IFormComponentType)
			{
				IFormComponentRhinoConverter converter = ((IFormComponentType)pd.getType()).getFormComponentRhinoConverter(pd.getName(),
					webComponent.getProperty(pd.getName()), application, this);
				// undefined means remove the property
				Object convertedValue = fromRhinoToDesignValue(Context.getUndefinedValue(), propAndName.getLeft(), application, this, propertyName);
				webComponent.setProperty(propAndName.getRight(), convertedValue);

			}
			else if (pd != null) webComponent.clearProperty(propertyName);
		}
		catch (JSONException e)
		{
			Debug.error(e);
		}
	}

	private Pair<PropertyDescription, String> getPropertyDescriptionAndName(String propertyName, WebObjectSpecification spec)
	{
		if (spec == null) return new Pair<PropertyDescription, String>(null, propertyName);
		String name = propertyName;
		PropertyDescription pd = spec.getProperty(name);
		if (pd == null)
		{
			int i = name.indexOf('.');
			if (i > 0)
			{
				String firstPart = name.substring(0, i);
				PropertyDescription property = spec.getProperty(firstPart);
				if (property != null && property.getType() instanceof IFormComponentType)
				{
					pd = property;
					name = firstPart;
				}
			}
		}
		if (pd == null && spec.getHandler(name) != null) pd = spec.getHandler(name).getAsPropertyDescription();
		if (pd == null)
		{
			// now try it if it is a more legacy name where the id is stripped from
			pd = spec.getProperty(name + "ID");
			if (pd != null) name = name + "ID";
		}
		return new Pair<PropertyDescription, String>(pd, name);
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
			Pair<PropertyDescription, String> propAndName = getPropertyDescriptionAndName(propertyName, spec);
			return fromDesignToRhinoValue(json.opt(propAndName.getRight()), propAndName.getLeft(), application, this, propertyName);
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
			else
			{
				int i = name.indexOf('.');
				if (i > 0)
				{
					String firstPart = name.substring(0, i);
					PropertyDescription property = spec.getProperty(firstPart);
					if (property != null && property.getType() instanceof IFormComponentType)
					{
						// undefined means remove the property
						Object convertedValue = fromRhinoToDesignValue(value, property, application, this, handlerName);
						webComponent.setProperty(firstPart, convertedValue);
						// TODO store the method parameters/arguments..
						return;
					}
				}
				Debug.log("Error: component " + webComponent.getTypeName() + " does not declare a handler named " + handlerName + ".");
			}
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
			else
			{
				int i = name.indexOf('.');
				if (i > 0)
				{
					String firstPart = name.substring(0, i);
					PropertyDescription property = spec.getProperty(firstPart);
					if (property != null && property.getType() instanceof IFormComponentType)
					{
						// undefined means remove the property
						Object convertedValue = fromRhinoToDesignValue(Context.getUndefinedValue(), property, application, this, handlerName);
						webComponent.setProperty(firstPart, convertedValue);
						return;
					}
				}
				Debug.log("Error: component " + webComponent.getTypeName() + " does not declare a handler named " + handlerName + ".");
			}
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

	public static Object fromRhinoToDesignValue(Object value, PropertyDescription pd, IApplication application, JSWebComponent webComponent,
		String propertyName)
	{
		Object result = null;
		if (pd != null && pd.getType() instanceof IFormComponentType)
		{
			String firstPart = propertyName;
			int i = firstPart.indexOf('.');
			if (i > 0)
			{
				firstPart = firstPart.substring(0, i);
			}
			IFormComponentRhinoConverter converter = ((IFormComponentType)pd.getType()).getFormComponentRhinoConverter(firstPart,
				webComponent.getBaseComponent(true).getProperty(firstPart), application, webComponent);
			result = converter.setRhinoToDesignValue(firstPart == propertyName ? "" : propertyName.substring(firstPart.length() + 1), value);
		}
		else if (pd != null && pd.getType() instanceof IRhinoDesignConverter)
		{
			result = ((IRhinoDesignConverter)pd.getType()).fromRhinoToDesignValue(value, pd, application, webComponent);
		}
		else
		{
			result = JSWebComponent.defaultRhinoToDesignValue(value, application);
		}
		return result;
	}

	public static Object fromDesignToRhinoValue(Object value, PropertyDescription pd, IApplication application, JSWebComponent webComponent,
		String propertyName)
	{
		Object result = value;
		if (pd != null && pd.getType() instanceof IFormComponentType)
		{
			String firstPart = propertyName;
			int i = firstPart.indexOf('.');
			if (i > 0)
			{
				firstPart = firstPart.substring(0, i);
			}
			IFormComponentRhinoConverter converter = ((IFormComponentType)pd.getType()).getFormComponentRhinoConverter(firstPart, value, application,
				webComponent);
			result = converter.getDesignToRhinoValue(firstPart == propertyName ? "" : propertyName.substring(firstPart.length() + 1));
		}
		else if (pd != null && pd.getType() instanceof IRhinoDesignConverter)
		{
			result = ((IRhinoDesignConverter)pd.getType()).fromDesignToRhinoValue(value, pd, application, webComponent);
		}
		return result == null ? Context.getUndefinedValue() : ServoyJSONObject.jsonNullToNull(result);
	}

}
