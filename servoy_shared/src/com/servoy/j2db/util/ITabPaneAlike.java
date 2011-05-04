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


import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.Icon;
import javax.swing.event.ChangeListener;


/**
 * @author jblok
 */
public interface ITabPaneAlike extends IFocusCycleRoot<Component>
{
	public void setEnabledAt(int index, boolean enabled);

	public boolean isEnabledAt(int index);

	public void setReadOnly(boolean b);

	public boolean isReadOnly();

	public void setTabPlacement(int tabPlacement);

	public void addChangeListener(ChangeListener l);

	public Component getSelectedComponent();

	public void setSelectedIndex(int i);

	public int getSelectedIndex();

	public int getTabIndex(Component component);

	public int getTabCount();

	/**
	 * @param index
	 * @param fg
	 */
	public void setForegroundAt(int index, Color fg);

	/**
	 * @param index
	 * @param bg
	 */
	public void setBackgroundAt(int index, Color bg);

	/**
	 * @param index
	 * @param text
	 */
	public void setTitleAt(int index, String text);

	public String getTitleAt(int index);

	public String getNameAt(int index);

	public String getFormNameAt(int index);

	/**
	 * @param string
	 * @param icon
	 * @param flp
	 * @param tip
	 */
	public void addTab(String name, String text, Icon icon, Component flp, String tip);

	/**
	 * @param font
	 */
	public void setFont(Font font);

	public Font getFont();

	/**
	 * @return
	 */
	public String getToolTipText();

	/**
	 * @param txt
	 */
	public void setToolTipText(String txt);

	/**
	 * @param color
	 */
	public void setForeground(Color color);

	/**
	 * @return
	 */
	public Color getBackground();

	/**
	 * @param color
	 */
	public void setBackground(Color color);

	/**
	 * @return
	 */
	public Color getForeground();

	/**
	 * @param index
	 * @return
	 */
	public Color getForegroundAt(int index);

	/**
	 * @param index
	 * @return
	 */
	public Color getBackgroundAt(int index);

	/**
	 * @param scroll_tab_layout
	 */
	public void setTabLayoutPolicy(int scroll_tab_layout);

	/**
	 * @param index
	 */
	public boolean removeTabAtPos(int index);

	/**
	 * Remove all tabs
	 */
	public boolean removeAllTabs();

	/**
	 * @param component the panel the select
	 */
	public void setSelectedComponent(Component component);

	/**
	 * @param name
	 * @param text
	 * @param icon
	 * @param flp
	 * @param tip
	 * @param index
	 */
	public void insertTab(String name, String text, Icon icon, Component flp, String tip, int index);

	/**
	 * @param key
	 * @param value
	 */
	public void putClientProperty(Object key, Object value);

	public Object getClientProperty(Object key);
}
