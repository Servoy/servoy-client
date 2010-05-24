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

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;

import javax.swing.border.Border;

/**
 * Interface to which form elements needs to conform, to be handled in the same way for swing(rich client) or wicket(webclient) UI.
 * @author jcompagner,jblok
 * @since 3.5
 */
public interface IComponent
{
	public void setComponentEnabled(boolean enabled);
	public boolean isEnabled();
	
	public void setComponentVisible(boolean visible);
	public boolean isVisible();

	public void setLocation(Point location);
	public Point getLocation();
	
	public void setSize(Dimension size);
	public Dimension getSize();
	
	public void setForeground(Color foreground);
	public Color getForeground();

	public void setBackground(Color background);
	public Color getBackground();

	public void setFont(Font font);
	public Font getFont();

	public void setBorder(Border border);
	public Border getBorder();

	/**
	 * Javascript name setter
	 */
	public void setName(String name);
	/**
	 * Javascript name getter
	 */
	public String getName();

	public void setOpaque(boolean opaque);
	public boolean isOpaque();

	public void setCursor(Cursor cursor);

	public void setToolTipText(String tooltip);
	public String getToolTipText();
	
	/**
	 * wicket id, normally the UUID prefixed with 'sv_'
	 */
	public String getId();
}
