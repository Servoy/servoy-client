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

import java.awt.Insets;

import javax.swing.border.Border;

/**
 * Interface for settings properties in changes recorder.
 * 
 * @author lvostinar
 */
public interface IStylePropertyChangesRecorder extends IStylePropertyChanges
{
	public void setBgcolor(String bgcolor);

	public void setFgcolor(String clr);

	public void setBorder(String border);

	public void setTransparent(boolean transparent);

	public void setLocation(int x, int y);

	public void setFont(String spec);

	public void setVisible(boolean visible);

	public void setSize(int width, int height, Border border, Insets margin, int fontSize);

	public void setSize(int width, int height, Border border, Insets margin, int fontSize, boolean isButton, int valign);
}
