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
import org.sablo.specification.property.IConvertedPropertyType;
import org.sablo.specification.property.IDataConverterContext;
import org.sablo.websocket.utils.DataConversion;

import com.servoy.j2db.persistence.Form;

/**
 * @author jcompagner
 */
public class FormPropertyType implements IConvertedPropertyType<Form>
{

	public static final FormPropertyType INSTANCE = new FormPropertyType();

	private FormPropertyType()
	{
	}

	@Override
	public String getName()
	{
		return "form";
	}

	@Override
	public Object parseConfig(JSONObject json)
	{
		return json;
	}

	@Override
	public Form defaultValue()
	{
		return null;
	}

	@Override
	public Form fromJSON(Object newValue, Form previousValue, IDataConverterContext dataConverterContext)
	{
		// ?
		return null;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, Form form, DataConversion clientConversion) throws JSONException
	{
		return writer.value(form != null ? form.getName() : null);
	}

}
