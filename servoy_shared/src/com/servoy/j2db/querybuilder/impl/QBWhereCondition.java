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
import com.servoy.j2db.query.AndOrCondition;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.querybuilder.IQueryBuilderCondition;
import com.servoy.j2db.querybuilder.IQueryBuilderWhereCondition;

/**
 * <p>The <code>QBWhereCondition</code> class is an essential component for managing the WHERE clause in queries within the Servoy environment.
 * It provides flexibility for dynamically adding, removing, and clearing conditions in a query, making it easier to build complex SQL conditions.
 * Through methods such as <code>js_add()</code> and <code>add()</code>, conditions can be added to the logical condition list, allowing
 * both unnamed and named conditions to be incorporated into the query. These conditions can be specified programmatically or by name,
 * offering significant versatility in query construction.</p>
 *
 * <p>The class offers robust functionality to handle the logical structure of conditions with methods like <code>getQueryCondition()</code>,
 * which retrieves or creates the <code>AndOrCondition</code> for the query. The <code>getQueryCondition(boolean create)</code> variant
 * ensures that the condition is generated when needed, providing more control over the query construction process.</p>
 *
 * <p>Additional methods like <code>remove()</code> and <code>clear()</code> allow for easy modification of the condition list.
 * The <code>remove()</code> method enables the removal of named conditions from the logical group, while <code>clear()</code>
 * resets all conditions in the WHERE clause, facilitating full control over the query structure.</p>
 *
 * @author rgansevles
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class QBWhereCondition extends QBLogicalCondition implements IQueryBuilderWhereCondition
{
	QBWhereCondition(QBSelect select)
	{
		super(select, select, null);
	}

	@Override
	public AndOrCondition getQueryCondition()
	{
		return getQueryCondition(true);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.querybuilder.impl.QBCondition#getQueryCondition(boolean)
	 */
	@Override
	public AndOrCondition getQueryCondition(boolean create)
	{
		QuerySelect query = getRoot().getQuery(create);
		return query == null ? null : query.getCondition();
	}

	/**
	 * @sameas com.servoy.j2db.querybuilder.impl.QBLogicalCondition#js_add(QBCondition)
	 *
	 * @return the current QBWhereCondition instance after adding the specified condition.
	 */
	@Override
	public QBWhereCondition js_add(QBCondition condition)
	{
		return (QBWhereCondition)super.js_add(condition);
	}

	@Override
	public QBWhereCondition add(IQueryBuilderCondition condition)
	{
		return (QBWhereCondition)super.add(condition);
	}

	/**
	 * Add a named condition to the AND or OR condition list.
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add("mycond", query.columns.orderdate.isNull)
	 *
	 * @param name the name of the condition
	 * @param condition the condition to add
	 *
	 * @return the current QBWhereCondition instance after adding the named condition.
	 */
	@Override
	public QBWhereCondition js_add(String name, QBCondition condition)
	{
		return (QBWhereCondition)super.js_add(name, condition);
	}

	@Override
	public QBWhereCondition add(String name, IQueryBuilderCondition condition)
	{
		return (QBWhereCondition)super.add(name, condition);
	}

	/**
	 * Remove a named condition from the AND or OR condition list.
	 *
	 * @param name The condition name.
	 *
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.remove("mycond")
	 *
	 * @return the current QBWhereCondition instance after removing the specified condition.
	 */
	@Override
	@JSFunction
	public QBWhereCondition remove(String name)
	{
		return (QBWhereCondition)super.remove(name);
	}

	/**
	 * Clear the conditions in the query where-clause.
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.clear()
	 *
	 * @return the current QBWhereCondition instance after clearing all conditions.
	 */
	@Override
	@JSFunction
	public QBWhereCondition clear()
	{
		return (QBWhereCondition)super.clear();
	}

}
