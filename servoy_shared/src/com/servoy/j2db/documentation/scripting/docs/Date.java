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
 * @author gerzse
 */
@ServoyDocumented(category = ServoyDocumented.JSLIB, publicName = "Date", scriptingName = "Date")
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
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/now
	 */
	public Date js_now()
	{
		return null;
	}

	/**
	 * Takes a date string (such as "Dec 25, 1995") and returns the number of milliseconds since January 1, 1970, 00:00:00 UTC.
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/parse
	 */
	public Date js_parse(String s)
	{
		return null;
	}

	/**
	 * Takes comma-delimited date parameters and returns the number of milliseconds between January 1, 1970, 00:00:00, universal time and the specified time. 
	 * 
	 * @param year A year after 1900.
	 * @param month A number between 0 and 11.
	 * @param date optional A number between 1 and 31.
	 * @param hrs optional A number between 0 and 23.
	 * @param min optional A number between 0 and 59.
	 * @param sec optional A number between 0 and 59.
	 * @param ms optional A number between 0 and 999.
	 *
	 * @sample
	 * // The number of milliseconds in the first minute after 1970 January 1st.
	 * application.output(Date.UTC(1970, 00, 01, 00, 01, 00, 00)); // prints: 60000.0
	 *
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/UTC
	 */
	public Date js_UTC(Number year, Number month, Number date, Number hrs, Number min, Number sec, Number ms)
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
	public Number jsFunction_getDate()
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
	public Number js_getDay()
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
	public Number js_getFullYear()
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
	public Number js_getHours()
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
	public Number jsFunction_getMilliseconds()
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
	public Number js_getMinutes()
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
	public Number js_getMonth()
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
	public Number js_getSeconds()
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
	public Number jsFunction_getTime()
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
	public Number js_getTimezoneOffset()
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
	public Number jsFunction_getUTCDate()
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
	public Number js_getUTCDay()
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
	public Number js_getUTCFullYear()
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
	public Number js_getUTCHours()
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
	public Number jsFunction_getUTCMilliseconds()
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
	public Number js_getUTCMinutes()
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
	public Number js_getUTCMonth()
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
	public Number js_getUTCSeconds()
	{
		return null;
	}

	/**
	 * Gets the year of the date.
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
	public void jsFunction_setDate(Number dayValue)
	{
	}

	/**
	 * Sets the full year of the date.
	 *
	 * @sample date.setFullYear(integer);
	 * 
	 * @param yearValue 
	 * @param monthValue optional 
	 * @param dayValue optional
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/setFullYear
	 */
	public void js_setFullYear(Number yearValue, Number monthValue, Number dayValue)
	{
	}

	/**
	 * Sets the hours of the date.
	 *
	 * @sample date.setHours(integer);
	 * 
	 * @param hoursValue 
	 * @param minutesValue optional 
	 * @param secondsValue optional
	 * @param msValue optional
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/setHours
	 */
	public void js_setHours(Number hoursValue, Number minutesValue, Number secondsValue, Number msValue)
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
	public void jsFunction_setMilliseconds(Number millisecondsValue)
	{
	}

	/**
	 * Sets the minutes of the date.
	 *
	 * @sample date.setMinutes(integer);
	 * 
	 * @param minutesValue 
	 * @param secondsValue optional
	 * @param msValue optional
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/setMinutes
	 */
	public void js_setMinutes(Number minutesValue, Number secondsValue, Number msValue)
	{
	}

	/**
	 * Sets the month of the date.
	 *
	 * @sample date.setMonth(integr);
	 * 
	 * @param monthValue 
	 * @param dayValue optional 
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/setMonth
	 */
	public void js_setMonth(Number monthValue, Number dayValue)
	{
	}

	/**
	 * Sets the seconds of the date.
	 *
	 * @sample date.setSeconds(integer);
	 * 
	 * @param secondsValue 
	 * @param msValue optional
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/setSeconds
	 */
	public void js_setSeconds(Number secondsValue, Number msValue)
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
	public void jsFunction_setTime(Number timeValue)
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
	public void jsFunction_setUTCDate(Number dayValue)
	{
	}

	/**
	 * Sets the year in UTC time.
	 *
	 * @sample date.setUTCFullYear(integer);
	 * 
	 * @param yearValue 
	 * @param monthValue optional 
	 * @param dayValue optional
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/setUTCFullYear
	 */
	public void js_setUTCFullYear(Number yearValue, Number monthValue, Number dayValue)
	{
	}

	/**
	 * Sets the hours in UTC time.
	 *
	 * @sample date.setUTCHours(integer);
	 * 
	 * @param hoursValue 
	 * @param minutesValue optional 
	 * @param secondsValue optional
	 * @param msValue optional
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/setUTCHours
	 */
	public void js_setUTCHours(Number hoursValue, Number minutesValue, Number secondsValue, Number msValue)
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
	public void jsFunction_setUTCMilliseconds(Number millisecondsValue)
	{
	}

	/**
	 * Sets the minutes in UTC time.
	 *
	 * @sample date.setUTCMinutes(integer);
	 * 
	 * @param minutesValue 
	 * @param secondsValue optional
	 * @param msValue optional
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/setUTCMinutes
	 */
	public void js_setUTCMinutes(Number minutesValue, Number secondsValue, Number msValue)
	{
	}

	/**
	 * Sets the month in UTC time.
	 *
	 * @sample date.setUTCMonth(integer);
	 * 
	 * @param monthValue 
	 * @param dayValue optional 
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/setUTCMonth
	 */
	public void js_setUTCMonth(Number monthValue, Number dayValue)
	{
	}

	/**
	 * Sets the seconds in UTC time.
	 *
	 * @sample date.setUTCSeconds(integer);
	 * 
	 * @param secondsValue 
	 * @param msValue optional
	 * 
	 * @link https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/setUTCSeconds
	 */
	public void js_setUTCSeconds(Number secondsValue, Number msValue)
	{
	}

	/**
	 * Sets the year of the date.
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
	public String js_toDateString()
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
	public String js_toLocaleDateString()
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
	public String js_toLocaleString()
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
	public String js_toLocaleTimeString()
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
	public String js_toTimeString()
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
	public String js_toUTCString()
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
	public Number js_valueOf()
	{
		return null;
	}
}
