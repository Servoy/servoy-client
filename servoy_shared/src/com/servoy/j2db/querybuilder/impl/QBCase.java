/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2021 Servoy BV

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

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.query.QuerySearchedCaseExpression;
import com.servoy.j2db.query.QueryWhenClause;
import com.servoy.j2db.querybuilder.IQueryBuilderCase;
import com.servoy.j2db.querybuilder.IQueryBuilderCondition;
import com.servoy.j2db.util.Pair;

/**
 * <p>The <code>QBCase</code> class is a utility for constructing SQL <code>CASE</code> expressions
 * within a <code>QBSelect</code> query. It allows for dynamic conditional logic by specifying
 * <code>WHEN</code> clauses and an optional <code>ELSE</code> clause to handle unmatched conditions.
 * This enables advanced query customizations, such as transforming values or applying conditional
 * calculations.</p>
 *
 * <p>The <code>when</code> method adds conditions to the <code>CASE</code> expression, while the
 * <code>else</code> method sets the default value for cases where no conditions are satisfied. The
 * <code>parent</code> and <code>root</code> properties provide access to the query's parent table
 * clause or the root query, allowing seamless integration with other query builder components.</p>
 *
 * <p>For more information about constructing and executing queries, refer to
 * <a href="./qbselect.md">QBSelect</a> section of this documentation.</p>
 *
 * @author rgansevles
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, scriptingName = "QBCase")
public class QBCase extends QBPart implements IQueryBuilderCase
{
	private final List<Pair<QBCondition, Object>> whenThen = new ArrayList<>();

	QBCase(QBSelect root, QBTableClause queryBuilderTableClause)
	{
		super(root, queryBuilderTableClause);
	}

	/**
	 * Add a when-clause to the case searched expression.
	 *
	 * @param condition The condition.
	 *
	 * @sampleas com.servoy.j2db.querybuilder.impl.QBSelect#js_case()
	 *
	 * @return A QBCaseWhen instance, allowing you to specify the then value for the provided when condition.
	 */
	@JSFunction
	public QBCaseWhen when(IQueryBuilderCondition condition)
	{
		return new QBCaseWhen(this, condition);
	}

	/**
	 * Set the return value to use when none of the when-clauses conditions are met.
	 *
	 * @param value The value.
	 *
	 * @sampleas com.servoy.j2db.querybuilder.impl.QBSelect#js_case()
	 *
	 * @return A QBSearchedCaseExpression that defines the value to return if none of the when clauses are satisfied.
	 */
	public QBSearchedCaseExpression jsFunction_else(Object value)
	{
		return qelse(value);
	}

	/**
	 * @deprecated replaced by else.
	 */
	@Deprecated
	@JSFunction
	public QBSearchedCaseExpression elseValue(Object value)
	{
		return qelse(value);
	}

	public QBSearchedCaseExpression qelse(Object value)
	{
		return new QBSearchedCaseExpression(getRoot(), getParent(), buildSearchedCaseExpression(getRoot(), whenThen, value));
	}

	QBCase withWhenThen(IQueryBuilderCondition whenCondition, Object thenValue)
	{
		whenThen.add(new Pair<>((QBCondition)whenCondition, thenValue));
		return this;
	}

	private static QuerySearchedCaseExpression buildSearchedCaseExpression(QBSelect root, List<Pair<QBCondition, Object>> whenThen, Object elseValue)
	{
		List<QueryWhenClause> qWhenThen = whenThen.stream()
			.map(pair -> new QueryWhenClause(pair.getLeft().getQueryCondition(), root.createOperand(pair.getRight(), null, 0)))
			.collect(toList());

		return new QuerySearchedCaseExpression(qWhenThen, root.createOperand(elseValue, null, 0), null);
	}

	@Override
	public String toString()
	{
		return "QBCase(Helper class for creating case expressions)";
	}
}
