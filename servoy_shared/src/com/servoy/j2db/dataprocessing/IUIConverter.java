/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

import java.util.Map;

import com.servoy.j2db.persistence.IColumnTypes;

/**
 * The interface to create ui converters with, when the converter converts from 1 type to the other.
 * 
 * @author rgansevles
 * 
 * @since 6.1
 */
public interface IUIConverter extends IBaseConverter
{
	/**
	 * Get the dataprovider types on which this converter can work.
	 * 
	 * @return the types
	 */
	public int[] getSupportedDataproviderTypes();

	/**
	 * returns the type the converter converts to, should be one of the {@link IColumnTypes}
	 * 
	 * this should return {@link Integer#MAX_VALUE} if the type is unknown, or when the default column type should be used.
	 * 
	 */
	public int getToObjectType(Map<String, String> props);
}
