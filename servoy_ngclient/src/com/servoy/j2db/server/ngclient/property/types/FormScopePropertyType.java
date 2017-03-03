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
import org.sablo.specification.property.IPropertyConverterForBrowser;
import org.sablo.specification.property.types.DefaultPropertyType;
import org.sablo.util.ValueReference;
import org.sablo.websocket.CurrentWindow;
import org.sablo.websocket.utils.DataConversion;

import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.server.ngclient.IContextProvider;
import com.servoy.j2db.server.ngclient.INGApplication;
import com.servoy.j2db.server.ngclient.INGClientWindow;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IRhinoToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISabloComponentToRhino;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IServerRhinoToRhino;

/**
 * This is a special type that is used in api calls to let servoy know that the api should return a form server instance itself
 * in the model this would be just the same as the {@link FormPropertyType} where the model value is just a string.
 * TODO this should be looked at for getRightForm for example of the SplitPane
 * @author jcompagner
 */
public class FormScopePropertyType extends DefaultPropertyType<Object>
	implements IRhinoToSabloComponent<Object>, ISabloComponentToRhino<Object>, IPropertyConverterForBrowser<FormScope>, IServerRhinoToRhino<Object>
{

	public static final FormScopePropertyType INSTANCE = new FormScopePropertyType();
	public static final String TYPE_NAME = "formscope";

	private FormScopePropertyType()
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
	public Object toSabloComponentValue(Object rhinoValue, Object previousComponentValue, PropertyDescription pd, BaseWebObject componentOrService)
	{
		INGApplication app = ((IContextProvider)componentOrService).getDataConverterContext().getApplication();
		if (rhinoValue instanceof String && app != null)
		{
			return app.getFormManager().getForm((String)rhinoValue).getFormScope();
		}
		return rhinoValue;
	}

	@Override
	public FormScope fromJSON(Object newJSONValue, FormScope previousSabloValue, PropertyDescription pd, IBrowserConverterContext dataConverterContext,
		ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		INGApplication app = ((IContextProvider)dataConverterContext.getWebObject()).getDataConverterContext().getApplication();
		if (newJSONValue instanceof String && app != null)
		{
			return app.getFormManager().getForm((String)newJSONValue).getFormScope();
		}
		return null;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, FormScope sabloValue, PropertyDescription pd, DataConversion clientConversion,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		if (sabloValue != null)
		{
			writer.value(sabloValue.getFormController().getName());
			if (CurrentWindow.get() instanceof INGClientWindow)
			{
				((INGClientWindow)CurrentWindow.get()).registerAllowedForm(sabloValue.getFormController().getName());
			}
		}
		else
		{
			writer.value(null);
		}
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
		INGApplication app = ((IContextProvider)componentOrService).getDataConverterContext().getApplication();
		if (webComponentValue instanceof String && app != null)
		{
			return app.getFormManager().getForm((String)webComponentValue).getFormScope();
		}
		return webComponentValue;
	}

	@Override
	public Object fromServerRhinoToRhinoValue(Object serverSideScriptingReturnValue, PropertyDescription pd, BaseWebObject componentOrService,
		Scriptable startScriptable)
	{
		return toRhinoValue(serverSideScriptingReturnValue, pd, componentOrService, startScriptable);
	}

}
