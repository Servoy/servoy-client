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

import com.servoy.j2db.util.IVisitor;
import com.servoy.j2db.util.serialize.ReplacedObject;

/**
 * Sort class in a query structure that refers to a column.
 * 
 * @author rob
 * 
 */
public final class QuerySort implements IQuerySort
{
	private IQuerySelectValue column;
	private final boolean ascending;

	/**
	 * @param table
	 * @param columnName
	 */
	public QuerySort(IQuerySelectValue column, boolean ascending)
	{
		this.column = column;
		this.ascending = ascending;
	}

	public IQuerySelectValue getColumn()
	{
		return column;
	}

	public boolean isAscending()
	{
		return ascending;
	}

	public Object shallowClone() throws CloneNotSupportedException
	{
		return super.clone();
	}

	public void acceptVisitor(IVisitor visitor)
	{
		column = (IQuerySelectValue)AbstractBaseQuery.acceptVisitor(column, visitor);
	}

	@Override
	public int hashCode()
	{
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((this.column == null) ? 0 : this.column.hashCode());
		result = PRIME * result + (this.ascending ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final QuerySort other = (QuerySort)obj;
		if (this.column == null)
		{
			if (other.column != null) return false;
		}
		else if (!this.column.equals(other.column)) return false;
		if (this.ascending != other.ascending) return false;
		return true;
	}

	@Override
	public String toString()
	{
		return new StringBuffer(column.toString()).append(ascending ? " ASC" : " DESC").toString(); //$NON-NLS-1$ //$NON-NLS-2$
	}

	///////// serialization ////////////////


	public Object writeReplace()
	{
		return new ReplacedObject(AbstractBaseQuery.QUERY_SERIALIZE_DOMAIN, getClass(), new Object[] { column, Boolean.valueOf(ascending) });
	}

	public QuerySort(ReplacedObject s)
	{
		Object[] members = (Object[])s.getObject();
		int i = 0;
		column = (IQuerySelectValue)members[i++];
		ascending = ((Boolean)members[i++]).booleanValue();
	}
}
