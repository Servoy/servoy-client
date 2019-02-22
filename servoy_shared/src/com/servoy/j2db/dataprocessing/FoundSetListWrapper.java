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

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyException;

/**
 * Class to make foundset look like a List
 * @author jblok
 */
public class FoundSetListWrapper implements List<IRecordInternal>, Serializable
{
	public static final FoundSetListWrapper EMPTY = new FoundSetListWrapper(null);

	private final IFoundSetInternal fs;

	public FoundSetListWrapper(IFoundSetInternal fs)
	{
		this.fs = fs;
	}

	public void add(int index, IRecordInternal element)
	{
		throw new UnsupportedOperationException("add not supported on foundset wrapper"); //$NON-NLS-1$
	}

	public boolean add(IRecordInternal o)
	{
		throw new UnsupportedOperationException("add not supported on foundset wrapper"); //$NON-NLS-1$
	}

	public boolean addAll(Collection< ? extends IRecordInternal> c)
	{
		throw new UnsupportedOperationException("add not supported on foundset wrapper"); //$NON-NLS-1$
	}

	public boolean addAll(int index, Collection< ? extends IRecordInternal> c)
	{
		throw new UnsupportedOperationException("add not supported on foundset wrapper"); //$NON-NLS-1$
	}

	public void clear()
	{
		if (fs != null) fs.clear();
	}

	public boolean contains(Object o)
	{
		return (fs != null) && (o instanceof IRecordInternal) && (fs.getRecordIndex((IRecordInternal)o) != -1);
	}

	public boolean containsAll(Collection< ? > c)
	{
		throw new UnsupportedOperationException("containsAll not supported on foundset wrapper"); //$NON-NLS-1$
	}

	public IRecordInternal get(int index)
	{
		return (fs == null) ? null : fs.getRecord(index);
	}

	public int indexOf(Object o)
	{
		return (fs != null && o instanceof IRecordInternal) ? fs.getRecordIndex((IRecordInternal)o) : -1;
	}

	public boolean isEmpty()
	{
		return (fs == null || fs.getSize() == 0);
	}

	public Iterator<IRecordInternal> iterator()
	{
		throw new UnsupportedOperationException("iterator not supported on foundset wrapper"); //$NON-NLS-1$
	}

	public int lastIndexOf(Object o)
	{
		throw new UnsupportedOperationException("lastIndexOf not supported on foundset wrapper"); //$NON-NLS-1$
	}

	public ListIterator<IRecordInternal> listIterator()
	{
		throw new UnsupportedOperationException("iterator not supported on foundset wrapper"); //$NON-NLS-1$
	}

	public ListIterator<IRecordInternal> listIterator(int index)
	{
		throw new UnsupportedOperationException("iterator not supported on foundset wrapper"); //$NON-NLS-1$
	}

	public IRecordInternal remove(int index)
	{
		IRecordInternal record = null;
		if (fs != null)
		{
			record = fs.getRecord(index);
			if (record != null)
			{
				try
				{
					fs.deleteRecord(record);
				}
				catch (ServoyException e)
				{
					Debug.error(e);
					return null;
				}
			}
		}
		return record;
	}

	public boolean remove(Object o)
	{
		if (fs != null && o instanceof IRecordInternal)
		{
			try
			{
				fs.deleteRecord((IRecordInternal)o);
				return true;
			}
			catch (ServoyException e)
			{
				Debug.error(e);
			}
		}
		return false;
	}

	public boolean removeAll(Collection< ? > c)
	{
		try
		{
			if (fs != null) fs.deleteAllRecords();
		}
		catch (ServoyException e)
		{
			Debug.error(e);
		}
		return false;
	}

	public boolean retainAll(Collection< ? > c)
	{
		throw new UnsupportedOperationException("retainAll not supported on foundset wrapper"); //$NON-NLS-1$
	}

	public IRecordInternal set(int index, IRecordInternal element)
	{
		throw new UnsupportedOperationException("set not supported on foundset wrapper"); //$NON-NLS-1$
	}

	public int size()
	{
		return fs == null ? 0 : fs.getSize();
	}

	public List<IRecordInternal> subList(int fromIndex, int toIndex)
	{
		throw new UnsupportedOperationException("subList not supported on foundset wrapper"); //$NON-NLS-1$
	}

	public Object[] toArray()
	{
		throw new UnsupportedOperationException("toArray not supported on foundset wrapper"); //$NON-NLS-1$
	}

	public <T> T[] toArray(T[] a)
	{
		throw new UnsupportedOperationException("toArray not supported on foundset wrapper"); //$NON-NLS-1$
	}

	public IRecordInternal getRecord(Object[] pk)
	{
		return fs == null ? null : fs.getRecord(pk);
	}

	public IFoundSetInternal getFoundSet()
	{
		return fs;
	}
}
