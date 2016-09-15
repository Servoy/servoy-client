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
package com.servoy.j2db.gui;


import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.ISmartClientApplication;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.smart.dataui.CellAdapter;
import com.servoy.j2db.util.IDelegate;
import com.servoy.j2db.util.UIUtils;
import com.servoy.j2db.util.model.AlwaysRowSelectedSelectionModel;

/**
 * @author jcompagner
 */
public class FixedJTable extends JTable
{
	//private boolean editable = true;

	private boolean autoScroll;

	protected final IApplication application;

	/**
	 * Constructor for TheRightTable.
	 */
	public FixedJTable(IApplication application)
	{
		super();
		this.application = application;
	}

	/**
	 * Constructor for TheRightTable.
	 * 
	 * @param dm
	 */
	public FixedJTable(IApplication application, TableModel dm)
	{
		super(dm);
		this.application = application;
	}

	/**
	 * Constructor for TheRightTable.
	 * 
	 * @param dm
	 * @param cm
	 */
	public FixedJTable(IApplication application, TableModel dm, TableColumnModel cm)
	{
		super(dm, cm);
		this.application = application;
	}

	/**
	 * Constructor for TheRightTable.
	 * 
	 * @param dm
	 * @param cm
	 * @param sm
	 */
	public FixedJTable(IApplication application, TableModel dm, TableColumnModel cm, ListSelectionModel sm)
	{
		super(dm, cm, sm);
		this.application = application;
	}

	/**
	 * @return Returns the editable.
	 */
	public boolean isEditable()
	{
		return true;
	}

	/**
	 * @see javax.swing.JComponent#processMouseEvent(java.awt.event.MouseEvent)
	 */
	@Override
	protected void processMouseEvent(MouseEvent e)
	{
		if (((e.getID() == MouseEvent.MOUSE_PRESSED && (getSelectionModel().getSelectionMode() == ListSelectionModel.SINGLE_SELECTION)) ||
			((e.getID() == MouseEvent.MOUSE_PRESSED || e.getID() == MouseEvent.MOUSE_RELEASED) &&
				(getSelectionModel().getSelectionMode() == ListSelectionModel.MULTIPLE_INTERVAL_SELECTION) && !UIUtils.isCommandKeyDown(e) &&
				!e.isShiftDown())) &&
			isEnabled())
		{
			Point p = e.getPoint();
			int row = rowAtPoint(p);
			if (row != -1 && getSelectedRow() != row)
			{
				ListSelectionModel rsm = getSelectionModel();
				if (rsm instanceof AlwaysRowSelectedSelectionModel && !((AlwaysRowSelectedSelectionModel)rsm).canChangeSelection()) return;
				if (!isEditing() || getCellEditor().stopCellEditing())
				{
					rsm.setSelectionInterval(row, row);
				}
			}
		}

		MouseEvent me = e;

		if (e.isAltDown())
		{
			me = new MouseEvent(e.getComponent(), e.getID(), e.getWhen(), e.getModifiers() | e.getModifiersEx(), e.getX(), e.getY(), e.getClickCount(),
				e.isPopupTrigger())
			{
				@Override
				public int getModifiers()
				{
					return (super.getModifiers() | super.getModifiersEx());
				}
			};
		}

		super.processMouseEvent(me);
	}

	/**
	 * @see javax.swing.JTable#changeSelection(int, int, boolean, boolean)
	 */
	@Override
	public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend)
	{
		if (cellEditor != null && getEditingRow() != rowIndex && !cellEditor.stopCellEditing())
		{
			// don't change selection if there is a celleditor hanging around.
			return;
		}

		ListSelectionModel rsm = getSelectionModel();
		boolean isSelected = false;
		for (int index : getSelectedRows())
			if (index == rowIndex) isSelected = true;
		if (!isSelected && rsm instanceof AlwaysRowSelectedSelectionModel && !((AlwaysRowSelectedSelectionModel)rsm).canChangeSelection()) return;
		ListSelectionModel csm = getColumnModel().getSelectionModel();

		// get the value so it will be loaded first.
		TableModel model = getModel();
		if (model instanceof FoundSet) ((FoundSet)model).getRecord(rowIndex);

		changeSelectionModel(csm, columnIndex, toggle, extend, false, false);
		changeSelectionModel(rsm, rowIndex, toggle, extend, rsm.getSelectionMode() != ListSelectionModel.SINGLE_SELECTION ? rsm.isSelectedIndex(rowIndex)
			: false, true);

		// Scroll after changing the selection as blit scrolling is immediate,
		// so that if we cause the repaint after the scroll we end up painting
		// everything!
		if (getAutoscrolls())
		{
			Rectangle cellRect = getCellRect(rowIndex, columnIndex, false);
			if (cellRect != null)
			{
				scrollRectToVisible(cellRect);
			}
		}
	}


	private void changeSelectionModel(ListSelectionModel sm, int index, boolean toggle, boolean extend, boolean selected, boolean row)
	{
		if (extend)
		{
			if (toggle)
			{
				sm.setAnchorSelectionIndex(index);
			}
			else
			{
				int anchorIndex = getAdjustedIndex(sm.getAnchorSelectionIndex(), row);
				if (anchorIndex == -1)
				{
					anchorIndex = 0;
				}

				sm.setSelectionInterval(anchorIndex, index);
			}
		}
		else
		{
			if (toggle)
			{
				if (selected && sm.getMinSelectionIndex() != sm.getMaxSelectionIndex())
				{
					sm.removeSelectionInterval(index, index);
				}
				else
				{
					sm.addSelectionInterval(index, index);
				}
			}
			else
			{
				sm.setSelectionInterval(index, index);
			}
		}
	}

	private int getAdjustedIndex(int index, boolean row)
	{
		int compare = row ? getRowCount() : getColumnCount();
		return index < compare ? index : -1;
	}


	/**
	 * @param editable The editable to set.
	 */
	public void setEditable(boolean editable)
	{
		if (!editable && isEditing())
		{
			getCellEditor().stopCellEditing();
		}
	}

	/*
	 * @see javax.swing.JTable#isCellEditable(int, int)
	 */
	@Override
	public boolean isCellEditable(int row, int column)
	{
		Object o = getCellEditor(row, column);
		if (o instanceof CellAdapter)
		{
			Component comp = ((CellAdapter)o).getEditor();
			boolean isReadOnlyEditor = (comp instanceof IDelegate) && (((IDelegate)comp).getDelegate() instanceof JEditorPane) &&
				!((JEditorPane)((IDelegate)comp).getDelegate()).isEditable();
			if ((comp instanceof JButton || comp instanceof JLabel || isReadOnlyEditor) && comp.isEnabled())
			{
				return true;
			}
		}
		return super.isCellEditable(row, column);
	}

	private Object[] editRequest;

	@Override
	public boolean editCellAt(int row, int column, EventObject e)
	{
		// te normal implementation retuns and does nothing at all when reqestion
		// focus on next cell called from within editingStop, we want the request
		// being noted and caried out later
		if (cellEditor != null)
		{
			// if the current one is already editing then don't try to stop it.
			if (getEditingRow() == row && getEditingColumn() == column) return true;

			if (!cellEditor.stopCellEditing() || cellEditor != null)
			{
				// do edit request after normal removeEditor is finished
				editRequest = new Object[] { new Integer(row), new Integer(column), e };
				// doggy return value, if someone depends on the return
				// value, we can for sure not return true (even if
				// processed later)
				return false;
			}
		}
		// have to "call selection is adjusting first"/select row first if row !=
		// selected row, it is a strange impl from sun, setting the selected row
		// later than the editor, we need other way around
		if (!setSelectedRow(row, e))
		{
			// selected index not actually changed, probably due to validation failed
			return false;
		}

		// if we are in multiselect we only edit when we click in single selected row; otherwise we just select (no edit)
		if (e instanceof MouseEvent && ((MouseEvent)e).getID() == MouseEvent.MOUSE_PRESSED &&
			getSelectionModel().getSelectionMode() == ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
		{
			MouseEvent me = (MouseEvent)e;
			if (UIUtils.isCommandKeyDown((MouseEvent)e) || me.isShiftDown())
			{
				return false;
			}
			else
			{
				ListSelectionModel sm = getSelectionModel();
				if (sm instanceof AlwaysRowSelectedSelectionModel)
				{
					AlwaysRowSelectedSelectionModel fsm = (AlwaysRowSelectedSelectionModel)sm;
					boolean selectedRow = fsm.setSelectedRow(row);
					if (!selectedRow) return false;
				}
			}
		}

		return super.editCellAt(row, column, e);
	}


	private boolean setSelectedRow(int row, EventObject event)
	{
		ListSelectionModel sm = getSelectionModel();
		if (sm instanceof AlwaysRowSelectedSelectionModel && sm.getSelectionMode() == ListSelectionModel.SINGLE_SELECTION)
		{
			AlwaysRowSelectedSelectionModel fsm = (AlwaysRowSelectedSelectionModel)sm;
			return fsm.setSelectedRow(row);
		}
		return true;
	}

	@Override
	public void removeEditor()
	{
		super.removeEditor();
		if (editRequest != null)// if there was edit requested during edit stop,
		// execute now
		{
			final int row = ((Integer)editRequest[0]).intValue();
			final int column = ((Integer)editRequest[1]).intValue();
			final EventObject eventObject = (EventObject)editRequest[2];
			editRequest = null;
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					if (!(isEditing() && getEditingRow() == row && getEditingColumn() == column))
					{
						editCellAt(row, column, eventObject);
					}
				}

			});
		}
	}

	/**
	 * @see javax.swing.JTable#columnSelectionChanged(javax.swing.event.ListSelectionEvent)
	 */
	@Override
	public void columnSelectionChanged(ListSelectionEvent e)
	{
		// If table is in single selection model then only the column selection of
		// the current
		// selected row can be changed.. So not repainting from row 0 to rowCount
		// but only selected row.
		if (getSelectionModel().getSelectionMode() == ListSelectionModel.SINGLE_SELECTION)
		{
			int columnCountMin1 = getColumnCount() - 1;
			if (getRowCount() <= 0 || columnCountMin1 < 0)
			{
				return;
			}
			int selectedRow = getSelectedRow();
			int firstIndex = Math.min(columnCountMin1, Math.max(e.getFirstIndex(), 0));
			int lastIndex = Math.min(columnCountMin1, Math.max(e.getLastIndex(), 0));
			Rectangle firstColumnRect = getCellRect(selectedRow, firstIndex, false);
			Rectangle lastColumnRect = getCellRect(selectedRow, lastIndex, false);
			repaint(firstColumnRect.union(lastColumnRect));
		}
		else
		{
			super.columnSelectionChanged(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.JComponent#paintImmediately(int, int, int, int)
	 */
	@Override
	public void paintImmediately(int x, int y, int w, int h)
	{
		if (autoScroll)
		{
			autoScroll = false;
			Rectangle cellRect = getCellRect(getSelectedRow(), getSelectedColumn(), false);
			if (cellRect != null)
			{
				scrollRectToVisible(cellRect);
			}
		}
		super.paintImmediately(x, y, w, h);
	}

	/*
	 * @see javax.swing.JComponent#repaint(long, int, int, int, int)
	 */
	@Override
	public void repaint(long tm, int x, int y, int width, int height)
	{
//		if (autoScroll)
//		{
//			autoScroll = false;
//			Rectangle cellRect = getCellRect(getSelectedRow(), getSelectedColumn(), false);
//			if (cellRect != null)
//			{
//				scrollRectToVisible(cellRect);
//			}
//		}
		super.repaint(tm, x, y, width, height);
	}

	/**
	 * @see javax.swing.JTable#valueChanged(javax.swing.event.ListSelectionEvent)
	 */
	@Override
	public void valueChanged(ListSelectionEvent e)
	{
		if (getSelectionModel().getSelectionMode() == ListSelectionModel.SINGLE_SELECTION)
		{
			if (e.getValueIsAdjusting()) return;
			int rowCount = getRowCount() - 1;
			int columnCount = getColumnCount() - 1;

			if (rowCount < 0 || columnCount < 0)
			{
				return;
			}

			int firstIndex = Math.min(rowCount, Math.max(e.getFirstIndex(), 0));
			int lastIndex = Math.min(rowCount, Math.max(e.getLastIndex(), 0));
			Rectangle firstRowRect1 = getCellRect(firstIndex, 0, false);
			Rectangle firstRowRect2 = getCellRect(firstIndex, columnCount, false);
			int paintImmediately = 0;
			if (application instanceof ISmartClientApplication)
			{
				paintImmediately = ((ISmartClientApplication)application).getPaintTableImmediately();
			}
			boolean valid = paintImmediately <= 0 && isValid();

			if (valid)
			{
				Rectangle currentRect = getVisibleRect();
				if (currentRect.y > firstRowRect1.y)
				{
					valid = false;
				}
				else if ((currentRect.y + currentRect.height) <= (firstRowRect1.y + firstRowRect2.height))
				{
					valid = false;
				}
			}

			if (getAutoscrolls())
			{
				if (true)
				{
					final Rectangle cellRect = getCellRect(getSelectedRow(), getSelectedColumn(), false);
					if (cellRect != null)
					{
						SwingUtilities.invokeLater(new Runnable()
						{
							public void run()
							{
								scrollRectToVisible(cellRect);

							}
						});
					}
				}
				else
				{
					autoScroll = true;
				}
			}

			if (valid)
			{
				super.paintImmediately(firstRowRect1.union(firstRowRect2));
			}
			else
			{
				repaint(firstRowRect1.union(firstRowRect2));
			}

			Rectangle lastRowRect1 = getCellRect(lastIndex, 0, false);
			Rectangle lastRowRect2 = getCellRect(lastIndex, columnCount, false);
			if (valid)
			{
				super.paintImmediately(lastRowRect1.union(lastRowRect2));
			}
			else
			{
				repaint(lastRowRect1.union(lastRowRect2));
			}
		}
		else
		{
			super.valueChanged(e);
		}
	}

	/**
	 * Constructor for TheRightTable.
	 * 
	 * @param numRows
	 * @param numColumns
	 */
	public FixedJTable(IApplication application, int numRows, int numColumns)
	{
		super(numRows, numColumns);
		this.application = application;
	}

	/**
	 * Constructor for TheRightTable.
	 * 
	 * @param rowData
	 * @param columnNames
	 */
	public FixedJTable(IApplication application, Vector rowData, Vector columnNames)
	{
		super(rowData, columnNames);
		this.application = application;
	}

	/**
	 * Constructor for TheRightTable.
	 * 
	 * @param rowData
	 * @param columnNames
	 */
	public FixedJTable(IApplication application, Object[][] rowData, Object[] columnNames)
	{
		super(rowData, columnNames);
		this.application = application;
	}

	/**
	 * @see javax.swing.JComponent#processKeyBinding(KeyStroke, KeyEvent, int, boolean)
	 */
	@Override
	protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed)
	{
		if (e.getKeyCode() == KeyEvent.VK_ENTER && e.getID() == KeyEvent.KEY_PRESSED)
		{
			int row = getSelectionModel().getAnchorSelectionIndex();
			int column = getColumnModel().getSelectionModel().getAnchorSelectionIndex();
			if (getEditingRow() != row && getEditingColumn() != column)
			{
				editCellAt(row, column, e);
				Component comp = getEditorComponent();
				if (comp != null)
				{
					comp.requestFocus();
					if (comp instanceof JComboBox)
					{
						((JComboBox)comp).setPopupVisible(true);
					}
					// If we have a button inside the cell, fire its action, don't waste 
					// the first VK_ENTER for just starting the edit.
					else if (comp instanceof JButton)
					{
						final JButton btn = (JButton)comp;
						SwingUtilities.invokeLater(new Runnable()
						{
							public void run()
							{
								btn.doClick();
							}
						});
					}
				}
				return true;
			}
			return false;
		}
		else if (isEditing() && e.getKeyCode() == KeyEvent.VK_ESCAPE)
		{
			getCellEditor().stopCellEditing();
			return false;
		}
		else if ((e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_UP) && e.getModifiers() == InputEvent.ALT_MASK)
		{
			return false;
		}
		else if (e.getKeyCode() == KeyEvent.VK_F && e.getModifiers() == InputEvent.CTRL_MASK)
		{
			return false;
		}
		else
		{
			return super.processKeyBinding(ks, e, condition, pressed);
		}
	}

	@Override
	public boolean getSurrendersFocusOnKeystroke()
	{
		return true;
	}

	// print grid lines as last step
	@Override
	protected void printComponent(Graphics g)
	{
		boolean bShowHorizontalLines = getShowHorizontalLines();
		boolean bShowVerticalLines = getShowVerticalLines();

		this.setShowHorizontalLines(false);
		this.setShowVerticalLines(false);

		super.printComponent(g);

		this.setShowHorizontalLines(bShowHorizontalLines);
		this.setShowVerticalLines(bShowVerticalLines);

		printGridLines(g, bShowHorizontalLines, bShowVerticalLines);
	}

	private void printGridLines(Graphics g, boolean bShowHorizontalLines, boolean bShowVerticalLines)
	{
		Rectangle clip = g.getClipBounds();

		Rectangle bounds = this.getBounds();
		// account for the fact that the graphics has already been translated
		// into the table's bounds
		bounds.x = bounds.y = 0;

		if (this.getRowCount() <= 0 || this.getColumnCount() <= 0 ||
		// this check prevents us from painting
		// when the clip doesn't intersect our bounds at all
			!bounds.intersects(clip))
		{
			return;
		}

		boolean ltr = this.getComponentOrientation().isLeftToRight();

		Point upperLeft = clip.getLocation();
		if (!ltr)
		{
			upperLeft.x++;
		}

		Point lowerRight = new Point(clip.x + clip.width - (ltr ? 1 : 0), clip.y + clip.height);

		int rMin = this.rowAtPoint(upperLeft);
		int rMax = this.rowAtPoint(lowerRight);
		// This should never happen (as long as our bounds intersect the clip,
		// which is why we bail above if that is the case).
		if (rMin == -1)
		{
			rMin = 0;
		}
		// If the table does not have enough rows to fill the view we'll get -1.
		// (We could also get -1 if our bounds don't intersect the clip,
		// which is why we bail above if that is the case).
		// Replace this with the index of the last row.
		if (rMax == -1)
		{
			rMax = this.getRowCount() - 1;
		}

		int cMin = this.columnAtPoint(ltr ? upperLeft : lowerRight);
		int cMax = this.columnAtPoint(ltr ? lowerRight : upperLeft);
		// This should never happen.
		if (cMin == -1)
		{
			cMin = 0;
		}

		//If the table does not have enough columns to fill the view we'll get -1.
		// Replace this with the index of the last column.
		if (cMax == -1)
		{
			cMax = this.getColumnCount() - 1;
		}

		// Paint the grid.
		g.setColor(this.getGridColor());

		Rectangle minCell = this.getCellRect(rMin, cMin, true);
		Rectangle maxCell = this.getCellRect(rMax, cMax, true);
		Rectangle damagedArea = minCell.union(maxCell);

		if (bShowHorizontalLines)
		{
			int tableWidth = damagedArea.x + damagedArea.width;
			int y = damagedArea.y;

			for (int row = rMin; row <= rMax; row++)
			{
				y += this.getRowHeight(row);
				g.drawLine(damagedArea.x, y - 1, tableWidth - 1, y - 1);
			}
		}

		if (bShowVerticalLines)
		{
			TableColumnModel cm = this.getColumnModel();
			int tableHeight = damagedArea.y + damagedArea.height;
			int x;
			if (this.getComponentOrientation().isLeftToRight())
			{
				x = damagedArea.x;
				for (int column = cMin; column <= cMax; column++)
				{
					int w = cm.getColumn(column).getWidth();
					x += w;
					g.drawLine(x - 1, 0, x - 1, tableHeight - 1);
				}
			}
			else
			{
				x = damagedArea.x;
				for (int column = cMax; column >= cMin; column--)
				{
					int w = cm.getColumn(column).getWidth();
					x += w;
					g.drawLine(x - 1, 0, x - 1, tableHeight - 1);
				}
			}
		}
	}
}
