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


import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;

/**
 * This class is resistent to inserts, adds and gets on positions not defined, and it uses the Utils.equalsObject(...) to perform equals on objects (which is
 * more data compatible).<br>
 * <br>
 * Changed the class to wrap an ArrayList instance instead of extending ArrayList because it is shared with Terracotta when running a cluster of servers and it would no be "portable" because it would extend
 * a "logically managed" class and override some of it's methods.
 *
 * @author jblok
 */
public class SafeArrayList<E> implements Collection<E>, List<E>, RandomAccess, Cloneable, java.io.Serializable
{
	private int initialCapacity;
	private final ArrayList<E> list;

	public SafeArrayList()
	{
		list = new ArrayList<E>(10);
	}

	public SafeArrayList(int initialCapacity)
	{
		list = new ArrayList<E>(initialCapacity);
		this.initialCapacity = initialCapacity;
	}

	public SafeArrayList(Collection< ? extends E> c)
	{
		list = new ArrayList<E>(c);
		initialCapacity = (int)Math.min((c.size() * 110L) / 100, Integer.MAX_VALUE);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#clone()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public SafeArrayList<E> clone()
	{
		return new SafeArrayList<E>((Collection< ? extends E>)list.clone());
	}

	public E get(int index)
	{
		if (index >= 0 && index < list.size())
		{
			return list.get(index);
		}
		return null;
	}

	public E remove(int index)
	{
		if (index >= 0 && index < list.size())
		{
			return list.remove(index);
		}
		return null;
	}

	public void add(int index, E obj)
	{
		int s = list.size();
		if (index > s)
		{
			for (int fill = s; fill < index; fill++)
			{
				list.add(null);
			}
		}
		if (index == s)
		{
			list.add(obj);
		}
		else
		{
			list.add(index, obj);
		}
	}

	public E set(int index, E obj)
	{
		int s = list.size();
		if (index == s)
		{
			list.add(obj);
			return null;
		}

		if (index > s)
		{
			for (int fill = s; fill <= index; fill++)
			{
				list.add(null);
			}
		}
		if (obj == null && index == s - 1 && index >= initialCapacity)
		{
			return remove(index);
		}
		else
		{
			return list.set(index, obj);
		}
	}

	/**
	 * Searches for the first occurence of the given argument, testing for equality using the <tt>equals</tt> method.
	 *
	 * @param elem an object.
	 * @return the index of the first occurrence of the argument in this list; returns <tt>-1</tt> if the object is not found.
	 * @see Object#equals(Object)
	 */
	public int indexOf(Object elem)
	{
		if (elem == null)
		{
			for (int i = 0; i < list.size(); i++)
			{
				if (get(i) == null)
				{
					return i;
				}
			}
		}
		else
		{
			for (int i = 0; i < list.size(); i++)
			{
				if (Utils.equalObjects(elem, get(i)))
				{
					return i;
				}
			}
		}
		return -1;
	}

	/**
	 * Returns the index of the last occurrence of the specified object in this list.
	 *
	 * @param elem the desired element.
	 * @return the index of the last occurrence of the specified object in this list; returns -1 if the object is not found.
	 */
	public int lastIndexOf(Object elem)
	{
		if (elem == null)
		{
			for (int i = list.size() - 1; i >= 0; i--)
			{
				if (get(i) == null)
				{
					return i;
				}
			}
		}
		else
		{
			for (int i = list.size() - 1; i >= 0; i--)
			{
				if (Utils.equalObjects(elem, get(i)))
				{
					return i;
				}
			}
		}
		return -1;
	}

	public boolean add(E o)
	{
		return list.add(o);
	}

	public boolean addAll(Collection< ? extends E> c)
	{
		return list.addAll(c);
	}

	public void clear()
	{
		list.clear();
	}

	public boolean contains(Object o)
	{
		return list.contains(o);
	}

	public boolean containsAll(Collection< ? > c)
	{
		return list.containsAll(c);
	}

	public boolean isEmpty()
	{
		return list.isEmpty();
	}

	public Iterator<E> iterator()
	{
		return list.iterator();
	}

	public boolean remove(Object o)
	{
		return list.remove(o);
	}

	public boolean removeAll(Collection< ? > c)
	{
		return list.removeAll(c);
	}

	public boolean retainAll(Collection< ? > c)
	{
		return list.retainAll(c);
	}

	public int size()
	{
		return list.size();
	}

	public Object[] toArray()
	{
		return list.toArray();
	}

	public <T> T[] toArray(T[] a)
	{
		return list.toArray(a);
	}

	public boolean addAll(int index, Collection< ? extends E> c)
	{
		return list.addAll(index, c);
	}

	public ListIterator<E> listIterator()
	{
		return list.listIterator();
	}

	public ListIterator<E> listIterator(int index)
	{
		return list.listIterator(index);
	}

	public List<E> subList(int fromIndex, int toIndex)
	{
		return list.subList(fromIndex, toIndex);
	}

	@Override
	public String toString()
	{
		return list.toString();
	}

}
