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
package com.servoy.j2db.component;

import java.util.List;

import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.ApplicationException;
import com.servoy.j2db.dataprocessing.DataAdapterList;
import com.servoy.j2db.dataprocessing.IFoundSet;
import com.servoy.j2db.dataprocessing.IModificationListener;
import com.servoy.j2db.dataprocessing.IRecord;
import com.servoy.j2db.dataprocessing.SortColumn;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyException;

/**
 * @author jcompagner
 *
 */
public class ServoyBeanState implements IRecord
{

	private final IRecord record;

	private final FormScope formScope;

	/**
	 * Constructor to create a ServoyBeanData for a record and formscope
	 */
	public ServoyBeanState(IRecord record, FormScope formScope)
	{
		if (record == null) throw new IllegalArgumentException("Record is null"); //$NON-NLS-1$
		if (formScope == null) throw new IllegalArgumentException("FormScope is null"); //$NON-NLS-1$
		this.record = record;
		this.formScope = formScope;
	}

	/**
	 * @see IRecord#has(String)
	 */
	public boolean has(String dataprovider)
	{
		if (dataprovider == null) return false;
		if (!((Scriptable)record).has(dataprovider, ((Scriptable)record)))
		{
			if (!formScope.has(dataprovider, formScope))
			{
				return ((Scriptable)record.getParentFoundSet()).has(dataprovider, (Scriptable)record.getParentFoundSet());
			}
		}
		return true;
	}

	public void addModificationListener(IModificationListener l)
	{
		record.addModificationListener(l);
		formScope.getModificationSubject().addModificationListener(l);
	}

	public void addModificationListner(IModificationListener l)
	{
		record.addModificationListener(l);
		formScope.getModificationSubject().addModificationListener(l);
	}

	public boolean existInDataSource()
	{
		return record.existInDataSource();
	}

	public boolean existInDB()
	{
		return record.existInDataSource();
	}

	public IFoundSet getParentFoundSet()
	{
		return record.getParentFoundSet();
	}

	public Object[] getPK()
	{
		return record.getPK();
	}

	public IFoundSet getRelatedFoundSet(String relationName, List<SortColumn> defaultSortColumns)
	{
		return record.getRelatedFoundSet(relationName);
	}

	public IFoundSet getRelatedFoundSet(String relationName)
	{
		return record.getRelatedFoundSet(relationName);
	}

	public Object getValue(String dataProviderID)
	{
		return DataAdapterList.getValueObject(record, formScope, dataProviderID);
	}

	public boolean isLocked()
	{
		return record.isLocked();
	}

	public void removeModificationListener(IModificationListener l)
	{
		record.removeModificationListener(l);
		formScope.getModificationSubject().removeModificationListener(l);
	}

	public void removeModificationListner(IModificationListener l)
	{
		record.removeModificationListener(l);
		formScope.getModificationSubject().removeModificationListener(l);
	}

	public Object setValue(String dataProviderID, Object value)
	{
		try
		{
			return DataAdapterList.setValueObject(record, formScope, dataProviderID, value);
		}
		catch (IllegalArgumentException ex)
		{
			Debug.trace(ex);
			formScope.getFormController().getApplication().handleException(null, new ApplicationException(ServoyException.INVALID_INPUT, ex));
		}
		return null;
	}

	public boolean startEditing()
	{
		return record.startEditing();
	}

	@Override
	public Exception getException()
	{
		return record.getException();
	}
}
