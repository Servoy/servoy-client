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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import com.servoy.j2db.IEventDelegator;
import com.servoy.j2db.util.Debug;

/**
 * Event delegator for TableModelEvent and ListDataEvent.
 * Also handles always-first-selection rule.
 *
 * @author rgansevles
 *
 */
public class TableAndListEventDelegate
{
	private List<TableModelListener> tableModelListeners;
	private List<ListDataListener> listDataListeners;

	private final ISwingFoundSet foundSet;

	public TableAndListEventDelegate(ISwingFoundSet foundSet)
	{
		this.foundSet = foundSet;
	}

	public void addTableModelListener(TableModelListener l)
	{
		if (tableModelListeners == null)
		{
			tableModelListeners = Collections.synchronizedList(new ArrayList<TableModelListener>());
		}
		tableModelListeners.add(l);
	}

	public void removeTableModelListener(TableModelListener l)
	{
		if (tableModelListeners != null)
		{
			tableModelListeners.remove(l);
		}
	}

	public boolean canDispose()
	{
		return tableModelListeners == null || tableModelListeners.size() == 0;
	}

	public void fireTableModelEvent(int firstRow, int lastRow, int column, int type)
	{
		if (tableModelListeners != null && tableModelListeners.size() != 0)
		{
			TableModelEvent e = new TableModelEvent(foundSet, firstRow, lastRow, column, type);
			Object[] array = tableModelListeners.toArray();
			for (Object element : array)
			{
				TableModelListener listener = (TableModelListener)element;
				listener.tableChanged(e);
			}
		}
	}

	public void addListDataListener(ListDataListener l)
	{
		if (listDataListeners == null)
		{
			listDataListeners = Collections.synchronizedList(new ArrayList<ListDataListener>());
		}
		listDataListeners.add(l);
	}

	public void removeListDataListener(ListDataListener l)
	{
		if (listDataListeners != null)
		{
			listDataListeners.remove(l);
		}
	}

	protected void fireContentsChanged(int index0, int index1)
	{
		if (listDataListeners != null && listDataListeners.size() != 0)
		{
			ListDataEvent e = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, index0, index1);
			Object[] array = listDataListeners.toArray();
			for (Object element : array)
			{
				ListDataListener listener = (ListDataListener)element;
				listener.contentsChanged(e);
			}
		}
	}

	protected void fireIntervalAdded(int index0, int index1)
	{
		if (listDataListeners != null && listDataListeners.size() != 0)
		{
			ListDataEvent e = new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, index0, index1);
			Object[] array = listDataListeners.toArray();
			for (Object element : array)
			{
				ListDataListener listener = (ListDataListener)element;
				listener.intervalAdded(e);
			}
		}
	}

	protected void fireIntervalRemoved(int index0, int index1)
	{
		if (listDataListeners != null && listDataListeners.size() != 0)
		{
			ListDataEvent e = new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, index0, index1);
			Object[] array = listDataListeners.toArray();
			for (Object element : array)
			{
				ListDataListener listener = (ListDataListener)element;
				listener.intervalRemoved(e);
			}
		}
	}

	public void fireTableAndListEvent(IEventDelegator eventDelegator, final int firstRow, final int lastRow, final int changeType)
	{
		Runnable runner = new Runnable()
		{
			public void run()
			{
				if (changeType == FoundSetEvent.CHANGE_INSERT)
				{
					fireIntervalAdded(firstRow, lastRow);
					fireTableModelEvent(firstRow, lastRow, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT);
					// always-first-selection rule
					if (foundSet.getSelectedIndex() == -1)
					{
						foundSet.setSelectedIndex(0);
					}
				}
				else if (changeType == FoundSetEvent.CHANGE_DELETE)
				{
					fireIntervalRemoved(firstRow, lastRow);
					fireTableModelEvent(firstRow, lastRow, TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE);
					// always-first-selection rule
					if (foundSet.getSize() == 0)
					{
						foundSet.setSelectedIndex(-1);
					}
				}
				else if (changeType == FoundSetEvent.CHANGE_UPDATE)
				{
					fireContentsChanged(firstRow, lastRow);
					fireTableModelEvent(firstRow, lastRow, TableModelEvent.ALL_COLUMNS, TableModelEvent.UPDATE);
				}
				else if (changeType == FoundSetEvent.FOUNDSET_INVALIDATED)
				{
					fireTableModelEvent(firstRow, lastRow, TableModelEvent.ALL_COLUMNS, TableModelEvent.UPDATE);
				}
			}
		};

		if (eventDelegator.isEventDispatchThread())
		{
			runner.run();
		}
		else
		{
			if (changeType == FoundSetEvent.CHANGE_DELETE || changeType == FoundSetEvent.CHANGE_INSERT)
			{
				Debug.trace("Listener invoked from non event dispatch thread.", new RuntimeException()); //$NON-NLS-1$
			}
			eventDelegator.invokeLater(runner);
		}
	}
}
