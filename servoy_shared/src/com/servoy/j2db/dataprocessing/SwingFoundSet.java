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
package com.servoy.j2db.dataprocessing;


import java.util.List;

import javax.swing.ListSelectionModel;
import javax.swing.event.ListDataListener;
import javax.swing.event.TableModelListener;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.model.AlwaysRowSelectedSelectionModel;

/**
 * The foundset to be used in swing apps
 *
 * @author jblok
 */
public class SwingFoundSet extends FoundSet implements ISwingFoundSet, Cloneable
{
	protected transient AlwaysRowSelectedSelectionModel selectionModel;
	private transient TableAndListEventDelegate tableAndListEventDelegate;

	SwingFoundSet(IFoundSetManagerInternal app, SQLSheet sheet, QuerySelect pkSelect, List<SortColumn> defaultSortColumns) throws ServoyException
	{
		this(app, null, sheet, pkSelect, defaultSortColumns);
	}

	//must be used by subclasses
	protected SwingFoundSet(IFoundSetManagerInternal app, IRecordInternal a_parent, SQLSheet sheet, QuerySelect pkSelect, List<SortColumn> defaultSortColumns)
		throws ServoyException
	{
		super(app, a_parent, null, sheet, pkSelect, defaultSortColumns);
		createSelectionModel();
	}

	private void createSelectionModel()
	{
		if (selectionModel == null)
		{
			selectionModel = new AlwaysRowSelectedSelectionModel(this);
			addListDataListener(selectionModel);
		}
	}

	/*
	 * _____________________________________________________________ Methods overloaded methods from super class
	 */

	@Override
	protected Object clone() throws CloneNotSupportedException
	{
		SwingFoundSet obj = (SwingFoundSet)super.clone();
		obj.selectionModel = null;
		obj.tableAndListEventDelegate = null;
		return obj;
	}

	@Override
	public IFoundSetInternal copyCurrentRecordFoundSet() throws ServoyException//used for printing current record
	{
		SwingFoundSet sfs = (SwingFoundSet)super.copyCurrentRecordFoundSet();
		if (sfs != null)
		{
			sfs.selectionModel = new AlwaysRowSelectedSelectionModel(this);
			sfs.addListDataListener(sfs.selectionModel);
		}
		return sfs;
	}

	@Override
	protected void fireSelectionAdjusting()
	{
		if (selectionModel == null) createSelectionModel();

		Runnable runner = new Runnable()
		{
			public void run()
			{
				selectionModel.fireAdjusting();
			}
		};

		IApplication app = fsm.getApplication();
		if (app.isEventDispatchThread())
		{
			runner.run();
		}
		else
		{
			app.invokeLater(runner);
		}
	}

	/**
	 * Returns the selectedRow.
	 *
	 * @return int
	 */
	@Override
	public int getSelectedIndex()
	{
		if (selectionModel == null) createSelectionModel();

		return selectionModel.getSelectedRow();
	}

	/**
	 * Sets the selectedRow.
	 *
	 * @param selectedRow The selectedRow to set
	 */
	@Override
	public void setSelectedIndex(int selectedRow)
	{
		if (selectionModel == null) createSelectionModel();
		selectionModel.setSelectedRow(selectedRow);
	}

	@Override
	protected void fireFoundSetEvent(final FoundSetEvent e)
	{
		super.fireFoundSetEvent(e);
		int type = e.getChangeType();
		int firstRow = e.getFirstRow();
		int lastRow = e.getLastRow();
		// always fire also if there are no listeners (because of always-first-selection rule)
		if (type == FoundSetEvent.CHANGE_INSERT || type == FoundSetEvent.CHANGE_DELETE)
		{
			// if for example both a record view and a table view listen for this event and the record view changes the selection before the table view tries to adjust it due to the insert (on the same selectionModel)
			// selection might become wrong (selection = 165 when only 164 records are available); so selectionModel needs to know; similar situations might happen for delete also
			boolean before = selectionModel.setFoundsetIsFiringSizeChangeTableAndListEvent(true);
			try
			{
				getTableAndListEventDelegate().fireTableAndListEvent(fsm.getApplication(), firstRow, lastRow, type);
			}
			finally
			{
				selectionModel.setFoundsetIsFiringSizeChangeTableAndListEvent(before);
			}
		}
		else if (e.getType() == FoundSetEvent.CONTENTS_CHANGED)
		{
			getTableAndListEventDelegate().fireTableAndListEvent(fsm.getApplication(), firstRow, lastRow, type);
		}
	}

	/*
	 * _____________________________________________________________ Methods from IEditListModel
	 */

	public Object getElementAt(int row)
	{
		return getRecord(row);
	}

	public void setElementAt(Object aValue, int rowIndex)
	{
		//not needed
	}

	@Override
	public int getSize()
	{
		return super.getSize();
	}

	public boolean isCellEditable(int row)
	{
		return isRecordEditable(row);
	}

	protected TableAndListEventDelegate getTableAndListEventDelegate()
	{
		if (tableAndListEventDelegate == null) tableAndListEventDelegate = new TableAndListEventDelegate(this);
		return tableAndListEventDelegate;
	}

	public void addListDataListener(ListDataListener l)
	{
		getTableAndListEventDelegate().addListDataListener(l);
	}

	public void removeListDataListener(ListDataListener l)
	{
		if (tableAndListEventDelegate != null)
		{
			tableAndListEventDelegate.removeListDataListener(l);
		}
	}

	/*
	 * _____________________________________________________________ Methods from TableModel
	 */
	public int getRowCount()
	{
		return super.getSize();
	}

	public int getColumnCount()
	{
		//do nothing handled in CellAdapter
		return 0;
	}

	public String getColumnName(int columnIndex)
	{
		//do nothing handled in CellAdapter
		return "unknown"; //$NON-NLS-1$
	}

	public Class getColumnClass(int columnIndex)
	{
		//do nothing handled in CellAdapter
		return Object.class;
	}

	public boolean isCellEditable(int rowIndex, int columnIndex)
	{
		return super.isRecordEditable(rowIndex);
	}

	public Object getValueAt(int rowIndex, int columnIndex)
	{
		//do nothing handled in CellAdapter
		return null;
	}

	public void setValueAt(Object aValue, int rowIndex, int columnIndex)
	{
		//do nothing handled in CellAdapter
	}

	public void addTableModelListener(TableModelListener l)
	{
		getTableAndListEventDelegate().addTableModelListener(l);
	}

	public void removeTableModelListener(TableModelListener l)
	{
		if (tableAndListEventDelegate != null)
		{
			tableAndListEventDelegate.removeTableModelListener(l);
		}
	}

	public void fireTableModelEvent(int firstRow, int lastRow, int column, int type)
	{
		if (tableAndListEventDelegate != null)
		{
			tableAndListEventDelegate.fireTableModelEvent(firstRow, lastRow, column, type);
		}
	}

	/*
	 * _____________________________________________________________ Methods from this class
	 */

	public AlwaysRowSelectedSelectionModel getSelectionModel()
	{
		return selectionModel;
	}

	@Override
	protected void setMultiSelectInternal(boolean isMultiSelect)
	{
		if (selectionModel == null) createSelectionModel();
		if (isMultiSelect != isMultiSelect())
		{
			selectionModel.setSelectionMode(isMultiSelect ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION);
		}
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.FoundSet#isMultiSelect()
	 */
	@Override
	public boolean isMultiSelect()
	{
		if (selectionModel == null) createSelectionModel();
		return selectionModel.getSelectionMode() == ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;
	}

	@Override
	public void setSelectedIndexes(int[] indexes)
	{
		if (selectionModel == null) createSelectionModel();
		selectionModel.setSelectedRows(indexes);
	}

	@Override
	public int[] getSelectedIndexes()
	{
		if (selectionModel == null) createSelectionModel();
		return selectionModel.getSelectedRows();
	}
}
