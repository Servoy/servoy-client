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

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.documentation.ServoyDocumented;

/**
 * <p>The <code>SpecialOperators</code> class Returns the type of the given object, one of these strings get returned:
 * <code>number</code>, <code>string</code>, <code>boolean</code>, <code>object</code>, <code>function</code> or <code>undefined</code>
 * if the object doesn't exists.</p>
 *
 * @sample
 * application.output(typeof "abc"); // string
 * application.output(typeof 10); // number
 * application.output(typeof 10.1); // number
 * application.output(typeof true); // boolean
 * application.output(typeof parseInt); // function
 * application.output(typeof application); // object
 * application.output(typeof somethingInexisting); // undefined
 *
 * @link https://developer.mozilla.org/en/JavaScript/Reference/Operators/typeof
 * @simplifiedSignature
 * 
 * @author gerzse
 */
@ServoyDocumented(category = ServoyDocumented.JSLIB, publicName = "Special Operators")
@ServoyClientSupport(mc = true, wc = true, sc = true, ng = true)
public class SpecialOperators
{

	@JSFunction
	public void typeof()
	{
	}
}
