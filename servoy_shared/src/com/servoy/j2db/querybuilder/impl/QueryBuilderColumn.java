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

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.query.CompareCondition;
import com.servoy.j2db.query.IQuerySelectValue;
import com.servoy.j2db.query.ISQLCondition;
import com.servoy.j2db.query.QueryAggregate;
import com.servoy.j2db.query.QueryColumn;
import com.servoy.j2db.query.SetCondition;
import com.servoy.j2db.querybuilder.IQueryBuilder;
import com.servoy.j2db.querybuilder.internal.IQueryBuilderColumnInternal;
import com.servoy.j2db.querybuilder.internal.IQueryBuilderInternal;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;

/**
 * @author rgansevles
 *
 */
public class QueryBuilderColumn extends AbstractQueryBuilderPart<QueryBuilderTableClause> implements IQueryBuilderColumnInternal
{
	private final QueryColumn queryColumn;
	private final boolean negate;

	QueryBuilderColumn(QueryBuilder root, QueryBuilderTableClause queryBuilderTableClause, QueryColumn queryColumn)
	{
		this(root, queryBuilderTableClause, queryColumn, false);
	}

	QueryBuilderColumn(QueryBuilder root, QueryBuilderTableClause parent, QueryColumn queryColumn, boolean negate)
	{
		super(root, parent);
		this.queryColumn = queryColumn;
		this.negate = negate;
	}

	QueryBuilderCondition createCompareCondition(int operator, Object value)
	{
		return createCondition(new CompareCondition(operator, this.getQuerySelectValue(), QueryBuilder.createOperand(value)));
	}

	QueryBuilderCondition createCondition(ISQLCondition queryCondition)
	{
		return new QueryBuilderCondition(getRoot(), getParent(), negate ? queryCondition.negate() : queryCondition);
	}


	public IQuerySelectValue getQuerySelectValue()
	{
		return queryColumn;
	}

	@JSFunction
	public QueryBuilderCondition gt(Object value)
	{
		return createCompareCondition(ISQLCondition.GT_OPERATOR, value);
	}

	@JSFunction
	public QueryBuilderCondition lt(Object value)
	{
		return createCompareCondition(ISQLCondition.LT_OPERATOR, value);
	}

	@JSFunction
	public QueryBuilderCondition ge(Object value)
	{
		return createCompareCondition(ISQLCondition.GTE_OPERATOR, value);
	}

	@JSFunction
	public QueryBuilderCondition le(Object value)
	{
		return createCompareCondition(ISQLCondition.LTE_OPERATOR, value);
	}

	@JSFunction
	public QueryBuilderCondition between(Object value1, Object value2)
	{
		return createCompareCondition(ISQLCondition.BETWEEN_OPERATOR, new Object[] { value1, value2 });
	}

	@JSFunction(value = "isin")
	public QueryBuilderCondition in(IQueryBuilder query) throws RepositoryException
	{
		return createCondition(new SetCondition(ISQLCondition.EQUALS_OPERATOR, new IQuerySelectValue[] { getQuerySelectValue() },
			((IQueryBuilderInternal)query).build(), true));
	}

	@JSReadonlyProperty
	public QueryBuilderCondition isNull()
	{
		return eq(null);
	}

	@JSFunction
	public QueryBuilderCondition eq(Object value)
	{
		return createCompareCondition(ISQLCondition.EQUALS_OPERATOR, value);
	}

	@JSFunction
	public QueryBuilderCondition like(String pattern)
	{
		return createCompareCondition(ISQLCondition.LIKE_OPERATOR, pattern);
	}

	@JSFunction
	public QueryBuilderCondition like(String pattern, char escape)
	{
		return createCompareCondition(ISQLCondition.LIKE_OPERATOR, new Object[] { pattern, String.valueOf(escape) });
	}

	@JSReadonlyProperty
	public QueryBuilderColumn not()
	{
		return new QueryBuilderColumn(getRoot(), getParent(), queryColumn, !negate);
	}

	@JSReadonlyProperty
	public QueryBuilderSort asc()
	{
		return new QueryBuilderSort(getRoot(), this, true);
	}

	@JSReadonlyProperty
	public QueryBuilderSort desc()
	{
		return new QueryBuilderSort(getRoot(), this, false);
	}

	@JSReadonlyProperty
	public QueryBuilderAggregate count()
	{
		return new QueryBuilderAggregate(getRoot(), getParent(), queryColumn, QueryAggregate.COUNT);
	}

	@JSReadonlyProperty
	public QueryBuilderAggregate avg()
	{
		return new QueryBuilderAggregate(getRoot(), getParent(), queryColumn, QueryAggregate.AVG);
	}

	@JSReadonlyProperty
	public QueryBuilderAggregate max()
	{
		return new QueryBuilderAggregate(getRoot(), getParent(), queryColumn, QueryAggregate.MAX);
	}

	@JSReadonlyProperty
	public QueryBuilderAggregate min()
	{
		return new QueryBuilderAggregate(getRoot(), getParent(), queryColumn, QueryAggregate.MIN);
	}

	@JSReadonlyProperty
	public QueryBuilderAggregate sum()
	{
		return new QueryBuilderAggregate(getRoot(), getParent(), queryColumn, QueryAggregate.SUM);
	}
}
