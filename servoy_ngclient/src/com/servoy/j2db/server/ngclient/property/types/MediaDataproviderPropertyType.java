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
import org.sablo.specification.property.types.ObjectPropertyType;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.JSONUtils;
import org.sablo.websocket.utils.JSONUtils.EmbeddableJSONWriter;
import org.sablo.websocket.utils.JSONUtils.IJSONStringWithClientSideType;
import org.sablo.websocket.utils.JSONUtils.JSONStringWithClientSideType;

import com.servoy.j2db.MediaURLStreamHandler;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.types.MediaPropertyType.MediaWrapper;
import com.servoy.j2db.server.ngclient.utils.NGUtils;
import com.servoy.j2db.util.Debug;

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

	private MediaDataproviderPropertyType()
	{
	}

	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

	@Override
	public Object fromJSON(Object newJSONValue, Object previousSabloValue, PropertyDescription pd, IBrowserConverterContext dataConverterContext,
		ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		// is default object conversion enough here? it should be able to handle at least dates in media dataprovider DPs
		return JSONUtils.defaultFromJSON(newJSONValue, previousSabloValue, pd, dataConverterContext, returnValueAdjustedIncommingValue);
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
					valueForProp = new MediaWrapper(sabloValue, MediaPropertyType.getMediaUrl(sabloValue,
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

			if (detectedPD != null)
			{
				jsonValueRepresentation = JSONUtils.FullValueToJSONConverter.INSTANCE.getConvertedValueWithClientType(valueForProp,
					detectedPD, dataConverterContext, false);

				if (jsonValueRepresentation == null || jsonValueRepresentation.toJSONString() == null ||
					jsonValueRepresentation.toJSONString().trim().length() == 0)
				{
					Debug.error("A MEDIA dataprovider is not able to send itself to client... (" + detectedPD + ", " + valueForProp + ")");
					jsonValueRepresentation = new JSONStringWithClientSideType("null", null);
				}

				if (jsonValueRepresentation.getClientSideType() != null)
				{
					// as client-side this type will be stored, we want to always give/wrap using 'object' type here because for example if jsonValueRepresentation.getClientSideType() is 'Date'
					// and client-side the value assigned afterwards to the DP would be something else, the client-to-server conversion should not use "Date" but "object" instead
					EmbeddableJSONWriter ejw = new EmbeddableJSONWriter(true); // that 'true' is a workaround for allowing directly a value instead of object or array
					final IJSONStringWithClientSideType jsonValue = jsonValueRepresentation;

					JSONUtils.writeConvertedValueWithClientType(ejw, null, jsonValueRepresentation.getClientSideType(),
						() -> {
							ejw.value(jsonValue);
							return null;
						});
					jsonValueRepresentation = new JSONStringWithClientSideType(ejw.toJSONString(), ObjectPropertyType.OBJECT_TYPE_JSON_STRING);
				}
			}

		}
		else
		{
			jsonValueRepresentation = new JSONStringWithClientSideType("null", null);
		}
		return jsonValueRepresentation;
	}

}
