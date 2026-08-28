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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * How a scope function's return value is rendered for the agent.
 *
 * <p>The values here are the ones the scripting engine actually hands back, which is why every
 * number arrives as a {@link Double} - that detail is the reason this class exists at all.</p>
 *
 * @author Servoy
 */
@Tag("unit")
public class McpResultsTest
{
	@Nested
	@DisplayName("numbers")
	class Numbers
	{
		@ParameterizedTest
		@CsvSource({ "91.0, 91", "0.0, 0", "-7.0, -7", "42.0, 42" })
		@DisplayName("lose the decimal when they are whole")
		void wholeNumbersHaveNoDecimal(double returned, String expected)
		{
			// the engine hands back a Double for every number, so a count would otherwise read "91.0"
			assertEquals(expected, McpResults.describe(Double.valueOf(returned)));
		}

		@Test
		@DisplayName("keep the decimal when they have one")
		void fractionsAreKept()
		{
			assertEquals("2.5", McpResults.describe(Double.valueOf(2.5)));
		}

		@Test
		@DisplayName("come out of an Integer the same way")
		void otherNumberTypes()
		{
			assertEquals("7", McpResults.describe(Integer.valueOf(7)));
			assertEquals("7", McpResults.describe(Long.valueOf(7L)));
		}

		@Test
		@DisplayName("keep their decimal form when too large to be shortened safely")
		void veryLargeNumbers()
		{
			// beyond 1e15 a double no longer represents every whole number, so it is left as it is
			assertEquals(String.valueOf(1e16), McpResults.describe(Double.valueOf(1e16)));
		}

		@Test
		@DisplayName("survive being not a number")
		void nanAndInfinity()
		{
			assertEquals("NaN", McpResults.describe(Double.valueOf(Double.NaN)));
			assertEquals("Infinity", McpResults.describe(Double.valueOf(Double.POSITIVE_INFINITY)));
		}
	}

	@Nested
	@DisplayName("scalars")
	class Scalars
	{
		@Test
		@DisplayName("a missing value is empty text, never the word null")
		void nullIsEmpty()
		{
			assertEquals("", McpResults.describe(null));
		}

		@Test
		@DisplayName("a string is handed over untouched")
		void stringsAreNotQuoted()
		{
			assertEquals("hello", McpResults.describe("hello"),
				"quoting it would make the agent read the quotes as part of the answer");
			assertEquals("", McpResults.describe(""));
		}

		@Test
		@DisplayName("a boolean reads as true or false")
		void booleans()
		{
			assertEquals("true", McpResults.describe(Boolean.TRUE));
			assertEquals("false", McpResults.describe(Boolean.FALSE));
		}
	}

	@Nested
	@DisplayName("structures")
	class Structures
	{
		@Test
		@DisplayName("a map becomes a JSON object, in its own order")
		void maps()
		{
			Map<String, Object> row = new LinkedHashMap<String, Object>();
			row.put("tenant_name", "acme");
			row.put("item_name", "Acme anvil");

			assertEquals("{\"tenant_name\":\"acme\",\"item_name\":\"Acme anvil\"}", McpResults.describe(row));
		}

		@Test
		@DisplayName("a list becomes a JSON array")
		void lists()
		{
			assertEquals("[\"a\",\"b\"]", McpResults.describe(List.of("a", "b")));
		}

		@Test
		@DisplayName("an array becomes a JSON array too")
		void arrays()
		{
			assertEquals("[1,2,3]", McpResults.describe(new Object[] { Integer.valueOf(1), Integer.valueOf(2), Integer.valueOf(3) }));
		}

		@Test
		@DisplayName("nesting is kept")
		void nested()
		{
			Map<String, Object> outer = new LinkedHashMap<String, Object>();
			outer.put("rows", List.of(Map.of("k", "v")));

			assertEquals("{\"rows\":[{\"k\":\"v\"}]}", McpResults.describe(outer));
		}

		@Test
		@DisplayName("an empty structure is still valid JSON")
		void empties()
		{
			assertEquals("[]", McpResults.describe(List.of()));
			assertEquals("{}", McpResults.describe(Map.of()));
		}
	}

	@Nested
	@DisplayName("anything else")
	class Fallback
	{
		@Test
		@DisplayName("falls back on toString rather than failing a call that already succeeded")
		void unknownTypes()
		{
			Object odd = new Object()
			{
				@Override
				public String toString()
				{
					return "something the renderer has never seen";
				}
			};

			assertEquals("something the renderer has never seen", McpResults.describe(odd));
		}

		@Test
		@DisplayName("renders a value that cannot be serialised, instead of throwing")
		void unserialisable()
		{
			Map<String, Object> selfReferencing = new LinkedHashMap<String, Object>();
			selfReferencing.put("me", selfReferencing);

			String described = McpResults.describe(selfReferencing);

			assertTrue(described != null && described.length() > 0,
				"a tool that ran successfully must not be reported as failed because of how it printed");
		}
	}
}
