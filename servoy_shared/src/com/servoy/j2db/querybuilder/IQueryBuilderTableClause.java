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
 * Common interface for table based clauses in a Servoy Query Objects builder.
 * <p>Current instances: {@link IQueryBuilder}: query and {@link IQueryBuilderJoin} join.
 * 
 * @author rgansevles
 *
 * @since 6.1
 */
public interface IQueryBuilderTableClause extends IQueryBuilderPart
{
	/**
	 * @return the dataSource
	 */
	String getDataSource();

	/**
	 * @return the tableAlias
	 */
	String getTableAlias();

	/**
	 * Get a column from the table.
	 */
	IQueryBuilderColumn getColumn(String name) throws RepositoryException;

	/**
	 * Get a column from the table with given alias.
	 * <p>The alias may be of the main table or any level deep joined table.
	 */
	IQueryBuilderColumn getColumn(String tableAlias, String name) throws RepositoryException;

	/**
	 * Get the joins clause of this table based clause.
	 * <p>Joins added to this clause will be based on this table clauses table.
	 */
	IQueryBuilderJoins joins() throws RepositoryException;

}
