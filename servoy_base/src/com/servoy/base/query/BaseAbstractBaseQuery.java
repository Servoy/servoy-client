/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.base.query;

import java.util.Arrays;


/**
 * @author rgansevles
 *
 */
public class BaseAbstractBaseQuery
{
	public static int hashCode(Object[] array)
	{
		final int PRIME = 31;
		if (array == null) return 0;
		int result = 1;
		for (Object element : array)
		{
			result = PRIME * result + (element == null ? 0 : element.hashCode());
		}
		return result;
	}

	static int hashCode(int[] array)
	{
		final int PRIME = 31;
		if (array == null) return 0;
		int result = 1;
		for (int element : array)
		{
			result = PRIME * result + element;
		}
		return result;
	}


	public static boolean arrayEquals(Object value1, Object value2)
	{
		if (value1 == null)
		{
			return value2 == null;
		}
		if (value1 instanceof Object[][] && value2 instanceof Object[][] && ((Object[][])value1).length == ((Object[][])value2).length)
		{
			Object[][] array = (Object[][])value1;
			for (int i = 0; i < array.length; i++)
			{
				if (!Arrays.equals(array[i], ((Object[][])value2)[i]))
				{
					return false;
				}
			}
			return true;
		}
		if (value1 instanceof Object[] && value2 instanceof Object[])
		{
			return Arrays.equals((Object[])value1, (Object[])value2);
		}
		return value1.equals(value2);
	}

	private static int arrays_hashCode(Object a[])
	{
		if (a == null) return 0;

		int result = 1;

		for (Object element : a)
			result = 31 * result + (element == null ? 0 : element.hashCode());

		return result;
	}

	public static int arrayHashcode(Object value)
	{
		if (value == null)
		{
			return 0;
		}
		if (value instanceof Object[][])
		{
			int hash = 0;
			Object[][] array = (Object[][])value;
			for (Object[] element : array)
			{
				hash = 31 * hash + arrays_hashCode(element);
			}
		}
		else if (value instanceof Object[])
		{
			return arrays_hashCode((Object[])value);
		}
		return value.hashCode();
	}

	public static String toString(Object o)
	{
		if (o == null)
		{
			return "<null>"; //$NON-NLS-1$
		}

		if (o instanceof Object[])
		{
			Object[] array = (Object[])o;
			StringBuilder sb = new StringBuilder().append('[');
			for (int i = 0; i < array.length; i++)
			{
				if (i > 0)
				{
					sb.append(',');
				}
				sb.append(toString(array[i]));
			}
			return sb.append(']').toString();
		}

		if (o instanceof int[])
		{
			int[] array = (int[])o;
			StringBuilder sb = new StringBuilder().append('[');
			for (int i = 0; i < array.length; i++)
			{
				if (i > 0)
				{
					sb.append(',');
				}
				sb.append(array[i]);
			}
			return sb.append(']').toString();
		}

		if (o instanceof byte[])
		{
			return "0x" + new String(encodeHex((byte[])o)); //$NON-NLS-1$
		}

		return o.toString();
	}

	/**
	 * Used building output as Hex
	 */
	private static final char[] DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	/**
	 * Converts an array of bytes into an array of characters representing the hexidecimal values of each byte in order. The returned array will be double the
	 * length of the passed array, as it takes two characters to represent any given byte.
	 * 
	 * @param data a byte[] to convert to Hex characters
	 * @return A char[] containing hexidecimal characters
	 */
	public static char[] /* Hex. */encodeHex(byte[] data)
	{
		int l = data.length;

		char[] out = new char[l << 1];

		// two characters form the hex value.
		for (int i = 0, j = 0; i < l; i++)
		{
			out[j++] = DIGITS[(0xF0 & data[i]) >>> 4];
			out[j++] = DIGITS[0x0F & data[i]];
		}

		return out;
	}
}
