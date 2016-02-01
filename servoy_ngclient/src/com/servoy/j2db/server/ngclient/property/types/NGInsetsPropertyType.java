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

import java.awt.Insets;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONWriter;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.types.InsetsPropertyType;
import org.sablo.websocket.utils.DataConversion;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.IDesignValueConverter;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.INGFormElement;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IDesignToFormElement;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToTemplateJSON;
import com.servoy.j2db.util.Debug;

/**
 * @author acostescu
 */
public class NGInsetsPropertyType extends InsetsPropertyType
	implements IDesignToFormElement<Object, Insets, Insets>, IFormElementToTemplateJSON<Insets, Insets>, IDesignValueConverter<Insets>
{

	public final static NGInsetsPropertyType NG_INSTANCE = new NGInsetsPropertyType();

	@Override
	public Insets toFormElementValue(Object designValue, PropertyDescription pd, FlattenedSolution flattenedSolution, INGFormElement formElement,
		PropertyPath propertyPath)
	{
		return fromJSON(designValue, null, pd, null, null);
	}

	@Override
	public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, Insets formElementValue, PropertyDescription pd,
		DataConversion browserConversionMarkers, FormElementContext formElementContext) throws JSONException
	{
		return toJSON(writer, key, formElementValue, pd, browserConversionMarkers, null);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.sablo.specification.property.IDesignValueConverter#fromDesignValue(java.lang.Object, org.sablo.specification.PropertyDescription)
	 */
	@Override
	public Insets fromDesignValue(Object newValue, PropertyDescription propertyDescription)
	{
		try
		{
			return fromJSON((newValue instanceof String && ((String)newValue).startsWith("{")) ? new JSONObject((String)newValue) : newValue, null,
				propertyDescription, null, null);
		}
		catch (Exception e)
		{
			Debug.error("can't parse '" + newValue + "' to the real type for property converter: " + propertyDescription.getType(), e);
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.sablo.specification.property.IDesignValueConverter#toDesignValue(java.lang.Object, org.sablo.specification.PropertyDescription)
	 */
	@Override
	public Object toDesignValue(Object value, PropertyDescription pd)
	{
		if (value instanceof Insets)
		{
			JSONStringer writer = new JSONStringer();
			toJSON(writer, null, (Insets)value, pd, null, null);
			return new JSONObject(writer.toString());
		}
		return value;
	}
}
