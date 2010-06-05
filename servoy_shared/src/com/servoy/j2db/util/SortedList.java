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


import java.util.AbstractList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;

/**
 * This list remains sorted
 * 
 * @author jcompagner
 */
public class SortedList<E> extends AbstractList<E>
{
	protected int _size;
	protected Object[] _data;
	protected Comparator _comp;
	private int _index;

	public SortedList()
	{
		this(COMPARABLE_COMPARATOR, 25);
	}

	public SortedList(int initialSize)
	{
		this(COMPARABLE_COMPARATOR, initialSize);
	}

	public SortedList(Collection< ? extends E> collection)
	{
		this(COMPARABLE_COMPARATOR, collection);
	}

	public SortedList(Comparator< ? > comp)
	{
		this(comp, 25);
	}

	public SortedList(Comparator< ? > comp, int initialSize)
	{
		_comp = comp;
		_data = new Object[initialSize];
	}

	public SortedList(Comparator< ? > comp, Collection< ? extends E> collection)
	{
		_comp = comp;
		_data = new Object[collection.size() + 25];
		addAll(collection);
	}

	/**
	 * @see java.util.List#get(int)
	 */
	@Override
	public E get(int index)
	{
		return (E)_data[index];
	}

	/**
	 * @see java.util.Collection#size()
	 */
	@Override
	public int size()
	{
		return _size;
	}

	/**
	 * @see java.util.Collection#add(java.lang.Object)
	 */
	@Override
	public boolean add(E o)
	{
		int index = indexOfIntern(o);
		if (index < 0) index = -(index + 1);
		try
		{
			if (index != _size)
			{
				System.arraycopy(_data, index, _data, index + 1, _size - index);
			}
			_data[index] = o;
		}
		catch (ArrayIndexOutOfBoundsException ex)
		{
			Object[] data = new Object[_size + 25];
			System.arraycopy(_data, 0, data, 0, index);
			System.arraycopy(_data, index, data, index + 1, _size - index);
			data[index] = o;
			_data = data;
		}
		_index = index;
		_size++;
		return true;
	}

	public void addAll(Iterator< ? extends E> it)
	{
		while (it.hasNext())
		{
			add(it.next());
		}
	}

	@Override
	public boolean addAll(Collection< ? extends E> c)
	{
		Iterator< ? extends E> it = c.iterator();
		boolean retval = it.hasNext();
		addAll(it);
		return retval;
	}

	public int getInsertIndex()
	{
		return _index;
	}

	@Override
	public int indexOf(Object o)
	{
		int index = indexOfIntern(o);
		return index < 0 ? -1 : index;
	}

	/**
	 * can return number lower then -1!!!
	 */
	protected int indexOfIntern(Object o)
	{
		int low = 0;
		int high = _size - 1;
		while (low <= high)
		{
			int mid = (low + high) >> 1;
			E midVal = (E)_data[mid];
			int cmp = _comp.compare(midVal, o);
			if (cmp < 0) low = mid + 1;
			else if (cmp > 0) high = mid - 1;
			else return mid; // key found
		}
		return -(low + 1);
	}

	/**
	 * @see java.util.Collection#contains(java.lang.Object)
	 */
	@Override
	public boolean contains(Object o)
	{
		return indexOfIntern(o) >= 0;
	}

	/**
	 * @see java.util.List#remove(int)
	 */
	@Override
	public E remove(int index)
	{
		if (index >= _size || index < 0) throw new ArrayIndexOutOfBoundsException("index greater than size or below zero for remove " + index); //$NON-NLS-1$
		E data = (E)_data[index];
		System.arraycopy(_data, index + 1, _data, index, _size - index - 1);
		_size--;
		_data[_size] = null;
		return data;
	}

	/**
	 * @see java.util.Collection#remove(java.lang.Object)
	 */
	@Override
	public boolean remove(Object o)
	{
		int index = indexOfIntern(o);
		if (index >= 0) remove(index);
		return index >= 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.AbstractList#clear()
	 */
	@Override
	public void clear()
	{
		_size = 0;
		_data = new Object[_data.length];
	}

	public static final Comparator<Object> COMPARABLE_COMPARATOR = new ComparableComparator();

	public static class ComparableComparator implements Comparator<Object>
	{
		/*
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		public int compare(Object o1, Object o2)
		{
			return ((Comparable)o1).compareTo(o2);
		}
	}
}
