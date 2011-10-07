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
 * Interface for building Servoy Query Objects.
 * 
 * @author rgansevles
 *
 * @since 6.1
 */

public interface IQueryBuilderColumn extends IQueryBuilderPart
{
	/**
	 * Compare column with a value or another column.
	 * Operator: greaterThen
	 * <pre>
	 * query.where().add(query.getColumn("value").gt(new Integer(100));
	 * </pre>
	 */
	IQueryBuilderCondition gt(Object value);

	/**
	 * Operator: lessThan
	 * @see #gt
	 */
	IQueryBuilderCondition lt(Object value);

	/**
	 * Operator: greaterThanOrEqual
	 * @see #gt
	 */
	IQueryBuilderCondition ge(Object value);

	/**
	 * Operator: lessThanOrEqual
	 * @see #gt
	 */
	IQueryBuilderCondition le(Object value);

	/**
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
	 * Operator: isNull
	 * @see #gt
	 */
	IQueryBuilderCondition isNull();

	/**
	 * Operator: equals
	 * When compared to null, results in isNull
	 * @see #gt
	 */
	IQueryBuilderCondition eq(Object value);

	/**
	 * Operator: like
	 * @see #gt
	 */
	IQueryBuilderCondition like(String pattern);

	/**
	 * Operator: like, with escape character
	 * @see #gt
	 */
	IQueryBuilderCondition like(String pattern, char escape);

	IQueryBuilderColumn not();

	IQueryBuilderSort asc();

	IQueryBuilderSort desc();

	IQueryBuilderAggregate count();

	IQueryBuilderAggregate max();

	IQueryBuilderAggregate min();

	IQueryBuilderAggregate avg();

	IQueryBuilderAggregate sum();
}
