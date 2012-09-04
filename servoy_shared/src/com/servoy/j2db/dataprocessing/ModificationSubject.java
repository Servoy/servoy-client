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


/**
 * Utility class that handles {@link IModificationListener} (de)registering and modification event firing.
 * 
 * @author rgansevles
 *
 * @since 6.1
 */
public class ModificationSubject implements IModificationSubject
{
	private final List<IModificationListener> listeners = Collections.synchronizedList(new ArrayList<IModificationListener>());

	public void addModificationListener(IModificationListener listener)
	{
		listeners.add(listener);
	}

	public void removeModificationListener(IModificationListener listener)
	{
		listeners.remove(listener);
	}

	/**
	 * Check if there are any listeners
	 */
	public boolean hasListeners()
	{
		if (listeners.size() > 0)
		{
			// if there are only subjects as listeners, check if they have listeners.
			for (Object element : listeners.toArray())
			{
				if (!(element instanceof IModificationSubject) || ((IModificationSubject)element).hasListeners())
				{
					return true;
				}
			}
		}
		return false;
	}

	public void fireModificationEvent(ModificationEvent event)
	{
		if (listeners.size() > 0)
		{
			for (Object element : listeners.toArray())
			{
				// this is needed because of the nested valueChanged and fireModificationEvent calls for DelegateModificationSubject
				// and the fact that listeners can be destroyed before the end of an iteration (in a deeper nested call), 
				// so valueChanged could be called on a destroyed/removed listener
				if (listeners.contains(element))
				{
					((IModificationListener)element).valueChanged(event);
				}
			}
		}
	}
}
