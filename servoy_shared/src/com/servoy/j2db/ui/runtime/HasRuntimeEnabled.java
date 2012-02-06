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
 * Runtime property interface for enabled.
 * 
 * @author rgansevles
 *
 * @since 6.1
 */
public interface HasRuntimeEnabled
{

	/**
	 * Gets or sets the enabled state of a specified field, also known as "grayed".
	 * true - enabled; false - not enabled; ! - the enabled state is inverted (the opposite).
	 * 
	 * NOTE: A disabled element cannot be selected by clicking the element (or by pressing the TAB key even if this option is supported by the operating system).
	 *
	 * NOTE: A label or button element will not disable if the "displayType" design time property for a field is set to HTML_AREA.
	 * 
	 * NOTE: The disabled "grayed" color is dependent on the LAF set in the Servoy Client Application Preferences. For more information see Preferences: Look And Feel in the Servoy Developer User's Guide.
	 *
	 * @sample
	 * //gets the enabled state of the field
	 * var currState = %%prefix%%%%elementName%%.enabled;
	 * 
	 * //sets the enabled state of the field
	 * %%prefix%%%%elementName%%.enabled = !currentState;
	 */
	@JSGetter
	public boolean isEnabled();

	@JSSetter
	public void setEnabled(boolean b);

}
