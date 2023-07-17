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

package com.servoy.j2db.dataprocessing;

import java.io.Serializable;

/**
 * Server-side filters used for data broadcast
 *
 * @author rgansevles
 *
 */
public class BroadcastFilter implements Serializable
{
	private static final long serialVersionUID = 1L;

	private final String tableName;
	private final BroadcastFilterOperator filterType;
	private final String columnName;
	private final Object[] filterValue;

	public BroadcastFilter(String tableName, String columnName, BroadcastFilterOperator filterType, Object[] filterValue)
	{
		this.tableName = tableName;
		this.columnName = columnName;
		this.filterType = filterType;
		this.filterValue = filterValue;
	}

	/**
	 * @return the tableName
	 */
	public String getTableName()
	{
		return tableName;
	}

	/**
	 * @return the filterType
	 */
	public BroadcastFilterOperator getFilterType()
	{
		return filterType;
	}

	/**
	 * @return the columnName
	 */
	public String getColumnName()
	{
		return columnName;
	}

	/**
	 * @return the filterValue
	 */
	public Object[] getFilterValue()
	{
		return filterValue;
	}

	// Currently only one operator supported
	public enum BroadcastFilterOperator
	{
		IN // used for 'in' and '=' filters
	}
}
