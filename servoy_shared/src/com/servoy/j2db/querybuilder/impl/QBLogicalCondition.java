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

import java.util.List;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.query.AndCondition;
import com.servoy.j2db.query.AndOrCondition;
import com.servoy.j2db.query.ISQLCondition;
import com.servoy.j2db.querybuilder.IQueryBuilderCondition;
import com.servoy.j2db.querybuilder.IQueryBuilderLogicalCondition;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;

/**
 * @author rgansevles
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, scriptingName = "QBLogicalCondition")
public class QBLogicalCondition extends QBCondition implements IQueryBuilderLogicalCondition
{
	private static final String[] EMPTY_STRINGS = new String[0];
//	private static final String CONDITION_ANONYMOUS = "<anonymous>"; // When no condition name is given

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
		getQueryCondition().addCondition(((QBCondition)condition).getQueryCondition());
		return this;
	}

	/** RAGTEST doc
	 * Add a named condition to the AND or OR condition list.
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add("mycond", query.columns.orderdate.isNull)
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
		getQueryCondition().addCondition(
//			name == null ? CONDITION_ANONYMOUS :
			name, ((QBCondition)condition).getQueryCondition());
		return this;
	}

	/** RAGTEST doc
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
		AndOrCondition condition = getQueryCondition(false);
		if (condition == null)
		{
			return EMPTY_STRINGS;
		}
		return condition.getConditionNames();
	}

	/** RAGTEST doc
	 * Remove a named condition from the AND or OR condition list.
	 *
	 * @param name The condition name.
	 *
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.remove("mycond")
	 */
	@JSFunction
	public QBLogicalCondition remove(String name)
	{
		AndOrCondition condition = getQueryCondition(false);
		if (condition != null)
		{
			condition.setCondition(
//				name == null ? CONDITION_ANONYMOUS :
				name, null);
		}
		return this;
	}

	/** RAGTEST doc
	 * Clear the conditions in the query where-clause.
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.clear()
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

	/** RAGTEST doc
	 * Get a named condition in the query where-clause.
	 *
	 * @param name The condition name.
	 *
	 * @sampleas conditionnames()
	 */
	@JSFunction
	public QBCondition getCondition(String name)
	{
		AndOrCondition queryCondition = getQueryCondition(false);
		if (queryCondition != null)
		{
			List<ISQLCondition> conditions = queryCondition.getConditions(name);
			if (conditions != null && !conditions.isEmpty())
			{
				ISQLCondition condition;
				if (conditions.size() == 1)
				{
					condition = conditions.get(0);
				}
				else
				{
					condition = new AndCondition(conditions);
				}
				return new QBCondition(getRoot(), getParent(), deepClone(condition));
			}
		}

		return null;
	}

}
