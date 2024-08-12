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

import static com.servoy.j2db.query.AbstractBaseQuery.deepClone;
import static com.servoy.j2db.query.AndCondition.and;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.query.AndCondition;
import com.servoy.j2db.query.AndOrCondition;
import com.servoy.j2db.query.ISQLCondition;
import com.servoy.j2db.querybuilder.IQueryBuilderCondition;
import com.servoy.j2db.querybuilder.IQueryBuilderLogicalCondition;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;

/**
 * Logical clause for a query; conditions can be added by name.
 *
 * @author rgansevles
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, scriptingName = "QBLogicalCondition")
public class QBLogicalCondition extends QBCondition implements IQueryBuilderLogicalCondition
{
	private static final String CONDITION_ANONYMOUS = "<anonymous>"; // When no condition name is given
	private static final String[] EMPTY_STRINGS = new String[0];

	QBLogicalCondition(QBSelect root, QBTableClause parent, AndOrCondition queryCondition)
	{
		super(root, parent, queryCondition);
	}

	@Override
	public AndOrCondition getQueryCondition()
	{
		return (AndOrCondition)super.getQueryCondition();
	}

	/**
	 * @param create
	 */
	public AndOrCondition getQueryCondition(boolean create)
	{
		return getQueryCondition();
	}

	/**
	 * Add a condition to the AND or OR condition list.
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.orderdate.isNull)
	 *
	 * @param condition the condition to add
	 */
	public QBLogicalCondition js_add(QBCondition condition)
	{
		return add(condition);
	}

	public QBLogicalCondition add(IQueryBuilderCondition condition)
	{
		return add(null, condition);
	}

	/**
	 * Add a named condition to the logical condition.
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * // create a logical condition
	 * var condition = query.and
	 * // add named conditions
	 * condition.add("undated", query.columns.orderdate.isNull)
	 * condition.add("expensive", query.columns.orderamount.gt(10000))
	 *
	 * query.where.add("mycond", condition)
	 *
	 * // part of the condition can be removed again
	 * condition.remove("undated")
	 *
	 * @param name the name of the condition
	 * @param condition the condition to add
	 */
	public QBLogicalCondition js_add(String name, QBCondition condition)
	{
		return add(name, condition);
	}

	public QBLogicalCondition add(String name, IQueryBuilderCondition condition)
	{
		getQueryCondition().addCondition(name == null ? CONDITION_ANONYMOUS : name, ((QBCondition)condition).getQueryCondition());
		return this;
	}

	/**
	 * Get the names for the conditions in the logical condition.
	 * @sample
	 * var cond = query.getCondition('mycond')
	 * for (var cname in cond.conditionnames)
	 * {
	 * 	var subcond = cond.getCondition(cname)
	 * }
	 */
	@JSReadonlyProperty
	public String[] conditionnames()
	{
		AndOrCondition condition = getQueryCondition(false);
		if (condition == null)
		{
			return EMPTY_STRINGS;
		}
		return condition.getConditionNames();
	}

	/**
	 * Remove a named condition from the logical condition.
	 *
	 * @param name The condition name.
	 *
	 * @sample
	 * var cond = query.getCondition('mycond')
	 * cond.remove('mysubcond')
	 */
	@JSFunction
	public QBLogicalCondition remove(String name)
	{
		AndOrCondition condition = getQueryCondition(false);
		if (condition != null)
		{
			condition.setCondition(name == null ? CONDITION_ANONYMOUS : name, null);
		}
		return this;
	}

	/**
	 * Clear the conditions in the logical condition.
	 * @sample
	 * var cond = query.getCondition('mycond')
	 * cond.clear()
	 */
	@JSFunction
	public QBLogicalCondition clear()
	{
		AndOrCondition condition = getQueryCondition(false);
		if (condition != null)
		{
			condition.clear();
		}
		return this;
	}

	/**
	 * Get a named condition in the logical condition.
	 *
	 * @param name The condition name.
	 *
	 * @sampleas conditionnames()
	 */
	@JSFunction
	public QBLogicalCondition getCondition(String name)
	{
		AndOrCondition queryCondition = getQueryCondition(false);
		if (queryCondition != null)
		{
			ISQLCondition condition = deepClone(and(queryCondition.getConditions(name == null ? CONDITION_ANONYMOUS : name)));
			if (condition != null)
			{
				if (!(condition instanceof AndOrCondition))
				{
					condition = new AndCondition(CONDITION_ANONYMOUS, condition);
				}
				return new QBLogicalCondition(getRoot(), getParent(), (AndOrCondition)condition);
			}
		}

		return null;
	}

	@Override
	public String toString()
	{
		return "QBLogicalCondition " + getQueryCondition().toString();
	}
}
