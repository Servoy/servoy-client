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

import org.mozilla.javascript.annotations.JSFunction;

/**
 * Runtime property interface for location.
 * 
 * @author rgansevles
 *
 * @since 6.1
 */
public interface HasRuntimeLocation extends HasRuntimeLocationGet
{
	/**
	 * Sets the location of an element. It takes as input the X (horizontal) and Y (vertical) coordinates - starting from the TOP LEFT side of the screen.
	 * 
	 * NOTE: getLocationX() can be used with getLocationY() to return the current location of an element; then use the X and Y coordinates with the setLocation function to set a new location. For Example:
	 * 
	 * //returns the X and Y coordinates
	 * var x = forms.company.elements.faxBtn.getLocationX();
	 * var y = forms.company.elements.faxBtn.getLocationY();
	 * 
	 * //sets the new location 10 px to the right; 10 px down from the current location
	 * forms.company.elements.faxBtn.setLocation(x+10,y+10);
	 *
	 * @sample
	 * %%prefix%%%%elementName%%.setLocation(200,200);
	 *
	 * @param x 
	 * the X coordinate of the element in pixels.
	 *
	 * @param y
	 * the Y coordinate of the element in pixels.
	 */
	@JSFunction
	public void setLocation(int x, int y);

}
