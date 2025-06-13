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

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.querybuilder.IQueryBuilderColumn;

/**
 * This interface lists all methods that are for comparing columns with values or other columns.
 * The query.columns.mycol.not prefix is supported to negate the compare condition.
 *
 * These methods are not placed in QBColumn, that way we can make sure that query.columns.mycol.not only has
 * compare methods in code completion and query.columns.mycol.not.sqrt (for example) gives a warning.
 *
 * @author rgansevles
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public interface QBColumnComparable extends IQueryBuilderColumn
{

	/**
	 * Compare column with a value or another column.
	 * Operator: greaterThan
	 * @param value
	 * @sample
	 * query.where.add(query.columns.flag.gt(0))
	 *
	 *  @return a QBCondition representing the "greater than" comparison.
	 */
	@JSFunction
	QBCondition gt(Object value);

	/**
	 * Compare column with a value or another column.
	 * Operator: lessThan
	 * @param value
	 * @sample
	 * query.where.add(query.columns.flag.lt(99))
	 *
	 *  @return a QBCondition representing the "less than" comparison.
	 */
	@JSFunction
	QBCondition lt(Object value);

	/**
	 * Compare column with a value or another column.
	 * Operator: greaterThanOrEqual
	 * @param value
	 * @sample
	 * query.where.add(query.columns.flag.ge(2))
	 *
	 *  @return a QBCondition representing the "greater than or equal to" comparison.
	 */
	@JSFunction
	QBCondition ge(Object value);

	/**
	 * Compare column with a value or another column.
	 * Operator: lessThanOrEqual
	 * @param value
	 * @sample
	 * query.where.add(query.columns.flag.le(2))
	 *
	 *  @return a QBCondition representing the "less than or equal to" comparison.
	 */
	@JSFunction
	QBCondition le(Object value);

	/**
	 * Compare column to a range of 2 values or other columns.
	 * @param value1
	 * @param value2
	 * @sample
	 * query.where.add(query.columns.flag.between(0, 5))
	 *
	 *  @return a QBCondition representing the "between" comparison for the two values.
	 */
	@JSFunction
	QBCondition between(Object value1, Object value2);

	/**
	 * Compare column with subquery result.
	 * @param query subquery
	 * @sample
	 * query.where.add(query.columns.flag.isin(query2))
	 *
	 *  @return a QBCondition representing the "in" comparison with a subquery.
	 */
	@JSFunction
	QBCondition isin(QBPart query);

	/**
	 * Compare column with custom query result.
	 * @param customQuery custom query
	 * @param args query arguments
	 * @sample
	 * query.where.add(query.columns.ccy.isin("select ccycode from currencies c where c.category = " + query.getTableAlias() + ".currency_category and c.flag = ?", ['T']))
	 *
	 *  @return a QBCondition representing the "in" comparison with a custom query and arguments.
	 */
	@JSFunction
	QBCondition isin(String customQuery, Object[] args);

	/**
	 * Compare column with values.
	 * @param values array of values
	 * @sample
	 * query.where.add(query.columns.flag.isin([1, 5, 99]))
	 *
	 *  @return a QBCondition representing the "in" comparison with a list of values.
	 */
	@JSFunction
	QBCondition isin(Object[] values);

	/**
	 * Compare column with a value or another column.
	 * Operator: equals
	 * @param value
	 * @sample
	 * query.where.add(query.columns.flag.eq(1))
	 *
	 *  @return a QBCondition representing the "equals" comparison.
	 */
	@JSFunction
	QBCondition eq(Object value);

}
