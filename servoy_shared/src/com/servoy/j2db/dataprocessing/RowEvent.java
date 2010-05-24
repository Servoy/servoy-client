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
package com.servoy.j2db.dataprocessing;


import java.util.EventObject;

/**
 * @author jblok
 */
class RowEvent extends EventObject
{
	public static final int INSERT = 1;
	public static final int UPDATE = 0;
	public static final int DELETE = -1;

	private final Row row;
	private final int type;
	private final Object[] changedColumnNames;

	RowEvent(Object source, Row r, int tableModelEventConstant, Object[] changedColumnNames)
	{
		super(source);
		row = r;
		type = tableModelEventConstant;
		this.changedColumnNames = changedColumnNames;
	}

	/**
	 * Returns the type.
	 * 
	 * @return int
	 */
	public int getType()
	{
		return type;
	}

	/**
	 * Returns the row.
	 * 
	 * @return Row
	 */
	public Row getRow()
	{
		return row;
	}

	/**
	 * Which colums have been updated, only used with UPDATE events
	 */
	public Object[] getChangedColumnNames()
	{
		return changedColumnNames;
	}
}
