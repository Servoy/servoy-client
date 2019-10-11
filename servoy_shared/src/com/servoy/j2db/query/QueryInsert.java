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

import java.util.Arrays;

import com.servoy.base.query.BaseAbstractBaseQuery;
import com.servoy.base.query.BaseQueryTable;
import com.servoy.j2db.util.serialize.ReplacedObject;
import com.servoy.j2db.util.visitor.IVisitor;


/**
 * Query insert statement.
 *
 * @author rgansevles
 *
 */
public class QueryInsert extends AbstractBaseQuery implements ISQLUpdate
{
	BaseQueryTable table;
	private QueryColumn[] columns = null;
	private Object values = null; // maybe SQLValue placeholder or subquery

	public QueryInsert(BaseQueryTable table)
	{
		this.table = table;
	}

	public QueryInsert(BaseQueryTable table, QueryColumn[] columns, Object values)
	{
		this.table = table;
		setColumnValues(columns, values);
	}

	public void setColumnValues(QueryColumn[] columns, Object values)
	{
		this.values = validateValues(columns, values);
		this.columns = columns;
	}

	public boolean removeColumn(QueryColumn column)
	{
		for (int i = 0; i < columns.length; i++)
		{
			if (columns[i].getName().equals(column.getName()))
			{
				if (values instanceof Placeholder && ((Placeholder)values).isSet() && ((Placeholder)values).getValue() instanceof Object[])
				{
					((Placeholder)values).setValue(removeItem((Object[])((Placeholder)values).getValue(), i));
				}
				else if (values instanceof Object[])
				{
					values = removeItem((Object[])values, i);
				}
				columns = (QueryColumn[])removeItem(columns, i);
				return true;
			}
		}
		return false;
	}

	/**
	 * Remove the nth item from the object array. Return an array of the same type.
	 */
	private Object[] removeItem(Object[] array, int n)
	{
		Object[] res = (Object[])java.lang.reflect.Array.newInstance(array.getClass().getComponentType(), array.length - 1);
		if (n > 0)
		{
			System.arraycopy(array, 0, res, 0, n);
		}
		if (n < res.length)
		{
			System.arraycopy(array, n + 1, res, n, res.length - n);
		}
		return res;
	}

	private static Object validateValues(QueryColumn[] columns, Object values)
	{
		Object vals = values;

		if (columns == null || columns.length == 0)
		{
			throw new IllegalArgumentException("Empty column set in insert"); //$NON-NLS-1$
		}

		if (vals instanceof Placeholder || vals instanceof ISQLSelect)
		{
			// placeholder or sub-query: ok
			return vals;
		}

		// convenience: array of objects as wide as columns, convert to array of value arrays
		if (vals instanceof Object[] && !(vals instanceof Object[][]) && ((Object[])vals).length == columns.length)
		{
			Object[][] converted = new Object[((Object[])vals).length][];
			for (int i = 0; i < converted.length; i++)
			{
				converted[i] = new Object[] { ((Object[])vals)[i] };
			}
			vals = converted;
		}

		if (vals == null || !(vals instanceof Object[][]) || ((Object[][])vals).length != columns.length)
		{
			throw new IllegalArgumentException("Value list does not match column list in insert"); //$NON-NLS-1$
		}

		// ok
		return vals;
	}

	public BaseQueryTable getTable()
	{
		return table;
	}

	public QueryColumn[] getColumns()
	{
		return columns;
	}

	public Object getValues()
	{
		return values;
	}

	@Override
	public Object shallowClone() throws CloneNotSupportedException
	{
		return super.clone();
	}

	public void acceptVisitor(IVisitor visitor)
	{
		table = AbstractBaseQuery.acceptVisitor(table, visitor);
		columns = AbstractBaseQuery.acceptVisitor(columns, visitor);
		values = AbstractBaseQuery.acceptVisitor(values, visitor);
		if (values instanceof Placeholder && visitor instanceof PlaceHolderSetter)
		{
			PlaceHolderSetter phs = (PlaceHolderSetter)visitor;
			Placeholder ph = (Placeholder)values;
			if (ph.getKey().equals(phs.getKey()))
			{
				ph.setValue(validateValues(columns, phs.getValue()));
			}
		}
	}

	@Override
	public int hashCode()
	{
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + BaseAbstractBaseQuery.hashCode(this.columns);
		result = PRIME * result + ((this.table == null) ? 0 : this.table.hashCode());
		result = PRIME * result + ((this.values == null) ? 0 : BaseAbstractBaseQuery.arrayHashcode(this.values));
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final QueryInsert other = (QueryInsert)obj;
		if (!Arrays.equals(this.columns, other.columns)) return false;
		if (this.table == null)
		{
			if (other.table != null) return false;
		}
		else if (!this.table.equals(other.table)) return false;
		return BaseAbstractBaseQuery.arrayEquals(this.values, other.values);
	}

	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer(comment == null ? "" : "/* " + comment + " */ ") //
			.append("INSERT INTO ") //
			.append(table.toString());
		sb.append('(');
		for (int i = 0; i < columns.length; i++)
		{
			if (i > 0)
			{
				sb.append('|');
			}
			sb.append(columns[i].toString());
		}
		sb.append(") VALUES ("); //$NON-NLS-1$

		if (values instanceof Object[][])
		{
			Object[][] cols = (Object[][])values;
			for (int k = 0; k < cols.length; k++)
			{
				if (k > 0)
				{
					sb.append('|');
				}
				sb.append(BaseAbstractBaseQuery.toString(cols[k]));
			}
		}
		else
		{
			sb.append(BaseAbstractBaseQuery.toString(values));
		}
		sb.append(')');

		return sb.toString();
	}

	///////// serialization ////////////////

	@Override
	public Object writeReplace()
	{
		// Note: when this serialized structure changes, make sure that old data (maybe saved as serialized xml) can still be deserialized!
		return new ReplacedObject(AbstractBaseQuery.QUERY_SERIALIZE_DOMAIN, getClass(),
			new Object[] { table, ReplacedObject.convertArray(columns, Object.class), values, comment });
	}

	public QueryInsert(ReplacedObject s)
	{
		Object[] members = (Object[])s.getObject();
		int i = 0;
		this.table = (QueryTable)members[i++];
		this.columns = (QueryColumn[])ReplacedObject.convertArray((Object[])members[i++], QueryColumn.class);
		this.values = members[i++];
		if (i < members.length) // comment is a new field that was added, so it is optional now
		{
			this.comment = (String)members[i++];
		}
	}

}
