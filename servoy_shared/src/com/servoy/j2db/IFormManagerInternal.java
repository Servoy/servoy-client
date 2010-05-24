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
package com.servoy.j2db;

import java.beans.PropertyChangeListener;

/**
 * @author Jan Blok
 */
public interface IFormManagerInternal extends IManager, PropertyChangeListener
{
	/**
	 * All user created dialogs (from JS) names will be prefixed with this string.
	 */
	public static final String USER_WINDOW_PREFIX = "FormWindow";

	/**
	 * Get clear of the login form
	 */
	public void clearLoginForm();

	/**
	 * Notify about solution loading
	 */
	public void showSolutionLoading(boolean loading);

	/**
	 * Get a form controller for external usage.
	 * @return IForm the form
	 */
//	public IForm getFormController(String name,Object parent);

	public IForm getCurrentMainShowingFormController();

	public void synchViewMenu(int viewType);


}
