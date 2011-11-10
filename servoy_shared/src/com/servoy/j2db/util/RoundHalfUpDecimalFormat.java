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

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;


public class RoundHalfUpDecimalFormat extends DecimalFormat
{
	private static ConcurrentHashMap<Locale, DecimalFormatSymbols> decimalFormatSymbols = new ConcurrentHashMap<Locale, DecimalFormatSymbols>();

	public static DecimalFormatSymbols getDecimalFormatSymbols(Locale locale)
	{
		DecimalFormatSymbols dfs = decimalFormatSymbols.get(locale);
		if (dfs == null)
		{
			dfs = new DecimalFormatSymbols(locale);
			decimalFormatSymbols.put(locale, dfs);
		}
		return dfs;
	}

	private boolean minusAtBack = false;

	public RoundHalfUpDecimalFormat(Locale locale)
	{
		this("#,##0.###", locale); //$NON-NLS-1$
	}

	public RoundHalfUpDecimalFormat(String format, Locale locale)
	{
		super(format, getDecimalFormatSymbols(locale));
		if (format.endsWith("-")) //$NON-NLS-1$
		{
			applyPattern(format.substring(0, format.length() - 1));
			minusAtBack = true;
		}
		setParseBigDecimal(true);
	}

	@Override
	public StringBuffer format(double number, StringBuffer result, FieldPosition fieldPosition)
	{
		if ("%".equals(getPositiveSuffix()) || "%".equals(getPositivePrefix())) //$NON-NLS-1$ //$NON-NLS-2$
		{
			return super.format(number, result, fieldPosition);
		}
		else
		{
			// super uses always ROUND_HALF_EVEN and we need ROUND_HALF_UP
			if (minusAtBack && number < 0)
			{
				StringBuffer sb = super.format(getRoundedUpNumber(Math.abs(number)), result, fieldPosition);
				sb.append('-');
				return sb;
			}
			else
			{
				return super.format(getRoundedUpNumber(number), result, fieldPosition);
			}
		}
	}

	private static final BigDecimal one = new BigDecimal(1);

	private double getRoundedUpNumber(double number)
	{
		BigDecimal nr;
		try
		{
			// I am using the String constructor + Double.toString, because numbers such as 0.15
			// are transformed to strings as "0.15" while the BigDecimal constructor that receives doubles will
			// see it as "0.1499999...." - they parse the double differently
			nr = new BigDecimal(Double.toString(number));
		}
		catch (NumberFormatException e)
		{
			// toString incompatible with BigDecimal (should no happen, but you never know what other differences they have)
			nr = new BigDecimal(number);
		}

		// do the round-up
		BigDecimal result = nr.divide(one, getMaximumFractionDigits(), BigDecimal.ROUND_HALF_UP);
		return result.doubleValue();
	}

	@Override
	public StringBuffer format(long number, StringBuffer result, FieldPosition fieldPosition)
	{
		if (minusAtBack && number < 0)
		{
			StringBuffer sb = super.format(Math.abs(number), result, fieldPosition);
			sb.append('-');
			return sb;
		}
		else
		{
			return super.format(number, result, fieldPosition);
		}
	}

	/**
	 * 
	 */

	@Override
	public Number parse(String source, ParsePosition pos)
	{
		if (minusAtBack && source.endsWith("-")) //$NON-NLS-1$
		{
			Number o = super.parse(source.substring(0, source.length() - 1), pos);
			if (o instanceof Double)
			{
				o = new Double(-((Double)o).doubleValue());
			}
			else if (o instanceof Long)
			{
				o = new Long(-((Long)o).longValue());
			}
			return o;
		}
		else if ("".equals(source)) //$NON-NLS-1$
		{
			return null;
		}
		else
		{
			return super.parse(source, pos);
		}
	}
}