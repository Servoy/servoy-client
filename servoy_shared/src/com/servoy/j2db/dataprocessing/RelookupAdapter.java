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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

/**
 * This class handles the updates based on change being done on LH side of a relation used for lookups, and will inform the DAL about refreshes
 * 
 * @author jblok
 */
public class RelookupAdapter implements IDataAdapter
{
	//representing dataprovider
	private final Set<String> dataProviderIDs;

	private final String dataProviderIDToLookup;
	private final List<String> dataProviderIDsToSet;
	private final DataAdapterList list;//parent
	private IRecordInternal state;//state to lookup in.

	public RelookupAdapter(DataAdapterList list, String dataProviderIDToSet, String dataProviderIDToLookup, Set<String> dataProviderIDs)
	{
		this.list = list;
		this.dataProviderIDsToSet = new ArrayList<String>(1);
		this.dataProviderIDsToSet.add(dataProviderIDToSet);
		this.dataProviderIDToLookup = dataProviderIDToLookup;
		this.dataProviderIDs = dataProviderIDs;
		if (Debug.tracing())
		{
			Debug.trace("Created lookup adapter on " + dataProviderIDToSet + " from " + dataProviderIDToLookup); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/*
	 * @see IDataAdapter#setState(State)
	 */
	public void setRecord(IRecordInternal s)
	{
		if (s != state)
		{
			this.state = s;
			if (s == null || s instanceof PrototypeState)
			{
				oldValues.clear();
			}
			else
			{
				Iterator<String> it = dataProviderIDs.iterator();
				while (it.hasNext())
				{
					String dataprovider = it.next();
					oldValues.put(dataprovider, s.getValue(dataprovider));
				}
			}
		}
	}

	/*
	 * @see IDataAdapter#getDataProviderID()
	 */
	public String getDataProviderID()
	{
		return null;
	}

	/*
	 * _____________________________________________________________ DataListener
	 */
	private final ArrayList<IDataAdapter> listeners = new ArrayList<IDataAdapter>();

	public void addDataListener(IDataAdapter l)
	{
		if (!listeners.contains(l) && l != this) listeners.add(l);
	}

	public void removeDataListener(IDataAdapter listener)
	{
		listeners.remove(listener);
	}

	public void displayValueChanged(ModificationEvent event)
	{
		valueChangedImpl(event);
	}

	public void valueChanged(ModificationEvent e)
	{
		valueChangedImpl(e);
	}

	/*
	 * _____________________________________________________________ JavaScriptModificationLisetner
	 */
	private final HashMap<String, Object> oldValues = new HashMap<String, Object>(5);

	private void valueChangedImpl(ModificationEvent e)
	{
		if (!findMode && (e.getRecord() == null || e.getRecord() == state) && dataProviderIDs.contains(e.getName()) && !(state instanceof PrototypeState))
		{
			Object oldValue = oldValues.get(e.getName());
			if (!Utils.equalObjects(oldValue, e.getValue()))
			{
				if (Debug.tracing())
				{
					Debug.trace("Executing lookup on " + dataProviderIDsToSet + " from " + dataProviderIDToLookup + " with value " + e.getValue()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
				oldValues.put(e.getName(), e.getValue());

				Object obj = state.getValue(dataProviderIDToLookup);
				// TODO CHECK this is disabled because if there are more then 1 relookup adapter then for every change an update will be generated
				// and so on and so on. Relookup just starts it now And blow this call there should be a stopEditing (see Record.fireJSModxxxx)
				// But currently if this is a global change then the record stays a bit longer in edit mode (until a stop global editing happens)
				if (state.startEditing()) //make sure this change is noted!
				{
					for (int i = 0; i < dataProviderIDsToSet.size(); i++)
					{
						String dataprovider = dataProviderIDsToSet.get(i);
						Object oldVal = state.setValue(dataprovider, obj);
						if (oldVal != obj) list.valueChanged(new ModificationEvent(dataprovider, obj, state));
					}
				}
			}
		}
	}

	public IDisplayRelatedData getDisplay()
	{
		return null;
	}

	private boolean findMode;

	public void setFindMode(boolean b)
	{
		findMode = b;
	}

	@Override
	public String toString()
	{
		return "RelookupAdapter lookup on " + dataProviderIDsToSet + " from " + dataProviderIDToLookup + ",  hash " + hashCode(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
	}

	public void addDataProviderId(String dataProviderID)
	{
		dataProviderIDsToSet.add(dataProviderID);
	}
}
