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

import com.servoy.extension.DependencyMetadata;
import com.servoy.extension.ExtensionDependencyDeclaration;
import com.servoy.extension.LibDependencyDeclaration;
import com.servoy.extension.ServoyDependencyDeclaration;

/**
 * Class that lists the extension identifier and complete dependency metadata (all that is declared in the xml, not just what is needed for dependency
 * resolving).
 * @author acostescu
 */
public class FullDependencyMetadata extends DependencyMetadata
{

	@SuppressWarnings("hiding")
	protected FullLibDependenncyDeclaration[] libDependencies;

	/**
	 * See {@link DependencyMetadata#DependencyMetadata(String, String, String, ServoyDependencyDeclaration, ExtensionDependencyDeclaration[], LibDependencyDeclaration[])}.
	 */
	public FullDependencyMetadata(String id, String version, String extensionName, ServoyDependencyDeclaration servoyDependency,
		ExtensionDependencyDeclaration[] extensionDependencies, FullLibDependenncyDeclaration[] libDependencies)
	{
		super(id, version, extensionName, servoyDependency, extensionDependencies, libDependencies);
	}

}
