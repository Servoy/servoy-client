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
import org.json.JSONWriter;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IConvertedPropertyType;
import org.sablo.specification.property.types.DefaultPropertyType;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.server.ngclient.MediaResourcesServlet;

/**
 * Property type that can generate from a byte[] value a resource URL to be used client side.
 * Used mostly from within {@link DataproviderPropertyType} of MEDIA type.
 *
 * @author acostescu
 */
public class ByteArrayResourcePropertyType extends DefaultPropertyType<byte[]> implements IConvertedPropertyType<byte[]>
{

	public static final ByteArrayResourcePropertyType INSTANCE = new ByteArrayResourcePropertyType();
	public final static String TYPE_NAME = "byteArray"; //$NON-NLS-1$

	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

	@Override
	public byte[] fromJSON(Object newJSONValue, byte[] previousSabloValue, PropertyDescription pd, IBrowserConverterContext dataConverterContext)
	{
		return previousSabloValue; // not supported yet; currently MediaResourcesServlet is used to upload directly to dataProviders
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, byte[] sabloValue, PropertyDescription pd, DataConversion clientConversion,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		JSONUtils.addKeyIfPresent(writer, key);
		if (sabloValue != null)
		{
			writer.object();
			MediaResourcesServlet.MediaInfo mediaInfo = MediaResourcesServlet.createMediaInfo(sabloValue);
			writer.key("url").value("resources/" + MediaResourcesServlet.DYNAMIC_DATA_ACCESS + "/" + mediaInfo.getName()); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
			writer.key("contentType").value(mediaInfo.getContentType()); //$NON-NLS-1$
			writer.endObject();
		}
		else
		{
			writer.value(null);
		}
		return writer;
	}

}
