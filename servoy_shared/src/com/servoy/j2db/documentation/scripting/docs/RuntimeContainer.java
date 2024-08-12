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
package com.servoy.j2db.documentation.scripting.docs;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.documentation.ServoyDocumented;

/**
 * A layout container's runtime API - it can be accessed via "containers.myContainersName." in form scopes.
 *
 * @author jcompagner
 * @since 2019.12
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "RuntimeContainer", scriptingName = "RuntimeContainer")
@ServoyClientSupport(mc = false, wc = false, sc = false, ng = true)
public class RuntimeContainer
{
	/**
	 * Adds one or more style classes to this container.
	 * @param classes one or more class names
	 * @return
	 */
	@JSFunction
	public boolean addStyleClasses(String... classes)
	{
		return true;
	}

	/**
	 * Removes one or more style classes to this container.
	 * @param classes one or more class names
	 * @return
	 */
	@JSFunction
	public boolean removeStyleClasses(String... classes)
	{
		return true;
	}

	/**
	 * returns true if this container has all the classes give.
	 *
	 * @param classes one or more class names
	 * @return
	 */
	@JSFunction
	public boolean hasStyleClasses(String... classes)
	{
		return true;
	}

	/**
	 * Sets a css style to the container.
	 * @param key css key to add
	 * @param value css value to add
	 */
	@JSFunction
	public void setCSSStyle(String key, String value)
	{
	}

	/**
	 * Removes a css style that was previously added.
	 * @param key css key to remove
	 */
	@JSFunction
	public void removeCSSStyle(String key)
	{
	}
}
