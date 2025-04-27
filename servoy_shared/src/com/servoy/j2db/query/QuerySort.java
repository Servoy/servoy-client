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
package com.servoy.j2db.query;

import com.servoy.j2db.persistence.SortingNullprecedence;
import com.servoy.j2db.util.serialize.ReplacedObject;
import com.servoy.j2db.util.visitor.IVisitor;

/**
 * Sort class in a query structure that refers to a column.
 *
 * @author rgansevles
 *
 */
public final class QuerySort implements IQuerySort
{
	private IQuerySelectValue column;
	private final boolean ascending;
	private final SortOptions options;

	public QuerySort(IQuerySelectValue column, boolean ascending, SortOptions options)
	{
		this.column = column;
		this.ascending = ascending;
		this.options = options;
	}

	public IQuerySelectValue getColumn()
	{
		return column;
	}

	public boolean isAscending()
	{
		return ascending;
	}

	public boolean isIgnoreCase()
	{
		return options.ignoreCase();
	}

	public SortingNullprecedence nullprecedence()
	{
		return options.nullprecedence();
	}

	@Override
	public Object shallowClone() throws CloneNotSupportedException
	{
		return super.clone();
	}

	public void acceptVisitor(IVisitor visitor)
	{
		column = AbstractBaseQuery.acceptVisitor(column, visitor);
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + (ascending ? 1231 : 1237);
		result = prime * result + ((column == null) ? 0 : column.hashCode());
		result = prime * result + ((options == null) ? 0 : options.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		QuerySort other = (QuerySort)obj;
		if (ascending != other.ascending) return false;
		if (column == null)
		{
			if (other.column != null) return false;
		}
		else if (!column.equals(other.column)) return false;
		if (options != other.options) return false;
		return true;
	}


	@Override
	public String toString()
	{
		return new StringBuilder(column.getColumnName()).append(ascending ? " ASC" : " DESC").toString();
	}

	///////// serialization ////////////////

	public Object writeReplace()
	{
		// Note: when this serialized structure changes, make sure that old data (maybe saved as serialized xml) can still be deserialized!
		return new ReplacedObject(AbstractBaseQuery.QUERY_SERIALIZE_DOMAIN, getClass(),
			new Object[] { column, Boolean.valueOf(ascending), options });
	}

	public QuerySort(ReplacedObject s)
	{
		Object[] members = (Object[])s.getObject();
		int i = 0;
		column = (IQuerySelectValue)members[i++];
		ascending = ((Boolean)members[i++]).booleanValue();
		options = i < members.length ? (SortOptions)members[i++] : SortOptions.NONE;
	}
}
