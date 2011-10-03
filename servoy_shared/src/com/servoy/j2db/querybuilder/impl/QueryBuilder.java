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
import com.servoy.j2db.query.OrCondition;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.query.QueryTable;
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
	private QuerySelect query;
	private QueryBuilderLogicalCondition where;
	private QueryTable queryTable;

	private Scriptable scriptableParent;

	/**
	 * @param tableProvider
	 * @param dataSource
	 */
	QueryBuilder(ITableProvider tableProvider, String dataSource)
	{
		super(dataSource);
		this.tableProvider = tableProvider;
	}

	public QuerySelect build()
	{
		return AbstractBaseQuery.deepClone(query);
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
		ITable tbl = tableProvider.getTable(dataSource);
		if (!(tbl instanceof Table))
		{
			throw new RepositoryException("Cannot resolve datasource '" + dataSource + "'");
		}
		return (Table)tbl;
	}

	@JSReadonlyProperty
	public QueryBuilderLogicalCondition where()
	{
		if (where == null)
		{
			AndCondition c = query.getCondition(QueryBuilder.CONDITION_WHERE);
			if (c == null)
			{
				query.setCondition(QueryBuilder.CONDITION_WHERE, c = new AndCondition());
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

	QuerySelect getQuery() throws RepositoryException
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
			return ((QueryBuilderColumn)value).getQueryColumn();
		}
		return value;
	}

}
