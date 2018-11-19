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
@ServoyDocumented(category = ServoyDocumented.JSLIB, publicName = "String", scriptingName = "String")
@ServoyClientSupport(mc = true, wc = true, sc = true)
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
	 * returns a string created by using the specified sequence of Unicode values.
	 * 
	 * @sample 
	 * String.fromCharCode(num)
	 * // String.fromCharCode(num1,num2,num3)
	 * 
	 * @return
	 * 
	 * @param num
	 * 
	 * @link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/String/fromCharCode
	 */
	public String js_fromCharCode(Number... num)
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
	 * returns a new string where the matches of the given regexp are replaced by newSubStr.
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
}
