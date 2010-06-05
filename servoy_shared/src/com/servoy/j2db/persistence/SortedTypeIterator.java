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
package com.servoy.j2db.persistence;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Enum for one type of Repository types
 * 
 * @author jblok
 */
public class SortedTypeIterator<T extends IPersist> implements Iterator<T>
{
/*
 * _____________________________________________________________ Declaration of attributes
 */
	private List<IPersist> internalList;
	private int index = 0;

/*
 * _____________________________________________________________ Declaration and definition of constructors
 */

	@SuppressWarnings("unchecked")
	public SortedTypeIterator(List<IPersist> list, int type, Comparator c)
	{
		if (type > 0)
		{
			//filter
			internalList = createFilteredList(list, type);
		}
		else
		{
			internalList = list;
		}

		IPersist[] a = internalList.toArray(new IPersist[internalList.size()]);
		Arrays.sort(a, c);
		internalList = Arrays.asList(a);
	}

/*
 * _____________________________________________________________ The methods below override methods from superclass <classname>
 */


/*
 * _____________________________________________________________ The methods below belong to interface <interfacename>
 */

	public boolean hasNext()
	{
		return (index < internalList.size());
	}

	@SuppressWarnings("unchecked")
	public T next()
	{
		T obj = (T)internalList.get(index);
		index++;
		return obj;
	}

	public void remove()
	{
		throw new UnsupportedOperationException();
	}

/*
 * _____________________________________________________________ The methods below belong to this class
 */

	@SuppressWarnings("unchecked")
	public static <T> List<T> createFilteredList(List<IPersist> list, int type)
	{
		List<T> array = new ArrayList<T>();
		if (list != null)
		{
			for (int i = 0; i < list.size(); i++)
			{
				IPersist p = list.get(i);
				if (p != null && p.getTypeID() == type)
				{
					array.add((T)p);
				}
			}
		}
		return array;
	}
}
