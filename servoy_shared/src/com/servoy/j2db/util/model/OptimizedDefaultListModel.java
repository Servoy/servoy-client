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


import javax.swing.DefaultListModel;
import javax.swing.event.ListDataListener;

import com.servoy.j2db.util.Debug;

/**
 * A DefaultListModel capable of firing events bundled. For example if you expect to add more elements to the list model, then you tell this to the list model
 * by calling startBundlingEvents(), add as many elements as you like in a continuous interval, then call stopBundlingEvents() and only one intervalAdded event
 * will be fired to listeners.<br>
 * <br>
 * <b>If startBundlingEvents() was called, and operations of different types/in different continuous intervals are performed, you will get an
 * IllegalStateException, because such operations cannot be bundled, so be careful when using startBundlingEvents() and stopBundlingEvents().</b><br>
 * <br>
 * This could be implemented to generate cached events instead of throwing IllegalStateException for these cases above, but then operations need to be
 * intercepted before the underlying vector is changed.
 *
 * @author acostescu
 */
public class OptimizedDefaultListModel extends DefaultListModel
{

	private final static int ADDED = 0;
	private final static int REMOVED = 1;
	private final static int CHANGED = 2;
	private final static int NONE = -2;

	private int bundledEventType = NONE;
	private int bundledIndex1;
	private int bundledIndex2;

	private boolean bundleEvents = false;

	public void startBundlingEvents()
	{
		if (bundleEvents && bundledEventType != NONE)
		{
			// in case startBundling is called twice, without a stopBundling, something is wrong, but try to handle it anyway (to avoid IllegalStateExceptions that might happen later)
			fireBundledEvent();
			Debug.warn("startBundlingEvents() called twice without stopBundlingEvents(). This is not expected and might hide unwanted behavior...");
		}
		bundleEvents = true;
	}

	public void stopBundlingEvents()
	{
		bundleEvents = false;
		if (bundledEventType != NONE)
		{
			fireBundledEvent();
		}
	}

	private void fireBundledEvent()
	{
		if (bundledEventType == ADDED)
		{
			fireIntervalAdded(this, bundledIndex1, bundledIndex2);
		}
		else if (bundledEventType == REMOVED)
		{
			fireIntervalRemoved(this, bundledIndex1, bundledIndex2);
		}
		else if (bundledEventType == CHANGED)
		{
			fireContentsChanged(this, bundledIndex1, bundledIndex2);
		}
		bundledEventType = NONE;
	}

	// intercept all

	@Override
	protected void fireContentsChanged(Object source, int index1, int index2)
	{
		if (bundleEvents)
		{
			if (bundledEventType == NONE)
			{
				bundledEventType = CHANGED;
				bundledIndex1 = Math.min(index1, index2);
				bundledIndex2 = Math.max(index1, index2);
			}
			else if (bundledEventType == CHANGED)
			{
				int i1 = Math.min(index1, index2);
				int i2 = Math.max(index1, index2);
				if ((bundledIndex2 < i1 - 1) || (bundledIndex1 > i2 + 1))
				{
					throw new IllegalStateException("Cannot bundle 'changed' event; intervals cannot be merged"); //$NON-NLS-1$
				}
				else
				{
					bundledIndex1 = Math.min(bundledIndex1, i1);
					bundledIndex2 = Math.max(bundledIndex2, i2);
				}
			}
			else
			{
				throw new IllegalStateException("Cannot bundle 'changed' event with '" + getEventTypeDescription(bundledEventType) + "' events"); //$NON-NLS-1$//$NON-NLS-2$
			}
		}
		else
		{
			super.fireContentsChanged(source, index1, index2);
		}
	}

	@Override
	protected void fireIntervalAdded(Object source, int index1, int index2)
	{
		if (bundleEvents)
		{
			if (bundledEventType == NONE)
			{
				bundledEventType = ADDED;
				bundledIndex1 = Math.min(index1, index2);
				bundledIndex2 = Math.max(index1, index2);
			}
			else if (bundledEventType == ADDED)
			{
				int i1 = Math.min(index1, index2);
				int i2 = Math.max(index1, index2);
				if ((bundledIndex2 < i1 - 1) || (bundledIndex1 > i1 + 1))
				{
					throw new IllegalStateException("Cannot bundle 'added' event; intervals cannot be merged"); //$NON-NLS-1$
				}
				else
				{
					bundledIndex1 = Math.min(bundledIndex1, i1);
					bundledIndex2 += i2 - i1 + 1;
				}
			}
			else
			{
				throw new IllegalStateException("Cannot bundle 'added' event with '" + getEventTypeDescription(bundledEventType) + "' events"); //$NON-NLS-1$//$NON-NLS-2$
			}
		}
		else
		{
			super.fireIntervalAdded(source, index1, index2);
		}
	}

	@Override
	protected void fireIntervalRemoved(Object source, int index1, int index2)
	{
		if (bundleEvents)
		{
			if (bundledEventType == NONE)
			{
				bundledEventType = REMOVED;
				bundledIndex1 = Math.min(index1, index2);
				bundledIndex2 = Math.max(index1, index2);
			}
			else if (bundledEventType == REMOVED)
			{
				int i1 = Math.min(index1, index2);
				int i2 = Math.max(index1, index2);
				if ((i1 > bundledIndex1) || (i2 < bundledIndex1 - 1))
				{
					throw new IllegalStateException("Cannot bundle 'removed' event; intervals cannot be merged"); //$NON-NLS-1$
				}
				else
				{
					bundledIndex2 += i2 - bundledIndex1 + 1;
					bundledIndex1 = Math.min(bundledIndex1, i1);
				}
			}
			else
			{
				throw new IllegalStateException("Cannot bundle 'removed' event with '" + getEventTypeDescription(bundledEventType) + "' events"); //$NON-NLS-1$//$NON-NLS-2$
			}
		}
		else
		{
			super.fireIntervalRemoved(source, index1, index2);
		}
	}

	private String getEventTypeDescription(int bundledEventType)
	{
		switch (bundledEventType)
		{
			case NONE :
				return "none"; //$NON-NLS-1$
			case ADDED :
				return "added"; //$NON-NLS-1$
			case REMOVED :
				return "removed"; //$NON-NLS-1$
			case CHANGED :
				return "changed"; //$NON-NLS-1$
			default :
				return "unknown"; //$NON-NLS-1$
		}
	}

	public boolean removeListDataListenerIfNeeded(ListDataListener l)
	{
		int oldSize = getListDataListeners().length;
		removeListDataListener(l);
		int newSize = getListDataListeners().length;
		return oldSize != newSize;
	}
}