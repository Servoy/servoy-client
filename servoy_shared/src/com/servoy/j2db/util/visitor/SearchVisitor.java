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
package com.servoy.j2db.util.visitor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Visitor class for searching elements that match a given filter.
 *
 * @see IVisitable
 * @author rgansevles
 *
 */
public class SearchVisitor<T> implements IVisitor
{
	private final Predicate<Object> filter;
	private final List<T> found = new ArrayList<>();

	public SearchVisitor(Predicate<Object> filter)
	{
		this.filter = filter;
	}

	public Object visit(Object o)
	{
		if (filter.test(o))
		{
			found.add((T)o);
		}
		return o;
	}

	public List<T> getFound()
	{
		return found;
	}
}
