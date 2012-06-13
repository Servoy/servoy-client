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
 * Solution model tab object.
 * 
 * @author rgansevles
 *
 * @since 6.1
 */
public interface ISMTab extends ISMHasUUID, ISMHasDesignTimeProperty
{

	/**
	 * @clonedesc com.servoy.j2db.persistence.Tab#getContainsFormID()
	 * 
	 * @sample
	 * var childForm = solutionModel.newForm('childForm', 'db:/example_data/child_table', null, false, 400, 300);
	 * var anotherChildForm = solutionModel.newForm('anotherChildForm', 'db:/example_data/child_table', null, false, 400, 300);
	 * var firstTab = tabs.newTab('firstTab', 'Child Form', childForm, relation);
	 * firstTab.containsForm = anotherChildForm;
	 */
	public ISMForm getContainsForm();

	public void setContainsForm(ISMForm form);

	/**
	 * @clonedesc com.servoy.j2db.persistence.Tab#getForeground()
	 * 
	 * @sample
	 * var firstTab = tabs.newTab('firstTab', 'Child Form', childForm, relation);
	 * firstTab.foreground = '#FF0000';
	 */
	public String getForeground();

	public void setForeground(String arg);

	/**
	 * @clonedesc com.servoy.j2db.persistence.Tab#getImageMediaID()
	 * 
	 * @sample
	 * var bytes = plugins.file.readFile('d:/ball.jpg');
	 * var ballImage = solutionModel.newMedia('ball.jpg', bytes);
	 * var firstTab = tabs.newTab('firstTab', 'Child Form', childForm, relation);
	 * firstTab.imageMedia = ballImage;
	 */
	public ISMMedia getImageMedia();

	public void setImageMedia(ISMMedia media);

	/**
	 * The X coordinate of the tab. This influences the order in which the tabs are displayed. 
	 * The tabs are displayed in increasing order of the X coordinate. If two tabs have the 
	 * same X coordinate, then they are displayed in increasing order of the Y coordinate.
	 * 
	 * @sample
	 * // Create two tabs, then make the second one be displayed to the left of the first
	 * // by setting their X coordinates in the needed order.
	 * var firstTab = tabs.newTab('firstTab', 'Child Form', childForm, relation);
	 * firstTab.x = 10;
	 * var secondTab = tabs.newTab('secondTab', 'Another Child Form', anotherChildForm);
	 * secondTab.x = 0;
	 */
	public int getX();

	public void setX(int x);

	/**
	 * The Y coordinate of the tab. Together with the X coordinate, this influences the order 
	 * in which the tabs are displayed. The tabs are displayed in increasing order of the X coordinate,
	 * and if two tabs have the same X coordinate, then they are displayed in increasing order 
	 * of the Y coordinate.
	 * 
	 * @sample
	 * // Create two tabs, then make the second one be displayed to the left of the first
	 * // by setting their X to the same value and Y coordinates in the needed order. 
	 * var firstTab = tabs.newTab('firstTab', 'Child Form', childForm, relation);
	 * firstTab.x = 0;
	 * firstTab.y = 10;
	 * var secondTab = tabs.newTab('secondTab', 'Another Child Form', anotherChildForm);
	 * secondTab.x = 0;
	 * secondTab.y = 0;
	 */
	public int getY();

	public void setY(int y);

	/**
	 * @clonedesc com.servoy.j2db.persistence.Tab#getName()
	 * 
	 * @sample
	 * var firstTab = tabs.newTab('firstTab', 'Child Form', childForm, relation);
	 * firstTab.name = 'firstTabRenamed';
	 */
	public String getName();

	public void setName(String arg);

	/**
	 * @clonedesc com.servoy.j2db.persistence.Tab#getRelationName()
	 * 
	 * @sample
	 * var firstTab = tabs.newTab('firstTab', 'Child Form', childForm);
	 * firstTab.relationName = 'parent_table_to_child_table';
	 */
	public String getRelationName();

	public void setRelationName(String arg);

	/**
	 * @clonedesc com.servoy.j2db.persistence.Tab#getText()
	 * 
	 * @sample
	 * var firstTab = tabs.newTab('firstTab', 'Child Form', childForm, relation);
	 * firstTab.text = 'Better Title';
	 */
	public String getText();

	public void setText(String arg);

	/**
	 * @clonedesc com.servoy.j2db.persistence.Tab#getToolTipText()
	 * 
	 * @sample
	 * var firstTab = tabs.newTab('firstTab', 'Child Form', childForm, relation);
	 * firstTab.toolTipText = 'Tooltip';
	 */
	public String getToolTipText();

	public void setToolTipText(String arg);

}