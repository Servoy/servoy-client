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
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.text.Format;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JRadioButton;
import javax.swing.JToggleButton;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.text.Document;

import com.servoy.base.util.ITagResolver;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IDisplayRelatedData;
import com.servoy.j2db.dataprocessing.IEditListener;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.gui.editlist.JNavigableEditList;
import com.servoy.j2db.gui.editlist.NavigableCellEditor;
import com.servoy.j2db.gui.editlist.NavigableCellRenderer;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.smart.dataui.DataComboBox.VariableSizeJSeparator;
import com.servoy.j2db.ui.IDataRenderer;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.IFormattingComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.IScrollPane;
import com.servoy.j2db.ui.ISupportCachedLocationAndSize;
import com.servoy.j2db.ui.ISupportOnRender;
import com.servoy.j2db.ui.ISupportValueList;
import com.servoy.j2db.ui.scripting.AbstractRuntimeField;
import com.servoy.j2db.ui.scripting.AbstractRuntimeScrollableValuelistComponent;
import com.servoy.j2db.ui.scripting.AbstractRuntimeValuelistComponent;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.EnableScrollPanel;
import com.servoy.j2db.util.ISupplyFocusChildren;
import com.servoy.j2db.util.RoundHalfUpDecimalFormat;
import com.servoy.j2db.util.StateFullSimpleDateFormat;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.UIUtils;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.editlist.IEditListEditor;
import com.servoy.j2db.util.editlist.JEditList;
import com.servoy.j2db.util.model.ComboModelListModelWrapper;

import sun.java2d.SunGraphics2D;

/**
 * Runtime swing radio/check box choice component
 * @author jblok, jcompagner
 */
public class DataChoice extends EnableScrollPanel implements IDisplayData, IFieldComponent, IScrollPane, IDisplayRelatedData, ListDataListener,
	ISupplyFocusChildren<Component>, ISupportCachedLocationAndSize, ISupportValueList, IFormattingComponent, ISupportOnRender
{
	private String dataProviderID;
	protected final ComboModelListModelWrapper list;
	protected final JEditList enclosedComponent;
	private JComponent rendererComponent;
	private JComponent editorComponent;
	private String tooltip;
	private Insets margin;
	private int halign;
	private final EventExecutor eventExecutor;
	protected final IApplication application;
	private final int choiceType;
	private MouseAdapter rightclickMouseAdapter = null;
	private IValueList vl;
	private final AbstractRuntimeValuelistComponent<IFieldComponent> scriptable;
	private Format format;
	private AbstractCell cellEditor;

	public DataChoice(IApplication app, AbstractRuntimeValuelistComponent<IFieldComponent> scriptable, IValueList vl, int choiceType, boolean multiselect)
	{
		super();
		setHorizontalAlignment(SwingConstants.LEFT);
		if (choiceType == Field.RADIOS || choiceType == Field.CHECKS)
		{
			setBorder(null);
		}
		application = app;
		this.vl = vl;
		this.choiceType = choiceType;
		list = new ComboModelListModelWrapper(vl, choiceType != Field.SPINNER, (choiceType == Field.MULTISELECT_LISTBOX || choiceType == Field.LIST_BOX));
		enclosedComponent = new JNavigableEditList();
		enclosedComponent.getActionMap().put("enter", null);
		enclosedComponent.getActionMap().put("select_current", new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (enclosedComponent.isEditable() && cellEditor != null)
				{
					enclosedComponent.editCellAt(enclosedComponent.getSelectedIndex(), e);
					cellEditor.doSelect(true, false);
				}
			}
		});
		eventExecutor = new EventExecutor(this, enclosedComponent);
		enclosedComponent.addKeyListener(eventExecutor);
		enclosedComponent.setModel(createJListModel(list));

		if (choiceType == Field.RADIOS)
		{
			enclosedComponent.setCellRenderer(new NavigableCellRenderer(new RadioCell()));
			cellEditor = new RadioCell();
			enclosedComponent.setCellEditor(new NavigableCellEditor(cellEditor));
		}
		else if (choiceType == Field.CHECKS)
		{
			enclosedComponent.setCellRenderer(new NavigableCellRenderer(new CheckBoxCell()));
			cellEditor = new CheckBoxCell();
			enclosedComponent.setCellEditor(new NavigableCellEditor(cellEditor));
		}
		else
		{
			enclosedComponent.setCellRenderer(new LabelCell(shouldPaintSelection()));
			cellEditor = new LabelCell(shouldPaintSelection());
			enclosedComponent.setCellEditor(cellEditor);
		}

		setMultiValueSelect(multiselect);
		this.scriptable = scriptable;
		if (scriptable instanceof AbstractRuntimeScrollableValuelistComponent< ? , ? >)
		{
			((AbstractRuntimeScrollableValuelistComponent<IFieldComponent, JComponent>)scriptable).setField(enclosedComponent);
			((AbstractRuntimeScrollableValuelistComponent<IFieldComponent, JComponent>)scriptable).setList(list);
		}

//		enclosedComponent.setPrototypeCellValue(new Integer(0));

		getViewport().setView(enclosedComponent);
	}

	/**
	 * @return the choiceType
	 */
	public int getChoiceType()
	{
		return choiceType;
	}

	/**
	 * @return the rendererComponent
	 */
	public JComponent getRendererComponent()
	{
		return rendererComponent;
	}

	/**
	 * @return the enclosedComponent
	 */
	public JEditList getEnclosedComponent()
	{
		return enclosedComponent;
	}

	public void installFormat(ComponentFormat componentFormat)
	{
		if (!componentFormat.parsedFormat.isEmpty())
		{
			String displayFormat = componentFormat.parsedFormat.getDisplayFormat();
			try
			{
				switch (Column.mapToDefaultType(componentFormat.uiType))
				{
					case IColumnTypes.NUMBER :
						format = new RoundHalfUpDecimalFormat(displayFormat, application.getLocale());
						break;
					case IColumnTypes.INTEGER :
						format = new RoundHalfUpDecimalFormat(displayFormat, application.getLocale());
						break;
					case IColumnTypes.DATETIME :
						format = new StateFullSimpleDateFormat(displayFormat,
							Boolean.TRUE.equals(UIUtils.getUIProperty(this, IApplication.DATE_FORMATTERS_LENIENT, Boolean.TRUE)));
						// format = new SimpleDateFormat(formatString);
						break;
					default :
						// TODO Jan/Johan what to do here? Should we create our own MaskFormatter? Where we can insert a mask??
						break;
				}
			}
			catch (Exception ex)
			{
				Debug.error(ex);
			}
		}
	}

	protected ListModel createJListModel(ComboModelListModelWrapper comboModel)
	{
		return comboModel;
	}

	protected boolean isRowSelected(int idx)
	{
		return list.isRowSelected(idx);
	}

	protected void setElementAt(Object b, int idx)
	{
		list.setElementAt(b, idx);
	}

	protected boolean shouldPaintSelection()
	{
		return true;
	}

	/**
	 *
	 */
	private void configureEditorAndRenderer()
	{
		// TODO Auto-generated method stub

	}

	protected void setMultiValueSelect(boolean multiselect)
	{
		if (choiceType == Field.RADIOS || choiceType == Field.LIST_BOX)
		{
			list.setMultiValueSelect(false);
		}
		else
		{
			list.setMultiValueSelect(multiselect);
		}
	}

	public final AbstractRuntimeField<IFieldComponent> getScriptObject()
	{
		return scriptable;
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

	private boolean wasEditable;

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
		repaint(); // foreground color changes
		if (!isValueValid)
		{
			previousValidValue = oldVal;
			application.invokeLater(new Runnable()
			{
				public void run()
				{
					enclosedComponent.requestFocus();
				}
			});
		}
		else
		{
			previousValidValue = null;
		}
	}

	public void notifyLastNewValueWasChange(Object oldVal, Object newVal)
	{
		if (previousValidValue != null)
		{
			oldVal = previousValidValue;
		}

		previousValue = newVal;

		eventExecutor.fireChangeCommand(oldVal, newVal, false, this);

		// if change cmd is not succeeded also don't call action cmd?
		if (isValueValid)
		{
			eventExecutor.fireActionCommand(false, this);
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
						eventExecutor.fireRightclickCommand(true, DataChoice.this, e.getModifiers(), e.getPoint());
					}
				}
			};
			enclosedComponent.addMouseListener(rightclickMouseAdapter);
			addMouseListener(rightclickMouseAdapter);
		}
	}

	public void setValidationEnabled(boolean b)
	{
		if (eventExecutor.getValidationEnabled() == b) return;

		if (vl.getFallbackValueList() != null)
		{
			if (b)
			{
				list.register(vl);
			}
			else
			{
				list.register(vl.getFallbackValueList());
			}
		}

		boolean prevEditState = editState;
		if (b)
		{
			setEditable(wasEditable);
		}
		else
		{
			wasEditable = !isReadOnly();
			if (!Boolean.TRUE.equals(application.getClientProperty(IApplication.LEAVE_FIELDS_READONLY_IN_FIND_MODE)))
			{
				setEditable(true);
			}
		}

		eventExecutor.setValidationEnabled(b);
		editState = prevEditState;
	}

	public void setSelectOnEnter(boolean b)
	{
		eventExecutor.setSelectOnEnter(b);
	}

	//_____________________________________________________________

	public void destroy()
	{
		list.deregister();
	}

	public void setMaxLength(int i)
	{
		//ignore
	}

	public void setMargin(Insets i)
	{
		margin = i;
	}

	public Insets getMargin()
	{
		return margin;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.awt.Component#setName(java.lang.String)
	 */
	@Override
	public void setName(String name)
	{
		super.setName(name);
		enclosedComponent.setName(name);
	}

	@Override
	public void setToolTipText(String tip)
	{
		if (tip != null && tip.indexOf("%%") != -1) //$NON-NLS-1$
		{
			tooltip = tip;
		}
		else
		{
			super.setToolTipText(tip);
			enclosedComponent.setToolTipText(tip);
		}
	}

	@Override
	public void setFont(Font font)
	{
		super.setFont(font);
		if (enclosedComponent != null) enclosedComponent.setFont(font);
	}

	@Override
	public Font getFont()
	{
		if (enclosedComponent != null)
		{
			return enclosedComponent.getFont();
		}
		return super.getFont();
	}

	public Document getDocument()
	{
		return null;
	}

	public abstract class AbstractCell extends AbstractCellEditor implements IEditListEditor, ListCellRenderer, ActionListener
	{
		public void doSelect(boolean isCommandKeyDown, boolean isShiftDown)
		{
			if (editorComponent instanceof JToggleButton) ((JToggleButton)editorComponent).doClick();
		}

		protected void createRenderer()
		{
			rendererComponent.setOpaque(false);
			((JToggleButton)rendererComponent).setMargin(margin);
			((JToggleButton)rendererComponent).setHorizontalAlignment(halign);
		}

		protected void createEditor()
		{
			editorComponent.setOpaque(false);
			((JToggleButton)editorComponent).addActionListener(this);
			((JToggleButton)editorComponent).setMargin(margin);
			((JToggleButton)editorComponent).setHorizontalAlignment(halign);
		}

		public Component getListCellEditorComponent(JEditList editList, Object value, boolean isSelected, int index)
		{
			if (editorComponent == null) createEditor();
			editorComponent.setFont(editList.getFont());
			((JToggleButton)editorComponent).setSelected(isRowSelected(index));
			editorComponent.setForeground(editList.getForeground());
//			editorComponent.setBackground(editList.getBackground());
			if (value == null)
			{
				((JToggleButton)editorComponent).setText(""); //$NON-NLS-1$
			}
			else
			{
				Object formattedValue = value;
				if (!"".equals(formattedValue) && format != null && formattedValue != null && !(formattedValue instanceof String)) //$NON-NLS-1$
				{
					try
					{
						formattedValue = format.format(formattedValue);
					}
					catch (IllegalArgumentException ex)
					{
						Debug.error("Error formatting value for combobox " + dataProviderID + ", " + ex); //$NON-NLS-1$//$NON-NLS-2$
					}
				}
				((JToggleButton)editorComponent).setText(resolver != null ? Text.processTags(formattedValue.toString(), resolver) : formattedValue.toString());
			}
			return editorComponent;
		}

		public Object getCellEditorValue()
		{
			return new Boolean(((JToggleButton)editorComponent).isSelected());
		}

		public Component getListCellRendererComponent(JList editList, Object value, int index, boolean isSelected, boolean cellHasFocus)
		{
			if (rendererComponent == null) createRenderer();
			((JToggleButton)rendererComponent).setSelected(isRowSelected(index));
			rendererComponent.setFont(editList.getFont());
			rendererComponent.setEnabled(editList.isEnabled());
			rendererComponent.setForeground(editList.getForeground());
//			rendererComponent.setBackground(editList.getBackground());
			if (value == null)
			{
				((JToggleButton)rendererComponent).setText(""); //$NON-NLS-1$
			}
			else
			{
				Object formattedValue = value;
				if (!"".equals(formattedValue) && format != null && formattedValue != null && !(formattedValue instanceof String)) //$NON-NLS-1$
				{
					try
					{
						formattedValue = format.format(formattedValue);
					}
					catch (IllegalArgumentException ex)
					{
						Debug.error("Error formatting value for combobox " + dataProviderID + ", " + ex); //$NON-NLS-1$//$NON-NLS-2$
					}
				}
				((JToggleButton)rendererComponent).setText(
					resolver != null ? Text.processTags(formattedValue.toString(), resolver) : formattedValue.toString());
			}
			return rendererComponent;
		}

		public void actionPerformed(ActionEvent e)
		{
			stopCellEditing();
		}

		private boolean cellEditStoping;

		@Override
		public boolean stopCellEditing()
		{
			if (cellEditStoping) return true;
			else
			{
				try
				{
					cellEditStoping = true;
					return super.stopCellEditing();
				}
				finally
				{
					cellEditStoping = false;
				}
			}
		}
	}

	public class RadioCell extends AbstractCell
	{
		public RadioCell()
		{
			super();
		}

		@Override
		protected void createRenderer()
		{
			rendererComponent = new JRadioButton()
			{
				private final boolean initialized = true;

				@Override
				public Color getForeground()
				{
					if (initialized && DataChoice.this.isValueValid())
					{
						return super.getForeground();
					}
					return Color.red;
				}
			};
			super.createRenderer();
		}

		@Override
		protected void createEditor()
		{
			editorComponent = new JRadioButton()
			{
				private final boolean initialized = true;

				@Override
				public Color getForeground()
				{
					if (initialized && DataChoice.this.isValueValid())
					{
						return super.getForeground();
					}
					return Color.red;
				}
			};
			super.createEditor();
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			stopCellEditing();
		}
	}

	public class CheckBoxCell extends AbstractCell
	{
		public CheckBoxCell()
		{
			super();
		}

		@Override
		protected void createRenderer()
		{
			rendererComponent = new JCheckBox()
			{
				private final boolean initialized = true;

				@Override
				public Color getForeground()
				{
					if (initialized && DataChoice.this.isValueValid())
					{
						return super.getForeground();
					}
					return Color.red;
				}
			};
			super.createRenderer();
		}

		@Override
		protected void createEditor()
		{
			editorComponent = new JCheckBox()
			{
				private final boolean initialized = true;

				@Override
				public Color getForeground()
				{
					if (initialized && DataChoice.this.isValueValid())
					{
						return super.getForeground();
					}
					return Color.red;
				}
			};
			super.createEditor();
		}
	}

	public class LabelCell extends AbstractCell implements MouseListener
	{
		private Border marginBorder;
		private final boolean paintSelection;

		public LabelCell(boolean paintSelection)
		{
			super();
			this.paintSelection = paintSelection;
		}

		@Override
		protected void createRenderer()
		{
			rendererComponent = new JLabel()
			{
				private final boolean initialized = true;

				@Override
				public Color getForeground()
				{
					if (initialized && DataChoice.this.isValueValid())
					{
						return super.getForeground();
					}
					return Color.red;
				}
			};
			if (margin != null)
			{
				marginBorder = BorderFactory.createEmptyBorder(margin.top, margin.left, margin.bottom, margin.right);
				((JLabel)rendererComponent).setBorder(marginBorder);
			}
			((JLabel)rendererComponent).setHorizontalAlignment(halign);
		}

		@Override
		protected void createEditor()
		{
			editorComponent = new JLabel()
			{
				private final boolean initialized = true;

				@Override
				public Color getForeground()
				{
					if (initialized && DataChoice.this.isValueValid())
					{
						return super.getForeground();
					}
					return Color.red;
				}
			};
			editorComponent.addMouseListener(this);
			if (margin != null) ((JLabel)editorComponent).setBorder(marginBorder);
			((JLabel)editorComponent).setHorizontalAlignment(halign);
		}

		@Override
		public Component getListCellEditorComponent(JEditList editList, Object value, boolean isSelected, int index)
		{
			if (IValueList.SEPARATOR.equals(value))
			{
				return null;
			}
			if (editorComponent == null) createEditor();
			editorComponent.setFont(editList.getFont());
			if (isRowSelected(index) && paintSelection)
			{
				((JLabel)editorComponent).setBackground(editList.getSelectionBackground());
				editorComponent.setForeground(editList.getSelectionForeground());
				editorComponent.setOpaque(true);
			}
			else
			{
				((JLabel)editorComponent).setBackground(editList.getBackground());
				editorComponent.setForeground(editList.getForeground());
				editorComponent.setOpaque(false);
			}

			if (value == null)
			{
				((JLabel)editorComponent).setText(""); //$NON-NLS-1$
			}
			else
			{
				Object formattedValue = value;
				if (!"".equals(formattedValue) && format != null && formattedValue != null && !(formattedValue instanceof String)) //$NON-NLS-1$
				{
					try
					{
						formattedValue = format.format(formattedValue);
					}
					catch (IllegalArgumentException ex)
					{
						Debug.error("Error formatting value for combobox " + dataProviderID + ", " + ex); //$NON-NLS-1$//$NON-NLS-2$
					}
				}
				((JLabel)editorComponent).setText(resolver != null ? Text.processTags(formattedValue.toString(), resolver) : formattedValue.toString());
			}
			return editorComponent;
		}

		@Override
		public Object getCellEditorValue()
		{
			return paintSelection ? Boolean.valueOf(editorComponent.getBackground().equals(enclosedComponent.getSelectionBackground())) : Boolean.TRUE;
		}

		@Override
		public Component getListCellRendererComponent(JList editList, Object value, int index, boolean isSelected, boolean cellHasFocus)
		{
			if (rendererComponent == null) createRenderer();
			if (IValueList.SEPARATOR.equals(value))
			{
				return new VariableSizeJSeparator(SwingConstants.HORIZONTAL, 15);
			}
			if (isRowSelected(index) && paintSelection)
			{
				((JLabel)rendererComponent).setBackground(editList.getSelectionBackground());
				rendererComponent.setForeground(editList.getSelectionForeground());
				rendererComponent.setOpaque(true);
			}
			else
			{
				((JLabel)rendererComponent).setBackground(editList.getBackground());
				rendererComponent.setForeground(editList.getForeground());
				rendererComponent.setOpaque(false);
			}
			rendererComponent.setFont(editList.getFont());
			rendererComponent.setEnabled(editList.isEnabled());
			if (value == null)
			{
				((JLabel)rendererComponent).setText(""); //$NON-NLS-1$
			}
			else
			{
				Object formattedValue = value;
				if (!"".equals(formattedValue) && format != null && formattedValue != null && !(formattedValue instanceof String)) //$NON-NLS-1$
				{
					try
					{
						formattedValue = format.format(formattedValue);
					}
					catch (IllegalArgumentException ex)
					{
						Debug.error("Error formatting value for combobox " + dataProviderID + ", " + ex); //$NON-NLS-1$//$NON-NLS-2$
					}
				}
				((JLabel)rendererComponent).setText(resolver != null ? Text.processTags(formattedValue.toString(), resolver) : formattedValue.toString());
			}
			rendererComponent.setBorder(marginBorder);
			if (cellHasFocus)
			{
				Border border = null;
				if (isSelected)
				{
					border = UIManager.getBorder("List.focusSelectedCellHighlightBorder");
				}
				if (border == null)
				{
					border = UIManager.getBorder("List.focusCellHighlightBorder");
				}
				if (margin != null)
				{
					rendererComponent.setBorder(BorderFactory.createCompoundBorder(marginBorder, border));
				}
				else
				{
					rendererComponent.setBorder(border);
				}
			}
			return rendererComponent;
		}

		public void mouseClicked(MouseEvent e)
		{
		}

		public void mousePressed(MouseEvent e)
		{
			if (SwingUtilities.isLeftMouseButton(e))
			{
				doSelect(UIUtils.isCommandKeyDown(e), e.isShiftDown());
			}
		}

		public void mouseReleased(MouseEvent e)
		{
		}

		public void mouseEntered(MouseEvent e)
		{
		}

		public void mouseExited(MouseEvent e)
		{
		}

		@Override
		public void doSelect(boolean isCommandKeyDown, boolean isShiftDown)
		{
			boolean selected = isRowSelected(enclosedComponent.getEditingRow());
			if (!isCommandKeyDown && !isShiftDown && choiceType == Field.MULTISELECT_LISTBOX)
			{
				list.setMultiValueSelect(false);
			}
			if (selected)
			{
				if (!isCommandKeyDown && choiceType == Field.MULTISELECT_LISTBOX && list.getSelectedRows().size() > 1)
				{
					// clear the selection list
					setElementAt(Boolean.TRUE, enclosedComponent.getEditingRow());
				}
				else
				{
					((JLabel)editorComponent).setBackground(enclosedComponent.getBackground());
				}
			}
			else
			{
				((JLabel)editorComponent).setBackground(enclosedComponent.getSelectionBackground());
			}
			if (isShiftDown && choiceType == Field.MULTISELECT_LISTBOX)
			{
				int clicked = enclosedComponent.getEditingRow();
				int firstSelected = list.getSelectedRow();
				int start = Math.min(clicked, firstSelected);
				int end = Math.max(clicked, firstSelected);
				List<Boolean> values = new ArrayList<Boolean>(list.getSize());
				for (int i = 0; i < list.getSize(); i++)
				{
					values.add((i >= start && i <= end) ? Boolean.TRUE : Boolean.FALSE);
				}
				list.setElements(values.toArray());
			}
			stopCellEditing();
			list.setMultiValueSelect(choiceType == Field.MULTISELECT_LISTBOX);
		}
	}

	private boolean needEntireState;

	public void setNeedEntireState(boolean b)
	{
		needEntireState = b;
	}

	public void setHorizontalAlignment(int a)
	{
		halign = a;
	}

	public void notifyVisible(boolean b, List invokeLaterRunnables)
	{
		//ignore
	}

	@Override
	public void setVerticalScrollBarPolicy(int policy)
	{
		super.setVerticalScrollBarPolicy(policy);
		if (policy == VERTICAL_SCROLLBAR_NEVER)
		{
			enclosedComponent.setVisibleRowCount(0);
			enclosedComponent.setLayoutOrientation(JList.VERTICAL_WRAP);
		}
	}

	protected void setVerticalScrollBarPolicySpecial(int policy)
	{
		super.setVerticalScrollBarPolicy(policy);
	}

	@Override
	public void setHorizontalScrollBarPolicy(int policy)
	{
		super.setHorizontalScrollBarPolicy(policy);
		if (policy == HORIZONTAL_SCROLLBAR_NEVER)
		{
			enclosedComponent.setVisibleRowCount(0);
			enclosedComponent.setLayoutOrientation(JList.HORIZONTAL_WRAP);
		}
	}

	/*
	 * _____________________________________________________________ Methods for IDisplayData
	 */

	public Object getValueObject()
	{
		Object[] rows = list.getSelectedRows().toArray();
		Object[] objs = new Object[rows.length];
		for (int i = 0; i < rows.length; i++)
		{
			objs[i] = list.getRealElementAt(((Integer)rows[i]).intValue());
		}

		return getScriptObject().getChoiceValue(objs, choiceType != Field.MULTISELECT_LISTBOX && choiceType != Field.CHECKS); // Field.RADIOS || choiceType == Field.LIST_BOX || choiceType == Field.SPINNER will use plain value
	}

	private Object previousValue;
	protected ITagResolver resolver;

	public void setTagResolver(ITagResolver resolver)
	{
		this.resolver = resolver;
	}

	public void setValueObject(Object data)
	{
		try
		{
			if (editProvider != null) editProvider.setAdjusting(true);

			if (!Utils.equalObjects(getValueObject(), data))
			{
				setValueValid(true, null);
			}

			if (list.getSelectedRows().size() > 0)
			{
				for (int i = 0; i < list.getSize(); i++)
				{
					list.setElementAt(Boolean.FALSE, i, true);
				}
			}

			if (needEntireState)
			{
				if (resolver != null)
				{
					if (tooltip != null)
					{
						enclosedComponent.setToolTipText(Text.processTags(tooltip, resolver));
					}
				}
				else
				{
					if (tooltip != null)
					{
						enclosedComponent.setToolTipText(null);
					}
				}
			}
			else
			{
				if (tooltip != null)
				{
					enclosedComponent.setToolTipText(tooltip);
				}
			}

			if (list.getSelectedRows().size() > 0 && previousValue != null && previousValue.equals(data))
			{
				return;
			}
			previousValue = data;

			if (data instanceof String)
			{
				String delim = (eventExecutor.getValidationEnabled() ? "\n" : "%\n"); //$NON-NLS-1$//$NON-NLS-2$
				StringTokenizer tk = new StringTokenizer(data.toString(), delim);
				while (tk.hasMoreTokens())
				{
					int row = list.realValueIndexOf(tk.nextToken());
					if (row >= 0) list.setElementAt(Boolean.TRUE, row);
				}
			}
			else
			{
				int row = list.realValueIndexOf(data);
				if (row >= 0) list.setElementAt(Boolean.TRUE, row);
			}
		}
		finally
		{
			if (editProvider != null) editProvider.setAdjusting(false);
		}
		fireOnRender(false);
	}

	public void fireOnRender(boolean force)
	{
		if (scriptable != null)
		{
			if (force) scriptable.getRenderEventExecutor().setRenderStateChanged();
			scriptable.getRenderEventExecutor().fireOnRender(hasFocus());
		}
	}

	public boolean needEditListener()
	{
		return true;
	}

	private EditProvider editProvider = null;

	public void addEditListener(IEditListener l)
	{
		if (editProvider == null)
		{
			editProvider = new EditProvider(this);
			addFocusListener(editProvider);
			enclosedComponent.getModel().addListDataListener(this);

//			addListSelectionListener(editProvider);
			editProvider.addEditListener(l);
		}
	}

	public boolean needEntireState()
	{
		return needEntireState;
	}

	/*
	 * @see IDisplayData#getDataProviderID()
	 */
	public String getDataProviderID()
	{
		return dataProviderID;
	}

	/*
	 * @see IFieldComponent#setDataProviderID(String)
	 */
	public void setDataProviderID(String dataProviderID)
	{
		this.dataProviderID = dataProviderID;
		list.setDataProviderID(dataProviderID);
	}

	/*
	 * @see IFieldComponent#setEditable(boolean)
	 */
	public void setEditable(boolean b)
	{
		editState = b;
		enclosedComponent.setEditable(b);
	}

	public boolean isEditable()
	{
		return !isReadOnly();
	}

	/*
	 * _____________________________________________________________ Methods for IDisplayRelatedData
	 */

	public void setRecord(IRecordInternal state, boolean stopEditing)
	{
		try
		{
			if (editProvider != null) editProvider.setAdjusting(true);
			list.fill(state);
		}
		finally
		{
			if (editProvider != null) editProvider.setAdjusting(false);
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
	private ArrayList<ILabel> labels;

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
		if (enclosedComponent.isEditing())
		{
			if (looseFocus)
			{
				enclosedComponent.getCellEditor().stopCellEditing();
			}
			else
			{
				editProvider.commitData();
			}
		}
		if (!isValueValid)
		{
			application.invokeLater(new Runnable()
			{
				public void run()
				{
					enclosedComponent.requestFocus();
				}
			});
			return false;
		}
		return true;
	}


	@Override
	public void setBackground(Color c)
	{
//		if (enclosedComponent != null) enclosedComponent.setBackground(bg);
		super.setBackground(c);
		getViewport().setBackground(c);

		if (enclosedComponent != null) enclosedComponent.setBackground(c);
		if (rendererComponent != null) rendererComponent.setBackground(c);
		if (editorComponent != null) editorComponent.setBackground(c);
	}


	@Override
	public void setForeground(Color c)
	{
//		if (enclosedComponent != null) enclosedComponent.setForeground(fg);
		super.setForeground(c);

		getViewport().setForeground(c);

		if (enclosedComponent != null) enclosedComponent.setForeground(c);
		if (rendererComponent != null) rendererComponent.setForeground(c);
		if (editorComponent != null) editorComponent.setForeground(c);
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
		if (labels == null) labels = new ArrayList(3);
		labels.add(label);
	}

	public List<ILabel> getLabelsFor()
	{
		return labels;
	}

	@Override
	public void setOpaque(boolean b)
	{
		// Called by the constructor of JScrollPane, so enclosed can be null.
		if (enclosedComponent != null) enclosedComponent.setOpaque(b);
		getViewport().setOpaque(b);
		super.setOpaque(b);
	}


	/*
	 * readonly---------------------------------------------------
	 */
	public boolean isReadOnly()
	{
		return !enclosedComponent.isEditable();
	}

	private boolean editState;

	public void setReadOnly(boolean b)
	{
		if (b && !enclosedComponent.isEditable()) return;
		if (b)
		{
			enclosedComponent.setEditable(!b);
			editState = true;
		}
		else
		{
			enclosedComponent.setEditable(editState);
		}
	}

	public void setComponentEnabled(final boolean b)
	{
		if (accessible || !b)
		{
			super.setEnabled(b);
			enclosedComponent.setEnabled(b);
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
		if (!b) enclosedComponent.setEnabled(b);
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

	public IValueList getValueList()
	{
		return vl;
	}

	public ListDataListener getListener()
	{
		return null;
	}

	public void setValueList(IValueList vl)
	{
		list.register(vl);
		this.vl = vl;
	}

	public void requestFocusToComponent()
	{
//		if (!enclosedComponent.hasFocus()) Don't test on hasFocus (it can have focus,but other component already did requestFocus)
		{
			if (isDisplayable())
			{
				// Must do it in a runnable or else others after a script can get focus first again..
				application.invokeLater(new Runnable()
				{
					public void run()
					{
						enclosedComponent.requestFocus();
					}
				});
			}
			else
			{
				wantFocus = true;
			}
		}
	}

	@Override
	public void requestFocus()
	{
		if (enclosedComponent != null) enclosedComponent.requestFocus();
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
			enclosedComponent.requestFocus();
		}
	}

	public Component[] getFocusChildren()
	{
		return new Component[] { enclosedComponent };
	}

	/**
	 * @see javax.swing.event.ListDataListener#contentsChanged(javax.swing.event.ListDataEvent)
	 */
	public void contentsChanged(ListDataEvent e)
	{
		if (!list.isValueListChanging() && e.getIndex0() == -1 && e.getIndex1() == -1 && editProvider != null && !editProvider.isAdjusting())
		{
			application.getFoundSetManager().getEditRecordList().prepareForSave(false);
		}
		else if (previousValue != null && list.isValueListChanging() && !(editProvider != null && editProvider.isAdjusting()))
		{
			setValueObject(previousValue);
		}
	}

	public void intervalAdded(ListDataEvent e)
	{
		if (previousValue != null && list.isValueListChanging() && !(editProvider != null && editProvider.isAdjusting()))
		{
			setValueObject(previousValue);
		}
	}

	public void intervalRemoved(ListDataEvent e)
	{
		if (previousValue != null && list.isValueListChanging() && !(editProvider != null && editProvider.isAdjusting()))
		{
			setValueObject(previousValue);
		}
	}


	@Override
	public String toString()
	{
		return scriptable.toString();
	}

	public List getDefaultSort()
	{
		return null;
	}

	public String getId()
	{
		return (String)getClientProperty("Id"); //$NON-NLS-1$
	}

	@Override
	public void paint(Graphics g)
	{
		// If we have regular SunGraphics2D object, just forward to superclass.
		if (g instanceof SunGraphics2D)
		{
			super.paint(g);
		}
		else
		{
			// If we are on Mac OS, we paint first to image buffer and then
			// to actual graphics. This is because the Aqua L&F on Mac does
			// not paint properly on graphics object that are not instances
			// of SunGraphics2D.
			if (Utils.isAppleMacOS())
			{
				// Create buffered image and send it to be painted by superclass.
				int width = this.getWidth();
				int height = this.getHeight();
				BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
				Graphics2D gr1 = (Graphics2D)img.getGraphics().create();
				super.paint(gr1);
				gr1.dispose();

				// Paint the image to the graphics that we received.
				Graphics2D g2d = (Graphics2D)g;
				g2d.drawRenderedImage(img, null);
			}
			// If we are not on Mac OS, just forward to superclass.
			else
			{
				super.paint(g);
			}
		}
	}
}
