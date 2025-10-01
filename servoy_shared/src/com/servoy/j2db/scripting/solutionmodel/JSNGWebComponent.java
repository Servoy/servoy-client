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

import org.json.JSONArray;
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
import com.servoy.j2db.util.ServoyJSONArray;
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
			WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(webComponent.getTypeName());

			Pair<PropertyDescription, String> propAndName = getPropertyDescriptionAndName(propertyName, spec);
			if (propAndName.getLeft() == null)
			{
				Debug.warn("Property '" + propertyName + "' not found in spec file of: " + webComponent.getTypeName() +
					". It was set using JSWebComponent.setJSONProperty API.");
			}
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
			WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(webComponent.getTypeName());
			Pair<PropertyDescription, String> propAndName = getPropertyDescriptionAndName(propertyName, spec);
			PropertyDescription pd = propAndName.getLeft();
			if (pd != null && pd.getType() instanceof IFormComponentType)
			{
				// undefined means remove the property
				Object convertedValue = fromRhinoToDesignValue(Context.getUndefinedValue(), propAndName.getLeft(), application, this, propertyName);
				webComponent.setProperty(propAndName.getRight(), convertedValue);

			}
			else if (pd != null) webComponent.clearProperty(propAndName.getRight());
		}
		catch (JSONException e)
		{
			Debug.error(e);
		}
	}

	@Override
	public boolean isJSONPropertySet(String propertyName)
	{
		WebComponent webComponent = getBaseComponent(false);
		JSONObject json = webComponent.getFlattenedJson();
		return json != null && json.has(propertyName);
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
			// some new (ng) components wrongly continued the "ID" suffix for props. (or handlers "MethodID") - for example "dataproviderID" or "onActionMethodID"
			// but we do hide the "ID" part both in developer and in online docs; so ppl. using solution model will probably try to use
			// the version without the "ID" suffix more and more - and we need to handle that correctly
			pd = spec.getProperty(name + "ID");
			if (pd == null && spec.getHandler(name + "MethodID") != null)
			{
				name = name + "MethodID";
				pd = spec.getHandler(name).getAsPropertyDescription();
			}
			else if (pd != null) name = name + "ID";
		}
		return new Pair<PropertyDescription, String>(pd, name);
	}

	@Override
	public Object getJSONProperty(String propertyName)
	{
		WebComponent webComponent = getBaseComponent(false);
		JSONObject json = webComponent.getFlattenedJson();
		if (json == null) return Context.getUndefinedValue();

		Object value;
		WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(webComponent.getTypeName());
		if (spec != null)
		{
			Pair<PropertyDescription, String> propAndName = getPropertyDescriptionAndName(propertyName, spec);
			if (!json.has(propAndName.getRight()) && propAndName.getLeft() != null && propAndName.getLeft().hasDefault())
			{
				value = propAndName.getLeft().getDefaultValue();
			}
			else
			{
				value = json.opt(propAndName.getRight());
			}
			value = fromDesignToRhinoValue(value, propAndName.getLeft(), application, this, propertyName);
			// JSONArray and JSONObject are automatically wrapped when going to Rhino through ServoyWrapFactory, so no need to treat them specially here
		}
		else
		{
			value = json.opt(propertyName);
		}
		// need to convert to plain because WrapFactory doesnt do this on purpose
		// and at deployment this could be a ServoyJSONObject, but in developer it is a JSONObject
		// so we need to make sure we always return a JSONObject.
		if (value instanceof ServoyJSONObject)
		{
			value = new JSONObject((ServoyJSONObject)value, ((ServoyJSONObject)value).keySet().toArray(new String[0]));
		}
		else if (value instanceof ServoyJSONArray)
		{
			ServoyJSONArray sArray = (ServoyJSONArray)value;
			JSONArray array = new JSONArray();
			for (int i = 0; i < sArray.length(); i++)
			{
				array.put(i, sArray.get(i));
			}
			value = sArray;

		}
		return value == null ? Context.getUndefinedValue() : ServoyJSONObject.jsonNullToNull(value);
	}

	@Override
	public void setHandler(String handlerName, JSMethod value)
	{
		WebComponent webComponent = getBaseComponent(false);
		WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(webComponent.getTypeName());
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
		WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(webComponent.getTypeName());
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
		if (jsonProperty == null || jsonProperty == JSONObject.NULL || jsonProperty == Context.getUndefinedValue())
		{
			jsonProperty = getJSONProperty(handlerName + "MethodID");
		}
		if (jsonProperty instanceof JSMethod) return (JSMethod)jsonProperty;
		else return null;
	}

	public static Object fromRhinoToDesignValue(Object value, PropertyDescription pd, IApplication application, JSWebComponent webComponent,
		String fullPropertyNameInCaseOfFormComponent)
	{
		Object result = null;
		if (pd != null && pd.getType() instanceof IFormComponentType)
		{
			String firstPart = fullPropertyNameInCaseOfFormComponent;
			int i = firstPart.indexOf('.');
			if (i > 0)
			{
				firstPart = firstPart.substring(0, i);
			}
			IFormComponentRhinoConverter converter = ((IFormComponentType)pd.getType()).getFormComponentRhinoConverter(firstPart,
				webComponent.getBaseComponent(true).getProperty(firstPart), application, webComponent);
			result = converter.setRhinoToDesignValue(
				firstPart == fullPropertyNameInCaseOfFormComponent ? "" : fullPropertyNameInCaseOfFormComponent.substring(firstPart.length() + 1), value);
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
		String fullPropertyNameInCaseOfFormComponent)
	{
		Object result = value;
		if (pd != null && pd.getType() instanceof IFormComponentType)
		{
			String firstPart = fullPropertyNameInCaseOfFormComponent;
			int i = firstPart.indexOf('.');
			if (i > 0)
			{
				firstPart = firstPart.substring(0, i);
			}
			IFormComponentRhinoConverter converter = ((IFormComponentType)pd.getType()).getFormComponentRhinoConverter(firstPart, value, application,
				webComponent);
			result = converter.getDesignToRhinoValue(
				firstPart == fullPropertyNameInCaseOfFormComponent ? "" : fullPropertyNameInCaseOfFormComponent.substring(firstPart.length() + 1));
		}
		else if (pd != null && pd.getType() instanceof IRhinoDesignConverter)
		{
			result = ((IRhinoDesignConverter)pd.getType()).fromDesignToRhinoValue(value, pd, application, webComponent);
		}
		return result == null ? Context.getUndefinedValue() : ServoyJSONObject.jsonNullToNull(result);
	}

	@Override
	public String[] getJSONPropertyNames(boolean includeAll)
	{
		WebComponent webComponent = getBaseComponent(false);
		if (includeAll)
		{
			WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(webComponent.getTypeName());
			if (spec != null)
			{
				return spec.getAllPropertiesNames().toArray(new String[0]);
			}
		}
		else
		{
			JSONObject json = webComponent.getFlattenedJson();
			if (json != null)
			{
				return json.keySet().toArray(new String[0]);
			}
		}
		return new String[] { };
	}

}
