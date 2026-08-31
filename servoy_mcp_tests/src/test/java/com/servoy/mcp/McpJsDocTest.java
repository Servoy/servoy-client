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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * What turns a scope function into a tool, and what the agent is told about it.
 *
 * @author Servoy
 */
@Tag("unit")
public class McpJsDocTest
{
	@Nested
	@DisplayName("the marker")
	class Marker
	{
		@ParameterizedTest
		@ValueSource(strings = { "@Tool", "@tool", "@TOOL", "@ToOl" })
		@DisplayName("is recognised whatever its case")
		void anyCase(String marker)
		{
			assertTrue(McpJsDoc.isTool("/**\n * does something\n * " + marker + "\n */\nfunction f() {}"));
		}

		@Test
		@DisplayName("is not a longer word that merely starts with it")
		void notAPrefixOfAnotherTag()
		{
			assertFalse(McpJsDoc.isTool("/**\n * @Toolbar something\n */\nfunction f() {}"),
				"@Toolbar is a different tag and must not publish the function");
		}

		@Test
		@DisplayName("is read from the documentation only, never from the body")
		void notFromTheBody()
		{
			// the point of this: a function that talks about tools is not a tool
			assertFalse(McpJsDoc.isTool("/**\n * plain\n */\nfunction f() { return '@Tool'; }"));
		}

		@Test
		@DisplayName("is absent when there is no documentation at all")
		void noDocumentation()
		{
			assertFalse(McpJsDoc.isTool("function f() {}"));
			assertFalse(McpJsDoc.isTool("// @Tool\nfunction f() {}"), "a line comment is not a documentation block");
			assertFalse(McpJsDoc.isTool(null));
		}

		@Test
		@DisplayName("leaves an unmarked function unparsed")
		void unmarkedIsNotATool()
		{
			assertNull(McpJsDoc.parse("myScope", "f", "/**\n * plain\n */\nfunction f() {}"));
		}
	}

	@Nested
	@DisplayName("the description")
	class Description
	{
		@Test
		@DisplayName("is the free text before the first tag, on one line")
		void collapsedToOneLine()
		{
			String documentation = McpJsDoc.extractDocumentationBlock(
				"/**\n * Counts the rows.\n * Slow on a big table.\n *\n * @Tool\n * @return {Number}\n */\nfunction f() {}");

			assertEquals("Counts the rows. Slow on a big table.", McpJsDoc.extractDescription(documentation),
				"blank lines drop out and the rest joins with a single space");
		}

		@Test
		@DisplayName("is empty when the block opens with a tag")
		void noFreeText()
		{
			String documentation = McpJsDoc.extractDocumentationBlock("/**\n * @Tool\n */\nfunction f() {}");
			assertEquals("", McpJsDoc.extractDescription(documentation));
		}

		@Test
		@DisplayName("stops at the first tag, even one that is not ours")
		void stopsAtAnyTag()
		{
			String documentation = McpJsDoc.extractDocumentationBlock(
				"/**\n * kept\n * @deprecated dropped\n * also dropped\n * @Tool\n */\nfunction f() {}");
			assertEquals("kept", McpJsDoc.extractDescription(documentation));
		}
	}

	@Nested
	@DisplayName("the documentation block")
	class Block
	{
		@Test
		@DisplayName("keeps only the first one")
		void onlyTheLeadingBlock()
		{
			String documentation = McpJsDoc.extractDocumentationBlock("/**\n * first\n */\n/**\n * second\n */\nfunction f() {}");
			assertEquals("first", documentation.trim());
		}

		@Test
		@DisplayName("is null when the comment is never closed")
		void unterminated()
		{
			assertNull(McpJsDoc.extractDocumentationBlock("/**\n * @Tool\nfunction f() {}"));
		}
	}

	@Nested
	@DisplayName("the parameters")
	class Parameters
	{
		@Test
		@DisplayName("keep their declared order, type and description")
		void inOrder()
		{
			List<McpTool.Parameter> parameters = McpJsDoc.extractParameters(McpJsDoc.extractDocumentationBlock(
				"/**\n * @Tool\n * @param {String} first the first one\n * @param {Number} second the second one\n */\nfunction f() {}"));

			assertEquals(2, parameters.size());
			assertEquals("first", parameters.get(0).getName());
			assertEquals("String", parameters.get(0).getServoyType());
			assertEquals("the first one", parameters.get(0).getDescription());
			assertEquals("second", parameters.get(1).getName());
			assertEquals("Number", parameters.get(1).getServoyType());
		}

		@Test
		@DisplayName("are optional when the name is bracketed")
		void bracketedIsOptional()
		{
			List<McpTool.Parameter> parameters = McpJsDoc.extractParameters(McpJsDoc.extractDocumentationBlock(
				"/**\n * @Tool\n * @param {String} required one\n * @param {Number} [maxResults] at most this many\n */\nfunction f() {}"));

			assertFalse(parameters.get(0).isOptional());
			assertTrue(parameters.get(1).isOptional());
			assertEquals("maxResults", parameters.get(1).getName(), "the brackets are not part of the name");
		}

		@Test
		@DisplayName("may have no description")
		void descriptionIsOptional()
		{
			List<McpTool.Parameter> parameters = McpJsDoc.extractParameters(
				McpJsDoc.extractDocumentationBlock("/**\n * @Tool\n * @param {String} bare\n */\nfunction f() {}"));

			assertEquals(1, parameters.size());
			assertEquals("", parameters.get(0).getDescription());
		}

		@Test
		@DisplayName("ignore a @param that does not declare a type")
		void malformedIsSkipped()
		{
			List<McpTool.Parameter> parameters = McpJsDoc.extractParameters(McpJsDoc.extractDocumentationBlock(
				"/**\n * @Tool\n * @param noType\n * @param {String} good\n */\nfunction f() {}"));

			assertEquals(1, parameters.size(), "the malformed tag is skipped rather than taking the good one with it");
			assertEquals("good", parameters.get(0).getName());
		}

		@Test
		@DisplayName("are none when the tool takes none")
		void noParameters()
		{
			McpTool tool = McpJsDoc.parse("myScope", "whoAmI", "/**\n * Who am I.\n * @Tool\n * @return {String}\n */\nfunction whoAmI() {}");

			assertNotNull(tool);
			assertTrue(tool.getParameters().isEmpty());
		}
	}

	@Nested
	@DisplayName("the published name")
	class Naming
	{
		@Test
		@DisplayName("carries the scope, so two scopes may use the same function name")
		void scopeQualified()
		{
			String declaration = "/**\n * @Tool\n */\nfunction count() {}";

			assertEquals("myScope_count", McpJsDoc.parse("myScope", "count", declaration).getName());
			assertEquals("salesTools_count", McpJsDoc.parse("salesTools", "count", declaration).getName());
		}
	}

	@Nested
	@DisplayName("against the sample solution")
	class SampleSolution
	{
		/**
		 * The parser is pointed at the real thing rather than at strings written to suit it. If the
		 * sample solution changes, these numbers change with it - which is the intention: the sample
		 * is the specification of what a tool looks like.
		 */
		@Test
		@DisplayName("finds every marked function in myScope, and no unmarked one")
		void myScope() throws Exception
		{
			List<String> names = toolNamesIn("myScope");

			assertEquals(List.of("myScope_test_scope_function", "myScope_add", "myScope_echo", "myScope_whoAmI",
				"myScope_unsupportedParameterType"), names,
				"unsupportedParameterType is marked and so is found here - it is the schema that refuses it later");
		}

		@Test
		@DisplayName("finds every marked function in salesTools")
		void salesTools() throws Exception
		{
			List<String> names = toolNamesIn("salesTools");

			assertEquals(6, names.size());
			assertTrue(names.contains("salesTools_countRows"));
			assertTrue(names.contains("salesTools_listMyItems"));
		}

		@Test
		@DisplayName("reads the description and parameters of a real tool")
		void aRealTool() throws Exception
		{
			McpTool add = toolIn("myScope", "add");

			assertNotNull(add, "myScope.add is expected to be a tool in the sample solution");
			assertFalse(add.getDescription().isEmpty(), "a tool without a description tells the agent nothing");
			assertEquals(2, add.getParameters().size());
			assertEquals("Number", add.getParameters().get(0).getServoyType());
		}

		private List<String> toolNamesIn(String scopeName) throws Exception
		{
			List<String> names = new ArrayList<String>();

			for (String declaration : McpTestFixture.declarations(McpTestFixture.scopeSource(scopeName)))
			{
				String functionName = McpTestFixture.functionName(declaration);
				if (functionName == null) continue;

				McpTool tool = McpJsDoc.parse(scopeName, functionName, declaration);
				if (tool != null) names.add(tool.getName());
			}

			return names;
		}

		private McpTool toolIn(String scopeName, String functionName) throws Exception
		{
			for (String declaration : McpTestFixture.declarations(McpTestFixture.scopeSource(scopeName)))
			{
				if (functionName.equals(McpTestFixture.functionName(declaration)))
				{
					return McpJsDoc.parse(scopeName, functionName, declaration);
				}
			}

			return null;
		}
	}
}
