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

package com.servoy.j2db.util;

import java.util.List;


/**
 * CSS rule interface to retrieve attributes.
 * 
 * @author lvostinar
 * @since 6.1
 * @see IStyleSheet
 */
public interface IStyleRule
{
	/**
	* Returns the number of attributes contained in this set.
	*
	* @return the number of attributes >= 0
	*/
	public int getAttributeCount();

	/**
	 * Checks whether the attribute exists in the set.
	 *
	 * @param attributeName the attribute name
	 * @return true if the attribute has a value specified
	 */
	public boolean hasAttribute(String attributeName);

	/**
	 * Fetches the value of the given attribute, as defined in CSS.
	 *
	 * @param key the non-null key of the attribute binding
	 * @return the value
	 */
	public String getValue(String attributeName);

	/**
	* Returns a list with the names of the attributes in the set(CSS rule).
	*
	* @return the names
	*/
	public List<String> getAttributeNames();
}
