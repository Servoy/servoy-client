/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2021 Servoy BV

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
 * Script constants for Query Builder column types.
 *
 * @author acostescu, rgansevles
 */
@SuppressWarnings("nls")
public interface IQueryConstants
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


	/**
	 * Constant used for casting.
	 *
	 * @sampleas com.servoy.j2db.querybuilder.impl.QBFunctions#cast(Object, String)
	 */
	public static final String TYPE_BIG_INTEGER = "big_integer";

	/**
	 * @sameas TYPE_BIG_INTEGER
	 */
	public static final String TYPE_BINARY = "binary";

	/**
	 * @sameas TYPE_BIG_INTEGER
	 */
	public static final String TYPE_BOOLEAN = "boolean";

	/**
	 * @sameas TYPE_BIG_INTEGER
	 */
	public static final String TYPE_CHARACTER = "character";

	/**
	 * @sameas TYPE_BIG_INTEGER
	 */
	public static final String TYPE_DATE = "date";

	/**
	 * @sameas TYPE_BIG_INTEGER
	 */
	public static final String TYPE_DOUBLE = "double";

	/**
	 * @sameas TYPE_BIG_INTEGER
	 */
	public static final String TYPE_INTEGER = "integer";

	/**
	 * @sameas TYPE_BIG_INTEGER
	 */
	public static final String TYPE_SHORT = "short";

	/**
	 * @sameas TYPE_BIG_INTEGER
	 */
	public static final String TYPE_BYTE = "byte";

	/**
	 * @sameas TYPE_BIG_INTEGER
	 */
	public static final String TYPE_TIME = "time";

	/**
	 * @sameas TYPE_BIG_INTEGER
	 */
	public static final String TYPE_TIMESTAMP = "timestamp";

	/**
	 * @sameas TYPE_BIG_INTEGER
	 */
	public static final String TYPE_STRING = "string";

	/**
	 * @sameas TYPE_BIG_INTEGER
	 */
	public static final String TYPE_TEXT = "text";

	/**
	 * @sameas TYPE_BIG_INTEGER
	 */
	public static final String TYPE_IMAGE = "image";

	/**
	 * @sameas TYPE_BIG_INTEGER
	 */
	public static final String TYPE_BIG_DECIMAL = "big_decimal";

	/**
	 * @sameas TYPE_BIG_INTEGER
	 */
	public static final String TYPE_BLOB = "blob";

	/**
	 * @sameas TYPE_BIG_INTEGER
	 */
	public static final String TYPE_CLOB = "clob";

	/**
	 * @sameas TYPE_BIG_INTEGER
	 */
	public static final String TYPE_FLOAT = "float";

}