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
 * Runtime property interface for size.
 * 
 * @author rgansevles
 *
 * @since 6.1
 */
public interface HasRuntimeSize extends HasRuntimeSizeGet
{
	/**
	 * Sets the size of the field. It takes as input the width and the height. 
	 * 
	 * NOTE: getWidth() can be used with getHeight() to set the size of an element using the setSize function. For Example: 
	 * 
	 * //returns the width (w) and height (h)
	 * var w = forms.company.elements.faxBtn.getWidth();
	 * var h = forms.company.elements.faxBtn.getHeight();
	 * 
	 * //sets the new size
	 * forms.company.elements.faxBtn.setSize(w,h);
	 * 
	 * //sets the new size and adds 1 px to both the width and height
	 * forms.company.elements.faxBtn.setSize(w+1,h+1);
	 *
	 * @sample
	 * %%prefix%%%%elementName%%.setSize(20,30);
	 *
	 * @param width 
	 * the width of the element in pixels.
	 *
	 * @param height
	 * the height of the element in pixels. 
	 */
	@JSFunction
	public void setSize(int width, int height);
}
