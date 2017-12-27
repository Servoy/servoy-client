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
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IClassPropertyType;
import org.sablo.specification.property.IPropertyConverterForBrowser;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.dataprocessing.IFoundSetInternal;

/**
 *
 * @author gboros
 */
public class FoundsetReferencePropertyTypeOld extends UUIDReferencePropertyType<IFoundSetInternal>
	implements IPropertyConverterForBrowser<IFoundSetInternal>, IClassPropertyType<IFoundSetInternal>
{

	public static final FoundsetReferencePropertyTypeOld INSTANCE = new FoundsetReferencePropertyTypeOld();
	public static final String TYPE_NAME = "foundsetref";

	private FoundsetReferencePropertyTypeOld()
	{
	}

	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

	@Override
	public IFoundSetInternal fromJSON(Object newValue, IFoundSetInternal previousValue, PropertyDescription pd, IBrowserConverterContext dataConverterContext,
		ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		if (newValue instanceof JSONObject)
		{
			JSONObject jsonFoundset = (JSONObject)newValue;
			return getReference(jsonFoundset.optString("foundsethash"));
		}
		return null;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, IFoundSetInternal value, PropertyDescription pd, DataConversion clientConversion,
		IBrowserConverterContext dataConverterContext) throws JSONException
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

	@Override
	public Class<IFoundSetInternal> getTypeClass()
	{
		return IFoundSetInternal.class;
	}
}
