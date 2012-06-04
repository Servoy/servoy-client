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

import org.mozilla.javascript.annotations.JSFunction;


/**
 * Interface for components with focus support
 * 
 * @author jcompagner
 * @since 6.1
 */
public interface HasRuntimeFocus
{
	/**
	 * Request the focus in this element. (Focus is also a text cursor on text components).
	 *
	 * @sample
	 * //request the focus in this %%prefix%%%%elementName%% (focus is also a text cursor on text components)
	 * %%prefix%%%%elementName%%.requestFocus();
	 */
	@JSFunction
	void requestFocus();

	/**
	 * Request the focus in this element. (Focus is also a text cursor on text components).
	 *
	 * @sample
	 * //request the focus in this %%prefix%%%%elementName%% (focus is also a text cursor on text components), skip on focus gained method call
	 * %%prefix%%%%elementName%%.requestFocus(false);
	 *
	 * @param mustExecuteOnFocusGainedMethod If true will execute onFocusGained method, else will not; default value is true. 
	 */
	@JSFunction
	void requestFocus(boolean mustExecuteOnFocusGainedMethod);
}
