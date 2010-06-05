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
 * Key class for named value in a query structure.
 * 
 * @author rob
 * 
 */

public final class PlaceholderKey implements IQueryElement
{
	QueryTable table;
	private final String name;

	/**
	 * @param name
	 */
	public PlaceholderKey(QueryTable table, String name)
	{
		this.table = table;
		this.name = name;
	}

	public String getName()
	{
		return name;
	}

	public Object shallowClone() throws CloneNotSupportedException
	{
		return super.clone();
	}

	public void acceptVisitor(IVisitor visitor)
	{
		table = (QueryTable)AbstractBaseQuery.acceptVisitor(table, visitor);
	}

	@Override
	public int hashCode()
	{
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((this.name == null) ? 0 : this.name.hashCode());
		result = PRIME * result + ((this.table == null) ? 0 : this.table.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final PlaceholderKey other = (PlaceholderKey)obj;
		if (this.name == null)
		{
			if (other.name != null) return false;
		}
		else if (!this.name.equals(other.name)) return false;
		if (this.table == null)
		{
			if (other.table != null) return false;
		}
		else if (!this.table.equals(other.table)) return false;
		return true;
	}

	@Override
	public String toString()
	{
		return new StringBuffer(table.toString()).append(':').append(name).toString();
	}

	///////// serialization ////////////////


	public Object writeReplace()
	{
		return new ReplacedObject(AbstractBaseQuery.QUERY_SERIALIZE_DOMAIN, getClass(), new Object[] { table, name });
	}

	public PlaceholderKey(ReplacedObject s)
	{
		Object[] members = (Object[])s.getObject();
		int i = 0;
		table = (QueryTable)members[i++];
		name = (String)members[i++];
	}

}
