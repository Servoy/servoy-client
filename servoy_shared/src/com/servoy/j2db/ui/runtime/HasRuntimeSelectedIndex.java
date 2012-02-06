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

package com.servoy.j2db.ui.runtime;

/**
 * Runtime property interface for se;ected index.
 * 
 * @author rgansevles
 *
 * @since 6.1
 */
public interface HasRuntimeSelectedIndex
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
	public int getSelectedIndex();

	/**
	 * Sets the selected record index in the current cached foundset in the specified portal.
	 * 
	 * @sampleas getSelectedIndex()
	 * 
	 * @param index the specified record index
	 */
	public void setSelectedIndex(int index);
}
