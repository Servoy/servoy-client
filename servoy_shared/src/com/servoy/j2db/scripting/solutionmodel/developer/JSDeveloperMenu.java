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

package com.servoy.j2db.scripting.solutionmodel.developer;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;

/**
 * @author jcompagner
 *
 * @since 2025.09
 *
 */
public class JSDeveloperMenu
{
	private final String text;
	private final int location;
	private final String[] componentNames;

	private String id;

	public JSDeveloperMenu(String text, int location)
	{
		this.text = text;
		this.location = location;
		this.componentNames = null;
	}

	public JSDeveloperMenu(String text, String[] componentNames)
	{
		this.text = text;
		this.location = IJSDeveloperBridge.LOCATION.getCOMPONENT();
		this.componentNames = componentNames;
	}

	/**
	 * @return the location
	 */
	@JSReadonlyProperty
	public int getLocation()
	{
		return location;
	}

	@JSReadonlyProperty
	public String getText()
	{
		return text;
	}

	@JSReadonlyProperty
	public String[] getComponentNames()
	{
		return componentNames;
	}

	@JSFunction
	public String getId()
	{
		return id;
	}

	@JSFunction
	public JSDeveloperMenu setId(String id)
	{
		this.id = id;
		return this;
	}
}
