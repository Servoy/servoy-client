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

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.gui.EnableTabPanel;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ITabPaneAlike;
import com.servoy.j2db.util.Utils;

/**
 * Regular tab panel with several child forms and a visible tab selection 
 * control.
 */
public class TabbedPanel extends EnableTabPanel implements ITabPaneAlike
{
	private final IApplication application;

	TabbedPanel(IApplication app)
	{
		application = app;
	}

	/**
	 * @see com.servoy.j2db.util.ITabPaneAlike#getTabIndex(java.awt.Component)
	 */
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

	public Component getFirstFocusableField()
	{
		return null;
	}

	public List<Component> getTabSeqComponents()
	{
		return null;
	}

	public boolean isTraversalPolicyEnabled()
	{
		return true;
	}

	public void setTabSeqComponents(List<Component> tabSequence)
	{
		// ignore
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
}