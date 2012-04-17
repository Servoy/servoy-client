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
 * Defines a multiple same lib id/different version occurrence in a resolved dependency tree path result.<br>
 * It also specifies whether or not there is a lib conflict or just one of the lib's version must be chosen with no conflict. 
 * @author acostescu
 */
@SuppressWarnings("nls")
public class LibChoice
{

	/** Whether or not the listed lib versions/dependencies are in conflict, or there is no conflict, but still one must be chosen. */
	public final boolean conflict;
	/** The list of lib version declarations for this lib id on the dependency tree path. */
	public final TrackableLibDependencyDeclaration[] libDependencies;

	/**
	 * Creates a new set of lib dependency declarations to choose from for a lib id.
	 * @param conflict if the listed lib versions/dependencies are in conflict, or there is no conflict, but still one must be chosen.
	 * @param libDependencies the list of lib version declarations for this lib id on the dependency tree path.
	 */
	public LibChoice(boolean conflict, TrackableLibDependencyDeclaration[] libDependencies)
	{
		this.conflict = conflict;
		this.libDependencies = libDependencies;
	}

	@Override
	public String toString()
	{
		return (conflict ? "conflict: " : "choice: ") + Arrays.asList(libDependencies);
	}

}