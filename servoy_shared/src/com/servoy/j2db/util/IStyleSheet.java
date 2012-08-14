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

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.util.List;

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
	public static final String COLOR_TRANSPARENT = "transparent"; //$NON-NLS-1$

	public static final String BORDER_STYLE_DASHED = "dashed"; //$NON-NLS-1$
	public static final String BORDER_STYLE_DOTTED = "dotted"; //$NON-NLS-1$
	public static final String BORDER_STYLE_GROOVE = "groove"; //$NON-NLS-1$
	public static final String BORDER_STYLE_RIDGE = "ridge"; //$NON-NLS-1$
	public static final String BORDER_STYLE_INSET = "inset"; //$NON-NLS-1$
	public static final String BORDER_STYLE_NONE = "none"; //$NON-NLS-1$
	public static final String BORDER_STYLE_OUTSET = "outset"; //$NON-NLS-1$
	public static final String BORDER_STYLE_SOLID = "solid"; //$NON-NLS-1$
	public static final String BORDER_STYLE_DOUBLE = "double"; //$NON-NLS-1$
	public static final String[] BORDER_STYLES = new String[] { BORDER_STYLE_SOLID, BORDER_STYLE_NONE, BORDER_STYLE_DASHED, BORDER_STYLE_DOTTED, BORDER_STYLE_GROOVE, BORDER_STYLE_RIDGE, BORDER_STYLE_INSET, BORDER_STYLE_OUTSET, BORDER_STYLE_DOUBLE };

	/**
	 * Get the attributes for a style selector.
	 * From the attributes values can be retrieved via for example: getAttribute(CSS.Attribute.HEIGHT)
	 * @param selector the class
	 * @return The attributes
	 * @see CSS.Attribute.HEIGHT
	 * @deprecated As of 6.1 release, deprecated because of reference to swing CSS parser.
	 */
	@Deprecated
	public AttributeSet getRule(String selector);

	/**
	 * Helper method, get the font from supplied attributes.
	 * @param a the attributes
	 * @return the font or null if not defined
	 * @deprecated As of 6.1 release, deprecated because of reference to swing CSS parser.
	 */
	@Deprecated
	public Font getFont(AttributeSet a);

	/**
	 * Helper method, get the margin from supplied attributes.
	 * @param a the attributes
	 * @return the margin or null if not defined
	 * @deprecated As of 6.1 release, deprecated because of reference to swing CSS parser.
	 */
	@Deprecated
	public Insets getMargin(AttributeSet a);

	/**
	 * Helper method, get the border from supplied attributes.
	 * @param a the attributes
	 * @return the border or null if not defined
	 * @deprecated As of 6.1 release, deprecated because of reference to swing CSS parser.
	 */
	@Deprecated
	public Border getBorder(AttributeSet a);

	/**
	 * Helper method, get the horizontal align from supplied attributes.
	 * @param a the attributes
	 * @return the align or -1 if not defined
	 * @deprecated As of 6.1 release, deprecated because of reference to swing CSS parser.
	 */
	@Deprecated
	public int getHAlign(AttributeSet a);

	/**
	 * Helper method, get the vertical align from supplied attributes.
	 * @param a the attributes
	 * @return the align or -1 if not defined
	 * @deprecated As of 6.1 release, deprecated because of reference to swing CSS parser.
	 */
	@Deprecated
	public int getVAlign(AttributeSet a);

	/**
	 * Helper method, get the foreground color from supplied attributes.
	 * @param a the attributes
	 * @return the foreground color
	 * @deprecated As of 6.1 release, deprecated because of reference to swing CSS parser.
	 */
	@Deprecated
	public Color getForeground(AttributeSet a);

	/**
	 * Helper method, get the background color from supplied attributes.
	 * @param a the attributes
	 * @return the background color
	 * @deprecated As of 6.1 release, deprecated because of reference to swing CSS parser.
	 */
	@Deprecated
	public Color getBackground(AttributeSet a);


	/**
	 * Check whatever the supplied attributes have border attribute
	 * @param a the attributes
	 * @return true if the attributes have border attribute, false otherwise
	 * @deprecated As of 6.1 release, deprecated because of reference to swing CSS parser.
	 */
	@Deprecated
	public boolean hasBorder(AttributeSet s);

	/**
	 * Check whatever the supplied attributes have margin attribute
	 * @param a the attributes
	 * @return true if the attributes have margin attribute, false otherwise
	 * @deprecated As of 6.1 release, deprecated because of reference to swing CSS parser.
	 */
	@Deprecated
	public boolean hasMargin(AttributeSet s);

	/**
	 * Check whatever the supplied attributes have font attribute
	 * @param a the attributes
	 * @return true if the attributes have font attribute, false otherwise
	 * @deprecated As of 6.1 release, deprecated because of reference to swing CSS parser.
	 */
	@Deprecated
	public boolean hasFont(AttributeSet s);

	/**
	 * Get the attributes for a style selector.
	 * From the attributes values can be retrieved via for example: getAttribute("height")
	 * @param selector the class
	 * @return The attributes
	 */
	public IStyleRule getCSSRule(String selector);

	/**
	 * Helper method, get the font from supplied attributes.
	 * @param a the attributes
	 * @return the font or null if not defined
	 */
	public Font getFont(IStyleRule a);

	/**
	 * Helper method, get the margin from supplied attributes.
	 * @param a the attributes
	 * @return the margin or null if not defined
	 */
	public Insets getMargin(IStyleRule a);

	/**
	 * Helper method, get the border from supplied attributes.
	 * @param a the attributes
	 * @return the border or null if not defined
	 */
	public Border getBorder(IStyleRule a);

	/**
	 * Helper method, get the horizontal align from supplied attributes.
	 * @param a the attributes
	 * @return the align or -1 if not defined
	 */
	public int getHAlign(IStyleRule a);

	/**
	 * Helper method, get the vertical align from supplied attributes.
	 * @param a the attributes
	 * @return the align or -1 if not defined
	 */
	public int getVAlign(IStyleRule a);

	/**
	 * Helper method, get the foreground color from supplied attributes.
	 * @param a the attributes
	 * @return the foreground color
	 */
	public Color getForeground(IStyleRule a);

	/**
	 * Helper method, get the multiple foregrounds colors from supplied attributes.
	 * Needed in cases like semitransparent color fallback mechanism:
	 * <p>ex: 
	 * <br/>color: rgb(100,0,0);<br/>
	 * color: rgba(255,0,0,0.5);
	 * </p>
	 * @param a the attributes
	 * @return the background color
	 */
	public List<Color> getForegrounds(IStyleRule a);

	/**
	 * Helper method, get the background color from supplied attributes.
	 * @param a the attributes
	 * @return the background color
	 */
	public Color getBackground(IStyleRule a);

	/**
	 * Helper method, get the multiple background colors from supplied attributes.
	 * Needed in cases like semitransparent color fallback mechanism:
	 * <p>ex: 
	 * <br/>background-color: rgb(100,0,0);<br/>
	 * background-color: rgba(255,0,0,0.5);
	 * </p>
	 * @param a the attributes
	 * @return the background color
	 */
	public List<Color> getBackgrounds(IStyleRule a);

	/**
	 * Check whatever the supplied attributes have border attribute
	 * @param a the attributes
	 * @return true if the attributes have border attribute, false otherwise
	 */
	public boolean hasBorder(IStyleRule s);

	/**
	 * Check whatever the supplied attributes have margin attribute
	 * @param a the attributes
	 * @return true if the attributes have margin attribute, false otherwise
	 */
	public boolean hasMargin(IStyleRule s);

	/**
	 * Check whatever the supplied attributes have font attribute
	 * @param a the attributes
	 * @return true if the attributes have font attribute, false otherwise
	 */
	public boolean hasFont(IStyleRule s);

	/**
	 * Get all the style names from this stylesheet.
	 * 
	 * @return all styles(rule) names
	 */
	public List<String> getStyleNames();
}
