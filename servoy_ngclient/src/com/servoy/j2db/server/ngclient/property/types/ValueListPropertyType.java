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
import org.sablo.websocket.IForJsonConverter;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.dataprocessing.LookupListModel;
import com.servoy.j2db.server.ngclient.property.types.ValueListPropertyType.ValueListPropertyWrapper;

/**
 * @author jcompagner
 *
 */
public class ValueListPropertyType implements IWrapperType<Object, ValueListPropertyWrapper>
{

	private static final Logger log = LoggerFactory.getLogger(ValueListPropertyType.class.getCanonicalName());

	public static final ValueListPropertyType INSTANCE = new ValueListPropertyType();

	private ValueListPropertyType()
	{
	}

	@Override
	public String getName()
	{
		return "valuelist";
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
				log.error("JSONException", e);
			}
		}
		return "";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sablo.specification.property.IClassPropertyType#getTypeClass()
	 */
	@Override
	public Class<ValueListPropertyWrapper> getTypeClass()
	{
		return ValueListPropertyWrapper.class;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sablo.specification.property.IClassPropertyType#fromJSON(java.lang.Object, java.lang.Object)
	 */
	@Override
	public Object fromJSON(Object newValue, ValueListPropertyWrapper previousValue)
	{
		if (previousValue != null)
		{
			return previousValue;
		}
		return newValue;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sablo.specification.property.IClassPropertyType#toJSON(org.json.JSONWriter, java.lang.Object, org.sablo.websocket.utils.DataConversion,
	 * org.sablo.websocket.IForJsonConverter)
	 */
	@Override
	public void toJSON(JSONWriter writer, ValueListPropertyWrapper object, DataConversion clientConversion, IForJsonConverter forJsonConverter)
		throws JSONException
	{
		if (object != null)
		{
			JSONUtils.toJSONValue(writer, object.getJsonValue(), clientConversion, forJsonConverter, null);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sablo.specification.property.IPropertyType#defaultValue()
	 */
	@Override
	public ValueListPropertyWrapper defaultValue()
	{
		// TODO Auto-generated method stub
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
		//first time it creates the wrapper then , it will always be a Wrapper (skips if statement)
		if (value instanceof ValueListPropertyWrapper) return (ValueListPropertyWrapper)value;
		if (value instanceof LookupListModel)
		{
			return new ValueListPropertyWrapper(value, dataConverterContext);
		}
		return new ValueListPropertyWrapper(value, dataConverterContext);
	}

	class ValueListPropertyWrapper
	{
		final Object value;
		IDataConverterContext dataConverterContext;
		Object jsonValue;

		ValueListPropertyWrapper(Object value)
		{
			this(value, null);
		}

		ValueListPropertyWrapper(Object value, IDataConverterContext dataConverterContext)
		{
			this.value = value;
			this.dataConverterContext = dataConverterContext;
		}

		Object getJsonValue()
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


}
