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


import java.awt.Component;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.plaf.ComponentUI;

import com.servoy.j2db.ui.IComponent;

/**
 * @author		jblok
 */
public class EnablePanel extends JPanel implements ISkinnable
{

	@Override
	public void setEnabled(boolean b)
	{
		Component[] all = getComponents();
		for (Component c : all)
		{
			setComponentEnabled(c, b);
		}
		super.setEnabled(b);
	}

	public void enablePanel(boolean b)
	{
		super.setEnabled(b);
	}

	private void setComponentEnabled(Component c, boolean b)
	{
		if (c == null) return;

		if (c instanceof IComponent)
		{
			((IComponent)c).setComponentEnabled(b);
		}
		else
		{
			c.setEnabled(b);
		}

		// Special behavior for splitpanes (we could also do if !enabledpanel..)
		if (c instanceof JSplitPane)
		{
			JSplitPane pane = (JSplitPane)c;
			Component left = pane.getLeftComponent();
			setComponentEnabled(left, b);
			Component right = pane.getRightComponent();
			setComponentEnabled(right, b);
		}
	}

	@Override
	public void setUI(ComponentUI ui)
	{
		super.setUI(ui);
	}

	public void setReadOnly(boolean readOnly)
	{
		// Does nothing here.. sup classes can impl this if they want to readonly the complete panel
	}
}
