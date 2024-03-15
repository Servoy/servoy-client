/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

import java.awt.Color;

import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.servoy.j2db.ui.IComponent;

/**
 * @author lvostinar
 *
 */
public class ComponentAccordionPanel extends JPanel implements IComponent
{
	public ComponentAccordionPanel()
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

	public String getId()
	{
		return (String)getClientProperty("Id");
	}

	private Color foreground = null;

	public Color getForegroundAt(int index)
	{
		return getForeground();
	}

	@Override
	public void setForeground(Color fg)
	{
		foreground = fg;
		super.setForeground(fg);
	}

	int alignment = SwingConstants.LEFT;

	public void setAllTabsAlignment(int arg0)
	{
		if (arg0 < 0)
		{
			arg0 = SwingConstants.LEFT;
		}
		alignment = arg0;
	}


	public int getAlignmentAt(int index)
	{
		return alignment;
	}
}
