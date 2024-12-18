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

import java.util.ArrayList;
import java.util.List;

import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

import com.servoy.base.persistence.IMobileProperties;
import com.servoy.base.persistence.constants.IPartConstants;
import com.servoy.base.solutionmodel.IBaseSMMethod;
import com.servoy.base.solutionmodel.IBaseSMVariable;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.solutionmodel.ISMFooter;

/**
 * The <code>JSFooter</code> class represents the footer section of a form in a solution model.
 * It provides functionality to manage and manipulate components specifically placed within the footer,
 * enabling dynamic customization and behavior. The class extends <code>JSPart</code> and implements <code>ISMFooter</code>,
 * integrating seamlessly into the solution model's architecture.
 *
 * ## Features and Functionalities
 * This class supports the creation of various UI components for the footer, including fields, text areas,
 * comboboxes, buttons, and labels. Each component can be dynamically added based on data provider references
 * and positioned horizontally to define their order. For example:
 * 1. Components such as fields or text areas can be created with display types like text fields or comboboxes.
 * 2. Buttons and labels can be defined with textual content and event handlers for interactive functionality.
 *
 * The <code>JSFooter</code> allows for sticky behavior, where the footer remains fixed and does not scroll out of view,
 * controlled by the <code>sticky</code> property. It also provides a method to retrieve all components belonging to the footer,
 * filtering based on their custom properties.
 *
 * Components within the footer can be removed by name, ensuring modularity and allowing developers to dynamically manage
 * the footer layout.
 *
 * @author rgansevles
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class JSFooter extends JSPart implements ISMFooter
{
	JSFooter(JSForm form, Part part, boolean isNew)
	{
		super(form, part, isNew);
	}

	@Override
	public JSForm getJSParent()
	{
		return (com.servoy.j2db.scripting.solutionmodel.JSForm)super.getJSParent();
	}

	/**
	 * Flag to set a set the footer sticky so it will not scroll out of view.
	 *
	 * @sample
	 * var form = solutionModel.newForm('newForm1', myDatasource);
	 * var footer = form.newFooter()
	 * footer.sticky = false // default: true
	 */
	@Override
	@JSGetter
	public boolean getSticky()
	{
		return getBaseComponent(false).getPartType() == IPartConstants.TITLE_FOOTER;
	}

	@Override
	@JSSetter
	public void setSticky(boolean sticky)
	{
		getBaseComponent(true).setPartType(sticky ? TITLE_FOOTER : FOOTER);
	}

	/**
	 * Creates a new JSField object on the footer.
	 *
	 * @sample
	 * var form = solutionModel.getForm('myform')
	 * var footer = form.getForm('myform').newFooter()
	 * footer.newField('myvar', JSField.TEXT_FIELD, 1);
	 * forms['newForm1'].controller.show();
	 *
	 * @param dataprovider the specified dataprovider name/JSVariable of the JSField object
	 *
	 * @param type the display type of the JSField object (see the Solution Model -> JSField node for display types)
	 *
	 * @param x the horizontal "x" position of the new element, defines the order of elements on the footer
	 *
	 * @return a new JSField object (of the specified display type)
	 *
	 * @deprecated Possible future api
	 */
	@Deprecated
	@JSFunction
	@Override
	public JSField newField(IBaseSMVariable dataprovider, int type, int x)
	{
		return newField(((JSVariable)dataprovider).getScriptVariable().getDataProviderID(), type, x);
	}

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSFooter#newField(IBaseSMVariable,int,int)
	 *
	 * @deprecated Possible future api
	 */
	@Deprecated
	@JSFunction
	@Override
	public JSField newField(String dataprovider, int type, int x)
	{
		return markForFooter(getJSParent().newField(dataprovider, type, x, 0, 10, 10));
	}

	/**
	 * Creates a new JSText object on the footer.
	 *
	 * @sample
	 * var form = solutionModel.getForm('myform')
	 * var footer = form.getForm('myform').newFooter()
	 * footer.newTextField('myvar', 1);
	 * forms['newForm1'].controller.show();
	 *
	 * @param dataprovider the specified dataprovider name/JSVariable of the JSField object
	 *
	 * @param x the horizontal "x" position of the new element, defines the order of elements on the footer
	 *
	 * @return a new JSText element
	 *
	 * @deprecated Possible future api
	 */
	@Deprecated
	@JSFunction
	@Override
	public JSText newTextField(IBaseSMVariable dataprovider, int x)
	{
		return (JSText)newField(dataprovider, Field.TEXT_FIELD, x);
	}

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSFooter#newTextField(IBaseSMVariable,int)
	 *
	 * @deprecated Possible future api
	 */
	@Deprecated
	@JSFunction
	@Override
	public JSText newTextField(String dataprovider, int x)
	{
		return (JSText)newField(dataprovider, Field.TEXT_FIELD, x);
	}

	/**
	 * Creates a new JSTextArea object on the footer.
	 *
	 * @sample
	 * var form = solutionModel.getForm('myform')
	 * var footer = form.getForm('myform').newFooter()
	 * footer.newTextArea('myvar', 1);
	 * forms['newForm1'].controller.show();
	 *
	 * @param dataprovider the specified dataprovider name/JSVariable of the JSField object
	 *
	 * @param x the horizontal "x" position of the new element, defines the order of elements on the footer
	 *
	 * @return a new JSTextArea element
	 *
	 * @deprecated Possible future api
	 */
	@Deprecated
	@JSFunction
	@Override
	public JSTextArea newTextArea(IBaseSMVariable dataprovider, int x)
	{
		return (JSTextArea)newField(dataprovider, Field.TEXT_AREA, x);
	}

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSFooter#newTextArea(IBaseSMVariable,int)
	 *
	 * @deprecated Possible future api
	 */
	@Deprecated
	@JSFunction
	@Override
	public JSTextArea newTextArea(String dataprovider, int x)
	{
		return (JSTextArea)newField(dataprovider, Field.TEXT_AREA, x);
	}

	/**
	 * Creates a new JSCombobox object on the footer.
	 *
	 * @sample
	 * var form = solutionModel.getForm('myform')
	 * var footer = form.getForm('myform').newFooter()
	 * footer.newCombobox('myvar', 1);
	 * forms['newForm1'].controller.show();
	 *
	 * @param dataprovider the specified dataprovider name/JSVariable of the JSField object
	 *
	 * @param x the horizontal "x" position of the new element, defines the order of elements on the footer
	 *
	 * @return a new JSCombobox element
	 *
	 * @deprecated Possible future api
	 */
	@Deprecated
	@JSFunction
	@Override
	public JSCombobox newCombobox(IBaseSMVariable dataprovider, int x)
	{
		return (JSCombobox)newField(dataprovider, Field.COMBOBOX, x);
	}

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSFooter#newCombobox(IBaseSMVariable,int)
	 *
	 * @deprecated Possible future api
	 */
	@Deprecated
	@JSFunction
	@Override
	public JSCombobox newCombobox(String dataprovider, int x)
	{
		return (JSCombobox)newField(dataprovider, Field.COMBOBOX, x);
	}

	/**
	 * Creates a new JSRadios object on the footer.
	 *
	 * @sample
	 * var form = solutionModel.getForm('myform')
	 * var footer = form.getForm('myform').newFooter()
	 * footer.newRadios('myvar', 1);
	 * forms['newForm1'].controller.show();
	 *
	 * @param dataprovider the specified dataprovider name/JSVariable of the JSField object
	 *
	 * @param x the horizontal "x" position of the new element, defines the order of elements on the footer
	 *
	 * @return a new JSRadios element
	 *
	 * @deprecated Possible future api
	 */
	@Deprecated
	@JSFunction
	@Override
	public JSRadios newRadios(IBaseSMVariable dataprovider, int x)
	{
		return (JSRadios)newField(dataprovider, Field.RADIOS, x);
	}

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSFooter#newRadios(IBaseSMVariable,int)
	 *
	 * @deprecated Possible future api
	 */
	@Deprecated
	@JSFunction
	@Override
	public JSRadios newRadios(String dataprovider, int x)
	{
		return (JSRadios)newField(dataprovider, Field.RADIOS, x);
	}

	/**
	 * Creates a new JSChecks object on the footer.
	 *
	 * @sample
	 * var form = solutionModel.getForm('myform')
	 * var footer = form.getForm('myform').newFooter()
	 * footer.newCheck('myvar', 1);
	 * forms['newForm1'].controller.show();
	 *
	 * @param dataprovider the specified dataprovider name/JSVariable of the JSField object
	 *
	 * @param x the horizontal "x" position of the new element, defines the order of elements on the footer
	 *
	 * @return a new JSChecks element
	 *
	 * @deprecated Possible future api
	 */
	@Deprecated
	@JSFunction
	@Override
	public JSChecks newCheck(IBaseSMVariable dataprovider, int x)
	{
		return (JSChecks)newField(dataprovider, Field.CHECKS, x);
	}

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSFooter#newCheck(IBaseSMVariable,int)
	 *
	 * @deprecated Possible future api
	 */
	@Deprecated
	@JSFunction
	@Override
	public JSChecks newCheck(String dataprovider, int x)
	{
		return (JSChecks)newField(dataprovider, Field.CHECKS, x);
	}

	/**
	 * Creates a new JSPassword object on the footer.
	 *
	 * @sample
	 * var form = solutionModel.getForm('myform')
	 * var footer = form.getForm('myform').newFooter()
	 * footer.newPassword('myvar', 1);
	 * forms['newForm1'].controller.show();
	 *
	 * @param dataprovider the specified dataprovider name/JSVariable of the JSField object
	 *
	 * @param x the horizontal "x" position of the new element, defines the order of elements on the footer
	 *
	 * @return a new JSPassword element
	 *
	 * @deprecated Possible future api
	 */
	@Deprecated
	@JSFunction
	@Override
	public JSPassword newPassword(IBaseSMVariable dataprovider, int x)
	{
		return (JSPassword)newField(dataprovider, Field.PASSWORD, x);
	}

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSFooter#newPassword(IBaseSMVariable,int)
	 *
	 * @deprecated Possible future api
	 */
	@Deprecated
	@JSFunction
	@Override
	public JSPassword newPassword(String dataprovider, int x)
	{
		return (JSPassword)newField(dataprovider, Field.PASSWORD, x);
	}

	/**
	 * Creates a new JSCalendar object on the footer.
	 *
	 * @sample
	 * var form = solutionModel.getForm('myform')
	 * var footer = form.getForm('myform').newFooter()
	 * footer.newCalendar('myvar', 1);
	 * forms['newForm1'].controller.show();
	 *
	 * @param dataprovider the specified dataprovider name/JSVariable of the JSField object
	 *
	 * @param x the horizontal "x" position of the new element, defines the order of elements on the footer
	 *
	 * @return a new JSCalendar element
	 *
	 * @deprecated Possible future api
	 */
	@Deprecated
	@JSFunction
	@Override
	public JSCalendar newCalendar(IBaseSMVariable dataprovider, int x)
	{
		return (JSCalendar)newField(dataprovider, Field.CALENDAR, x);
	}

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSFooter#newCalendar(IBaseSMVariable,int)
	 *
	 * @deprecated Possible future api
	 */
	@Deprecated
	@JSFunction
	@Override
	public JSCalendar newCalendar(String dataprovider, int x)
	{
		return (JSCalendar)newField(dataprovider, Field.CALENDAR, x);
	}

	/**
	 * Creates a new button on the footer.
	 *
	 * @sample
	 * var form = solutionModel.getForm('myform')
	 * var footer = form.getForm('myform').newFooter()
	 * footer.newButton('myvar', form.getMethod('doit'));
	 * forms['newForm1'].controller.show();
	 *
	 * @param text the text on the button
	 *
	 * @param x the horizontal "x" position of the new element, defines the order of elements on the footer
	 *
	 * @param jsmethod the method assigned to handle an onAction event
	 *
	 * @return a new JSCalendar element
	 */
	@JSFunction
	@Override
	public JSButton newButton(String text, int x, IBaseSMMethod jsmethod)
	{
		return markForFooter(getJSParent().newButton(text, x, 0, 10, 10, jsmethod));
	}

	/**
	 * Creates a new label on the footer.
	 *
	 * @sample
	 * var form = solutionModel.getForm('myform')
	 * var footer = form.getForm('myform').newFooter()
	 * footer.newButton('myvar', form.getMethod('doit'));
	 * forms['newForm1'].controller.show();
	 *
	 * @param text the text on the label
	 *
	 * @param x the horizontal "x" position of the new element, defines the order of elements on the footer
	 *
	 * @return a new JSCalendar element
	 *
	 * @deprecated Possible future api
	 */
	@Deprecated
	@JSFunction
	@Override
	public JSLabel newLabel(String text, int x)
	{
		return markForFooter(getJSParent().newLabel(text, x, 0, 10, 10));
	}

	/**
	 * Returns a array of all the IBaseSMComponents that a footer has.
	 *
	 * @sample
	 * var form = solutionModel.getForm("myForm");
	 * var footer = form.getFooter();
	 * var components = footer.getComponents();
	 * for (var i in components)
	 * 	application.output("Component type and name: " + components[i]);
	 *
	 * @return an array of all the JSComponents on the footer.
	 */
	@JSFunction
	@Override
	public JSComponent[] getComponents()
	{
		List<JSComponent< ? >> footerComponents = new ArrayList<JSComponent< ? >>();
		for (JSComponent< ? > comp : getJSParent().getComponents())
		{
			if (Boolean.TRUE.equals(comp.getBaseComponent(false).getCustomMobileProperty(IMobileProperties.FOOTER_ITEM.propertyName)))
			{
				footerComponents.add(comp);
			}
		}
		return footerComponents.toArray(new JSComponent[footerComponents.size()]);
	}

	/**
	 * Removes a named component from the footer.
	 *
	 * @sample
	 * var form = solutionModel.getForm("myForm");
	 * var footer = form.getFooter();
	 * footer.removeComponent('myfield1')
	 *
	 * @param name the specified name of the component to remove
	 *
	 * @return true is the component has been successfully removed; false otherwise
	 */
	@JSFunction
	@Override
	public boolean removeComponent(String name)
	{
		JSForm form = getJSParent();
		for (JSComponent< ? > comp : form.getComponents())
		{
			if (name.equals(comp.getName()) &&
				Boolean.TRUE.equals(comp.getBaseComponent(false).getCustomMobileProperty(IMobileProperties.FOOTER_ITEM.propertyName)))
			{
				return form.removeComponent(name);
			}
		}

		return false;
	}

	private <T extends JSBase< ? >> T markForFooter(T comp)
	{
		comp.getBaseComponent(true).putCustomMobileProperty(IMobileProperties.FOOTER_ITEM.propertyName, Boolean.TRUE);
		return comp;
	}
}
