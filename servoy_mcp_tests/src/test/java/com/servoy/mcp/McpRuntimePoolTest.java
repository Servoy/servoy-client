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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.servoy.j2db.server.shared.IHeadlessClient;

/**
 * The client pool, and what invalidating a solution does to it.
 *
 * <p>This is where one agent's session is kept away from another's, so it is worth asserting rather
 * than assuming. The clients here are stand-ins: what matters is which of them is handed back for a
 * given key, not what a real one would do with a tool.</p>
 *
 * @author Servoy
 */
@Tag("integration")
public class McpRuntimePoolTest
{
	private static final String SOLUTION = "mcp_sample";

	/**
	 * A runtime whose clients are freshly minted stand-ins, and a record of what it asked for.
	 */
	private static final class Harness
	{
		final List<String> opened = new ArrayList<String>();

		final McpRuntime runtime;

		Harness()
		{
			runtime = new McpRuntime(solutionName -> {
				opened.add(solutionName);
				IHeadlessClient client = mock(IHeadlessClient.class);
				when(client.isValid()).thenReturn(Boolean.TRUE);
				return client;
			});
		}

		IHeadlessClient borrow(String solution, String user, String tenant) throws Exception
		{
			return runtime.getClient(new McpClientKey(solution, user, tenant));
		}

		void giveBack(String solution, String user, String tenant, IHeadlessClient client)
		{
			runtime.releaseClient(new McpClientKey(solution, user, tenant), client);
		}
	}

	@Test
	@DisplayName("two users never share a client")
	void twoUsersGetTwoClients() throws Exception
	{
		Harness harness = new Harness();

		IHeadlessClient alice = harness.borrow(SOLUTION, "alice-uid", "acme");
		IHeadlessClient bob = harness.borrow(SOLUTION, "bob-uid", "globex");

		// the identity is part of the pool key precisely so that this cannot happen
		assertNotSame(alice, bob, "handing one user's client to another would leak the session");
		assertEquals(2, harness.opened.size());
	}

	@Test
	@DisplayName("one user in two tenants gets two clients")
	void twoTenantsGetTwoClients() throws Exception
	{
		Harness harness = new Harness();

		IHeadlessClient acme = harness.borrow(SOLUTION, "alice-uid", "acme");
		IHeadlessClient globex = harness.borrow(SOLUTION, "alice-uid", "globex");

		// the tenant filters the data, so a client set up for one is wrong for the other
		assertNotSame(acme, globex);
	}

	@Test
	@DisplayName("a returned client comes back to the same user")
	void aReturnedClientIsReused() throws Exception
	{
		Harness harness = new Harness();

		IHeadlessClient first = harness.borrow(SOLUTION, "alice-uid", "acme");
		harness.giveBack(SOLUTION, "alice-uid", "acme", first);
		IHeadlessClient second = harness.borrow(SOLUTION, "alice-uid", "acme");

		// this is the whole point of pooling: an agent making several calls does not pay for a new
		// client each time
		assertSame(first, second);
		assertEquals(1, harness.opened.size(), "a second client should not have been opened");
	}

	@Test
	@DisplayName("the solution is recoverable from the pool key, or nothing could be opened")
	void thePoolKeyCarriesTheSolution() throws Exception
	{
		Harness harness = new Harness();

		harness.borrow(SOLUTION, "alice-uid", "acme");

		assertEquals(SOLUTION, harness.opened.get(0));
	}

	@Test
	@DisplayName("invalidating a solution throws its pooled clients away")
	void invalidationDiscardsPooledClients() throws Exception
	{
		Harness harness = new Harness();

		IHeadlessClient first = harness.borrow(SOLUTION, "alice-uid", "acme");
		harness.giveBack(SOLUTION, "alice-uid", "acme", first);

		harness.runtime.invalidate(SOLUTION);

		IHeadlessClient afterwards = harness.borrow(SOLUTION, "alice-uid", "acme");

		// a pooled client still has the previous version of the solution open, so reusing one after
		// the solution changed would serve the old tools
		assertNotSame(first, afterwards);
		assertEquals(2, harness.opened.size());
		verify(first).shutDown(true);
	}

	@Test
	@DisplayName("invalidating one solution leaves another one's clients alone")
	void invalidationIsPerSolution() throws Exception
	{
		Harness harness = new Harness();

		IHeadlessClient other = harness.borrow("another_solution", "alice-uid", "acme");
		harness.giveBack("another_solution", "alice-uid", "acme", other);

		harness.runtime.invalidate(SOLUTION);

		assertSame(other, harness.borrow("another_solution", "alice-uid", "acme"),
			"a solution that did not change should not lose its clients");
	}

	@Test
	@DisplayName("invalidating something never asked for is harmless")
	void invalidatingAnUnknownSolutionDoesNothing()
	{
		Harness harness = new Harness();

		harness.runtime.invalidate("never_heard_of_it");
		harness.runtime.invalidate(null);
		harness.runtime.invalidate("   ");

		assertTrue(harness.opened.isEmpty());
	}

	@Test
	@DisplayName("an invalid client is not handed out again")
	void anInvalidClientIsReplaced() throws Exception
	{
		Harness harness = new Harness();

		IHeadlessClient first = harness.borrow(SOLUTION, "alice-uid", "acme");
		when(first.isValid()).thenReturn(Boolean.FALSE);
		harness.giveBack(SOLUTION, "alice-uid", "acme", first);

		// testOnBorrow is what keeps a dead client from being handed to the next call
		assertNotSame(first, harness.borrow(SOLUTION, "alice-uid", "acme"));
	}
}
