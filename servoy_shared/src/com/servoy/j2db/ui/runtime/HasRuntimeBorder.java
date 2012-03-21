/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.j2db.ui.runtime;

import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

/**
 * Runtime property interface for border.
 * 
 * @author rgansevles
 *
 * @since 6.1
 */
public interface HasRuntimeBorder
{
	/**
	 * Gets or sets the border attribute(s) of a specified element. 
	 * 
	 * The border attributes:
	 * 
	 * borderType - EmptyBorder, EtchedBorder, BevelBorder, LineBorder, TitleBorder, MatteBorder, SpecialMatteBorder.
	 * size - (numeric value) for: bottom, left, right, top.
	 * color - (hexadecimal value) for: bottom, left, right, top.
	 * dash pattern - (numeric value) for selected side(s).
	 * rounding radius - (numeric value) for selected side(s).
	 * 
	 * NOTE: Use the same value(s) and order of attribute(s) from the element design time property "borderType".
	 *
	 * @sample
	 * //sets the border type to "LineBorder"
	 * //sets a 1 px line width for the bottom and left side of the border
	 * //sets the hexadecimal color of the border to "#ccffcc"
	 * %%prefix%%%%elementName%%.border = 'LineBorder,1,#ccffcc';
	 *
	 * @param spec the border attributes
	 */
	@JSGetter
	public String getBorder();

	@JSSetter
	public void setBorder(String spec);

}
