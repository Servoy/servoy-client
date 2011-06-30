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
package com.servoy.j2db.server.headlessclient.dataui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.border.Border;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.text.Document;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.string.AppendingStringBuffer;
import org.apache.wicket.validation.IValidationError;

import com.servoy.j2db.FormManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IMainContainer;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IDisplayRelatedData;
import com.servoy.j2db.dataprocessing.IEditListener;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.dataprocessing.JSDataSet;
import com.servoy.j2db.dataprocessing.SortColumn;
import com.servoy.j2db.dataprocessing.ValueListFactory;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.scripting.JSEvent;
import com.servoy.j2db.server.headlessclient.MainPage;
import com.servoy.j2db.server.headlessclient.ServoyForm;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.IProviderStylePropertyChanges;
import com.servoy.j2db.ui.IScriptBaseMethods;
import com.servoy.j2db.ui.IScriptDataComboboxMethods;
import com.servoy.j2db.ui.IStylePropertyChanges;
import com.servoy.j2db.ui.ISupportWebBounds;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.FormatParser;
import com.servoy.j2db.util.ITagResolver;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.RoundHalfUpDecimalFormat;
import com.servoy.j2db.util.StateFullSimpleDateFormat;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.Utils;

/**
 * Represents a drop down field in the webbrowser.
 * 
 * @author jcompagner
 */
public class WebDataComboBox extends DropDownChoice implements IFieldComponent, IDisplayData, IScriptDataComboboxMethods, IDisplayRelatedData,
	IProviderStylePropertyChanges, ISupportWebBounds, IRightClickListener
{
	private static final long serialVersionUID = 1L;

	private static final String NO_COLOR = "NO_COLOR"; //$NON-NLS-1$

	private final WebComboModelListModelWrapper list;
	private final WebEventExecutor eventExecutor;

	private int dataType;

	private final FormatParser parsedFormat;
//	private String completeFormat;
//	protected String displayFormat;
	protected IConverter converter;

	private String inputId;
	private Cursor cursor;
	private int maxLength;
	private Insets margin;
	private int horizontalAlignment;
	private final IApplication application;
	private final ChangesRecorder jsChangeRecorder = new ChangesRecorder(TemplateGenerator.DEFAULT_LABEL_PADDING, TemplateGenerator.DEFAULT_LABEL_PADDING);

	private boolean editable = true;
	private boolean readOnly = false;
	private boolean enabledState = true;
	private boolean editState = true;


	private final IValueList valueList;

	public WebDataComboBox(IApplication application, String name, IValueList valueList)
	{
		super(name);
		this.parsedFormat = new FormatParser();
		this.application = application;
		this.valueList = valueList;
		boolean useAJAX = Utils.getAsBoolean(application.getRuntimeProperties().get("useAJAX")); //$NON-NLS-1$
		eventExecutor = new WebEventExecutor(this, useAJAX);
		setOutputMarkupPlaceholderTag(true);

		list = new WebComboModelListModelWrapper(valueList, false)
		{
			private static final long serialVersionUID = 1L;
			private boolean valueInList = true;

			/**
			 * @see com.servoy.j2db.util.ComboModelListModelWrapper#getSize()
			 */
			@Override
			public int size()
			{
				if (!valueInList)
				{
					return super.size() + 1;
				}
				return super.size();
			}

			/**
			 * @see com.servoy.j2db.util.ComboModelListModelWrapper#getElementAt(int)
			 */
			@Override
			public Object getElementAt(int index)
			{
				int idx = index;
				if (!valueInList)
				{
					if (idx == 0)
					{
						if (hasRealValues())
						{
							return null;
						}
						return getRealSelectedItem();
					}
					idx--;
				}
				return super.getElementAt(idx);
			}

			/**
			 * @see com.servoy.j2db.util.ComboModelListModelWrapper#getRealElementAt(int)
			 */
			@Override
			public Object getRealElementAt(int index)
			{
				int idx = index;
				if (!valueInList)
				{
					if (idx == 0)
					{
						return getRealSelectedItem();
					}
					idx--;
				}
				return super.getRealElementAt(idx);
			}

			/**
			 * @see com.servoy.j2db.util.ComboModelListModelWrapper#realValueIndexOf(java.lang.Object)
			 */
			@Override
			public int realValueIndexOf(Object obj)
			{
				if (!valueInList)
				{
					Object realSelectedItem = getRealSelectedItem();
					if (Utils.equalObjects(realSelectedItem, obj))
					{
						return 0;
					}
					int index = super.realValueIndexOf(obj);
					if (index != -1) return index + 1;
					return -1;
				}
				else
				{
					return super.realValueIndexOf(obj);
				}
			}

			/**
			 * @see com.servoy.j2db.util.ComboModelListModelWrapper#indexOf(java.lang.Object)
			 */
			@Override
			public int indexOf(Object o)
			{
				// the web always will call List.indexOf instead of realindex of because o == the real object here.
				int index = listModel.realValueIndexOf(o);
				if (!valueInList)
				{
					index = index + 1;
				}
				if (hideFirstValue) index--;
				return index;
			}

			/**
			 * @see com.servoy.j2db.util.ComboModelListModelWrapper#setSelectedItem(java.lang.Object)
			 */
			@Override
			public void setSelectedItem(Object anObject)
			{
				super.setSelectedItem(anObject);
				Object realSelectedItem = getRealSelectedItem();
				valueInList = (super.realValueIndexOf(realSelectedItem) != -1);
			}
		};

		list.addListDataListener(new ListDataListener()
		{
			public void intervalRemoved(ListDataEvent e)
			{
				if (ignoreChanges) return;
				jsChangeRecorder.setChanged();
				Object obj = list.getSelectedItem();
				if (obj != null) list.setSelectedItem(obj);
			}

			public void intervalAdded(ListDataEvent e)
			{
				if (ignoreChanges) return;
				jsChangeRecorder.setChanged();
				Object obj = list.getSelectedItem();
				if (obj != null) list.setSelectedItem(obj);
			}

			public void contentsChanged(ListDataEvent e)
			{
				if (ignoreChanges) return;
				jsChangeRecorder.setChanged();
				Object obj = list.getSelectedItem();
				if (obj != null) list.setSelectedItem(obj);
			}
		});
		setChoices(list);

		setChoiceRenderer(new WebChoiceRenderer(this, list));

		// select tag can't never be editable. Editable flag shouldn't do anything.
//		add(new AttributeModifier("disabled",true, new Model()
//		{
//			public Object getObject(Component component)
//			{
//				return (editable ? AttributeModifier.VALUELESS_ATTRIBUTE_REMOVE : AttributeModifier.VALUELESS_ATTRIBUTE_ADD) ;
//			}
//		}));

//		add(new AttributeModifier("readonly",true, new Model()
//		{
//			private static final long serialVersionUID = 1L;
//
//			public Object getObject()
//			{
//				return (editable ? AttributeModifier.VALUELESS_ATTRIBUTE_REMOVE : AttributeModifier.VALUELESS_ATTRIBUTE_ADD) ;
//			}
//		}));
		add(new FocusIfInvalidAttributeModifier(this));
		add(StyleAttributeModifierModel.INSTANCE);
		add(TooltipAttributeModifier.INSTANCE);
	}


	/**
	 * @see org.apache.wicket.Component#getLocale()
	 */
	@Override
	public Locale getLocale()
	{
		return application.getLocale();
	}

	@Override
	public void error(IValidationError error)
	{
		super.error(error);
		setValueValid(false, getModelObject());
	}

	@Override
	protected boolean isSelected(Object object, int index, String selected)
	{
		if (object == null && selected == getNoSelectionValue()) return true;
		// WebChoiceRenderer.getRealValue == selected does a toString from the real so object must also do just to string
		return Utils.equalObjects(object != null ? object.toString() : null, selected);
	}

	public IStylePropertyChanges getStylePropertyChanges()
	{
		return jsChangeRecorder;
	}

	@Override
	public String getMarkupId()
	{
		return WebComponentSpecialIdMaker.getSpecialIdIfAppropriate(this);
	}

	@Override
	protected CharSequence getDefaultChoice(Object selected)
	{
		int index = -1;
		if (selected != getNoSelectionValue())
		{
			index = list.realValueIndexOf(selected);
		}
		if (index < -1)
		{
			StringBuilder buffer = new StringBuilder(32);
			buffer.append("\n<option selected=\"selected\""); //$NON-NLS-1$

			// Add body of option tag
			buffer.append(" value=\""); //$NON-NLS-1$
			buffer.append(selected);
			buffer.append("\">").append(getChoiceRenderer().getDisplayValue(selected)).append("</option>"); //$NON-NLS-1$ //$NON-NLS-2$
			return buffer;
		}
		return ""; //$NON-NLS-1$
	}

	@Override
	protected void appendOptionHtml(AppendingStringBuffer buffer, Object choice, int index, String selected)
	{
		Object displayValue = getChoiceRenderer().getDisplayValue(choice);
		if (IValueList.SEPARATOR_VALUE.equals(displayValue))
		{
			// create a separator
			buffer.append("\n<optgroup label=\" \" style=\"border-top: 1px solid gray; margin-top: 7px; margin-bottom: 7px;\"></optgroup>");
		}
		else
		{
			super.appendOptionHtml(buffer, choice, index, selected);
		}
	}

	/*
	 * _____________________________________________________________ Methods for event handling
	 */
	public void addScriptExecuter(IScriptExecuter el)
	{
		eventExecutor.setScriptExecuter(el);
	}

	public IEventExecutor getEventExecutor()
	{
		return eventExecutor;
	}


	public void setEnterCmds(String[] ids, Object[][] args)
	{
		eventExecutor.setEnterCmds(ids, args);
	}

	public void setLeaveCmds(String[] ids, Object[][] args)
	{
		eventExecutor.setLeaveCmds(ids, args);
	}

	public boolean isValueValid()
	{
		return isValueValid;
	}

	private boolean isValueValid = true;
	private Object previousValidValue;
	private String tmpForeground = NO_COLOR;

	public void setValueValid(boolean valid, Object oldVal)
	{
		application.getRuntimeProperties().put(IServiceProvider.RT_LASTFIELDVALIDATIONFAILED_FLAG, Boolean.valueOf(!valid));
		if (!valid)
		{
			getStylePropertyChanges().setChanged();
			requestFocus();
		}
		if (valid == isValueValid)
		{
			return;
		}
		isValueValid = valid;
		if (!isValueValid)
		{
			previousValidValue = oldVal;
			if (tmpForeground == NO_COLOR)
			{
				tmpForeground = js_getFgcolor();
				js_setFgcolor("red"); //$NON-NLS-1$
			}
		}
		else
		{
			previousValidValue = null;
			if (tmpForeground != NO_COLOR)
			{
				js_setFgcolor(tmpForeground);
				tmpForeground = NO_COLOR;
			}
		}
	}

	public void notifyLastNewValueWasChange(final Object oldVal, final Object newVal)
	{
		if (eventExecutor.hasChangeCmd() || eventExecutor.hasActionCmd())
		{
			ServoyForm form = findParent(ServoyForm.class);
			form.addDelayedAction(new ServoyForm.IDelayedAction()
			{
				public void execute()
				{
					eventExecutor.fireChangeCommand(previousValidValue == null ? oldVal : previousValidValue, newVal, false, WebDataComboBox.this);

					//if change cmd is not succeeded also don't call action cmd?
					if (isValueValid)
					{
						eventExecutor.fireActionCommand(false, WebDataComboBox.this);
					}
				}

				public Component getComponent()
				{
					return WebDataComboBox.this;
				}

			});
		}
		else
		{
			setValueValid(true, null);
		}
	}

	public void setChangeCmd(String id, Object[] args)
	{
		eventExecutor.setChangeCmd(id, args);
	}

	public void setActionCmd(String id, Object[] args)
	{
		eventExecutor.setActionCmd(id, args);
	}

	public void setValidationEnabled(boolean b)
	{
		if (eventExecutor.getValidationEnabled() == b) return;
		if (dataProviderID != null && dataProviderID.startsWith(ScriptVariable.GLOBAL_DOT_PREFIX)) return;
		eventExecutor.setValidationEnabled(b);

		if (valueList.getFallbackValueList() != null)
		{
			if (b)
			{
				list.register(valueList);
			}
			else
			{
				list.register(valueList.getFallbackValueList());
			}
		}

		boolean old;
		if (b)
		{
			old = enabledState;
			setComponentEnabled(enabledState, false, readOnly);
			enabledState = old;
			setEditable(editState);
		}
		else if (enabledState && (!isEditable() || !isEnabled()))
		{
			old = editState;
			setEditable(true);
			editState = old;

			old = enabledState;
			setComponentEnabled(true, false, false);
			enabledState = old;
		}
	}

	public void setSelectOnEnter(boolean b)
	{
		eventExecutor.setSelectOnEnter(b);
	}

	//_____________________________________________________________

	@Override
	protected void onRender(final MarkupStream markupStream)
	{
		super.onRender(markupStream);
		jsChangeRecorder.setRendered();
		IModel model = getInnermostModel();
		if (model instanceof RecordItemModel)
		{
			((RecordItemModel)model).updateRenderedValue(this);
		}
	}

	@Override
	protected void onComponentTag(ComponentTag tag)
	{
		super.onComponentTag(tag);

		boolean useAJAX = Utils.getAsBoolean(application.getRuntimeProperties().get("useAJAX")); //$NON-NLS-1$
		if (useAJAX)
		{
			Object oe = js_getClientProperty("ajax.enabled"); //$NON-NLS-1$
			if (oe != null) useAJAX = Utils.getAsBoolean(oe);
		}
		if (!useAJAX)
		{
			Form f = getForm();
			if (f != null)
			{
				if (eventExecutor.hasRightClickCmd())
				{
					CharSequence urlr = urlFor(IRightClickListener.INTERFACE);
					// We need a "return false;" so that the context menu is not displayed in the browser.
					tag.put("oncontextmenu", f.getJsForInterfaceUrl(urlr) + " return false;"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}
	}

	/**
	 * @see wicket.markup.html.form.FormComponent#getInputName()
	 */
	@Override
	public String getInputName()
	{
		if (inputId == null)
		{
			Page page = findPage();
			if (page instanceof MainPage)
			{
				inputId = ((MainPage)page).nextInputNameId();
			}
			else
			{
				return super.getInputName();
			}
		}
		return inputId;
	}

	public void setNeedEntireState(boolean needEntireState)
	{
		this.needEntireState = needEntireState;
	}

	private boolean needEntireState;

	public boolean needEntireState()
	{
		return needEntireState;
	}

	public void setMaxLength(int maxLength)
	{
		this.maxLength = maxLength;
	}

	public void setMargin(Insets margin)
	{
		this.margin = margin;
	}

	public void setHorizontalAlignment(int horizontalAlignment)
	{
		this.horizontalAlignment = horizontalAlignment;
	}

	public void setCursor(Cursor cursor)
	{
		this.cursor = cursor;
	}

	public Object getValueObject()
	{
		//ignore.. pull models
		return null;
	}

	public void setValueObject(Object value)
	{
		jsChangeRecorder.testChanged(this, value);
		refreshValueInList();
		if (jsChangeRecorder.isChanged())
		{
			// this component is going to update it's contents, without the user changing the
			// components contents; so remove invalid state if necessary
			setValueValid(true, null);
		}
	}

	public void refreshValueInList()
	{
		Object modelObject = getModelObject();
		int realValueIndexOf = list.realValueIndexOf(modelObject);
		if (realValueIndexOf == -1)
		{
			if (modelObject == null || "".equals(modelObject) || list.hasRealValues()) //$NON-NLS-1$
			{
				list.setSelectedItem(null);
			}
			else
			{
				list.setSelectedItem(modelObject);
			}
		}
		else
		{
			list.setSelectedItem(list.getElementAt(realValueIndexOf));
		}
	}

	public boolean needEditListner()
	{
		return false;
	}

	public void addEditListener(IEditListener l)
	{
		// TODO Auto-generated method stub
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IDisplayData#getDocument()
	 */
	public Document getDocument()
	{
		return null;
	}


	/*
	 * _____________________________________________________________ Methods for IDisplayRelatedData
	 */
	private boolean ignoreChanges;

	public void setRecord(IRecordInternal state, boolean stopEditing)
	{
		Object selectedItem = list.getSelectedItem();
		boolean listContentChanged = false;
		try
		{
			ignoreChanges = true;
			Object[] oldListValue = list.toArray();
			list.fill(state);
			listContentChanged = !list.compareTo(oldListValue);
		}
		finally
		{
			ignoreChanges = false;
		}
		if (listContentChanged || !Utils.equalObjects(list.getSelectedItem(), selectedItem))
		{
			getStylePropertyChanges().setChanged();
		}
	}

	public String getSelectedRelationName()
	{
		if (relationName == null && list != null)
		{
			relationName = list.getRelationName();
		}
		return relationName;
	}

	private String relationName = null;

	public String[] getAllRelationNames()
	{
		String selectedRelationName = getSelectedRelationName();
		if (selectedRelationName == null)
		{
			return new String[0];
		}
		else
		{
			return new String[] { selectedRelationName };
		}
	}

	public boolean stopUIEditing(boolean looseFocus)
	{
		if (!isValueValid)
		{
			requestFocus();
			return false;
		}
		return true;
	}

	public void notifyVisible(boolean b, List<Runnable> invokeLaterRunnables)
	{
		//ignore
	}

	public void destroy()
	{
		list.deregister();
	}

	public List<SortColumn> getDefaultSort()
	{
		return null;
	}


	public void js_requestFocus(Object[] vargs)
	{
		if (vargs != null && vargs.length >= 1 && !Utils.getAsBoolean(vargs[0]))
		{
			eventExecutor.skipNextFocusGain();
		}
		requestFocus();
	}

	public void requestFocus()
	{
		// is the current container always the right one...
		IMainContainer currentContainer = ((FormManager)application.getFormManager()).getCurrentContainer();
		if (currentContainer instanceof MainPage)
		{
			((MainPage)currentContainer).componentToFocus(this);
		}
	}

	public String js_getValueListName()
	{
		if (list != null)
		{
			return list.getName();
		}
		return null;
	}

	public void js_setValueListItems(Object value)
	{
		if (list != null && (value instanceof JSDataSet || value instanceof IDataSet))
		{
			String vlName = list.getName();
			ValueList valuelist = application.getFlattenedSolution().getValueList(vlName);
			if (valuelist != null && valuelist.getValueListType() == ValueList.CUSTOM_VALUES)
			{
				String format = list.getFormat();
				int type = list.getValueType();
				IValueList newVl = ValueListFactory.fillRealValueList(application, valuelist, ValueList.CUSTOM_VALUES, format, type, value);
				list.register(newVl);
				getStylePropertyChanges().setChanged();
			}
		}
	}

	@Override
	public void updateModel()
	{
		Object input = getConvertedInput();
		if (input instanceof Date)
		{
			IConverter c = getConverter(Date.class);
			setModelObject(c.convertToObject(c.convertToString(input, getLocale()), getLocale()));
		}
		else
		{
			setModelObject(input);
		}
	}

	@Override
	public IConverter getConverter(Class cls)
	{
		if (converter != null) return converter;

		if (dataType == IColumnTypes.DATETIME)
		{
			StateFullSimpleDateFormat displayFormatter = new StateFullSimpleDateFormat(parsedFormat.getDisplayFormat(), /* getClientTimeZone() */null,
				application.getLocale(), true);
			converter = new FormatConverter(this, eventExecutor, displayFormatter, parsedFormat);
		}
		else if (dataType == IColumnTypes.INTEGER || dataType == IColumnTypes.NUMBER)
		{
			RoundHalfUpDecimalFormat displayFormatter = new RoundHalfUpDecimalFormat(parsedFormat.getDisplayFormat(), application.getLocale());
			converter = new FormatConverter(this, eventExecutor, displayFormatter, parsedFormat);
		}
		else
		{
			return super.getConverter(cls);
		}
		return converter;
	}

	/*
	 * format---------------------------------------------------
	 */
	public void setFormat(int type, String format)
	{
		this.dataType = type;

		jsChangeRecorder.setChanged();
		converter = null;
		if (format != null && format.length() != 0)
		{
			parsedFormat.setFormat(format);
			if (parsedFormat.hasEditFormat())
			{
				Debug.log("WARNING Display and Edit formats are not used in browser comboboxes. Such browser controls do not support editing"); //$NON-NLS-1$
			}
		}
	}

	public String getFormat()
	{
		return parsedFormat.getFormat();
	}

	public void js_setFormat(String format)
	{
		this.setFormat(dataType, application.getI18NMessageIfPrefixed(format));
	}

	public String js_getFormat()
	{
		return this.parsedFormat.getFormat();
	}

	private List<ILabel> labels;

	public int getDataType()
	{
		return dataType;
	}


	/*
	 * visible---------------------------------------------------
	 */
	public void setComponentVisible(boolean visible)
	{
		setVisible(visible);
		if (labels != null)
		{
			for (int i = 0; i < labels.size(); i++)
			{
				ILabel label = labels.get(i);
				label.setComponentVisible(visible);
			}
		}
	}

	public boolean js_isVisible()
	{
		return isVisible();
	}

	public void js_setVisible(boolean visible)
	{
		setVisible(visible);
		jsChangeRecorder.setVisible(visible);
		if (labels != null)
		{
			for (int i = 0; i < labels.size(); i++)
			{
				ILabel label = labels.get(i);
				if (label instanceof IScriptBaseMethods)
				{
					((IScriptBaseMethods)label).js_setVisible(visible);
				}
				else
				{
					label.setComponentVisible(visible);
				}
			}
		}
	}

	public void addLabelFor(ILabel label)
	{
		if (labels == null) labels = new ArrayList<ILabel>(3);
		labels.add(label);
	}

	public String[] js_getLabelForElementNames()
	{
		if (labels != null)
		{
			List<String> al = new ArrayList<String>(labels.size());
			for (int i = 0; i < labels.size(); i++)
			{
				ILabel label = labels.get(i);
				if (label.getName() != null && !"".equals(label.getName()) && !label.getName().startsWith(ComponentFactory.WEB_ID_PREFIX)) //$NON-NLS-1$
				{
					al.add(label.getName());
				}
			}
			return al.toArray(new String[al.size()]);
		}
		return new String[0];
	}


	/*
	 * enabled---------------------------------------------------
	 */
	public void js_setEnabled(final boolean b)
	{
		setComponentEnabled(b);
	}

	public void setComponentEnabled(boolean b)
	{
		setComponentEnabled(b, true, readOnly);
	}

	public void setComponentEnabled(boolean b, boolean setLabels, boolean readOnly)
	{
		if (accessible)
		{
			enabledState = b;
			super.setEnabled(b && !readOnly);
			jsChangeRecorder.setChanged();
			if (labels != null && setLabels)
			{
				for (int i = 0; i < labels.size(); i++)
				{
					ILabel label = labels.get(i);
					label.setComponentEnabled(b);
				}
			}
		}
	}

	public boolean js_isEnabled()
	{
		return isEnabled();
	}

	private boolean accessible = true;

	public void setAccessible(boolean b)
	{
		if (!b) setComponentEnabled(b);
		accessible = b;
	}


	/*
	 * readonly/editable---------------------------------------------------
	 */
	public boolean isReadOnly()
	{
		return !isEnabled();
	}

	public boolean js_isReadOnly()
	{
		return isReadOnly();
	}

	public void js_setReadOnly(boolean b)
	{
		if (b == readOnly) return;
		readOnly = b;

		if (b)
		{
			if (isEnabled())
			{
				boolean old;
				if (isEditable())
				{
					old = editState;
					setEditable(false);
					editState = old;
				}
				old = enabledState;
				setComponentEnabled(false, false, readOnly);
				enabledState = old;
			}
		}
		else
		{
			setComponentEnabled(enabledState, false, readOnly);
			setEditable(editState);
		}
	}

	public boolean js_isEditable()
	{
		return isEditable();
	}

	public void js_setEditable(boolean b)
	{
		setEditable(b);
	}

	public boolean isEditable()
	{
		return editable;
	}

	public void setEditable(boolean b)
	{
		// never allow combos with real values to be editable
		editState = b && (!list.hasRealValues());
		editable = b && (!list.hasRealValues());
	}


	/*
	 * dataprovider---------------------------------------------------
	 */
	public void setDataProviderID(String dataProviderID)
	{
		this.dataProviderID = dataProviderID;
		list.setDataProviderID(dataProviderID);
	}

	public String getDataProviderID()
	{
		return dataProviderID;
	}

	public String js_getDataProviderID()
	{
		return dataProviderID;
	}

	private String dataProviderID;


	/*
	 * name---------------------------------------------------
	 */
	public String js_getName()
	{
		String jsName = getName();
		if (jsName != null && jsName.startsWith(ComponentFactory.WEB_ID_PREFIX)) jsName = null;
		return jsName;
	}

	public void setName(String n)
	{
		name = n;
	}

	private String name;

	public String getName()
	{
		return name;
	}

	public String js_getElementType()
	{
		return "COMBOBOX"; //$NON-NLS-1$
	}


	/*
	 * border---------------------------------------------------
	 */
	private Border border;

	public void setBorder(Border border)
	{
		this.border = border;
	}

	public Border getBorder()
	{
		return border;
	}


	/*
	 * opaque---------------------------------------------------
	 */
	public void setOpaque(boolean opaque)
	{
		this.opaque = opaque;
	}

	private boolean opaque;

	public boolean js_isTransparent()
	{
		return !opaque;
	}

	public void js_setTransparent(boolean b)
	{
		opaque = !b;
		jsChangeRecorder.setTransparent(b);
	}

	public boolean isOpaque()
	{
		return opaque;
	}


	/*
	 * titleText---------------------------------------------------
	 */

	private String titleText = null;

	public void setTitleText(String title)
	{
		this.titleText = title;
	}

	public String js_getTitleText()
	{
		return Text.processTags(titleText, resolver);
	}

	/*
	 * tooltip---------------------------------------------------
	 */
	public String js_getToolTipText()
	{
		return tooltip;
	}

	private String tooltip;

	public void setToolTipText(String tooltip)
	{
		if (Utils.stringIsEmpty(tooltip))
		{
			this.tooltip = null;
		}
		else
		{
			this.tooltip = tooltip;
		}
	}

	public void js_setToolTipText(String tooltp)
	{
		setToolTipText(tooltp);
		jsChangeRecorder.setChanged();
	}

	protected ITagResolver resolver;

	public void setTagResolver(ITagResolver resolver)
	{
		this.resolver = resolver;
	}

	/**
	 * @see com.servoy.j2db.ui.IComponent#getToolTipText()
	 */
	public String getToolTipText()
	{
		if (tooltip != null && getInnermostModel() instanceof RecordItemModel)
		{
			return Text.processTags(tooltip, resolver);
		}
		return tooltip;
	}

	/*
	 * font---------------------------------------------------
	 */
	public void setFont(Font font)
	{
		this.font = font;
	}

	private Font font;

	public void js_setFont(String spec)
	{
		font = PersistHelper.createFont(spec);
		jsChangeRecorder.setFont(spec);
	}

	public Font getFont()
	{
		return font;
	}


	/*
	 * bgcolor---------------------------------------------------
	 */
	public String js_getBgcolor()
	{
		return PersistHelper.createColorString(background);
	}

	public void js_setBgcolor(String bgcolor)
	{
		background = PersistHelper.createColor(bgcolor);
		jsChangeRecorder.setBgcolor(bgcolor);
	}

	private Color background;

	public void setBackground(Color cbg)
	{
		this.background = cbg;
	}

	public Color getBackground()
	{
		return background;
	}


	/*
	 * fgcolor---------------------------------------------------
	 */
	public String js_getFgcolor()
	{
		return PersistHelper.createColorString(foreground);
	}

	public void js_setFgcolor(String fgcolor)
	{
		foreground = PersistHelper.createColor(fgcolor);
		jsChangeRecorder.setFgcolor(fgcolor);
	}

	private Color foreground;

	public void setForeground(Color cfg)
	{
		this.foreground = cfg;
	}

	public Color getForeground()
	{
		return foreground;
	}

	public void js_setBorder(String spec)
	{
		setBorder(ComponentFactoryHelper.createBorder(spec));
		jsChangeRecorder.setBorder(spec);
	}

	/*
	 * location---------------------------------------------------
	 */
	private Point location = new Point(0, 0);

	public int js_getLocationX()
	{
		return getLocation().x;
	}

	public int js_getLocationY()
	{
		return getLocation().y;
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptBaseMethods#js_getAbsoluteFormLocationY()
	 */
	public int js_getAbsoluteFormLocationY()
	{
		WebDataRenderer parent = findParent(WebDataRenderer.class);
		if (parent != null)
		{
			return parent.getYOffset() + getLocation().y;
		}
		return getLocation().y;
	}

	public void js_setLocation(int x, int y)
	{
		location = new Point(x, y);
		jsChangeRecorder.setLocation(x, y);
	}

	public void setLocation(Point location)
	{
		this.location = location;
	}

	public Point getLocation()
	{
		return location;
	}

	/*
	 * client properties for ui---------------------------------------------------
	 */

	public void js_putClientProperty(Object key, Object value)
	{
		if (clientProperties == null)
		{
			clientProperties = new HashMap<Object, Object>();
		}
		clientProperties.put(key, value);
	}

	private Map<Object, Object> clientProperties;

	public Object js_getClientProperty(Object key)
	{
		if (clientProperties == null) return null;
		return clientProperties.get(key);
	}

	/*
	 * size---------------------------------------------------
	 */
	private Dimension size = new Dimension(0, 0);

	public Dimension getSize()
	{
		return size;
	}

	public void js_setSize(int width, int height)
	{
		size = new Dimension(width, height);
		jsChangeRecorder.setSize(width, height, null, null, 0);
	}

	public Rectangle getWebBounds()
	{
		Dimension d = jsChangeRecorder.calculateWebSize(size.width, size.height, null, null, 0, null);
		return new Rectangle(location, d);
	}

	/**
	 * @see com.servoy.j2db.ui.ISupportWebBounds#getPaddingAndBorder()
	 */
	public Insets getPaddingAndBorder()
	{
		return jsChangeRecorder.getPaddingAndBorder(size.height, null, null, 0, null);
	}


	public void setSize(Dimension size)
	{
		this.size = size;
	}

	public int js_getWidth()
	{
		return size.width;
	}

	public int js_getHeight()
	{
		return size.height;
	}

	public void setRightClickCommand(String rightClickCmd, Object[] args)
	{
		eventExecutor.setRightClickCmd(rightClickCmd, args);
	}

	public void onRightClick()
	{
		Form f = getForm();
		if (f != null)
		{
			// If form validation fails, we don't execute the method.
			if (f.process()) eventExecutor.onEvent(JSEvent.EventType.rightClick, null, this, IEventExecutor.MODIFIERS_UNSPECIFIED);
		}
	}

	@Override
	public String toString()
	{
		return js_getElementType() + "(web)[name:" + js_getName() + ",x:" + js_getLocationX() + ",y:" + js_getLocationY() + ",width:" + js_getWidth() + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
			",height:" + js_getHeight() + ",value:" + getDefaultModelObjectAsString() + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}
