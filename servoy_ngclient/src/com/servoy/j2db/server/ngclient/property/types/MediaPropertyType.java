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
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IWrapperType;
import org.sablo.specification.property.IWrappingContext;
import org.sablo.specification.property.WrappingContext;
import org.sablo.specification.property.types.DefaultPropertyType;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.MediaURLStreamHandler;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.scripting.solutionmodel.JSMedia;
import com.servoy.j2db.scripting.solutionmodel.JSWebComponent;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.IContextProvider;
import com.servoy.j2db.server.ngclient.INGApplication;
import com.servoy.j2db.server.ngclient.IServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.MediaResourcesServlet;
import com.servoy.j2db.server.ngclient.property.types.MediaPropertyType.MediaWrapper;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToTemplateJSON;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IRhinoDesignConverter;
import com.servoy.j2db.util.ImageLoader;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 */
public class MediaPropertyType extends DefaultPropertyType<Object>
	implements IWrapperType<Object, MediaWrapper>, ISupportTemplateValue<Object>, IFormElementToTemplateJSON<Object, Object>, IRhinoDesignConverter
{
	public static final MediaPropertyType INSTANCE = new MediaPropertyType();
	public static final String TYPE_NAME = "media";

	private MediaPropertyType()
	{
	}

	@Override
	public boolean valueInTemplate(Object object, PropertyDescription pd, FormElementContext formElementContext)
	{
		return false;
	}

	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

	@Override
	public Object parseConfig(JSONObject json)
	{
		return json;
	}

	@Override
	public MediaWrapper fromJSON(Object newValue, MediaWrapper previousValue, PropertyDescription propertyDescription,
		IBrowserConverterContext dataConverterContext, ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		if (dataConverterContext instanceof IWrappingContext) return wrap(newValue, previousValue, propertyDescription, (IWrappingContext)dataConverterContext);
		else return wrap(newValue, previousValue, propertyDescription, new WrappingContext(dataConverterContext.getWebObject(), propertyDescription.getName()));
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, MediaWrapper object, PropertyDescription propertyDescription, DataConversion clientConversion,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		if (object != null)
		{
			JSONUtils.addKeyIfPresent(writer, key);
			writer.value(object.mediaUrl);
		}
		return writer;
	}

	@Override
	public Object unwrap(MediaWrapper value)
	{
		return value != null ? value.mediaId : null;
	}

	@Override
	public MediaWrapper wrap(Object value, MediaWrapper previousValue, PropertyDescription propertyDescription, IWrappingContext dataConverterContext)
	{
		if (previousValue != null && Utils.equalObjects(value, previousValue.mediaUrl))
		{
			return previousValue;
		}
		IServoyDataConverterContext servoyDataConverterContext = ((IContextProvider)dataConverterContext.getWebObject()).getDataConverterContext();
		FlattenedSolution flattenedSolution = servoyDataConverterContext.getSolution();
		INGApplication application = servoyDataConverterContext.getApplication();

		String url = getMediaUrl(value, flattenedSolution, application);

		if (url != null) return new MediaWrapper(value, url);

		Debug.log("cannot convert media " + value + " using converter context " + servoyDataConverterContext);
		return null;
	}

	public String getMediaUrl(Object value, FlattenedSolution flattenedSolution, INGApplication application)
	{
		String url = null;
		Media media = null;
		if (value instanceof CharSequence)
		{
			value = ((CharSequence)value).toString();
		}
		if (value instanceof Integer)
		{
			media = flattenedSolution.getMedia(((Integer)value).intValue());
		}
		else if (value instanceof String && ((String)value).toLowerCase().startsWith(MediaURLStreamHandler.MEDIA_URL_DEF))
		{
			media = flattenedSolution.getMedia(((String)value).substring(MediaURLStreamHandler.MEDIA_URL_DEF.length()));
		}
		else
		{
			UUID uuid = Utils.getAsUUID(value, false);
			if (uuid != null) media = (Media)flattenedSolution.searchPersist(uuid);
		}
		if (media != null)
		{
			url = "resources/" + MediaResourcesServlet.FLATTENED_SOLUTION_ACCESS + "/" + media.getRootObject().getName() + "/" + media.getName();
			Dimension imageSize = ImageLoader.getSize(media.getMediaData());
			boolean paramsAdded = false;
			if (imageSize != null)
			{
				paramsAdded = true;
				url += "?imageWidth=" + imageSize.width + "&imageHeight=" + imageSize.height;
			}
			if (application != null)
			{
				Solution sc = flattenedSolution.getSolutionCopy(false);
				if (sc != null && sc.getMedia(media.getName()) != null)
				{
					if (paramsAdded) url += "&";
					else url += "?";
					url += "uuid=" + application.getWebsocketSession().getUuid() + "&lm:" + sc.getLastModifiedTime();
				}
			}
		}
		return url;
	}

	static class MediaWrapper
	{
		Object mediaId;
		String mediaUrl;

		MediaWrapper(Object mediaId, String mediaUrl)
		{
			this.mediaId = mediaId;
			this.mediaUrl = mediaUrl;
		}
	}

	@Override
	public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, Object formElementValue, PropertyDescription pd,
		DataConversion browserConversionMarkers, FormElementContext formElementContext) throws JSONException
	{
		FlattenedSolution fs = formElementContext.getFlattenedSolution();
		if (fs != null)
		{
			String url = getMediaUrl(formElementValue, fs, null);
			if (url != null)
			{
				return toJSON(writer, key, new MediaWrapper(formElementValue, url), pd, browserConversionMarkers, null);
			}
		}

		return writer;
	}

	@Override
	public Object fromRhinoToDesignValue(Object value, PropertyDescription pd, IApplication application, JSWebComponent webComponent)
	{
		if (value instanceof String)
		{
			Media media = application.getFlattenedSolution().getMedia((String)value);
			if (media != null) return media.getUUID();
		}
		else if (value instanceof JSMedia)
		{
			return ((JSMedia)value).getUUID();
		}
		return value;
	}

	@Override
	public Object fromDesignToRhinoValue(Object value, PropertyDescription pd, IApplication application, JSWebComponent webComponent)
	{
		Media media = null;
		UUID uuid = Utils.getAsUUID(value, false);
		if (uuid != null)
		{
			media = (Media)application.getFlattenedSolution().searchPersist(uuid);
		}
		if (value instanceof String && media == null)
		{
			media = application.getFlattenedSolution().getMedia((String)value);
		}
		if (media != null)
		{
			return application.getScriptEngine().getSolutionModifier().getMedia(media.getName());
		}
		return null;
	}
}