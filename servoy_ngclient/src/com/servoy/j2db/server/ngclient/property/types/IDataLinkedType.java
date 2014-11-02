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

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.property.IServoyAwarePropertyValue;

/**
 * Interface for types that can have their value linked to data in a record or in a form/global variable.
 * For example dataprovider types can be linked to nothing, to a global or form variable or to a record dataprovider.
 *
 * The type can report whether or not a (runtime - sablo component property) value is or is not dependent on the foundset's record or scripting variables.
 * This way components that are linked to foundsets can know what to send for all records and what to send for each record to the browser.
 *
 * Usually this type of property also uses {@link IServoyAwarePropertyValue} as a runtime (sablo) property value.
 *
 * By default, types that do not implement this interface are considered to not depend on record data.
 *
 * @author acostescu
 * @see IServoyAwarePropertyValue
 */
public interface IDataLinkedType<FormElementT>
{

	/**
	 * @param value this is the template/form element value of the property.
	 * @return true if this value depends on foundset record or scripting variables and false if not.
	 */
	boolean isLinkedToData(FormElementT formElementValue, PropertyDescription pd, FlattenedSolution flattenedSolution, FormElement formElement);

}
