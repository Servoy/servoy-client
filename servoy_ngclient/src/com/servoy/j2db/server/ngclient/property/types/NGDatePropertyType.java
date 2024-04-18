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

import java.sql.Time;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.types.DatePropertyType;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.INGFormElement;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IDesignToFormElement;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToTemplateJSON;

/**
 *
 * @author acostescu, gboros
 */
public class NGDatePropertyType extends DatePropertyType implements IDesignToFormElement<Long, Date, Date>, IFormElementToTemplateJSON<Date, Date>
{

	public static final String SVY_DATE_CLIENT_SIDE_TYPE_NAME = "svy_date";
	public final static NGDatePropertyType NG_INSTANCE = new NGDatePropertyType();

	@Override
	public Date toFormElementValue(Long designValue, PropertyDescription pd, FlattenedSolution flattenedSolution, INGFormElement formElement,
		PropertyPath propertyPath)
	{
		return fromJSON(designValue, null, pd, null, null);
	}

	@Override
	public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, Date formElementValue, PropertyDescription pd, FormElementContext formElementContext)
		throws JSONException
	{
		if (formElementValue == null) return writer;
		return toJSON(writer, key, formElementValue, pd, null);
	}


	@Override
	public Date fromJSON(Object newValue, Date previousValue, PropertyDescription pd, IBrowserConverterContext dataConverterContext,
		ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		return fromJSON(newValue, pd == null ? false : hasNoDateConversion(pd));
	}

	public Date fromJSON(Object newValue, boolean hasNoDateConversion)
	{
		if (newValue instanceof String)
		{
			String sDate = (String)newValue;
			// if no date conversion replace client time zone with server time zone
			if (hasNoDateConversion)
			{
				return Date.from(LocalDateTime.parse(sDate, DateTimeFormatter.ISO_OFFSET_DATE_TIME).atZone(ZoneId.systemDefault()).toInstant());
			}
			else
			{
				return Date.from(OffsetDateTime.parse(sDate).toInstant());
			}
		}
		return null;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, Date value, PropertyDescription pd, IBrowserConverterContext dataConverterContext)
		throws JSONException
	{
		// NOTE: pd and dataConverterContext can be null if call come from 'object' type / JSONUtils.defaultToJSONValue(...) conversions which rely on this Date type
		JSONUtils.addKeyIfPresent(writer, key);
		String sDate = toStringDate(value, pd);

		return writer.value(sDate);
	}

	public static String toStringDate(Date value, PropertyDescription pd)
	{
		String sDate;
		OffsetDateTime offsetDT;
		// java.sqlDate  seems to only be created by variable assignment where we make a new sql.Date() of a js date.
		// what the datbase returns (can also be java.sql.Time) is a question, so we can't just always assume that is also a sql date..
		Date tmp = value;
		if (tmp instanceof java.sql.Date || tmp instanceof Time)
		{
			tmp = new Date(tmp.getTime());
		}
		offsetDT = OffsetDateTime.ofInstant(tmp.toInstant(), ZoneId.systemDefault());
		// remove time zone info from sDate if no date conversion
		if (pd != null && hasNoDateConversion(pd))
		{
			sDate = offsetDT.toLocalDateTime().toString();
		}
		else
		{
			sDate = offsetDT.toString();
			if (sDate.indexOf('+') != -1)
			{
				String[] sDateA = sDate.split("\\+");
				String[] offset = sDateA[1].split(":");
				if (offset.length > 2) // seconds in offset, cut it, as it can't be handled in js
				{
					StringBuilder sDateBuilder = new StringBuilder(sDateA[0]).append('+');
					sDateBuilder.append(offset[0]).append(':').append(offset[1]);
					sDate = sDateBuilder.toString();
				}
			}
			else if (sDate.indexOf('-') != -1)
			{
				// handle timezone with -
				int index = sDate.lastIndexOf('-');
				int timeIndex = sDate.indexOf('T');
				if (index > timeIndex)
				{
					String offset = sDate.substring(index + 1);
					String[] offsetParts = offset.split(":");
					if (offsetParts.length > 2) // seconds in offset, cut it, as it can't be handled in js
					{
						StringBuilder sDateBuilder = new StringBuilder(sDate.substring(0, index)).append('-');
						sDateBuilder.append(offsetParts[0]).append(':').append(offsetParts[1]);
						sDate = sDateBuilder.toString();
					}
				}
			}
		}
		return sDate;
	}

	public static boolean hasNoDateConversion(PropertyDescription pd)
	{
		boolean hasNoDateConversion = false;
		Object pdConfig = pd.getConfig();

		if (pdConfig instanceof JSONObject)
		{
			hasNoDateConversion = ((JSONObject)pdConfig).optBoolean("useLocalDateTime");
		}
		return hasNoDateConversion;
	}

	@Override
	public boolean writeClientSideTypeName(JSONWriter w, String keyToAddTo, PropertyDescription pd)
	{
		JSONUtils.addKeyIfPresent(w, keyToAddTo);
		w.value(SVY_DATE_CLIENT_SIDE_TYPE_NAME);
		return true;
	}

}
