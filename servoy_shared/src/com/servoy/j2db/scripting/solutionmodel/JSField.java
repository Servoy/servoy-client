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

import java.awt.Point;
import java.util.Iterator;

import org.mozilla.javascript.Function;
import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

import com.servoy.base.persistence.IMobileProperties;
import com.servoy.base.solutionmodel.IBaseSMMethod;
import com.servoy.base.solutionmodel.IBaseSMValueList;
import com.servoy.base.solutionmodel.mobile.IMobileSMField;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.solutionmodel.ISMField;
import com.servoy.j2db.solutionmodel.ISMMethod;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

// Documented via JSFieldWithConstants
// JSFieldWithConstants was introduced to prevent the duplication of constants like JSRadios.PASSWORD == JSField.PASSWORD
@Deprecated
public class JSField extends JSComponent<Field> implements ISMField, IMobileSMField
{
	private final IApplication application;

	// support constants
	public JSField()
	{
		this(null, null, null, false);
	}

	public JSField(IJSParent< ? > parent, Field field, IApplication application, boolean isNew)
	{
		super(parent, field, isNew);
		this.application = application;
	}

	public static JSField createField(IJSParent< ? > parent, Field field, IApplication application, boolean isNew)
	{
		if (field == null)
		{
			return null;
		}

		return application.getScriptEngine().getSolutionModifier().createField(parent, field, isNew);
	}

	/**
	 * @clonedesc com.servoy.base.persistence.IBaseFieldCommon#getDataProviderID()
	 *
	 * @sample
	 * // Normally the dataprovider is specified when a component is created.
	 * var field = form.newField('parent_table_text', JSField.TEXT_FIELD, 10, 40, 100, 20);
	 * // But it can be modified later if needed.
	 * field.dataProviderID = 'parent_table_id';
	 */
	@JSGetter
	public String getDataProviderID()
	{
		return getBaseComponent(false).getDataProviderID();
	}

	@JSSetter
	public void setDataProviderID(String arg)
	{
		getBaseComponent(true).setDataProviderID(arg);
	}

	/**
	 * @clonedesc com.servoy.base.persistence.IBaseGraphicalComponentCommon#getDisplaysTags()
	 *
	 * @sample
	 * var field = form.newField('parent_table_text', JSField.TEXT_FIELD, 10, 40, 100, 20);
	 * field.displaysTags = true;
	 */

	@JSGetter
	public boolean getDisplaysTags()
	{
		return getBaseComponent(false).getDisplaysTags();
	}

	@JSSetter
	public void setDisplaysTags(boolean arg)
	{
		getBaseComponent(true).setDisplaysTags(arg);
	}

	/**
	 * @clonedesc com.servoy.base.persistence.IBaseFieldCommon#getDisplayType()
	 *
	 * @sample
	 * // The display type is specified when the field is created.
	 * var cal = form.newField('my_table_date', JSField.CALENDAR, 10, 10, 100, 20);
	 * // But it can be changed if needed.
	 * cal.dataProviderID = 'my_table_text';
	 * cal.displayType = JSField.TEXT_FIELD;
	 */
	@JSGetter
	public int getDisplayType()
	{
		return getBaseComponent(false).getDisplayType();
	}

	@JSSetter
	public void setDisplayType(int arg)
	{
		getBaseComponent(true).setDisplayType(arg);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Field#getEditable()
	 *
	 * @sample
	 * var field = form.newField('my_table_text', JSField.TEXT_FIELD, 10, 10, 100, 20);
	 * field.editable = false;
	 */
	@JSGetter
	public boolean getEditable()
	{
		return getBaseComponent(false).getEditable();
	}

	@JSSetter
	public void setEditable(boolean arg)
	{
		getBaseComponent(true).setEditable(arg);
	}

	/**
	 * @clonedesc com.servoy.base.persistence.IBaseFieldCommon#getFormat()
	 *
	 * @sample
	 * var field = form.newField('my_table_number', JSField.TEXT_FIELD, 10, 10, 100, 20);
	 * field.format = '$#.00';
	 */
	@JSGetter
	public String getFormat()
	{
		return getBaseComponent(false).getFormat();
	}

	@JSSetter
	public void setFormat(String arg)
	{
		getBaseComponent(true).setFormat(arg);
	}

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSGraphicalComponent#getHorizontalAlignment()
	 */
	@JSGetter
	public int getHorizontalAlignment()
	{
		return getBaseComponent(false).getHorizontalAlignment();
	}

	@JSSetter
	public void setHorizontalAlignment(int arg)
	{
		getBaseComponent(true).setHorizontalAlignment(arg);
	}

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSGraphicalComponent#getMargin()
	 */
	@JSGetter
	public String getMargin()
	{
		return PersistHelper.createInsetsString(getBaseComponent(false).getMargin());
	}

	@JSSetter
	public void setMargin(String margin)
	{
		getBaseComponent(true).setMargin(PersistHelper.createInsets(margin));
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
	@JSGetter
	public int getScrollbars()
	{
		return getBaseComponent(false).getScrollbars();
	}

	@JSSetter
	public void setScrollbars(int arg)
	{
		getBaseComponent(true).setScrollbars(arg);
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
	@JSGetter
	public boolean getSelectOnEnter()
	{
		return getBaseComponent(false).getSelectOnEnter();
	}

	@JSSetter
	public void setSelectOnEnter(boolean arg)
	{
		getBaseComponent(true).setSelectOnEnter(arg);
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
	 * @deprecated  As of release 4.1, replaced by getTitleText()
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
	 * var form = solutionModel.newForm('someForm', 'db:/example_data/my_table', null, false, 640, 480);
	 * var field = form.newField('my_table_number', JSField.TEXT_FIELD, 10, 10, 100, 20);
	 * field.titleText = 'Column Title';
	 * form.view = JSForm.LOCKED_TABLE_VIEW;
	 * forms['someForm'].controller.show()
	 */
	@JSGetter
	public String getTitleText()
	{
		return getBaseComponent(false).getText();
	}

	@JSSetter
	public void setTitleText(String arg)
	{
		getBaseComponent(true).setText(arg);
	}

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSGraphicalComponent#getToolTipText()
	 */
	@JSGetter
	public String getToolTipText()
	{
		return getBaseComponent(false).getToolTipText();
	}

	@JSSetter
	public void setToolTipText(String arg)
	{
		getBaseComponent(true).setToolTipText(arg);
	}

	/**
	 * @clonedesc com.servoy.base.persistence.IBaseFieldCommon#getPlaceholderText()
	 *
	 * @sample
	 * field.placeholderText = 'Search';
	 */
	@JSGetter
	public String getPlaceholderText()
	{
		return getBaseComponent(false).getPlaceholderText();
	}

	@JSSetter
	public void setPlaceholderText(String arg)
	{
		getBaseComponent(true).setPlaceholderText(arg);
	}

	/**
	 * @clonedesc com.servoy.base.persistence.IBaseField#getValuelistID()
	 *
	 * @sample
	 * var vlist = solutionModel.newValueList('options', JSValueList.CUSTOM_VALUES);
	 * vlist.customValues = "one\ntwo\nthree\nfour";
	 * var cmb = form.newField('my_table_options', JSField.COMBOBOX, 10, 100, 100, 20);
	 * cmb.valuelist = vlist;
	 */
	@JSGetter
	public JSValueList getValuelist()
	{
		ValueList vl = application.getFlattenedSolution().getValueList(getBaseComponent(false).getValuelistID());
		if (vl != null)
		{
			return new JSValueList(vl, application, false);
		}
		return null;
	}

	@JSSetter
	public void setValuelist(IBaseSMValueList valuelist)
	{
		if (valuelist == null)
		{
			getBaseComponent(true).setValuelistID(null);
		}
		else
		{
			getBaseComponent(true).setValuelistID(((JSValueList)valuelist).getValueList().getUUID().toString());
		}
	}

	/**
	 * @clonedesc com.servoy.j2db.scripting.solutionmodel.JSGraphicalComponent#getVerticalAlignment()
	 * @sampleas com.servoy.j2db.scripting.solutionmodel.JSGraphicalComponent#getVerticalAlignment()
	 *
	 * @deprecated not used (is ignored) on fields
	 */
	@Deprecated
	public int js_getVerticalAlignment()
	{
		return getBaseComponent(false).getVerticalAlignment();
	}

	/**
	 * @deprecated  As of release 4.1, replaced by setOnAction(JSMethod).
	 */
	@Deprecated
	public void js_setOnActionMethod(Function function)
	{
		ScriptMethod scriptMethod = JSForm.getScriptMethod(function, application.getFlattenedSolution());
		if (scriptMethod != null)
		{
			getBaseComponent(true).setOnActionMethodID(scriptMethod.getUUID().toString());
		}
		else
		{
			getBaseComponent(true).setOnActionMethodID(null);
		}
	}

	/**
	 * @deprecated  As of release 4.1, replaced by setOnDataChange(JSMethod).
	 */
	@Deprecated
	public void js_setOnDataChangeMethod(Function function)
	{
		ScriptMethod scriptMethod = JSForm.getScriptMethod(function, application.getFlattenedSolution());
		if (scriptMethod != null)
		{
			getBaseComponent(true).setOnDataChangeMethodID(scriptMethod.getUUID().toString());
		}
		else
		{
			getBaseComponent(true).setOnDataChangeMethodID(null);
		}
	}

	/**
	 * @deprecated  As of release 4.1, replaced by setOnFocusGained(JSMethod).
	 */
	@Deprecated
	public void js_setOnFocusGainedMethod(Function function)
	{
		ScriptMethod scriptMethod = JSForm.getScriptMethod(function, application.getFlattenedSolution());
		if (scriptMethod != null)
		{
			getBaseComponent(true).setOnFocusGainedMethodID(scriptMethod.getUUID().toString());
		}
		else
		{
			getBaseComponent(true).setOnFocusGainedMethodID(null);
		}
	}

	/**
	 * @deprecated  As of release 4.1, replaced by setOnFocusLost(JSMethod).
	 */
	@Deprecated
	public void js_setOnFocusLostMethod(Function function)
	{
		ScriptMethod scriptMethod = JSForm.getScriptMethod(function, application.getFlattenedSolution());
		if (scriptMethod != null)
		{
			getBaseComponent(true).setOnFocusLostMethodID(scriptMethod.getUUID().toString());
		}
		else
		{
			getBaseComponent(true).setOnFocusLostMethodID(null);
		}
	}

	@Deprecated
	public void js_setText(String arg)
	{
		getBaseComponent(true).setText(arg);
	}

	@Deprecated
	public void js_setVerticalAlignment(int arg)
	{
		getBaseComponent(true).setVerticalAlignment(arg);
	}

	/**
	 * @clonedesc com.servoy.base.persistence.IBaseGraphicalComponent#getOnActionMethodID()
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
	@JSGetter
	public JSMethod getOnAction()
	{
		return getEventHandler(application, StaticContentSpecLoader.PROPERTY_ONACTIONMETHODID);
	}

	@JSSetter
	public void setOnAction(IBaseSMMethod method)
	{
		setEventHandler(application, StaticContentSpecLoader.PROPERTY_ONACTIONMETHODID, (JSMethod)method);
	}

	/**
	 * @clonedesc com.servoy.base.persistence.IBaseField#getOnDataChangeMethodID()
	 *
	 * @sample
	 * var form = solutionModel.newForm('someForm', 'db:/example_data/parent_table', null, false, 620, 300);
	 * var onDataChangeMethod = form.newMethod('function onDataChange(oldValue, newValue, event) { application.output("Data changed from " + oldValue + " to " + newValue + " at " + event.getTimestamp()); }');
	 * var field = form.newField('parent_table_text', JSField.TEXT_FIELD, 10, 10, 100, 20);
	 * field.onDataChange = onDataChangeMethod;
	 * forms['someForm'].controller.show();
	 */
	@JSGetter
	public JSMethod getOnDataChange()
	{
		return getEventHandler(application, StaticContentSpecLoader.PROPERTY_ONDATACHANGEMETHODID);
	}

	@JSSetter
	public void setOnDataChange(IBaseSMMethod method)
	{
		setEventHandler(application, StaticContentSpecLoader.PROPERTY_ONDATACHANGEMETHODID, (JSMethod)method);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Field#getOnRenderMethodID()
	 *
	 * @sample
	 * field.onRender = form.newMethod('function onRender(event) { event.getElement().bgcolor = \'#00ff00\' }');
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
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSGraphicalComponent#getOnRightClick()
	 */
	@JSGetter
	public JSMethod getOnRightClick()
	{
		return getEventHandler(application, StaticContentSpecLoader.PROPERTY_ONRIGHTCLICKMETHODID);
	}

	@JSSetter
	public void setOnRightClick(ISMMethod method)
	{
		setEventHandler(application, StaticContentSpecLoader.PROPERTY_ONRIGHTCLICKMETHODID, (JSMethod)method);
	}

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
	@JSGetter
	public JSMethod getOnFocusGained()
	{
		return getEventHandler(application, StaticContentSpecLoader.PROPERTY_ONFOCUSGAINEDMETHODID);
	}

	@JSSetter
	public void setOnFocusGained(IBaseSMMethod method)
	{
		setEventHandler(application, StaticContentSpecLoader.PROPERTY_ONFOCUSGAINEDMETHODID, (JSMethod)method);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Field#getOnFocusLostMethodID()
	 *
	 * @sampleas getOnFocusGained()
	 */
	@JSGetter
	public JSMethod getOnFocusLost()
	{
		return getEventHandler(application, StaticContentSpecLoader.PROPERTY_ONFOCUSLOSTMETHODID);
	}

	@JSSetter
	public void setOnFocusLost(IBaseSMMethod method)
	{
		setEventHandler(application, StaticContentSpecLoader.PROPERTY_ONFOCUSLOSTMETHODID, (JSMethod)method);
	}

	@Override
	@JSFunction
	public JSTitle getTitle()
	{
		return new JSTitle(getJSParent(), getTitleForComponent(this), false);
	}

	public static GraphicalComponent getTitleForComponent(JSComponent< ? > comp)
	{
		JSForm parentForm = (JSForm)comp.getJSParent();
		String cGroup = comp.getGroupID();
		if (cGroup != null)
		{
			Iterator<GraphicalComponent> comps = parentForm.getSupportChild().getGraphicalComponents();
			for (GraphicalComponent gc : Utils.iterate(comps))
			{
				if (cGroup.equals(gc.getGroupID()))
				{
					// I guess the following if might as well not be; the location thing is for legacy solutions (before the COMPONENT_TITLE existed)
					if (Boolean.TRUE.equals(gc.getCustomMobileProperty(IMobileProperties.COMPONENT_TITLE.propertyName)) || gc.getLocation().y < comp.getY() ||
						(gc.getLocation().y == comp.getY() && gc.getLocation().x < comp.getX()))
					{
						return gc;
					}
				}
			}
		}

		if (cGroup == null)
		{
			comp.setGroupID(cGroup = UUID.randomUUID().toString());
		}

		parentForm.checkModification();
		GraphicalComponent titleLabel;
		try
		{
			titleLabel = parentForm.getSupportChild().createNewGraphicalComponent(new Point(comp.getX(), comp.getY()));
		}
		catch (RepositoryException e)
		{
			// should not happen
			throw new RuntimeException(e);
		}
		titleLabel.setGroupID(cGroup);
		titleLabel.putCustomMobileProperty(IMobileProperties.COMPONENT_TITLE.propertyName, Boolean.TRUE);
		return titleLabel;
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
