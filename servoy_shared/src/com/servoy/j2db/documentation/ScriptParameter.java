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
package com.servoy.j2db.documentation;

public class ScriptParameter implements IParameter
{
	private final String name;
	private final String type;
	private final boolean optional;
	private final boolean vararg;

	public ScriptParameter(String name, String type, boolean optional, boolean vararg)
	{
		this.name = name;
		this.type = type;
		this.optional = optional;
		this.vararg = vararg;
	}

	public String getName()
	{
		return name;
	}

	public String getType()
	{
		return type;
	}

	public boolean isOptional()
	{
		return optional;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.eclipse.core.doc.IParameter#isVarArgs()
	 */
	public boolean isVarArgs()
	{
		return vararg;
	}

}
