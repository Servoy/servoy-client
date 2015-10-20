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
package com.servoy.j2db.gui;


import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.plaf.ComponentUI;

import com.servoy.j2db.smart.dataui.FormLookupPanel;
import com.servoy.j2db.util.EnablePanel;
import com.servoy.j2db.util.ISkinnable;


/**
 * @author jblok
 */
public class EnableTabPanel extends JTabbedPane implements ISkinnable
{
	private boolean readOnly = false;

	@Override
	public void setEnabled(boolean b)
	{
		super.setEnabled(b);
		if (!b)
		{
			if (getComponentCount() > 0) setSelectedIndex(0);
		}
		Component[] comps = getComponents();
		for (Component c : comps)
		{
			c.setEnabled(b);
		}
	}

	/**
	 * @see javax.swing.JTabbedPane#setEnabledAt(int, boolean)
	 */
	@Override
	public void setEnabledAt(int index, boolean enabled)
	{
		super.setEnabledAt(index, enabled);
		if (getTabCount() > index)
		{
			getComponentAt(index).setEnabled(enabled);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.util.ITabPaneAlike#setReadOnly(boolean)
	 */
	public void setReadOnly(boolean b)
	{
		for (int i = 0; i < getTabCount(); i++)
		{
			Component comp = getComponentAt(i);
			if (comp instanceof EnablePanel)
			{
				((EnablePanel)comp).setReadOnly(b);
			}
		}
		readOnly = b;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.util.ITabPaneAlike#isReadOnly()
	 */
	public boolean isReadOnly()
	{
		return readOnly;
	}


	@Override
	public void setUI(ComponentUI ui)
	{
		super.setUI(ui);
	}

	public void addTab(String name, String text, Icon icon, Component flp, String tip)
	{
		super.addTab(text, icon, flp, tip);
		// Propagate the current opacity to the new tab.
		if (flp instanceof JComponent)
		{
			JComponent jFLP = (JComponent)flp;
			jFLP.setOpaque(isOpaque());
		}
	}

	public void insertTab(String name, String text, Icon icon, Component flp, String tip, int index)
	{
		super.insertTab(text, icon, flp, tip, index);
		// Propagate the current opacity to the new tab.
		if (flp instanceof JComponent)
		{
			JComponent jFLP = (JComponent)flp;
			jFLP.setOpaque(isOpaque());
		}
	}

	public String getNameAt(int index)
	{
		Component c = getComponentAt(index);
		if (c != null) return c.getName();
		return null;
	}

	public String getFormNameAt(int index)
	{
		Component c = getComponentAt(index);
		if (c != null) return ((FormLookupPanel)c).getFormName();
		return null;
	}

	// Here just do the workaround with changing the UIs.
	@Override
	public void setOpaque(boolean isOpaque)
	{
		super.setOpaque(isOpaque);
		// Also propagate the change to all tabs.
		for (int i = 0; i < getTabCount(); i++)
		{
			Component comp = getComponentAt(i);
			if (comp instanceof EnablePanel)
			{
				((EnablePanel)comp).setOpaque(isOpaque);
			}
		}
	}

}
