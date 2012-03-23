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
package com.servoy.j2db.ui.runtime;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.dataprocessing.IFoundSet;


/**
 * Base interface for tabpanel-like components.
 * 
 * @author rgansevles
 * @since 6.1
 */

public interface IRuntimeTabPaneAlike extends HasRuntimeReadOnly, IRuntimeComponent
{
	/**
	 * Adds a relationless or related form as a tab in a specified tabpanel.
	 * 
	 * @sample %%prefix%%%%elementName%%.addTab(forms.orders,'ordersTab','Orders',null,null,'#000000','#BBCCEE');
	 *
	 * @param formName the specified form/form name you wish to add as a tab
	 * @param name optional the specified name for the tab or NULL (default is null)
	 * @param tabText optional the specified text for the tab (default is null)
	 * @param tooltip optional a specified tooltip for the tab (default is null)
	 * @param iconURL optional a specified icon image or icon URL for the tab (default is null)
	 * @param fg optional the HTML RGB Hexadecimal foreground color for the tab (default is null)
	 * @param bg optional the HTML RGB Hexadecimal background color for the tab (default is null)
	 * @param relation optional the specified name of the related foundset (default is null)
	 * @param tabIndex optional the specified index of a tab, default is -1, will add tab to the end
	 * 
	 * @return a boolean value indicating if tab was successfully added
	 */
	public boolean addTab(String formName, String tabName, String tabText, String toolTip, String iconURL, String fg, String bg, String relation, int tabIndex);

	public boolean addTab(String formName, String tabName, String tabText, String toolTip, String iconURL, String fg, String bg, IFoundSet relation,
		int tabIndex);

	public boolean addTab(String formName, String tabName, String tabText, String toolTip, String iconURL, String fg, String bg, String relation);

	public boolean addTab(String formName, String tabName, String tabText, String toolTip, String iconURL, String fg, String bg, IFoundSet relation);

	public boolean addTab(String formName, String tabName, String tabText, String toolTip, String iconURL, String fg, String bg);

	public boolean addTab(String formName, String tabName, String tabText, String toolTip, String iconURL);

	public boolean addTab(String formName, String tabName);


	/**
	 * Removes a specified tab in a tabpanel; can be based on a relation or relationless.
	 * 
	 * NOTE: In Servoy 4.x (and higher), the addTab function applies to relationless or related tabs in a tabpanel.
	 *
	 * @sample 
	 * %%prefix%%%%elementName%%.removeTabAt(3);
	 *
	 * @param index The index of the tab to remove.
	 * 
	 * @return a boolean value indicating if tab was successfully removed
	 */
	public boolean removeTabAt(int i);

	/**
	 * Removes all tabs for a specified tabpanel.
	 * 
	 * @sample 
	 * %%prefix%%%%elementName%%.removeAllTabs();
	 * 
	 * @return a boolean value indicating if tabs were successfully removed
	 */
	@JSFunction
	public boolean removeAllTabs();

	/**
	 * Returns the maximum tab index for a specified tabpanel.
	 *
	 * @sample 
	 * var max = %%prefix%%%%elementName%%.getMaxTabIndex();
	 * 
	 * @return
	 * maximum tab index (number)
	 */
	public int getMaxTabIndex();

	/**
	 * Returns the form name for a specified tab of a tabpanel.
	 * 
	 * @sample
	 * var formName = %%prefix%%%%elementName%%.getSelectedTabFormName(3);
	 * 
	 * @param i index of the tab
	 * 
	 * @return the name of the form
	 */
	public String getTabFormNameAt(int i);

	/**
	 * Returns the relation name for a specified tab of a tabpanel.
	 * 
	 * @sample
	 * var relName = %%prefix%%%%elementName%%.getTabRelationNameAt(3);
	 * 
	 * @param i index of the tab
	 * 
	 * @return relation name
	 */
	public String getTabRelationNameAt(int i);

	/**
	 * Returns the foreground color for a specified tab of a tabpanel. 
	 * 
	 * @sample
	 * var color = %%prefix%%%%elementName%%.getTabFGColorAt(3); 
	 * 
	 * @param i the number of the specified tab
	 * 
	 * @return color as hexadecimal RGB string
	 */
	public String getTabFGColorAt(int i);

	/**
	 * Sets the foreground color for a specified tab in a tabpanel.
	 * 
	 * @sample
	 * %%prefix%%%%elementName%%.setTabFGColorAt(3,'#000000');
	 * 
	 * @param i the number of the specified tab
	 * @param s the hexadecimal RGB color value to be set.
	 */
	public void setTabFGColorAt(int i, String s);

	/**
	 * Gets or sets the selected tab index for the specified tabpanel.
	 * When setting the value either the tab index or the tab name can be used.
	 * When getting the value, the tab index (not the name) will be returned all the time.
	 *
	 * @sample 
	 * //gets the selected tab index of the tabpanel
	 * var current = %%prefix%%%%elementName%%.tabIndex;
	 * 
	 * //sets (goes to) the selected tabIndex of the tabpanel
	 * %%prefix%%%%elementName%%.tabIndex = current + 1;
	 * 
	 * //or sets (goes to) the tab with the specified name
	 * %%prefix%%%%elementName%%.tabIndex = 'tab_name';
	 */
	public int getTabIndex();

	public void setTabIndex(int index);

	public void setTabIndex(String name);

	/**
	 * Returns the name - the "name" design time property value - for a specified tab of a tabpanel. 
	 * 
	 * @sample
	 * var tabName = %%prefix%%%%elementName%%.getTabNameAt(3);
	 * 
	 * @param i The number of the specified tab.
	 * 
	 * @return The tab name
	 */
	public String getTabNameAt(int i);

	/**
	 * Returns the text for a specified tab of a tabpanel.
	 * 
	 * @sample var tabText = %%prefix%%%%elementName%%.getTabTextAt(3);
	 * 
	 * @param i The number of the specified tab.
	 * 
	 * @return The tab text.
	 */
	public String getTabTextAt(int i);

	/**
	 * Sets the text for a specified tab in a tabpanel.
	 * 
	 * @sample %%prefix%%%%elementName%%.setTabTextAt(3,'newTitle');
	 *
	 * @param index the number of the specified tab
	 * @param text the text to be set for the specified tab
	 */
	public void setTabTextAt(int i, String s);

	/**
	 * Sets the status of a specified tab in a tabpanel.
	 * 
	 * @sample
	 * %%prefix%%%%elementName%%.setTabEnabledAt(3,true);
	 * 
	 * @param i the number of the specified tab.
	 * @param b true if enabled; or false if disabled.
	 */
	public void setTabEnabledAt(int i, boolean b);

	/**
	 * Returns the enabled status of a specified tab in a tabpanel.
	 * 
	 * @sample
	 * var status = %%prefix%%%%elementName%%.isTabEnabledAt(3);
	 * 
	 * @param i the number of the specified tab.
	 * 
	 * @return True if tab is enabled, false otherwise.
	 */
	public boolean isTabEnabledAt(int i);

}