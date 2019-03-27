/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2018 Servoy BV

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

import com.servoy.j2db.query.ColumnType;

/**
 * @author rob
 *
 */
public class ProcedureColumn implements Serializable
{

	private final String name;
	private final ColumnType columnType;

	/**
	 * @param colDatatype
	 * @param columnType
	 */
	public ProcedureColumn(String name, ColumnType columnType)
	{
		this.name = name;
		this.columnType = columnType;
	}

	/**
	 * @return the name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * @return the columnType
	 */
	public ColumnType getColumnType()
	{
		return columnType;
	}

	@Override
	public String toString()
	{
		return "ProcedureColumn[" + name + "," + columnType + "]";
	}

}
