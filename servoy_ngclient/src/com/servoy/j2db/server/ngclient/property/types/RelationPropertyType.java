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
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IPropertyConverterForBrowser;
import org.sablo.specification.property.types.DefaultPropertyType;
import org.sablo.util.ValueReference;
import org.sablo.websocket.CurrentWindow;
import org.sablo.websocket.utils.DataConversion;

import com.servoy.j2db.dataprocessing.RelatedFoundSet;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.INGClientWindow;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToTemplateJSON;

/**
 * @author jcompagner
 */
public class RelationPropertyType extends DefaultPropertyType<RelatedFoundSet>
	implements IPropertyConverterForBrowser<String>, IFormElementToTemplateJSON<String, RelatedFoundSet>
{
	public static RelationPropertyType INSTANCE = new RelationPropertyType();
	public static final String TYPE_NAME = "relation";

	private RelationPropertyType()
	{
	}

	@Override
	public String fromJSON(Object newJSONValue, String previousSabloValue, PropertyDescription propertyDescription, IBrowserConverterContext context,
		ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		// never allow to change
		return previousSabloValue;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, String sabloValue, PropertyDescription propertyDescription, DataConversion clientConversion,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		if (key != null)
		{
			writer.key(key);
		}
		String relationName = sabloValue;
		if (CurrentWindow.get() instanceof INGClientWindow)
		{
			relationName = ((INGClientWindow)CurrentWindow.get()).registerAllowedRelation(relationName,
				((WebFormComponent)dataConverterContext.getWebObject()).getFormElement());
		}
		writer.value(relationName);
		return writer;
	}

	@Override
	public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, String formElementValue, PropertyDescription pd,
		DataConversion browserConversionMarkers, FormElementContext formElementContext) throws JSONException
	{
		if (key != null)
		{
			writer.key(key);
		}
		String relationName = formElementValue;
		if (CurrentWindow.safeGet() instanceof INGClientWindow)
		{
			relationName = ((INGClientWindow)CurrentWindow.get()).registerAllowedRelation(relationName, formElementContext.getFormElement());
		}
		writer.value(relationName);
		return writer;
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
}
