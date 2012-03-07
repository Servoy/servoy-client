/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * Map to wrap another map that does not allow null key or value.
 * 
 * @author rgansevles
 *
 */
public class AllowNullMap<K, V> extends AbstractMap<K, V> implements Serializable
{
	private static final Object NULL = new Object()
	{
		@Override
		public String toString()
		{
			return "<NULL>"; //$NON-NLS-1$
		}
	};

	private final Map<K, V> map;

	public AllowNullMap(Map<K, V> map)
	{
		this.map = map;
	}

	@SuppressWarnings("unchecked")
	@Override
	public V put(K key, V value)
	{
		Object oldValue = map.put((K)(key == null ? NULL : key), (V)(value == null ? NULL : value));
		return (V)(oldValue == NULL ? null : oldValue);
	}

	@SuppressWarnings("unchecked")
	@Override
	public V get(Object key)
	{
		Object get = map.get(key == null ? NULL : key);
		return (V)(get == NULL ? null : get);
	}

	@Override
	public V remove(Object key)
	{
		return map.remove(key == null ? NULL : key);
	}

	@Override
	public boolean containsKey(Object key)
	{
		return map.containsKey(key == null ? NULL : key);
	}

	@Override
	public Set<Entry<K, V>> entrySet()
	{
		return new NullkeyEntrySet<K, V>(map.entrySet());
	}

	private static class NullkeyEntrySet<K, V> extends AbstractSet<Entry<K, V>>
	{
		private final Set<Entry<K, V>> set;

		public NullkeyEntrySet(Set<Entry<K, V>> set)
		{
			this.set = set;
		}

		@Override
		public int size()
		{
			return set.size();
		}

		@Override
		public Iterator<Entry<K, V>> iterator()
		{
			final Iterator<Entry<K, V>> iterator = set.iterator();
			return new Iterator<Map.Entry<K, V>>()
			{

				public boolean hasNext()
				{
					return iterator.hasNext();
				}

				public Entry<K, V> next()
				{
					final Entry<K, V> next = iterator.next();
					return new Entry<K, V>()
					{
						@SuppressWarnings("unchecked")
						public K getKey()
						{
							Object key = next.getKey();
							return (K)(key == NULL ? null : key);
						}

						@SuppressWarnings("unchecked")
						public V getValue()
						{
							Object value = next.getValue();
							return (V)(value == NULL ? null : value);
						}

						@SuppressWarnings("unchecked")
						public V setValue(V value)
						{
							Object oldval = next.setValue((V)(value == null ? NULL : value));
							return (V)(oldval == NULL ? null : oldval);
						}
					};
				}

				public void remove()
				{
					iterator.remove();
				}
			};
		}
	}
}
