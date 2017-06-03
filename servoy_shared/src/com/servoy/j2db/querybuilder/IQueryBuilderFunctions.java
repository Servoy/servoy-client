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
	 * @see IQueryBuilderFunction
	 * @see IQueryBuilderColumn#cast(String)
	 * @param object value to case
	 * @param type type name
	 */
	IQueryBuilderFunction cast(Object object, String type);

	/**
	 * Create floor(column) expression
	 */
	IQueryBuilderFunction floor(Object arg);

	/**
	 * Create round(column) expression
	 */
	IQueryBuilderFunction round(Object arg);

	/**
	 * Create ceil(column) expression
	 */
	IQueryBuilderFunction ceil(Object arg);

	/**
	 * Create upper(column) expression
	 */
	IQueryBuilderFunction upper(Object arg);

	/**
	 * Create abs(column) expression
	 */
	IQueryBuilderFunction abs(Object arg);

	/**
	 * Create sqrt(column) expression
	 */
	IQueryBuilderFunction sqrt(Object arg);

	/**
	 * Create lower(column) expression
	 */
	IQueryBuilderFunction lower(Object arg);

	/**
	 * Create trim(column) expression
	 */
	IQueryBuilderFunction trim(Object arg);

	/**
	 * Create trim([leading | trailing | both] [characters] from column)
	 */
	IQueryBuilderFunction trim(String leading_trailing_both, String characters, String fromKeyword, Object value);

	/**
	 * Create length(column) expression
	 */
	IQueryBuilderFunction length(Object arg);

	/**
	 * Create bit_length(column) expression
	 */
	IQueryBuilderFunction bit_length(Object arg);

	/**
	 * Create substring(column, pos) expression
	 */
	IQueryBuilderFunction substring(Object arg, int pos);

	/**
	 * Create substring(column, pos, len) expression
	 */
	IQueryBuilderFunction substring(Object arg, int pos, int len);

	/**
	 * Create locate(str1, str2) expression
	 */
	IQueryBuilderFunction locate(Object str1, Object str2);

	/**
	 * Create locate(str1, str2, start) expression
	 */
	IQueryBuilderFunction locate(Object arg1, Object arg2, int start);

	/**
	 * Create nullif(arg1, arg2) expression
	 */
	IQueryBuilderFunction nullif(Object arg1, Object arg2);

	/**
	 * Create mod(dividend, divisor) expression
	 */
	IQueryBuilderFunction mod(Object dividend, Object divisor);

	/**
	 * Create plus(args, arg2) expression
	 */
	IQueryBuilderFunction plus(Object arg1, Object arg2);

	/**
	 * Create minus(args, arg2) expression
	 */
	IQueryBuilderFunction minus(Object arg1, Object arg2);

	/**
	 * Create multiply(args, arg2) expression
	 */
	IQueryBuilderFunction multiply(Object arg1, Object arg2);

	/**
	 * Create divide(args, arg2) expression
	 */
	IQueryBuilderFunction divide(Object arg1, Object arg2);

	/**
	 * Create concat(args, arg2) expression
	 */
	IQueryBuilderFunction concat(Object arg1, Object arg2);

	/**
	 * Create second(date) expression
	 */
	IQueryBuilderFunction second(Object date);

	/**
	 * Create minute(date) expression
	 */
	IQueryBuilderFunction minute(Object date);

	/**
	 * Create hour(date) expression
	 */
	IQueryBuilderFunction hour(Object date);

	/**
	 * Create day(date) expression
	 */
	IQueryBuilderFunction day(Object date);

	/**
	 * Create month(date) expression
	 */
	IQueryBuilderFunction month(Object date);

	/**
	 * Create year(date) expression
	 */
	IQueryBuilderFunction year(Object date);

	/**
	 * Create coalesce(arg1, arg2, ...) expression
	 */
	IQueryBuilderFunction coalesce(Object... args);

}
