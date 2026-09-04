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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The JSON Schema an agent is given for a tool's arguments.
 *
 * @author Servoy
 */
@Tag("unit")
public class McpToolSchemaTest
{
	private static McpTool tool(McpTool.Parameter... parameters)
	{
		return new McpTool("myScope", "aTool", "does something", List.of(parameters));
	}

	private static McpTool.Parameter parameter(String name, String type)
	{
		return new McpTool.Parameter(name, type, "", false);
	}

	@Nested
	@DisplayName("the supported types")
	class SupportedTypes
	{
		@ParameterizedTest
		@CsvSource({ "String, string", "string, string", "STRING, string", "Number, number", "number, number" })
		@DisplayName("map onto a JSON Schema type, whatever their case")
		void mapped(String declared, String expected) throws Exception
		{
			assertEquals(expected, McpToolSchema.toJsonType(tool(), parameter("p", declared)));
		}

		@ParameterizedTest
		@ValueSource(strings = { "JSON", "json", "Object", "object" })
		@DisplayName("leave JSON unconstrained, since anything may arrive")
		void jsonHasNoType(String declared) throws Exception
		{
			assertNull(McpToolSchema.toJsonType(tool(), parameter("p", declared)),
				"no type at all is what lets an object, an array or a scalar through");
		}

		@Test
		@DisplayName("tolerate padding around the type")
		void trimmed() throws Exception
		{
			assertEquals("string", McpToolSchema.toJsonType(tool(), parameter("p", "  String  ")));
		}
	}

	@Nested
	@DisplayName("an unsupported type")
	class UnsupportedTypes
	{
		@ParameterizedTest
		@ValueSource(strings = { "Date", "Boolean", "JSFoundSet", "Array", "" })
		@DisplayName("is refused, whatever it is")
		void refused(String declared)
		{
			assertThrows(McpToolSchema.UnsupportedTypeException.class,
				() -> McpToolSchema.toJsonType(tool(), parameter("when", declared)));
		}

		@Test
		@DisplayName("is refused when no type is declared at all")
		void nullType()
		{
			assertThrows(McpToolSchema.UnsupportedTypeException.class,
				() -> McpToolSchema.toJsonType(tool(), parameter("p", null)));
		}

		@Test
		@DisplayName("says which function and which parameter, so the log is actionable")
		void namesTheOffender()
		{
			McpToolSchema.UnsupportedTypeException thrown = assertThrows(McpToolSchema.UnsupportedTypeException.class,
				() -> McpToolSchema.buildInputSchema(tool(parameter("when", "Date"))));

			String message = thrown.getMessage();
			assertTrue(message.contains("scopes.myScope.aTool"), message);
			assertTrue(message.contains("'when'"), message);
			assertTrue(message.contains("'Date'"), message);
			assertTrue(message.contains("String, Number or JSON"), message);
		}

		@Test
		@DisplayName("takes the whole tool down, not just the parameter")
		void wholeToolIsRefused()
		{
			// half a signature would be worse than none: the agent would call it and fail
			assertThrows(McpToolSchema.UnsupportedTypeException.class,
				() -> McpToolSchema.buildInputSchema(tool(parameter("fine", "String"), parameter("when", "Date"))));
		}
	}

	@Nested
	@DisplayName("the schema")
	class Schema
	{
		@Test
		@DisplayName("describes a tool that takes nothing")
		void noParameters() throws Exception
		{
			assertEquals("{\"type\":\"object\",\"properties\":{}}", McpToolSchema.buildInputSchema(tool()),
				"no required array at all, rather than an empty one");
		}

		@Test
		@DisplayName("names every parameter and marks the mandatory ones")
		void required() throws Exception
		{
			McpTool withOptional = new McpTool("myScope", "aTool", "d", List.of(
				new McpTool.Parameter("nameContains", "String", "part of a name", false),
				new McpTool.Parameter("maxResults", "Number", "at most this many", true)));

			assertEquals("{\"type\":\"object\",\"properties\":{" +
				"\"nameContains\":{\"type\":\"string\",\"description\":\"part of a name\"}," +
				"\"maxResults\":{\"type\":\"number\",\"description\":\"at most this many\"}}," +
				"\"required\":[\"nameContains\"]}", McpToolSchema.buildInputSchema(withOptional));
		}

		@Test
		@DisplayName("leaves out the type for a JSON parameter but keeps its description")
		void jsonParameter() throws Exception
		{
			String schema = McpToolSchema.buildInputSchema(
				tool(new McpTool.Parameter("payload", "JSON", "any JSON value", false)));

			assertTrue(schema.contains("\"payload\":{\"description\":\"any JSON value\"}"), schema);
			assertFalse(schema.contains("\"type\":\"json\""), schema);
		}

		@Test
		@DisplayName("has no required array when everything is optional")
		void allOptional() throws Exception
		{
			String schema = McpToolSchema.buildInputSchema(
				tool(new McpTool.Parameter("maxResults", "Number", "", true)));

			assertFalse(schema.contains("required"), schema);
		}
	}

	@Nested
	@DisplayName("quoting")
	class Quoting
	{
		@Test
		@DisplayName("escapes what would otherwise break the JSON")
		void escapes()
		{
			assertEquals("\"a \\\"quoted\\\" word\"", McpToolSchema.quote("a \"quoted\" word"));
			assertEquals("\"back\\\\slash\"", McpToolSchema.quote("back\\slash"));
			assertEquals("\"two\\nlines\"", McpToolSchema.quote("two\nlines"));
			assertEquals("\"a\\tb\"", McpToolSchema.quote("a\tb"));
		}

		@Test
		@DisplayName("escapes control characters as \\u")
		void controlCharacters()
		{
			assertEquals("\"a\\u0000b\"", McpToolSchema.quote("a\0b"));
		}

		@Test
		@DisplayName("turns a missing value into an empty string, not into null")
		void nullBecomesEmpty()
		{
			assertEquals("\"\"", McpToolSchema.quote(null));
		}

		@Test
		@DisplayName("leaves ordinary text alone")
		void plainText()
		{
			assertEquals("\"maxResults\"", McpToolSchema.quote("maxResults"));
		}

		@Test
		@DisplayName("survives a description written by someone who used quotes")
		void quotesInADescriptionDoNotBreakTheSchema() throws Exception
		{
			String schema = McpToolSchema.buildInputSchema(
				tool(new McpTool.Parameter("q", "String", "use \"quotes\" freely", false)));

			assertTrue(schema.contains("\\\"quotes\\\""), schema);
		}
	}
}
