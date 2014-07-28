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

import java.awt.Dimension;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.sablo.specification.property.IDataConverterContext;
import org.sablo.specification.property.IWrapperType;
import org.sablo.websocket.utils.DataConversion;

import com.servoy.j2db.MediaURLStreamHandler;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.server.ngclient.IContextProvider;
import com.servoy.j2db.server.ngclient.IServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.MediaResourcesServlet;
import com.servoy.j2db.server.ngclient.property.types.MediaPropertyType.MediaWrapper;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ImageLoader;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 */
public class MediaPropertyType implements IWrapperType<Object, MediaWrapper>
{
	public static final MediaPropertyType INSTANCE = new MediaPropertyType();

	private MediaPropertyType()
	{
	}

	@Override
	public String getName()
	{
		return "media";
	}

	@Override
	public Object parseConfig(JSONObject json)
	{
		return json;
	}

	@Override
	public MediaWrapper fromJSON(Object newValue, MediaWrapper previousValue, IDataConverterContext dataConverterContext)
	{
		return wrap(newValue, previousValue, dataConverterContext);
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, MediaWrapper object, DataConversion clientConversion) throws JSONException
	{
		if (object != null) writer.value(object.mediaUrl);
		return writer;
	}

	@Override
	public MediaWrapper defaultValue()
	{
		return null;
	}

	@Override
	public Object unwrap(MediaWrapper value)
	{
		return value != null ? value.mediaId : null;
	}

	/*
	 * @see org.sablo.specification.property.IWrapperType#wrap(java.lang.Object, java.lang.Object)
	 */
	@Override
	public MediaWrapper wrap(Object value, MediaWrapper previousValue, IDataConverterContext dataConverterContext)
	{
		if (previousValue != null && Utils.equalObjects(value, previousValue.mediaUrl))
		{
			return previousValue;
		}
		IServoyDataConverterContext servoyDataConverterContext = ((IContextProvider)dataConverterContext.getWebObject()).getDataConverterContext();
		Media media = null;
		if (value instanceof Integer)
		{
			media = servoyDataConverterContext.getSolution().getMedia(((Integer)value).intValue());
		}
		else if (value instanceof String && ((String)value).toLowerCase().startsWith(MediaURLStreamHandler.MEDIA_URL_DEF))
		{
			media = servoyDataConverterContext.getSolution().getMedia(((String)value).substring(MediaURLStreamHandler.MEDIA_URL_DEF.length()));
		}
		if (media != null)
		{
			String url = "resources/" + MediaResourcesServlet.FLATTENED_SOLUTION_ACCESS + "/" + media.getRootObject().getName() + "/" + media.getName();
			Dimension imageSize = ImageLoader.getSize(media.getMediaData());
			boolean paramsAdded = false;
			if (imageSize != null)
			{
				paramsAdded = true;
				url += "?imageWidth=" + imageSize.width + "&imageHeight=" + imageSize.height;
			}
			if (servoyDataConverterContext.getApplication() != null)
			{
				Solution sc = servoyDataConverterContext.getSolution().getSolutionCopy(false);
				if (sc != null && sc.getMedia(media.getName()) != null)
				{
					if (paramsAdded) url += "&";
					else url += "?";
					url += "uuid=" + servoyDataConverterContext.getApplication().getWebsocketSession().getUuid() + "&lm:" + sc.getLastModifiedTime();
				}
			}
			return new MediaWrapper(value, url);
		}

		Debug.log("cannot convert media " + value + " using converter context " + servoyDataConverterContext);
		return null;
	}

	class MediaWrapper
	{
		Object mediaId;
		String mediaUrl;

		MediaWrapper(Object mediaId, String mediaUrl)
		{
			this.mediaId = mediaId;
			this.mediaUrl = mediaUrl;
		}
	}
}