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

import static com.servoy.j2db.query.AbstractBaseQuery.QUERY_SERIALIZE_DOMAIN;

import com.servoy.base.query.BaseQueryTable;
import com.servoy.j2db.util.serialize.ReplacedObject;
import com.servoy.j2db.util.visitor.IVisitor;

/**
 * Select statement wrapper: select * from <query>.
 *
 * @author rgansevles
 */
public final class QuerySelectAllFrom implements ISQLSelect
{
	private ISQLSelect select;

	private static final long serialVersionUID = 1L;

	private final String name;

	public QuerySelectAllFrom(ISQLSelect select, String name)
	{
		this.select = select;
		this.name = name;
	}

	/**
	 * @return the select
	 */
	public ISQLSelect getSelect()
	{
		return select;
	}

	/**
	 * @return the name
	 */
	public String getName()
	{
		return name;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.query.ISQLQuery#getTable()
	 */
	@Override
	public BaseQueryTable getTable()
	{
		return select.getTable();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.query.ISQLCloneable#shallowClone()
	 */
	@Override
	public Object shallowClone() throws CloneNotSupportedException
	{
		return clone();
	}

	public void acceptVisitor(IVisitor visitor)
	{
		select = AbstractBaseQuery.acceptVisitor(select, visitor);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((select == null) ? 0 : select.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		QuerySelectAllFrom other = (QuerySelectAllFrom)obj;
		if (name == null)
		{
			if (other.name != null) return false;
		}
		else if (!name.equals(other.name)) return false;
		if (select == null)
		{
			if (other.select != null) return false;
		}
		else if (!select.equals(other.select)) return false;
		return true;
	}

	@Override
	public String toString()
	{
		return new StringBuffer("SELECT ALL FROM <").append(select).append("> ").append(name).toString();
	}

	///////// serialization ////////////////

	@Override
	public Object writeReplace()
	{
		// Note: when this serialized structure changes, make sure that old data (maybe saved as serialized xml) can still be deserialized!
		return new ReplacedObject(QUERY_SERIALIZE_DOMAIN, getClass(), new Object[] { select, name });
	}

	public QuerySelectAllFrom(ReplacedObject s)
	{
		Object[] members = (Object[])s.getObject();
		int i = 0;
		this.select = (QuerySelect)members[i++];
		this.name = (String)members[i++];
	}
}
