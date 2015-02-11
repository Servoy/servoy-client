/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 */

package com.servoy.j2db.server.ngclient.property;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mozilla.javascript.Scriptable;
import org.sablo.BaseWebObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.CustomJSONPropertyType;
import org.sablo.specification.property.IConvertedPropertyType;
import org.sablo.specification.property.IDataConverterContext;
import org.sablo.specification.property.ISupportsGranularUpdates;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.types.IDataLinkedType;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToTemplateJSON;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISabloComponentToRhino;

/**
 * Implementation for the complex custom type "foundset".
 *
 * @author acostescu
 */
public class FoundsetPropertyType extends CustomJSONPropertyType<FoundsetTypeSabloValue> implements
	IFormElementToTemplateJSON<JSONObject, FoundsetTypeSabloValue>, IFormElementToSabloComponent<JSONObject, FoundsetTypeSabloValue>,
	IConvertedPropertyType<FoundsetTypeSabloValue>, ISabloComponentToRhino<FoundsetTypeSabloValue>, ISupportsGranularUpdates<FoundsetTypeSabloValue>,
	IDataLinkedType<JSONObject, FoundsetTypeSabloValue>
{

	public static final FoundsetPropertyType INSTANCE = new FoundsetPropertyType(null);

	public static final String TYPE_NAME = "foundset";

	public FoundsetPropertyType(PropertyDescription definition)
	{
		super("foundset", definition);
	}

	@Override
	public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, JSONObject formElementValue, PropertyDescription pd, DataConversion conversionMarkers,
		FlattenedSolution fs, FormElement formElement) throws JSONException
	{
		// this just dumps an empty/dummy value
		if (conversionMarkers != null) conversionMarkers.convert(TYPE_NAME); // so that the client knows it must use the custom client side JS for what JSON it gets

		JSONUtils.addKeyIfPresent(writer, key);
		writer.object();
		writer.key(FoundsetTypeSabloValue.SERVER_SIZE).value(0);
		writer.key(FoundsetTypeSabloValue.SELECTED_ROW_INDEXES).array().endArray();
		writer.key(FoundsetTypeSabloValue.MULTI_SELECT).value(false);

		// viewPort
		writer.key(FoundsetTypeSabloValue.VIEW_PORT).object().key(FoundsetTypeSabloValue.START_INDEX).value(0).key(FoundsetTypeSabloValue.SIZE).value(0).key(
			FoundsetTypeSabloValue.ROWS).array().endArray().endObject();
		// end viewPort

		writer.endObject();
		return writer;
	}

	@Override
	public FoundsetTypeSabloValue toSabloComponentValue(JSONObject formElementValue, PropertyDescription pd, FormElement formElement,
		WebFormComponent component, DataAdapterList dal)
	{
		return new FoundsetTypeSabloValue(formElementValue, pd.getName(), dal);
	}

	@Override
	public FoundsetTypeSabloValue fromJSON(Object newJSONValue, FoundsetTypeSabloValue previousSabloValue, IDataConverterContext dataConverterContext)
	{
		if (previousSabloValue != null)
		{
			previousSabloValue.browserUpdatesReceived(newJSONValue);
		}
		// else there's nothing to do here / this type can't receive browser updates when server has no value for it

		return previousSabloValue;
	}

	@Override
	public JSONWriter changesToJSON(JSONWriter writer, String key, FoundsetTypeSabloValue sabloValue, DataConversion clientConversion,
		IDataConverterContext dataConverterContext) throws JSONException
	{
		if (sabloValue != null)
		{
			JSONUtils.addKeyIfPresent(writer, key);
			sabloValue.changesToJSON(writer, clientConversion);
		}
		return null;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, FoundsetTypeSabloValue sabloValue, DataConversion clientConversion,
		IDataConverterContext dataConverterContext) throws JSONException
	{
		if (sabloValue != null)
		{
			JSONUtils.addKeyIfPresent(writer, key);
			sabloValue.toJSON(writer, clientConversion);
		}
		return null;
	}

	@Override
	public boolean isValueAvailableInRhino(FoundsetTypeSabloValue webComponentValue, PropertyDescription pd, BaseWebObject componentOrService)
	{
		return false;
	}

	@Override
	public Object toRhinoValue(FoundsetTypeSabloValue webComponentValue, PropertyDescription pd, BaseWebObject componentOrService, Scriptable startScriptable)
	{
		return Scriptable.NOT_FOUND;
	}

	@Override
	public TargetDataLinks getDataLinks(JSONObject formElementValue, PropertyDescription pd, FlattenedSolution flattenedSolution, FormElement formElement)
	{
		return TargetDataLinks.LINKED_TO_ALL; // if you change this you should call this method in FoundsetTypeSabloValue.attach as well when registering as listener to DAL
	}

}
