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

package com.servoy.j2db.scripting.api;

/**
 * @author jcompagner
 *
 */
public interface IJSController
{
	/**
	 * Gets or sets the enabled state of a form; also known as "grayed-out".
	 * 
	 * Notes:
	 * -A disabled element(s) cannot be selected by clicking the form.
	 * -The disabled "grayed" color is dependent on the LAF set in the Servoy Smart Client Application Preferences.
	 *
	 * @sample
	 * //gets the enabled state of the form
	 * var state = %%prefix%%controller.enabled;
	 * //enables the form for input
	 * %%prefix%%controller.enabled = true;
	 */
	public boolean getEnabled();

	public void setEnabled(boolean b);

	/**
	 * Gets the current record index of the current foundset.
	 *
	 * @sample
	 * //gets the current record index in the current foundset
	 * var current = %%prefix%%controller.getSelectedIndex();
	 * //sets the next record in the foundset, will be reflected in UI
	 * %%prefix%%controller.setSelectedIndex(current+1);
	 * @return the index
	 */
	public int getSelectedIndex();

	/**
	 * Sets the current record index of the current foundset.
	 *
	 * @sample
	 * //gets the current record index in the current foundset
	 * var current = %%prefix%%controller.getSelectedIndex();
	 * //sets the next record in the foundset, will be reflected in UI
	 * %%prefix%%controller.setSelectedIndex(current+1);
	 * 
	 * @param index the index to select 
	 */
	public void setSelectedIndex(int index);
}
