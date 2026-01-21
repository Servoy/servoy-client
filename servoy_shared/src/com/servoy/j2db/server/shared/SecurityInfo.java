/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

package com.servoy.j2db.server.shared;


/**
 * @author edit
 *
 */
public class SecurityInfo implements Comparable<SecurityInfo>
{
	public final String element_uid; // element UUID (PersistIdentifier.toJSONString() in case of form elements) or column name for tables
	public final int access;

	public SecurityInfo(String element_uid, int access)
	{
		this.element_uid = element_uid;
		this.access = access;
	}

	public String getName()
	{
		return element_uid;
	}

	// needed in order for remove(Object) to work correctly in a sorted list
	public int compareTo(SecurityInfo o)
	{
		int result;
		if (element_uid == o.element_uid)
		{
			result = 0;
		}
		else if (element_uid == null) result = -1;
		else if (o.element_uid == null) result = 1;
		else result = element_uid.compareToIgnoreCase(o.element_uid); // case insensitive sort

		if (result == 0) result = element_uid.compareTo(o.element_uid); // do not consider equal ones that match case insensitive
		if (result == 0)
		{
			// names are the same; check pwd
			result = access - o.access;
		}

		return result;
	}

	// needed in order for remove(Object) to work correctly in an ArrayList
	@Override
	public boolean equals(Object o)
	{
		if (o instanceof SecurityInfo) return (compareTo((SecurityInfo)o) == 0);
		return false;
	}

}
