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


import java.util.Comparator;

/**
 * @author jblok
 */
public class SizeComparator implements Comparator
{

	/*
	 * @see Comparator#compare(Object, Object)
	 */
	public int compare(Object o1, Object o2)
	{
		if (o1 instanceof ISupportBounds && o2 instanceof ISupportBounds)
		{
			ISupportBounds p1 = (ISupportBounds)o1;
			ISupportBounds p2 = (ISupportBounds)o2;
			int diff = (p2.getSize().width * p2.getSize().height - p1.getSize().width * p1.getSize().height);
			return diff;
		}
		return 0;
	}

}