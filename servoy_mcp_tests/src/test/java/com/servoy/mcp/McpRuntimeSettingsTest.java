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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntPredicate;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.servoy.j2db.util.Settings;

/**
 * The four <code>servoy.mcpClientPool.*</code> settings: their names, their defaults, and how a
 * configured value is read.
 *
 * <p>{@link McpRuntime#readInt} takes the lookup as a function, so none of this touches the global
 * {@link Settings}. The boundary tests pass the production validity predicates
 * ({@link McpRuntime#VALID_MAX_CLIENTS_PER_KEY} and its siblings) to it, so a wrong rule in
 * production fails here; {@link LiveValues} checks the values the static initialisers resolved.</p>
 *
 * @author Servoy
 */
@Tag("unit")
@SuppressWarnings("nls")
public class McpRuntimeSettingsTest
{

	private static final String KEY = "servoy.test.setting";

	private static int read(String value, int defaultValue, IntPredicate valid)
	{
		Map<String, String> settings = new HashMap<String, String>();
		if (value != null) settings.put(KEY, value);
		return McpRuntime.readInt(settings::get, KEY, defaultValue, valid);
	}

	@Nested
	class Names
	{
		@Test
		@DisplayName("the defaults are 100, 8, unlimited and 10 seconds")
		void theDefaults()
		{
			assertEquals(100, Settings.MCP_CLIENT_POOL_MAX_CLIENTS_PER_KEY_DEFAULT);
			assertEquals(8, Settings.MCP_CLIENT_POOL_MAX_IDLE_PER_KEY_DEFAULT);
			assertEquals(-1, Settings.MCP_CLIENT_POOL_MAX_TOTAL_CLIENTS_DEFAULT);
			assertEquals(10, Settings.MCP_CLIENT_POOL_WAIT_TIMEOUT_DEFAULT);
		}

		@Test
		@DisplayName("the property names are the documented ones")
		void thePropertyNames()
		{
			assertEquals("servoy.mcpClientPool.maxClientsPerSolUsrTenant", Settings.MCP_CLIENT_POOL_MAX_CLIENTS_PER_KEY);
			assertEquals("servoy.mcpClientPool.maxIdleClientsPerSolUsrTenant", Settings.MCP_CLIENT_POOL_MAX_IDLE_PER_KEY);
			assertEquals("servoy.mcpClientPool.maxTotalClients", Settings.MCP_CLIENT_POOL_MAX_TOTAL_CLIENTS);
			assertEquals("servoy.mcpClientPool.waitForAvailableClientTimeout", Settings.MCP_CLIENT_POOL_WAIT_TIMEOUT);
		}
	}

	@Nested
	class LiveValues
	{
		// a developer's own servoy.properties would otherwise turn these into false failures

		@Test
		@DisplayName("the per-key limit is the default when the property is not set")
		void perKey()
		{
			Assumptions.assumeTrue(Settings.getInstance().getProperty(Settings.MCP_CLIENT_POOL_MAX_CLIENTS_PER_KEY) == null);
			assertEquals(100, McpRuntime.MAX_CLIENTS_PER_KEY);
		}

		@Test
		@DisplayName("the idle limit is the default when the property is not set")
		void idle()
		{
			Assumptions.assumeTrue(Settings.getInstance().getProperty(Settings.MCP_CLIENT_POOL_MAX_IDLE_PER_KEY) == null);
			assertEquals(8, McpRuntime.MAX_IDLE_PER_KEY);
		}

		@Test
		@DisplayName("the overall max is unlimited when the property is not set")
		void total()
		{
			Assumptions.assumeTrue(Settings.getInstance().getProperty(Settings.MCP_CLIENT_POOL_MAX_TOTAL_CLIENTS) == null);
			assertEquals(-1, McpRuntime.TOTAL_MAX_CLIENTS);
		}

		@Test
		@DisplayName("the wait timeout is ten seconds when the property is not set")
		void timeout()
		{
			Assumptions.assumeTrue(Settings.getInstance().getProperty(Settings.MCP_CLIENT_POOL_WAIT_TIMEOUT) == null);
			assertEquals(10, McpRuntime.WAIT_FOR_AVAILABLE_CLIENT_TIMEOUT);
		}
	}

	@Nested
	class ReadInt
	{
		@ParameterizedTest
		@NullAndEmptySource
		@ValueSource(strings = { " ", "   ", "null", "NULL", "Null", " null " })
		@DisplayName("a missing, blank or null value is not set: the default, and nothing logged")
		void notSet(String value)
		{
			try (McpLogCapture log = McpLogCapture.of("servoy.mcp"))
			{
				assertEquals(42, read(value, 42, McpRuntime.VALID_MAX_CLIENTS_PER_KEY));
				assertTrue(log.lines().isEmpty(), "nothing to complain about: " + log.lines());
			}
		}

		@ParameterizedTest
		@CsvSource({ "25, 25", "'25', 25", "' 25 ', 25", "'\t7\t', 7" })
		@DisplayName("a number is read, surrounding blanks are ignored")
		void aNumber(String value, int expected)
		{
			try (McpLogCapture log = McpLogCapture.of("servoy.mcp"))
			{
				assertEquals(expected, read(value, 42, McpRuntime.VALID_MAX_CLIENTS_PER_KEY));
				assertTrue(log.lines().isEmpty(), log.lines().toString());
			}
		}

		@ParameterizedTest
		@ValueSource(strings = { "abc", "12x", "1.5", "99999999999" })
		@DisplayName("something that is not an integer gives the default and one warning")
		void notANumber(String value)
		{
			try (McpLogCapture log = McpLogCapture.of("servoy.mcp"))
			{
				assertEquals(42, read(value, 42, McpRuntime.VALID_MAX_CLIENTS_PER_KEY));

				assertEquals(1, log.lines().size(), "exactly one line: " + log.lines());
				assertTrue(log.lines().get(0).startsWith("WARN "), log.lines().toString());
				assertTrue(log.contains("'" + value + "'"), "naming the bad value: " + log.lines());
				assertTrue(log.contains(KEY), "and the property: " + log.lines());
				assertTrue(log.contains("using 42"), "and what is used instead: " + log.lines());
			}
		}

		@Test
		@DisplayName("a value the property refuses gives the default and one warning")
		void refusedValue()
		{
			try (McpLogCapture log = McpLogCapture.of("servoy.mcp"))
			{
				assertEquals(100, read("0", 100, McpRuntime.VALID_MAX_CLIENTS_PER_KEY));
				assertEquals(1, log.lines().size(), log.lines().toString());
			}
		}

		@ParameterizedTest
		@CsvSource({
			"0, 100, false", "-1, 100, false", "-5, 100, false", "1, 1, true", "100, 100, true" })
		@DisplayName("the per-key limit must be above zero")
		void perKeyLimit(String value, int expected, boolean accepted)
		{
			assertBoundary(value, 100, McpRuntime.VALID_MAX_CLIENTS_PER_KEY, expected, accepted);
		}

		@ParameterizedTest
		@CsvSource({
			"0, 0, true", "8, 8, true", "-1, 8, false", "-5, 8, false" })
		@DisplayName("the idle limit may be zero but not negative")
		void idleLimit(String value, int expected, boolean accepted)
		{
			assertBoundary(value, 8, McpRuntime.VALID_MAX_IDLE_PER_KEY, expected, accepted);
		}

		@ParameterizedTest
		@CsvSource({
			"-1, -1, true", "3, 3, true", "0, -1, false", "-2, -1, false", "-5, -1, false" })
		@DisplayName("the overall max is unlimited or above zero, and zero falls back to unlimited")
		void overallMax(String value, int expected, boolean accepted)
		{
			assertBoundary(value, -1, McpRuntime.VALID_TOTAL_MAX_CLIENTS, expected, accepted);
		}

		@ParameterizedTest
		@CsvSource({
			"0, 10, false", "-1, 10, false", "-30, 10, false", "1, 1, true", "45, 45, true" })
		@DisplayName("the wait timeout must be above zero")
		void waitTimeout(String value, int expected, boolean accepted)
		{
			assertBoundary(value, 10, McpRuntime.VALID_WAIT_TIMEOUT, expected, accepted);
		}

		private void assertBoundary(String value, int defaultValue, IntPredicate valid, int expected, boolean accepted)
		{
			try (McpLogCapture log = McpLogCapture.of("servoy.mcp"))
			{
				assertEquals(expected, read(value, defaultValue, valid), "for '" + value + "'");

				if (accepted) assertTrue(log.lines().isEmpty(), "a valid value is not warned about: " + log.lines());
				else assertEquals(1, log.lines().size(), "a refused value is warned about once: " + log.lines());
			}
		}

		@Test
		@DisplayName("the lookup is asked for the property it was given")
		void theKeyIsPassedOn()
		{
			Map<String, String> settings = new HashMap<String, String>();
			settings.put("a", "5");
			settings.put("b", "6");

			assertEquals(6, McpRuntime.readInt(settings::get, "b", 1, McpRuntime.VALID_MAX_CLIENTS_PER_KEY));
			assertFalse(settings.containsKey("c"));
			assertEquals(1, McpRuntime.readInt(settings::get, "c", 1, McpRuntime.VALID_MAX_CLIENTS_PER_KEY));
		}
	}
}
