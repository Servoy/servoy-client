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
@ServoyDocumented(category = ServoyDocumented.JSLIB, publicName = "Number", scriptingName = "Number")
@ServoyClientSupport(mc = true, wc = true, sc = true, ng = true)
public class Number
{
	/**
	 * The largest representable number. 
	 * 
	 * @sample
	 * application.output("Largest number: " + Number.MAX_VALUE);
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Number/MAX_VALUE
	 */
	public Number js_getMAX_VALUE()
	{
		return null;
	}

	public void js_setMAX_VALUE(Number mAXVALUE)
	{
	}

	/**
	 * The smallest representable number. 
	 * 
	 * @sample
	 * application.output("Smallest number: " + Number.MIN_VALUE);
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Number/MIN_VALUE
	 */
	public Number js_getMIN_VALUE()
	{
		return null;
	}

	public void js_setMIN_VALUE(Number mINVALUE)
	{
	}

	/**
	 * Special "not a number" value.
	 * 
	 * @sample
	 * application.output("NaN: " + Number.NaN);
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Number/NaN
	 */
	public Object js_getsamecase_NaN()
	{
		return null;
	}

	public void js_setsamecase_NaN(Object naN)
	{
	}

	/**
	 * Special value representing negative infinity; returned on overflow.
	 * 
	 * @sample
	 * application.output("Negative infinity: " + Number.NEGATIVE_INFINITY);
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Number/NEGATIVE_INFINITY
	 */
	public Number js_getNEGATIVE_INFINITY()
	{
		return null;
	}

	public void js_setNEGATIVE_INFINITY(Number nEGATIVEINFINITY)
	{
	}

	/**
	 * Special value representing infinity; returned on overflow.
	 * 
	 * @sample
	 * application.output("Positive infinity: " + Number.POSITIVE_INFINITY);
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Number/POSITIVE_INFINITY
	 */
	public Number js_getPOSITIVE_INFINITY()
	{
		return null;
	}

	public void js_setPOSITIVE_INFINITY(Number pOSITIVEINFINITY)
	{
	}

	/**
	 * Returns a string representing the number in fixed-point notation. 
	 *
	 * @sample
	 * var n = 123.45678;
	 * application.output(n.toFixed(3));
	 * 
	 * @return A string representing the number in fixed-point notation.
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Number/toFixed
	 */
	public String js_toFixed()
	{
		return null;
	}

	/**
	 * @clonedesc js_toFixed()
	 * @sampleas js_toFixed()
	 * 
	 * @param digits The number of digits to appear after the decimal point. Defaults to 0.
	 * 
	 * @return A string representing the number in fixed-point notation.
	 * 
	 */
	public String js_toFixed(Number digits)
	{
		return null;
	}

	/**
	 * Returns a string representing the number in exponential notation. 
	 * 
	 * @sample
	 * var n = 123.45678;
	 * application.output(n.toExponential(3));
	 *  
	 * 
	 * @return A string representing the number in exponential notation. 
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Number/toExponential
	 */
	public String js_toExponential()
	{
		return null;
	}

	/**
	 * @clonedesc js_toExponential()
	 * @sampleas js_toExponential()
	 * 
	 * @param fractionDigits An integer specifying the number of digits after the decimal point. Defaults to as many digits as necessary to specify the number. 
	 * 
	 * @return A string representing the number in exponential notation. 
	 * 
	 */
	public String js_toExponential(Number fractionDigits)
	{
		return null;
	}

	/**
	 * Converts the number into a string which is suitable for presentation in the given locale.
	 * 
	 * @sample
	 * var n = 1000000;
	 * application.output(n.toLocaleString());
	 * 
	 * @return A string representing the number in the current locale.
	 * 
	 * @link https://developer.mozilla.org/En/Core_JavaScript_1.5_Reference/Global_Objects/Number/ToLocaleString
	 */
	public String js_toLocaleString()
	{
		return null;
	}

	/**
	 * Returns a string representing the number to a specified precision in fixed-point or exponential notation. 
	 * 
	 * @sample
	 * var n = 123.45678;
	 * application.output(n.toPrecision(5));
	 * 
	 * 
	 * @return A string representing the number to a specified precision in fixed-point or exponential notation.
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Number/toPrecision
	 */
	public String js_toPrecision()
	{
		return null;
	}

	/**
	 * @clonedesc js_toPrecision()
	 * @sampleas js_toPrecision()
	 * 
	 * @param precision An integer specifying the number of significant digits.
	 * 
	 * @return A string representing the number to a specified precision in fixed-point or exponential notation.
	 * 
	 */
	public String js_toPrecision(Number precision)
	{
		return null;
	}

	/**
	 * Returns a string representing the specified Number object.
	 * 
	 * @sample
	 * var n = 7;
	 * application.output(n.toString()); //displays "7"
	 * application.output(n.toString(2)); //displays "111"
	 *   
	 * 
	 * @return A string representing the specified Number object.
	 * 
	 * @link https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/Number/toString
	 */
	public String js_toString()
	{
		return null;
	}

	/**
	 * @clonedesc js_toString()
	 * @sampleas js_toString()
	 *   
	 * @param radix An integer between 2 and 36 specifying the base to use for representing numeric values
	 * 
	 * @return A string representing the specified Number object.
	 * 
	 */
	public String js_toString(Number radix)
	{
		return null;
	}
}
