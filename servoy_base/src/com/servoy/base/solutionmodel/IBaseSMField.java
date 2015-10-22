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

import com.servoy.base.persistence.constants.IFieldConstants;
import com.servoy.base.scripting.annotations.ServoyClientSupport;


/**
 * Solution model field component (for mobile as well as other clients).
 * 
 * @author rgansevles
 * @author acostescu
 *
 * @since 7.0
 */
@ServoyClientSupport(mc = true, wc = true, sc = true)
public interface IBaseSMField extends IBaseSMComponent
{
	/**
	 * Constant for specifying the display type of a JSField. Sets the display type of the
	 * field to text field. The field will show regular text on a single line.
	 * 
	 * @sample
	 * var tfield = form.newField('my_table_text', JSField.TEXT_FIELD, 10, 460, 100, 20);
	 */
	public static final int TEXT_FIELD = IFieldConstants.TEXT_FIELD;
	/**
	 * Constant for specifying the display type of a JSField. Sets the display type of the
	 * field to text area. The field will show text on multiple lines.
	 * 
	 * @sample
	 * var tarea = form.newField('my_table_text', JSField.TEXT_AREA, 10, 400, 100, 50);
	 */
	public static final int TEXT_AREA = IFieldConstants.TEXT_AREA;
	/**
	 * Constant for specifying the display type of a JSField. Sets the display type of the
	 * field to combobox. 
	 * 
	 * @sample
	 * var vlist = solutionModel.newValueList('options', JSValueList.CUSTOM_VALUES);
	 * vlist.customValues = "one\ntwo\nthree\nfour";
	 * var cmb = form.newField('my_table_options', JSField.COMBOBOX, 10, 100, 100, 20);
	 * cmb.valuelist = vlist;
	 */
	public static final int COMBOBOX = IFieldConstants.COMBOBOX;
	/**
	 * Constant for specifying the display type of a JSField. Sets the display type of the
	 * field to radio buttons. The field will show a radio button, or a list of them if 
	 * the valuelist property is also set.
	 * 
	 * @sample
	 * var vlist = solutionModel.newValueList('options', JSValueList.CUSTOM_VALUES);
	 * vlist.customValues = "one\ntwo\nthree\nfour";
	 * var radio = form.newField('my_table_options', JSField.RADIOS, 10, 280, 100, 50);
	 * radio.valuelist = vlist;
	 */
	public static final int RADIOS = IFieldConstants.RADIOS;
	/**
	 * Constant for specifying the display type of a JSField. Sets the display type of the
	 * field to checkbox. The field will show a checkbox, or a list of checkboxes if the valuelist
	 * property is also set.
	 * 
	 * @sample
	 * var vlist = solutionModel.newValueList('options', JSValueList.CUSTOM_VALUES);
	 * vlist.customValues = "one\ntwo\nthree\nfour";
	 * var chk = form.newField('my_table_options', JSField.CHECKS, 10, 40, 100, 50);
	 * chk.valuelist = vlist;
	 */
	public static final int CHECKS = IFieldConstants.CHECKS;
	/**
	 * Constant for specifying the display type of a JSField. Sets the display type of the 
	 * field to calendar. The field will show a formatted date and will have a button which
	 * pops up a calendar for date selection.
	 *
	 * @sample 
	 * var cal = form.newField('my_table_date', JSField.CALENDAR, 10, 10, 100, 20);
	 */
	public static final int CALENDAR = IFieldConstants.CALENDAR;
	/**
	 * Constant for specifying the display type of a JSField. Sets the display type of the
	 * fiels to password. The field will allow the user to enter passwords, masking the typed
	 * characters.
	 * 
	 * @sample
	 * 	var pwd = form.newField('my_table_text', JSField.PASSWORD, 10, 250, 100, 20);
	 */
	public static final int PASSWORD = IFieldConstants.PASSWORD;
	/**
	 * Constant for specifying the display type of a JSField. Sets the display type of the 
	 * field to RTF area. The field will display formatted RTF content.
	 * 
	 * @sample
	 * 	var rtf = form.newField('my_table_rtf', JSField.RTF_AREA, 10, 340, 100, 50);
	 */
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public static final int RTF_AREA = IFieldConstants.RTF_AREA;
	/**
	 * Constant for specifying the display type of a JSField. Sets the display type of the
	 * field to HTML area. The field will display formatted HTML content.
	 * 
	 * @sample
	 * var html = form.newField('my_table_html', JSField.HTML_AREA, 10, 130, 100, 50);
	 */
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public static final int HTML_AREA = IFieldConstants.HTML_AREA;
	/**
	 * Constant for specifying the display type of a JSField. Sets the display type of the
	 * field to image. The field will display images.
	 * 
	 * @sample
	 * var img = form.newField('my_table_image', JSField.IMAGE_MEDIA, 10, 190, 100, 50);
	 */
	// to be included in mobile in future versions
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public static final int IMAGE_MEDIA = IFieldConstants.IMAGE_MEDIA;
	/**
	 * Constant for specifying the display type of a JSField. Sets the display type of the
	 * field to type ahead. The field will show regular text, but will have type ahead 
	 * capabilities.
	 * 
	 * @sample
	 * var vlist = solutionModel.newValueList('options', JSValueList.CUSTOM_VALUES);
	 * vlist.customValues = "one\ntwo\nthree\nfour";
	 * var tahead = form.newField('my_table_text', JSField.TYPE_AHEAD, 10, 490, 100, 20);
	 * tahead.valuelist = vlist;
	 */
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public static final int TYPE_AHEAD = IFieldConstants.TYPE_AHEAD;
	/**
	 * Constant for specifying the display type of a JSField. Sets the display type of the
	 * field to list box. The field will show a selection list with single choice selection.
	 * 
	 * @sample
	 * var vlist = solutionModel.newValueList('options', JSValueList.CUSTOM_VALUES);
	 * vlist.customValues = "one\ntwo\nthree\nfour";
	 * var list = form.newField('my_table_list', JSField.LIST_BOX, 10, 280, 100, 50);
	 * list.valuelist = vlist;
	 * 
	 * @deprecated replaced by JSField.LISTBOX
	 */
	@Deprecated
	public static final int LIST_BOX = IFieldConstants.LIST_BOX;
	/**
	 * Constant for specifying the display type of a JSField. Sets the display type of the
	 * field to list box. The field will show a selection list with single choice selection.
	 * 
	 * @sample
	 * var vlist = solutionModel.newValueList('options', JSValueList.CUSTOM_VALUES);
	 * vlist.customValues = "one\ntwo\nthree\nfour";
	 * var list = form.newField('my_table_list', JSField.LISTBOX, 10, 280, 100, 50);
	 * list.valuelist = vlist;
	 */
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public static final int LISTBOX = IFieldConstants.LIST_BOX;
	/**
	 * Constant for specifying the display type of a JSField. Sets the display type of the
	 * field to list box. The field will show a selection list with multiple choice selection.
	 * 
	 * @sample
	 * var vlist = solutionModel.newValueList('options', JSValueList.CUSTOM_VALUES);
	 * vlist.customValues = "one\ntwo\nthree\nfour";
	 * var list = form.newField('my_table_options', JSField.MULTISELECT_LISTBOX, 10, 280, 100, 50);
	 * list.valuelist = vlist;
	 */
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public static final int MULTISELECT_LISTBOX = IFieldConstants.MULTISELECT_LISTBOX;
	/**
	 * Constant for specifying the display type of a JSField. Sets the display type of the
	 * field to spinner. The field will show a spinner.
	 * 
	 * @sample
	 * var vlist = solutionModel.newValueList('options', JSValueList.CUSTOM_VALUES);
	 * vlist.customValues = "one\ntwo\nthree\nfour";
	 * var spinner = form.newField('my_spinner', JSField.SPINNER, 10, 460, 100, 20);
	 * spinner.valuelist = vlist;
	 */
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public static final int SPINNER = IFieldConstants.SPINNER;

	/**
	 * The dataprovider of the component.
	 * 
	 * @sample
	 * // Normally the dataprovider is specified when a component is created.
	 * var field = form.newField('parent_table_text', JSField.TEXT_FIELD, 10, 40, 100, 20);
	 * // But it can be modified later if needed.
	 * field.dataProviderID = 'parent_table_id';
	 * 
	 */
	public String getDataProviderID();


	/**
	 * The type of display used by the field. Can be one of CALENDAR, CHECKS,
	 * COMBOBOX, HTML_AREA, IMAGE_MEDIA, PASSWORD, RADIOS, RTF_AREA, TEXT_AREA,
	 * TEXT_FIELD, TYPE_AHEAD, LIST_BOX, MULTISELECT_LISTBOX or SPINNER.
	 *
	 * @sample 
	 * // The display type is specified when the field is created.
	 * var cal = form.newField('my_table_date', JSField.CALENDAR, 10, 10, 100, 20);
	 * // But it can be changed if needed.
	 * cal.dataProviderID = 'my_table_text';
	 * cal.displayType = JSField.TEXT_FIELD;
	 */
	public int getDisplayType();

	/**
	 * The valuelist that is used by this field when displaying data. Can be used
	 * with fields of type CHECKS, COMBOBOX, RADIOS and TYPE_AHEAD.
	 * 
	 * @sample
	 * var vlist = solutionModel.newValueList('options', JSValueList.CUSTOM_VALUES);
	 * vlist.customValues = "one\ntwo\nthree\nfour";
	 * var cmb = form.newField('my_table_options', JSField.COMBOBOX, 10, 100, 100, 20);
	 * cmb.valuelist = vlist;
	 */
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public IBaseSMValueList getValuelist();

	public String getFormat();

	public void setFormat(String arg);

	public void setDataProviderID(String arg);

	public void setDisplayType(int arg);

	public void setValuelist(IBaseSMValueList valuelist);

	public void setOnAction(IBaseSMMethod method);

	/**
	 * The method that is executed when the component is clicked.
	 * 
	 * @sample
	 * var doNothingMethod = form.newMethod('function doNothing() { application.output("Doing nothing."); }');
	 * var onClickMethod = form.newMethod('function onClick(event) { application.output("I was clicked at " + event.getTimestamp()); }');
	 * var onDoubleClickMethod = form.newMethod('function onDoubleClick(event) { application.output("I was double-clicked at " + event.getTimestamp()); }');
	 * var onRightClickMethod = form.newMethod('function onRightClick(event) { application.output("I was right-clicked at " + event.getTimestamp()); }');
	 * // At creation the button has the 'doNothing' method as onClick handler, but we'll change that later.
	 * var btn = form.newButton('I am a button', 10, 40, 200, 20, doNothingMethod);
	 * btn.onAction = onClickMethod;
	 * btn.onDoubleClick = onDoubleClickMethod;
	 * btn.onRightClick = onRightClickMethod;
	 */
	public IBaseSMMethod getOnAction();

	public void setOnDataChange(IBaseSMMethod method);

	/**
	 * Method that is executed when the data in the component is successfully changed.
	 * 
	 * @sample
	 * var form = solutionModel.newForm('someForm', 'db:/example_data/parent_table', null, false, 620, 300);
	 * var onDataChangeMethod = form.newMethod('function onDataChange(oldValue, newValue, event) { application.output("Data changed from " + oldValue + " to " + newValue + " at " + event.getTimestamp()); }');
	 * var field = form.newField('parent_table_text', JSField.TEXT_FIELD, 10, 10, 100, 20);
	 * field.onDataChange = onDataChangeMethod;
	 * forms['someForm'].controller.show();
	 */
	public IBaseSMMethod getOnDataChange();

	/**
	 * The text that is displayed in field when the field doesn't have a text value.
	 * 
	 * @sample
	 * field.placeholderText = 'Search';
	 */
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public String getPlaceholderText();

	public void setPlaceholderText(String arg);
}