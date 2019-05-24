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

import com.servoy.base.query.IJoinConstants;

/**
 * Join clause in Servoy Query Objects.
 *
 * @author rgansevles
 *
 * @since 6.1
 */

public interface IQueryBuilderJoin extends IQueryBuilderTableClause
{
	/**
	 * Constant for join types.
	 * @see #on()
	 */
	static final int INNER_JOIN = IJoinConstants.INNER_JOIN;

	/**
	 * Constant for join types.
	 * @see #on()
	 */
	static final int LEFT_OUTER_JOIN = IJoinConstants.LEFT_OUTER_JOIN;

	/**
	 * Constant for join types.
	 * @see #on()
	 */
	static final int FULL_JOIN = IJoinConstants.FULL_JOIN;

	/**
	 * Constant for join types.
	 * @see #on()
	 */
	static final int RIGHT_OUTER_JOIN = IJoinConstants.RIGHT_OUTER_JOIN;

	/**
	 * Get the on clause for the join.
	 * <pre>
	 * query.joins().add(detailDataSource,  IQueryBuilderJoin.LEFT_OUTER_JOIN, "detail")
	 *     .on().add(query.getColumn("pk").eq(query.getColumn("detail", "fk")));
	 * </pre>
	 */
	IQueryBuilderLogicalCondition on();
}
