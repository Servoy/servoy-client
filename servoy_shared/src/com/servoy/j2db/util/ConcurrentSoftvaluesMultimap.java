/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2021 Servoy BV

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

import static com.google.common.cache.CacheBuilder.newBuilder;
import static java.util.Collections.emptyList;
import static java.util.Collections.synchronizedList;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * Multimap like implementation with soft reference to the values.
 *
 * Key or value null is not allowed.
 *
 * Equality of values is done using '=='.
 *
 * @author rgansevles
 *
 */
public class ConcurrentSoftvaluesMultimap<K, V>
{
	private final Map<K, List<ConcurrentMap<List< ? >, V>>> cache = newBuilder()
		.softValues()
		.<K, List<ConcurrentMap<List< ? >, V>>> build()
		.asMap();

	public List<V> get(K key)
	{
		List<ConcurrentMap<List< ? >, V>> list = cache.get(key);
		if (list == null)
		{
			return emptyList();
		}

		return list.stream().map(ConcurrentMap::values).flatMap(Collection::stream).collect(toList());
	}

	public void add(K key, V value)
	{
		List<ConcurrentMap<List< ? >, V>> list = cache.get(key);
		if (list == null)
		{
			list = synchronizedList(new ArrayList<ConcurrentMap<List< ? >, V>>()
			{
				@Override
				public int hashCode()
				{
					return 1;
				}

				@Override
				public boolean equals(Object o)
				{
					// do not compare on elements (stackoverflow error)
					return this == o;
				}
			});
			cache.put(key, list);
		}
		else if (list.stream().map(ConcurrentMap::values).flatMap(Collection::stream).anyMatch(v -> v == value))
		{
			// already contains value
			return;
		}

		// put a map in the list with a hard ref to the list and a soft reg to the value.
		// when the value is GC'ed, the listValueRef becomes empty and no longer refers to the list.
		// The the list (soft values in the main map) will then be removed.
		ConcurrentMap<List< ? >, V> listValueRef = newBuilder()
			.softValues()
			.<List< ? >, V> build()
			.asMap();

		listValueRef.put(list, value);
		list.add(listValueRef);
	}

	public V remove(K key, V value)
	{
		List<ConcurrentMap<List< ? >, V>> list = cache.get(key);
		if (list != null)
		{
			Iterator<ConcurrentMap<List< ? >, V>> iterator = list.iterator();
			while (iterator.hasNext())
			{
				if (iterator.next().values().stream().anyMatch(v -> v == value))
				{
					iterator.remove();
					return value;
				}
			}
		}
		return null;
	}

	public Collection<V> allValues()
	{
		return cache.values().stream().flatMap(List::stream).map(Map::values).flatMap(Collection::stream).collect(toList());
	}
}
