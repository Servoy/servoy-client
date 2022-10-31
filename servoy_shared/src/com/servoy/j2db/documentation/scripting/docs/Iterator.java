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

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.documentation.ServoyDocumented;

/**
 * @author jcompanger
 * @since 2022.12
 */
@ServoyDocumented(category = ServoyDocumented.JSLIB, publicName = "Iterator", scriptingName = "Iterator")
@ServoyClientSupport(mc = true, wc = true, sc = true, ng = true)
public class Iterator
{
	/**
	 * An iterator is an object which defines a sequence and potentially a return value upon its termination
	 * Returns the next IterableValue  for this Iterable
	 *
	 * @sample for(var entry of set.value()) {}
	 *
	 * @return the next IterableValue
	 */
	@JSFunction
	public IterableValue next()
	{
		return null;
	}
}
