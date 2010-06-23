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
package com.servoy.j2db.util;

/**
 * @author jblok
 */
public interface IUIBlocker
{
	/**
	 * Block the Graphical User Interface, don't let any cmds happen, cursor is set to wait cursor.
	 * <br><b>Note:</b>always call this method with a finally block on releaseGUI!
	 * @param reason is shown in staus
	 */
	public void blockGUI(String reason);
	
	/**
	 * Release the Graphical User Interface, status is defaulting and cursor goes back to normal.
	 */
	public void releaseGUI();
}
