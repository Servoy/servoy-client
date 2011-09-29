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
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.SwingConstants;

import com.l2fprod.common.swing.JOutlookBar;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.EnablePanel;
import com.servoy.j2db.util.ITabPaneAlike;
import com.servoy.j2db.util.Utils;

/**
 * Swing accordion panel implementation.
 * 
 * @author lvostinar
 * @since 6.1
 *
 */
public class AccordionPanel extends JOutlookBar implements ITabPaneAlike
{

	private final IApplication application;
	private boolean readOnly = false;

	AccordionPanel(IApplication app)
	{
		application = app;
	}

	public void setTabSeqComponents(List<Component> tabSequence)
	{
		// ignore

	}

	public List<Component> getTabSeqComponents()
	{
		return null;
	}

	public Component getFirstFocusableField()
	{
		return null;
	}

	public Component getLastFocusableField()
	{
		Component cc = this.getSelectedComponent();
		if ((cc instanceof FormLookupPanel) && ((FormLookupPanel)cc).isReady())
		{
			FormLookupPanel flp = (FormLookupPanel)cc;
			return (Component)flp.getFormPanel().getFormUI();
		}
		return null;
	}

	public boolean isTraversalPolicyEnabled()
	{
		return true;
	}

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

	public boolean isReadOnly()
	{
		return readOnly;
	}

	public int getTabIndex(Component component)
	{
		for (int i = 0; i < getTabCount(); i++)
		{
			Component comp = getComponentAt(i);
			if (comp == component)
			{
				return i;
			}
		}
		return -1;
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

	public boolean removeTabAtPos(int index)
	{
		Component comp = getComponentAt(index);
		if (comp instanceof FormLookupPanel)
		{
			List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>();
			boolean ok = ((FormLookupPanel)comp).notifyVisible(false, invokeLaterRunnables);
			Utils.invokeLater(application, invokeLaterRunnables);
			if (!ok) return false;
		}
		super.removeTabAt(index);
		return true;
	}

	public boolean removeAllTabs()
	{
		for (int i = 0; i < getTabCount(); i++)
		{
			Component comp = getComponentAt(i);
			if (comp instanceof FormLookupPanel)
			{
				List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>();
				boolean ok = ((FormLookupPanel)comp).notifyVisible(false, invokeLaterRunnables);
				Utils.invokeLater(application, invokeLaterRunnables);
				if (!ok) return false;
			}
		}
		try
		{
			super.removeAll();
		}
		catch (Exception ex1)
		{
			// do it one more time to be sure...
			try
			{
				super.removeAll();
			}
			catch (Exception ex2)
			{
				Debug.error("Error removing all tabs", ex2); //$NON-NLS-1$
			}
		}
		return true;
	}


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

	@Override
	public boolean isEnabledAt(int index)
	{
		return super.isEnabledAt(index) && isEnabled();
	}

	@Override
	public void setEnabled(boolean enabled)
	{
		super.setEnabled(enabled);
		for (int i = 0; i < getTabCount(); i++)
		{
			setEnabledAt(i, enabled);
		}
	}

	int alignment = SwingConstants.LEFT;

	@Override
	public void setAllTabsAlignment(int arg0)
	{
		alignment = arg0;
		super.setAllTabsAlignment(arg0);
	}


	@Override
	public int getAlignmentAt(int index)
	{
		if (alignment >= 0)
		{
			return alignment;
		}
		else
		{
			return super.getAlignmentAt(index);
		}
	}

	private Color foreground = null;

	@Override
	public Color getForegroundAt(int index)
	{
		Color superForeground = super.getForegroundAt(index);
		if (superForeground == null)
		{
			superForeground = foreground;
		}
		return superForeground;
	}

	@Override
	public void setForeground(Color fg)
	{
		foreground = fg;
		super.setForeground(fg);
	}

}
