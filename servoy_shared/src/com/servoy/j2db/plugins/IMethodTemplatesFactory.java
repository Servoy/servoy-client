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

package com.servoy.j2db.plugins;

import com.servoy.j2db.persistence.ArgumentType;
import com.servoy.j2db.persistence.IMethodArgument;
import com.servoy.j2db.persistence.IMethodTemplate;

/**
 * Interface for factory classes that create instances of IMethodArgument and IMethodTemplate.
 * 
 * Such an interface is sent to any IMethodTemplateProvider in order to facilitate the
 * creation of method templates.
 * 
 * @see com.servoy.j2db.plugins.IMethodTemplatesProvider
 * 
 * @author gerzse
 */
public interface IMethodTemplatesFactory
{
	IMethodArgument createMethodArgument(String name, ArgumentType type, String description);

	IMethodTemplate createMethodTemplate(String name, String description, ArgumentType returnType, String returnTypeDescription, IMethodArgument[] arguments,
		String defaultMethodCode, boolean addTodoBlock);
}
