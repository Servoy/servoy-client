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
package com.servoy.j2db.util.gui;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.DefaultListSelectionModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import com.servoy.j2db.FormController;
import com.servoy.j2db.dataprocessing.IRecord;
import com.servoy.j2db.dataprocessing.ISwingFoundSet;
import com.servoy.j2db.util.Debug;

/**
 * @author gboros
 */
public class AlwaysFirstRowSelectedSelectionModel extends DefaultListSelectionModel implements ListDataListener
{
	private final List<FormController> formControllers;
	private final ISwingFoundSet foundset;

	// the following two flags I think can be removed altogether if we override insertIndexInterval and removeIndexInterval completely, so super.these are only called
	// from foundset ListDataListener - this way no UI JTable/JList will decide, only the foundset itself; not completely sure about this though
	private boolean foundsetIsFiringSizeChange = false;
	private boolean selectionAlreadyAdjustedBySizeChangeListeners = false;

	public AlwaysFirstRowSelectedSelectionModel(ISwingFoundSet foundset)
	{
		this.foundset = foundset;
		formControllers = Collections.synchronizedList(new ArrayList<FormController>(3));
		setSelectionMode(SINGLE_SELECTION);
	}

	public void addFormController(FormController formController)
	{
		if (formController != null && !formControllers.contains(formController))
		{
			formControllers.add(formController);
		}
	}

	public void removeFormController(FormController formController)
	{
		if (formController != null)
		{
			formControllers.remove(formController);
		}
	}

	private boolean testStopUIEditing()
	{
		for (Object fco : formControllers.toArray())
		{
			FormController fc = (FormController)fco;
			if (fc.isFormVisible() && !fc.stopUIEditing(false))
			{
				return false;
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
			if (getSelectionMode() == SINGLE_SELECTION)
			{
				int selectedRow = getSelectedRow();
				super.removeIndexInterval(index0, index1);
				if (selectedRow >= index0 && selectedRow <= index1)
				{
					// selected record was removed, set selection after the removed block or before (if at the end)
					// note: default behaviour of DefaultListSelectionModel is to set selected index to -1 when selected was removed
					setSelectedRow(Math.min(index0, foundset.getSize() - 1));
				}
			}
			else
			{
				super.removeIndexInterval(index0, index1);
			}
		}
	}

	public boolean setSelectedRow(int row)
	{
		if (row != getSelectedRow())
		{
			if (!canChangeSelection()) return false;

			return setSelectedRow(row, false, true);
		}
		return true;
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
		if (row >= 0)
		{
			IRecord record = foundset.getRecord(row);
			if (record == null || record == foundset.getPrototypeState()) return false;
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

}
