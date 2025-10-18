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

/**
 * RAGTEST doc
 * @author rgansevles
 *
 */
public enum QBNullPrecedence
{
	/**
	 * RAGTEST doc
	 * Constant for the joinType of a join or a relation. It is also used in solutionModel.newRelation(...) and in the QueryBuilder.
	 *
	 * @sample
	 * 	var query = datasources.db.example_data.orders.createSelect();
	 *  /** @type {QBJoin<db:/example_data/order_details>} *&#47;
	 * 	var join = query.joins.add('db:/example_data/order_details', QBJoin.INNER_JOIN, 'odetail')
	 * 	join.on.add(join.columns.orderid.eq(query.columns.orderid))
	 */
	nullsFirst,

	/**
	 * @sameas INNER_JOIN
	 * @see #INNER_JOIN
	 */
	nullsLast,

	/**
	 * @sameas INNER_JOIN
	 * @see #INNER_JOIN
	 */
	databaseDefault,


}
