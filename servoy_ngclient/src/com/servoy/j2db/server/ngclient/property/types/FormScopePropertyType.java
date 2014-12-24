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

import org.json.JSONObject;
import org.sablo.BaseWebObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.types.DefaultPropertyType;

import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.server.ngclient.IContextProvider;
import com.servoy.j2db.server.ngclient.INGApplication;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IRhinoToSabloComponent;

/**
 * This is a special type that is used in api calls to let servoy know that the api should return a form server instance itself
 * in the model this would be just the same as the {@link FormPropertyType} where the model value is just a string.
 * TODO this should be looked at for getRightForm for example of the SplitPane
 * @author jcompagner
 */
public class FormScopePropertyType extends DefaultPropertyType<FormScope> implements IRhinoToSabloComponent<FormScope>
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
	public FormScope toSabloComponentValue(Object rhinoValue, FormScope previousComponentValue, PropertyDescription pd, BaseWebObject componentOrService)
	{
		INGApplication app = ((IContextProvider)componentOrService).getDataConverterContext().getApplication();
		if (rhinoValue instanceof String && app != null)
		{
			return app.getFormManager().getForm((String)rhinoValue).getFormScope();
		}
		return null;
	}

}
