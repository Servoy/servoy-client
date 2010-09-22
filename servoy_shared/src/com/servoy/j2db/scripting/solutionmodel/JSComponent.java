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

import com.servoy.j2db.IApplication;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.scripting.IJavaScriptType;
import com.servoy.j2db.util.PersistHelper;

/**
 * @author jcompagner
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class JSComponent<T extends BaseComponent> extends JSBaseComponent<T> implements IJavaScriptType
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
	 * field.borderType = 'LineBorder,2,#FF0000';
	 */
	public String js_getBorderType()
	{
		return getBaseComponent(false).getBorderType();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.BaseComponent#getFontType()
	 * 
	 * @sample
	 * //HINT: To know exactly the notation of this property set it in the designer and then read it once out through the solution model.
	 * var label = form.newLabel('Text here', 10, 50, 100, 20);
	 * label.fontType = 'Times New Roman,1,14';
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
	 * var form = solutionModel.newForm('printForm', 'example_data', 'parent_table', 'null', false, 400, 300);
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
	 * var form = solutionModel.newForm('printForm', 'example_data', 'parent_table', 'null', false, 400, 300);
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

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return getClass().getName() + ": " + getBaseComponent(false).getName(); //$NON-NLS-1$
	}
}
