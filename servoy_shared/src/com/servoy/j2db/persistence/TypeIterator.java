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
package com.servoy.j2db.persistence;


import java.util.Iterator;
import java.util.List;

import com.servoy.j2db.util.FilteredIterator;
import com.servoy.j2db.util.IFilter;

/**
 * Enum for one type of Repository types
 * 
 * @author jblok
 */
public class TypeIterator<T extends IPersist> extends FilteredIterator<T>
{

	public TypeIterator(List<IPersist> list, final int type)
	{
		this(list.iterator(), type, null);
	}

	public TypeIterator(List<IPersist> list, final int type, final IFilter<T> extraFilter)
	{
		this(list.iterator(), type, extraFilter);
	}

	public TypeIterator(Iterator<IPersist> iterator, final int type)
	{
		this(iterator, type, null);
	}

	public TypeIterator(Iterator<IPersist> iterator, final int type, final IFilter<T> extraFilter)
	{
		super(iterator, new IFilter<T>()
		{
			public boolean match(Object p)
			{
				return p instanceof IPersist && ((IPersist)p).getTypeID() == type && (extraFilter == null || extraFilter.match(p));
			}
		});
	}

	@Override
	public void remove()
	{
		throw new UnsupportedOperationException();
	}
}
