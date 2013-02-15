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

import java.util.Comparator;

/**
 * Sort based on a list of comparators, first one that does not return 0 wins.
 * 
 * @author rgansevles
 *
 */
public class CompositeComparator<T> implements Comparator<T>
{
	private final Comparator< ? super T>[] comparators;

	public CompositeComparator(Comparator< ? super T>... comparators)
	{
		this.comparators = comparators;
	}

	public final int compare(T o1, T o2)
	{
		for (Comparator< ? super T> comparator : comparators)
		{
			int c = comparator.compare(o1, o2);
			if (c != 0) return c;
		}

		return 0;
	}
}
