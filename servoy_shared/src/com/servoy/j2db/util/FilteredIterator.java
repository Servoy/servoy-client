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

import java.util.Iterator;


/**
 * Iterator that filters based on IFilter.
 * 
 * The filter must make sure that only objects of type T are returned.
 * 
 * @author rob
 * 
 * @param <T>
 */
public class FilteredIterator<T> implements Iterator<T>
{
	private final Iterator< ? > iterator;
	private final IFilter<T> filter;

	private Object nextObject = null;
	private boolean nextObjectSet = false;

	public FilteredIterator(Iterator< ? > iterator, IFilter<T> filter)
	{
		this.iterator = iterator;
		this.filter = filter;
	}

	public boolean hasNext()
	{
		while (!nextObjectSet && iterator.hasNext())
		{
			nextObject = iterator.next();
			if (filter.match(nextObject))
			{
				// now we know nextObject is of class T
				nextObjectSet = true;
			}
			else
			{
				nextObject = null;
			}
		}
		return nextObjectSet;
	}

	public T next()
	{
		if (hasNext())
		{
			nextObjectSet = false;
			T res = (T)nextObject; // filter must make sure type matches
			nextObject = null;
			return res;
		}
		return null;
	}

	public void remove()
	{
		if (nextObjectSet)
		{
			nextObjectSet = false;
			nextObject = null;
			iterator.remove();
		}
	}

}
