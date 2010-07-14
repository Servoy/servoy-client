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
 * Class for a column referring to a table in a query structure.
 * 
 * @author rgansevles
 * 
 */
public final class QueryColumn implements IQuerySelectValue
{
	private QueryTable table;
	private transient String name;
	private transient ColumnType columnType;
	private transient boolean identity;
	private final int id; // id of this column, known on the server, may be used to lookup name and columnType

	public QueryColumn(QueryTable table, int id, String name, int sqlType, int length, int scale, boolean identity)
	{
		if (table == null || (id == -1 && name == null))
		{
			throw new IllegalArgumentException("Null table or column argument"); //$NON-NLS-1$
		}
		if (id == -1 && name.indexOf('.') >= 0)
		{
			throw new IllegalArgumentException("Invalid column name '" + name + '\''); //$NON-NLS-1$
		}
		this.table = table;
		this.id = id;
		this.name = name;
		this.columnType = ColumnType.getInstance(sqlType, length, scale);
		this.identity = identity;
	}

	public QueryColumn(QueryTable table, int id, String name, int sqlType, int length, int scale)
	{
		this(table, id, name, sqlType, length, scale, false);
	}

	public QueryColumn(QueryTable table, int id, String name, int sqlType, int length)
	{
		this(table, id, name, sqlType, length, 0, false);
	}

	public QueryColumn(QueryTable table, String name)
	{
		this(table, -1, name, -1, -1, 0, false);
	}

	public boolean isComplete()
	{
		return name != null;
	}

	public String getName()
	{
		if (name == null)
		{
			throw new IllegalStateException("Name requested on incomplete column"); //$NON-NLS-1$
		}
		return name;
	}

	public String getAlias()
	{
		return null;
	}

	public QueryTable getTable()
	{
		return table;
	}

	public int getId()
	{
		return id;
	}

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

	public ColumnType getColumnType()
	{
		if (name == null)
		{
			throw new IllegalStateException("Column type requested on incomplete column"); //$NON-NLS-1$
		}
		return columnType;
	}

	public boolean isIdentity()
	{
		if (name == null)
		{
			throw new IllegalStateException("Identity requested on incomplete column"); //$NON-NLS-1$
		}
		return identity;
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
		final QueryColumn other = (QueryColumn)obj;
		if (this.id != -1 || other.id != -1) return this.id == other.id;
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
		StringBuffer sb = new StringBuffer(table.toString()).append('.').append(id).append('=');
		if (name == null)
		{
			sb.append('?');
		}
		else
		{
			sb.append(name);
			sb.append(columnType.toString());
			if (isIdentity())
			{
				sb.append(" IDENTITY"); //$NON-NLS-1$
			}
		}
		return sb.toString();
	}


	///////// serialization ////////////////

	public Object writeReplace()
	{
		if (id == -1)
		{
			// server id not known, must serialize complete info
			return new ReplacedObject(AbstractBaseQuery.QUERY_SERIALIZE_DOMAIN, getClass(),
				new Object[] { table, name, new int[] { columnType.getSqlType(), columnType.getLength(), columnType.getScale(), identity ? 1 : 0 } });
		}
		else
		{
			// server id known, just serialize id and table, the server will update
			return new ReplacedObject(AbstractBaseQuery.QUERY_SERIALIZE_DOMAIN, getClass(), new Object[] { table, new Integer(id) });
		}
	}

	public QueryColumn(ReplacedObject s)
	{
		Object[] members = (Object[])s.getObject();
		if (members.length == 2)
		{
			// just the id and table are serialized, the server must update the fields
			int i = 0;
			table = (QueryTable)members[i++];
			id = ((Integer)members[i++]).intValue();
			name = null;
			columnType = null;
			identity = false;
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
	public void update(String name, int sqlType, int length, int scale, boolean identity)
	{
		this.name = name;
		this.columnType = ColumnType.getInstance(sqlType, length, scale);
		this.identity = identity;
	}

}
