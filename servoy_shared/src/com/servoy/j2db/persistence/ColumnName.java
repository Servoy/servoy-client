/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2022 Servoy BV

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

import java.io.Serializable;
import java.util.Objects;

/**
 * Simple container for table and column name.
 *
 * @author rgansevles
 *
 */
public class ColumnName implements Serializable
{
	private final String tableName;
	private final String columnName;

	public ColumnName(String tableName, String columnName)
	{
		this.tableName = tableName;
		this.columnName = columnName;
	}

	/**
	 * @return the tableName
	 */
	public String getTableName()
	{
		return tableName;
	}

	/**
	 * @return the columnName
	 */
	public String getColumnName()
	{
		return columnName;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(columnName, tableName);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		ColumnName other = (ColumnName)obj;
		return Objects.equals(columnName, other.columnName) && Objects.equals(tableName, other.tableName);
	}

	@Override
	public String toString()
	{
		return "ColumnName [tableName=" + tableName + ", columnName=" + columnName + "]";
	}


}
