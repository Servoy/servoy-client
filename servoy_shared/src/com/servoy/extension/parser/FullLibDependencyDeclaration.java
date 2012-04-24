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

package com.servoy.extension.parser;

import com.servoy.extension.LibDependencyDeclaration;

/**
 * Full library dependency declaration (as defined in extension.xml); it also knows the extension .
 * @author acostescu
 */
public class FullLibDependencyDeclaration extends LibDependencyDeclaration
{

	public final String relativePath;

	/**
	 * See {@link LibDependencyDeclaration#LibDependencyDeclaration(String, String, String, String)}.
	 * @param relativePath relative path in the .exp package to the lib's file.
	 */
	public FullLibDependencyDeclaration(String id, String version, String minVersion, String maxVersion, String relativePath) throws IllegalArgumentException
	{
		super(id, version, minVersion, maxVersion);
		this.relativePath = relativePath;
	}

}
