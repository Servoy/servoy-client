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

import com.servoy.base.scripting.annotations.ServoyClientSupport;


/**
 * Solution model base component interface, for mobile & other clients.
 * 
 * @author rgansevles
 * @author acostescu
 *
 * @since 7.0
 */
@ServoyClientSupport(mc = true, wc = true, sc = true)
public interface IBaseSMComponent
{

	/**
	 * The x coordinate of the component on the form.
	 * 
	 * @sample
	 * var field = form.newField('parent_table_text', JSField.TEXT_FIELD, 10, 10, 100, 20);
	 * application.output('original location: ' + field.x + ', ' + field.y);
	 * field.x = 90;
	 * field.y = 90;
	 * application.output('changed location: ' + field.x + ', ' + field.y);
	 */
	public int getX();

	/**
	 * The y coordinate of the component on the form.
	 * 
	 * @sampleas getX()
	 */
	public int getY();

	/**
	 * @clonedesc com.servoy.j2db.persistence.BaseComponent#getName()
	 * 
	 * @sample
	 * var form = solutionModel.newForm('someForm', 'db:/example_data/parent_table', null, false, 620, 300);
	 * var label = form.newLabel('Label', 10, 10, 150, 150);
	 * label.name = 'myLabel'; // Give a name to the component.
	 * forms['someForm'].controller.show()
	 * // Now use the name to access the component.
	 * forms['someForm'].elements['myLabel'].text = 'Updated text';
	 */
	public String getName();

	/**
	 * @clonedesc com.servoy.j2db.persistence.BaseComponent#getEnabled()
	 * 
	 * @sample
	 * var form = solutionModel.newForm('printForm', 'db:/example_data/parent_table', null, false, 400, 300);
	 * var field = form.newField('parent_table_text', JSField.TEXT_FIELD, 10, 10, 100, 20);
	 * field.enabled = false;
	 */
	public boolean getEnabled();

	/**
	 * @clonedesc com.servoy.j2db.persistence.BaseComponent#getVisible()
	 * 
	 * @sample
	 * var form = solutionModel.newForm('printForm', 'db:/example_data/parent_table', null, false, 400, 300);
	 * var field = form.newField('parent_table_text', JSField.TEXT_FIELD, 10, 10, 100, 20);
	 * field.visible = false;
	 */
	public boolean getVisible();

	/**
	 * The width in pixels of the component.
	 * 
	 * @sample
	 * var field = form.newField('parent_table_text', JSField.TEXT_FIELD, 10, 10, 100, 20);
	 * application.output('original width: ' + field.width);
	 * application.output('original height: ' + field.height);
	 * field.width = 200;
	 * field.height = 100;
	 * application.output('modified width: ' + field.width);
	 * application.output('modified height: ' + field.height);
	 */
	public int getWidth();

	/**
	 * The height in pixels of the component.
	 * 
	 * @sampleas getWidth()
	 */
	public int getHeight();

	/**
	 * A String representing a group ID for this component. If several
	 * components have the same group ID then they belong to the same
	 * group of components. Using the group itself, all components can
	 * be disabled/enabled or made invisible/visible.
	 * The group id should be a javascript compatible identifier to allow access of the group in scripting.
	 *
	 * @sample 
	 * var form = solutionModel.newForm('someForm', 'db:/example_data/parent_table', null, false, 400, 300);
	 * var label = form.newLabel('Green', 10, 10, 100, 20);
	 * var field = form.newField('parent_table_text', JSField.TEXT_FIELD, 10, 40, 100, 20);
	 * label.groupID = 'someGroup';
	 * field.groupID = 'someGroup';	
	 * forms['someForm'].elements.someGroup.enabled = false;
	 */
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public String getGroupID();

// TODO use this description for mobile and another sample somehow
//	/**
//	 * The jQuery mobile style (theme) to use for this field.
//	 */
	/**
	 * @clonedesc com.servoy.base.persistence.IBaseComponent#getStyleClass()
	 * 
	 * @sample
	 * var form = solutionModel.newForm('printForm', 'db:/example_data/parent_table', null, false, 400, 300);
	 * var field = form.newField('parent_table_text', JSField.TEXT_FIELD, 10, 10, 100, 20);
	 * var style = solutionModel.newStyle('myStyle','field.fancy { background-color: yellow; }');
	 * form.styleName = 'myStyle'; // First set the style on the form.
	 * field.styleClass = 'fancy'; // Then set the style class on the field.
	 */
	public String getStyleClass();

	public void setX(int x);

	public void setY(int y);

	public void setName(String arg);

	public void setEnabled(boolean arg);

	public void setVisible(boolean arg);

	public void setWidth(int width);

	public void setHeight(int height);

	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public void setGroupID(String arg);

	public void setStyleClass(String arg);
}