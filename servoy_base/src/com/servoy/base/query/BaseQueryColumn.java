/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.base.query;


/**
 * Quedry column for mobile and regular clients.
 *
 * @author rgansevles
 *
 */
public class BaseQueryColumn implements IBaseQuerySelectValue
{
	protected BaseQueryTable table;
	protected transient String name;
	protected String alias;
	protected transient BaseColumnType columnType;
	protected transient boolean identity;
	protected int id; // id of this column, known on the server, may be used to lookup name and columnType

	public BaseQueryColumn(BaseQueryTable table, int id, String name, BaseColumnType columnType, boolean identity)
	{
		this(table, id, name, null, columnType, identity);
	}

	public BaseQueryColumn(BaseQueryTable table, int id, String name, String alias, BaseColumnType columnType, boolean identity)
	{
		if (table == null || (id == -1 && name == null))
		{
			throw new IllegalArgumentException("Null table or column argument"); //$NON-NLS-1$
		}
		this.table = table;
		this.id = id;
		this.name = name;
		this.alias = alias;
		this.columnType = columnType;
		this.identity = identity;
	}

	protected BaseQueryColumn()
	{
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
		return alias;
	}

	public BaseQueryTable getTable()
	{
		return table;
	}

	public int getId()
	{
		return id;
	}

	public BaseQueryColumn getColumn()
	{
		return this;
	}

	public BaseColumnType getColumnType()
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
		result = PRIME * result + ((this.alias == null) ? 0 : this.alias.hashCode());
		result = PRIME * result + ((this.table == null) ? 0 : this.table.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final BaseQueryColumn other = (BaseQueryColumn)obj;
		if (this.name == null)
		{
			if (other.name != null) return false;
		}
		else if (!this.name.equals(other.name)) return false;
		if (this.alias == null)
		{
			if (other.alias != null) return false;
		}
		else if (!this.alias.equals(other.alias)) return false;
		if (this.table == null)
		{
			if (other.table != null) return false;
		}
		else if (!this.table.equals(other.table)) return false;
		return id != -1 && this.id == other.id;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder(table.toString()).append('.').append(id).append('=');
		sb.append((name == null) ? "?" : name); //$NON-NLS-1$
		if (alias != null) sb.append(" AS ").append(alias); //$NON-NLS-1$
		if (name != null)
		{
			sb.append(columnType.toString());
			if (isIdentity())
			{
				sb.append(" IDENTITY"); //$NON-NLS-1$
			}
		}
		return sb.toString();
	}

}
