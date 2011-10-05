/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

package com.servoy.j2db.querybuilder.impl;

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.ITableProvider;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.query.AbstractBaseQuery;
import com.servoy.j2db.query.AndCondition;
import com.servoy.j2db.query.ExistsCondition;
import com.servoy.j2db.query.ISQLSelect;
import com.servoy.j2db.query.OrCondition;
import com.servoy.j2db.query.QueryColumnValue;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.query.QueryTable;
import com.servoy.j2db.querybuilder.IQueryBuilder;
import com.servoy.j2db.querybuilder.IQueryBuilderCondition;
import com.servoy.j2db.querybuilder.IQueryBuilderLogicalCondition;
import com.servoy.j2db.querybuilder.internal.IQueryBuilderConditionInternal;
import com.servoy.j2db.querybuilder.internal.IQueryBuilderInternal;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;

/**
 * @author rgansevles
 *
 */
public class QueryBuilder extends QueryBuilderTableClause implements IQueryBuilderInternal
{
	public static final String CONDITION_WHERE = "WHERE";

	private final ITableProvider tableProvider;

	private QueryBuilderResult result;
	private QueryBuilderSorts sort;
	private QueryBuilderGroupBy groupBy;
	private QuerySelect query;
	private QueryBuilderLogicalCondition where;
	private QueryTable queryTable;

	private Scriptable scriptableParent;

	QueryBuilder(ITableProvider tableProvider, String dataSource, String alias)
	{
		super(dataSource, alias);
		this.tableProvider = tableProvider;
	}

	/**
	 * @param querySelect
	 */
	QueryBuilder(QuerySelect querySelect)
	{
		this(null, null, null);
		this.query = querySelect;
	}

	public QuerySelect build() throws RepositoryException
	{
		return AbstractBaseQuery.deepClone(getQuery());
	}

	/**
	 * @param scriptableParent
	 */
	void setScriptableParent(Scriptable scriptableParent)
	{
		this.scriptableParent = scriptableParent;
	}

	/**
	 * @return the scriptableParent
	 */
	Scriptable getScriptableParent()
	{
		return scriptableParent;
	}

	Table getTable(String dataSource) throws RepositoryException
	{
		if (dataSource == null)
		{
			throw new RepositoryException("Cannot access table in query without dataSource");
		}
		ITable tbl = tableProvider.getTable(dataSource);
		if (!(tbl instanceof Table))
		{
			throw new RepositoryException("Cannot resolve datasource '" + dataSource + "'");
		}
		return (Table)tbl;
	}

	@JSReadonlyProperty
	public QueryBuilderLogicalCondition where() throws RepositoryException
	{
		if (where == null)
		{
			AndCondition c = getQuery().getCondition(QueryBuilder.CONDITION_WHERE);
			if (c == null)
			{
				getQuery().setCondition(QueryBuilder.CONDITION_WHERE, c = new AndCondition());
			}
			where = new QueryBuilderLogicalCondition(this, this, c);
		}
		return where;
	}

	@JSReadonlyProperty
	public QueryBuilderResult result()
	{
		if (result == null)
		{
			result = new QueryBuilderResult(this);
		}
		return result;
	}

	@JSReadonlyProperty
	public QueryBuilderSorts sort()
	{
		if (sort == null)
		{
			sort = new QueryBuilderSorts(this);
		}
		return sort;
	}

	@JSReadonlyProperty
	public QueryBuilderGroupBy groupBy()
	{
		if (groupBy == null)
		{
			groupBy = new QueryBuilderGroupBy(this);
		}
		return groupBy;
	}

	@JSReadonlyProperty
	public QueryBuilderLogicalCondition or()
	{
		return new QueryBuilderLogicalCondition(getRoot(), this, new OrCondition());
	}

	@JSFunction
	public QueryBuilderCondition not(IQueryBuilderLogicalCondition cond)
	{
		return new QueryBuilderCondition(getRoot(), cond.getParent(), ((IQueryBuilderConditionInternal)cond).getQueryCondition().negate());
	}

	@JSFunction
	public QueryBuilderCondition not(IQueryBuilderCondition cond)
	{
		return new QueryBuilderCondition(this, ((IQueryBuilderConditionInternal)cond).getParent(),
			((IQueryBuilderConditionInternal)cond).getQueryCondition().negate());
	}

	@JSFunction
	public QueryBuilderCondition exists(IQueryBuilder q) throws RepositoryException
	{
		ISQLSelect select = ((IQueryBuilderInternal)q).build();
		if (select instanceof QuerySelect && ((QuerySelect)select).getColumns() == null)
		{
			// no columns, add 'select 1'
			((QuerySelect)select).addColumn(new QueryColumnValue(Integer.valueOf(1), null, true));
		}
		return new QueryBuilderCondition(this, this, new ExistsCondition(select, true));
	}

	public QuerySelect getQuery() throws RepositoryException
	{
		if (query == null)
		{
			query = new QuerySelect(getQueryTable());
		}
		return query;
	}

	@Override
	QueryTable getQueryTable() throws RepositoryException
	{
		if (queryTable == null)
		{
			queryTable = new QueryTable(getTable().getSQLName(), getTable().getCatalog(), getTable().getSchema());
		}
		return queryTable;
	}

	static Object createOperand(Object value)
	{
		if (value instanceof QueryBuilderColumn)
		{
			return ((QueryBuilderColumn)value).getQuerySelectValue();
		}
		return value;
	}

}
