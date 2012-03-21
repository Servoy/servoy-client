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
 * Runtime property interface for foreground and background colors.
 * 
 * @author rgansevles
 *
 * @since 6.1
 */
public interface HasRuntimeFgBgColor
{
	/**
	 * Gets or sets the background color of a field. The color has to be set using the hexadecimal RGB value as used in HTML.
	 *
	 * @sample
	 * //sets the background color of the field
	 * %%prefix%%%%elementName%%.bgcolor = "#FFFFFF";
	 * //gets the background color of the field
	 * var c = %%prefix%%%%elementName%%.bgcolor;
	 */
	@JSGetter
	public String getBgcolor();

	@JSSetter
	public void setBgcolor(String clr);

	/**
	 * Gets or sets the foreground color of a field. The color has to be set using the hexadecimal RGB value as used in HTML.
	 *
	 * @sample
	 * //sets the foreground color of the field
	 * %%prefix%%%%elementName%%.fgcolor = "#000000";
	 * 
	 * //gets the foreground color of the field
	 * var c = %%prefix%%%%elementName%%.fgcolor;
	 */
	@JSGetter
	public String getFgcolor();

	@JSSetter
	public void setFgcolor(String clr);
}
