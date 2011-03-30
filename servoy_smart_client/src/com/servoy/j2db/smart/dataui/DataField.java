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


import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.DecimalFormatSymbols;
import java.text.Format;
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
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
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

import com.servoy.j2db.ControllerUndoManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IFormUIInternal;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.ISmartClientApplication;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.CustomValueList;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IEditListener;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.dataprocessing.JSDataSet;
import com.servoy.j2db.dataprocessing.ValueListFactory;
import com.servoy.j2db.dataprocessing.ValueFactory.DbIdentValue;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.ui.IDataRenderer;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.IScriptBaseMethods;
import com.servoy.j2db.ui.IScriptFieldMethods;
import com.servoy.j2db.ui.ISupportCachedLocationAndSize;
import com.servoy.j2db.ui.RenderEventExecutor;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.FormatParser;
import com.servoy.j2db.util.HtmlUtils;
import com.servoy.j2db.util.ISkinnable;
import com.servoy.j2db.util.ITagResolver;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.RoundHalfUpDecimalFormat;
import com.servoy.j2db.util.StateFullSimpleDateFormat;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.UIUtils;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.docvalidator.LengthDocumentValidator;
import com.servoy.j2db.util.docvalidator.LowerCaseDocumentValidator;
import com.servoy.j2db.util.docvalidator.NumberDocumentValidator;
import com.servoy.j2db.util.docvalidator.UpperCaseDocumentValidator;
import com.servoy.j2db.util.docvalidator.ValidatingDocument;
import com.servoy.j2db.util.gui.FixedMaskFormatter;

/**
 * Runtime swing field
 * @author jblok, jcompagner
 */
public class DataField extends JFormattedTextField implements IDisplayData, IFieldComponent, ISkinnable, IScriptFieldMethods, ISupportCachedLocationAndSize
{
	private static final long serialVersionUID = 1L;

	private static final String MAX_LENGTH_VALIDATOR = "maxLength"; //$NON-NLS-1$

	private String tooltip;

	// when a parse error occurred the value should always be set in setValue(), otherwise unsaved data is displayed in the field
	private boolean parseErrorOccurred = false;
	private MouseAdapter rightclickMouseAdapter = null;

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
			format.setGroupingUsed(true);
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
			if (value == null || value.toString().trim().equals("")) { //$NON-NLS-1$
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

	public class NullDateFormatter extends DateFormatter
	{
		private static final long serialVersionUID = 1L;

		private final boolean editFormatter;

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
									if (Boolean.TRUE.equals(UIUtils.getUIProperty(DataField.this, IApplication.DATE_FORMATTERS_ROLL_INSTEAD_OF_ADD,
										Boolean.FALSE)))
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

		/**
		 * @see javax.swing.JFormattedTextField.AbstractFormatter#stringToValue(java.lang.String)
		 */
		@Override
		public Object stringToValue(String text) throws ParseException
		{
			if (text == null || text.trim().equals("") || text.equals(editFormat) || text.equals(displayFormat)) return null; //$NON-NLS-1$
			try
			{
				StateFullSimpleDateFormat format = (StateFullSimpleDateFormat)getFormat();
				format.setOriginal((Date)getValue());
				super.stringToValue(text);
				return format.getMergedDate();
			}
			catch (ParseException e)
			{
				throw e;
			}
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
			FixedMaskFormatter maskFormatter = new FixedMaskFormatter(maskPattern)
			{
				/**
				 * @see com.servoy.j2db.smart.dataui.ServoyMaskFormatter#valueToString(java.lang.Object)
				 */
				@Override
				public String valueToString(Object value) throws ParseException
				{
					return super.valueToString(NullDateFormatter.this.valueToString(value));
				}

				/**
					 * @see com.servoy.j2db.smart.dataui.ServoyMaskFormatter#stringToValue(java.lang.String)
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
			};
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

	DataField(IApplication application, IValueList list)
	{
		this(application);
		this.list = list;
	}

	public DataField(IApplication application)
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

		addKeyListener(new KeyAdapter()
		{

			@Override
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
				{
					eventExecutor.actionPerformed(e.getModifiers());
				}
			}

			@Override
			public void keyReleased(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
				{
					restorePreviousValidValue();
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

	public boolean needEditListner()
	{
		return true;
	}

	protected EditProvider editProvider = null;

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

	public void setMaxLength(int i)
	{
		// do not set max length check when it has been set in setFormat() 
		if (list == null && editorDocument.getValidator(MAX_LENGTH_VALIDATOR) == null)
		{
			editorDocument.setValidator(MAX_LENGTH_VALIDATOR, new LengthDocumentValidator(i));
		}
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


	/**
	 * Returns an AbstractFormatterFactory suitable for the passed in Object type.
	 * 
	 * @see JFormattedTextField.getDefaultFormatterFactory(Object)
	 */
	protected AbstractFormatterFactory getDefaultFormatterFactory(Object type)
	{
		if (type instanceof DateFormat)
		{
			return new DefaultFormatterFactory(new DateFormatter((DateFormat)type));
		}
		if (type instanceof NumberFormat)
		{
			return new DefaultFormatterFactory(new NumberFormatter((NumberFormat)type));
		}
		if (type instanceof Format)
		{
			return new DefaultFormatterFactory(new InternationalFormatter((Format)type));
		}
		if (type instanceof Date)
		{
			return new DefaultFormatterFactory(new DateFormatter());
		}
		if (type instanceof Number)
		{
			AbstractFormatter displayFormatter = new NumberFormatter(new RoundHalfUpDecimalFormat("#.##", application.getLocale())); //$NON-NLS-1$
			return new DefaultFormatterFactory(displayFormatter, displayFormatter, displayFormatter);
		}
		return new DefaultFormatterFactory(new DefaultFormatter());
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
				setFormatterFactory(new DefaultFormatterFactory(new InternationalFormatter()));
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

	public int getDataType()
	{
		return dataType;
	}


	/*
	 * format---------------------------------------------------
	 */
	private String completeFormat;
	protected String displayFormat;
	protected String editFormat;
	private AbstractFormatterFactory saveFormatter;
	private final AbstractFormatterFactory disabledFormatter = new DefaultFormatterFactory(new InternationalFormatter());

	private List<ILabel> labels;

	public String getFormat()
	{
		if (displayFormat == editFormat) return displayFormat;
		else return displayFormat + "|" + editFormat; //$NON-NLS-1$
	}

	public void js_setFormat(String format)
	{
		setFormat(dataType, application.getI18NMessageIfPrefixed(format));
	}

	public String js_getFormat()
	{
		return completeFormat;
	}

	public void setFormat(int dataType, String format)
	{
		this.dataType = dataType;
		this.completeFormat = format;
		this.displayFormat = format;
		this.editFormat = format;
		if (format != null && format.length() != 0)
		{
			FormatParser fp = new FormatParser(format);

			displayFormat = fp.getDisplayFormat();
			editFormat = fp.getEditFormat();

			if (fp.isAllLowerCase())
			{
				editorDocument.setValidator("LowerCaseDocumentValidator", new LowerCaseDocumentValidator()); //$NON-NLS-1$
				TextFormatter display = new TextFormatter();
				TextFormatter edit = new TextFormatter();
				setFormatterFactory(new DefaultFormatterFactory(display, display, edit, edit));
			}
			else if (fp.isAllUpperCase())
			{
				editorDocument.setValidator("UpperCaseDocumentValidator", new UpperCaseDocumentValidator()); //$NON-NLS-1$
				TextFormatter display = new TextFormatter();
				TextFormatter edit = new TextFormatter();
				setFormatterFactory(new DefaultFormatterFactory(display, display, edit, edit));
			}
			else if (fp.isNumberValidator())
			{
				editorDocument.setValidator("NumberDocumentValidator", new NumberDocumentValidator()); //$NON-NLS-1$
				TextFormatter display = new TextFormatter();
				TextFormatter edit = new TextFormatter();
				setFormatterFactory(new DefaultFormatterFactory(display, display, edit, edit));
			}
			else
			{
				int maxLength = fp.getMaxLength() == null ? -1 : fp.getMaxLength().intValue();
				// if there is no display format, but the max lenght is set, then generate a display format.
				if (maxLength != -1 && (displayFormat == null || displayFormat.length() == 0))
				{
					char[] chars = new char[maxLength];
					for (int i = 0; i < chars.length; i++)
						chars[i] = '#';
					displayFormat = new String(chars);
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
								if (editFormat.equals("raw") || editFormat.equals("")) editFormat = displayFormat; //$NON-NLS-1$//$NON-NLS-2$

								displayFormatter = new NullNumberFormatter(new RoundHalfUpDecimalFormat(displayFormat, application.getLocale()));// example: $#,###.##
								editFormatter = new NullNumberFormatter(new RoundHalfUpDecimalFormat(editFormat, application.getLocale()), maxLength);
								setFormatterFactory(new DefaultFormatterFactory(displayFormatter, displayFormatter, editFormatter, editFormatter));
								break;
							case IColumnTypes.INTEGER :
								if (editFormat.equals("raw") || editFormat.equals("")) editFormat = displayFormat; //$NON-NLS-1$//$NON-NLS-2$

								displayFormatter = new NullNumberFormatter(new RoundHalfUpDecimalFormat(displayFormat, application.getLocale()));// example: $#,###.##
								editFormatter = new NullNumberFormatter(new RoundHalfUpDecimalFormat(editFormat, application.getLocale()), maxLength);
								setFormatterFactory(new DefaultFormatterFactory(displayFormatter, displayFormatter, editFormatter, editFormatter));
								break;
							case IColumnTypes.DATETIME :
								boolean mask = fp.isMask();
								char placeHolder = fp.getPlaceHolderCharacter();
								if (mask || editFormat.equals("raw") || editFormat.equals("")) editFormat = displayFormat; //$NON-NLS-1$//$NON-NLS-2$

								displayFormatter = new NullDateFormatter(new StateFullSimpleDateFormat(displayFormat, false));
								editFormatter = new NullDateFormatter(new StateFullSimpleDateFormat(editFormat, Boolean.TRUE.equals(UIUtils.getUIProperty(this,
									IApplication.DATE_FORMATTERS_LENIENT, Boolean.TRUE))), !mask);
								if (mask)
								{
									editFormatter = ((NullDateFormatter)editFormatter).getMaskFormatter(placeHolder);
								}
								else
								{
									// date formats are default in override mode
									setCaret(getOvertypeCaret());
								}
								setFormatterFactory(new DefaultFormatterFactory(displayFormatter, displayFormatter, editFormatter)); // example: MM/dd/yyyy
								break;
							default :
								displayFormatter = new ServoyMaskFormatter(displayFormat, true);
								editFormatter = new ServoyMaskFormatter(displayFormat, false);

								if (fp.isRaw())
								{
									((ServoyMaskFormatter)editFormatter).setValueContainsLiteralCharacters(false);
									((ServoyMaskFormatter)displayFormatter).setValueContainsLiteralCharacters(false);
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
								setFormatterFactory(new DefaultFormatterFactory(displayFormatter, displayFormatter, editFormatter));
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
			setFormatterFactory(new DefaultFormatterFactory(display, display, edit, edit));
		}
	}


	/*
	 * caret---------------------------------------------------
	 */
	public int js_getCaretPosition()
	{
		return getCaretPosition();
	}

	public void js_setCaretPosition(int pos)
	{
		if (pos < 0)
		{
			pos = 0;
		}
		if (pos > getDocument().getLength())
		{
			pos = getDocument().getLength();
		}
		setCaretPosition(pos);
	}


	/*
	 * bgcolor---------------------------------------------------
	 */
	public String js_getBgcolor()
	{
		return PersistHelper.createColorString(getBackground());
	}

	public void js_setBgcolor(String clr)
	{
		setBackground(PersistHelper.createColor(clr));
	}


	/*
	 * fgcolor---------------------------------------------------
	 */
	public String js_getFgcolor()
	{
		return PersistHelper.createColorString(getForeground());
	}

	public void js_setFgcolor(String clr)
	{
		setForeground(PersistHelper.createColor(clr));
	}


	public void js_setBorder(String spec)
	{
		Border border = ComponentFactoryHelper.createBorder(spec);
		Border oldBorder = getBorder();
		if (oldBorder instanceof CompoundBorder && ((CompoundBorder)oldBorder).getInsideBorder() != null)
		{
			Insets insets = ((CompoundBorder)oldBorder).getInsideBorder().getBorderInsets(this);
			setBorder(BorderFactory.createCompoundBorder(border, BorderFactory.createEmptyBorder(insets.top, insets.left, insets.bottom, insets.right)));
		}
		else
		{
			setBorder(border);
		}
	}

	public String js_getBorder()
	{
		return ComponentFactoryHelper.createBorderString(getBorder());
	}

	/*
	 * visible---------------------------------------------------
	 */
	public boolean js_isVisible()
	{
		return isVisible();
	}

	public void js_setVisible(boolean b)
	{
		setVisible(b);
	}

	public void setComponentVisible(boolean b_visible)
	{
		setVisible(b_visible);
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
	 * opaque---------------------------------------------------
	 */
	public boolean js_isTransparent()
	{
		return !isOpaque();
	}

	public void js_setTransparent(boolean b)
	{
		setOpaque(!b);
		repaint();
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
	 * readonly---------------------------------------------------
	 */
	public boolean isReadOnly()
	{
		return !isEditable();
	}

	public boolean js_isReadOnly()
	{
		return isReadOnly();
	}

	protected boolean editState;

	public void js_setReadOnly(boolean b)
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

	public boolean js_isEditable()
	{
		return isEditable();
	}

	public void js_setEditable(boolean b)
	{
		setEditable(b);
	}


	/*
	 * location---------------------------------------------------
	 */

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

	public void js_setLocation(int x, int y)
	{
		cachedLocation = new Point(x, y);
		setLocation(x, y);
	}

	public Point getCachedLocation()
	{
		return cachedLocation;
	}

	/*
	 * client properties for ui---------------------------------------------------
	 */

	public void js_putClientProperty(Object key, Object value)
	{
		putClientProperty(key, value);
		if (IApplication.DATE_FORMATTERS_LENIENT.equals(key))
		{
			AbstractFormatterFactory ff = getFormatterFactory();
			if (ff instanceof DefaultFormatterFactory)
			{
				AbstractFormatter formatter = ((DefaultFormatterFactory)ff).getEditFormatter();
				if (formatter instanceof NullDateFormatter)
				{
					((NullDateFormatter)formatter).setLenient(Boolean.TRUE.equals(UIUtils.getUIProperty(this, IApplication.DATE_FORMATTERS_LENIENT,
						Boolean.TRUE)));
				}
			}
		}
	}

	public Object js_getClientProperty(Object key)
	{
		return getClientProperty(key);
	}


	/*
	 * size---------------------------------------------------
	 */
	private Dimension cachedSize;

	public void js_setSize(int x, int y)
	{
		cachedSize = new Dimension(x, y);
		setSize(x, y);
	}

	public Dimension getCachedSize()
	{
		return cachedSize;
	}

	public int js_getWidth()
	{
		return getSize().width;
	}

	public int js_getHeight()
	{
		return getSize().height;
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

	public String js_getTitleText()
	{
		return Text.processTags(titleText, resolver);
	}

	/*
	 * tooltip---------------------------------------------------
	 */
	public void js_setToolTipText(String txt)
	{
		setToolTipText(txt);
	}

	public String js_getToolTipText()
	{
		return getToolTipText();
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
		if (dataProviderID != null && dataProviderID.startsWith(ScriptVariable.GLOBAL_DOT_PREFIX)) return;

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
			setEditable(true);// allow search
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

	public void js_requestFocus(final Object[] vargs)
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
						if (vargs != null && vargs.length >= 1 && !Utils.getAsBoolean(vargs[0]))
						{
							eventExecutor.skipNextFocusGain();
						}
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

	public String js_getSelectedText()
	{
		return this.getSelectedText();
	}

	public void js_selectAll()
	{
		this.selectAll();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.dataui.IBaseScriptMethods#js_replaceSelectedText(java.lang.String)
	 */
	public void js_replaceSelectedText(String s)
	{
		if (editProvider != null) editProvider.startEdit();
		this.replaceSelection(s);
		if (editProvider != null) editProvider.commitData();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.dataui.IBaseScriptMethods#js_setFont(java.lang.String)
	 */
	public void js_setFont(String spec)
	{
		setFont(PersistHelper.createFont(spec));
	}

	public String js_getFont()
	{
		return PersistHelper.createFontString(getFont());
	}

	public String js_getDataProviderID()
	{
		return getDataProviderID();
	}

	public String js_getElementType()
	{
		return IScriptBaseMethods.TEXT_FIELD;
	}

	public String js_getName()
	{
		String jsName = getName();
		if (jsName != null && jsName.startsWith(ComponentFactory.WEB_ID_PREFIX)) jsName = null;
		return jsName;
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
			String name = list.getName();
			ValueList valuelist = application.getFlattenedSolution().getValueList(name);
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
			}
		}
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
		if (e.getKeyCode() == KeyEvent.VK_DECIMAL && isEditable())
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
		return js_getElementType() + "[name:" + js_getName() + ",x:" + js_getLocationX() + ",y:" + js_getLocationY() + ",width:" + js_getWidth() + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
			",height:" + js_getHeight() + ",value:" + getValueObject() + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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

		if (eventExecutor.mustFireFocusLostCommand())
		{
			eventExecutor.skipNextFocusLost();
			eventExecutor.fireLeaveCommands(this, false, IEventExecutor.MODIFIERS_UNSPECIFIED);
		}
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

	@Override
	protected void paintComponent(Graphics g)
	{
		if (!isIgnoreOnRender && eventExecutor != null) eventExecutor.fireOnRender(this, hasFocus());
		super.paintComponent(g);
	}

	/*
	 * @see com.servoy.j2db.ui.ISupportOnRenderCallback#getRenderEventExecutor()
	 */
	public RenderEventExecutor getRenderEventExecutor()
	{
		return eventExecutor;
	}

	private boolean isIgnoreOnRender;

	public void setIgnoreOnRender(boolean isIgnoreOnRender)
	{
		this.isIgnoreOnRender = isIgnoreOnRender;
	}
}
