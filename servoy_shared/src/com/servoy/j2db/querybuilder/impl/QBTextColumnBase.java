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

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;

/**
 * This interface lists functions on test columns.
 *
 * @author rgansevles
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, realClass = QBTextColumn.class)
public interface QBTextColumnBase
{
	/**
	 * Create upper(column) expression
	 * @sample
	 * query.result.add(query.columns.custname.upper)
	 */
	@JSReadonlyProperty(debuggerRepresentation = "Query upper clause")
	QBTextColumnBase upper();

	/**
	 * Create lower(column) expression
	 * @sample
	 * query.result.add(query.columns.custname.lower)
	 */
	@JSReadonlyProperty(debuggerRepresentation = "Query lower clause")
	QBTextColumnBase lower();

	/**
	 * Create trim(column) expression
	 * @sample
	 * query.result.add(query.columns.custname.trim)
	 */
	@JSReadonlyProperty(debuggerRepresentation = "Query trim function")
	QBTextColumnBase trim();

	/**
	 * Create length(column) expression
	 * @sample
	 * query.result.add(query.columns.custname.len)
	 */
	@JSReadonlyProperty(debuggerRepresentation = "Query length clause")
	QBIntegerColumnBase len();

	/**
	 * Create substring(pos) expression
	 * @param pos
	 * @sample
	 * query.result.add(query.columns.mycol.substring(3))
	 */
	@JSFunction
	public QBTextColumnBase substring(int pos);

	/**
	 * Create substring(pos, len) expression
	 * @param pos
	 * @param len
	 * @sample
	 * query.result.add(query.columns.mycol.substring(3, 2))
	 */
	@JSFunction
	public QBTextColumnBase substring(int pos, int len);

	/**
	 * Create bit_length(column) expression
	 * @sample
	 * query.result.add(query.columns.custname.bit_length)
	 */
	@JSReadonlyProperty(debuggerRepresentation = "Query bit_length clause")
	public QBIntegerColumnBase bit_length();


	/**
	 * Create locate(arg) expression
	 * @param arg string to locate
	 * @sample
	 * query.result.add(query.columns.mycol.locate('sample'))
	 */
	@JSFunction
	public QBIntegerColumnBase locate(Object arg);

	/**
	 * Create locate(arg, start) expression
	 * @param arg string to locate
	 * @param start start pos
	 * @sample
	 * query.result.add(query.columns.mycol.locate('sample', 5))
	 */
	@JSFunction
	public QBIntegerColumnBase locate(Object arg, int start);


	/**
	 * Compare column with a value or another column.
	 * Operator: like
	 *
	 * @param pattern the string value of the pattern
	 *
	 * @sample
	 * query.where.add(query.columns.companyname.like('Serv%'))
	 *
	 * // case-insensitive compares can be done using the upper (or lower) functions,
	 * // this can be useful when using for example German letters like ß,
	 * query.where.add(query.columns.companyname.upper.like(query.functions.upper('groß%')))
	 */
	@JSFunction
	QBCondition like(Object pattern);

	/**
	 * Compare column with a value or another column.
	 * Operator: like, with escape character
	 *
	 * @param pattern the string value of the pattern
	 * @param escape the escape char
	 *
	 * @sample
	 * query.where.add(query.columns.companyname.like('X_%', '_'))
	 */
	@JSFunction
	QBCondition like(Object pattern, char escape);
}
