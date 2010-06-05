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

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.text.DateFormatSymbols;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.DuplicateFormatFlagsException;
import java.util.FormatFlagsConversionMismatchException;
import java.util.Formattable;
import java.util.Formatter;
import java.util.FormatterClosedException;
import java.util.IllegalFormatCodePointException;
import java.util.IllegalFormatConversionException;
import java.util.IllegalFormatFlagsException;
import java.util.IllegalFormatPrecisionException;
import java.util.IllegalFormatWidthException;
import java.util.Locale;
import java.util.MissingFormatArgumentException;
import java.util.MissingFormatWidthException;
import java.util.TimeZone;
import java.util.UnknownFormatConversionException;
import java.util.UnknownFormatFlagsException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sun.misc.DoubleConsts;
import sun.misc.FormattedFloatingDecimal;
import sun.misc.FpUtils;


public final class FixedFormatter implements Closeable, Flushable
{
	private Appendable a;
	private Locale l;

	private IOException lastException;

	private char zero = '0';
	private static double scaleUp;

	// 1 (sign) + 19 (max # sig digits) + 1 ('.') + 1 ('e') + 1 (sign)
	// + 3 (max # exp digits) + 4 (error) = 30
	private static final int MAX_FD_CHARS = 30;

	// Initialize internal data.
	private void init(Appendable a, Locale l)
	{
		this.a = a;
		this.l = l;
		setZero();
	}

	public FixedFormatter()
	{
		init(new StringBuilder(), Locale.getDefault());
	}

	public FixedFormatter(Appendable a)
	{
		init(a, Locale.getDefault());
	}

	public FixedFormatter(Locale l)
	{
		init(new StringBuilder(), l);
	}

	public FixedFormatter(Appendable a, Locale l)
	{
		if (a == null) a = new StringBuilder();
		init(a, l);
	}

	public FixedFormatter(String fileName) throws FileNotFoundException
	{
		init(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName))), Locale.getDefault());
	}

	public FixedFormatter(String fileName, String csn) throws FileNotFoundException, UnsupportedEncodingException
	{
		this(fileName, csn, Locale.getDefault());
	}

	public FixedFormatter(String fileName, String csn, Locale l) throws FileNotFoundException, UnsupportedEncodingException
	{
		init(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), csn)), l);
	}

	public FixedFormatter(File file) throws FileNotFoundException
	{
		init(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file))), Locale.getDefault());
	}

	public FixedFormatter(File file, String csn) throws FileNotFoundException, UnsupportedEncodingException
	{
		this(file, csn, Locale.getDefault());
	}

	/**
	 * Constructs a new formatter with the specified file, charset, and
	 * locale.
	 *
	 * @param  file
	 *         The file to use as the destination of this formatter.  If the
	 *         file exists then it will be truncated to zero size; otherwise,
	 *         a new file will be created.  The output will be written to the
	 *         file and is buffered.
	 *
	 * @param  csn
	 *         The name of a supported {@linkplain java.nio.charset.Charset
	 *         charset}
	 *
	 * @param  l
	 *         The {@linkplain java.util.Locale locale} to apply during
	 *         formatting.  If <tt>l</tt> is <tt>null</tt> then no localization
	 *         is applied.
	 *
	 * @throws  FileNotFoundException
	 *          If the given file object does not denote an existing, writable
	 *          regular file and a new regular file of that name cannot be
	 *          created, or if some other error occurs while opening or
	 *          creating the file
	 *
	 * @throws  SecurityException
	 *          If a security manager is present and {@link
	 *          SecurityManager#checkWrite checkWrite(file.getPath())} denies
	 *          write access to the file
	 *
	 * @throws  UnsupportedEncodingException
	 *          If the named charset is not supported
	 */
	public FixedFormatter(File file, String csn, Locale l) throws FileNotFoundException, UnsupportedEncodingException
	{
		init(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), csn)), l);
	}


	public FixedFormatter(PrintStream ps)
	{
		if (ps == null) throw new NullPointerException();
		init(ps, Locale.getDefault());
	}


	public FixedFormatter(OutputStream os)
	{
		init(new BufferedWriter(new OutputStreamWriter(os)), Locale.getDefault());
	}


	public FixedFormatter(OutputStream os, String csn) throws UnsupportedEncodingException
	{
		this(os, csn, Locale.getDefault());
	}


	public FixedFormatter(OutputStream os, String csn, Locale l) throws UnsupportedEncodingException
	{
		init(new BufferedWriter(new OutputStreamWriter(os, csn)), l);
	}

	private void setZero()
	{
		if ((l != null) && !l.equals(Locale.US))
		{
			DecimalFormatSymbols dfs = new DecimalFormatSymbols(l);
			zero = dfs.getZeroDigit();
		}
	}


	public Locale locale()
	{
		ensureOpen();
		return l;
	}


	public Appendable out()
	{
		ensureOpen();
		return a;
	}


	@Override
	public String toString()
	{
		ensureOpen();
		return a.toString();
	}

	public void flush()
	{
		ensureOpen();
		if (a instanceof Flushable)
		{
			try
			{
				((Flushable)a).flush();
			}
			catch (IOException ioe)
			{
				lastException = ioe;
			}
		}
	}

	public void close()
	{
		if (a == null) return;
		try
		{
			if (a instanceof Closeable) ((Closeable)a).close();
		}
		catch (IOException ioe)
		{
			lastException = ioe;
		}
		finally
		{
			a = null;
		}
	}

	private void ensureOpen()
	{
		if (a == null) throw new FormatterClosedException();
	}

	public IOException ioException()
	{
		return lastException;
	}

	public FixedFormatter format(String format, Object... args)
	{
		return format(l, format, args);
	}

	public FixedFormatter format(Locale l, String format, Object... args)
	{
		ensureOpen();

		// index of last argument referenced
		int last = -1;
		// last ordinary index
		int lasto = -1;

		FormatString[] fsa = parse(format);
		for (FormatString fs : fsa)
		{
			int index = fs.index();
			try
			{
				switch (index)
				{
					case -2 : // fixed string, "%n", or "%%"
						fs.print(null, l);
						break;
					case -1 : // relative index
						if (last < 0 || (args != null && last > args.length - 1)) throw new MissingFormatArgumentException(fs.toString());
						fs.print((args == null ? null : args[last]), l);
						break;
					case 0 : // ordinary index
						lasto++;
						last = lasto;
						if (args != null && lasto > args.length - 1) throw new MissingFormatArgumentException(fs.toString());
						fs.print((args == null ? null : args[lasto]), l);
						break;
					default : // explicit index
						last = index - 1;
						if (args != null && last > args.length - 1) throw new MissingFormatArgumentException(fs.toString());
						fs.print((args == null ? null : args[last]), l);
						break;
				}
			}
			catch (IOException x)
			{
				lastException = x;
			}
		}
		return this;
	}

	// %[argument_index$][flags][width][.precision][t]conversion
	private static final String formatSpecifier = "%(\\d+\\$)?([-#+ 0,(\\<]*)?(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z%])";

	private static Pattern fsPattern = Pattern.compile(formatSpecifier);

	// Look for format specifiers in the format string.
	private FormatString[] parse(String s)
	{
		ArrayList al = new ArrayList();
		Matcher m = fsPattern.matcher(s);
		int i = 0;
		while (i < s.length())
		{
			if (m.find(i))
			{
				// Anything between the start of the string and the beginning
				// of the format specifier is either fixed text or contains
				// an invalid format string.
				if (m.start() != i)
				{
					// Make sure we didn't miss any invalid format specifiers
					checkText(s.substring(i, m.start()));
					// Assume previous characters were fixed text
					al.add(new FixedString(s.substring(i, m.start())));
				}

				// Expect 6 groups in regular expression
				String[] sa = new String[6];
				for (int j = 0; j < m.groupCount(); j++)
				{
					sa[j] = m.group(j + 1);
// 		    System.out.print(sa[j] + " ");
				}
// 		System.out.println();
				al.add(new FormatSpecifier(this, sa));
				i = m.end();
			}
			else
			{
				// No more valid format specifiers.  Check for possible invalid
				// format specifiers.
				checkText(s.substring(i));
				// The rest of the string is fixed text
				al.add(new FixedString(s.substring(i)));
				break;
			}
		}
//   	FormatString[] fs = new FormatString[al.size()];
//   	for (int j = 0; j < al.size(); j++)
//   	    System.out.println(((FormatString) al.get(j)).toString());
		return (FormatString[])al.toArray(new FormatString[0]);
	}

	private void checkText(String s)
	{
		int idx;
		// If there are any '%' in the given string, we got a bad format
		// specifier.
		if ((idx = s.indexOf('%')) != -1)
		{
			char c = (idx > s.length() - 2 ? '%' : s.charAt(idx + 1));
			throw new UnknownFormatConversionException(String.valueOf(c));
		}
	}

	private interface FormatString
	{
		int index();

		void print(Object arg, Locale l) throws IOException;

		String toString();
	}

	private class FixedString implements FormatString
	{
		private final String s;

		FixedString(String s)
		{
			this.s = s;
		}

		public int index()
		{
			return -2;
		}

		public void print(Object arg, Locale l) throws IOException
		{
			a.append(s);
		}

		@Override
		public String toString()
		{
			return s;
		}
	}

	public enum BigDecimalLayoutForm
	{
		SCIENTIFIC, DECIMAL_FLOAT
	};

	private class FormatSpecifier implements FormatString
	{
		private int index = -1;
		private Flags f = Flags.NONE;
		private int width;
		private int precision;
		private boolean dt = false;
		private char c;

		private final FixedFormatter formatter;

		// cache the line separator
		private String ls;

		private int index(String s)
		{
			if (s != null)
			{
				try
				{
					index = Integer.parseInt(s.substring(0, s.length() - 1));
				}
				catch (NumberFormatException x)
				{
					assert (false);
				}
			}
			else
			{
				index = 0;
			}
			return index;
		}

		public int index()
		{
			return index;
		}

		private Flags flags(String s)
		{
			f = Flags.parse(s);
			if (f.contains(Flags.PREVIOUS)) index = -1;
			return f;
		}

		Flags flags()
		{
			return f;
		}

		private int width(String s)
		{
			width = -1;
			if (s != null)
			{
				try
				{
					width = Integer.parseInt(s);
					if (width < 0) throw new IllegalFormatWidthException(width);
				}
				catch (NumberFormatException x)
				{
					assert (false);
				}
			}
			return width;
		}

		int width()
		{
			return width;
		}

		private int precision(String s)
		{
			precision = -1;
			if (s != null)
			{
				try
				{
					// remove the '.'
					precision = Integer.parseInt(s.substring(1));
					if (precision < 0) throw new IllegalFormatPrecisionException(precision);
				}
				catch (NumberFormatException x)
				{
					assert (false);
				}
			}
			return precision;
		}

		int precision()
		{
			return precision;
		}

		private char conversion(String s)
		{
			c = s.charAt(0);
			if (!dt)
			{
				if (!Conversion.isValid(c)) throw new UnknownFormatConversionException(String.valueOf(c));
				if (Character.isUpperCase(c)) f.add(Flags.UPPERCASE);
				c = Character.toLowerCase(c);
				if (Conversion.isText(c)) index = -2;
			}
			return c;
		}

		private char conversion()
		{
			return c;
		}

		FormatSpecifier(FixedFormatter formatter, String[] sa)
		{
			this.formatter = formatter;
			int idx = 0;

			index(sa[idx++]);
			flags(sa[idx++]);
			width(sa[idx++]);
			precision(sa[idx++]);

			if (sa[idx] != null)
			{
				dt = true;
				if (sa[idx].equals("T")) f.add(Flags.UPPERCASE);
			}
			conversion(sa[++idx]);

			if (dt) checkDateTime();
			else if (Conversion.isGeneral(c)) checkGeneral();
			else if (c == Conversion.CHARACTER) checkCharacter();
			else if (Conversion.isInteger(c)) checkInteger();
			else if (Conversion.isFloat(c)) checkFloat();
			else if (Conversion.isText(c)) checkText();
			else throw new UnknownFormatConversionException(String.valueOf(c));
		}

		public void print(Object arg, Locale l) throws IOException
		{
			if (dt)
			{
				printDateTime(arg, l);
				return;
			}
			switch (c)
			{
				case Conversion.DECIMAL_INTEGER :
				case Conversion.OCTAL_INTEGER :
				case Conversion.HEXADECIMAL_INTEGER :
					printInteger(arg, l);
					break;
				case Conversion.SCIENTIFIC :
				case Conversion.GENERAL :
				case Conversion.DECIMAL_FLOAT :
				case Conversion.HEXADECIMAL_FLOAT :
					printFloat(arg, l);
					break;
				case Conversion.CHARACTER :
					printCharacter(arg);
					break;
				case Conversion.BOOLEAN :
					printBoolean(arg);
					break;
				case Conversion.STRING :
					printString(arg, l);
					break;
				case Conversion.HASHCODE :
					printHashCode(arg);
					break;
				case Conversion.LINE_SEPARATOR :
					if (ls == null) ls = System.getProperty("line.separator");
					a.append(ls);
					break;
				case Conversion.PERCENT_SIGN :
					a.append('%');
					break;
				default :
					assert false;
			}
		}

		private void printInteger(Object arg, Locale l) throws IOException
		{
			if (arg == null) print("null");
			else if (arg instanceof Byte) print(((Byte)arg).byteValue(), l);
			else if (arg instanceof Short) print(((Short)arg).shortValue(), l);
			else if (arg instanceof Integer) print(((Integer)arg).intValue(), l);
			else if (arg instanceof Long) print(((Long)arg).longValue(), l);
			else if (arg instanceof BigInteger) print(((BigInteger)arg), l);
			else failConversion(c, arg);
		}

		private void printFloat(Object arg, Locale l) throws IOException
		{
			if (arg == null) print("null");
			else if (arg instanceof Float) print(((Float)arg).floatValue(), l);
			else if (arg instanceof Double) print(((Double)arg).doubleValue(), l);
			else if (arg instanceof BigDecimal) print(((BigDecimal)arg), l);
			else if (arg instanceof Number) print(((Number)arg).doubleValue(), l);
			else failConversion(c, arg);
		}

		private void printDateTime(Object arg, Locale l) throws IOException
		{
			if (arg == null)
			{
				print("null");
				return;
			}
			Calendar cal = null;

			// Instead of Calendar.setLenient(true), perhaps we should
			// wrap the IllegalArgumentException that might be thrown?
			if (arg instanceof Long)
			{
				// Note that the following method uses an instance of the
				// default time zone (TimeZone.getDefaultRef().
				cal = Calendar.getInstance(l);
				cal.setTimeInMillis((Long)arg);
			}
			else if (arg instanceof Date)
			{
				// Note that the following method uses an instance of the
				// default time zone (TimeZone.getDefaultRef().
				cal = Calendar.getInstance(l);
				cal.setTime((Date)arg);
			}
			else if (arg instanceof Calendar)
			{
				cal = (Calendar)((Calendar)arg).clone();
				cal.setLenient(true);
			}
			else
			{
				failConversion(c, arg);
			}
			print(cal, c, l);
		}

		private void printCharacter(Object arg) throws IOException
		{
			if (arg == null)
			{
				print("null");
				return;
			}
			String s = null;
			if (arg instanceof Character)
			{
				s = ((Character)arg).toString();
			}
			else if (arg instanceof Byte)
			{
				byte i = ((Byte)arg).byteValue();
				if (Character.isValidCodePoint(i)) s = new String(Character.toChars(i));
				else throw new IllegalFormatCodePointException(i);
			}
			else if (arg instanceof Short)
			{
				short i = ((Short)arg).shortValue();
				if (Character.isValidCodePoint(i)) s = new String(Character.toChars(i));
				else throw new IllegalFormatCodePointException(i);
			}
			else if (arg instanceof Integer)
			{
				int i = ((Integer)arg).intValue();
				if (Character.isValidCodePoint(i)) s = new String(Character.toChars(i));
				else throw new IllegalFormatCodePointException(i);
			}
			else
			{
				failConversion(c, arg);
			}
			print(s);
		}

		private void printString(Object arg, Locale l) throws IOException
		{
			if (arg == null)
			{
				print("null");
			}
			else if (arg instanceof Formattable)
			{
				FixedFormatter fmt = formatter;
				if (formatter.locale() != l) fmt = new FixedFormatter(formatter.out(), l);
				((Formattable)arg).formatTo(new Formatter(), f.valueOf(), width, precision);
			}
			else
			{
				print(arg.toString());
			}
		}

		private void printBoolean(Object arg) throws IOException
		{
			String s;
			if (arg != null) s = ((arg instanceof Boolean) ? ((Boolean)arg).toString() : Boolean.toString(true));
			else s = Boolean.toString(false);
			print(s);
		}

		private void printHashCode(Object arg) throws IOException
		{
			String s = (arg == null ? "null" : Integer.toHexString(arg.hashCode()));
			print(s);
		}

		private void print(String s) throws IOException
		{
			if (precision != -1 && precision < s.length()) s = s.substring(0, precision);
			if (f.contains(Flags.UPPERCASE)) s = s.toUpperCase();
			a.append(justify(s));
		}

		private String justify(String s)
		{
			if (width == -1) return s;
			StringBuilder sb = new StringBuilder();
			boolean pad = f.contains(Flags.LEFT_JUSTIFY);
			int sp = width - s.length();
			if (!pad) for (int i = 0; i < sp; i++)
				sb.append(' ');
			sb.append(s);
			if (pad) for (int i = 0; i < sp; i++)
				sb.append(' ');
			return sb.toString();
		}

		@Override
		public String toString()
		{
			StringBuilder sb = new StringBuilder('%');
			// Flags.UPPERCASE is set internally for legal conversions.
			Flags dupf = f.dup().remove(Flags.UPPERCASE);
			sb.append(dupf.toString());
			if (index > 0) sb.append(index).append('$');
			if (width != -1) sb.append(width);
			if (precision != -1) sb.append('.').append(precision);
			if (dt) sb.append(f.contains(Flags.UPPERCASE) ? 'T' : 't');
			sb.append(f.contains(Flags.UPPERCASE) ? Character.toUpperCase(c) : c);
			return sb.toString();
		}

		private void checkGeneral()
		{
			if ((c == Conversion.BOOLEAN || c == Conversion.HASHCODE) && f.contains(Flags.ALTERNATE)) failMismatch(Flags.ALTERNATE, c);
			// '-' requires a width
			if (width == -1 && f.contains(Flags.LEFT_JUSTIFY)) throw new MissingFormatWidthException(toString());
			checkBadFlags(Flags.PLUS, Flags.LEADING_SPACE, Flags.ZERO_PAD, Flags.GROUP, Flags.PARENTHESES);
		}

		private void checkDateTime()
		{
			if (precision != -1) throw new IllegalFormatPrecisionException(precision);
			if (!DateTime.isValid(c)) throw new UnknownFormatConversionException("t" + c);
			checkBadFlags(Flags.ALTERNATE);
			// '-' requires a width
			if (width == -1 && f.contains(Flags.LEFT_JUSTIFY)) throw new MissingFormatWidthException(toString());
		}

		private void checkCharacter()
		{
			if (precision != -1) throw new IllegalFormatPrecisionException(precision);
			checkBadFlags(Flags.ALTERNATE);
			// '-' requires a width
			if (width == -1 && f.contains(Flags.LEFT_JUSTIFY)) throw new MissingFormatWidthException(toString());
		}

		private void checkInteger()
		{
			checkNumeric();
			if (precision != -1) throw new IllegalFormatPrecisionException(precision);

			if (c == Conversion.DECIMAL_INTEGER) checkBadFlags(Flags.ALTERNATE);
			else if (c == Conversion.OCTAL_INTEGER) checkBadFlags(Flags.GROUP);
			else checkBadFlags(Flags.GROUP);
		}

		private void checkBadFlags(Flags... badFlags)
		{
			for (Flags badFlag : badFlags)
				if (f.contains(badFlag)) failMismatch(badFlag, c);
		}

		private void checkFloat()
		{
			checkNumeric();
			if (c == Conversion.DECIMAL_FLOAT)
			{
			}
			else if (c == Conversion.HEXADECIMAL_FLOAT)
			{
				checkBadFlags(Flags.PARENTHESES, Flags.GROUP);
			}
			else if (c == Conversion.SCIENTIFIC)
			{
				checkBadFlags(Flags.GROUP);
			}
			else if (c == Conversion.GENERAL)
			{
				checkBadFlags(Flags.ALTERNATE);
			}
		}

		private void checkNumeric()
		{
			if (width != -1 && width < 0) throw new IllegalFormatWidthException(width);

			if (precision != -1 && precision < 0) throw new IllegalFormatPrecisionException(precision);

			// '-' and '0' require a width
			if (width == -1 && (f.contains(Flags.LEFT_JUSTIFY) || f.contains(Flags.ZERO_PAD))) throw new MissingFormatWidthException(toString());

			// bad combination
			if ((f.contains(Flags.PLUS) && f.contains(Flags.LEADING_SPACE)) || (f.contains(Flags.LEFT_JUSTIFY) && f.contains(Flags.ZERO_PAD))) throw new IllegalFormatFlagsException(
				f.toString());
		}

		private void checkText()
		{
			if (precision != -1) throw new IllegalFormatPrecisionException(precision);
			switch (c)
			{
				case Conversion.PERCENT_SIGN :
					if (f.valueOf() != Flags.LEFT_JUSTIFY.valueOf() && f.valueOf() != Flags.NONE.valueOf()) throw new IllegalFormatFlagsException(f.toString());
					// '-' requires a width
					if (width == -1 && f.contains(Flags.LEFT_JUSTIFY)) throw new MissingFormatWidthException(toString());
					break;
				case Conversion.LINE_SEPARATOR :
					if (width != -1) throw new IllegalFormatWidthException(width);
					if (f.valueOf() != Flags.NONE.valueOf()) throw new IllegalFormatFlagsException(f.toString());
					break;
				default :
					assert false;
			}
		}

		private void print(byte value, Locale l) throws IOException
		{
			long v = value;
			if (value < 0 && (c == Conversion.OCTAL_INTEGER || c == Conversion.HEXADECIMAL_INTEGER))
			{
				v += (1L << 8);
				assert v >= 0 : v;
			}
			print(v, l);
		}

		private void print(short value, Locale l) throws IOException
		{
			long v = value;
			if (value < 0 && (c == Conversion.OCTAL_INTEGER || c == Conversion.HEXADECIMAL_INTEGER))
			{
				v += (1L << 16);
				assert v >= 0 : v;
			}
			print(v, l);
		}

		private void print(int value, Locale l) throws IOException
		{
			long v = value;
			if (value < 0 && (c == Conversion.OCTAL_INTEGER || c == Conversion.HEXADECIMAL_INTEGER))
			{
				v += (1L << 32);
				assert v >= 0 : v;
			}
			print(v, l);
		}

		private void print(long value, Locale l) throws IOException
		{

			StringBuilder sb = new StringBuilder();

			if (c == Conversion.DECIMAL_INTEGER)
			{
				boolean neg = value < 0;
				char[] va;
				if (value < 0) va = Long.toString(value, 10).substring(1).toCharArray();
				else va = Long.toString(value, 10).toCharArray();

				// leading sign indicator
				leadingSign(sb, neg);

				// the value
				localizedMagnitude(sb, va, f, adjustWidth(width, f, neg), l);

				// trailing sign indicator
				trailingSign(sb, neg);
			}
			else if (c == Conversion.OCTAL_INTEGER)
			{
				checkBadFlags(Flags.PARENTHESES, Flags.LEADING_SPACE, Flags.PLUS);
				String s = Long.toOctalString(value);
				int len = (f.contains(Flags.ALTERNATE) ? s.length() + 1 : s.length());

				// apply ALTERNATE (radix indicator for octal) before ZERO_PAD
				if (f.contains(Flags.ALTERNATE)) sb.append('0');
				if (f.contains(Flags.ZERO_PAD)) for (int i = 0; i < width - len; i++)
					sb.append('0');
				sb.append(s);
			}
			else if (c == Conversion.HEXADECIMAL_INTEGER)
			{
				checkBadFlags(Flags.PARENTHESES, Flags.LEADING_SPACE, Flags.PLUS);
				String s = Long.toHexString(value);
				int len = (f.contains(Flags.ALTERNATE) ? s.length() + 2 : s.length());

				// apply ALTERNATE (radix indicator for hex) before ZERO_PAD
				if (f.contains(Flags.ALTERNATE)) sb.append(f.contains(Flags.UPPERCASE) ? "0X" : "0x");
				if (f.contains(Flags.ZERO_PAD)) for (int i = 0; i < width - len; i++)
					sb.append('0');
				if (f.contains(Flags.UPPERCASE)) s = s.toUpperCase();
				sb.append(s);
			}

			// justify based on width
			a.append(justify(sb.toString()));
		}

		// neg := val < 0
		private StringBuilder leadingSign(StringBuilder sb, boolean neg)
		{
			if (!neg)
			{
				if (f.contains(Flags.PLUS))
				{
					sb.append('+');
				}
				else if (f.contains(Flags.LEADING_SPACE))
				{
					sb.append(' ');
				}
			}
			else
			{
				if (f.contains(Flags.PARENTHESES)) sb.append('(');
				else sb.append('-');
			}
			return sb;
		}

		// neg := val < 0
		private StringBuilder trailingSign(StringBuilder sb, boolean neg)
		{
			if (neg && f.contains(Flags.PARENTHESES)) sb.append(')');
			return sb;
		}

		private void print(BigInteger value, Locale l) throws IOException
		{
			StringBuilder sb = new StringBuilder();
			boolean neg = value.signum() == -1;
			BigInteger v = value.abs();

			// leading sign indicator
			leadingSign(sb, neg);

			// the value
			if (c == Conversion.DECIMAL_INTEGER)
			{
				char[] va = v.toString().toCharArray();
				localizedMagnitude(sb, va, f, adjustWidth(width, f, neg), l);
			}
			else if (c == Conversion.OCTAL_INTEGER)
			{
				String s = v.toString(8);

				int len = s.length() + sb.length();
				if (neg && f.contains(Flags.PARENTHESES)) len++;

				// apply ALTERNATE (radix indicator for octal) before ZERO_PAD
				if (f.contains(Flags.ALTERNATE))
				{
					len++;
					sb.append('0');
				}
				if (f.contains(Flags.ZERO_PAD))
				{
					for (int i = 0; i < width - len; i++)
						sb.append('0');
				}
				sb.append(s);
			}
			else if (c == Conversion.HEXADECIMAL_INTEGER)
			{
				String s = v.toString(16);

				int len = s.length() + sb.length();
				if (neg && f.contains(Flags.PARENTHESES)) len++;

				// apply ALTERNATE (radix indicator for hex) before ZERO_PAD
				if (f.contains(Flags.ALTERNATE))
				{
					len += 2;
					sb.append(f.contains(Flags.UPPERCASE) ? "0X" : "0x");
				}
				if (f.contains(Flags.ZERO_PAD)) for (int i = 0; i < width - len; i++)
					sb.append('0');
				if (f.contains(Flags.UPPERCASE)) s = s.toUpperCase();
				sb.append(s);
			}

			// trailing sign indicator
			trailingSign(sb, (value.signum() == -1));

			// justify based on width
			a.append(justify(sb.toString()));
		}

		private void print(float value, Locale l) throws IOException
		{
			print((double)value, l);
		}

		private void print(double value, Locale l) throws IOException
		{
			StringBuilder sb = new StringBuilder();
			boolean neg = Double.compare(value, 0.0) == -1;

			if (!Double.isNaN(value))
			{
				double v = Math.abs(value);

				// leading sign indicator
				leadingSign(sb, neg);

				// the value
				if (!Double.isInfinite(v)) print(sb, v, l, f, c, precision, neg);
				else sb.append(f.contains(Flags.UPPERCASE) ? "INFINITY" : "Infinity");

				// trailing sign indicator
				trailingSign(sb, neg);
			}
			else
			{
				sb.append(f.contains(Flags.UPPERCASE) ? "NAN" : "NaN");
			}

			// justify based on width
			a.append(justify(sb.toString()));
		}

		// !Double.isInfinite(value) && !Double.isNaN(value)
		private void print(StringBuilder sb, double value, Locale l, Flags f, char c, int precision, boolean neg) throws IOException
		{
			if (c == Conversion.SCIENTIFIC)
			{
				// Create a new FormattedFloatingDecimal with the desired
				// precision.
				int prec = (precision == -1 ? 6 : precision);

				FormattedFloatingDecimal fd = new FormattedFloatingDecimal(value, prec, FormattedFloatingDecimal.Form.SCIENTIFIC);

				char[] v = new char[MAX_FD_CHARS];
				int len = fd.getChars(v);

				char[] mant = addZeros(mantissa(v, len), prec);

				// If the precision is zero and the '#' flag is set, add the
				// requested decimal point.
				if (f.contains(Flags.ALTERNATE) && (prec == 0)) mant = addDot(mant);

				char[] exp = (value == 0.0) ? new char[] { '+', '0', '0' } : exponent(v, len);

				int newW = width;
				if (width != -1) newW = adjustWidth(width - exp.length - 1, f, neg);
				localizedMagnitude(sb, mant, f, newW, null);

				sb.append(f.contains(Flags.UPPERCASE) ? 'E' : 'e');

				Flags flags = f.dup().remove(Flags.GROUP);
				char sign = exp[0];
				assert (sign == '+' || sign == '-');
				sb.append(sign);

				char[] tmp = new char[exp.length - 1];
				System.arraycopy(exp, 1, tmp, 0, exp.length - 1);
				sb.append(localizedMagnitude(null, tmp, flags, -1, null));
			}
			else if (c == Conversion.DECIMAL_FLOAT)
			{
				// Create a new FormattedFloatingDecimal with the desired
				// precision.
				int prec = (precision == -1 ? 6 : precision);

				FormattedFloatingDecimal fd = new FormattedFloatingDecimal(value, prec, FormattedFloatingDecimal.Form.DECIMAL_FLOAT);

				// MAX_FD_CHARS + 1 (round?)
				char[] v = new char[MAX_FD_CHARS + 1 + Math.abs(fd.getExponent())];
				int len = fd.getChars(v);

				char[] mant = addZeros(mantissa(v, len), prec);

				// If the precision is zero and the '#' flag is set, add the
				// requested decimal point.
				if (f.contains(Flags.ALTERNATE) && (prec == 0)) mant = addDot(mant);

				int newW = width;
				if (width != -1) newW = adjustWidth(width, f, neg);
				localizedMagnitude(sb, mant, f, newW, l);
			}
			else if (c == Conversion.GENERAL)
			{
				int prec = precision;
				if (precision == -1) prec = 6;
				else if (precision == 0) prec = 1;

				FormattedFloatingDecimal fd = new FormattedFloatingDecimal(value, prec, FormattedFloatingDecimal.Form.GENERAL);

				// MAX_FD_CHARS + 1 (round?)
				char[] v = new char[MAX_FD_CHARS + 1 + Math.abs(fd.getExponent())];
				int len = fd.getChars(v);

				char[] exp = exponent(v, len);
				if (exp != null)
				{
					prec -= 1;
				}
				else
				{
					prec = prec - (value == 0 ? 0 : fd.getExponentRounded()) - 1;
				}

				char[] mant = addZeros(mantissa(v, len), prec);
				// If the precision is zero and the '#' flag is set, add the
				// requested decimal point.
				if (f.contains(Flags.ALTERNATE) && (prec == 0)) mant = addDot(mant);

				int newW = width;
				if (width != -1)
				{
					if (exp != null) newW = adjustWidth(width - exp.length - 1, f, neg);
					else newW = adjustWidth(width, f, neg);
				}
				localizedMagnitude(sb, mant, f, newW, null);

				if (exp != null)
				{
					sb.append(f.contains(Flags.UPPERCASE) ? 'E' : 'e');

					Flags flags = f.dup().remove(Flags.GROUP);
					char sign = exp[0];
					assert (sign == '+' || sign == '-');
					sb.append(sign);

					char[] tmp = new char[exp.length - 1];
					System.arraycopy(exp, 1, tmp, 0, exp.length - 1);
					sb.append(localizedMagnitude(null, tmp, flags, -1, null));
				}
			}
			else if (c == Conversion.HEXADECIMAL_FLOAT)
			{
				int prec = precision;
				if (precision == -1)
				// assume that we want all of the digits
				prec = 0;
				else if (precision == 0) prec = 1;

				String s = hexDouble(value, prec);

				char[] va;
				boolean upper = f.contains(Flags.UPPERCASE);
				sb.append(upper ? "0X" : "0x");

				if (f.contains(Flags.ZERO_PAD)) for (int i = 0; i < width - s.length() - 2; i++)
					sb.append('0');

				int idx = s.indexOf('p');
				va = s.substring(0, idx).toCharArray();
				if (upper)
				{
					String tmp = new String(va);
					// don't localize hex
					tmp = tmp.toUpperCase(Locale.US);
					va = tmp.toCharArray();
				}
				sb.append(prec != 0 ? addZeros(va, prec) : va);
				sb.append(upper ? 'P' : 'p');
				sb.append(s.substring(idx + 1));
			}
		}

		private char[] mantissa(char[] v, int len)
		{
			int i;
			for (i = 0; i < len; i++)
			{
				if (v[i] == 'e') break;
			}
			char[] tmp = new char[i];
			System.arraycopy(v, 0, tmp, 0, i);
			return tmp;
		}

		private char[] exponent(char[] v, int len)
		{
			int i;
			for (i = len - 1; i >= 0; i--)
			{
				if (v[i] == 'e') break;
			}
			if (i == -1) return null;
			char[] tmp = new char[len - i - 1];
			System.arraycopy(v, i + 1, tmp, 0, len - i - 1);
			return tmp;
		}

		// Add zeros to the requested precision.
		private char[] addZeros(char[] v, int prec)
		{
			// Look for the dot.  If we don't find one, the we'll need to add
			// it before we add the zeros.
			int i;
			for (i = 0; i < v.length; i++)
			{
				if (v[i] == '.') break;
			}
			boolean needDot = false;
			if (i == v.length)
			{
				needDot = true;
			}

			// Determine existing precision.
			int outPrec = v.length - i - (needDot ? 0 : 1);
			assert (outPrec <= prec);
			if (outPrec == prec) return v;

			// Create new array with existing contents.
			char[] tmp = new char[v.length + prec - outPrec + (needDot ? 1 : 0)];
			System.arraycopy(v, 0, tmp, 0, v.length);

			// Add dot if previously determined to be necessary.
			int start = v.length;
			if (needDot)
			{
				tmp[v.length] = '.';
				start++;
			}

			// Add zeros.
			for (int j = start; j < tmp.length; j++)
				tmp[j] = '0';

			return tmp;
		}

		// Method assumes that d > 0.
		private String hexDouble(double d, int prec)
		{
			// Let Double.toHexString handle simple cases
			if (!FpUtils.isFinite(d) || d == 0.0 || prec == 0 || prec >= 13)
			// remove "0x"
			return Double.toHexString(d).substring(2);
			else
			{
				assert (prec >= 1 && prec <= 12);

				int exponent = FpUtils.getExponent(d);
				boolean subnormal = (exponent == DoubleConsts.MIN_EXPONENT - 1);

				// If this is subnormal input so normalize (could be faster to
				// do as integer operation).
				if (subnormal)
				{
					scaleUp = FpUtils.scalb(1.0, 54);
					d *= scaleUp;
					// Calculate the exponent.  This is not just exponent + 54
					// since the former is not the normalized exponent.
					exponent = FpUtils.getExponent(d);
					assert exponent >= DoubleConsts.MIN_EXPONENT && exponent <= DoubleConsts.MAX_EXPONENT : exponent;
				}

				int precision = 1 + prec * 4;
				int shiftDistance = DoubleConsts.SIGNIFICAND_WIDTH - precision;
				assert (shiftDistance >= 1 && shiftDistance < DoubleConsts.SIGNIFICAND_WIDTH);

				long doppel = Double.doubleToLongBits(d);
				// Deterime the number of bits to keep.
				long newSignif = (doppel & (DoubleConsts.EXP_BIT_MASK | DoubleConsts.SIGNIF_BIT_MASK)) >> shiftDistance;
				// Bits to round away.
				long roundingBits = doppel & ~(~0L << shiftDistance);

				// To decide how to round, look at the low-order bit of the
				// working significand, the highest order discarded bit (the
				// round bit) and whether any of the lower order discarded bits
				// are nonzero (the sticky bit).

				boolean leastZero = (newSignif & 0x1L) == 0L;
				boolean round = ((1L << (shiftDistance - 1)) & roundingBits) != 0L;
				boolean sticky = shiftDistance > 1 && (~(1L << (shiftDistance - 1)) & roundingBits) != 0;
				if ((leastZero && round && sticky) || (!leastZero && round))
				{
					newSignif++;
				}

				long signBit = doppel & DoubleConsts.SIGN_BIT_MASK;
				newSignif = signBit | (newSignif << shiftDistance);
				double result = Double.longBitsToDouble(newSignif);

				if (Double.isInfinite(result))
				{
					// Infinite result generated by rounding
					return "1.0p1024";
				}
				else
				{
					String res = Double.toHexString(result).substring(2);
					if (!subnormal) return res;
					else
					{
						// Create a normalized subnormal string.
						int idx = res.indexOf('p');
						if (idx == -1)
						{
							// No 'p' character in hex string.
							assert false;
							return null;
						}
						else
						{
							// Get exponent and append at the end.
							String exp = res.substring(idx + 1);
							int iexp = Integer.parseInt(exp) - 54;
							return res.substring(0, idx) + "p" + Integer.toString(iexp);
						}
					}
				}
			}
		}

		private void print(BigDecimal value, Locale l) throws IOException
		{
			if (c == Conversion.HEXADECIMAL_FLOAT) failConversion(c, value);
			StringBuilder sb = new StringBuilder();
			boolean neg = value.signum() == -1;
			BigDecimal v = value.abs();
			// leading sign indicator
			leadingSign(sb, neg);

			// the value
			print(sb, v, l, f, c, precision, neg);

			// trailing sign indicator
			trailingSign(sb, neg);

			// justify based on width
			a.append(justify(sb.toString()));
		}

		// value > 0
		private void print(StringBuilder sb, BigDecimal value, Locale l, Flags f, char c, int precision, boolean neg) throws IOException
		{
			if (c == Conversion.SCIENTIFIC)
			{
				// Create a new BigDecimal with the desired precision.
				int prec = (precision == -1 ? 6 : precision);
				int scale = value.scale();
				int origPrec = value.precision();
				int nzeros = 0;
				int compPrec;

				if (prec > origPrec - 1)
				{
					compPrec = origPrec;
					nzeros = prec - (origPrec - 1);
				}
				else
				{
					compPrec = prec + 1;
				}

				MathContext mc = new MathContext(compPrec);
				BigDecimal v = new BigDecimal(value.unscaledValue(), scale, mc);

				BigDecimalLayout bdl = new BigDecimalLayout(v.unscaledValue(), v.scale(), BigDecimalLayoutForm.SCIENTIFIC);

				char[] mant = bdl.mantissa();

				// Add a decimal point if necessary.  The mantissa may not
				// contain a decimal point if the scale is zero (the internal
				// representation has no fractional part) or the original
				// precision is one. Append a decimal point if '#' is set or if
				// we require zero padding to get to the requested precision.
				if ((origPrec == 1 || !bdl.hasDot()) && (nzeros > 0 || (f.contains(Flags.ALTERNATE)))) mant = addDot(mant);

				// Add trailing zeros in the case precision is greater than
				// the number of available digits after the decimal separator.
				mant = trailingZeros(mant, nzeros);

				char[] exp = bdl.exponent();
				int newW = width;
				if (width != -1) newW = adjustWidth(width - exp.length - 1, f, neg);
				localizedMagnitude(sb, mant, f, newW, null);

				sb.append(f.contains(Flags.UPPERCASE) ? 'E' : 'e');

				Flags flags = f.dup().remove(Flags.GROUP);
				char sign = exp[0];
				assert (sign == '+' || sign == '-');
				sb.append(exp[0]);

				char[] tmp = new char[exp.length - 1];
				System.arraycopy(exp, 1, tmp, 0, exp.length - 1);
				sb.append(localizedMagnitude(null, tmp, flags, -1, null));
			}
			else if (c == Conversion.DECIMAL_FLOAT)
			{
				// Create a new BigDecimal with the desired precision.
				int prec = (precision == -1 ? 6 : precision);
				int scale = value.scale();
				int origPrec = value.precision();
				int nzeros = 0;
				int compPrec;
				if (scale < prec)
				{
					compPrec = origPrec;
					nzeros = prec - scale;
				}
				else
				{
					compPrec = origPrec - (scale - prec);
				}
				MathContext mc = new MathContext(compPrec);
				BigDecimal v = new BigDecimal(value.unscaledValue(), scale, mc);

				BigDecimalLayout bdl = new BigDecimalLayout(v.unscaledValue(), v.scale(), BigDecimalLayoutForm.DECIMAL_FLOAT);

				char mant[] = bdl.mantissa();

				// Add a decimal point if necessary.  The mantissa may not
				// contain a decimal point if the scale is zero (the internal
				// representation has no fractional part).  Append a decimal
				// point if '#' is set or we require zero padding to get to the
				// requested precision.
				if (scale == 0 && (f.contains(Flags.ALTERNATE) || nzeros > 0)) mant = addDot(bdl.mantissa());

				// Add trailing zeros if the precision is greater than the
				// number of available digits after the decimal separator.
				mant = trailingZeros(mant, nzeros);

				localizedMagnitude(sb, mant, f, adjustWidth(width, f, neg), l);
			}
			else if (c == Conversion.GENERAL)
			{
				int prec = precision;
				if (precision == -1) prec = 6;
				else if (precision == 0) prec = 1;

				BigDecimal tenToTheNegFour = BigDecimal.valueOf(1, 4);
				BigDecimal tenToThePrec = BigDecimal.valueOf(1, -prec);
				if ((value.equals(BigDecimal.ZERO)) || ((value.compareTo(tenToTheNegFour) != -1) && (value.compareTo(tenToThePrec) == -1)))
				{

					int e = -value.scale() + (value.unscaledValue().toString().length() - 1);

					// xxx.yyy
					//   g precision (# sig digits) = #x + #y
					//   f precision = #y
					//   exponent = #x - 1
					// => f precision = g precision - exponent - 1
					// 0.000zzz
					//   g precision (# sig digits) = #z
					//   f precision = #0 (after '.') + #z
					//   exponent = - #0 (after '.') - 1
					// => f precision = g precision - exponent - 1
					prec = prec - e - 1;

					print(sb, value, l, f, Conversion.DECIMAL_FLOAT, prec, neg);
				}
				else
				{
					print(sb, value, l, f, Conversion.SCIENTIFIC, prec - 1, neg);
				}
			}
			else if (c == Conversion.HEXADECIMAL_FLOAT)
			{
				// This conversion isn't supported.  The error should be
				// reported earlier.
				assert false;
			}
		}

		private class BigDecimalLayout
		{
			private StringBuilder mant;
			private StringBuilder exp;
			private boolean dot = false;

			public BigDecimalLayout(BigInteger intVal, int scale, BigDecimalLayoutForm form)
			{
				layout(intVal, scale, form);
			}

			public boolean hasDot()
			{
				return dot;
			}

			// char[] with canonical string representation
			public char[] layoutChars()
			{
				StringBuilder sb = new StringBuilder(mant);
				if (exp != null)
				{
					sb.append('E');
					sb.append(exp);
				}
				return toCharArray(sb);
			}

			public char[] mantissa()
			{
				return toCharArray(mant);
			}

			// The exponent will be formatted as a sign ('+' or '-') followed
			// by the exponent zero-padded to include at least two digits.
			public char[] exponent()
			{
				return toCharArray(exp);
			}

			private char[] toCharArray(StringBuilder sb)
			{
				if (sb == null) return null;
				char[] result = new char[sb.length()];
				sb.getChars(0, result.length, result, 0);
				return result;
			}

			private void layout(BigInteger intVal, int scale, BigDecimalLayoutForm form)
			{
				char coeff[] = intVal.toString().toCharArray();

				// Construct a buffer, with sufficient capacity for all cases.
				// If E-notation is needed, length will be: +1 if negative, +1
				// if '.' needed, +2 for "E+", + up to 10 for adjusted
				// exponent.  Otherwise it could have +1 if negative, plus
				// leading "0.00000"
				mant = new StringBuilder(coeff.length + 14);

				if (scale == 0)
				{
					int len = coeff.length;
					if (len > 1)
					{
						mant.append(coeff[0]);
						if (form == BigDecimalLayoutForm.SCIENTIFIC)
						{
							mant.append('.');
							dot = true;
							mant.append(coeff, 1, len - 1);
							exp = new StringBuilder("+");
							if (len < 10) exp.append("0").append(len - 1);
							else exp.append(len - 1);
						}
						else
						{
							mant.append(coeff, 1, len - 1);
						}
					}
					else
					{
						mant.append(coeff);
						if (form == BigDecimalLayoutForm.SCIENTIFIC) exp = new StringBuilder("+00");
					}
					return;
				}
				long adjusted = -(long)scale + (coeff.length - 1);
				if (form == BigDecimalLayoutForm.DECIMAL_FLOAT)
				{
					// count of padding zeros
					int pad = scale - coeff.length;
					if (pad >= 0)
					{
						// 0.xxx form
						mant.append("0.");
						dot = true;
						for (; pad > 0; pad--)
							mant.append('0');
						mant.append(coeff);
					}
					else
					{
						// xx.xx form
						mant.append(coeff, 0, -pad);
						mant.append('.');
						dot = true;
						mant.append(coeff, -pad, scale);
					}
				}
				else
				{
					// x.xxx form
					mant.append(coeff[0]);
					if (coeff.length > 1)
					{
						mant.append('.');
						dot = true;
						mant.append(coeff, 1, coeff.length - 1);
					}
					exp = new StringBuilder();
					if (adjusted != 0)
					{
						long abs = Math.abs(adjusted);
						// require sign
						exp.append(adjusted < 0 ? '-' : '+');
						if (abs < 10) exp.append('0');
						exp.append(abs);
					}
					else
					{
						exp.append("+00");
					}
				}
			}
		}

		private int adjustWidth(int width, Flags f, boolean neg)
		{
			int newW = width;
			if (newW != -1 && neg && f.contains(Flags.PARENTHESES)) newW--;
			return newW;
		}

		// Add a '.' to th mantissa if required
		private char[] addDot(char[] mant)
		{
			char[] tmp = mant;
			tmp = new char[mant.length + 1];
			System.arraycopy(mant, 0, tmp, 0, mant.length);
			tmp[tmp.length - 1] = '.';
			return tmp;
		}

		// Add trailing zeros in the case precision is greater than the number
		// of available digits after the decimal separator.
		private char[] trailingZeros(char[] mant, int nzeros)
		{
			char[] tmp = mant;
			if (nzeros > 0)
			{
				tmp = new char[mant.length + nzeros];
				System.arraycopy(mant, 0, tmp, 0, mant.length);
				for (int i = mant.length; i < tmp.length; i++)
					tmp[i] = '0';
			}
			return tmp;
		}

		private void print(Calendar t, char c, Locale l) throws IOException
		{
			StringBuilder sb = new StringBuilder();
			print(sb, t, c, l);

			// justify based on width
			String s = justify(sb.toString());
			if (f.contains(Flags.UPPERCASE)) s = s.toUpperCase();

			a.append(s);
		}

		private Appendable print(StringBuilder sb, Calendar t, char c, Locale l) throws IOException
		{
			assert (width == -1);
			if (sb == null) sb = new StringBuilder();
			switch (c)
			{
				case DateTime.HOUR_OF_DAY_0 : // 'H' (00 - 23)
				case DateTime.HOUR_0 : // 'I' (01 - 12)
				case DateTime.HOUR_OF_DAY : // 'k' (0 - 23) -- like H
				case DateTime.HOUR :
				{ // 'l' (1 - 12) -- like I
					int i = t.get(Calendar.HOUR_OF_DAY);
					if (c == DateTime.HOUR_0 || c == DateTime.HOUR) i = (i == 0 || i == 12 ? 12 : i % 12);
					Flags flags = (c == DateTime.HOUR_OF_DAY_0 || c == DateTime.HOUR_0 ? Flags.ZERO_PAD : Flags.NONE);
					sb.append(localizedMagnitude(null, i, flags, 2, l));
					break;
				}
				case DateTime.MINUTE :
				{ // 'M' (00 - 59)
					int i = t.get(Calendar.MINUTE);
					Flags flags = Flags.ZERO_PAD;
					sb.append(localizedMagnitude(null, i, flags, 2, l));
					break;
				}
				case DateTime.NANOSECOND :
				{ // 'N' (000000000 - 999999999)
					int i = t.get(Calendar.MILLISECOND) * 1000000;
					Flags flags = Flags.ZERO_PAD;
					sb.append(localizedMagnitude(null, i, flags, 9, l));
					break;
				}
				case DateTime.MILLISECOND :
				{ // 'L' (000 - 999)
					int i = t.get(Calendar.MILLISECOND);
					Flags flags = Flags.ZERO_PAD;
					sb.append(localizedMagnitude(null, i, flags, 3, l));
					break;
				}
				case DateTime.MILLISECOND_SINCE_EPOCH :
				{ // 'Q' (0 - 99...?)
					long i = t.getTimeInMillis();
					Flags flags = Flags.NONE;
					sb.append(localizedMagnitude(null, i, flags, width, l));
					break;
				}
				case DateTime.AM_PM :
				{ // 'p' (am or pm)
					// Calendar.AM = 0, Calendar.PM = 1, LocaleElements defines upper
					String[] ampm = { "AM", "PM" };
					if (l != null && l != Locale.US)
					{
						DateFormatSymbols dfs = new DateFormatSymbols(l);
						ampm = dfs.getAmPmStrings();
					}
					String s = ampm[t.get(Calendar.AM_PM)];
					sb.append(s.toLowerCase(l != null ? l : Locale.US));
					break;
				}
				case DateTime.SECONDS_SINCE_EPOCH :
				{ // 's' (0 - 99...?)
					long i = t.getTimeInMillis() / 1000;
					Flags flags = Flags.NONE;
					sb.append(localizedMagnitude(null, i, flags, width, l));
					break;
				}
				case DateTime.SECOND :
				{ // 'S' (00 - 60 - leap second)
					int i = t.get(Calendar.SECOND);
					Flags flags = Flags.ZERO_PAD;
					sb.append(localizedMagnitude(null, i, flags, 2, l));
					break;
				}
				case DateTime.ZONE_NUMERIC :
				{ // 'z' ({-|+}####) - ls minus?
					int i = t.get(Calendar.ZONE_OFFSET);
					boolean neg = i < 0;
					sb.append(neg ? '-' : '+');
					if (neg) i = -i;
					int min = i / 60000;
					// combine minute and hour into a single integer
					int offset = (min / 60) * 100 + (min % 60);
					Flags flags = Flags.ZERO_PAD;

					sb.append(localizedMagnitude(null, offset, flags, 4, l));
					break;
				}
				case DateTime.ZONE :
				{ // 'Z' (symbol)
					TimeZone tz = t.getTimeZone();
					sb.append(tz.getDisplayName((t.get(Calendar.DST_OFFSET) != 0), TimeZone.SHORT, l));
					break;
				}

					// Date
				case DateTime.NAME_OF_DAY_ABBREV : // 'a'
				case DateTime.NAME_OF_DAY :
				{ // 'A'
					int i = t.get(Calendar.DAY_OF_WEEK);
					Locale lt = ((l == null) ? Locale.US : l);
					DateFormatSymbols dfs = new DateFormatSymbols(lt);
					if (c == DateTime.NAME_OF_DAY) sb.append(dfs.getWeekdays()[i]);
					else sb.append(dfs.getShortWeekdays()[i]);
					break;
				}
				case DateTime.NAME_OF_MONTH_ABBREV : // 'b'
				case DateTime.NAME_OF_MONTH_ABBREV_X : // 'h' -- same b
				case DateTime.NAME_OF_MONTH :
				{ // 'B'
					int i = t.get(Calendar.MONTH);
					Locale lt = ((l == null) ? Locale.US : l);
					DateFormatSymbols dfs = new DateFormatSymbols(lt);
					if (c == DateTime.NAME_OF_MONTH) sb.append(dfs.getMonths()[i]);
					else sb.append(dfs.getShortMonths()[i]);
					break;
				}
				case DateTime.CENTURY : // 'C' (00 - 99)
				case DateTime.YEAR_2 : // 'y' (00 - 99)
				case DateTime.YEAR_4 :
				{ // 'Y' (0000 - 9999)
					int i = t.get(Calendar.YEAR);
					int size = 2;
					switch (c)
					{
						case DateTime.CENTURY :
							i /= 100;
							break;
						case DateTime.YEAR_2 :
							i %= 100;
							break;
						case DateTime.YEAR_4 :
							size = 4;
							break;
					}
					Flags flags = Flags.ZERO_PAD;
					sb.append(localizedMagnitude(null, i, flags, size, l));
					break;
				}
				case DateTime.DAY_OF_MONTH_0 : // 'd' (01 - 31)
				case DateTime.DAY_OF_MONTH :
				{ // 'e' (1 - 31) -- like d
					int i = t.get(Calendar.DATE);
					Flags flags = (c == DateTime.DAY_OF_MONTH_0 ? Flags.ZERO_PAD : Flags.NONE);
					sb.append(localizedMagnitude(null, i, flags, 2, l));
					break;
				}
				case DateTime.DAY_OF_YEAR :
				{ // 'j' (001 - 366)
					int i = t.get(Calendar.DAY_OF_YEAR);
					Flags flags = Flags.ZERO_PAD;
					sb.append(localizedMagnitude(null, i, flags, 3, l));
					break;
				}
				case DateTime.MONTH :
				{ // 'm' (01 - 12)
					int i = t.get(Calendar.MONTH) + 1;
					Flags flags = Flags.ZERO_PAD;
					sb.append(localizedMagnitude(null, i, flags, 2, l));
					break;
				}

					// Composites
				case DateTime.TIME : // 'T' (24 hour hh:mm:ss - %tH:%tM:%tS)
				case DateTime.TIME_24_HOUR :
				{ // 'R' (hh:mm same as %H:%M)
					char sep = ':';
					print(sb, t, DateTime.HOUR_OF_DAY_0, l).append(sep);
					print(sb, t, DateTime.MINUTE, l);
					if (c == DateTime.TIME)
					{
						sb.append(sep);
						print(sb, t, DateTime.SECOND, l);
					}
					break;
				}
				case DateTime.TIME_12_HOUR :
				{ // 'r' (hh:mm:ss [AP]M)
					char sep = ':';
					print(sb, t, DateTime.HOUR_0, l).append(sep);
					print(sb, t, DateTime.MINUTE, l).append(sep);
					print(sb, t, DateTime.SECOND, l).append(' ');
					// this may be in wrong place for some locales
					StringBuilder tsb = new StringBuilder();
					print(tsb, t, DateTime.AM_PM, l);
					sb.append(tsb.toString().toUpperCase(l != null ? l : Locale.US));
					break;
				}
				case DateTime.DATE_TIME :
				{ // 'c' (Sat Nov 04 12:02:33 EST 1999)
					char sep = ' ';
					print(sb, t, DateTime.NAME_OF_DAY_ABBREV, l).append(sep);
					print(sb, t, DateTime.NAME_OF_MONTH_ABBREV, l).append(sep);
					print(sb, t, DateTime.DAY_OF_MONTH_0, l).append(sep);
					print(sb, t, DateTime.TIME, l).append(sep);
					print(sb, t, DateTime.ZONE, l).append(sep);
					print(sb, t, DateTime.YEAR_4, l);
					break;
				}
				case DateTime.DATE :
				{ // 'D' (mm/dd/yy)
					char sep = '/';
					print(sb, t, DateTime.MONTH, l).append(sep);
					print(sb, t, DateTime.DAY_OF_MONTH_0, l).append(sep);
					print(sb, t, DateTime.YEAR_2, l);
					break;
				}
				case DateTime.ISO_STANDARD_DATE :
				{ // 'F' (%Y-%m-%d)
					char sep = '-';
					print(sb, t, DateTime.YEAR_4, l).append(sep);
					print(sb, t, DateTime.MONTH, l).append(sep);
					print(sb, t, DateTime.DAY_OF_MONTH_0, l);
					break;
				}
				default :
					assert false;
			}
			return sb;
		}

		// -- Methods to support throwing exceptions --

		private void failMismatch(Flags f, char c)
		{
			String fs = f.toString();
			throw new FormatFlagsConversionMismatchException(fs, c);
		}

		private void failConversion(char c, Object arg)
		{
			throw new IllegalFormatConversionException(c, arg.getClass());
		}

		private char getZero(Locale l)
		{
			if ((l != null) && !l.equals(locale()))
			{
				DecimalFormatSymbols dfs = new DecimalFormatSymbols(l);
				return dfs.getZeroDigit();
			}
			return zero;
		}

		private StringBuilder localizedMagnitude(StringBuilder sb, long value, Flags f, int width, Locale l)
		{
			char[] va = Long.toString(value, 10).toCharArray();
			return localizedMagnitude(sb, va, f, width, l);
		}

		private StringBuilder localizedMagnitude(StringBuilder sb, char[] value, Flags f, int width, Locale l)
		{
			if (sb == null) sb = new StringBuilder();
			int begin = sb.length();

			char zero = getZero(l);

			// determine localized grouping separator and size
			char grpSep = '\0';
			int grpSize = -1;
			char decSep = '\0';

			int len = value.length;
			int dot = len;
			for (int j = 0; j < len; j++)
			{
				if (value[j] == '.')
				{
					dot = j;
					break;
				}
			}

			if (dot < len)
			{
				if (l == null || l.equals(Locale.US))
				{
					decSep = '.';
				}
				else
				{
					DecimalFormatSymbols dfs = new DecimalFormatSymbols(l);
					decSep = dfs.getDecimalSeparator();
				}
			}

			if (f.contains(Flags.GROUP))
			{
				if (l == null || l.equals(Locale.US))
				{
					grpSep = ',';
					grpSize = 3;
				}
				else
				{
					DecimalFormatSymbols dfs = new DecimalFormatSymbols(l);
					grpSep = dfs.getGroupingSeparator();
					DecimalFormat df = (DecimalFormat)NumberFormat.getIntegerInstance(l);
					grpSize = df.getGroupingSize();
				}
			}

			// localize the digits inserting group separators as necessary
			for (int j = 0; j < len; j++)
			{
				if (j == dot)
				{
					sb.append(decSep);
					// no more group separators after the decimal separator
					grpSep = '\0';
					continue;
				}

				char c = value[j];
				sb.append((char)((c - '0') + zero));
				if (grpSep != '\0' && j != dot - 1 && ((dot - j) % grpSize == 1)) sb.append(grpSep);
			}

			// apply zero padding
			len = sb.length();
			if (width != -1 && f.contains(Flags.ZERO_PAD)) for (int k = 0; k < width - len; k++)
				sb.insert(begin, zero);

			return sb;
		}
	}

	private static class Flags
	{
		private int flags;

		static final Flags NONE = new Flags(0); // ''

		// duplicate declarations from Formattable.java
		static final Flags LEFT_JUSTIFY = new Flags(1 << 0); // '-'
		static final Flags UPPERCASE = new Flags(1 << 1); // '^'
		static final Flags ALTERNATE = new Flags(1 << 2); // '#'

		// numerics
		static final Flags PLUS = new Flags(1 << 3); // '+'
		static final Flags LEADING_SPACE = new Flags(1 << 4); // ' '
		static final Flags ZERO_PAD = new Flags(1 << 5); // '0'
		static final Flags GROUP = new Flags(1 << 6); // ','
		static final Flags PARENTHESES = new Flags(1 << 7); // '('

		// indexing
		static final Flags PREVIOUS = new Flags(1 << 8); // '<'

		private Flags(int f)
		{
			flags = f;
		}

		public int valueOf()
		{
			return flags;
		}

		public boolean contains(Flags f)
		{
			return (flags & f.valueOf()) == f.valueOf();
		}

		public Flags dup()
		{
			return new Flags(flags);
		}

		private Flags add(Flags f)
		{
			flags |= f.valueOf();
			return this;
		}

		public Flags remove(Flags f)
		{
			flags &= ~f.valueOf();
			return this;
		}

		public static Flags parse(String s)
		{
			char[] ca = s.toCharArray();
			Flags f = new Flags(0);
			for (char element : ca)
			{
				Flags v = parse(element);
				if (f.contains(v)) throw new DuplicateFormatFlagsException(v.toString());
				f.add(v);
			}
			return f;
		}

		// parse those flags which may be provided by users
		private static Flags parse(char c)
		{
			switch (c)
			{
				case '-' :
					return LEFT_JUSTIFY;
				case '#' :
					return ALTERNATE;
				case '+' :
					return PLUS;
				case ' ' :
					return LEADING_SPACE;
				case '0' :
					return ZERO_PAD;
				case ',' :
					return GROUP;
				case '(' :
					return PARENTHESES;
				case '<' :
					return PREVIOUS;
				default :
					throw new UnknownFormatFlagsException(String.valueOf(c));
			}
		}

		// Returns a string representation of the current <tt>Flags</tt>.
		public static String toString(Flags f)
		{
			return f.toString();
		}

		@Override
		public String toString()
		{
			StringBuilder sb = new StringBuilder();
			if (contains(LEFT_JUSTIFY)) sb.append('-');
			if (contains(UPPERCASE)) sb.append('^');
			if (contains(ALTERNATE)) sb.append('#');
			if (contains(PLUS)) sb.append('+');
			if (contains(LEADING_SPACE)) sb.append(' ');
			if (contains(ZERO_PAD)) sb.append('0');
			if (contains(GROUP)) sb.append(',');
			if (contains(PARENTHESES)) sb.append('(');
			if (contains(PREVIOUS)) sb.append('<');
			return sb.toString();
		}
	}

	private static class Conversion
	{
		// Byte, Short, Integer, Long, BigInteger
		// (and associated primitives due to autoboxing)
		static final char DECIMAL_INTEGER = 'd';
		static final char OCTAL_INTEGER = 'o';
		static final char HEXADECIMAL_INTEGER = 'x';
		static final char HEXADECIMAL_INTEGER_UPPER = 'X';

		// Float, Double, BigDecimal
		// (and associated primitives due to autoboxing)
		static final char SCIENTIFIC = 'e';
		static final char SCIENTIFIC_UPPER = 'E';
		static final char GENERAL = 'g';
		static final char GENERAL_UPPER = 'G';
		static final char DECIMAL_FLOAT = 'f';
		static final char HEXADECIMAL_FLOAT = 'a';
		static final char HEXADECIMAL_FLOAT_UPPER = 'A';

		// Character, Byte, Short, Integer
		// (and associated primitives due to autoboxing)
		static final char CHARACTER = 'c';
		static final char CHARACTER_UPPER = 'C';

		// java.util.Date, java.util.Calendar, long
		static final char DATE_TIME = 't';
		static final char DATE_TIME_UPPER = 'T';

		// if (arg.TYPE != boolean) return boolean
		// if (arg != null) return true; else return false;
		static final char BOOLEAN = 'b';
		static final char BOOLEAN_UPPER = 'B';
		// if (arg instanceof Formattable) arg.formatTo()
		// else arg.toString();
		static final char STRING = 's';
		static final char STRING_UPPER = 'S';
		// arg.hashCode()
		static final char HASHCODE = 'h';
		static final char HASHCODE_UPPER = 'H';

		static final char LINE_SEPARATOR = 'n';
		static final char PERCENT_SIGN = '%';

		static boolean isValid(char c)
		{
			return (isGeneral(c) || isInteger(c) || isFloat(c) || isText(c) || c == 't' || c == 'c');
		}

		// Returns true iff the Conversion is applicable to all objects.
		static boolean isGeneral(char c)
		{
			switch (c)
			{
				case BOOLEAN :
				case BOOLEAN_UPPER :
				case STRING :
				case STRING_UPPER :
				case HASHCODE :
				case HASHCODE_UPPER :
					return true;
				default :
					return false;
			}
		}

		// Returns true iff the Conversion is an integer type.
		static boolean isInteger(char c)
		{
			switch (c)
			{
				case DECIMAL_INTEGER :
				case OCTAL_INTEGER :
				case HEXADECIMAL_INTEGER :
				case HEXADECIMAL_INTEGER_UPPER :
					return true;
				default :
					return false;
			}
		}

		// Returns true iff the Conversion is a floating-point type.
		static boolean isFloat(char c)
		{
			switch (c)
			{
				case SCIENTIFIC :
				case SCIENTIFIC_UPPER :
				case GENERAL :
				case GENERAL_UPPER :
				case DECIMAL_FLOAT :
				case HEXADECIMAL_FLOAT :
				case HEXADECIMAL_FLOAT_UPPER :
					return true;
				default :
					return false;
			}
		}

		// Returns true iff the Conversion does not require an argument
		static boolean isText(char c)
		{
			switch (c)
			{
				case LINE_SEPARATOR :
				case PERCENT_SIGN :
					return true;
				default :
					return false;
			}
		}
	}

	private static class DateTime
	{
		static final char HOUR_OF_DAY_0 = 'H'; // (00 - 23)
		static final char HOUR_0 = 'I'; // (01 - 12)
		static final char HOUR_OF_DAY = 'k'; // (0 - 23) -- like H
		static final char HOUR = 'l'; // (1 - 12) -- like I
		static final char MINUTE = 'M'; // (00 - 59)
		static final char NANOSECOND = 'N'; // (000000000 - 999999999)
		static final char MILLISECOND = 'L'; // jdk, not in gnu (000 - 999)
		static final char MILLISECOND_SINCE_EPOCH = 'Q'; // (0 - 99...?)
		static final char AM_PM = 'p'; // (am or pm)
		static final char SECONDS_SINCE_EPOCH = 's'; // (0 - 99...?)
		static final char SECOND = 'S'; // (00 - 60 - leap second)
		static final char TIME = 'T'; // (24 hour hh:mm:ss)
		static final char ZONE_NUMERIC = 'z'; // (-1200 - +1200) - ls minus?
		static final char ZONE = 'Z'; // (symbol)

		// Date
		static final char NAME_OF_DAY_ABBREV = 'a'; // 'a'
		static final char NAME_OF_DAY = 'A'; // 'A'
		static final char NAME_OF_MONTH_ABBREV = 'b'; // 'b'
		static final char NAME_OF_MONTH = 'B'; // 'B'
		static final char CENTURY = 'C'; // (00 - 99)
		static final char DAY_OF_MONTH_0 = 'd'; // (01 - 31)
		static final char DAY_OF_MONTH = 'e'; // (1 - 31) -- like d
// *    static final char ISO_WEEK_OF_YEAR_2    = 'g'; // cross %y %V
// *    static final char ISO_WEEK_OF_YEAR_4    = 'G'; // cross %Y %V
		static final char NAME_OF_MONTH_ABBREV_X = 'h'; // -- same b
		static final char DAY_OF_YEAR = 'j'; // (001 - 366)
		static final char MONTH = 'm'; // (01 - 12)
// *    static final char DAY_OF_WEEK_1         = 'u'; // (1 - 7) Monday
// *    static final char WEEK_OF_YEAR_SUNDAY   = 'U'; // (0 - 53) Sunday+
// *    static final char WEEK_OF_YEAR_MONDAY_01 = 'V'; // (01 - 53) Monday+
// *    static final char DAY_OF_WEEK_0         = 'w'; // (0 - 6) Sunday
// *    static final char WEEK_OF_YEAR_MONDAY   = 'W'; // (00 - 53) Monday
		static final char YEAR_2 = 'y'; // (00 - 99)
		static final char YEAR_4 = 'Y'; // (0000 - 9999)

		// Composites
		static final char TIME_12_HOUR = 'r'; // (hh:mm:ss [AP]M)
		static final char TIME_24_HOUR = 'R'; // (hh:mm same as %H:%M)
// *    static final char LOCALE_TIME   = 'X'; // (%H:%M:%S) - parse format?
		static final char DATE_TIME = 'c';
		// (Sat Nov 04 12:02:33 EST 1999)
		static final char DATE = 'D'; // (mm/dd/yy)
		static final char ISO_STANDARD_DATE = 'F'; // (%Y-%m-%d)
// *    static final char LOCALE_DATE           = 'x'; // (mm/dd/yy)

		static boolean isValid(char c)
		{
			switch (c)
			{
				case HOUR_OF_DAY_0 :
				case HOUR_0 :
				case HOUR_OF_DAY :
				case HOUR :
				case MINUTE :
				case NANOSECOND :
				case MILLISECOND :
				case MILLISECOND_SINCE_EPOCH :
				case AM_PM :
				case SECONDS_SINCE_EPOCH :
				case SECOND :
				case TIME :
				case ZONE_NUMERIC :
				case ZONE :

					// Date
				case NAME_OF_DAY_ABBREV :
				case NAME_OF_DAY :
				case NAME_OF_MONTH_ABBREV :
				case NAME_OF_MONTH :
				case CENTURY :
				case DAY_OF_MONTH_0 :
				case DAY_OF_MONTH :
// *        case ISO_WEEK_OF_YEAR_2:
// *        case ISO_WEEK_OF_YEAR_4:
				case NAME_OF_MONTH_ABBREV_X :
				case DAY_OF_YEAR :
				case MONTH :
// *        case DAY_OF_WEEK_1:
// *        case WEEK_OF_YEAR_SUNDAY:
// *        case WEEK_OF_YEAR_MONDAY_01:
// *        case DAY_OF_WEEK_0:
// *        case WEEK_OF_YEAR_MONDAY:
				case YEAR_2 :
				case YEAR_4 :

					// Composites
				case TIME_12_HOUR :
				case TIME_24_HOUR :
// *        case LOCALE_TIME:
				case DATE_TIME :
				case DATE :
				case ISO_STANDARD_DATE :
// *        case LOCALE_DATE:
					return true;
				default :
					return false;
			}
		}
	}
}
