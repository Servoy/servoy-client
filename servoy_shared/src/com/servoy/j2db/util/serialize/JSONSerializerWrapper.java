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

import org.jabsorb.JSONSerializer;
import org.jabsorb.serializer.MarshallException;
import org.jabsorb.serializer.Serializer;
import org.jabsorb.serializer.SerializerState;
import org.jabsorb.serializer.UnmarshallException;
import org.jabsorb.serializer.impl.ArraySerializer;
import org.jabsorb.serializer.impl.BeanSerializer;
import org.jabsorb.serializer.impl.BooleanSerializer;
import org.jabsorb.serializer.impl.DateSerializer;
import org.jabsorb.serializer.impl.DictionarySerializer;
import org.jabsorb.serializer.impl.ListSerializer;
import org.jabsorb.serializer.impl.MapSerializer;
import org.jabsorb.serializer.impl.NumberSerializer;
import org.jabsorb.serializer.impl.PrimitiveSerializer;
import org.jabsorb.serializer.impl.RawJSONArraySerializer;
import org.jabsorb.serializer.impl.RawJSONObjectSerializer;
import org.jabsorb.serializer.impl.SetSerializer;
import org.jabsorb.serializer.impl.StringSerializer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Wrapper;

import com.servoy.j2db.util.Debug;

/**
 * Wrapper for JSONSerializer, handles a few exceptions to the default JSONSerializer and adds a defaultSerializer for cases when no class hint is given.
 * 
 * @author rgansevles
 * 
 */
@SuppressWarnings("nls")
public class JSONSerializerWrapper
{
	private JSONSerializer serializer;
	private final Serializer defaultSerializer;
	private final boolean handleArrays;
	private final boolean handleByteArrays;

	public JSONSerializerWrapper(Serializer defaultSerializer)
	{
		this(defaultSerializer, false, false);
	}

	public JSONSerializerWrapper(Serializer defaultSerializer, boolean handleArrays)
	{
		this(defaultSerializer, handleArrays, false);
	}

	public JSONSerializerWrapper(Serializer defaultSerializer, boolean handleArrays, boolean handleByteArrays)
	{
		this.defaultSerializer = defaultSerializer;
		this.handleArrays = handleArrays;
		this.handleByteArrays = handleByteArrays;
	}

	public Object toJSON(Object obj) throws Exception
	{
		if (obj instanceof String) return JSONObject.quote((String)obj);
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
				public Object marshall(SerializerState state, Object parent, Object java, Object ref) throws MarshallException
				{
					// NativeArray may contain wrapped data
					return super.marshall(state, parent, wrapToJSON(java), ref);
				}

				@Override
				public Object unmarshall(SerializerState state, Class clazz, Object json) throws UnmarshallException
				{
					if (clazz == null && defaultSerializer != null && defaultSerializer.getSerializableClasses() != null &&
						defaultSerializer.getSerializableClasses().length > 0 && json instanceof JSONObject && !((JSONObject)json).has("javaClass"))
					{
						// default serializer when there is no class hint
						clazz = defaultSerializer.getSerializableClasses()[0];
					}
					if (clazz == null && json instanceof JSONArray)
					{
						// default object array when there is no class hint
						clazz = Object[].class;
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

				// registerDefaultSerializers
				serializer.registerSerializer(new RawJSONArraySerializer());
				serializer.registerSerializer(new RawJSONObjectSerializer());
				serializer.registerSerializer(new BeanSerializer());
				serializer.registerSerializer(new ArraySerializer());
				serializer.registerSerializer(new DictionarySerializer());
				serializer.registerSerializer(new MapSerializer());
				serializer.registerSerializer(new SetSerializer());
				if (!handleArrays)
				{
					serializer.registerSerializer(new ListSerializer()); // is handled by NativeObjectSerializer
				}
				serializer.registerSerializer(new DateSerializer());
				serializer.registerSerializer(handleByteArrays ? new StringByteArraySerializer() : new StringSerializer()); // handle byte arrays as base64 encoded?
				serializer.registerSerializer(new NumberSerializer());
				serializer.registerSerializer(new BooleanSerializer());
				serializer.registerSerializer(new PrimitiveSerializer());

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
		// unwrap rhino object, don't unwrap NativeArray, those are handled by the NativeObjectSerializer
		Object obj = object;
		if (obj instanceof Wrapper && !(obj instanceof NativeArray))
		{
			obj = ((Wrapper)obj).unwrap();
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
		if (obj == JSONObject.NULL)
		{
			return null;
		}

		return obj;
	}
}
