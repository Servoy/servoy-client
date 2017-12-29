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
package com.servoy.j2db.util.visitor;

import java.util.HashMap;
import java.util.Map;

import com.servoy.j2db.util.IDelegate;

/**
 * Delegate vistor that calls the visit method from its delegate only once per element.
 *
 * @see IVisitable
 *
 * @author rgansevles
 */
public class VisitOnceDelegateVisitor implements IVisitor, IDelegate<IVisitor>
{
	private final Map<EqualityWrapper, Object> map = new HashMap<EqualityWrapper, Object>();
	private final IVisitor visitor;

	public VisitOnceDelegateVisitor(IVisitor visitor)
	{
		this.visitor = visitor;
	}

	public Object visit(Object o)
	{
		EqualityWrapper wrapper = new EqualityWrapper(o);
		Object visited = map.get(wrapper);
		if (visited != null || map.containsKey(wrapper))
		{
			// already visited before, return previous result
			return new IVisitor.VisitorResult(visited, false);
		}

		Object visitedResult = visitor.visit(o);
		if (visitedResult instanceof VisitorResult)
		{
			// delete returned whether or not to continue
			visited = ((VisitorResult)visitedResult).object;
		}
		else
		{
			visited = visitedResult;
			visitedResult = new IVisitor.VisitorResult(visited, true);
		}
		map.put(wrapper, visited);
		return visitedResult;
	}

	public IVisitor getDelegate()
	{
		return visitor;
	}

	/**
	 * Wrapper around object that uses pointer equality on wrapped object for equals.
	 *
	 * @author rgansevles
	 *
	 */
	static class EqualityWrapper
	{
		final private Object o;

		EqualityWrapper(Object o)
		{
			this.o = o;
		}

		@Override
		public int hashCode()
		{
			if (o == null) return 0;
			return o.hashCode();
		}

		@Override
		public boolean equals(Object obj)
		{
			// only equals if wrapping the same object pointer
			return obj instanceof EqualityWrapper && ((EqualityWrapper)obj).o == o;
		}
	}
}
