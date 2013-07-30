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

import com.servoy.base.solutionmodel.IBaseSMField;

/**
 * Solution model field component.
 * 
 * @author rgansevles
 *
 * @since 6.1
 */
public interface ISMField extends IBaseSMField, ISMComponent
{

	/**
	 * Flag that tells if the content of the field can be edited or not. 
	 * The default value of this flag is "true", that is the content can be edited.
	 * 
	 * @sample
	 * var field = form.newField('my_table_text', JSField.TEXT_FIELD, 10, 10, 100, 20);
	 * field.editable = false;
	 */
	public boolean getEditable();

	/**
	 * The format that should be applied when displaying the data in the component.
	 * There are different options for the different dataprovider types that are assigned to this field.
	 * For Integer fields, there is a display and an edit format, using http://docs.oracle.com/javase/7/docs/api/java/text/DecimalFormat.html and the max (string) length can be set.
	 * For Text/String fields, there are options to force uppercase,lowercase or only numbers. Or a mask can be set that restrict the input the pattern chars can be found here: http://docs.oracle.com/javase/7/docs/api/javax/swing/text/MaskFormatter.html
	 * A mask can have a placehoder (what is shown when there is no data) and if the data must be stored raw (without literals of the mask). A max text length can also be set to force
	 * the max text length input, this doesn't work on mask because that max length is controlled with the mask.
	 * For Date fields a display and edit format can be set by using a pattern from here: http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html, you can also say this must behave like a mask (the edit format)
	 * A mask only works with when the edit format is exactly that mask (1 char is 1 number/char), because for example MM then only 2 numbers are allowed MMM that displays the month as a string is not supported as a mask.
	 * Some examples are "dd-MM-yyyy", "MM-dd-yyyy", etc.
	 * The format property is also used to set the UI Converter, this means that you can convert the value object to something else before it gets set into the field, this can also result in a type change of the data. 
	 * So a string in scripting/db is converted to a integer in the ui, then you have to set an integer format. 
	 * This property is applicable only for types: TEXT_FIELD, COMBOBOX, TYPE_AHEAD, CALENDAR and SPINNER.
	 * 
	 * @sample
	 * var field = form.newField('my_table_number', JSField.TEXT_FIELD, 10, 10, 100, 20);
	 * field.format = '$#.00';
	 */
	public String getFormat();

	/**
	 * Horizontal alignment of the text inside the component. Can be one of
	 * LEFT, CENTER or RIGHT.
	 * 
	 * Note that this property does not refer to the horizontal alignment
	 * of the component inside the form.
	 * 
	 * @sample 
	 * var leftAlignedLabel = form.newLabel('LEFT', 10, 10, 300, 20);
	 * leftAlignedLabel.horizontalAlignment = SM_ALIGNMENT.LEFT;
	 * var hCenteredLabel = form.newLabel('CENTER', 10, 40, 300, 20);
	 * hCenteredLabel.horizontalAlignment = SM_ALIGNMENT.CENTER;
	 * var rightAlignedLabel = form.newLabel('RIGHT', 10, 70, 300, 20);
	 * rightAlignedLabel.horizontalAlignment = SM_ALIGNMENT.RIGHT;
	 */

	public int getHorizontalAlignment();

	/**
	 * The margins of the component. They are specified in this order, 
	 * separated by commas: top, left, bottom, right.
	 * 
	 * @sample
	 * var label = form.newLabel('Label', 10, 10, 150, 150);
	 * label.background = 'yellow';
	 * label.margin = '10,20,30,40';
	 */
	public String getMargin();

	/**
	 * Scrollbar options for the vertical and horizontal scrollbars. Each of the
	 * vertical and horizontal scrollbars can be configured to display all the time,
	 * to display only when needed or to never display.
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
	 * Flag that tells if the content of the field should be automatically selected
	 * when the field receives focus. The default value of this field is "false".
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
	 * An index that specifies the position of the component in the tab sequence. The components 
	 * are put into the tab sequence in increasing order of this property. A value of 0 means
	 * to use the default mechanism of building the tab sequence (based on their location on the form).
	 * A value of -2 means to remove the component from the tab sequence.
	 * 
	 * @sample
	 * // Create three fields. Based on how they are placed, by default they will come one
	 * // after another in the tab sequence.
	 * var fieldOne = form.newField('parent_table_id', JSField.TEXT_FIELD, 10, 10, 100, 20);
	 * var fieldTwo = form.newField('parent_table_text', JSField.TEXT_FIELD, 10, 40, 100, 20);
	 * var fieldThree = form.newField('parent_table_id', JSField.TEXT_FIELD, 10, 70, 100, 20);
	 * // Set the third field come before the first in the tab sequence, and remove the 
	 * // second field from the tab sequence.
	 * fieldOne.tabSeq = 2;
	 * fieldTwo.tabSeq = SM_DEFAULTS.IGNORE;
	 * fieldThree.tabSeq = 1;
	 */
	public int getTabSeq();

	/**
	 * The text that is displayed in the column header associated with the component when the form
	 * is in table view.
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
	 * The text displayed when hovering over the component with a mouse cursor.
	 * 
	 * NOTE:
	 * HTML should be used for multi-line tooltips; you can also use any
	 * valid HTML tags to format tooltip text. For example: 
	 * <html>This includes<b>bolded text</b> and 
	 * <font color='blue'>BLUE</font> text as well.</html>
	 * 
	 * @sample
	 * var label = form.newLabel('Stop the mouse over me!', 10, 10, 200, 20);
	 * label.toolTipText = 'I\'m the tooltip. Do you see me?';
	 */
	public String getToolTipText();

	/**
	 * @clonedesc com.servoy.base.solutionmodel.IBaseSMField#getValuelist()
	 * @see com.servoy.base.solutionmodel.IBaseSMField#getValuelist()
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

	/**
	 * @sameas com.servoy.base.solutionmodel.IBaseSMField#getOnAction()
	 * @see com.servoy.base.solutionmodel.IBaseSMField#getOnAction()
	 */
	public ISMMethod getOnAction();

	/**
	 * @clonedesc com.servoy.base.solutionmodel.IBaseSMField#getOnDataChange()
	 * @see com.servoy.base.solutionmodel.IBaseSMField#getOnDataChange()
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
	 * The method that is executed when the component is rendered.
	 * 
	 * @sample
	 * field.onRender = form.newMethod('function onRender(event) { event.getElement().bgcolor = \'#00ff00\' }');
	 */
	public ISMMethod getOnRender();

	public void setOnRightClick(ISMMethod method);

	/**
	 * The method that is executed when the component is right clicked.
	 * 
	 * @see #getOnAction()
	 */
	public ISMMethod getOnRightClick();

	/**
	 * The method that is executed when the component gains focus.
	 * NOTE: Do not call methods that will influence the focus itself.
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

	/**
	 * The method that is executed when the component looses focus.
	 * 
	 * @see #getOnFocusGained()
	 */
	public ISMMethod getOnFocusLost();

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString();

}