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
package com.servoy.j2db.property;


import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import com.servoy.j2db.FormController;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.FoundSetManager;
import com.servoy.j2db.dataprocessing.SortColumn;
import com.servoy.j2db.gui.FixedJTable;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnWrapper;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IColumn;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.smart.J2DBClient;
import com.servoy.j2db.util.Debug;


/**
 * Editor to fill the initial sort property
 * 
 * @author jblok
 */
public class SortEditor implements IOptimizedPropertyEditor
{
	private SEditor editor;
	private String selected_value;

	public void addActionListener(ActionListener l)
	{
//		dpe.addActionListener(l);
	}

	public void removeActionListener(ActionListener l)
	{
//		dpe.removeActionListener(l);
	}

//	private void fireActionEvent()
//	{
//		ActionEvent ae = null;
//		Iterator iter = elist.iterator();
//		while (iter.hasNext())
//		{
//			ActionListener listener = (ActionListener) iter.next();
//			// Lazily create the event:
//			if (ae == null) ae = new ActionEvent(this,ActionEvent.ACTION_PERFORMED,"OK");
//			listener.actionPerformed(ae);
//		}
//	}

	public void addPropertyChangeListener(PropertyChangeListener listener)
	{
	}

	public void removePropertyChangeListener(PropertyChangeListener listener)
	{
	}

	public String getJavaInitializationString()
	{
		return null;
	}

	public String[] getTags()
	{
		return null;
	}

	public boolean isPaintable()
	{
		return false;
	}

	public void paintValue(Graphics gfx, Rectangle box)
	{
	}

	public void setAsText(String text)
	{
	}

	public String getAsText()
	{
		return selected_value;
	}

	public Object getValue()
	{
		return selected_value;
	}

	public void setValue(Object value)
	{
		if (value != null)
		{
			selected_value = value.toString();
		}
		else
		{
			selected_value = null;
		}
	}

	public void prepareForVisible(IApplication app, boolean b)
	{
		if (b)
		{
			((SEditor)getCustomEditor()).setValue(app, selected_value);
		}
		else
		{
			selected_value = ((SEditor)getCustomEditor()).getSelectedValue();
		}
	}

	public Component getCustomEditor()
	{
		if (editor == null)
		{
			editor = new SEditor();
		}
		return editor;
	}

	public boolean supportsCustomEditor()
	{
		return true;
	}

	public void init(IApplication app)
	{
		//ignore
	}

	public void init(IApplication app, ITable t, List<SortColumn> sortColumns)
	{
		((SEditor)getCustomEditor()).init(app, t, sortColumns);
	}

	public List<SortColumn> getData()
	{
		return ((SEditor)getCustomEditor()).getData();
	}
}


class SEditor extends JPanel implements ActionListener, ListSelectionListener
{
	private IApplication application;

	private final JTable table;
	private final DataProviderEditor dpe;
	private SortModel model;

	public SEditor()
	{
		JPanel movePane = new JPanel();
		movePane.setLayout(new BoxLayout(movePane, BoxLayout.Y_AXIS));
		movePane.setMaximumSize(new Dimension(100, 200));

		JButton downButton = new JButton("move down"); //$NON-NLS-1$
		Dimension minimumSize = downButton.getPreferredSize();//new Dimension(100,20);
		final JButton rightButton = new JButton(" >> "); //$NON-NLS-1$
		rightButton.addActionListener(this);
		rightButton.setActionCommand("right"); //$NON-NLS-1$
		rightButton.setPreferredSize(minimumSize);
		rightButton.setMinimumSize(minimumSize);
		rightButton.setMaximumSize(minimumSize);
//		rightButton.setAlignmentX(0);
//		rightButton.setAlignmentY(0);
		movePane.add(rightButton);

		movePane.add(Box.createRigidArea(new Dimension(0, J2DBClient.BUTTON_SPACING)));

		JButton leftButton = new JButton(" << "); //$NON-NLS-1$
		leftButton.addActionListener(this);
		leftButton.setActionCommand("left"); //$NON-NLS-1$
		leftButton.setPreferredSize(minimumSize);
		leftButton.setMinimumSize(minimumSize);
		leftButton.setMaximumSize(minimumSize);
//		leftButton.setAlignmentX(0);
//		leftButton.setAlignmentY(0);
		movePane.add(leftButton);

		movePane.add(Box.createRigidArea(new Dimension(0, J2DBClient.BUTTON_SPACING)));

		JButton upButton = new JButton("move up"); //$NON-NLS-1$
		upButton.addActionListener(this);
		upButton.setActionCommand("up"); //$NON-NLS-1$
		upButton.setPreferredSize(minimumSize);
		upButton.setMinimumSize(minimumSize);
		upButton.setMaximumSize(minimumSize);
		movePane.add(upButton);

		movePane.add(Box.createRigidArea(new Dimension(0, J2DBClient.BUTTON_SPACING)));

		downButton.addActionListener(this);
		downButton.setActionCommand("down"); //$NON-NLS-1$
		downButton.setPreferredSize(minimumSize);
		downButton.setMinimumSize(minimumSize);
		downButton.setMaximumSize(minimumSize);
		movePane.add(downButton);

		movePane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

		dpe = new DataProviderEditor(false);
		dpe.addActionListener(this);

		table = new FixedJTable(application);
		table.setRowHeight(20);
		JScrollPane tableScroll = new JScrollPane(table);
		tableScroll.setPreferredSize(new Dimension(320, 200));
//		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		add(dpe);//listScroll);//, BorderLayout.WEST);
		add(movePane);//,BorderLayout.CENTER);
		add(tableScroll);//, BorderLayout.EAST);
	}

	public String getSelectedValue()
	{
		return FoundSetManager.getSortColumnsAsString(getData());
	}

	public List<SortColumn> getData()
	{
		if (model != null)
		{
			if (table.isEditing())
			{
				if (table.getCellEditor() != null) table.getCellEditor().stopCellEditing();
			}
			return model.getData();
		}
		else
		{
			return new ArrayList<SortColumn>();
		}
	}

	void setValue(IApplication app, String notused)
	{
		application = app;
		try
		{
			FormManager fm = (FormManager)application.getFormManager();
			FormController fc = fm.getCurrentMainShowingFormController();
			if (fc != null)
			{
				Form form = fc.getForm();
				ITable t = application.getFoundSetManager().getTable(form.getDataSource());
				if (t != null)
				{
					List<SortColumn> list = application.getFoundSetManager().getSortColumns(t, form.getInitialSort());
					init(app, t, list);
				}
			}
		}
		catch (RepositoryException e)
		{
			Debug.error(e);
		}
	}


	void init(IApplication app, ITable t, List<SortColumn> sortColumns)
	{
		application = app;
		try
		{
			dpe.init(app);
			dpe.setDefinedTable(t);
			dpe.setAllowMultipleSelections(true);
			dpe.setShowRelatedOnly(false);
			dpe.setShowColumnsOnly(true);
			dpe.setHideMediaColumns(true);
			dpe.setShowSortableOnly(true);
			dpe.setRelatedEnabled(true);
			dpe.dontShowNoneOption();
			dpe.setReturnValueAsString(false);
			dpe.showDataEx(null);
			if (sortColumns == null)
			{
				model = new SortModel(new ArrayList<SortColumn>());
			}
			else
			{
				model = new SortModel(sortColumns);
			}

			table.setModel(model);
			table.getColumnModel().getColumn(0).setMinWidth(110);
			table.getColumnModel().getColumn(0).setWidth(120);
			//		table.getColumnModel().getColumn(1).setWidth(60);
			//		table.getColumnModel().getColumn(2).setWidth(60);
			table.getColumnModel().getColumn(1).setCellRenderer(new RadioRenderer());
			table.getColumnModel().getColumn(1).setCellEditor(new RadioRenderer());
			table.setAutoCreateColumnsFromModel(false);
			table.getSelectionModel().addListSelectionListener(this);
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
	}

	public void actionPerformed(ActionEvent event)
	{
		if (table.isEditing())
		{
			TableCellEditor tce = table.getCellEditor();
			tce.stopCellEditing();
		}

		String command = event.getActionCommand();
		if (command.equals("left")) left(); //$NON-NLS-1$
		else if (command.equals("right") || command.equals("OK")) right(); //$NON-NLS-1$ //$NON-NLS-2$
		else if (command.equals("up")) up(); //$NON-NLS-1$
		else if (command.equals("down")) down(); //$NON-NLS-1$
	}

	void flagChanged()
	{
//		stringVal = null;
	}

	private void up()
	{
		flagChanged();
		int[] rows = table.getSelectedRows();
		if (rows.length > 0)
		{
			Arrays.sort(rows);
			if (rows[0] > 0)
			{
				for (int r : rows)
				{
					model.up(r);
				}
				table.clearSelection();
				for (int r : rows)
				{
					table.addRowSelectionInterval(r - 1, r - 1);
				}
			}
		}
	}

	private void down()
	{
		flagChanged();
		int[] rows = table.getSelectedRows();
		if (rows.length > 0)
		{
			Arrays.sort(rows);
			if (rows[rows.length - 1] < model.getRowCount() - 1)
			{
				for (int i = rows.length - 1; i >= 0; i--)
				{
					int r = rows[i];
					model.down(r);
				}
				table.clearSelection();
				for (int r : rows)
				{
					table.addRowSelectionInterval(r + 1, r + 1);
				}
			}
		}
	}

	private void left()
	{
		flagChanged();
		model.deleteRows(table.getSelectedRows());
		table.clearSelection();
	}

	private void right()
	{
		flagChanged();
		Object o = dpe.getValue();
		if (o != null)
		{
			int currentSize = model.getRowCount();
			if (o instanceof Column)
			{
				if (model.addRow(new SortColumn((Column)o))) table.setRowSelectionInterval(currentSize, currentSize);
			}
			else if (o instanceof ColumnWrapper)
			{
				if (model.addRow(new SortColumn((ColumnWrapper)o))) table.setRowSelectionInterval(currentSize, currentSize);
			}
			else
			{
				boolean clear = false;
				IDataProvider[] array = (IDataProvider[])o;
				for (IDataProvider element : array)
				{
					boolean added = false;
					if (element instanceof ColumnWrapper)
					{
						added = model.addRow(new SortColumn((ColumnWrapper)element));
					}
					else if (element instanceof Column)
					{
						added = model.addRow(new SortColumn((Column)element));
					}
					if (added)
					{
						if (!clear)
						{
							table.clearSelection();
							clear = true;
						}
						table.addRowSelectionInterval(currentSize, currentSize);
						currentSize++;
					}
				}
			}
		}
	}

	/**
	 * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
	 */
	public void valueChanged(ListSelectionEvent e)
	{
		flagChanged();//if clicked in table assume editted
	}

}


class RadioRenderer extends DefaultCellEditor implements TableCellRenderer, ActionListener
{
	private final JRadioButton r1;
	private final JRadioButton r2;

//	private JPanel comp;
	public RadioRenderer()
	{
		super(new JCheckBox());

		editorComponent = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
		r1 = new JRadioButton("asc"); //$NON-NLS-1$ 
		r1.setMargin(new Insets(0, 0, 0, 0));
		r1.setOpaque(false);
		r1.setFocusable(false);
		r1.addActionListener(this);
		r2 = new JRadioButton("desc"); //$NON-NLS-1$ 
		r2.setMargin(new Insets(0, 0, 0, 0));
		r2.setOpaque(false);
		r2.setFocusable(false);
		r2.addActionListener(this);
		ButtonGroup group = new ButtonGroup();
		group.add(r1);
		group.add(r2);
		editorComponent.add(r1);
		editorComponent.add(r2);
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
	{
		return getTableCellRendererComponent(table, value, isSelected, true, row, column);
	}

	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
	{
		Integer i = (Integer)value;
		if (i.intValue() == SortColumn.DESCENDING)
		{
			r2.setSelected(true);
		}
		else
		{
			r1.setSelected(true);
		}

//		use color that won't get overridden again by laf
		Color foreground = table.getForeground();
		Color background = table.getBackground();
		if (!hasFocus && isSelected)
		{
			foreground = table.getSelectionForeground();
			background = table.getSelectionBackground();
		}
		editorComponent.setBackground(background);
		r1.setForeground(foreground);
		r2.setForeground(foreground);

		return editorComponent;
	}

	@Override
	public Object getCellEditorValue()
	{
		int val = SortColumn.ASCENDING;
		if (r2.isSelected())
		{
			val = SortColumn.DESCENDING;
		}
		return new Integer(val);
	}

	public void actionPerformed(ActionEvent a)
	{
		fireEditingStopped();
	}
}

class SortModel extends AbstractTableModel
{
	private final List<SortColumn> rows;

	SortModel(List<SortColumn> rows) throws Exception
	{
		this.rows = rows;
	}

	public List<SortColumn> getData()
	{
		return rows;
	}

	public boolean addRow(SortColumn c)
	{
		if (!rows.contains(c))
		{
			rows.add(c);
			fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
			return true;
		}
		return false;
	}

	public void deleteRows(int[] indexes)
	{
		Arrays.sort(indexes);
		for (int i = indexes.length - 1; i >= 0; i--)
		{
			rows.remove(indexes[i]);
			fireTableRowsDeleted(rows.size() - 1, rows.size() - 1);
		}
	}

	public void up(int index)
	{
		if (index > 0)
		{
			SortColumn obj = rows.get(index - 1);
			rows.remove(index - 1);
			rows.add(index, obj);
		}
		fireTableDataChanged();
	}

	public void down(int index)
	{
		if (index >= 0 && index < rows.size() - 1)
		{
			SortColumn obj = rows.get(index);
			rows.remove(index);
			rows.add(index + 1, obj);
		}
		fireTableDataChanged();
	}

	public Object getRow(int row)
	{
		return rows.get(row);
	}


	final String[] columnNames = { "Name", "Sorting" }; //$NON-NLS-1$ //$NON-NLS-2$

	public int getColumnCount()
	{
		return columnNames.length;
	}

	public int getRowCount()
	{
		int rowCount = rows.size();
		return rowCount;
	}

	@Override
	public String getColumnName(int col)
	{
		return columnNames[col];
	}

	public Object getValueAt(int row, int col)
	{
		SortColumn v = rows.get(row);
		switch (col)
		{
			case -1 :
				return v;
//		case 0:
//			return new Boolean(v.getInUse());
			case 0 :
				String title = null;
				IColumn c = v.getColumn();
				if (c instanceof Column)
				{
					title = ((Column)c).getTitle();
				}
				return (title == null ? v.getName() : title);
			case 1 :
				return new Integer(v.getSortOrder());
			default :
				return null;
		}
	}

	/*
	 * JTable uses this method to determine the default renderer/ editor for each cell. If we didn't implement this method, then the last aggregateVariable
	 * would contain text ("true"/"false"), rather than a check box.
	 */
	@Override
	public Class< ? > getColumnClass(int c)
	{
		switch (c)
		{
//			case 0:
//			return Boolean.class;

			case 0 :
				return String.class;

			case 1 :
				return Integer.class;

			default :
				return String.class;
		}
	}

	/*
	 * Don't need to implement this method unless your table's editable.
	 */
	@Override
	public boolean isCellEditable(int row, int col)
	{
		if (col == 0)
		{
			return false;
		}
		else
		{
			return true;
		}
	}

	/*
	 * Don't need to implement this method unless your table's data can change.
	 */
	@Override
	public void setValueAt(Object value, int row, int col)
	{
		SortColumn sc = rows.get(row);
		switch (col)
		{
//		case 0:
//			sc.setInUse(((Boolean)value).booleanValue());
//			break;
			case 0 :
				//ignore
				break;
			case 1 :
				sc.setSortOrder(((Integer)value).intValue());
				break;
			default :
		}
	}
}
