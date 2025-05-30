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
 * Results section in Servoy Query Objects.
 *
 * @author rgansevles
 *
 * @since 6.1
 */

public interface IQueryBuilderResult extends IQueryBuilderPart
{
	/**
	 * Get query builder parent.
	 */
	IQueryBuilder getParent();

	/**
	 * Add the tables' primary pk columns in alphabetical order to the query result.
	 */
	IQueryBuilderResult addPk() throws RepositoryException;

	/**
	 * Add a column by name to the query result.
	 */
	IQueryBuilderResult add(String columnName) throws RepositoryException;

	/**
	 * Add a column with alias by name to the query result.
	 */
	IQueryBuilderResult add(String columnName, String alias) throws RepositoryException;

	/**
	 * Add a column to the query result.
	 * <pre>
	 * query.result().add(query.getColumn("value1")).add(query.getColumn("value2"));
	 * </pre>
	 */
	IQueryBuilderResult add(IQueryBuilderColumn column) throws RepositoryException;

	/**
	 * Add a column with an to the query result.
	 * <pre>
	 * query.result().add(query.getColumn("value1", "myalias1")).add(query.getColumn("value2", "myalias1"));
	 * </pre>
	 */
	IQueryBuilderResult add(IQueryBuilderColumn column, String alias) throws RepositoryException;

	/**
	 * Add a fixed value to the query result.
	 * <pre>
	 *  // select 100, value2 from tab
	 * query.result().addValue(new Integer(100)).add(query.getColumn("value2"));
	 * </pre>
	 */
	IQueryBuilderResult addValue(Object value) throws RepositoryException;

	/**
	 * Add a fixed value with alias to the query result.
	 * <pre>
	 *  // select 100 as myval, value2 from tab
	 * query.result().addValue(new Integer(100), "myval").add(query.getColumn("value2"));
	 * </pre>
	 */
	IQueryBuilderResult addValue(Object value, String alias) throws RepositoryException;

	/**
	 * Add a custom subquery to the query result.
	 * <pre>
	 *  // make sure the subquery returns exactly 1 value.
	 *  // select (select max from othertab where val = 'test') from tab
	 * query.result().addSubSelect("select max from othertab where val = ?", new Object[] { "test" }));
	 * </pre>
	 */
	IQueryBuilderResult addSubSelect(String customQuery, Object[] args) throws RepositoryException;

	/**
	 * Add a custom subquery with alias to the query result.
	 * <pre>
	 *  // make sure the subquery returns exactly 1 value.
	 *  // select (select max from othertab where val = 'test') as mx from tab
	 * query.result().addSubSelect("select max from othertab where val = ?", new Object[] { "test" }, "mx"));
	 * </pre>
	 */
	IQueryBuilderResult addSubSelect(String customQuery, Object[] args, String alias) throws RepositoryException;

	/**
	 * Add a subquery with alias to the query result.
	 * <pre>
	 *  // make sure the subquery returns exactly 1 value.
	 * query.result().addSubSelect(query2, "subval"));
	 * </pre>
	 */
	public IQueryBuilderResult addSubSelect(IQueryBuilder query, String alias) throws RepositoryException;

	/**
	 * Add a subquery to the query result.
	 * <pre>
	 *  // make sure the subquery returns exactly 1 value.
	 * query.result().addSubSelect(query2));
	 * </pre>
	 */
	public IQueryBuilderResult addSubSelect(IQueryBuilder query) throws RepositoryException;

	/**
	 * Set the distinct flag for the query.
	 */
	IQueryBuilderResult setDistinct(boolean distinct) throws RepositoryException;

	/**
	 * Get the distinct flag for the query.
	 */
	boolean isDistinct() throws RepositoryException;

	/**
	 * Clear the columns in the query result.
	 */
	IQueryBuilderResult clear();

	/**
	 * Remove a column by name from the query result.
	 */
	IQueryBuilderResult remove(String name) throws RepositoryException;

	/**
	 * Remove a column from the query result.
	 * <pre>
	 * query.result().remove(query.getColumn("value1"));
	 * </pre>
	 */
	IQueryBuilderResult remove(IQueryBuilderColumn column) throws RepositoryException;

}
