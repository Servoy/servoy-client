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

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.sablo.specification.property.IClassPropertyType;
import org.sablo.specification.property.IDataConverterContext;
import org.sablo.specification.property.IPropertyConverter;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.dataprocessing.IFoundSetInternal;

/**
 *
 * @author gboros
 */
public class FoundsetReferencePropertyType extends ReferencePropertyType<IFoundSetInternal> implements IPropertyConverter<IFoundSetInternal>,
	IClassPropertyType<IFoundSetInternal>
{

	public static final FoundsetReferencePropertyType INSTANCE = new FoundsetReferencePropertyType();
	public static final String TYPE_NAME = "foundsetref";

	private FoundsetReferencePropertyType()
	{
	}

	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

	@Override
	public IFoundSetInternal fromJSON(Object newValue, IFoundSetInternal previousValue, IDataConverterContext dataConverterContext)
	{
		if (newValue instanceof JSONObject)
		{
			JSONObject jsonFoundset = (JSONObject)newValue;
			return getReference(jsonFoundset.optInt("foundsethash"));
		}
		return null;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, IFoundSetInternal value, DataConversion clientConversion, IDataConverterContext dataConverterContext)
		throws JSONException
	{
		JSONUtils.addKeyIfPresent(writer, key);
		writer.object();
		writer.key("foundsethash").value(addReference(value));
		writer.key("foundsetdatasource").value(value.getDataSource());
		writer.key("foundsetpk").value(value.getTable().getRowIdentColumnNames().next());
		writer.key("svyType").value(getName());
		writer.endObject();
		return writer;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.sablo.specification.property.IClassPropertyType#getTypeClass()
	 */
	@Override
	public Class<IFoundSetInternal> getTypeClass()
	{
		return IFoundSetInternal.class;
	}
}
