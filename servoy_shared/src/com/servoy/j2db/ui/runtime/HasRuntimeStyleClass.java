/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

import com.servoy.base.scripting.annotations.ServoyClientSupport;

/**
 * @author lvostinar
 *
 */
@ServoyClientSupport(ng = true, mc = false, wc = false, sc = false)
public interface HasRuntimeStyleClass
{

	/**
	 * Adds a style to the styleClass property. This works only for NGClient where multiple styles are supported.
	 *
	 * @param styleName the name of the style class to add
	 *
	 * @sample
	 * %%prefix%%%%elementName%%.addStyleClass('redbg');
	 *
	 */
	@JSFunction
	public void addStyleClass(String styleName);

	/**
	 * Removes a style from the styleClass property. This works only for NGClient where multiple styles are supported.
	 *
	 * @param styleName the name of the style class to remove
	 *
	 * @sample
	 *  %%prefix%%%%elementName%%.removeStyleClass('redbg');
	 *
	 */
	@JSFunction
	public void removeStyleClass(String styleName);
}
