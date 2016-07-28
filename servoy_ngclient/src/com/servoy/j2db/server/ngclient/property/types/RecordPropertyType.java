/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

import java.util.Collection;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.sablo.BaseWebObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IClassPropertyType;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.dataprocessing.Record;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.property.FoundsetPropertyType;
import com.servoy.j2db.server.ngclient.property.FoundsetTypeSabloValue;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToTemplateJSON;
import com.servoy.j2db.util.Pair;

/**
 * @author lvostinar
 *
 */
public class RecordPropertyType extends ReferencePropertyType<Record> implements IClassPropertyType<Record>, IFormElementToTemplateJSON<Record, Record>
{
	public static final RecordPropertyType INSTANCE = new RecordPropertyType();
	public static final String TYPE_NAME = "record"; //$NON-NLS-1$

	protected RecordPropertyType()
	{
	}

	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

	@Override
	public Record fromJSON(Object newJSONValue, Record previousSabloValue, PropertyDescription pd, IBrowserConverterContext dataConverterContext,
		ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		Record record = null;
		if (newJSONValue instanceof JSONObject)
		{
			JSONObject jsonRecord = (JSONObject)newJSONValue;
			BaseWebObject webObject = dataConverterContext.getWebObject();
			if (jsonRecord.has("recordhash"))
			{
				record = getReference(jsonRecord.optInt("recordhash"));
			}
			if (record == null && webObject != null && jsonRecord.has(FoundsetTypeSabloValue.ROW_ID_COL_KEY))
			{
				String rowIDValue = jsonRecord.optString(FoundsetTypeSabloValue.ROW_ID_COL_KEY);
				Pair<String, Integer> splitHashAndIndex = FoundsetTypeSabloValue.splitPKHashAndIndex(rowIDValue);
				Collection<PropertyDescription> properties = webObject.getSpecification().getProperties(FoundsetPropertyType.INSTANCE);
				for (PropertyDescription foundsetPd : properties)
				{
					FoundsetTypeSabloValue fsSablo = (FoundsetTypeSabloValue)webObject.getProperty(foundsetPd.getName());
					int recordIndex = fsSablo.getFoundset().getRecordIndex(splitHashAndIndex.getLeft(), splitHashAndIndex.getRight().intValue());
					if (recordIndex != -1)
					{
						record = (Record)fsSablo.getFoundset().getRecord(recordIndex);
						break;
					}
				}
			}
		}
		return record;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, Record sabloValue, PropertyDescription pd, DataConversion clientConversion,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		JSONUtils.addKeyIfPresent(writer, key);
		writer.object();
		writer.key("recordhash").value(addReference(sabloValue));
		if (sabloValue != null) writer.key(FoundsetTypeSabloValue.ROW_ID_COL_KEY).value(
			sabloValue.getPKHashKey() + "_" + sabloValue.getParentFoundSet().getRecordIndex(sabloValue));
		writer.key("svyType").value(getName());
		writer.endObject();
		return writer;
	}

	@Override
	public Class<Record> getTypeClass()
	{
		return Record.class;
	}

	@Override
	public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, Record formElementValue, PropertyDescription pd,
		DataConversion browserConversionMarkers, FormElementContext formElementContext) throws JSONException
	{
		return toJSON(writer, key, formElementValue, pd, browserConversionMarkers, null);
	}
}
