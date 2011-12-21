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
 * Public base interface for a converter.
 * 
 * @see IColumnConverter
 * @see IUIConverter
 * 
 * @author jblok, rgansevles
 * 
 */
public interface IBaseConverter extends IColumnTypes
{
	/**
	 * Convert from input data type to a converted object.
	 * <p>For column converters, this method converts from db value to dataprovider value.
	 * <p>For ui converters, this method converts from dataprovider value to ui value.
	 * 
	 * @param props properties for this converter
	 * @param input_type the type of the object
	 * @param input the value to convert
	 * @return the converted object
	 * @throws Exception
	 */
	public Object convertToObject(Map<String, String> props, int input_type, Object input) throws Exception;

	/**
	 * Convert from a converted object to the input type.
	 * <p>For column converters, this method converts from to dataprovider value to db value.
	 * <p>For ui converters, this method converts from ui value to dataprovider value.
	 * 
	 * @param props properties for this converter
	 * @param input_type the Object to convert
	 * @param converted the input value
	 * @return the value in the input type
	 */
	public Object convertFromObject(Map<String, String> props, int input_type, Object converted) throws Exception;

	/**
	 * Called for default properties about the converter.
	 */
	public Map<String, String> getDefaultProperties();

	/**
	 * Get the name for this converter
	 */
	public String getName();
}
