/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.j2db.server.ngclient;

import java.util.Iterator;

import javax.swing.event.ListDataListener;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.dataprocessing.LookupListModel;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyException;

/**
 * Value list based on table column. To be used when the valuelist property is -none-.
 * @author emera
 */
public class ColumnBasedValueList implements IValueList
{

	private final LookupListModel dlm;
	private final String dataProviderID;
	private boolean fillFirstTime;

	public ColumnBasedValueList(IApplication application, String serverName, String tableName, String dataProviderID)
	{
		dlm = new LookupListModel(application, serverName, tableName, dataProviderID);
		this.dataProviderID = dataProviderID;
		fillFirstTime = true;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see javax.swing.ListModel#getSize()
	 */
	@Override
	public int getSize()
	{
		return dlm.getSize();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.swing.ListModel#getElementAt(int)
	 */
	@Override
	public Object getElementAt(int index)
	{
		return dlm.getElementAt(index);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.swing.ListModel#addListDataListener(javax.swing.event.ListDataListener)
	 */
	@Override
	public void addListDataListener(ListDataListener l)
	{
		dlm.addListDataListener(l);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.swing.ListModel#removeListDataListener(javax.swing.event.ListDataListener)
	 */
	@Override
	public void removeListDataListener(ListDataListener l)
	{
		dlm.removeListDataListener(l);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.dataprocessing.IValueList#getRealElementAt(int)
	 */
	@Override
	public Object getRealElementAt(int row)
	{
		return dlm.getElementAt(row);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.dataprocessing.IValueList#getRelationName()
	 */
	@Override
	public String getRelationName()
	{
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.dataprocessing.IValueList#fill(com.servoy.j2db.dataprocessing.IRecordInternal)
	 */
	@Override
	public void fill(IRecordInternal parentState)
	{
		try
		{
			String filter = parentState.getValue(dataProviderID) != null ? parentState.getValue(dataProviderID).toString() : null;
			dlm.fill(parentState, dataProviderID, filter, fillFirstTime);
			fillFirstTime = false;
		}
		catch (ServoyException e)
		{
			Debug.log(e);
		}
	}

	public LookupListModel getListModel()
	{
		return dlm;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.dataprocessing.IValueList#realValueIndexOf(java.lang.Object)
	 */
	@Override
	public int realValueIndexOf(Object obj)
	{
		return indexOf(obj);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.dataprocessing.IValueList#indexOf(java.lang.Object)
	 */
	@Override
	public int indexOf(Object elem)
	{
		int idx = 0;
		Iterator<Object> it = dlm.iterator();
		while (it.hasNext())
		{
			if (it.next().equals(elem)) return idx;
			idx++;
		}
		return -1;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.dataprocessing.IValueList#deregister()
	 */
	@Override
	public void deregister()
	{
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.dataprocessing.IValueList#getAllowEmptySelection()
	 */
	@Override
	public boolean getAllowEmptySelection()
	{
		return true;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.dataprocessing.IValueList#getName()
	 */
	@Override
	public String getName()
	{
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.dataprocessing.IValueList#hasRealValues()
	 */
	@Override
	public boolean hasRealValues()
	{
		return true;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.dataprocessing.IValueList#setFallbackValueList(com.servoy.j2db.dataprocessing.IValueList)
	 */
	@Override
	public void setFallbackValueList(IValueList list)
	{
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.dataprocessing.IValueList#getFallbackValueList()
	 */
	@Override
	public IValueList getFallbackValueList()
	{
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.dataprocessing.IValueList#getValueList()
	 */
	@Override
	public ValueList getValueList()
	{
		return null;
	}


	@Override
	public IDataProvider[] getDependedDataProviders()
	{
		return null;
	}
}
