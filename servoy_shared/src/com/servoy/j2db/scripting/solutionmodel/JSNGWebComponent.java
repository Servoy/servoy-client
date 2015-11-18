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
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebComponentSpecification;

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
			WebComponent webComponent = getBaseComponent(true);

			if (value instanceof JSValueList)
			{
				// should we move this into a IRhinoDesignConverter impl?
				value = new Integer(((JSValueList)value).getValueList().getID());
			}
			else
			{
				WebComponentSpecification spec = WebComponentSpecProvider.getInstance().getWebComponentSpecification(webComponent.getTypeName());
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
	public Object getJSONProperty(String propertyName)
	{
		WebComponent webComponent = getBaseComponent(false);
		JSONObject json = webComponent.getFlattenedJson();
		Object value = json.opt(propertyName);
		WebComponentSpecification spec = WebComponentSpecProvider.getInstance().getWebComponentSpecification(webComponent.getTypeName());
		if (spec != null)
		{
			PropertyDescription pd = spec.getProperty(propertyName);
			if (pd != null && pd.getType() instanceof IRhinoDesignConverter)
			{
				value = ((IRhinoDesignConverter)pd.getType()).fromDesignToRhinoValue(value, pd, application, this);
			}
			// JSONArray and JSONObject are automatically wrapped when going to Rhino through ServoyWrapFactory, so no need to treat them specially here
		}
		return value;
	}

	@Override
	public void setHandler(String handlerName, Object value)
	{
		WebComponent webComponent = getBaseComponent(false);
		WebComponentSpecification spec = WebComponentSpecProvider.getInstance().getWebComponentSpecification(webComponent.getTypeName());
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
	public Object getHandler(String handlerName)
	{
		return getJSONProperty(handlerName);
	}

}
