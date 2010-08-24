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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.border.Border;
import javax.swing.text.Document;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.AjaxCallDecorator;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.servoy.j2db.FormManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IMainContainer;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.component.INullableAware;
import com.servoy.j2db.dataprocessing.CustomValueList;
import com.servoy.j2db.dataprocessing.FindState;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IEditListener;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.dataprocessing.JSDataSet;
import com.servoy.j2db.dataprocessing.ValueListFactory;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.scripting.JSEvent;
import com.servoy.j2db.server.headlessclient.MainPage;
import com.servoy.j2db.server.headlessclient.ServoyForm;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.IProviderStylePropertyChanges;
import com.servoy.j2db.ui.IScriptBaseMethods;
import com.servoy.j2db.ui.IScriptCheckBoxMethods;
import com.servoy.j2db.ui.IStylePropertyChanges;
import com.servoy.j2db.ui.ISupportWebBounds;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.ISupplyFocusChildren;
import com.servoy.j2db.util.ITagResolver;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.Utils;

/**
 * Represents a checkbox field in the webbrowser.
 * 
 * @author jcompagner
 */
public class WebDataCheckBox extends MarkupContainer implements IFieldComponent, IDisplayData, IProviderStylePropertyChanges, IScriptCheckBoxMethods,
	INullableAware, ISupportWebBounds, IRightClickListener, ISupplyFocusChildren<Component>
{
	private static final long serialVersionUID = 1L;
	private static final String NO_COLOR = "NO_COLOR";

	protected IValueList onValue;
	private final WebEventExecutor eventExecutor;

	private Cursor cursor;
	private boolean needEntireState;
	private int maxLength;
	private Insets margin;
	private int horizontalAlignment;
	private Point loc;
	private String inputId;
	private boolean allowNull = true;
	private String tmpForeground = NO_COLOR;

	private final ChangesRecorder jsChangeRecorder = new ChangesRecorder(TemplateGenerator.DEFAULT_FIELD_BORDER_SIZE, TemplateGenerator.DEFAULT_FIELD_PADDING);

	private final IApplication application;
	private final MyCheckBox box;

	public WebDataCheckBox(IApplication application, String id, String text, IValueList list)
	{
		this(application, id, text);
		onValue = list;
	}


	public WebDataCheckBox(IApplication application, String id, String text)
	{
		super(id);
		this.application = application;
		box = new MyCheckBox("check_" + id); //$NON-NLS-1$

		boolean useAJAX = Utils.getAsBoolean(application.getRuntimeProperties().get("useAJAX"));
		eventExecutor = new WebEventExecutor(box, useAJAX);
		setOutputMarkupPlaceholderTag(true);

		add(box);
		add(new Label("text_" + id, "")); //$NON-NLS-1$ //$NON-NLS-2$
		text = Text.processTags(text, null);
		setText(text);

		box.add(new FocusIfInvalidAttributeModifier(box));
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

	public IStylePropertyChanges getStylePropertyChanges()
	{
		return jsChangeRecorder;
	}


	/**
	 * @see wicket.Component#getMarkupId()
	 */
	@Override
	public String getMarkupId()
	{
		return WebComponentSpecialIdMaker.getSpecialIdIfAppropriate(this);
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
		//we cannot add focus in web on checkbox, onAction runs at same time as onfocus, making onfocus useless, and giving unprdicted behaviour
		//eventExecutor.setEnterCmds(ids);
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

	public void setValueValid(boolean valid, Object oldVal)
	{
		application.getRuntimeProperties().put(IServiceProvider.RT_LASTFIELDVALIDATIONFAILED_FLAG, Boolean.valueOf(!valid));
		isValueValid = valid;
		if (!isValueValid)
		{
			previousValidValue = oldVal;
			requestFocus();
			if (tmpForeground == NO_COLOR)
			{
				tmpForeground = js_getFgcolor();
				js_setFgcolor("red");
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

	public Component[] getFocusChildren()
	{
		Component component = null;
		if (onValue != null) component = this;
		else component = box;
		return new Component[] { component };
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
					Object value = oldVal;
					if (previousValidValue != null) value = oldVal;
					eventExecutor.fireChangeCommand(value, newVal, false, WebDataCheckBox.this);

					//if change cmd is not succeeded also don't call action cmd?
					if (isValueValid)
					{
						eventExecutor.fireActionCommand(false, WebDataCheckBox.this);
					}
				}

				public Component getComponent()
				{
					return WebDataCheckBox.this;
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

	private boolean wasEditable;
	private boolean oldEditState;

	public void setValidationEnabled(boolean b)
	{
		if (eventExecutor.getValidationEnabled() == b) return;

		if (b)
		{
			editable = wasEditable;
			editState = oldEditState;
		}
		else
		{
			wasEditable = editable;
			oldEditState = editState;
			setEditable(true);
		}
		eventExecutor.setValidationEnabled(b);
	}

	public void setSelectOnEnter(boolean b)
	{
		eventExecutor.setSelectOnEnter(b);
	}

	//_____________________________________________________________

	/**
	 * @see wicket.markup.html.WebMarkupContainer#onRender()
	 */
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
			Object oe = js_getClientProperty("ajax.enabled");
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
	 * @see com.servoy.j2db.ui.IFieldComponent#setNeedEntireState(boolean)
	 */
	public void setNeedEntireState(boolean needEntireState)
	{
		this.needEntireState = needEntireState;
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IDisplayData#needEntireState()
	 */
	public boolean needEntireState()
	{
		return needEntireState;
	}

	/**
	 * @see com.servoy.j2db.ui.IFieldComponent#setMaxLength(int)
	 */
	public void setMaxLength(int maxLength)
	{
		this.maxLength = maxLength;
	}

	private boolean editable;

	public void setEditable(boolean b)
	{
		editState = b;
		editable = b;
	}

	/**
	 * @see com.servoy.j2db.ui.IFieldComponent#setMargin(java.awt.Insets)
	 */
	public void setMargin(Insets margin)
	{
		this.margin = margin;
	}

	/**
	 * @see com.servoy.j2db.ui.IFieldComponent#setHorizontalAlignment(int)
	 */
	public void setHorizontalAlignment(int horizontalAlignment)
	{
		this.horizontalAlignment = horizontalAlignment;
	}

	/**
	 * @see com.servoy.j2db.ui.IComponent#setCursor(java.awt.Cursor)
	 */
	public void setCursor(Cursor cursor)
	{
		this.cursor = cursor;
	}

	public Object getValueObject()
	{
		//ignore.. pull models
		return null;
	}

	public void setAllowNull(boolean allowNull)
	{
		this.allowNull = allowNull;
	}

	public boolean getAllowNull()
	{
		return allowNull;
	}

	// if this check box is linked to a integer non-null table column,
	// we must change null to "0" by default (as the user sees the check-box
	// unchecked - and when he tries to save he will not have null content problems)
	private void changeNullTo0IfNeeded()
	{
		IRecordInternal record = null;
		IModel model = getInnermostModel();
		if (model instanceof RecordItemModel)
		{
			record = ((RecordItemModel)model).getRecord();
		}
		if ((!allowNull) && record != null && !(record instanceof FindState) && record.getValue(dataProviderID) == null && dataType == IColumnTypes.INTEGER &&
			record.startEditing())
		{
			record.setValue(dataProviderID, new Integer(0));
		}
	}

	public void setValueObject(Object value)
	{
		jsChangeRecorder.testChanged(this, value);
		changeNullTo0IfNeeded(); // for record view
		if (jsChangeRecorder.isChanged())
		{
			// this component is going to update it's contents, without the user changing the
			// components contents; so remove invalid state if necessary
			setValueValid(true, null);
		}
	}

	public void setText(String txt)
	{
		Component c = get("text_" + getId()); //$NON-NLS-1$
		if (txt == null || txt.trim().length() == 0)
		{
			c.setVisible(false);
		}
		else
		{
			c.setDefaultModelObject(txt);
			c.setVisible(true);
		}
	}

	public boolean needEditListner()
	{
		return false;
	}

	public void addEditListener(IEditListener l)
	{
		//ignore
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IDisplayData#getDocument()
	 */
	public Document getDocument()
	{
		return null;
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

	@Override
	public String toString()
	{
		return js_getElementType() + "(web)[name:" + js_getName() + ",x:" + js_getLocationX() + ",y:" + js_getLocationY() + ",width:" + js_getWidth() + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
			",height:" + js_getHeight() + ",value:" + getDefaultModelObject() + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	private final class MyCheckBox extends CheckBox implements IResolveObject, IDisplayData
	{
		private static final long serialVersionUID = 1L;

		private MyCheckBox(String id)
		{
			super(id);
			setOutputMarkupPlaceholderTag(true);
			add(new AttributeModifier("disabled", true, new Model<String>() //$NON-NLS-1$
				{
					private static final long serialVersionUID = 1L;

					@Override
					public String getObject()
					{
						return ((WebDataCheckBox.this.isEnabled() && !WebDataCheckBox.this.isReadOnly()) ? AttributeModifier.VALUELESS_ATTRIBUTE_REMOVE
							: AttributeModifier.VALUELESS_ATTRIBUTE_ADD);
					}
				}));
			add(new StyleAppendingModifier(new Model<String>()
			{
				private static final long serialVersionUID = 1L;

				@Override
				public String getObject()
				{
					return "width:14px;height:14px;";
				}
			}));
		}

		/**
		 * @see wicket.Component#isEnabled()
		 */
		@Override
		public boolean isEnabled()
		{
			return WebDataCheckBox.this.isEnabled();
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

		/*
		 * _____________________________________________________________ Methods for model object resolve
		 */
		public Object resolveDisplayValue(Object realVal)
		{
			if (onValue != null && onValue.getSize() >= 1)
			{
				Object real = onValue.getRealElementAt(0);
				if (real == null)
				{
					return new Boolean(realVal == null);
				}
				else
				{
					return new Boolean(real.equals(realVal));
				}
			}
			else
			{
				if (realVal instanceof Number)
				{
					return new Boolean(((Number)realVal).intValue() >= 1);
				}
				else if (realVal == null)
				{
					return new Boolean(false);
				}
				else
				{
					return new Boolean("1".equals(realVal.toString()));
				}
			}
		}

		@Override
		protected void onBeforeRender()
		{
			super.onBeforeRender();
			changeNullTo0IfNeeded(); // for table/list view
		}

		protected ITagResolver resolver;

		public void setTagResolver(ITagResolver resolver)
		{
			this.resolver = resolver;
		}

		public Object resolveRealValue(Object displayVal)
		{
			if (onValue != null && onValue.getSize() >= 1)
			{
				return (Utils.getAsBoolean(displayVal) ? onValue.getRealElementAt(0) : null);
			}
			else
			{
//		TODO this seems not possible in web and we don't have the previousRealValue			
//					// if value == null and still nothing selected return null (no data change)
//					if (previousRealValue == null && !Utils.getAsBoolean(displayVal))
//					{
//						return null;
//					}
				return new Integer((Utils.getAsBoolean(displayVal) ? 1 : 0));
			}
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplayData#getValueObject()
		 */
		public Object getValueObject()
		{
			return WebDataCheckBox.this.getValueObject();
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplayData#setValueObject(java.lang.Object)
		 */
		public void setValueObject(Object data)
		{
			WebDataCheckBox.this.setValueObject(data);
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplayData#needEditListner()
		 */
		public boolean needEditListner()
		{
			return WebDataCheckBox.this.needEditListner();
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplayData#needEntireState()
		 */
		public boolean needEntireState()
		{
			return WebDataCheckBox.this.needEntireState();
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplayData#setNeedEntireState(boolean)
		 */
		public void setNeedEntireState(boolean needEntireState)
		{
			WebDataCheckBox.this.setNeedEntireState(needEntireState);
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplayData#addEditListener(com.servoy.j2db.dataprocessing.IEditListener)
		 */
		public void addEditListener(IEditListener editListener)
		{
			WebDataCheckBox.this.addEditListener(editListener);
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplayData#getDataProviderID()
		 */
		public String getDataProviderID()
		{
			return WebDataCheckBox.this.getDataProviderID();
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplayData#setDataProviderID(java.lang.String)
		 */
		public void setDataProviderID(String dataProviderID)
		{
			WebDataCheckBox.this.setDataProviderID(dataProviderID);
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplayData#getDocument()
		 */
		public Document getDocument()
		{
			return WebDataCheckBox.this.getDocument();
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplayData#getFormat()
		 */
		public String getFormat()
		{
			return WebDataCheckBox.this.getFormat();
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplayData#notifyLastNewValueWasChange(java.lang.Object, java.lang.Object)
		 */
		public void notifyLastNewValueWasChange(Object oldVal, Object newVal)
		{
			WebDataCheckBox.this.notifyLastNewValueWasChange(oldVal, newVal);
		}

		public boolean isValueValid()
		{
			return WebDataCheckBox.this.isValueValid();
		}

		public void setValueValid(boolean valid, Object oldVal)
		{
			WebDataCheckBox.this.setValueValid(valid, oldVal);
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplay#setValidationEnabled(boolean)
		 */
		public void setValidationEnabled(boolean b)
		{
			WebDataCheckBox.this.setValidationEnabled(b);
		}

		/**
		 * @see com.servoy.j2db.dataprocessing.IDisplay#stopUIEditing(boolean)
		 */
		public boolean stopUIEditing(boolean looseFocus)
		{
			return WebDataCheckBox.this.stopUIEditing(looseFocus);
		}

		public boolean isReadOnly()
		{
			return WebDataCheckBox.this.isReadOnly();
		}

		@Override
		protected void onRender(final MarkupStream markupStream)
		{
			super.onRender(markupStream);

			IModel model = WebDataCheckBox.this.getInnermostModel();

			if (model instanceof RecordItemModel)
			{
				((RecordItemModel)model).updateRenderedValue(this);
			}
		}
	}


	/*
	 * jsmethods---------------------------------------------------
	 */
	public String js_getValueListName()
	{
		if (onValue != null)
		{
			return onValue.getName();
		}
		return null;
	}

	public void js_setValueListItems(Object value)
	{
		if (onValue != null && (value instanceof JSDataSet || value instanceof IDataSet))
		{
			String name = onValue.getName();
			ValueList valuelist = application.getFlattenedSolution().getValueList(name);
			if (valuelist != null && valuelist.getValueListType() == ValueList.CUSTOM_VALUES)
			{
				String format = null;
				int type = 0;
				if (onValue instanceof CustomValueList)
				{
					format = ((CustomValueList)onValue).getFormat();
					type = ((CustomValueList)onValue).getType();
				}
				IValueList newVl = ValueListFactory.fillRealValueList(application, valuelist, ValueList.CUSTOM_VALUES, format, type, value);
				onValue = newVl;
				getStylePropertyChanges().setChanged();
			}
		}
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
			((MainPage)currentContainer).componentToFocus(box);
		}
	}

	private int dataType;
	private String format;

	public void setFormat(int type, String format)
	{
		this.dataType = type;
		this.format = format;
	}

	public String getFormat()
	{
		return format;
	}

	public int getDataType()
	{
		return dataType;
	}

	/*
	 * readonly---------------------------------------------------
	 */
	private boolean editState;

	public void js_setReadOnly(boolean b)
	{
		if (b && !editable) return;
		if (b)
		{
			setEditable(false);
			editState = true;
		}
		else
		{
			setEditable(editState);
		}
		jsChangeRecorder.setChanged();
	}

	public boolean js_isReadOnly()
	{
		return isReadOnly();
	}

	public boolean isReadOnly()
	{
		return !editable;
	}


	/*
	 * dataprovider---------------------------------------------------
	 */
	private String dataProviderID;

	public String js_getDataProviderID()
	{
		return dataProviderID;
	}

	public void setDataProviderID(String dataProviderID)
	{
		this.dataProviderID = dataProviderID;
	}

	public String getDataProviderID()
	{
		return dataProviderID;
	}

	/**
	 * @see com.servoy.j2db.ui.IScriptBaseMethods#js_getElementType()
	 */
	public String js_getElementType()
	{
		return "CHECK";
	}

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
			tooltip = null;
		}
		this.tooltip = tooltip;
	}

	public void js_setToolTipText(String tooltip)
	{
		setToolTipText(tooltip);
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

	private List<ILabel> labels;

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
				if (label.getName() != null && !"".equals(label.getName()) && !label.getName().startsWith(ComponentFactory.WEB_ID_PREFIX))
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

	public void setComponentEnabled(final boolean b)
	{
		if (accessible)
		{
			editState = b;
			super.setEnabled(b);
			jsChangeRecorder.setChanged();
			if (labels != null)
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
		jsChangeRecorder.setSize(width, height, border, margin, 0);
	}

	public Rectangle getWebBounds()
	{
		Dimension d = jsChangeRecorder.calculateWebSize(size.width, size.height, border, margin, 0, null);
		return new Rectangle(location, d);
	}

	/**
	 * @see com.servoy.j2db.ui.ISupportWebBounds#getPaddingAndBorder()
	 */
	public Insets getPaddingAndBorder()
	{
		return jsChangeRecorder.getPaddingAndBorder(size.height, border, margin, 0, null);
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
		add(new ServoyAjaxEventBehavior("oncontextmenu") //$NON-NLS-1$
		{
			@Override
			protected void onEvent(AjaxRequestTarget target)
			{
				eventExecutor.onEvent(JSEvent.EventType.rightClick, target, WebDataCheckBox.this,
					Utils.getAsInteger(RequestCycle.get().getRequest().getParameter(IEventExecutor.MODIFIERS_PARAMETER)));
			}

			@Override
			public boolean isEnabled(Component component)
			{
				if (super.isEnabled(component))
				{
					Object oe = WebDataCheckBox.this.js_getClientProperty("ajax.enabled");
					if (oe != null) return Utils.getAsBoolean(oe);
					return true;
				}
				return false;
			}

			// We need to return false, otherwise the context menu of the browser is displayed.
			@Override
			protected IAjaxCallDecorator getAjaxCallDecorator()
			{
				return new AjaxCallDecorator()
				{
					@Override
					public CharSequence decorateScript(CharSequence script)
					{
						return script + " return false;";
					}
				};
			}
		});
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

	// Searches for a parent form, up the hierarchy of controls in the page.
	private Form getForm()
	{
		Component c = this;
		while ((c != null) && !(c instanceof Form))
			c = c.getParent();
		return (Form)c;
	}
}
