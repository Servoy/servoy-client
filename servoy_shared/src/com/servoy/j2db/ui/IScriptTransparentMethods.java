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
public interface IScriptTransparentMethods extends IScriptBaseMethods
{
	public void js_setToolTipText(String tooltip);

	/**
	 *  Gets or sets the tool tip text of an element; text displays when the mouse cursor hovers over an element. 
	 *  
	 *  NOTE: HTML should be used for multi-line tooltips; you can also use any valid HTML tags to format tooltip text.
	 *
	 * @sample
	 * //gets the tooltip text of the element
	 * var toolTip = %%prefix%%%%elementName%%.toolTipText;
	 * 
	 * //sets the tooltip text of the element
	 * %%prefix%%%%elementName%%.toolTipText = "New tip";
	 * %%prefix%%%%elementName%%.toolTipText = "<html>This includes <b>bolded text</b> and <font color='blue'>BLUE</font> text as well.";
	 */
	public String js_getToolTipText();

	/**
	 * Sets the font name, style, and size of an element. 
	 * 
	 * font name - the name of the font family.
	 * style - the type of the font. (plain = 0; bold = 1; italic = 2; bold-italic = 3).
	 * size - the size of the font (in points).
	 * 
	 * @sample
	 * %%prefix%%%%elementName%%.setFont('Tahoma,1,11');
	 *
	 * @param spec the font name, style, size
	 */
	public String js_getFont();

	public void js_setFont(String spec);

	/**
	 * Gets or sets the transparency of an element; true - transparent; false - not transparent.
	 *  
	 * NOTE: transparency can be inverted using ! operator: elements.elementName.transparent = !elements.elementName.transparent;
	 * 
	 * NOTE: transparency will be mostly used for background color, a transparent element will receive the background of the element "beneath" it, a non transparent one will use its own background color
	 *
	 * @sample
	 * //gets the transparency of the element
	 * var currentState = %%prefix%%%%elementName%%.transparent;
	 * 
	 * //sets the transparency of the element
	 * %%prefix%%%%elementName%%.transparent = !currentState;
	 */
	public boolean js_isTransparent();

	public void js_setTransparent(boolean b);
}
