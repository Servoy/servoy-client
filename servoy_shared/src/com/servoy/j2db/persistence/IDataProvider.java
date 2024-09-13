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
package com.servoy.j2db.persistence;

import com.servoy.base.persistence.constants.IColumnTypeConstants;

public interface IDataProvider
{
	String getDataProviderID(); // get the id of the provider

	/**
	 * Leaves in dataprovider tree can be columns
	 *
	 * @return a column or column and relation, null if no database column depenency
	 */
	ColumnWrapper getColumnWrapper();

	int getLength(); // max length the provider can hold, especially used by columns varchars, -1 or 0 means is undefined

	boolean isEditable();

	int getFlags();

	/**
	 * @param flag
	 */
	default boolean hasFlag(int flag)
	{
		return (getFlags() & flag) != 0;
	}

	/**
	 * returns a in the Column class defined type,return -1 if unkown or can be anything (happpens only in script calcs for now)
	 */
	int getDataProviderType();

	default String getDataProviderTypeName()
	{
		return getTypeName(getDataProviderType());
	}

	public static String getTypeName(int dataProviderType)
	{
		switch (dataProviderType)
		{
			case IColumnTypeConstants.DATETIME :
				return "DATETIME";
			case IColumnTypeConstants.TEXT :
				return "TEXT";
			case IColumnTypeConstants.NUMBER :
				return "NUMBER";
			case IColumnTypeConstants.INTEGER :
				return "INTEGER";
			case IColumnTypeConstants.MEDIA :
				return "MEDIA";
			default :
				return null;
		}
	}

}
