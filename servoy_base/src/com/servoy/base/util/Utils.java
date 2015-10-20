/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.base.util;

import java.util.Date;

/**
 * Utility methods
 * @author gboros
 *
 */
public class Utils
{
	public static String createPKHashKey(Object[] pk)
	{
		StringBuilder sb = new StringBuilder();
		if (pk != null)
		{
			for (Object val : pk)
			{
				String str;
				if (val instanceof String && ((String)val).length() == 36 && ((String)val).split("-").length == 5) //$NON-NLS-1$
				{
					// make sure UUID PKs are matched regardless of casing (MSQ Sqlserver returns uppercase UUID strings for uniqueidentifier columns)
					str = ((String)val).toLowerCase();
				}
				else if (val instanceof Date)
				{
					str = Long.toString(((Date)val).getTime());
				}
				else
				{
					str = convertToString(val);
				}
				if (val != null)
				{
					sb.append(str.length());
				}
				sb.append('.');
				sb.append(str);
				sb.append(';');
			}
		}
		return sb.toString();
	}

	/**
	 * Convert to string representation, remove trailing '.0' for numbers.
	 * 
	 * @return the result string
	 */
	public static String convertToString(Object o)
	{
		if (!(o instanceof Number))
		{
			return String.valueOf(o);
		}

		String numberToString = o.toString();
		int i;
		for (i = numberToString.length() - 1; i > 0; i--)
		{
			if (numberToString.charAt(i) != '0')
			{
				break;
			}
		}
		if (numberToString.charAt(i) == '.')
		{
			return numberToString.substring(0, i);
		}
		return numberToString;
	}
}
