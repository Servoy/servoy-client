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

import com.servoy.j2db.querybuilder.IQueryBuilderColumn;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;
import com.servoy.j2db.scripting.annotations.JSRealClass;

/**
 * This interface lists functions on text columns.
 *
 * @author rgansevles
 *
 */
@JSRealClass(QBTextColumn.class)
public interface QBTextColumnBase extends IQueryBuilderColumn
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
	 *
	 * @return the QBTextColumn that can be added to the result.
	 */
	@JSFunction
	public QBTextColumnBase substring(int pos);

	/**
	 * Create substring(pos, len) expression
	 * @param pos
	 * @param len
	 * @sample
	 * query.result.add(query.columns.mycol.substring(3, 2))
	 *
	 * @return the QBTextColumn that can be added to the result.
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
	 *
	 * @return the QBIntegerColumn that can be added to the result.
	 */
	@JSFunction
	public QBIntegerColumnBase locate(Object arg);

	/**
	 * Create locate(arg, start) expression
	 * @param arg string to locate
	 * @param start start pos
	 * @sample
	 * query.result.add(query.columns.mycol.locate('sample', 5))
	 *
	 * @return the QBIntegerColumn that can be added to the result.
	 */
	@JSFunction
	public QBIntegerColumnBase locate(Object arg, int start);
}
