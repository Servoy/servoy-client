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


/**
 * If the desired dependency resolve path has lib choices to be made (either compatible libs but different versions or lib conflicts),
 * classes that implement this interface will choose the lib version to use.<br><br>
 * 
 * @author acostescu
 */
public interface ILibVersionChooser
{

	/**
	 * Picks one of lib versions for the given conflicting/multiple occurrence of a lib.
	 * @param libChoice a lib choice that must be handled for the desired dependency resolve path.
	 * @return one lib declaration is selected and returned.
	 */
	TrackableLibDependencyDeclaration chooseLibDeclaration(LibChoice libChoices);

}
