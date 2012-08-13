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
	 * Add a column to the query result.
	 * <pre>
	 * query.result().add(query.getColumn("value1")).add(query.getColumn("value2"));
	 * </pre>
	 */
	IQueryBuilderResult add(IQueryBuilderColumn column) throws RepositoryException;

	/**
	 * Add a fixed value to the query result.
	 * <pre>
	 *  // select 100, value2 from tab
	 * query.result().addValue(new Integer(100)).add(query.getColumn("value2"));
	 * </pre>
	 */
	IQueryBuilderResult addValue(Object value) throws RepositoryException;

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
}
