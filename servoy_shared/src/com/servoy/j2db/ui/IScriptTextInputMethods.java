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


/**
 * @author jcompagner
 *
 */
public interface IScriptTextInputMethods extends IScriptInputMethods
{
	/**
	 * Gets or sets the number value (position) of the text caret (text "I" bar) in a field.
	 *
	 * @sample
	 * //get the current caretposition
	 * var caretPos = %%prefix%%%%elementName%%.caretPosition;
	 * //add one and set it
	 * %%prefix%%%%elementName%%.caretPosition = caretPos+1;
	 */
	public int js_getCaretPosition();

	public void js_setCaretPosition(int pos);

	/**
	 * Returns the currently selected text in the specified field. 
	 * 
	 * NOTE: This function is for field elements only.
	 *
	 * @sample var my_text = %%prefix%%%%elementName%%.getSelectedText();
	 * 
	 * @return The selected text from the component.
	 */
	public String js_getSelectedText();

	/**
	 * Selects all the contents of a field.
	 *
	 * @sample %%prefix%%%%elementName%%.selectAll();
	 */
	public void js_selectAll();

	/**
	 * Replaces the selected text; if no text has been selected, the replaced value will be inserted at the last cursor position.
	 * 
	 * NOTE: replaceSelectedText applies to text fields and all XXX_AREA displayType text - RTF_AREA, HTML_AREA, or TEXT_AREA.
	 *
	 * @sample 
	 * //returns the current selected text
	 * var my_text = %%prefix%%%%elementName%%.getSelectedText();
	 * 
	 * //replaces the current selected text
	 * %%prefix%%%%elementName%%.replaceSelectedText('John');
	 * 
	 * @param s The replacement text.
	 */
	public void js_replaceSelectedText(String s);
}
