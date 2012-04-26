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

import java.io.File;


/**
 * Classes that implement this interface have access to .exp packages.<br>
 * They are able to provide dependency & other information from the package.xml file as well as the full contents of the .exp package.
 * 
 * @author acostescu
 */
public interface IExtensionProvider
{

	/**
	 * Returns an array of extension & dependency info with one element for each available version that satisfies the given dependency
	 * @param extensionDependency the dependency's declaration (id, minVer, maxVer).
	 * @return information about the available compatible versions of the extension.
	 */
	DependencyMetadata[] getDependencyMetadata(ExtensionDependencyDeclaration extensionDependency);

	/**
	 * Gives the extension package file that corresponds to a version of an extension.<BR>
	 * This method must be able to provide a valid .exp file for every DependencyMetadata object that {@link #getDependencyMetadata(ExtensionDependencyDeclaration)} returned.
	 * 
	 * @param extensionId the extension contained in the .exp file.
	 * @param version the version of the extension from the .exp file.
	 * @return the .exp package file or null if not available.
	 */
	File getEXPFile(String extensionId, String version);

	/**
	 * If problems were encountered while trying to provide extension contents, they will be remembered.
	 * @return any problems encountered that might be of interest to the user.
	 */
	String[] getWarnings();

}