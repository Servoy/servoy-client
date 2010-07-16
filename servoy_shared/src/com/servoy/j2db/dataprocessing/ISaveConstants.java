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
 * Constants used in stopEditting to give the status of the save.
 * 
 * @author jcompagner,jblok
 */
public interface ISaveConstants
{
	/**
	 * Stop edit did succeed.
	 */
	public static final int STOPPED = 1;

	/**
	 * Stop edit was blocked by autosave false flag. 
	 */
	public static final int AUTO_SAVE_BLOCKED = 2;

	/**
	 * Stop edit failed because of a validation error.
	 */
	public static final int VALIDATION_FAILED = 4;

	/**
	 * Stop edit failed because of a database save/update error.
	 */
	public static final int SAVE_FAILED = 8;
}
