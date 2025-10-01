/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2024 Servoy BV

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

/**
 * A key based on the object reference equality of a number of values.
 *
 * @author rgansevles
 *
 */
public class ObjectKey
{
	private final Object[] values;

	public ObjectKey(Object... values)
	{
		this.values = values;
	}

	@Override
	public int hashCode()
	{
		int prime = 31;
		int result = 1;
		for (Object value : values)
		{
			result = prime * result + (value == null ? 0 : value.hashCode());
		}
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		ObjectKey other = (ObjectKey)obj;
		if (this.values.length != other.values.length)
		{
			return false;
		}
		for (int i = 0; i < values.length; i++)
		{
			if (values[i] != other.values[i])
			{
				return false;
			}
		}
		return true;
	}
}
