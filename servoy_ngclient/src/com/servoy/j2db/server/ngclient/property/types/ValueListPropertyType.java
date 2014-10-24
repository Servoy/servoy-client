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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.sablo.specification.property.IDataConverterContext;
import org.sablo.specification.property.IWrapperType;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.dataprocessing.LookupListModel;
import com.servoy.j2db.server.ngclient.property.types.ValueListPropertyType.ValueListPropertyWrapper;
import com.servoy.j2db.util.Debug;

/**
 * @author jcompagner
 *
 */
public class ValueListPropertyType implements IWrapperType<Object, ValueListPropertyWrapper>, ISupportTemplateValue<Object>
{

	public static final ValueListPropertyType INSTANCE = new ValueListPropertyType();
	public static final String TYPE_NAME = "valuelist";

	private ValueListPropertyType()
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
		if (json != null && json.has("for"))
		{
			try
			{
				return json.getString("for");
			}
			catch (JSONException e)
			{
				Debug.error("JSONException", e);
			}
		}
		return "";
	}

	@Override
	public ValueListPropertyWrapper fromJSON(Object newValue, ValueListPropertyWrapper previousValue, IDataConverterContext dataConverterContext)
	{
		if (previousValue != null)
		{
			return previousValue;
		}
		return wrap(newValue, previousValue, dataConverterContext); // TODO I think this is not supported actually; so it could return null instead
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, ValueListPropertyWrapper object, DataConversion clientConversion) throws JSONException
	{
		if (object != null)
		{
			// TODO we should have type info here to send instead of null for real/display values
			JSONUtils.toBrowserJSONFullValue(writer, key, object.getJsonValue(), null, clientConversion);
		}
		return writer;
	}

	@Override
	public ValueListPropertyWrapper defaultValue()
	{
		return null;
	}

	@Override
	public Object unwrap(ValueListPropertyWrapper value)
	{
		return value != null ? value.value : null;
	}

	@Override
	public ValueListPropertyWrapper wrap(Object value, ValueListPropertyWrapper previousValue, IDataConverterContext dataConverterContext)
	{
		// first time it creates the wrapper then, it will always be a Wrapper (skips if statement)
		if (value instanceof ValueListPropertyWrapper) return (ValueListPropertyWrapper)value;
//		if (value instanceof LookupListModel)
//		{
//			return new ValueListPropertyWrapper(value);
//		}
		return new ValueListPropertyWrapper(value);
	}

	class ValueListPropertyWrapper
	{

		final Object value;
		Object jsonValue;

		ValueListPropertyWrapper(Object value)
		{
			this.value = value;
		}

		Object getJsonValue() // TODO this should return TypedData<List<Map<String, Object>>> instead
		{
			if (value instanceof IValueList)
			{
				IValueList list = (IValueList)value;
				List<Map<String, Object>> array = new ArrayList<>(list.getSize());
				for (int i = 0; i < list.getSize(); i++)
				{
					Map<String, Object> map = new HashMap<String, Object>();
					map.put("realValue", list.getRealElementAt(i));
					Object displayValue = list.getElementAt(i);
					map.put("displayValue", displayValue != null ? displayValue : "");
					array.add(map);
				}
				jsonValue = array;
			}
			else if (value instanceof LookupListModel)
			{
				LookupListModel listModel = (LookupListModel)value;
				List<Map<String, Object>> array = new ArrayList<>(listModel.getSize());
				for (int i = 0; i < listModel.getSize(); i++)
				{
					Map<String, Object> map = new HashMap<String, Object>();
					map.put("realValue", listModel.getRealElementAt(i));
					Object displayValue = listModel.getElementAt(i);
					map.put("displayValue", displayValue != null ? displayValue : "");
					array.add(map);
				}
				jsonValue = array;
			}

			return jsonValue;
		}

	}

	@Override
	public boolean valueInTemplate(Object object)
	{
		return false;
	}


}
