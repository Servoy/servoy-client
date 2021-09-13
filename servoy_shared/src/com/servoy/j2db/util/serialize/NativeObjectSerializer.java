/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.j2db.util.serialize;

import org.jabsorb.serializer.AbstractSerializer;
import org.jabsorb.serializer.MarshallException;
import org.jabsorb.serializer.ObjectMatch;
import org.jabsorb.serializer.SerializerState;
import org.jabsorb.serializer.UnmarshallException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.IdScriptableObject;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

import com.servoy.j2db.scripting.JSMap;
import com.servoy.j2db.util.Utils;


/**
 * Rhino NativeObject JSON serializer
 *
 * @author gboros
 */
public class NativeObjectSerializer extends AbstractSerializer
{
	private static final long serialVersionUID = 1L;

	private static final String PROPERTY_MARK = "_"; //$NON-NLS-1$

	private static Class[] _serializableClasses = new Class[] { NativeObject.class, NativeObject[].class, NativeArray.class };

	private static Class[] _JSONClasses = new Class[] { JSONObject.class, JSONArray.class };

	public NativeObjectSerializer()
	{
	}

	public Class[] getJSONClasses()
	{
		return _JSONClasses;
	}

	public Class[] getSerializableClasses()
	{
		return _serializableClasses;
	}

	public Object marshall(SerializerState state, Object parent, Object o) throws MarshallException
	{
		if (!(o instanceof NativeObject || o instanceof NativeArray))
		{
			throw new MarshallException("cannot marshall NativeObject using class " + o.getClass()); //$NON-NLS-1$
		}

		if (o instanceof NativeArray)
		{
			NativeArray nativeArray = (NativeArray)o;
			JSONArray jsonArray = new JSONArray();
			long length = nativeArray.getLength();

			for (int i = 0; i < length; i++)
			{
				Object elem = nativeArray.get(i, nativeArray);
				jsonArray.put(ser.marshall(state, o, elem == Scriptable.NOT_FOUND ? null : elem, new Integer(i)));
			}
			return jsonArray;
		}

		// else NativeObject

		IdScriptableObject no = (IdScriptableObject)o;
		JSONObject obj = new JSONObject();

		Object[] noIDs = no.getIds();
		String propertyKey;
		Object propertyValue;
		for (Object element : noIDs)
		{
			// id can be Integer or String
			if (element instanceof Integer)
			{
				propertyKey = ((Integer)element).toString();
				propertyValue = no.get(((Integer)element).intValue(), no);
			}
			else if (element instanceof String)
			{
				propertyKey = (String)element;
				propertyValue = no.get((String)element, no);
			}
			else
			{
				// should not happen
				continue;
			}
			if (propertyValue instanceof Function) // allow but ignore functions nested in objects
			{
				continue;
			}
			propertyValue = JSONSerializerWrapper.wrapToJSON(propertyValue);
			try
			{
				obj.put(propertyKey, ser.marshall(state, o, propertyValue, propertyKey));
			}
			catch (JSONException e)
			{
				throw new MarshallException("JSONException: " + e.getMessage(), e); //$NON-NLS-1$
			}
		}

		return obj;

	}

	@SuppressWarnings("nls")
	public ObjectMatch tryUnmarshall(SerializerState state, Class clazz, Object json) throws UnmarshallException
	{
		if (json instanceof JSONArray || json instanceof JSONObject)
		{
			return ObjectMatch.OKAY;
		}

		throw new UnmarshallException("not a NativeObject");
	}

	@SuppressWarnings("nls")
	public Object unmarshall(SerializerState state, Class clazz, Object json) throws UnmarshallException
	{
		if (json instanceof JSONArray)
		{
			return unmarshallJSONArray(state, (JSONArray)json);
		}

		// else JSONObject

		return unmarshallJSONObject(state, clazz, (JSONObject)json);
	}

	public Object unmarshallJSONObject(SerializerState state, Class clazz, JSONObject jso) throws UnmarshallException
	{
		boolean hasJavaClass = jso.has("javaClass"); // legacy, remove prefixes

		JSMap<Object, Object> no = new JSMap<Object, Object>(NativeArray.class.equals(clazz) ? "Array" : null);
		for (String jsonKey : Utils.iterate(jso.keys()))
		{
			String jsonProperty = jsonKey;
			if (hasJavaClass)
			{
				// legacy
				if (jsonKey.equals("javaClass")) continue;

				if (jsonKey.startsWith(PROPERTY_MARK))
				{
					jsonProperty = jsonKey.substring(PROPERTY_MARK.length());
				}
			}

			Object jsonValue;
			try
			{
				jsonValue = jso.get(jsonKey);
			}
			catch (JSONException e)
			{
				throw new UnmarshallException("JSONException: " + e.getMessage(), e); //$NON-NLS-1$
			}

			jsonValue = getUnmarshalled(state, jsonValue);
			try
			{
				int jsonIntKey = Integer.parseInt(jsonProperty);
				no.put(Integer.valueOf(jsonIntKey), jsonValue);
			}
			catch (NumberFormatException ex)
			{
				// property key is a string
				no.put(jsonProperty, jsonValue);
			}
		}

		return no;
	}

	public Object unmarshallJSONArray(SerializerState state, JSONArray jso) throws UnmarshallException
	{
		int length = jso.length();
		Object[] array = new Object[length];
		for (int i = 0; i < length; i++)
		{
			Object jsonValue;
			try
			{
				jsonValue = jso.get(i);
			}
			catch (JSONException e)
			{
				throw new UnmarshallException("JSONException: " + e.getMessage(), e); //$NON-NLS-1$
			}

			array[i] = getUnmarshalled(state, jsonValue);
		}

		return array;
	}

	private Object getUnmarshalled(SerializerState state, Object jsonValue) throws UnmarshallException
	{
		Object unmarshalled;
		if (jsonValue instanceof JSONObject)
		{
			JSONObject jsonObjectValue = (JSONObject)jsonValue;
			Class< ? > clazz = null;
			if (jsonObjectValue.has("javaClass")) //$NON-NLS-1$
			{
				String classHint;
				try
				{
					classHint = jsonObjectValue.getString("javaClass"); //$NON-NLS-1$
				}
				catch (JSONException e)
				{
					throw new UnmarshallException("JSONException: " + e.getMessage(), e); //$NON-NLS-1$
				}
				try
				{
					if (Undefined.class.getName().equals(classHint))
					{
						return Undefined.instance;
					}
					else
					{
						clazz = Class.forName(classHint);
					}
				}
				catch (ClassNotFoundException ex)
				{
					throw new UnmarshallException("cannot find class for " + classHint); //$NON-NLS-1$
				}
			}
			unmarshalled = ser.unmarshall(state, clazz, jsonValue);
		}
		else if (jsonValue instanceof JSONArray)
		{
			unmarshalled = unmarshallJSONArray(state, (JSONArray)jsonValue);
		}
		else
		{
			unmarshalled = jsonValue;
		}
		return JSONSerializerWrapper.unwrapFromJSON(unmarshalled);
	}
}
