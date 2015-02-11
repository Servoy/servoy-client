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

import com.servoy.j2db.server.ngclient.property.ComponentTypeSabloValue;
import com.servoy.j2db.server.ngclient.property.IDataLinkedPropertyValue;
import com.servoy.j2db.util.Pair;

/**
 * An IDataLinkedType that works with wrapped {@link IDataLinkedPropertyValue} instances. So the value of the property will use a wrapped {@link IDataLinkedPropertyValue}
 * that can register as listener to the DAL instead of the actual value of the property registering to the DAL.
 *
 * TODO this interface is only needed for some code in {@link ComponentTypeSabloValue} that searches for a root property name based on a possibly nested value; it should be refactored somehow.
 *
 * @author acostescu
 * @see IDataLinkedType
 */
public interface IWrapperDataLinkedType<FormElementT, T> extends IDataLinkedType<FormElementT, T>
{

	/**
	 * Returns the nested data linked property value if any and it's (so nested) property description.
	 */
	Pair<IDataLinkedPropertyValue, PropertyDescription> getWrappedDataLinkedValue(T propertyValue, PropertyDescription pd);

}
