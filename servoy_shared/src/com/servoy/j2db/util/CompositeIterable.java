/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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
 * Iterable to combine multiple iterables into 1.
 * 
 * @author rgansevles
 * 
 * @since 6.1
 *
 */
public class CompositeIterable<T> implements Iterable<T>
{
	private final Iterable< ? extends T>[] iterables;
	Iterator< ? extends T> iterator;
	int n = 0;

	public CompositeIterable(Iterable< ? extends T>... iterables)
	{
		this.iterables = iterables;
	}

	public Iterator<T> iterator()
	{
		return new Iterator<T>()
		{
			public boolean hasNext()
			{
				if (iterables == null)
				{
					return false;
				}

				for (; iterator == null && n < iterables.length; n++)
				{
					if (iterables[n] != null)
					{
						iterator = iterables[n].iterator();
					}
				}

				if (iterator == null)
				{
					return false;
				}

				if (iterator.hasNext())
				{
					return true;
				}

				iterator = null;
				return hasNext();
			}

			public T next()
			{
				if (hasNext())
				{
					return iterator.next();
				}
				return null;
			}

			public void remove()
			{
				if (iterator != null || hasNext())
				{
					iterator.remove();
				}
			}
		};
	}
}
