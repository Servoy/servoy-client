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

import com.servoy.j2db.IApplication;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.scripting.IJavaScriptType;
import com.servoy.j2db.util.IAnchorConstants;
import com.servoy.j2db.util.PersistHelper;

/**
 * @author jcompagner
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class JSComponent<T extends BaseComponent> extends JSBase<T> implements IJavaScriptType
{

	protected JSComponent(IJSParent parent, T baseComponent, boolean isNew)
	{
		super(parent, baseComponent, isNew);
	}

	/**
	 * Set the event handler for the method key, JSMethod may contain arguments.
	 */
	protected void setEventHandler(IApplication application, String methodKey, JSMethod method)
	{
		JSForm.setEventHandler(application, getBaseComponent(true), methodKey, method);
	}

	/**
	 * Get the event handler for the method key, JSMethod may contain arguments.
	 */
	protected JSMethod getEventHandler(IApplication application, String methodKey)
	{
		return JSForm.getEventHandler(application, getBaseComponent(false), methodKey, getJSParent());
	}


	/**
	 * @clonedesc com.servoy.j2db.persistence.BaseComponent#getBackground()
	 * 
	 * @sample
	 * // This property can be used on all types of components.
	 * // Here it is illustrated only for labels and fields.
	 * var greenLabel = form.newLabel('Green',10,10,100,50);
	 * greenLabel.background = 'green'; // Use generic names for colors.	
	 * var redField = form.newField('parent_table_text',JSField.TEXT_FIELD,10,110,100,30);
	 * redField.background = '#FF0000'; // Use RGB codes for colors.
	 */
	public String js_getBackground()
	{
		return PersistHelper.createColorString(getBaseComponent(false).getBackground());
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.BaseComponent#getBorderType()
	 * 
	 * @sample
	 * //HINT: To know exactly the notation of this property set it in the designer and then read it once out through the solution model.
	 * var field = form.newField('my_table_text', JSField.TEXT_FIELD, 10, 10, 100, 20);
	 * field.borderType = solutionModel.createBorder(1,'#ff0000');;
	 */
	public String js_getBorderType()
	{
		return getBaseComponent(false).getBorderType();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.BaseComponent#getFontType()
	 * 
	 * @sample
	 * var label = form.newLabel('Text here', 10, 50, 100, 20);
	 * label.fontType = solutionModel.createFont('Times New Roman',1,14);
	 */
	public String js_getFontType()
	{
		return getBaseComponent(false).getFontType();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.BaseComponent#getForeground()
	 * 
	 * @sample 
	 * // This property can be used on all types of components.
	 * // Here it is illustrated only for labels and fields.
	 * var labelWithBlueText = form.newLabel('Blue text', 10, 10, 100, 30);
	 * labelWithBlueText.foreground = 'blue'; // Use generic names for colors.
	 * var fieldWithYellowText = form.newField('parent_table_text', JSField.TEXT_FIELD, 10, 50, 100, 20);
	 * fieldWithYellowText.foreground = '#FFFF00'; // Use RGB codes for colors.
	 */
	public String js_getForeground()
	{
		return PersistHelper.createColorString(getBaseComponent(false).getForeground());
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.BaseComponent#getPrintSliding()
	 * 
	 * @sample
	 * var form = solutionModel.newForm('printForm', 'db:/example_data/parent_table', null, false, 400, 300);
	 * var slidingLabel = form.newLabel('Some long text here', 10, 10, 5, 5);
	 * slidingLabel.printSliding = SM_PRINT_SLIDING.GROW_HEIGHT | SM_PRINT_SLIDING.GROW_WIDTH;
	 * slidingLabel.background = 'gray';
	 * forms['printForm'].controller.showPrintPreview();
	 */
	public int js_getPrintSliding()
	{
		return getBaseComponent(false).getPrintSliding();
	}


	/**
	 * @clonedesc com.servoy.j2db.persistence.BaseComponent#getStyleClass()
	 * 
	 * @sample
	 * var form = solutionModel.newForm('printForm', 'db:/example_data/parent_table', null, false, 400, 300);
	 * var field = form.newField('parent_table_text', JSField.TEXT_FIELD, 10, 10, 100, 20);
	 * var style = solutionModel.newStyle('myStyle','field.fancy { background-color: yellow; }');
	 * form.styleName = 'myStyle'; // First set the style on the form.
	 * field.styleClass = 'fancy'; // Then set the style class on the field.
	 */
	public String js_getStyleClass()
	{
		return getBaseComponent(false).getStyleClass();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.BaseComponent#getTransparent()
	 * 
	 * @sample
	 * // Load an image from disk an create a Media object based on it.
	 * var imageBytes = plugins.file.readFile('d:/ball.jpg');
	 * var media = solutionModel.newMedia('ball.jpg', imageBytes);
	 * // Put on the form a label with the image.
	 * var image = form.newLabel('', 10, 10, 100, 100);
	 * image.imageMedia = media;
	 * // Put two fields over the image. The second one will be transparent and the
	 * // image will shine through.
	 * var nonTransparentField = form.newField('parent_table_text', JSField.TEXT_FIELD, 10, 20, 100, 20);
	 * var transparentField = form.newField('parent_table_text', JSField.TEXT_FIELD, 10, 50, 100, 20);
	 * transparentField.transparent = true;
	 */
	public boolean js_getTransparent()
	{
		return getBaseComponent(false).getTransparent();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.BaseComponent#getAnchors()
	 * 
	 * @sample
	 * var form = solutionModel.newForm('mediaForm', 'db:/example_data/parent_table', null, false, 400, 300);
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
	 * var form = solutionModel.newForm('someForm', 'db:/example_data/parent_table', null, false, 620, 300);
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
	 * var form = solutionModel.newForm('printForm', 'db:/example_data/parent_table', null, false, 400, 300);
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
	 * @clonedesc com.servoy.j2db.persistence.BaseComponent#getEnabled()
	 * 
	 * @sample
	 * var form = solutionModel.newForm('printForm', 'db:/example_data/parent_table', null, false, 400, 300);
	 * var field = form.newField('parent_table_text', JSField.TEXT_FIELD, 10, 10, 100, 20);
	 * field.enabled = false;
	 */
	public boolean js_getEnabled()
	{
		return getBaseComponent(false).getEnabled();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.BaseComponent#getVisible()
	 * 
	 * @sample
	 * var form = solutionModel.newForm('printForm', 'db:/example_data/parent_table', null, false, 400, 300);
	 * var field = form.newField('parent_table_text', JSField.TEXT_FIELD, 10, 10, 100, 20);
	 * field.visible = false;
	 */
	public boolean js_getVisible()
	{
		return getBaseComponent(false).getVisible();
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
	 * var form = solutionModel.newForm('someForm', 'db:/example_data/parent_table', null, false, 400, 300);
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

	public void js_setBackground(String arg)
	{
		getBaseComponent(true).setBackground(PersistHelper.createColor(arg));
	}

	public void js_setBorderType(String arg)
	{
		getBaseComponent(true).setBorderType(arg);
	}

	public void js_setFontType(String arg)
	{
		getBaseComponent(true).setFontType(arg);
	}


	public void js_setForeground(String arg)
	{
		getBaseComponent(true).setForeground(PersistHelper.createColor(arg));
	}

	public void js_setPrintSliding(int i)
	{
		getBaseComponent(true).setPrintSliding(i);
	}

	public void js_setStyleClass(String arg)
	{
		getBaseComponent(true).setStyleClass(arg);
	}

	public void js_setTransparent(boolean arg)
	{
		getBaseComponent(true).setTransparent(arg);
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

	public void js_setEnabled(boolean arg)
	{
		getBaseComponent(true).setEnabled(arg);
	}

	public void js_setVisible(boolean arg)
	{
		getBaseComponent(true).setVisible(arg);
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

	/**
	 * @see java.lang.Object#toString()
	 */
	@SuppressWarnings("nls")
	@Override
	public String toString()
	{
		T comp = getBaseComponent(false);
		Point loc = comp.getLocation();
		Dimension dim = comp.getSize();
		return getClass().getSimpleName() + "[name:" + comp.getName() + ",form:" + ((ISupportName)comp.getParent()).getName() + ",x:" + loc.x + ",y:" + loc.y +
			",width:" + dim.width + ",height:" + dim.height + ']';
	}
}
