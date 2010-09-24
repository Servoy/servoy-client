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

import javax.swing.border.TitledBorder;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.IPrefixedConstantsObject;

/**
 * @author lvostinar
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class TITLEJUSTIFICATION implements IPrefixedConstantsObject
{
	/**
	 * Default justification for the title text
	 * 
	 * @sample
	 * var form = solutionModel.getForm("someForm");
	 * form.borderType = solutionModel.createBorder('Test',null,'#ff0000',SM_TITLEJUSTIFICATION.DEFAULT,SM_TITLEPOSITION.TOP);
	 * 
	 */
	public static final int DEFAULT = TitledBorder.DEFAULT_JUSTIFICATION;

	/**
	 * Position title text at the left side of the border line.
	 * 
	 * @sample
	 * var form = solutionModel.getForm("someForm");
	 * form.borderType = solutionModel.createBorder('Test',null,'#ff0000',SM_TITLEJUSTIFICATION.LEFT,SM_TITLEPOSITION.TOP);
	 */
	public static final int LEFT = TitledBorder.LEFT;

	/**
	 * Position title text in the center of the border line.
	 * 
	 * @sample
	 * var form = solutionModel.getForm("someForm");
	 * form.borderType = solutionModel.createBorder('Test',null,'#ff0000',SM_TITLEJUSTIFICATION.CENTER,SM_TITLEPOSITION.TOP);
	 */
	public static final int CENTER = TitledBorder.CENTER;

	/**
	 * Position title text at the right side of the border line.
	 * 
	 * @sample
	 * var form = solutionModel.getForm("someForm");
	 * form.borderType = solutionModel.createBorder('Test',null,'#ff0000',SM_TITLEJUSTIFICATION.RIGHT,SM_TITLEPOSITION.TOP);
	 */
	public static final int RIGHT = TitledBorder.RIGHT;

	/**
	 * Position title text at the left side of the border line
	 *  for left to right orientation, at the right side of the 
	 *  border line for right to left orientation.
	 *  
	 * @sample
	 * var form = solutionModel.getForm("someForm");
	 * form.borderType = solutionModel.createBorder('Test',null,'#ff0000',SM_TITLEJUSTIFICATION.LEADING,SM_TITLEPOSITION.TOP);
	 */
	public static final int LEADING = TitledBorder.LEADING;

	/**
	 * Position title text at the right side of the border line
	 *  for left to right orientation, at the left side of the 
	 *  border line for right to left orientation.
	 *  
	 * @sample
	 * var form = solutionModel.getForm("someForm");
	 * form.borderType = solutionModel.createBorder('Test',null,'#ff0000',SM_TITLEJUSTIFICATION.TRAILING,SM_TITLEPOSITION.TOP);
	 */
	public static final int TRAILING = TitledBorder.TRAILING;

	/**
	 * @see com.servoy.j2db.scripting.IPrefixedConstantsObject#getPrefix()
	 */
	public String getPrefix()
	{
		return "SM_TITLEJUSTIFICATION"; //$NON-NLS-1$
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "Titled Border Text Justification Constants"; //$NON-NLS-1$
	}
}
