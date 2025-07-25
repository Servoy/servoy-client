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

import com.servoy.j2db.documentation.ServoyDocumented;

/**
 * <p>The <code>QBGenericColumn</code> class represents a generic (untyped) column in a <code>QBSelect</code> query.
 *
 * <p>For more information about constructing and executing queries and columns, refer to the
 * <a href="https://docs.servoy.com/reference/servoycore/dev-api/database-manager/qbcolumn">QBSelect</a> section of this documentation.</p>
 *
 * @author rgansevles
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, extendsComponent = "QBColumn")
public interface QBGenericColumn extends QBGenericColumnBase, QBDatetimeColumnBase, QBIntegerColumnBase, QBNumberColumnFunctions<QBGenericColumn>,
	QBMediaColumnBase, QBArrayColumnBase, QBNumberColumnBase, QBTextColumnBase, QBColumnBaseFunctions<QBGenericColumn>
{
}
