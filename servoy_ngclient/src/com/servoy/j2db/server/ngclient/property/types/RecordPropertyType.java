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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IClassPropertyType;
import org.sablo.specification.property.IDataConverterContext;
import org.sablo.specification.property.types.DefaultPropertyType;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.dataprocessing.Record;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToTemplateJSON;

/**
 * @author lvostinar
 *
 */
public class RecordPropertyType extends DefaultPropertyType<Record> implements IClassPropertyType<Record>, IFormElementToTemplateJSON<Record, Record>
{
	public static final RecordPropertyType INSTANCE = new RecordPropertyType();
	public static final String TYPE_NAME = "record"; //$NON-NLS-1$
	private final List<WeakReference<Record>> allRecords = new ArrayList<WeakReference<Record>>();

	protected RecordPropertyType()
	{
	}

	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

	@Override
	public Record fromJSON(Object newJSONValue, Record previousSabloValue, IDataConverterContext dataConverterContext)
	{
		if (newJSONValue instanceof JSONObject)
		{
			JSONObject jsonRecord = (JSONObject)newJSONValue;
			int recordHashCode = jsonRecord.optInt("recordhash");
			if (recordHashCode > 0)
			{
				for (int i = 0; i <= allRecords.size(); i++)
				{
					WeakReference<Record> wr = allRecords.get(i);
					Record record = wr.get();
					if (record != null && record.hashCode() == recordHashCode)
					{
						return record;
					}
				}
			}

		}
		return null;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, Record sabloValue, DataConversion clientConversion, IDataConverterContext dataConverterContext)
		throws JSONException
	{
		JSONUtils.addKeyIfPresent(writer, key);
		writer.object();
		writer.key("recordhash").value(sabloValue.hashCode());
		writer.key("svyType").value(getName());
		writer.endObject();
		WeakReference<Record> wf = new WeakReference<Record>(sabloValue);
		if (!allRecords.contains(wf))
		{
			allRecords.add(wf);
		}
		return writer;
	}

	@Override
	public Class<Record> getTypeClass()
	{
		return Record.class;
	}

	@Override
	public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, Record formElementValue, PropertyDescription pd,
		DataConversion browserConversionMarkers, FlattenedSolution fs, FormElementContext formElementContext) throws JSONException
	{
		return toJSON(writer, key, formElementValue, browserConversionMarkers, null);
	}
}
