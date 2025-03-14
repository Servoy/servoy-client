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
 * The String object is used to represent and manipulate a sequence of characters.<br/><br/>
 *
 * For more information see: <a href="https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/String">String (MDN)</a>.
 *
 * @author gerzse
 */
@ServoyDocumented(category = ServoyDocumented.JSLIB, publicName = "String", scriptingName = "String")
@ServoyClientSupport(mc = true, wc = true, sc = true, ng = true)
public class String
{

	/**
	 * Gives the length of the string.
	 *
	 * @sample string.length;
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/length
	 */
	public Number js_getLength()
	{
		return null;
	}

	public void js_setLength(Number length)
	{
	}

	/**
	 * returns a copy of the string embedded within an anchor &lt;A&gt; tag set.
	 *
	 * @sample string.anchor();
	 *
	 * @param nameAttribute
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/anchor
	 */
	public String js_anchor(String nameAttribute)
	{
		return null;
	}

	/**
	 * returns a copy of the string embedded within an &lt;BIG&gt; tag set.
	 *
	 * @sample string.big();
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/big
	 */
	public String js_big()
	{
		return null;
	}

	/**
	 * returns a copy of the string embedded within an &lt;BLINK&gt; tag set.
	 *
	 * @sample string.blink();
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/blink
	 */
	public String js_blink()
	{
		return null;
	}

	/**
	 * returns a copy of the string embedded within an &lt;B&gt; tag set.
	 *
	 * @sample string.bold();
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/bold
	 */
	public String js_bold()
	{
		return null;
	}

	/**
	 * returns a character of the string.
	 *
	 * @sample string.charAt(integer_position);
	 *
	 * @param index
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/charAt
	 */
	public Number js_charAt(Number index)
	{
		return null;
	}

	/**
	 * returns a decimal code of the char in the string.
	 *
	 * @sample string.charCodeAt(integer_position);
	 *
	 * @param index
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/charCodeAt
	 */
	public Number js_charCodeAt(Number index)
	{
		return null;
	}

	/**
	 * returns a non-negative integer that is the Unicode code point value at the given position. Note that this function does not give the nth code point in a string, but the code point starting at the specified string index.
	 *
	 * @sample string.codePointAt(integer_position);
	 *
	 * @param index
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/String/codePointAt
	 */
	public Number js_codePointAt(Number index)
	{
		return null;
	}

	/**
	 * returns a string that appends the parameter string to the string.
	 *
	 * @sample string.concat(string);
	 *
	 * @param string2
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/concat
	 */
	public String js_concat(String string2)
	{
		return null;
	}

	/**
	* @clonedesc js_concat(String)
	* @sampleas js_concat(String)
	 *
	 * @param string2
	 * @param stringN
	 *
	 */
	public String js_concat(String string2, String stringN)
	{
		return null;
	}

	/**
	 * Determines whether a string ends with the characters of a specified string, returning true or false as appropriate.
	 *
	 *@sample
	 *  var str1 = 'Cats are the best!';
	 *  application.output(str1.endsWith('best', 17));
	 *
	 * @param searchString The characters to be searched for at the end of str.
	 *
	 * @return true if the given characters are found at the end of the string; otherwise, false
	 */
	public String js_endsWith(String searchString)
	{
		return null;
	}

	/**
	 * @clonedesc js_endsWith(String)
	 * @sampleas js_endsWith(String)
	 *
	 * @param searchString The characters to be searched for at the end of str.
	 * @param length If provided, it is used as the length of str. Defaults to str.length.
	 *
	 * @return true if the given characters are found at the end of the string; otherwise, false
	 */
	public String js_endsWith(String searchString, Number length)
	{
		return null;
	}

	/**
	 * returns a boolean that checks if the given string is equal to the string
	 *
	 * @sample string.equals(string);
	 *
	 * @param other
	 */
	public Boolean js_equals(String other)
	{
		return null;
	}

	/**
	 * returns a boolean that checks if the given string is equal to the string ignoring case
	 *
	 * @sample string.equalsIgnoreCase(string);
	 *
	 * @param other
	 */
	public Boolean js_equalsIgnoreCase(String other)
	{
		return null;
	}

	/**
	 * returns a copy of the string embedded within an anchor &lt;TT&gt; tag set.
	 *
	 * @sample string.fixed();
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/fixed
	 */
	public String js_fixed()
	{
		return null;
	}

	/**
	 * returns a copy of the string embedded within an &lt;FONT&gt; tag set, the color param is assigned the the color attribute.
	 *
	 * @sample string.fontcolor(color);
	 *
	 * @param color
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/fontcolor
	 */
	public String js_fontcolor(String color)
	{
		return null;
	}

	/**
	 * returns a copy of the string embedded within an &lt;FONT&gt; tag set, The size param is set to the SIZE attribute
	 *
	 * @sample string.fontsize(size);
	 *
	 * @param size
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/fontsize
	 */
	public String js_fontsize(Number size)
	{
		return null;
	}

	/**
	 * Static method that returns a string created from the specified sequence of UTF-16 code units.
	 *
	 * @sample
	 * String.fromCharCode(0x2014); // returns "—"
	 * String.fromCharCode(65, 66, 67); // returns "ABC"
	 *
	 * @return A string of length N consisting of the N specified UTF-16 code units.
	 *
	 * @param num A sequence of numbers that are UTF-16 code units. The range is between 0 and 65535 (0xFFFF). Numbers greater than 0xFFFF are truncated. No validity checks are performed.
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/String/fromCharCode
	 */
	public String js_fromCharCode(Number... num)
	{
		return null;
	}

	/**
	 * Static method that returns a string created by using the specified sequence of code points.
	 * String.fromCharCode() cannot return supplementary characters (i.e. code points 0x010000 – 0x10FFFF) by specifying their code point. Instead, it requires the UTF-16 surrogate pair for that. String.fromCodePoint(), on the other hand, can return 4-byte supplementary characters, as well as the more common 2-byte BMP characters, by specifying their code point (which is equivalent to the UTF-32 code unit).
	 *
	 * RangeError is thrown if an invalid Unicode code point is given (e.g. "RangeError: NaN is not a valid code point").
	 *
	 * @sample
	 * String.fromCodePoint(42); // "*"
	 * String.fromCodePoint(65, 90); // "AZ"
	 * String.fromCodePoint(0x2f804); // "\uD87E\uDC04"
	 * String.fromCodePoint(-1); // RangeError
	 * String.fromCodePoint(3.14); // RangeError
	 *
	 * @return A string created by using the specified sequence of code points.
	 *
	 * @param num A sequence of code points.
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/String/fromCodePoint
	 */
	public String js_fromCodePoint(Number... num)
	{
		return null;
	}

	/**
	 * returns the found index of the given string in string.
	 *
	 * @sample string.indexOf(string,startPosition);
	 *
	 * @param searchValue
	 * @param fromIndex
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/indexOf
	 */
	public Number js_indexOf(String searchValue, Number fromIndex)
	{
		return null;
	}

	/**
	 * returns a copy of the string embedded within an &lt;I&gt; tag set
	 *
	 * @sample string.italics();
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/italics
	 */
	public String js_italics()
	{
		return null;
	}

	/**
	 * returns the found index of the given string in string from the end.
	 *
	 * @sample string.lastIndexOf(string,startPosition);
	 *
	 * @param searchValue
	 * @param fromIndex
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/lastIndexOf
	 */
	public Number js_lastIndexOf(String searchValue, Number fromIndex)
	{
		return null;
	}

	/**
	 * returns a copy of the string embedded within an &lt;A&gt; tag set.
	 *
	 * @sample string.link(url);
	 *
	 * @param hrefAttribute
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/link
	 */
	public String js_link(String hrefAttribute)
	{
		return null;
	}

	/**
	 * @sample
	 * var s = "Have a nice day!";
	 * application.output(s.localeCompare("Hello"));
	 *
	 * @param otherString
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/localeCompare
	 */
	public Number js_localeCompare(String otherString)
	{
		return null;
	}

	/**
	 * returns an array of strings within the current string that matches the regexp.
	 *
	 * @sample string.match(regexpr);
	 *
	 * @param regexp
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/match
	 */
	public Array js_match(RegExp regexp)
	{
		return null;
	}

	/**
	 * Returns the Unicode Normalization Form of the string. (defaults to "NFC" form)
	 *
	 * @return A string containing the Unicode Normalization Form of the given string.
	 */
	public String js_normalize()
	{
		return null;
	}

	/**
	 * Returns the Unicode Normalization Form of the string.
	 * form param can be one of "NFC", "NFD", "NFKC", or "NFKD", specifying the Unicode Normalization Form. If omitted or undefined, "NFC" is used.
	 *
	 * These values have the following meanings:
	 *
	 * "NFC"
	 * Canonical Decomposition, followed by Canonical Composition.
	 * "NFD"
	 * Canonical Decomposition.
	 * "NFKC"
	 * Compatibility Decomposition, followed by Canonical Composition.
	 * "NFKD"
	 * Compatibility Decomposition.
	 *
	 * @sample
	 * var string1 = '\u00F1';           // �
	 * var string2 = '\u006E\u0303';     // �
	 *
	 * string1 = string1.normalize('NFD');
	 * string2 = string2.normalize('NFD');
	 *
	 * application.output(string1 === string2); // true
	 * application.output(string1.length);      // 2
	 * application.output(string2.length);      // 2
	 *
	 * @param form  param can be one of "NFC", "NFD", "NFKC", or "NFKD",
	 * @return A string containing the Unicode Normalization Form of the given string.
	 */
	public String js_normalize(String form)
	{
		return null;
	}

	/**
	 * The padStart() method pads the current string with another string (multiple times, if needed) until the resulting string reaches the given length. The padding is applied from the start of the current string.
	 * The default value used for padding is the unicode "space" character (U+0020) - if no padString argument is used.
	 *
	 * @sample string.padStart(10);
	 *
	 * @param targetLength The length of the resulting string once the current str has been padded. If the value is less than or equal to str.length, then str is returned as-is.
	 *
	 * @return A String of the specified targetLength with spaces applied from the start.
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/String/padStart
	 */
	public String js_padStart(int targetLength)
	{
		return null;
	}

	/**
	 * The padStart() method pads the current string with another string (multiple times, if needed) until the resulting string reaches the given length. The padding is applied from the start of the current string.
	 *
	 * @sample string.padStart(10, '*');
	 *
	 * @param targetLength The length of the resulting string once the current str has been padded. If the value is less than or equal to str.length, then str is returned as-is.
	 * @param padString The string to pad the current str with. If padString is too long to stay within the targetLength, it will be truncated from the end. The default value is the unicode "space" character (U+0020).
	 *
	 * @return A String of the specified targetLength with padString applied from the start.
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/String/padStart
	 */
	public String js_padStart(int targetLength, String padString)
	{
		return null;
	}

	/**
	 * The padEnd() method pads the current string with a given string (repeated, if needed) so that the resulting string reaches a given length. The padding is applied from the end of the current string.
	 * The default value used for padding is the unicode "space" character (U+0020) - if no padString argument is used.
	 *
	 * @sample string.padEnd(10, '*');
	 *
	 * @param targetLength The length of the resulting string once the current str has been padded. If the value is less than or equal to str.length, then str is returned as-is.
	 *
	 * @return A String of the specified targetLength with spaces applied at the end of the current str.
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/String/padEnd
	 */
	public String js_padEnd(int targetLength)
	{
		return null;
	}

	/**
	 * The padEnd() method pads the current string with a given string (repeated, if needed) so that the resulting string reaches a given length. The padding is applied from the end of the current string.
	 *
	 * @sample string.padEnd(10, '*');
	 *
	 * @param targetLength The length of the resulting string once the current str has been padded. If the value is less than or equal to str.length, then str is returned as-is.
	 * @param padString The string to pad the current str with. If padString is too long to stay within the targetLength, it will be truncated from the end. The default value is the unicode "space" character (U+0020).
	 *
	 * @return A String of the specified targetLength with the padString applied at the end of the current str.
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/String/padEnd
	 */
	public String js_padEnd(int targetLength, String padString)
	{
		return null;
	}

	/**
	 * Returns the string stripped of whitespace from both ends.
	 *
	 * @sample string.trim();
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/trim
	 */
	public String js_trim()
	{
		return null;
	}

	/**
	 * Removes whitespace from the beginning of a string and returns a new string, without modifying the original string.
	 *
	 * @sample string.trimStart();
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/String/trimStart
	 */
	public String js_trimStart()
	{
		return null;
	}

	/**
	 * Removes whitespace from the ending of a string and returns a new string, without modifying the original string.
	 *
	 * @sample string.trimEnd();
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/String/trimEnd
	 */
	public String js_trimEnd()
	{
		return null;
	}

	/**
	 * Constructs and returns a new string which contains the specified number of copies of the string on which it was called, concatenated together.
	 *
	 * @sample
	 * var str = 'abc'.repeat(2); // 'abcabc'
	 *
	 * @param count An integer between 0 and +Infinity, indicating the number of times to repeat the string.
	 * @return  A new string containing the specified number of copies of the given string.
	 */
	public String js_repeat(Number count)
	{
		return null;
	}

	/**
	 * returns a new string where the matches of the given reg exp are replaced by newSubStr.
	 *
	 * @sample
	 * string.replace(regexp,newSubStr);
	 * //var re = /(\w+)\s(\w+)/;
	 * //var str = "John Smith";
	 * //var newstr = str.replace(re, "$2, $1");
	 * //application.output(newstr);
	 *
	 * @param regexp
	 * @param newSubStr
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/replace
	 */
	public String js_replace(RegExp regexp, String newSubStr)
	{
		return null;
	}

	/**
	 * returns a new string where the matches of the given regexp are replaced by the return value of the function.
	 * The function parameter is the function to be invoked to create the new substring (to put in place of the substring received from parameter #1).
	 *
	 * @sample
	 * //the callback definition
	 * function replacer(match, p1, p2, p3, offset, string){
	 * 		// match is the matched substring
	 * 		// p1 is non-digits, p2 digits, and p3 non-alphanumerics
	 * 		// offset is the offset of the matched substring within the total string being examined
	 * 		// string is the total string being examined
	 *  	return [p1, p2, p3].join(' - ');
	 * }
	 * // using replace method with replacer callback
	 * newString = "abc12345#$*%".replace(/([^\d]*)(\d*)([^\w]*)/, replacer);
	 *
	 * @param regexp
	 * @param function
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/replace
	 */
	public String js_replace(RegExp regexp, Function function)
	{
		return null;
	}

	/**
	 * returns a new string where the first match of the given substr is replaced by newSubStr.
	 *
	 * @sample string.replace(substr,newSubStr);
	 *
	 * @param substr
	 * @param newSubStr
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/replace
	 */
	public String js_replace(String substr, String newSubStr)
	{
		return null;
	}

	/**
	 * returns a new string where the first match of the given substr is replaced by the return value of the function.
	 * The function parameter is the function to be invoked to create the new substring (to put in place of the substring received from parameter #1).
	 *
	 * @sample
	 * // the callback definition
	 * function replacer(match){
	 * 		return match.toUpperCase()
	 * }
	 * // using replace method with replacer callback
	 * var newString = "abc".replace("a", replacer);
	 *
	 * @param substr
	 * @param function
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/replace
	 */
	public String js_replace(String substr, Function function)
	{
		return null;
	}

	/**
	 * returns an index where the first match is found of the regexp
	 *
	 * @sample string.search(regexpr);
	 *
	 * @param regexp
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/search
	 */
	public Number js_search(RegExp regexp)
	{
		return null;
	}

	/**
	 * returns a substring of the string.
	 *
	 * @sample string.slice(start,end);
	 *
	 * @param beginSlice
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/slice
	 */
	public String js_slice(Number beginSlice)
	{
		return null;
	}

	/**
	 * @clonedesc js_slice(Number)
	 * @sampleas js_slice(Number)
	 *
	 * @param beginSlice
	 * @param endSlice
	 *
	 */
	public String js_slice(Number beginSlice, Number endSlice)
	{
		return null;
	}

	/**
	 * returns a copy of the string embedded within an &lt;SMALL&gt; tag set.
	 *
	 * @sample string.small();
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/small
	 */
	public String js_small()
	{
		return null;
	}

	/**
	 * returns an array of objects whose elements are segments of the current string.
	 *
	 * @sample
	 * var myString = 'Hello 1 word. Sentence number 2.';
	 * var splits = myString.split(' ');
	 * application.output(splits);
	 *
	 * @param separator Specifies the string which denotes the points at which each split should occur. If separator is an empty string, str is converted to an array of characters.
	 * @param limit Optional integer specifying a limit on the number of splits to be found.
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/split
	 */
	public String js_split(String separator, Number limit)
	{
		return null;
	}

	/**
	 * returns an array of objects whose elements are segments of the current string.
	 *
	 * @sample
	 * var myString = 'Hello 1 word. Sentence number 2.';
	 * var splits = myString.split(new RegExp(/(\d)/), 2);
	 * application.output(splits); //prints [Hello , 1]
	 *
	 * @param separator Specifies the string which denotes the points at which each split should occur. If separator is an empty string, str is converted to an array of characters.
	 * @param limit Optional integer specifying a limit on the number of splits to be found.
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/split
	 */
	public String js_split(RegExp separator, Number limit)
	{
		return null;
	}

	/**
	 * Determines whether a string begins with the characters of a specified string, returning true or false as appropriate.
	 *
	 * @sample
	 *  var str1 = 'Cats are the best!';
	 *  application.output(str1.startsWith('Cats'));
	 *
	 * @param searchString The characters to be searched for at the start of this string.
	 *
	 * @return true if the given characters are found at the beginning of the string; otherwise, false
	 */
	public String js_startsWith(String searchString)
	{
		return null;
	}

	/**
	 * @clonedesc js_startsWith(String)
	 * @sampleas js_startsWith(String)
	 *
	 * @param searchString The characters to be searched for at the start of this string.
	 * @param position The position in this string at which to begin searching for searchString. Defaults to 0.
	 *
	 * @return true if the given characters are found at the beginning of the string; otherwise, false
	 */
	public String js_startsWith(String searchString, Number position)
	{
		return null;
	}

	/**
	 * returns a copy of the string embedded within an &lt;STRIKE&gt; tag set.
	 *
	 * @sample string.strike();
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/strike
	 */
	public String js_strike()
	{
		return null;
	}

	/**
	 * returns a copy of the string embedded within an &lt;SUB&gt; tag set.
	 *
	 * @sample string.sub();
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/sub
	 */
	public String js_sub()
	{
		return null;
	}

	/**
	 * returns a substring of the string from the start with the number of chars specified.
	 *
	 * @sample string.substr(start, number_of_chars);
	 *
	 * @param start
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/substr
	 */
	public String js_substr(Number start)
	{
		return null;
	}

	/**
	 * @clonedesc js_substr(Number)
	 * @sampleas js_substr(Number)
	 *
	 * @param start
	 * @param length
	 *
	 */
	public String js_substr(Number start, Number length)
	{
		return null;
	}

	/**
	 * Returns a substring of the string from the start index until the end index.
	 *
	 * @sample string.substring(start, end);
	 *
	 * @param indexA
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/substring
	 */
	public String js_substring(Number indexA)
	{
		return null;
	}

	/**
	* @clonedesc js_substring(Number)
	* @sampleas js_substring(Number)
	 *
	 * @param indexA
	 * @param indexB
	 *
	 */
	public String js_substring(Number indexA, Number indexB)
	{
		return null;
	}

	/**
	 * returns a copy of the string embedded within an &lt;SUP&gt; tag set.
	 *
	 * @sample string.sup();
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/sup
	 */
	public String js_sup()
	{
		return null;
	}

	/**
	 *
	 * @sample
	 * var s = "Have a nice day!";
	 * application.output(s.toLocaleLowerCase());
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/toLocaleLowerCase
	 */
	public String js_toLocaleLowerCase()
	{
		return null;
	}

	/**
	 * returns a string with all lowercase letters of the current string.
	 *
	 * @sample string.toLowerCase();
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/toLowerCase
	 */
	public String js_toLowerCase()
	{
		return null;
	}

	/**
	 *
	 * @sample
	 * var s = "Have a nice day!";
	 * application.output(s.toLocaleUpperCase());
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/toLocaleUpperCase
	 */
	public String js_toLocaleUpperCase()
	{
		return null;
	}

	/**
	 * returns a string with all uppercase letters of the current string.
	 *
	 * @sample string.toUpperCase();
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/String/toUpperCase
	 */
	public String js_toUpperCase()
	{
		return null;
	}

	/**
	 * Determines whether one string may be found within another string.
	 *
	 * @sample string.includes('foo');
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/String/includes
	 */
	public String js_includes()
	{
		return null;
	}

	/**
	 * Returns an iterator of all results matching this string against a regular expression, including capturing groups.
	 *
	 * @sample
	 * const regexp = /t(e)(st(\d?))/g;
	 * const str = "test1test2";
	 * const array = [...str.matchAll(regexp)];
	 * application.output(array[0]);
	 *
	 * @param regexp
	 *
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/String/matchAll
	 */
	public Array js_matchAll(RegExp regexp)
	{
		return null;
	}
}
