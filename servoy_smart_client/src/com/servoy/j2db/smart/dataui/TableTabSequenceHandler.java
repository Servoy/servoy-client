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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.table.TableColumn;

import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ISupplyFocusChildren;

public class TableTabSequenceHandler
{

	private Action defaultTabAction = null;
	private Action defaultShiftTabAction = null;
	private Action newTabAction = null;
	private Action newShiftTabAction = null;
	private List<TableColumn> columnTabSequence;
	private final JTable table;

	public TableTabSequenceHandler(JTable table, ArrayList<TableColumn> columnTabSequence)
	{
		this.table = table;
		if (columnTabSequence.size() > 0)
		{
			this.columnTabSequence = columnTabSequence;
			overrideDefaultTabBehavior();
		}
		else
		{
			this.columnTabSequence = null;
		}
	}

	public void setNewTabSequence(List<Component> list)
	{
		boolean isDefaultBehavior = (list == null || list.size() == 0);
		ArrayList<TableColumn> newTabSequence = null;

		if (!isDefaultBehavior)
		{
			newTabSequence = new ArrayList<TableColumn>();
			// generate column order from component list
			for (Component comp : list)
			{
				Enumeration<TableColumn> allColumns = table.getColumnModel().getColumns();
				while (allColumns.hasMoreElements())
				{
					CellAdapter column = (CellAdapter)allColumns.nextElement();
					if (componentIdentifiesColumn(comp, column) && !newTabSequence.contains(column))
					{
						newTabSequence.add(column);
						break;
					}
				}
			}
			if (newTabSequence.size() == 0) isDefaultBehavior = true;
		}

		if (isDefaultBehavior)
		{
			if (columnTabSequence != null) restoreDefaultTabBehavior();
		}
		else
		{
			if (columnTabSequence == null)
			{
				columnTabSequence = newTabSequence;
				overrideDefaultTabBehavior();
			}
			else
			{
				columnTabSequence = newTabSequence;
			}
		}
	}

	public List<String> getTabSequence()
	{
		List<String> tabSequence = new ArrayList<String>();
		if (columnTabSequence != null)
		{
			for (TableColumn tc : columnTabSequence)
			{
				String name = ((CellAdapter)tc).getName();
				if (name != null) tabSequence.add(name);
			}
		}
		return tabSequence;
	}

	private boolean componentIdentifiesColumn(Component comp, CellAdapter column)
	{
		if (comp == column.getEditor())
		{
			return true;
		}
		else if (column.getEditor() instanceof ISupplyFocusChildren)
		{
			for (Object child : ((ISupplyFocusChildren)column.getEditor()).getFocusChildren())
			{
				if (child == comp) return true;
			}
		}
		return false;
	}

	private void overrideDefaultTabBehavior()
	{
		// take TAB order into consideration (otherwise TAB/SHIFT+TAB will trigger behaviour in BasicTableUI to take cells sequentially - this behaviour is overridden)

		InputMap im = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		ActionMap am = table.getActionMap();
		KeyStroke shiftTab = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, java.awt.event.InputEvent.SHIFT_DOWN_MASK);
		KeyStroke tab = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0);
		if (newTabAction == null)
		{
			newTabAction = new AbstractAction()
			{
				public void actionPerformed(ActionEvent e)
				{
					selectNextCell(e);
				}
			};
			newShiftTabAction = new AbstractAction()
			{
				public void actionPerformed(ActionEvent e)
				{
					selectPreviousCell(e);
				}
			};

			defaultTabAction = am.get(im.get(tab));
			defaultShiftTabAction = am.get(im.get(shiftTab));
		}

		am.put(im.get(tab), newTabAction);
		am.put(im.get(shiftTab), newShiftTabAction);
	}

	private void restoreDefaultTabBehavior()
	{
		if (newTabAction != null)
		{
			InputMap im = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
			ActionMap am = table.getActionMap();
			KeyStroke shiftTab = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, java.awt.event.InputEvent.SHIFT_DOWN_MASK);
			KeyStroke tab = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0);

			am.put(im.get(tab), defaultTabAction);
			am.put(im.get(shiftTab), defaultShiftTabAction);
		}
		columnTabSequence = null;
	}

	protected void selectPreviousCell(ActionEvent e)
	{
		// SHIFT+TAB
		JTable t = (JTable)e.getSource();
		if (t.getColumnCount() == 0 || t.getRowCount() == 0) return;
		if (t.isEditing() && !t.getCellEditor().stopCellEditing()) return;
		int column = t.getColumnModel().getSelectionModel().getLeadSelectionIndex();
		int row = t.getSelectionModel().getLeadSelectionIndex();

		if (column > t.getColumnCount() || column < 0 || row > t.getRowCount() || row < 0)
		{
			column = 0;
			row = 0;
		}
		else
		{
			TableColumn currentColumn = t.getColumnModel().getColumn(column);
			int tabIndex = columnTabSequence.indexOf(currentColumn);
			if (tabIndex != -1)
			{
				// select previous cell according to specified tab sequence
				tabIndex--;
				if (tabIndex < 0)
				{
					tabIndex = columnTabSequence.size() - 1;
					row--;
					if (row < 0) row = t.getRowCount() - 1;
				}
				int newColumnIndex = getColumnIndex(columnTabSequence.get(tabIndex), t);
				if (newColumnIndex == -1) tabIndex = -1; // just use normal sequencing, cause we can't find the new column's index
				else column = newColumnIndex;
			}

			if (tabIndex == -1)
			{
				// focus is now on a column that is not part of specified column tab sequence; just select previous cell normally
				defaultShiftTabAction.actionPerformed(e);
				return;
			}
		}
		t.changeSelection(row, column, false, false);
	}

	protected void selectNextCell(ActionEvent e)
	{
		// TAB
		JTable t = (JTable)e.getSource();
		if (t.getColumnCount() == 0 || t.getRowCount() == 0) return;
		if (t.isEditing() && !t.getCellEditor().stopCellEditing()) return;
		int column = t.getColumnModel().getSelectionModel().getLeadSelectionIndex();
		int row = t.getSelectionModel().getLeadSelectionIndex();

		if (column > t.getColumnCount() || column < 0 || row > t.getRowCount() || row < 0)
		{
			column = 0;
			row = 0;
		}
		else
		{
			TableColumn currentColumn = t.getColumnModel().getColumn(column);
			int tabIndex = columnTabSequence.indexOf(currentColumn);
			if (tabIndex != -1)
			{
				// select previous cell according to specified tab sequence
				tabIndex++;
				if (tabIndex >= columnTabSequence.size())
				{
					tabIndex = 0;
					row++;
					if (row >= t.getRowCount()) row = 0;
				}
				int newColumnIndex = getColumnIndex(columnTabSequence.get(tabIndex), t);
				if (newColumnIndex == -1) tabIndex = -1; // just use normal sequencing, cause we can't find the new column's index
				else column = newColumnIndex;
			}

			if (tabIndex == -1)
			{
				// focus is now on a column that is not part of specified column tab sequence; just select next cell normally
				defaultTabAction.actionPerformed(e);
				return;
			}
		}
		t.changeSelection(row, column, false, false);
	}

	private int getColumnIndex(TableColumn tableColumn, JTable t)
	{
		for (int i = t.getColumnModel().getColumnCount() - 1; i >= 0; i--)
		{
			if (tableColumn == t.getColumnModel().getColumn(i))
			{
				return i;
			}
		}
		Debug.error("Cannot find next/prev column in tab sequence... something is wrong");
		return -1;
	}

}