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
 * Query join based on user-defined string. The foreign tables (comma-separated string) are used as one string, the join condition is not part of this join and
 * must be applied separately to the select query.
 * 
 * @author rgansevles
 * 
 */
public final class QueryCustomJoin implements ISQLJoin
{
	private final String name;
	private QueryTable primaryTable;
	private final String foreignTables;

	/**
	 * Custom join with one or more tables.
	 * 
	 * @param name
	 * @param primaryTable
	 * @param foreignTables
	 */
	public QueryCustomJoin(String name, QueryTable primaryTable, String foreignTables)
	{
		this.name = name;
		this.primaryTable = primaryTable;
		this.foreignTables = foreignTables;
	}

	public String getName()
	{
		return name;
	}

	public QueryTable getPrimaryTable()
	{
		return primaryTable;
	}

	public String getForeignTables()
	{
		return foreignTables;
	}

	public Object shallowClone() throws CloneNotSupportedException
	{
		return super.clone();
	}

	public void acceptVisitor(IVisitor visitor)
	{
		primaryTable = (QueryTable)AbstractBaseQuery.acceptVisitor(primaryTable, visitor);
	}

	@Override
	public int hashCode()
	{
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((this.foreignTables == null) ? 0 : this.foreignTables.hashCode());
		result = PRIME * result + ((this.name == null) ? 0 : this.name.hashCode());
		result = PRIME * result + ((this.primaryTable == null) ? 0 : this.primaryTable.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final QueryCustomJoin other = (QueryCustomJoin)obj;
		if (this.foreignTables == null)
		{
			if (other.foreignTables != null) return false;
		}
		else if (!this.foreignTables.equals(other.foreignTables)) return false;
		if (this.name == null)
		{
			if (other.name != null) return false;
		}
		else if (!this.name.equals(other.name)) return false;
		if (this.primaryTable == null)
		{
			if (other.primaryTable != null) return false;
		}
		else if (!this.primaryTable.equals(other.primaryTable)) return false;
		return true;
	}

	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer("CUSTOM JOIN");//$NON-NLS-1$
		sb.append(' ').append(name);
		sb.append(" FROM ").append(primaryTable.toString()); //$NON-NLS-1$
		sb.append(" TO ").append(foreignTables); //$NON-NLS-1$
		return sb.toString();
	}


	///////// serialization ////////////////


	public Object writeReplace()
	{
		return new ReplacedObject(AbstractBaseQuery.QUERY_SERIALIZE_DOMAIN, getClass(), new Object[] { name, primaryTable, foreignTables });
	}

	public QueryCustomJoin(ReplacedObject s)
	{
		Object[] members = (Object[])s.getObject();
		int i = 0;
		name = (String)members[i++];
		primaryTable = (QueryTable)members[i++];
		foreignTables = (String)members[i++];
	}

}
