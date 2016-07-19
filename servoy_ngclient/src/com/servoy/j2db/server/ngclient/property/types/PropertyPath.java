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

package com.servoy.j2db.server.ngclient.property.types;

import java.util.ArrayList;
import java.util.List;

/**
 * The 'path' to a property (useful for nested property types or nested child components).
 * When component property type will implement api calls directly through the type this class can be removed (currently it is the only one using it).
 *
 * @author acostescu
 */
public class PropertyPath
{

	protected List<Object> path = new ArrayList<Object>();
	private boolean shouldAddElementName = true; // another hack - when not directly a child of a form, a child component will not be referenced by name, but by index or custom property key

	public void add(int i)
	{
		path.add(Integer.valueOf(i));
	}

	public void add(String p)
	{
		path.add(p);
	}

	public void backOneLevel()
	{
		if (path.size() > 0) path.remove(path.size() - 1);
	}

	public boolean shouldAddElementNameAndClearFlag()
	{
		boolean tmp = shouldAddElementName;
		shouldAddElementName = false;
		return tmp;
	}

	public PropertyPath setShouldAddElementName()
	{
		this.shouldAddElementName = true;
		return this;
	}

	public Object[] currentPathCopy()
	{
		return path.toArray();
	}

}
