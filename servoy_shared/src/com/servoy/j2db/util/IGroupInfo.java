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


import java.util.Comparator;

public interface IGroupInfo
{
	/**
	 * Get the number of fields to group by.
	 * 
	 * @return the number of fields to group by
	 */
	int getFieldCount();

	/**
	 * Get the <code>i</code>th group field value
	 * 
	 * @param object the object to get the field value for
	 * @param i the group field index
	 * 
	 * @return the <code>i</code>th group field value
	 */
	Object getFieldValue(Object object, int i);

	/**
	 * Return the comparator to sort the <code>i</code>th group field by.
	 * A <code>null</code> value specifies the default sort order.
	 * 
	 * Make sure the comparator never returns "0", because that would mean replacing existing values (because of TreeMap).
	 * 
	 * @param i the group field index
	 * 
	 * @return the <code>i</code>th group field comparator
	 */
	Comparator getFieldComparator(int i);

	/**
	 * Return the comparator to sort objects in the same group by.
	 * 
	 * @return the comparator to sort objects in the same group by.
	 */
	Comparator getObjectComparator();

}
