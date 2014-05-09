/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package org.sablo;

import java.util.HashMap;
import java.util.Map;

/**
 * @author jblok
 */
public abstract class Container extends WebComponent
{
	protected final Map<String, WebComponent> components = new HashMap<>();

	public Container(String name)
	{
		super(name);
	}

	public void add(WebComponent component)
	{
		components.put(component.getName(), component);
	}

	public WebComponent getWebComponent(String name)
	{
		return components.get(name);
	}

	public Map<String, WebComponent> getWebComponents()
	{
		return components;
	}
}
