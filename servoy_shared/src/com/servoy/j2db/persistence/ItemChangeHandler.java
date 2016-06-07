/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

package com.servoy.j2db.persistence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.servoy.j2db.util.Debug;

/**
 * @author gboros
 *
 */
public class ItemChangeHandler<E, I>
{
	private enum OPERATION
	{
		CREATED, REMOVED, CHANGED
	}

	protected final WeakHashMap<E, List<IItemChangeListener<I>>> listeners = new WeakHashMap<>();

	public void add(E element, IItemChangeListener<I> listener)
	{
		if (element != null && listener != null)
		{
			List<IItemChangeListener<I>> itemListeners = listeners.get(element);
			if (itemListeners == null)
			{
				itemListeners = new CopyOnWriteArrayList<IItemChangeListener<I>>();
				listeners.put(element, itemListeners);
			}
			if (!itemListeners.contains(listener))
			{
				itemListeners.add(listener);
			}
		}
	}

	public void remove(E element, IItemChangeListener<I> listener)
	{
		if (element != null && listener != null)
		{
			List<IItemChangeListener<I>> itemListeners = listeners.get(element);
			if (itemListeners != null)
			{
				itemListeners.remove(listener);
				if (itemListeners.size() == 0)
				{
					listeners.remove(element);
				}
			}
		}
	}

	public void fireItemCreated(E element, I item)
	{
		fireItemOperation(element, OPERATION.CREATED, item, null);
	}

	public void fireItemRemoved(E element, I item)
	{
		fireItemOperation(element, OPERATION.REMOVED, item, null);
	}

	public void fireItemChanged(E element, I item)
	{
		Collection<I> items = new ArrayList<I>(1);
		items.add(item);
		fireItemOperation(element, OPERATION.CHANGED, null, items);
	}

	public void fireItemChanged(E element, Collection<I> items)
	{
		fireItemOperation(element, OPERATION.CHANGED, null, items);
	}


	private void fireItemOperation(E element, OPERATION operation, I item, Collection<I> items)
	{
		if (element != null)
		{
			List<IItemChangeListener<I>> itemListeners = listeners.get(element);
			if (itemListeners != null)
			{
				for (IItemChangeListener<I> listener : itemListeners)
				{
					try
					{
						switch (operation)
						{
							case CREATED :
								listener.itemCreated(item);
								break;
							case REMOVED :
								listener.itemRemoved(item);
								break;
							case CHANGED :
								listener.itemChanged(items);
								break;
						}
					}
					catch (Exception ex)
					{
						Debug.error(ex);//an exception should never interupt the process
					}
				}
			}
		}
	}
}
