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

import static com.servoy.j2db.query.AndCondition.and;
import static com.servoy.j2db.query.OrCondition.or;
import static com.servoy.j2db.util.Utils.stream;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.servoy.base.query.BaseQueryTable;
import com.servoy.j2db.query.QueryFunction.QueryFunctionType;
import com.servoy.j2db.util.TypePredicate;
import com.servoy.j2db.util.serialize.ReplacedObject;
import com.servoy.j2db.util.visitor.IVisitor;
import com.servoy.j2db.util.visitor.ObjectCountVisitor;

/**
 * Query select statement.
 * @author rgansevles
 */
public final class QuerySelect extends AbstractBaseQuery implements ISQLSelect
{
	private BaseQueryTable table;
	private ArrayList<IQuerySelectValue> columns; // declare as ArrayList in stead of List -> must be sure that it is Serializable
	private boolean distinct = false;
	private boolean plain_pk_select = false;
	private AndCondition condition = new AndCondition();
	private ArrayList<ISQLJoin> joins;
	private ArrayList<IQuerySort> sorts;
	private ArrayList<IQuerySelectValue> groupBy;
	private AndCondition having = null;
	private int lockMode = LOCK_MODE_NONE;

	private static final long serialVersionUID = 1L;

	public QuerySelect(BaseQueryTable table)
	{
		this.table = table;
	}

	@Override
	public boolean supportsLimit()
	{
		return true;
	}

	public ArrayList<IQuerySelectValue> getColumns()
	{
		return columns;
	}

	public IQuerySelectValue getColumn(String aliasOrName)
	{
		return stream(getColumns())
			.filter(col -> col.getAliasOrName().equals(aliasOrName))
			.findFirst()
			.orElse(null);
	}

	public String[] getColumnNames()
	{
		return stream(getColumns()).map(IQuerySelectValue::getAliasOrName).toArray(String[]::new);
	}

	public void addColumn(IQuerySelectValue c)
	{
		if (c == null)
		{
			return;
		}
		if (columns == null)
		{
			columns = new ArrayList<>();
		}
		columns.add(c);
	}

	public void removeColumn(IQuerySelectValue c)
	{
		if (c != null && columns != null && columns.remove(c) && columns.isEmpty())
		{
			columns = null;
		}
	}

	public void setTable(BaseQueryTable table)
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
		if (srts != null && srts.size() > 0)
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

	public void setGroupBy(ArrayList< ? extends IQuerySelectValue> gb)
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
		groupBy = i == 0 ? null : new ArrayList<IQuerySelectValue>(gb);
	}

	public void setCondition(String name, ISQLCondition c)
	{
		condition.setCondition(name, c);
	}

	public void addCondition(String name, ISQLCondition c)
	{
		condition.addCondition(name, c);
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

		List<ISQLCondition> clauses = condition.getConditions(name);
		if (clauses == null || clauses.isEmpty())
		{
			// nothing to OR against
			setCondition(name, c);
		}
		else
		{
			if (clauses.size() == 1 && clauses.get(0) instanceof OrCondition)
			{
				// add to existing OR
				((OrCondition)clauses.get(0)).addCondition(c);
			}
			else if (clauses.size() == 1 && BooleanCondition.FALSE_CONDITION.equals(clauses.get(0)))
			{
				// c OR FALSE is equivalent to c
				setCondition(name, c);
			}
			else
			{
				condition.setCondition(name, or(c, and(clauses)));
			}
		}
	}

	public void clearCondition()
	{
		condition.clear();
	}

	public void setHaving(ISQLCondition c)
	{
		if (c == null || c instanceof AndCondition)
		{
			having = (AndCondition)c;
		}
		else
		{
			having = new AndCondition();
			having.addCondition(c);
		}
	}

	public void addHaving(ISQLCondition c)
	{
		if (having == null)
		{
			setHaving(c);
		}
		else
		{
			having.addCondition(c);
		}
	}

	public void clearJoins()
	{
		joins = null;
	}

	public void clearCondition(String name)
	{
		setCondition(name, null);
	}

	public void clearHaving()
	{
		having = null;
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

	public void removeJoin(ISQLJoin join)
	{
		if (join != null && joins != null && joins.remove(join) && joins.isEmpty())
		{
			joins = null;
		}
	}

	public void removeJoinsWithOrigin(Object origin)
	{
		if (origin != null && joins != null)
		{
			Iterator<ISQLJoin> iterator = joins.iterator();
			while (iterator.hasNext())
			{
				ISQLJoin join = iterator.next();
				if (origin == join.getOrigin())
				{
					iterator.remove();
				}
			}
			if (joins.isEmpty())
			{
				joins = null;
			}
		}
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
		if (!sorts.contains(s)) sorts.add(s);
	}

	public void addGroupBy(IQuerySelectValue c)
	{
		if (c == null)
		{
			return;
		}
		if (groupBy == null)
		{
			groupBy = new ArrayList<IQuerySelectValue>();
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

	public ISQLCondition getCondition(String name)
	{
		return and(getConditions(name));
	}

	public List<ISQLCondition> getConditions(String name)
	{
		return condition.getConditions(name);
	}

	public String[] getConditionNames()
	{
		return condition.getConditionNames();
	}

	public ISQLCondition getConditionClone(String name)
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

	public ISQLJoin getJoin(BaseQueryTable primaryTable, String relationName)
	{
		return joins.stream()
			.filter(join -> relationName.equals(join.getRelationName()) && primaryTable.equals(join.getPrimaryTable()))
			.findAny().orElse(null);
	}

	public ArrayList<ISQLJoin> getJoinsClone()
	{
		return deepClone(joins);
	}

	public ArrayList<IQuerySelectValue> getColumnsClone()
	{
		return deepClone(columns);
	}

	public ArrayList<IQuerySelectValue> getGroupByClone()
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

	public ArrayList<IQuerySelectValue> getGroupBy()
	{
		return groupBy;
	}

	public BaseQueryTable getTable()
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
			case LOCK_MODE_LOCK_BLOCK :
				return "BLOCK"; //$NON-NLS-1$
			case LOCK_MODE_LOCK_NOWAIT :
				return "NOWAIT"; //$NON-NLS-1$

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
		return !condition.isEmpty() || having != null || joins != null;
	}

	/**
	 * The where clause, never null, may be empty
	 */
	public AndCondition getCondition()
	{
		return condition;
	}

	/**
	 * One condition to rule them all. Does not include having-conditions.
	 */
	public AndCondition getWhere()
	{
		if (condition.isEmpty())
		{
			return null;
		}

		return new AndCondition(condition.getAllConditions());
	}

	public AndCondition getWhereClone()
	{
		return deepClone(getWhere());
	}

	/**
	 * All having-conditions combined.
	 *
	 * @return
	 */
	public AndCondition getHaving()
	{
		return having;
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
		selectCount.removeUnusedJoins(true, true);

		IQuerySelectValue agregee;
		int aggregateQuantifier;
		if (selectCount.joins != null && (selectCount.distinct || distinctCount))
		{
			aggregateQuantifier = QueryAggregate.DISTINCT;
			ArrayList<IQuerySelectValue> selectColumns = selectCount.getColumns();
			IQuerySelectValue[] cols = selectColumns.toArray(new IQuerySelectValue[selectColumns.size()]);
			if (cols.length == 1)
			{
				agregee = cols[0];
			}
			else
			{
				agregee = new QueryFunction(QueryFunctionType.concat, cols, null);
			}
		}
		else
		{
			aggregateQuantifier = QueryAggregate.ALL;
			agregee = new QueryColumnValue("*", null, true);
		}

		selectCount.setColumns(
			new ArrayList<IQuerySelectValue>(asList(new QueryAggregate(QueryAggregate.COUNT, aggregateQuantifier, agregee, name, null, false))));
		return selectCount;
	}

	/**
	 * Remove joins that whose foreign table is not referred to in this query.
	 */
	public void removeUnusedJoins(boolean keepInnerjoins, boolean keepPermanent)
	{
		boolean updated = true;
		while (joins != null && updated)
		{
			updated = false;
			int njoins = joins.size();
			for (int i = 0; i < njoins && !updated; i++)
			{
				ISQLJoin join = joins.get(i);
				if (!(join instanceof ISQLTableJoin) ||
					(keepInnerjoins && ((ISQLTableJoin)join).hasInnerJoin()) ||
					(keepPermanent && ((ISQLTableJoin)join).isPermanent()))
				{
					// count may depend on related records or is marked as permanent
					continue;
				}

				ISQLTableJoin tableJoin = (ISQLTableJoin)join;

				if (!(tableJoin.getForeignTableReference() instanceof TableExpression))
				{
					continue; // derived table
				}

				if (!isJoinTableUsed(tableJoin))
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

	/**
	 * Check whether the join is needed because its table is used somewhere in the query.
	 */
	public boolean isJoinTableUsed(ISQLTableJoin tableJoin)
	{
		BaseQueryTable joinTable = tableJoin.getForeignTable();
		ObjectCountVisitor selectCounter = new ObjectCountVisitor(joinTable, true);
		ObjectCountVisitor joinCounter = new ObjectCountVisitor(joinTable, true);
		acceptVisitor(selectCounter);
		tableJoin.acceptVisitor(joinCounter);
		return selectCounter.getCount() > joinCounter.getCount();
	}

	/**
	 * Get the real QueryColumn for a column in the query.
	 * <p>
	 * For a column from a table it is the column itself
	 * <br>
	 * For a column that refers to a derived table this function returns the actual column from the derived table.
	 * For example, <code>table_1.fld</code> is returned as real column for <code>col</code> in
	 * <pre>
	 * SELECT col
	   FROM
	      (SELECT fld
	       FROM table_1) derived_table_name;
	 * </pre>
	 *
	 * @param column
	 */
	public Optional<QueryColumn> getRealColumn(IQuerySelectValue column)
	{
		if (column == null)
		{
			return Optional.empty();
		}
		QueryColumn qColumn = column.getColumn();
		if (qColumn == null)
		{
			return Optional.empty();
		}
		BaseQueryTable qTable = qColumn.getTable();
		if (qTable.getDataSource() != null)
		{
			// column on regular table
			return Optional.of(qColumn);
		}
		// find real column in derived table
		return AbstractBaseQuery.<DerivedTable> searchOne(this, new TypePredicate<>(DerivedTable.class, derivedTable -> derivedTable.getTable() == qTable))
			.map(DerivedTable::getQuery)
			.map(QuerySelect::getColumns)
			.flatMap(derivedColumns -> derivedColumns.stream()
				.filter(Objects::nonNull)
				.filter(
					dtcol -> qColumn.getName().equals(dtcol.getAlias()) || (dtcol.getColumn() != null && qColumn.getName().equals(dtcol.getColumn().getName())))
				.map(IQuerySelectValue::getColumn)
				.filter(Objects::nonNull)
				.findAny())
			.flatMap(this::getRealColumn); // recursive for nested derived tables
	}


	public void acceptVisitor(IVisitor visitor)
	{
		table = AbstractBaseQuery.acceptVisitor(table, visitor);
		columns = AbstractBaseQuery.acceptVisitor(columns, visitor);
		condition = AbstractBaseQuery.acceptVisitor(condition, visitor);
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
		result = prime * result + ((condition == null) ? 0 : condition.hashCode());
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
		if (condition == null)
		{
			if (other.condition != null) return false;
		}
		else if (!condition.equals(other.condition)) return false;
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
		StringBuffer sb = new StringBuffer(comment == null ? "" : "/* " + comment + " */ ") //
			.append("SELECT");
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
		sb.append(" WHERE ").append(condition.toString()); //$NON-NLS-1$
		if (having != null)
		{
			sb.append(" HAVING ").append(having.toString()); //$NON-NLS-1$
		}
		stream(joins).forEach(join -> sb.append(' ').append(join.toString()));
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
		// Note: when this serialized structure changes, make sure that old data (maybe saved as serialized xml) can still be deserialized!
		return new ReplacedObject(QUERY_SERIALIZE_DOMAIN, getClass(), new Object[] { table, columns, new Byte(
			(byte)(distinct ? 1 : 0 + (plain_pk_select ? 2 : 0) + (4 * lockMode))), condition, having, joins, sorts, groupBy, comment });
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

		Object hv = members[i++];
		if (hv instanceof Map && ((Map)hv).size() > 0)
		{
			// old format Having was map, but only actually 1 key was used
			this.having = (AndCondition)((Map)hv).values().iterator().next();
		}
		else
		{
			this.having = (AndCondition)hv;
		}
		this.joins = (ArrayList<ISQLJoin>)members[i++];
		this.sorts = (ArrayList<IQuerySort>)members[i++];
		this.groupBy = (ArrayList<IQuerySelectValue>)members[i++];
		if (i < members.length) // comment is a new field that was added, so it is optional now
		{
			this.comment = (String)members[i++];
		}
	}
}
