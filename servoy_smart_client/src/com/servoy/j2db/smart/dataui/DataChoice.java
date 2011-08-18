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
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.AbstractCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JRadioButton;
import javax.swing.JToggleButton;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.text.Document;

import sun.java2d.SunGraphics2D;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IDisplayRelatedData;
import com.servoy.j2db.dataprocessing.IEditListener;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.gui.editlist.JNavigableEditList;
import com.servoy.j2db.gui.editlist.NavigableCellEditor;
import com.servoy.j2db.gui.editlist.NavigableCellRenderer;
import com.servoy.j2db.ui.IDataRenderer;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.IScrollPane;
import com.servoy.j2db.ui.ISupportCachedLocationAndSize;
import com.servoy.j2db.ui.ISupportValueList;
import com.servoy.j2db.ui.RenderEventExecutor;
import com.servoy.j2db.ui.scripting.AbstractRuntimeField;
import com.servoy.j2db.ui.scripting.AbstractRuntimeScrollableValuelistComponent;
import com.servoy.j2db.util.EnableScrollPanel;
import com.servoy.j2db.util.ISupplyFocusChildren;
import com.servoy.j2db.util.ITagResolver;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.editlist.IEditListEditor;
import com.servoy.j2db.util.editlist.JEditList;
import com.servoy.j2db.util.model.ComboModelListModelWrapper;

/**
 * Runtime swing radio/check box choice component
 * @author jblok, jcompagner
 */
public class DataChoice extends EnableScrollPanel implements IDisplayData, IFieldComponent, IScrollPane, IDisplayRelatedData, ListDataListener,
	ISupplyFocusChildren<Component>, ISupportCachedLocationAndSize, ISupportValueList
{
	private String dataProviderID;
	private final ComboModelListModelWrapper list;
	private final JEditList enclosedComponent;
	private JToggleButton rendererComponent;
	private JToggleButton editorComponent;
	private String tooltip;
	private Insets margin;
	private int halign;
	private final EventExecutor eventExecutor;
	private final IApplication application;
	private final boolean isRadioList;
	private MouseAdapter rightclickMouseAdapter = null;
	private IValueList vl;
	private final AbstractRuntimeScrollableValuelistComponent<IFieldComponent, JComponent> scriptable;

	public DataChoice(IApplication app, AbstractRuntimeScrollableValuelistComponent<IFieldComponent, JComponent> scriptable, IValueList vl, boolean isRadioList)
	{
		super();
		setHorizontalAlignment(SwingConstants.LEFT);
		setBorder(null);
		application = app;
		this.vl = vl;
		this.isRadioList = isRadioList;
		list = new ComboModelListModelWrapper(vl, true);
		enclosedComponent = new JNavigableEditList();
		eventExecutor = new EventExecutor(this, enclosedComponent);
		enclosedComponent.addKeyListener(eventExecutor);
		enclosedComponent.setModel(list);

		if (isRadioList)
		{
			enclosedComponent.setCellRenderer(new NavigableCellRenderer(new RadioCell()));
			enclosedComponent.setCellEditor(new NavigableCellEditor(new RadioCell()));
			list.setMultiValueSelect(false);
		}
		else
		{
			enclosedComponent.setCellRenderer(new NavigableCellRenderer(new CheckBoxCell()));
			enclosedComponent.setCellEditor(new NavigableCellEditor(new CheckBoxCell()));
			list.setMultiValueSelect(true);
		}
		this.scriptable = scriptable;
		scriptable.setField(enclosedComponent);
		scriptable.setList(list);

//		enclosedComponent.setPrototypeCellValue(new Integer(0));

		getViewport().setView(enclosedComponent);
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
			if (!Boolean.TRUE.equals(application.getUIProperty(IApplication.LEAVE_FIELDS_READONLY_IN_FIND_MODE)))
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

	public int getDataType()
	{
		return dataType;
	}

	private int dataType;
	private String format;

	public void setFormat(int dataType, String format)
	{
		this.dataType = dataType;
		this.format = format;
	}

	public String getFormat()
	{
		return format;
	}

	public Document getDocument()
	{
		return null;
	}

	public class RadioCell extends AbstractCellEditor implements IEditListEditor, ListCellRenderer, ActionListener
	{
//		private JRadioButton rendererComponent;
//		private JRadioButton editorComponent;
		public RadioCell()
		{
			super();
		}

		private void createRenderer()
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
			rendererComponent.setOpaque(false);
			rendererComponent.setMargin(margin);
			rendererComponent.setHorizontalAlignment(halign);
		}

		private void createEditor()
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
			editorComponent.setOpaque(false);
			editorComponent.addActionListener(this);
			editorComponent.setMargin(margin);
			editorComponent.setHorizontalAlignment(halign);
		}

		public Component getListCellEditorComponent(JEditList editList, Object value, boolean isSelected, int index)
		{
			ComboModelListModelWrapper model = (ComboModelListModelWrapper)editList.getModel();
			if (editorComponent == null) createEditor();
			editorComponent.setFont(editList.getFont());
			editorComponent.setSelected(model.isRowSelected(index));
			editorComponent.setForeground(editList.getForeground());
//			editorComponent.setBackground(editList.getBackground()); 
			if (value == null)
			{
				editorComponent.setText(""); //$NON-NLS-1$
			}
			else
			{
				editorComponent.setText(resolver != null ? Text.processTags(value.toString(), resolver) : value.toString());
			}
			return editorComponent;
		}

		public Object getCellEditorValue()
		{
			return new Boolean(editorComponent.isSelected());
		}

		public Component getListCellRendererComponent(JList editList, Object value, int index, boolean isSelected, boolean cellHasFocus)
		{
			ComboModelListModelWrapper model = (ComboModelListModelWrapper)editList.getModel();
			if (rendererComponent == null) createRenderer();
			rendererComponent.setSelected(model.isRowSelected(index));
			rendererComponent.setFont(editList.getFont());
			rendererComponent.setEnabled(editList.isEnabled());
			rendererComponent.setForeground(editList.getForeground());
//			rendererComponent.setBackground(editList.getBackground());
			if (value == null)
			{
				rendererComponent.setText(""); //$NON-NLS-1$
			}
			else
			{
				rendererComponent.setText(resolver != null ? Text.processTags(value.toString(), resolver) : value.toString());
			}
			return rendererComponent;
		}

		public void actionPerformed(ActionEvent e)
		{
			stopCellEditing();
		}
	}

	public class CheckBoxCell extends AbstractCellEditor implements IEditListEditor, ListCellRenderer, ActionListener
	{
//		private JCheckBox rendererComponent;
//		private JCheckBox editorComponent;
		public CheckBoxCell()
		{
			super();
		}

		private void createRenderer()
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
			rendererComponent.setOpaque(false);
			rendererComponent.setMargin(margin);
			rendererComponent.setHorizontalAlignment(halign);
		}

		private void createEditor()
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
			editorComponent.setOpaque(false);
			editorComponent.addActionListener(this);
			editorComponent.setMargin(margin);
			editorComponent.setHorizontalAlignment(halign);
		}

		public Component getListCellEditorComponent(JEditList editList, Object value, boolean isSelected, int index)
		{
			ComboModelListModelWrapper model = (ComboModelListModelWrapper)editList.getModel();
			if (editorComponent == null) createEditor();
			editorComponent.setFont(editList.getFont());
			editorComponent.setSelected(model.isRowSelected(index));
			editorComponent.setForeground(editList.getForeground());
			if (value == null)
			{
				editorComponent.setText(""); //$NON-NLS-1$
			}
			else
			{
				editorComponent.setText(resolver != null ? Text.processTags(value.toString(), resolver) : value.toString());
			}
			return editorComponent;
		}

		public Object getCellEditorValue()
		{
			return new Boolean(editorComponent.isSelected());
		}

		public Component getListCellRendererComponent(JList editList, Object value, int index, boolean isSelected, boolean cellHasFocus)
		{
			ComboModelListModelWrapper model = (ComboModelListModelWrapper)editList.getModel();
			if (rendererComponent == null) createRenderer();
			rendererComponent.setFont(editList.getFont());
			rendererComponent.setSelected(model.isRowSelected(index));
			rendererComponent.setEnabled(editList.isEnabled());
			rendererComponent.setForeground(editList.getForeground());
			if (value == null)
			{
				rendererComponent.setText(""); //$NON-NLS-1$
			}
			else
			{
				rendererComponent.setText(resolver != null ? Text.processTags(value.toString(), resolver) : value.toString());
			}
			return rendererComponent;
		}

		public void actionPerformed(ActionEvent e)
		{
			stopCellEditing();
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

		return getScriptObject().getChoiceValue(objs, isRadioList);
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

			if (data == null) return;

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
//		super.setForeground(fg);

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

	public void requestFocus(Object[] vargs)
	{
//		if (!enclosedComponent.hasFocus()) Don't test on hasFocus (it can have focus,but other component already did requestFocus)
		{
			if (vargs != null && vargs.length >= 1 && !Utils.getAsBoolean(vargs[0]))
			{
				eventExecutor.skipNextFocusGain();
			}
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
		if (e.getIndex0() == -1 && e.getIndex1() == -1 && editProvider != null && !editProvider.isAdjusting())
		{
			// Check if focus event is already done or not..
			if (eventExecutor.hasEnterCmds() && eventExecutor.mustFireFocusGainedCommand())
			{
				eventExecutor.skipNextFocusGain();
				eventExecutor.fireEnterCommands(false, this, IEventExecutor.MODIFIERS_UNSPECIFIED);
			}
			editProvider.commitData();
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

	@Override
	protected void paintComponent(Graphics g)
	{
		if (eventExecutor != null) eventExecutor.fireOnRender(this, hasFocus());
		super.paintComponent(g);
	}

	/*
	 * @see com.servoy.j2db.ui.ISupportOnRenderCallback#getRenderEventExecutor()
	 */
	public RenderEventExecutor getRenderEventExecutor()
	{
		return eventExecutor;
	}
}
