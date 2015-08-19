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
import org.sablo.specification.property.IConvertedPropertyType;
import org.sablo.specification.property.types.DefaultPropertyType;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.server.ngclient.HTMLTagsConverter;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.util.HtmlUtils;

/**
 * Property type that can work with a normal string, but if the String is HTML, it will alter it's value (interprets javascript callbacks/media with blobloader)
 * Used mostly from within {@link DataproviderPropertyType} of type TEXT.
 *
 * @author acostescu
 */
public class HTMLStringPropertyType extends DefaultPropertyType<String> implements IConvertedPropertyType<String>
{

	public static final HTMLStringPropertyType INSTANCE = new HTMLStringPropertyType();
	public final static String TYPE_NAME = "HTMLString"; //$NON-NLS-1$
	public static final String CONFIG_OPTION_PARSEHTML = "parsehtml";

	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

	@Override
	public Object parseConfig(JSONObject config)
	{
		return Boolean.valueOf(config.optBoolean(CONFIG_OPTION_PARSEHTML, false));
	}

	@Override
	public String fromJSON(Object newJSONValue, String previousSabloValue, PropertyDescription pd, IBrowserConverterContext dataConverterContext)
	{
		return (String)newJSONValue;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, String sabloValue, PropertyDescription pd, DataConversion clientConversion,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		JSONUtils.addKeyIfPresent(writer, key);
		if (HtmlUtils.startsWithHtml(sabloValue))
		{
			writer.value(HTMLTagsConverter.convert(sabloValue, ((WebFormComponent)dataConverterContext.getWebObject()).getDataConverterContext(),
				((Boolean)pd.getConfig()).booleanValue()));
		}
		else
		{
			writer.value(sabloValue);
		}
		return writer;
	}

}
