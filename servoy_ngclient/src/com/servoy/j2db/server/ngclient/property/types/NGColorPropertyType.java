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

import java.awt.Color;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mozilla.javascript.Scriptable;
import org.sablo.IWebObjectContext;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.types.ColorPropertyType;
import org.sablo.websocket.utils.DataConversion;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.IDesignValueConverter;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.INGFormElement;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IDesignToFormElement;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToTemplateJSON;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IRhinoToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISabloComponentToRhino;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.PersistHelper;

/**
 *
 * @author acostescu
 */
public class NGColorPropertyType extends ColorPropertyType implements IDesignToFormElement<Object, Color, Color>, IFormElementToTemplateJSON<Color, Color>,
	ISabloComponentToRhino<Color>, IRhinoToSabloComponent<Color>, IDesignValueConverter<Color>
{

	public final static NGColorPropertyType NG_INSTANCE = new NGColorPropertyType();

	@Override
	public Color toFormElementValue(Object designValue, PropertyDescription pd, FlattenedSolution flattenedSolution, INGFormElement formElement,
		PropertyPath propertyPath)
	{
		return fromJSON(designValue, null, pd, null, null);
	}

	@Override
	public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, Color formElementValue, PropertyDescription pd,
		DataConversion browserConversionMarkers, FormElementContext formElementContext) throws JSONException
	{
		return toJSON(writer, key, formElementValue, pd, browserConversionMarkers, null);
	}

	@Override
	public boolean isValueAvailableInRhino(Color webComponentValue, PropertyDescription pd, IWebObjectContext webObjectContext)
	{
		return true;
	}

	@Override
	public Object toRhinoValue(Color webComponentValue, PropertyDescription pd, IWebObjectContext componentOrService, Scriptable startScriptable)
	{
		return PersistHelper.createColorString(webComponentValue);
	}

	@Override
	public Color toSabloComponentValue(Object rhinoValue, Color previousComponentValue, PropertyDescription pd, IWebObjectContext componentOrService)
	{
		if (rhinoValue instanceof String)
		{
			return PersistHelper.createColor(rhinoValue.toString());
		}
		return (Color)(rhinoValue instanceof Color ? rhinoValue : null);
	}

	@Override
	public Color fromDesignValue(Object newValue, PropertyDescription propertyDescription)
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

	@Override
	public Object toDesignValue(Object value, PropertyDescription pd)
	{
		return (value instanceof Color) ? getStringValue((Color)value) : value;
	}

}
