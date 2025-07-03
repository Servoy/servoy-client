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
import java.util.Map.Entry;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.PropertyDescriptionBuilder;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IConvertedPropertyType;
import org.sablo.specification.property.IPropertyCanDependsOn;
import org.sablo.specification.property.IPropertyType;
import org.sablo.specification.property.IPropertyWithClientSideConversions;
import org.sablo.specification.property.IWrapperType;
import org.sablo.specification.property.IWrappingContext;
import org.sablo.specification.property.WrappingContext;
import org.sablo.specification.property.types.DefaultPropertyType;
import org.sablo.specification.property.types.TypesRegistry;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.JSONUtils;
import org.sablo.websocket.utils.JSONUtils.EmbeddableJSONWriter;
import org.sablo.websocket.utils.JSONUtils.FullValueToJSONConverter;

import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyJSONObject;

/**
 * This type is helpful for more advanced components like servoy-extra-components treeview or dataset based ng grid.<br/>
 * It is documented on wiki so others might/will likely use it too.
 *
 * @author acostescu
 */
public class DatasetPropertyType extends DefaultPropertyType<IDataSet> implements IConvertedPropertyType<IDataSet>,
	IPropertyWithClientSideConversions<IDataSet>, IPropertyCanDependsOn
{

	public static final DatasetPropertyType INSTANCE = new DatasetPropertyType();
	public static final String TYPE_NAME = "dataset";
	private static final String VALUE_KEY = "v";
	private static final String TYPES_KEY = "t";
	private static final String INCLUDES_COLUMN_NAMES_KEY = "i";
	
	private String[] dependencies;

	private DatasetPropertyType()
	{
	}

	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

	@Override
	public IDataSet fromJSON(Object newValue, IDataSet previousValue, PropertyDescription pd, IBrowserConverterContext dataConverterContext,
		ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		// ?
		return null;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, IDataSet value, PropertyDescription propertyDescription,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		JSONUtils.addKeyIfPresent(writer, key);
		if (value == null)
		{
			return writer.value(null);
		}

		DatasetConfig datasetConfig = (DatasetConfig)propertyDescription.getConfig();

		writer.object();
		if (datasetConfig.isIncludeColumnNames()) writer.key(INCLUDES_COLUMN_NAMES_KEY).value(true);
		writer.key(VALUE_KEY).array();
		HashMap<Integer, EmbeddableJSONWriter> columnTypes = null;

		if (value.getColumnCount() > 0)
		{
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
					Object v;
					pd = datasetConfig.getColumnType(columnNames[j]);
					if (pd != null)
					{
						if (pd.getType() instanceof IWrapperType< ? , ? >)
						{
							// this will probably only happen for MediaPropertyType here
							IWrappingContext c = (dataConverterContext instanceof IWrappingContext ? (IWrappingContext)dataConverterContext
								: new WrappingContext(dataConverterContext.getWebObject(), pd.getName()));
							v = ((IWrapperType<Object, ? >)pd.getType()).wrap(row[j], null, pd, c);
						}
						else
						{
							v = row[j];
						}

						// see if it this type also needs client side conversions
						if (pd.getType() instanceof IPropertyWithClientSideConversions< ? >) // no use checking here IPropertyConverterForBrowserWithDynamicClientType as dataset won't have those
						{
							if (columnTypes == null || !columnTypes.containsKey(Integer.valueOf(j)))
							{
								EmbeddableJSONWriter clientSideType = JSONUtils.getClientSideTypeJSONString(pd);
								if (clientSideType != null)
								{
									if (columnTypes == null) columnTypes = new HashMap<>();
									columnTypes.put(Integer.valueOf(j), clientSideType);
								}
							}
						}
					}
					else v = row[j];

					// if we have a PD with to client json conversions from .spec for this column it will use it, otherwise it will just use default conversions (that work with dates as well)
					FullValueToJSONConverter.INSTANCE.toJSONValue(writer, null, v, pd, null);
				}

				writer.endArray();
			}

		}

		writer.endArray();

		// see if any of the column types defined in .spec need client-side conversions (probably only dates can be in datasets and need this - if they are set as such in .spec; if they would not be specified in .spec then defaultToJSON will write the with types anyway)
		if (columnTypes != null)
		{
			writer.key(TYPES_KEY).object();
			for (Entry<Integer, EmbeddableJSONWriter> te : columnTypes.entrySet())
			{
				writer.key(String.valueOf(te.getKey())).value(te.getValue());
			}
			writer.endObject();
		}
		writer.endObject();

		return writer;
	}

	@Override
	public Object parseConfig(JSONObject config)
	{
		boolean includeColumnNames = false;
		HashMap<String, PropertyDescription> columnTypes = new HashMap<String, PropertyDescription>();

		if (config != null)
		{
			dependencies = getDependencies(config, dependencies);
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
						if (pt != null) columnTypes.put(propertyName, new PropertyDescriptionBuilder().withName(propertyName).withType(pt).build());
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

	@Override
	public boolean writeClientSideTypeName(JSONWriter w, String keyToAddTo, PropertyDescription pd)
	{
		JSONUtils.addKeyIfPresent(w, keyToAddTo);
		w.value(TYPE_NAME);
		return true;
	}
	
	@Override
	public String[] getDependencies()
	{
		return dependencies;
	}

}
