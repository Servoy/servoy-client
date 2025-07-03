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
import org.sablo.IWebObjectContext;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IPropertyCanDependsOn;
import org.sablo.specification.property.IPropertyConverterForBrowser;
import org.sablo.specification.property.types.DefaultPropertyType;
import org.sablo.util.ValueReference;
import org.sablo.websocket.CurrentWindow;

import com.servoy.j2db.dataprocessing.RelatedFoundSet;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.INGClientWindow;
import com.servoy.j2db.server.ngclient.NGClientWindow;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.component.RhinoConversion;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToTemplateJSON;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IRhinoToSabloComponent;

/**
 * @author jcompagner
 */
public class RelationPropertyType extends DefaultPropertyType<String>
	implements IPropertyConverterForBrowser<String>, IFormElementToTemplateJSON<String, String>, IRhinoToSabloComponent<String>, IPropertyCanDependsOn
{
	public static RelationPropertyType INSTANCE = new RelationPropertyType();
	public static final String TYPE_NAME = "relation";

	private String[] dependencies;

	private RelationPropertyType()
	{
	}

	@Override
	public String fromJSON(Object newJSONValue, String previousSabloValue, PropertyDescription propertyDescription, IBrowserConverterContext context,
		ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		if (newJSONValue instanceof String)
		{
			return NGClientWindow.getCurrentWindow().getRelationName((String)newJSONValue,
				((WebFormComponent)context.getWebObject()).getFormElement());
		}

		// never allow to change
		return previousSabloValue;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, String sabloValue, PropertyDescription propertyDescription,
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
	public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, String formElementValue, PropertyDescription pd, FormElementContext formElementContext)
		throws JSONException
	{
		if (formElementValue == null) return writer;

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
		dependencies = getDependencies(json, dependencies);
		return json;
	}

	@Override
	public String toSabloComponentValue(Object rhinoValue, String previousComponentValue, PropertyDescription pd,
		IWebObjectContext webObjectContext)
	{
		if (rhinoValue == null || RhinoConversion.isUndefinedOrNotFound(rhinoValue)) return null;
		if (rhinoValue instanceof RelatedFoundSet)
		{
			return ((RelatedFoundSet)rhinoValue).getRelationName();
		}
		return (String)rhinoValue;
	}

	@Override
	public String[] getDependencies()
	{
		return dependencies;
	}
}
