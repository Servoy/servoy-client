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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
	private final ReferenceQueue<V> queue = new ReferenceQueue<V>();

	private final Map<K, List<SoftReferenceWithData<V, K>>> cache = newBuilder()
		.<K, List<SoftReferenceWithData<V, K>>> build()
		.asMap();

	public Collection<V> get(K key)
	{
		List<SoftReferenceWithData<V, K>> list = cache.get(key);
		if (list == null)
		{
			return emptyList();
		}
		return list.stream().map(SoftReference::get).filter(Objects::nonNull).collect(toList());
	}

	public void add(K key, V value)
	{
		List<SoftReferenceWithData<V, K>> list = cache.get(key);
		if (list == null)
		{
			cache.put(key, Collections.singletonList(new SoftReferenceWithData<V, K>(value, queue, key)));
		}
		else if (list.stream().map(SoftReference::get).anyMatch(v -> v == value))
		{
			// already contains value
			return;
		}
		else
		{
			// if it is a list with 1 item its a single immutable list, create now a copy
			if (list.size() == 1)
			{
				list = synchronizedList(new ArrayList<SoftReferenceWithData<V, K>>(list));
				cache.put(key, list);
			}
			list.add(new SoftReferenceWithData<V, K>(value, queue, key));
		}
		cleanup();
	}

	public V remove(K key, V value)
	{
		List<SoftReferenceWithData<V, K>> list = cache.get(key);
		if (list != null)
		{
			// if it is a list with 1 item its a single immutable list that can be removed from. just test the single value.
			if (list.size() == 1)
			{
				V v = list.get(0).get();
				if (v == null || v == value)
				{
					cache.remove(key);
					cleanup();
					return value;
				}
				cleanup();
			}
			else
			{
				try
				{
					Iterator<SoftReferenceWithData<V, K>> iterator = list.iterator();
					while (iterator.hasNext())
					{
						SoftReferenceWithData<V, K> ref = iterator.next();
						V val = ref.get();
						if (val == null) iterator.remove();
						else if (val == value)
						{
							iterator.remove();
							return value;
						}
					}
				}
				finally
				{
					if (list.size() == 0) cache.remove(key);
					cleanup();
				}
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private void cleanup()
	{
		SoftReferenceWithData<V, K> removedRef = (SoftReferenceWithData<V, K>)queue.poll();
		while (removedRef != null)
		{
			K key = removedRef.getData();
			List<SoftReferenceWithData<V, K>> list = cache.get(key);
			if (list != null)
			{
				if (list.size() == 1)
				{
					// single soft ref that is queued so this one can be removed right away
					cache.remove(key);
				}
				else
				{
					Iterator<SoftReferenceWithData<V, K>> iterator = list.iterator();
					while (iterator.hasNext())
					{
						if (iterator.next().get() == null) iterator.remove();
					}
					if (list.size() == 0) cache.remove(key);
				}
			}
			removedRef = (SoftReferenceWithData<V, K>)queue.poll();
		}
	}

	public Collection<V> allValues()
	{
		return cache.values().stream().flatMap(Collection::stream).map(SoftReference::get).filter(Objects::nonNull).collect(toList());
	}
}
