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

import java.awt.print.PageFormat;

import com.servoy.base.scripting.annotations.ServoyClientSupport;

/**
 * Constants for page orientation (printing). 
 * 
 * @since 7.0
 * @author acostescu
 */
@ServoyClientSupport(ng = false, wc = true, sc = true)
public interface ISMPageOrientation
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
	 * 
	 * @sample
	 * var form = solutionModel.getForm("some_form");
	 * form.defaultPageFormat = solutionModel.createPageFormat(792,612,72,72,72,72,SM_ORIENTATION.LANDSCAPE,SM_UNITS.PIXELS);
	 */
	public static final int LANDSCAPE = PageFormat.LANDSCAPE;

	/**
	 * Reverse landscape page orientation.
	 * 
	 * @sample
	 * var form = solutionModel.getForm("some_form");
	 * form.defaultPageFormat = solutionModel.createPageFormat(792,612,72,72,72,72,SM_ORIENTATION.REVERSE_LANDSCAPE,SM_UNITS.PIXELS);
	 */
	public static final int REVERSE_LANDSCAPE = PageFormat.REVERSE_LANDSCAPE;

}
