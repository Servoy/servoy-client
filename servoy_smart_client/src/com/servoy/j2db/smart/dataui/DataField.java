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
package com.servoy.j2db.smart.dataui;


import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JFormattedTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.event.ListDataListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.text.Caret;
import javax.swing.text.DateFormatter;
import javax.swing.text.DefaultFormatter;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import javax.swing.text.InternationalFormatter;
import javax.swing.text.MaskFormatter;
import javax.swing.text.NavigationFilter;
import javax.swing.text.NumberFormatter;

import org.jdesktop.xswingx.PromptSupport;

import com.servoy.base.util.ITagResolver;
import com.servoy.j2db.ControllerUndoManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IFormUIInternal;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.ISmartClientApplication;
import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.dataprocessing.CustomValueList;
import com.servoy.j2db.dataprocessing.GlobalMethodValueList;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IEditListener;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.dataprocessing.ValueFactory.DbIdentValue;
import com.servoy.j2db.dnd.FormDataTransferHandler;
import com.servoy.j2db.dnd.ISupportDragNDropTextTransfer;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.ui.IDataRenderer;
import com.servoy.j2db.ui.IEditProvider;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.IFormattingComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.ISupportCachedLocationAndSize;
import com.servoy.j2db.ui.ISupportEditProvider;
import com.servoy.j2db.ui.ISupportFormatter;
import com.servoy.j2db.ui.ISupportOnRender;
import com.servoy.j2db.ui.ISupportPlaceholderText;
import com.servoy.j2db.ui.ISupportSpecialClientProperty;
import com.servoy.j2db.ui.ISupportValueList;
import com.servoy.j2db.ui.scripting.AbstractRuntimeField;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.FormatParser.ParsedFormat;
import com.servoy.j2db.util.HtmlUtils;
import com.servoy.j2db.util.ISkinnable;
import com.servoy.j2db.util.RoundHalfUpDecimalFormat;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.StateFullSimpleDateFormat;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.UIUtils;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.docvalidator.LengthDocumentValidator;
import com.servoy.j2db.util.docvalidator.LowerCaseDocumentValidator;
import com.servoy.j2db.util.docvalidator.NumberDocumentValidator;
import com.servoy.j2db.util.docvalidator.UpperCaseDocumentValidator;
import com.servoy.j2db.util.docvalidator.ValidatingDocument;
import com.servoy.j2db.util.text.FixedMaskFormatter;
import com.servoy.j2db.util.text.ServoyMaskFormatter;

/**
 * Runtime swing field
 * @author jblok, jcompagner
 */
public class DataField extends JFormattedTextField
	implements IDisplayData, IFieldComponent, ISkinnable, ISupportCachedLocationAndSize, ISupportDragNDropTextTransfer, ISupportEditProvider, ISupportValueList,
	ISupportSpecialClientProperty, ISupportFormatter, IFormattingComponent, ISupportPlaceholderText, ISupportOnRender
{
	private static final long serialVersionUID = 1L;

	private static final String MAX_LENGTH_VALIDATOR = "maxLength"; //$NON-NLS-1$

	private String tooltip;

	// when a parse error occurred the value should always be set in setValue(), otherwise unsaved data is displayed in the field
	private boolean parseErrorOccurred = false;
	private MouseAdapter rightclickMouseAdapter = null;
	private final AbstractRuntimeField<IFieldComponent> scriptable;

	/**
	 * A formatter that extends our formatter to check for the valuelist
	 * If the valuelist is attached then the value -> string must first get from the list (and that will then be formatter by the mask)
	 * for string -> value what is get out of the mask will be checked if it is in the list. as a display and then the real will be given.
	 *
	 */
	private final class ValueListMaskFormatter extends ServoyMaskFormatter
	{
		/**
		 * @param mask
		 * @param displayFormatter
		 */
		private ValueListMaskFormatter(String mask, boolean displayFormatter) throws ParseException
		{
			super(mask, displayFormatter);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.smart.dataui.ServoyMaskFormatter#valueToString(java.lang.Object)
		 */
		@Override
		public String valueToString(Object value) throws ParseException
		{
			if (list != null)
			{
				int index = list.realValueIndexOf(value);
				if (index != -1)
				{
					return super.valueToString(list.getElementAt(index));
				}
				else
				{
					if (list.hasRealValues())
					{
						if (!eventExecutor.getValidationEnabled() && value != null)
						{
							return value.toString();
						}
						else
						{
							return super.valueToString(null);
						}
					}
				}
			}
			return super.valueToString(value);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.smart.dataui.ServoyMaskFormatter#stringToValue(java.lang.String)
		 */
		@Override
		public Object stringToValue(String value) throws ParseException
		{
			Object valueObject = super.stringToValue(value);
			if (list != null)
			{
				int index = list.indexOf(valueObject);
				if (index != -1)
				{
					return list.getRealElementAt(index);
				}
				else
				{
					if (list.hasRealValues())
					{
						if (!eventExecutor.getValidationEnabled())
						{
							return valueObject;
						}
						else
						{
							return null;
						}
					}
				}
			}
			return valueObject;
		}
	}

	/**
	 * @author jcompagner
	 */
	public class NullNumberFormatter extends NumberFormatter
	{
		private static final long serialVersionUID = 1L;

		private NumberDocumentValidator validator;

		private final int maxLength;


		/**
		 * Constructor for NullNumberFormatter.
		 *
		 * @param format
		 *
		 */
		public NullNumberFormatter(NumberFormat format)
		{
			this(format, -1);
		}

		public NullNumberFormatter(NumberFormat format, int maxLength)
		{
			super(format);
			this.maxLength = maxLength;
//			format.setGroupingUsed(true); // this is done now directly in RoundHalfUp...
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see javax.swing.text.InternationalFormatter#install(javax.swing.JFormattedTextField)
		 */
		@Override
		public void install(JFormattedTextField ftf)
		{
			int caret = getCaretPosition();
			super.install(ftf);
			int length = ftf.getDocument().getLength();
			setCaretPosition(caret > length ? length : caret);
		}

		/**
		 * @see javax.swing.text.DefaultFormatter#getNavigationFilter()
		 */
		@Override
		protected NavigationFilter getNavigationFilter()
		{
			return super.getNavigationFilter();
		}

		/**
		 * @see javax.swing.text.DefaultFormatter#getDocumentFilter()
		 */
		@Override
		protected DocumentFilter getDocumentFilter()
		{
			if (list == null)
			{
				if (validator == null)
				{
					validator = new NumberDocumentValidator(RoundHalfUpDecimalFormat.getDecimalFormatSymbols(application.getLocale()), maxLength);
				}
				return validator;
			}
			return super.getDocumentFilter();
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see javax.swing.text.InternationalFormatter#valueToString(java.lang.Object)
		 */
		@Override
		public String valueToString(Object value) throws ParseException
		{
			if (value == null || value.toString().trim().equals("")) //$NON-NLS-1$
			{
				return ""; //$NON-NLS-1$
			}
			// if it is converted by a converter it could already be a string, then just display that.
			if (value instanceof String) return (String)value;
			if (list != null)
			{
				int index = list.realValueIndexOf(value);
				if (index != -1)
				{
					value = list.getElementAt(index);
					if (value != null)
					{
						try
						{
							return super.valueToString(value);
						}
						catch (Exception e)
						{
						}
						return value.toString();
					}
				}
				else
				{
					if (list.hasRealValues())
					{
						if (!eventExecutor.getValidationEnabled())
						{
							return value != null ? value.toString() : null;
						}
						else
						{
							value = null;
						}
					}
				}
			}
			if (displayFormat.endsWith("-") && value != null && ((Number)value).intValue() >= 0) //$NON-NLS-1$
			{
				return super.valueToString(value) + " "; //$NON-NLS-1$
			}
			return super.valueToString(value);
		}

		/**
		 * @see javax.swing.JFormattedTextField.AbstractFormatter#stringToValue(java.lang.String)
		 */
		@Override
		public Object stringToValue(String text) throws ParseException
		{
			if (text == null || text.trim().equals("")) return null; //$NON-NLS-1$
			if (list != null)
			{
				int index = list.indexOf(text);
				if (index != -1)
				{
					return list.getRealElementAt(index);
				}
				else
				{
					if (list.hasRealValues())
					{
						if (!eventExecutor.getValidationEnabled())
						{
							return text;
						}
						else
						{
							return null;
						}
					}
					else
					{
						return super.stringToValue(text);
					}
				}
			}
			if (displayFormat.endsWith("-")) //$NON-NLS-1$
			{
				text = text.trim();
			}
			Object o = super.stringToValue(text);
			Object previousValue = getValue();
			if (previousValue != null && o != null && previousValue.getClass() != o.getClass())
			{
				// use for example the wicket converters
				if (previousValue instanceof Float)
				{
					o = new Float(((Number)o).floatValue());
				}
				else if (previousValue instanceof BigDecimal)
				{
					o = new BigDecimal(((Number)o).doubleValue());
				}
				else if (previousValue instanceof Double)
				{
					o = new Double(((Number)o).doubleValue());
				}
			}
			return o;
		}
	}

	/**
	 * Interface for setting initial value to formatter.
	 *
	 * @author rgansevles
	 */
	public interface ISetInitialValue<T>
	{
		void setInitialValue(T val);
	}

	public class NullDateFormatter extends DateFormatter implements ISetInitialValue<Date>
	{
		private static final long serialVersionUID = 1L;

		private final boolean editFormatter;

		private Date lastMergedDate;

		/**
		 * Constructor for NullNumberFormatter.
		 *
		 * @param format
		 */
		public NullDateFormatter(StateFullSimpleDateFormat format)
		{
			this(format, false);
		}

		/**
		 * Constructor for NullNumberFormatter.
		 *
		 * @param format
		 */
		public NullDateFormatter(StateFullSimpleDateFormat format, boolean editFormatter)
		{
			super(format);
			this.editFormatter = editFormatter;
			setOverwriteMode(true);
		}

		/**
		 * @see javax.swing.JFormattedTextField.AbstractFormatter#getFormattedTextField()
		 */
		@Override
		protected JFormattedTextField getFormattedTextField()
		{
			JFormattedTextField field = super.getFormattedTextField();
			if (field == null) field = DataField.this;
			return field;
		}


		/**
		 * @see javax.swing.text.InternationalFormatter#getActions()
		 */
		@Override
		protected Action[] getActions()
		{
			if (isEditable())
			{
				Action[] superActions = super.getActions();

				// be able to catch the "increment"/"decrement" actions (so we can restrain the changes to only the selected field)
				if (superActions != null)
				{
					for (int i = 0; i < superActions.length; i++)
					{
						if ("increment".equals(superActions[i].getValue(Action.NAME)) || "decrement".equals(superActions[i].getValue(Action.NAME))) //$NON-NLS-1$ //$NON-NLS-2$
						{
							final Action superAction = superActions[i];
							superActions[i] = new AbstractAction((String)superActions[i].getValue(Action.NAME))
							{
								public void actionPerformed(ActionEvent e)
								{
									if (Boolean.TRUE.equals(
										UIUtils.getUIProperty(DataField.this, IApplication.DATE_FORMATTERS_ROLL_INSTEAD_OF_ADD, Boolean.FALSE)))
									{
										((StateFullSimpleDateFormat)getFormat()).setRollInsteadOfAdd(true);
										try
										{
											superAction.actionPerformed(e);
										}
										finally
										{
											((StateFullSimpleDateFormat)getFormat()).setRollInsteadOfAdd(false);
										}
									}
									else
									{
										superAction.actionPerformed(e);
									}
								}
							};
						}
					}
				}

				return superActions;
			}
			return null;
		}

		public void setLenient(boolean lenient)
		{
			((StateFullSimpleDateFormat)getFormat()).setLenient(lenient);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see javax.swing.text.InternationalFormatter#install(javax.swing.JFormattedTextField)
		 */
		@Override
		public void install(JFormattedTextField ftf)
		{
			int caret = getCaretPosition();
			super.install(ftf);
			int length = ftf.getDocument().getLength();
			setCaretPosition(caret > length ? length : caret);
		}

		public void setInitialValue(Date val)
		{
			this.lastMergedDate = val;
		}

		/**
		 * @see javax.swing.JFormattedTextField.AbstractFormatter#stringToValue(java.lang.String)
		 */
		@Override
		public Object stringToValue(String text) throws ParseException
		{
			if (text == null || text.trim().equals("") || text.equals(editFormat) || text.equals(displayFormat))
			{
				return null;
			}

			// keep track of last parsed value so that when the field is made empty (editable combo), next date merge will use prev value, not null
			StateFullSimpleDateFormat format = (StateFullSimpleDateFormat)getFormat();
			format.setOriginal(lastMergedDate);
			super.stringToValue(text);
			lastMergedDate = format.getMergedDate();
			return lastMergedDate;
		}

		/**
		 * @see javax.swing.JFormattedTextField.AbstractFormatter#valueToString(java.lang.Object)
		 */
		@Override
		public String valueToString(Object value) throws ParseException
		{
			if (value == null)
			{
				if (editFormatter && getFormattedTextField().isEditable())
				{
					SwingUtilities.invokeLater(new Runnable()
					{
						public void run()
						{
							selectAll();
						}
					});
					return editFormat;
				}
				else return ""; //$NON-NLS-1$
			}
			// if it is converted by a converter it could already be a string, then just display that.
			if (value instanceof String) return (String)value;

			try
			{
				return super.valueToString(value);
			}
			catch (ParseException e)
			{
				Debug.error("Error formatting date: " + value + " to:" + displayFormat + " for dataproviderid: " + dataProviderID); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				throw e;
			}
		}

		/**
		 * @param placeHolder
		 * @return
		 * @throws ParseException
		 */
		public AbstractFormatter getMaskFormatter(char placeHolder) throws ParseException
		{
			String pattern = ((StateFullSimpleDateFormat)getFormat()).toPattern();
			String maskPattern = pattern.replace('y', '#').replace('M', '#').replace('w', '#').replace('W', '#').replace('D', '#').replace('d', '#').replace(
				'F', '#').replace('a', '?').replace('H', '#').replace('k', '#').replace('K', '#').replace('h', '#').replace('m', '#').replace('s', '#').replace(
					'S', '#');
			FixedMaskFormatter maskFormatter = new FixedMaskDateFormatter(maskPattern);
			maskFormatter.setValueClass(String.class);
			if (placeHolder != 0)
			{
				maskFormatter.setPlaceholderCharacter(placeHolder);
			}
			else
			{
				maskFormatter.setPlaceholder(pattern);
			}
			return maskFormatter;
		}

		private final class FixedMaskDateFormatter extends FixedMaskFormatter implements ISetInitialValue<Date>
		{
			/**
			 * @param mask
			 */
			private FixedMaskDateFormatter(String mask) throws ParseException
			{
				super(mask);
			}

			public void setInitialValue(Date val)
			{
				NullDateFormatter.this.setInitialValue(val);
			}

			/**
			 * @see com.servoy.j2db.util.text.ServoyMaskFormatter#valueToString(java.lang.Object)
			 */
			@Override
			public String valueToString(Object value) throws ParseException
			{
				return super.valueToString(NullDateFormatter.this.valueToString(value));
			}

			/**
				 * @see com.servoy.j2db.util.text.ServoyMaskFormatter#stringToValue(java.lang.String)
				 */
			@Override
			public Object stringToValue(String value) throws ParseException
			{
				Object s = super.stringToValue(value);
				if (s instanceof String)
				{
					return NullDateFormatter.this.stringToValue((String)s);
				}
				return s;
			}

			/**
			 * @see javax.swing.JFormattedTextField.AbstractFormatter#getActions()
			 */
			@Override
			protected Action[] getActions()
			{
				return NullDateFormatter.this.getActions();
			}
		}
	}

	private class TextFormatter extends InternationalFormatter
	{
		private static final long serialVersionUID = 1L;

		/**
		 * @see javax.swing.text.InternationalFormatter#install(javax.swing.JFormattedTextField)
		 */
		@Override
		public void install(JFormattedTextField ftf)
		{
			int caret = getCaretPosition();
			super.install(ftf);
			int length = ftf.getDocument().getLength();
			setCaretPosition(caret > length ? length : caret);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see javax.swing.text.InternationalFormatter#stringToValue(java.lang.String)
		 */
		@Override
		public Object stringToValue(String text) throws ParseException
		{
			if (list != null)
			{
				int index = list.indexOf(text);
				if (index > -1)
				{
					return list.getRealElementAt(index);
				}
				else
				{
					if (list.hasRealValues())
					{
						if (!eventExecutor.getValidationEnabled())
						{
							return text;
						}
						else
						{
							return null;
						}
					}
					else
					{
						return super.stringToValue(text);
					}
				}
			}
			else
			{
				return super.stringToValue(text);
			}
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see javax.swing.text.InternationalFormatter#valueToString(java.lang.Object)
		 */
		@Override
		public String valueToString(Object value) throws ParseException
		{
			if (list != null)
			{
				// if it is in find mode and the list reports to have real values and it is a global method valuelist
				// test first if the given value is really a real value by comparing a real value class with the give class.
				if (value != null && !eventExecutor.getValidationEnabled() && list.hasRealValues() && list instanceof GlobalMethodValueList)
				{
					if (list.getSize() == 0 || (list.getSize() == 1 && list.getAllowEmptySelection()))
					{
						((GlobalMethodValueList)list).fill();
					}
					if (list.getSize() > 0)
					{
						Object real = list.getRealElementAt(list.getSize() - 1);
						if (real != null && !real.getClass().equals(value.getClass()))
						{
							return value.toString();
						}
					}
				}
				int index = list.realValueIndexOf(value);
				if (index != -1)
				{
					value = list.getElementAt(index);
				}
				else
				{
					if (list.hasRealValues())
					{
						if (!eventExecutor.getValidationEnabled())
						{
							return value != null ? value.toString() : null;
						}
						else
						{
							value = null;
						}
					}
				}
			}
			return super.valueToString(value);
		}
	}

	private class EditingFixedDefaultFormatterFactory extends DefaultFormatterFactory
	{
		public EditingFixedDefaultFormatterFactory(JFormattedTextField.AbstractFormatter defaultFormat)
		{
			super(defaultFormat);
		}

		public EditingFixedDefaultFormatterFactory(JFormattedTextField.AbstractFormatter defaultFormat, JFormattedTextField.AbstractFormatter displayFormat,
			JFormattedTextField.AbstractFormatter editFormat)
		{
			super(defaultFormat, displayFormat, editFormat);
		}

		public EditingFixedDefaultFormatterFactory(JFormattedTextField.AbstractFormatter defaultFormat, JFormattedTextField.AbstractFormatter displayFormat,
			JFormattedTextField.AbstractFormatter editFormat, JFormattedTextField.AbstractFormatter nullFormat)
		{
			super(defaultFormat, displayFormat, editFormat, nullFormat);
		}

		@Override
		public JFormattedTextField.AbstractFormatter getFormatter(JFormattedTextField source)
		{
			JFormattedTextField.AbstractFormatter format = null;

			if (source == null)
			{
				return null;
			}
			Object value = source.getValue();

			if (value == null)
			{
				format = getNullFormatter();
			}
			if (format == null)
			{
				// added isEditable check
				if (source.hasFocus() && source.isEditable())
				{
					format = getEditFormatter();
				}
				else
				{
					format = getDisplayFormatter();
				}
				if (format == null)
				{
					format = getDefaultFormatter();
				}
			}
			return format;
		}
	}

	protected EventExecutor eventExecutor;

	private ControllerUndoManager undoManager;

	protected IApplication application;

	protected IValueList list;

	private final Document plainDocument;
	private ValidatingDocument editorDocument;

	private final String decimalSeparator;
	private boolean decimalMode;

	private Caret defaultCaret;
	private Caret overtypeCaret;

	private boolean toggleOverwrite = true;

	DataField(IApplication application, AbstractRuntimeField<IFieldComponent> scriptable, IValueList list)
	{
		this(application, scriptable);
		this.list = list;
	}

	public DataField(IApplication application, AbstractRuntimeField<IFieldComponent> scriptable)
	{
		super();// new InternationalFormatter()); //why is InternationalFormatter
		// needed, causes trouble on date objects??
		this.application = application;
		eventExecutor = new EventExecutor(this)
		{
			@Override
			public void fireLeaveCommands(Object display, boolean focusEvent, int modifiers)
			{
				if (hasLeaveCmds())
				{
					try
					{
						commitEdit();
					}
					catch (ParseException ex)
					{
						Debug.error(ex);
					}
				}

				super.fireLeaveCommands(display, focusEvent, modifiers);
			}
		};
		plainDocument = getDocument();
		setDocument(editorDocument = new ValidatingDocument());
		this.scriptable = scriptable;
		addKeyListener(new KeyAdapter()
		{
			private boolean enterKeyPressed = false;

			@Override
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
				{
					enterKeyPressed = true;
				}
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE && !isValueValid)
				{
					e.consume();
				}
			}

			@Override
			public void keyReleased(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ENTER && enterKeyPressed)
				{
					enterKeyPressed = false;
					eventExecutor.actionPerformed(e.getModifiers());
				}
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
				{
					if (!isValueValid)
					{
						restorePreviousValidValue();
						e.consume();
					}
				}
			}
		});
		DecimalFormatSymbols dfs = RoundHalfUpDecimalFormat.getDecimalFormatSymbols(application.getLocale());
		decimalSeparator = String.valueOf(dfs.getDecimalSeparator());
		setFocusLostBehavior(COMMIT);
		addMouseListener(eventExecutor);
		addKeyListener(eventExecutor);
		setDragEnabledEx(true);
	}

	public final AbstractRuntimeField<IFieldComponent> getScriptObject()
	{
		return scriptable;
	}

	private Caret getOvertypeCaret()
	{
		if (overtypeCaret == null)
		{
			defaultCaret = getCaret();
			overtypeCaret = new OvertypeCaret();
			if (defaultCaret == null)
			{
				overtypeCaret.setBlinkRate(500);
			}
			else
			{
				overtypeCaret.setBlinkRate(defaultCaret.getBlinkRate());
			}
		}
		return overtypeCaret;
	}

	private Caret getDefaultCaret()
	{
		getOvertypeCaret();
		return defaultCaret;
	}

	// MAC FIX
	@Override
	public Insets getInsets()
	{
		Insets insets = super.getInsets();
		if (insets == null)
		{
			insets = new Insets(0, 0, 0, 0);
		}
		return insets;
	}

	// MAC FIX
	@Override
	public Insets getMargin()
	{
		Insets insets = super.getMargin();
		if (insets == null)
		{
			insets = new Insets(0, 0, 0, 0);
		}
		return insets;
	}

	public void destroy()
	{
		if (list != null) list.deregister();
	}

	private void setDragEnabledEx(boolean b)
	{
		try
		{
			Method m = getClass().getMethod("setDragEnabled", new Class[] { boolean.class }); //$NON-NLS-1$
			m.invoke(this, new Object[] { new Boolean(b) });
		}
		catch (Exception e)
		{
		}
	}

	/**
	 * Fix for bad font rendering (bad kerning == strange spacing) in java 1.5 see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5097047
	 */
	@Override
	public FontMetrics getFontMetrics(Font font)
	{
		if (application != null)//getFontMetrics can be called in the constructor super call before application is assigned
		{
			boolean isPrinting = Utils.getAsBoolean(application.getRuntimeProperties().get("isPrinting")); //$NON-NLS-1$
			if (isPrinting)
			{
				Graphics g = (Graphics)application.getRuntimeProperties().get("printGraphics"); //$NON-NLS-1$
				if (g != null)
				{
					String text = getText();
					// only return print graphics font metrics if text does not start with 'W',
					// because of left side bearing issue
					if (!(text != null && text.length() > 0 && text.charAt(0) == 'W')) return g.getFontMetrics(font);
				}
			}
		}
		return super.getFontMetrics(font);
	}

	private ControllerUndoManager getUndoManager()
	{
		if (undoManager == null)
		{
			Container con = getParent();
			while (con != null && !(con instanceof IFormUIInternal))
			{
				con = con.getParent();
			}
			if (con != null)
			{
				undoManager = ((IFormUIInternal)con).getUndoManager();
			}
		}
		return undoManager;
	}

	@Override
	public void setUI(ComponentUI ui)
	{
		super.setUI(ui);
	}

	protected String dataProviderID;

	public boolean needEditListener()
	{
		return true;
	}

	protected EditProvider editProvider = null;

	public IEditProvider getEditProvider()
	{
		return editProvider;
	}

	public void addEditListener(IEditListener l)
	{
		if (editProvider == null)
		{
			editProvider = new EditProvider(this);
			addFocusListener(editProvider);
			addPropertyChangeListener("value", editProvider); //$NON-NLS-1$
			editProvider.addEditListener(l);
			editProvider.setEditable(isEditable());
			try
			{
				DropTarget dt = getDropTarget();
				if (dt != null) dt.addDropTargetListener(editProvider);
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
	}

	public IEventExecutor getEventExecutor()
	{
		return eventExecutor;
	}

	@Override
	public void setEditable(boolean b)
	{
		editState = b;
		super.setEditable(b);
		if (editProvider != null) editProvider.setEditable(b);
	}

	private int maxLength = -1;

	public void setMaxLength(int i)
	{
		// do not set max length check when it has been set in setFormat()
		if (list == null && editorDocument.getValidator(MAX_LENGTH_VALIDATOR) == null)
		{
			editorDocument.setValidator(MAX_LENGTH_VALIDATOR, new LengthDocumentValidator(i));
		}
		maxLength = i;
	}

	@Override
	public void setMargin(Insets m)
	{
		// super.setMargin(m); //seems to have no effect
		setBorder(BorderFactory.createCompoundBorder(getBorder(), BorderFactory.createEmptyBorder(m.top, m.left, m.bottom, m.right)));
	}

	public String getDataProviderID()
	{
		return dataProviderID;
	}

	public void setDataProviderID(String id)
	{
		dataProviderID = id;
	}

	private boolean needEntireState;

	public boolean needEntireState()
	{
		return needEntireState;
	}

	public void setNeedEntireState(boolean b)
	{
		needEntireState = b;
	}


	public Object getValueObject()
	{
		return getValue();
	}

	protected ITagResolver resolver;

	public void setTagResolver(ITagResolver resolver)
	{
		this.resolver = resolver;
	}

	public void setValueObject(Object obj)
	{
		try
		{
			if (editProvider != null)
			{
				editProvider.setAdjusting(true);
			}
			if (((DefaultFormatterFactory)getFormatterFactory()) != null &&
				((DefaultFormatterFactory)getFormatterFactory()).getEditFormatter() instanceof ISetInitialValue)
			{
				((ISetInitialValue)((DefaultFormatterFactory)getFormatterFactory()).getEditFormatter()).setInitialValue(obj);
			}
			setValueEx(obj);
		}
		catch (Exception e)
		{
			try
			{
				setValueEx(null);
			}
			catch (Exception ex)
			{
			}
			Debug.trace("Format Error in field " + getName() + " " + dataProviderID + " " + e); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		finally
		{
			if (editProvider != null)
			{
				editProvider.setAdjusting(false);
			}
		}
		fireOnRender(false);
	}

	public void fireOnRender(boolean force)
	{
		if (!isIgnoreOnRender && scriptable != null)
		{
			if (force) scriptable.getRenderEventExecutor().setRenderStateChanged();
			scriptable.getRenderEventExecutor().fireOnRender(hasFocus());
		}
	}

	// also used by datacalendar we than don't want the edit to be blocked by
	// adjusting
	void setValueExFromCalendar(Object obj)
	{
		boolean editable = false;
		if (editProvider != null)
		{
			editable = editProvider.isEditable();
			editProvider.setEditable(true);
			// can this be done.. change command fired twice now without it.
			editProvider.setAdjusting(true);
			editProvider.startEdit();
		}
		try
		{
			if (getFormatterFactory() == disabledFormatter && saveFormatter != null)
			{
				try
				{
					obj = saveFormatter.getFormatter(this).valueToString(obj);
				}
				catch (ParseException e)
				{
					Debug.error(e);
				}
			}
			setValueEx(obj);
		}
		finally
		{
			if (editProvider != null)
			{
				editProvider.setAdjusting(false);
				editProvider.commitData();
				editProvider.setEditable(editable);
			}
		}
	}

	private void setValueEx(Object obj)
	{
		if (needEntireState && !skipPropertyChange)
		{
			if (resolver != null)
			{
				// do not process tags on other objects like numbers, these will be formatted using the formatter
				if (obj instanceof String)
				{
					obj = Text.processTags((String)obj, resolver);
				}
			}
			if (tooltip != null)
			{
				super.setToolTipText(""); //$NON-NLS-1$
			}

		}

		if (obj instanceof DbIdentValue)
		{
			obj = ((DbIdentValue)obj).getPkValue();
		}

		// when a parse error occurred the value should always be set in setValue(), otherwise unsaved data is displayed in the field
		if (parseErrorOccurred || !Utils.equalObjects(getValue(), obj))
		{
			if (obj instanceof String && getFormatterFactory() == null)
			{
				// this may no longer be necessary as a FormatterFactory should always be set now
				// when setFormat() gets called... (a null formatter factory would mean that text
				// field commitEdit() would not work - so you cannot really use such a field for editing)
				setFormatterFactory(new EditingFixedDefaultFormatterFactory(new InternationalFormatter()));
			}
			boolean enableEdits = false;
			if (getUndoManager() != null)
			{
				if (!undoManager.isIgnoreEdits())
				{
					enableEdits = true;
					undoManager.setIgnoreEdits(true);
				}
			}
			super.setValue(obj);

			// if text is changed through the outside (a new record is set, set the caret to the length pos)
			// and the current field has not the focus?
			if (hasFocus() && isDisplayable())
			{
				setCaretPosition(getText().length());
			}

			if (enableEdits)
			{
				undoManager.setIgnoreEdits(false);
			}
			setValueValid(true, null);
		}
	}

	protected boolean skipPropertyChange = false;

	@Override
	protected void invalidEdit()
	{
		if (eventExecutor.getValidationEnabled())
		{
			super.invalidEdit();
		}
	}

	/**
	 * @see javax.swing.text.JTextComponent#setText(java.lang.String)
	 */
	@Override
	public void setText(String t)
	{
		if (!Utils.equalObjects(getText(), t)) super.setText(t);
	}

	// work around for bug in jdk 1.4 ,assumes that (isEditing == true)
	@Override
	protected void processFocusEvent(FocusEvent e)
	{
		boolean enableEdits = false;
		if (getUndoManager() != null)
		{
			if (!undoManager.isIgnoreEdits())
			{
				enableEdits = true;
				undoManager.setIgnoreEdits(true);
			}
		}
		Object o = getValue();
		int start = getSelectionStart();
		int end = getSelectionEnd();

		super.processFocusEvent(e);

		if ((start != end && getValue() == o) || (Utils.isAppleMacOS() && getValue() == o))
		{
			select(start, end);
		}
		if (enableEdits)
		{
			undoManager.setIgnoreEdits(false);
		}
	}

	// work around for bug in jdk 1.4 ,the jformattedtextfield does fire odd values
	@Override
	protected void firePropertyChange(String propertyName, Object oldValue, Object newValue)
	{
		if (!isValueValid && "value".equals(propertyName) && Utils.equalObjects(newValue, previousValidValue)) //$NON-NLS-1$
		{
			// added this because invalid fields with DATE/NUMBER data providers will
			// have oldValue = last valid value, not the currently displayed invalid value - thus
			// the equalObjects would return true although it shouldn't and the field would not be validated
			// through DisplaysAdapter
			setValueValid(true, null);
		}
		if ("value".equals(propertyName) && skipPropertyChange) //$NON-NLS-1$
		{
			return;
		}
		else if (oldValue == null && newValue != null)
		{
			if (!newValue.equals("")) //$NON-NLS-1$
			{
				super.firePropertyChange(propertyName, oldValue, newValue);
			}
		}
		else if ("value".equals(propertyName) && Utils.equalObjects(oldValue, newValue)) //$NON-NLS-1$
		{
			skipPropertyChange = true;
			try
			{
				setValueEx(oldValue);
			}
			finally
			{
				skipPropertyChange = false;
			}
			return;
		}
		else
		{
			super.firePropertyChange(propertyName, oldValue, newValue);
		}
	}

	private int dataType;

	/*
	 * format---------------------------------------------------
	 */
	protected String displayFormat;
	protected String editFormat;
	private AbstractFormatterFactory saveFormatter;
	private final AbstractFormatterFactory disabledFormatter = new DefaultFormatterFactory(new InternationalFormatter());

	private List<ILabel> labels;

	/**
	 * This method is only used by DataCalendar to initialize the JDateChooser format
	 */
	public String getFormat()
	{
		if (fp == null) return null;
		if (fp.hasEditFormat())
		{
			return fp.getEditFormat();
		}
		else
		{
			return fp.getDisplayFormat();
		}
	}

	protected ParsedFormat fp;

	public void installFormat(ComponentFormat componentFormat)
	{
		fp = componentFormat.parsedFormat;
		this.dataType = componentFormat.uiType;
		this.displayFormat = null;
		this.editFormat = null;
		editorDocument.clearValidators();
		boolean emptyCustom = (list instanceof CustomValueList) && !(list instanceof GlobalMethodValueList) && list.getSize() == 0;
		if (!fp.isEmpty() && (list == null || (!list.hasRealValues() && !emptyCustom)))
		{
			displayFormat = fp.getDisplayFormat();
			editFormat = fp.getEditFormat();
			if (fp.getMaxLength() != null && fp.getMaxLength().intValue() > 0)
			{
				editorDocument.setValidator(MAX_LENGTH_VALIDATOR, new LengthDocumentValidator(fp.getMaxLength().intValue()));
			}

			if (fp.isAllLowerCase())
			{
				editorDocument.setValidator("LowerCaseDocumentValidator", new LowerCaseDocumentValidator()); //$NON-NLS-1$
				TextFormatter display = new TextFormatter();
				TextFormatter edit = new TextFormatter();
				setFormatterFactory(new EditingFixedDefaultFormatterFactory(display, display, edit, edit));
			}
			else if (fp.isAllUpperCase())
			{
				editorDocument.setValidator("UpperCaseDocumentValidator", new UpperCaseDocumentValidator()); //$NON-NLS-1$
				TextFormatter display = new TextFormatter();
				TextFormatter edit = new TextFormatter();
				setFormatterFactory(new EditingFixedDefaultFormatterFactory(display, display, edit, edit));
			}
			else if (fp.isNumberValidator())
			{
				editorDocument.setValidator("NumberDocumentValidator", new NumberDocumentValidator()); //$NON-NLS-1$
				TextFormatter display = new TextFormatter();
				TextFormatter edit = new TextFormatter();
				setFormatterFactory(new EditingFixedDefaultFormatterFactory(display, display, edit, edit));
			}
			else
			{
				int maxLength = fp.getMaxLength() == null ? -1 : fp.getMaxLength().intValue();
				// if there is no display format, but the max length is set, then generate a display format.
				if (maxLength != -1 && (displayFormat == null || displayFormat.length() == 0))
				{
					// if this is just a text type textfield then just set those formatters (the max length is already set)
					if (Column.mapToDefaultType(dataType) == IColumnTypes.TEXT)
					{
						TextFormatter display = new TextFormatter();
						TextFormatter edit = new TextFormatter();
						setFormatterFactory(new EditingFixedDefaultFormatterFactory(display, display, edit, edit));
					}
					else
					{
						char[] chars = new char[maxLength];
						for (int i = 0; i < chars.length; i++)
							chars[i] = '#';
						displayFormat = new String(chars);
					}
				}

				if (displayFormat != null)
				{
					if (editFormat == null) editFormat = displayFormat;
					try
					{
						JFormattedTextField.AbstractFormatter displayFormatter = null;
						JFormattedTextField.AbstractFormatter editFormatter = null;
						switch (Column.mapToDefaultType(dataType))
						{
							case IColumnTypes.NUMBER :
								displayFormatter = new NullNumberFormatter(new RoundHalfUpDecimalFormat(displayFormat, application.getLocale()));// example: $#,###.##
								editFormatter = new NullNumberFormatter(new RoundHalfUpDecimalFormat(editFormat, application.getLocale()), maxLength);
								setFormatterFactory(new EditingFixedDefaultFormatterFactory(displayFormatter, displayFormatter, editFormatter, editFormatter));
								break;

							case IColumnTypes.INTEGER :
								displayFormatter = new NullNumberFormatter(new RoundHalfUpDecimalFormat(displayFormat, application.getLocale()));// example: $#,###.##
								editFormatter = new NullNumberFormatter(new RoundHalfUpDecimalFormat(editFormat, application.getLocale()), maxLength);
								setFormatterFactory(new EditingFixedDefaultFormatterFactory(displayFormatter, displayFormatter, editFormatter, editFormatter));
								break;

							case IColumnTypes.DATETIME :
								boolean mask = fp.isMask();
								char placeHolder = fp.getPlaceHolderCharacter();
								if (mask) editFormat = displayFormat;

								displayFormatter = new NullDateFormatter(new StateFullSimpleDateFormat(displayFormat, false));
								editFormatter = new NullDateFormatter(new StateFullSimpleDateFormat(editFormat,
									Boolean.TRUE.equals(UIUtils.getUIProperty(this, IApplication.DATE_FORMATTERS_LENIENT, Boolean.TRUE))), !mask);
								if (mask)
								{
									editFormatter = ((NullDateFormatter)editFormatter).getMaskFormatter(placeHolder);
								}
								else
								{
									// date formats are default in override mode
									setCaret(getOvertypeCaret());
								}
								setFormatterFactory(new EditingFixedDefaultFormatterFactory(displayFormatter, displayFormatter, editFormatter)); // example: MM/dd/yyyy
								break;

							default :
								displayFormatter = new ValueListMaskFormatter(displayFormat, true);
								editFormatter = new ValueListMaskFormatter(displayFormat, false);

								if (fp.isRaw())
								{
									((ServoyMaskFormatter)editFormatter).setValueContainsLiteralCharacters(false);
									((ServoyMaskFormatter)displayFormatter).setValueContainsLiteralCharacters(false);
								}

								if (fp.getAllowedCharacters() != null)
								{
									((ServoyMaskFormatter)editFormatter).setValidCharacters(fp.getAllowedCharacters());
									((ServoyMaskFormatter)displayFormatter).setValidCharacters(fp.getAllowedCharacters());
								}
								if (editFormat != null)
								{
									if (editFormat.length() == 1)
									{
										((ServoyMaskFormatter)editFormatter).setPlaceholderCharacter(editFormat.charAt(0));
									}
									else
									{
										((ServoyMaskFormatter)editFormatter).setPlaceholder(editFormat);
									}
								}
								setFormatterFactory(new EditingFixedDefaultFormatterFactory(displayFormatter, displayFormatter, editFormatter));
								// format overrules max length check
								editorDocument.setValidator(MAX_LENGTH_VALIDATOR, new LengthDocumentValidator(0));
								break;
						}
					}
					catch (Exception ex)
					{
						Debug.error(ex);
					}
				}
			}
		}
		else
		//for text fields
		{
			TextFormatter display = new TextFormatter();
			TextFormatter edit = new TextFormatter();
			setFormatterFactory(new EditingFixedDefaultFormatterFactory(display, display, edit, edit));
		}
		if (maxLength >= 0 && editorDocument.getValidator(MAX_LENGTH_VALIDATOR) == null)
		{
			editorDocument.setValidator(MAX_LENGTH_VALIDATOR, new LengthDocumentValidator(maxLength));
		}
	}

	public void setComponentVisible(boolean b_visible)
	{
		if (viewable || !b_visible)
		{
			setVisible(b_visible);
		}
	}

	@Override
	public void setVisible(boolean flag)
	{
		super.setVisible(flag);
		if (labels != null)
		{
			for (int i = 0; i < labels.size(); i++)
			{
				ILabel label = labels.get(i);
				label.setComponentVisible(flag);
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
	 * readonly---------------------------------------------------
	 */
	public boolean isReadOnly()
	{
		return !isEditable();
	}

	protected boolean editState;

	public void setReadOnly(boolean b)
	{
		if (b && !isEditable()) return;
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

	public int getAbsoluteFormLocationY()
	{
		Container parent = getParent();
		while ((parent != null) && !(parent instanceof IDataRenderer))
		{
			parent = parent.getParent();
		}
		if (parent != null)
		{
			return ((IDataRenderer)parent).getYOffset() + getLocation().y;
		}
		return getLocation().y;
	}

	private Point cachedLocation;

	public Point getCachedLocation()
	{
		return cachedLocation;
	}

	public void setClientProperty(Object key, Object value)
	{
		if (IApplication.DATE_FORMATTERS_LENIENT.equals(key))
		{
			AbstractFormatterFactory ff = getFormatterFactory();
			if (ff instanceof DefaultFormatterFactory)
			{
				AbstractFormatter formatter = ((DefaultFormatterFactory)ff).getEditFormatter();
				if (formatter instanceof NullDateFormatter)
				{
					((NullDateFormatter)formatter).setLenient(
						Boolean.TRUE.equals(UIUtils.getUIProperty(this, IApplication.DATE_FORMATTERS_LENIENT, Boolean.TRUE)));
				}
			}
		}
	}

	/*
	 * size---------------------------------------------------
	 */
	private Dimension cachedSize;

	public Dimension getCachedSize()
	{
		return cachedSize;
	}

	public void setCachedLocation(Point location)
	{
		this.cachedLocation = location;
	}

	public void setCachedSize(Dimension size)
	{
		this.cachedSize = size;
	}

	// If component not shown or not added yet
	// and request focus is called it should wait for the component
	// to be created.
	boolean wantFocus = false;

	@Override
	public void addNotify()
	{
		super.addNotify();
		if (wantFocus)
		{
			wantFocus = false;
			requestFocus();
		}
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

	@Override
	public String getToolTipText()
	{
		if (resolver != null && tooltip != null)
		{
			String oldValue = tooltip;
			tooltip = null;
			super.setToolTipText(Text.processTags(oldValue, resolver));
			tooltip = oldValue;
		}
		return super.getToolTipText();
	}

	@Override
	public String getToolTipText(MouseEvent event)
	{
		return getToolTipText();
	}

	@Override
	public void setToolTipText(String tip)
	{
		if (tip != null && tip.indexOf("%%") != -1) //$NON-NLS-1$
		{
			tooltip = tip;
		}
		else if (!Utils.stringIsEmpty(tip))
		{
			if (!Utils.stringContainsIgnoreCase(tip, "<html")) //$NON-NLS-1$
			{
				super.setToolTipText(tip);
			}
			else if (HtmlUtils.hasUsefulHtmlContent(tip))
			{
				super.setToolTipText(tip);
			}
		}
		else
		{
			super.setToolTipText(null);
		}
	}


	/*
	 * _____________________________________________________________ Methods for event handling
	 */
	public void addScriptExecuter(IScriptExecuter el)
	{
		eventExecutor.setScriptExecuter(el);
	}

	public void setEnterCmds(String[] ids, Object[][] args)
	{
		eventExecutor.setEnterCmds(ids, args);
	}

	public void setLeaveCmds(String[] ids, Object[][] args)
	{
		eventExecutor.setLeaveCmds(ids, args);
	}

	@Override
	public Color getForeground()
	{
		if (isValueValid())
		{
			return super.getForeground();
		}
		return Color.red;
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
		if (!valid)
		{
			application.invokeLater(new Runnable()
			{
				public void run()
				{
					requestFocus();
				}
			});
		}

		if (valid == isValueValid)
		{
			// if valid state does not change do nothing;
			// second call with valid=false should keep oldVal from first call
			return;
		}

		isValueValid = valid;
		repaint(); // foreground color changes

		if (isValueValid)
		{
			previousValidValue = null;
		}
		else
		{
			previousValidValue = oldVal;
		}
	}

	public void restorePreviousValidValue()
	{
		if (!isValueValid)
		{
			setValueObject(previousValidValue);
			setValueValid(true, null);
			if (editProvider != null) editProvider.commitData();
		}
	}

	public void notifyLastNewValueWasChange(Object oldVal, Object newVal)
	{
		if (previousValidValue != null)
		{
			oldVal = previousValidValue;
		}
		eventExecutor.fireChangeCommand(oldVal, newVal, false, this);
	}

	public void setChangeCmd(String id, Object[] args)
	{
		eventExecutor.setChangeCmd(id, args);
	}

	public void setActionCmd(String id, Object[] args)
	{
		eventExecutor.setActionCmd(id, args);
	}

	public void setRightClickCommand(String rightClickCmd, Object[] args)
	{
		eventExecutor.setRightClickCmd(rightClickCmd, args);
		if (rightClickCmd != null && rightclickMouseAdapter == null)
		{
			rightclickMouseAdapter = new MouseAdapter()
			{
				@Override
				public void mousePressed(MouseEvent e)
				{
					if (e.isPopupTrigger()) handle(e);
				}

				@Override
				public void mouseReleased(MouseEvent e)
				{
					if (e.isPopupTrigger()) handle(e);
				}

				private void handle(MouseEvent e)
				{
					if (isEnabled())
					{
						eventExecutor.fireRightclickCommand(true, DataField.this, e.getModifiers(), e.getPoint());
					}
				}
			};
			addMouseListener(rightclickMouseAdapter);
		}
	}

	protected boolean wasEditable;

	public void setValidationEnabled(boolean b)
	{
		if (eventExecutor.getValidationEnabled() == b) return;
		if (dataProviderID != null && ScopesUtils.isVariableScope(dataProviderID)) return;

		eventExecutor.setValidationEnabled(b);

		boolean prevEditState = editState;
		if (b)
		{
			setEditable(wasEditable);
			setDocument(editorDocument);
			if (editProvider != null)
			{
				editProvider.setAdjusting(true);
			}
			try
			{
				setValue(null);//prevent errors
			}
			finally
			{
				if (editProvider != null)
				{
					editProvider.setAdjusting(false);
				}
			}
			setFormatterFactory(saveFormatter);
		}
		else
		{
			wasEditable = isEditable();
			if (!Boolean.TRUE.equals(application.getClientProperty(IApplication.LEAVE_FIELDS_READONLY_IN_FIND_MODE)))
			{
				setEditable(true);
			}
			setDocument(plainDocument);

			saveFormatter = getFormatterFactory();
			// create empty formatter
			setFormatterFactory(disabledFormatter);
		}
		editState = prevEditState;
	}

	public void setSelectOnEnter(boolean b)
	{
		eventExecutor.setSelectOnEnter(b);
	}

	//_____________________________________________________________

	public void requestFocusToComponent()
	{
//		if (!hasFocus()) Don't test on hasFocus (it can have focus,but other component already did requestFocus)
		{
			if (isDisplayable())
			{
				// Must do it in a runnable or else others after a script can get focus first again..
				application.invokeLater(new Runnable()
				{
					public void run()
					{
						requestFocus();
					}
				});
			}
			else
			{
				wantFocus = true;
			}
		}
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

	/**
	 * @see javax.swing.JFormattedTextField#commitEdit()
	 */
	@Override
	public void commitEdit() throws ParseException
	{
		parseErrorOccurred = false;
		AbstractFormatterFactory factory = getFormatterFactory();
		if (factory instanceof DefaultFormatterFactory)
		{
			AbstractFormatter formatter = ((DefaultFormatterFactory)factory).getEditFormatter();
			if (formatter != null && formatter != getFormatter())
			{
				// only a editFormatter can commit an edit.
				return;
			}
		}
		try
		{
			super.commitEdit();
		}
		catch (ParseException e)
		{
			parseErrorOccurred = true;
			setValueValid(false, getValueObject());
			throw e;
		}

	}

	/**
	 * @see javax.swing.JComponent#processKeyBinding(javax.swing.KeyStroke,java.awt.event.KeyEvent, int, boolean)
	 */
	@Override
	protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed)
	{
		if (!hasFocus())
		{
			// FIX for tableview (JTable) that the field doesn't already have focus before it get's the first keyevent
			if (getFormatterFactory() instanceof DefaultFormatterFactory && ((DefaultFormatterFactory)getFormatterFactory()).getEditFormatter() != null)
			{
				DefaultFormatterFactory dff = (DefaultFormatterFactory)getFormatterFactory();
				setFormatter(dff.getEditFormatter());
			}
			setCaretPosition(getText().length());
			selectAll();
			eventExecutor.skipSelectOnEnter();
		}
		if (e.getKeyCode() == KeyEvent.VK_DECIMAL && isEditable() && Column.mapToDefaultType(dataType) == IColumnTypes.NUMBER)
		{
			if (e.getID() == KeyEvent.KEY_PRESSED)
			{
				decimalMode = true;
			}
			else
			{
				decimalMode = false;
			}
		}
		else if (decimalMode)
		{
			if (e.getID() == KeyEvent.KEY_TYPED)
			{
				replaceSelection(decimalSeparator);
				return true;
			}
		}
		else
		{
			decimalMode = false;

			AbstractFormatterFactory formatterFactory = getFormatterFactory();
			DefaultFormatter formatter = null;
			if (formatterFactory instanceof DefaultFormatterFactory)
			{
				DefaultFormatterFactory factory = (DefaultFormatterFactory)formatterFactory;
				AbstractFormatter editFormatter = factory.getEditFormatter();
				if (editFormatter == null) editFormatter = factory.getDefaultFormatter();
				if (editFormatter instanceof DefaultFormatter && !(editFormatter instanceof MaskFormatter))
				{
					formatter = (DefaultFormatter)editFormatter;
				}
			}
			if (Column.mapToDefaultType(dataType) == IColumnTypes.DATETIME && formatter != null && formatter.getOverwriteMode() && isEditable())
			{
				String selected = getSelectedText();
				if (selected == null || !getText().equals(selected))
				{
					if (e.getKeyCode() == KeyEvent.VK_DELETE)
					{
						if (e.getID() == KeyEvent.KEY_RELEASED)
						{
							Caret c = getCaret();
							if (c.getDot() >= 0 && getText().length() > (c.getDot()))
							{
								int dot = c.getDot();
								setSelectionStart(c.getDot());
								setSelectionEnd(c.getDot() + 1);
								replaceSelection(" "); //$NON-NLS-1$
								c.setDot(dot);
							}
						}
						return true;
					}
					else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE)
					{
						if (e.getID() == KeyEvent.KEY_RELEASED)
						{
							Caret c = getCaret();
							if (c.getDot() > 0)
							{
								int dot = c.getDot();
								setSelectionStart(c.getDot() - 1);
								setSelectionEnd(c.getDot());
								replaceSelection(" "); //$NON-NLS-1$
								c.setDot(dot - 1);
								return true;
							}

							e.setKeyCode(KeyEvent.VK_LEFT);
							e.setKeyChar(KeyEvent.CHAR_UNDEFINED);
							ks = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, ks.getModifiers(), ks.isOnKeyRelease());
						}
						return true;
					}
				}
			}
			if (e.getKeyCode() == KeyEvent.VK_INSERT && formatter != null)
			{
				if (e.getID() == KeyEvent.KEY_PRESSED && toggleOverwrite)
				{
					formatter.setOverwriteMode(!formatter.getOverwriteMode());
					toggleOverwrite = false;
					((ISmartClientApplication)application).updateInsertModeIcon(this);

					int caretPos = getCaretPosition();
					if (formatter.getOverwriteMode())
					{
						setCaret(getOvertypeCaret());
					}
					else
					{
						setCaret(getDefaultCaret());
					}
					setCaretPosition(caretPos);
				}
				else if (e.getID() == KeyEvent.KEY_RELEASED)
				{
					toggleOverwrite = true;
				}
			}

			return super.processKeyBinding(ks, e, condition, pressed);
		}
		return false;
	}

	@Override
	public void setFormatter(AbstractFormatter format)
	{
		super.setFormatter(format);
	}

	@Override
	public String toString()
	{
		return scriptable.toString();
	}


	public boolean stopUIEditing(boolean looseFocus)
	{
		try
		{
			commitEdit();
		}
		catch (ParseException e)
		{
			return false;
		}

		if (!isValueValid)
		{
			application.invokeLater(new Runnable()
			{
				public void run()
				{
					requestFocus();
				}
			});
			return false;
		}

		if (looseFocus && canLooseFocus() && eventExecutor.mustFireFocusLostCommand())
		{
			eventExecutor.skipNextFocusLost();
			eventExecutor.fireLeaveCommands(this, false, IEventExecutor.MODIFIERS_UNSPECIFIED);
		}
		return true;
	}

	protected boolean canLooseFocus()
	{
		return true;
	}

	public String getId()
	{
		return (String)getClientProperty("Id"); //$NON-NLS-1$
	}

	private Color opaqueBackground;
	private static final Color TRANSPARENT_BACKGROUND = new Color(0, 0, 0, 0);

	// work around transparency problem on MacOS Leopard
	// http://lists.apple.com/archives/java-dev/2007/Nov/msg00253.html
	@Override
	public void setOpaque(boolean opaque)
	{
		if (opaque != isOpaque())
		{
			if (opaque)
			{
				super.setBackground(opaqueBackground);
			}
			else if (opaqueBackground != null)
			{
				opaqueBackground = getBackground();
				super.setBackground(TRANSPARENT_BACKGROUND);
			}
		}
		super.setOpaque(opaque);
	}

	// work around transparency problem in MacOS Leopard
	// http://lists.apple.com/archives/java-dev/2007/Nov/msg00253.html
	@Override
	public void setBackground(Color color)
	{
		if (isOpaque())
		{
			super.setBackground(color);
		}
		else
		{
			opaqueBackground = color;
		}
	}

	private boolean isIgnoreOnRender;

	public void setIgnoreOnRender(boolean isIgnoreOnRender)
	{
		this.isIgnoreOnRender = isIgnoreOnRender;
	}

	private TransferHandler textTransferHandler;

	/*
	 * @see com.servoy.j2db.dnd.ISupportTextTransfer#clearTransferHandler()
	 */
	public void clearTransferHandler()
	{
		textTransferHandler = getTransferHandler();
		setTransferHandler(null);
	}

	public TransferHandler getTextTransferHandler()
	{
		return textTransferHandler;
	}

	@Override
	public void copy()
	{
		if (textTransferHandler != null)
		{
			Action copyAction = FormDataTransferHandler.getCopyFormDataAction();
			copyAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, (String)copyAction.getValue(Action.NAME),
				EventQueue.getMostRecentEventTime(), getCurrentEventModifiers()));
		}
		else super.copy();
	}

	@Override
	public void cut()
	{
		if (textTransferHandler != null && isEditable() && isEnabled())
		{
			Action cutAction = FormDataTransferHandler.getCutFormDataAction();
			cutAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, (String)cutAction.getValue(Action.NAME),
				EventQueue.getMostRecentEventTime(), getCurrentEventModifiers()));
		}
		else super.cut();
	}

	@Override
	public void paste()
	{
		if (textTransferHandler != null && isEditable() && isEnabled())
		{
			Action pasteAction = FormDataTransferHandler.getPasteFormDataAction();
			pasteAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, (String)pasteAction.getValue(Action.NAME),
				EventQueue.getMostRecentEventTime(), getCurrentEventModifiers()));
		}
		else super.paste();
	}

	private int getCurrentEventModifiers()
	{
		int modifiers = 0;
		AWTEvent currentEvent = EventQueue.getCurrentEvent();
		if (currentEvent instanceof InputEvent)
		{
			modifiers = ((InputEvent)currentEvent).getModifiers();
		}
		else if (currentEvent instanceof ActionEvent)
		{
			modifiers = ((ActionEvent)currentEvent).getModifiers();
		}
		return modifiers;
	}


	@Override
	public void setPlaceholderText(String text)
	{
		PromptSupport.uninstall(this);
		PromptSupport.setPrompt(application.getI18NMessageIfPrefixed(text), this);
		repaint();
	}
}
