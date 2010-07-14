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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.servoy.j2db.util.IVisitor;
import com.servoy.j2db.util.ObjectCountVisitor;
import com.servoy.j2db.util.serialize.ReplacedObject;

/**
 * Query select statement.
 * 
 * @author rgansevles
 * 
 */
public final class QuerySelect extends AbstractBaseQuery implements ISQLSelect
{

	/**
	 * 
	 */

	private QueryTable table;
	private ArrayList<IQuerySelectValue> columns; // declare as ArrayList in stead of List -> must be sure that it is Serializable
	private boolean distinct = false;
	private boolean plain_pk_select = false;
	private HashMap<String, AndCondition> conditions = null; // Map of AndCondition objects
	private ArrayList<ISQLJoin> joins;
	private ArrayList<IQuerySort> sorts;
	private ArrayList<QueryColumn> groupBy;
	private HashMap<String, AndCondition> having = null; // Map of AndCondition objects
	private int lockMode = LOCK_MODE_NONE;

	private static final long serialVersionUID = 1L;

	public QuerySelect(QueryTable table)
	{
		this.table = table;
	}


	public ArrayList<IQuerySelectValue> getColumns()
	{
		return columns;
	}


	public void addColumn(IQuerySelectValue c)
	{
		if (c == null)
		{
			return;
		}
		if (columns == null)
		{
			columns = new ArrayList<IQuerySelectValue>();
		}
		columns.add(c);
	}

	public void setTable(QueryTable table)
	{
		this.table = table;
	}

	public void setColumns(ArrayList<IQuerySelectValue> cols)
	{
		int i;
		for (i = 0; cols != null && i < cols.size(); i++)
		{
			Object col = cols.get(i);
			if (!(col instanceof IQuerySelectValue))
			{
				throw new IllegalArgumentException("Unknown column class " + col.getClass().getName()); //$NON-NLS-1$
			}
		}
		columns = i == 0 ? null : cols;
	}

	public void setJoins(ArrayList<ISQLJoin> jns)
	{
		int i;
		for (i = 0; jns != null && i < jns.size(); i++)
		{
			Object join = jns.get(i);
			if (!(join instanceof ISQLJoin))
			{
				throw new IllegalArgumentException("Unknown join class " + join.getClass().getName()); //$NON-NLS-1$
			}
		}
		joins = i == 0 ? null : jns;
	}

	public void setSorts(ArrayList< ? extends IQuerySort> srts)
	{
		if (srts != null)
		{
			sorts = new ArrayList<IQuerySort>(srts.size());
			for (int i = 0; i < srts.size(); i++)
			{
				Object sort = srts.get(i);
				if (!(sort instanceof IQuerySort))
				{
					sorts = null;
					throw new IllegalArgumentException("Unknown sort class " + sort.getClass().getName()); //$NON-NLS-1$
				}
				sorts.add((IQuerySort)sort);
			}
		}
		else
		{
			sorts = null;
		}
	}

	public void setGroupBy(ArrayList<QueryColumn> gb)
	{
		int i;
		for (i = 0; gb != null && i < gb.size(); i++)
		{
			Object sort = gb.get(i);
			if (!(sort instanceof QueryColumn))
			{
				throw new IllegalArgumentException("Unknown group by class " + sort.getClass().getName()); //$NON-NLS-1$
			}
		}
		groupBy = i == 0 ? null : gb;
	}

	public void setCondition(String name, ISQLCondition c)
	{
		conditions = setInConditionMap(conditions, name, c);
	}

	public void addCondition(String name, ISQLCondition c)
	{
		conditions = addToConditionMap(conditions, name, c);
	}

	/**
	 * Add a condition that 'OR's with the existing condition.
	 * 
	 * @param name condition name
	 * @param c condition
	 */
	public void addConditionOr(String name, ISQLCondition c)
	{
		if (c == null)
		{
			return;
		}

		AndCondition condition = null;
		if (conditions != null)
		{
			condition = conditions.get(name);
		}
		if (condition == null)
		{
			// nothing to OR against
			addCondition(name, c);
		}
		else
		{
			List<ISQLCondition> clauses = condition.getConditions();
			if (clauses.size() == 1 && clauses.get(0) instanceof OrCondition)
			{
				((OrCondition)clauses.get(0)).addCondition(c);
			}
			else if (clauses.size() > 0 && BooleanCondition.FALSE_CONDITION.equals(clauses.get(0)))
			{
				// c OR FALSE is equivalent to c
				clauses.set(0, c);
			}
			else
			{
				// note that the values of the conditions map are always of type AndCondition
				AndCondition newCondition = new AndCondition();
				OrCondition or = new OrCondition();
				or.addCondition(condition);
				or.addCondition(c);
				newCondition.addCondition(or);
				conditions.put(name, newCondition);
			}
		}
	}

	public void setHaving(String name, ISQLCondition c)
	{
		having = setInConditionMap(having, name, c);
	}

	public void addHaving(String name, ISQLCondition c)
	{
		having = addToConditionMap(having, name, c);
	}

	public void clearJoins()
	{
		joins = null;
	}

	public void clearCondition(String name)
	{
		setCondition(name, null);
	}

	public void clearHaving(String name)
	{
		setHaving(name, null);
	}

	public void clearSorts()
	{
		sorts = null;
	}

	public void clearGroupBy()
	{
		groupBy = null;
	}

	public void addJoin(ISQLJoin join)
	{
		if (join == null)
		{
			return;
		}
		if (joins == null)
		{
			joins = new ArrayList<ISQLJoin>();
		}
		joins.add(join);
	}

	public void addSort(IQuerySort s)
	{
		if (s == null)
		{
			return;
		}
		if (sorts == null)
		{
			sorts = new ArrayList<IQuerySort>();
		}
		sorts.add(s);
	}

	public void addGroupBy(QueryColumn c)
	{
		if (c == null)
		{
			return;
		}
		if (groupBy == null)
		{
			groupBy = new ArrayList<QueryColumn>();
		}
		groupBy.add(c);
	}

	public void setDistinct(boolean d)
	{
		distinct = d;
	}

	public void setPlainPKSelect(boolean p)
	{
		plain_pk_select = p;
	}

	public void setLockMode(int lockMode)
	{
		this.lockMode = lockMode;
	}

	public AndCondition getCondition(String name)
	{
		if (conditions == null)
		{
			return null;
		}
		return conditions.get(name);
	}

	public AndCondition getConditionClone(String name)
	{
		return deepClone(getCondition(name));
	}

	public boolean isDistinct()
	{
		return distinct;
	}

	public boolean isUnique()
	{
		return distinct || (plain_pk_select && joins == null);
	}

	public ArrayList<ISQLJoin> getJoins()
	{
		return joins;
	}

	public ISQLJoin getJoin(QueryTable primaryTable, String name)
	{
		for (int i = 0; joins != null && i < joins.size(); i++)
		{
			ISQLJoin join = joins.get(i);
			if (name.equals(join.getName()) && primaryTable.equals(join.getPrimaryTable()))
			{
				return join;
			}
		}
		return null;
	}

	public ArrayList<ISQLJoin> getJoinsClone()
	{
		return deepClone(joins);
	}

	public ArrayList<IQuerySelectValue> getColumnsClone()
	{
		return deepClone(columns);
	}

	public ArrayList<QueryColumn> getGroupByClone()
	{
		return deepClone(groupBy);
	}

	public ArrayList<IQuerySort> getSortsClone()
	{
		return deepClone(sorts);
	}

	public ArrayList<IQuerySort> getSorts()
	{
		return sorts;
	}

	public ArrayList<QueryColumn> getGroupBy()
	{
		return groupBy;
	}

	public QueryTable getTable()
	{
		return table;
	}

	public int getLockMode()
	{
		return lockMode;
	}

	public String getLockModeString()
	{
		switch (lockMode)
		{
			case LOCK_MODE_NONE :
				return "NONE"; //$NON-NLS-1$
			case LOCK_MODE_UPDATE :
				return "UPDATE"; //$NON-NLS-1$

			default :
				return "!UKNOWN!"; //$NON-NLS-1$
		}
	}


	/**
	 * Is any condition set?
	 * 
	 * @return
	 */
	public boolean hasAnyCondition()
	{
		return conditions != null || having != null || joins != null;
	}

	/**
	 * One condition to rule them all. Does not include having-conditions.
	 * 
	 * @return
	 */
	public ISQLCondition getWhere()
	{
		if (conditions == null)
		{
			return null;
		}

		if (conditions.size() == 1)
		{
			return conditions.values().iterator().next();
		}

		AndCondition where = new AndCondition();
		Iterator<AndCondition> it = conditions.values().iterator();
		while (it.hasNext())
		{
			where.addCondition(it.next());
		}

		return where;
	}

	public ISQLCondition getWhereClone()
	{
		return deepClone(getWhere());
	}

	/**
	 * All having-conditions combined.
	 * 
	 * @return
	 */
	public ISQLCondition getHaving()
	{
		if (having == null)
		{
			return null;
		}

		if (having.size() == 1)
		{
			return having.values().iterator().next();
		}

		AndCondition hv = new AndCondition();
		Iterator<AndCondition> it = having.values().iterator();
		while (it.hasNext())
		{
			hv.addCondition(it.next());
		}

		return hv;
	}

	public ISQLCondition getHavingClone()
	{
		return deepClone(getHaving());
	}

	@Override
	public Object shallowClone() throws CloneNotSupportedException
	{
		return super.clone();
	}

	public QuerySelect getSelectCount(String name, boolean distinctCount)
	{
		QuerySelect selectCount = deepClone(this);

		selectCount.clearSorts();

		// remove unused joins
		selectCount.removeUnusedJoins(true);

		IQuerySelectValue agregee;
		if (selectCount.joins != null && (selectCount.distinct || distinctCount))
		{
			ArrayList<IQuerySelectValue> selectColumns = selectCount.getColumns();
			IQuerySelectValue[] cols = selectColumns.toArray(new IQuerySelectValue[selectColumns.size()]);
			if (cols.length == 1)
			{
				agregee = new QueryFunction(QueryFunction.DISTINCT, cols[0], null);
			}
			else
			{
				agregee = new QueryFunction(QueryFunction.DISTINCT, new QueryFunction(QueryFunction.CONCAT, cols, null), null);
			}
		}
		else
		{
			agregee = new QueryColumnValue(new Integer(1), null);
		}

		ArrayList<IQuerySelectValue> countCol = new ArrayList<IQuerySelectValue>();
		countCol.add(new QueryAggregate(QueryAggregate.COUNT, agregee, name));
		selectCount.setColumns(countCol);
		return selectCount;
	}

	/**
	 * Remove joins that whose foreign table is not referred to in this query.
	 */
	public void removeUnusedJoins(boolean keepInnerjoins)
	{
		boolean updated = true;
		while (joins != null && updated)
		{
			updated = false;
			int njoins = joins.size();
			for (int i = 0; i < njoins && !updated; i++)
			{
				ISQLJoin join = joins.get(i);
				if (!(join instanceof QueryJoin) || (keepInnerjoins && ((QueryJoin)join).getJoinType() == ISQLJoin.INNER_JOIN))
				{
					// count may depend on related records
					continue;
				}

				QueryTable joinTable = ((QueryJoin)join).getForeignTable();
				ObjectCountVisitor selectCounter = new ObjectCountVisitor(joinTable, true);
				ObjectCountVisitor joinCounter = new ObjectCountVisitor(joinTable, true);
				acceptVisitor(selectCounter);
				join.acceptVisitor(joinCounter);
				if (selectCounter.getCount() == joinCounter.getCount())
				{
					// the table is not referenced outside the join; it may be removed
					if (njoins == 1)
					{
						joins = null;
					}
					else
					{
						joins.remove(i);
					}
					updated = true;
				}
			}
		}
	}


	protected static HashMap<String, AndCondition> setInConditionMap(HashMap<String, AndCondition> map, String name, ISQLCondition c)
	{
		HashMap<String, AndCondition> retval = map;
		if (c == null)
		{
			if (retval != null)
			{
				retval.remove(name);
				if (retval.size() == 0)
				{
					return null;
				}
			}
		}
		else
		{
			AndCondition condition;
			if (c instanceof AndCondition)
			{
				condition = (AndCondition)c;
			}
			else
			{
				condition = new AndCondition();
				condition.addCondition(c);
			}
			if (retval == null)
			{
				retval = new HashMap<String, AndCondition>();
			}
			retval.put(name, condition);
		}
		return retval;
	}

	protected static HashMap<String, AndCondition> addToConditionMap(HashMap<String, AndCondition> map, String name, ISQLCondition c)
	{
		HashMap<String, AndCondition> retval = map;
		if (c != null)
		{
			AndCondition condition = null;
			if (retval == null)
			{
				retval = new HashMap<String, AndCondition>();
			}
			else
			{
				condition = retval.get(name);
			}

			if (condition == null)
			{
				condition = new AndCondition();
				retval.put(name, condition);
			}
			condition.addCondition(c);
		}
		return retval;
	}

	public void acceptVisitor(IVisitor visitor)
	{
		table = AbstractBaseQuery.acceptVisitor(table, visitor);
		columns = AbstractBaseQuery.acceptVisitor(columns, visitor);
		conditions = AbstractBaseQuery.acceptVisitor(conditions, visitor);
		having = AbstractBaseQuery.acceptVisitor(having, visitor);
		joins = AbstractBaseQuery.acceptVisitor(joins, visitor);
		sorts = AbstractBaseQuery.acceptVisitor(sorts, visitor);
		groupBy = AbstractBaseQuery.acceptVisitor(groupBy, visitor);
	}


	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((columns == null) ? 0 : columns.hashCode());
		result = prime * result + ((conditions == null) ? 0 : conditions.hashCode());
		result = prime * result + ((having == null) ? 0 : having.hashCode());
		result = prime * result + (distinct ? 1231 : 1237);
		result = prime * result + ((groupBy == null) ? 0 : groupBy.hashCode());
		result = prime * result + ((joins == null) ? 0 : joins.hashCode());
		result = prime * result + lockMode;
		result = prime * result + (plain_pk_select ? 1231 : 1237);
		result = prime * result + ((sorts == null) ? 0 : sorts.hashCode());
		result = prime * result + ((table == null) ? 0 : table.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final QuerySelect other = (QuerySelect)obj;
		if (columns == null)
		{
			if (other.columns != null) return false;
		}
		else if (!columns.equals(other.columns)) return false;
		if (conditions == null)
		{
			if (other.conditions != null) return false;
		}
		else if (!conditions.equals(other.conditions)) return false;
		if (having == null)
		{
			if (other.having != null) return false;
		}
		else if (!having.equals(other.having)) return false;
		if (distinct != other.distinct) return false;
		if (groupBy == null)
		{
			if (other.groupBy != null) return false;
		}
		else if (!groupBy.equals(other.groupBy)) return false;
		if (joins == null)
		{
			if (other.joins != null) return false;
		}
		else if (!joins.equals(other.joins)) return false;
		if (lockMode != other.lockMode) return false;
		if (plain_pk_select != other.plain_pk_select) return false;
		if (sorts == null)
		{
			if (other.sorts != null) return false;
		}
		else if (!sorts.equals(other.sorts)) return false;
		if (table == null)
		{
			if (other.table != null) return false;
		}
		else if (!table.equals(other.table)) return false;
		return true;
	}


	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer("SELECT"); //$NON-NLS-1$
		if (distinct)
		{
			sb.append(" DISTINCT"); //$NON-NLS-1$
		}
		for (int i = 0; columns != null && i < columns.size(); i++)
		{
			sb.append(i == 0 ? ' ' : ',');
			sb.append(columns.get(i).toString());
		}
		sb.append(" FROM ").append(table.toString()); //$NON-NLS-1$

		if (conditions != null)
		{
			Iterator<Entry<String, AndCondition>> it = conditions.entrySet().iterator();
			for (int i = 0; it.hasNext(); i++)
			{
				if (i > 0)
				{
					sb.append(" [AND] "); //$NON-NLS-1$
				}
				Map.Entry<String, AndCondition> entry = it.next();
				sb.append(' ').append(entry.getKey().toString()).append(' ').append(entry.getValue().toString());
			}
		}

		if (having != null)
		{
			sb.append(" HAVING "); //$NON-NLS-1$
			Iterator<Entry<String, AndCondition>> it = having.entrySet().iterator();
			for (int i = 0; it.hasNext(); i++)
			{
				if (i > 0)
				{
					sb.append(" [AND] "); //$NON-NLS-1$
				}
				Map.Entry<String, AndCondition> entry = it.next();
				sb.append(' ').append(entry.getKey().toString()).append(' ').append(entry.getValue().toString());
			}
		}

		for (int i = 0; joins != null && i < joins.size(); i++)
		{
			sb.append(' ').append(joins.get(i).toString());
		}
		if (sorts != null)
		{
			sb.append(" ORDER BY "); //$NON-NLS-1$
			for (int i = 0; sorts != null && i < sorts.size(); i++)
			{
				if (i > 0)
				{
					sb.append(',');
				}
				sb.append(sorts.get(i).toString());
			}

		}
		if (groupBy != null)
		{
			sb.append(" GROUP BY "); //$NON-NLS-1$
			for (int i = 0; groupBy != null && i < groupBy.size(); i++)
			{
				if (i > 0)
				{
					sb.append(',');
				}
				sb.append(groupBy.get(i).toString());
			}

		}
		if (lockMode != LOCK_MODE_NONE)
		{
			sb.append(" * LOCK "); //$NON-NLS-1$
			sb.append(getLockModeString());
			sb.append(" *"); //$NON-NLS-1$
		}

		return sb.toString();
	}

	///////// serialization ////////////////

	@Override
	public Object writeReplace()
	{
		return new ReplacedObject(QUERY_SERIALIZE_DOMAIN, getClass(), new Object[] { table, columns, new Byte((byte)(distinct ? 1 : 0 +
			(plain_pk_select ? 2 : 0) + (4 * lockMode))), conditions, having, joins, sorts, groupBy });
	}

	@SuppressWarnings("unchecked")
	public QuerySelect(ReplacedObject s)
	{
		Object[] members = (Object[])s.getObject();
		int i = 0;
		this.table = (QueryTable)members[i++];
		this.columns = (ArrayList<IQuerySelectValue>)members[i++];
		byte bits = ((Byte)members[i++]).byteValue();
		this.distinct = (bits & 1) != 0;
		this.plain_pk_select = (bits & 2) != 0;
		this.lockMode = (bits / 4);
		this.conditions = (HashMap<String, AndCondition>)members[i++];
		this.having = (HashMap<String, AndCondition>)members[i++];
		this.joins = (ArrayList<ISQLJoin>)members[i++];
		this.sorts = (ArrayList<IQuerySort>)members[i++];
		this.groupBy = (ArrayList<QueryColumn>)members[i++];
	}

}
