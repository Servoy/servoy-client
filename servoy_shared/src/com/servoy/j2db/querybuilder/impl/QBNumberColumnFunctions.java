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
 * This interface lists the functions on numeric (int or number) columns, some return the same column type as column they are applied to.
 *
 * @author rgansevles
 *
 */
public interface QBNumberColumnFunctions<T>
{
	/**
	 * Create abs(column) expression
	 * @sample
	 * query.result.add(query.columns.custname.abs)
	 *
	 *  @return a QBFunction representing the absolute value function.
	 */
	@JSReadonlyProperty
	public T abs();


	/**
	 * Create sqrt(column) expression
	 * @sample
	 * query.result.add(query.columns.custname.sqrt)
	 *
	 *  @return a QBFunction representing the square root function.
	 */
	@JSReadonlyProperty
	public QBNumberColumnBase sqrt();


	/**
	 * Create mod(arg) expression
	 * @param arg mod arg
	 * @sample
	 * query.result.add(query.columns.mycol.mod(2))
	 *
	 *  @return a QBFunction representing the modulo operation.
	 */
	@JSFunction
	public T mod(Object arg);

	/**
	 * Add up value
	 * @param arg nr to add
	 * @sample
	 * query.result.add(query.columns.mycol.plus(2))
	 *
	 *  @return a QBFunction representing the addition operation.
	 */
	@JSFunction
	public QBNumberColumnBase plus(Object arg);

	/**
	 * Subtract value
	 * @param arg nr to subtract
	 * @sample
	 * query.result.add(query.columns.mycol.minus(2))
	 *
	 *  @return a QBFunction representing the subtraction operation.
	 */
	@JSFunction
	public QBNumberColumnBase minus(Object arg);

	/**
	 * Multiply with value
	 * @param arg nr to multiply with
	 * @sample
	 * query.result.add(query.columns.mycol.multiply(2))
	 *
	 *  @return a QBFunction representing the multiplication operation.
	 */
	@JSFunction
	public QBNumberColumnBase multiply(Object arg);

	/**
	 * Divide by value
	 * @param arg nr to divide by
	 * @sample
	 * query.result.add(query.columns.mycol.divide(2))
	 *
	 *  @return a QBFunction representing the division operation.
	 */
	@JSFunction
	public QBNumberColumnBase divide(Object arg);

	/**
	 * Create an aggregate sum expression.
	 * @sample
	 * 	var query = datasources.db.example_data.orders.createSelect();
	 * 	query.groupBy.addPk() // have to group by on pk when using having-conditions in (foundset) pk queries
	 * 	.root.having.add(query.joins.orders_to_order_details.columns.quantity.sum.gt(100))
	 * 	foundset.loadRecords(query)
	 *
	 *  @return a QBAggregate representing the sum aggregate function.
	 */
	@JSReadonlyProperty
	T sum();

	/**
	 * Create an aggregate average expression.
	 * @sample
	 * 	var query = datasources.db.example_data.orders.createSelect();
	 * 	query.groupBy.addPk() // have to group by on pk when using having-conditions in (foundset) pk queries
	 * 	.root.having.add(query.joins.orders_to_order_details.columns.quantity.avg.eq(1))
	 * 	foundset.loadRecords(query)
	 *
	 *  @return a QBAggregate representing the average aggregate function.
	 */
	@JSReadonlyProperty
	T avg();

}
