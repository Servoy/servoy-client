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
 * Dummy class for listing methods for JavaScript types in a manner that
 * suits our documentation generator.
 *
 * @author gerzse
 */
@ServoyDocumented(category = ServoyDocumented.JSLIB, publicName = "Date", scriptingName = "Date")
@ServoyClientSupport(mc = true, wc = true, sc = true)
public class Date
{
	/**
	 * Use the current date and time to create an instance of the object date.
	 *
	 * @sample
	 * var date = new Date();
	 */
	public void jsConstructor_Date()
	{
	}

	/**
	 * Use the date specified by the string to create the instance of the date object. String format is "month day, year hours:minutes:seconds".
	 *
	 * @param dateString
	 *
	 * @sample
	 * var date = new Date(dateString);
	 */
	public void jsConstructor_Date(String dateString)
	{
	}

	/**
	 * Create an instance of date with the specified values.
	 *
	 * @param year
	 * @param month
	 * @param day
	 *
	 * @sample
	 * var date = new Date(year, month, day);
	 */
	public void jsConstructor_Date(Number year, Number month, Number day)
	{
	}

	/**
	 * Create an instance of date with the specified values.
	 *
	 * @param year
	 * @param month
	 * @param day
	 * @param hours
	 * @param minutes
	 * @param seconds
	 *
	 * @sample
	 * var date = new Date(year, month, day, hours, minutes, seconds);
	 */
	public void jsConstructor_Date(Number year, Number month, Number day, Number hours, Number minutes, Number seconds)
	{

	}

	/**
	 * Create an instance of date with the specified values.
	 *
	 * @param year
	 * @param month
	 * @param day
	 * @param hours
	 * @param minutes
	 * @param seconds
	 * @param milliseconds
	 *
	 * @sample
	 * var date = new Date(year, month, day, hours, minutes, seconds, milliseconds)
	 */
	public void jsConstructor_Date(Number year, Number month, Number day, Number hours, Number minutes, Number seconds, Number milliseconds)
	{

	}

	/**
	 * Create instance of date. The argument is the number of milliseconds since 1 January 1970 00:00:00 UTC.
	 *
	 * @param milliseconds
	 *
	 * @sample
	 * var date = new Date(milliseconds);
	 */
	public void jsConstructor_Date(Number milliseconds)
	{

	}

	/**
	 * Returns the milliseconds elapsed since 1 January 1970 00:00:00 UTC up until now.
	 *
	 * @sample
	 * var timestamp = Date.now();
	 *
	 * @link https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/Date/now
	 */
	@JSFunction
	public Number now()
	{
		return null;
	}

	/**
	 * Takes a date string (such as "Dec 25, 1995") and returns the number of milliseconds since January 1, 1970, 00:00:00 UTC.
	 *
	 * @sample
	 * var str = Date.parse("Wed, 09 Aug 1995 00:00:00 GMT");
	 * application.output(str);
	 *
	 * @link https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/Date/parse
	 */
	@JSFunction
	public Number parse(String s)
	{
		return null;
	}

	/**
	 * Takes comma-delimited date parameters and returns the number of milliseconds between January 1, 1970, 00:00:00, universal time and the specified time.
	 *
	 * @param year A year after 1900.
	 * @param month A number between 0 and 11.
	 *
	 * @sample
	 * // The number of milliseconds in the first minute after 1970 January 1st.
	 * application.output(Date.UTC(1970, 00, 01, 00, 01, 00, 00)); // prints: 60000.0
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/UTC
	 */
	@JSFunction
	public Number UTC(Number year, Number month)
	{
		return null;
	}

	/**
	* @clonedesc UTC(Number, Number)
	* @sampleas UTC(Number, Number)
	*
	* @param year A year after 1900.
	* @param month A number between 0 and 11.
	* @param date A number between 1 and 31.
	*/
	@JSFunction
	public Number UTC(Number year, Number month, Number date)
	{
		return null;
	}

	/**
	* @clonedesc UTC(Number, Number)
	* @sampleas UTC(Number, Number)
	*
	* @param year A year after 1900.
	* @param month A number between 0 and 11.
	* @param date A number between 1 and 31.
	* @param hrs A number between 0 and 23.
	*
	*/
	@JSFunction
	public Number UTC(Number year, Number month, Number date, Number hrs)
	{
		return null;
	}

	/**
	* @clonedesc UTC(Number, Number)
	* @sampleas UTC(Number, Number)
	*
	* @param year A year after 1900.
	* @param month A number between 0 and 11.
	* @param date A number between 1 and 31.
	* @param hrs A number between 0 and 23.
	* @param min A number between 0 and 59.
	*
	*/
	@JSFunction
	public Number UTC(Number year, Number month, Number date, Number hrs, Number min)
	{
		return null;
	}

	/**
	* @clonedesc UTC(Number, Number)
	* @sampleas UTC(Number, Number)
	*
	* @param year A year after 1900.
	* @param month A number between 0 and 11.
	* @param date A number between 1 and 31.
	* @param hrs A number between 0 and 23.
	* @param min A number between 0 and 59.
	* @param sec A number between 0 and 59.
	*
	*/
	@JSFunction
	public Number UTC(Number year, Number month, Number date, Number hrs, Number min, Number sec)
	{
		return null;
	}

	/**
	* @clonedesc UTC(Number, Number)
	* @sampleas UTC(Number, Number)
	*
	* @param year A year after 1900.
	* @param month A number between 0 and 11.
	* @param date A number between 1 and 31.
	* @param hrs A number between 0 and 23.
	* @param min A number between 0 and 59.
	* @param sec A number between 0 and 59.
	* @param ms A number between 0 and 999.
	*
	*/
	@JSFunction
	public Number UTC(Number year, Number month, Number date, Number hrs, Number min, Number sec, Number ms)
	{
		return null;
	}

	/**
	 * Gets the day of month.
	 *
	 * @sample date.getDate();
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/getDate
	 */
	@JSFunction
	public Number getDate()
	{
		return null;
	}

	/**
	 * Gets the day of the week (sunday = 0).
	 *
	 * @sample date.getDay();
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/getDay
	 */
	@JSFunction
	public Number getDay()
	{
		return null;
	}

	/**
	 * Gets the full year of the date.
	 *
	 * @sample date.getFullYear();
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/getFullYear
	 */
	@JSFunction
	public Number getFullYear()
	{
		return null;
	}

	/**
	 * Gets the hours of the date.
	 *
	 * @sample date.getHours();
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/getHours
	 */
	@JSFunction
	public Number getHours()
	{
		return null;
	}

	/**
	 * Gets the milliseconds of the date.
	 *
	 * @sample date.getMilliseconds();
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/getMilliseconds
	 */
	@JSFunction
	public Number getMilliseconds()
	{
		return null;
	}

	/**
	 * Gets the minutes of the date.
	 *
	 * @sample date.getMinutes();
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/getMinutes
	 */
	@JSFunction
	public Number getMinutes()
	{
		return null;
	}

	/**
	 * Gets the month of the date.
	 *
	 * @sample date.getMonth();
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/getMonth
	 */
	@JSFunction
	public Number getMonth()
	{
		return null;
	}

	/**
	 * Gets the seconds of the date.
	 *
	 * @sample date.getSeconds();
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/getSeconds
	 */
	@JSFunction
	public Number getSeconds()
	{
		return null;
	}

	/**
	 * The value returned by the getTime method is the number of milliseconds since 1 January 1970 00:00:00.
	 *
	 * @sample date.getTime();
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/getTime
	 */
	@JSFunction
	public Number getTime()
	{
		return null;
	}

	/**
	 * Gets the number of minutes between GMT and this date.
	 *
	 * @sample date.getTimezoneOffset();
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/getTimezoneOffset
	 */
	@JSFunction
	public Number getTimezoneOffset()
	{
		return null;
	}

	/**
	 * Gets the UTC date.
	 *
	 * @sample date.getUTCDate();
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/getUTCDate
	 */
	@JSFunction
	public Number getUTCDate()
	{
		return null;
	}

	/**
	 * Gets the day in UTC time.
	 *
	 * @sample date.getUTCDay();
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/getUTCDay
	 */
	@JSFunction
	public Number getUTCDay()
	{
		return null;
	}

	/**
	 * Gets the full year in UTC time.
	 *
	 * @sample date.getUTCFullYear();
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/getUTCFullYear
	 */
	@JSFunction
	public Number getUTCFullYear()
	{
		return null;
	}

	/**
	 * Gets the hours in UTC time.
	 *
	 * @sample date.getUTCHours();
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/getUTCHours
	 */
	@JSFunction
	public Number getUTCHours()
	{
		return null;
	}

	/**
	 * Gets the milliseconds in UTC time.
	 *
	 * @sample date.getUTCMilliseconds();
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/getUTCMilliseconds
	 */
	@JSFunction
	public Number getUTCMilliseconds()
	{
		return null;
	}

	/**
	 * Gets the minutes in UTC time.
	 *
	 * @sample date.getUTCMinutes();
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/getUTCMinutes
	 */
	@JSFunction
	public Number getUTCMinutes()
	{
		return null;
	}

	/**
	 * Gets the month in UTC time.
	 *
	 * @sample date.getUTCMonth();
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/getUTCMonth
	 */
	@JSFunction
	public Number getUTCMonth()
	{
		return null;
	}

	/**
	 * Gets the seconds in UTC time.
	 *
	 * @sample date.getUTCSeconds();
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/getUTCSeconds
	 */
	@JSFunction
	public Number getUTCSeconds()
	{
		return null;
	}

	/**
	 * Gets the year of the date.
	 *
	 * @deprecated Obsolete method, replaced by {@link #getFullYear())}.
	 *
	 * @sample date.getYear();
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/getYear
	 */
	@Deprecated
	public Number jsFunction_getYear()
	{
		return null;
	}

	/**
	 * Sets the date.
	 *
	 * @sample date.setDate(integer);
	 *
	 * @param dayValue
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/setDate
	 */
	@JSFunction
	public void setDate(Number dayValue)
	{
	}

	/**
	 * Sets the full year of the date.
	 *
	 * @sample date.setFullYear(integer);
	 *
	 * @param yearValue
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/setFullYear
	 */
	@JSFunction
	public void setFullYear(Number yearValue)
	{
	}

	/**
	* @clonedesc setFullYear(Number)
	* @sampleas setFullYear(Number)
	*
	* @param yearValue
	* @param monthValue
	*/
	@JSFunction
	public void setFullYear(Number yearValue, Number monthValue)
	{
	}

	/**
	* @clonedesc setFullYear(Number)
	* @sampleas setFullYear(Number)
	*
	* @param yearValue
	* @param monthValue
	* @param dayValue
	*/
	@JSFunction
	public void setFullYear(Number yearValue, Number monthValue, Number dayValue)
	{
	}

	/**
	 * Sets the hours of the date.
	 *
	 * @sample date.setHours(integer);
	 *
	 * @param hoursValue
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/setHours
	 */
	@JSFunction
	public void setHours(Number hoursValue)
	{
	}

	/**
	 * @clonedesc setHours(Number)
	 * @sampleas setHours(Number)
	 *
	 * @param hoursValue
	 * @param minutesValue
	 *
	 */
	@JSFunction
	public void setHours(Number hoursValue, Number minutesValue)
	{
	}

	/**
	 *
	 * @clonedesc setHours(Number)
	 * @sampleas setHours(Number)
	 *
	 * @param hoursValue
	 * @param minutesValue
	 * @param secondsValue
	 */
	@JSFunction
	public void setHours(Number hoursValue, Number minutesValue, Number secondsValue)
	{
	}

	/**
	 * @clonedesc setHours(Number)
	 * @sampleas setHours(Number)
	 *
	 * @param hoursValue
	 * @param minutesValue
	 * @param secondsValue
	 * @param msValue
	 *
	 */
	@JSFunction
	public void setHours(Number hoursValue, Number minutesValue, Number secondsValue, Number msValue)
	{
	}

	/**
	 * Sets the milliseconds of the date.
	 *
	 * @sample date.setMilliseconds(integer);
	 *
	 * @param millisecondsValue
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/setMilliseconds
	 */
	@JSFunction
	public void setMilliseconds(Number millisecondsValue)
	{
	}

	/**
	 * Sets the minutes of the date.
	 *
	 * @sample date.setMinutes(integer);
	 *
	 * @param minutesValue
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/setMinutes
	 */
	@JSFunction
	public void setMinutes(Number minutesValue)
	{
	}

	/**
	 * @clonedesc setMinutes(Number)
	 * @sampleas setMinutes(Number)
	 *
	 * @param minutesValue
	 * @param secondsValue
	 */
	@JSFunction
	public void setMinutes(Number minutesValue, Number secondsValue)
	{
	}

	/**
	 * @clonedesc setMinutes(Number)
	 * @sampleas setMinutes(Number)
	 *
	 * @param minutesValue
	 * @param secondsValue
	 * @param msValue
	 *
	 */
	@JSFunction
	public void setMinutes(Number minutesValue, Number secondsValue, Number msValue)
	{
	}

	/**
	 * Sets the month of the date.
	 *
	 * @sample date.setMonth(integr);
	 *
	 * @param monthValue
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/setMonth
	 */
	@JSFunction
	public void setMonth(Number monthValue)
	{
	}

	/**
	 * @clonedesc setMonth(Number)
	 * @sampleas setMonth(Number)
	 *
	 * @param monthValue
	 * @param dayValue
	 *
	 */
	@JSFunction
	public void setMonth(Number monthValue, Number dayValue)
	{
	}

	/**
	 * Sets the seconds of the date.
	 *
	 * @sample date.setSeconds(integer);
	 *
	 * @param secondsValue
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/setSeconds
	 */
	@JSFunction
	public void setSeconds(Number secondsValue)
	{
	}

	/**
	 * @clonedesc setSeconds(Number)
	 * @sampleas setSeconds(Number)
	 *
	 * @param secondsValue
	 * @param msValue
	 *
	 */
	@JSFunction
	public void setSeconds(Number secondsValue, Number msValue)
	{
	}

	/**
	 * Sets the milliseconds of the date.
	 *
	 * @sample date.setTime(integer);
	 *
	 * @param timeValue
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/setTime
	 */
	@JSFunction
	public void setTime(Number timeValue)
	{
	}

	/**
	 * Sets the UTC date.
	 *
	 * @sample date.setUTCDate(integer);
	 *
	 * @param dayValue
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/setUTCDate
	 */
	@JSFunction
	public void setUTCDate(Number dayValue)
	{
	}

	/**
	 * Sets the year in UTC time.
	 *
	 * @sample date.setUTCFullYear(integer);
	 *
	 * @param yearValue
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/setUTCFullYear
	 */
	@JSFunction
	public void setUTCFullYear(Number yearValue)
	{
	}

	/**
	 * @clonedesc setUTCFullYear(Number)
	 * @sampleas setUTCFullYear(Number)
	 *
	 * @param yearValue
	 * @param monthValue
	 *
	 */
	@JSFunction
	public void setUTCFullYear(Number yearValue, Number monthValue)
	{
	}

	/**
	 * @clonedesc setUTCFullYear(Number)
	 * @sampleas setUTCFullYear(Number)
	 *
	 * @param yearValue
	 * @param monthValue
	 * @param dayValue
	 *
	 */
	@JSFunction
	public void setUTCFullYear(Number yearValue, Number monthValue, Number dayValue)
	{
	}

	/**
	 * Sets the hours in UTC time.
	 *
	 * @sample date.setUTCHours(integer);
	 *
	 * @param hoursValue
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/setUTCHours
	 */
	@JSFunction
	public void setUTCHours(Number hoursValue)
	{
	}

	/**
	 * @clonedesc setUTCHours(Number)
	 * @sampleas setUTCHours(Number)
	 *
	 * @param hoursValue
	 * @param minutesValue
	 *
	 */
	@JSFunction
	public void setUTCHours(Number hoursValue, Number minutesValue)
	{
	}

	/**
	 * @clonedesc setUTCHours(Number)
	 * @sampleas setUTCHours(Number)
	 *
	 * @param hoursValue
	 * @param minutesValue
	 * @param secondsValue
	 *
	 */
	@JSFunction
	public void setUTCHours(Number hoursValue, Number minutesValue, Number secondsValue)
	{
	}

	/**
	 * @clonedesc setUTCHours(Number)
	 * @sampleas setUTCHours(Number)
	 *
	 * @param hoursValue
	 * @param minutesValue
	 * @param secondsValue
	 * @param msValue
	 *
	 */
	@JSFunction
	public void setUTCHours(Number hoursValue, Number minutesValue, Number secondsValue, Number msValue)
	{
	}

	/**
	 * Sets the milliseconds in UTC time.
	 *
	 * @sample date.setUTCMilliseconds(integer);
	 *
	 * @param millisecondsValue
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/setUTCMilliseconds
	 */
	@JSFunction
	public void setUTCMilliseconds(Number millisecondsValue)
	{
	}

	/**
	 * Sets the minutes in UTC time.
	 *
	 * @sample date.setUTCMinutes(integer);
	 *
	 * @param minutesValue
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/setUTCMinutes
	 */
	@JSFunction
	public void setUTCMinutes(Number minutesValue)
	{
	}

	/**
	 * @clonedesc setUTCMinutes(Number)
	 * @sampleas setUTCMinutes(Number)
	 *
	 * @param minutesValue
	 * @param secondsValue
	 */
	@JSFunction
	public void setUTCMinutes(Number minutesValue, Number secondsValue)
	{
	}

	/**
	 * @clonedesc setUTCMinutes(Number)
	 * @sampleas setUTCMinutes(Number)
	 *
	 * @param minutesValue
	 * @param secondsValue
	 * @param msValue
	 *
	 */
	@JSFunction
	public void setUTCMinutes(Number minutesValue, Number secondsValue, Number msValue)
	{
	}

	/**
	 * Sets the month in UTC time.
	 *
	 * @sample date.setUTCMonth(integer);
	 *
	 * @param monthValue
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/setUTCMonth
	 */
	@JSFunction
	public void setUTCMonth(Number monthValue)
	{
	}

	/**
	 * @clonedesc setUTCMonth(Number)
	 * @sampleas setUTCMonth(Number)
	 *
	 * @param monthValue
	 * @param dayValue
	 *
	 */
	@JSFunction
	public void setUTCMonth(Number monthValue, Number dayValue)
	{
	}

	/**
	 * Sets the seconds in UTC time.
	 *
	 * @sample date.setUTCSeconds(integer);
	 *
	 * @param secondsValue
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/setUTCSeconds
	 */
	@JSFunction
	public void setUTCSeconds(Number secondsValue)
	{
	}

	/**
	 * @clonedesc setUTCSeconds(Number)
	 * @sampleas setUTCSeconds(Number)
	 *
	 * @param secondsValue
	 * @param msValue
	 *
	 */
	@JSFunction
	public void setUTCSeconds(Number secondsValue, Number msValue)
	{
	}

	/**
	 * Sets the year of the date.
	 *
	 * @deprecated Obsolete method, replaced by {@link #setFullYear())}.
	 *
	 * @sample date.setYear(integer);
	 *
	 * @param yearValue
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/setYear
	 */
	@Deprecated
	public void jsFunction_setYear(Number yearValue)
	{
	}

	/**
	 * Returns a string version of the date.
	 *
	 * @sample date.toDateString();
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/toDateString
	 */
	@JSFunction
	public String toDateString()
	{
		return null;
	}

	/**
	 * Returns a string version of the local time zone of the date.
	 *
	 * @sample date.toLocaleDateString();
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/toLocaleDateString
	 */
	@JSFunction
	public String toLocaleDateString()
	{
		return null;
	}

	/**
	 * Returns a string version of the local time zone of the date.
	 *
	 * @sample date.toLocaleString();
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/toLocaleString
	 */
	@JSFunction
	public String toLocaleString()
	{
		return null;
	}

	/**
	 * Returns a string version of the local time zone of the date.
	 *
	 * @sample date.toLocaleTimeString();
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/toLocaleTimeString
	 */
	@JSFunction
	public String toLocaleTimeString()
	{
		return null;
	}

	/**
	 * Returns a string version of the date.
	 *
	 * @sample date.toTimeString();
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/toTimeString
	 */
	@JSFunction
	public String toTimeString()
	{
		return null;
	}

	/**
	 * Returns a string version of the UTC value of the date.
	 *
	 * @sample date.toUTCString();
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/toUTCString
	 */
	@JSFunction
	public String toUTCString()
	{
		return null;
	}

	/**
	 * Return integer milliseconds count
	 *
	 * @sample date.valueOf(integer);
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/valueOf
	 */
	@JSFunction
	public Number valueOf()
	{
		return null;
	}
}
