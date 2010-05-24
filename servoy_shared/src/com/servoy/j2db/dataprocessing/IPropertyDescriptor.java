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

/**
 * Interface for describing properties of Column Converters or Validators Try to use the the implementation class {@link PropertyDescriptor} this interface can
 * change with new Servoy versions if new functionality is needed.
 * 
 * @author jcompagner
 * 
 * @since 5.0
 * @see PropertyDescriptor
 */
public interface IPropertyDescriptor
{
	public static final int STRING = 1;
	public static final int NUMBER = 2;
	public static final int GLOBAL_METHOD = 3;

	/**
	 * The label that will be shown in the developer for this property. If null then just the property will be printed.
	 * 
	 * @return The label for this property.
	 */
	public String getLabel();

	/**
	 * The type of the property, return one of the types {@link #STRING} or {@link #GLOBAL_METHOD}
	 * 
	 * @return The type of this property.
	 */
	public int getType();

	/**
	 * For type {@link #STRING} or {@link #NUMBER} you can define predefined list so that users can only choose one of those.
	 * 
	 * @return An array of predefined choices.
	 */
	public String[] getChoices();
}
