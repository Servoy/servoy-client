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

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mozilla.javascript.Scriptable;
import org.sablo.BaseWebObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IConvertedPropertyType;
import org.sablo.specification.property.types.DefaultPropertyType;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.DataConversion;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.scripting.solutionmodel.JSForm;
import com.servoy.j2db.scripting.solutionmodel.JSWebComponent;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.NGClientEntryFilter;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToTemplateJSON;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISabloComponentToRhino;
import com.servoy.j2db.util.IRhinoDesignConverter;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 */
public class FormComponentPropertyType extends DefaultPropertyType<Object>
	implements IConvertedPropertyType<Object>, ISabloComponentToRhino<Object>, IFormElementToTemplateJSON<Object, Object>, IRhinoDesignConverter
{
	public static final FormComponentPropertyType INSTANCE = new FormComponentPropertyType();
	public static final String TYPE_NAME = "formcomponent";

	protected FormComponentPropertyType()
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
		return json;
	}

	@Override
	public Object fromJSON(Object newJSONValue, Object previousSabloValue, PropertyDescription pd, IBrowserConverterContext dataConverterContext,
		ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		return newJSONValue;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, Object sabloValue, PropertyDescription pd, DataConversion clientConversion,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		return writer;
	}

	@Override
	public boolean isValueAvailableInRhino(Object webComponentValue, PropertyDescription pd, BaseWebObject componentOrService)
	{
		return true;
	}

	@Override
	public Object toRhinoValue(Object webComponentValue, PropertyDescription pd, BaseWebObject componentOrService, Scriptable startScriptable)
	{
		// TODO return here a NativeScriptable object that understand the full hiearchy?
		return webComponentValue;
	}

	@Override
	public String defaultValue(PropertyDescription pd)
	{
		return null;
	}

	@Override
	public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, Object formElementValue, PropertyDescription pd,
		DataConversion browserConversionMarkers, FormElementContext formElementContext) throws JSONException
	{
		FlattenedSolution fs = formElementContext.getFlattenedSolution();

		Form form = getForm(formElementValue, fs);
		if (form != null)
		{
			writer.key(key);
			writer.object();
			String formUrl = NGClientEntryFilter.SOLUTIONS_PATH + form.getSolution().getName() + '/' + NGClientEntryFilter.FORMS_PATH + form.getName() +
				".html?formcomponent=true";
			if (formElementContext.getContext() != null && formElementContext.getContext().getApplication() != null)
			{
				formUrl += "&sessionId=" + formElementContext.getContext().getApplication().getWebsocketSession().getUuid();
			}
			writer.key("svy_form_url");
			writer.value(formUrl);
			writer.endObject();
		}
		return writer;
	}

	/**
	 * @param formElementValue
	 * @param fs
	 * @return
	 */
	public Form getForm(Object formElementValue, FlattenedSolution fs)
	{
		Form form = null;
		if (formElementValue instanceof Integer)
		{
			form = fs.getForm(((Integer)formElementValue).intValue());
		}
		else if (formElementValue instanceof String || formElementValue instanceof UUID)
		{

			UUID uuid = Utils.getAsUUID(formElementValue, false);
			if (uuid != null) form = (Form)fs.searchPersist(uuid);
			else form = fs.getForm((String)formElementValue);
		}
		return form;
	}

	@Override
	public Object fromRhinoToDesignValue(Object value, PropertyDescription pd, IApplication application, JSWebComponent webComponent)
	{
		if (value instanceof String)
		{
			Form f = application.getFlattenedSolution().getForm((String)value);
			if (f != null) return f.getUUID();
		}
		else if (value instanceof JSForm)
		{
			return ((JSForm)value).getUUID();
		}
		return value;
	}

	@Override
	public Object fromDesignToRhinoValue(Object value, PropertyDescription pd, IApplication application, JSWebComponent webComponent)
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
			return application.getScriptEngine().getSolutionModifier().getForm(form.getName());
		}

		return null;
	}
}
