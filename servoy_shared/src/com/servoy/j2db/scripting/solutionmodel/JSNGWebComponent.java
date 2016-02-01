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

import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.Context;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectSpecification;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IRhinoDesignConverter;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

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

			if (value instanceof JSValueList)
			{
				// should we move this into a IRhinoDesignConverter impl?
				value = new Integer(((JSValueList)value).getValueList().getID());
			}
			else if (value instanceof JSForm)
			{
				value = ((JSForm)value).getName();
			}
			else
			{
				WebObjectSpecification spec = WebComponentSpecProvider.getInstance().getWebComponentSpecification(webComponent.getTypeName());
				PropertyDescription pd = spec.getProperty(propertyName);
				if (pd == null) pd = spec.getHandler(propertyName);
				if (pd != null && pd.getType() instanceof IRhinoDesignConverter)
				{
					value = ((IRhinoDesignConverter)pd.getType()).fromRhinoToDesignValue(value, pd, application, this);
				}
				else
				{
					value = defaultRhinoToDesignValue(value, application);
				}
			}
			JSONObject jsonObject = webComponent.getJson() == null ? new ServoyJSONObject(true, true) : webComponent.getJson();
			jsonObject.put(propertyName, value);
			webComponent.setJson(jsonObject);
		}
		catch (JSONException e)
		{
			Debug.error(e);
		}
	}

	@Override
	public void resetJSONProperty(String propertyName)
	{
		// TODO we could do some checks here that it's really a property not a handler
		try
		{
			WebComponent webComponent = getBaseComponent(true);

			JSONObject jsonObject = webComponent.getJson() == null ? new ServoyJSONObject(true, true) : webComponent.getJson();
			jsonObject.remove(propertyName);
			webComponent.setJson(jsonObject);
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

		Object value = json.opt(propertyName);
		WebObjectSpecification spec = WebComponentSpecProvider.getInstance().getWebComponentSpecification(webComponent.getTypeName());
		if (spec != null)
		{
			PropertyDescription pd = spec.getProperty(propertyName);
			if (pd == null) pd = spec.getHandler(propertyName);
			if (pd != null && pd.getType() instanceof IRhinoDesignConverter)
			{
				return ((IRhinoDesignConverter)pd.getType()).fromDesignToRhinoValue(value, pd, application, this);
			}
			if (value != null && "form".equals(pd.getType().getName()))
			{
				Form form = null;
				UUID uuid = Utils.getAsUUID(value, false);
				if (uuid != null)
				{
					form = (Form)application.getFlattenedSolution().searchPersist(uuid);
				}
				if (value instanceof String && form == null)
				{
					form = application.getFlattenedSolution().getForm((String)value);
				}
				if (form != null)
				{
					return application.getScriptEngine().getSolutionModifier().instantiateForm(form, false);
				}
			}
			// JSONArray and JSONObject are automatically wrapped when going to Rhino through ServoyWrapFactory, so no need to treat them specially here
		}
		return value == null ? Context.getUndefinedValue() : ServoyJSONObject.jsonNullToNull(value);
	}

	@Override
	public void setHandler(String handlerName, JSMethod value)
	{
		WebComponent webComponent = getBaseComponent(false);
		WebObjectSpecification spec = WebComponentSpecProvider.getInstance().getWebComponentSpecification(webComponent.getTypeName());
		if (spec != null)
		{
			if (spec.getHandler(handlerName) != null)
			{
				setJSONProperty(handlerName, value);
			}
			else Debug.log("Error: component " + webComponent.getTypeName() + " does not declare a handler named " + handlerName + ".");
		}
	}

	@Override
	public void resetHandler(String handlerName)
	{
		resetJSONProperty(handlerName); // TODO we could do some checks here that it's really a handler not a property
	}

	@Override
	public JSMethod getHandler(String handlerName)
	{
		Object jsonProperty = getJSONProperty(handlerName);
		if (jsonProperty instanceof JSMethod) return (JSMethod)jsonProperty;
		else return null;
	}

}
