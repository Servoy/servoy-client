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
import java.util.Iterator;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.scripting.IJavaScriptType;
import com.servoy.j2db.util.Utils;


/**
 * @author jcompagner
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class JSPortal extends JSComponent<Portal> implements IJSParent, IJavaScriptType
{
	private final IApplication application;

	/**
	 * @param baseComponent
	 */
	public JSPortal(JSForm form, Portal portal, IApplication application, boolean isNew)
	{
		super(form, portal, isNew);
		this.application = application;
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.IJSParent#getBaseComponent()
	 */
	public ISupportChilds getSupportChild()
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
	public JSField js_newField(Object dataprovider, int displaytype, int x, int width, int height)
	{
		try
		{
			Field field = getBaseComponent(true).createNewField(new Point(js_getX() + x, js_getY()));
			field.setDisplayType(displaytype);
			field.setSize(new Dimension(width, height));
			if (dataprovider instanceof String)
			{
				field.setDataProviderID((String)dataprovider);
			}
			else if (dataprovider instanceof JSVariable)
			{
				field.setDataProviderID(((JSVariable)dataprovider).js_getName());
			}
			return new JSField(this, field, application, true);
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
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
	public JSField js_newTextField(Object dataprovider, int x, int width, int height)
	{
		return js_newField(dataprovider, Field.TEXT_FIELD, x, width, height);
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
	public JSField js_newTextArea(Object dataprovider, int x, int width, int height)
	{
		return js_newField(dataprovider, Field.TEXT_AREA, x, width, height);
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
	public JSField js_newComboBox(Object dataprovider, int x, int width, int height)
	{
		return js_newField(dataprovider, Field.COMBOBOX, x, width, height);
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
	public JSField js_newRadios(Object dataprovider, int x, int width, int height)
	{
		return js_newField(dataprovider, Field.RADIOS, x, width, height);
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
	public JSField js_newCheck(Object dataprovider, int x, int width, int height)
	{
		return js_newField(dataprovider, Field.CHECKS, x, width, height);
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
	public JSField js_newCalendar(Object dataprovider, int x, int width, int height)
	{
		return js_newField(dataprovider, Field.CALENDAR, x, width, height);
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
	public JSField js_newRtfArea(Object dataprovider, int x, int width, int height)
	{
		return js_newField(dataprovider, Field.RTF_AREA, x, width, height);
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
	public JSField js_newHtmlArea(Object dataprovider, int x, int width, int height)
	{
		return js_newField(dataprovider, Field.HTML_AREA, x, width, height);
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
	public JSField js_newImageMedia(Object dataprovider, int x, int width, int height)
	{
		return js_newField(dataprovider, Field.IMAGE_MEDIA, x, width, height);
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
	public JSField js_newTypeAhead(Object dataprovider, int x, int width, int height)
	{
		return js_newField(dataprovider, Field.TYPE_AHEAD, x, width, height);
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
	public JSField js_newPassword(Object dataprovider, int x, int width, int height)
	{
		return js_newField(dataprovider, Field.PASSWORD, x, width, height);
	}

	/**
	 * Creates a new button on the portal with the given text, place, size and JSMethod as the onClick action.
	 *
	 * @sample 
	 * var clickMethod = form.newFormMethod('function clickMe() { application.output("I was clicked!"); }');
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
	public JSButton js_newButton(String text, int x, int width, int height, Object action)
	{
		try
		{
			GraphicalComponent gc = getBaseComponent(true).createNewGraphicalComponent(new Point(js_getX() + x, js_getY()));
			gc.setSize(new Dimension(width, height));
			gc.setText(text);
			int id = JSForm.getMethodId(action, gc, application);
			gc.setOnActionMethodID(id);
			return new JSButton(this, gc, application, true);
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Creates a new label on the form, with the given text, place and size.
	 *
	 * @sample
	 * var clickMethod = form.newFormMethod('function clickMe() { application.output("I was clicked!"); }');
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
	public JSLabel js_newLabel(String txt, int x, int width, int height)
	{
		return js_newLabel(txt, x, width, height, null);
	}

	/**
	 * Creates a new label on the form, with the given text, place, size and an JSMethod as the onClick action.
	 *
	 * @sample
	 * var clickMethod = form.newFormMethod('function clickMe() { application.output("I was clicked!"); }');
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
	public JSLabel js_newLabel(String text, int x, int width, int height, Object action)
	{
		try
		{
			GraphicalComponent gc = getBaseComponent(true).createNewGraphicalComponent(new Point(js_getX() + x, js_getY()));
			gc.setSize(new Dimension(width, height));
			gc.setText(text);
			int id = JSForm.getMethodId(action, gc, application);
			if (id != -1) gc.setOnActionMethodID(id);
			return new JSLabel(this, gc, application, true);
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
	public JSField js_getField(String name)
	{
		if (name == null) return null;

		Iterator<Field> fields = getBaseComponent(false).getFields();
		while (fields.hasNext())
		{
			Field field = fields.next();
			if (name.equals(field.getName()))
			{
				return new JSField(this, field, application, false);
			}
		}
		return null;
	}

	/**
	 * Retrieves an array with all fields in a portal.
	 * 
	 * @sampleas js_getField(String)
	 * 
	 * @return An array with JSField instances corresponding to all fields in the portal.
	 */
	public JSField[] js_getFields()
	{
		ArrayList<JSField> fields = new ArrayList<JSField>();
		Iterator<Field> iterator = getBaseComponent(false).getFields();
		while (iterator.hasNext())
		{
			Field field = iterator.next();
			fields.add(new JSField(this, field, application, false));
		}
		return fields.toArray(new JSField[fields.size()]);
	}

	/**
	 * Retrieves a button from the portal based on the name of the button.
	 * 
	 * @sample
	 * var clickMethod = form.newFormMethod('function clickMe() { application.output("I was clicked!"); }');
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
	public JSButton js_getButton(String name)
	{
		if (name == null) return null;

		Iterator<GraphicalComponent> graphicalComponents = getBaseComponent(false).getGraphicalComponents();
		while (graphicalComponents.hasNext())
		{
			GraphicalComponent button = graphicalComponents.next();
			if (name.equals(button.getName()) && button.getOnActionMethodID() != 0 && button.getShowClick())
			{
				return new JSButton(this, button, application, false);
			}
		}
		return null;
	}

	/**
	 * Retrieves an array with all buttons in the portal.
	 * 
	 * @sampleas js_getButton(String)
	 * 
	 * @return An array with all buttons in the portal.
	 */
	public JSButton[] js_getButtons()
	{
		ArrayList<JSButton> buttons = new ArrayList<JSButton>();
		Iterator<GraphicalComponent> graphicalComponents = getBaseComponent(false).getGraphicalComponents();
		while (graphicalComponents.hasNext())
		{
			GraphicalComponent button = graphicalComponents.next();
			if (button.getOnActionMethodID() != 0 && button.getShowClick())
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
	public JSLabel js_getLabel(String name)
	{
		if (name == null) return null;

		Iterator<GraphicalComponent> graphicalComponents = getBaseComponent(false).getGraphicalComponents();
		while (graphicalComponents.hasNext())
		{
			GraphicalComponent button = graphicalComponents.next();
			if (name.equals(button.getName()) && !(button.getOnActionMethodID() != 0 && button.getShowClick()))
			{
				return new JSLabel(this, button, application, false);
			}
		}
		return null;
	}

	/**
	 * Retrieves all labels from the portal.
	 * 
	 * @sampleas js_getLabel(String)
	 * 
	 * @return An array of JSLabel instances corresponding to all labels in the portal.
	 */
	public JSLabel[] js_getLabels()
	{
		ArrayList<JSLabel> labels = new ArrayList<JSLabel>();
		Iterator<GraphicalComponent> graphicalComponents = getBaseComponent(false).getGraphicalComponents();
		while (graphicalComponents.hasNext())
		{
			GraphicalComponent button = graphicalComponents.next();
			if (!(button.getOnActionMethodID() != 0 && button.getShowClick()))
			{
				labels.add(new JSLabel(this, button, application, false));
			}
		}
		return labels.toArray(new JSLabel[labels.size()]);
	}

//	public Shape createNewShape(Point location) throws RepositoryException
//	{
//		return getBaseComponent(true).createNewShape(location);
//	}

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
	public String js_getInitialSort()
	{
		return getBaseComponent(false).getInitialSort();
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
	public Dimension js_getIntercellSpacing()
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
	public boolean js_getMultiLine()
	{
		return getBaseComponent(false).getMultiLine();
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
	public String js_getRelationName()
	{
		return getBaseComponent(false).getRelationName();
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
	public boolean js_getReorderable()
	{
		return getBaseComponent(false).getReorderable();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Portal#getResizeble()
	 * 
	 * @sample
	 * var childrenPortal = form.newPortal('pp', 'parent_to_child', 10, 10, 620, 460);
	 * childrenPortal.newTextField('child_table_id', 0, 100, 20);
	 * childrenPortal.newTextField('child_table_text',150,100,20);
	 * childrenPortal.newTextField('child_table_parent_id', 300, 100, 20);
	 * // Make the columns resizeable. By default they are not resizeable.
	 * childrenPortal.resizeble = true;
	 */
	@Deprecated
	public boolean js_getResizeble()
	{
		return js_getResizable();
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
	public boolean js_getResizable()
	{
		return getBaseComponent(false).getResizable();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Portal#getRowBGColorCalculation()
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
	 * @deprecated
	 *
	 * @see com.servoy.j2db.scripting.solutionmodel.JSPortal#js_getOnRender()
	 */
	@Deprecated
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
	public int js_getRowHeight()
	{
		return getBaseComponent(false).getRowHeight();
	}

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSField#js_getScrollbars()
	 */
	public int js_getScrollbars()
	{
		return getBaseComponent(false).getScrollbars();
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
	public boolean js_getShowHorizontalLines()
	{
		return getBaseComponent(false).getShowHorizontalLines();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Portal#getShowVerticalLines()
	 * 
	 * @sampleas js_getShowHorizontalLines()
	 */
	public boolean js_getShowVerticalLines()
	{
		return getBaseComponent(false).getShowVerticalLines();
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
	public boolean js_getSortable()
	{
		return getBaseComponent(false).getSortable();
	}

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSGraphicalComponent#js_getTabSeq()
	 */
	public int js_getTabSeq()
	{
		return getBaseComponent(false).getTabSeq();
	}

	public void js_setInitialSort(String arg)
	{
		getBaseComponent(true).setInitialSort(arg);
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
	public void js_setIntercellSpacing(int width, int height)
	{
		getBaseComponent(true).setIntercellSpacing(new Dimension(width, height));
	}

	public void js_setMultiLine(boolean arg)
	{
		getBaseComponent(true).setMultiLine(arg);
	}

	public void js_setRelationName(String arg)
	{
		getBaseComponent(true).setRelationName(Utils.toEnglishLocaleLowerCase(arg));
		// clear the data provider lookups affected by this change
		FlattenedSolution fs = application.getFlattenedSolution();
		fs.flushDataProviderLookups(getBaseComponent(true));
	}

	public void js_setReorderable(boolean arg)
	{
		getBaseComponent(true).setReorderable(arg);
	}

	@Deprecated
	public void js_setResizeble(boolean arg)
	{
		js_setResizable(arg);
	}

	public void js_setResizable(boolean arg)
	{
		getBaseComponent(true).setResizable(arg);
	}

	@Deprecated
	public void js_setRowBGColorCalculation(String arg)
	{
		getBaseComponent(true).setRowBGColorCalculation(arg);
	}

	public void js_setRowHeight(int arg)
	{
		getBaseComponent(true).setRowHeight(arg);
	}

	public void js_setScrollbars(int i)
	{
		getBaseComponent(true).setScrollbars(i);
	}

	public void js_setShowHorizontalLines(boolean arg)
	{
		getBaseComponent(true).setShowHorizontalLines(arg);
	}

	public void js_setShowVerticalLines(boolean arg)
	{
		getBaseComponent(true).setShowVerticalLines(arg);
	}

	public void js_setSortable(boolean arg)
	{
		getBaseComponent(true).setSortable(arg);
	}

	public void js_setTabSeq(int arg)
	{
		getBaseComponent(true).setTabSeq(arg);
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.JSComponent#js_setX(int)
	 */
	@Override
	public void js_setX(int x)
	{
		int xDif = x - js_getX();
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
		super.js_setX(x);
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.JSComponent#js_setY(int)
	 */
	@Override
	public void js_setY(int y)
	{
		int yDif = y - js_getY();
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
		super.js_setY(y);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Portal#getOnRenderMethodID()
	 * 
	 * @sample
	 * portal.onRender = form.newFormMethod('function onRender(event) { event.getElement().bgcolor = \'#00ff00\' }');
	 */
	public JSMethod js_getOnRender()
	{
		return getEventHandler(application, StaticContentSpecLoader.PROPERTY_ONRENDERMETHODID.getPropertyName());
	}

	public void js_setOnRender(JSMethod method)
	{
		setEventHandler(application, StaticContentSpecLoader.PROPERTY_ONRENDERMETHODID.getPropertyName(), method);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Portal#getOnDragMethodID()
	 * 
	 * @sample
	 * form.onDrag = form.newFormMethod('function onDrag(event) { application.output("onDrag intercepted from " + event.getSource()); }');
	 * form.onDragEnd = form.newFormMethod('function onDragEnd(event) { application.output("onDragEnd intercepted from " + event.getSource()); }');
	 * form.onDragOver = form.newFormMethod('function onDragOver(event) { application.output("onDragOver intercepted from " + event.getSource()); }');
	 * form.onDrop = form.newFormMethod('function onDrop(event) { application.output("onDrop intercepted from " + event.getSource()); }');
	 */
	public JSMethod js_getOnDrag()
	{
		return getEventHandler(application, StaticContentSpecLoader.PROPERTY_ONDRAGMETHODID.getPropertyName());
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Portal#getOnDragEndMethodID()
	 * 
	 * @sampleas js_getOnDrag()
	 */
	public JSMethod js_getOnDragEnd()
	{
		return getEventHandler(application, StaticContentSpecLoader.PROPERTY_ONDRAGENDMETHODID.getPropertyName());
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Portal#getOnDragOverMethodID()
	 * 
	 * @sampleas js_getOnDrag()
	 */
	public JSMethod js_getOnDragOver()
	{
		return getEventHandler(application, StaticContentSpecLoader.PROPERTY_ONDRAGOVERMETHODID.getPropertyName());
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Portal#getOnDropMethodID()
	 * 
	 * @sampleas js_getOnDrag()
	 */
	public JSMethod js_getOnDrop()
	{
		return getEventHandler(application, StaticContentSpecLoader.PROPERTY_ONDROPMETHODID.getPropertyName());
	}

	public void js_setOnDrag(JSMethod method)
	{
		setEventHandler(application, StaticContentSpecLoader.PROPERTY_ONDRAGMETHODID.getPropertyName(), method);
	}

	public void js_setOnDragEnd(JSMethod method)
	{
		setEventHandler(application, StaticContentSpecLoader.PROPERTY_ONDRAGENDMETHODID.getPropertyName(), method);
	}

	public void js_setOnDragOver(JSMethod method)
	{
		setEventHandler(application, StaticContentSpecLoader.PROPERTY_ONDRAGOVERMETHODID.getPropertyName(), method);
	}

	public void js_setOnDrop(JSMethod method)
	{
		setEventHandler(application, StaticContentSpecLoader.PROPERTY_ONDROPMETHODID.getPropertyName(), method);
	}
}
