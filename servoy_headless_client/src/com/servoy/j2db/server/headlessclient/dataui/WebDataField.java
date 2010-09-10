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
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.border.Border;
import javax.swing.text.Document;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.IModelComparator;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.validation.IValidationError;

import com.servoy.j2db.FormManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IMainContainer;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.CustomValueList;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IEditListener;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.dataprocessing.JSDataSet;
import com.servoy.j2db.dataprocessing.ValueListFactory;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.scripting.JSEvent;
import com.servoy.j2db.server.headlessclient.MainPage;
import com.servoy.j2db.server.headlessclient.ServoyForm;
import com.servoy.j2db.server.headlessclient.mask.MaskBehavior;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.IProviderStylePropertyChanges;
import com.servoy.j2db.ui.IScriptBaseMethods;
import com.servoy.j2db.ui.IScriptFieldMethods;
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
import com.servoy.j2db.util.UIUtils;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.gui.FixedMaskFormatter;

/**
 * Represents a text (single line) field in the webbrowser. 
 * 
 * @author jcompagner
 */
public class WebDataField extends TextField<Object> implements IFieldComponent, IDisplayData, IScriptFieldMethods, IProviderStylePropertyChanges,
	ISupportWebBounds, IRightClickListener
{
	/**
		 * @author jcompagner
		 *
		 */
	private interface ITestFormatsCallback
	{

		/**
		 * @param displayValue
		 * @param editValue
		 */
		void differentEditAndDisplay(String displayValue, String editValue);

	}

	private static final long serialVersionUID = 1L;

	private static final String NO_COLOR = "NO_COLOR"; //$NON-NLS-1$

	private Cursor cursor;
	private boolean needEntireState;
	private int dataType;
	private Insets margin;
	private int horizontalAlignment;

	private final ChangesRecorder jsChangeRecorder = new ChangesRecorder(TemplateGenerator.DEFAULT_FIELD_BORDER_SIZE, TemplateGenerator.DEFAULT_FIELD_PADDING);

	private final WebEventExecutor eventExecutor;
	private String inputId;

	protected final FormatParser parsedFormat;

//	private String completeFormat;
//	private String editFormat;
//	protected String displayFormat;

	protected IConverter converter;
	protected IValueList list;

	protected final IApplication application;

	public WebDataField(IApplication application, String id, IValueList list)
	{
		this(application, id);
		this.list = list;
	}

	/**
	 * @param id
	 */
	public WebDataField(IApplication application, String id)
	{
		super(id);
		this.parsedFormat = new FormatParser();
		this.application = application;
		boolean useAJAX = Utils.getAsBoolean(application.getRuntimeProperties().get("useAJAX")); //$NON-NLS-1$
		eventExecutor = new WebEventExecutor(this, useAJAX);
		setOutputMarkupPlaceholderTag(true);
		setVersioned(false);

		add(new AttributeModifier("readonly", true, new Model<String>() //$NON-NLS-1$
			{
				private static final long serialVersionUID = 1L;

				@Override
				public String getObject()
				{
					return (editable ? AttributeModifier.VALUELESS_ATTRIBUTE_REMOVE : AttributeModifier.VALUELESS_ATTRIBUTE_ADD);
				}
			}));
		add(new FocusIfInvalidAttributeModifier(this));
		add(StyleAttributeModifierModel.INSTANCE);
		add(TooltipAttributeModifier.INSTANCE);
		add(new ConsumeEnterAttributeModifier(this, eventExecutor));
		add(new FilterBackspaceKeyAttributeModifier(new Model<String>()
		{
			private static final long serialVersionUID = 1332637522687352873L;

			@Override
			public String getObject()
			{
				return editable ? null : FilterBackspaceKeyAttributeModifier.SCRIPT;
			}
		}));
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
	protected Object convertValue(String[] value) throws ConversionException
	{
		String tmp = value != null && value.length > 0 ? value[0] : null;
		if (getConvertEmptyInputStringToNull() && Strings.isEmpty(tmp))
		{
			return null;
		}
		return tmp;
	}

	@Override
	public void error(IValidationError error)
	{
		super.error(error);
		setValueValid(false, getModelObject());
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
		if (eventExecutor.hasChangeCmd())
		{
			ServoyForm form = findParent(ServoyForm.class);
			form.addDelayedAction(new ServoyForm.IDelayedAction()
			{
				public void execute()
				{
					Object value = oldVal;
					if (previousValidValue != null) value = oldVal;

					eventExecutor.fireChangeCommand(value, newVal, false, WebDataField.this);
				}

				public Component getComponent()
				{
					return WebDataField.this;
				}

			});
		}
		else
		{
			setValueValid(true, null);
		}
		// if display formats and edit formats are not the same tell the change recorder that the value is changed
		// so that the display format is pushed to the web.
		testFormats(new ITestFormatsCallback()
		{
			public void differentEditAndDisplay(String displayValue, String editValue)
			{
				getStylePropertyChanges().setValueChanged();
			}
		});

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

	public void setValidationEnabled(boolean validation)
	{
		if (eventExecutor.getValidationEnabled() == validation)
		{
			if (!validation && !editable) // web cell based view - reinitializing a component onBeforeRender can result in readonly being set back to true while in find; when validation is reset to false, we must restore find state for the control
			{
				boolean prevEditState = editState;
				setEditable(true);// allow search
				editState = prevEditState;
			}
			return;
		}
		eventExecutor.setValidationEnabled(validation);
		boolean prevEditState = editState;
		if (validation)
		{
			setEditable(wasEditable);
		}
		else
		{
			wasEditable = editable;
			setEditable(true);// allow search
		}
		editState = prevEditState;
	}

	public void setSelectOnEnter(boolean b)
	{
		eventExecutor.setSelectOnEnter(b);
	}

	//_____________________________________________________________

	/**
	 * @see wicket.Component#getComparator()
	 */
	@Override
	public IModelComparator getModelComparator()
	{
		return ComponentValueComparator.COMPARATOR;
	}

	/**
	 * @see org.apache.wicket.Component#renderHead(org.apache.wicket.markup.html.internal.HtmlHeaderContainer)
	 */
	@Override
	public void renderHead(final HtmlHeaderContainer container)
	{
		super.renderHead(container);

		if (eventExecutor.getValidationEnabled())
		{
			testFormats(new ITestFormatsCallback()
			{
				public void differentEditAndDisplay(String displayValue, String editValue)
				{
					container.getHeaderResponse().renderOnDomReadyJavascript(
						"Servoy.Validation.attachDisplayEditFormat('" + getMarkupId() + "', '" + displayValue + "','" + editValue + "')"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				}
			});
		}

	}

	@SuppressWarnings("nls")
	private void testFormats(ITestFormatsCallback callback)
	{
		if (isValueValid())
		{
			if (parsedFormat.hasEditFormat() && getConverter(getType()) instanceof FormatConverter)
			{
				Object value = getDefaultModelObject();
				try
				{
					String displayValue = converter.convertToString(value, application.getLocale());
					String editValue = ((FormatConverter)converter).convertToEditString(value, application.getLocale());
					if (!Utils.equalObjects(displayValue, editValue))
					{
						callback.differentEditAndDisplay(displayValue, editValue);
					}
				}
				catch (Exception e)
				{
					Debug.error(e);
				}
			}
			else if (Column.mapToDefaultType(dataType) == IColumnTypes.DATETIME && !parsedFormat.isMask())
			{
				Object value = getDefaultModelObject();
				if (value == null)
				{
					callback.differentEditAndDisplay("", parsedFormat.getDisplayFormat());
				}
			}
		}
	}

	@Override
	protected void onRender(final MarkupStream markupStream)
	{
		super.onRender(markupStream);
		jsChangeRecorder.setRendered();
		IModel< ? > model = getInnermostModel();

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
			Form< ? > f = getForm();
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

	public IStylePropertyChanges getStylePropertyChanges()
	{
		return jsChangeRecorder;
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
		if (maxLengthBehavior != null) remove(maxLengthBehavior);
		if (maxLength > 0)
		{
			maxLengthBehavior = new FindModeDisabledSimpleAttributeModifier(getEventExecutor(), "maxlength", Integer.toString(maxLength)); //$NON-NLS-1$
			add(maxLengthBehavior);
		}
	}

	/**
	 * @see wicket.Component#getConverter()
	 */
	@SuppressWarnings("nls")
	@Override
	public IConverter getConverter(Class< ? > cls)
	{
		if (converter != null) return converter;

		int mappedType = Column.mapToDefaultType(dataType);
		if (list == null && mappedType == IColumnTypes.TEXT)
		{
			if (parsedFormat.isAllUpperCase())
			{
				converter = new IConverter()
				{
					public String convertToString(Object value, Locale locale)
					{
						if (value == null) return "";
						return value.toString().toUpperCase(getLocale());
					}

					public Object convertToObject(String value, Locale locale)
					{
						if (value == null) return null;
						return value.toUpperCase(getLocale());
					}
				};
			}
			else if (parsedFormat.isAllLowerCase())
			{
				converter = new IConverter()
				{
					public String convertToString(Object value, Locale locale)
					{
						if (value == null) return "";
						return value.toString().toLowerCase(getLocale());
					}

					public Object convertToObject(String value, Locale locale)
					{
						if (value == null) return null;
						return value.toLowerCase(getLocale());
					}
				};
			}
			else if (parsedFormat.getDisplayFormat() != null)
			{
				try
				{
					final FixedMaskFormatter displayFormatter = new FixedMaskFormatter(parsedFormat.getDisplayFormat());
					displayFormatter.setValueContainsLiteralCharacters(!parsedFormat.isRaw());
					if (parsedFormat.getPlaceHolderString() != null) displayFormatter.setPlaceholder(parsedFormat.getPlaceHolderString());
					else if (parsedFormat.getPlaceHolderCharacter() != 0) displayFormatter.setPlaceholderCharacter(parsedFormat.getPlaceHolderCharacter());

					converter = new IConverter()
					{
						public String convertToString(Object value, Locale locale)
						{
							if (value == null) return ""; //$NON-NLS-1$
							try
							{
								return displayFormatter.valueToString(value);
							}
							catch (ParseException e)
							{
								Debug.log(e);
								return value.toString();
							}
						}

						public Object convertToObject(String value, Locale locale)
						{
							if (value == null || "".equals(value.trim())) return null; //$NON-NLS-1$
							try
							{
								return displayFormatter.parse(value);
							}
							catch (Exception e)
							{
								String extraMsg = ""; //$NON-NLS-1$
								if (getName() != null)
								{
									extraMsg = " on component " + getName(); //$NON-NLS-1$
								}
								extraMsg += " with dataprovider: " + getDataProviderID(); //$NON-NLS-1$
								throw new ConversionException(
									"Can't convert from string '" + value + "' to object with format: " + parsedFormat.getEditFormat() + extraMsg, e).setConverter(this); //$NON-NLS-1$ //$NON-NLS-2$
							}
						}
					};
				}
				catch (ParseException e)
				{
					Debug.error("format problem: " + parsedFormat.getDisplayFormat(), e); //$NON-NLS-1$
					return super.getConverter(cls);
				}

			}
			else converter = super.getConverter(cls);
		}
		else if (list != null)
		{
			converter = new ValuelistValueConverter(list, this);
		}
		else if (parsedFormat.getDisplayFormat() == null)
		{
			converter = super.getConverter(cls);
		}
		else if (mappedType == IColumnTypes.DATETIME)
		{
			boolean lenient = Boolean.TRUE.equals(UIUtils.getUIProperty(this, application, IApplication.DATE_FORMATTERS_LENIENT, Boolean.TRUE));
			StateFullSimpleDateFormat displayFormatter = new StateFullSimpleDateFormat(parsedFormat.getDisplayFormat(), null, application.getLocale(), lenient);
			String eFormat = parsedFormat.getEditFormat();
			if (!parsedFormat.isMask() && parsedFormat.getEditFormat() != null) //$NON-NLS-1$
			{
				StateFullSimpleDateFormat editFormatter = new StateFullSimpleDateFormat(eFormat, null, application.getLocale(), lenient);
				converter = new FormatConverter(this, eventExecutor, displayFormatter, editFormatter, parsedFormat);
			}
			else
			{
				converter = new FormatConverter(this, eventExecutor, displayFormatter, parsedFormat);
			}
		}
		else if (mappedType == IColumnTypes.INTEGER || mappedType == IColumnTypes.NUMBER)
		{
			RoundHalfUpDecimalFormat displayFormatter = new RoundHalfUpDecimalFormat(parsedFormat.getDisplayFormat(), application.getLocale());
			if (parsedFormat.getEditFormat() != null)
			{
				RoundHalfUpDecimalFormat editFormatter = new RoundHalfUpDecimalFormat(parsedFormat.getEditFormat(), application.getLocale());
				converter = new FormatConverter(this, eventExecutor, displayFormatter, editFormatter, parsedFormat);
			}
			else
			{
				converter = new FormatConverter(this, eventExecutor, displayFormatter, parsedFormat);
			}
		}
		return converter;
	}

	public int getDataType()
	{
		return this.dataType;
	}

	/**
	 * @see org.apache.wicket.markup.html.form.FormComponent#shouldTrimInput()
	 */
	@Override
	protected boolean shouldTrimInput()
	{
		if (parsedFormat.getDisplayFormat() != null && Column.mapToDefaultType(dataType) == IColumnTypes.TEXT) return false;
		return parsedFormat.getEditFormat() == null || !parsedFormat.isMask();
	}

	@SuppressWarnings("nls")
	public void setFormat(int type, String format)
	{
		int mappedType = Column.mapToDefaultType(type);
		// only add type validators the first time (not when format is set through script)
		if (this.dataType == 0)
		{
			if (mappedType == IColumnTypes.DATETIME)
			{
				setType(Date.class);
			}
			else if (mappedType == IColumnTypes.NUMBER)
			{
				setType(Double.class);
				DecimalFormatSymbols dfs = RoundHalfUpDecimalFormat.getDecimalFormatSymbols(application.getLocale());
				add(new FindModeDisabledSimpleAttributeModifier(getEventExecutor(), "onkeypress", "return Servoy.Validation.numbersonly(event, true, '" +
					dfs.getDecimalSeparator() + "','" + dfs.getGroupingSeparator() + "','" + dfs.getCurrencySymbol() + "','" + dfs.getPercent() + "');"));
			}
			else if (mappedType == IColumnTypes.INTEGER)
			{
				setType(Integer.class);
				DecimalFormatSymbols dfs = RoundHalfUpDecimalFormat.getDecimalFormatSymbols(application.getLocale());
				add(new FindModeDisabledSimpleAttributeModifier(getEventExecutor(), "onkeypress", "return Servoy.Validation.numbersonly(event, false, '" +
					dfs.getDecimalSeparator() + "','" + dfs.getGroupingSeparator() + "','" + dfs.getCurrencySymbol() + "','" + dfs.getPercent() + "');"));
			}
			else if (mappedType == IColumnTypes.TEXT && list != null)
			{
				setType(String.class);
			}
		}
		this.dataType = type;

		jsChangeRecorder.setChanged();
		converter = null;
		if (format != null && format.length() != 0)
		{
			parsedFormat.setFormat(format);
			if (formatAttributeModifier != null) remove(formatAttributeModifier);
			if (parsedFormat.isAllUpperCase())
			{
				formatAttributeModifier = new ReadOnlyAndEnableTestAttributeModifier("onkeypress", "return Servoy.Validation.changeCase(this,event,true);");
			}
			else if (parsedFormat.isAllLowerCase())
			{
				formatAttributeModifier = new ReadOnlyAndEnableTestAttributeModifier("onkeypress", "return Servoy.Validation.changeCase(this,event,false);");
			}
			else if (mappedType == IColumnTypes.DATETIME && parsedFormat.isMask()) //$NON-NLS-1$
			{
				String maskPattern = parsedFormat.getDateMask();
				setType(Date.class);
				String placeHolder = parsedFormat.getDisplayFormat();
				if (parsedFormat.getPlaceHolderString() != null) placeHolder = parsedFormat.getPlaceHolderString();
				else if (parsedFormat.getPlaceHolderCharacter() != 0) placeHolder = Character.toString(parsedFormat.getPlaceHolderCharacter());
				formatAttributeModifier = new MaskBehavior(maskPattern.toString(), placeHolder, this);
			}
			else if (mappedType == IColumnTypes.TEXT && parsedFormat.isNumberValidator())
			{
				setType(String.class);
				DecimalFormatSymbols dfs = RoundHalfUpDecimalFormat.getDecimalFormatSymbols(application.getLocale());
				formatAttributeModifier = new FindModeDisabledSimpleAttributeModifier(getEventExecutor(), "onkeypress",
					"return Servoy.Validation.numbersonly(event, true, '" + dfs.getDecimalSeparator() + "','" + dfs.getGroupingSeparator() + "','" +
						dfs.getCurrencySymbol() + "','" + dfs.getPercent() + "');");
			}
			else if (mappedType == IColumnTypes.TEXT)
			{
				setType(String.class);
				String placeHolder = null;
				if (parsedFormat.getPlaceHolderString() != null) placeHolder = parsedFormat.getPlaceHolderString();
				else if (parsedFormat.getPlaceHolderCharacter() != 0) placeHolder = Character.toString(parsedFormat.getPlaceHolderCharacter());
				formatAttributeModifier = new MaskBehavior(parsedFormat.getDisplayFormat(), placeHolder, this);
			}
			if (formatAttributeModifier != null) add(formatAttributeModifier);

		}
	}

	public void js_setFormat(String format)
	{
		this.setFormat(dataType, application.getI18NMessageIfPrefixed(format));
		jsChangeRecorder.setChanged();
	}

	public String js_getFormat()
	{
		return parsedFormat.getFormat();
	}

	public String getFormat()
	{
		return parsedFormat.getFormat();
	}

	public Insets getMargin()
	{
		return margin;
	}

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
		if (jsChangeRecorder.isChanged())
		{
			// this component is going to update it's contents, without the user changing the
			// components contents; so remove invalid state if necessary
			setValueValid(true, null);
		}
	}

	public boolean needEditListner()
	{
		return false;
	}

	public void addEditListener(IEditListener l)
	{
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IDisplayData#getDocument()
	 */
	public Document getDocument()
	{
		return null;
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IDisplay#stopUIEditing(boolean)
	 */
	public boolean stopUIEditing(boolean looseFocus)
	{
		if (!isValueValid)
		{
			requestFocus();
			return false;
		}
		return true;
	}

	/*
	 * jsmethods---------------------------------------------------
	 */
	public int js_getCaretPosition()
	{
		// TODO ignore for the web??
		return -1;
	}

	public void js_setCaretPosition(int pos)
	{
		// TODO ignore for the web??
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
		// is the current main container always the right one?
		IMainContainer currentContainer = ((FormManager)application.getFormManager()).getCurrentContainer();
		if (currentContainer instanceof MainPage)
		{
			((MainPage)currentContainer).componentToFocus(this);
		}
	}

	public void js_selectAll()
	{
		// TODO Auto-generated method stub
	}

	public void js_replaceSelectedText(String s)
	{
		// TODO ignore in web?
	}

	public String js_getSelectedText()
	{
		// TODO Auto-generated method stub
		return null;
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
			String listName = list.getName();
			ValueList valuelist = application.getFlattenedSolution().getValueList(listName);
			if (valuelist != null && valuelist.getValueListType() == ValueList.CUSTOM_VALUES)
			{
				String format = null;
				int type = 0;
				if (list instanceof CustomValueList)
				{
					format = ((CustomValueList)list).getFormat();
					type = ((CustomValueList)list).getType();
				}
				IValueList newVl = ValueListFactory.fillRealValueList(application, valuelist, ValueList.CUSTOM_VALUES, format, type, value);
				list = newVl;
				getStylePropertyChanges().setChanged();
			}
		}
	}

	/*
	 * readonly/editable---------------------------------------------------
	 */
	private boolean editable;

	public boolean js_isEditable()
	{
		return editable;
	}

	public void js_setEditable(boolean edit)
	{
		this.editable = edit;
	}

	public boolean js_isReadOnly()
	{
		return !editable;
	}

	public boolean isReadOnly()
	{
		return !editable;
	}

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

	public void setEditable(boolean b)
	{
		editState = b;
		editable = b;
	}


	/*
	 * dataprovider---------------------------------------------------
	 */
	public void setDataProviderID(String dataProviderID)
	{
		this.dataProviderID = dataProviderID;
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

	/**
	 * @see com.servoy.j2db.ui.IScriptBaseMethods#js_getElementType()
	 */
	public String js_getElementType()
	{
		return "TEXT_FIELD"; //$NON-NLS-1$
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

	public void js_setToolTipText(String tip)
	{
		setToolTipText(tip);
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

	public void setComponentEnabled(final boolean b)
	{
		if (accessible)
		{
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
		if (IApplication.DATE_FORMATTERS_LENIENT.equals(key))
		{
			if (converter instanceof FormatConverter)
			{
				((FormatConverter)converter).setLenient(Boolean.TRUE.equals(UIUtils.getUIProperty(this, application, IApplication.DATE_FORMATTERS_LENIENT,
					Boolean.TRUE)));
			}
		}
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

	private SimpleAttributeModifier maxLengthBehavior;

	private IBehavior formatAttributeModifier;

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
	}

	public void onRightClick()
	{
		Form< ? > f = getForm();
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
