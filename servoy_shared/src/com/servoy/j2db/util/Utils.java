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

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SimpleTimeZone;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import javax.print.attribute.Size2DSyntax;
import javax.print.attribute.standard.MediaSize;

import org.apache.commons.codec.binary.Base64;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeError;
import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.Wrapper;
import org.mozilla.javascript.xml.XMLObject;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.IEventDelegator;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.MediaURLStreamHandler;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.Record;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.persistence.ISupportExtendsID;
import com.servoy.j2db.scripting.IScriptable;
import com.servoy.j2db.scripting.IScriptableProvider;
import com.servoy.j2db.ui.runtime.HasRuntimeClientProperty;
import com.servoy.j2db.util.docvalidator.IdentDocumentValidator;

import de.rtner.security.auth.spi.PBKDF2Engine;
import de.rtner.security.auth.spi.PBKDF2HexFormatter;
import de.rtner.security.auth.spi.PBKDF2Parameters;

/**
 * Utility methods
 * Normal Use: static methods <br>
 *
 * @author jblok
 */
public final class Utils
{
	/**
	 * The password hash prefix if it is the new PBKDF2 password or a md5 hash.
	 */
	private static final String PBKDF2_PREFIX = "PBKDF2:"; //$NON-NLS-1$

	// Client platforms
	public static final int PLATFORM_OTHER = 0;
	public static final int PLATFORM_WINDOWS = 1;
	public static final int PLATFORM_MAC = 2;
	public static final int PLATFORM_LINUX = 3;


	public static boolean isInheritedFormElement(Object element, IPersist context)
	{
		if (element instanceof Form)
		{
			return false;
		}
		if (context instanceof Form && element instanceof IPersist && (((IPersist)element).getAncestor(IRepository.FORMS) != context))
		{
			if (element instanceof IPersist && (((IPersist)element).getAncestor(IRepository.FORMS) != context))
			{
				// child of super-form, readonly
				return true;
			}
		}
		if (element instanceof FormElementGroup)
		{
			Iterator<IFormElement> elements = ((FormElementGroup)element).getElements();
			while (elements.hasNext())
			{
				if (isInheritedFormElement(elements.next(), context))
				{
					return true;
				}
			}
		}
		if (element instanceof ISupportExtendsID)
		{
			return PersistHelper.isOverrideElement((ISupportExtendsID)element);
		}
		// child of this form, not of a inherited form
		return false;
	}

	/**
	 * Change the passed class name to its corresponding file name. E.G. change &quot;Utilities&quot; to &quot;Utilities.class&quot;.
	 *
	 * @param name Class name to be changed.
	 *
	 * @throws IllegalArgumentException If a null <TT>name</TT> passed.
	 */
	public static String changeClassNameToFileName(String name)
	{
		if (name == null)
		{
			throw new IllegalArgumentException("Class Name == null"); //$NON-NLS-1$
		}
		return name.replace('.', '/').concat(".class"); //$NON-NLS-1$
	}

	/**
	 * Change the passed file name to its corresponding class name. E.G. change &quot;Utilities.class&quot; to &quot;Utilities&quot;.
	 *
	 * @param name Class name to be changed. If this does not represent a Java class then <TT>null</TT> is returned.
	 *
	 * @throws IllegalArgumentException If a null <TT>name</TT> passed.
	 */
	public static String changeFileNameToClassName(String name)
	{
		if (name == null)
		{
			throw new IllegalArgumentException("File Name == null"); //$NON-NLS-1$
		}
		String className = null;
		if (name.toLowerCase().endsWith(".class")) //$NON-NLS-1$
		{
			className = name.replace('/', '.');
			className = className.replace('\\', '.');
			className = className.substring(0, className.length() - 6);
		}
		return className;
	}

	/**
	 * Insert an array into another array at a certain position. Both arrays may be null, resulting array will be extended to fit. Element type will be
	 * preserved.
	 *
	 * @param src
	 * @param toAdd
	 * @param position
	 * @param n
	 * @return the resulting array
	 */
	public static <T> T[] arrayInsert(T[] src, Object[] toAdd, int position, int n)
	{
		if (src == null && toAdd == null)
		{
			// nothing to add
			return null;
		}

		T[] res;
		if (src == null)
		{
			res = (T[])java.lang.reflect.Array.newInstance(toAdd.getClass().getComponentType(), position + n);
			System.arraycopy(toAdd, 0, res, position, Math.min(toAdd.length, n));
		}
		else
		{
			res = (T[])java.lang.reflect.Array.newInstance(src.getClass().getComponentType(), Math.max(src.length, position) + n);
			if (position > 0 && src.length > 0)
			{
				System.arraycopy(src, 0, res, 0, Math.min(src.length, position));
			}
			if (position < src.length)
			{
				System.arraycopy(src, position, res, position + n, src.length - position);
			}
			if (toAdd != null)
			{
				System.arraycopy(toAdd, 0, res, position, Math.min(toAdd.length, n));
			}
		}
		return res;
	}

	/**
	 * Join 2 arrays into 1. Element type will be preserved.
	 *
	 * @param array1
	 * @param array2
	 * @return the resulting array
	 */
	public static <T> T[] arrayJoin(T[] array1, Object[] array2)
	{
		if (array1 == null || (array1.length == 0 && array2 != null))
		{
			return (T[])array2;
		}
		if (array2 == null || array2.length == 0)
		{
			return array1;
		}

		return arrayInsert(array1, array2, array1.length, array2.length);
	}

	/**
	 * Add an element to an array. Element type will be preserved.
	 *
	 * @param array
	 * @param element
	 * @param append
	 * @return the resulting array
	 */
	public static <T> T[] arrayAdd(T[] array, T element, boolean append)
	{
		T[] res;
		if (array == null)
		{
			res = (T[])java.lang.reflect.Array.newInstance(element == null ? Object.class : element.getClass(), 1);
		}
		else
		{
			res = (T[])java.lang.reflect.Array.newInstance(array.getClass().getComponentType(), array.length + 1);
			System.arraycopy(array, 0, res, append ? 0 : 1, array.length);
		}
		res[append ? res.length - 1 : 0] = element;
		return res;
	}

	/**
	 * Merge two arrays in 1, the upperArray will be overlaid onto the lowerArray.
	 *
	 * <p>
	 * For example:
	 *
	 * <br>
	 * upper = [x, y] lower = [a,b,c] => overlaid = [x, y, c]
	 *
	 * <br>
	 * upper = [a, b c] lower = [x, y] => overlaid = [a, b, c]
	 *
	 * @param upperAarray
	 * @param lowerAarray
	 */
	public static <T> T[] arrayMerge(T[] upperAarray, T[] lowerAarray)
	{
		if (upperAarray == null)
		{
			return lowerAarray;
		}
		if (lowerAarray == null || lowerAarray.length <= upperAarray.length)
		{
			return upperAarray;
		}

		// both arrays filled and lowerArray is longer than upperArray
		T[] mergedArgs = (T[])java.lang.reflect.Array.newInstance(upperAarray.getClass().getComponentType(), lowerAarray.length);

		System.arraycopy(upperAarray, 0, mergedArgs, 0, upperAarray.length);
		System.arraycopy(lowerAarray, upperAarray.length, mergedArgs, upperAarray.length, lowerAarray.length - upperAarray.length);
		return mergedArgs;
	}


	public static <T> List<T> asList(Iterator< ? extends T> it)
	{
		List<T> lst = new ArrayList<T>();
		while (it.hasNext())
		{
			lst.add(it.next());
		}
		return lst;
	}

	public static <T> T[] asArray(Iterator< ? extends T> it, Class<T> clazz)
	{
		List<T> lst = asList(it);
		return lst.toArray((T[])java.lang.reflect.Array.newInstance(clazz, lst.size()));
	}

	public static <T> Iterator<T> asSortedIterator(Iterator< ? extends T> it, Comparator< ? super T> comparator)
	{
		Object[] array = asList(it).toArray();
		Arrays.sort(array, (Comparator<Object>)comparator);
		return (Iterator<T>)Arrays.asList(array).iterator();
	}

	/**
	 * Get a sub-array. Element type will be preserved.
	 *
	 * @param array
	 * @param beginIndex
	 * @param endIndex
	 * @return the resulting array
	 */
	public static <T> T[] arraySub(T[] array, int beginIndex, int endIndex)
	{
		if (array == null || (beginIndex == 0 && endIndex == array.length))
		{
			return array;
		}

		if (beginIndex > endIndex)
		{
			throw new IllegalArgumentException("arraySub: " + beginIndex + '>' + endIndex); //$NON-NLS-1$
		}

		T[] res = (T[])java.lang.reflect.Array.newInstance(array.getClass().getComponentType(), endIndex - beginIndex);
		System.arraycopy(array, beginIndex, res, 0, endIndex - beginIndex);
		return res;
	}

	/*
	 * _____________________________________________________________ Declaration of attributes
	 */

	/*
	 * _____________________________________________________________ Declaration and definition of constructors
	 */
	private Utils()
	{
	}

	/*
	 * _____________________________________________________________ The methods below belong to this class
	 */
	/**
	 * Try to parse the given string as a long
	 *
	 * @param s the string to parse
	 * @return the parsed long - or 0 (zero) if the parse doesn't succeed
	 */
	public static long getAsLong(String s)
	{
		return getAsLong(s, false);
	}

	/**
	 * Try to parse the given object as a long
	 *
	 * @param o the object (String, Number, ...) to parse
	 * @return the parsed long - or 0 (zero) if the parse doesn't succeed
	 */
	public static long getAsLong(Object o)
	{
		return getAsLong(o, false);
	}

	/**
	 * Try to parse the given string as a long
	 *
	 * @param s the string to parse
	 * @param throwOnException whether or not to throw a RuntimeException on failure
	 * @return the parsed long - or 0 (zero) if the parse doesn't succeed and throwOnException is false
	 */
	public static long getAsLong(String s, boolean throwOnException)
	{
		if (s == null) return 0l;
		try
		{
			// Note: very big longs may parse incorrectly due to precision loss (should not be a problem in the Servoy context).
			return new Double(s.replace(',', '.')).longValue();
		}
		catch (Exception ex)
		{
			if (throwOnException)
			{
				if (ex instanceof RuntimeException) throw (RuntimeException)ex;
				else throw new RuntimeException(ex.getMessage());
			}
			else
			{
				return 0l;
			}
		}
	}

	/**
	 * Try to parse the given object as a long
	 *
	 * @param o the Object (Number, String, ...) to parse
	 * @param throwOnException whether or not to throw a RuntimeException on failure
	 * @return the parsed long - or 0 (zero) if the parse doesn't succeed and throwOnException is false
	 */
	public static long getAsLong(Object o, boolean throwOnException)
	{
		if (o == null) return 0;
		if (o instanceof Number)
		{
			return ((Number)o).longValue();
		}
		if (o instanceof Boolean)
		{
			return ((Boolean)o).booleanValue() ? 1 : 0;
		}
		return getAsLong(o.toString(), throwOnException);
	}

	/**
	 * Try to parse the given string as an integer
	 *
	 * @param s the string to parse
	 * @return the parsed integer - or 0 (zero) if the parse doesn't succeed
	 */
	public static int getAsInteger(String s)
	{
		if (s == null) return 0;
		try
		{
			return new Double(s.replace(',', '.')).intValue();
		}
		catch (Exception ex)
		{
			return 0;
		}
	}

	/**
	 * Try to parse the given string as an integer
	 *
	 * @param s the string to parse
	 * @param throwOnException whether or not to throw a RuntimeException on failure
	 * @return the parsed integer - or 0 (zero) if the parse doesn't succeed and throwOnException is false
	 * @throws RuntimeException in case of trouble when throwOnException is true
	 */
	public static int getAsInteger(String s, boolean throwOnException)
	{
		if (s == null) return 0;
		try
		{
			return new Double(s.replace(',', '.')).intValue();
		}
		catch (Exception ex)
		{
			if (throwOnException)
			{
				if (ex instanceof RuntimeException) throw (RuntimeException)ex;
				else throw new RuntimeException(ex.getMessage());
			}
			else
			{
				return 0;
			}
		}
	}

	/**
	 * Try to parse the given string as a double
	 *
	 * @param s the string to parse
	 * @return the parsed double - or 0 (zero) if the parse doesn't succeed
	 */
	public static double getAsDouble(String s)
	{
		if (s == null) return 0;
		try
		{
			return new Double(s.replace(',', '.')).doubleValue();
		}
		catch (Exception ex)
		{
			return 0;
		}
	}

	/**
	 * Try to parse the given string as a double
	 *
	 * @param s the string to parse
	 * @param throwOnFail whether or not to throw a RuntimeException on failure
	 * @return the parsed double - or 0 (zero) if the parse doesn't succeed and throwOnFail is false
	 */
	public static double getAsDouble(String s, boolean throwOnFail)
	{
		if (s == null) return 0;
		try
		{
			return new Double(s.replace(',', '.')).doubleValue();
		}
		catch (Exception ex)
		{
			if (throwOnFail)
			{
				if (ex instanceof RuntimeException) throw (RuntimeException)ex;
				else throw new RuntimeException(ex.getMessage());
			}
			else
			{
				return 0;
			}
		}
	}

	/*
	 * public static String getAsMoney(String s) { NumberFormat nf = NumberFormat.getCurrencyInstance(); return nf.format(getAsDouble(s)); }
	 */
	/**
	 * Try to parse the given string as a boolean
	 *
	 * @param s the string to parse
	 * @return the boolean (false if the parse doesn't succeed)
	 */
	public static boolean getAsBoolean(String s)
	{
		if (s == null) return false;
		s = s.toLowerCase();

		boolean retval = false;
		if (s.startsWith("1")) //$NON-NLS-1$
		{
			retval = true;
		}
		else if (s.startsWith("y")) //$NON-NLS-1$
		{
			retval = true;
		}
		else if (s.startsWith("t")) //$NON-NLS-1$
		{
			retval = true;
		}
		return retval;
	}

	/**
	 * Try to parse the given object as a boolean
	 *
	 * @param o the object (Boolean, Number, String, ...) to parse
	 * @return the boolean (false if the parse doesn't succeed)
	 */
	public static boolean getAsBoolean(Object o)
	{
		if (o == null) return false;
		if (o instanceof Boolean)
		{
			return ((Boolean)o).booleanValue();
		}
		else if (o instanceof Number)
		{
			return (((Number)o).intValue() > 0);
		}
		else
		{
			return getAsBoolean(o.toString());
		}
	}

	public static BigDecimal roundNumber(Object number, int precision, boolean throwOnFail)
	{
		MathContext mathContext = new MathContext(precision);
		if (number instanceof BigDecimal)
		{
			return ((BigDecimal)number).round(mathContext);
		}
		return new BigDecimal(Utils.getAsDouble(number, throwOnFail)).round(mathContext);
	}


	/**
	 * Format a given number (Visitor (locale) specific)
	 *
	 * @param param the input number
	 * @param digits number of digits
	 * @return the formatted number
	 */
	public static String formatNumber(Locale locale, String param, String digits)
	{
		return formatNumber(locale, getAsDouble(param), getAsInteger(digits));
	}

	/**
	 * Checks whether the <code>string</code> is considered empty. Empty means that the string may contain whitespace, but no visible characters.
	 *
	 * "\n\t " is considered empty, while " a" is not.
	 *
	 * @param str the string
	 * @return true if the string is null or ""
	 */
	public static boolean stringIsEmpty(final CharSequence str)
	{
		return str == null || str.length() == 0 || str.toString().trim().length() == 0;
	}

	public static CharSequence stringLimitLenght(final CharSequence str, int length)
	{
		if (str == null) return null;
		return (str.length() > length ? str.subSequence(0, length) : str);
	}

	public static boolean stringContainsIgnoreCase(final CharSequence source, String contains)
	{
		if (stringIsEmpty(source) || stringIsEmpty(contains)) return false;
		return (source.toString().toLowerCase().contains(contains.toLowerCase()));
	}

	/**
	 * Compares two strings no matter if they are null
	 *
	 * @param left string
	 * @param right string
	 * @return true if they are the same
	 */
	public static boolean stringSafeEquals(String left, String right)
	{
		return safeEquals(left, right);
	}

	/**
	 * Compares two objects no matter if they are null
	 *
	 * @param left object
	 * @param right object
	 * @return true if they are the same
	 */
	public static boolean safeEquals(Object left, Object right)
	{
		if (left == null)
		{
			return right == null;
		}
		return left.equals(right);
	}

	/**
	 * Format a given number (Visitor (locale) specific)
	 *
	 * @param param the input number
	 * @param digits number of digits
	 * @return the formatted number
	 */
	public static String formatNumber(Locale locale, double param, int digits)
	{
		RoundHalfUpDecimalFormat nf = new RoundHalfUpDecimalFormat(locale);
		int digits_nr = digits;
		nf.setMaximumFractionDigits(digits_nr);
		nf.setMinimumFractionDigits(digits_nr);
		return nf.format(param);
	}

	/**
	 * Format a given number of miliseconds in a formatted time
	 *
	 * Note:if the time (in milliseconds) is smaller than ~month, it is calulated without a timezone)
	 *
	 * @param msec the miliseconds (current time can be get by 'new java.util.Date().getTime()')
	 * @param format the display format (format used from java.text.SimpleDateFormat!)
	 * @return the formatted time
	 * @see java.text.SimpleDateFormat
	 */
	public static String formatTime(int msec, String format)
	{
		return formatTime((long)msec, format);
	}

	/**
	 * Format a given number of miliseconds in a formatted time
	 *
	 * Note:if the time (in milliseconds) is smaller than ~month, it is calulated without a timezone)
	 *
	 * @param msec the miliseconds (current time can be get by 'new java.util.Date().getTime()')
	 * @param format the display format
	 * @return the formatted time
	 * @see java.text.SimpleDateFormat
	 */
	public static String formatTime(long msec, String format)
	{
		Date d = new Date(msec);
		SimpleDateFormat sdf = new SimpleDateFormat(format == null ? "yyyy.MM.dd G 'at' hh:mm:ss z" : format); //$NON-NLS-1$
		if (msec < 2678400000L && msec >= 0) //if smaller than a ~month
		{
			//format the time without timezone (GMT +0)
			//now it is possible to format for example telephone calling seconds to a formatted time (hh:mm:ss)
			//otherwise the timezone is involed
			sdf.setTimeZone(new SimpleTimeZone(0, "GMT")); //$NON-NLS-1$
		}
		return sdf.format(d);
	}

	/**
	 * Create a Date (time) from a String, returns null on failure<br>
	 * Example: Timestamp t = Utils.parseDate("23-06-1975 6:08 AM", "dd-MM-yyyy hh:mm a"); <br>
	 *
	 * @param datetime the date as formatted string
	 * @param format the format to be used
	 * @return the Timestamp object
	 * @see java.text.SimpleDateFormat
	 */
	public static Timestamp parseDate(String datetime, String format)
	{
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		Date d = sdf.parse(datetime, new ParsePosition(0));
		return new Timestamp(d.getTime());
	}

	/**
	 * Parse a javascript string into a java string, example parseJSString("'HelloWorld'") returns:HelloWorld
	 * @param o
	 *
	 * @return the parsed object
	 */
	public static Object parseJSExpression(Object o)
	{
		return parseJSExpression(o, Types.OTHER);
	}

	/**
	 *   The same as parseJSExpression but try to convert the object to the given type parameter
	 * @param  type from java.sql.Types  ,  java.sql.Types.OTHER to get the behavior of parseJSExpression(Object o)
	 */
	public static Object parseJSExpression(Object o, int type)
	{
		if (o instanceof String)
		{
			int tp = Column.mapToDefaultType(type);

			String s = ((String)o).trim();
			if ("".equals(s)) return null;
			if (tp == Types.OTHER || type == Types.BOOLEAN || type == Types.BIT)
			{
				if ("true".equals(s)) return Boolean.TRUE;
				if ("false".equals(s)) return Boolean.FALSE;
				if (type == Types.BOOLEAN || type == Types.BIT) return null;
			}
			if (tp == Types.OTHER || tp == IColumnTypes.NUMBER)
			{
				try
				{
					return Double.valueOf(s);
				}
				catch (NumberFormatException e)
				{
					if (tp != Types.OTHER) return null;
				}
			}

			if (tp == IColumnTypes.INTEGER)
			{
				try
				{
					return Integer.valueOf(s);
				}
				catch (NumberFormatException e)
				{
					if ("true".equals(s)) return Boolean.TRUE;
					if ("false".equals(s)) return Boolean.FALSE;
					return null;
				}
			}

			if (tp == Types.OTHER || tp == IColumnTypes.TEXT)
			{
				if ((s.charAt(0) == '\'' && s.charAt(s.length() - 1) == '\'') || (s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"'))
				{
					return s.substring(1, s.length() - 1);
				}
			}

			return null;
		}

		// non-string, keep original
		return o;
	}

	public static Object[] parseJSExpressions(List<Object> exprs)
	{
		if (exprs == null) return null;
		Object[] parsed = new Object[exprs.size()];
		for (int i = 0; i < parsed.length; i++)
		{
			parsed[i] = parseJSExpression(exprs.get(i));
		}
		return parsed;
	}

	/**
	 * Represent a primitive as js string.
	 * Booleans and Numbers are converted to their string representation.
	 * Strings are quoted with single quotes.
	 *
	 * @see #parseJSExpression(Object) reverse method
	 *
	 * @param o
	 * @return the result string
	 */
	public static String makeJSExpression(Object o)
	{
		if (o instanceof CharSequence)
		{
			return '\'' + o.toString().replaceAll("'", "\\\\$0") + '\''; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return o == null ? null : o.toString();
	}

	//exact word match
	public static String stringReplaceExact(String org, String source, String destination)
	{
		return stringReplace(org, source, destination, -1, true, false);
	}

	/**
	 * Method for replacing part of a string with a string
	 *
	 * @param org the orginal string
	 * @param source the string to search
	 * @param destination the string to replace
	 * @return the result
	 */
	public static String stringReplace(String org, String source, String destination)
	{
		return stringReplace(org, source, destination, -1, false, false);
	}

	public static String stringReplaceCaseInsensitiveSearch(String org, String source, String destination)
	{
		return stringReplace(org, source, destination, -1, false, true);
	}

	public static String stringReplace(String org, String source, String destination, int replaceOccurence)
	{
		return stringReplace(org, source, destination, replaceOccurence, false, false);
	}

	public static String stringReplace(String org, String source, String destination, int replaceOccurence, boolean mustExact, boolean caseInsensitiveSearch)
	{
		if (org == null) return null;
		if (source == null || source.length() == 0) return org;
		String searchOrg = org;
		if (caseInsensitiveSearch)
		{
			searchOrg = org.toLowerCase();
			source = source.toLowerCase();
		}
		int index = searchOrg.indexOf(source);
		if (index != -1)
		{
			int occurence = 0;
			StringBuilder sb = new StringBuilder();
			int startIndex = 0;
			boolean isExact = true;
			while (index != -1)
			{
				if (mustExact)
				{
					//check left
					if (index > 0)
					{
						isExact = !Character.isJavaIdentifierStart(org.charAt(index - 1));
					}
					//check right
					if ((index + source.length() < org.length()) && isExact)
					{
						isExact = !Character.isJavaIdentifierStart(org.charAt(index + source.length()));
					}
				}
				sb.append(org.substring(startIndex, index));
				if (mustExact)
				{
					if (isExact)
					{
						sb.append(destination);
					}
					else
					{
						sb.append(source);
					}
				}
				else
				{
					if (replaceOccurence < 0 || replaceOccurence == occurence)
					{
						sb.append(destination);
					}
					else
					{
						sb.append(source);
					}
				}

				startIndex = index + source.length();
				index = searchOrg.indexOf(source, startIndex);
				occurence++;
			}
			sb.append(org.substring(startIndex, org.length()));//add tail
			return sb.toString();
		}
		else
		{
			return org;
		}
	}

	public static String stringReplaceRecursive(String org, String source, String destination)
	{
		if (org == null) return null;
		if (source.length() == 0) return org;
		while (org.indexOf(source) != -1)
		{
			org = stringReplace(org, source, destination);
		}
		return org;
	}

	/**
		 * Removes all substrings between '<' and corresponding '>'. If the number of '<' characters differs from the number of '>' characters, the behavior is
		 * undetermined.
		 *
		 * @param str the initial string.
		 * @return a string equal to the given string from which all substrings between '<' and corresponding '>' were removed.
		 */
	public static String stringRemoveTags(String str)
	{
		if (str == null) return null;
		StringBuilder sb = new StringBuilder(str.length());
		int opened = 0;
		char ch;

		for (int i = 0; i < str.length(); i++)
		{
			ch = str.charAt(i);
			if (ch == '<')
			{
				opened++;
			}
			else
			{
				if (opened == 0)
				{
					sb.append(ch);
				}
				else if (ch == '>')
				{
					opened--;
				}
			}
		}
		return sb.toString();
	}

	/**
	 * Escape quotes in a string, handle already escaped quotes
	 *
	 * @param str
	 * @param quote
	 */
	public static String stringEscapeQuote(String str, char quote)
	{
		if (str == null || ((str.indexOf('\\') < 0) && str.indexOf(quote) < 0)) return str;

		StringBuilder sb = new StringBuilder(str.length() + 10);
		for (char c : str.toCharArray())
		{
			if (c == '\\' || c == quote)
			{
				sb.append('\\');
			}
			sb.append(c);
		}

		return sb.toString();
	}

	public static String stringJoin(Object[] array, char separator)
	{
		if (array == null) return null;
		return stringJoin(Arrays.asList(array).iterator(), separator);
	}

	public static String stringJoin(Object[] array, String separator)
	{
		if (array == null) return null;
		return stringJoin(Arrays.asList(array).iterator(), separator);
	}

	/**
	 * <p>
	 * Joins the elements of the provided <code>Iterator</code> into a single String containing the provided elements.
	 * </p>
	 *
	 * <p>
	 * No delimiter is added before or after the list. Null objects or empty strings within the iteration are represented by empty strings.
	 * </p>
	 *
	 * @param iterator the <code>Iterator</code> of values to join together, may be null
	 * @param separator the separator character to use
	 * @return the joined String, <code>null</code> if null iterator input
	 */
	public static String stringJoin(Iterator< ? > iterator, char separator)
	{
		if (iterator == null)
		{
			return null;
		}
		StringBuilder buf = new StringBuilder(256); // Java default is 16, probably too small
		while (iterator.hasNext())
		{
			Object obj = iterator.next();
			if (obj != null)
			{
				buf.append(obj);
			}
			if (iterator.hasNext())
			{
				buf.append(separator);
			}
		}
		return buf.toString();
	}

	public static String stringJoin(Iterator< ? > iterator, String separator)
	{
		if (iterator == null)
		{
			return null;
		}
		StringBuilder buf = new StringBuilder(256); // Java default is 16, probably too small
		while (iterator.hasNext())
		{
			Object obj = iterator.next();
			if (obj != null)
			{
				buf.append(obj);
			}
			if (iterator.hasNext())
			{
				buf.append(separator);
			}
		}
		return buf.toString();
	}

	/**
	 * Converts a Java String to an HTML markup string, but does not convert normal spaces to non-breaking space entities (&lt;nbsp&gt;).
	 *
	 * @param s The string to be escaped
	 * @see Utils#escapeMarkup(String, boolean)
	 * @return The escaped string
	 */
	@Deprecated
	public static CharSequence escapeMarkup(final String s)
	{
		return HtmlUtils.escapeMarkup(s);
	}

	/**
	 * Converts a Java String to an HTML markup String by replacing illegal characters with HTML entities where appropriate. Spaces are converted to
	 * non-breaking spaces (&lt;nbsp&gt;) if escapeSpaces is true, tabs are converted to four non-breaking spaces, less than signs are converted to &amp;lt;
	 * entities and greater than signs to &amp;gt; entities.
	 *
	 * @param s The string to escape
	 * @param escapeSpaces True to replace ' ' with nonbreaking space
	 * @return The escaped string
	 */
	@Deprecated
	public static CharSequence escapeMarkup(final String s, final boolean escapeSpaces)
	{
		return HtmlUtils.escapeMarkup(s, escapeSpaces);
	}

	/**
	 * Converts a Java String to an HTML markup String by replacing illegal characters with HTML entities where appropriate. Spaces are converted to
	 * non-breaking spaces (&lt;nbsp&gt;) if escapeSpaces is true, tabs are converted to four non-breaking spaces, less-than signs are converted to &amp;lt;
	 * entities and greater-than signs to &amp;gt; entities.
	 *
	 * @param s The string to escape
	 * @param escapeSpaces True to replace ' ' with nonbreaking space
	 * @param convertToHtmlUnicodeEscapes True to convert non-7 bit characters to unicode HTML (&#...)
	 * @return The escaped string
	 */
	@Deprecated
	public static CharSequence escapeMarkup(final String s, final boolean escapeSpaces, final boolean convertToHtmlUnicodeEscapes)
	{
		return HtmlUtils.escapeMarkup(s, escapeSpaces, convertToHtmlUnicodeEscapes);
	}

	/**
	 * Converts a String to multiline HTML markup by replacing newlines with line break entities (&lt;br/&gt;) and multiple occurrences of newline with
	 * paragraph break entities (&lt;p&gt;).
	 *
	 * @param s String to transform
	 * @return String with all single occurrences of newline replaced with &lt;br/&gt; and all multiple occurrences of newline replaced with &lt;p&gt;.
	 */
	public static CharSequence toMultilineMarkup(final CharSequence s)
	{
		if (s == null)
		{
			return null;
		}

		final StringBuilder buffer = new StringBuilder();
		int newlineCount = 0;

		buffer.append("<p>"); //$NON-NLS-1$
		for (int i = 0; i < s.length(); i++)
		{
			final char c = s.charAt(i);

			switch (c)
			{
				case '\n' :
					newlineCount++;
					break;

				case '\r' :
					break;

				default :
					if (newlineCount == 1)
					{
						buffer.append("<br/>"); //$NON-NLS-1$
					}
					else if (newlineCount > 1)
					{
						buffer.append("</p><p>"); //$NON-NLS-1$
					}

					buffer.append(c);
					newlineCount = 0;
					break;
			}
		}
		if (newlineCount == 1)
		{
			buffer.append("<br/>"); //$NON-NLS-1$
		}
		else if (newlineCount > 1)
		{
			buffer.append("</p><p>"); //$NON-NLS-1$
		}
		buffer.append("</p>"); //$NON-NLS-1$
		return buffer;
	}

	/**
	 * Format a given number (Visitor (locale) specific)
	 *
	 * @param param the input number
	 * @param digits number of digits
	 * @return the formatted number
	 */
	public static String formatNumber(Locale locale, double param, String digits)
	{
		return formatNumber(locale, param, getAsInteger(digits));
	}

	/**
	 * Format a given number (Visitor (locale) specific)
	 *
	 * @param param the input number
	 * @param digits number of digits
	 * @return the formatted number
	 */
	public static String formatNumber(Locale locale, Object param, String digits)
	{
		return formatNumber(locale, getAsDouble(param), getAsInteger(digits));
	}

	/**
	 * Try to parse the given object as a double
	 *
	 * @param o the object (Number, String, ...) to parse
	 * @return the parsed double - or 0 (zero) if the parse doesn't succeed
	 */
	public static double getAsDouble(Object o)
	{
		if (o == null) return 0;
		if (o instanceof Number)
		{
			return ((Number)o).doubleValue();
		}
		else
		{
			return getAsDouble(o.toString());
		}
	}

	/**
	 * Try to parse the given object as a double
	 *
	 * @param o the object (Number, String, ...) to parse
	 * @param throwOnFail whether or not to throw a RuntimeException on failure
	 * @return the parsed double - or 0 (zero) if the parse doesn't succeed and throwOnFail is false
	 */
	public static double getAsDouble(Object o, boolean throwOnFail)
	{
		if (o == null) return 0;
		if (o instanceof Number)
		{
			return ((Number)o).doubleValue();
		}
		else
		{
			return getAsDouble(o.toString(), throwOnFail);
		}
	}

	/**
	 * Try to parse the given object as a float
	 *
	 * @param o the object (Number, String, ...) to parse
	 * @return the parsed float or 0 (zero) if the parse doesn't succeed
	 */
	public static float getAsFloat(Object o)
	{
		if (o == null) return 0;
		if (o instanceof Number)
		{
			return ((Number)o).floatValue();
		}
		else
		{
			return getAsFloat(o.toString());
		}
	}

	/**
	 * Try to parse the given object as a float
	 *
	 * @param o the object (Number, String, ...) to parse
	 * @param throwOnException whether or not to throw a RuntimeException on failure
	 * @return the parsed float - or 0 (zero) if the parse doesn't succeed and throwOnException is false
	 */
	public static float getAsFloat(Object o, boolean throwOnException)
	{
		if (o == null) return 0;
		if (o instanceof Number)
		{
			return ((Number)o).floatValue();
		}
		else
		{
			return getAsFloat(o.toString(), throwOnException);
		}
	}

	/**
	 * Try to parse the given string as a float
	 *
	 * @param s the string to parse
	 * @return the float - or 0 (zero) if the parse doesn't succeed
	 */
	public static float getAsFloat(String s)
	{
		if (s == null) return 0;
		try
		{
			return new Float(s.replace(',', '.')).floatValue();
		}
		catch (Exception ex)
		{
			return 0;
		}
	}

	/**
	 * Try to parse the given string as a float
	 *
	 * @param s the string to parse
	 * @param throwOnException whether or not to throw a RuntimeException on failure
	 * @return the float - or 0 (zero) if the parse doesn't succeed if throwOnException is false
	 */
	public static float getAsFloat(String s, boolean throwOnException)
	{
		if (s == null) return 0;
		try
		{
			return new Float(s.replace(',', '.')).floatValue();
		}
		catch (Exception ex)
		{
			if (throwOnException)
			{
				if (ex instanceof RuntimeException) throw (RuntimeException)ex;
				else throw new RuntimeException(ex.getMessage());
			}
			else
			{
				return 0;
			}
		}
	}

	/**
	 * Try to parse the given object as a UUID
	 *
	 * @param o the object to try
	 * @param throwOnException whether or not to throw a RuntimeException on failure
	 * @return UUID or null if o is not recognized
	 * @return the UUID - or null if the parse doesn't succeed if throwOnException is false
	 */
	public static UUID getAsUUID(Object o, boolean throwOnException)
	{
		if (o == null)
		{
			return null;
		}
		try
		{
			if (o instanceof byte[] && ((byte[])o).length == 16)
			{
				return new UUID((byte[])o);
			}

			if (o instanceof String)
			{
				return UUID.fromString((String)o);
			}

			if (o instanceof UUID)
			{
				return (UUID)o;
			}
			if (o instanceof NativeJavaObject)
			{
				return getAsUUID(((NativeJavaObject)o).unwrap(), throwOnException);
			}
		}
		catch (RuntimeException e)
		{
			if (throwOnException)
			{
				throw e;
			}
		}
		String msg = "Could not parse UUID from object " + o + " type " + o.getClass().getName(); //$NON-NLS-1$ //$NON-NLS-2$
		if (throwOnException)
		{
			throw new RuntimeException(msg);
		}
		Debug.trace(msg);
		return null;
	}

	/*
	 * _____________________________________________________________ The methods below override methods from superclass <classname>
	 */

	/*
	 * _____________________________________________________________ The methods below belong to interface <interfacename>
	 */

	/**
	 * count the numbers in a string
	 *
	 * @param s the string with the numbers
	 * @return the count
	 */
	public static int countNumbers(String s)
	{
		if (s == null) return 0;
		String p = findNumberEx(s);
		StringTokenizer tk = new StringTokenizer(p, " "); //$NON-NLS-1$
		return tk.countTokens();
	}

	/**
	 * hardcore try to find the last number in a string
	 *
	 * @param s the string with the number
	 * @return only the digits
	 */
	public static String findLastNumber(String s)
	{
		if (s == null) return null;
		String p = findNumberEx(s);
		int index = p.lastIndexOf(" "); // incase it is shomething like '100 fl. (45 EUR)' //$NON-NLS-1$
		if (index != -1)
		{
			p = p.substring(index + 1, p.length());
		}
		//		p = p.replace(',','.'); //replace '35,0' to become '35.0'
		//		System.out.println("lastnumber "+p);
		return p;
	}

	/**
	 * hardcore try to find a number in a string
	 *
	 * @param s the string with the number
	 * @return only the digits
	 */
	public static String findNumber(String s)
	{
		if (s == null) return null;
		String p = findNumberEx(s);
		int index = p.indexOf(" "); // incase it is shomething like '100 fl. (45 EUR)' //$NON-NLS-1$
		if (index != -1)
		{
			p = p.substring(0, index);
		}
		if (p.endsWith(",") || p.endsWith(".")) //$NON-NLS-1$ //$NON-NLS-2$
		{
			p = p.substring(0, p.length() - 1);
		}
		//		p = p.replace(',','.'); //replace '35,0' to become '35.0'
		return p;
	}

	/**
	 * hardcore try to find a number in a string
	 *
	 * @param s the string with the number
	 * @return only the digits
	 */
	private static String findNumberEx(String s)
	{
		if (s.endsWith(",") || s.endsWith(".")) s = s.substring(0, s.length() - 1); //$NON-NLS-1$ //$NON-NLS-2$
		if (s.startsWith(",") || s.startsWith(".")) s = s.substring(1); //$NON-NLS-1$ //$NON-NLS-2$
		char[] array = s.toCharArray();
		boolean founddigit = false;
		for (int i = 0; i < array.length; i++)
		{
			char currentchar = array[i];
			if (!founddigit && Character.isDigit(currentchar))
			{
				founddigit = true;
			}
			if (!Character.isDigit(currentchar) && currentchar != ',')
			{
				if (currentchar == '.')//the price has many times dots for example 'kr. 1000','fl.100',etc
				{
					if (!founddigit)
					{
						array[i] = ' ';
					}
				}
				else
				{
					array[i] = ' ';
				}
			}
		}
		String p = new String(array);
		p = p.trim();
		return p;
	}

	/*
	 * _____________________________________________________________ The methods below belong to this class
	 */
	/**
	 * Try to parse the given object as an integer
	 *
	 * @param o the object (Number, String, ...) to parse
	 * @return the parsed integer - or 0 (zero) if the parse doesn't succeed
	 */
	public static int getAsInteger(Object o)
	{
		if (o == null) return 0;
		if (o instanceof Number)
		{
			return ((Number)o).intValue();
		}
		if (o instanceof Boolean)
		{
			return ((Boolean)o).booleanValue() ? 1 : 0;
		}
		return getAsInteger(o.toString(), false);
	}

	/**
	 * Try to parse the given object as an integer
	 *
	 * @param o the object (Number, String, ...) to parse
	 * @param throwOnException whether to throw an exception if parsing failed
	 * @return the parsed integer - or 0 (zero) if the parse doesn't succeed and throwOnException is false
	 */
	public static int getAsInteger(Object o, boolean throwOnException)
	{
		if (o == null) return 0;
		if (o instanceof Number)
		{
			return ((Number)o).intValue();
		}
		if (o instanceof Boolean)
		{
			return ((Boolean)o).booleanValue() ? 1 : 0;
		}
		return getAsInteger(o.toString(), throwOnException);
	}

	public static String getURLContent(URL url)
	{
		StringBuffer sb = new StringBuffer();
		String charset = null;
		try
		{
			URLConnection connection = url.openConnection();
			InputStream is = connection.getInputStream();
			final String type = connection.getContentType();
			if (type != null)
			{
				final String[] parts = type.split(";");
				for (int i = 1; i < parts.length && charset == null; i++)
				{
					final String t = parts[i].trim();
					final int index = t.toLowerCase().indexOf("charset=");
					if (index != -1) charset = t.substring(index + 8);
				}
			}
			InputStreamReader isr = null;
			if (charset != null) isr = new InputStreamReader(is, charset);
			else isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			int read = 0;
			while ((read = br.read()) != -1)
			{
				sb.append((char)read);
			}
			br.close();
			isr.close();
			is.close();
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return sb.toString();
	}

	public static byte[] getURLContent(String url)
	{
		return getURLContent(url, null);
	}

	public static byte[] getURLContent(String url, IServiceProvider serviceProvider)
	{
		ByteArrayOutputStream sb = new ByteArrayOutputStream();
		try
		{
			URL u;
			if (serviceProvider != null && url.startsWith(MediaURLStreamHandler.MEDIA_URL_DEF))
			{
				u = new URL(null, url, new MediaURLStreamHandler(serviceProvider));
			}
			else
			{
				u = new URL(url);
			}
			InputStream is = u.openStream();
			BufferedInputStream bis = new BufferedInputStream(is);
			Utils.streamCopy(bis, sb);
			bis.close();
			is.close();
		}
		catch (Exception e)
		{
			Debug.error(e);
			return null;
		}
		return sb.toByteArray();
	}

	/**
	 * Doesn't close the input stream.
	 * @param is
	 */
	public static byte[] getBytesFromInputStream(InputStream is) throws IOException
	{
		ByteArrayOutputStream sb = new ByteArrayOutputStream();
		BufferedInputStream bis = new BufferedInputStream(is);
		Utils.streamCopy(bis, sb);
		bis.close();
		return sb.toByteArray();
	}

	//if you want to be notified every x bytes
	public static int streamCopy(InputStream is, OutputStream os, ActionListener l, int actionSize) throws IOException
	{
		int actionBlock = actionSize;

		if (is == null || os == null) return 0;
		int bufferSize = 128;
		byte[] buffer = new byte[bufferSize];
		int length = 0;
		int totalLength = 0;
		while ((length = is.read(buffer)) >= 0)
		{
			os.write(buffer, 0, length);
			totalLength += length;

			if (actionSize > 0 && totalLength > actionBlock)
			{
				actionBlock += actionSize;
				if (l != null) l.actionPerformed(new ActionEvent("streamCopy", ActionEvent.ACTION_PERFORMED, "streamCopy")); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return totalLength;
	}

	public static int streamCopy(InputStream is, OutputStream os) throws IOException
	{
		return streamCopy(is, os, null, 0);
	}

	public static int readerWriterCopy(Reader is, Writer os) throws IOException
	{
		int bufferSize = 128;
		char[] buffer = new char[bufferSize];
		int length = 0;
		while ((length = is.read(buffer)) >= 0)
		{
			os.write(buffer, 0, length);
		}
		return length;
	}

	public static void rollback(Connection connection)
	{
		try
		{
			if (connection != null)
			{
				connection.rollback();
			}
		}
		catch (SQLException e)
		{
			Debug.error(e);
		}
	}

	public static <T extends Closeable> T close(T closeable)
	{
		try
		{
			if (closeable != null)
			{
				closeable.close();
			}
		}
		catch (IOException e)
		{
			Debug.error(e);
		}
		return null;
	}

	public static <T extends Closeable> T closeQuietly(T closeable)
	{
		try
		{
			if (closeable != null)
			{
				closeable.close();
			}
		}
		catch (IOException e)
		{
			//ignore
		}
		return null;
	}

	public static <T extends Connection> T closeConnection(T connection)
	{
		try
		{
			if (connection != null)
			{
				if (!connection.isClosed())
				{
					connection.close();
				}
			}
		}
		catch (SQLException e)
		{
			Debug.error(e);
		}
		return null;
	}

	public static <T extends Statement> T closeStatement(T statement)
	{
		try
		{
			if (statement != null)
			{
				statement.close();
			}
		}
		catch (SQLException e)
		{
			Debug.error(e);
		}
		return null;
	}

	public static <T extends ResultSet> T closeResultSet(T resultSet)
	{
		try
		{
			if (resultSet != null)
			{
				resultSet.close();
			}
		}
		catch (SQLException e)
		{
			Debug.error(e);
		}
		return null;
	}

	public static void releaseConnection(Connection connection)
	{
		if (connection != null && connection instanceof ITransactionConnection)
		{
			((ITransactionConnection)connection).release();
		}
	}

	public static String calculateMD5HashBase64(String password)
	{
		String result = null;
		try
		{
			MessageDigest md = MessageDigest.getInstance("MD5"); //$NON-NLS-1$
			byte[] hash = md.digest(password.getBytes("UTF-8")); //$NON-NLS-1$
			result = encodeBASE64(hash).trim();
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return result;
	}

	public static String calculateAndPrefixPBKDF2PasswordHash(String password)
	{
		return PBKDF2_PREFIX + calculatePBKDF2(password, 9999);
	}

	/**
	 * Hashes the given string with the PKCS/PBKDF2 algoritme see http://en.wikipedia.org/wiki/PBKDF2 for more information
	 *
	 * @param textString The string to hash
	 * @param iterations Number of hash iterations to be done (should be higher then 1000)
	 * @return the hash of the string
	 */
	@SuppressWarnings("nls")
	public static String calculatePBKDF2(String textString, int iterations)
	{
		try
		{
			SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
			byte[] salt = new byte[8];
			sr.nextBytes(salt);
			PBKDF2Parameters p = new PBKDF2Parameters("HmacSHA1", "ISO-8859-1", salt, iterations);
			PBKDF2Engine e = new PBKDF2Engine(p);
			p.setDerivedKey(e.deriveKey(textString));
			return new PBKDF2HexFormatter().toString(p);
		}
		catch (NoSuchAlgorithmException e)
		{
			Debug.error("No SHA1 algorime found under the name SHA1PRNG", e);
		}
		return null;
	}


	public static boolean validatePrefixedPBKDF2Hash(String password, String hash)
	{
		if (hash.startsWith(PBKDF2_PREFIX))
		{
			return validatePBKDF2Hash(password, hash.substring(PBKDF2_PREFIX.length()));
		}
		return false;
	}

	public static boolean validatePBKDF2Hash(String password, String hash)
	{
		PBKDF2Parameters p = new PBKDF2Parameters();
		p.setHashAlgorithm("HmacSHA1");
		p.setHashCharset("ISO-8859-1");
		if (new PBKDF2HexFormatter().fromString(p, hash))
		{
			return false;
		}
		PBKDF2Engine e = new PBKDF2Engine(p);
		return e.verifyKey(password);
	}


	@Deprecated
	public static String calculateMD5Hash(String input)
	{
		return calculateMD5HashBase64(input);
	}

	public static String calculateMD5HashBase16(String input)
	{
		String result = null;
		try
		{
			MessageDigest md = MessageDigest.getInstance("MD5"); //$NON-NLS-1$
			byte[] messageDigest = md.digest(input.getBytes("UTF-8")); //$NON-NLS-1$
			BigInteger number = new BigInteger(1, messageDigest);
			result = number.toString(16);
			if (result.length() < 32)
			{
				for (int i = 1; i <= 32 - result.length(); i++)
					result = '0' + result;
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return result;
	}

	public static String encodeBASE64(byte[] data)
	{
		String result = null;
		try
		{
			result = Base64.encodeBase64String(data).trim();
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return result;
	}

	public static byte[] decodeBASE64(String data)
	{
		byte[] result = null;
		try
		{
			result = Base64.decodeBase64(data);
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return result;
	}

	/**
	 * will give you the index in the string of the first char in the char array that is found in that string.
	 *
	 * @param string
	 * @param chars
	 * @return the index of the first char found or -1 if no char is found
	 */
	public static int firstIndexOf(String string, char[] chars)
	{
		return firstIndexOf(string, chars, 0);
	}

	/**
	 * will give you the index in the string of the first char in the char array that is found in that string
	 * starting at the startIndex
	 *
	 * @param string
	 * @param chars
	 * @param startIndex
	 * @return the index of the first char found or -1 if no char is found
	 */
	public static int firstIndexOf(String string, char[] chars, int startIndex)
	{
		if (startIndex < 0 || string.length() <= startIndex) return -1;

		String charsString = new String(chars);
		int counter = startIndex;
		while (counter < string.length())
		{
			char ch = string.charAt(counter);
			if (charsString.indexOf(ch) != -1) return counter;
			counter++;
		}
		return -1;
	}

	public static int stringIndexOf(String string, int ch, int escape)
	{
		return stringIndexOf(string, ch, escape, 0);
	}

	public static int stringIndexOf(String string, int ch, int escape, int beginIndex)
	{
		int i = string.indexOf(ch, beginIndex);
		while (i > 0 && string.charAt(i - 1) == escape)
		{
			i = string.indexOf(ch, i + 1);
		}
		return i;
	}

	public static String unescape(String string, int escape)
	{
		StringBuilder buffer = new StringBuilder(string.length());
		int i = string.indexOf(escape), b = 0;
		while (i >= 0)
		{
			buffer.append(string.substring(b, i));
			if (i + 1 < string.length())
			{
				buffer.append(string.charAt(i + 1));
			}
			b = i + 2;
			i = string.indexOf(string, b);
		}
		if (b < string.length())
		{
			buffer.append(string.substring(b));
		}
		return buffer.toString();
	}

	public static String[] stringSplit(String string, int separator, int escape)
	{
		int i = stringIndexOf(string, separator, escape);
		return i >= 0 ? new String[] { unescape(string.substring(0, i), escape), string.substring(i + 1) } : new String[] { unescape(string, escape), null };
	}

	public static String[] stringSplit(final String s, final String split)
	{
		if (s == null)
		{
			return new String[0];
		}
		final List<String> strings = new ArrayList<String>();
		int pos = 0;
		while (true)
		{
			int next = s.indexOf(split, pos);
			if (next == -1)
			{
				strings.add(s.substring(pos));
				break;
			}
			else
			{
				strings.add(s.substring(pos, next));
			}
			pos = next + 1;
		}
		final String[] result = new String[strings.size()];
		strings.toArray(result);
		return result;
	}

	public static String longToHexString(long n, int digits)
	{
		StringBuilder buffer = new StringBuilder(Long.toHexString(n));
		if (buffer.length() > digits) return buffer.substring(buffer.length() - digits);
		while (buffer.length() < digits)
			buffer.insert(0, '0');
		return buffer.toString();
	}

	public static int compare(double d1, double d2)
	{
		if (d1 < d2) return -1; // Neither val is NaN, thisVal is smaller
		if (d1 > d2) return 1; // Neither val is NaN, thisVal is larger
		long thisBits = Double.doubleToLongBits(d1);
		long anotherBits = Double.doubleToLongBits(d2);
		return (thisBits == anotherBits ? 0 : // Values are equal
			(thisBits < anotherBits ? -1 : // (-0.0, 0.0) or (!NaN, NaN)
				1)); // (0.0, -0.0) or (NaN, !NaN)
	}

//	public static int compare(String s1, String s2)
//	{
//		if (s1 == null) return s2 == null ? 0 : -1;
//		if (s2 == null) return 1;
//		return s1.compareTo(s2);
//	}
//
//	public static boolean arrayContentEquals(Object[] a, Object[] a2)
//	{
//		if (a == a2) return true;
//		if (a == null || a2 == null) return false;
//
//		int length = a.length;
//		if (a2.length != length) return false;
//
//		for (int i = 0; i < length; i++)
//		{
//			Object o1 = a[i];
//			Object o2 = a2[i];
//			if (o1 instanceof ISupportContentEquals && o2 instanceof ISupportContentEquals)
//			{
//				if (!((ISupportContentEquals)o1).contentEquals(o2)) return false;
//			}
//			else if (!(o1 == null ? o2 == null : o1.equals(o2)))
//			{
//				return false;
//			}
//		}
//		return true;
//	}

	public static InputStream closeInputStream(InputStream is)
	{
		return close(is);
	}

	public static Reader closeReader(Reader r)
	{
		return close(r);
	}

	public static void invokeLater(IEventDelegator delegator, List<Runnable> runnables)
	{
		if (runnables != null)
		{
			for (Runnable r : runnables)
			{
				delegator.invokeLater(r);
			}
		}
	}

//	@Deprecated
//	public static String firstLetterToUpperCase(String string)
//	{
//		return stringInitCap(string);
//		if (string == null || string.length() < 1) return string;
//		return string.substring(0, 1).toUpperCase() + string.substring(1);
//	}

	public static Object mapToNullIfUnmanageble(Object value)
	{
		if (value instanceof Undefined || value == Scriptable.NOT_FOUND)
		{
			return null;
		}
		if (value instanceof Number)
		{
			if (Double.isNaN(((Number)value).doubleValue()))
			{
				return null;
			}
			if (Double.isInfinite(((Number)value).doubleValue()))
			{
				return null;
			}
		}
		return value;
	}

	public static final float PPI = 72f; // standard number of pixels per inch

	public static double convertPageFormatUnit(int oldUnit, int newUnit, double value)
	{
		return new MediaSize(0, (float)value, oldUnit).getY(newUnit);
	}

	/**
	 * Create a PageFormat object from the dimensions and margins.
	 *
	 * @param width the actual paper width - ignoring orientation. It is the width of the paper as seen by the printer.
	 * @param height the actual paper height - ignoring orientation. It is the height of the paper as seen by the printer.
	 * @param lm left margin of the page, not paper. So this is the left margin affected by orientation, as used in application.
	 * @param rm right margin of the page, not paper. So this is the right margin affected by orientation, as used in application.
	 * @param tm top margin of the page, not paper. So this is the top margin affected by orientation, as used in application.
	 * @param bm bottom margin of the page, not paper. So this is the bottom margin affected by orientation, as used in application.
	 * @param orientation the orientation of the page. Establishes a relation between page and paper coordinates.
	 * @param units INCHES or MM.
	 * @return the required PageFormat object.
	 */
	public static PageFormat createPageFormat(double width, double height, double lm, double rm, double tm, double bm, int orientation, int units)
	{
		double pixWidth = convertPageFormatUnit(units, Size2DSyntax.INCH, width) * PPI;
		double pixHeight = convertPageFormatUnit(units, Size2DSyntax.INCH, height) * PPI;
		double pixLm = convertPageFormatUnit(units, Size2DSyntax.INCH, lm) * PPI;
		double pixRm = convertPageFormatUnit(units, Size2DSyntax.INCH, rm) * PPI;
		double pixTm = convertPageFormatUnit(units, Size2DSyntax.INCH, tm) * PPI;
		double pixBm = convertPageFormatUnit(units, Size2DSyntax.INCH, bm) * PPI;

		// The margins of the Paper object are relative to the physical paper, so independent
		// of the text orientation; The PageFormat object takes the orientation into account relative to the text.
		// We have to convert back to the paper-relative margins here...
		double paperLm;
		double paperRm;
		double paperTm;
		double paperBm;
		if (orientation == PageFormat.LANDSCAPE)
		{
			paperLm = pixTm;
			paperRm = pixBm;
			paperTm = pixRm;
			paperBm = pixLm;
		}
		else if (orientation == PageFormat.PORTRAIT)
		{
			paperLm = pixLm;
			paperRm = pixRm;
			paperTm = pixTm;
			paperBm = pixBm;
		}
		else
		// orientation == PageFormat.REVERSE_LANDSCAPE
		{
			paperLm = pixBm;
			paperRm = pixTm;
			paperTm = pixLm;
			paperBm = pixRm;
		}

		PageFormat pf = new PageFormat();
		pf.setOrientation(orientation);
		Paper paper = new Paper();
		paper.setSize(pixWidth, pixHeight);
		paper.setImageableArea(paperLm, paperTm, pixWidth - (paperLm + paperRm), pixHeight - (paperTm + paperBm));
		pf.setPaper(paper);

		return pf;
	}

	public static final double DEFAULT_EQUALS_PRECISION = 1e-7d;

	//null,null == true
	public final static boolean equalObjects(Object oldObj, Object obj)
	{
		return equalObjects(oldObj, obj, DEFAULT_EQUALS_PRECISION, false);
	}

	public final static boolean equalObjects(Object oldObj, Object obj, boolean ignoreCase)
	{
		return equalObjects(oldObj, obj, DEFAULT_EQUALS_PRECISION, ignoreCase);
	}

	public final static boolean equalObjects(Object oldObj, Object obj, double equalsPrecision)
	{
		return equalObjects(oldObj, obj, equalsPrecision, false);
	}

	public final static boolean equalObjects(Object oldObj, Object obj, double equalsPrecision, boolean ignoreCase)
	{
		if (oldObj instanceof Wrapper) oldObj = ((Wrapper)oldObj).unwrap();
		if (obj instanceof Wrapper) obj = ((Wrapper)obj).unwrap();

		if (oldObj == obj)
		{
			return true;
		}
		if (oldObj == null && obj != null)
		{
			return false;
		}
		if (oldObj != null && obj == null)
		{
			return false;
		}

		// Compare UUID with possible storage for UUID
		if (oldObj.getClass() == UUID.class)
		{
			if (obj.getClass() == byte[].class && ((byte[])obj).length == 16)
			{
				// compare UUID and byte[]
				return oldObj.equals(new UUID((byte[])obj));
			}
			if (obj.getClass() == String.class && ((String)obj).length() == 36)
			{
				// compare UUID and String
				return oldObj.toString().equals(obj);
			}
			return oldObj.equals(obj);
		}
		if (obj.getClass() == UUID.class)
		{
			if (oldObj.getClass() == byte[].class && ((byte[])oldObj).length == 16)
			{
				// compare UUID and byte[]
				return obj.equals(new UUID((byte[])oldObj));
			}
			if (oldObj.getClass() == String.class && ((String)oldObj).length() == 36)
			{
				// compare UUID and String
				return obj.toString().equals(oldObj);
			}
			return obj.equals(oldObj);
		}

		if (oldObj.getClass().isArray() && obj.getClass().isArray())
		{
			int length = Array.getLength(obj);
			if (length == Array.getLength(oldObj))
			{
				for (int i = 0; i < length; i++)
				{
					if (!equalObjects(Array.get(obj, i), Array.get(oldObj, i), equalsPrecision, ignoreCase)) return false;
				}
				return true;
			}
			return false;
		}

		//in case one side is String and other Number -> make both string
		if (oldObj instanceof Number && obj instanceof String)
		{
			try
			{
				obj = new Double((String)obj);
			}
			catch (Exception e)
			{
				oldObj = oldObj.toString();
			}
		}
		else if (obj instanceof Number && oldObj instanceof String)
		{
			try
			{
				oldObj = new Double((String)oldObj);
			}
			catch (Exception e)
			{
				obj = obj.toString();
			}
		}

		if (oldObj instanceof BigDecimal && !(obj instanceof BigDecimal))
		{
			if (obj instanceof Long)
			{
				obj = BigDecimal.valueOf(((Long)obj).longValue());
			}
			else if (obj instanceof Double)
			{
				obj = BigDecimal.valueOf(((Double)obj).doubleValue());
			}
		}
		else if (obj instanceof BigDecimal && !(oldObj instanceof BigDecimal))
		{
			if (oldObj instanceof Long)
			{
				oldObj = BigDecimal.valueOf(((Long)oldObj).longValue());
			}
			else if (oldObj instanceof Double)
			{
				oldObj = BigDecimal.valueOf(((Double)oldObj).doubleValue());
			}
		}
		// separate tests for BigDecimal and Long, the tests based on Double may give
		// incorrect results for Long values not fitting in a double mantissa.
		// note that 2.0 is not equal to 2.00 according to BigDecimal.equals()
		if (oldObj instanceof BigDecimal && obj instanceof BigDecimal)
		{
			return ((BigDecimal)oldObj).subtract((BigDecimal)obj).abs().doubleValue() < equalsPrecision;
		}
		if (oldObj instanceof Long && obj instanceof Long)
		{
			return oldObj.equals(obj);
		}

		// Always cast to double so we don't lose precision.
		if (oldObj instanceof Number && obj instanceof Number)
		{
			if (oldObj instanceof Float || oldObj instanceof Double || oldObj instanceof BigDecimal || obj instanceof Float || obj instanceof Double ||
				obj instanceof BigDecimal)
			{
				double a = ((Number)oldObj).doubleValue();
				double b = ((Number)obj).doubleValue();
				return a == b || Math.abs(a - b) < equalsPrecision;
			}
			return ((Number)oldObj).longValue() == ((Number)obj).longValue();
		}

		if (oldObj instanceof Date && obj instanceof Date)
		{
			return (((Date)oldObj).getTime() == ((Date)obj).getTime());
		}

		if (ignoreCase && oldObj instanceof String && obj instanceof String)
		{
			return ((String)oldObj).equalsIgnoreCase((String)obj);
		}

		return oldObj.equals(obj);
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


	public static OutputStream closeOutputStream(OutputStream os)
	{
		try
		{
			if (os != null)
			{
				os.close();
			}
		}
		catch (IOException e)
		{
			Debug.error(e);
		}
		return null;
	}

	public static Writer closeWriter(Writer w)
	{
		return close(w);
	}

	public static byte[] readFile(File f, long size)
	{
		if (f != null && f.exists())
		{

			FileInputStream fis = null;
			try
			{
				int length = (int)f.length();
				fis = new FileInputStream(f);
				FileChannel fc = fis.getChannel();
				if (size > length || size < 0) size = length;
				ByteBuffer bb = ByteBuffer.allocate((int)size);
				fc.read(bb);
				bb.rewind();
				byte[] bytes = null;
				if (bb.hasArray())
				{
					bytes = bb.array();
				}
				else
				{
					bytes = new byte[(int)size];
					bb.get(bytes, 0, (int)size);
				}
				return bytes;
			}
			catch (Exception e)
			{
				Debug.error("Error reading file: " + f, e); //$NON-NLS-1$
			}
			finally
			{
				try
				{
					if (fis != null) fis.close();
				}
				catch (Exception ex)
				{
				}
			}

			//			ByteArrayOutputStream sb = new ByteArrayOutputStream();
			//			try
			//			{
			//				FileInputStream is = new FileInputStream(f);
			//				BufferedInputStream bis = new BufferedInputStream(is);
			//				streamCopy(bis, sb);
			//				closeInputStream(bis);
			//			}
			//			catch (Exception e)
			//			{
			//				Debug.error(e);
			//			}
			//			return sb.toByteArray();
		}
		return null;
	}

	public static byte[] getFileContent(File f)
	{
		if (f != null) return readFile(f, -1);
		return null;
	}

	public static String getTXTFileContent(File f)
	{

		return getTXTFileContent(f, Charset.defaultCharset());
	}

	public static String getTXTFileContent(File f, Charset charset)
	{
		if (f != null /* && f.exists() */)
		{
			if (Thread.currentThread().isInterrupted())
			{
				Thread.interrupted(); // reset interrupted flag of current thread, FileChannel.read() will throw an exception for it.
			}
			FileInputStream fis = null;
			try
			{
				int length = (int)f.length();
				if (f.exists())
				{
					fis = new FileInputStream(f);
					FileChannel fc = fis.getChannel();
					ByteBuffer bb = ByteBuffer.allocate(length);
					fc.read(bb);
					bb.rewind();
					CharBuffer cb = charset.decode(bb);
					return cb.toString();
				}
			}
			catch (Exception e)
			{
				Debug.error("Error reading txt file: " + f, e); //$NON-NLS-1$
			}
			finally
			{
				closeInputStream(fis);
			}
		}
		return null;
	}

	public static InputStream getUTF8EncodedStream(String out)
	{
		byte[] content = null;
		try
		{
			content = out.getBytes("UTF-8"); //$NON-NLS-1$
		}
		catch (UnsupportedEncodingException e)
		{
			Debug.error(e);
		}
		if (content == null) content = out.getBytes();
		return new ByteArrayInputStream(content);
	}

	public static String getTXTFileContent(InputStream f, Charset charset)
	{
		return getTXTFileContent(f, charset, true);
	}

	public static String getTXTFileContent(InputStream f, Charset charset, boolean closeStream)
	{
		InputStreamReader isr = null;
		if (charset != null) isr = new InputStreamReader(f, charset);
		else isr = new InputStreamReader(f);
		try
		{
			StringBuilder sb = new StringBuilder();
			BufferedReader br = new BufferedReader(isr);
			String line;
			while ((line = br.readLine()) != null)
			{
				sb.append(line);
				sb.append('\n');
			}
			if (sb.length() > 0) sb.setLength(sb.length() - 1); // remove newline
			if (closeStream)
			{
				closeReader(br);
			}
			return sb.toString();
		}
		catch (IOException e)
		{
			Debug.error("Error reading txt file", e); //$NON-NLS-1$
		}
		return null;
	}

	public static boolean writeTXTFile(File file, String content)
	{
		return writeTXTFile(file, content, Charset.defaultCharset());
	}

	public static boolean writeTXTFile(File f, String content, Charset charset)
	{
		if (f != null)
		{
			FileOutputStream fos = null;
			try
			{
				fos = new FileOutputStream(f);
				FileChannel fc = fos.getChannel();
				ByteBuffer bb = charset.encode(content);
				fc.write(bb);
				bb.rewind();
				return true;
			}
			catch (Exception e)
			{
				Debug.error("Error writing txt file: " + f, e); //$NON-NLS-1$
			}
			finally
			{
				closeOutputStream(fos);
			}
		}
		return false;
	}

	public static boolean writeFile(File f, byte[] content)
	{
		if (f != null)
		{
			FileOutputStream fos = null;
			try
			{
				f.getParentFile().mkdirs();
				fos = new FileOutputStream(f);
				fos.write(content);
				fos.flush();
				return true;
			}
			catch (Exception e)
			{
				Debug.error("Error writing file: " + f, e); //$NON-NLS-1$
			}
			finally
			{
				closeOutputStream(fos);
			}
		}
		return false;
	}

	public static boolean isAppleMacOS()
	{
		return getPlatform() == PLATFORM_MAC;
	}

	public static boolean isValidEmailAddress(String email)
	{
		return (email != null
			? (Pattern.compile("^[_a-z0-9-+]+(\\.[_a-z0-9-+]+)*@[a-z0-9-]+(\\.[a-z0-9-]+)*(\\.[a-z]{2,16})$", Pattern.CASE_INSENSITIVE).matcher( //$NON-NLS-1$
				email).matches())
			: false);
	}

	public static boolean isValidJavaIdentifier(String s)
	{
		return IdentDocumentValidator.isJavaIdentifier(s);
	}

	public static boolean isValidJavaSimpleOrQualifiedName(String s)
	{
		boolean ok = true;
		StringTokenizer tokenizer = new StringTokenizer(s, ".", false); //$NON-NLS-1$
		while (tokenizer.hasMoreTokens())
		{
			ok = IdentDocumentValidator.isJavaIdentifier(tokenizer.nextToken());
		}
		return ok;
	}

	public static int getPlatform()
	{
		return getPlatform(System.getProperty("os.name")); //$NON-NLS-1$
	}

	public static int getPlatform(String osname)
	{
		if (osname != null)
		{
			String lc = osname.toLowerCase();
			if (lc.contains("mac")) return PLATFORM_MAC; //$NON-NLS-1$
			if (lc.contains("linux")) return PLATFORM_LINUX; //$NON-NLS-1$
			if (lc.contains("win")) return PLATFORM_WINDOWS; //$NON-NLS-1$
		}
		return PLATFORM_OTHER;
	}

	public static String getPlatformAsString()
	{
		switch (Utils.getPlatform())
		{
			case Utils.PLATFORM_LINUX :
				return "linux"; //$NON-NLS-1$
			case Utils.PLATFORM_MAC :
				return "mac"; //$NON-NLS-1$
			case Utils.PLATFORM_WINDOWS :
				return "windows"; //$NON-NLS-1$
			default :
				return "other"; //$NON-NLS-1$
		}
	}

	public static String getDotQualitfied(Object... tokens)
	{
		if (tokens == null)
		{
			return null;
		}
		if (tokens.length == 0)
		{
			return ""; //$NON-NLS-1$
		}
		if (tokens.length == 1)
		{
			return String.valueOf(tokens[0]);
		}
		StringBuilder ptr = new StringBuilder(String.valueOf(tokens[0]));
		for (int i = 1; i < tokens.length; i++)
		{
			ptr.append('.');
			ptr.append(String.valueOf(tokens[i]));
		}
		return ptr.toString();
	}

	public static String toEnglishLocaleLowerCase(String text)
	{
		if (text == null) return null;
		return text.toLowerCase(Locale.ENGLISH);
	}

	public static String stringInitCap(Object text)
	{
		if (text != null)
		{
			try
			{
				StringBuilder sb = new StringBuilder();
				StringTokenizer st = new StringTokenizer(text.toString(), " "); //$NON-NLS-1$
				int i = 0;
				while (st.hasMoreTokens())
				{
					String value = st.nextToken();
					String value_upper = value.substring(0, 1);
					String value_lower = value.substring(1);

					value_upper = value_upper.toUpperCase();
					value_lower = value_lower.toLowerCase();
					sb.append(value_upper + value_lower);
					if (st.hasMoreTokens())
					{
						sb.append(" "); //$NON-NLS-1$
					}
					i++;
				}
				return sb.toString();
			}
			catch (Exception ex)
			{
				Debug.error(ex);
				return text.toString();
			}
		}
		else
		{
			return ""; //$NON-NLS-1$
		}
	}

	/**
	 * Get the rectangle that surrounds all elements
	 *
	 * @param elements
	 * @return the rectangle
	 */
	public static Rectangle getBounds(Iterator< ? > elements)
	{
		int minx = -1;
		int miny = -1;
		int maxx = -1;
		int maxy = -1;
		while (elements != null && elements.hasNext())
		{
			Object element = elements.next();
			if (element instanceof ISupportBounds)
			{
				java.awt.Point location = ((ISupportBounds)element).getLocation();
				java.awt.Dimension size = ((ISupportBounds)element).getSize();
				if (location != null && size != null)
				{
					if (minx == -1 || minx > location.x) minx = location.x;
					if (miny == -1 || miny > location.y) miny = location.y;
					if (maxx == -1 || maxx < location.x + size.width) maxx = location.x + size.width;
					if (maxy == -1 || maxy < location.y + size.height) maxy = location.y + size.height;
				}
			}
		}

		return new Rectangle(minx, miny, maxx - minx, maxy - miny);
	}

	/**
	 * Set Calendar time part to 00:00:00:000
	 *
	 * @param cal
	 */
	public static void applyMinTime(Calendar cal)
	{
		cal.set(Calendar.HOUR_OF_DAY, 00); //h:00
		cal.set(Calendar.MINUTE, 00);//x:00
		cal.set(Calendar.SECOND, 00);
		cal.set(Calendar.MILLISECOND, 000);
	}

	/**
	 * Set Calendar time part to 23:59:59:999
	 *
	 * @param cal
	 */
	public static void applyMaxTime(Calendar cal)
	{
		cal.set(Calendar.HOUR_OF_DAY, 23); //h:23
		cal.set(Calendar.MINUTE, 59);//x:59
		cal.set(Calendar.SECOND, 59);
		cal.set(Calendar.MILLISECOND, 999);
	}

	public static String[] getTokenElements(String value, String delim, boolean trim)
	{
		if (value == null)
		{
			return new String[] { };
		}

		List<String> lst = new ArrayList<String>();
		StringTokenizer tokemizer = new StringTokenizer(value, delim);
		while (tokemizer.hasMoreElements())
		{
			String token = tokemizer.nextToken();
			if (trim)
			{
				lst.add(token.trim());
			}
			else
			{
				lst.add(token);
			}
		}
		return lst.toArray(new String[lst.size()]);
	}

	/**
	 * Iterate over iterator.
	 * <pre>
	 * for (T o: Utils.iterate(iterator))
	 * {
	 *     o. ....
	 * }
	 * </pre>
	 * @param iterator when null, iterate over empty list
	 */
	public static <T> Iterable<T> iterate(final Iterator<T> iterator)
	{
		return iterator == null ? Collections.<T> emptyList() : new Iterable<T>()
		{
			public Iterator<T> iterator()
			{
				return iterator;
			}
		};
	}

	public static <T> Iterable<T> iterate(Iterable<T> iterable)
	{
		return iterable == null ? Collections.<T> emptyList() : iterable;
	}

	/**
	 * Iterate over enumeration.
	 * <pre>
	 * for (T o: Utils.iterate(enumeration))
	 * {
	 *     o. ....
	 * }
	 * </pre>
	 * @param enumeration when null, iterate over empty list
	 */
	public static <T> Iterable<T> iterate(final Enumeration<T> enumeration)
	{
		return iterate(enumeration == null ? null : new Iterator<T>()
		{
			@Override
			public boolean hasNext()
			{
				return enumeration.hasMoreElements();
			}

			@Override
			public T next()
			{
				return enumeration.nextElement();
			}

			@Override
			public void remove()
			{
				throw new UnsupportedOperationException();
			}
		});
	}


	/**
	 * Returns true if the given client/application type is a Swing client and false if it is not.
	 * @param applicationType the type to check
	 */
	public static boolean isSwingClient(int applicationType)
	{
		return applicationType == IApplication.CLIENT || applicationType == IApplication.OFFLINE || applicationType == IApplication.RUNTIME;
	}

	private static String getPrefixedType(String type, String prefix)
	{
		return prefix != null ? prefix + type : type;
	}

	/**
	 * Removes the first map entry based on a map value
	 *
	 * @param value the map value to remove the map entry
	 * @param map a map from which to remove the value and key
	 * @return the previous value associated with key, or null if there was no mapping for key
	 */
	public static <K, V> K mapRemoveByValue(V value, Map<K, V> map)
	{
		K removalKey = null;

		for (Map.Entry<K, V> entry : map.entrySet())
		{
			if (value == null && entry.getValue() == null)
			{
				removalKey = entry.getKey();
				break;
			}
			else if (value != null && value.equals(entry.getValue()))
			{
				removalKey = entry.getKey();
				break;
			}
		}
		if (removalKey != null)
		{
			map.remove(removalKey);
			return removalKey;
		}
		return null;
	}

	/**
	 * Returns the first Key of a map based on the value parameter
	 *
	 * @param map
	 * @param value
	 * @return the first key of a map based on the value parameter
	 */
	public static <K, V> K mapGetKeyByValue(Map<K, V> map, V value)
	{
		for (Entry<K, V> entry : map.entrySet())
		{
			if (value == null && entry.getValue() == null)
			{
				return entry.getKey();
			}
			else if (value != null && value.equals(entry.getValue()))
			{
				return entry.getKey();
			}
		}
		return null;
	}

	public static <T, E> Set<T> mapGetKeysByValue(Map<T, E> map, E value)
	{
		Set<T> keys = new HashSet<T>();
		for (Entry<T, E> entry : map.entrySet())
		{
			if (value == null && entry.getValue() == null)
			{
				keys.add(entry.getKey());
			}
			else if (value != null && value.equals(entry.getValue()))
			{
				keys.add(entry.getKey());
			}
		}
		return keys;
	}


	/**
	 * Returns a js/json string representation of the given {@link Scriptable}
	 * @param obj
	 * @return the scriptable as string
	 */
	public static String getScriptableString(Scriptable obj)
	{
		if (obj == null) return "null"; //$NON-NLS-1$
		return getScriptableString(obj, new HashMap<Scriptable, CharSequence>()).toString();
	}

	/**
	 * Returns a js/json string representation of the given Array, that can have {@link Scriptable} objects inside it.
	 * @param array
	 * @return the scriptable as string
	 */
	public static String getScriptableString(Object[] array)
	{
		if (array == null) return "null"; //$NON-NLS-1$
		return getArrayString(array).toString();
	}

	public static Object removeJavascripLinkFromDisplay(IDisplayData display, Object[] value)
	{
		Object obj = value == null ? display.getValueObject() : value[0];

		if (obj instanceof String && display instanceof IScriptableProvider)
		{
			IScriptable scriptable = ((IScriptableProvider)display).getScriptObject();
			if (scriptable instanceof HasRuntimeClientProperty)
			{
				HasRuntimeClientProperty scriptableWithClientProperty = (HasRuntimeClientProperty)scriptable;
				if (!Boolean.TRUE.equals(scriptableWithClientProperty.getClientProperty(IApplication.ALLOW_JAVASCRIPT_LINK_INPUT)))
				{
					obj = ((String)obj).replaceAll("(?i)javascript:", ""); //$NON-NLS-1$ //$NON-NLS-2$
					display.setValueObject(obj);
				}
			}
		}

		return obj;
	}

	/**
	 * Returns a js/json string representation of the given array.
	 * @param a
	 * @return the array as string
	 */
	private static CharSequence getArrayString(Object[] a)
	{
		StringBuilder buf = new StringBuilder();
		buf.append('[');
		for (int i = 0; i < a.length; i++)
		{
			if (i > 0) buf.append(", "); //$NON-NLS-1$
			if (a[i] instanceof Scriptable) buf.append(getScriptableString((Scriptable)a[i], new HashMap<Scriptable, CharSequence>()));
			else if (a[i] instanceof Object[]) buf.append(getArrayString((Object[])a[i]));
			else buf.append(String.valueOf(a[i]));
		}
		buf.append(']');
		return buf;
	}

	/**
	 * Returns a js/json string representation of the given {@link Scriptable}
	 * @param scriptable
	 * @param processed map to prevent loops in graph
	 * @return the scriptable as string
	 */
	private static CharSequence getScriptableString(Scriptable scriptable, Map<Scriptable, CharSequence> processed)
	{
		if (scriptable instanceof Record || scriptable instanceof FoundSet) return scriptable.toString();
		if (scriptable instanceof XMLObject || scriptable instanceof NativeError) return scriptable.toString();
		CharSequence processedString = processed.get(scriptable);
		if (processedString != null)
		{
			return processedString;
		}
		if (processed.size() > 10) return scriptable.toString();

		if (scriptable instanceof NativeArray) processed.put(scriptable, "Array[SelfRef]"); //$NON-NLS-1$
		else processed.put(scriptable, "Object[SelfRef]"); //$NON-NLS-1$
		Object[] ids = scriptable.getIds();
		if (ids != null && ids.length > 0)
		{
			StringBuilder sb = new StringBuilder();
			if (scriptable instanceof NativeArray) sb.append('[');
			else sb.append('{');
			for (Object object : ids)
			{
				if (!(object instanceof Integer))
				{
					sb.append(object);
					sb.append(':');
				}
				Object value = null;
				if (object instanceof String)
				{
					value = scriptable.get((String)object, scriptable);
				}
				else if (object instanceof Number)
				{
					value = scriptable.get(((Number)object).intValue(), scriptable);
				}
				if (!(value instanceof NativeJavaMethod))
				{
					if (value instanceof Scriptable)
					{
						sb.append(getScriptableString((Scriptable)value, processed));
					}
					else
					{
						sb.append(value);
					}
					sb.append(',');
				}
			}
			sb.setLength(sb.length() - 1);
			if (scriptable instanceof NativeArray) sb.append(']');
			else sb.append('}');
			processed.put(scriptable, sb);
			return sb;
		}

		Object defaultValue;
		try
		{
			defaultValue = scriptable.getDefaultValue(String.class);
		}
		catch (Exception e)
		{
			defaultValue = null;
		}
		if (defaultValue == null) defaultValue = scriptable.toString();
		processed.put(scriptable, defaultValue.toString());
		return defaultValue.toString();
	}
}
