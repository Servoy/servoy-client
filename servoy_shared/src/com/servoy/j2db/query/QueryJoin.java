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

import static com.servoy.base.query.IQueryConstants.INNER_JOIN;
import static com.servoy.base.query.IQueryConstants.LEFT_OUTER_JOIN;
import static com.servoy.base.query.IQueryConstants.RIGHT_OUTER_JOIN;

import java.util.List;

import com.servoy.base.query.BaseQueryTable;
import com.servoy.base.query.IBaseSQLCondition;
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
	private static final int PERMANENT_MASK = 1 << 16;

	private String relationName;
	private String alias;
	private BaseQueryTable primaryTable;
	private ITableReference foreignTableReference;
	private AndCondition condition;
	private int joinType;
	private final boolean permanent;

	private transient Object origin; // origin, transient, only used in the client

	/**
	 * Constructor for join clause. The condition must meet the following criteria:
	 * <ul>
	 * <li>operator type = 'and' of at least 1 subcondition
	 * <li>all subconditions are of type 'compare'
	 * </ul>
	 *
	 * @param name
	 * @param primaryTable
	 * @param foreignTable
	 * @param condition
	 * @param joinType
	 * @param permanent
	 */
	public QueryJoin(String name, BaseQueryTable primaryTable, BaseQueryTable foreignTable, ISQLCondition condition, int joinType, boolean permanent)
	{
		this(name, primaryTable, new TableExpression(foreignTable), condition, joinType, permanent, name);
	}

	/**
	 * Constructor for join clause. The condition must meet the following criteria:
	 * <ul>
	 * <li>operator type = 'and' of at least 1 subcondition
	 * <li>all subconditions are of type 'compare'
	 * </ul>
	 *
	 * @param name
	 * @param primaryTable
	 * @param foreignTable
	 * @param condition
	 * @param joinType
	 * @param permanent
	 * @param alias
	 */
	public QueryJoin(String name, BaseQueryTable primaryTable, BaseQueryTable foreignTable, ISQLCondition condition, int joinType, boolean permanent,
		String alias)
	{
		this(name, primaryTable, new TableExpression(foreignTable), condition, joinType, permanent, alias);
	}

	/**
	 * Constructor for join clause. The condition must meet the following criteria:
	 * <ul>
	 * <li>operator type = 'and' of at least 1 subcondition
	 * <li>all subconditions are of type 'compare'
	 * </ul>
	 *
	 * @param relationName
	 * @param primaryTable
	 * @param foreignTableReference
	 * @param condition
	 * @param joinType
	 * @param permanent
	 * @param alias
	 */
	public QueryJoin(String relationName, BaseQueryTable primaryTable, ITableReference foreignTableReference, ISQLCondition condition, int joinType,
		boolean permanent, String alias)
	{
		this.relationName = relationName;
		this.primaryTable = primaryTable;
		this.foreignTableReference = foreignTableReference;
		this.joinType = joinType;
		this.permanent = permanent;
		this.condition = getAndCondition(condition, foreignTableReference);
		this.alias = alias;
	}

	private static AndCondition getAndCondition(ISQLCondition c, ITableReference foreignTableReference)
	{
		AndCondition condition;
		if (c instanceof CompareCondition)
		{
			condition = new AndCondition();
			condition.addCondition(c);
		}
		else if (c instanceof AndCondition)
		{
//			 subconditions of type COMPARE
			for (ISQLCondition condition2 : ((AndCondition)c).getAllConditions())
			{
				if (!(condition2 instanceof CompareCondition))
				{
					throw new IllegalArgumentException(
						"Expecting compare-condition in join on table " + foreignTableReference + ", receiving " + condition2.getClass().getName()); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			condition = (AndCondition)c;
		}
		else
		{
			throw new IllegalArgumentException(
				"Expecting compare-condition in join on table " + foreignTableReference + ", receiving " + c.getClass().getName()); //$NON-NLS-1$ //$NON-NLS-2$
		}

		return condition;
	}

	@Override
	public String getName()
	{
		return alias == null ? relationName : alias;
	}

	public String getRelationName()
	{
		return relationName;
	}

	@Override
	public String getAlias()
	{
		return alias;
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

	@Override
	public boolean isPermanent()
	{
		return permanent;
	}

	@Override
	public void setOrigin(Object origin)
	{
		this.origin = origin;
	}

	@Override
	public Object getOrigin()
	{
		return origin;
	}


	public BaseQueryTable getPrimaryTable()
	{
		return primaryTable;
	}

	@Override
	public ITableReference getForeignTableReference()
	{
		return foreignTableReference;
	}

	/**
	 * Invert the direction of this join.
	 */
	public void invert(String newName)
	{
		// switch the tables
		BaseQueryTable tmp = primaryTable;
		primaryTable = ((TableExpression)foreignTableReference).getTable();
		foreignTableReference = new TableExpression(tmp);

		// the primary keys must be swapped around
		if (condition != null)
		{
			for (List<ISQLCondition> conditions : condition.getConditions().values())
			{
				for (int i = 0; i < conditions.size(); i++)
				{
					if (conditions.get(i) instanceof CompareCondition)
					{
						CompareCondition cond = (CompareCondition)conditions.get(i);
						if ((cond.getOperator() & IBaseSQLCondition.OPERATOR_MASK) == IBaseSQLCondition.EQUALS_OPERATOR &&
							cond.getOperand1() instanceof QueryColumn &&
							cond.getOperand2() instanceof QueryColumn)
						{
							conditions.set(i, new CompareCondition(cond.getOperator(), (QueryColumn)cond.getOperand2(), cond.getOperand1()));
						}
					}
				}
			}
		}

		// join type: left outer join becomes right outer join and vv. other joins stay the same
		if (joinType == LEFT_OUTER_JOIN) joinType = RIGHT_OUTER_JOIN;
		else if (joinType == RIGHT_OUTER_JOIN) joinType = LEFT_OUTER_JOIN;

		this.alias = newName;
		this.relationName = null;
	}


	@Override
	public Object shallowClone() throws CloneNotSupportedException
	{
		return super.clone();
	}

	public void acceptVisitor(IVisitor visitor)
	{
		primaryTable = AbstractBaseQuery.acceptVisitor(primaryTable, visitor);
		foreignTableReference = AbstractBaseQuery.acceptVisitor(foreignTableReference, visitor);
		condition = AbstractBaseQuery.acceptVisitor(condition, visitor);
	}

	@Override
	public int hashCode()
	{
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((alias == null) ? 0 : alias.hashCode());
		result = PRIME * result + ((condition == null) ? 0 : condition.hashCode());
		result = PRIME * result + ((foreignTableReference == null) ? 0 : foreignTableReference.hashCode());
		result = PRIME * result + joinType;
		result = PRIME * result + (permanent ? 1231 : 1237);
		result = PRIME * result + ((primaryTable == null) ? 0 : primaryTable.hashCode());
		result = PRIME * result + ((relationName == null) ? 0 : relationName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final QueryJoin other = (QueryJoin)obj;
		if (alias == null)
		{
			if (other.alias != null) return false;
		}
		else if (!alias.equals(other.alias)) return false;
		if (condition == null)
		{
			if (other.condition != null) return false;
		}
		else if (!condition.equals(other.condition)) return false;
		if (foreignTableReference == null)
		{
			if (other.foreignTableReference != null) return false;
		}
		else if (!foreignTableReference.equals(other.foreignTableReference)) return false;
		if (joinType != other.joinType) return false;
		if (permanent != other.permanent) return false;
		if (primaryTable == null)
		{
			if (other.primaryTable != null) return false;
		}
		else if (!primaryTable.equals(other.primaryTable)) return false;
		if (relationName == null)
		{
			if (other.relationName != null) return false;
		}
		else if (!relationName.equals(other.relationName)) return false;
		return true;
	}

	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer(JOIN_TYPES_NAMES[joinType].toUpperCase()).append(' ');
		if (alias == null)
		{
			sb.append(relationName);
		}
		else
		{
			sb.append(alias);
			if (relationName != null && !relationName.equals(alias))
			{
				sb.append(" (").append(relationName).append(')');
			}
		}
		if (permanent)
		{
			sb.append('!');
		}
		sb.append(" FROM ").append(primaryTable.toString()); //$NON-NLS-1$
		sb.append(" TO ").append(foreignTableReference.toString()); //$NON-NLS-1$
		if (condition != null)
		{
			sb.append(" ON ").append(condition.toString()); //$NON-NLS-1$
		}
		return sb.toString();
	}

	///////// serialization ////////////////


	public Object writeReplace()
	{
		int joinTypeAndPermant = joinType | (permanent ? PERMANENT_MASK : 0);
		// Note: when this serialized structure changes, make sure that old data (maybe saved as serialized xml) can still be deserialized!
		return new ReplacedObject(AbstractBaseQuery.QUERY_SERIALIZE_DOMAIN, getClass(),
			new Object[] { relationName, primaryTable, foreignTableReference, condition, Integer.valueOf(joinTypeAndPermant), alias });
	}

	public QueryJoin(ReplacedObject s)
	{
		Object[] members = (Object[])s.getObject();
		int i = 0;
		relationName = (String)members[i++];
		primaryTable = (QueryTable)members[i++];
		Object foreignTableOrReference = members[i++];
		if (foreignTableOrReference instanceof QueryTable)
		{
			// legacy
			foreignTableReference = new TableExpression((QueryTable)foreignTableOrReference);
		}
		else
		{
			foreignTableReference = (ITableReference)foreignTableOrReference;
		}
		condition = (AndCondition)members[i++];
		int joinTypeAndPermant = ((Integer)members[i++]).intValue();
		joinType = joinTypeAndPermant & ~PERMANENT_MASK;
		permanent = (joinTypeAndPermant & PERMANENT_MASK) != 0;
		if (i < members.length) // alias is a new field that was added, so it is optional now
		{
			this.alias = (String)members[i++];
		}
	}

}
