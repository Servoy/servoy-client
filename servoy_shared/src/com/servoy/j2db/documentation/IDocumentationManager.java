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

import java.util.SortedMap;


/**
 * Documentation manager interface, implemented via extension point.
 */
public interface IDocumentationManager
{
	public void addObject(IObjectDocumentation object);

	public SortedMap<String, IObjectDocumentation> getObjects();

	public IObjectDocumentation getObjectByQualifiedName(String qualifiedName);

	public IFunctionDocumentation createFunctionDocumentation(String mainName, Class< ? >[] argsTypes, Integer type, boolean deprecated, int state);

	public IObjectDocumentation createObjectDocumentation(String category, String qualifiedName, String publicName, String scriptingName, String[] parentClasses);
}
