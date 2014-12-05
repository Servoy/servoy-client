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
import org.json.JSONWriter;
import org.mozilla.javascript.Scriptable;
import org.sablo.BaseWebObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.types.DimensionPropertyType;
import org.sablo.websocket.utils.DataConversion;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.IServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IDesignToFormElement;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToTemplateJSON;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISabloComponentToRhino;
import com.servoy.j2db.util.PersistHelper;

/**
 *
 * @author acostescu
 */
public class NGDimensionPropertyType extends DimensionPropertyType implements IDesignToFormElement<JSONObject, Dimension, Dimension>,
	IFormElementToTemplateJSON<Dimension, Dimension>, ISabloComponentToRhino<Dimension>
{

	public final static NGDimensionPropertyType NG_INSTANCE = new NGDimensionPropertyType();

	@Override
	public Dimension toFormElementValue(JSONObject designValue, PropertyDescription pd, FlattenedSolution flattenedSolution, FormElement formElement,
		PropertyPath propertyPath)
	{
		return fromJSON(designValue, null, null);
	}

	@Override
	public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, Dimension formElementValue, PropertyDescription pd,
		DataConversion browserConversionMarkers, IServoyDataConverterContext servoyDataConverterContext) throws JSONException
	{
		return toJSON(writer, key, formElementValue, browserConversionMarkers, null);
	}

	@Override
	public boolean isValueAvailableInRhino(Dimension webComponentValue, PropertyDescription pd, BaseWebObject componentOrService)
	{
		return true;
	}

	@Override
	public Object toRhinoValue(Dimension webComponentValue, PropertyDescription pd, BaseWebObject componentOrService, Scriptable startScriptable)
	{
		return PersistHelper.createDimensionString(webComponentValue);
	}

}
