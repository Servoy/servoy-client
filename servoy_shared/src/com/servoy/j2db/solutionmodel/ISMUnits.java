/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.j2db.solutionmodel;

import javax.print.attribute.Size2DSyntax;

import com.servoy.j2db.util.Utils;

/**
 * Constants for units of measurement - page format.
 * 
 * @since 7.0
 * @author acostescu
 */
public interface ISMUnits
{

	/**
	 * Millimeters length unit.
	 * 
	 * @sample
	 * var form = solutionModel.getForm("someForm");
	 * form.defaultPageFormat = solutionModel.createPageFormat(215,279,25,25,25,25,SM_ORIENTATION.PORTRAIT,SM_UNITS.MM);
	 */
	public static final int MM = Size2DSyntax.MM;

	/**
	 * Inch length unit.
	 * 
	 * @sample
	 * var form = solutionModel.getForm("someForm");
	 * form.defaultPageFormat = solutionModel.createPageFormat(8.5,11,1,1,1,1,SM_ORIENTATION.PORTRAIT,SM_UNITS.INCH);
	 */
	public static final int INCH = Size2DSyntax.INCH;

	/**
	 * Pixels length unit.
	 * 
	 * @sample
	 * var form = solutionModel.getForm("someForm");
	 * form.defaultPageFormat = solutionModel.createPageFormat(612,792,72,72,72,72,SM_ORIENTATION.PORTRAIT,SM_UNITS.PIXELS);
	 */
	public static final int PIXELS = (int)(Size2DSyntax.INCH / Utils.PPI);

}
