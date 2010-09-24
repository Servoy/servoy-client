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

package com.servoy.j2db.scripting.solutionmodel;

import java.awt.Font;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.IPrefixedConstantsObject;

/**
 * @author lvostinar
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class FONTSTYLE implements IPrefixedConstantsObject
{
	/**
	 * Plain(normal) font style.
	 * 
	 * @sample
	 * var form = solutionModel.getForm("someForm");
	 * var component = form.getComponent("someComponent")
	 * component.fontType = solutionModel.createFont('Arial',SM_FONTSTYLE.PLAIN,10);
	 */
	public static final int PLAIN = Font.PLAIN;

	/**
	 * Bold font style.
	 * 
	 * @sample
	 * var form = solutionModel.getForm("someForm");
	 * var component = form.getComponent("someComponent")
	 * component.fontType = solutionModel.createFont('Arial',SM_FONTSTYLE.BOLD,12);
	 */
	public static final int BOLD = Font.BOLD;

	/**
	 * Italic font style.
	 * 
	 * @sample
	 * var form = solutionModel.getForm("someForm");
	 * var component = form.getComponent("someComponent")
	 * component.fontType = solutionModel.createFont('Arial',SM_FONTSTYLE.ITALIC,14);
	 */
	public static final int ITALIC = Font.ITALIC;

	/**
	 * Bold and italic font style.
	 * 
	 * @sample
	 * var form = solutionModel.getForm("someForm");
	 * var component = form.getComponent("someComponent")
	 * component.fontType = solutionModel.createFont('Arial',SM_FONTSTYLE.BOLD_ITALIC,20);
	 */
	public static final int BOLD_ITALIC = Font.BOLD + Font.ITALIC;

	/**
	 * @see com.servoy.j2db.scripting.IPrefixedConstantsObject#getPrefix()
	 */
	public String getPrefix()
	{
		return "SM_FONTSTYLE"; //$NON-NLS-1$
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "Font Style Constants"; //$NON-NLS-1$
	}
}
