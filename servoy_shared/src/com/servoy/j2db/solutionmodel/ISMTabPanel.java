/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.j2db.solutionmodel;


/**
 * Solution tab panel object.
 * 
 * @author rgansevles
 *
 * @since 6.1
 */
public interface ISMTabPanel extends ISMComponent
{

	/**
	 * Adds a new tab with the text label and JSForm.
	 *
	 * @sample 
	 * // Create a parent form.
	 * var form = solutionModel.newForm('parentForm', 'db:/example_data/parent_table', null, false, 640, 480);
	 * // Create a first child form.
	 * var childOne = solutionModel.newForm('childOne', 'db:/example_data/child_table', null, false, 400, 300);
	 * childOne.newField('child_table_text', JSField.TEXT_FIELD, 10, 10, 100, 20);
	 * // Create a relation to link the parent form to the first child form.
	 * var parentToChild = solutionModel.newRelation('parentToChild','db:/example_data/parent_table','db:/example_data/child_table',JSRelation.INNER_JOIN);
	 * parentToChild.newRelationItem('parent_table_id','=','child_table_parent_id');
	 * // Create a second child form.
	 * var childTwo = solutionModel.newForm('childTwo', 'db:/example_data/my_table', null, false, 400, 300);
	 * childTwo.newField('my_table_image', JSField.IMAGE_MEDIA, 10, 10, 100, 100);
	 * // Create a tab panel and add two tabs to it, with the two child forms.
	 * var tabPanel = form.newTabPanel('tabs', 10, 10, 620, 460);
	 * tabPanel.newTab('tab1', 'Child Two', childOne, parentToChild); // The first form uses the relation.
	 * tabPanel.newTab('tab2', 'Child Two', childTwo);
	 *
	 * @param name The name of the new tab.
	 *
	 * @param text The text to be displayed on the new tab.
	 *
	 * @param form The JSForm instance that should be displayed in the new tab.
	 *
	 * @return A JSTab instance representing the newly created and added tab.
	 */
	public ISMTab newTab(String name, String text, ISMForm form);

	/**
	 * Adds a new tab with the text label and JSForm and JSRelation (can be null for unrelated).
	 *
	 * @sample 
	 * // Create a parent form.
	 * var form = solutionModel.newForm('parentForm', 'db:/example_data/parent_table', null, false, 640, 480);
	 * // Create a first child form.
	 * var childOne = solutionModel.newForm('childOne', 'db:/example_data/child_table', null, false, 400, 300);
	 * childOne.newField('child_table_text', JSField.TEXT_FIELD, 10, 10, 100, 20);
	 * // Create a relation to link the parent form to the first child form.
	 * var parentToChild = solutionModel.newRelation('parentToChild','db:/example_data/parent_table','db:/example_data/child_table',JSRelation.INNER_JOIN);
	 * parentToChild.newRelationItem('parent_table_id','=','child_table_parent_id');
	 * // Create a second child form.
	 * var childTwo = solutionModel.newForm('childTwo', 'db:/example_data/my_table', null, false, 400, 300);
	 * childTwo.newField('my_table_image', JSField.IMAGE_MEDIA, 10, 10, 100, 100);
	 * // Create a tab panel and add two tabs to it, with the two child forms.
	 * var tabPanel = form.newTabPanel('tabs', 10, 10, 620, 460);
	 * tabPanel.newTab('tab1', 'Child Two', childOne, parentToChild); // The first form uses the relation.
	 * tabPanel.newTab('tab2', 'Child Two', childTwo);
	 *
	 * @param name The name of the new tab.
	 *
	 * @param text The text to be displayed on the new tab.
	 *
	 * @param form The JSForm instance that should be displayed in the new tab.
	 *
	 * @param relation A JSRelation object that relates the parent form with the form
	 *                          that will be displayed in the new tab.
	 *                          
	 * @return A JSTab instance representing the newly created and added tab.
	 */
	public ISMTab newTab(String name, String text, ISMForm form, Object relation);

	/**
	 * Returns a JSTab instance representing the tab which has the specified name.
	 * 
	 * @param name The name of the tab that should be returned.
	 * 
	 * @sample
	 * var tabPanel = form.newTabPanel('tabs', 10, 10, 620, 460);
	 * tabPanel.newTab('tab1', 'Child Two', childOne);
	 * tabPanel.newTab('tab2', 'Child Two', childTwo);
	 * tabPanel.getTab('tab2').text = 'Child Two Changed';
	 * 
	 * @return A JSTab instance represented the requested tab.
	 */
	public ISMTab getTab(String name);

	/**
	 * Removes the tab with the specified name from the tab panel.
	 *  
	 * @param name the name of the tab to be removed
	 * 
	 * @sample
	 * var tabPanel = form.newTabPanel('tabs', 10, 10, 620, 460);
	 * tabPanel.newTab('tab1', 'Child Two', childOne);
	 * tabPanel.newTab('tab2', 'Child Two', childTwo);
	 * tabPanel.removeTab('tab1');
	 * 
	 */
	public void removeTab(String name);

	/**
	 * Returns an array of JSTab instances holding the tabs of the tab panel.
	 *
	 * @sample
	 * var tabPanel = form.newTabPanel('tabs', 10, 10, 620, 460);
	 * tabPanel.newTab('tab1', 'Child Two', childOne);
	 * tabPanel.newTab('tab2', 'Child Two', childTwo);
	 * var tabs = tabPanel.getTabs();
	 * for (var i=0; i<tabs.length; i++)
	 * 	application.output("Tab " + i + " has text " + tabs[i].text);
	 * 
	 * @return An array of JSTab instances representing all tabs of this tabpanel.
	 */
	public ISMTab[] getTabs();

	/**
	 * @clonedesc com.servoy.j2db.persistence.TabPanel#getScrollTabs()
	 * 
	 * @sample
	 * var tabPanel = form.newTabPanel('tabs', 10, 10, 200, 200);
	 * tabPanel.newTab('tab1', 'Child Two', childOne, parentToChild); // The first form uses the relation.
	 * tabPanel.newTab('tab2', 'Child Two', childTwo);
	 * tabPanel.scrollTabs = true;
	 */
	public boolean getScrollTabs();

	/**
	 * @clonedesc com.servoy.j2db.persistence.TabPanel#getTabOrientation()
	 * 
	 * @sample
	 * var tabPanel = form.newTabPanel('tabs', 10, 10, 620, 460);
	 * tabPanel.newTab('tab1', 'Child Two', childOne, parentToChild); // The first form uses the relation.
	 * tabPanel.newTab('tab2', 'Child Two', childTwo);
	 * // The SM_ALIGNMENT constants TOP, RIGHT, BOTTOM and LEFT can be used to put the
	 * // tabs into the needed position. Use SM_DEFAULTS.NONE to hide the tabs.
	 * // The SM_ALIGNMENT constants SPLIT_HORIZONTAL, SPLIT_VERTICAL can be used to create a split pane
	 * // where the first tab will be left component and the second tab will the right component. 
	 * tabPanel.tabOrientation = SM_ALIGNMENT.BOTTOM;  
	 */
	public int getTabOrientation();

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSGraphicalComponent#getTabSeq()
	 */
	public int getTabSeq();

	public void setOnChange(ISMMethod method);

	/**
	 * @clonedesc com.servoy.j2db.persistence.TabPanel#getOnChangeMethodID()
	 *
	 * @sample 
	 * var onChangeMethod = form.newMethod('function onTabChange(previousIndex, event) { application.output("Tab changed from previous index " + previousIndex + " at " + event.getTimestamp()); }');
	 * var tabPanel = form.newTabPanel('tabs', 10, 10, 620, 460);
	 * tabPanel.newTab('tab1', 'Child Two', childOne);
	 * tabPanel.newTab('tab2', 'Child Two', childTwo);
	 * tabPanel.onChange = onChangeMethod;
	 */
	public ISMMethod getOnChange();

	public void setScrollTabs(boolean arg);

	/**
	 * sets the tab orientation, use one of the ALIGNMENT constants: SM_ALIGNMENT.TOP,BOTTOM,LEFT,RIGHT
	 * or use SM_DEFAULTS.NONE to hide the tabs.
	 */
	public void setTabOrientation(int arg);

	public void setTabSeq(int arg);

}