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

package com.servoy.base.solutionmodel;



/**
 * Solution tab panel object (for mobile as well as other clients).
 * 
 * @author rgansevles
 * @author acostescu
 * @since 7.0
 */
public interface IBaseSMTabPanel extends IBaseSMComponent
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
	public IBaseSMTab newTab(String name, String text, IBaseSMForm form);

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
	public IBaseSMTab[] getTabs();
}