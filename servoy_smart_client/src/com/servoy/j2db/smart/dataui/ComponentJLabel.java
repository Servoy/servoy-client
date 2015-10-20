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
package com.servoy.j2db.smart.dataui;

import javax.swing.Icon;
import javax.swing.JLabel;

import com.servoy.j2db.ui.IStandardLabel;
import com.servoy.j2db.util.ImageLoader;

/**
 * @author jcompagner
 */
public class ComponentJLabel extends JLabel implements IStandardLabel
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param text
	 * @param icon
	 * @param horizontalAlignment
	 */
	public ComponentJLabel(String text, Icon icon, int horizontalAlignment)
	{
		super(text, icon, horizontalAlignment);
	}

	/**
	 * @param text
	 * @param horizontalAlignment
	 */
	public ComponentJLabel(String text, int horizontalAlignment)
	{
		super(text, horizontalAlignment);
	}

	/**
	 * @param text
	 */
	public ComponentJLabel(String text)
	{
		super(text);
	}

	/**
	 * @param image
	 * @param horizontalAlignment
	 */
	public ComponentJLabel(Icon image, int horizontalAlignment)
	{
		super(image, horizontalAlignment);
	}

	/**
	 * @param image
	 */
	public ComponentJLabel(Icon image)
	{
		super(image);
	}

	public ComponentJLabel()
	{
		super();
	}
	
	public void setComponentEnabled(boolean enabled)
	{
		setEnabled(enabled);
	}
	public void setComponentVisible(boolean b_visible)
	{
		setVisible(b_visible);
	}

	public void setIcon(byte[] icon)
	{
		setIcon(ImageLoader.getIcon(icon, -1, -1, true));
	}
	
	public String getId() 
	{
		return (String)getClientProperty("Id");
	}
}
