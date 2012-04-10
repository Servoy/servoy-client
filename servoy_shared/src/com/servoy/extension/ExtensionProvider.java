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

package com.servoy.extension;

/**
 * Classes that implement this interface have access to .exp packages.<br>
 * They are able to provide dependency & other information from the extension.xml file as well as the full contents of the .exp package.
 * 
 * @author acostescu
 */
public interface ExtensionProvider
{

	/**
	 * Returns an array of extension & dependency info with one element for each available version that satisfies the given dependency
	 * @param extensionDependency the dependency's declaration (id, minVer, maxVer).
	 * @return information about the available compatible versions of the extension.
	 */
	DependencyMetadata[] getDependencyMetadata(ExtensionDependencyDeclaration extensionDependency);

}