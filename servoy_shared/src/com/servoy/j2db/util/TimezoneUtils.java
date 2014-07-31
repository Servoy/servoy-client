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

import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.IServiceProvider;

/**
 * This class contains utility methods for handling time zones. WC and SC need different conversions at SQLEngine level.
 * @see ClientManager#showStringDatesTheSameOnAllClients.
 */
public class TimezoneUtils
{

	public static long convertToTimezone(long date, TimeZone sourceTimeZone, TimeZone destinationTimeZone)
	{
		if (sourceTimeZone == null || destinationTimeZone == null) return date;

		if (sourceTimeZone.equals(destinationTimeZone)) return date;

		GregorianCalendar source = new GregorianCalendar(sourceTimeZone);
		GregorianCalendar destination = new GregorianCalendar(destinationTimeZone);

		source.setTimeInMillis(date);

		destination.set(Calendar.YEAR, source.get(Calendar.YEAR));
		destination.set(Calendar.MONTH, source.get(Calendar.MONTH));
		destination.set(Calendar.DAY_OF_MONTH, source.get(Calendar.DAY_OF_MONTH));
		destination.set(Calendar.HOUR_OF_DAY, source.get(Calendar.HOUR_OF_DAY));
		destination.set(Calendar.MINUTE, source.get(Calendar.MINUTE));
		destination.set(Calendar.SECOND, source.get(Calendar.SECOND));
		destination.set(Calendar.MILLISECOND, source.get(Calendar.MILLISECOND));

		if (Debug.tracing())
		{
			Debug.trace("converting date: " + date + " to " + destination.getTimeInMillis() + " source timezone:" + sourceTimeZone + " dest: " +
				destinationTimeZone);
		}

		return destination.getTimeInMillis();
	}

	public static long convertOutgoingDate(long date, TimeZone clientTimeZone, TimeZone serverTimeZone, boolean smartClient)
	{
		if (smartClient)
		{
			return convertToTimezone(date, serverTimeZone, clientTimeZone);
		}
		else
		{
			return convertToTimezone(date, clientTimeZone, serverTimeZone);
		}
	}

	public static Date convertIncomingDate(Date date, TimeZone clientTimeZone, TimeZone serverTimeZone, boolean smartClient)
	{
		long ms;
		if (smartClient)
		{
			ms = convertToTimezone(date.getTime(), clientTimeZone, serverTimeZone);
		}
		else
		{
			ms = convertToTimezone(date.getTime(), serverTimeZone, clientTimeZone);
		}

		if (date.getClass() == Timestamp.class)
		{
			return new Timestamp(ms);
		}
		if (date.getClass() == Time.class)
		{
			return new Time(ms);
		}
		if (date.getClass() == java.sql.Date.class)
		{
			return new java.sql.Date(ms);
		}
		else return new Date(ms);
	}

	public static Date getClientDate(IServiceProvider application)
	{
		try
		{
			// because of the fact that in non-SC dates format is applied on server timezone, if clients are set to display dates according
			// to their timezone info, non-SC dates are shifted by the time zone difference when used in JS; see ClientManager.getConversionTimezone()...
			// so simle new Date() in this case would really be wrong (unfortunately this problem also manifests when using simple new Date() as JS object...)
			if (application instanceof IApplication && Utils.isSwingClient(((IApplication)application).getApplicationType()))
			{
				return new Date();
			}
			else
			{
				return new Date(TimezoneUtils.convertToTimezone(System.currentTimeMillis(), application.getTimeZone(), TimeZone.getDefault()));
			}
		}
		catch (Exception e)
		{
			// should never happen (remote exception for non RMI client)
			Debug.warn(e);
			return new Date();
		}
	}

}