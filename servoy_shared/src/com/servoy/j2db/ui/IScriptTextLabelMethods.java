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
public interface IScriptTextLabelMethods extends IScriptLabelMethods
{
	/**
	 * Gets or sets the text that is displayed on the label, button or image.
	 * 
	 * NOTE: The .text property applies to labels, buttons, or images ONLY.
	 *
	 * @sample
	 * //gets the text of the element
	 * var my_text = %%prefix%%%%elementName%%.text;
	 *
	 * //sets the text of the element
	 * %%prefix%%%%elementName%%.text = my_text + 'is cool';
	 */
	public String js_getText();

	public void js_setText(String txt);
}