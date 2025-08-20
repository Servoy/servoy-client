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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

import com.servoy.base.persistence.constants.IFieldConstants;
import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.base.solutionmodel.IBaseSMButton;
import com.servoy.base.solutionmodel.IBaseSMComponent;
import com.servoy.base.solutionmodel.IBaseSMField;
import com.servoy.base.solutionmodel.IBaseSMLabel;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.scripting.IJavaScriptType;
import com.servoy.j2db.solutionmodel.ISMMethod;
import com.servoy.j2db.solutionmodel.ISMPortal;
import com.servoy.j2db.util.Utils;


/**
 * @author jcompagner
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, extendsComponent = "JSComponent")
@ServoyClientSupport(ng = false, mc = false, wc = true, sc = true)
@Deprecated
public class JSPortal extends JSComponent<Portal> implements IJSParent<Portal>, IJavaScriptType, ISMPortal
{
	private final IApplication application;

	public JSPortal(IJSParent< ? > form, Portal portal, IApplication application, boolean isNew)
	{
		super(form, portal, isNew);
		this.application = application;
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.IJSParent#getBaseComponent()
	 */
	public Portal getSupportChild()
	{
		return getBaseComponent(false);
	}

	/**
	 * Creates a new field on this form. The type of the field is specified by
	 * using one of the JSField constants like JSField.TEXT_FIELD.
	 *
	 * @sample
	 * var childrenPortal = form.newPortal('pp', 'parent_to_my_table', 10, 10, 1180, 780);
	 *
	 * var vlist = solutionModel.newValueList('options', JSValueList.CUSTOM_VALUES);
	 * vlist.customValues = "one\ntwo\nthree\nfour";
	 *
	 * var cal = childrenPortal.newField('my_table_date', JSField.CALENDAR, 0, 60, 20);
	 * var chk = childrenPortal.newField('my_table_options', JSField.CHECKS, 60, 60, 50);
	 * chk.valuelist = vlist;
	 * var cmb = childrenPortal.newField('my_table_options', JSField.COMBOBOX, 120, 160, 20);
	 * cmb.valuelist = vlist;
	 * var html = childrenPortal.newField('my_table_html', JSField.HTML_AREA, 180, 60, 50);
	 * var img = childrenPortal.newField('my_table_image', JSField.IMAGE_MEDIA, 240, 60, 50);
	 * var pwd = childrenPortal.newField('my_table_text', JSField.PASSWORD, 300, 60, 20);
	 * var radio = childrenPortal.newField('my_table_options', JSField.RADIOS, 360, 60, 50);
	 * radio.valuelist = vlist;
	 * var rtf = childrenPortal.newField('my_table_rtf', JSField.RTF_AREA, 420, 60, 50);
	 * var tarea = childrenPortal.newField('my_table_text', JSField.TEXT_AREA, 480, 60, 50);
	 * var tfield = childrenPortal.newField('my_table_text', JSField.TEXT_FIELD, 540, 60, 20);
	 * var tahead = childrenPortal.newField('my_table_text', JSField.TYPE_AHEAD, 600, 60, 20);
	 * tahead.valuelist = vlist;
	 *
	 * @param dataprovider The data provider for this field. Can be either a column name, or an instance of JSVariable.
	 *
	 * @param displaytype The display type of the field. Use constants from JSField for this parameter.
	 *
	 * @param x The x coordinate of the field. If the portal does not have the "multiLine" property set, then the x coordinates are used only for determining the order of the columns in the grid. If the portal has the "multiLine" property set, then the components are actually displayed at the specified coordinates.
	 *
	 * @param width The width of the field.
	 *
	 * @param height The height of the field. In a portal the height of all components is set to the height of the first component, unless the "multiLine" property is set.
	 *
	 * @return A JSField instance that corresponds to the newly created field.
	 */
	@JSFunction
	public JSField newField(Object dataprovider, int displaytype, int x, int width, int height)
	{
		try
		{
			Field field = getBaseComponent(true).createNewField(new Point(getX() + x, getY()));
			field.setDisplayType(displaytype);
			field.setSize(new Dimension(width, height));
			if (dataprovider instanceof String)
			{
				field.setDataProviderID((String)dataprovider);
			}
			else if (dataprovider instanceof JSVariable)
			{
				field.setDataProviderID(((JSVariable)dataprovider).getName());
			}
			return JSField.createField(this, field, application, true);
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
	}

	public IBaseSMField newField(Object dataprovider, int type, int x, int y, int width, int height)
	{
		return newField(dataprovider, IFieldConstants.TEXT_FIELD, x, width, height);
	}

	/**
	 * Creates a new text field in the portal. It is equivalent to calling "newField"
	 * with the type JSField.TEXT_FIELD.
	 *
	 * @sample
	 * var childrenPortal = form.newPortal('pp', 'parent_to_my_table', 10, 10, 1180, 780);
	 * var tfield = childrenPortal.newTextField('my_table_text', 540, 60, 20);
	 *
	 * @param dataprovider The data provider for this field. Can be either a column name, or an instance of JSVariable.
	 *
	 * @param x The x coordinate of the field. If the portal does not have the "multiLine" property set, then the x coordinates are used only for determining the order of the columns in the grid. If the portal has the "multiLine" property set, then the components are actually displayed at the specified coordinates.
	 *
	 * @param width The width of the field.
	 *
	 * @param height The height of the field. In a portal the height of all components is set to the height of the first component, unless the "multiLine" property is set.
	 *
	 * @return A JSField instance that corresponds to the newly created text field.
	 */
	@JSFunction
	public JSField newTextField(Object dataprovider, int x, int width, int height)
	{
		return newField(dataprovider, Field.TEXT_FIELD, x, width, height);
	}

	/**
	 * Creates a new text area field in the portal. It is equivalent to calling "newField"
	 * with the type JSField.TEXT_AREA.
	 *
	 * @sample
	 * var childrenPortal = form.newPortal('pp', 'parent_to_my_table', 10, 10, 1180, 780);
	 * var tarea = childrenPortal.newTextArea('my_table_text', 480, 60, 50);
	 *
	 * @param dataprovider The data provider for this field. Can be either a column name, or an instance of JSVariable.
	 *
	 * @param x The x coordinate of the field. If the portal does not have the "multiLine" property set, then the x coordinates are used only for determining the order of the columns in the grid. If the portal has the "multiLine" property set, then the components are actually displayed at the specified coordinates.
	 *
	 * @param width The width of the field.
	 *
	 * @param height The height of the field. In a portal the height of all components is set to the height of the first component, unless the "multiLine" property is set.
	 *
	 * @return A JSField instance that corresponds to the newly created text area field.
	 */
	@JSFunction
	public JSField newTextArea(Object dataprovider, int x, int width, int height)
	{
		return newField(dataprovider, Field.TEXT_AREA, x, width, height);
	}

	/**
	 * Creates a new combobox field in the portal. It is equivalent to calling "newField"
	 * with the type JSField.COMBOBOX.
	 *
	 * @sample
	 * var childrenPortal = form.newPortal('pp', 'parent_to_my_table', 10, 10, 1180, 780);
	 * var vlist = solutionModel.newValueList('options', JSValueList.CUSTOM_VALUES);
	 * vlist.customValues = "one\ntwo\nthree\nfour";
	 * var cmb = childrenPortal.newComboBox('my_table_options', 120, 160, 20);
	 * cmb.valuelist = vlist;
	 *
	 * @param dataprovider The data provider for this field. Can be either a column name, or an instance of JSVariable.
	 *
	 * @param x The x coordinate of the field. If the portal does not have the "multiLine" property set, then the x coordinates are used only for determining the order of the columns in the grid. If the portal has the "multiLine" property set, then the components are actually displayed at the specified coordinates.
	 *
	 * @param width The width of the field.
	 *
	 * @param height The height of the field. In a portal the height of all components is set to the height of the first component, unless the "multiLine" property is set.
	 *
	 * @return A JSField instance that corresponds to the newly created combobox field.
	 */
	@JSFunction
	public JSField newComboBox(Object dataprovider, int x, int width, int height)
	{
		return newField(dataprovider, Field.COMBOBOX, x, width, height);
	}

	/**
	 * Creates a new radio buttons field in the portal. It is equivalent to calling "newField"
	 * with the type JSField.RADIOS.
	 *
	 * @sample
	 * var childrenPortal = form.newPortal('pp', 'parent_to_my_table', 10, 10, 1180, 780);
	 * var vlist = solutionModel.newValueList('options', JSValueList.CUSTOM_VALUES);
	 * vlist.customValues = "one\ntwo\nthree\nfour";
	 * var radio = childrenPortal.newRadios('my_table_options', 360, 60, 50);
	 * radio.valuelist = vlist;
	 *
	 * @param dataprovider The data provider for this field. Can be either a column name, or an instance of JSVariable.
	 *
	 * @param x The x coordinate of the field. If the portal does not have the "multiLine" property set, then the x coordinates are used only for determining the order of the columns in the grid. If the portal has the "multiLine" property set, then the components are actually displayed at the specified coordinates.
	 *
	 * @param width The width of the field.
	 *
	 * @param height The height of the field. In a portal the height of all components is set to the height of the first component, unless the "multiLine" property is set.
	 *
	 * @return A JSField instance that corresponds to the newly created radio buttons.
	 */
	@JSFunction
	public JSField newRadios(Object dataprovider, int x, int width, int height)
	{
		return newField(dataprovider, Field.RADIOS, x, width, height);
	}

	/**
	 * Creates a new checkbox field in the portal. It is equivalent to calling "newField"
	 * with the type JSField.CHECKS.
	 *
	 * @sample
	 * var childrenPortal = form.newPortal('pp', 'parent_to_my_table', 10, 10, 1180, 780);
	 * var vlist = solutionModel.newValueList('options', JSValueList.CUSTOM_VALUES);
	 * vlist.customValues = "one\ntwo\nthree\nfour";
	 * var chk = childrenPortal.newCheck('my_table_options', 60, 60, 50);
	 * chk.valuelist = vlist;
	 *
	 * @param dataprovider The data provider for this field. Can be either a column name, or an instance of JSVariable.
	 *
	 * @param x The x coordinate of the field. If the portal does not have the "multiLine" property set, then the x coordinates are used only for determining the order of the columns in the grid. If the portal has the "multiLine" property set, then the components are actually displayed at the specified coordinates.
	 *
	 * @param width The width of the field.
	 *
	 * @param height The height of the field. In a portal the height of all components is set to the height of the first component, unless the "multiLine" property is set.
	 *
	 * @return A JSField instance that corresponds to the newly created checkbox field.
	 */
	@JSFunction
	public JSField newCheck(Object dataprovider, int x, int width, int height)
	{
		return newField(dataprovider, Field.CHECKS, x, width, height);
	}

	/**
	 * Creates a new calendar field in the portal. It is equivalent to calling "newField"
	 * with the type JSField.CALENDAR.
	 *
	 * @sample
	 * var childrenPortal = form.newPortal('pp', 'parent_to_my_table', 10, 10, 1180, 780);
	 * var cal = childrenPortal.newCalendar('my_table_date', 0, 60, 20);
	 *
	 * @param dataprovider The data provider for this field. Can be either a column name, or an instance of JSVariable.
	 *
	 * @param x The x coordinate of the field. If the portal does not have the "multiLine" property set, then the x coordinates are used only for determining the order of the columns in the grid. If the portal has the "multiLine" property set, then the components are actually displayed at the specified coordinates.
	 *
	 * @param width The width of the field.
	 *
	 * @param height The height of the field. In a portal the height of all components is set to the height of the first component, unless the "multiLine" property is set.
	 *
	 * @return A JSField instance that corresponds to the newly created calendar.
	 */
	@JSFunction
	public JSField newCalendar(Object dataprovider, int x, int width, int height)
	{
		return newField(dataprovider, Field.CALENDAR, x, width, height);
	}

	/**
	 * Creates a new RTF Area field in the portal. It is equivalent to calling "newField"
	 * with the type JSField.RTF_AREA.
	 *
	 * @sample
	 * var childrenPortal = form.newPortal('pp', 'parent_to_my_table', 10, 10, 1180, 780);
	 * var rtf = childrenPortal.newRtfArea('my_table_rtf', 420, 60, 50);
	 *
	 * @param dataprovider The data provider for this field. Can be either a column name, or an instance of JSVariable.
	 *
	 * @param x The x coordinate of the field. If the portal does not have the "multiLine" property set, then the x coordinates are used only for determining the order of the columns in the grid. If the portal has the "multiLine" property set, then the components are actually displayed at the specified coordinates.
	 *
	 * @param width The width of the field.
	 *
	 * @param height The height of the field. In a portal the height of all components is set to the height of the first component, unless the "multiLine" property is set.
	 *
	 * @return A JSField instance that corresponds to the newly created RTF Area field.
	 */
	@JSFunction
	public JSField newRtfArea(Object dataprovider, int x, int width, int height)
	{
		return newField(dataprovider, Field.RTF_AREA, x, width, height);
	}

	/**
	 * Creates a new HTML Area field in the portal. It is equivalent to calling "newField"
	 * with the type JSField.HTML_AREA.
	 *
	 * @sample
	 * var childrenPortal = form.newPortal('pp', 'parent_to_my_table', 10, 10, 1180, 780);
	 * var html = childrenPortal.newHtmlArea('my_table_html', 180, 60, 50);
	 *
	 * @param dataprovider The data provider for this field. Can be either a column name, or an instance of JSVariable.
	 *
	 * @param x The x coordinate of the field. If the portal does not have the "multiLine" property set, then the x coordinates are used only for determining the order of the columns in the grid. If the portal has the "multiLine" property set, then the components are actually displayed at the specified coordinates.
	 *
	 * @param width The width of the field.
	 *
	 * @param height The height of the field. In a portal the height of all components is set to the height of the first component, unless the "multiLine" property is set.
	 *
	 * @return A JSField instance that corresponds to the newly created HTML Area field.
	 */
	@JSFunction
	public JSField newHtmlArea(Object dataprovider, int x, int width, int height)
	{
		return newField(dataprovider, Field.HTML_AREA, x, width, height);
	}

	/**
	 * Creates a new Image Media field in the portal. It is equivalent to calling "newField"
	 * with the type JSField.IMAGE_MEDIA.
	 *
	 * @sample
	 * var childrenPortal = form.newPortal('pp', 'parent_to_my_table', 10, 10, 1180, 780);
	 * var img = childrenPortal.newImageMedia('my_table_image', 240, 60, 50);
	 *
	 * @param dataprovider The data provider for this field. Can be either a column name, or an instance of JSVariable.
	 *
	 * @param x The x coordinate of the field. If the portal does not have the "multiLine" property set, then the x coordinates are used only for determining the order of the columns in the grid. If the portal has the "multiLine" property set, then the components are actually displayed at the specified coordinates.
	 *
	 * @param width The width of the field.
	 *
	 * @param height The height of the field. In a portal the height of all components is set to the height of the first component, unless the "multiLine" property is set.
	 *
	 * @return A JSField instance that corresponds to the newly created Image Media field.
	 */
	@JSFunction
	public JSField newImageMedia(Object dataprovider, int x, int width, int height)
	{
		return newField(dataprovider, Field.IMAGE_MEDIA, x, width, height);
	}

	/**
	 * Creates a new type ahead field in the portal. It is equivalent to calling "newField"
	 * with the type JSField.TYPE_AHEAD.
	 *
	 * @sample
	 * var childrenPortal = form.newPortal('pp', 'parent_to_my_table', 10, 10, 1180, 780);
	 * var vlist = solutionModel.newValueList('options', JSValueList.CUSTOM_VALUES);
	 * vlist.customValues = "one\ntwo\nthree\nfour";
	 * var tahead = childrenPortal.newTypeAhead('my_table_text', 600, 60, 20);
	 * tahead.valuelist = vlist;
	 *
	 * @param dataprovider The data provider for this field. Can be either a column name, or an instance of JSVariable.
	 *
	 * @param x The x coordinate of the field. If the portal does not have the "multiLine" property set, then the x coordinates are used only for determining the order of the columns in the grid. If the portal has the "multiLine" property set, then the components are actually displayed at the specified coordinates.
	 *
	 * @param width The width of the field.
	 *
	 * @param height The height of the field. In a portal the height of all components is set to the height of the first component, unless the "multiLine" property is set.
	 *
	 * @return A JSField instance that corresponds to the newly created type ahead field.
	 */
	@JSFunction
	public JSField newTypeAhead(Object dataprovider, int x, int width, int height)
	{
		return newField(dataprovider, Field.TYPE_AHEAD, x, width, height);
	}

	/**
	 * Creates a new password field in the portal. It is equivalent to calling "newField"
	 * with the type JSField.PASSWORD.
	 *
	 * @sample
	 * var childrenPortal = form.newPortal('pp', 'parent_to_my_table', 10, 10, 1180, 780);
	 * var pwd = childrenPortal.newPassword('my_table_text', 300, 60, 20);
	 *
	 * @param dataprovider The data provider for this field. Can be either a column name, or an instance of JSVariable.
	 *
	 * @param x The x coordinate of the field. If the portal does not have the "multiLine" property set, then the x coordinates are used only for determining the order of the columns in the grid. If the portal has the "multiLine" property set, then the components are actually displayed at the specified coordinates.
	 *
	 * @param width The width of the field.
	 *
	 * @param height The height of the field. In a portal the height of all components is set to the height of the first component, unless the "multiLine" property is set.
	 *
	 * @return A JSField instance that corresponds to the newly created password field.
	 */
	@JSFunction
	public JSField newPassword(Object dataprovider, int x, int width, int height)
	{
		return newField(dataprovider, Field.PASSWORD, x, width, height);
	}

	/**
	 * Creates a new button on the portal with the given text, place, size and JSMethod as the onClick action.
	 *
	 * @sample
	 * var clickMethod = form.newMethod('function clickMe() { application.output("I was clicked!"); }');
	 * var childrenPortal = form.newPortal('pp', 'parent_to_child', 10, 10, 620, 460);
	 * childrenPortal.newButton('Click me!', 400, 100, 20, clickMethod);
	 *
	 * @param text The text to be displayed on the button.
	 *
	 * @param x The x coordinate of the button. If the portal does not have the "multiLine" property set, then the x coordinates are used only for determining the order of the columns in the grid. If the portal has the "multiLine" property set, then the components are actually displayed at the specified coordinates.
	 *
	 * @param width The width of the button.
	 *
	 * @param height The height of the button. In a portal the height of all components is set to the height of the first component, unless the "multiLine" property is set.
	 *
	 * @param action The JSMethod object that should be executed when the button is clicked.
	 *
	 * @return A JSButton instance representing the newly created button.
	 */
	@JSFunction
	public JSButton newButton(String text, int x, int width, int height, Object action)
	{
		try
		{
			GraphicalComponent gc = getBaseComponent(true).createNewGraphicalComponent(new Point(getX() + x, getY()));
			gc.setSize(new Dimension(width, height));
			gc.setText(text);
			if (action instanceof JSMethod)
			{
				JSButton button = new JSButton(this, gc, application, true);
				button.setOnAction((JSMethod)action);
				return button;
			}
			else
			{
				String uuid = JSForm.getMethodId(action, gc, application);
				gc.setOnActionMethodID(uuid);
				return new JSButton(this, gc, application, true);
			}
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
	}

	public IBaseSMButton newButton(String text, int x, int y, int width, int height, Object action)
	{
		return newButton(text, x, width, height, action);
	}

	/**
	 * Creates a new label on the form, with the given text, place and size.
	 *
	 * @sample
	 * var clickMethod = form.newMethod('function clickMe() { application.output("I was clicked!"); }');
	 * var childrenPortal = form.newPortal('pp', 'parent_to_my_table', 10, 10, 1180, 780);
	 * var calLabel = childrenPortal.newLabel('Date', 120, 60, 20);
	 * // This will result in a button being actually created, because we specify an action.
	 * var textLabel = childrenPortal.newLabel('Text', 180, 60, 20, clickMethod);
	 *
	 * @param txt The text that will be displayed in the label.
	 *
	 * @param x The x coordinate of the label. If the portal does not have the "multiLine" property set, then the x coordinates are used only for determining the order of the columns in the grid. If the portal has the "multiLine" property set, then the components are actually displayed at the specified coordinates.
	 *
	 * @param width The width of the label.
	 *
	 * @param height The height of the label. In a portal the height of all components is set to the height of the first component, unless the "multiLine" property is set.
	 *
	 * @return A JSLabel instance that represents the newly created label.
	 */
	@JSFunction
	public JSLabel newLabel(String txt, int x, int width, int height)
	{
		return newLabel(txt, x, width, height, null);
	}

	public IBaseSMLabel newLabel(String txt, int x, @SuppressWarnings("unused") int y, int width, int height)
	{
		return newLabel(txt, x, width, height, null);
	}

	/**
	 * Creates a new label on the form, with the given text, place, size and an JSMethod as the onClick action.
	 *
	 * @sample
	 * var clickMethod = form.newMethod('function clickMe() { application.output("I was clicked!"); }');
	 * var childrenPortal = form.newPortal('pp', 'parent_to_my_table', 10, 10, 1180, 780);
	 * var calLabel = childrenPortal.newLabel('Date', 120, 60, 20);
	 * // This will result in a button being actually created, because we specify an action.
	 * var textLabel = childrenPortal.newLabel('Text', 180, 60, 20, clickMethod);
	 *
	 * @param text The text that will be displayed in the label.
	 *
	 * @param x The x coordinate of the label. If the portal does not have the "multiLine" property set, then the x coordinates are used only for determining the order of the columns in the grid. If the portal has the "multiLine" property set, then the components are actually displayed at the specified coordinates.
	 *
	 * @param width The width of the label.
	 *
	 * @param height The height of the label. In a portal the height of all components is set to the height of the first component, unless the "multiLine" property is set.
	 *
	 * @param action The JSMethod object that should be executed when the label is clicked.
	 *
	 * @return A JSLabel instance that represents the newly created label.
	 */
	@JSFunction
	public JSLabel newLabel(String text, int x, int width, int height, Object action)
	{
		try
		{
			GraphicalComponent gc = getBaseComponent(true).createNewGraphicalComponent(new Point(getX() + x, getY()));
			gc.setSize(new Dimension(width, height));
			gc.setText(text);
			if (action instanceof JSMethod)
			{
				JSLabel label = new JSLabel(this, gc, application, true);
				label.setOnAction((JSMethod)action);
				return label;
			}
			else
			{
				String uuid = JSForm.getMethodId(action, gc, application);
				if (uuid != null) gc.setOnActionMethodID(uuid);
				return new JSLabel(this, gc, application, true);
			}
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Retrieves a field from this portal based on the name of the field.
	 *
	 * @sample
	 * var childrenPortal = form.newPortal('pp', 'parent_to_my_table', 10, 10, 1180, 780);
	 * var cal = childrenPortal.newField('my_table_date', JSField.CALENDAR, 0, 60, 20);
	 * var tfield = childrenPortal.newField('my_table_text', JSField.TEXT_FIELD, 60, 60, 20);
	 * tfield.name = 'textField'; // Give a name to the field so we can retrieve it later by name.
	 * // Retrieve the text field by its name and do something with it.
	 * var textFieldBack = childrenPortal.getField('textField');
	 * textFieldBack.background = 'yellow';
	 * // Retrieve the calendar field through the array of all fields and do something with it.
	 * var allFields = childrenPortal.getFields();
	 * var calFieldBack = allFields[0];
	 * calFieldBack.foreground = 'red';
	 *
	 * @param name The name of the field to retrieve.
	 *
	 * @return A JSField instance corresponding to the field with the specified name.
	 */
	@JSFunction
	public JSField getField(String name)
	{
		if (name == null) return null;

		Iterator<Field> fields = getBaseComponent(false).getFields();
		while (fields.hasNext())
		{
			Field field = fields.next();
			if (name.equals(field.getName()))
			{
				return JSField.createField(this, field, application, false);
			}
		}
		return null;
	}

	/**
	 * Retrieves an array with all fields in a portal.
	 *
	 * @sampleas getField(String)
	 *
	 * @return An array with JSField instances corresponding to all fields in the portal.
	 */
	@JSFunction
	public JSField[] getFields()
	{
		ArrayList<JSField> fields = new ArrayList<JSField>();
		Iterator<Field> iterator = getBaseComponent(false).getFields();
		while (iterator.hasNext())
		{
			Field field = iterator.next();
			fields.add(JSField.createField(this, field, application, false));
		}
		return fields.toArray(new JSField[fields.size()]);
	}

	/**
	 * Retrieves a button from the portal based on the name of the button.
	 *
	 * @sample
	 * var clickMethod = form.newMethod('function clickMe() { application.output("I was clicked!"); }');
	 * var childrenPortal = form.newPortal('pp', 'parent_to_child', 10, 10, 620, 460);
	 * var btn = childrenPortal.newButton('Click me!', 400, 100, 20, clickMethod);
	 * btn.name = 'clickMeBtn'; // Give a name to the button, so we can retrieve it by name later.
	 * // Retrieve the button by name and do something with it.
	 * var btnBack = childrenPortal.getButton('clickMeBtn');
	 * btnBack.background = 'yellow';
	 * // Retrieve the button through the array of all buttons and do something with it.
	 * var allButtons = childrenPortal.getButtons();
	 * var btnBackAgain = allButtons[0];
	 * btnBackAgain.foreground = 'red';
	 *
	 * @param name The name of the button to retrieve.
	 *
	 * @return A JSButton instance that corresponds to the button with the specified name.
	 */
	@JSFunction
	public JSButton getButton(String name)
	{
		if (name == null) return null;

		Iterator<GraphicalComponent> graphicalComponents = getBaseComponent(false).getGraphicalComponents();
		while (graphicalComponents.hasNext())
		{
			GraphicalComponent button = graphicalComponents.next();
			if (name.equals(button.getName()) && ComponentFactory.isButton(button))
			{
				return new JSButton(this, button, application, false);
			}
		}
		return null;
	}

	/**
	 * Retrieves an array with all buttons in the portal.
	 *
	 * @sampleas getButton(String)
	 *
	 * @return An array with all buttons in the portal.
	 */
	@JSFunction
	public JSButton[] getButtons()
	{
		ArrayList<JSButton> buttons = new ArrayList<JSButton>();
		Iterator<GraphicalComponent> graphicalComponents = getBaseComponent(false).getGraphicalComponents();
		while (graphicalComponents.hasNext())
		{
			GraphicalComponent button = graphicalComponents.next();
			if (ComponentFactory.isButton(button))
			{
				buttons.add(new JSButton(this, button, application, false));
			}
		}
		return buttons.toArray(new JSButton[buttons.size()]);
	}

	/**
	 * Retrieves a label from this portal based on the name of the label.
	 *
	 * @sample
	 * var calLabel = childrenPortal.newLabel('Date', 120, 60, 20);
	 * var textLabel = childrenPortal.newLabel('Text', 180, 60, 20);
	 * textLabel.name = 'textLabel'; // Give a name to this label, so we can retrieve it by name.
	 * // Retrieve the second label by name.
	 * var textLabelBack = childrenPortal.getLabel('textLabel');
	 * textLabelBack.background = 'yellow';
	 * // Retrieve the first label through the array of all labels.
	 * var allLabels = childrenPortal.getLabels();
	 * var calLabelBack = allLabels[0];
	 * calLabelBack.foreground = 'red';
	 *
	 * @param name The name of the label to retrieve.
	 *
	 * @return A JSLabel instance corresponding to the label with the specified name.
	 */
	@JSFunction
	public JSLabel getLabel(String name)
	{
		if (name == null) return null;

		Iterator<GraphicalComponent> graphicalComponents = getBaseComponent(false).getGraphicalComponents();
		while (graphicalComponents.hasNext())
		{
			GraphicalComponent label = graphicalComponents.next();
			if (name.equals(label.getName()) && !ComponentFactory.isButton(label))
			{
				return new JSLabel(this, label, application, false);
			}
		}
		return null;
	}

	/**
	 * Retrieves all labels from the portal.
	 *
	 * @sampleas getLabel(String)
	 *
	 * @return An array of JSLabel instances corresponding to all labels in the portal.
	 */
	@JSFunction
	public JSLabel[] getLabels()
	{
		ArrayList<JSLabel> labels = new ArrayList<JSLabel>();
		Iterator<GraphicalComponent> graphicalComponents = getBaseComponent(false).getGraphicalComponents();
		while (graphicalComponents.hasNext())
		{
			GraphicalComponent label = graphicalComponents.next();
			if (!ComponentFactory.isButton(label))
			{
				labels.add(new JSLabel(this, label, application, false));
			}
		}
		return labels.toArray(new JSLabel[labels.size()]);
	}

	public IBaseSMComponent[] getComponents()
	{
		List<JSComponent< ? >> lst = new ArrayList<JSComponent< ? >>();
		lst.addAll(Arrays.asList(getLabels()));
		lst.addAll(Arrays.asList(getButtons()));
		lst.addAll(Arrays.asList(getFields()));
		return lst.toArray(new JSComponent[lst.size()]);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Portal#getInitialSort()
	 *
	 * @sample
	 * var childrenPortal = form.newPortal('pp',rel,10,10,620,460);
	 * childrenPortal.newTextField('child_table_id', 0, 100, 20);
	 * childrenPortal.newTextField('child_table_text',100,100,20);
	 * childrenPortal.newTextField('child_table_parent_id', 200, 100, 20);
	 * childrenPortal.initialSort = 'child_table_text desc';
	 */
	@JSGetter
	public String getInitialSort()
	{
		return getBaseComponent(false).getInitialSort();
	}

	@JSSetter
	public void setInitialSort(String arg)
	{
		getBaseComponent(true).setInitialSort(arg);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Portal#getIntercellSpacing()
	 *
	 * @sample
	 * var spacing = childrenPortal.getIntercellSpacing();
	 * application.output("horizontal spacing: " + spacing.width);
	 * application.output("vertical spacing: " + spacing.height);
	 *
	 * @return A java.awt.Dimension object holding the horizontal and vertical intercell spacing.
	 */
	@JSFunction
	public Dimension getIntercellSpacing()
	{
		return getBaseComponent(false).getIntercellSpacing();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Portal#getMultiLine()
	 *
	 * @sample
	 * var childrenPortal = form.newPortal('pp',rel,10,10,620,460);
	 * // Set the fields some distance apart horizontally. By default this distance
	 * // is ignored and the components are put in a grid.
	 * var idField = childrenPortal.newTextField('child_table_id', 0, 100, 20);
	 * idField.background = 'yellow';
	 * var textField = childrenPortal.newTextField('child_table_text',150,100,20);
	 * textField.background = 'green';
	 * var parentIdField = childrenPortal.newTextField('child_table_parent_id', 300, 100, 20);
	 * parentIdField.background = 'orange';
	 * // Disable the grid placing of components, and make the distance between components
	 * // become active.
	 * childrenPortal.multiLine = true;
	 */
	@JSGetter
	public boolean getMultiLine()
	{
		return getBaseComponent(false).getMultiLine();
	}

	@JSSetter
	public void setMultiLine(boolean arg)
	{
		getBaseComponent(true).setMultiLine(arg);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Portal#getRelationName()
	 *
	 * @sample
	 * // Create the portal based on one relation.
	 * var childrenPortal = form.newPortal('pp', 'parent_to_child', 10, 10, 620, 460);
	 * var idField = childrenPortal.newTextField('child_table_id', 0, 100, 20);
	 * var textField = childrenPortal.newTextField('child_table_text',150,100,20);
	 * var parentIdField = childrenPortal.newTextField('child_table_parent_id', 300, 100, 20);
	 * // Now make the portal be based on another relation.
	 * childrenPortal.relationName = 'parent_to_smaller_children';
	 */
	@JSGetter
	public String getRelationName()
	{
		return getBaseComponent(false).getRelationName();
	}

	@JSSetter
	public void setRelationName(String arg)
	{
		getBaseComponent(true).setRelationName(Utils.toEnglishLocaleLowerCase(arg));
		// clear the data provider lookups affected by this change
		FlattenedSolution fs = application.getFlattenedSolution();
		fs.flushDataProviderLookups(getBaseComponent(true));
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Portal#getReorderable()
	 *
	 * @sample
	 * var childrenPortal = form.newPortal('pp', 'parent_to_child', 10, 10, 620, 460);
	 * childrenPortal.newTextField('child_table_id', 0, 100, 20);
	 * childrenPortal.newTextField('child_table_text',150,100,20);
	 * childrenPortal.newTextField('child_table_parent_id', 300, 100, 20);
	 * childrenPortal.reorderable = true;
	 */
	@JSGetter
	public boolean getReorderable()
	{
		return getBaseComponent(false).getReorderable();
	}

	@JSSetter
	public void setReorderable(boolean arg)
	{
		getBaseComponent(true).setReorderable(arg);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Portal#getResizeble()
	 *
	 * @deprecated As of release 6.0, replaced by {@link #getResizable()}.
	 *
	 * @sample
	 * var childrenPortal = form.newPortal('pp', 'parent_to_child', 10, 10, 620, 460);
	 * childrenPortal.newTextField('child_table_id', 0, 100, 20);
	 * childrenPortal.newTextField('child_table_text',150,100,20);
	 * childrenPortal.newTextField('child_table_parent_id', 300, 100, 20);
	 * // Make the columns resizable. By default they are not resizable.
	 * childrenPortal.resizeble = true;
	 */
	@Deprecated
	public boolean js_getResizeble()
	{
		return getResizable();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Portal#getResizable()
	 *
	 * @sample
	 * var childrenPortal = form.newPortal('pp', 'parent_to_child', 10, 10, 620, 460);
	 * childrenPortal.newTextField('child_table_id', 0, 100, 20);
	 * childrenPortal.newTextField('child_table_text',150,100,20);
	 * childrenPortal.newTextField('child_table_parent_id', 300, 100, 20);
	 * // Make the columns resizable. By default they are not resizable.
	 * childrenPortal.resizable = true;
	 */
	@JSGetter
	public boolean getResizable()
	{
		return getBaseComponent(false).getResizable();
	}

	@JSSetter
	public void setResizable(boolean arg)
	{
		getBaseComponent(true).setResizable(arg);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Portal#getRowBGColorCalculation()
	 *
	 * @deprecated As of release 6.0, replaced by onRender event.
	 *
	 * @sample
	 * var childrenPortal = form.newPortal('pp', 'parent_to_child', 10, 10, 620, 460);
	 * childrenPortal.newTextField('child_table_id', 0, 100, 20);
	 * childrenPortal.newTextField('child_table_text',150,100,20);
	 * childrenPortal.newTextField('child_table_parent_id', 300, 100, 20);
	 * // Set the row background color calculation. The name should be of a calculation that
	 * // exists in the table.
	 * childrenPortal.rowBGColorCalculation = 'row_color';
	 *
	 */
	@Deprecated
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public String js_getRowBGColorCalculation()
	{
		return getBaseComponent(false).getRowBGColorCalculation();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Portal#getRowHeight()
	 *
	 * @sample
	 * var childrenPortal = form.newPortal('pp', 'parent_to_child', 10, 10, 620, 460);
	 * childrenPortal.newTextField('child_table_id', 0, 100, 20);
	 * childrenPortal.newTextField('child_table_text',150,100,20);
	 * childrenPortal.newTextField('child_table_parent_id', 300, 100, 20);
	 * childrenPortal.rowHeight = 30;
	 */
	@JSGetter
	public int getRowHeight()
	{
		return getBaseComponent(false).getRowHeight();
	}

	@JSSetter
	public void setRowHeight(int arg)
	{
		getBaseComponent(true).setRowHeight(arg);
	}

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSField#getScrollbars()
	 */
	@JSGetter
	public int getScrollbars()
	{
		return getBaseComponent(false).getScrollbars();
	}

	@JSSetter
	public void setScrollbars(int i)
	{
		getBaseComponent(true).setScrollbars(i);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Portal#getShowHorizontalLines()
	 *
	 * @sample
	 * var childrenPortal = form.newPortal('pp', 'parent_to_child', 10, 10, 620, 460);
	 * childrenPortal.newTextField('child_table_id', 0, 100, 20);
	 * childrenPortal.newTextField('child_table_text',150,100,20);
	 * childrenPortal.newTextField('child_table_parent_id', 300, 100, 20);
	 * childrenPortal.showHorizontalLines = true;
	 * childrenPortal.showVerticalLines = true;
	 */
	@JSGetter
	public boolean getShowHorizontalLines()
	{
		return getBaseComponent(false).getShowHorizontalLines();
	}

	@JSSetter
	public void setShowHorizontalLines(boolean arg)
	{
		getBaseComponent(true).setShowHorizontalLines(arg);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Portal#getShowVerticalLines()
	 *
	 * @sampleas getShowHorizontalLines()
	 */
	@JSGetter
	public boolean getShowVerticalLines()
	{
		return getBaseComponent(false).getShowVerticalLines();
	}

	@JSSetter
	public void setShowVerticalLines(boolean arg)
	{
		getBaseComponent(true).setShowVerticalLines(arg);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Portal#getSortable()
	 *
	 * @sample
	 * var childrenPortal = form.newPortal('pp', 'parent_to_child', 10, 10, 620, 460);
	 * childrenPortal.newTextField('child_table_id', 0, 100, 20);
	 * childrenPortal.newTextField('child_table_text',150,100,20);
	 * childrenPortal.newTextField('child_table_parent_id', 300, 100, 20);
	 * childrenPortal.sortable = true;
	 */
	@JSGetter
	public boolean getSortable()
	{
		return getBaseComponent(false).getSortable();
	}

	@JSSetter
	public void setSortable(boolean arg)
	{
		getBaseComponent(true).setSortable(arg);
	}

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSGraphicalComponent#getTabSeq()
	 */
	@JSGetter
	public int getTabSeq()
	{
		return getBaseComponent(false).getTabSeq();
	}

	@JSSetter
	public void setTabSeq(int arg)
	{
		getBaseComponent(true).setTabSeq(arg);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Portal#getIntercellSpacing()
	 *
	 * @sample
	 * var childrenPortal = form.newPortal('pp',rel,10,10,620,460);
	 * childrenPortal.newTextField('child_table_id', 0, 100, 20);
	 * childrenPortal.newTextField('child_table_text',100,100,20);
	 * childrenPortal.newTextField('child_table_parent_id', 200, 100, 20);
	 * childrenPortal.setIntercellSpacing(5,10);
	 *
	 * @param width The horizontal spacing between cells.
	 *
	 * @param height The vertical spacing between cells.
	 */
	@JSFunction
	public void setIntercellSpacing(int width, int height)
	{
		getBaseComponent(true).setIntercellSpacing(new Dimension(width, height));
	}

	@Deprecated
	public void js_setResizeble(boolean arg)
	{
		setResizable(arg);
	}

	@Deprecated
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public void js_setRowBGColorCalculation(String arg)
	{
		getBaseComponent(true).setRowBGColorCalculation(arg);
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.JSComponent#setX(int)
	 */
	@Override
	@JSSetter
	public void setX(int x)
	{
		int xDif = x - getX();
		Iterator<IPersist> allObjects = getBaseComponent(true).getAllObjects();
		while (allObjects.hasNext())
		{
			IPersist persist = allObjects.next();
			if (persist instanceof BaseComponent)
			{
				BaseComponent baseComponent = (BaseComponent)persist;
				Point location = baseComponent.getLocation();
				((BaseComponent)persist).setLocation(new Point(location.x + xDif, location.y));
			}
		}
		super.setX(x);
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.JSComponent#setY(int)
	 */
	@Override
	@JSSetter
	public void setY(int y)
	{
		int yDif = y - getY();
		Iterator<IPersist> allObjects = getBaseComponent(true).getAllObjects();
		while (allObjects.hasNext())
		{
			IPersist persist = allObjects.next();
			if (persist instanceof BaseComponent)
			{
				BaseComponent baseComponent = (BaseComponent)persist;
				Point location = baseComponent.getLocation();
				((BaseComponent)persist).setLocation(new Point(location.x, location.y + yDif));
			}
		}
		super.setY(y);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Portal#getOnRenderMethodID()
	 *
	 * @sample
	 * portal.onRender = form.newMethod('function onRender(event) { event.getElement().bgcolor = \'#00ff00\' }');
	 */
	@JSGetter
	public JSMethod getOnRender()
	{
		return getEventHandler(application, StaticContentSpecLoader.PROPERTY_ONRENDERMETHODID);
	}

	@JSSetter
	public void setOnRender(ISMMethod method)
	{
		setEventHandler(application, StaticContentSpecLoader.PROPERTY_ONRENDERMETHODID, (JSMethod)method);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Portal#getOnDragMethodID()
	 *
	 * @sample
	 * form.onDrag = form.newMethod('function onDrag(event) { application.output("onDrag intercepted from " + event.getSource()); }');
	 * form.onDragEnd = form.newMethod('function onDragEnd(event) { application.output("onDragEnd intercepted from " + event.getSource()); }');
	 * form.onDragOver = form.newMethod('function onDragOver(event) { application.output("onDragOver intercepted from " + event.getSource()); }');
	 * form.onDrop = form.newMethod('function onDrop(event) { application.output("onDrop intercepted from " + event.getSource()); }');
	 */
	@JSGetter
	public JSMethod getOnDrag()
	{
		return getEventHandler(application, StaticContentSpecLoader.PROPERTY_ONDRAGMETHODID);
	}

	@JSSetter
	public void setOnDrag(ISMMethod method)
	{
		setEventHandler(application, StaticContentSpecLoader.PROPERTY_ONDRAGMETHODID, (JSMethod)method);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Portal#getOnDragEndMethodID()
	 *
	 * @sampleas getOnDrag()
	 */
	@JSGetter
	public JSMethod getOnDragEnd()
	{
		return getEventHandler(application, StaticContentSpecLoader.PROPERTY_ONDRAGENDMETHODID);
	}

	@JSSetter
	public void setOnDragEnd(ISMMethod method)
	{
		setEventHandler(application, StaticContentSpecLoader.PROPERTY_ONDRAGENDMETHODID, (JSMethod)method);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Portal#getOnDragOverMethodID()
	 *
	 * @sampleas getOnDrag()
	 */
	@JSGetter
	public JSMethod getOnDragOver()
	{
		return getEventHandler(application, StaticContentSpecLoader.PROPERTY_ONDRAGOVERMETHODID);
	}

	@JSSetter
	public void setOnDragOver(ISMMethod method)
	{
		setEventHandler(application, StaticContentSpecLoader.PROPERTY_ONDRAGOVERMETHODID, (JSMethod)method);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Portal#getOnDropMethodID()
	 *
	 * @sampleas getOnDrag()
	 */
	@JSGetter
	public JSMethod getOnDrop()
	{
		return getEventHandler(application, StaticContentSpecLoader.PROPERTY_ONDROPMETHODID);
	}

	@JSSetter
	public void setOnDrop(ISMMethod method)
	{
		setEventHandler(application, StaticContentSpecLoader.PROPERTY_ONDROPMETHODID, (JSMethod)method);
	}
}
