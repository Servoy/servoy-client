/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2024 Servoy BV

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

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.query.IQuerySelectValue;
import com.servoy.j2db.querybuilder.IQueryBuilderPart;

/**
 * RAGTEST doc
 * @author rob
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, scriptingName = "QBColumn")
public interface QBColumn
{
	IQuerySelectValue getQuerySelectValue();

	/**
	 * Extract year from date
	 * @sample
	 * query.result.add(query.columns.mydatecol.year)
	 */
	QBFunction year();

	/**
	 * Extract month from date
	 * @sample
	 * query.result.add(query.columns.mydatecol.month)
	 */
	QBFunction month();

	/**
	 * Extract day from date
	 * @sample
	 * query.result.add(query.columns.mydatecol.day)
	 */
	QBFunction day();

	/**
	 * Extract hour from date
	 * @sample
	 * query.result.add(query.columns.mydatecol.hour)
	 */
	QBIntegerColumnBase hour();

	/**
	 * Extract minute from date
	 * @sample
	 * query.result.add(query.columns.mydatecol.minute)
	 */
	QBFunction minute();

	/**
	 * Extract second from date
	 * @sample
	 * query.result.add(query.columns.mydatecol.second)
	 */
	QBFunction second();

	/**
	 * Create ceil(column) expression
	 * @sample
	 * query.result.add(query.columns.mycol.ceil)
	 */
	QBFunction ceil();

	/**
	 * Create round(column) expression
	 * @sample
	 * query.result.add(query.columns.mycol.round)
	 */
	QBFunction round();

	/**
	 * Create floor(column) expression
	 * @sample
	 * query.result.add(query.columns.mycol.floor)
	 */
	QBFunction floor();

	/**
	 * Concatename with value
	 * @param arg valeu to concatenate with
	 * @sample
	 * query.result.add(query.columns.firstname.concat(' ').concat(query.columns.lastname))
	 */
	QBFunction concat(Object arg);

	/**
	 * Divide by value
	 * @param arg nr to divide by
	 * @sample
	 * query.result.add(query.columns.mycol.divide(2))
	 */
	QBFunction divide(Object arg);

	/**
	 * Multiply with value
	 * @param arg nr to multiply with
	 * @sample
	 * query.result.add(query.columns.mycol.multiply(2))
	 */
	QBFunction multiply(Object arg);

	/**
	 * Subtract value
	 * @param arg nr to subtract
	 * @sample
	 * query.result.add(query.columns.mycol.minus(2))
	 */
	QBFunction minus(Object arg);

	/**
	 * Add up value
	 * @param arg nr to add
	 * @sample
	 * query.result.add(query.columns.mycol.plus(2))
	 */
	QBFunction plus(Object arg);

	/**
	 * Create mod(arg) expression
	 * @param arg mod arg
	 * @sample
	 * query.result.add(query.columns.mycol.mod(2))
	 */
	QBFunction mod(Object arg);

	/**
	 * Create nullif(arg) expression
	 * @param arg object to compare
	 * @sample
	 * query.result.add(query.columns.mycol.nullif('none'))
	 */
	QBFunction nullif(Object arg);

	/**
	 * Create locate(arg, start) expression
	 * @param arg string to locate
	 * @param start start pos
	 * @sample
	 * query.result.add(query.columns.mycol.locate('sample', 5))
	 */
	QBFunction locate(Object arg, int start);

	/**
	 * Create locate(arg) expression
	 * @param arg string to locate
	 * @sample
	 * query.result.add(query.columns.mycol.locate('sample'))
	 */
	QBFunction locate(Object arg);

	/**
	 * Create substring(pos, len) expression
	 * @param pos
	 * @param len
	 * @sample
	 * query.result.add(query.columns.mycol.substring(3, 2))
	 */
	QBFunction substring(int pos, int len);

	/**
	 * Create substring(pos) expression
	 * @param pos
	 * @sample
	 * query.result.add(query.columns.mycol.substring(3))
	 */
	QBFunction substring(int pos);

	/**
	 * Create cast(column, type) expression
	 * @param type string type, see QUERY_COLUMN_TYPES
	 * @sample
	 * query.result.add(query.columns.mycol.cast(QUERY_COLUMN_TYPES.TYPE_INTEGER))
	 */
	QBFunction cast(String type);

	/**
	 * Create bit_length(column) expression
	 * @sample
	 * query.result.add(query.columns.custname.bit_length)
	 */
	QBFunction bit_length();

	QBFunction length();

	/**
	 * Create length(column) expression
	 * @sample
	 * query.result.add(query.columns.custname.len)
	 */
	QBFunction len();

	/**
	 * Create trim(column) expression
	 * @sample
	 * query.result.add(query.columns.custname.trim)
	 */
	QBFunction trim();

	/**
	 * Create lower(column) expression
	 * @sample
	 * query.result.add(query.columns.custname.lower)
	 */
	QBFunction lower();

	/**
	 * Create sqrt(column) expression
	 * @sample
	 * query.result.add(query.columns.custname.sqrt)
	 */
	QBFunction sqrt();

	/**
	 * Create abs(column) expression
	 * @sample
	 * query.result.add(query.columns.custname.abs)
	 */
	QBFunction abs();

	/**
	 * Create upper(column) expression
	 * @sample
	 * query.result.add(query.columns.custname.upper)
	 */
	QBFunction upper();

	/**
	 * Create an aggregate sum expression.
	 * @sample
	 * 	var query = datasources.db.example_data.orders.createSelect();
	 * 	query.groupBy.addPk() // have to group by on pk when using having-conditions in (foundset) pk queries
	 * 	.root.having.add(query.joins.orders_to_order_details.columns.quantity.count.sum(10))
	 * 	foundset.loadRecords(query)
	 */
	QBColumn sum();

//	/**
//	 * Create an aggregate min expression.
//	 * @sample
//	 * 	var query = datasources.db.example_data.orders.createSelect();
//	 * 	query.groupBy.addPk() // have to group by on pk when using having-conditions in (foundset) pk queries
//	 * 	.root.having.add(query.joins.orders_to_order_details.columns.quantity.count.min(10))
//	 * 	foundset.loadRecords(query)
//	 */
//	T min();
//
//	/**
//	 * Create an aggregate max expression.
//	 * @sample
//	 * 	var query = datasources.db.example_data.orders.createSelect();
//	 * 	query.groupBy.addPk() // have to group by on pk when using having-conditions in (foundset) pk queries
//	 * 	.root.having.add(query.joins.orders_to_order_details.columns.quantity.count.max(10))
//	 * 	foundset.loadRecords(query)
//	 */
//	default T max()
//	{
//		return (T)_max();
//	}
//
//	// RAGTEST doc
//	QBColumn< ? > _max();
//
//	/**
//	 * Create an aggregate average expression.
//	 * @sample
//	 * 	var query = datasources.db.example_data.orders.createSelect();
//	 * 	query.groupBy.addPk() // have to group by on pk when using having-conditions in (foundset) pk queries
//	 * 	.root.having.add(query.joins.orders_to_order_details.columns.quantity.avg.eq(1))
//	 * 	foundset.loadRecords(query)
//	 */
//	T avg();

	/**
	 * Create an aggregate count expression.
	 * @sample
	 * 	var query = datasources.db.example_data.orders.createSelect();
	 * 	query.groupBy.addPk() // have to group by on pk when using having-conditions in (foundset) pk queries
	 * 	.root.having.add(query.joins.orders_to_order_details.columns.quantity.count.eq(0))
	 * 	foundset.loadRecords(query)
	 */
	QBAggregate count();

	/**
	 * Create an descending sort expression
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.sort
	 * .add(query.joins.orders_to_order_details.columns.quantity.desc)
	 * .add(query.columns.companyid)
	 * foundset.loadRecords(query)
	 */
	QBSort desc();

	/**
	 * Create an ascending sort expression
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.sort
	 * .add(query.joins.orders_to_order_details.columns.quantity.asc)
	 * .add(query.columns.companyid)
	 * foundset.loadRecords(query)
	 */
	QBSort asc();

	/**
	 * Create a negated condition.
	 * @sample
	 * query.where.add(query.columns.flag.not.eq(1))
	 *
	 */
	QBColumn not();

	/**
	 * Compare column with a value or another column.
	 * Operator: like, with escape character
	 *
	 * @param pattern the string value of the pattern
	 * @param escape the escape char
	 *
	 * @sample
	 * query.where.add(query.columns.companyname.like('X_%', '_'))
	 */
	QBCondition like(Object pattern, char escape);

	/**
	 * Compare column with a value or another column.
	 * Operator: like
	 *
	 * @param pattern the string value of the pattern
	 *
	 * @sample
	 * query.where.add(query.columns.companyname.like('Serv%'))
	 *
	 * // case-insensitive compares can be done using the upper (or lower) functions,
	 * // this can be useful when using for example German letters like ß,
	 * query.where.add(query.columns.companyname.upper.like(query.functions.upper('groß%')))
	 */
	QBCondition like(Object pattern);

	/**
	 * Compare column with a value or another column.
	 * Operator: equals
	 * @param value
	 * @sample
	 * query.where.add(query.columns.flag.eq(1))
	 */
	QBCondition eq(Object value);

	/**
	 * Compare column with null.
	 * @sample
	 * query.where.add(query.columns.flag.isNull)
	 */
	QBCondition isNull();

	QBCondition in(String customQuery, Object[] args);

	QBCondition in(Object[] values);

	/**
	 * Compare column with values.
	 * @param values array of values
	 * @sample
	 * query.where.add(query.columns.flag.isin([1, 5, 99]))
	 */
	QBCondition js_isin(Object[] values);

	QBCondition in(IQueryBuilderPart query);

	/**
	 * Compare column with custom query result.
	 * @param customQuery custom query
	 * @param args query arguments
	 * @sample
	 * query.where.add(query.columns.ccy.isin("select ccycode from currencies c where c.category = " + query.getTableAlias() + ".currency_category and c.flag = ?", ['T']))
	 */
	QBCondition js_isin(String customQuery, Object[] args);

	/**
	 * Compare column with subquery result.
	 * @param query subquery
	 * @sample
	 * query.where.add(query.columns.flag.isin(query2))
	 */
	QBCondition js_isin(QBPart query);

	/**
	 * Compare column to a range of 2 values or other columns.
	 * @param value1
	 * @param value2
	 * @sample
	 * query.where.add(query.columns.flag.between(0, 5))
	 */
	QBCondition between(Object value1, Object value2);

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#le(Object)
	 * @param value
	 * @sample
	 * query.where.add(query.columns.flag.le(2))
	 */
	QBCondition le(Object value);

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#ge(Object)
	 * @param value
	 * @sample
	 * query.where.add(query.columns.flag.ge(2))
	 */
	QBCondition ge(Object value);

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#lt(Object)
	 * @param value
	 * @sample
	 * query.where.add(query.columns.flag.lt(99))
	 */
	QBCondition lt(Object value);

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#gt(Object)
	 * @param value
	 * @sample
	 * query.where.add(query.columns.flag.gt(0))
	 */
	QBCondition gt(Object value);


}
