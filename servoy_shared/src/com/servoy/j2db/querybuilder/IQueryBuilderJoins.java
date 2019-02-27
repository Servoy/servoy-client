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
 * Joins section in a Servoy Query Objects builder.
 *
 * @author rgansevles
 *
 * @since 6.1
 */
public interface IQueryBuilderJoins extends IQueryBuilderPart
{
	/**
	 * Add a join clause from the parent query builder part to the specified data source.
	 *
	 * @param dataSource data source
	 * @param joinType join type, one of {@link IQueryBuilderJoin#LEFT_OUTER_JOIN}, {@link IQueryBuilderJoin#INNER_JOIN}, {@link IQueryBuilderJoin#RIGHT_OUTER_JOIN}, {@link IQueryBuilderJoin#FULL_JOIN}
	 * @param alias alias for joining table
	 *  <pre>
	 * query.joins().add(detailDataSource,  IQueryBuilderJoin.LEFT_OUTER_JOIN, "detail")
	 *     .on().add(query.getColumn("pk").eq(query.getColumn("detail", "fk")));
	 * </pre>
	 */
	IQueryBuilderJoin add(String dataSource, int joinType, String alias) throws RepositoryException;

	/**
	 * Add a join with no alias for the joining table.
	 *
	 * @see #add(String, int, String)
	 */
	IQueryBuilderJoin add(String dataSource, int joinType) throws RepositoryException;

	/**
	 * Add a join based on relation or add a manual join.
	 * When dataSourceOrRelation is a relation name, a join will be added based on the relation.
	 * When dataSourceOrRelation is a data source, an empty join will be added with join type {@link IQueryBuilderJoin#LEFT_OUTER_JOIN}.
	 * @param dataSourceOrRelation add join based on relation or data source
	 * @param alias alias, when null default is used
	 *
	 * @see #add(String, int, String)
	 */
	IQueryBuilderJoin add(String dataSourceOrRelation, String alias) throws RepositoryException;

	/**
	 * Add a join with join type {@link IQueryBuilderJoin#LEFT_OUTER_JOIN} and no alias for the joining table.
	 *
	 * @see #add(String, int, String)
	 */
	IQueryBuilderJoin add(String dataSource) throws RepositoryException;

	/**
	 * Add a join clause from the parent query builder part to a derived table based on another query.
	 *
	 * @see #add(IQueryBuilder, int, String)
	 */
	IQueryBuilderJoin add(IQueryBuilder subqueryBuilder, int joinType);

	/**
	 * Add a join clause from the parent query builder part to a derived table based on another query.
	 *
	 * @param subqueryBuilder
	 * @param joinType join type, one of {@link IQueryBuilderJoin#LEFT_OUTER_JOIN}, {@link IQueryBuilderJoin#INNER_JOIN}, {@link IQueryBuilderJoin#RIGHT_OUTER_JOIN}, {@link IQueryBuilderJoin#FULL_JOIN}
	 * @param alias alias for joining derived table
	 *  <pre>
	 * IQueryBuilderJoin join = query.joins().add(subquery, IQueryBuilderJoin.LEFT_OUTER_JOIN, "detail");
	 * join.on().add(query.getColumn("pk").eq(join.getColumn("fk")));
	 * </pre>
	 */
	IQueryBuilderJoin add(IQueryBuilder subqueryBuilder, int joinType, String alias);

}
