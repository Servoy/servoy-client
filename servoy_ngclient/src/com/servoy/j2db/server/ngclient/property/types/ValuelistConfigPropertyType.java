/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2023 Servoy BV

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

package com.servoy.j2db.server.ngclient.property.types;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mozilla.javascript.Scriptable;
import org.sablo.IWebObjectContext;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IConvertedPropertyType;
import org.sablo.specification.property.types.DefaultPropertyType;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.INGFormElement;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementDefaultValueToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IRhinoToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISabloComponentToRhino;

/**
 * @author lvostinar
 *
 */
public class ValuelistConfigPropertyType extends DefaultPropertyType<ValuelistConfigTypeSabloValue>
	implements IConvertedPropertyType<ValuelistConfigTypeSabloValue>, IFormElementDefaultValueToSabloComponent<JSONObject, ValuelistConfigTypeSabloValue>,
	ISabloComponentToRhino<ValuelistConfigTypeSabloValue>, IRhinoToSabloComponent<ValuelistConfigTypeSabloValue>
{

	public static final ValuelistConfigPropertyType INSTANCE = new ValuelistConfigPropertyType();
	public static final String TYPE_NAME = "valuelistConfig";

	@Override
	public String getName()
	{
		return TYPE_NAME;
	}


	@Override
	public ValuelistConfigTypeSabloValue fromJSON(Object newJSONValue, ValuelistConfigTypeSabloValue previousSabloValue, PropertyDescription pd,
		IBrowserConverterContext dataConverterContext, ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		//we do not allow changes coming in from the client
		return previousSabloValue;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, ValuelistConfigTypeSabloValue sabloValue, PropertyDescription pd,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		Map<String, String> map = new HashMap<>();
		map.put("filterType", sabloValue.getFilterType());
		map.put("filterDestination", sabloValue.getFilterDestination());
		return JSONUtils.toBrowserJSONFullValue(writer, key, map, null, null);
	}

	@Override
	public ValuelistConfigTypeSabloValue toSabloComponentValue(JSONObject formElementValue, PropertyDescription pd, INGFormElement formElement,
		WebFormComponent component,
		DataAdapterList dataAdapterList)
	{
		return new ValuelistConfigTypeSabloValue(formElementValue != null ? formElementValue.optString("filterType") : null,
			formElementValue != null ? formElementValue.optString("filterDestination") : null);
	}

	@Override
	public ValuelistConfigTypeSabloValue toSabloComponentDefaultValue(PropertyDescription pd, INGFormElement formElement, WebFormComponent component,
		DataAdapterList dataAdapterList)
	{
		return new ValuelistConfigTypeSabloValue(null, null);
	}


	@Override
	public boolean isValueAvailableInRhino(ValuelistConfigTypeSabloValue webComponentValue, PropertyDescription pd, IWebObjectContext webObjectContext)
	{
		return false;
	}

	@Override
	public Object toRhinoValue(ValuelistConfigTypeSabloValue webComponentValue, PropertyDescription pd, IWebObjectContext componentOrService,
		Scriptable startScriptable)
	{
		return Scriptable.NOT_FOUND;
	}

	@Override
	public ValuelistConfigTypeSabloValue toSabloComponentValue(Object rhinoValue, ValuelistConfigTypeSabloValue previousComponentValue, PropertyDescription pd,
		IWebObjectContext componentOrService)
	{
		return previousComponentValue;
	}
}