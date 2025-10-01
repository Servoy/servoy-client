/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2022 Servoy BV

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

package com.servoy.j2db.documentation.scripting.docs;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;

/**
 * <p>
 * The <code>IterableValue</code> object is designed for use in JavaScript loops to streamline iteration processes.
 * It provides access to the current value of the iterable and indicates whether the iteration is complete.
 * </p>
 *
 * @author jcompanger
 * @since 2022.12
 */
@ServoyDocumented(category = ServoyDocumented.JSLIB, publicName = "IterableValue", scriptingName = "IterableValue")
@ServoyClientSupport(mc = true, wc = true, sc = true, ng = true)
public class IterableValue
{
	/**
	 * This is used in Iterators (for of loops)
	 * This returns the value of the current Iterable
	 */
	@JSReadonlyProperty
	public Object getValue()
	{
		return null;
	}

	/**
	 * This returns a boolean if this was the last value (true) or not.
	 */
	@JSReadonlyProperty
	public boolean getDone()
	{
		return false;
	}

}
