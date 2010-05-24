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
import java.util.List;

import org.jabsorb.JSONSerializer;
import org.jabsorb.serializer.Serializer;
import org.jabsorb.serializer.SerializerState;
import org.jabsorb.serializer.UnmarshallException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Wrapper;

import com.servoy.j2db.util.Debug;

/**
 * Wrapper for JSONSerializer, handles a few exceptions to the default JSONSerializer and adds a defaultSerializer for cases when no class hint is given.
 * 
 * @author rob
 * 
 */
public class JSONSerializerWrapper
{
	private JSONSerializer serializer;
	private final Serializer defaultSerializer;

	public JSONSerializerWrapper(Serializer defaultSerializer)
	{
		this.defaultSerializer = defaultSerializer;
	}

	public Object toJSON(Object obj) throws Exception
	{
		SerializerState state = new SerializerState();
		return getSerializer().marshall(state, null, wrapToJSON(obj), "result");
	}

	public Object fromJSON(String data) throws Exception
	{
		return unwrapFromJSON(getSerializer().fromJSON(data));
	}

	public Object fromJSON(JSONObject json) throws Exception
	{
		SerializerState state = new SerializerState();
		return unwrapFromJSON(getSerializer().unmarshall(state, null, json));
	}

	protected JSONSerializer getSerializer()
	{
		if (serializer == null)
		{
			serializer = new JSONSerializer()
			{
				@Override
				public Object unmarshall(SerializerState state, Class clazz, Object json) throws UnmarshallException
				{
					if (clazz == null && defaultSerializer != null && defaultSerializer.getSerializableClasses() != null &&
						defaultSerializer.getSerializableClasses().length > 0 && json instanceof JSONObject && !((JSONObject)json).has("javaClass"))
					{
						// default serializer when there is no class hint
						clazz = defaultSerializer.getSerializableClasses()[0];
					}
					if (clazz == null && defaultSerializer != null && defaultSerializer.getSerializableClasses() != null &&
						defaultSerializer.getSerializableClasses().length > 1 && json instanceof JSONArray)
					{
						// default serializer when there is no class hint
						clazz = defaultSerializer.getSerializableClasses()[1];
					}
					return super.unmarshall(state, clazz, json);
				}

				@Override
				public boolean isPrimitive(Object o)
				{
					if (o != null)
					{
						Class cls = o.getClass();
						if (cls == java.math.BigDecimal.class || cls == java.math.BigInteger.class)
						{
							return true;
						}
					}
					return super.isPrimitive(o);
				}
			};
			try
			{
				serializer.setFixupDuplicates(false);
				serializer.registerDefaultSerializers();
				if (defaultSerializer != null)
				{
					serializer.registerSerializer(defaultSerializer);
				}
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
		return serializer;
	}

	/**
	 * 
	 * Wrap to serialize with JSONRPC.
	 * 
	 * @param obj to serialize to JSON
	 * @return obj ready to be serialized with JSONRPC
	 */
	public static Object wrapToJSON(Object object)
	{
		// unwrap rhino object, dont unwrap NativeArrat, those are handled by the NativeObjectSerializer
		Object obj = object;
		if (obj instanceof Wrapper && !(obj instanceof NativeArray))
		{
			obj = ((Wrapper)obj).unwrap();
		}
		// change simple array to ArrayList (need this because of bug in JSONRPC with array in array)
		if (obj instanceof Object[])
		{
			Object[] objArray = (Object[])obj;
			List<Object> arrayToArrayList = new ArrayList<Object>(objArray.length);
			for (Object element : objArray)
			{
				arrayToArrayList.add(wrapToJSON(element));
			}

			return arrayToArrayList;
		}

		return obj;
	}

	/**
	 * 
	 * Unwrap from JSONRPC serialized object.
	 * 
	 * @param obj deserialized from JSON
	 * @return object
	 */

	public static Object unwrapFromJSON(Object obj)
	{
		//change ArrayList to simple array (need this because of bug in JSONRPC with array in array)
		if (obj instanceof ArrayList)
		{
			List<Object> objArrayList = (List<Object>)obj;
			Object[] plainArray = new Object[objArrayList.size()];

			for (int i = 0; i < objArrayList.size(); i++)
			{
				plainArray[i] = unwrapFromJSON(objArrayList.get(i));
			}

			return plainArray;
		}
		if (obj == JSONObject.NULL)
		{
			return null;
		}

		return obj;
	}


}
