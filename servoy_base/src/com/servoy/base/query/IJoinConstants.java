/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.base.query;

/**
 * Constants useful when dealing with SQL joins.
 *
 * @author acostescu
 *
 */
public interface IJoinConstants
{
	/**
	 * Constant for the joinType of a join or a relation. It is also used in solutionModel.newRelation(...) and in the QueryBuilder.
	 *
	 * @sample
	 * 	var query = datasources.db.example_data.orders.createSelect();
	 *  /** @type {QBJoin<db:/example_data/order_details>} *&#47;
	 * 	var join = query.joins.add('db:/example_data/order_details', QBJoin.INNER_JOIN, 'odetail')
	 * 	join.on.add(join.columns.orderid.eq(query.columns.orderid))
	 */
	public static final int INNER_JOIN = 0;

	/**
	 * @sameas INNER_JOIN
	 * @see #INNER_JOIN
	 */
	public static final int LEFT_OUTER_JOIN = 1;

	/**
	 * @sameas INNER_JOIN
	 * @see #INNER_JOIN
	 */
	public static final int RIGHT_OUTER_JOIN = 2;

	/**
	 * @sameas INNER_JOIN
	 * @see #INNER_JOIN
	 */
	public static final int FULL_JOIN = 3;

}
