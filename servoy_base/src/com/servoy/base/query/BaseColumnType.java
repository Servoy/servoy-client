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
package com.servoy.base.query;

import java.io.Serializable;


/** Container for column types describing type, length and scale (for numerical columns).
 * @author rgansevles
 *
 */
public class BaseColumnType implements Serializable
{
	protected int sqlType;
	protected int length;
	protected int scale;

	public BaseColumnType(int sqlType, int length, int scale)
	{
		this.sqlType = sqlType;
		this.length = length;
		this.scale = scale;
	}

	protected BaseColumnType()
	{
	}

	public int getLength()
	{
		return length;
	}

	public int getScale()
	{
		return scale;
	}

	public int getSqlType()
	{
		return sqlType;
	}

	@Override
	public String toString()
	{
		return "<" + sqlType + ',' + length + ',' + scale + '>'; //$NON-NLS-1$
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + this.length;
		result = prime * result + this.scale;
		result = prime * result + this.sqlType;
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final BaseColumnType other = (BaseColumnType)obj;
		if (this.length != other.length) return false;
		if (this.scale != other.scale) return false;
		if (this.sqlType != other.sqlType) return false;
		return true;
	}
}
