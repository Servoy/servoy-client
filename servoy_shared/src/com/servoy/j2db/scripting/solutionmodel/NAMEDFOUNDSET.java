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

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.scripting.IPrefixedConstantsObject;

/**
 * Constants used for a form's namedFoundset property.
 * 
 * @since 6.1
 * @author acostescu
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class NAMEDFOUNDSET implements IPrefixedConstantsObject
{

	/**
	 * @clonedesc com.servoy.j2db.Form#NAMED_FOUNDSET_EMPTY
	 * 
	 * @sample
	 *	// form with empty foundset
	 *	var frmEmpty = solutionModel.newForm('products_empty', 'example_data', 'products', null, true, 640, 480);
	 *	frmEmpty.newLabel("Empty FoundSet",10,10,200,20);
	 *	frmEmpty.newField('categoryid',JSField.TEXT_FIELD,10,40,200,20);
	 *	frmEmpty.newField('productname',JSField.TEXT_FIELD,10,70,200,20);
	 *	frmEmpty.namedFoundSet = SM_NAMEDFOUNDSET.EMPTY;
	 */
	public static final String EMPTY = Form.NAMED_FOUNDSET_EMPTY;

	/**
	 * @clonedesc com.servoy.j2db.Form#NAMED_FOUNDSET_SEPARATE
	 * 
	 * @sample
	 *	// form with separate foundset
	 *	var frmSeparate = solutionModel.newForm('products_separate', 'example_data', 'products', null, true, 640, 480);
	 *	frmSeparate.newLabel("Separate FoundSet",10,10,200,20);
	 *	frmSeparate.newField('categoryid',JSField.TEXT_FIELD,10,40,200,20);
	 *	frmSeparate.newField('productname',JSField.TEXT_FIELD,10,70,200,20);
	 *	frmSeparate.namedFoundSet = SM_NAMEDFOUNDSET.SEPARATE;
	 *	forms['products_separate'].controller.find();
	 *	forms['products_separate'].categoryid = '=2';
	 *	forms['products_separate'].controller.search();
	 */
	public static final String SEPARATE = Form.NAMED_FOUNDSET_SEPARATE;

	public String getPrefix()
	{
		return "SM_NAMEDFOUNDSET"; //$NON-NLS-1$
	}

	@Override
	public String toString()
	{
		return "Named foundset constants"; //$NON-NLS-1$
	}
}