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
package com.servoy.mcp;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the documentation block of a scope function and turns it into tool metadata.
 *
 * <p>The input is the raw text returned by <code>ScriptMethod.getDeclaration()</code>, which starts
 * at the documentation block and runs through the whole function. Nothing here uses DLTK: DLTK is a
 * design time facility and is not available at runtime on the application server. Plain string
 * handling is also the established precedent - <code>ScriptEngine</code> reads
 * <code>&#64;AllowToRunInFind</code> from the same raw declaration with a simple search.</p>
 *
 * <p>The format understood here is a normal documentation block carrying an extra marker:</p>
 *
 * <pre>
 * &#47;**
 *  * Looks up a customer by its number.
 *  *
 *  * &#64;Tool
 *  *
 *  * &#64;param {String} customerNumber the number to look up
 *  * &#64;param {Boolean} [includeOrders] also return the orders
 *  *&#47;
 * function getCustomer(customerNumber, includeOrders) { ... }
 * </pre>
 *
 * <p><b>Assumptions made for the MVP</b>, all of them open for review:</p>
 * <ul>
 * <li><code>&#64;Tool</code> is a bare marker. It carries no name and no description of its own.</li>
 * <li>The tool description is the free text before the first tag.</li>
 * <li>Parameters come from <code>&#64;param</code> tags, in declaration order.</li>
 * <li>A parameter whose name is written in brackets is optional.</li>
 * <li>The marker is matched case insensitively, so <code>&#64;tool</code> works too - the case in
 * Jira is written both ways.</li>
 * </ul>
 *
 * @author Servoy
 */
public class McpJsDoc
{
	/** The marker that turns a scope function into a published tool. */
	private static final String TOOL_MARKER = "@tool"; //$NON-NLS-1$

	/** &#64;param {Type} name description - with the name optionally in brackets. */
	private static final Pattern PARAM_PATTERN = Pattern.compile(
		"@param\\s+\\{([^}]*)\\}\\s*(\\[?)([A-Za-z_$][A-Za-z0-9_$]*)\\]?\\s*(.*)"); //$NON-NLS-1$

	private McpJsDoc()
	{
	}

	/**
	 * Tests whether a declaration carries the tool marker.
	 *
	 * <p>Only the documentation block is searched, so a function that merely mentions
	 * <code>&#64;Tool</code> inside its body or in a string literal is not picked up.</p>
	 *
	 * @param declaration the raw declaration, may be <code>null</code>
	 */
	public static boolean isTool(String declaration)
	{
		String documentation = extractDocumentationBlock(declaration);
		if (documentation == null) return false;

		return containsMarker(documentation);
	}

	/**
	 * Builds the tool metadata for a scope function.
	 *
	 * @param scopeName the scope the function lives in
	 * @param functionName the function name
	 * @param declaration the raw declaration
	 * @return the tool, or <code>null</code> when the declaration carries no tool marker
	 */
	public static McpTool parse(String scopeName, String functionName, String declaration)
	{
		String documentation = extractDocumentationBlock(declaration);
		if (documentation == null || !containsMarker(documentation)) return null;

		return new McpTool(scopeName, functionName, extractDescription(documentation), extractParameters(documentation));
	}

	/**
	 * Returns the content of the leading documentation block, with the comment framing removed, or
	 * <code>null</code> when the declaration has no documentation block.
	 */
	static String extractDocumentationBlock(String declaration)
	{
		if (declaration == null) return null;

		int start = declaration.indexOf("/**"); //$NON-NLS-1$
		if (start < 0) return null;

		int end = declaration.indexOf("*/", start); //$NON-NLS-1$
		if (end < 0) return null;

		String[] lines = declaration.substring(start + 3, end).split("\n"); //$NON-NLS-1$
		StringBuilder cleaned = new StringBuilder();
		for (String line : lines)
		{
			String trimmed = line.trim();
			// drop the leading star of a documentation line, but keep a line that merely starts with one
			if (trimmed.startsWith("*")) trimmed = trimmed.substring(1).trim(); //$NON-NLS-1$
			cleaned.append(trimmed).append('\n');
		}
		return cleaned.toString();
	}

	/**
	 * True when the documentation contains the tool marker as a tag of its own, rather than as part
	 * of a longer word such as <code>&#64;Toolbar</code>.
	 */
	private static boolean containsMarker(String documentation)
	{
		String lower = documentation.toLowerCase();
		int index = lower.indexOf(TOOL_MARKER);
		while (index >= 0)
		{
			int after = index + TOOL_MARKER.length();
			boolean endsHere = after >= lower.length() || !Character.isLetterOrDigit(lower.charAt(after));
			if (endsHere) return true;
			index = lower.indexOf(TOOL_MARKER, after);
		}
		return false;
	}

	/**
	 * The free text before the first tag, collapsed into a single line.
	 */
	static String extractDescription(String documentation)
	{
		StringBuilder description = new StringBuilder();
		for (String line : documentation.split("\n")) //$NON-NLS-1$
		{
			String trimmed = line.trim();
			if (trimmed.startsWith("@")) break; //$NON-NLS-1$
			if (trimmed.length() == 0) continue;
			if (description.length() > 0) description.append(' ');
			description.append(trimmed);
		}
		return description.toString();
	}

	/**
	 * The declared parameters, in the order they appear.
	 */
	static List<McpTool.Parameter> extractParameters(String documentation)
	{
		List<McpTool.Parameter> parameters = new ArrayList<McpTool.Parameter>();

		for (String line : documentation.split("\n")) //$NON-NLS-1$
		{
			String trimmed = line.trim();
			if (!trimmed.startsWith("@param")) continue; //$NON-NLS-1$

			Matcher matcher = PARAM_PATTERN.matcher(trimmed);
			if (!matcher.find()) continue;

			String type = matcher.group(1).trim();
			boolean optional = matcher.group(2).length() > 0;
			String name = matcher.group(3);
			String description = matcher.group(4).trim();

			parameters.add(new McpTool.Parameter(name, type, description, optional));
		}

		return parameters;
	}
}
