/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.query.AbstractBaseQuery;
import com.servoy.j2db.query.AndCondition;
import com.servoy.j2db.query.AndOrCondition;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.querybuilder.IQueryBuilderCondition;
import com.servoy.j2db.querybuilder.IQueryBuilderWhereCondition;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;

/**
 * Where clause for a query, conditions can be added by name.
 * @author rgansevles
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class QBWhereCondition extends QBLogicalCondition implements IQueryBuilderWhereCondition
{
	private static final String[] EMPTY_STRINGS = new String[0];
	private static final String CONDITION_ANONYMOUS = "<anonymous>"; // When no condition name is given

	QBWhereCondition(QBSelect select)
	{
		super(select, select, null);
	}

	@Override
	public AndOrCondition getQueryCondition()
	{
		return getRoot().getQuery().getWhere();
	}

	/**
	 * @sameas com.servoy.j2db.querybuilder.impl.QBLogicalCondition#js_add(QBCondition)
	 */
	@Override
	public QBWhereCondition js_add(QBCondition condition)
	{
		return add(null, condition);
	}

	@Override
	public QBWhereCondition add(IQueryBuilderCondition condition)
	{
		return add(null, condition);
	}

	/**
	 * Add a named condition to the AND or OR condition list.
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add("mycond", query.columns.orderdate.isNull)
	 *
	 * @param name the name of the condition
	 * @param condition the condition to add
	 */
	public QBWhereCondition js_add(String name, QBCondition condition)
	{
		return add(name, condition);
	}

	public QBWhereCondition add(String name, IQueryBuilderCondition condition)
	{
		getRoot().getQuery().addCondition(name == null ? CONDITION_ANONYMOUS : name, ((QBCondition)condition).getQueryCondition());
		return this;
	}

	/**
	 * Get the names for the conditions in the query where-clause.
	 * @sample
	 * var q = foundset.getQuery()
	 * for (var c in q.where.conditionnames)
	 * {
	 * 	var cond = q.where.getCondition(c)
	 * }
	 */
	@JSReadonlyProperty
	public String[] conditionnames()
	{
		QuerySelect query = getRoot().getQuery(false);

		if (query == null)
		{
			return EMPTY_STRINGS;
		}
		return query.getConditionNames();
	}

	/**
	 * Remove a named condition from the AND or OR condition list.
	 *
	 * @param name The condition name.
	 *
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.remove("mycond")
	 */
	@JSFunction
	public QBWhereCondition remove(String name)
	{
		QuerySelect query = getRoot().getQuery(false);
		if (query != null)
		{
			query.setCondition(name == null ? CONDITION_ANONYMOUS : name, null);
		}
		return this;
	}

	/**
	 * Clear the conditions in the query where-clause.
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.clear()
	 */
	@JSFunction
	public QBWhereCondition clear()
	{
		QuerySelect query = getRoot().getQuery(false);
		if (query != null)
		{
			query.clearConditions();
		}
		return this;
	}

	/**
	 * Get a named condition in the query where-clause.
	 *
	 * @param name The condition name.
	 *
	 * @sampleas conditionnames()
	 */
	@JSFunction
	public QBCondition getCondition(String name)
	{
		QuerySelect query = getRoot().getQuery(false);
		AndCondition condition = query == null ? null : query.getCondition(name);
		return condition == null ? null : new QBCondition(getRoot(), getParent(), AbstractBaseQuery.deepClone(condition));
	}
}
