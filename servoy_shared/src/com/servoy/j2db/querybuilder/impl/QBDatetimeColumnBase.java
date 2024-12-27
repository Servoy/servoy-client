/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2024 Servoy BV

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

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;

/**
 * RAGTEST doc
 * @author rgansevles
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, scriptingName = "QBDatetimeColumn")
public interface QBDatetimeColumnBase
{

	/**
	 * Extract hour from date
	 * @sample
	 * query.result.add(query.columns.mydatecol.hour)
	 */
	@JSReadonlyProperty
	QBIntegerColumnBase hour();

	/**
	 * Extract second from date
	 * @sample
	 * query.result.add(query.columns.mydatecol.second)
	 */
	@JSReadonlyProperty
	public QBIntegerColumnBase second();

	/**
	 * Extract minute from date
	 * @sample
	 * query.result.add(query.columns.mydatecol.minute)
	 */
	@JSReadonlyProperty
	public QBIntegerColumnBase minute();

	/**
	 * Extract day from date
	 * @sample
	 * query.result.add(query.columns.mydatecol.day)
	 */
	@JSReadonlyProperty
	public QBIntegerColumnBase day();

	/**
	 * Extract month from date
	 * @sample
	 * query.result.add(query.columns.mydatecol.month)
	 */
	@JSReadonlyProperty
	public QBIntegerColumnBase month();

	/**
	 * Extract year from date
	 * @sample
	 * query.result.add(query.columns.mydatecol.year)
	 */
	@JSReadonlyProperty
	public QBIntegerColumnBase year();
}
