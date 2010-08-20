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

package com.servoy.j2db.persistence;

import com.servoy.j2db.plugins.IMethodTemplatesFactory;

/**
 * Default implementation of factory for creating method templates.
 * 
 * @author gerzse
 */
public class MethodTemplatesFactory implements IMethodTemplatesFactory
{
	private static MethodTemplatesFactory theInstance = null;

	public static MethodTemplatesFactory getInstance()
	{
		if (theInstance == null) theInstance = new MethodTemplatesFactory();
		return theInstance;
	}

	public IMethodArgument createMethodArgument(String name, ArgumentType type, String description)
	{
		return new MethodArgument(name, type, description);
	}

	public IMethodTemplate createMethodTemplate(String name, String description, ArgumentType returnType, String returnTypeDescription,
		IMethodArgument[] arguments, String defaultMethodCode, boolean addTodoBlock)
	{
		MethodArgument signature = new MethodArgument(name, returnType, returnTypeDescription);
		MethodArgument[] args = new MethodArgument[arguments.length];
		for (int i = 0; i < arguments.length; i++)
			args[i] = new MethodArgument(arguments[i]);
		return new MethodTemplate(description, signature, args, defaultMethodCode, addTodoBlock);
	}

}
