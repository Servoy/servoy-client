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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import org.jabsorb.serializer.AbstractSerializer;
import org.jabsorb.serializer.MarshallException;
import org.jabsorb.serializer.ObjectMatch;
import org.jabsorb.serializer.SerializerState;
import org.jabsorb.serializer.UnmarshallException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.IdScriptableObject;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeFunction;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.scripting.JSMap;


/**
 * Rhino NativeObject JSON serializer
 * 
 * @author gboros
 */
public class NativeObjectSerializer extends AbstractSerializer
{
	private static final long serialVersionUID = 1L;

	private static final String PROPERTY_MARK = "_"; //$NON-NLS-1$

	private static Class[] _serializableClassesStandard = new Class[] { NativeObject.class, NativeObject[].class, NativeArray.class };
	private static Class[] _serializableClassesWithArrays = new Class[] { NativeObject.class, NativeObject[].class, NativeArray.class, List.class, ArrayList.class, LinkedList.class, Vector.class };

	private static Class[] _JSONClassesStandard = new Class[] { JSONObject.class };
	private static Class[] _JSONClassesWithArrays = new Class[] { JSONObject.class, JSONArray.class };

	private final boolean prefixKeys;

	private final boolean addJavaClassHint;

	private final boolean handleArrays;

	public NativeObjectSerializer(boolean prefixKeys, boolean addJavaClassHint)
	{
		this(prefixKeys, addJavaClassHint, false);
	}

	public NativeObjectSerializer(boolean prefixKeys, boolean addJavaClassHint, boolean handleArrays)
	{
		this.prefixKeys = prefixKeys;
		this.addJavaClassHint = addJavaClassHint;
		this.handleArrays = handleArrays;
	}

	public Class[] getJSONClasses()
	{
		return handleArrays ? _JSONClassesWithArrays : _JSONClassesStandard;
	}

	public Class[] getSerializableClasses()
	{
		return handleArrays ? _serializableClassesWithArrays : _serializableClassesStandard;
	}

	public Object marshall(SerializerState state, Object parent, Object o) throws MarshallException
	{
		if (!(o instanceof NativeObject || o instanceof NativeArray || o instanceof List< ? >))
		{
			throw new MarshallException("cannot marshall NativeObject using class " + o.getClass()); //$NON-NLS-1$
		}

		if (handleArrays)
		{
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

			if (o instanceof List< ? >)
			{
				List< ? > list = (List< ? >)o;
				JSONArray jsonArray = new JSONArray();
				long length = list.size();

				for (int i = 0; i < length; i++)
				{
					Object elem = list.get(i);
					jsonArray.put(ser.marshall(state, o, elem, new Integer(i)));
				}
				return jsonArray;
			}
		}

		// else NativeObject

		IdScriptableObject no = (IdScriptableObject)o;
		JSONObject obj = new JSONObject();
		try
		{
			if (addJavaClassHint && ser.getMarshallClassHints()) obj.put("javaClass", o.getClass().getName()); //$NON-NLS-1$
		}
		catch (JSONException e)
		{
			throw new MarshallException("JSONException: " + e.getMessage(), e); //$NON-NLS-1$
		}

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
			if (propertyValue instanceof NativeFunction)
			{
				continue;
			}
			propertyValue = JSONSerializerWrapper.wrapToJSON(propertyValue);
			try
			{
				obj.put(prefixKeys ? (PROPERTY_MARK + propertyKey) : propertyKey, ser.marshall(state, o, propertyValue, propertyKey));
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
		if (handleArrays && json instanceof JSONArray)
		{
			return ObjectMatch.OKAY;
		}

		// else JSONObject

		JSONObject jso = (JSONObject)json;
		try
		{
			String java_class = jso.getString("javaClass"); //$NON-NLS-1$

			if (java_class == null) throw new UnmarshallException("no type hint"); //$NON-NLS-1$
			if (!(java_class.equals("org.mozilla.javascript.NativeObject") || java_class.equals("org.mozilla.javascript.NativeArray"))) throw new UnmarshallException(
				"not a NativeObject");

			return ObjectMatch.OKAY;
		}
		catch (JSONException e)
		{
			throw new UnmarshallException("JSONException: " + e.getMessage(), e); //$NON-NLS-1$
		}
	}

	@SuppressWarnings("nls")
	public Object unmarshall(SerializerState state, Class clazz, Object json) throws UnmarshallException
	{
		if (handleArrays && json instanceof JSONArray)
		{
			return unmarshallJSONArray(state, (JSONArray)json);
		}

		// else JSONObject
		JSONObject jso = (JSONObject)json;
		if ("java.util.ArrayList".equals(jso.opt("javaClass")))
		{
			// Legacy format: generated by ListSerializer
			Object list = jso.opt("list");
			if (list instanceof JSONArray)
			{
				return unmarshallJSONArray(state, (JSONArray)list);
			}
		}

		return unmarshallJSONObject(state, clazz, jso);
	}

	public Object unmarshallJSONObject(SerializerState state, Class clazz, JSONObject jso) throws UnmarshallException
	{
		if (jso.has("javaClass")) //$NON-NLS-1$
		{
			try
			{
				clazz = Class.forName(jso.getString("javaClass")); //$NON-NLS-1$
			}
			catch (ClassNotFoundException cnfe)
			{
				throw new UnmarshallException(cnfe.getMessage());
			}
			catch (JSONException e)
			{
				throw new UnmarshallException("JSONException: " + e.getMessage(), e); //$NON-NLS-1$
			}
		}

		if (!(NativeObject.class.equals(clazz) || NativeArray.class.equals(clazz)))
		{
			throw new UnmarshallException("invalid class " + clazz); //$NON-NLS-1$
		}

		String name = null;
		if (NativeArray.class.equals(clazz)) name = "Array";
		JSMap no = new JSMap(name);
		Iterator<String> jsonKeysIte = jso.keys();
		String jsonKey, jsonProperty;
		Object jsonValue;
		boolean hasPropertyMark = prefixKeys && hasPropertyMark(jso);
		while (jsonKeysIte.hasNext())
		{
			jsonKey = jsonKeysIte.next();
			if (!hasPropertyMark || jsonKey.startsWith(PROPERTY_MARK))
			{
				if (prefixKeys && jsonKey.startsWith(PROPERTY_MARK))
				{
					jsonProperty = jsonKey.substring(PROPERTY_MARK.length());
				}
				else
				{
					jsonProperty = jsonKey;
				}
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
					no.put(new Integer(jsonIntKey), jsonValue);
				}
				catch (NumberFormatException ex)
				{
					// property key is a string
					no.put(jsonProperty, jsonValue);
				}
			}
		}

		return no;
	}


	public Object unmarshallJSONArray(SerializerState state, JSONArray jso) throws UnmarshallException
	{
		JSMap no = new JSMap("Array");

		int length = jso.length();
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

			jsonValue = getUnmarshalled(state, jsonValue);

			no.put(new Integer(i), jsonValue);
		}

		return no;
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
					clazz = Class.forName(classHint);
				}
				catch (ClassNotFoundException ex)
				{
					throw new UnmarshallException("cannot find class for " + classHint); //$NON-NLS-1$
				}
			}
			unmarshalled = ser.unmarshall(state, clazz, jsonValue);
		}
		else if (handleArrays && jsonValue instanceof JSONArray)
		{
			unmarshalled = ser.unmarshall(state, NativeArray.class, jsonValue);
		}
		else
		{
			unmarshalled = jsonValue;
		}
		return JSONSerializerWrapper.unwrapFromJSON(unmarshalled);
	}

	private boolean hasPropertyMark(JSONObject jso)
	{
		if (jso != null)
		{
			Iterator<String> jsonKeysIte = jso.keys();
			while (jsonKeysIte.hasNext())
			{
				String jsonKey = jsonKeysIte.next();
				if (jsonKey.startsWith(PROPERTY_MARK))
				{
					return true;
				}
			}
		}
		return false;
	}
}
