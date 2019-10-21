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
import java.util.HashMap;
import java.util.List;

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
	private static final String ANONYMOUS = "__ANONYMOUS_CONDITION__";

	private BaseQueryTable table;
	private List columns = new ArrayList();
	private List values = new ArrayList();
	private HashMap<String, AndCondition> conditions = null; // Map of AndCondition objects
	private List joins; // joins in update statements are not supported by hibernate.


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

	public List getColumns()
	{
		return columns;
	}

	public List getValues()
	{
		return values;
	}

	public void setCondition(ISQLCondition c)
	{
		setCondition(ANONYMOUS, c);
	}

	public void addCondition(ISQLCondition c)
	{
		addCondition(ANONYMOUS, c);
	}

	public void setCondition(String name, ISQLCondition c)
	{
		conditions = QuerySelect.setInConditionMap(conditions, name, c);
	}

	public void addCondition(String name, ISQLCondition c)
	{
		conditions = QuerySelect.addToConditionMap(conditions, name, c);
	}

//	public void setJoins(List jns)
//	{
//		int i;
//		for (i = 0; jns != null && i < jns.size(); i++)
//		{
//			Object join = jns.get(i);
//			if (!(join instanceof SQLJoin))
//			{
//				throw new IllegalArgumentException("Unknown join class "+join.getClass().getName()); //$NON-NLS-1$
//			}
//		}
//		joins = i == 0 ? null : jns;
//	}

//	public void addJoin(SQLJoin join)
//	{
//		if (joins == null)
//		{
//			joins = new ArrayList();
//		}
//		joins.add(join);
//	}

	public ISQLCondition getCondition()
	{
		return QuerySelect.getConditionMapCondition(conditions);
	}

	public ISQLCondition getConditionClone()
	{
		return AbstractBaseQuery.deepClone(getCondition());
	}

//	public List getJoins()
//	{
//		return joins;
//	}

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
		table = AbstractBaseQuery.acceptVisitor(table, visitor);
		columns = AbstractBaseQuery.acceptVisitor(columns, visitor);
		values = AbstractBaseQuery.acceptVisitor(values, visitor);
		conditions = AbstractBaseQuery.acceptVisitor(conditions, visitor);
		joins = AbstractBaseQuery.acceptVisitor(joins, visitor);
	}

	@Override
	public int hashCode()
	{
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((this.columns == null) ? 0 : this.columns.hashCode());
		result = PRIME * result + ((this.conditions == null) ? 0 : this.conditions.hashCode());
		result = PRIME * result + ((this.joins == null) ? 0 : this.joins.hashCode());
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
		if (this.conditions == null)
		{
			if (other.conditions != null) return false;
		}
		else if (!this.conditions.equals(other.conditions)) return false;
		if (this.joins == null)
		{
			if (other.joins != null) return false;
		}
		else if (!this.joins.equals(other.joins)) return false;
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
		if (conditions != null)
		{
			sb.append(" WHERE ").append(getCondition()); //$NON-NLS-1$
		}
		for (int i = 0; joins != null && i < joins.size(); i++)
		{
			sb.append(' ').append(joins.get(i).toString());
		}

		return sb.toString();
	}

	///////// serialization ////////////////

	@Override
	public Object writeReplace()
	{
		// Note: when this serialized structure changes, make sure that old data (maybe saved as serialized xml) can still be deserialized!
		return new ReplacedObject(AbstractBaseQuery.QUERY_SERIALIZE_DOMAIN, getClass(), new Object[] { table, columns, values, conditions, joins, comment });
	}

	public QueryUpdate(ReplacedObject s)
	{
		Object[] members = (Object[])s.getObject();
		int i = 0;
		this.table = (QueryTable)members[i++];
		this.columns = (List)members[i++];
		this.values = (List)members[i++];
		this.conditions = (HashMap<String, AndCondition>)members[i++];
		this.joins = (List)members[i++];
		if (i < members.length) // comment is a new field that was added, so it is optional now
		{
			this.comment = (String)members[i++];
		}
	}

}
