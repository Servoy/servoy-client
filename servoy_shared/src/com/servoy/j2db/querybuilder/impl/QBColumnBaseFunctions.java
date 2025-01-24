/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;

/**
 * This interface lists the functions on columns that apply to all columns.
 *
 * @author rgansevles
 *
 */
public interface QBColumnBaseFunctions<T> extends QBColumnComparable
{
	/**
	 * Create an aggregate min expression.
	 * @sample
	 * 	var query = datasources.db.example_data.orders.createSelect();
	 * 	query.groupBy.addPk() // have to group by on pk when using having-conditions in (foundset) pk queries
	 * 	.root.having.add(query.joins.orders_to_order_details.columns.quantity.count.min(10))
	 * 	foundset.loadRecords(query)
	 *
	 *  @return a QBColumn representing the minimum aggregate function.
	 */
	@JSReadonlyProperty
	T min();

	/**
	 * Create an aggregate max expression.
	 * @sample
	 * 	var query = datasources.db.example_data.orders.createSelect();
	 * 	query.groupBy.addPk() // have to group by on pk when using having-conditions in (foundset) pk queries
	 * 	.root.having.add(query.joins.orders_to_order_details.columns.quantity.count.max(10))
	 * 	foundset.loadRecords(query)
	 *
	 *  @return a QBColumn representing the sum aggregate function.
	 */
	@JSReadonlyProperty
	T max();

	/**
	 * Create nullif(arg) expression
	 * @param arg object to compare
	 * @sample
	 * query.result.add(query.columns.mycol.nullif('none'))
	 *
	 *  @return a QBColumn representing the nullif expression.
	 */
	@JSFunction
	T nullif(Object arg);

	/**
	 * Create coalesce(arg) expression
	 * @param value when column is null
	 * @sample
	 * query.result.add(query.columns.mycol.coalesce('defval'))
	 *
	 *  @return a QBColumn representing the coalesce expression.
	 */
	@JSFunction
	T coalesce(Object... args);

}
