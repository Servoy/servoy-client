/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2024 Servoy BV

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

import java.util.Iterator;

import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ES6Iterator;
import org.mozilla.javascript.Scriptable;

public final class IterableES6Iterator extends ES6Iterator
{
	private final Iterator<Object> iterator;
	private static final String ITERATOR_TAG = "FoundSetIterator";

	IterableES6Iterator(Scriptable scope, Iterable dataModel)
	{
		super(scope, ITERATOR_TAG);
		this.iterator = dataModel.iterator();
	}

	@Override
	protected boolean isDone(Context cx, Scriptable scope)
	{
		return !iterator.hasNext();
	}

	@Override
	protected Object nextValue(Context cx, Scriptable scope)
	{
		if (!iterator.hasNext())
		{
			return null;
		}
		return iterator.next();
	}

	@Override
	public String getClassName()
	{
		return ITERATOR_TAG;
	}

	@Override
	public Object get(String name, Scriptable start)
	{
		if (NEXT_METHOD.equals(name))
		{
			return new Callable()
			{
				@Override
				public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
				{
					return IterableES6Iterator.this.next(cx, scope);
				}

			};
		}
		return super.get(name, start);
	}
}