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

package com.servoy.j2db.scripting.solutionmodel;

import java.awt.Dimension;
import java.awt.Point;

import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.ISupportAnchors;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.persistence.ISupportPrinting;
import com.servoy.j2db.util.IAnchorConstants;

/**
 * @author lvostinar
 *
 */
public class JSBaseComponent<T extends AbstractBase & IFormElement & ISupportPrinting & ISupportBounds & ISupportAnchors> extends JSBase<T>
{

	/**
	 * @param parent
	 * @param baseComponent
	 * @param isNew
	 */
	public JSBaseComponent(IJSParent parent, T baseComponent, boolean isNew)
	{
		super(parent, baseComponent, isNew);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.BaseComponent#getAnchors()
	 * 
	 * @sample
	 * var form = solutionModel.newForm('mediaForm', 'example_data', 'parent_table', 'null', false, 400, 300);
	 * var strechAllDirectionsLabel = form.newLabel('Strech all directions', 10, 10, 380, 280);
	 * strechAllDirectionsLabel.background = 'red';
	 * strechAllDirectionsLabel.anchors = SM_ANCHOR.ALL;	
	 * var strechVerticallyLabel = form.newLabel('Strech vertically', 10, 10, 190, 280);
	 * strechVerticallyLabel.background = 'green';
	 * strechVerticallyLabel.anchors = SM_ANCHOR.WEST | SM_ANCHOR.NORTH | SM_ANCHOR.SOUTH;
	 * var strechHorizontallyLabel = form.newLabel('Strech horizontally', 10, 10, 380, 140);
	 * strechHorizontallyLabel.background = 'blue';
	 * strechHorizontallyLabel.anchors = SM_ANCHOR.NORTH | SM_ANCHOR.WEST | SM_ANCHOR.EAST;
	 * var stickToTopLeftCornerLabel = form.newLabel('Stick to top-left corner', 10, 10, 200, 100);
	 * stickToTopLeftCornerLabel.background = 'orange';
	 * stickToTopLeftCornerLabel.anchors = SM_ANCHOR.NORTH | SM_ANCHOR.WEST; // This is equivalent to SM_ANCHOR.DEFAULT 
	 * var stickToBottomRightCornerLabel = form.newLabel('Stick to bottom-right corner', 190, 190, 200, 100);
	 * stickToBottomRightCornerLabel.background = 'pink';
	 * stickToBottomRightCornerLabel.anchors = SM_ANCHOR.SOUTH | SM_ANCHOR.EAST;
	 */
	public int js_getAnchors()
	{
		int anchors = getBaseComponent(false).getAnchors();
		if (anchors <= 0) return IAnchorConstants.DEFAULT;
		return anchors;
	}

	/**
	 * The Z index of this component. If two components overlap,
	 * then the component with higher Z index is displayed above
	 * the component with lower Z index.
	 *
	 * @sample
	 * var labelBelow = form.newLabel('Green', 10, 10, 100, 50);
	 * labelBelow.background = 'green';	
	 * labelBelow.formIndex = 10;
	 * var fieldAbove = form.newField('parent_table_text', JSField.TEXT_FIELD, 10, 10, 100, 30);
	 * fieldAbove.background = '#FF0000';
	 * fieldAbove.formIndex = 20;
	 */
	public int js_getFormIndex()
	{
		return getBaseComponent(false).getFormIndex();
	}


	public void js_setFormIndex(int arg)
	{
		getBaseComponent(true).setFormIndex(arg);
	}

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
	public int js_getX()
	{
		return getBaseComponent(false).getLocation().x;
	}

	/**
	 * The y coordinate of the component on the form.
	 * 
	 * @sampleas js_getX()
	 */
	public int js_getY()
	{
		return getBaseComponent(false).getLocation().y;
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.BaseComponent#getName()
	 * 
	 * @sample
	 * var form = solutionModel.newForm('someForm', 'example_data', 'parent_table', 'null', false, 620, 300);
	 * var label = form.newLabel('Label', 10, 10, 150, 150);
	 * label.name = 'myLabel'; // Give a name to the component.
	 * forms['someForm'].controller.show()
	 * // Now use the name to access the component.
	 * forms['someForm'].elements['myLabel'].text = 'Updated text';
	 */
	public String js_getName()
	{
		return getBaseComponent(false).getName();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.BaseComponent#getPrintable()
	 * 
	 * @sample
	 * var form = solutionModel.newForm('printForm', 'example_data', 'parent_table', 'null', false, 400, 300);
	 * var printedField = form.newField('parent_table_text', JSField.TEXT_FIELD, 10, 10, 100, 20);
	 * var notPrintedField = form.newField('parent_table_id', JSField.TEXT_FIELD, 10, 40, 100, 20);
	 * notPrintedField.printable = false; // This field won't show up in print preview and won't be printed.
	 * forms['printForm'].controller.showPrintPreview()
	 */
	public boolean js_getPrintable()
	{
		return getBaseComponent(false).getPrintable();
	}

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
	public int js_getWidth()
	{
		return getBaseComponent(false).getSize().width;
	}

	/**
	 * The height in pixels of the component.
	 * 
	 * @sampleas js_getWidth()
	 */
	public int js_getHeight()
	{
		return getBaseComponent(false).getSize().height;
	}

	/**
	 * A String representing a group ID for this component. If several
	 * components have the same group ID then they belong to the same
	 * group of components. Using the group itself, all components can
	 * be disabled/enabled or made invisible/visible.
	 * The group id should be a javascript compatible identifier to allow access of the group in scripting.
	 *
	 * @sample 
	 * var form = solutionModel.newForm('someForm', 'example_data', 'parent_table', 'null', false, 400, 300);
	 * var label = form.newLabel('Green', 10, 10, 100, 20);
	 * var field = form.newField('parent_table_text', JSField.TEXT_FIELD, 10, 40, 100, 20);
	 * label.groupID = 'someGroup';
	 * field.groupID = 'someGroup';	
	 * forms['someForm'].elements.someGroup.enabled = false;
	 */
	public String js_getGroupID()
	{
		return getBaseComponent(false).getGroupID();
	}

	public void js_setAnchors(int arg)
	{
		int anchors = arg;
		// if default is set just reset it really back to 0 so that default is always used.
		if (arg == IAnchorConstants.DEFAULT)
		{
			anchors = 0;
		}
		getBaseComponent(true).setAnchors(anchors);
	}

	public void js_setX(int x)
	{
		getBaseComponent(true).setLocation(new Point(x, getBaseComponent(true).getLocation().y));
	}

	public void js_setY(int y)
	{
		getBaseComponent(true).setLocation(new Point(getBaseComponent(true).getLocation().x, y));
	}

	public void js_setName(String arg)
	{
		getBaseComponent(true).setName(arg);
	}

	public void js_setPrintable(boolean arg)
	{
		getBaseComponent(true).setPrintable(arg);
	}

	public void js_setWidth(int width)
	{
		getBaseComponent(true).setSize(new Dimension(width, getBaseComponent(true).getSize().height));
	}

	public void js_setHeight(int height)
	{
		getBaseComponent(true).setSize(new Dimension(getBaseComponent(true).getSize().width, height));
	}

	public void js_setGroupID(String arg)
	{
		getBaseComponent(true).setGroupID(arg);
	}
}
