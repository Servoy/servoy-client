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

package com.servoy.base.persistence.constants;


/**
 * Constants useful when dealing with forms.
 *
 * @author acostescu
 *
 */
public interface IFormConstants
{

	// FORM VIEW TYPES ------------------------
	/**
	 * Constant for method setView(...) to show in recordview.
	 */
	public static final int VIEW_TYPE_RECORD = 0;

	/**
	 * Constant for method setView(...) to show in listview.
	 */
	public static final int VIEW_TYPE_LIST = 1;

	/**
	 * Constant for locked record view.
	 */
	public static final int VIEW_TYPE_TABLE = 2;
	public static final int VIEW_TYPE_TABLE_LOCKED = 3;
	public static final int VIEW_TYPE_LIST_LOCKED = 4;
	public static final int VIEW_TYPE_RECORD_LOCKED = 5;


	// ENCAPSULATION ------------------------

	/**
	 * Everything is public.
	 */
	public static final int DEFAULT = 0;

	/**
	 * Completely private persist as far as scripting is concerned, only used for example in designer in a related tab (identical to MODULE_SCOPE as far as designer is concerned).
	 * Doesn't show up in the code completion (forms.xxx) at all
	 */
	public static final int HIDE_IN_SCRIPTING_MODULE_SCOPE = 1;

	/**
	 * Flags the persist as module scope, only the module itself will see the persist, solutions/modules
	 * that have included the module of this persist will not see this persist in the code completion or
	 * any other dialog like place tab.
	 */
	public static final int MODULE_SCOPE = 2;

	/**
	 * Hides the selected record dataproviders (columns,calculations and relations) from code completion.
	 */
	public static final int HIDE_DATAPROVIDERS = 4;

	/**
	 * Hides the foundset property of the form.
	 */
	public static final int HIDE_FOUNDSET = 8;

	/**
	 * Hides the controller property of the form.
	 */
	public static final int HIDE_CONTROLLER = 16;

	/**
	 * Hides the elements property of the form.
	 */
	public static final int HIDE_ELEMENTS = 32;

	/**
	 * Hides the containers property of the form.
	 */
	public static final int HIDE_CONTAINERS = 64;

	// FORM NAVIGATOR TYPES ------------------------
	/**
	 * Constants for navigator types
	 */
	public static final int NAVIGATOR_DEFAULT = 0;
	public static final int NAVIGATOR_NONE = -1;
	public static final int NAVIGATOR_IGNORE = -2;
}
