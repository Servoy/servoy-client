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

public class StringInCodeSerializer
{

	private static final char[] escapedCharactersMemory = new char[] { '\n', '\r', '\t', '\\', '\'' };

	private static final String[] escapedCharactersSource = new String[] { "\\n", "\\r", "\\t", "\\\\", "\\'" };

	public static String getSourceCodeString(String memoryString, String quotationMark)
	{
		StringBuffer result = new StringBuffer(quotationMark);
		char c;
		boolean normalChar;
		for (int i = 0; i < memoryString.length(); i++)
		{
			c = memoryString.charAt(i);
			normalChar = true;
			for (int j = 0; j < escapedCharactersMemory.length; j++)
			{
				if (c == escapedCharactersMemory[j])
				{
					result.append(escapedCharactersSource[j]);
					normalChar = false;
					break;
				}
			}
			if (normalChar)
			{
				result.append(c);
			}
		}
		result.append(quotationMark);
		return result.toString();
	}

	/**
	 * Use Utils.parseJSExpression to get an accurate JS evaluated value. This method is created to be used together with getSourceCodeString() - so as to
	 * escape/unescape the exact same chars.
	 * 
	 * @param sourceCodeString the source formatted string (with quotation marks).
	 * @return the un-escaped String.
	 */
	public static String getMemoryString(String sourceCodeString)
	{
		StringBuffer result = new StringBuffer();
		boolean normalChar;
		for (int i = 1; i < sourceCodeString.length() - 1; i++)
		{
			normalChar = true;
			for (int j = 0; j < escapedCharactersSource.length; j++)
			{
				if (escapedCharactersSource[j].regionMatches(0, sourceCodeString, i, escapedCharactersSource[j].length()))
				{
					result.append(escapedCharactersMemory[j]);
					normalChar = false;
					i += escapedCharactersSource[j].length() - 1;
					break;
				}
			}
			if (normalChar)
			{
				result.append(sourceCodeString.charAt(i));
			}
		}
		return result.toString();
	}

}
