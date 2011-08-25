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

import org.mozilla.javascript.Function;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.scripting.IConstantsObject;
import com.servoy.j2db.util.PersistHelper;

@ServoyDocumented(category = ServoyDocumented.RUNTIME, extendsComponent = "JSComponent")
public class JSField extends JSComponent<Field> implements IConstantsObject
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
	 * var radio = form.newField('my_table_list', JSField.LIST_BOX, 10, 280, 100, 50);
	 * radio.valuelist = vlist;
	 */
	public static final int LIST_BOX = Field.LIST_BOX;

	/**
	 * Constant for specifying the display type of a JSField. Sets the display type of the
	 * field to list box. The field will show a selection list with multiple choice selection.
	 * 
	 * @sample
	 * var vlist = solutionModel.newValueList('options', JSValueList.CUSTOM_VALUES);
	 * vlist.customValues = "one\ntwo\nthree\nfour";
	 * var radio = form.newField('my_table_options', JSField.MULTI_SELECTION_LIST_BOX, 10, 280, 100, 50);
	 * radio.valuelist = vlist;
	 */
	public static final int MULTI_SELECTION_LIST_BOX = Field.MULTI_SELECTION_LIST_BOX;

	private final IApplication application;

	// support constants
	public JSField()
	{
		this(null, null, null, false);
	}

	/**
	 * @param field
	 */
	public JSField(IJSParent parent, Field field, IApplication application, boolean isNew)
	{
		super(parent, field, isNew);
		this.application = application;
	}

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSGraphicalComponent#js_getDataProviderID()
	 */
	public String js_getDataProviderID()
	{
		return getBaseComponent(false).getDataProviderID();
	}

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSGraphicalComponent#js_getDataProviderID()
	 */
	public boolean js_getDisplaysTags()
	{
		return getBaseComponent(false).getDisplaysTags();
	}

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
	public int js_getDisplayType()
	{
		return getBaseComponent(false).getDisplayType();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Field#getEditable()
	 * 
	 * @sample
	 * var field = form.newField('my_table_text', JSField.TEXT_FIELD, 10, 10, 100, 20);
	 * field.editable = false;
	 */
	public boolean js_getEditable()
	{
		return getBaseComponent(false).getEditable();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Field#getFormat()
	 * 
	 * @sample
	 * var field = form.newField('my_table_number', JSField.TEXT_FIELD, 10, 10, 100, 20);
	 * field.format = '$#.00';
	 */
	public String js_getFormat()
	{
		return getBaseComponent(false).getFormat();
	}

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSGraphicalComponent#js_getHorizontalAlignment()
	 */
	public int js_getHorizontalAlignment()
	{
		return getBaseComponent(false).getHorizontalAlignment();
	}

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSGraphicalComponent#js_getMargin()
	 */
	public String js_getMargin()
	{
		return PersistHelper.createInsetsString(getBaseComponent(false).getMargin());
	}

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
	public int js_getScrollbars()
	{
		return getBaseComponent(false).getScrollbars();
	}

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
	public boolean js_getSelectOnEnter()
	{
		return getBaseComponent(false).getSelectOnEnter();
	}

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSGraphicalComponent#js_getTabSeq()
	 */
	public int js_getTabSeq()
	{
		return getBaseComponent(false).getTabSeq();
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.JSField#js_getDataProviderID()
	 * @see com.servoy.j2db.dataprocessing.FoundSet#js_getDataProviderValue(String)
	 */
	@Deprecated
	public String js_getText()
	{
		return getBaseComponent(false).getText();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Field#getText()
	 * 
	 * @sample
	 * var form = solutionModel.newForm('someForm', 'example_data', 'my_table', 'null', false, 640, 480);
	 * var field = form.newField('my_table_number', JSField.TEXT_FIELD, 10, 10, 100, 20);
	 * field.titleText = 'Column Title';
	 * form.view = JSForm.LOCKED_TABLE_VIEW;
	 * forms['someForm'].controller.show()
	 */
	public String js_getTitleText()
	{
		return getBaseComponent(false).getText();
	}

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSGraphicalComponent#js_getToolTipText()
	 */
	public String js_getToolTipText()
	{
		return getBaseComponent(false).getToolTipText();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Field#getValuelistID()
	 * 
	 * @sample
	 * var vlist = solutionModel.newValueList('options', JSValueList.CUSTOM_VALUES);
	 * vlist.customValues = "one\ntwo\nthree\nfour";
	 * var cmb = form.newField('my_table_options', JSField.COMBOBOX, 10, 100, 100, 20);
	 * cmb.valuelist = vlist;
	 */
	public JSValueList js_getValuelist()
	{
		ValueList vl = application.getFlattenedSolution().getValueList(getBaseComponent(false).getValuelistID());
		if (vl != null)
		{
			return new JSValueList(vl, application, false);
		}
		return null;
	}

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSGraphicalComponent#js_getVerticalAlignment()
	 */
	@Deprecated
	public int js_getVerticalAlignment()
	{
		return getBaseComponent(false).getVerticalAlignment();
	}

	public void js_setDataProviderID(String arg)
	{
		getBaseComponent(true).setDataProviderID(arg);
	}

	public void js_setDisplaysTags(boolean arg)
	{
		getBaseComponent(true).setDisplaysTags(arg);
	}

	public void js_setDisplayType(int arg)
	{
		getBaseComponent(true).setDisplayType(arg);
	}

	public void js_setEditable(boolean arg)
	{
		getBaseComponent(true).setEditable(arg);
	}

	public void js_setFormat(String arg)
	{
		getBaseComponent(true).setFormat(arg);
	}

	public void js_setHorizontalAlignment(int arg)
	{
		getBaseComponent(true).setHorizontalAlignment(arg);
	}

	public void js_setMargin(String margin)
	{
		getBaseComponent(true).setMargin(PersistHelper.createInsets(margin));
	}

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSGraphicalComponent#js_setOnActionMethod(Function)
	 */
	@Deprecated
	public void js_setOnActionMethod(Function function)
	{
		ScriptMethod scriptMethod = JSForm.getScriptMethod(function, application.getFlattenedSolution());
		if (scriptMethod != null)
		{
			getBaseComponent(true).setOnActionMethodID(scriptMethod.getID());
		}
		else
		{
			getBaseComponent(true).setOnActionMethodID(0);
		}
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.JSField#js_getOnDataChange() 
	 */
	@Deprecated
	public void js_setOnDataChangeMethod(Function function)
	{
		ScriptMethod scriptMethod = JSForm.getScriptMethod(function, application.getFlattenedSolution());
		if (scriptMethod != null)
		{
			getBaseComponent(true).setOnDataChangeMethodID(scriptMethod.getID());
		}
		else
		{
			getBaseComponent(true).setOnDataChangeMethodID(0);
		}
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.JSField#js_getOnFocusGained()
	 */
	@Deprecated
	public void js_setOnFocusGainedMethod(Function function)
	{
		ScriptMethod scriptMethod = JSForm.getScriptMethod(function, application.getFlattenedSolution());
		if (scriptMethod != null)
		{
			getBaseComponent(true).setOnFocusGainedMethodID(scriptMethod.getID());
		}
		else
		{
			getBaseComponent(true).setOnFocusGainedMethodID(0);
		}
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.JSField#js_getOnFocusLost() 
	 */
	@Deprecated
	public void js_setOnFocusLostMethod(Function function)
	{
		ScriptMethod scriptMethod = JSForm.getScriptMethod(function, application.getFlattenedSolution());
		if (scriptMethod != null)
		{
			getBaseComponent(true).setOnFocusLostMethodID(scriptMethod.getID());
		}
		else
		{
			getBaseComponent(true).setOnFocusLostMethodID(0);
		}
	}

	public void js_setScrollbars(int arg)
	{
		getBaseComponent(true).setScrollbars(arg);
	}

	public void js_setSelectOnEnter(boolean arg)
	{
		getBaseComponent(true).setSelectOnEnter(arg);
	}

	public void js_setTabSeq(int arg)
	{
		getBaseComponent(true).setTabSeq(arg);
	}

	@Deprecated
	public void js_setText(String arg)
	{
		getBaseComponent(true).setText(arg);
	}

	public void js_setTitleText(String arg)
	{
		getBaseComponent(true).setText(arg);
	}

	public void js_setToolTipText(String arg)
	{
		getBaseComponent(true).setToolTipText(arg);
	}

	public void js_setValuelist(JSValueList valuelist)
	{
		if (valuelist == null)
		{
			getBaseComponent(true).setValuelistID(0);
		}
		else
		{
			getBaseComponent(true).setValuelistID(valuelist.getValueList().getID());
		}
	}

	@Deprecated
	public void js_setVerticalAlignment(int arg)
	{
		getBaseComponent(true).setVerticalAlignment(arg);
	}


	public void js_setOnAction(JSMethod method)
	{
		setEventHandler(application, StaticContentSpecLoader.PROPERTY_ONACTIONMETHODID.getPropertyName(), method);
	}

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSGraphicalComponent#js_getOnAction()
	 */
	public JSMethod js_getOnAction()
	{
		return getEventHandler(application, StaticContentSpecLoader.PROPERTY_ONACTIONMETHODID.getPropertyName());
	}

	public void js_setOnDataChange(JSMethod method)
	{
		setEventHandler(application, StaticContentSpecLoader.PROPERTY_ONDATACHANGEMETHODID.getPropertyName(), method);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Field#getOnDataChangeMethodID()
	 * 
	 * @sample
	 * var form = solutionModel.newForm('someForm', 'example_data', 'parent_table', 'null', false, 620, 300);
	 * var onDataChangeMethod = form.newFormMethod('function onDataChange(oldValue, newValue, event) { application.output("Data changed from " + oldValue + " to " + newValue + " at " + event.getTimestamp()); }');
	 * var field = form.newField('parent_table_text', JSField.TEXT_FIELD, 10, 10, 100, 20);
	 * field.onDataChange = onDataChangeMethod;
	 * forms['someForm'].controller.show();
	 */
	public JSMethod js_getOnDataChange()
	{
		return getEventHandler(application, StaticContentSpecLoader.PROPERTY_ONDATACHANGEMETHODID.getPropertyName());
	}

	public void js_setOnRender(JSMethod method)
	{
		setEventHandler(application, StaticContentSpecLoader.PROPERTY_ONRENDERMETHODID.getPropertyName(), method);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Field#getOnRenderMethodID()
	 * 
	 * @sample
	 * field.onRender = form.newFormMethod('function onRender(event) { event.getElement().bgcolor = \'#00ff00\' }');
	 */
	public JSMethod js_getOnRender()
	{
		return getEventHandler(application, StaticContentSpecLoader.PROPERTY_ONRENDERMETHODID.getPropertyName());
	}


	public void js_setOnRightClick(JSMethod method)
	{
		setEventHandler(application, StaticContentSpecLoader.PROPERTY_ONRIGHTCLICKMETHODID.getPropertyName(), method);
	}

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSGraphicalComponent#js_getOnRightClick()
	 */
	public JSMethod js_getOnRightClick()
	{
		return getEventHandler(application, StaticContentSpecLoader.PROPERTY_ONRIGHTCLICKMETHODID.getPropertyName());
	}

	public void js_setOnFocusGained(JSMethod method)
	{
		setEventHandler(application, StaticContentSpecLoader.PROPERTY_ONFOCUSGAINEDMETHODID.getPropertyName(), method);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Field#getOnFocusGainedMethodID()
	 * 
	 * @sample
	 * var form = solutionModel.newForm('someForm', 'example_data', 'parent_table', 'null', false, 620, 300);
	 * var onFocusLostMethod = form.newFormMethod('function onFocusLost(event) { application.output("Focus lost at " + event.getTimestamp()); }');
	 * var onFocusGainedMethod = form.newFormMethod('function onFocusGained(event) { application.output("Focus gained at " + event.getTimestamp()); }');
	 * var field = form.newField('parent_table_text', JSField.TEXT_FIELD, 10, 10, 100, 20);
	 * field.onFocusGained = onFocusGainedMethod;
	 * field.onFocusLost = onFocusLostMethod;
	 * forms['someForm'].controller.show()
	 */
	public JSMethod js_getOnFocusGained()
	{
		return getEventHandler(application, StaticContentSpecLoader.PROPERTY_ONFOCUSGAINEDMETHODID.getPropertyName());
	}

	public void js_setOnFocusLost(JSMethod method)
	{
		setEventHandler(application, StaticContentSpecLoader.PROPERTY_ONFOCUSLOSTMETHODID.getPropertyName(), method);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Field#getOnFocusLostMethodID()
	 * 
	 * @sampleas js_getOnFocusGained()
	 */
	public JSMethod js_getOnFocusLost()
	{
		return getEventHandler(application, StaticContentSpecLoader.PROPERTY_ONFOCUSLOSTMETHODID.getPropertyName());
	}


	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.JSComponent#toString()
	 */
	@Override
	public String toString()
	{
		return "Field: " + getBaseComponent(false).getName(); //$NON-NLS-1$
	}
}