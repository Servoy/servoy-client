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

import com.servoy.j2db.scripting.api.solutionmodel.IBaseSMTab;


/**
 * Solution model tab object.
 * 
 * @author rgansevles
 *
 * @since 6.1
 */
public interface ISMTab extends IBaseSMTab, ISMHasUUID
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
	 * @clonedesc com.servoy.j2db.persistence.Tab#getToolTipText()
	 * 
	 * @sample
	 * var firstTab = tabs.newTab('firstTab', 'Child Form', childForm, relation);
	 * firstTab.toolTipText = 'Tooltip';
	 */
	public String getToolTipText();

	public void setToolTipText(String arg);

	/**
	 * @clonedesc com.servoy.j2db.persistence.Tab#getMnemonic()
	 * 
	 * @sample
	 * var childForm = solutionModel.newForm('childForm', 'db:/example_data/child_table', null, false, 400, 300);
	 * var anotherChildForm = solutionModel.newForm('anotherChildForm', 'db:/example_data/child_table', null, false, 400, 300);
	 * var firstTab = tabs.newTab('firstTab', 'Child Form', childForm, relation);
	 * firstTab.mnemonic = 'a';
	 */
	public String getMnemonic();

	public void setMnemonic(String arg);

}