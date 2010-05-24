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

import java.util.AbstractSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.WeakHashMap;

/**
 * A hashtable-based <tt>Set</tt> implementation with <em>weak values</em>.
 */

public class WeakHashSet<E> extends AbstractSet<E>
{
	final private WeakHashMap<E, Object> map;

	public WeakHashSet()
	{
		map = new WeakHashMap<E, Object>();
	}

	/**
	 * Constructs a new set containing the elements in the specified collection. The <tt>WeakHashSet</tt> is created with default load factor (0.75) and an
	 * initial capacity sufficient to contain the elements in the specified collection.
	 * 
	 * @param c the collection whose elements are to be placed into this set.
	 * @throws NullPointerException if the specified collection is null.
	 */
	public WeakHashSet(Collection< ? extends E> c)
	{
		map = new WeakHashMap<E, Object>(Math.max((int)(c.size() / .75f) + 1, 16));
		addAll(c);
	}

	/**
	 * Constructs a new, empty set; the backing <tt>WeakHashSet</tt> instance has the specified initial capacity and the specified load factor.
	 * 
	 * @param initialCapacity the initial capacity of the hash map.
	 * @param loadFactor the load factor of the hash map.
	 * @throws IllegalArgumentException if the initial capacity is less than zero, or if the load factor is nonpositive.
	 */
	public WeakHashSet(int initialCapacity, float loadFactor)
	{
		map = new WeakHashMap<E, Object>(initialCapacity, loadFactor);
	}

	/**
	 * Constructs a new, empty set; the backing <tt>WeakHashSet</tt> instance has the specified initial capacity and default load factor, which is
	 * <tt>0.75</tt>.
	 * 
	 * @param initialCapacity the initial capacity of the hash table.
	 * @throws IllegalArgumentException if the initial capacity is less than zero.
	 */
	public WeakHashSet(int initialCapacity)
	{
		map = new WeakHashMap<E, Object>(initialCapacity);
	}

	/**
	 * Returns an iterator over the elements in this set. The elements are returned in no particular order.
	 * 
	 * @return an Iterator over the elements in this set.
	 * @see ConcurrentModificationException
	 */
	@Override
	public Iterator<E> iterator()
	{
		return map.keySet().iterator();
	}

	/**
	 * Returns the number of elements in this set (its cardinality).
	 * 
	 * @return the number of elements in this set (its cardinality).
	 */
	@Override
	public int size()
	{
		return map.size();
	}

	/**
	 * Returns <tt>true</tt> if this set contains no elements.
	 * 
	 * @return <tt>true</tt> if this set contains no elements.
	 */
	@Override
	public boolean isEmpty()
	{
		return map.isEmpty();
	}

	/**
	 * Returns <tt>true</tt> if this set contains the specified element.
	 * 
	 * @param o element whose presence in this set is to be tested.
	 * @return <tt>true</tt> if this set contains the specified element.
	 */
	@Override
	public boolean contains(Object o)
	{
		return map.containsKey(o);
	}

	/**
	 * Adds the specified element to this set if it is not already present.
	 * 
	 * @param o element to be added to this set.
	 * @return <tt>true</tt> if the set did not already contain the specified element.
	 */
	@Override
	public boolean add(E o)
	{
		return map.put(o, Boolean.TRUE) == null;
	}

	/**
	 * Removes the specified element from this set if it is present.
	 * 
	 * @param o object to be removed from this set, if present.
	 * @return <tt>true</tt> if the set contained the specified element.
	 */
	@Override
	public boolean remove(Object o)
	{
		return map.remove(o) == Boolean.TRUE;
	}

	/**
	 * Removes all of the elements from this set.
	 */
	@Override
	public void clear()
	{
		map.clear();
	}

}
