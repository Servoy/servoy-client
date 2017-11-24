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
@ServoyDocumented(category = ServoyDocumented.JSLIB, publicName = "JS Lib")
@ServoyClientSupport(ng = true, mc = true, wc = true, sc = true)
public class JSLib
{
	/**
	 * Numeric value representing infinity.
	 * 
	 * @sample Infinity
	 * 
	 * @link https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/Infinity
	 * 
	 * @staticCall
	 */
	public Number js_getsamecase_Infinity()
	{
		return null;
	}

	public void js_setsamecase_Infinity(Number infinity)
	{
	}

	/**
	 * Value representing Not-a-Number. 
	 * 
	 * @sample NaN
	 * 
	 * @link https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/Nan
	 * 
	 * @staticCall
	 */
	public Number js_getsamecase_NaN()
	{
		return null;
	}

	public void js_setsamecase_NaN(Number naN)
	{
	}

	/**
	 * The value undefined.
	 * 
	 * @sample undefined
	 * 
	 * @link https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/undefined
	 * 
	 * @staticCall
	 */
	public Object js_getUndefined()
	{
		return null;
	}

	public void js_setUndefined(Object undefined)
	{
	}

	/**
	 * Decodes a URI previously encoded with encodeURI or another similar routine.
	 * 
	 * @link https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/decodeURI
	 * 
	 * @sampleas js_encodeURI(String)
	 * 
	 * @param encodedURI
	 * 
	 * @staticCall
	 * 
	 */
	public String js_decodeURI(String encodedURI)
	{
		return null;
	}

	/**
	 * Decodes a URI component previously created by encodeURIComponent or by a similar routine.
	 * 
	 * @param encodedURI
	 * 
	 * @link https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/decodeURIComponent
	 * 
	 * @sampleas js_encodeURIComponent(String)
	 * 
	 * @staticCall
	 * 
	 */
	public String js_decodeURIComponent(String encodedURI)
	{
		return null;
	}

	/**
	 * Encodes a URI by replacing certain characters with escape sequences.
	 * 
	 * @link https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/encodeURI
	 * 
	 * @sample
	 * var str = "http://www.mysite.com/my code.asp?name=[cool]";
	 * var encoded = encodeURI(str);
	 * var decoded = decodeURI(encoded);
	 * application.output(encoded);//http://www.mysite.com/my%20code.asp?name=%5bcool%5d
	 * application.output(decoded);//http://www.mysite.com/my code.asp?name=[cool]
	 * 
	 * @param URI
	 * 
	 * @staticCall
	 * 
	 */
	public String js_encodeURI(String URI)
	{
		return null;
	}

	/**
	 * Encodes a URI component by replacing all special characters with their corresponding UTF-8 escape sequences.
	 * 
	 * @link https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/encodeURIComponent
	 * 
	 * @sample
	 * var str = "my code.asp?name=[cool]";
	 * var encoded = encodeURIComponent(str);
	 * var decoded = decodeURIComponent(encoded);
	 * application.output(encoded); //my%20code.asp%3fname%3d%5bcool%5d
	 * application.output(decoded); //my code.asp?name=[cool]
	 * 
	 * @param URI
	 * 
	 * @staticCall
	 * 
	 */
	public String js_encodeURIComponent(String URI)
	{
		return null;
	}

	/**
	 * Returns the hexadecimal encoding of a given string.
	 * 
	 * @deprecated Not needed anymore, use native javascript methods (escape(string)).
	 * 
	 * @sample
	 * var encoded = escape("Hello World!");
	 * application.output(encoded); // prints: Hello%20World%21
	 * application.output(unescape(encoded)); // prints: Hello World!
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Guide/Predefined_Functions/escape_and_unescape_Functions
	 */
	@Deprecated
	public String js_escape(String str)
	{
		return null;
	}

	/**
	 * Evaluates JavaScript code passed as a string. Returns the value returned by the evaluated code.
	 * 
	 * @sample
	 * eval("var x = 2 + 3;");
	 * application.output(x); // prints: 5.0
	 *
	 * @link https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/eval
	 * 
	 * @param expression
	 * 
	 * @staticCall
	 */
	public Object js_eval(String expression)
	{
		return null;
	}


	/**
	 * Returns true if the given number is a finite number.
	 * 
	 * @sample
	 * application.output(isFinite(1)); // prints: true
	 * application.output(isFinite(Infinity)); // prints: false
	 * application.output(isFinite(isNaN)); // prints: false
	 *
	 * @link https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/isFinite
	 * 
	 * @param n
	 * 
	 * @staticCall
	 */
	public Boolean js_isFinite(Number n)
	{
		return null;
	}

	/**
	 * The NaN property indicates that a value is 'Not a Number'.
	 *
	 * @sample isNaN( value )
	 * 
	 * @link https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/isNaN
	 * 
	 * @param value
	 * 
	 * @staticCall
	 */
	public void js_isNaN(Object value)
	{
	}

	/**
	 * Returns true if the given name can be used as a valid name for an XML element or attribute.
	 * 
	 * @sample
	 * application.output(isXMLName("good_name")); // prints: true
	 * application.output(isXMLName("bad name")); // because of the space, prints: false
	 *
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 * 
	 * @param name
	 * 
	 * @staticCall
	 */
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public Boolean js_isXMLName(String name)
	{
		return null;
	}

	/**
	 * Makes a floating point number from the starting numbers in a given string.
	 *
	 * @sample parseFloat('string')
	 * 
	 * @link https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/parseFloat
	 * 
	 * @param text
	 * 
	 * @staticCall
	 */
	public Number js_parseFloat(String text)
	{
		return null;
	}

	/**
	 * Makes a integer from the starting numbers in a given string in the base specified.
	 *
	 * @sample parseInt('0774')
	 * 
	 * @link https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/parseInt
	 * 
	 * @param text
	 * 
	 * @staticCall
	 */
	public Number js_parseInt(String text)
	{
		return null;
	}

	/**
	 * Makes a integer from the starting numbers in a given string in the base specified.
	 *
	 * @sample parseInt('0774' , 8)
	 * 
	 * @link https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/parseInt
	 * 
	 * @param text
	 * @param radix
	 * 
	 * @staticCall
	 */
	public Number js_parseInt(String text, int radix)
	{
		return null;
	}


	/**
	 * Returns the ASCII encoding of a string that was previously encoded with escape or another similar routine.
	 * 
	 * @deprecated Not needed anymore, use native javascript methods (unescape(string)).
	 * 
	 * @sample
	 * var encoded = escape("Hello World!");
	 * application.output(encoded); // prints: Hello%20World%21
	 * application.output(unescape(encoded)); // prints: Hello World!
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Guide/Predefined_Functions/escape_and_unescape_Functions
	 */
	@Deprecated
	public String js_unescape(String str)
	{
		return null;
	}

	/**
	 * Returns the string representation behind a given object.
	 * 
	 * @sample
	 * application.output(uneval(isNaN)); // prints something like: function isNaN() { [native code for isNaN, arity=1] }
	 * 
	 * @param obj
	 * 
	 * @staticCall
	 */
	public String js_uneval(Object obj)
	{
		return null;
	}
}
