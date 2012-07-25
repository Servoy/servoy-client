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
package com.servoy.j2db.ui.runtime;

import org.mozilla.javascript.annotations.JSFunction;

/**
 * Interface for components with value list support
 * 
 * @author rgansevles
 * 
 * @since 6.1
 */
public interface HasRuntimeValuelist
{
	/**
	 * Returns the current valuelist name for the specified field; returns NULL if no valuelist.
	 *
	 * @sample var name = %%prefix%%%%elementName%%.getValueListName();
	 * 
	 * @return The valuelist name.
	 */
	@JSFunction
	public String getValueListName();

	/**
	 * Sets the display/real values to the custom valuelist of the element (if element has custom valuelist).
	 * <br>This does not effect the value list with same name list on other elements or value lists at application level.
	 * 
	 * <br>Should receive a dataset parameter, first column is for display values, second column (optional) is for real values.
	 * 
	 * NOTE: if you modify values for checkbox/radio field, note that having one value in valuelist is a special case, so switching between one value and 0/multiple values after form is created may have side effects
	 * @sample
	 * var dataset = databaseManager.createEmptyDataSet(0,new Array('display_values','optional_real_values'));
	 * dataset.addRow(['aa',1]);
	 * dataset.addRow(['bb',2]);
	 * dataset.addRow(['cc',3]);
	 * // %%prefix%%%%elementName%% should have a valuelist attached
	 * %%prefix%%%%elementName%%.setValueListItems(dataset);
	 *
	 * @param dataset first column is display value, second column is real value
	 */
	@JSFunction
	public void setValueListItems(Object value);
}
