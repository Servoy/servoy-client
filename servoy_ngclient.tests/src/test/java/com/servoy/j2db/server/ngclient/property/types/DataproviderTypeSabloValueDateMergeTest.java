package com.servoy.j2db.server.ngclient.property.types;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * SVY-21658: editing a date dataprovider through a field whose format shows only a time (e.g. HH:mm) stored a value
 * one hour off whenever the edit crossed the DST switch on that day.
 * <p>
 * The old merge in {@code browserUpdateReceived} added the wall-clock difference between the old and the new value
 * (both formatted/parsed on 1970-01-01, which has no DST switch) as milliseconds to the old value. On a DST switch
 * day that day is 23 or 25 hours long, so the result was shifted by one hour. These tests pin the replacement
 * {@link DataproviderTypeSabloValue#mergeDateByFormat(Date, Date, String, TimeZone)}, which merges only the calendar
 * fields that the format displays/edits, computed in the client time zone - keeping the date part of the old value
 * for a time-only format and the time part for a date-only format, and being immune to the DST boundary.
 */
public class DataproviderTypeSabloValueDateMergeTest
{
	private static final TimeZone BRUSSELS = TimeZone.getTimeZone("Europe/Brussels");

	/** A Date at the given wall-clock time in the given zone (so the test does not depend on the JVM default zone). */
	private static Date dateIn(TimeZone zone, int year, int month1Based, int day, int hour, int minute, int second)
	{
		Calendar cal = new GregorianCalendar(zone);
		cal.clear();
		cal.set(year, month1Based - 1, day, hour, minute, second);
		return cal.getTime();
	}

	private static void assertWallClock(TimeZone zone, Date actual, int year, int month1Based, int day, int hour,
		int minute, int second)
	{
		Calendar cal = new GregorianCalendar(zone);
		cal.setTime(actual);
		assertEquals(year, cal.get(Calendar.YEAR), "year");
		assertEquals(month1Based - 1, cal.get(Calendar.MONTH), "month");
		assertEquals(day, cal.get(Calendar.DAY_OF_MONTH), "day");
		assertEquals(hour, cal.get(Calendar.HOUR_OF_DAY), "hour");
		assertEquals(minute, cal.get(Calendar.MINUTE), "minute");
		assertEquals(second, cal.get(Calendar.SECOND), "second");
	}

	@Nested
	class TimeOnlyFormatOnDstDays
	{
		/** The reported case: 25/10/2026 is the autumn (fall-back) switch day in Brussels (25 hours long). */
		@Test
		void autumnSwitchDayKeepsDateAndTypedTime()
		{
			Date oldValue = dateIn(BRUSSELS, 2026, 10, 25, 0, 1, 0); // 00:01 on the switch day
			Date typed = dateIn(BRUSSELS, 1970, 1, 1, 7, 0, 0); // browser sends 07:00 (date part is meaningless)

			Date merged = DataproviderTypeSabloValue.mergeDateByFormat(oldValue, typed, "HH:mm", BRUSSELS);

			// expected 25/10/2026 07:00 - the old code produced 06:00
			assertWallClock(BRUSSELS, merged, 2026, 10, 25, 7, 0, 0);
		}

		/** The spring (spring-forward) switch day in Brussels (23 hours long). */
		@Test
		void springSwitchDayKeepsDateAndTypedTime()
		{
			Date oldValue = dateIn(BRUSSELS, 2027, 3, 28, 0, 1, 0); // 00:01 on the switch day
			Date typed = dateIn(BRUSSELS, 1970, 1, 1, 7, 0, 0); // browser sends 07:00

			Date merged = DataproviderTypeSabloValue.mergeDateByFormat(oldValue, typed, "HH:mm", BRUSSELS);

			// expected 28/03/2027 07:00 - the old code produced 08:00
			assertWallClock(BRUSSELS, merged, 2027, 3, 28, 7, 0, 0);
		}

		/** A plain, non-switch day must obviously keep working too. */
		@Test
		void normalDayKeepsDateAndTypedTime()
		{
			Date oldValue = dateIn(BRUSSELS, 2026, 6, 15, 0, 1, 30);
			Date typed = dateIn(BRUSSELS, 1970, 1, 1, 7, 0, 0);

			Date merged = DataproviderTypeSabloValue.mergeDateByFormat(oldValue, typed, "HH:mm", BRUSSELS);

			// only HH:mm are merged, so the original seconds (30) stay
			assertWallClock(BRUSSELS, merged, 2026, 6, 15, 7, 0, 30);
		}
	}

	@Nested
	class FormatDecidesWhichFieldsAreMerged
	{
		/** A date-only format keeps the time part of the old value and only changes the date. */
		@Test
		void dateOnlyFormatKeepsTimeOfOldValue()
		{
			Date oldValue = dateIn(BRUSSELS, 2026, 1, 10, 14, 30, 45);
			Date typed = dateIn(BRUSSELS, 2026, 10, 25, 9, 0, 0); // new date, time part should be ignored

			Date merged = DataproviderTypeSabloValue.mergeDateByFormat(oldValue, typed, "dd-MM-yyyy", BRUSSELS);

			assertWallClock(BRUSSELS, merged, 2026, 10, 25, 14, 30, 45);
		}

		/** A full date-time format merges everything, so the result equals the newly typed value. */
		@Test
		void fullDateTimeFormatTakesNewValue()
		{
			Date oldValue = dateIn(BRUSSELS, 2026, 1, 10, 14, 30, 45);
			Date typed = dateIn(BRUSSELS, 2026, 10, 25, 9, 5, 7);

			Date merged = DataproviderTypeSabloValue.mergeDateByFormat(oldValue, typed, "yyyy-MM-dd HH:mm:ss", BRUSSELS);

			assertWallClock(BRUSSELS, merged, 2026, 10, 25, 9, 5, 7);
		}

		/** Literal text inside single quotes carries no calendar field and must not affect the merge. */
		@Test
		void literalTextInQuotesIsIgnored()
		{
			Date oldValue = dateIn(BRUSSELS, 2026, 10, 25, 0, 1, 0);
			Date typed = dateIn(BRUSSELS, 1970, 1, 1, 7, 30, 0);

			// the quoted 'uur' contains a 'u' (day-number-of-week letter) that must be ignored because it is a literal
			Date merged = DataproviderTypeSabloValue.mergeDateByFormat(oldValue, typed, "HH 'uur' mm", BRUSSELS);

			assertWallClock(BRUSSELS, merged, 2026, 10, 25, 7, 30, 0);
		}

		/** Seconds-precision format keeps the milliseconds of the old value (not in the format). */
		@Test
		void fieldsNotInFormatKeepOldValue()
		{
			Calendar oldCal = new GregorianCalendar(BRUSSELS);
			oldCal.clear();
			oldCal.set(2026, Calendar.JUNE, 15, 0, 1, 0);
			oldCal.set(Calendar.MILLISECOND, 123);
			Date oldValue = oldCal.getTime();
			Date typed = dateIn(BRUSSELS, 1970, 1, 1, 7, 0, 0);

			Date merged = DataproviderTypeSabloValue.mergeDateByFormat(oldValue, typed, "HH:mm", BRUSSELS);

			Calendar mergedCal = new GregorianCalendar(BRUSSELS);
			mergedCal.setTime(merged);
			assertEquals(123, mergedCal.get(Calendar.MILLISECOND), "milliseconds should be kept from the old value");
			assertWallClock(BRUSSELS, merged, 2026, 6, 15, 7, 0, 0);
		}
	}

	@Nested
	class YearAndMonthBoundariesAroundMidnight
	{
		/**
		 * Time-only edit on the last minute of the year: the field shows only HH:mm, so typing an earlier time must
		 * NOT roll the date back a day/year - the date part (31 Dec 2026) stays exactly as it was. This is the kind of
		 * regression the old millisecond-delta merge could cause: a negative wall-clock delta subtracted days.
		 */
		@Test
		void timeOnlyEditOnLastMinuteOfYearKeepsTheDate()
		{
			Date oldValue = dateIn(BRUSSELS, 2026, 12, 31, 23, 59, 0);
			Date typed = dateIn(BRUSSELS, 1970, 1, 1, 0, 0, 0); // user types 00:00

			Date merged = DataproviderTypeSabloValue.mergeDateByFormat(oldValue, typed, "HH:mm", BRUSSELS);

			// stays on 31 Dec 2026, only the time becomes 00:00 - it must not jump to 1 Jan 2027 or back a day
			assertWallClock(BRUSSELS, merged, 2026, 12, 31, 0, 0, 0);
		}

		/**
		 * Time-only edit on the first minute of the year, typing a late time: the date (1 Jan 2027) must stay, it must
		 * not roll back to 31 Dec 2026.
		 */
		@Test
		void timeOnlyEditOnFirstMinuteOfYearKeepsTheDate()
		{
			Date oldValue = dateIn(BRUSSELS, 2027, 1, 1, 0, 0, 0);
			Date typed = dateIn(BRUSSELS, 1970, 1, 1, 23, 59, 0); // user types 23:59

			Date merged = DataproviderTypeSabloValue.mergeDateByFormat(oldValue, typed, "HH:mm", BRUSSELS);

			assertWallClock(BRUSSELS, merged, 2027, 1, 1, 23, 59, 0);
		}

		/** Date-only edit that crosses the year boundary forward: 31 Dec 2026 -> 1 Jan 2027, keeping the old time. */
		@Test
		void dateOnlyEditCrossesYearForwardKeepingTime()
		{
			Date oldValue = dateIn(BRUSSELS, 2026, 12, 31, 23, 59, 30);
			Date typed = dateIn(BRUSSELS, 2027, 1, 1, 8, 0, 0); // new date, time ignored

			Date merged = DataproviderTypeSabloValue.mergeDateByFormat(oldValue, typed, "dd-MM-yyyy", BRUSSELS);

			assertWallClock(BRUSSELS, merged, 2027, 1, 1, 23, 59, 30);
		}

		/** Date-only edit that crosses the year boundary backward: 1 Jan 2027 -> 31 Dec 2026, keeping the old time. */
		@Test
		void dateOnlyEditCrossesYearBackwardKeepingTime()
		{
			Date oldValue = dateIn(BRUSSELS, 2027, 1, 1, 0, 0, 30);
			Date typed = dateIn(BRUSSELS, 2026, 12, 31, 8, 0, 0); // new date, time ignored

			Date merged = DataproviderTypeSabloValue.mergeDateByFormat(oldValue, typed, "dd-MM-yyyy", BRUSSELS);

			assertWallClock(BRUSSELS, merged, 2026, 12, 31, 0, 0, 30);
		}

		/**
		 * Full date-time edit right across midnight into the new year: everything is merged, so the result is exactly
		 * the typed value (1 Jan 2027 00:00:00), proving no off-by-one on the year/month/day fields.
		 */
		@Test
		void fullDateTimeEditAcrossMidnightIntoNewYear()
		{
			Date oldValue = dateIn(BRUSSELS, 2026, 12, 31, 23, 59, 59);
			Date typed = dateIn(BRUSSELS, 2027, 1, 1, 0, 0, 0);

			Date merged = DataproviderTypeSabloValue.mergeDateByFormat(oldValue, typed, "yyyy-MM-dd HH:mm:ss", BRUSSELS);

			assertWallClock(BRUSSELS, merged, 2027, 1, 1, 0, 0, 0);
		}

		/**
		 * Time-only edit on the last minute of a month that is longer than the typed month context. Because only the
		 * time fields are touched, the 31st stays the 31st (no clamp to the 30th or similar).
		 */
		@Test
		void timeOnlyEditOnLastDayOfLongMonthKeepsDay()
		{
			Date oldValue = dateIn(BRUSSELS, 2026, 1, 31, 23, 59, 0); // 31 Jan
			Date typed = dateIn(BRUSSELS, 1970, 1, 1, 0, 1, 0); // user types 00:01

			Date merged = DataproviderTypeSabloValue.mergeDateByFormat(oldValue, typed, "HH:mm", BRUSSELS);

			assertWallClock(BRUSSELS, merged, 2026, 1, 31, 0, 1, 0);
		}
	}
}
