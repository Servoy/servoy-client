/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.j2db.server.ngclient.property.types;

import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IPropertyType;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.property.IFindModeAwarePropertyValue;

/**
 * Interface for types that might be interested in find mode state.
 * For example dataprovider types that are not global/form variables want to know about this so as to no longer do specific conversions
 * when in find mode.
 *
 * @author acostescu
 *
 * @see IFindModeAwarePropertyValue
 */
public interface IFindModeAwareType<FormElementT, T> extends IPropertyType<T>
{

	/**
	 * Returns true if this property type is interested in find mode state or false otherwise.
	 */
	boolean isFindModeAware(FormElementT formElementValue, PropertyDescription pd, FlattenedSolution flattenedSolution, FormElement formElement);

}
