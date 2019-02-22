/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2018 Servoy BV

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

import com.servoy.base.query.BaseQueryTable;
import com.servoy.j2db.util.serialize.ReplacedObject;
import com.servoy.j2db.util.visitor.IVisitor;

/**
 * @author rgansevles
 *
 */
public class DerivedTable implements ITableReference
{
	private QuerySelect query;
	private QueryTable table;

	public DerivedTable(QuerySelect query, String name)
	{
		this.query = query;
		table = new QueryTable(null, null, null, null, name);
	}

	public QuerySelect getQuery()
	{
		return query;
	}

	@Override
	public BaseQueryTable getTable()
	{
		return table;
	}

	public Object shallowClone() throws CloneNotSupportedException
	{
		return super.clone();
	}

	public void acceptVisitor(IVisitor visitor)
	{
		query = AbstractBaseQuery.acceptVisitor(query, visitor);
		table = AbstractBaseQuery.acceptVisitor(table, visitor);
	}

	@Override
	public String toString()
	{
		return query + " AS " + table.getAlias();
	}


	///////// serialization ////////////////

	public Object writeReplace()
	{
		// Note: when this serialized structure changes, make sure that old data (maybe saved as serialized xml) can still be deserialized!
		return new ReplacedObject(AbstractBaseQuery.QUERY_SERIALIZE_DOMAIN, getClass(), new Object[] { query, table });
	}

	public DerivedTable(ReplacedObject s)
	{
		int i = 0;
		Object[] members = (Object[])s.getObject();
		query = (QuerySelect)members[i++];
		table = (QueryTable)members[i++];
	}

}
