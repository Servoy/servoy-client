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

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;

/**
 * <p>The <code>QBColumn</code> class represents a column in a <code>QBSelect</code> query. It is used
 * to define conditions, aggregate functions, and transformations within queries. The class provides
 * a range of properties and methods for handling column-specific operations such as mathematical
 * expressions, sorting, and conditional comparisons.</p>
 *
 * <p>Key properties include <code>abs</code>, <code>ceil</code>, and <code>floor</code> for mathematical
 * transformations, and <code>avg</code>, <code>count</code>, and <code>sum</code> for aggregate functions.
 * Sorting can be applied using <code>asc</code> and <code>desc</code>, while date manipulations are
 * supported with properties like <code>day</code>, <code>month</code>, and <code>year</code>.</p>
 *
 * <p>Methods enable additional functionality, including comparisons (<code>eq</code>, <code>between</code>,
 * <code>like</code>), mathematical operations (<code>plus</code>, <code>minus</code>, <code>mod</code>),
 * and string manipulations (<code>substring</code>, <code>concat</code>). The <code>parent</code> and
 * <code>root</code> properties provide access to the query's structure, supporting complex query building
 * and integration.</p>
 *
 * <p>For more information about constructing and executing queries, refer to the
 * <a href="./qbselect.md">QBSelect</a> section of this documentation.</p>
 *
 * @author rgansevles
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public interface QBColumn extends QBColumnRagtest<QBColumn>, QBColumnCompare
{
	/**
	 * Compare column with null.
	 * @sample
	 * query.where.add(query.columns.flag.isNull)
	 */
	@JSReadonlyProperty
	default QBCondition isNull()
	{
		return eq(null);
	}

	/**
	 * Create a negated condition.
	 * @sample
	 * query.where.add(query.columns.flag.not.eq(1))
	 *
	 */
	@JSReadonlyProperty
	QBColumnCompare not();


	/**
	 * Create an ascending sort expression
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.sort
	 * .add(query.joins.orders_to_order_details.columns.quantity.asc)
	 * .add(query.columns.companyid)
	 * foundset.loadRecords(query)
	 */
	@JSReadonlyProperty
	QBSort asc();

	/**
	 * Create an descending sort expression
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.sort
	 * .add(query.joins.orders_to_order_details.columns.quantity.desc)
	 * .add(query.columns.companyid)
	 * foundset.loadRecords(query)
	 */
	@JSReadonlyProperty
	QBSort desc();

	/**
	 * Create an aggregate count expression.
	 * @sample
	 * 	var query = datasources.db.example_data.orders.createSelect();
	 * 	query.groupBy.addPk() // have to group by on pk when using having-conditions in (foundset) pk queries
	 * 	.root.having.add(query.joins.orders_to_order_details.columns.quantity.count.eq(0))
	 * 	foundset.loadRecords(query)
	 */
	@JSReadonlyProperty
	QBCountAggregate count();

	/**
	 * Concatenate with value
	 * @param arg value to concatenate with
	 * @sample
	 * query.result.add(query.columns.firstname.concat(' ').concat(query.columns.lastname))
	 */
	@JSFunction
	// concat is supported on most column types, databases can automatically convert number columns to string before concatenation
	public QBTextColumnBase concat(Object arg);

	/**
	 * Create cast(column, type) expression
	 * @param type string type, see QUERY_COLUMN_TYPES
	 * @sample
	 * query.result.add(query.columns.mycol.cast(QUERY_COLUMN_TYPES.TYPE_INTEGER))
	 */
	@JSFunction
	public QBColumn cast(String type);

}
