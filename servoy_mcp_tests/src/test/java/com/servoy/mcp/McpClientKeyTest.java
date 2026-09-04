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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * The key a pooled client is filed under.
 *
 * <p>It is what keeps one user's client away from another user: the identity is part of the key, so
 * a client can only ever be handed back to the user it was opened for. These tests are therefore
 * about isolation, not about string formatting.</p>
 *
 * @author Servoy
 */
@Tag("unit")
public class McpClientKeyTest
{
	@Test
	@DisplayName("carries the solution, the user and the tenant")
	void allThreeSegments()
	{
		McpClientKey key = new McpClientKey("mcp_sample", "alice-uid", "acme");

		assertEquals("mcp_sample", key.getSolutionName());
		assertEquals("alice-uid", key.getUserUid());
		assertEquals("acme", key.getTenant());
		assertEquals("mcp_sample|alice-uid|acme", key.toPoolKey());
	}

	@Test
	@DisplayName("puts the solution first, so the pool can recover it")
	void solutionIsRecoverable()
	{
		// the pool creates a client from the key alone, and needs to know which solution to open
		String poolKey = new McpClientKey("mcp_sample", "alice-uid", "acme").toPoolKey();

		assertEquals("mcp_sample", McpClientKey.solutionNameFromPoolKey(poolKey));
	}

	@Test
	@DisplayName("recovers the solution even from a key with no separator")
	void malformedPoolKey()
	{
		assertEquals("mcp_sample", McpClientKey.solutionNameFromPoolKey("mcp_sample"));
	}

	@Test
	@DisplayName("treats a missing user or tenant as empty rather than as null")
	void nullsBecomeEmpty()
	{
		McpClientKey key = new McpClientKey("mcp_sample", null, null);

		assertEquals("", key.getUserUid());
		assertEquals("", key.getTenant());
		assertEquals("mcp_sample||", key.toPoolKey(), "the segments stay in place, so the solution is still first");
	}

	@Test
	@DisplayName("refuses to exist without a solution")
	void solutionIsMandatory()
	{
		assertThrows(IllegalArgumentException.class, () -> new McpClientKey(null, "alice-uid", "acme"));
	}

	@Test
	@DisplayName("is equal only to a key with the same three parts")
	void equality()
	{
		McpClientKey alice = new McpClientKey("mcp_sample", "alice-uid", "acme");

		assertEquals(alice, new McpClientKey("mcp_sample", "alice-uid", "acme"));
		assertEquals(alice.hashCode(), new McpClientKey("mcp_sample", "alice-uid", "acme").hashCode());
	}

	@Test
	@DisplayName("keeps two users apart")
	void differentUsersDifferentClients()
	{
		McpClientKey alice = new McpClientKey("mcp_sample", "alice-uid", "acme");
		McpClientKey bob = new McpClientKey("mcp_sample", "bob-uid", "acme");

		assertNotEquals(alice, bob, "sharing a client between two users would leak one user's session to the other");
		assertNotEquals(alice.toPoolKey(), bob.toPoolKey());
	}

	@Test
	@DisplayName("keeps one user's two tenants apart")
	void sameUserDifferentTenants()
	{
		McpClientKey acme = new McpClientKey("mcp_sample", "alice-uid", "acme");
		McpClientKey globex = new McpClientKey("mcp_sample", "alice-uid", "globex");

		assertNotEquals(acme, globex, "the tenant filters the data, so it cannot be left out of the key");
	}

	@Test
	@DisplayName("keeps two solutions apart")
	void differentSolutions()
	{
		assertNotEquals(new McpClientKey("mcp_sample", "alice-uid", "acme"),
			new McpClientKey("other_solution", "alice-uid", "acme"));
	}

	@Test
	@DisplayName("is not equal to something that merely looks like it")
	void notEqualToOtherTypes()
	{
		McpClientKey key = new McpClientKey("mcp_sample", "alice-uid", "acme");

		assertNotEquals(key, key.toPoolKey());
		assertNotEquals(key, null);
	}
}
