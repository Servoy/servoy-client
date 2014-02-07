/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 */

package com.servoy.j2db.server.webclient2.utils;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mozilla.javascript.NativeDate;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.UniqueTag;

import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.dataprocessing.LookupListModel;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.server.webclient2.ClientConversion;
import com.servoy.j2db.server.webclient2.property.PropertyType;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Utils;

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
	 * @return the new writer object to continue writing JSON.
	 * @throws JSONException
	 * @throws IllegalArgumentException if the given object could not be written to JSON for some reason.
	 */
	public static JSONWriter toJSONValue(JSONWriter writer, Object value) throws JSONException, IllegalArgumentException
	{
		return toJSONValue(writer, value, null);
	}

	/**
	 * Writes the given object into the JSONWriter. (it is meant to be used for transforming the basic types that can be sent by beans/components)
	 * @param writer the JSONWriter.
	 * @param value the value to be written to the writer.
	 * @param clientConversion the object where the type (like Date) of the conversion that should happen on the client.
	 * @return the new writer object to continue writing JSON.
	 * @throws JSONException
	 * @throws IllegalArgumentException if the given object could not be written to JSON for some reason.
	 */
	public static JSONWriter toJSONValue(JSONWriter writer, Object value, ClientConversion clientConversion) throws JSONException, IllegalArgumentException
	{
		JSONWriter w = writer;
		if (value == null || value == UniqueTag.NOT_FOUND || value == Undefined.instance || value == JSONObject.NULL)
		{
			w = w.value(null); // null is allowed
		}
		else if (value instanceof Integer || value instanceof Long)
		{
			w = w.value(((Number)value).longValue());
		}
		else if (value instanceof Boolean)
		{
			w = w.value(((Boolean)value).booleanValue());
		}
		else if (value instanceof Number)
		{
			w = w.value(((Number)value).doubleValue());
		}
		else if (value instanceof String)
		{
			w = w.value(value);
		}
		else if (value instanceof Date || value instanceof NativeDate)
		{
			if (clientConversion != null) clientConversion.convert("Date");
			Date date = (Date)((value instanceof NativeDate) ? ((NativeDate)value).unwrap() : value);
			w = w.value(date.getTime());
		}
		else if (value instanceof Color)
		{
			w = w.value(PersistHelper.createColorString((Color)value));
		}
		else if (value instanceof Point)
		{
			w = w.object();
			w = w.key("x").value(((Point)value).getX());
			w = w.key("y").value(((Point)value).getY());
			w = w.endObject();
		}
		else if (value instanceof Dimension)
		{
			w = w.object();
			w = w.key("width").value(((Dimension)value).getWidth());
			w = w.key("height").value(((Dimension)value).getHeight());
			w = w.endObject();
		}
		else if (value instanceof IValueList)
		{
			w = w.array();
			IValueList list = (IValueList)value;
			for (int i = 0; i < list.getSize(); i++)
			{
				w = w.object();
				Object realElement = list.getRealElementAt(i);
				w = w.key("realValue");
				toJSONValue(writer, realElement, clientConversion);
				w = w.key("displayValue");
				toJSONValue(writer, list.getElementAt(i), clientConversion);
				w = w.endObject();
			}
			w = w.endArray();
		}
		else if (value instanceof LookupListModel)
		{
			w = w.array();
			LookupListModel list = (LookupListModel)value;
			for (int i = 0; i < list.getSize(); i++)
			{
				w = w.object();
				Object realElement = list.getRealElementAt(i);
				w = w.key("realValue");
				toJSONValue(writer, realElement, clientConversion);
				w = w.key("displayValue");
				toJSONValue(writer, list.getElementAt(i), clientConversion);
				w = w.endObject();
			}
			w = w.endArray();
		}
		else if (value instanceof ComponentFormat)
		{
			ComponentFormat format = (ComponentFormat)value;
			w = w.object();
			//w.key("for").value(format..);
			w.key("type").value(Column.getDisplayTypeString(format.uiType));
			w.key("display").value(format.parsedFormat.getDisplayFormat());
			w.endObject();
		}
		else if (value instanceof JSONArray)
		{
			w = w.value(value);
		}
		else if (value instanceof JSONObject)
		{
			w = w.value(value);
		}
		else if (value instanceof List)
		{
			List< ? > lst = (List< ? >)value;
			w.array();
			for (int i = 0; i < lst.size(); i++)
			{
				if (clientConversion != null) clientConversion.pushNode(String.valueOf(i));
				toJSONValue(w, lst.get(i), clientConversion);
				if (clientConversion != null) clientConversion.popNode();
			}
			w.endArray();
		}
		else if (value instanceof Object[])
		{
			Object[] array = (Object[])value;
			w.array();
			for (int i = 0; i < array.length; i++)
			{
				if (clientConversion != null) clientConversion.pushNode(String.valueOf(i));
				toJSONValue(w, array[i], clientConversion);
				if (clientConversion != null) clientConversion.popNode();
			}
			w.endArray();
		}
		else if (value instanceof Map)
		{
			w = w.object();
			Map<String, ? > map = (Map<String, ? >)value;
			for (Entry<String, ? > entry : map.entrySet())
			{
				if (clientConversion != null) clientConversion.pushNode(entry.getKey());
				w.key(entry.getKey());
				toJSONValue(w, entry.getValue(), clientConversion);
				if (clientConversion != null) clientConversion.popNode();
			}
			w = w.endObject();
		}
		else if (value instanceof Form)
		{
			w = w.value(toStringObject(value, PropertyType.form));
		}
		else if (value instanceof JSONWritable)
		{
			toJSONValue(w, ((JSONWritable)value).toMap(), clientConversion);
		}
		else
		{
			throw new IllegalArgumentException("unsupported value type for value: " + value);
		}
		return w;
	}

	/**
	 * Converts a JSON value to a Java value based on bean spec type.
	 * @param propertyValue can be a JSONObject or array or primitive. (so something deserialized from a JSON string)
	 * @return the corresponding Java object based on bean spec.
	 */
	public static Object toJavaObject(Object propertyValue, PropertyType componentSpecType) throws JSONException
	{
		if (propertyValue != null && componentSpecType != null)
		{
			switch (componentSpecType)
			{
				case dimension :
					if (propertyValue instanceof Object[])
					{
						return new Dimension(Utils.getAsInteger(((Object[])propertyValue)[0]), Utils.getAsInteger(((Object[])propertyValue)[1]));
					}
					if (propertyValue instanceof JSONObject)
					{
						return new Dimension(((JSONObject)propertyValue).getInt("width"), ((JSONObject)propertyValue).getInt("height"));
					}
					if (propertyValue instanceof NativeObject)
					{
						NativeObject value = (NativeObject)propertyValue;
						return new Dimension(Utils.getAsInteger(value.get("width", value)), Utils.getAsInteger(value.get("height", value)));
					}
					break;

				case point :
					if (propertyValue instanceof Object[])
					{
						return new Point(Utils.getAsInteger(((Object[])propertyValue)[0]), Utils.getAsInteger(((Object[])propertyValue)[1]));
					}
					if (propertyValue instanceof JSONObject)
					{
						return new Point(((JSONObject)propertyValue).getInt("x"), ((JSONObject)propertyValue).getInt("y"));
					}
					if (propertyValue instanceof NativeObject)
					{
						NativeObject value = (NativeObject)propertyValue;
						return new Point(Utils.getAsInteger(value.get("x", value)), Utils.getAsInteger(value.get("y", value)));
					}
					break;

				case color :
					if (propertyValue instanceof String)
					{
						return PersistHelper.createColor(propertyValue.toString());
					}
					break;
				case format :
					if (propertyValue instanceof String)
					{
						//todo recreate ComponentFormat object (it has quite a lot of dependencies , application,pesist  etc)
						return propertyValue;
					}
					break;

				default :
			}
		}

		return propertyValue;
	}

	public static Object toStringObject(Object propertyValue, PropertyType propertyType)
	{
		if (propertyValue != null && propertyType != null)
		{
			switch (propertyType)
			{
				case dimension :
					if (propertyValue instanceof Dimension)
					{
						return PersistHelper.createDimensionString((Dimension)propertyValue);
					}
					break;

				case point :
					if (propertyValue instanceof Point)
					{
						return PersistHelper.createPointString((Point)propertyValue);
					}
					break;

				case color :
					if (propertyValue instanceof Color)
					{
						return PersistHelper.createColorString((Color)propertyValue);
					}
					break;

				case form :
					if (propertyValue instanceof Form)
					{
						Form frm = (Form)propertyValue;
						return "solutions/" + frm.getSolution().getName() + "/forms/" + frm.getName() + ".html";
					}
					break;

				default :
			}
		}

		return propertyValue;
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

	/**
	 * Adds all properties of the given object as key-value pairs in the writer.
	 * @param propertyWriter the writer.
	 * @param objectToMerge the object contents to be merged into the writer prepared object.
	 * @throws JSONException if the writer is not prepared (to write object contents) or other json exception occurs.
	 */
	public static void addObjectPropertiesToWriter(JSONWriter propertyWriter, JSONObject objectToMerge) throws JSONException
	{
		Iterator< ? > it = objectToMerge.keys();
		while (it.hasNext())
		{
			String key = (String)it.next();
			propertyWriter.key(key).value(objectToMerge.get(key));
		}
	}

	public static JSONWriter addObjectPropertiesToWriter(JSONWriter jsonWriter, Map<String, Object> properties) throws JSONException, IllegalArgumentException
	{
		for (Entry<String, Object> entry : properties.entrySet())
		{
			toJSONValue(jsonWriter.key(entry.getKey()), entry.getValue());
		}
		return jsonWriter;
	}

	public static interface JSONWritable
	{
		Map<String, Object> toMap();
	}
}
