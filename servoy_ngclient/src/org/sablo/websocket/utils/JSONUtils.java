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

package org.sablo.websocket.utils;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.sablo.specification.property.IComplexPropertyValue;
import org.sablo.websocket.IForJsonConverter;

import com.servoy.j2db.server.ngclient.NGClientForJsonConverter.ConversionLocation;
import com.servoy.j2db.util.Debug;


/**
 * Utility methods for JSON usage.
 *
 * @author acostescu
 */
@SuppressWarnings("nls")
public class JSONUtils
{
	/**
	 * Writes the given object into the JSONWriter. (it is meant to be used for transforming the basic types that can be sent by beans/components)
	 * @param writer the JSONWriter.
	 * @param value the value to be written to the writer.
	 * @return the writer object to continue writing JSON.
	 * @throws JSONException
	 * @throws IllegalArgumentException if the given object could not be written to JSON for some reason.
	 */
	public static JSONWriter toJSONValue(JSONWriter writer, Object value, IForJsonConverter forJsonConverter, ConversionLocation toDestinationType)
		throws JSONException, IllegalArgumentException
	{
		return toJSONValue(writer, value, null, forJsonConverter, toDestinationType);
	}

	/**
	 * Writes the given object into the JSONWriter. (it is meant to be used for transforming the basic types that can be sent by beans/components)
	 * @param writer the JSONWriter.
	 * @param value the value to be written to the writer.
	 * @param clientConversion the object where the type (like Date) of the conversion that should happen on the client.
	 * @param forJsonConverter 
	 * @return the writer object to continue writing JSON.
	 * @throws JSONException
	 * @throws IllegalArgumentException if the given object could not be written to JSON for some reason.
	 */
	public static JSONWriter toJSONValue(JSONWriter writer, Object value, DataConversion clientConversion, IForJsonConverter forJsonConverter,
		ConversionLocation toDestinationType) throws JSONException, IllegalArgumentException
	{
		if (value instanceof IComplexPropertyValue)
		{
			if (toDestinationType == ConversionLocation.BROWSER_UPDATE) return ((IComplexPropertyValue)value).changesToJSON(writer, clientConversion);
			else if (toDestinationType == ConversionLocation.BROWSER) return ((IComplexPropertyValue)value).toJSON(writer, clientConversion);
			else if (toDestinationType == ConversionLocation.DESIGN) return ((IComplexPropertyValue)value).toDesignJSON(writer); // less frequent or never
			else Debug.error(new RuntimeException("Trying to conver a java object to JSON value of unknown/unsupported destination type."));
		}

		JSONWriter w = writer;

		final Object converted = forJsonConverter == null ? value : forJsonConverter.convertForJson(value);

		if (converted == null || converted == JSONObject.NULL)
		{
			w = w.value(null); // null is allowed
		}
		else if (converted instanceof Integer || converted instanceof Long)
		{
			w = w.value(((Number)converted).longValue());
		}
		else if (converted instanceof Boolean)
		{
			w = w.value(((Boolean)converted).booleanValue());
		}
		else if (converted instanceof Number)
		{
			w = w.value(((Number)converted).doubleValue());
		}
		else if (converted instanceof String)
		{
			w = w.value(converted);
		}
		else if (converted instanceof CharSequence)
		{
			w = w.value(converted.toString());
		}
		else if (converted instanceof Point)
		{
			w = w.object();
			w = w.key("x").value(((Point)converted).getX());
			w = w.key("y").value(((Point)converted).getY());
			w = w.endObject();
		}
		else if (converted instanceof Dimension)
		{
			w = w.object();
			w = w.key("width").value(((Dimension)converted).getWidth());
			w = w.key("height").value(((Dimension)converted).getHeight());
			w = w.endObject();
		}
		else if (converted instanceof Date)
		{
			if (clientConversion != null) clientConversion.convert("Date");
			w = w.value(((Date)converted).getTime());
		}
		else if (converted instanceof Insets)
		{
			Insets i = (Insets)converted;
			w.object();
			w.key("paddingTop").value(i.top + "px");
			w.key("paddingBottom").value(i.bottom + "px");
			w.key("paddingLeft").value(i.left + "px");
			w.key("paddingRight").value(i.right + "px");
			w.endObject();
		}
		else if (converted instanceof Font)
		{
			Font font = (Font)converted;
			w.object();
			if (font.isBold())
			{
				w.key("fontWeight").value("bold");
			}
			if (font.isItalic())
			{
				w.key("italic").value("italic"); //$NON-NLS-1$
			}
			w.key("fontSize").value(font.getSize() + "px");
			w.key("fontFamily").value(font.getFamily() + ", Verdana, Arial");
			w.endObject();
		}
		else if (converted instanceof JSONArray) // TODO are we using JSON object or Map and Lists? ( as internal representation of properties)
		{
			w = w.value(converted);
		}
		else if (converted instanceof JSONObject)
		{
			w = w.value(converted);
		}
		else if (converted instanceof List)
		{
			List< ? > lst = (List< ? >)converted;
			w.array();
			for (int i = 0; i < lst.size(); i++)
			{
				if (clientConversion != null) clientConversion.pushNode(String.valueOf(i));
				toJSONValue(w, lst.get(i), clientConversion, forJsonConverter, toDestinationType);
				if (clientConversion != null) clientConversion.popNode();
			}
			w.endArray();
		}
		else if (converted instanceof Object[])
		{
			Object[] array = (Object[])converted;
			w.array();
			for (int i = 0; i < array.length; i++)
			{
				if (clientConversion != null) clientConversion.pushNode(String.valueOf(i));
				toJSONValue(w, array[i], clientConversion, forJsonConverter, toDestinationType);
				if (clientConversion != null) clientConversion.popNode();
			}
			w.endArray();
		}
		else if (converted instanceof Map)
		{
			w = w.object();
			Map<String, ? > map = (Map<String, ? >)converted;
			for (Entry<String, ? > entry : map.entrySet())
			{
				if (clientConversion != null) clientConversion.pushNode(entry.getKey());
				//TODO remove the need for this when going to full tree recursion for sendChanges()
				String[] keys = entry.getKey().split("\\.");
				if (keys.length > 1)
				{
					//LIMITATION of JSONWriter because it can't add a property to an already written object
					// currently for 2 properties like complexmodel.firstNameDataprovider
					//								   size
					//								   complexmodel.lastNameDataprovider
					// it creates 2 json entries with the same key ('complexmodel') and on the client side it only takes one of them
					w.key(keys[0]);
					w.object();
					w.key(keys[1]);
					toJSONValue(w, entry.getValue(), clientConversion, forJsonConverter, toDestinationType);
					w.endObject();
				}// END TODO REMOVE
				else
				{
					w.key(entry.getKey());
					toJSONValue(w, entry.getValue(), clientConversion, forJsonConverter, toDestinationType);
				}
				if (clientConversion != null) clientConversion.popNode();
			}
			w = w.endObject();
		}
		else if (converted instanceof JSONWritable)
		{
			toJSONValue(w, ((JSONWritable)converted).toMap(), clientConversion, forJsonConverter, toDestinationType);
		}
		else
		{
			throw new IllegalArgumentException("unsupported value type for value: " + converted);
		}

		return w;
	}

	/**
	 * Validates a String to be valid JSON content and normalizes it.
	 * @param json the json content to check.
	 * @return the given JSON normalized.
	 * @throws JSONException if the given JSON is not valid
	 */
	public static String validateAndTrimJSON(String json) throws JSONException
	{
		if (json == null) return null;

		return new JSONObject(json).toString(); // just to validate - can we do this nicer with available lib (we might not need the "normalize" part)?
	}

//	/**
//	 * Adds all properties of the given object as key-value pairs in the writer.
//	 * @param propertyWriter the writer.
//	 * @param objectToMerge the object contents to be merged into the writer prepared object.
//	 * @throws JSONException if the writer is not prepared (to write object contents) or other json exception occurs.
//	 */
//	public static void addObjectPropertiesToWriter(JSONWriter propertyWriter, JSONObject objectToMerge) throws JSONException
//	{
//		Iterator< ? > it = objectToMerge.keys();
//		while (it.hasNext())
//		{
//			String key = (String)it.next();
//			propertyWriter.key(key).value(objectToMerge.get(key));
//		}
//	}

	public static JSONWriter addObjectPropertiesToWriter(JSONWriter jsonWriter, Map<String, Object> properties, IForJsonConverter forJsonConverter)
		throws JSONException, IllegalArgumentException
	{
		for (Entry<String, Object> entry : properties.entrySet())
		{
			toJSONValue(jsonWriter.key(entry.getKey()), entry.getValue(), forJsonConverter, ConversionLocation.BROWSER);
		}
		return jsonWriter;
	}

	public static interface JSONWritable
	{
		Map<String, Object> toMap();
	}
}
