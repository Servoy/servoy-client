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

import com.servoy.j2db.querybuilder.impl.QBColumn;

/**
 * Functions to be used in queries.
 *
 * @author rgansevles
 *
 * @since 6.1
 */

public interface IQueryBuilderFunctions extends IQueryBuilderPart
{
	/**
	 * Get query builder parent.
	 */
	IQueryBuilder getParent();

	/** Cast using type name.
	 * @see QBColumn#cast(String)
	 * @param object value to case
	 * @param type type name
	 */
	IQueryBuilderColumn cast(Object object, String type);

	/**
	 * Create floor(column) expression
	 */
	IQueryBuilderColumn floor(Object arg);

	/**
	 * Create round(column) expression
	 */
	IQueryBuilderColumn round(Object arg);

	/**
	 * Create round(column, decimals) expression
	 */
	IQueryBuilderColumn round(Object arg, int decimals);

	/**
	 * Create ceil(column) expression
	 */
	IQueryBuilderColumn ceil(Object arg);

	/**
	 * Create upper(column) expression
	 */
	IQueryBuilderColumn upper(Object arg);

	/**
	 * Create abs(column) expression
	 */
	IQueryBuilderColumn abs(Object arg);

	/**
	 * Create sqrt(column) expression
	 */
	IQueryBuilderColumn sqrt(Object arg);

	/**
	 * Create lower(column) expression
	 */
	IQueryBuilderColumn lower(Object arg);

	/**
	 * Create trim(column) expression
	 */
	IQueryBuilderColumn trim(Object arg);

	/**
	 * Create trim([leading | trailing | both] [characters] from column)
	 */
	IQueryBuilderColumn trim(String leading_trailing_both, String characters, String fromKeyword, Object value);

	/**
	 * Create length(column) expression
	 */
	IQueryBuilderColumn len(Object arg);

	default IQueryBuilderColumn length(Object arg)
	{
		return len(arg);
	}

	/**
	 * Create bit_length(column) expression
	 */
	IQueryBuilderColumn bit_length(Object arg);

	/**
	 * Create substring(column, pos) expression
	 */
	IQueryBuilderColumn substring(Object arg, int pos);

	/**
	 * Create substring(column, pos, len) expression
	 */
	IQueryBuilderColumn substring(Object arg, int pos, int len);

	/**
	 * Create locate(str1, str2) expression
	 */
	IQueryBuilderColumn locate(Object str1, Object str2);

	/**
	 * Create locate(str1, str2, start) expression
	 */
	IQueryBuilderColumn locate(Object arg1, Object arg2, int start);

	/**
	 * Create nullif(arg1, arg2) expression
	 */
	IQueryBuilderColumn nullif(Object arg1, Object arg2);

	/**
	 * Create mod(dividend, divisor) expression
	 */
	IQueryBuilderColumn mod(Object dividend, Object divisor);

	/**
	 * Create plus(args, arg2) expression
	 */
	IQueryBuilderColumn plus(Object arg1, Object arg2);

	/**
	 * Create minus(args, arg2) expression
	 */
	IQueryBuilderColumn minus(Object arg1, Object arg2);

	/**
	 * Create multiply(args, arg2) expression
	 */
	IQueryBuilderColumn multiply(Object arg1, Object arg2);

	/**
	 * Create divide(args, arg2) expression
	 */
	IQueryBuilderColumn divide(Object arg1, Object arg2);

	/**
	 * Create concat(args, arg2) expression
	 */
	IQueryBuilderColumn concat(Object arg1, Object arg2);

	/**
	 * Create second(date) expression
	 */
	IQueryBuilderColumn second(Object date);

	/**
	 * Create minute(date) expression
	 */
	IQueryBuilderColumn minute(Object date);

	/**
	 * Create hour(date) expression
	 */
	IQueryBuilderColumn hour(Object date);

	/**
	 * Create day(date) expression
	 */
	IQueryBuilderColumn day(Object date);

	/**
	 * Create month(date) expression
	 */
	IQueryBuilderColumn month(Object date);

	/**
	 * Create year(date) expression
	 */
	IQueryBuilderColumn year(Object date);

	/**
	 * Create coalesce(arg1, arg2, ...) expression
	 */
	IQueryBuilderColumn coalesce(Object... args);

	/**
	 * Create a call to a custom-defined function name(arg1, arg2, ...).
	 * Note that the function has to exist in the actual database, this may not be working cross-database.
	 */
	IQueryBuilderColumn custom(String name, Object... args);
}
