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
 * This interface lists functions on number columns.
 *
 * @author rgansevles
 *
 */
@JSRealClass(QBNumberColumn.class)
public interface QBNumberColumnBase extends IQueryBuilderColumn
{
	/**
	 * Create floor(column) expression
	 * @sample
	 * query.result.add(query.columns.mycol.floor)
	 *
	 *  @return a QBIntegerColumn representing the floor expression for the column.
	 */
	@JSReadonlyProperty(debuggerRepresentation = "Query floor function")
	public QBIntegerColumnBase floor();


	/**
	 * Create round(column) expression
	 * @sample
	 * query.result.add(query.columns.mycol.round)
	 *
	 *  @return a QBIntegerColumn representing the round expression for the column.
	 */
	@JSReadonlyProperty(debuggerRepresentation = "Query round function")
	public QBIntegerColumnBase round();


	/**
	 * Create ceil(column) expression
	 * @sample
	 * query.result.add(query.columns.mycol.ceil)
	 *
	 *  @return a QBIntegerColumn representing the ceil expression for the column.
	 */
	@JSReadonlyProperty(debuggerRepresentation = "Query ceil function")
	public QBIntegerColumnBase ceil();
}
