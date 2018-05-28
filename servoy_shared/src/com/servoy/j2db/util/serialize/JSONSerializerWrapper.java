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
import java.util.NoSuchElementException;

import org.jabsorb.JSONSerializer;
import org.jabsorb.serializer.MarshallException;
import org.jabsorb.serializer.SerializerState;
import org.jabsorb.serializer.UnmarshallException;
import org.jabsorb.serializer.impl.ArraySerializer;
import org.jabsorb.serializer.impl.BeanSerializer;
import org.jabsorb.serializer.impl.BooleanSerializer;
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
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Wrapper;

import com.servoy.j2db.dataprocessing.IDatabaseManager;
import com.servoy.j2db.querybuilder.impl.QBFactory;
import com.servoy.j2db.util.Debug;

/**
 * Wrapper for JSONSerializer, handles a few exceptions to the default JSONSerializer and adds a defaultSerializer for cases when no class hint is given.
 *
 * @author rgansevles
 *
 */
@SuppressWarnings("nls")
public class JSONSerializerWrapper implements IQueryBuilderFactoryProvider
{
	private JSONSerializer serializer;
	private final boolean handleByteArrays;

	ThreadLocal<IDatabaseManager> currentDBMGR = new ThreadLocal<IDatabaseManager>();

	public JSONSerializerWrapper(boolean handleByteArrays)
	{
		this.handleByteArrays = handleByteArrays;
	}

	public Object toJSON(Object obj) throws MarshallException
	{
		if (obj instanceof String) return JSONObject.quote((String)obj);
		SerializerState state = new SerializerState();
		return getSerializer().marshall(state, null, wrapToJSON(obj), "result");
	}

	public Object fromJSON(IDatabaseManager databaseManager, String data) throws UnmarshallException
	{
		if (databaseManager == null)
		{
			return fromJSON(data);
		}
		IDatabaseManager tmp = currentDBMGR.get();
		try
		{
			currentDBMGR.set(databaseManager);
			return fromJSON(data);
		}
		finally
		{
			currentDBMGR.set(tmp);
		}
	}

	public Object fromJSON(String data) throws UnmarshallException
	{
		try
		{
			return unwrapFromJSON(getSerializer().fromJSON(data));
		}
		catch (UnmarshallException e)
		{
			Debug.error(e);
			throw e;
		}
	}

	public Object fromJSON(IDatabaseManager databaseManager, JSONObject json) throws UnmarshallException
	{
		if (databaseManager == null)
		{
			return fromJSON(json);
		}
		IDatabaseManager tmp = currentDBMGR.get();
		try
		{
			currentDBMGR.set(databaseManager);
			return fromJSON(json);
		}
		finally
		{
			currentDBMGR.set(tmp);
		}
	}

	public Object fromJSON(JSONObject json) throws UnmarshallException
	{
		SerializerState state = new SerializerState();
		return unwrapFromJSON(getSerializer().unmarshall(state, null, json));
	}

	protected synchronized JSONSerializer getSerializer()
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
					if ((clazz == null || clazz == Object.class) && json instanceof JSONObject && !((JSONObject)json).has("javaClass"))
					{
						// NativeObjectSerializer when there is no class hint
						clazz = NativeObject.class;
					}
					if (clazz == null && json instanceof JSONArray)
					{
						// default native array when there is no class hint
						clazz = NativeArray.class;
					}
					if ((clazz == null || clazz == Object.class) && json instanceof Boolean)
					{
						// hack to make sure BooleanSerializer is used
						clazz = Boolean.class;
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

				@Override
				protected Class getClassFromHint(Object o) throws UnmarshallException
				{
					if (o == null)
					{
						return null;
					}
					if (o instanceof JSONObject)
					{
						String className = "(unknown)";
						try
						{
							className = ((JSONObject)o).getString("javaClass");
							return Class.forName(className);
						}
						catch (Exception e)
						{
							throw new UnmarshallException("Class specified in javaClass hint not found: " + className, e);
						}
					}
					if (o instanceof JSONArray)
					{
						JSONArray arr = (JSONArray)o;
						if (arr.length() == 0)
						{
							// assume Object array (best guess)
							return Object[].class;
						}
						// return type of first element
						Class compClazz;
						try
						{
							compClazz = getClassFromHint(arr.get(0));
						}
						catch (JSONException e)
						{
							throw (NoSuchElementException)new NoSuchElementException(e.getMessage()).initCause(e);
						}
						try
						{
							if (compClazz.isArray())
							{
								return Class.forName("[" + compClazz.getName());
							}
							return Class.forName("[L" + compClazz.getName() + ";");
						}
						catch (ClassNotFoundException e)
						{
							throw new UnmarshallException("problem getting array type", e);
						}
					}
					return o.getClass();
				}
			};
			try
			{
				serializer.setFixupDuplicates(false);

				// registerDefaultSerializers
				serializer.registerSerializer(new BeanSerializer()); // least-specific serializers first, they will be selected last if no other serializer matches
				serializer.registerSerializer(new RawJSONArraySerializer()
				{
					@Override
					public boolean canSerialize(Class clazz, Class jsonClazz)
					{
						// make sure JSONArray subclasses are also serialized as just the json
						return JSONArray.class.isAssignableFrom(clazz) && (jsonClazz == null || jsonClazz == JSONArray.class);
					}
				});
				serializer.registerSerializer(new RawJSONObjectSerializer()
				{
					@Override
					public boolean canSerialize(Class clazz, Class jsonClazz)
					{
						// make sure JSONObject subclasses are also serialized as just the json
						return JSONObject.class.isAssignableFrom(clazz) && (jsonClazz == null || jsonClazz == JSONObject.class);
					}
				});
				serializer.registerSerializer(new ArraySerializer());
				serializer.registerSerializer(new DictionarySerializer());
				serializer.registerSerializer(new MapSerializer());
				serializer.registerSerializer(new SetSerializer());
				serializer.registerSerializer(new ListSerializer());
				serializer.registerSerializer(new ServoyDateSerializer());
				serializer.registerSerializer(handleByteArrays ? new StringByteArraySerializer() : new StringSerializer()); // handle byte arrays as base64 encoded?
				serializer.registerSerializer(new NumberSerializer());
				serializer.registerSerializer(new BooleanSerializer());
				serializer.registerSerializer(new PrimitiveSerializer());

				serializer.registerSerializer(new QueryBuilderSerializer(this));

				serializer.registerSerializer(new NativeObjectSerializer());
				serializer.registerSerializer(new NullForUndefinedSerializer());
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
		return serializer;
	}

	public QBFactory getQueryBuilderFactory()
	{
		IDatabaseManager dbmgr = currentDBMGR.get();
		if (dbmgr != null)
		{
			return (QBFactory)dbmgr.getQueryFactory();
		}
		return null;
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
		// put this back for legacy behavior support, arrays used to be serialized as json objects
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
