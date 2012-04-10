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

package com.servoy.extension.dependency;

/**
 * Node in the resolved dependency tree path (which is a list).<br>
 * It also specifies the operation used that lead to this node being considered (update installed extension/simple dependency resolve).
 * @author acostescu
 */
@SuppressWarnings("nls")
public class ExtensionNode
{

	public final static int SIMPLE_DEPENDENCY_RESOLVE = 1;
	public final static int REPLACE_RESOLVE = 2;

	public final String id;
	public final String version;
	public final int resolveType;

	public ExtensionNode(String id, String version, int resolveType)
	{
		this.id = id;
		this.version = version;
		this.resolveType = resolveType;
	}

	@Override
	public String toString()
	{
		String type = String.valueOf(resolveType);
		switch (resolveType)
		{
			case SIMPLE_DEPENDENCY_RESOLVE :
				type = "S";
				break;
			case REPLACE_RESOLVE :
				type = "R";
				break;
		}
		return "(" + id + ", " + version + ", " + type + ")";
	}

}