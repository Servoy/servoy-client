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
 * Runtime property interface for client property.
 * 
 * @author rgansevles
 *
 * @since 6.1
 */
public interface HasRuntimeClientProperty
{
	/**
	 * Sets the value for the specified element client property key.
	 *
	 * NOTE: Depending on the operating system, a user interface property name may be available.
	 * 
	 * @sample
	 * %%prefix%%%%elementName%%.putClientProperty('ToolTipText','some text');
	 * 
	 * @param key user interface key (depends on operating system)
	 * @param value a predefined value for the key
	 */
	@JSFunction
	public void putClientProperty(Object key, Object value);

	/**
	 * Gets the specified client property for the element based on a key.
	 * 
	 * NOTE: Depending on the operating system, a user interface property name may be available.
	 *
	 * @sample var property = %%prefix%%%%elementName%%.getClientProperty('ToolTipText');
	 * 
	 * @param key user interface key (depends on operating system)
	 * 
	 * @return The value of the property for specified key.
	 */
	@JSFunction
	public Object getClientProperty(Object key);
}
