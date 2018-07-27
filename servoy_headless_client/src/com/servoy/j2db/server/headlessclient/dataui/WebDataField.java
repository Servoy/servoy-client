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
import java.util.List;
import java.util.Locale;

import javax.swing.border.Border;
import javax.swing.event.ListDataListener;
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
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.IModelComparator;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.string.AppendingStringBuffer;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.validation.IValidationError;

import com.servoy.base.util.ITagResolver;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IMainContainer;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.dataprocessing.CustomValueList;
import com.servoy.j2db.dataprocessing.GlobalMethodValueList;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IEditListener;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.ISupportTextSetup;
import com.servoy.j2db.scripting.JSEvent;
import com.servoy.j2db.server.headlessclient.MainPage;
import com.servoy.j2db.server.headlessclient.mask.MaskBehavior;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.IFormattingComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.IProviderStylePropertyChanges;
import com.servoy.j2db.ui.IStylePropertyChanges;
import com.servoy.j2db.ui.ISupportInputSelection;
import com.servoy.j2db.ui.ISupportOnRender;
import com.servoy.j2db.ui.ISupportSimulateBounds;
import com.servoy.j2db.ui.ISupportSimulateBoundsProvider;
import com.servoy.j2db.ui.ISupportSpecialClientProperty;
import com.servoy.j2db.ui.ISupportValueList;
import com.servoy.j2db.ui.ISupportWebBounds;
import com.servoy.j2db.ui.scripting.AbstractRuntimeField;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.FormatParser;
import com.servoy.j2db.util.FormatParser.ParsedFormat;
import com.servoy.j2db.util.IDestroyable;
import com.servoy.j2db.util.RoundHalfUpDecimalFormat;
import com.servoy.j2db.util.StateFullSimpleDateFormat;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.UIUtils;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.text.FixedMaskFormatter;

/**
 * Represents a text (single line) field in the webbrowser.
 *
 * @author jcompagner
 */
public class WebDataField extends TextField<Object>
	implements IFieldComponent, IDisplayData, IProviderStylePropertyChanges, ISupportWebBounds, IRightClickListener, ISupportValueList, ISupportInputSelection,
	ISupportSpecialClientProperty, IFormattingComponent, ISupportSimulateBoundsProvider, IDestroyable, ISupportOnRender
{
	/**
	 * @author jcompagner
	 *
	 */
	private final class NumberValidationModel extends AbstractReadOnlyModel<CharSequence>
	{
		private final boolean decimal;

		NumberValidationModel(boolean decimal)
		{
			this.decimal = decimal;

		}

		@SuppressWarnings("nls")
		@Override
		public CharSequence getObject()
		{
			DecimalFormatSymbols dfs = RoundHalfUpDecimalFormat.getDecimalFormatSymbols(application.getLocale());
			AppendingStringBuffer sb = new AppendingStringBuffer();
			sb.append("return Servoy.Validation.numbersonly(event,");
			sb.append(decimal);
			sb.append(",'");
			sb.append(dfs.getDecimalSeparator());
			sb.append("','");
			sb.append(dfs.getGroupingSeparator());
			sb.append("','");
			sb.append(dfs.getCurrencySymbol());
			sb.append("','");
			sb.append(dfs.getPercent());
			sb.append("',this,");
			sb.append(parsedFormat.getMaxLength() == null ? -1 : parsedFormat.getMaxLength().intValue());
			sb.append(");");
			return sb;
		}
	}

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

//	private Cursor cursor;
	private boolean needEntireState;
	private int dataType;
	private Insets margin;
	private int horizontalAlignment;

	private final WebEventExecutor eventExecutor;
	private String inputId;

	protected ParsedFormat parsedFormat = FormatParser.parseFormatProperty(null);

//	private String completeFormat;
//	private String editFormat;
//	protected String displayFormat;
	FocusIfInvalidAttributeModifier focusIfInvalidAttributeModifier;
	protected IConverter converter;
	protected IValueList list;
	private final AbstractRuntimeField<IFieldComponent> scriptable;
	protected final IApplication application;

	public WebDataField(IApplication application, AbstractRuntimeField<IFieldComponent> scriptable, String id, IValueList list)
	{
		this(application, scriptable, id);
		this.list = list;
		this.horizontalAlignment = ISupportTextSetup.LEFT;
	}

	public WebDataField(IApplication application, AbstractRuntimeField<IFieldComponent> scriptable, String id)
	{
		this(application, scriptable, id, (IComponent)null);
	}

	/**
	 * @param id
	 */
	public WebDataField(final IApplication application, final AbstractRuntimeField<IFieldComponent> scriptable, String id, final IComponent enclosingComponent)
	{
		super(id);
		this.horizontalAlignment = ISupportTextSetup.LEFT;
		this.application = application;
		boolean useAJAX = Utils.getAsBoolean(application.getRuntimeProperties().get("useAJAX")); //$NON-NLS-1$
		eventExecutor = new WebEventExecutor(this, useAJAX)
		{
			@Override
			protected Object getSource(Object display)
			{
				return enclosingComponent != null ? enclosingComponent : super.getSource(display);
			}
		};
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

		add(new AttributeModifier("placeholder", true, new Model<String>() //$NON-NLS-1$
		{
			private static final long serialVersionUID = 1L;

			@Override
			public String getObject()
			{
				return application.getI18NMessageIfPrefixed(scriptable.getPlaceholderText());
			}
		})
		{
			@Override
			public boolean isEnabled(Component component)
			{
				return super.isEnabled(component) && scriptable.getPlaceholderText() != null;
			}
		});
		add(new SimpleAttributeModifier("autocomplete", "false"));

		focusIfInvalidAttributeModifier = new FocusIfInvalidAttributeModifier(this);
		add(focusIfInvalidAttributeModifier);
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
		this.scriptable = scriptable;
	}

	public void destroy()
	{
		if (list != null) list.deregister();
	}

	public final AbstractRuntimeField<IFieldComponent> getScriptObject()
	{
		return scriptable;
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
			if (valid != isValueValid)
			{
				getStylePropertyChanges().setChanged();
			}
			requestFocusToComponent();
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
				tmpForeground = scriptable.getFgcolor();
				scriptable.setFgcolor("red"); //$NON-NLS-1$
			}
		}
		else
		{
			previousValidValue = null;
			if (tmpForeground != NO_COLOR)
			{
				scriptable.setFgcolor(tmpForeground);
				tmpForeground = NO_COLOR;
			}
		}
	}

	public void notifyLastNewValueWasChange(final Object oldVal, final Object newVal)
	{
		if (eventExecutor.hasChangeCmd())
		{
			application.invokeLater(new Runnable()
			{
				public void run()
				{
					WebEventExecutor.setSelectedIndex(WebDataField.this, null, IEventExecutor.MODIFIERS_UNSPECIFIED);
					eventExecutor.fireChangeCommand(previousValidValue == null ? oldVal : previousValidValue, newVal, false, WebDataField.this);
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
		}, ((oldVal == null) && (newVal != null)));
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
			// web cell based view - reinitializing a component onBeforeRender can result in readonly being set back to true while in find; when validation is reset to false, we must restore find state for the control
			if (!validation && !editable && !Boolean.TRUE.equals(application.getClientProperty(IApplication.LEAVE_FIELDS_READONLY_IN_FIND_MODE)))
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
			if (!Boolean.TRUE.equals(application.getClientProperty(IApplication.LEAVE_FIELDS_READONLY_IN_FIND_MODE)))
			{
				setEditable(true);
			}
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
		if (eventExecutor.getValidationEnabled() && isEnabled() && isEditable())
		{
			container.getHeaderResponse().renderOnDomReadyJavascript("Servoy.Validation.detachDisplayEditFormat('" + getMarkupId() + "');"); //$NON-NLS-1$ //$NON-NLS-2$
			testFormats(new ITestFormatsCallback()
			{
				public void differentEditAndDisplay(String displayValue, String editValue)
				{
					container.getHeaderResponse().renderOnDomReadyJavascript(
						"Servoy.Validation.attachDisplayEditFormat('" + getMarkupId() + "', '" + displayValue + "','" + editValue + "');"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				}
			}, false);
		}
		if (isEnabled() && isEditable() && Column.mapToDefaultType(dataType) == IColumnTypes.NUMBER)
		{
			DecimalFormatSymbols dfs = RoundHalfUpDecimalFormat.getDecimalFormatSymbols(application.getLocale());
			container.getHeaderResponse().renderOnDomReadyJavascript("$(function(){$(\"#" + getMarkupId() + //$NON-NLS-1$
				"\").numpadDecSeparator({useRegionalSettings: false,separator: '" + dfs.getDecimalSeparator() + "'});});"); //$NON-NLS-1$
		}
		if (scriptable.getPlaceholderText() != null)
		{
			container.getHeaderResponse().renderOnLoadJavascript("$('#" + getMarkupId() + "').placeholder();");
		}
	}

	@SuppressWarnings("nls")
	private void testFormats(ITestFormatsCallback callback, boolean isChangedFromNullValue)
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
					else if (value == null)
					{
						callback.differentEditAndDisplay("", "");
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
				if (((value == null) || isChangedFromNullValue) && editable)
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
		getStylePropertyChanges().setRendered();
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
			Object oe = scriptable.getClientProperty("ajax.enabled"); //$NON-NLS-1$
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
		return scriptable.getChangesRecorder();
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
	private int maxLength = -1;

	public void setMaxLength(int maxLength)
	{
		this.maxLength = maxLength;
		addMaxLengthBehavior(maxLength);
	}

	public void addMaxLengthBehavior(int maxLength)
	{
		if (maxLengthBehavior != null) remove(maxLengthBehavior);
		maxLengthBehavior = null;
		if (maxLength > 0)
		{
			maxLengthBehavior = new FindModeDisabledSimpleAttributeModifier(getEventExecutor(), "maxlength", Integer.toString(maxLength).intern()); //$NON-NLS-1$
			add(maxLengthBehavior);
		}
	}

	public static IConverter getTextConverter(final ParsedFormat fp, final Locale l, final String name, final String dataProviderID)
	{
		if (fp.isAllUpperCase())
		{
			return new IConverter()
			{
				public String convertToString(Object value, Locale locale)
				{
					if (value == null) return "";
					return value.toString().toUpperCase(l);
				}

				public Object convertToObject(String value, Locale locale)
				{
					if (value == null) return null;
					return value.toUpperCase(l);
				}
			};
		}
		if (fp.isAllLowerCase())
		{
			return new IConverter()
			{
				public String convertToString(Object value, Locale locale)
				{
					if (value == null) return "";
					return value.toString().toLowerCase(l);
				}

				public Object convertToObject(String value, Locale locale)
				{
					if (value == null) return null;
					return value.toLowerCase(l);
				}
			};
		}
		if (fp.getDisplayFormat() != null)
		{
			try
			{
				final FixedMaskFormatter displayFormatter = new FixedMaskFormatter(fp.getDisplayFormat());
				displayFormatter.setValueContainsLiteralCharacters(!fp.isRaw());
				if (fp.getPlaceHolderString() != null) displayFormatter.setPlaceholder(fp.getPlaceHolderString());
				else if (fp.getPlaceHolderCharacter() != 0) displayFormatter.setPlaceholderCharacter(fp.getPlaceHolderCharacter());

				return new IConverter()
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
							if (name != null)
							{
								extraMsg = " on component " + name; //$NON-NLS-1$
							}
							extraMsg += " with dataprovider: " + dataProviderID; //$NON-NLS-1$
							throw new ConversionException("Can't convert from string '" + value + "' to object with format: " + fp.getEditFormat() + extraMsg, //$NON-NLS-1$//$NON-NLS-2$
								e).setConverter(this);
						}
					}
				};
			}
			catch (ParseException e)
			{
				Debug.error("format problem: " + fp.getDisplayFormat(), e); //$NON-NLS-1$
				return null;
			}
		}
		return null;
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
		String displayFormat = parsedFormat.getDisplayFormat();
		if (list == null && mappedType == IColumnTypes.TEXT)
		{
			converter = getTextConverter(parsedFormat, getLocale(), getName(), getDataProviderID());
			if (converter == null)
			{
				converter = super.getConverter(cls);
			}
		}
		else if (displayFormat == null && list == null)
		{
			converter = super.getConverter(cls);
		}
		else if (mappedType == IColumnTypes.DATETIME)
		{
			boolean lenient = Boolean.TRUE.equals(
				UIUtils.getUIProperty(this.getScriptObject(), application, IApplication.DATE_FORMATTERS_LENIENT, Boolean.TRUE));
			StateFullSimpleDateFormat displayFormatter = new StateFullSimpleDateFormat(displayFormat, null, application.getLocale(), lenient);
			if (!parsedFormat.isMask() && parsedFormat.getEditFormat() != null)
			{
				StateFullSimpleDateFormat editFormatter = new StateFullSimpleDateFormat(parsedFormat.getEditFormat(), null, application.getLocale(), lenient);
				converter = new FormatConverter(this, eventExecutor, displayFormatter, editFormatter, parsedFormat);
			}
			else
			{
				converter = new FormatConverter(this, eventExecutor, displayFormatter, parsedFormat);
			}
		}
		else if ((mappedType == IColumnTypes.INTEGER || mappedType == IColumnTypes.NUMBER) && (list == null || !list.hasRealValues()))
		{
			int maxLength = parsedFormat.getMaxLength() == null ? -1 : parsedFormat.getMaxLength().intValue();
			// if there is no display format, but the max length is set, then generate a display format.
			if (maxLength != -1 && (displayFormat == null || displayFormat.length() == 0))
			{
				char[] chars = new char[maxLength];
				for (int i = 0; i < chars.length; i++)
					chars[i] = '#';
				displayFormat = new String(chars);
			}
			if (displayFormat != null)
			{
				RoundHalfUpDecimalFormat displayFormatter = new RoundHalfUpDecimalFormat(displayFormat, application.getLocale());
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
		}

		if (list != null)
		{
			if (converter == null && mappedType == IColumnTypes.TEXT && list instanceof GlobalMethodValueList)
			{
				converter = getTextConverter(parsedFormat, getLocale(), getName(), getDataProviderID());
			}
			converter = new ValuelistValueConverter(list, this, converter);
		}

		if (converter == null) converter = super.getConverter(cls);

		return converter;
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

	protected boolean needsFormatOnchange()
	{
		return false;
	}

	@SuppressWarnings("nls")
	public void installFormat(com.servoy.j2db.component.ComponentFormat componentFormat)
	{
		int mappedType = Column.mapToDefaultType(componentFormat.uiType);
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
				if (list == null)
				{
					add(new FindModeDisabledSimpleAttributeModifier(getEventExecutor(), "onkeypress", new NumberValidationModel(true)));
				}
			}
			else if (mappedType == IColumnTypes.INTEGER)
			{
				setType(Integer.class);
				if (list == null)
				{
					add(new FindModeDisabledSimpleAttributeModifier(getEventExecutor(), "onkeypress", new NumberValidationModel(false)));
				}
			}
			else if ((mappedType == IColumnTypes.MEDIA || mappedType == IColumnTypes.TEXT) && list != null) // media and list != null is binary uuid column as real in a valuelist.
			{
				setType(String.class);
			}
		}
		this.dataType = componentFormat.uiType;

		getStylePropertyChanges().setChanged();
		converter = null;
		boolean emptyCustom = (list instanceof CustomValueList) && list.getSize() == 0;
		parsedFormat = componentFormat.parsedFormat;
		if (formatAttributeModifier != null)
		{
			remove(formatAttributeModifier);
			formatAttributeModifier = null;
		}
		if (formatPasteBehavior != null)
		{
			remove(formatPasteBehavior);
			formatPasteBehavior = null;
		}
		if (formatOnChangeBehavior != null)
		{
			remove(formatOnChangeBehavior);
			formatOnChangeBehavior = null;
		}
		addMaxLengthBehavior(-1);
		if (!componentFormat.parsedFormat.isEmpty() && (list == null || (!list.hasRealValues() && !emptyCustom)))
		{
			int maxLength = parsedFormat.getMaxLength() != null ? parsedFormat.getMaxLength().intValue() : 0;
			if (maxLength > 0)
			{
				addMaxLengthBehavior(parsedFormat.getMaxLength().intValue());
			}
			if (parsedFormat.isAllUpperCase())
			{
				formatAttributeModifier = new ReadOnlyAndEnableTestAttributeModifier("onkeypress",
					"return Servoy.Validation.changeCase(this,event,true," + maxLength + ");");
				formatPasteBehavior = new ReadOnlyAndEnableTestAttributeModifier("onpaste",
					"Servoy.Validation.pasteHandler(this, function(el){el.value = el.value.toUpperCase();});");
				if (needsFormatOnchange())
				{
					formatOnChangeBehavior = new AttributeModifier("onchange", true, new Model<String>()
					{
						private static final long serialVersionUID = 1L;

						@Override
						public String getObject()
						{
							return "if (this.value) this.value = this.value.toUpperCase()"; //$NON-NLS-1$
						}
					})
					{
						@Override
						protected String newValue(final String currentValue, final String replacementValue)
						{
							return currentValue + ";" + replacementValue;
						}
					};
				}
			}
			else if (parsedFormat.isAllLowerCase())
			{
				formatAttributeModifier = new ReadOnlyAndEnableTestAttributeModifier("onkeypress",
					"return Servoy.Validation.changeCase(this,event,false," + maxLength + ");");
				formatPasteBehavior = new ReadOnlyAndEnableTestAttributeModifier("onpaste",
					"Servoy.Validation.pasteHandler(this, function(el){el.value = el.value.toLowerCase();});");
				if (needsFormatOnchange())
				{
					formatOnChangeBehavior = new AttributeModifier("onchange", true, new Model<String>()
					{
						private static final long serialVersionUID = 1L;

						@Override
						public String getObject()
						{
							return "if (this.value) this.value = this.value.toLowerCase()"; //$NON-NLS-1$
						}
					})
					{
						@Override
						protected String newValue(final String currentValue, final String replacementValue)
						{
							return currentValue + ";" + replacementValue;
						}
					};
				}
			}
			else if (mappedType == IColumnTypes.DATETIME && parsedFormat.isMask())
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
					("return Servoy.Validation.numbersonly(event, true, '" + dfs.getDecimalSeparator() + "','" + dfs.getGroupingSeparator() + "','" +
						dfs.getCurrencySymbol() + "','" + dfs.getPercent() + "');").intern());
				formatPasteBehavior = new FindModeDisabledSimpleAttributeModifier(getEventExecutor(), "onpaste",
					"Servoy.Validation.pasteHandler(this, function(el){el.value = el.value.replace(/[^0-9\\-\\,\\.\\$\\%]+/,'');});");
			}
			else if (mappedType == IColumnTypes.TEXT && parsedFormat.getDisplayFormat() != null)
			{
				setType(String.class);
				String placeHolder = null;
				if (parsedFormat.getPlaceHolderString() != null) placeHolder = parsedFormat.getPlaceHolderString();
				else if (parsedFormat.getPlaceHolderCharacter() != 0) placeHolder = Character.toString(parsedFormat.getPlaceHolderCharacter());
				formatAttributeModifier = new MaskBehavior(parsedFormat.getDisplayFormat(), placeHolder, this, parsedFormat.getAllowedCharacters());
			}
			if (formatAttributeModifier != null) add(formatAttributeModifier);
			if (formatPasteBehavior != null) add(formatPasteBehavior);
			if (formatOnChangeBehavior != null) add(formatOnChangeBehavior);

		}
		if (maxLength > 0 && maxLengthBehavior == null)
		{
			addMaxLengthBehavior(maxLength);
		}

	}

	public Insets getMargin()
	{
		return margin;
	}

	public void setMargin(Insets margin)
	{
		this.margin = margin;
	}

	public int getHorizontalAlignment()
	{
		return this.horizontalAlignment;
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
//		this.cursor = cursor;
	}

	@Override
	protected String getModelValue()
	{
		if (getDefaultModelObject() == null)
		{
			if (getValueList() != null)
			{
				int index = list.realValueIndexOf(null);
				if (index != -1) return (String)getValueList().getElementAt(index);
			}
			return null;
		}
		else return getDefaultModelObjectAsString();
	}

	public Object getValueObject()
	{
		return getDefaultModelObject();
	}

	public void setValueObject(Object value)
	{
		((ChangesRecorder)getStylePropertyChanges()).testChanged(this, value);
		if (getStylePropertyChanges().isChanged())
		{
			// this component is going to update it's contents, without the user changing the
			// components contents; so remove invalid state if necessary
			setValueValid(true, null);
		}
	}

	public boolean needEditListener()
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
			requestFocusToComponent();
			return false;
		}
		return true;
	}

	public void requestFocusToComponent()
	{
		if (!isReadOnly() && isEnabled())
		{
			// is the current main container always the right one?
			IMainContainer currentContainer = ((FormManager)application.getFormManager()).getCurrentContainer();
			if (currentContainer instanceof MainPage)
			{
				((MainPage)currentContainer).componentToFocus(this);
			}
		}
	}

	@SuppressWarnings("nls")
	public void selectAll()
	{
		Page page = findPage();
		if (page instanceof MainPage)
		{
			((MainPage)page).getPageContributor().addDynamicJavaScript("document.getElementById('" + getMarkupId() + "').select();");
		}
	}

	@SuppressWarnings("nls")
	public void replaceSelectedText(String s)
	{
		Page page = findPage();
		if (page instanceof MainPage)
		{
			((MainPage)page).getPageContributor().addDynamicJavaScript("Servoy.Utils.replaceSelectedText('" + getMarkupId() + "','" + s + "');");
		}
	}

	public String getSelectedText()
	{
		return null;
	}

	public IValueList getValueList()
	{
		return list;
	}

	public void setValueList(IValueList vl)
	{
		this.list = vl;

	}

	public ListDataListener getListener()
	{
		return null;
	}

	/*
	 * readonly/editable---------------------------------------------------
	 */
	private boolean editable;

	public boolean isReadOnly()
	{
		return !editable;
	}

	public boolean isEditable()
	{
		return editable;
	}

	private boolean editState;

	public void setReadOnly(boolean b)
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
	}

	public void setEditable(boolean b)
	{
		editState = b;
		editable = b;
		toggleFocusBehavior();
	}

	protected void toggleFocusBehavior()
	{
		List<FocusIfInvalidAttributeModifier> list = this.getBehaviors(FocusIfInvalidAttributeModifier.class);
		if (isReadOnly() || !isEnabled())
		{
			if (list.size() > 0) remove(focusIfInvalidAttributeModifier);
		}
		else
		{
			if (list.size() == 0) add(focusIfInvalidAttributeModifier);
		}
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

	private String dataProviderID;


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

	public String getTitleText()
	{
		return Text.processTags(titleText, resolver);
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

	public Font getFont()
	{
		return font;
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


	/*
	 * visible---------------------------------------------------
	 */
	public void setComponentVisible(boolean visible)
	{
		if (viewable || !visible)
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
	}

	public void addLabelFor(ILabel label)
	{
		if (labels == null) labels = new ArrayList<ILabel>(3);
		labels.add(label);
	}

	public List<ILabel> getLabelsFor()
	{
		return labels;
	}


	public void setComponentEnabled(final boolean b)
	{
		if (accessible || !b)
		{

			super.setEnabled(b);
			getStylePropertyChanges().setChanged();
			if (labels != null)
			{
				for (int i = 0; i < labels.size(); i++)
				{
					ILabel label = labels.get(i);
					label.setComponentEnabled(b);
				}
			}
			toggleFocusBehavior();
		}
	}

	private boolean accessible = true;

	public void setAccessible(boolean b)
	{
		if (!b) setComponentEnabled(b);
		accessible = b;
	}

	private boolean viewable = true;

	public void setViewable(boolean b)
	{
		if (!b) setComponentVisible(b);
		this.viewable = b;
	}

	public boolean isViewable()
	{
		return viewable;
	}

	/*
	 * location---------------------------------------------------
	 */
	private Point location = new Point(0, 0);

	public int getAbsoluteFormLocationY()
	{
		WebDataRenderer parent = findParent(WebDataRenderer.class);
		if (parent != null)
		{
			return parent.getYOffset() + getLocation().y;
		}
		return getLocation().y;
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

	public void setClientProperty(Object key, Object value)
	{
		if (IApplication.DATE_FORMATTERS_LENIENT.equals(key))
		{
			if (converter instanceof FormatConverter)
			{
				((FormatConverter)converter).setLenient(
					Boolean.TRUE.equals(UIUtils.getUIProperty(this.getScriptObject(), application, IApplication.DATE_FORMATTERS_LENIENT, Boolean.TRUE)));
			}
		}
	}

	/*
	 * size---------------------------------------------------
	 */
	private Dimension size = new Dimension(0, 0);

	private FindModeDisabledSimpleAttributeModifier maxLengthBehavior;

	private IBehavior formatAttributeModifier, formatPasteBehavior, formatOnChangeBehavior;

	public Dimension getSize()
	{
		return size;
	}

	public Rectangle getWebBounds()
	{
		Dimension d = ((ChangesRecorder)getStylePropertyChanges()).calculateWebSize(size.width, size.height, border, margin, 0, null);
		return new Rectangle(location, d);
	}

	/**
	 * @see com.servoy.j2db.ui.ISupportWebBounds#getPaddingAndBorder()
	 */
	public Insets getPaddingAndBorder()
	{
		return ((ChangesRecorder)getStylePropertyChanges()).getPaddingAndBorder(size.height, border, margin, 0, null);
	}

	public Insets getPadding()
	{
		return ((ChangesRecorder)getStylePropertyChanges()).getPadding(border, margin);
	}

	public void setSize(Dimension size)
	{
		this.size = size;
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
		return scriptable.toString("value:" + getDefaultModelObjectAsString()); //$NON-NLS-1$
	}

	@Override
	protected void onBeforeRender()
	{
		super.onBeforeRender();
		fireOnRender(false);
	}

	public void fireOnRender(boolean force)
	{
		if (!isIgnoreOnRender && scriptable != null)
		{
			boolean isFocused = false;
			IMainContainer currentContainer = ((FormManager)application.getFormManager()).getCurrentContainer();
			if (currentContainer instanceof MainPage)
			{
				isFocused = this.equals(((MainPage)currentContainer).getFocusedComponent());
			}
			if (force) scriptable.getRenderEventExecutor().setRenderStateChanged();
			scriptable.getRenderEventExecutor().fireOnRender(isFocused);
		}
	}

	private boolean isIgnoreOnRender;

	public void setIgnoreOnRender(boolean isIgnoreOnRender)
	{
		this.isIgnoreOnRender = isIgnoreOnRender;
	}

	public ISupportSimulateBounds getBoundsProvider()
	{
		return findParent(ISupportSimulateBounds.class);
	}
}
