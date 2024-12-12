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

package com.servoy.j2db.dataprocessing;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;

/**
 * <p><code>JSBaseRecord</code> serves as the foundational type for a record, representing a single
 * row within a foundset. This includes various foundset types such as <code>JSFoundSet</code>,
 * <code>ViewFoundSet</code>, or <code>MenuFoundSet</code>.</p>
 *
 * <p>Each <code>JSBaseRecord</code> is associated with a foundset, providing access to the dataset
 * it belongs to. This association allows manipulation and interaction with the data structure.</p>
 *
 * <p>The <code>getDataSource()</code> method retrieves the datasource linked to the record,
 * offering insights into the origin of the data.</p>
 *
 * <p>For more details, refer to the <a href="./jsrecord.md">JSRecord</a> documentation.</p>
 *
 * @author lvostinar
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "JSBaseRecord", scriptingName = "JSBaseRecord")
public interface IJSBaseRecord
{
	@JSReadonlyProperty
	IJSBaseFoundSet getFoundset();

	@JSFunction
	String getDataSource();
}
