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

import java.awt.print.PageFormat;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.IPrefixedConstantsObject;

/**
 * @author laurian
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class PAGEORIENTATION implements IPrefixedConstantsObject
{
	/**
	 * Portrait page orientation.
	 * 
	 * @sample
	 * var form = solutionModel.getForm("some_form");
	 * form.defaultPageFormat = solutionModel.createPageFormat(612,792,72,72,72,72,SM_ORIENTATION.PORTRAIT,SM_UNITS.PIXELS);
	 */
	public static final int PORTRAIT = PageFormat.PORTRAIT;

	/**
	 * Landscape page orientation.
	 * var form = solutionModel.getForm("some_form");
	 * form.defaultPageFormat = solutionModel.createPageFormat(792,612,72,72,72,72,SM_ORIENTATION.LANDSCAPE,SM_UNITS.PIXELS);
	 */
	public static final int LANDSCAPE = PageFormat.LANDSCAPE;

	/**
	 * var form = solutionModel.getForm("some_form");
	 * form.defaultPageFormat = solutionModel.createPageFormat(792,612,72,72,72,72,SM_ORIENTATION.REVERSE_LANDSCAPE,SM_UNITS.PIXELS);
	 * 
	 * Reverse landscape page orientation.
	 */
	public static final int REVERSE_LANDSCAPE = PageFormat.REVERSE_LANDSCAPE;

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "Page Orientation Constants"; //$NON-NLS-1$
	}

	/**
	 * @see com.servoy.j2db.scripting.IPrefixedConstantsObject#getPrefix()
	 */
	public String getPrefix()
	{
		return "SM_ORIENTATION"; //$NON-NLS-1$
	}

}
