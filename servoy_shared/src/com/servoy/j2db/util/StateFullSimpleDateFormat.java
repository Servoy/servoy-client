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
package com.servoy.j2db.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

/**
 * Only changes the field in the object which are being formatted, keeps the other fields intact
 *
 * @author jcompagner, jblok
 */
public class StateFullSimpleDateFormat extends SimpleDateFormat
{
	private static final Pattern datePattern = Pattern.compile("[^GyMwWDdFE]+");

	private static final long serialVersionUID = 1L;

	private final Calendar stateFullCalendar;

	private boolean rollInsteadOfAdd = false;

	public StateFullSimpleDateFormat(String pattern, boolean lenient)
	{
		this(pattern, TimeZone.getDefault(), Locale.getDefault(), lenient);
	}

	public StateFullSimpleDateFormat(String pattern, TimeZone zone, Locale locale, boolean lenient)
	{
		super(checkPattern(pattern), locale == null ? Locale.getDefault() : locale);
		if (datePattern.matcher(pattern).matches()) zone = null;
		if (zone == null) zone = TimeZone.getDefault();
		if (locale == null) locale = Locale.getDefault();
		calendar = new GregorianCalendar(zone, locale)
		{
			private static final long serialVersionUID = 1L;

			/**
			 * @see java.util.Calendar#set(int, int)
			 */
			@Override
			public void set(int field, int value)
			{
				if (field == Calendar.YEAR && value == 0)
				{
					// when editing, sometimes (using backspace) the year may be set to 0 which is not a valid date in GregorianCalendar.
					// Setting year to 0 switches to era to BC.
					return;
				}
				super.set(field, value);
				stateFullCalendar.set(field, value);
			}

			@Override
			public void add(int field, int amount)
			{
				if (rollInsteadOfAdd)
				{
					super.roll(field, amount);
				}
				else
				{
					super.add(field, amount);
				}
			}

		};
		setLenient(lenient);

		stateFullCalendar = Calendar.getInstance(zone, locale);
		setTimeZone(zone);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.text.DateFormat#parse(java.lang.String)
	 */
	public Date parse(String source, Date original) throws ParseException
	{
		stateFullCalendar.setTime(original);
		return super.parse(source);
	}

	public void setOriginal(Date original)
	{
		if (original == null)
		{
			stateFullCalendar.clear();
		}
		else
		{
			stateFullCalendar.setTime(original);
		}
	}

	/**
	 * @return
	 */
	public Date getMergedDate()
	{
		return stateFullCalendar.getTime();
	}

	public void setRollInsteadOfAdd(boolean rollInsteadOfAdd)
	{
		this.rollInsteadOfAdd = rollInsteadOfAdd;
	}

	// returns the pattern if it is valid, else returns a default pattern
	private static String checkPattern(String pattern)
	{
		try
		{
			new SimpleDateFormat(pattern);
			return pattern;
		}
		catch (Exception ex)
		{
			Debug.error("Invalid date pattern : '" + pattern + "', continue using default pattern ...", ex); //$NON-NLS-1$ //$NON-NLS-2$
			return new SimpleDateFormat().toPattern();
		}
	}
}
