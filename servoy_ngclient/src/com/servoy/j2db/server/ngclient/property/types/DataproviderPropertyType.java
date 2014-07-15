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

import java.util.Date;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.sablo.specification.property.IDataConverterContext;
import org.sablo.specification.property.IWrapperType;
import org.sablo.websocket.IForJsonConverter;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.server.ngclient.HTMLTagsConverter;
import com.servoy.j2db.server.ngclient.IContextProvider;
import com.servoy.j2db.server.ngclient.IServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.MediaResourcesServlet;
import com.servoy.j2db.server.ngclient.property.DataproviderConfig;
import com.servoy.j2db.server.ngclient.property.types.DataproviderPropertyType.DataproviderWrapper;
import com.servoy.j2db.util.HtmlUtils;

/**
 * @author jcompagner
 *
 */
public class DataproviderPropertyType implements IWrapperType<Object, DataproviderWrapper>
{

	public static final DataproviderPropertyType INSTANCE = new DataproviderPropertyType();

	private DataproviderPropertyType()
	{
	}

	@Override
	public String getName()
	{
		return "dataprovider";
	}

	@Override
	public Object parseConfig(JSONObject json)
	{

		String onDataChange = null;
		String onDataChangeCallback = null;
		boolean hasParseHtml = false;
		if (json != null)
		{
			JSONObject onDataChangeObj = json.optJSONObject("ondatachange");
			if (onDataChangeObj != null)
			{
				onDataChange = onDataChangeObj.optString("onchange", null);
				onDataChangeCallback = onDataChangeObj.optString("callback", null);
			}
			hasParseHtml = json.optBoolean("parsehtml");
		}

		return new DataproviderConfig(onDataChange, onDataChangeCallback, hasParseHtml);
	}

	@Override
	public Class<DataproviderWrapper> getTypeClass()
	{
		return DataproviderWrapper.class;
	}

	@Override
	public Object fromJSON(Object newValue, DataproviderWrapper previousValue)
	{
		/** TODO if the dataprovider is of type date it has to apply date converter 
		   it should do(after andrei finishes his case SVY-6608 ) something like :
		    // get type from DataProviderLookup
		    if (type == IColumnTypes.DATETIME)
		    {
		     IPropertyType< ? > sabloType = TypesRegistry.getType("date");
		     value = ((IClassPropertyType<Date, Date>)sabloType).fromJSON(value, null);
		    }
		 **/
		if (previousValue != null)
		{
			if (previousValue.value instanceof Date)
			{
				return new Date((long)newValue);
			}
		}
		return newValue;
	}

	@Override
	public void toJSON(JSONWriter writer, DataproviderWrapper object, DataConversion clientConversion, IForJsonConverter forJsonConverter) throws JSONException
	{
		if (object != null)
		{
			JSONUtils.toJSONValue(writer, object.getJsonValue(), clientConversion, forJsonConverter, null);
		}
	}

	@Override
	public DataproviderWrapper defaultValue()
	{
		return null;
	}

	@Override
	public Object unwrap(DataproviderWrapper value)
	{
		return value != null ? value.value : null;
	}

	/*
	 * @see org.sablo.specification.property.IWrapperType#wrap(java.lang.Object, java.lang.Object, org.sablo.specification.property.IDataConverterContext)
	 */
	@Override
	public DataproviderWrapper wrap(Object value, DataproviderWrapper previousValue, IDataConverterContext dataConverterContext)
	{
		return new DataproviderWrapper(value, dataConverterContext);
	}

	class DataproviderWrapper
	{
		final Object value;
		IDataConverterContext dataConverterContext;
		Object jsonValue;

		DataproviderWrapper(Object value)
		{
			this(value, null);
		}

		DataproviderWrapper(Object value, IDataConverterContext dataConverterContext)
		{
			this.value = value;
			this.dataConverterContext = dataConverterContext;
		}

		Object getJsonValue()
		{
			if (jsonValue == null)
			{
				if (value instanceof byte[])
				{
					jsonValue = new HashMap<String, Object>();
					MediaResourcesServlet.MediaInfo mediaInfo = MediaResourcesServlet.getMediaInfo((byte[])value);
					((HashMap<String, Object>)jsonValue).put("url", "resources/" + MediaResourcesServlet.DYNAMIC_DATA_ACCESS + "/" + mediaInfo.getName());
					((HashMap<String, Object>)jsonValue).put("contentType", mediaInfo.getContentType());
				}
				else if (HtmlUtils.startsWithHtml(value) && dataConverterContext != null)
				{
					IServoyDataConverterContext servoyDataConverterContext = ((IContextProvider)dataConverterContext.getWebObject()).getDataConverterContext();
					jsonValue = HTMLTagsConverter.convert(value.toString(), servoyDataConverterContext,
						((DataproviderConfig)dataConverterContext.getPropertyDescription().getConfig()).hasParseHtml());
				}
				else
				{
					jsonValue = value;
				}
			}

			return jsonValue;
		}

		@Override
		public boolean equals(Object o)
		{
			if (value == null) return ((o instanceof DataproviderWrapper) && (((DataproviderWrapper)o).value == null));
			if (o instanceof DataproviderWrapper) return value.equals(((DataproviderWrapper)o).value);
			return false;
		}
	}
}
