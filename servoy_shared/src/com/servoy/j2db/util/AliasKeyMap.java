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

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * Map with alias for some of the key values.
 *
 * @author rgansevles
 *
 */
public class AliasKeyMap<K, A, V> extends AbstractMap<K, V> implements Serializable
{
	private final Map<K, V> map;
	private Map<A, K> aliases;
	private final AliasGetter<A, V> aliasGetter;

	public AliasKeyMap(Map<K, V> map, AliasGetter<A, V> aliasGetter)
	{
		this.map = map;
		this.aliasGetter = aliasGetter;
	}

	public AliasKeyMap(Map<K, V> map)
	{
		this(map, null);
	}

	@SuppressWarnings("unchecked")
	protected A getAlias(V value)
	{
		if (aliasGetter != null)
		{
			return aliasGetter.getAlias(value);
		}
		if (value instanceof ISupportAlias< ? >)
		{
			return ((ISupportAlias< ? extends A>)value).getAlias();
		}
		return null;
	}

	@Override
	public V put(K key, V value)
	{
		V old = map.put(key, value);
		if (old != null) removeAlias(key); // old alias must go away; even if the put value is the same, it's alias might have changed

		A alias = getAlias(value);
		if (alias != null && !alias.equals(key))
		{
			if (aliases == null)
			{
				aliases = new HashMap<A, K>();
			}
			aliases.put(alias, key);
		}
		return old;
	}

	@Override
	public V remove(Object key)
	{
		return super.remove(removeAlias(key));
	}

	@SuppressWarnings("unchecked")
	protected K removeAlias(Object key)
	{
		if (aliases == null) return (K)key;

		if (aliases.containsKey(key))
		{
			return aliases.remove(key);
		}

		K realKey = (K)key;
		// find mappings to old key
		if (realKey != null)
		{
			Iterator<K> iterator = aliases.values().iterator();
			while (iterator.hasNext())
			{
				if (realKey.equals(iterator.next()))
				{
					iterator.remove();
					break;
				}
			}
		}
		return realKey;
	}

	@Override
	public V get(Object key)
	{
		return map.get((aliases != null && aliases.containsKey(key)) ? aliases.get(key) : key);
	}

	@Override
	public boolean containsKey(Object key)
	{
		return (aliases != null && aliases.containsKey(key)) || super.containsKey(key);
	}

	/**
	 * @param oldKey
	 */
	public void updateAlias(Object oldKey)
	{
		K realKey = removeAlias(oldKey);

		if (map.containsKey(realKey))
		{
			A alias = getAlias(map.get(realKey));
			if (alias != null && !alias.equals(realKey))
			{
				if (aliases == null)
				{
					aliases = new HashMap<A, K>();
				}
				aliases.put(alias, realKey);
			}
		}
	}

	@Override
	public void clear()
	{
		super.clear();
		aliases = null;
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet()
	{
		return map.entrySet();
	}

	public static interface ISupportAlias<A>
	{
		A getAlias();
	}

	public static interface AliasGetter<A, V> extends Serializable
	{
		A getAlias(V value);
	}

}
