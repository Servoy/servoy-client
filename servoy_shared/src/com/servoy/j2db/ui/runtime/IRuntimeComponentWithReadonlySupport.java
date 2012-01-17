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
package com.servoy.j2db.ui.runtime;

import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;


/**
 * Interface for components with readonly support
 * @author jcompagner
 * @since 6.1
 */
public interface IRuntimeComponentWithReadonlySupport extends IRuntimeComponent
{
	/**
	 * Gets or sets the editable/read-only state of a field; true - read-only; false - editable; ! - the editable/read-only state is inverted (the opposite). 
	 * 
	 * NOTE: A field set as read-only can be selected by clicking (or pressing the TAB key if this option is supported by the operating system) and the field data can be copied.
	 *
	 * @sample
	 * //gets the editable/read-only state of the field
	 * var currentState = %%prefix%%%%elementName%%.readOnly;
	 * 
	 * //sets the editable/read-only state of the field
	 * %%prefix%%%%elementName%%.readOnly = !currentState;
	 */
	@JSGetter
	public boolean isReadOnly();

	@JSSetter
	public void setReadOnly(boolean b);
}
