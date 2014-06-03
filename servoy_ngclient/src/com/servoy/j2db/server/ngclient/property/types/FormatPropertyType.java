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
import org.sablo.specification.property.types.DefaultPropertyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO is format really mapped on a special object (like ParsedFormat)
 * maybe this should go into Servoy
 * @author jcompagner
 *
 */
public class FormatPropertyType extends DefaultPropertyType<String> {

	private static final Logger log = LoggerFactory.getLogger(FormatPropertyType.class.getCanonicalName());
	
	public static final FormatPropertyType INSTANCE = new FormatPropertyType();
	
	private FormatPropertyType() {
	}
	
	@Override
	public String getName() {
		return "format";
	}

	@Override
	public Object parseConfig(JSONObject json) {

		if (json != null && json.has("for"))
		{
			try
			{
				return json.getString("for");
			}
			catch (JSONException e)
			{
				log.error("JSONException",e);
			}
		}
		return "";
	}
}
