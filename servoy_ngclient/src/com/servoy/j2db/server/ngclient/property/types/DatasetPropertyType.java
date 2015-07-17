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

import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IConvertedPropertyType;
import org.sablo.specification.property.IDataConverterContext;
import org.sablo.specification.property.IPropertyType;
import org.sablo.specification.property.IWrapperType;
import org.sablo.specification.property.types.DefaultPropertyType;
import org.sablo.specification.property.types.TypesRegistry;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;
import org.sablo.websocket.utils.JSONUtils.FullValueToJSONConverter;

import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyJSONObject;

/**
 * Currently this type is only used when set from scripting - for setValueListItems api call, but in the future
 * it could be used in other places as well.
 *
 * @author acostescu
 */
public class DatasetPropertyType extends DefaultPropertyType<IDataSet> implements IConvertedPropertyType<IDataSet>
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
	public JSONWriter toJSON(JSONWriter writer, String key, IDataSet value, DataConversion clientConversion, IDataConverterContext dataConverterContext)
		throws JSONException
	{
		JSONUtils.addKeyIfPresent(writer, key);
		if (value == null)
		{
			return writer.value(null);
		}

		writer.array();

		if (value.getColumnCount() > 0)
		{
			DatasetConfig datasetConfig = (DatasetConfig)dataConverterContext.getPropertyDescription().getConfig();
			String[] columnNames = value.getColumnNames();

			if (datasetConfig.isIncludeColumnNames() && columnNames != null)
			{
				writer.array();

				for (String columnName : columnNames)
				{
					writer.value(columnName);
				}

				writer.endArray();
			}

			for (int i = 0; i < value.getRowCount(); i++)
			{
				writer.array();

				Object[] row = value.getRow(i);
				PropertyDescription pd;
				for (int j = 0; j < row.length; j++)
				{
					pd = datasetConfig.getColumnType(columnNames[j]);
					if (pd != null)
					{
						Object v;
						if (pd.getType() instanceof IWrapperType< ? , ? >)
						{
							v = ((IWrapperType<Object, ? >)pd.getType()).wrap(row[j], null, dataConverterContext);
						}
						else
						{
							v = row[j];
						}
						FullValueToJSONConverter.INSTANCE.toJSONValue(writer, null, v, pd, clientConversion, null);
					}
					else
					{
						writer.value(row[j]);
					}
				}

				writer.endArray();
			}
		}

		writer.endArray();

		return writer;
	}

	@Override
	public Object parseConfig(JSONObject config)
	{
		boolean includeColumnNames = false;
		HashMap<String, PropertyDescription> columnTypes = new HashMap<String, PropertyDescription>();

		if (config != null)
		{
			includeColumnNames = config.optBoolean("includeColumnNames");
			JSONObject columnTypesObj = config.optJSONObject("columnTypes");
			if (columnTypesObj != null)
			{
				String[] names = ServoyJSONObject.getNames(columnTypesObj);
				for (String propertyName : names)
				{
					try
					{
						IPropertyType< ? > pt = TypesRegistry.getType(columnTypesObj.get(propertyName).toString());
						if (pt != null) columnTypes.put(propertyName, new PropertyDescription(propertyName, pt));
					}
					catch (JSONException ex)
					{
						Debug.error(ex);
					}
				}
			}
		}

		return new DatasetConfig(includeColumnNames, columnTypes);
	}
}
