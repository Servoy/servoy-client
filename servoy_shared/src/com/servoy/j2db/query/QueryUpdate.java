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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.servoy.base.query.BaseAbstractBaseQuery;
import com.servoy.base.query.BaseQueryTable;
import com.servoy.j2db.util.serialize.ReplacedObject;
import com.servoy.j2db.util.visitor.IVisitor;

/**
 * Query update statement.
 *
 * @author rgansevles
 *
 */
public class QueryUpdate extends AbstractBaseQuery implements ISQLUpdate
{
	private BaseQueryTable table;
	private List<QueryColumn> columns = new ArrayList<>();
	private List<Object> values = new ArrayList<>();
	private AndCondition condition = new AndCondition();

	public QueryUpdate(BaseQueryTable table)
	{
		this.table = table;
	}

	public void addValue(QueryColumn column, Object value)
	{
		columns.add(column);
		values.add(value);
	}

	public boolean hasValues()
	{
		return (values.size() > 0);
	}

	public List<QueryColumn> getColumns()
	{
		return columns;
	}

	public List<Object> getValues()
	{
		return values;
	}

	public void setCondition(ISQLCondition c)
	{
		setCondition(null, c);
	}

	public void addCondition(ISQLCondition c)
	{
		addCondition(null, c);
	}

	public void setCondition(String name, ISQLCondition c)
	{
		condition.setCondition(name, c);
	}

	public void addCondition(String name, ISQLCondition c)
	{
		condition.addCondition(name, c);
	}

	public ISQLCondition getCondition()
	{
		return condition;
	}

	public ISQLCondition getConditionClone()
	{
		return AbstractBaseQuery.deepClone(getCondition());
	}

	public BaseQueryTable getTable()
	{
		return table;
	}

	@Override
	public Object shallowClone() throws CloneNotSupportedException
	{
		return super.clone();
	}

	public void acceptVisitor(IVisitor visitor)
	{
		table = acceptVisitor(table, visitor);
		columns = acceptVisitor(columns, visitor);
		values = acceptVisitor(values, visitor);
		condition = acceptVisitor(condition, visitor);
	}

	@Override
	public int hashCode()
	{
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((this.columns == null) ? 0 : this.columns.hashCode());
		result = PRIME * result + ((this.condition == null) ? 0 : this.condition.hashCode());
		result = PRIME * result + ((this.table == null) ? 0 : this.table.hashCode());
		result = PRIME * result + ((this.values == null) ? 0 : this.values.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final QueryUpdate other = (QueryUpdate)obj;
		if (this.columns == null)
		{
			if (other.columns != null) return false;
		}
		else if (!this.columns.equals(other.columns)) return false;
		if (this.condition == null)
		{
			if (other.condition != null) return false;
		}
		else if (!this.condition.equals(other.condition)) return false;
		if (this.table == null)
		{
			if (other.table != null) return false;
		}
		else if (!this.table.equals(other.table)) return false;
		if (this.values == null)
		{
			if (other.values != null) return false;
		}
		else if (!this.values.equals(other.values)) return false;
		return true;
	}

	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer(comment == null ? "" : "/* " + comment + " */ ") //
			.append("UPDATE ") //
			.append(table.toString()) //
			.append(" SET (");
		for (int i = 0; i < columns.size(); i++)
		{
			if (i > 0)
			{
				sb.append('|');
			}
			sb.append(columns.get(i).toString());
		}
		sb.append(") = ("); //$NON-NLS-1$


		for (int i = 0; i < values.size(); i++)
		{
			if (i > 0)
			{
				sb.append('|');
			}
			sb.append(BaseAbstractBaseQuery.toString(values.get(i)));
		}
		sb.append(')');
		if (condition.getConditions() != null)
		{
			sb.append(" WHERE ").append(condition); //$NON-NLS-1$
		}

		return sb.toString();
	}

	///////// serialization ////////////////

	@Override
	public Object writeReplace()
	{
		// Note: when this serialized structure changes, make sure that old data (maybe saved as serialized xml) can still be deserialized!
		return new ReplacedObject(AbstractBaseQuery.QUERY_SERIALIZE_DOMAIN, getClass(), new Object[] { table, columns, values, condition, null, comment });
	}

	public QueryUpdate(ReplacedObject s)
	{
		Object[] members = (Object[])s.getObject();
		int i = 0;
		this.table = (QueryTable)members[i++];
		this.columns = (List)members[i++];
		this.values = (List)members[i++];

		// condition used to be a HashMap<String, AndCondition> or null, now it is a AndCondition
		Object cond = members[i++];
		if (cond instanceof Map)
		{
			AndCondition c = new AndCondition();
			((Map<String, AndCondition>)cond).entrySet().forEach(entry -> c.setCondition(entry.getKey(), entry.getValue()));
			this.condition = c;
		}
		else if (cond == null)
		{
			this.condition = new AndCondition();
		}
		else
		{
			this.condition = (AndCondition)cond;
		}

		/* this.joins = (List) members[i++]; */ i++;
		if (i < members.length) // comment is a new field that was added, so it is optional now
		{
			this.comment = (String)members[i++];
		}
	}

}
