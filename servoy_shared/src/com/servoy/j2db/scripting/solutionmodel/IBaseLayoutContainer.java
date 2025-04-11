/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

package com.servoy.j2db.scripting.solutionmodel;

import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

import com.servoy.j2db.persistence.LayoutContainer;

/**
 * @author lvostinar
 *
 */
public interface IBaseLayoutContainer
{
	/**
	 * @clonedesc com.servoy.j2db.persistence.LayoutContainer#getCssClasses()
	 *
	 * @sample
	 * layoutContainer.cssClasses = 'myContainer';
	 *
	 * @return A space-separated string of CSS classes applied to the layout container.
	 */
	@JSGetter
	default public String getCssClasses()
	{
		return getLayoutContainer().getCssClasses();
	}

	@JSSetter
	default public void setCssClasses(String cssClasses)
	{
		checkModification();
		getLayoutContainer().setCssClasses(cssClasses);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.LayoutContainer#getAttribute()
	 *
	 * @param name the attributes name
	 *
	 * @sample
	 * layoutContainer.getAttribute('class');
	 *
	 * @return The value of the specified attribute, or null if the attribute is not set.
	 */
	@JSFunction
	default public String getAttribute(String name)
	{
		return getLayoutContainer().getAttribute(name);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.LayoutContainer#putAttribute()
	 *
	 * @sample
	 * layoutContainer.putAttribute('class','container fluid');
	 *
	 * @param key
	 * @param value
	 */
	@JSFunction
	default public void putAttribute(String name, String value)
	{
		checkModification();
		getLayoutContainer().putAttribute(name, value);
	}

	/**
	 * Remove a layout container (with all its children) from hierarchy.
	 *
	 * @sample
	 * layoutContainer.remove();
	 */
	@JSFunction
	default public void remove()
	{
		checkModification();
		getJSParent().getSupportChild().removeChild(getLayoutContainer());
	}

	IJSParent getJSParent();

	void checkModification();

	LayoutContainer getLayoutContainer();
}
