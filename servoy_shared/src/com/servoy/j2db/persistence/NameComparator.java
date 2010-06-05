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
public class NameComparator implements Comparator<Object>
{
	public static final Comparator<Object> INSTANCE = new NameComparator();

	private NameComparator()
	{
	}

	/**
	 * @see java.util.Comparator#compare(Object, Object)
	 */
	public int compare(Object o1, Object o2)
	{
		if (o1 instanceof ISupportName && o2 instanceof ISupportName)
		{
			String name1 = ((ISupportName)o1).getName();
			String name2 = ((ISupportName)o2).getName();
			if (name1 == name2) return 0; // for both null cases mainly
			else if (name1 == null) return -1;
			else if (name2 == null) return 1;
			return name1.compareToIgnoreCase(name2);
		}
		else if (o1 instanceof ISupportName && !(o2 instanceof ISupportName))
		{
			return 1;
		}
		else if (!(o1 instanceof ISupportName) && o2 instanceof ISupportName)
		{
			return -1;
		}
		else if (o1 != null && o2 != null)
		{
			return o1.toString().compareToIgnoreCase(o2.toString());
		}
		else if (o1 == null && o2 == null)
		{
			return 0;
		}
		else if (o1 == null)
		{
			return -1;
		}
		else
		{
			return 1; // o2 == null
		}
	}
}
