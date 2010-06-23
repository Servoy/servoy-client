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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * java.util.Properties class copy with more access possibilities.
 * @author jblok
 */
public class OpenProperties extends HashMap<String, String>
{
	private static final String keyValueSeparators = "=: \t\r\n\f";

	private static final String strictKeyValueSeparators = "=:";

	private static final String specialSaveChars = "=: \t\r\n\f#!";

	private static final String whiteSpaceChars = " \t\r\n\f";

	public synchronized void load(InputStream inStream) throws IOException
	{
		load(new InputStreamReader(inStream, "8859_1"));
	}

	public synchronized void load(Reader inStream) throws IOException
	{
		BufferedReader in = new BufferedReader(inStream);
		while (true)
		{
			// Get next line
			String line = in.readLine();
			if (line == null) return;

			if (line.length() > 0)
			{

				// Find start of key
				int len = line.length();
				int keyStart;
				for (keyStart = 0; keyStart < len; keyStart++)
					if (whiteSpaceChars.indexOf(line.charAt(keyStart)) == -1) break;

				// Blank lines are ignored
				if (keyStart == len) continue;

				// Continue lines that end in slashes if they are not comments
				char firstChar = line.charAt(keyStart);
				if ((firstChar != '#') && (firstChar != '!'))
				{
					while (continueLine(line))
					{
						String nextLine = in.readLine();
						if (nextLine == null) nextLine = "";
						String loppedLine = line.substring(0, len - 1);
						// Advance beyond whitespace on new line
						int startIndex;
						for (startIndex = 0; startIndex < nextLine.length(); startIndex++)
							if (whiteSpaceChars.indexOf(nextLine.charAt(startIndex)) == -1) break;
						nextLine = nextLine.substring(startIndex, nextLine.length());
						line = new String(loppedLine + nextLine);
						len = line.length();
					}

					// Find separation between key and value
					int separatorIndex;
					for (separatorIndex = keyStart; separatorIndex < len; separatorIndex++)
					{
						char currentChar = line.charAt(separatorIndex);
						if (currentChar == '\\') separatorIndex++;
						else if (keyValueSeparators.indexOf(currentChar) != -1) break;
					}

					// Skip over whitespace after key if any
					int valueIndex;
					for (valueIndex = separatorIndex; valueIndex < len; valueIndex++)
						if (whiteSpaceChars.indexOf(line.charAt(valueIndex)) == -1) break;

					// Skip over one non whitespace key value separators if any
					if (valueIndex < len) if (strictKeyValueSeparators.indexOf(line.charAt(valueIndex)) != -1) valueIndex++;

					// Skip over white space after other separators if any
					while (valueIndex < len)
					{
						if (whiteSpaceChars.indexOf(line.charAt(valueIndex)) == -1) break;
						valueIndex++;
					}
					String key = line.substring(keyStart, separatorIndex);
					String value = (separatorIndex < len) ? line.substring(valueIndex, len) : "";

					// Convert then store key and value
					key = loadConvert(key);
					value = loadConvert(value);
					put(key, value);
				}
			}
		}
	}

	/*
	 * Returns true if the given line is a line that must be appended to the next line
	 */
	private boolean continueLine(String line)
	{
		int slashCount = 0;
		int index = line.length() - 1;
		while ((index >= 0) && (line.charAt(index--) == '\\'))
			slashCount++;
		return (slashCount % 2 == 1);
	}

	/*
	 * Converts encoded &#92;uxxxx to unicode chars and changes special saved chars to their original forms
	 */
	private String loadConvert(String theString)
	{
		char aChar;
		int len = theString.length();
		StringBuffer outBuffer = new StringBuffer(len);

		for (int x = 0; x < len;)
		{
			aChar = theString.charAt(x++);
			if (aChar == '\\')
			{
				aChar = theString.charAt(x++);
				if (aChar == 'u')
				{
					// Read the xxxx
					int value = 0;
					for (int i = 0; i < 4; i++)
					{
						aChar = theString.charAt(x++);
						switch (aChar)
						{
							case '0' :
							case '1' :
							case '2' :
							case '3' :
							case '4' :
							case '5' :
							case '6' :
							case '7' :
							case '8' :
							case '9' :
								value = (value << 4) + aChar - '0';
								break;
							case 'a' :
							case 'b' :
							case 'c' :
							case 'd' :
							case 'e' :
							case 'f' :
								value = (value << 4) + 10 + aChar - 'a';
								break;
							case 'A' :
							case 'B' :
							case 'C' :
							case 'D' :
							case 'E' :
							case 'F' :
								value = (value << 4) + 10 + aChar - 'A';
								break;
							default :
								throw new IllegalArgumentException("Malformed \\uxxxx encoding.");
						}
					}
					outBuffer.append((char)value);
				}
				else
				{
					if (aChar == 't') aChar = '\t';
					else if (aChar == 'r') aChar = '\r';
					else if (aChar == 'n') aChar = '\n';
					else if (aChar == 'f') aChar = '\f';
					outBuffer.append(aChar);
				}
			}
			else outBuffer.append(aChar);
		}
		return outBuffer.toString();
	}

	/*
	 * Converts unicodes to encoded &#92;uxxxx and writes out any of the characters in specialSaveChars with a preceding slash
	 */
	private String saveConvert(String theString, boolean escapeSpace)
	{
		int len = theString.length();
		StringBuffer outBuffer = new StringBuffer(len * 2);

		for (int x = 0; x < len; x++)
		{
			char aChar = theString.charAt(x);
			switch (aChar)
			{
				case ' ' :
					if (x == 0 || escapeSpace) outBuffer.append('\\');

					outBuffer.append(' ');
					break;
				case '\\' :
					outBuffer.append('\\');
					outBuffer.append('\\');
					break;
				case '\t' :
					outBuffer.append('\\');
					outBuffer.append('t');
					break;
				case '\n' :
					outBuffer.append('\\');
					outBuffer.append('n');
					break;
				case '\r' :
					outBuffer.append('\\');
					outBuffer.append('r');
					break;
				case '\f' :
					outBuffer.append('\\');
					outBuffer.append('f');
					break;
				default :
					if ((aChar < 0x0020) || (aChar > 0x007e))
					{
						outBuffer.append('\\');
						outBuffer.append('u');
						outBuffer.append(toHex((aChar >> 12) & 0xF));
						outBuffer.append(toHex((aChar >> 8) & 0xF));
						outBuffer.append(toHex((aChar >> 4) & 0xF));
						outBuffer.append(toHex(aChar & 0xF));
					}
					else
					{
						if (specialSaveChars.indexOf(aChar) != -1) outBuffer.append('\\');
						outBuffer.append(aChar);
					}
			}
		}
		return outBuffer.toString();
	}

	/**
	 * Calls the <code>store(OutputStream out, String header)</code> method and suppresses IOExceptions that were thrown.
	 * 
	 * @deprecated This method does not throw an IOException if an I/O error occurs while saving the property list. As of the Java 2 platform v1.2, the
	 *             preferred way to save a properties list is via the <code>store(OutputStream out,
	 * String header)</code> method.
	 * 
	 * @param out an output stream.
	 * @param header a description of the property list.
	 * @exception ClassCastException if this <code>Properties</code> object contains any keys or values that are not <code>Strings</code>.
	 */
	@Deprecated
	public synchronized void save(OutputStream out, String header)
	{
		try
		{
			store(new OutputStreamWriter(out, "8859_1"), header);
		}
		catch (IOException e)
		{
		}
	}

	/**
	 * Writes this property list (key and element pairs) in this <code>Properties</code> table to the output stream in a format suitable for loading into a
	 * <code>Properties</code> table using the {@link #load(InputStream) load} method. The stream is written using the ISO 8859-1 character encoding.
	 * <p>
	 * Properties from the defaults table of this <code>Properties</code> table (if any) are <i>not</i> written out by this method.
	 * <p>
	 * If the header argument is not null, then an ASCII <code>#</code> character, the header string, and a line separator are first written to the output
	 * stream. Thus, the <code>header</code> can serve as an identifying comment.
	 * <p>
	 * Next, a comment line is always written, consisting of an ASCII <code>#</code> character, the current date and time (as if produced by the
	 * <code>toString</code> method of <code>Date</code> for the current time), and a line separator as generated by the Writer.
	 * <p>
	 * Then every entry in this <code>Properties</code> table is written out, one per line. For each entry the key string is written, then an ASCII
	 * <code>=</code>, then the associated element string. Each character of the key and element strings is examined to see whether it should be rendered as an
	 * escape sequence. The ASCII characters <code>\</code>, tab, form feed, newline, and carriage return are written as <code>\\</code>, <code>\t</code>,
	 * <code>\f</code> <code>\n</code>, and <code>\r</code>, respectively. Characters less than <code>&#92;u0020</code> and characters greater than
	 * <code>&#92;u007E</code> are written as <code>&#92;u</code><i>xxxx</i> for the appropriate hexadecimal value <i>xxxx</i>. For the key, all space
	 * characters are written with a preceding <code>\</code> character. For the element, leading space characters, but not embedded or trailing space
	 * characters, are written with a preceding <code>\</code> character. The key and element characters <code>#</code>, <code>!</code>, <code>=</code>, and
	 * <code>:</code> are written with a preceding backslash to ensure that they are properly loaded.
	 * <p>
	 * After the entries have been written, the output stream is flushed. The output stream remains open after this method returns.
	 * 
	 * @param out an output stream.
	 * @param header a description of the property list.
	 * @exception IOException if writing this property list to the specified output stream throws an <tt>IOException</tt>.
	 * @exception ClassCastException if this <code>Properties</code> object contains any keys or values that are not <code>Strings</code>.
	 * @exception NullPointerException if <code>out</code> is null.
	 * @since 1.2
	 */
	public synchronized void store(Writer out, String header) throws IOException
	{
		BufferedWriter awriter;
		awriter = new BufferedWriter(out);
		if (header != null) writeln(awriter, "#" + header);
		writeln(awriter, "#" + new Date().toString());
		Iterator it = entrySet().iterator();
		while (it.hasNext())
		{
			Map.Entry entry = (Map.Entry)it.next();
			String key = (String)entry.getKey();
			String val = (String)entry.getValue();
			key = saveConvert(key, true);

			/*
			 * No need to escape embedded and trailing spaces for value, hence pass false to flag.
			 */
			val = saveConvert(val, false);
			writeln(awriter, key + "=" + val);
		}
		awriter.flush();
	}

	private static void writeln(BufferedWriter bw, String s) throws IOException
	{
		bw.write(s);
		bw.newLine();
	}

	/**
	 * Convert a nibble to a hex character
	 * 
	 * @param nibble the nibble to convert.
	 */
	private static char toHex(int nibble)
	{
		return hexDigit[(nibble & 0xF)];
	}

	/** A table of hex digits */
	private static final char[] hexDigit = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
}
