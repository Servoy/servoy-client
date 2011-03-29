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

import com.servoy.j2db.util.Immutable;
import com.servoy.j2db.util.serialize.ReplacedObject;
import com.servoy.j2db.util.visitor.IVisitor;

/**
 * Query sort class that uses a user-defined string.
 * 
 * @author rgansevles
 * 
 */
public final class QueryCustomSort implements IQuerySort, Immutable
{
	private final String sort;

	/**
	 * @param string
	 */
	public QueryCustomSort(String sort)
	{
		this.sort = sort;
	}

	public String getSort()
	{
		return sort;
	}

	public Object shallowClone() throws CloneNotSupportedException
	{
		return super.clone();
	}

	@Override
	public int hashCode()
	{
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((this.sort == null) ? 0 : this.sort.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final QueryCustomSort other = (QueryCustomSort)obj;
		if (this.sort == null)
		{
			if (other.sort != null) return false;
		}
		else if (!this.sort.equals(other.sort)) return false;
		return true;
	}

	public void acceptVisitor(IVisitor visitor)
	{
	}

	@Override
	public String toString()
	{
		return "CUSTOM SORT " + sort; //$NON-NLS-1$
	}


	///////// serialization ////////////////


	public Object writeReplace()
	{
		return new ReplacedObject(AbstractBaseQuery.QUERY_SERIALIZE_DOMAIN, getClass(), sort);
	}

	public QueryCustomSort(ReplacedObject s)
	{
		sort = (String)s.getObject();
	}


}
