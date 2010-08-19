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

import java.util.Map;

import com.servoy.j2db.persistence.IColumnTypes;

/**
 * Public interface for a converter, this one is deprecated from 5.0 on Please use {@link ITypedColumnConverter} from 5.0 on so that the converter will return
 * its to object type.
 * 
 * @author jblok
 * 
 * @see {@link ITypedColumnConverter}
 */
public interface IColumnConverter extends IColumnTypes
{
	public final String TO_OBJECT_NAME_PROPERTY = "globalConverterToObjectMethodName"; //$NON-NLS-1$
	public final String FROM_OBJECT_NAME_PROPERTY = "globalConverterFromObjectMethodName"; //$NON-NLS-1$
	public final String TYPE_NAME_PROPERTY = "type"; //$NON-NLS-1$

	/**
	 * Convert from column type to an Object.
	 * 
	 * @param props properties for this converter
	 * @param dbvalue the db value to convert
	 * @return the object
	 * @throws Exception
	 */
	public Object convertToObject(Map<String, String> props, int column_type, Object dbvalue) throws Exception;

	/**
	 * Convert from Object to column type.
	 * 
	 * @param props properties for this converter
	 * @param column_type the Object to convert
	 * @param obj the input value
	 * @return the value to be stored in the db
	 */
	public Object convertFromObject(Map<String, String> props, int column_type, Object obj) throws Exception;

	/**
	 * Called for default properties about the converter.
	 */
	public Map<String, String> getDefaultProperties();

	/**
	 * Get the database output types on which this converter can work.
	 * 
	 * @return the types
	 */
	public int[] getSupportedColumnTypes();

	/**
	 * Get the name for this converter
	 */
	public String getName();
}
