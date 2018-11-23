/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

import java.util.Iterator;

import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

import com.servoy.base.persistence.IMobileProperties;
import com.servoy.base.persistence.IMobileProperties.MobileProperty;
import com.servoy.base.persistence.constants.IPartConstants;
import com.servoy.base.solutionmodel.IBaseSMMethod;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.solutionmodel.ISMHeader;
import com.servoy.j2db.util.Utils;

/**
 * Solution model header object on form.
 *
 * @author rgansevles
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class JSHeader extends JSPart implements ISMHeader
{
	JSHeader(JSForm form, Part part, boolean isNew)
	{
		super(form, part, isNew);
	}

	@Override
	public JSForm getJSParent()
	{
		return (JSForm)super.getJSParent();
	}

	/**
	 * Flag to set a set the header sticky so it will not scroll out of view.
	 *
	 * @sample
	 * var form = solutionModel.newForm('newForm1', myDatasource);
	 * var header = form.newHeader()
	 * header.sticky = false // default: true
	 */
	@Override
	@JSGetter
	public boolean getSticky()
	{
		return getBaseComponent(false).getPartType() == IPartConstants.TITLE_HEADER;
	}

	@Override
	@JSSetter
	public void setSticky(boolean sticky)
	{
		getBaseComponent(true).setPartType(sticky ? TITLE_HEADER : HEADER);
	}

	/**
	 * Creates a new left-button on the form header.
	 *
	 * @sample
	 * var form = solutionModel.newForm('newForm1', myDatasource);
	 * var header = form.newHeader()
	 * var button = header.newLeftButton('back', form.getMethod('goBack'))
	 *
	 * @param text the text on the button
	 *
	 * @param jsmethod the method assigned to handle an onAction event
	 *
	 * @return a new JSButton object
	 */
	@JSFunction
	@Override
	public JSButton newLeftButton(String text, IBaseSMMethod jsmethod)
	{
		return newButtonImpl(true, text, jsmethod);
	}

	/**
	 * Returns the left-button on the form header if present.
	 *
	 * @sample
	 * var form = solutionModel.getForm('myform');
	 * var header = form.getHeader()
	 * var button = header.getLeftButton()
	 * button.iconType = JSButton.ICON_HOME
	 *
	 * @return a JSButton object if the left-button exists, null otherwise
	 */
	@JSFunction
	@Override
	public JSButton getLeftButton()
	{
		return getButtonImpl(true);
	}

	/**
	 * Remove the left-button on the form header if present.
	 *
	 * @sample
	 * var form = solutionModel.getForm('myform');
	 * var header = form.getHeader()
	 * var removed = header.removeLeftButton()
	 *
	 * @return true if button existed and was removed
	 */
	@JSFunction
	@Override
	public boolean removeLeftButton()
	{
		return removeComponent(IMobileProperties.HEADER_LEFT_BUTTON);
	}

	/**
	 * Creates a new right-button on the form header.
	 *
	 * @sample
	 * var form = solutionModel.newForm('newForm1', myDatasource);
	 * var header = form.newHeader()
	 * var button = header.newRightButton('save', form.getMethod('doSave'))
	 *
	 * @param text the text on the button
	 *
	 * @param jsmethod the method assigned to handle an onAction event
	 *
	 * @return a new JSButton object
	 */
	@JSFunction
	@Override
	public JSButton newRightButton(String text, IBaseSMMethod jsmethod)
	{
		return newButtonImpl(false, text, jsmethod);
	}

	/**
	 * Returns the right-button on the form header if present.
	 *
	 * @sample
	 * var form = solutionModel.getForm('myform');
	 * var header = form.getHeader()
	 * var button = header.getRightButton()
	 * button.iconType = JSButton.ICON_GEARS
	 *
	 * @return a JSButton object if the right-button exists, null otherwise
	 */
	@JSFunction
	@Override
	public JSButton getRightButton()
	{
		return getButtonImpl(false);
	}

	/**
	 * Remove the right-button on the form header if present.
	 *
	 * @sample
	 * var form = solutionModel.getForm('myform');
	 * var header = form.getHeader()
	 * var removed = header.removeRightButton()
	 *
	 * @return true if button existed and was removed
	 */
	@JSFunction
	@Override
	public boolean removeRightButton()
	{
		return removeComponent(IMobileProperties.HEADER_RIGHT_BUTTON);
	}

	/**
	 * Creates a new header text label on the form header.
	 *
	 * @sample
	 * var form = solutionModel.newForm('newForm1', myDatasource);
	 * var header = form.newHeader()
	 * var title = header.newHeaderText('Contacts')
	 *
	 * @param text the text on the header
	 *
	 * @return a new JSTitle object
	 */
	@JSFunction
	@Override
	public JSTitle newHeaderText(String txt)
	{
		JSLabel label = getJSParent().newLabel(txt, 0, 0, 10, 10);
		label.getBaseComponent(true).putCustomMobileProperty(IMobileProperties.HEADER_ITEM.propertyName, Boolean.TRUE);
		label.getBaseComponent(true).putCustomMobileProperty(IMobileProperties.HEADER_TEXT.propertyName, Boolean.TRUE);
		return new JSTitle(getJSParent(), label.getBaseComponent(false), true);
	}

	/**
	 * Returns the header text label on the form header if present.
	 *
	 * @sample
	 * var form = solutionModel.getForm('myform');
	 * var header = form.getHeader()
	 * var title = header.getHeaderText()
	 * title.text = 'Customers'
	 *
	 * @return a JSTitle object if the header text exists, null otherwise
	 */
	@JSFunction
	@Override
	public JSTitle getHeaderText()
	{
		JSForm form = getJSParent();
		for (GraphicalComponent label : Utils.iterate(
			form.getApplication().getFlattenedSolution().getFlattenedForm(form.getSupportChild(), false).getGraphicalComponents()))
		{
			if (Boolean.TRUE.equals(label.getCustomMobileProperty(IMobileProperties.HEADER_TEXT.propertyName)))
			{
				return new JSTitle(getJSParent(), label, false);
			}
		}
		return null;
	}

	/**
	 * Remove the header text label on the form header if present.
	 *
	 * @sample
	 * var form = solutionModel.getForm('myform');
	 * var header = form.getHeader()
	 * var removed = header.removeHeaderText()
	 *
	 * @return true if header text existed and was removed
	 */
	@JSFunction
	@Override
	public boolean removeHeaderText()
	{
		return removeComponent(IMobileProperties.HEADER_TEXT);
	}

	private JSButton newButtonImpl(boolean left, String txt, Object method)
	{
		JSButton button = getJSParent().newButton(txt, 0, 0, 10, 10, method);
		button.getBaseComponent(true).putCustomMobileProperty(IMobileProperties.HEADER_ITEM.propertyName, Boolean.TRUE);
		button.getBaseComponent(true).putCustomMobileProperty(
			left ? IMobileProperties.HEADER_LEFT_BUTTON.propertyName : IMobileProperties.HEADER_RIGHT_BUTTON.propertyName, Boolean.TRUE);
		return button;
	}

	private JSButton getButtonImpl(boolean left)
	{
		JSForm form = getJSParent();
		for (GraphicalComponent button : Utils.iterate(
			form.getApplication().getFlattenedSolution().getFlattenedForm(form.getSupportChild(), false).getGraphicalComponents()))
		{
			if (Boolean.TRUE.equals(
				button.getCustomMobileProperty(left ? IMobileProperties.HEADER_LEFT_BUTTON.propertyName : IMobileProperties.HEADER_RIGHT_BUTTON.propertyName)))
			{
				return new JSButton(form, button, form.getApplication(), false);
			}
		}
		return null;
	}

	private boolean removeComponent(MobileProperty<Boolean> property)
	{
		JSForm form = getJSParent();
		form.checkModification();
		Iterator<GraphicalComponent> graphicalComponents = form.getSupportChild().getGraphicalComponents();
		while (graphicalComponents.hasNext())
		{
			GraphicalComponent button = graphicalComponents.next();
			if (Boolean.TRUE.equals(button.getCustomMobileProperty(property.propertyName)))
			{
				form.getSupportChild().removeChild(button);
				return true;
			}
		}
		return false;
	}
}
