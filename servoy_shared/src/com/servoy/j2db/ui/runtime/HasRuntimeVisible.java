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
 * Runtime property interface for visibility.
 * 
 * @author rgansevles
 *
 * @since 6.1
 */
public interface HasRuntimeVisible
{
	/**
	 * Gets or sets the visibility of an element; true - visible; false - not visible; ! - the visibility state is inverted (the opposite).
	 * 
	 * NOTE: The visibility of an element is not persistent; the state of visibility only applies to the current user in his/her current session.
	 *
	 * @sample
	 * //sets the element as visible
	 * forms.company.elements.faxBtn.visible = true;
	 * 
	 * //gets the visibility of the element
	 * var currentState = forms.company.elements.faxBtn.visible;
	 * 
	 * //sets the element as not visible when the current state is visible
	 * forms.company.elements.faxBtn.visible = !currentState;
	 */
	@JSGetter
	public boolean isVisible();

	@JSSetter
	public void setVisible(boolean b);

}
