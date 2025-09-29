/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

package com.servoy.j2db.scripting.solutionmodel.developer;

import org.mozilla.javascript.Function;
import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;

/**
 * @author jcompagner
 *
 * @since 2025.09
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "developerBridge", scriptingName = "developerBridge")
@ServoyClientSupport(ng = true, mc = true, wc = true, sc = true)
public interface IJSDeveloperBridge
{
	public static final Location LOCATION = new Location();

	@JSReadonlyProperty
	public Location getLOCATION();

	/**
	 * Shows the form in in the the ui of the developer (so in a dialog) where a developer can interact with.
	 *
	 * @param formName
	 */
	@JSFunction
	void showForm(String formName);

	/**
	 * Creates an instanceof a JSDeveloperMenu that can be used in the register function to add a menu item to the context menu or in the form editor.
	 * The location is one of the servoyDeveloper.LOCATIOM properties
	 *
	 * @param text
	 * @param location
	 *
	 * @return
	 */
	@JSFunction
	JSDeveloperMenu createMenu(String text, int location);


	/**
	 * Creates an instanceof a JSDeveloperMenu that can be used in the register function to add a menu item to the context menu or in the form editor for specific components.
	 * The componentNames is an array of component names
	 *
	 * @param text
	 * @param componentNames
	 *
	 * @return
	 */
	@JSFunction
	JSDeveloperMenu createComponentMenu(String text, String[] componentNames);

	/**
	 * Registers a menu item to show in the developer
	 *
	 * The callback is called when this menu item is clicked on.
	 *
	 * @param menu
	 * @param callback
	 */
	@JSFunction
	void registerMenuItem(JSDeveloperMenu menu, Function callback);


	/**
	 * Registers a menu item to show in the developer
	 *
	 * The callback is called when this menu item is clicked on.
	 *
	 * The enabler function is called when this menu wants to show and the enable function can return true of false depending on which it wants to show
	 *
	 * @param menu
	 * @param callback
	 * @parma enabler
	@JSFunction
	void registerMenuItem(JSDeveloperMenu menu, Function callback, Function enabler);
	 */
}
