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

import com.servoy.j2db.querybuilder.IQueryBuilderColumn;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;
import com.servoy.j2db.scripting.annotations.JSRealClass;

/**
 * This interface lists functions on array columns.
 *
 * @author rgansevles
 *
 */
@JSRealClass(QBArrayColumn.class)
public interface QBArrayColumnBase extends IQueryBuilderColumn
{
	/**
	 * Create cardinality(column) expression
	 * @sample
	 * query.result.add(query.columns.arraycol.cardinality)
	 */
	@JSReadonlyProperty(debuggerRepresentation = "Query cardinality clause")
	QBIntegerColumnBase cardinality();

	/**
	 * Create a condition that tests if the array column contains the given value.
	 *
	 * Only supported on databases with native array support (PostgreSQL, HSQLDB), and only on array columns.
	 *
	 * The value is converted to the element type of the array column (for example a number for a bigint array or a
	 * string for a uuid array); a value that cannot be converted gives an error.
	 * Values are compared exactly: text is case sensitive, and for a timestamp array the value must be the exact same
	 * instant (including milliseconds), nothing is truncated. Truncate the value yourself first when you want
	 * a coarser match (for example the start of the day). For a date array only the day is compared (the time part of
	 * the given value is dropped).
	 *
	 * Standard sql null semantics apply: a null array contains nothing, so neither contains() nor not.contains() match
	 * the rows where the array is null. contains(null) never matches (null is not equal to anything); not.contains(null)
	 * matches all rows with an array.
	 *
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.tags.contains('urgent'))
	 * foundset.loadRecords(query);
	 *
	 * @param value the element to look for in the array
	 *
	 * @return a QBCondition that is true when the array contains the value.
	 */
	@JSFunction
	QBCondition contains(Object value);

	/**
	 * Create a condition that tests if the array column contains all of the given values.
	 *
	 * Only supported on databases with native array support (PostgreSQL, HSQLDB), and only on array columns.
	 *
	 * The order and duplicates in the given values do not matter, an empty array matches every row that has an array.
	 * The values are converted and compared in the same way as for {@link #contains(Object)}, a null value in the given
	 * values never matches (null is not equal to anything).
	 * A null array never matches, the negated condition (not.containsAll) does not match the rows where the array is null
	 * either (standard sql null semantics).
	 *
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.tags.containsAll(['urgent', 'open']))
	 * foundset.loadRecords(query);
	 *
	 * @param values the elements that must all be present in the array
	 *
	 * @return a QBCondition that is true when the array contains all of the values.
	 */
	@JSFunction
	QBCondition containsAll(Object[] values);
}
