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

package com.servoy.j2db.querybuilder;

import com.servoy.j2db.persistence.RepositoryException;

/**
 * Column in Servoy Query Objects.
 * 
 * @author rgansevles
 *
 * @since 6.1
 */

public interface IQueryBuilderColumn extends IQueryBuilderPart
{
	/**
	 * Compare column with a value or another column.
	 * Operator: equals
	 * <pre>
	 * query.where().add(query.getColumn("value").eq(new Integer(100));
	 * </pre>
	 */
	IQueryBuilderCondition eq(Object value);

	/**
	 * Compare column with a value or another column.
	 * Operator: greaterThan
	 * @see #eq
	 */
	IQueryBuilderCondition gt(Object value);

	/**
	 * Compare column with a value or another column.
	 * Operator: lessThan
	 * @see #eq
	 */
	IQueryBuilderCondition lt(Object value);

	/**
	 * Compare column with a value or another column.
	 * Operator: greaterThanOrEqual
	 * @see #eq
	 */
	IQueryBuilderCondition ge(Object value);

	/**
	 * Compare column with a value or another column.
	 * Operator: lessThanOrEqual
	 * @see #eq
	 */
	IQueryBuilderCondition le(Object value);

	/**
	 * Compare column with a value or another column.
	 * Compare column to between 2 values or other columns.
	 * <pre>
	 * query.where().add(query.getColumn("value").between(new Integer(1), new Integer(99));
	 * </pre>
	 */
	IQueryBuilderCondition between(Object value1, Object value2);

	/**
	 * Compare column with subquery result.
	 * <pre>
	 * query.where().add(query.getColumn("value").in(subQuery.result().add(query.getColumn("code")).getParent().where().add(subQuery.getColumn("flag").eq("T"))));
	 * </pre>
	 */
	IQueryBuilderCondition in(IQueryBuilderPart query) throws RepositoryException;

	/**
	 * Compare column with values.
	 * <pre>
	 * query.where().add(query.getColumn("value").in(subQuery.result().add(new Object[]{Integer.valueOf(1), Integer.valueof(100)});
	 * </pre>
	 */
	IQueryBuilderCondition in(Object[] values);

	/**
	 * Compare column with a value or another column.
	 * Operator: isNull
	 * @see #eq
	 */
	IQueryBuilderCondition isNull();

	/**
	 * Compare column with a value or another column.
	 * Operator: like
	 * @see #eq
	 */
	IQueryBuilderCondition like(String pattern);

	/**
	 * Compare column with a value or another column.
	 * Operator: like, with escape character
	 * @see #eq
	 */
	IQueryBuilderCondition like(String pattern, char escape);

	/**
	 * Create a negated condition.
	 * <pre>
	 * query.where().add(query.getColumn("value").not().eq(new Integer(100));
	 * </pre>
	 * @return
	 */
	IQueryBuilderColumn not();

	/**
	 * Create an ascending sort expression
	 * <pre>
	 * query.sort().add(query.getColumn("value").asc());
	 * </pre>
	 */
	IQueryBuilderSort asc();

	/**
	 * Create an descending sort expression
	 * @see #asc
	 */
	IQueryBuilderSort desc();

	/**
	 * Create an aggregate expression.
	 * Aggregate: count
	 * <pre>
	 * // select value, count(value) from tab group by value order by count(value) desc 
	 * query.result().add(query.getColumn("value")).add(query.getColumn("value").count())
	 *     .getParent().groupBy().add("value")
	 *     .getParent().sort().add(query.getColumn("value").count().desc());
	 * </pre>
	 */
	IQueryBuilderAggregate count();

	/**
	 * Create an aggregate expression.
	 * Aggregate: max
	 * @see #count
	 */
	IQueryBuilderAggregate max();

	/**
	 * Create an aggregate expression.
	 * Aggregate: min
	 * @see #count
	 */
	IQueryBuilderAggregate min();

	/**
	 * Create an aggregate expression.
	 * Aggregate: avg
	 * @see #count
	 */
	IQueryBuilderAggregate avg();

	/**
	 * Create an aggregate expression.
	 * Aggregate: sum
	 * @see #count
	 */
	IQueryBuilderAggregate sum();

	/**
	 * Create upper(column) expression
	 */
	IQueryBuilderFunction upper();

	// TODO: add other functions
}
