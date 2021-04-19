/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2020 Servoy BV

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
package com.servoy.base.query;

import java.io.Serializable;


/**
 * Holder for type information like column type and native tyep name.
 *
 * @author rgansevles
 *
 */
public class TypeInfo implements Serializable
{
	private final BaseColumnType columnType;
	private final String nativeTypename;

	public TypeInfo(BaseColumnType columnType, String nativeTypename)
	{
		this.columnType = columnType;
		this.nativeTypename = nativeTypename;
	}


	/**
	 * @return the columnType
	 */
	public BaseColumnType getColumnType()
	{
		return columnType;
	}

	/**
	 * @return the nativeTypename
	 */
	public String getNativeTypename()
	{
		return nativeTypename;
	}

	@Override
	public String toString()
	{
		return columnType + (nativeTypename == null ? "" : ("(" + nativeTypename + ")"));
	}


	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((columnType == null) ? 0 : columnType.hashCode());
		result = prime * result + ((nativeTypename == null) ? 0 : nativeTypename.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		TypeInfo other = (TypeInfo)obj;
		if (columnType == null)
		{
			if (other.columnType != null) return false;
		}
		else if (!columnType.equals(other.columnType)) return false;
		if (nativeTypename == null)
		{
			if (other.nativeTypename != null) return false;
		}
		else if (!nativeTypename.equals(other.nativeTypename)) return false;
		return true;
	}


}
