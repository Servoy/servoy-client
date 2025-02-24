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

package com.servoy.j2db.documentation.scripting.docs;

import org.mozilla.javascript.NativeObject;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.documentation.ServoyDocumented;

/**
 * The javascript BigInt implementation.<br/>
 * BigInt is a built-in object that provides a way to represent whole numbers larger than 2^53-1.
 *
 * For more information see: <a href="https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/BigInt">BigInt (MDN)</a>.
 *
 * @author emera
 */
@ServoyDocumented(category = ServoyDocumented.JSLIB, publicName = "BigInt", scriptingName = "BigInt")
@ServoyClientSupport(mc = true, wc = true, sc = true, ng = true)
public class BigInt
{
	/**
	 * The BigInt() function returns primitive values of type BigInt.
	 * Note: BigInt() can only be called without new. Attempting to construct it with new throws a TypeError.
	 *
	 * @sample const hugeString = BigInt("9007199254740991");
	 *
	 * @param value
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/BigInt/BigInt
	 */
	public void jsConstructor_BigInt(String value)
	{
	}

	/**
	 * The BigInt() function returns primitive values of type BigInt.
	 * Note: BigInt() can only be called without new. Attempting to construct it with new throws a TypeError.
	 *
	 * @sample const b = BigInt(9007199254740991);
	 *
	 * @param value
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/BigInt/BigInt
	 */
	public void jsConstructor_BigInt(Number value)
	{
	}

	/**
	 * The BigInt() function returns primitive values of type BigInt.
	 * Note: BigInt() can only be called without new. Attempting to construct it with new throws a TypeError.
	 *
	 * @sample const b = BigInt(false);
	 *
	 * @param value
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/BigInt/BigInt
	 */
	public void jsConstructor_BigInt(Boolean value)
	{
	}

	/**
	 * The BigInt() function returns primitive values of type BigInt.
	 * Note: BigInt() can only be called without new. Attempting to construct it with new throws a TypeError.
	 *
	 * @sample const b = BigInt(9007199254740991n);
	 *
	 * @param value
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/BigInt/BigInt
	 */
	public void jsConstructor_BigInt(BigInt value)
	{
	}

	/**
	 * Converts the BigInt value to a string in a specified base.
	 *
	 * @sample
	 * /** @type {BigInt} *&#47;
	 * let b = 1024n;
	 * b.toString(16);
	 *
	 * @param radix An integer between 2 and 36 that specifies the base of the returned string.
	 * @return The string representation of the BigInt in the specified base.
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/BigInt/toString
	 */
	public String js_toString(int radix)
	{
		return null;
	}

	/**
	 * Converts the BigInt value to a string using the default base (10).
	 *
	 * @sample
	 * /** @type {BigInt} *&#47;
	 * let b = 1024n;
	 * b.toString();
	 *
	 * @return The string representation of the BigInt in base 10.
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/BigInt/toString
	 */
	public String js_toString()
	{
		return null;
	}

	/**
	 * Converts the BigInt value to a locale-sensitive string representation.
	 *
	 * @sample BigInt(123456789123456789)toLocaleString();
	 *
	 * @return The locale-sensitive string representation of the BigInt using the default locale.
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/BigInt/toLocaleString
	 */
	public String js_toLocaleString()
	{
		return null;
	}

	/**
	 * Converts the BigInt value to a locale-sensitive string representation.
	 *
	 * @sample BigInt(123456789123456789).toLocaleString('de-DE');
	 *
	 * @param locales A string with a BCP 47 language tag, or an array of such strings. Corresponds to the locales parameter of the Intl.NumberFormat() constructor.
	 * @return The locale-sensitive string representation of the BigInt.
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/BigInt/toLocaleString
	 */
	public String js_toLocaleString(String locales)
	{
		return null;
	}

	/**
	 * Converts the BigInt value to a locale-sensitive string representation with the specified locales and formatting options.
	 *
	 * @sample 123456789012345678901234567890n.toLocaleString("de-DE", { style: 'currency', currency: 'EUR' });
	 *
	 * @param locales A string with a BCP 47 language tag, or an array of such strings.
	 * @param options An object with formatting options, such as `useGrouping` and `minimumIntegerDigits`.
	 * @return The locale-sensitive string representation of the BigInt using the specified locale(s) and options.
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/BigInt/toLocaleString
	 */
	public String js_toLocaleString(String locales, NativeObject options)
	{
		return null;
	}

	/**
	 * Returns a BigInt limited to a signed integer value of n bits.
	 *
	 * @sample BigInt.asIntN(8, 257n);
	 *
	 * @param bits The number of bits to retain.
	 * @return A new BigInt limited to n signed bits.
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/BigInt/asIntN
	 */
	public BigInt js_asIntN(int bits)
	{
		return null;
	}

	/**
	 * Returns a BigInt limited to an unsigned integer value of n bits.
	 *
	 * @sample BigInt.asUintN(8, 257n);
	 *
	 * @param bits The number of bits to retain.
	 * @return A new BigInt limited to n unsigned bits.
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/BigInt/asUintN
	 */
	public BigInt js_asUintN(int bits)
	{
		return null;
	}

	/**
	 * Returns the primitive value of the BigInt object.
	 *
	 * @sample 123n.valueOf();
	 *
	 * @return The primitive BigInt value.
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/BigInt/valueOf
	 */
	public BigInt js_valueOf()
	{
		return null;
	}
}
