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

/**
 * Dummy class for listing methods for JavaScript types in a manner that
 * suits our documentation generator.
 * 
 * @link https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/JSON
 * 
 * @author jcompagner
 */
@ServoyDocumented(category = ServoyDocumented.JSLIB, publicName = "JSON", scriptingName = "JSON")
public class JSON
{
	/**
	 * Parses a string as JSON and returns the parsed value.
	 *
	 * @sample JSON.parse('[1, 5, "false"]');
	 * 
	 * @param text The string to parse as JSON.  See the JSON object for a description of JSON syntax.
	 * 
	 * @link https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/JSON/parse
	 */
	public Object js_parse(String text)
	{
		return null;
	}

	/**
	 * Parses a string as JSON and returns the parsed value.
	 *
	 * @sample var transformed = JSON.parse('{"p": 5}', function(k, v) { if (k === "") return v; return v * 2; });  
	 * 
	 * @param text The string to parse as JSON.  See the JSON object for a description of JSON syntax.
	 * 
	 * @param reviver A function, prescribes how the value originally produced by parsing is transformed, before being returned. 
	 * 
	 * @link https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/JSON/parse
	 */
	public Object js_parse(String text, Function reviver)
	{
		return null;
	}

	/**
	 * Convert a value to JSON, optionally replacing values if a replacer function is specified, or optionally including only the specified properties if a replacer array is specified
	 *
	 * @sample JSON.stringify([1, "false", false])
	 * 
	 * @param value The value to convert to a JSON string.
	 * 
	 * @link https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/JSON/stringify
	 */
	public String js_stringify(Object value)
	{
		return null;
	}

	/**
	 * Convert a value to JSON, optionally replacing values if a replacer function is specified, or optionally including only the specified properties if a replacer array is specified
	 *
	 * @sample 
	 * function censor(key, value) {  
	 *  if (typeof(value) == "string") {  
	 *    return undefined;  
	 *  }   
	 *  return value;  
	 * }  
	 *       
	 * var foo = {foundation: "Mozilla", model: "box", week: 45, transport: "car", month: 7};  
	 * var jsonString = JSON.stringify(foo, censor);  
	 * 
	 * @param value The value to convert to a JSON string.
	 * 
	 * @param replacer If a function, transforms values and properties encountered while stringifying; if an array, specifies the set of properties included in objects in the final string.
	 * 
	 * @link https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/JSON/stringify
	 */
	public String js_stringify(Object value, Object replacer)
	{
		return null;
	}

	/**
	 * Convert a value to JSON, optionally replacing values if a replacer function is specified, or optionally including only the specified properties if a replacer array is specified
	 *
	 * @sample JSON.stringify({ uno: 1, dos : 2 }, null, '\t') 
	 * 
	 * @param value The value to convert to a JSON string.
	 * 
	 * @param replacer If a function, transforms values and properties encountered while stringifying; if an array, specifies the set of properties included in objects in the final string.
	 * 
	 * @param space The space argument may be used to control spacing in the final string. If it is a number, successive levels in the stringification will each be indented by this many space characters (up to 10). If it is a string, successive levels will indented by this string (or the first ten characters of it).
	 * 
	 * @link https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/JSON/stringify
	 */
	public String js_stringify(Object value, Object replacer, Object space)
	{
		return null;
	}
}
