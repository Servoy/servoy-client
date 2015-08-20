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
package com.servoy.j2db.util.model;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import com.servoy.j2db.IFormController;
import com.servoy.j2db.dataprocessing.EditRecordList;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecord;
import com.servoy.j2db.dataprocessing.ISaveConstants;
import com.servoy.j2db.dataprocessing.ISwingFoundSet;
import com.servoy.j2db.util.Debug;

/**
 * Selection model that will always keep at least one row selected
 *
 * @author gboros
 */
public class AlwaysRowSelectedSelectionModel extends DefaultListSelectionModel implements ListDataListener
{
	private final List<IFormController> formControllers;
	private final ISwingFoundSet foundset;
	private boolean isPrinting = false;

	// the following two flags I think can be removed altogether if we override insertIndexInterval and removeIndexInterval completely, so super.these are only called
	// from foundset ListDataListener - this way no UI JTable/JList will decide, only the foundset itself; not completely sure about this though
	private boolean foundsetIsFiringSizeChange = false;
	private boolean selectionAlreadyAdjustedBySizeChangeListeners = false;
	private int rowBeforeSelectionListeners;

	public AlwaysRowSelectedSelectionModel(ISwingFoundSet foundset)
	{
		this.foundset = foundset;
		formControllers = Collections.synchronizedList(new ArrayList<IFormController>(3));
		setSelectionMode(SINGLE_SELECTION);
	}

	public void addFormController(IFormController formController)
	{
		if (formController != null && !formControllers.contains(formController))
		{
			formControllers.add(formController);
		}
	}

	public void removeFormController(IFormController formController)
	{
		if (formController != null)
		{
			formControllers.remove(formController);
		}
	}

	/**
	 * 	used to allow setting the selection to -1 when size >0  for printing
	 */
	public void hideSelectionForPrinting()
	{
		this.isPrinting = true;
		this.setSelectedRow(-1);
		this.isPrinting = false;
	}

	private boolean testStopUIEditing()
	{
		for (IFormController fco : formControllers.toArray(new IFormController[formControllers.size()]))
		{
			if (fco.isFormVisible())
			{
				EditRecordList editRecordList = fco.getApplication().getFoundSetManager().getEditRecordList();
				IRecord[] editedRecords = editRecordList.getEditedRecords((IFoundSetInternal)fco.getFoundSet());
				if (editedRecords.length > 0)
				{
					int stopEditing = editRecordList.stopEditing(false, Arrays.asList(editedRecords));
					return stopEditing == ISaveConstants.STOPPED || stopEditing == ISaveConstants.AUTO_SAVE_BLOCKED;
				}
			}
		}
		return true;
	}

	@Override
	public void insertIndexInterval(int index, int length, boolean before)
	{
		if (!(foundsetIsFiringSizeChange && selectionAlreadyAdjustedBySizeChangeListeners))
		{
			// in this case the selection was already changed after the actual insert took place (by other foundset listeners) - so calling this method can result in incorrect selection;
			// so ignore, as a new selection was already decided upon for after this insert
			selectionAlreadyAdjustedBySizeChangeListeners = true;
			super.insertIndexInterval(index, length, before);
		}
	}

	@Override
	public void removeIndexInterval(int index0, int index1)
	{
		if (!(foundsetIsFiringSizeChange && selectionAlreadyAdjustedBySizeChangeListeners))
		{
			// in this case the selection was already changed after the actual insert took place (by other foundset listeners) - so calling this method can result in incorrect selection;
			// so ignore, as a new selection was already decided upon for after this remove
			selectionAlreadyAdjustedBySizeChangeListeners = true;
//			if (getSelectionMode() == SINGLE_SELECTION)
			{
				int selectedRow = getSelectedRow();//save the selection
				this.rowBeforeSelectionListeners = Integer.MIN_VALUE;//make sure we start fresh
				super.removeIndexInterval(index0, index1);//this can trigger selectionListeners that may include executing onRecordSelect, which can change the selection
				int rowAfterSelectionListeners = getSelectedRow();//get the selected row after the new row is selected
				if (selectedRow >= index0 && selectedRow <= index1 && foundset.getSize() > 0)
				{
					// selected record was removed, set selection after the removed block or before (if at the end)
					// note: default behavior of DefaultListSelectionModel is to set selected index to -1 when selected was removed
					int selection = Math.min(index0, foundset.getSize());
					// if it is set to the foundset.getSize() - 1 but the foundset had more rows, then just select the first..
					// else it will load in the next pks and the selection will be somewhere in the middle
					if (selection == foundset.getSize() && foundset.hadMoreRows())
					{
						selection = 0;
					}
					//adjust the selection if super.removeIndexInterval() did not set it correctly but only if the listeners (onRecordSelect) didn't already adjust it
					if (selection != rowAfterSelectionListeners &&
						(rowBeforeSelectionListeners != Integer.MIN_VALUE && rowAfterSelectionListeners == rowBeforeSelectionListeners) &&
						selection < foundset.getSize())
					{
						// i have to call the setSelectionInterval else our methods will test if the record is there
						super.setSelectionInterval(selection, selection);
					}
				}
			}
//			else
//			{
//				super.removeIndexInterval(index0, index1);
//			}
		}
	}

	public boolean setSelectedRow(int row)
	{
		if (getSelectionMode() == SINGLE_SELECTION && row == getSelectedRow()) return true;
		if (!canChangeSelection()) return false;

		return setSelectedRow(row, false, true);
	}

	public boolean canChangeSelection()
	{
		int currentSelectedRow = getSelectedRow();
		if (currentSelectedRow != -1 && !testStopUIEditing())
		{
			if (Debug.tracing())
			{
				Debug.trace("could not leave record, validation or save failed"); //$NON-NLS-1$
			}
			return false;
		}

		return true;
	}

	private boolean setSelectedRow(int row, boolean keepOldSelections, boolean valueIsAdjusting)
	{
		if (row >= 0 && !getRecordAndTestSize(row))
		{
			return false;
		}

		boolean oldIsAdjusting = getValueIsAdjusting();
		setValueIsAdjusting(valueIsAdjusting);
		if (row == -1)
		{
			if (foundsetIsFiringSizeChange) selectionAlreadyAdjustedBySizeChangeListeners = true;
			clearSelection();
		}
		else setSelectedRows(new int[] { row }, keepOldSelections);
		setValueIsAdjusting(oldIsAdjusting);
		return true;
	}

	public int getSelectedRow()
	{
		int[] selectedRows = getSelectedRows();
		return selectedRows.length > 0 ? selectedRows[0] : -1;
	}

	public void setSelectedRows(int[] rows)
	{
		setSelectedRows(rows, false);
	}

	private void setSelectedRows(int[] rows, boolean keepOldSelections)
	{
		if (rows != null && rows.length > 0)
		{
			for (int row : rows)
			{
				if (row == -1) continue;
				IRecord record = foundset.getRecord(row);
				if (record == null || record == foundset.getPrototypeState())
				{
					// don't allow this selection at all
					return;
				}
			}
			if (foundsetIsFiringSizeChange) selectionAlreadyAdjustedBySizeChangeListeners = true;
			if (keepOldSelections) addSelectionInterval(rows[0], rows[0]);
			else setSelectionInterval(rows[0], rows[0]);
			for (int i = 1; i < rows.length; i++)
				addSelectionInterval(rows[i], rows[i]);
			fireValueChanged(false);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.swing.DefaultListSelectionModel#fireValueChanged(int, int, boolean)
	 */
	@Override
	protected void fireValueChanged(int firstIndex, int lastIndex, boolean isAdjusting)
	{
		// make sure if the selection is still -1 but the size > 0 (and the SelectionModel wanted to fire because of the index added index changes)
		// that we just set the selected row to the first index. (and that will do the real fire)
		if (getSelectedRow() == -1 && (firstIndex != lastIndex || !isPrinting) && foundset.getSize() > 0)
		{
			//save the selected row for removeIndexInterval
			this.rowBeforeSelectionListeners = firstIndex;
			setSelectedRow(firstIndex);
		}
		else
		{
			super.fireValueChanged(firstIndex, lastIndex, isAdjusting);
		}
	}

	public int[] getSelectedRows()
	{
		ArrayList<Integer> selectedIndexes = new ArrayList<Integer>();

		int minIdx = getMinSelectionIndex();
		int maxIdx = getMaxSelectionIndex();

		for (int i = minIdx; i <= maxIdx; i++)
		{
			if (isSelectedIndex(i)) selectedIndexes.add(new Integer(i));
		}

		int[] iSelectedIndexes = new int[selectedIndexes.size()];
		for (int i = 0; i < selectedIndexes.size(); i++)
			iSelectedIndexes[i] = selectedIndexes.get(i).intValue();

		return iSelectedIndexes;
	}

	public void fireAdjusting()
	{
		fireValueChanged(true);
	}

	public boolean setFoundsetIsFiringSizeChangeTableAndListEvent(boolean b)
	{
		boolean before = foundsetIsFiringSizeChange;
		foundsetIsFiringSizeChange = b;
		selectionAlreadyAdjustedBySizeChangeListeners = false;
		return before;
	}

	public void contentsChanged(ListDataEvent e)
	{
		// nothing useful here
	}

	public void intervalAdded(ListDataEvent e)
	{
		insertIndexInterval(e.getIndex0(), e.getIndex1() - e.getIndex0() + 1, true);
	}

	public void intervalRemoved(ListDataEvent e)
	{
		removeIndexInterval(e.getIndex0(), e.getIndex1());
	}

	@Override
	public void setSelectionMode(int selectionMode)
	{
		int oldSelection = getSelectionMode();
		super.setSelectionMode(selectionMode);
		if (selectionMode == ListSelectionModel.SINGLE_SELECTION && selectionMode != oldSelection)
		{
			setSelectedRow(getSelectedRow(), false, true);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.swing.DefaultListSelectionModel#setSelectionInterval(int, int)
	 */
	@Override
	public void setSelectionInterval(int index0, int index1)
	{
		if (index1 > index0)
		{
			if (!getRecordAndTestSize(index1)) return;
		}
		else if (!getRecordAndTestSize(index0))
		{
			return;
		}
		super.setSelectionInterval(index0, index1);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.swing.DefaultListSelectionModel#setAnchorSelectionIndex(int)
	 */
	@Override
	public void setAnchorSelectionIndex(int anchorIndex)
	{
		if (!getRecordAndTestSize(anchorIndex)) return;
		super.setAnchorSelectionIndex(anchorIndex);
	}

	/**
	 * @param index
	 */
	private boolean getRecordAndTestSize(int index)
	{
		foundset.getRecord(index);
		// don't allow selection beyond the size
		if (foundset.getSize() <= index) return false;
		return true;
	}
}
