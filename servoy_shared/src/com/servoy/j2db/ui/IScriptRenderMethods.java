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

import com.servoy.j2db.documentation.ServoyDocumented;

/**
 * @author gboros
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, scriptingName = "Renderable")
public interface IScriptRenderMethods
{
	public static final String JS_RENDERABLE = "Renderable";//$NON-NLS-1$

	/**
	 * @sameas com.servoy.j2db.ui.IScriptBaseMethods#js_getBgcolor()
	 */
	public String js_getBgcolor();

	public void js_setBgcolor(String clr);

	/**
	 * @sameas com.servoy.j2db.ui.IScriptBaseMethods#js_getFgcolor()
	 */
	public String js_getFgcolor();

	public void js_setFgcolor(String clr);

	/**
	 * @sameas com.servoy.j2db.ui.IScriptBaseMethods#js_isVisible()
	 */
	public boolean js_isVisible();

	public void js_setVisible(boolean b);

	/**
	 * @sameas com.servoy.j2db.ui.IScriptBaseMethods#js_isEnabled()
	 */
	public boolean js_isEnabled();

	public void js_setEnabled(boolean b);

	/**
	 * @sameas com.servoy.j2db.ui.IScriptBaseMethods#js_getLocationX()
	 */
	public int js_getLocationX();

	/**
	 * @sameas com.servoy.j2db.ui.IScriptBaseMethods#js_getLocationY()
	 */
	public int js_getLocationY();

	/**
	 * @sameas com.servoy.j2db.ui.IScriptBaseMethods#js_getAbsoluteFormLocationY()
	 */
	public int js_getAbsoluteFormLocationY();

	/**
	 * @sameas com.servoy.j2db.ui.IScriptBaseMethods#js_getWidth()
	 */
	public int js_getWidth();

	/**
	 * @sameas com.servoy.j2db.ui.IScriptBaseMethods#js_getHeight()
	 */
	public int js_getHeight();

	/**
	 * @sameas com.servoy.j2db.ui.IScriptBaseMethods#js_getName()
	 */
	public String js_getName();

	/**
	 * @sameas com.servoy.j2db.ui.IScriptBaseMethods#js_getElementType()
	 */
	public String js_getElementType();

	/**
	 * @sameas com.servoy.j2db.ui.IScriptBaseMethods#js_putClientProperty(Object, Object)
	 */
	public void js_putClientProperty(Object key, Object value);

	/**
	 * @sameas com.servoy.j2db.ui.IScriptBaseMethods#js_getClientProperty(Object)
	 */
	public Object js_getClientProperty(Object key);

	/**
	 * Gets or sets the border attribute(s) of a specified element. 
	 * 
	 * The border attributes:
	 * 
	 * borderType - EmptyBorder, EtchedBorder, BevelBorder, LineBorder, TitleBorder, MatteBorder, SpecialMatteBorder.
	 * size - (numeric value) for: bottom, left, right, top.
	 * color - (hexadecimal value) for: bottom, left, right, top.
	 * dash pattern - (numeric value) for selected side(s).
	 * rounding radius - (numeric value) for selected side(s).
	 * 
	 * NOTE: Use the same value(s) and order of attribute(s) from the element design time property "borderType".
	 *
	 * @sample
	 * //sets the border type to "LineBorder"
	 * //sets a 1 px line width for the bottom and left side of the border
	 * //sets the hexadecimal color of the border to "#ccffcc"
	 * %%prefix%%%%elementName%%.border = 'LineBorder,1,#ccffcc'
	 */
	public String js_getBorder();

	public void js_setBorder(String spec);

	/**
	 * @sameas com.servoy.j2db.ui.IScriptTransparentMethods#js_getToolTipText()
	 */
	public String js_getToolTipText();

	public void js_setToolTipText(String tooltip);

	/**
	 * Gets or sets the font name, style, and size of an element. 
	 * 
	 * font name - the name of the font family.
	 * style - the type of the font. (plain = 0; bold = 1; italic = 2; bold-italic = 3).
	 * size - the size of the font (in points).
	 * 
	 * @sample
	 * %%prefix%%%%elementName%%.font = 'Tahoma,1,11';
	 */
	public String js_getFont();

	public void js_setFont(String spec);

	/**
	 * @sameas com.servoy.j2db.ui.IScriptTransparentMethods#js_isTransparent()
	 */
	public boolean js_isTransparent();

	public void js_setTransparent(boolean b);


	/**
	 * @sameas com.servoy.j2db.ui.IScriptDataProviderMethods#js_getDataProviderID()
	 */
	public String js_getDataProviderID();
}
