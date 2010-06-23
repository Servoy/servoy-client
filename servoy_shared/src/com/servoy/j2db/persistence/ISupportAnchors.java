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

import com.servoy.j2db.util.IAnchorConstants;


/**
 * Anchor support interface
 * @author jblok
 */
public interface ISupportAnchors extends IAnchorConstants
{
	public void setAnchors(int arg);

	/**
	 * Enables a component to stick to a specific side of form and/or to 
	 * grow or shrink when a window is resized. 
	 * 
	 * If opposite anchors are activated then the component with grow or 
	 * shrink with the window. For example if Top and Bottom are activated, 
	 * then the component will grow/shrink when the window is vertically 
	 * resized. If Left and Right are activated then the component
	 * will grow/shrink when the window is horizontally resized. 
	 * 
	 * If opposite anchors are not activated, then the component will 
	 * keep a constant distance from the sides of the window which
	 * correspond to the activated anchors.
	 */
	public int getAnchors();
}
