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

package com.servoy.j2db.querybuilder;

import com.servoy.base.query.IQueryConstants;

/**
 * Script constants for Query Builder column types.
 *
 * @author rgansevles
 */
public interface IQueryBuilderConstants
{
	/**
	 * Constant used for casting.
	 *
	 * @sampleas com.servoy.j2db.querybuilder.impl.QBFunctions#cast(Object, String)
	 */
	public static final String TYPE_BIG_INTEGER = IQueryConstants.TYPE_BIG_INTEGER;

	/**
	 * @sameas TYPE_BIG_INTEGER
	 */
	public static final String TYPE_BINARY = IQueryConstants.TYPE_BINARY;

	/**
	 * @sameas TYPE_BIG_INTEGER
	 */
	public static final String TYPE_BOOLEAN = IQueryConstants.TYPE_BOOLEAN;

	/**
	 * @sameas TYPE_BIG_INTEGER
	 */
	public static final String TYPE_CHARACTER = IQueryConstants.TYPE_CHARACTER;

	/**
	 * @sameas TYPE_BIG_INTEGER
	 */
	public static final String TYPE_DATE = IQueryConstants.TYPE_DATE;

	/**
	 * @sameas TYPE_BIG_INTEGER
	 */
	public static final String TYPE_DOUBLE = IQueryConstants.TYPE_DOUBLE;

	/**
	 * @sameas TYPE_BIG_INTEGER
	 */
	public static final String NUMERIC = IQueryConstants.NUMERIC;

	/**
	 * @sameas TYPE_BIG_INTEGER
	 */
	public static final String TYPE_INTEGER = IQueryConstants.TYPE_INTEGER;

	/**
	 * @sameas TYPE_BIG_INTEGER
	 */
	public static final String TYPE_SHORT = IQueryConstants.TYPE_SHORT;

	/**
	 * @sameas TYPE_BIG_INTEGER
	 */
	public static final String TYPE_BYTE = IQueryConstants.TYPE_BYTE;

	/**
	 * @sameas TYPE_BIG_INTEGER
	 */
	public static final String TYPE_TIME = IQueryConstants.TYPE_TIME;

	/**
	 * @sameas TYPE_BIG_INTEGER
	 */
	public static final String TYPE_TIMESTAMP = IQueryConstants.TYPE_TIMESTAMP;

	/**
	 * @sameas TYPE_BIG_INTEGER
	 */
	public static final String TYPE_STRING = IQueryConstants.TYPE_STRING;

	/**
	 * @sameas TYPE_BIG_INTEGER
	 */
	public static final String TYPE_TEXT = IQueryConstants.TYPE_TEXT;

	/**
	 * @sameas TYPE_BIG_INTEGER
	 */
	public static final String TYPE_IMAGE = IQueryConstants.TYPE_IMAGE;

	/**
	 * @sameas TYPE_BIG_INTEGER
	 */
	public static final String TYPE_BIG_DECIMAL = IQueryConstants.TYPE_BIG_DECIMAL;

	/**
	 * @sameas TYPE_BIG_INTEGER
	 */
	public static final String TYPE_BLOB = IQueryConstants.TYPE_BLOB;

	/**
	 * @sameas TYPE_BIG_INTEGER
	 */
	public static final String TYPE_CLOB = IQueryConstants.TYPE_CLOB;

	/**
	 * @sameas TYPE_BIG_INTEGER
	 */
	public static final String TYPE_FLOAT = IQueryConstants.TYPE_FLOAT;

}