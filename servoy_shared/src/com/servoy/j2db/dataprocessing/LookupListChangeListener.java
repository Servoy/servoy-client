/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 * Will update the value of a display when receiving any change event.
 * Refactored. Moved out of DataLookupField so that it can be reused for WebDataLookupField as well.
 * @author jcompagner
 * @author acostescu
 */
public class LookupListChangeListener implements ListDataListener
{

	protected final IDisplayData display;

	public LookupListChangeListener(IDisplayData display)
	{
		this.display = display;
	}

	protected void changed()
	{
		Object value = display.getValueObject();
		if (value != null)
		{
			boolean needEntireState = display.needEntireState();
			display.setNeedEntireState(false);
			try
			{
				display.setValueObject(null);
				display.setValueObject(value);
			}
			finally
			{
				display.setNeedEntireState(needEntireState);
			}
		}
	}

	public void intervalAdded(ListDataEvent e)
	{
		changed();
	}

	public void intervalRemoved(ListDataEvent e)
	{
		changed();
	}

	public void contentsChanged(ListDataEvent e)
	{
		changed();
	}

}
