/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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

/**
 * Dummy class for listing methods for JavaScript types in a manner that
 * suits our documentation generator.
 *
 * @author gerzse
 */
@ServoyDocumented(category = ServoyDocumented.JSLIB, publicName = "Object", scriptingName = "Object")
@ServoyClientSupport(mc = true, wc = true, sc = true, ng = true)
public class Object
{
	/**
	 * Determines whether two values are the same value.
	 *
	 * @sample
	 * Object.is('foo', 'foo');
	 *
	 * @param value1 The first value to compare.
	 * @param value2 The second value to compare.
	 * @return a Boolean indicating whether or not the two arguments are the same value.
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Object/is
	 */
	public static boolean js_is(Object value1, Object value2)
	{
		return false;
	}

	/**
	 * Copy the values of all enumerable own properties from one or more source objects to a target object.
	 *
	 * @sample
	 * var object1 = { a: 1, b: 2, c: 3};
	 * var object2 = Object.assign({c: 4, d: 5}, object1);
	 * application.output(object2.c, object2.d);
	 *
	 * @param target The target object.
	 * @param sources The source object(s).
	 * @return The target object.
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Object/assign
	 */
	public static Object js_assign(Object target, Object... sources)
	{
		return null;
	}
}
