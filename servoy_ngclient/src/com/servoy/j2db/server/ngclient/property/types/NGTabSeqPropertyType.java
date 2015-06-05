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
import org.sablo.specification.property.types.TabSeqPropertyType;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.server.ngclient.FormElementHelper;
import com.servoy.j2db.server.ngclient.INGFormElement;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IDesignDefaultToFormElement;

/**
 * Design-aware tab sequence type. It will try to always feed tabsequences in FormElement, even if default tab
 * sequence is used. This is necessary to make it work for example if you have a default tab sequence in main form
 * and the main form contains a tab panel with a form containing non-default tab-sequence.<BR/><BR/>
 *
 * In this case, even the main form needs non-default tab sequence to work properly.
 *
 * @author acostescu
 */
public class NGTabSeqPropertyType extends TabSeqPropertyType implements IDesignDefaultToFormElement<Integer, Integer, Integer>
{

	public final static NGTabSeqPropertyType NG_INSTANCE = new NGTabSeqPropertyType();

	@Override
	public Integer toFormElementValue(Integer designValue, PropertyDescription pd, FlattenedSolution flattenedSolution, INGFormElement formElement,
		PropertyPath propertyPath)
	{
		return FormElementHelper.INSTANCE.getControlledTabSeqReplacementFor(designValue, pd, formElement.getForm(), formElement.getPersistIfAvailable(),
			flattenedSolution);
	}

	@Override
	public Integer toDefaultFormElementValue(PropertyDescription pd, FlattenedSolution flattenedSolution, INGFormElement formElement, PropertyPath propertyPath)
	{
		return toFormElementValue(Integer.valueOf(0), pd, flattenedSolution, formElement, propertyPath);
	}

}
