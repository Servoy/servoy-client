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

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.ui.IFormLookupPanel;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.EnablePanel;
import com.servoy.j2db.util.ITabPaneAlike;
import com.servoy.j2db.util.SafeArrayList;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.gui.FixedCardLayout;

/**
 * Special case of tab panel that does not display the tab selection control.
 */
public class TablessPanel extends EnablePanel implements ITabPaneAlike
{
	private final IApplication application;
	private final FixedCardLayout forms;
	private int selectedTab = -1;
	private ChangeListener listner;
	private boolean readOnly;
	private static int counter = 0;

	TablessPanel(IApplication app)
	{
		application = app;
		forms = new FixedCardLayout();
		setLayout(forms);
		setFocusable(true);
		setFocusCycleRoot(true);
		setFocusTraversalPolicy(ServoyFocusTraversalPolicy.datarenderPolicy);
	}

	/**
	 * @see com.servoy.j2db.util.ITabPaneAlike#getTabIndex(java.awt.Component)
	 */
	public int getTabIndex(Component component)
	{
		for (int i = 0; i < getComponentCount(); i++)
		{
			Component comp = getComponent(i);
			if (comp == component)
			{
				return i;
			}
		}
		return -1;
	}

	public void addChangeListener(ChangeListener l)
	{
		listner = l;
	}

	public void addTab(String name, String text, Icon c, Component flp, String tooltip)
	{
		int count = getTabCount();
		setTitleAt(count, text);
		// if component with same name is already present in FixedCardLayout will be removed, so name must be unique
		add(flp, ((IFormLookupPanel)flp).getFormName() + "_" + checkCounter());

		// By the time a tab is added, the opacity may have been already set.
		// So just make sure its propagated to the new tab.
		if (flp instanceof JComponent)
		{
			JComponent jFLP = (JComponent)flp;
			jFLP.setOpaque(isOpaque());
		}

		if (count == 0)
		{
			setSelectedIndex(0);
		}

		if (tooltip != null)
		{
			((JComponent)flp).setToolTipText(tooltip);
		}
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				getParent().validate();
			}
		});
	}

	/**
	 * @see com.servoy.j2db.util.ITabPaneAlike#insertTab(java.lang.String, java.lang.String, javax.swing.Icon, java.awt.Component, java.lang.String, int)
	 */
	public void insertTab(String name, String text, Icon icon, Component flp, String tooltip, int index)
	{
		setTitleAt(index, text);

		// because we use FixedCardLayout, we need to first remove the forms from the right,
		// so the layout manager will have the forms in the right order
		ArrayList<Component> nextComponents = new ArrayList<Component>();
		nextComponents.add(flp);
		int componentCount = getComponentCount();
		for (int i = index; i < componentCount; i++)
		{
			nextComponents.add(getComponent(index));
			remove(index);
		}
		for (Component c : nextComponents)
			add(c, ((IFormLookupPanel)c).getFormName() + "_" + checkCounter());

		// By the time a tab is inserted, the opacity may have been already set.
		// So just make sure its propagated to the new tab.
		if (flp instanceof JComponent)
		{
			JComponent jFLP = (JComponent)flp;
			jFLP.setOpaque(isOpaque());
		}

		if (selectedTab == -1)
		{
			setSelectedIndex(0);
		}

		if (tooltip != null)
		{
			((JComponent)flp).setToolTipText(tooltip);
		}
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				getParent().validate();
			}
		});
	}

	public Component getSelectedComponent()
	{
		if (getComponentCount() > 0 && selectedTab >= 0 && selectedTab < getComponentCount())
		{
			return getComponent(selectedTab);
		}
		else
		{
			return null;
		}
	}

	public String getNameAt(int index)
	{
		Component c = getComponent(index);
		if (c != null) return c.getName();
		return null;
	}

	public String getFormNameAt(int index)
	{
		Component c = getComponent(index);
		if (c != null) return ((FormLookupPanel)c).getFormName();
		return null;
	}

	public int getSelectedIndex()
	{
		return selectedTab;
	}

	public int getTabCount()
	{
		return getComponentCount();
	}

	public void setBackgroundAt(int index, Color bg)
	{
		//ignore
	}

	public void setForegroundAt(int index, Color fg)
	{
		//ignore
	}

	public void setSelectedIndex(int i)
	{
		if (i >= 0 && i < getComponentCount())
		{
			selectedTab = i;
			forms.show(this, i, false);
			listner.stateChanged(new ChangeEvent(this));


			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					getParent().repaint();
				}
			});

		}
	}

	/*
	 * @see com.servoy.j2db.util.ITabPaneAlike#setSelectedComponent(java.awt.Component)
	 */
	public void setSelectedComponent(Component component)
	{
		Component[] components = getComponents();
		for (int i = 0; i < components.length; i++)
		{
			if (components[i] == component)
			{
				setSelectedIndex(i);
				return;
			}
		}
	}

	public void setTabPlacement(int tabPlacement)
	{
		//ignore
	}

	public void setEnabledAt(int index, boolean b)
	{
		Component[] components = getComponents();
		if (index >= 0 && index < components.length)
		{
			Component c = components[index];
			if (c instanceof IComponent)
			{
				((IComponent)c).setComponentEnabled(b);
			}
			else
			{
				c.setEnabled(b);
			}
		}
	}

	public boolean isEnabledAt(int index)
	{
		Component[] components = getComponents();
		if (components.length > index)
		{
			return components[index].isEnabled();
		}
		return true;
	}

	public Color getBackgroundAt(int index)
	{
		return null;
	}

	public Color getForegroundAt(int index)
	{
		return null;
	}

	@Override
	public void setReadOnly(boolean b)
	{
		for (int i = 0; i < getComponentCount(); i++)
		{
			Component comp = getComponent(i);
			if (comp instanceof EnablePanel)
			{
				((EnablePanel)comp).setReadOnly(b);
			}
		}
		readOnly = b;
	}

	@Override
	public void setOpaque(boolean isOpaque)
	{
		super.setOpaque(isOpaque);
		// Propagate the opacity to the children.
		for (int i = 0; i < getComponentCount(); i++)
		{
			Component comp = getComponent(i);
			if (comp instanceof EnablePanel)
			{
				((EnablePanel)comp).setOpaque(isOpaque);
			}
		}
	}

	public boolean isReadOnly()
	{
		return readOnly;
	}

	private final SafeArrayList<String> titles = new SafeArrayList<String>(3);
	private final SafeArrayList<Integer> mnemonics = new SafeArrayList<Integer>(3);

	public String getTitleAt(int index)
	{
		return titles.get(index);
	}

	public void setTitleAt(int index, String text)
	{
		titles.add(index, text);
	}

	public int getMnemonicAt(int index)
	{
		return mnemonics.get(index).intValue();
	}

	public void setMnemonicAt(int index, int mnemonic)
	{
		mnemonics.add(index, Integer.valueOf(mnemonic));
	}

	public void setTabLayoutPolicy(int scroll_tab_layout)
	{
		//ignore
	}

	public boolean removeTabAtPos(int index)
	{
		Component comp = getComponent(index);
		if (comp instanceof FormLookupPanel)
		{
			List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>();
			boolean ok = ((FormLookupPanel)comp).notifyVisible(false, invokeLaterRunnables);
			Utils.invokeLater(application, invokeLaterRunnables);
			if (!ok) return false;
		}
		remove(index);
		if (index == selectedTab)
		{
			if (selectedTab > 0)
			{
				if (index == getComponentCount())
				{
					//index moves down if it is the last tab
					setSelectedIndex(--selectedTab);
				}
				else
				{
					//index stays the same if is in the middle 
					setSelectedIndex(selectedTab);
				}
			}
			else
			{
				setSelectedIndex(0);
			}
		}
		else if (index < selectedTab)
		{
			selectedTab--;
		}

		return true;
	}

	public boolean removeAllTabs()
	{
		for (int i = 0; i < getComponentCount(); i++)
		{
			Component comp = getComponent(i);
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
		invalidate();
		getParent().repaint();
		selectedTab = -1;
		return true;
	}

	public Component getFirstFocusableField()
	{
		Component cc = this.getSelectedComponent();
		if ((cc instanceof FormLookupPanel) && ((FormLookupPanel)cc).isReady())
		{
			FormLookupPanel flp = (FormLookupPanel)cc;
			return (Component)flp.getFormPanel().getFormUI();
		}
		else
		{
			return null;
		}
	}

	public List<Component> getTabSeqComponents()
	{
		Component firstFocusable = getFirstFocusableField();
		if (firstFocusable != null)
		{
			List<Component> tabSeq = new ArrayList<Component>();
			tabSeq.add(firstFocusable);
			return tabSeq;
		}
		else
		{
			return null;
		}
	}

	private int checkCounter()
	{
		return counter++;
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
		return getFirstFocusableField();
	}

	@Override
	public void setToolTipTextAt(int index, String text)
	{
		Component c = getComponent(index);
		if (c != null) ((JComponent)c).setToolTipText(text);
	}

	@Override
	public String getToolTipTextAt(int index)
	{
		Component c = getComponent(index);
		if (c != null) return ((JComponent)c).getToolTipText();
		return null;
	}


}