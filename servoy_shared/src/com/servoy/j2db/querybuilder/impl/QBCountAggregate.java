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
 * RAGTEST doc  count distinct()
 * @author rgansevles
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, extendsComponent = "QBIntegerColumn")
public interface QBCountAggregate
{

	/** Add a distinct qualifier to the aggregate
	 * @sample
	 * // count the number of countries that we ship orders to
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.result.add(query.columns.shipcountry.count.distinct);
	 */
	@JSReadonlyProperty
	default QBIntegerColumnBase distinct()
	{
		return ((QBAggregate)this).countDistinct();
	}
}
