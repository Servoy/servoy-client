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

import com.servoy.j2db.scripting.IPrefixedConstantsObject;

/**
 * @author lvostinar
 *
 */
public class TITLEPOSITION implements IPrefixedConstantsObject
{
	/**
	 * Default vertical orientation for the title text
	 * 
	 * @sample
	 * var form = solutionModel.getForm("someForm");
	 * form.borderType = solutionModel.createBorder('Test',null,'#ff0000',SM_TITLEJUSTIFICATION.DEFAULT,SM_TITLEPOSITION.DEFAULT_POSITION);
	 */
	public static final int DEFAULT = TitledBorder.DEFAULT_POSITION;

	/**
	 * Title above the border's top line
	 * 
	 * @sample
	 * var form = solutionModel.getForm("someForm");
	 * form.borderType = solutionModel.createBorder('Test',null,'#ff0000',SM_TITLEJUSTIFICATION.DEFAULT,SM_TITLEPOSITION.ABOVE_TOP);
	 */
	public static final int ABOVE_TOP = TitledBorder.ABOVE_TOP;

	/**
	 * Title in the middle of the border's top line
	 * 
	 * @sample
	 * var form = solutionModel.getForm("someForm");
	 * form.borderType = solutionModel.createBorder('Test',null,'#ff0000',SM_TITLEJUSTIFICATION.DEFAULT,SM_TITLEPOSITION.TOP);
	 */
	public static final int TOP = TitledBorder.TOP;

	/**
	 * Title above the border's bottom line
	 * 
	 * @sample
	 * var form = solutionModel.getForm("someForm");
	 * form.borderType = solutionModel.createBorder('Test',null,'#ff0000',SM_TITLEJUSTIFICATION.DEFAULT,SM_TITLEPOSITION.BELOW_TOP);
	 */
	public static final int BELOW_TOP = TitledBorder.BELOW_TOP;

	/**
	 * Title in the middle of the border's bottom line
	 * 
	 * @sample
	 * var form = solutionModel.getForm("someForm");
	 * form.borderType = solutionModel.createBorder('Test',null,'#ff0000',SM_TITLEJUSTIFICATION.DEFAULT,SM_TITLEPOSITION.ABOVE_BOTTOM);
	 */
	public static final int ABOVE_BOTTOM = TitledBorder.ABOVE_BOTTOM;

	/**
	 * Title below the border's bottom line
	 * 
	 * @sample
	 * var form = solutionModel.getForm("someForm");
	 * form.borderType = solutionModel.createBorder('Test',null,'#ff0000',SM_TITLEJUSTIFICATION.DEFAULT,SM_TITLEPOSITION.BELOW_BOTTOM);
	 */
	public static final int BELOW_BOTTOM = TitledBorder.BELOW_BOTTOM;

	/**
	 * @see com.servoy.j2db.scripting.IPrefixedConstantsObject#getPrefix()
	 */
	public String getPrefix()
	{
		return "SM_TITLEPOSITION"; //$NON-NLS-1$
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "Titled Border Text Position Constants"; //$NON-NLS-1$
	}
}
