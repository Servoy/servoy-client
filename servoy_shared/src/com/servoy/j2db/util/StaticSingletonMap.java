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

package com.servoy.j2db.util;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

/**
 * A map required for sharing objects through static means.
 * It produces only one map instance per class-loader.
 * @author acostescu
 */
public class StaticSingletonMap implements Map<String, Object>
{

	private static Map<String, Object> instance = new Hashtable<String, Object>();

	public static Map<String, Object> instance()
	{
		return instance;
	}

	public Object put(String key, Object value)
	{
		return instance.put(key, value);
	}

	@Override
	public int size()
	{
		return instance.size();
	}

	@Override
	public boolean isEmpty()
	{
		return instance.isEmpty();
	}

	@Override
	public boolean containsKey(Object key)
	{
		return instance.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value)
	{
		return instance.containsValue(value);
	}

	@Override
	public Object get(Object key)
	{
		return instance.get(key);
	}

	@Override
	public Object remove(Object key)
	{
		return remove(key);
	}

	@Override
	public void putAll(Map< ? extends String, ? extends Object> m)
	{
		instance.putAll(m);
	}

	@Override
	public void clear()
	{
		instance.clear();
	}

	@Override
	public Set<String> keySet()
	{
		return instance.keySet();
	}

	@Override
	public Collection<Object> values()
	{
		return instance.values();
	}

	@Override
	public Set<java.util.Map.Entry<String, Object>> entrySet()
	{
		return instance.entrySet();
	}

}
