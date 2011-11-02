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

package com.servoy.j2db.scripting.solutionmodel;

import com.servoy.j2db.IForm;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.IPrefixedConstantsObject;

/**
 * Constants used for a form's selectionMode property. See also the foundset.multiSelect property.
 * 
 * @since 6.1
 * @author acostescu
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class SELECTIONMODE implements IPrefixedConstantsObject
{

	/**
	 * @clonedesc com.servoy.j2db.IForm#SELECTION_MODE_DEFAULT
	 * 
	 * @sample
	 * var myForm = solutionModel.getForm('my_form_name');
	 * myForm.selectionMode = SM_SELECTION.DEFAULT;
	 */
	public static final int DEFAULT = IForm.SELECTION_MODE_DEFAULT;

	/**
	 * @clonedesc com.servoy.j2db.IForm#SELECTION_MODE_SINGLE
	 * 
	 * @sample
	 * var myForm = solutionModel.getForm('my_form_name');
	 * myForm.selectionMode = SM_SELECTION.SINGLE;
	 */
	public static final int SINGLE = IForm.SELECTION_MODE_SINGLE;

	/**
	 * @clonedesc com.servoy.j2db.IForm#SELECTION_MODE_MULTI
	 * 
	 * @sample
	 * var myForm = solutionModel.getForm('my_form_name');
	 * myForm.selectionMode = SM_SELECTION.MULTI;
	 */
	public static final int MULTI = IForm.SELECTION_MODE_MULTI;

	public String getPrefix()
	{
		return "SM_SELECTIONMODE"; //$NON-NLS-1$
	}

	@Override
	public String toString()
	{
		return "Selection type constants"; //$NON-NLS-1$
	}
}