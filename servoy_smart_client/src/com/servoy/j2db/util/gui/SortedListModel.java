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
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;

import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import com.servoy.j2db.util.SortedList;

/**
 * @author jcompagner
 */
public class SortedListModel implements ListModel
{
	private final SortedList list;
	private final ArrayList listeners;
	private Comparator comparator;

	public SortedListModel(Comparator comparator)
	{
		list = new SortedList(comparator);
		listeners = new ArrayList(3);
	}

	public SortedListModel(Comparator comparator, Collection collection)
	{
		list = new SortedList(comparator, collection);
		listeners = new ArrayList(3);
	}

	public int getSize()
	{
		return list.size();
	}

	public Object getElementAt(int index)
	{
		return list.get(index);
	}

	public void add(Object object)
	{
		list.add(object);
		int index = list.indexOf(object);
		fireIntervalAdded(index);
	}

	public Object remove(int index)
	{
		Object o = list.remove(index);
		fireIntervalRemoved(index, index);
		return o;
	}

	public void remove(Object object)
	{
		int index = list.indexOf(object);
		if (index >= 0)
		{
			list.remove(index);
			fireIntervalRemoved(index, index);
		}
	}

	public void removeAll()
	{
		int size = list.size();
		if (size > 0)
		{
			list.clear();
			fireIntervalRemoved(0, size - 1);
		}
	}

	public void resort()
	{
		if (list.size() > 0)
		{
			ArrayList tmpList = new ArrayList();
			Iterator iterator = list.iterator();
			while (iterator.hasNext())
			{
				tmpList.add(iterator.next());
				iterator.remove();
			}
			list.addAll(tmpList);
			fireContentsChanged(0, tmpList.size() - 1);
		}
	}

	public void addListDataListener(ListDataListener l)
	{
		listeners.add(l);
	}

	public void removeListDataListener(ListDataListener l)
	{
		listeners.remove(l);
	}

	protected void fireIntervalAdded(int index)
	{
		if (listeners.size() > 0)
		{
			ListDataEvent lde = new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, index, index);
			for (int i = 0; i < listeners.size(); i++)
			{
				((ListDataListener)listeners.get(i)).intervalAdded(lde);
			}
		}
	}

	protected void fireIntervalRemoved(int index1, int index2)
	{
		if (listeners.size() > 0)
		{
			ListDataEvent lde = new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, index1, index2);
			for (int i = 0; i < listeners.size(); i++)
			{
				((ListDataListener)listeners.get(i)).intervalRemoved(lde);
			}
		}
	}

	protected void fireContentsChanged(int index1, int index2)
	{
		if (listeners.size() > 0)
		{
			ListDataEvent lde = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, index1, index2);
			for (int i = 0; i < listeners.size(); i++)
			{
				((ListDataListener)listeners.get(i)).contentsChanged(lde);
			}
		}
	}
}