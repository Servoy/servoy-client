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
import java.util.List;

import com.servoy.j2db.util.IDestroyable;

/**
 * handles refresh when a lh side of relation is changed, the normal displaysAdapter created for related field does nothing on relation refresh
 * 
 * @author jblok
 */
public class RelatedFieldHolder implements IDisplayRelatedData
{
	private List displays; //IDisplayData's
	private final DataAdapterList dal;

	public RelatedFieldHolder(DataAdapterList dal)
	{
		this.dal = dal;
	}

	public String getSelectedRelationName()
	{
		return null;
	}

	public String[] getAllRelationNames()
	{
		return new String[0];
	}

	public boolean stopUIEditing(boolean looseFocus)
	{
		return true;
	}

	public void notifyVisible(boolean b, List invokeLaterRunnables)
	{
		//ignore
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.dataprocessing.IDisplayRelatedData#deregister()
	 */
	public void destroy()
	{
		for (int i = 0; i < displays.size(); i++)
		{
			Object display = displays.get(i);
			if (display instanceof IDestroyable)
			{
				((IDestroyable)display).destroy();
			}
		}
	}

	public void setValidationEnabled(boolean b)
	{
		// causes validation enabeld being set 2 times. (Done already DataAdapter) 
//		for (int i = 0; i < displays.size(); i++)
//		{
//			IDisplayData display = (IDisplayData)displays.get(i);
//			display.setValidationEnabled(b);
//		}
	}

	//key is changed update all the related fields
	public void setRecord(IRecordInternal state, boolean stopEditing)
	{
		if (state != null)
		{
			for (int i = 0; i < displays.size(); i++)
			{
				IDisplayData display = (IDisplayData)displays.get(i);
				String id = display.getDataProviderID();
				display.setTagResolver(dal);
				Object value = null;
				if (id != null)
				{
					value = dal.getValueObject(state, id);
				}
				display.setValueObject(value);//refresh
			}
		}
	}

	public void addDisplay(IDisplayData display)
	{
		if (displays == null) displays = new ArrayList();

		if (!displays.contains(display))
		{
			displays.add(display);
		}
	}

	@Override
	public String toString()
	{
		return "RelatedFieldHolder -1 with " + displays.size() + " displays,  hash " + hashCode(); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public List getDefaultSort()
	{
		return null;
	}

	public boolean isReadOnly()
	{
		return true;
	}

	public boolean isEnabled()
	{
		return false;
	}
}
