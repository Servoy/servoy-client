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

import com.servoy.j2db.documentation.ServoyDocumented;

@ServoyDocumented(category = ServoyDocumented.JSLIB, publicName = "Special Operators")
public class SpecialOperators
{
	/**
	 * Returns the type of the given object, one of these get returned: number, string, boolean, object, function, undefined.
	 *
	 * @sample
	 * application.output(typeof("abc")); // string
	 * application.output(typeof(10)); // number
	 * application.output(typeof(10.1)); // number
	 * application.output(typeof(true)); // boolean
	 * application.output(typeof(parseInt)); // function
	 * application.output(typeof(application)); // object
	 * application.output(typeof(somethingInexisting)); // undefined
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Operators/Special_Operators/typeof_Operator
	 */
	public void js_typeof()
	{
	}
}
