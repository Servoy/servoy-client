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


import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Enum for one type of Repository types
 * 
 * @author jblok
 */
public class TypeIterator<T extends IPersist> implements Iterator<T>
{
/*
 * _____________________________________________________________ Declaration of attributes
 */
	private final List<IPersist> internalList;
	private int type = 0;
	private int index = 0;

/*
 * _____________________________________________________________ Declaration and definition of constructors
 */
	public TypeIterator(List<IPersist> list, int type)
	{
		internalList = list;
		this.type = type;
		prepareNext();//prepare first
	}

/*
 * _____________________________________________________________ The methods below override methods from superclass <classname>
 */


/*
 * _____________________________________________________________ The methods below belong to interface <interfacename>
 */

	public boolean hasNext()
	{
		return (index != -1);
	}


	private void prepareNext()
	{
		if (index == -1) return; //is already done

		for (int i = index; i < internalList.size(); i++)
		{
			IPersist p = internalList.get(i);
			if (p != null && p.getTypeID() == type)
			{
				index = i;
				return;
			}
		}

		index = -1; //done
	}

	@SuppressWarnings("unchecked")
	public T next()
	{
		if (index == -1)
		{
			throw new NoSuchElementException();
		}
		T obj = (T)internalList.get(index);

		index++;//skip current
		prepareNext();

		return obj;
	}

	public void remove()
	{
		throw new UnsupportedOperationException();
	}

/*
 * _____________________________________________________________ The methods below belong to this class
 */
}
