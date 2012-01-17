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
package com.servoy.j2db.ui;

import com.servoy.j2db.ui.runtime.IRuntimePortal;

/**
 * Methods for runtime portal that are not shared with java api ({@link IRuntimePortal})
 * 
 * @author rgansevles
 * 
 * @since 6.1
 *
 */
public interface IScriptPortalMethods
{
	/**
	 * Gets the selected record index in the current cached foundset in the specified portal.
	 *
	 * @sample
	 * //gets the selected record index in the foundset
	 * var current = %%prefix%%%%elementName%%.getSelectedIndex();
	 * 
	 * //sets the next record index in the foundset
	 * %%prefix%%%%elementName%%.setSelectedIndex(current+1);
	 * 
	 * @return The selected index (integer).
	 */
	public int jsFunction_getSelectedIndex();

	/**
	 * Sets the selected record index in the current cached foundset in the specified portal.
	 * 
	 * @sampleas jsFunction_getSelectedIndex()
	 * 
	 * @param index the specified record index
	 */
	public void jsFunction_setSelectedIndex(int index);
}