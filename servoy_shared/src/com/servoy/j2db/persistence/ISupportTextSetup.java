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
package com.servoy.j2db.persistence;


import java.awt.Insets;

/**
 * @author jblok
 */
public interface ISupportTextSetup
{
	public static final int CENTER = 0;

	public static final int LEFT = 2;
	public static final int RIGHT = 4;

	public static final int TOP = 1;
	public static final int BOTTOM = 3;

	public void setVerticalAlignment(int arg);

	/**
	 * The vertical alignment of the text inside the component. Can be one of
	 * TOP, CENTER or BOTTOM.
	 * 
	 * Note that this property does not refer to the vertical alignment of the
	 * component inside the form.
	 */
	public int getVerticalAlignment();

	public void setHorizontalAlignment(int arg);

	/**
	 * Horizontal alignment of the text inside the component. Can be one of
	 * LEFT, CENTER or RIGHT.
	 * 
	 * Note that this property does not refer to the horizontal alignment
	 * of the component inside the form.
	 */
	public int getHorizontalAlignment();

	public String getFontType();

	public void setFontType(String f);

	/**
	 * The margins of the component. They are specified in this order, 
	 * separated by commas: top, left, bottom, right.
	 */
	public Insets getMargin();

	public void setMargin(Insets arg);
}
