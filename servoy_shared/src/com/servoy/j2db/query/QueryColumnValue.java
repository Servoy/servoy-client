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

import java.util.Date;

import com.servoy.base.query.BaseColumnType;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.util.serialize.ReplacedObject;
import com.servoy.j2db.util.visitor.IVisitor;


/**
 * Container for a fixed value in a query structure (for instance, "select 1 from tab".
 *
 * @author rgansevles
 *
 */
public final class QueryColumnValue implements IQuerySelectValue
{
	private Object value;
	private final String alias;
	private final boolean fixedvalue;

	public QueryColumnValue(Object value, String alias)
	{
		this(value, alias, false);
	}

	public QueryColumnValue(Object value, String alias, boolean fixedvalue)
	{
		this.value = value;
		this.alias = alias;
		this.fixedvalue = fixedvalue;
	}

	@Override
	public IQuerySelectValue asAlias(String newAlias)
	{
		return new QueryColumnValue(value, newAlias, fixedvalue);
	}

	public Object getValue()
	{
		return value;
	}

	public String getAlias()
	{
		return alias;
	}

	/**
	 * @return the fixedvalue
	 */
	public boolean isFixedvalue()
	{
		return fixedvalue;
	}

	public QueryColumn getColumn()
	{
		return null;
	}

	@Override
	public BaseColumnType getColumnType()
	{
		if (value instanceof Integer || value instanceof Long)
		{
			return ColumnType.getColumnType(IColumnTypes.INTEGER);
		}
		if (value instanceof Number)
		{
			return ColumnType.getColumnType(IColumnTypes.NUMBER);
		}
		if (value instanceof String)
		{
			return ColumnType.getColumnType(IColumnTypes.TEXT);
		}
		if (value instanceof Date)
		{
			return ColumnType.getColumnType(IColumnTypes.DATETIME);
		}
		if (value instanceof byte[])
		{
			return ColumnType.getColumnType(IColumnTypes.MEDIA);
		}

		return null;
	}

	@Override
	public Object shallowClone() throws CloneNotSupportedException
	{
		return super.clone();
	}

	public void acceptVisitor(IVisitor visitor)
	{
		value = AbstractBaseQuery.acceptVisitor(value, visitor);
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((alias == null) ? 0 : alias.hashCode());
		result = prime * result + (fixedvalue ? 1231 : 1237);
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		QueryColumnValue other = (QueryColumnValue)obj;
		if (alias == null)
		{
			if (other.alias != null) return false;
		}
		else if (!alias.equals(other.alias)) return false;
		if (fixedvalue != other.fixedvalue) return false;
		if (value == null)
		{
			if (other.value != null) return false;
		}
		else if (!value.equals(other.value)) return false;
		return true;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder(alias == null ? "<anonymous>" : alias).append('='); //$NON-NLS-1$
		if (value == null)
		{
			sb.append("<null>"); //$NON-NLS-1$
		}
		else
		{
			sb.append(value.toString());
		}
		if (fixedvalue) sb.append('*');
		return sb.toString();
	}


	///////// serialization ////////////////


	public Object writeReplace()
	{
		// Note: when this serialized structure changes, make sure that old data (maybe saved as serialized xml) can still be deserialized!
		return new ReplacedObject(AbstractBaseQuery.QUERY_SERIALIZE_DOMAIN, getClass(), new Object[] { value, alias, Boolean.valueOf(fixedvalue) });
	}

	public QueryColumnValue(ReplacedObject s)
	{
		Object[] members = (Object[])s.getObject();
		int i = 0;
		value = members[i++];
		alias = (String)members[i++];
		fixedvalue = ((Boolean)members[i++]).booleanValue();
	}

}
