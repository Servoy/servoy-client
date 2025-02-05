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
import org.sablo.specification.property.IPropertyWithClientSideConversions;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.IContextProvider;
import com.servoy.j2db.server.ngclient.property.FoundsetPropertyType;
import com.servoy.j2db.server.ngclient.property.FoundsetTypeSabloValue;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToTemplateJSON;
import com.servoy.j2db.util.Utils;

/**
 * @author lvostinar
 *
 */
@SuppressWarnings("nls")
public class RecordPropertyType extends UUIDReferencePropertyType<IRecordInternal> implements
	IClassPropertyType<IRecordInternal>, IFormElementToTemplateJSON<IRecordInternal, IRecordInternal>,
	IPropertyWithClientSideConversions<IRecordInternal>
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
	public IRecordInternal fromJSON(Object newJSONValue, IRecordInternal previousSabloValue, PropertyDescription pd,
		IBrowserConverterContext converterContext,
		ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		IRecordInternal record = null;
		if (newJSONValue instanceof JSONObject)
		{
			JSONObject jsonRecord = (JSONObject)newJSONValue;
			BaseWebObject webObject = converterContext.getWebObject();
			if (webObject != null && jsonRecord.has(FoundsetTypeSabloValue.ROW_ID_COL_KEY))
			{
				String rowIDValue = jsonRecord.optString(FoundsetTypeSabloValue.ROW_ID_COL_KEY);
				if (jsonRecord.has(FoundsetTypeSabloValue.FOUNDSET_ID))
				{
					int foundsetID = Utils.getAsInteger(jsonRecord.get(FoundsetTypeSabloValue.FOUNDSET_ID));
					if (foundsetID >= 0 && webObject instanceof IContextProvider)
					{
						IFoundSetInternal foundset = ((IContextProvider)webObject).getDataConverterContext().getApplication().getFoundSetManager().findFoundset(
							foundsetID);
						if (foundset != null)
						{
							int recordIndex = foundset.getRecordIndex(rowIDValue, 0);
							if (recordIndex != -1)
							{
								return foundset.getRecord(recordIndex);
							}
						}
					}
				}
				Collection<PropertyDescription> properties = webObject.getSpecification().getProperties(FoundsetPropertyType.INSTANCE);
				// FIXME: why do we check here only the root properties of webObject's model (PD)? what if the record is from a foundset property that is nested inside objects/arrays?
				// actually the following loop I think is more a fallback, as normally, client sent records will have a foundsetId and would be identified above
				// this searches in all foundset properties for a record based on it's index hint and pk hash; can we remove this loop completely?
				// so if the record was on client due to toJSON then it does have 'recordhash' which is treated after this loop; if it originates in a client side record representation
				// that normally would send the foundsetId as well...
				for (PropertyDescription foundsetPd : properties)
				{
					FoundsetTypeSabloValue fsSablo = (FoundsetTypeSabloValue)webObject.getProperty(foundsetPd.getName());
					if (fsSablo != null && fsSablo.getFoundset() != null)
					{
						int recordIndex = fsSablo.getFoundset().getRecordIndex(rowIDValue, 0);
						if (recordIndex != -1)
						{
							record = fsSablo.getFoundset().getRecord(recordIndex);
							break;
						}
					}
				}
			}
			if (record == null && jsonRecord.has("recordhash")) //$NON-NLS-1$
			{
				record = getReference(jsonRecord.optString("recordhash"), converterContext); //$NON-NLS-1$
			}

		}
		return record;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, IRecordInternal sabloValue, PropertyDescription pd, IBrowserConverterContext converterContext)
		throws JSONException
	{
		JSONUtils.addKeyIfPresent(writer, key);
		if (sabloValue != null)
		{
			writer.object();
			writer.key("recordhash").value(addReference(sabloValue, converterContext));
			writer.key(FoundsetTypeSabloValue.FOUNDSET_ID).value(sabloValue.getParentFoundSet().getID());
			IFoundSetInternal foundset = sabloValue.getParentFoundSet();
			writer.key(FoundsetTypeSabloValue.ROW_ID_COL_KEY)
				.value(foundset.isInFindMode() ? String.valueOf(foundset.getRecordIndex(sabloValue)) : sabloValue.getPKHashKey());
			writer.endObject();
		}
		else writer.value(null);
		return writer;
	}

	@Override
	public Class<IRecordInternal> getTypeClass()
	{
		return IRecordInternal.class;
	}

	@Override
	public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, IRecordInternal formElementValue, PropertyDescription pd,
		FormElementContext formElementContext)
		throws JSONException
	{
		if (formElementValue == null) return writer;
		return toJSON(writer, key, formElementValue, pd, null);
	}

	@Override
	public boolean writeClientSideTypeName(JSONWriter w, String keyToAddTo, PropertyDescription pd)
	{
		JSONUtils.addKeyIfPresent(w, keyToAddTo);
		w.value(TYPE_NAME);
		return true;
	}

}
