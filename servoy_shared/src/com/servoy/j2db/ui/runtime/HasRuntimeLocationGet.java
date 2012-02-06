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
 * Runtime property interface for get location.
 * 
 * @author rgansevles
 *
 * @since 6.1
 */
public interface HasRuntimeLocationGet
{
	/**
	 * Returns the x location of the current element. 
	 * 
	 * NOTE: getLocationX() can be used with getLocationY() to set the location of an element using the setLocation function. For Example:
	 * 
	 * //returns the X and Y coordinates
	 * var x = forms.company.elements.faxBtn.getLocationX();
	 * var y = forms.company.elements.faxBtn.getLocationY();
	 * 
	 * //sets the new location 10 px to the right; 10 px down from the current location
	 * forms.company.elements.faxBtn.setLocation(x+10,y+10);
	 *
	 * @sample
	 * var x = %%prefix%%%%elementName%%.getLocationX();
	 * 
	 * @return The x location of the element in pixels.
	 */
	@JSFunction
	public int getLocationX();

	/**
	 * Returns the y location of the current element. 
	 * 
	 * NOTE: getLocationY() can be used with getLocationX() to set the location of an element using the setLocation function. For Example:
	 * 
	 * //returns the X and Y coordinates
	 * var x = forms.company.elements.faxBtn.getLocationX();
	 * var y = forms.company.elements.faxBtn.getLocationY();
	 * 
	 * //sets the new location 10 px to the right; 10 px down from the current location
	 * forms.company.elements.faxBtn.setLocation(x+10,y+10);
	 *
	 * @sample 
	 * var y =  %%prefix%%%%elementName%%.getLocationY();
	 * 
	 * @return The y location of the element in pixels.
	 */
	@JSFunction
	public int getLocationY();

	/**
	 * Returns the absolute form (designed) Y location.
	 *
	 * @sample
	 * var absolute_y = %%prefix%%%%elementName%%.getAbsoluteFormLocationY();
	 * 
	 * @return The y location of the form in pixels.
	 */
	@JSFunction
	public int getAbsoluteFormLocationY();
}
