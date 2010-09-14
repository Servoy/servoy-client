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

import java.awt.Font;
import java.awt.Insets;

import javax.swing.border.Border;
import javax.swing.text.AttributeSet;
import javax.swing.text.html.CSS;

import com.servoy.j2db.plugins.IClientPluginAccess;

/**
 * StyleSheet interface to retrieve attributes for component decoration
 * @author jblok
 * @since 6.0
 * @see IClientPluginAccess
 */
public interface IStyleSheet
{
	/**
	 * Get the attributes for a style selector.
	 * From the attributes values can be retrieved via for example: getAttribute(CSS.Attribute.HEIGHT)
	 * @param selector the class
	 * @return The attributes
	 * @see CSS.Attribute.HEIGHT
	 */
	public AttributeSet getRule(String selector);

	/**
	 * Helper method, get the font from supplied attributes.
	 * @param a the attributes
	 * @return the font or null if not defined
	 */
	public Font getFont(AttributeSet a);

	/**
	 * Helper method, get the margin from supplied attributes.
	 * @param a the attributes
	 * @return the margin or null if not defined
	 */
	public Insets getMargin(AttributeSet a);

	/**
	 * Helper method, get the border from supplied attributes.
	 * @param a the attributes
	 * @return the border or null if not defined
	 */
	public Border getBorder(AttributeSet a);

	/**
	 * Helper method, get the horizontal align from supplied attributes.
	 * @param a the attributes
	 * @return the align or -1 if not defined
	 */
	public int getHAlign(AttributeSet a);

	/**
	 * Helper method, get the vertical align from supplied attributes.
	 * @param a the attributes
	 * @return the align or -1 if not defined
	 */
	public int getVAlign(AttributeSet a);
}
