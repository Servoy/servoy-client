/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2019 Servoy BV

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
package com.servoy.j2db.util;

import java.util.function.Predicate;

public class TypePredicate<T> implements Predicate<Object>
{
	private final Class<T> cls;
	private final Predicate< ? super T> delegate;

	public TypePredicate(Class<T> cls)
	{
		this(cls, null);
	}

	public TypePredicate(Class<T> cls, Predicate< ? super T> delegate)
	{
		this.cls = cls;
		this.delegate = delegate;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean test(Object o)
	{
		if (o != null && cls.isInstance(o))
		{
			return delegate == null || delegate.test((T)o);
		}
		return false;
	}


}
