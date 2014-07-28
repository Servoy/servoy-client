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
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.sablo.specification.property.IConvertedPropertyType;
import org.sablo.specification.property.IDataConverterContext;
import org.sablo.websocket.ConversionLocation;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.persistence.Column;

/**
 * TODO is format really mapped on a special object (like ParsedFormat)
 * maybe this should go into Servoy
 * @author jcompagner
 *
 */
public class FormatPropertyType implements IConvertedPropertyType<ComponentFormat>
{

	private static final Logger log = LoggerFactory.getLogger(FormatPropertyType.class.getCanonicalName());

	public static final FormatPropertyType INSTANCE = new FormatPropertyType();

	private FormatPropertyType()
	{
	}

	@Override
	public String getName()
	{
		return "format";
	}

	@Override
	public Object parseConfig(JSONObject json)
	{

		if (json != null && json.has("for"))
		{
			try
			{
				return json.getString("for");
			}
			catch (JSONException e)
			{
				log.error("JSONException", e);
			}
		}
		return "";
	}

	@Override
	public ComponentFormat defaultValue()
	{
		return null;
	}

	@Override
	public ComponentFormat fromJSON(Object newValue, ComponentFormat previousValue, IDataConverterContext dataConverterContext)
	{
		// ?
		return null;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, ComponentFormat format, DataConversion clientConversion) throws JSONException
	{
		if (format == null) return writer.value(null);

		Map<String, Object> map = new HashMap<>();
		String type = Column.getDisplayTypeString(format.uiType);
		if (type.equals("INTEGER")) type = "NUMBER";
		map.put("type", type);

		boolean isMask = format.parsedFormat.isMask();
		String mask = format.parsedFormat.getEditFormat();
		if (isMask && type.equals("DATETIME"))
		{
			mask = format.parsedFormat.getDateMask();
		}
		else if (format.parsedFormat.getDisplayFormat() != null && type.equals("TEXT"))
		{
			isMask = true;
			mask = format.parsedFormat.getDisplayFormat();
		}
		String placeHolder = null;
		if (format.parsedFormat.getPlaceHolderString() != null) placeHolder = format.parsedFormat.getPlaceHolderString();
		else if (format.parsedFormat.getPlaceHolderCharacter() != 0) placeHolder = Character.toString(format.parsedFormat.getPlaceHolderCharacter());
		map.put("isMask", Boolean.valueOf(isMask));
		map.put("edit", mask);
		map.put("placeHolder", placeHolder);
		map.put("allowedCharacters", format.parsedFormat.getAllowedCharacters());
		map.put("display", format.parsedFormat.getDisplayFormat());

		return JSONUtils.toJSONValue(writer, map, null, clientConversion, ConversionLocation.BROWSER);
	}
}
