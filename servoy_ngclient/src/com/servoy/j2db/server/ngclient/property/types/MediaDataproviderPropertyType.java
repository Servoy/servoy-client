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

import java.util.Date;

import org.json.JSONException;
import org.json.JSONString;
import org.json.JSONWriter;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IConvertedPropertyType;
import org.sablo.specification.property.IPropertyConverterForBrowserWithDynamicClientType;
import org.sablo.specification.property.types.DefaultPropertyType;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.JSONUtils;
import org.sablo.websocket.utils.JSONUtils.EmbeddableJSONWriter;
import org.sablo.websocket.utils.JSONUtils.IJSONStringWithClientSideType;
import org.sablo.websocket.utils.JSONUtils.JSONStringWithClientSideType;

import com.servoy.j2db.MediaURLStreamHandler;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.types.MediaPropertyType.MediaWrapper;
import com.servoy.j2db.server.ngclient.utils.NGUtils;

/**
 * Property type that can work with any dataprovider type (number, text, date) and is able to handle byte arrays as URLs.
 * Used from within {@link DataproviderPropertyType} of MEDIA type.
 *
 * @author acostescu
 */
@SuppressWarnings("nls")
public class MediaDataproviderPropertyType extends DefaultPropertyType<Object>
	implements IConvertedPropertyType<Object>, IPropertyConverterForBrowserWithDynamicClientType<Object>
{

	public static final MediaDataproviderPropertyType INSTANCE = new MediaDataproviderPropertyType();
	public final static String TYPE_NAME = "mediaDataprovider"; //$NON-NLS-1$

	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

	@Override
	public Object fromJSON(Object newJSONValue, Object previousSabloValue, PropertyDescription pd, IBrowserConverterContext dataConverterContext,
		ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		// TODO how do we know that client wants to set a Date if dp type is MEDIA? (newJSONValue would be the millis number in that case)
		// maybe some typeHint in spec config for the DP or let the client send conversion info hint?
		return newJSONValue;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, Object sabloValue, PropertyDescription pd, IBrowserConverterContext dataConverterContext)
		throws JSONException
	{
		JSONUtils.addKeyIfPresent(writer, key);
		IJSONStringWithClientSideType jsonValue = toJSONInternal(sabloValue, pd, dataConverterContext);

		if (jsonValue.getClientSideType() != null)
		{
			JSONUtils.writeConvertedValueWithClientType(writer, key, jsonValue.getClientSideType(), () -> {
				writer.value(jsonValue);
				return null;
			});
		}
		else writer.value(jsonValue);

		return writer;
	}

	@Override
	public JSONString toJSONWithDynamicClientSideType(JSONWriter writer, Object sabloValue, PropertyDescription pd,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		IJSONStringWithClientSideType jsonValue = toJSONInternal(sabloValue, pd, dataConverterContext);

		writer.value(jsonValue);
		return jsonValue.getClientSideType();
	}

	private IJSONStringWithClientSideType toJSONInternal(Object sabloValue, PropertyDescription pd, IBrowserConverterContext dataConverterContext)
		throws JSONException
	{
		IJSONStringWithClientSideType jsonValueRepresentation = null;
		Object valueForProp = sabloValue;

		if (sabloValue != null)
		{
			PropertyDescription detectedPD;

			if (sabloValue.getClass().isArray() && sabloValue.getClass().getComponentType() == byte.class)
			{
				detectedPD = NGUtils.MEDIA_DATAPROVIDER_BYTE_ARRAY_CACHED_PD;
			}
			else if (sabloValue instanceof Date)
			{
				detectedPD = NGUtils.DATE_DATAPROVIDER_CACHED_PD;
			}
			else if (sabloValue instanceof String)
			{
				if (((String)sabloValue).toLowerCase().startsWith(MediaURLStreamHandler.MEDIA_URL_DEF))
				{
					valueForProp = new MediaWrapper(sabloValue, MediaPropertyType.INSTANCE.getMediaUrl(sabloValue,
						((WebFormComponent)dataConverterContext.getWebObject()).getDataConverterContext().getApplication().getFlattenedSolution(), null));
					detectedPD = NGUtils.MEDIA_CACHED_PD;
				}
				else if (Boolean.TRUE.equals(pd.getConfig())) // see how NGUtils.TEXT_PARSEHTML_DATAPROVIDER_CACHED_PD is created
				{
					detectedPD = NGUtils.TEXT_PARSEHTML_DATAPROVIDER_CACHED_PD;
				}
				else // see how NGUtils.TEXT_NO_PARSEHTML_DATAPROVIDER_CACHED_PD is created
				{
					detectedPD = NGUtils.TEXT_NO_PARSEHTML_DATAPROVIDER_CACHED_PD;
				}
			}
			else
			{
				// other primitive types
				detectedPD = null;
				EmbeddableJSONWriter ejw = new EmbeddableJSONWriter(true);
				ejw.value(sabloValue);
				jsonValueRepresentation = new JSONStringWithClientSideType(ejw.toJSONString(), null);
			}

			if (detectedPD != null) jsonValueRepresentation = JSONUtils.getFullConvertedValueWithClientType(valueForProp, detectedPD, dataConverterContext);

		}
		else
		{
			jsonValueRepresentation = new JSONStringWithClientSideType("null", null);
		}
		return jsonValueRepresentation;
	}

}
