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
package com.servoy.j2db.util;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Map interface wrapper on JSONObject.
 *
 * @author rgansevles
 *
 */
public class JSONWrapperMap<T> extends AbstractMap<String, T>
{
	// original source, will be cleared when json object is touched
	protected String source;

	// json object based on source
	private JSONObject json; // NOTE: never use this field directly when possibly making changes, always call getJson() to keep source in sync!

	public JSONWrapperMap(String source)
	{
		this.source = source;
	}

	public JSONWrapperMap(JSONObject json)
	{
		this.json = json;
	}

	/**
	 * Method to get the json object in a lazy way, postpone parsing of the json if possible.
	 *
	 * @return
	 */
	protected JSONObject getJson()
	{
		if (json == null && source != null)
		{
			try
			{
				json = new ServoyJSONObject(source, true);
			}
			catch (JSONException e)
			{
				throw new RuntimeException(e);
			}
		}

		// clear source, it may be outdated when json changes
		source = null;
		return json;
	}

	@Override
	public Set<String> keySet()
	{
		Set<String> set = new HashSet<String>(size());
		Iterator<String> keys = getJson().keys();
		while (keys.hasNext())
		{
			set.add(keys.next());
		}
		return set;
	}

	@Override
	public Set<java.util.Map.Entry<String, T>> entrySet()
	{
		Set<java.util.Map.Entry<String, T>> set = new HashSet<java.util.Map.Entry<String, T>>();
		Iterator<String> keys = getJson().keys();
		while (keys.hasNext())
		{
			set.add((java.util.Map.Entry<String, T>)new JSONWrapperMapEntry(keys.next(), getJson()));
		}
		return set;
	}

	@Override
	public boolean containsKey(Object key)
	{
		if (key instanceof String)
		{
			return getJson().has((String)key);
		}
		return false;
	}

	protected Object toJava(Object o)
	{
		return ServoyJSONObject.toJava(o);
	}

	@Override
	public T get(Object key)
	{
		if (key instanceof String)
		{
			return (T)toJava(getJson().opt((String)key));
		}
		return null;
	}

	@Override
	public T remove(Object key)
	{
		if (key instanceof String)
		{
			return (T)toJava(getJson().remove((String)key));
		}
		return null;
	}

	@Override
	public T put(String key, T value)
	{
		try
		{
			Object old = getJson().opt(key);
			getJson().put(key, value);
			return (T)toJava(old);
		}
		catch (JSONException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public int size()
	{
		return getJson().length();
	}

	@Override
	public String toString()
	{
		if (source == null)
		{
			// source is touched
			source = ServoyJSONObject.toString(getJson(), true, true, true);
		}
		return source;
	}

	public class JSONWrapperMapEntry implements Entry<String, Object>
	{

		private final String key;
		private final JSONObject json;

		public JSONWrapperMapEntry(String key, JSONObject json)
		{
			this.key = key;
			this.json = json;
		}

		public String getKey()
		{
			return key;
		}

		public Object getValue()
		{
			if (json.has(key))
			{
				try
				{
					return toJava(json.get(key));
				}
				catch (JSONException e)
				{
					throw new RuntimeException(e);
				}
			}
			return null;
		}

		public Object setValue(Object value)
		{
			try
			{
				return toJava(json.put(key, value));
			}
			catch (JSONException e)
			{
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * merge 2 maps into 1, map2 overrides values in map1
	 *
	 * @param map1
	 * @param map2
	 * @return
	 */
	public static <T> JSONWrapperMap< ? > mergeMaps(JSONWrapperMap< ? extends T> map1, JSONWrapperMap< ? extends T> map2)
	{
		if (map1 == null)
		{
			return map2;
		}
		if (map2 == null)
		{
			return map1;
		}

		JSONWrapperMap<T> merged = new JSONWrapperMap<T>(new JSONObject());
		for (String key2 : map2.keySet())
		{
			T val1 = map2.get(key2);
			if (map1.containsKey(key2))
			{
				Object val2 = map1.get(key2);
				if (val1 instanceof JSONWrapperMap && val2 instanceof JSONWrapperMap)
				{
					merged.put(key2, (T)mergeMaps((JSONWrapperMap< ? >)val1, (JSONWrapperMap< ? >)val2));
					continue;
				}
			}
			merged.put(key2, val1);
		}
		for (String key1 : map1.keySet())
		{
			if (!map2.containsKey(key1))
			{
				merged.put(key1, map1.get(key1));
			}
		}

		return merged;
	}
}
