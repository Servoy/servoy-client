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

import com.servoy.base.query.BaseQueryColumn;
import com.servoy.base.query.BaseQueryTable;
import com.servoy.j2db.util.serialize.IWriteReplaceExtended;
import com.servoy.j2db.util.serialize.ReplacedObject;
import com.servoy.j2db.util.visitor.IVisitor;


/**
 * Class for a column referring to a table in a query structure.
 *
 * @author rgansevles
 *
 */
public final class QueryColumn extends BaseQueryColumn implements IWriteReplaceExtended, IQuerySelectValue
{
	public QueryColumn(BaseQueryTable table, int id, String name, ColumnType columnType, int flags, boolean identity)
	{
		super(table, id, name, columnType, flags, identity);
	}

	public QueryColumn(BaseQueryTable table, int id, String name, String alias, ColumnType columnType, int flags, boolean identity)
	{
		super(table, id, name, alias, columnType, flags, identity);
	}

	public QueryColumn(BaseQueryTable table, int id, String name, int sqlType, int length, int scale, int flags, boolean identity)
	{
		this(table, id, name, ColumnType.getInstance(sqlType, length, scale), flags, identity);
	}

	public QueryColumn(BaseQueryTable table, int id, String name, int sqlType, int length, int scale, int flags)
	{
		this(table, id, name, ColumnType.getInstance(sqlType, length, scale), flags, false);
	}

	public QueryColumn(BaseQueryTable table, String name)
	{
		this(table, -1, name, ColumnType.DUMMY, 0, false);
	}

	@Override
	public IQuerySelectValue asAlias(String newAlias)
	{
		return new QueryColumn(table, id, name, newAlias, ColumnType.getInstance(columnType.getSqlType(), columnType.getLength(), columnType.getScale()),
			getFlags(), identity);
	}

	@Override
	public QueryColumn getColumn()
	{
		return this;
	}

	public Object shallowClone() throws CloneNotSupportedException
	{
		return super.clone();
	}

	public void acceptVisitor(IVisitor visitor)
	{
		table = AbstractBaseQuery.acceptVisitor(table, visitor);
	}

	///////// serialization ////////////////

	public Object writeReplace()
	{
		return writeReplace(false);
	}

	public ReplacedObject writeReplace(boolean full)
	{
		// Note: when this serialized structure changes, make sure that old data (maybe saved as serialized xml) can still be deserialized!
		if (id == -1 || full)
		{
			// server id not known, must serialize complete info
			return new ReplacedObject(
				AbstractBaseQuery.QUERY_SERIALIZE_DOMAIN,
				getClass(),
				new Object[] { table, name, new int[] { columnType.getSqlType(), columnType.getLength(), columnType.getScale(), identity ? 1 : 0, flags }, alias });
		}
		else
		{
			// server id known, just serialize id and table, the server will update
			return new ReplacedObject(AbstractBaseQuery.QUERY_SERIALIZE_DOMAIN, getClass(), new Object[] { table, new Integer(id), alias });
		}
	}

	public QueryColumn(ReplacedObject s)
	{
		Object[] members = (Object[])s.getObject();
		if (members[1] instanceof Integer)
		{
			// just the id and table are serialized, optionally alias, the server must update the fields
			int i = 0;
			table = (QueryTable)members[i++];
			id = ((Integer)members[i++]).intValue();
			alias = i < members.length ? (String)members[i++] : null;
			name = null;
			columnType = null;
			identity = false;
			flags = 0;
		}
		else
		{
			// all fields are serialized
			int i = 0;
			table = (QueryTable)members[i++];
			name = (String)members[i++];
			int[] numbers = (int[])members[i++];
			columnType = ColumnType.getInstance(numbers[0], numbers[1], numbers[2]);
			identity = numbers[3] == 1;
			flags = numbers.length < 5 ? 0 : numbers[4]; // was added later, some old stored QueryColumns may not have this
			alias = i < members.length ? (String)members[i++] : null;
			id = -1;
		}
	}

	/**
	 * Update the fields that have not been set in serialization
	 *
	 * @param name
	 * @param sqlType
	 * @param length
	 * @param scale
	 * @param identity
	 */
	public void update(String name, int sqlType, int length, int scale, int flags, boolean identity)
	{
		this.name = name;
		this.columnType = ColumnType.getInstance(sqlType, length, scale);
		this.flags = flags;
		this.identity = identity;
	}

}
