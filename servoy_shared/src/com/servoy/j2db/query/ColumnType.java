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
package com.servoy.j2db.query;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;


/** Container for column types describing type, length and scale (for numerical columns).
 * @author rob
 *
 */
public class ColumnType implements Serializable
{
	private int sqlType;
	private int length;
	private int scale;
	
	private static Map instances = new HashMap();
	
	private ColumnType(int sqlType, int length, int scale)
	{
		this.sqlType = sqlType;
		this.length = length;
		this.scale = scale;
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
	
	static ColumnType getInstance(int sqlType, int length, int scale)
	{
		ColumnType instance;
		synchronized (instances)
		{
			ColumnType ct = new ColumnType(sqlType, length, scale);
			instance = (ColumnType)instances.get(ct);
			if (instance == null)
			{
				instances.put(ct, ct);
				instance = ct;
			}
		}
		return instance;
	}
	
	public String toString()
	{
		return "<"+sqlType+','+length+','+scale+'>'; //$NON-NLS-1$
	}

	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + this.length;
		result = prime * result + this.scale;
		result = prime * result + this.sqlType;
		return result;
	}

	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final ColumnType other = (ColumnType) obj;
		if (this.length != other.length) return false;
		if (this.scale != other.scale) return false;
		if (this.sqlType != other.sqlType) return false;
		return true;
	}
}
