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

import java.util.Arrays;

/**
 * A extension dependency path result found when trying to resolve an install/update.
 * @author acostescu
 */
public class DependencyPath
{

	/** The list of extension/version nodes in this valid dependency path. */
	public final ExtensionNode[] extensionPath;
	/** Any lib choices that must be made on this dependency path. Each lib choice is a list of conflicting/non-conflicting lib declarations with the same lib id but other versions. Can be null. */
	public final LibChoice[] libChoices;

	/**
	 * Creates a new extension dependency path result.
	 * @param extensionPath the list of extension/version nodes in this valid dependency path.
	 * @param libConflicts any lib conflicts found on this dependency path. First index identifies a list of more then one conflicting lib declarations with the same lib id. Can be null.
	 */
	public DependencyPath(ExtensionNode[] extensionPath, LibChoice[] libChoices)
	{
		this.extensionPath = extensionPath;
		this.libChoices = libChoices;
	}

	@Override
	@SuppressWarnings("nls")
	public String toString()
	{
		return "{" + Arrays.asList(extensionPath) + ", LIB Choices: " + (libChoices == null ? null : Arrays.asList(libChoices)) + "}";
	}

}
