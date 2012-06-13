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

import java.awt.Dimension;


/**
 * Solution model portal object.
 * 
 * @author rgansevles
 *
 * @since 6.1
 */
public interface ISMPortal extends ISMComponent
{

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
	public ISMField newField(Object dataprovider, int displaytype, int x, int width, int height);

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
	public ISMField newTextField(Object dataprovider, int x, int width, int height);

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
	public ISMField newTextArea(Object dataprovider, int x, int width, int height);

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
	public ISMField newComboBox(Object dataprovider, int x, int width, int height);

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
	public ISMField newRadios(Object dataprovider, int x, int width, int height);

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
	public ISMField newCheck(Object dataprovider, int x, int width, int height);

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
	public ISMField newCalendar(Object dataprovider, int x, int width, int height);

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
	public ISMField newRtfArea(Object dataprovider, int x, int width, int height);

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
	public ISMField newHtmlArea(Object dataprovider, int x, int width, int height);

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
	public ISMField newImageMedia(Object dataprovider, int x, int width, int height);

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
	public ISMField newTypeAhead(Object dataprovider, int x, int width, int height);

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
	public ISMField newPassword(Object dataprovider, int x, int width, int height);

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
	public ISMButton newButton(String text, int x, int width, int height, Object action);

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
	public ISMLabel newLabel(String txt, int x, int width, int height);

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
	public ISMLabel newLabel(String text, int x, int width, int height, Object action);

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
	public ISMField getField(String name);

	/**
	 * Retrieves an array with all fields in a portal.
	 * 
	 * @sampleas getField(String)
	 * 
	 * @return An array with JSField instances corresponding to all fields in the portal.
	 */
	public ISMField[] getFields();

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
	public ISMButton getButton(String name);

	/**
	 * Retrieves an array with all buttons in the portal.
	 * 
	 * @sampleas getButton(String)
	 * 
	 * @return An array with all buttons in the portal.
	 */
	public ISMButton[] getButtons();

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
	public ISMLabel getLabel(String name);

	/**
	 * Retrieves all labels from the portal.
	 * 
	 * @sampleas getLabel(String)
	 * 
	 * @return An array of JSLabel instances corresponding to all labels in the portal.
	 */
	public ISMLabel[] getLabels();

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
	public String getInitialSort();

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
	public Dimension getIntercellSpacing();

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
	public boolean getMultiLine();

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
	public String getRelationName();

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
	public boolean getReorderable();

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
	public boolean getResizable();

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
	public int getRowHeight();

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSField#getScrollbars()
	 */
	public int getScrollbars();

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
	public boolean getShowHorizontalLines();

	/**
	 * @clonedesc com.servoy.j2db.persistence.Portal#getShowVerticalLines()
	 * 
	 * @sampleas getShowHorizontalLines()
	 */
	public boolean getShowVerticalLines();

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
	public boolean getSortable();

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSGraphicalComponent#getTabSeq()
	 */
	public int getTabSeq();

	public void setInitialSort(String arg);

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
	public void setIntercellSpacing(int width, int height);

	public void setMultiLine(boolean arg);

	public void setRelationName(String arg);

	public void setReorderable(boolean arg);

	public void setResizable(boolean arg);

	public void setRowHeight(int arg);

	public void setScrollbars(int i);

	public void setShowHorizontalLines(boolean arg);

	public void setShowVerticalLines(boolean arg);

	public void setSortable(boolean arg);

	public void setTabSeq(int arg);

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.JSComponent#setX(int)
	 */
	public void setX(int x);

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.JSComponent#setY(int)
	 */
	public void setY(int y);

	/**
	 * @clonedesc com.servoy.j2db.persistence.Portal#getOnRenderMethodID()
	 * 
	 * @sample
	 * portal.onRender = form.newMethod('function onRender(event) { event.getElement().bgcolor = \'#00ff00\' }');
	 */
	public ISMMethod getOnRender();

	public void setOnRender(ISMMethod method);

	/**
	 * @clonedesc com.servoy.j2db.persistence.Portal#getOnDragMethodID()
	 * 
	 * @sample
	 * form.onDrag = form.newMethod('function onDrag(event) { application.output("onDrag intercepted from " + event.getSource()); }');
	 * form.onDragEnd = form.newMethod('function onDragEnd(event) { application.output("onDragEnd intercepted from " + event.getSource()); }');
	 * form.onDragOver = form.newMethod('function onDragOver(event) { application.output("onDragOver intercepted from " + event.getSource()); }');
	 * form.onDrop = form.newMethod('function onDrop(event) { application.output("onDrop intercepted from " + event.getSource()); }');
	 */
	public ISMMethod getOnDrag();

	/**
	 * @clonedesc com.servoy.j2db.persistence.Portal#getOnDragEndMethodID()
	 * 
	 * @sampleas getOnDrag()
	 */
	public ISMMethod getOnDragEnd();

	/**
	 * @clonedesc com.servoy.j2db.persistence.Portal#getOnDragOverMethodID()
	 * 
	 * @sampleas getOnDrag()
	 */
	public ISMMethod getOnDragOver();

	/**
	 * @clonedesc com.servoy.j2db.persistence.Portal#getOnDropMethodID()
	 * 
	 * @sampleas getOnDrag()
	 */
	public ISMMethod getOnDrop();

	public void setOnDrag(ISMMethod method);

	public void setOnDragEnd(ISMMethod method);

	public void setOnDragOver(ISMMethod method);

	public void setOnDrop(ISMMethod method);

}