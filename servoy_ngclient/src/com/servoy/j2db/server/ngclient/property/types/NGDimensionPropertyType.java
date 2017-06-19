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

import java.awt.Dimension;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONWriter;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.sablo.IWebObjectContext;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.types.DimensionPropertyType;
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
import com.servoy.j2db.util.Utils;

/**
 *
 * @author acostescu
 */
public class NGDimensionPropertyType extends DimensionPropertyType implements IDesignToFormElement<JSONObject, Dimension, Dimension>,
	IFormElementToTemplateJSON<Dimension, Dimension>, ISabloComponentToRhino<Dimension>, IRhinoToSabloComponent<Dimension>, IDesignValueConverter<Dimension>
{

	public final static NGDimensionPropertyType NG_INSTANCE = new NGDimensionPropertyType();

	@Override
	public Dimension toFormElementValue(JSONObject designValue, PropertyDescription pd, FlattenedSolution flattenedSolution, INGFormElement formElement,
		PropertyPath propertyPath)
	{
		return fromJSON(designValue, null, pd, null, null);
	}

	@Override
	public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, Dimension formElementValue, PropertyDescription pd,
		DataConversion browserConversionMarkers, FormElementContext formElementContext) throws JSONException
	{
		return toJSON(writer, key, formElementValue, pd, browserConversionMarkers, null);
	}

	@Override
	public boolean isValueAvailableInRhino(Dimension webComponentValue, PropertyDescription pd, IWebObjectContext webObjectContext)
	{
		return true;
	}

	@Override
	public Object toRhinoValue(Dimension webComponentValue, PropertyDescription pd, IWebObjectContext componentOrService, Scriptable startScriptable)
	{
		return PersistHelper.createDimensionString(webComponentValue);
	}

	@Override
	public Dimension toSabloComponentValue(Object rhinoValue, Dimension previousComponentValue, PropertyDescription pd, IWebObjectContext componentOrService)
	{
		if (rhinoValue instanceof Object[])
		{
			return new Dimension(Utils.getAsInteger(((Object[])rhinoValue)[0]), Utils.getAsInteger(((Object[])rhinoValue)[1]));
		}
		if (rhinoValue instanceof NativeObject)
		{
			NativeObject value = (NativeObject)rhinoValue;
			return new Dimension(Utils.getAsInteger(value.get("width", value)), Utils.getAsInteger(value.get("height", value)));
		}
		return (Dimension)(rhinoValue instanceof Dimension ? rhinoValue : null);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.sablo.specification.property.IDesignValueConverter#fromDesignValue(java.lang.Object, org.sablo.specification.PropertyDescription)
	 */
	@Override
	public Dimension fromDesignValue(Object newValue, PropertyDescription propertyDescription)
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
		if (value instanceof Dimension)
		{
			JSONStringer writer = new JSONStringer();
			toJSON(writer, null, (Dimension)value, pd, null, null);
			return new JSONObject(writer.toString());
		}
		return value;
	}
}
