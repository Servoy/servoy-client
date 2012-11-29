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

import com.servoy.j2db.scripting.api.solutionmodel.IBaseSMField;

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

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSGraphicalComponent#getOnAction()
	 */
	public ISMMethod getOnAction();

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