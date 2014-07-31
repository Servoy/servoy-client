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
import org.sablo.specification.property.IDataConverterContext;
import org.sablo.specification.property.IWrapperType;
import org.sablo.websocket.utils.DataConversion;

import com.servoy.j2db.server.ngclient.HTMLTagsConverter;
import com.servoy.j2db.server.ngclient.IServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.NGClientForJsonConverter;
import com.servoy.j2db.server.ngclient.property.types.TagStringPropertyType.TagStringWrapper;
import com.servoy.j2db.util.HtmlUtils;


/**
 * @author jcompagner
 *
 */
public class TagStringPropertyType implements IWrapperType<Object, TagStringWrapper>
{

	public static final TagStringPropertyType INSTANCE = new TagStringPropertyType();

	private TagStringPropertyType()
	{
	}

	@Override
	public String getName()
	{
		return "tagstring";
	}

	@Override
	public Object parseConfig(JSONObject json)
	{
		return Boolean.valueOf(json != null && json.optBoolean("hidetags"));
	}

	@Override
	public TagStringWrapper fromJSON(Object newValue, TagStringWrapper previousValue, IDataConverterContext dataConverterContext)
	{
		return wrap(newValue, previousValue, dataConverterContext);
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, TagStringWrapper object, DataConversion clientConversion) throws JSONException
	{
		if (object != null) writer.value(object.getJsonValue());
		return writer;
	}

	@Override
	public TagStringWrapper defaultValue()
	{
		return null;
	}

	@Override
	public Object unwrap(TagStringWrapper value)
	{
		return value != null ? value.value : null;
	}

	@Override
	public TagStringWrapper wrap(Object value, TagStringWrapper previousValue, IDataConverterContext dataConverterContext)
	{
		return new TagStringWrapper(value, dataConverterContext);
	}

	class TagStringWrapper
	{
		final Object value;
		IServoyDataConverterContext dataConverterContext;
		Object jsonValue;

		TagStringWrapper(Object value)
		{
			this(value, null);
		}

		TagStringWrapper(Object value, IDataConverterContext dataConverterContext)
		{
			this.value = value;
			this.dataConverterContext = NGClientForJsonConverter.getServoyConverterContext(dataConverterContext);
		}

		Object getJsonValue()
		{
			if (jsonValue == null)
			{
				if (HtmlUtils.startsWithHtml(value))
				{
					if (dataConverterContext != null && dataConverterContext.getSolution() != null && dataConverterContext.getApplication() != null)
					{
						jsonValue = HTMLTagsConverter.convert(value.toString(), dataConverterContext, false);
					}
					else
					{
						// TODO - it could still return "value" if we know HTMLTagsConverter.convert() would not want to touch that (so simple HTML)
						// design-time wrap (used by FormElement); no component available
						// return empty value as we don't want to expose the actual design-time stuff that would normally get encrypted by HTMLTagsConverter.convert() or is not yet valid (blobloader without an application instance for example).
						return "<html></html>";
					}
				}
				else
				{
					jsonValue = value;
				}
			}

			return jsonValue;
		}
	}
}
