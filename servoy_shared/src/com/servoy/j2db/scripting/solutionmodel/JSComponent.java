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
import java.util.Map;

import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.IAnchorConstants;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.StaticContentSpecLoader.TypedProperty;
import com.servoy.j2db.scripting.IJavaScriptType;
import com.servoy.j2db.solutionmodel.ISMComponent;
import com.servoy.j2db.solutionmodel.ISMHasDesignTimeProperty;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, scriptingName = "JSComponent")
public class JSComponent<T extends BaseComponent> extends JSBase<T> implements IJavaScriptType, ISMComponent, ISMHasDesignTimeProperty
{

	protected JSComponent(IJSParent< ? > parent, T baseComponent, boolean isNew)
	{
		super(parent, baseComponent, isNew);
	}

	/**
	 * Set the event handler for the method key, JSMethod may contain arguments.
	 */
	protected void setEventHandler(IApplication application, TypedProperty<Integer> methodProperty, JSMethod method)
	{
		JSForm.setEventHandler(application, getBaseComponent(true), methodProperty, method);
	}

	/**
	 * Get the event handler for the method key, JSMethod may contain arguments.
	 */
	protected JSMethod getEventHandler(IApplication application, TypedProperty<Integer> methodProperty)
	{
		return JSForm.getEventHandler(application, getBaseComponent(false), methodProperty, getJSParent());
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
	@JSGetter
	public String getBackground()
	{
		return PersistHelper.createColorString(getBaseComponent(false).getBackground());
	}

	@JSSetter
	public void setBackground(String arg)
	{
		getBaseComponent(true).setBackground(PersistHelper.createColor(arg));
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.BaseComponent#getBorderType()
	 *
	 * @sample
	 * //HINT: To know exactly the notation of this property set it in the designer and then read it once out through the solution model.
	 * var field = form.newField('my_table_text', JSField.TEXT_FIELD, 10, 10, 100, 20);
	 * field.borderType = solutionModel.createLineBorder(1,'#ff0000');
	 */
	@JSGetter
	public String getBorderType()
	{
		return getBaseComponent(false).getBorderType();
	}

	@JSSetter
	public void setBorderType(String arg)
	{
		getBaseComponent(true).setBorderType(arg);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.BaseComponent#getFontType()
	 *
	 * @sample
	 * var label = form.newLabel('Text here', 10, 50, 100, 20);
	 * label.fontType = solutionModel.createFont('Times New Roman',1,14);
	 */
	@JSGetter
	public String getFontType()
	{
		return getBaseComponent(false).getFontType();
	}

	@JSSetter
	public void setFontType(String arg)
	{
		getBaseComponent(true).setFontType(arg);
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
	@JSGetter
	public String getForeground()
	{
		return PersistHelper.createColorString(getBaseComponent(false).getForeground());
	}

	@JSSetter
	public void setForeground(String arg)
	{
		getBaseComponent(true).setForeground(PersistHelper.createColor(arg));
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
	@JSGetter
	public int getPrintSliding()
	{
		return getBaseComponent(false).getPrintSliding();
	}

	@JSSetter
	public void setPrintSliding(int i)
	{
		getBaseComponent(true).setPrintSliding(i);
	}

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
	@JSGetter
	public String getStyleClass()
	{
		return getBaseComponent(false).getStyleClass();
	}

	@JSSetter
	public void setStyleClass(String arg)
	{
		getBaseComponent(true).setStyleClass(arg);
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
	@JSGetter
	public boolean getTransparent()
	{
		return getBaseComponent(false).getTransparent();
	}

	@JSSetter
	public void setTransparent(boolean arg)
	{
		getBaseComponent(true).setTransparent(arg);
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
	@JSGetter
	public int getAnchors()
	{
		int anchors = getBaseComponent(false).getAnchors();
		if (anchors <= 0) return IAnchorConstants.DEFAULT;
		return anchors;
	}

	@JSSetter
	public void setAnchors(int arg)
	{
		int anchors = arg;
		// if default is set just reset it really back to 0 so that default is always used.
		if (arg == IAnchorConstants.DEFAULT)
		{
			anchors = 0;
		}
		getBaseComponent(true).setAnchors(anchors);
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
	@JSGetter
	public int getFormIndex()
	{
		return getBaseComponent(false).getFormIndex();
	}

	@JSSetter
	public void setFormIndex(int arg)
	{
		getBaseComponent(true).setFormIndex(arg);
	}

	/**
	 * @sameas com.servoy.j2db.solutionmodel.ISMPortal#setX(int)
	 */
	@JSGetter
	public int getX()
	{
		return getBaseComponent(false).getLocation().x;
	}

	@JSSetter
	public void setX(int x)
	{
		getBaseComponent(true).setLocation(new Point(x, getBaseComponent(true).getLocation().y));
	}

	/**
	 * @clonedesc com.servoy.j2db.solutionmodel.ISMPortal#setY(int)
	 *
	 * @sampleas getX()
	 */
	@JSGetter
	public int getY()
	{
		return getBaseComponent(false).getLocation().y;
	}

	@JSSetter
	public void setY(int y)
	{
		getBaseComponent(true).setLocation(new Point(getBaseComponent(true).getLocation().x, y));
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
	@JSGetter
	public String getName()
	{
		return getBaseComponent(false).getName();
	}

	@JSSetter
	public void setName(String arg)
	{
		getBaseComponent(true).setName(arg);
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
	@JSGetter
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public boolean getPrintable()
	{
		return getBaseComponent(false).getPrintable();
	}

	@JSSetter
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public void setPrintable(boolean arg)
	{
		getBaseComponent(true).setPrintable(arg);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.BaseComponent#getEnabled()
	 *
	 * @sample
	 * var form = solutionModel.newForm('printForm', 'db:/example_data/parent_table', null, false, 400, 300);
	 * var field = form.newField('parent_table_text', JSField.TEXT_FIELD, 10, 10, 100, 20);
	 * field.enabled = false;
	 */
	@JSGetter
	public boolean getEnabled()
	{
		return getBaseComponent(false).getEnabled();
	}

	@JSSetter
	public void setEnabled(boolean arg)
	{
		getBaseComponent(true).setEnabled(arg);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.BaseComponent#getVisible()
	 *
	 * @sample
	 * var form = solutionModel.newForm('printForm', 'db:/example_data/parent_table', null, false, 400, 300);
	 * var field = form.newField('parent_table_text', JSField.TEXT_FIELD, 10, 10, 100, 20);
	 * field.visible = false;
	 */
	@JSGetter
	public boolean getVisible()
	{
		return getBaseComponent(false).getVisible();
	}

	@JSSetter
	public void setVisible(boolean arg)
	{
		getBaseComponent(true).setVisible(arg);
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
	@JSGetter
	public int getWidth()
	{
		return getBaseComponent(false).getSize().width;
	}

	@JSSetter
	public void setWidth(int width)
	{
		getBaseComponent(true).setSize(new Dimension(width, getBaseComponent(true).getSize().height));
	}

	/**
	 * The height in pixels of the component.
	 *
	 * @sampleas getWidth()
	 */
	@JSGetter
	public int getHeight()
	{
		return getBaseComponent(false).getSize().height;
	}

	@JSSetter
	public void setHeight(int height)
	{
		getBaseComponent(true).setSize(new Dimension(getBaseComponent(true).getSize().width, height));
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
	@JSGetter
	public String getGroupID()
	{
		return getBaseComponent(false).getGroupID();
	}

	@JSSetter
	public void setGroupID(String arg)
	{
		getBaseComponent(true).setGroupID(arg);
	}


	/**
	 *
	 * Get a design-time property of an element.
	 *
	 * @param key the name of the property
	 *
	 * @sample
	 * var frm = solutionModel.getForm('orders')
	 * var fld = frm.getField('fld')
	 * var prop = fld.getDesignTimeProperty('myprop')
	 */
	@JSFunction
	public Object getDesignTimeProperty(String key)
	{
		return Utils.parseJSExpression(getBaseComponent(false).getCustomDesignTimeProperty(key));
	}

	/** Set a design-time property of an element.
	 *
	 * @param key the name of the property
	 * @param value the value to store
	 *
	 * @sample
	 * var frm = solutionModel.getForm('orders')
	 * var fld = frm.getField('fld')
	 * fld.putDesignTimeProperty('myprop', 'strawberry')
	 */
	@JSFunction
	public Object putDesignTimeProperty(String key, Object value)
	{
		return Utils.parseJSExpression(getBaseComponent(true).putCustomDesignTimeProperty(key, Utils.makeJSExpression(value)));
	}

	/** Clear a design-time property of an element.
	 *
	 * @param key the name of the property
	 *
	 * @sample
	 * var frm = solutionModel.getForm('orders')
	 * var fld = frm.getField('fld')
	 * fld.removeDesignTimeProperty('myprop')
	 */
	@JSFunction
	public Object removeDesignTimeProperty(String key)
	{
		return Utils.parseJSExpression(getBaseComponent(true).clearCustomDesignTimeProperty(key));
	}

	/** Get the design-time properties of an element.
	 *
	 * @sample
	 * var frm = solutionModel.getForm('orders')
	 * var fld = frm.getField('fld')
	 * var propNames = fld.getDesignTimePropertyNames()
	 */
	@JSFunction
	public String[] getDesignTimePropertyNames()
	{
		Map<String, Object> propsMap = getBaseComponent(false).getCustomDesignTimeProperties();
		String[] designTimePropertyNames = new String[0];
		if (propsMap != null)
		{
			designTimePropertyNames = propsMap.keySet().toArray(new String[propsMap.size()]);
		}
		return designTimePropertyNames;
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

	/**
	 * Returns the name of the form. (may be empty string as well)
	 *
	 *
	 * @sample
	 * var name = %%prefix%%%%elementName%%.getFormName();
	 *
	 * @return The name of the form.
	 */
	@JSFunction
	public String getFormName()
	{
		IJSParent< ? > parent = getJSParent();
		while (!(parent instanceof JSForm))
		{
			parent = parent.getJSParent();
		}
		return ((JSForm)parent).getName();
	}
}
