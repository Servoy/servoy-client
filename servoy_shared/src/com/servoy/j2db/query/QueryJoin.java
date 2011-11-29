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

import java.util.List;

import com.servoy.j2db.util.serialize.ReplacedObject;
import com.servoy.j2db.util.visitor.IVisitor;

/**
 * Join element in a query structure.
 * 
 * @author rgansevles
 * 
 */
public final class QueryJoin implements ISQLTableJoin
{
	private String name;
	private QueryTable primaryTable;
	private QueryTable foreignTable;
	private AndCondition condition;
	private int joinType;

	/**
	 * Constructor for join clause. The condition must meet the following criteria:
	 * <ul>
	 * <li>operator type = 'and' of at least 1 subcondition
	 * <li>all subconditions are of type 'compare'
	 * </ul>
	 * 
	 * @param name
	 * @param table
	 * @param condition
	 * @param joinType
	 */
	public QueryJoin(String name, QueryTable primaryTable, QueryTable foreignTable, ISQLCondition condition, int joinType)
	{
		this.name = name;
		this.primaryTable = primaryTable;
		this.foreignTable = foreignTable;
		this.joinType = joinType;
		setCondition(condition);
	}

	public void setCondition(ISQLCondition c)
	{
		if (c instanceof CompareCondition)
		{
			condition = new AndCondition();
			condition.addCondition(c);
		}
		else if (c instanceof AndCondition)
		{
//			 subconditions of type COMPARE
			List conditions = ((AndCondition)c).getConditions();
			for (int i = 0; i < conditions.size(); i++)
			{
				if (!(conditions.get(i) instanceof CompareCondition))
				{
					throw new IllegalArgumentException(
						"Expecting compare-condition in join on table " + foreignTable.getName() + ", receiving " + conditions.get(i).getClass().getName()); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			condition = (AndCondition)c;
		}
		else
		{
			throw new IllegalArgumentException(
				"Expecting compare-condition in join on table " + foreignTable.getName() + ", receiving " + c.getClass().getName()); //$NON-NLS-1$ //$NON-NLS-2$
		}

	}

	public String getName()
	{
		return name;
	}

	public AndCondition getCondition()
	{
		return condition;
	}

	public int getJoinType()
	{
		return joinType;
	}

	public void setJoinType(int joinType)
	{
		this.joinType = joinType;
	}

	public boolean hasInnerJoin()
	{
		return joinType == INNER_JOIN;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.query.ISQLJoin#getPrimaryTable()
	 */
	public QueryTable getPrimaryTable()
	{
		return primaryTable;
	}

	public QueryTable getForeignTable()
	{
		return foreignTable;
	}

	/**
	 * Invert the direction of this join.
	 */
	public void invert(String newName)
	{
		// switch the tables
		QueryTable tmp = primaryTable;
		primaryTable = foreignTable;
		foreignTable = tmp;

		// the primary keys must be swapped around
		if (condition != null)
		{
			List conditions = condition.getConditions();
			for (int i = 0; i < conditions.size(); i++)
			{
				CompareCondition cond = (CompareCondition)conditions.get(i);
				if ((cond.getOperator() & ISQLCondition.OPERATOR_MASK) == ISQLCondition.EQUALS_OPERATOR && cond.getOperand1() instanceof QueryColumn &&
					cond.getOperand2() instanceof QueryColumn)
				{
					conditions.set(i, new CompareCondition(cond.getOperator(), (QueryColumn)cond.getOperand2(), cond.getOperand1()));
				}
			}
		}

		// join type: left outer join becomes right outer join and vv. other joins stay the same
		if (joinType == LEFT_OUTER_JOIN) joinType = RIGHT_OUTER_JOIN;
		else if (joinType == RIGHT_OUTER_JOIN) joinType = LEFT_OUTER_JOIN;

		this.name = newName;
	}


	public Object shallowClone() throws CloneNotSupportedException
	{
		return super.clone();
	}

	public void acceptVisitor(IVisitor visitor)
	{
		primaryTable = AbstractBaseQuery.acceptVisitor(primaryTable, visitor);
		foreignTable = AbstractBaseQuery.acceptVisitor(foreignTable, visitor);
		condition = AbstractBaseQuery.acceptVisitor(condition, visitor);
	}

	@Override
	public int hashCode()
	{
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((this.condition == null) ? 0 : this.condition.hashCode());
		result = PRIME * result + ((this.foreignTable == null) ? 0 : this.foreignTable.hashCode());
		result = PRIME * result + this.joinType;
		result = PRIME * result + ((this.name == null) ? 0 : this.name.hashCode());
		result = PRIME * result + ((this.primaryTable == null) ? 0 : this.primaryTable.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final QueryJoin other = (QueryJoin)obj;
		if (this.condition == null)
		{
			if (other.condition != null) return false;
		}
		else if (!this.condition.equals(other.condition)) return false;
		if (this.foreignTable == null)
		{
			if (other.foreignTable != null) return false;
		}
		else if (!this.foreignTable.equals(other.foreignTable)) return false;
		if (this.joinType != other.joinType) return false;
		if (this.name == null)
		{
			if (other.name != null) return false;
		}
		else if (!this.name.equals(other.name)) return false;
		if (this.primaryTable == null)
		{
			if (other.primaryTable != null) return false;
		}
		else if (!this.primaryTable.equals(other.primaryTable)) return false;
		return true;
	}

	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer(JOIN_TYPES_NAMES[joinType].toUpperCase());
		sb.append(' ').append(name);
		sb.append(" FROM ").append(primaryTable.toString()); //$NON-NLS-1$
		sb.append(" TO ").append(foreignTable.toString()); //$NON-NLS-1$
		if (condition != null)
		{
			sb.append(" ON ").append(condition.toString()); //$NON-NLS-1$
		}
		return sb.toString();
	}

	///////// serialization ////////////////


	public Object writeReplace()
	{
		return new ReplacedObject(AbstractBaseQuery.QUERY_SERIALIZE_DOMAIN, getClass(),
			new Object[] { name, primaryTable, foreignTable, condition, Integer.valueOf(joinType) });
	}

	public QueryJoin(ReplacedObject s)
	{
		Object[] members = (Object[])s.getObject();
		int i = 0;
		name = (String)members[i++];
		primaryTable = (QueryTable)members[i++];
		foreignTable = (QueryTable)members[i++];
		condition = (AndCondition)members[i++];
		joinType = ((Integer)members[i++]).intValue();
	}


}
