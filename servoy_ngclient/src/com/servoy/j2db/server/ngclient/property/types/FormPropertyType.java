/*
 * Copyright (C) 2014 Servoy BV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.servoy.j2db.server.ngclient.property.types;

import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mozilla.javascript.Scriptable;
import org.sablo.IWebObjectContext;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IConvertedPropertyType;
import org.sablo.specification.property.IPropertyCanDependsOn;
import org.sablo.specification.property.types.DefaultPropertyType;
import org.sablo.util.ValueReference;
import org.sablo.websocket.CurrentWindow;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.FormController;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.scripting.solutionmodel.JSForm;
import com.servoy.j2db.scripting.solutionmodel.JSWebComponent;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.IContextProvider;
import com.servoy.j2db.server.ngclient.INGClientWindow;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToTemplateJSON;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISabloComponentToRhino;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IRhinoDesignConverter;
import com.servoy.j2db.util.UUID;

/**
 * @author jcompagner
 */
public class FormPropertyType extends DefaultPropertyType<Object>
	implements IConvertedPropertyType<Object>, ISabloComponentToRhino<Object>, IFormElementToTemplateJSON<Object, Object>, IRhinoDesignConverter,
	IPropertyCanDependsOn
{
	public static final FormPropertyType INSTANCE = new FormPropertyType();
	public static final String TYPE_NAME = "form";

	private String[] dependencies;

	protected FormPropertyType()
	{
	}

	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

	@Override
	public Object parseConfig(JSONObject json)
	{
		dependencies = getDependencies(json, dependencies);
		return json;
	}

	@Override
	public Object fromJSON(Object newJSONValue, Object previousSabloValue, PropertyDescription pd, IBrowserConverterContext dataConverterContext,
		ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		if (newJSONValue instanceof JSONObject)
		{
			Iterator<String> it = ((JSONObject)newJSONValue).keys();
			if (it.hasNext())
			{
				String key = it.next();
				try
				{
					newJSONValue = ((JSONObject)newJSONValue).get(key);
				}
				catch (JSONException e)
				{
					Debug.error(e);
				}
			}
		}
		if (newJSONValue != null && CurrentWindow.get() instanceof INGClientWindow)
		{
			try
			{
				// check if component is allowed to show the form
				if (dataConverterContext != null && dataConverterContext.getWebObject() instanceof WebFormComponent)
					((INGClientWindow)CurrentWindow.get()).isVisibleAllowed(newJSONValue.toString(), null,
						((WebFormComponent)dataConverterContext.getWebObject()).getFormElement());
				// check if this form is allowed to be shown globally (via window service)
				else((INGClientWindow)CurrentWindow.get()).isVisibleAllowed(newJSONValue.toString(), null, null);
			}
			catch (Exception ex)
			{
				Debug.error(ex);
				return null;
			}
		}
		return newJSONValue;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, Object sabloValue, PropertyDescription pd, IBrowserConverterContext dataConverterContext)
		throws JSONException
	{
		if (key != null)
		{
			writer.key(key);
		}
		String formName = null;
		if (sabloValue instanceof String)
		{
			formName = (String)sabloValue;
			if (dataConverterContext != null && dataConverterContext.getWebObject() instanceof IContextProvider)
			{
				FlattenedSolution flattenedSolution = ((IContextProvider)dataConverterContext.getWebObject()).getDataConverterContext().getApplication()
					.getFlattenedSolution();
				Form form = flattenedSolution.getForm(formName);
				// form name
				if (form == null)
				{
					form = (Form)flattenedSolution.searchPersist((String)sabloValue);
				}
				if (form != null)
				{
					formName = form.getName();
				}
			}
		}
		else if (sabloValue instanceof Integer && dataConverterContext != null && dataConverterContext.getWebObject() instanceof IContextProvider)
		{
			FlattenedSolution flattenedSolution = ((IContextProvider)dataConverterContext.getWebObject()).getDataConverterContext().getApplication()
				.getFlattenedSolution();
			Form form = flattenedSolution.getForm((Integer)sabloValue);
			if (form != null)
			{
				formName = form.getName();
			}
			else
			{
				formName = null;
				Debug.error("Cannot handle integer value for Form type: " + sabloValue);
			}
		}
		else if (sabloValue instanceof CharSequence)
		{
			formName = ((CharSequence)sabloValue).toString();
		}
		else if (sabloValue instanceof Form)
		{
			formName = ((Form)sabloValue).getName();
		}
		else if (sabloValue instanceof FormController)
		{
			formName = ((FormController)sabloValue).getName();
		}
		else if (sabloValue instanceof FormScope)
		{
			formName = ((FormScope)sabloValue).getScopeName();
		}
		else
		{
			formName = null;
			Debug.error("Cannot handle unknown value for Form type: " + sabloValue);
		}
		writer.value(formName);
		if (CurrentWindow.get() instanceof INGClientWindow)
		{
			// if this is a web component that triggered it, register to only allow this form for that component
			if (dataConverterContext != null && dataConverterContext.getWebObject() instanceof WebFormComponent)
				((INGClientWindow)CurrentWindow.get()).registerAllowedForm(formName, ((WebFormComponent)dataConverterContext.getWebObject()).getFormElement());
			// else register it for null then this form is allowed globally (a form in dialog of popup)
			else((INGClientWindow)CurrentWindow.get()).registerAllowedForm(formName, null);
		}
		return writer;
	}

	@Override
	public boolean isValueAvailableInRhino(Object webComponentValue, PropertyDescription pd, IWebObjectContext webObjectContext)
	{
		return true;
	}

	@Override
	public Object toRhinoValue(Object webComponentValue, PropertyDescription pd, IWebObjectContext webObjectContext, Scriptable startScriptable)
	{
		if (webComponentValue instanceof Form)
		{
			return ((Form)webComponentValue).getName();
		}
		else
		{
			if (webComponentValue != null && webObjectContext != null && webObjectContext.getUnderlyingWebObject() instanceof IContextProvider)
			{
				// form is stored as uuid on disk
				FlattenedSolution solution = ((IContextProvider)webObjectContext.getUnderlyingWebObject()).getDataConverterContext().getSolution();
				Form form = solution.getForm(webComponentValue.toString());
				if (form == null)
				{
					form = (Form)solution.searchPersist(webComponentValue.toString());
				}
				if (form != null)
				{
					return form.getName();
				}
			}
		}
		return webComponentValue;
	}

	@Override
	public String defaultValue(PropertyDescription pd)
	{
		return null;
	}

	@Override
	public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, Object formElementValue, PropertyDescription pd, FormElementContext formElementContext)
		throws JSONException
	{
		if (formElementValue == null) return writer;

		FlattenedSolution fs = formElementContext.getFlattenedSolution();

		Form form = null;
		if (formElementValue instanceof Integer)
		{
			form = fs.getForm(((Integer)formElementValue).intValue());
		}
		else if (formElementValue instanceof String || formElementValue instanceof UUID)
		{
			form = fs.getForm(formElementValue.toString());
			if (form == null)
			{
				form = (Form)fs.searchPersist(formElementValue.toString());
			}
		}
		if (form != null)
		{
			writer.key(key);
			writer.value(form.getName());
			if (CurrentWindow.safeGet() instanceof INGClientWindow)
			{
				((INGClientWindow)CurrentWindow.get()).registerAllowedForm(form.getName(), formElementContext.getFormElement());
			}
		}
		return writer;
	}

	@Override
	public Object fromRhinoToDesignValue(Object value, PropertyDescription pd, IApplication application, JSWebComponent webComponent)
	{
		if (value instanceof String)
		{
			Form f = application.getFlattenedSolution().getForm((String)value);
			if (f != null) return f.getUUID().toString();
		}
		else if (value instanceof JSForm)
		{
			return ((JSForm)value).getUUID().toString();
		}
		return value;
	}

	@Override
	public Object fromDesignToRhinoValue(Object value, PropertyDescription pd, IApplication application, JSWebComponent webComponent)
	{
		Form form = null;
		if (value != null)
		{
			form = application.getFlattenedSolution().getForm(value.toString());
			if (form == null) form = (Form)application.getFlattenedSolution().searchPersist(value.toString());
		}
		if (form != null)
		{
			return application.getScriptEngine().getSolutionModifier().getForm(form.getName());
		}

		return null;
	}

	@Override
	public String[] getDependencies()
	{
		return dependencies;
	}
}
