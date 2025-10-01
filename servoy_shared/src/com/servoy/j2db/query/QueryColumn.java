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

import com.servoy.base.query.BaseColumnType;
import com.servoy.base.query.BaseQueryColumn;
import com.servoy.base.query.BaseQueryTable;
import com.servoy.j2db.util.UUID;
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
	protected UUID uuid = UUID.randomUUID(); // uuid of this column, known on the server, may be used to lookup name and columnType

	public QueryColumn(BaseQueryTable table, String name, BaseColumnType columnType, String nativeTypename, int flags, boolean identity)
	{
		super(table, name, columnType, nativeTypename, flags, identity);
	}

	public QueryColumn(BaseQueryTable table, String name, String alias, BaseColumnType columnType, String nativeTypename, int flags,
		boolean identity)
	{
		super(table, name, alias, columnType, nativeTypename, flags, identity);
	}

	public QueryColumn(BaseQueryTable table, UUID uuid, String name, BaseColumnType columnType, String nativeTypename, int flags, boolean identity)
	{
		super(table, name, columnType, nativeTypename, flags, identity);
		this.uuid = uuid;
	}

	public QueryColumn(BaseQueryTable table, String name, BaseColumnType columnType, String nativeTypename, int flags)
	{
		this(table, name, columnType, nativeTypename, flags, false);
	}

	public QueryColumn(BaseQueryTable table, String name, int sqlType, int length, int scale, String nativeTypename, int flags)
	{
		this(table, name, ColumnType.getInstance(sqlType, length, scale), nativeTypename, flags, false);
	}

	public QueryColumn(BaseQueryTable table, String name)
	{
		this(table, name, ColumnType.UNKNOWN, null, 0, false);
	}

	@Override
	public QueryColumn asAlias(String newAlias)
	{
		return new QueryColumn(table, name, newAlias, columnType, nativeTypename, getFlags(), identity);
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
		// server id not known, must serialize complete info
		return new ReplacedObject(
			AbstractBaseQuery.QUERY_SERIALIZE_DOMAIN,
			getClass(),
			new Object[] { table, name, new int[] { columnType.getSqlType(), columnType.getLength(), columnType.getScale(), identity ? 1
				: 0, flags, columnType.getSubType() }, alias, nativeTypename });
	}

	public QueryColumn(ReplacedObject s)
	{
		Object[] members = (Object[])s.getObject();
		// all fields are serialized
		int i = 0;
		table = (QueryTable)members[i++];
		name = (String)members[i++];
		int[] numbers = (int[])members[i++];
		identity = numbers[3] == 1;
		flags = numbers.length < 5 ? 0 : numbers[4]; // was added later, some old stored QueryColumns may not have this
		int subType = numbers.length < 6 ? 0 : numbers[5]; // was added later, some old stored QueryColumns may not have this
		alias = i < members.length ? (String)members[i++] : null;
		nativeTypename = i < members.length ? (String)members[i++] : null;

		columnType = ColumnType.getInstance(numbers[0], numbers[1], numbers[2], subType);
		uuid = null;
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
	public void update(String name, ColumnType columnType, String nativeTypename, int flags, boolean identity)
	{
		this.name = name;
		this.columnType = columnType;
		this.nativeTypename = nativeTypename;
		this.flags = flags;
		this.identity = identity;
	}

	public UUID getUUID()
	{
		return uuid;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.base.query.BaseQueryColumn#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (super.equals(obj))
		{
			return uuid != null && uuid.equals(((QueryColumn)obj).uuid);
		}
		return false;
	}
}
