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

package com.servoy.j2db.solutionmodel;

import com.servoy.j2db.persistence.Field;

/**
 * Solution model field component.
 * 
 * @author rgansevles
 *
 * @since 6.1
 */
public interface ISMField extends ISMComponent
{

	/**
	 * Constant for specifying the display type of a JSField. Sets the display type of the
	 * field to text field. The field will show regular text on a single line.
	 * 
	 * @sample
	 * var tfield = form.newField('my_table_text', JSField.TEXT_FIELD, 10, 460, 100, 20);
	 */
	public static final int TEXT_FIELD = Field.TEXT_FIELD;
	/**
	 * Constant for specifying the display type of a JSField. Sets the display type of the
	 * field to text area. The field will show text on multiple lines.
	 * 
	 * @sample
	 * var tarea = form.newField('my_table_text', JSField.TEXT_AREA, 10, 400, 100, 50);
	 */
	public static final int TEXT_AREA = Field.TEXT_AREA;
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
	public static final int COMBOBOX = Field.COMBOBOX;
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
	public static final int RADIOS = Field.RADIOS;
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
	public static final int CHECKS = Field.CHECKS;
	/**
	 * Constant for specifying the display type of a JSField. Sets the display type of the 
	 * field to calendar. The field will show a formatted date and will have a button which
	 * pops up a calendar for date selection.
	 *
	 * @sample 
	 * var cal = form.newField('my_table_date', JSField.CALENDAR, 10, 10, 100, 20);
	 */
	public static final int CALENDAR = Field.CALENDAR;
	/**
	 * Constant for specifying the display type of a JSField. Sets the display type of the
	 * fiels to password. The field will allow the user to enter passwords, masking the typed
	 * characters.
	 * 
	 * @sample
	 * 	var pwd = form.newField('my_table_text', JSField.PASSWORD, 10, 250, 100, 20);
	 */
	public static final int PASSWORD = Field.PASSWORD;
	/**
	 * Constant for specifying the display type of a JSField. Sets the display type of the 
	 * field to RTF area. The field will display formatted RTF content.
	 * 
	 * @sample
	 * 	var rtf = form.newField('my_table_rtf', JSField.RTF_AREA, 10, 340, 100, 50);
	 */
	public static final int RTF_AREA = Field.RTF_AREA;
	/**
	 * Constant for specifying the display type of a JSField. Sets the display type of the
	 * field to HTML area. The field will display formatted HTML content.
	 * 
	 * @sample
	 * var html = form.newField('my_table_html', JSField.HTML_AREA, 10, 130, 100, 50);
	 */
	public static final int HTML_AREA = Field.HTML_AREA;
	/**
	 * Constant for specifying the display type of a JSField. Sets the display type of the
	 * field to image. The field will display images.
	 * 
	 * @sample
	 * var img = form.newField('my_table_image', JSField.IMAGE_MEDIA, 10, 190, 100, 50);
	 */
	public static final int IMAGE_MEDIA = Field.IMAGE_MEDIA;
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
	public static final int TYPE_AHEAD = Field.TYPE_AHEAD;
	/**
	 * Constant for specifying the display type of a JSField. Sets the display type of the
	 * field to list box. The field will show a selection list with single choice selection.
	 * 
	 * @sample
	 * var vlist = solutionModel.newValueList('options', JSValueList.CUSTOM_VALUES);
	 * vlist.customValues = "one\ntwo\nthree\nfour";
	 * var list = form.newField('my_table_list', JSField.LIST_BOX, 10, 280, 100, 50);
	 * list.valuelist = vlist;
	 */
	public static final int LIST_BOX = Field.LIST_BOX;
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
	public static final int MULTISELECT_LISTBOX = Field.MULTISELECT_LISTBOX;
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
	public static final int SPINNER = Field.SPINNER;

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSGraphicalComponent#getDataProviderID()
	 */
	public String getDataProviderID();

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSGraphicalComponent#getDataProviderID()
	 */
	public boolean getDisplaysTags();

	/**
	 * @clonedesc com.servoy.j2db.persistence.Field#getDisplayType()
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
	 * @clonedesc com.servoy.j2db.persistence.Field#getEditable()
	 * 
	 * @sample
	 * var field = form.newField('my_table_text', JSField.TEXT_FIELD, 10, 10, 100, 20);
	 * field.editable = false;
	 */
	public boolean getEditable();

	/**
	 * @clonedesc com.servoy.j2db.persistence.Field#getFormat()
	 * 
	 * @sample
	 * var field = form.newField('my_table_number', JSField.TEXT_FIELD, 10, 10, 100, 20);
	 * field.format = '$#.00';
	 */
	public String getFormat();

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSGraphicalComponent#getHorizontalAlignment()
	 */

	public int getHorizontalAlignment();

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSGraphicalComponent#getMargin()
	 */
	public String getMargin();

	/**
	 * @clonedesc com.servoy.j2db.persistence.Field#getScrollbars()
	 *
	 * @sample
	 * var noScrollbars = form.newField('my_table_text', JSField.TEXT_AREA, 10, 10, 100, 100);
	 * noScrollbars.scrollbars = SM_SCROLLBAR.HORIZONTAL_SCROLLBAR_NEVER | SM_SCROLLBAR.VERTICAL_SCROLLBAR_NEVER;
	 * var neededScrollbars = form.newField('my_table_text', JSField.TEXT_AREA, 120, 10, 100, 100);
	 * neededScrollbars.scrollbars = SM_SCROLLBAR.HORIZONTAL_SCROLLBAR_AS_NEEDED | SM_SCROLLBAR.VERTICAL_SCROLLBAR_AS_NEEDED;
	 * var alwaysScrollbars = form.newField('my_table_text', JSField.TEXT_AREA, 230, 10, 100, 100);
	 * alwaysScrollbars.scrollbars = SM_SCROLLBAR.HORIZONTAL_SCROLLBAR_ALWAYS | SM_SCROLLBAR.VERTICAL_SCROLLBAR_ALWAYS;
	 */
	public int getScrollbars();

	/**
	 * @clonedesc com.servoy.j2db.persistence.Field#getSelectOnEnter()
	 * 
	 * @sample
	 * // Create two fields and set one of them to have "selectOnEnter" true. As you tab
	 * // through the fields you can notice how the text inside the second field gets
	 * // automatically selected when the field receives focus.
	 * var fieldNoSelect = form.newField('my_table_text', JSField.TEXT_FIELD, 10, 10, 100, 20);
	 * var fieldSelect = form.newField('my_table_text', JSField.TEXT_FIELD, 10, 40, 100, 20);
	 * fieldSelect.selectOnEnter = true;
	 */
	public boolean getSelectOnEnter();

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSGraphicalComponent#getTabSeq()
	 */
	public int getTabSeq();

	/**
	 * @clonedesc com.servoy.j2db.persistence.Field#getText()
	 * 
	 * @sample
	 * var form = solutionModel.newForm('someForm', 'db:/example_data/my_table', null, false, 640, 480);
	 * var field = form.newField('my_table_number', JSField.TEXT_FIELD, 10, 10, 100, 20);
	 * field.titleText = 'Column Title';
	 * form.view = JSForm.LOCKED_TABLE_VIEW;
	 * forms['someForm'].controller.show()
	 */
	public String getTitleText();

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSGraphicalComponent#getToolTipText()
	 */
	public String getToolTipText();

	/**
	 * @clonedesc com.servoy.j2db.persistence.Field#getValuelistID()
	 * 
	 * @sample
	 * var vlist = solutionModel.newValueList('options', JSValueList.CUSTOM_VALUES);
	 * vlist.customValues = "one\ntwo\nthree\nfour";
	 * var cmb = form.newField('my_table_options', JSField.COMBOBOX, 10, 100, 100, 20);
	 * cmb.valuelist = vlist;
	 */
	public ISMValueList getValuelist();

	public void setDataProviderID(String arg);

	public void setDisplaysTags(boolean arg);

	public void setDisplayType(int arg);

	public void setEditable(boolean arg);

	public void setFormat(String arg);

	public void setHorizontalAlignment(int arg);

	public void setMargin(String margin);

	public void setScrollbars(int arg);

	public void setSelectOnEnter(boolean arg);

	public void setTabSeq(int arg);

	public void setTitleText(String arg);

	public void setToolTipText(String arg);

	public void setValuelist(ISMValueList valuelist);

	public void setOnAction(ISMMethod method);

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSGraphicalComponent#getOnAction()
	 */
	public ISMMethod getOnAction();

	public void setOnDataChange(ISMMethod method);

	/**
	 * @clonedesc com.servoy.j2db.persistence.Field#getOnDataChangeMethodID()
	 * 
	 * @sample
	 * var form = solutionModel.newForm('someForm', 'db:/example_data/parent_table', null, false, 620, 300);
	 * var onDataChangeMethod = form.newMethod('function onDataChange(oldValue, newValue, event) { application.output("Data changed from " + oldValue + " to " + newValue + " at " + event.getTimestamp()); }');
	 * var field = form.newField('parent_table_text', JSField.TEXT_FIELD, 10, 10, 100, 20);
	 * field.onDataChange = onDataChangeMethod;
	 * forms['someForm'].controller.show();
	 */
	public ISMMethod getOnDataChange();

	public void setOnRender(ISMMethod method);

	/**
	 * @clonedesc com.servoy.j2db.persistence.Field#getOnRenderMethodID()
	 * 
	 * @sample
	 * field.onRender = form.newMethod('function onRender(event) { event.getElement().bgcolor = \'#00ff00\' }');
	 */
	public ISMMethod getOnRender();

	public void setOnRightClick(ISMMethod method);

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSGraphicalComponent#getOnRightClick()
	 */
	public ISMMethod getOnRightClick();

	public void setOnFocusGained(ISMMethod method);

	/**
	 * @clonedesc com.servoy.j2db.persistence.Field#getOnFocusGainedMethodID()
	 * 
	 * @sample
	 * var form = solutionModel.newForm('someForm', 'db:/example_data/parent_table', null, false, 620, 300);
	 * var onFocusLostMethod = form.newMethod('function onFocusLost(event) { application.output("Focus lost at " + event.getTimestamp()); }');
	 * var onFocusGainedMethod = form.newMethod('function onFocusGained(event) { application.output("Focus gained at " + event.getTimestamp()); }');
	 * var field = form.newField('parent_table_text', JSField.TEXT_FIELD, 10, 10, 100, 20);
	 * field.onFocusGained = onFocusGainedMethod;
	 * field.onFocusLost = onFocusLostMethod;
	 * forms['someForm'].controller.show()
	 */
	public ISMMethod getOnFocusGained();

	public void setOnFocusLost(ISMMethod method);

	/**
	 * @clonedesc com.servoy.j2db.persistence.Field#getOnFocusLostMethodID()
	 * 
	 * @sampleas getOnFocusGained()
	 */
	public ISMMethod getOnFocusLost();

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.JSComponent#toString()
	 */
	public String toString();

}