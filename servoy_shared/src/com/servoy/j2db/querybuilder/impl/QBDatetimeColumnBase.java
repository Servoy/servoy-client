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

import com.servoy.j2db.querybuilder.IQueryBuilderColumn;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;
import com.servoy.j2db.scripting.annotations.JSRealClass;

/**
 * This interface lists functions on datetime columns.
 *
 * @author rgansevles
 *
 */
@JSRealClass(QBDatetimeColumn.class)
public interface QBDatetimeColumnBase extends IQueryBuilderColumn
{

	/**
	 * Extract hour from date
	 * @sample
	 * query.result.add(query.columns.mydatecol.hour)
	 *
	 *  @return a QBIntegerColumn representing the extraction of the hour from a date.
	 */
	@JSReadonlyProperty(debuggerRepresentation = "Query timestamp hour function")
	QBIntegerColumnBase hour();

	/**
	 * Extract second from date
	 * @sample
	 * query.result.add(query.columns.mydatecol.second)
	 *
	 *  @return a QBIntegerColumn representing the extraction of the second from a date.
	 */
	@JSReadonlyProperty(debuggerRepresentation = "Query timestamp second function")
	public QBIntegerColumnBase second();

	/**
	 * Extract minute from date
	 * @sample
	 * query.result.add(query.columns.mydatecol.minute)
	 *
	 *  @return a QBIntegerColumn representing the extraction of the minute from a date.
	 */
	@JSReadonlyProperty(debuggerRepresentation = "Query timestamp minute function")
	public QBIntegerColumnBase minute();

	/**
	 * Extract day from date
	 * @sample
	 * query.result.add(query.columns.mydatecol.day)
	 *
	 *  @return a QBIntegerColumn representing the extraction of the day from a date.
	 */
	@JSReadonlyProperty(debuggerRepresentation = "Query timestamp day function")
	public QBIntegerColumnBase day();

	/**
	 * Extract month from date
	 * @sample
	 * query.result.add(query.columns.mydatecol.month)
	 *
	 *  @return a QBIntegerColumn representing the extraction of the month from a date.
	 */
	@JSReadonlyProperty(debuggerRepresentation = "Query timestamp month function")
	public QBIntegerColumnBase month();

	/**
	 * Extract year from date
	 * @sample
	 * query.result.add(query.columns.mydatecol.year)
	 *
	 *  @return a QBIntegerColumn representing the extraction of the year from a date.
	 */
	@JSReadonlyProperty(debuggerRepresentation = "Query timestamp year function")
	public QBIntegerColumnBase year();
}
