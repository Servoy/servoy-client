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
package com.servoy.j2db.scripting.info;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.IPrefixedConstantsObject;

@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class ARGUMENTS implements IPrefixedConstantsObject
{

	/**
	 * The number of arguments passed to the function.
	 * 
	 * @sample
	 * function addNumbers(x, y) {
	 * 	if (arguments.length == addNumbers.length) {
	 * 		return (x + y);
	 * 	} 
	 * 	else {
	 * 		return 0;
	 * 	}
	 * }
	 * var result = addNumbers(3, 4, 5);
	 * application.output(result); // returns 0
	 * result = addNumbers(3, 4);
	 * application.output(result); // returns 7
	 * result = addNumbers(103, 104);
	 * application.output(result); // returns 207
	 * 
	 * @link https://developer.mozilla.org/en/JavaScript/Reference/Functions_and_function_scope/arguments/length
	 * 
	 */
	public static final String length = "length"; //$NON-NLS-1$

	/**
	 * The currently executing function.
	 * 
	 * @sample
	 * function makeFactorialFunc() {
	 * 	application.output('making a factorial function!');
	 * 	return function(x) {
	 * 		if (x <= 1)
	 * 			return 1;
	 * 		return x * arguments.callee(x - 1);
	 * 	};
	 * }
	 * var result = makeFactorialFunc()(5); // returns 120 (5 * 4 * 3 * 2 * 1)
	 * application.output("Result = " + result);
	 * 
	 * @link https://developer.mozilla.org/en/JavaScript/Reference/Functions_and_function_scope/arguments/callee
	 * 
	 */
	public static final String callee = "callee"; //$NON-NLS-1$

	public static String getArgPrefix()
	{
		return "arguments"; //$NON-NLS-1$
	}

	/**
	 * @see com.servoy.j2db.scripting.IPrefixedConstantsObject#getPrefix()
	 */
	public String getPrefix()
	{
		return "arguments"; //$NON-NLS-1$
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "arguments object"; //$NON-NLS-1$
	}


}
