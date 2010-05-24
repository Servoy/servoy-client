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
package com.servoy.j2db.util.toolbar;


/**
 * Interface for manipulating the toolbars.
 * 
 * @author jblok
 */
public interface IToolbarPanel
{
	/**
	 * Returns the current used toolbar names.
	 * 
	 * @return an string array of names
	 */
	public String[] getToolBarNames();

	/**
	 * Creates a toolbar with a underlying name and display name on the first row possible where it fits.
	 * 
	 * @param name The internal name of the toolbar
	 * @param displayName The name that is displayed to a user.
	 * @return The Toolbar that is created
	 */
	public Toolbar createToolbar(String name, String displayName);

	/**
	 * Creates a toolbar with a underlying name and display name on row that is specified.
	 * 
	 * @param name The internal name of the toolbar
	 * @param displayName The name that is displayed to a user.
	 * @param wantedRow The row where it should be displayed.
	 * @return The Toolbar that is created
	 */
	public Toolbar createToolbar(String name, String displayName, int wantedRow);

	/**
	 * Removes the toolbar specified with the name from the toolbar panel
	 * 
	 * @param name
	 */
	public void removeToolBar(String name);

	/**
	 * Returns the toolbar that has the given name.
	 * 
	 * @param name The name
	 * @return The toolbar that has that name or null if there wasn't a toolbar with that name.
	 */
	public Toolbar getToolBar(String name);

	/**
	 * Set the visibility of toolbar with the given name.
	 * 
	 * @param name
	 * @param visible
	 */
	public void setToolbarVisible(String name, boolean visible);
}
