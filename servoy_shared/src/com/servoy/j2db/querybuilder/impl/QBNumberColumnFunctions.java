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
	 *  @return a QBColumn representing the absolute value of the column.
	 */
	@JSReadonlyProperty(debuggerRepresentation = "Query abs clause")
	public T abs();


	/**
	 * Create sqrt(column) expression
	 * @sample
	 * query.result.add(query.columns.custname.sqrt)
	 *
	 *  @return a QBNumberColumn representing the square root of the column.
	 */
	@JSReadonlyProperty(debuggerRepresentation = "Query sqrt function clause")
	public QBNumberColumnBase sqrt();


	/**
	 * Create mod(arg) expression
	 * @param arg mod arg
	 * @sample
	 * query.result.add(query.columns.mycol.mod(2))
	 *
	 *  @return a QBColumn representing the modulo of the column.
	 */
	@JSFunction
	public T mod(Object arg);

	/**
	 * Add up value
	 * @param value nr to add
	 * @sample
	 * query.result.add(query.columns.mycol.plus(2))
	 *
	 *  @return a QBNumberColumn representing the column added with the value.
	 */
	@JSFunction
	public QBNumberColumnBase plus(Object value);

	/**
	 * Subtract value
	 * @param value nr to subtract
	 * @sample
	 * query.result.add(query.columns.mycol.minus(2))
	 *
	 *  @return a QBNumberColumn representing the column substracted with the value.
	 */
	@JSFunction
	public QBNumberColumnBase minus(Object value);

	/**
	 * Multiply with value
	 * @param value nr to multiply with
	 * @sample
	 * query.result.add(query.columns.mycol.multiply(2))
	 *
	 *  @return a QBNumberColumn representing the column multiplied with the value.
	 */
	@JSFunction
	public QBNumberColumnBase multiply(Object value);

	/**
	 * Divide by value
	 * @param value nr to divide by
	 * @sample
	 * query.result.add(query.columns.mycol.divide(2))
	 *
	 *  @return a QBNumberColumn representing the column divided by the value.
	 */
	@JSFunction
	public QBNumberColumnBase divide(Object value);

	/**
	 * Create an aggregate sum expression.
	 * @sample
	 * 	var query = datasources.db.example_data.orders.createSelect();
	 * 	query.groupBy.addPk() // have to group by on pk when using having-conditions in (foundset) pk queries
	 * 	.root.having.add(query.joins.orders_to_order_details.columns.quantity.sum.gt(100))
	 * 	foundset.loadRecords(query)
	 *
	 *  @return a QBColumn representing the sum aggregate of the column.
	 */
	@JSReadonlyProperty(debuggerRepresentation = "Query sum clause")
	T sum();

	/**
	 * Create an aggregate average expression.
	 * @sample
	 * 	var query = datasources.db.example_data.orders.createSelect();
	 * 	query.groupBy.addPk() // have to group by on pk when using having-conditions in (foundset) pk queries
	 * 	.root.having.add(query.joins.orders_to_order_details.columns.quantity.avg.eq(1))
	 * 	foundset.loadRecords(query)
	 *
	 *  @return a QBColumn representing the average aggregate of the column.
	 */
	@JSReadonlyProperty(debuggerRepresentation = "Query avg clause")
	T avg();

}
