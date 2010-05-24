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
import java.util.Iterator;

import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.util.Utils;


/**
 * An (not visible)adapter needed for calculations working with input form other calcs but are not showing on form (ie have no display)
 * 
 * @author jblok
 */
public class DataAdapter implements IDataAdapter
{
	//the state (=model) where to get/set the data
	private IRecordInternal state;

	//representing dataprovider
	private final String dataProviderID;
	private Object oldVal;

	public DataAdapter(String dataProviderID)
	{
		this.dataProviderID = dataProviderID;
	}

	/*
	 * @see IDataAdapter#setState(State)
	 */
	public void setRecord(final IRecordInternal state)
	{
		this.state = state;
		if (state != null && !(state instanceof PrototypeState || state instanceof FindState))
		{
			// TODO: test here if the dataprovider == relation or a calculation before adding it to the thread pool?
			if (dataProviderID.startsWith(ScriptVariable.GLOBAL_DOT_PREFIX))
			{
				oldVal = state.getValue(dataProviderID);
			}
			else
			{
				boolean thread = dataProviderID.indexOf('.') != -1;
				if (!thread)
				{
					thread = state.getRawData().containsCalculation(dataProviderID) && state.getRawData().mustRecalculate(dataProviderID, true);
				}
				if (thread)
				{
					oldVal = null;
					state.getParentFoundSet().getFoundSetManager().getApplication().getScheduledExecutor().execute(new Runnable()
					{
						public void run()
						{
							if (state == DataAdapter.this.state)
							{
								oldVal = state.getValue(dataProviderID);
							}
						}
					});
				}
				else
				{
					oldVal = state.getValue(dataProviderID);
				}
			}
		}
		else
		{
			oldVal = null;
		}
	}

	/*
	 * @see IDataAdapter#getDataProviderID()
	 */
	public String getDataProviderID()
	{
		return dataProviderID;
	}

	/*
	 * _____________________________________________________________ DataListener
	 */
	private final ArrayList listners = new ArrayList();

	public void addDataListener(IDataAdapter l)
	{
		if (!listners.contains(l) && l != this) listners.add(l);
	}

	public void removeDataListener(IDataAdapter listner)
	{
		listners.remove(listner);
	}

	private void fireModificationEvent(String name, Object value, IRecord record)
	{
		ModificationEvent e = null;
		Iterator it = listners.iterator();
		while (it.hasNext())
		{
			if (e == null) e = new ModificationEvent(name, value, record);
			IDataAdapter listner = (IDataAdapter)it.next();
			listner.displayValueChanged(e);
		}
	}

	public void displayValueChanged(ModificationEvent event)
	{
		valueChanged(event);
	}

	/*
	 * _____________________________________________________________ JavaScriptModificationListner
	 */
	public void valueChanged(ModificationEvent e)
	{
//		if (e.getName().equals(dataProviderID))
		if (!findMode)
		{
			if (state != null)
			{
				//TODO For Johan: e.getRecord should be the same as state??!

				//just make sure the data is refreshed
				Object obj = state.getValue(dataProviderID);
				if (!Utils.equalObjects(oldVal, obj))
				{
					oldVal = obj;
					fireModificationEvent(dataProviderID, obj, e.getRecord());
				}
			}
		}
	}

	private boolean findMode;

	public void setFindMode(boolean b)
	{
		findMode = b;
	}

	@Override
	public String toString()
	{
		return "DataAdapter " + dataProviderID + ",  hash " + hashCode(); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
