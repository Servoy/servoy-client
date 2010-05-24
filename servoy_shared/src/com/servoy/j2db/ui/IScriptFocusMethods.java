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

public interface IScriptFocusMethods extends IScriptDataProviderMethods, IScriptTitleTextMethods
{
	/**
	 * Requests the focus for an element; can skip the onFocusGained event for an element by using requestFocus(false). 
	 * When focus is requested on a text field, a text cursor will be displayed in the specified field.
	 * 
	 * NOTE: This function is for field elements only; for more information about the optional parameter, see the onFocusGained design time property in the section on Fields.
	 *
	 * @sample
	 * //requests the focus for the element
	 * //prevents the onFocusGained method from executing
	 * %%prefix%%%%elementName%%.requestFocus(false);
	 * 
	 * @param mustExecuteOnFocusGainedMethod optional true - to execute the onFocusGained method;  false - to prevent the onFocusGained method from executing, default is true (if not specified). 
	 */
	public void js_requestFocus(Object[] vargs);

	/**
	 * Returns an Array of label element names that has this field filled in as the labelFor.
	 *
	 * @sample
	 * var array =elements.name_first.getLabelForElementNames();
	 * for (var i =0; i<array.length;i++)
	 * {
	 * 	elements[array[i]].fgcolor = "#ff00ff";
	 * }
	 * 
	 * @return An array with element names.
	 */
	public String[] js_getLabelForElementNames();
}
