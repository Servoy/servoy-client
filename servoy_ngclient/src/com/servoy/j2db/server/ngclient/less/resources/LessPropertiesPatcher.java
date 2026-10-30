/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2026 Servoy BV

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

package com.servoy.j2db.server.ngclient.less.resources;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Applies property edits made in the Servoy Theme Properties editor to the text of a solution properties .less file in place: only the
 * lines of the edited properties change, everything else (variables the selected theme version does not know, comments, order, mixins,
 * rules) is kept byte for byte.
 *
 * @since 2026.12
 */
@SuppressWarnings("nls")
public class LessPropertiesPatcher
{
	/**
	 * One edited property.
	 *
	 * @param name the variable name without @
	 * @param value the new value, or null to remove the declaration (the property is back at the theme default)
	 * @param defaultValue the theme default, written as a "// default:" comment on a new line; may be null
	 * @param category the theme editor category, used as "START <category>" header when the declaration is added
	 */
	public static record Edit(String name, String value, String defaultValue, String category)
	{
	}

	private static final String END_MARKER = "// END own custom properties";

	private LessPropertiesPatcher()
	{
	}

	/**
	 * @return the text with the edits applied
	 */
	public static String apply(String text, List<Edit> edits)
	{
		String result = text == null ? "" : text;
		for (Edit edit : edits)
		{
			result = apply(result, edit);
		}
		return result;
	}

	private static String apply(String text, Edit edit)
	{
		// the last declaration on a line of its own is the one LESS uses; nested declarations (inside a rule or mixin) are not touched
		Matcher m = Pattern.compile("(?m)^([ \\t]*@" + Pattern.quote(edit.name()) + "[ \\t]*:[ \\t]*)([^;\\r\\n]*)(;[^\\r\\n]*)(\\r?\\n|$)").matcher(text);
		int start = -1, end = -1, valueStart = -1, valueEnd = -1;
		while (m.find())
		{
			if (insideBlock(text, m.start())) continue;
			start = m.start();
			end = m.end();
			valueStart = m.start(2);
			valueEnd = m.end(2);
		}
		if (start >= 0)
		{
			if (edit.value() == null)
			{
				return removeLine(text, start, end);
			}
			return text.substring(0, valueStart) + edit.value() + text.substring(valueEnd);
		}
		if (edit.value() == null) return text;
		return add(text, edit);
	}

	private static String removeLine(String text, int start, int end)
	{
		String before = text.substring(0, start);
		String after = text.substring(end);
		// the editor separates declarations with an empty line; drop it together with the declaration
		if (before.endsWith("\r\n\r\n") || before.endsWith("\n\n"))
		{
			before = before.substring(0, before.length() - (before.endsWith("\r\n") ? 2 : 1));
		}
		return before + after;
	}

	private static String add(String text, Edit edit)
	{
		String nl = text.contains("\r\n") ? "\r\n" : "\n";
		String declaration = "@" + edit.name() + ": " + edit.value() + ";" + (edit.defaultValue() != null ? " // default:" + edit.defaultValue() : "");
		String header = edit.category() != null ? "/* START " + edit.category() + " */" : null;
		if (header != null)
		{
			int h = text.indexOf(header);
			if (h >= 0)
			{
				int insertAt = endOfCategory(text, h + header.length());
				return text.substring(0, insertAt) + nl + nl + declaration + text.substring(insertAt);
			}
		}
		String block = (header != null ? header + nl + nl : "") + declaration;
		int endMarker = text.indexOf(END_MARKER);
		if (endMarker >= 0)
		{
			return text.substring(0, endMarker) + block + nl + nl + text.substring(endMarker);
		}
		String trimmed = text.replaceAll("(\\r?\\n)+$", "");
		return trimmed + nl + nl + block + (text.endsWith("\n") ? nl : "");
	}

	/** the end of the last declaration line of the category that starts after index from */
	private static int endOfCategory(String text, int from)
	{
		int next = text.indexOf("/* START ", from);
		int endMarker = text.indexOf(END_MARKER, from);
		int limit = text.length();
		if (next >= 0) limit = next;
		if (endMarker >= 0 && endMarker < limit) limit = endMarker;
		String section = text.substring(from, limit);
		Matcher m = Pattern.compile("(?m)^[ \\t]*@[\\w-]+[ \\t]*:[^\\r\\n]*").matcher(section);
		int last = -1;
		while (m.find())
			last = m.end();
		return last >= 0 ? from + last : from;
	}

	/** true when index lies inside a { } block, ignoring braces in comments and strings */
	private static boolean insideBlock(String text, int index)
	{
		int depth = 0;
		boolean inLine = false, inBlock = false;
		char quote = 0;
		for (int i = 0; i < index; i++)
		{
			char c = text.charAt(i);
			char n = i + 1 < index ? text.charAt(i + 1) : 0;
			if (inLine)
			{
				if (c == '\n') inLine = false;
			}
			else if (inBlock)
			{
				if (c == '*' && n == '/')
				{
					inBlock = false;
					i++;
				}
			}
			else if (quote != 0)
			{
				if (c == quote) quote = 0;
			}
			else if (c == '/' && n == '/') inLine = true;
			else if (c == '/' && n == '*') inBlock = true;
			else if (c == '"' || c == '\'') quote = c;
			else if (c == '{') depth++;
			else if (c == '}') depth = Math.max(0, depth - 1);
		}
		return depth > 0;
	}
}
