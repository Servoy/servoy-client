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

package com.servoy.j2db.dataprocessing;

/**
 * Constants for SQL data manipulation actions.
 * 
 * @author gerzse
 */
public interface ISQLActionTypes
{
	/**
	 * Constant expressing that no SQL action took place.
	 * 
	 * @sample
	 * // solution onDataBroadcast event handler 
	 * function onDataBroadcast(dataSource, action, pks, cached) {
	 * 	if (action == SQL_ACTION_TYPES.INSERT_ACTION)
	 * 		application.output("it was an insert");
	 * 	else if (action == SQL_ACTION_TYPES.DELETE_ACTION)
	 * 		application.output("it was a delete");
	 * 	else if (action == SQL_ACTION_TYPES.UPDATE_ACTION)
	 * 		application.output("it was an update");
	 * 	else if (action == SQL_ACTION_TYPES.SELECT_ACTION)
	 * 		application.output("it was a select");
	 * 	else if (action == SQL_ACTION_TYPES.NO_ACTION)
	 * 		application.output("it was nothing");
	 * 	else
	 * 		application.output("what was this?");
	 * }
	 */
	public static final int NO_ACTION = 0;

	/**
	 * Constant for the "delete" SQL action.
	 */
	public static final int DELETE_ACTION = 1;

	/**
	 * Constant for the "insert" SQL action.
	 */
	public static final int INSERT_ACTION = 2;

	/**
	 * Constant for the "update" SQL action.
	 */
	public static final int UPDATE_ACTION = 3;

	/**
	 * Constant for the "select" SQL action.
	 */
	public static final int SELECT_ACTION = 4;
}
