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
import java.util.Arrays;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.sablo.specification.property.IConvertedPropertyType;
import org.sablo.specification.property.IDataConverterContext;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.dataprocessing.IDataSet;

/**
 * Currently this type is only used when set from scripting - for setValueListItems api call, but in the future
 * it could be used in other places as well.
 *
 * @author acostescu
 */
public class DatasetPropertyType implements IConvertedPropertyType<IDataSet>
{

	public static final DatasetPropertyType INSTANCE = new DatasetPropertyType();
	public static final String TYPE_NAME = "dataset";

	private DatasetPropertyType()
	{
	}

	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

	@Override
	public IDataSet fromJSON(Object newValue, IDataSet previousValue, IDataConverterContext dataConverterContext)
	{
		// ?
		return null;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, IDataSet value, DataConversion clientConversion) throws JSONException
	{
		if (value == null)
		{
			JSONUtils.addKeyIfPresent(writer, key);
			return writer.value(null);
		}

		List<List<Object>> array = new ArrayList<>(value.getRowCount());
		if (value.getColumnCount() >= 1)
		{
			for (int i = 0; i < value.getRowCount(); i++)
			{
				Object[] row = value.getRow(i);
				array.add(Arrays.asList(row));
			}
		}

		return JSONUtils.toBrowserJSONValue(writer, key, array, null, clientConversion);
	}

	@Override
	public Object parseConfig(JSONObject config)
	{
		return config;
	}

	@Override
	public IDataSet defaultValue()
	{
		return null;
	}

}
