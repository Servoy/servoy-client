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
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeListener;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.util.ITabPaneAlike;

/**
 * Swing accordion panel implementation.
 *
 * @author lvostinar
 * @since 6.1
 *
 */
public class AccordionPanel extends JPanel implements ITabPaneAlike
{

	private final IApplication application;
	private boolean readOnly = false;

	AccordionPanel(IApplication app)
	{
		application = app;
		setBorder(BorderFactory.createEmptyBorder());
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
		readOnly = b;
	}

	public boolean isReadOnly()
	{
		return readOnly;
	}

	public int getTabIndex(Component component)
	{
		return -1;
	}

	public String getNameAt(int index)
	{
		return null;
	}

	public String getFormNameAt(int index)
	{
		return null;
	}

	public boolean removeTabAtPos(int index)
	{
		return true;
	}

	public boolean removeAllTabs()
	{

		return true;
	}


	@Override
	public void setOpaque(boolean isOpaque)
	{
		super.setOpaque(isOpaque);
	}

	public void addTab(String name, String text, Icon icon, Component flp, String tip)
	{

	}

	public void insertTab(String name, String text, Icon icon, Component flp, String tip, int index)
	{

	}

	@Override
	public boolean isEnabledAt(int index)
	{
		return isEnabled();
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

	public void setAllTabsAlignment(int arg0)
	{
		alignment = arg0;
	}


	public int getAlignmentAt(int index)
	{
		return alignment;
	}

	private Color foreground = null;

	@Override
	public Color getForegroundAt(int index)
	{
		return null;
	}

	@Override
	public void setForeground(Color fg)
	{
		foreground = fg;
		super.setForeground(fg);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.util.ITabPaneAlike#setEnabledAt(int, boolean)
	 */
	@Override
	public void setEnabledAt(int index, boolean enabled)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.util.ITabPaneAlike#setTabPlacement(int)
	 */
	@Override
	public void setTabPlacement(int tabPlacement)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.util.ITabPaneAlike#addChangeListener(javax.swing.event.ChangeListener)
	 */
	@Override
	public void addChangeListener(ChangeListener l)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.util.ITabPaneAlike#getSelectedComponent()
	 */
	@Override
	public Component getSelectedComponent()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.util.ITabPaneAlike#setSelectedIndex(int)
	 */
	@Override
	public void setSelectedIndex(int i)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.util.ITabPaneAlike#getSelectedIndex()
	 */
	@Override
	public int getSelectedIndex()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.util.ITabPaneAlike#getTabCount()
	 */
	@Override
	public int getTabCount()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.util.ITabPaneAlike#setForegroundAt(int, java.awt.Color)
	 */
	@Override
	public void setForegroundAt(int index, Color fg)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.util.ITabPaneAlike#setBackgroundAt(int, java.awt.Color)
	 */
	@Override
	public void setBackgroundAt(int index, Color bg)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.util.ITabPaneAlike#setTitleAt(int, java.lang.String)
	 */
	@Override
	public void setTitleAt(int index, String text)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.util.ITabPaneAlike#getTitleAt(int)
	 */
	@Override
	public String getTitleAt(int index)
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.util.ITabPaneAlike#setMnemonicAt(int, int)
	 */
	@Override
	public void setMnemonicAt(int index, int mnemonic)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.util.ITabPaneAlike#getMnemonicAt(int)
	 */
	@Override
	public int getMnemonicAt(int index)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.util.ITabPaneAlike#getBackgroundAt(int)
	 */
	@Override
	public Color getBackgroundAt(int index)
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.util.ITabPaneAlike#setTabLayoutPolicy(int)
	 */
	@Override
	public void setTabLayoutPolicy(int scroll_tab_layout)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.util.ITabPaneAlike#setSelectedComponent(java.awt.Component)
	 */
	@Override
	public void setSelectedComponent(Component component)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.util.ITabPaneAlike#setToolTipTextAt(int, java.lang.String)
	 */
	@Override
	public void setToolTipTextAt(int index, String text)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.util.ITabPaneAlike#getToolTipTextAt(int)
	 */
	@Override
	public String getToolTipTextAt(int index)
	{
		// TODO Auto-generated method stub
		return null;
	}

}
