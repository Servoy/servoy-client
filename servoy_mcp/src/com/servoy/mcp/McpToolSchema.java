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

/**
 * Turns tool metadata into the JSON Schema published to the agent.
 *
 * <p><b>A tool takes String, Number or JSON. Nothing else.</b> That is the agreed contract, and it
 * is enforced rather than accommodated: a parameter declared as anything else makes the whole tool
 * unpublishable, and {@link McpToolRegistry} leaves it out and says why. Flattening an unsupported
 * type into a string would produce a tool that looks callable and misbehaves, which is worse than a
 * tool that is visibly absent.</p>
 *
 * <table border="1">
 * <caption>documented type to JSON Schema</caption>
 * <tr><th>declared</th><th>schema</th></tr>
 * <tr><td><code>String</code></td><td><code>"type":"string"</code></td></tr>
 * <tr><td><code>Number</code></td><td><code>"type":"number"</code></td></tr>
 * <tr><td><code>JSON</code>, <code>Object</code></td><td>no type constraint - any JSON value</td></tr>
 * </table>
 *
 * <p>JSON deliberately carries no <code>type</code>. Constraining it to <code>object</code> would
 * make a tool that wants an array fail validation in the client before the call is even made.</p>
 *
 * <p>There is no <code>userToken</code> parameter. The token arrives in the
 * <code>Authorization: Bearer</code> header, so it belongs to the connection rather than to every
 * individual tool, and stays out of the schemas the agent sees - see {@link McpIdentity}.</p>
 *
 * @author Servoy
 */
@SuppressWarnings("nls")
public class McpToolSchema
{
	private McpToolSchema()
	{
	}

	/**
	 * Builds the JSON Schema describing the arguments of a tool.
	 *
	 * @param tool the tool to describe
	 * @return a JSON object as text, ready to publish
	 * @throws UnsupportedTypeException when a parameter is declared as something other than String,
	 *         Number or JSON
	 */
	public static String buildInputSchema(McpTool tool) throws UnsupportedTypeException
	{
		StringBuilder properties = new StringBuilder();
		List<String> required = new ArrayList<String>();

		for (McpTool.Parameter parameter : tool.getParameters())
		{
			if (properties.length() > 0) properties.append(",");
			properties.append(describeParameter(tool, parameter));
			if (!parameter.isOptional()) required.add(parameter.getName());
		}

		StringBuilder schema = new StringBuilder();
		schema.append("{\"type\":\"object\",\"properties\":{").append(properties).append("}");

		if (!required.isEmpty())
		{
			schema.append(",\"required\":[");
			for (int i = 0; i < required.size(); i++)
			{
				if (i > 0) schema.append(",");
				schema.append(quote(required.get(i)));
			}
			schema.append("]");
		}

		schema.append("}");
		return schema.toString();
	}

	/**
	 * One entry of the <code>properties</code> object.
	 */
	private static String describeParameter(McpTool tool, McpTool.Parameter parameter) throws UnsupportedTypeException
	{
		StringBuilder property = new StringBuilder();
		property.append(quote(parameter.getName())).append(":{");

		String jsonType = toJsonType(tool, parameter);
		if (jsonType != null)
		{
			property.append("\"type\":").append(quote(jsonType)).append(",");
		}

		property.append("\"description\":").append(quote(parameter.getDescription()));
		property.append("}");

		return property.toString();
	}

	/**
	 * Maps a documented type onto a JSON Schema type.
	 *
	 * @return the schema type, or <code>null</code> for JSON, which is left unconstrained
	 * @throws UnsupportedTypeException for anything outside the three supported types
	 */
	static String toJsonType(McpTool tool, McpTool.Parameter parameter) throws UnsupportedTypeException
	{
		String declared = parameter.getServoyType();
		String type = declared == null ? "" : declared.trim();

		if ("string".equalsIgnoreCase(type)) return "string";
		if ("number".equalsIgnoreCase(type)) return "number";
		if ("json".equalsIgnoreCase(type) || "object".equalsIgnoreCase(type)) return null;

		throw new UnsupportedTypeException(tool, parameter);
	}

	/**
	 * Minimal JSON string quoting. The values quoted here are names and text taken from
	 * documentation blocks, so only the mandatory escapes are needed.
	 */
	static String quote(String value)
	{
		if (value == null) return "\"\"";

		StringBuilder quoted = new StringBuilder(value.length() + 2);
		quoted.append('"');
		for (int i = 0; i < value.length(); i++)
		{
			char c = value.charAt(i);
			switch (c)
			{
				case '"' :
					quoted.append("\\\"");
					break;
				case '\\' :
					quoted.append("\\\\");
					break;
				case '\n' :
					quoted.append("\\n");
					break;
				case '\r' :
					quoted.append("\\r");
					break;
				case '\t' :
					quoted.append("\\t");
					break;
				default :
					if (c < 0x20)
					{
						quoted.append(String.format("\\u%04x", Integer.valueOf(c)));
					}
					else
					{
						quoted.append(c);
					}
			}
		}
		quoted.append('"');
		return quoted.toString();
	}

	/**
	 * Raised when a tool declares a parameter type that cannot be published.
	 */
	public static class UnsupportedTypeException extends Exception
	{
		public UnsupportedTypeException(McpTool tool, McpTool.Parameter parameter)
		{
			super("scopes." + tool.getScopeName() + "." + tool.getFunctionName() + " declares parameter '" +
				parameter.getName() + "' as '" + parameter.getServoyType() +
				"'; a tool parameter must be String, Number or JSON");
		}
	}
}
