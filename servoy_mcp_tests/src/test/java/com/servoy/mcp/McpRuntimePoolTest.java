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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
		final List<String> opened = Collections.synchronizedList(new ArrayList<String>());

		/** Every client the pool asked for, in order; only filled by the limit constructors. */
		final List<IHeadlessClient> clients = Collections.synchronizedList(new ArrayList<IHeadlessClient>());

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

		Harness(int perKey, int idle, int total, Duration wait)
		{
			this(perKey, idle, total, wait, true);
		}

		Harness(int perKey, int idle, int total, Duration wait, boolean clientsAreValid)
		{
			runtime = new McpRuntime(solutionName -> {
				opened.add(solutionName);
				IHeadlessClient client = mock(IHeadlessClient.class);
				when(client.isValid()).thenReturn(Boolean.valueOf(clientsAreValid));
				clients.add(client);
				return client;
			}, perKey, idle, total, wait);
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

	private static final long TOLERANCE_MILLIS = 50;

	@AfterEach
	void dropTheSharedRuntime()
	{
		McpRuntime.shutDown();
	}

	/** Whether the stand-in was told to shut down, the way the pool's factory does it. */
	private static boolean wasShutDown(IHeadlessClient client)
	{
		return mockingDetails(client).getInvocations().stream()
			.anyMatch(invocation -> "shutDown".equals(invocation.getMethod().getName()) &&
				Boolean.TRUE.equals(invocation.getArguments()[0]));
	}

	private static long shutDownCount(List<IHeadlessClient> clients)
	{
		synchronized (clients)
		{
			return clients.stream().filter(McpRuntimePoolTest::wasShutDown).count();
		}
	}

	/** Borrows on its own thread, and records what came of it. */
	private static final class Borrower extends Thread
	{
		private final Harness harness;
		private final String user;

		volatile IHeadlessClient got;
		volatile Throwable failure;

		Borrower(Harness harness, String user)
		{
			this.harness = harness;
			this.user = user;
			setDaemon(true);
		}

		@Override
		public void run()
		{
			try
			{
				got = harness.borrow(SOLUTION, user, "acme");
			}
			catch (Throwable t)
			{
				failure = t;
			}
		}
	}

	/** Waits until the thread is parked, which for a borrower means it is queued for a client. */
	private static void awaitBlocked(Thread thread) throws InterruptedException
	{
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (thread.getState() != Thread.State.TIMED_WAITING && thread.getState() != Thread.State.WAITING)
		{
			if (!thread.isAlive()) fail("the borrower finished instead of waiting for a client");
			if (System.nanoTime() > deadline) fail("the borrower never started waiting, it is " + thread.getState());
			thread.join(5);
		}
	}

	@Nested
	class WaitingForAClient
	{
		@Test
		@DisplayName("a call over the per-key limit waits the whole timeout and is refused with the pool timeout")
		@Timeout(10)
		void timesOutAfterTheCap() throws Exception
		{
			Harness harness = new Harness(2, 2, -1, Duration.ofMillis(200));
			harness.borrow(SOLUTION, "alice-uid", "acme");
			harness.borrow(SOLUTION, "alice-uid", "acme");

			try (McpLogCapture log = McpLogCapture.of("servoy.mcp"))
			{
				long started = System.nanoTime();
				McpClientPoolTimeoutException refused = assertThrows(McpClientPoolTimeoutException.class,
					() -> harness.borrow(SOLUTION, "alice-uid", "acme"));
				long waited = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);

				assertTrue(waited >= 200 - TOLERANCE_MILLIS, "it must have waited for the timeout, waited " + waited + " ms");
				assertTrue(waited < 5000, "and not for much longer, waited " + waited + " ms");
				assertEquals(2, harness.opened.size(), "no third client may have been opened");

				// what the agent is told
				String message = refused.getMessage();
				assertTrue(message.contains("Timed out after 200 milliseconds"), message);
				assertTrue(message.contains("available Servoy client"), message);
				assertFalse(message.contains(SOLUTION), "the pool key must stay out of the agent's message: " + message);
				assertFalse(message.contains("alice-uid"), message);
				assertFalse(message.contains("acme"), message);
				assertFalse(message.contains("limit"), message);
				assertNotNull(refused.getCause(), "the pool's own exception is kept for the log");

				// what the operator is told
				assertTrue(log.contains("WARN"), "the refusal should be a WARN: " + log.lines());
				assertTrue(log.contains(SOLUTION + "|alice-uid|acme"), "naming the pool key: " + log.lines());
				assertTrue(log.contains("2 in use for this key (limit 2 per solution/user/tenant)"),
					"and the per-key count and limit: " + log.lines());
				assertTrue(log.contains("2 in use in total (limit -1)"), "and the totals: " + log.lines());
			}
		}

		@Test
		@DisplayName("a blocked call gets the client that is returned in time")
		@Timeout(15)
		void aReturnedClientServesTheWaiter() throws Exception
		{
			Harness harness = new Harness(1, 1, -1, Duration.ofSeconds(5));
			IHeadlessClient held = harness.borrow(SOLUTION, "alice-uid", "acme");

			Borrower waiter = new Borrower(harness, "alice-uid");
			waiter.start();
			awaitBlocked(waiter);

			harness.giveBack(SOLUTION, "alice-uid", "acme", held);
			waiter.join(10_000);

			assertFalse(waiter.isAlive(), "the waiter should have been served");
			assertNull(waiter.failure, "and not have failed: " + waiter.failure);
			assertSame(held, waiter.got, "with the client that was given back");
			assertEquals(1, harness.opened.size(), "no second client should have been opened");
		}

		@Test
		@DisplayName("the limit is per user and tenant, another key is not held up by it")
		@Timeout(10)
		void theCapIsPerKey() throws Exception
		{
			Harness harness = new Harness(1, 1, -1, Duration.ofMillis(200));
			harness.borrow(SOLUTION, "alice-uid", "acme");

			assertNotNull(harness.borrow(SOLUTION, "bob-uid", "acme"), "another user is under the cap");
			assertNotNull(harness.borrow(SOLUTION, "alice-uid", "globex"), "and so is the same user in another tenant");
			assertThrows(McpClientPoolTimeoutException.class, () -> harness.borrow(SOLUTION, "alice-uid", "acme"));
		}

		@Test
		@DisplayName("the overall client max holds across keys, and a returned client of another key makes room")
		@Timeout(10)
		void theOverallMaxHoldsAcrossKeys() throws Exception
		{
			Harness harness = new Harness(100, 8, 2, Duration.ofMillis(200));
			IHeadlessClient a = harness.borrow(SOLUTION, "a-uid", "acme");
			harness.borrow(SOLUTION, "b-uid", "acme");

			assertThrows(McpClientPoolTimeoutException.class, () -> harness.borrow(SOLUTION, "c-uid", "acme"),
				"two clients are out, the third key has to wait");

			harness.giveBack(SOLUTION, "a-uid", "acme", a);
			IHeadlessClient c = harness.borrow(SOLUTION, "c-uid", "acme");

			assertNotNull(c);
			assertNotSame(a, c, "a client is never handed to another key");
			assertTrue(wasShutDown(a), "the idle client of the first key was given up to make room");
		}

		@ParameterizedTest
		@CsvSource({ "300, 300 milliseconds", "1000, 1 second" })
		@DisplayName("the wait is the configured one, and the message says so")
		@Timeout(10)
		void theConfiguredWaitIsUsed(long millis, String expectedText) throws Exception
		{
			Harness harness = new Harness(1, 1, -1, Duration.ofMillis(millis));
			harness.borrow(SOLUTION, "alice-uid", "acme");

			long started = System.nanoTime();
			McpClientPoolTimeoutException refused = assertThrows(McpClientPoolTimeoutException.class,
				() -> harness.borrow(SOLUTION, "alice-uid", "acme"));
			long waited = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);

			assertEquals(Duration.ofMillis(millis), refused.getTimeout());
			assertTrue(refused.getMessage().contains("Timed out after " + expectedText), refused.getMessage());
			assertTrue(waited >= millis - TOLERANCE_MILLIS, "waited " + waited + " ms of " + millis);
		}

		@Test
		@DisplayName("a new client that fails validation is not reported as a pool timeout")
		@Timeout(15)
		void aFailedValidationIsNotATimeout() throws Exception
		{
			Harness harness = new Harness(2, 2, -1, Duration.ofSeconds(5), false);

			try (McpLogCapture log = McpLogCapture.of("servoy.mcp"))
			{
				long started = System.nanoTime();
				assertThrows(NoSuchElementException.class, () -> harness.borrow(SOLUTION, "alice-uid", "acme"));
				long waited = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);

				assertTrue(waited < 2500, "it comes back at once, not after the wait: " + waited + " ms");
				assertFalse(log.contains("timed out after"), "and is not announced as a timeout: " + log.lines());
			}
		}
	}

	@Nested
	class Configuration
	{
		@Test
		@DisplayName("the limits given to the runtime reach the pool")
		void theConfigIsPassedToThePool() throws Exception
		{
			// the idle limit is not the pool2 default (8), so an ignored parameter would show
			McpRuntime runtime = new Harness(20, 3, 7, Duration.ofSeconds(3)).runtime;

			assertEquals(20, runtime.getMaxClientsPerKey());
			assertEquals(3, runtime.getMaxIdleClientsPerKey());
		}

		@Test
		@DisplayName("the one-argument constructor passes the configured limits, in the right order")
		void theOneArgumentConstructorUsesTheConfiguredValues() throws Exception
		{
			McpRuntime runtime = new McpRuntime(solutionName -> mock(IHeadlessClient.class));
			try
			{
				assertEquals(McpRuntime.MAX_CLIENTS_PER_KEY, runtime.getMaxClientsPerKey());
				assertEquals(McpRuntime.MAX_IDLE_PER_KEY, runtime.getMaxIdleClientsPerKey());
			}
			finally
			{
				runtime.drain();
			}
		}

		@Test
		@DisplayName("nine clients for one key do not block under a limit of twenty")
		@Timeout(5)
		void nineClientsFitUnderTwenty() throws Exception
		{
			// with the config dropped the pool runs on its defaults: eight per key, and a ninth caller
			// that waits forever - which is the timeout of this test
			Harness harness = new Harness(20, 8, -1, Duration.ofSeconds(3));

			for (int i = 0; i < 9; i++)
			{
				assertNotNull(harness.borrow(SOLUTION, "alice-uid", "acme"));
			}
			assertEquals(9, harness.opened.size());
		}

		@Test
		@DisplayName("a limit of two refuses the third client instead of waiting forever")
		@Timeout(5)
		void aLimitOfTwoRefusesTheThird() throws Exception
		{
			Harness harness = new Harness(2, 8, -1, Duration.ofMillis(200));
			harness.borrow(SOLUTION, "alice-uid", "acme");
			harness.borrow(SOLUTION, "alice-uid", "acme");

			assertThrows(McpClientPoolTimeoutException.class, () -> harness.borrow(SOLUTION, "alice-uid", "acme"));
		}

		@Test
		@DisplayName("an overall max of seven refuses the eighth client of other users")
		@Timeout(5)
		void anOverallMaxOfSevenRefusesTheEighth() throws Exception
		{
			Harness harness = new Harness(20, 8, 7, Duration.ofMillis(200));
			for (int i = 0; i < 7; i++)
			{
				assertNotNull(harness.borrow(SOLUTION, "user-" + i, "acme"));
			}

			assertThrows(McpClientPoolTimeoutException.class, () -> harness.borrow(SOLUTION, "user-7", "acme"));
		}

		@Test
		@DisplayName("only the idle limit's worth of returned clients is kept")
		@Timeout(10)
		void onlyTheIdleLimitIsKept() throws Exception
		{
			Harness harness = new Harness(100, 3, -1, Duration.ofSeconds(5));
			List<IHeadlessClient> borrowed = new ArrayList<IHeadlessClient>();
			for (int i = 0; i < 10; i++)
			{
				borrowed.add(harness.borrow(SOLUTION, "alice-uid", "acme"));
			}

			for (IHeadlessClient client : borrowed)
			{
				harness.giveBack(SOLUTION, "alice-uid", "acme", client);
			}

			assertEquals(10, harness.clients.size());
			assertEquals(7, shutDownCount(harness.clients), "ten came back, three may stay, seven are shut down");
		}
	}

	@Nested
	class Draining
	{
		@Test
		@DisplayName("draining shuts the idle clients down")
		void drainShutsIdleClientsDown() throws Exception
		{
			Harness harness = new Harness();
			IHeadlessClient client = harness.borrow(SOLUTION, "alice-uid", "acme");
			harness.giveBack(SOLUTION, "alice-uid", "acme", client);

			harness.runtime.drain();

			verify(client).shutDown(true);
		}

		@Test
		@DisplayName("a client on loan during the drain is shut down when it comes back")
		void aLoanedClientIsShutDownOnReturn() throws Exception
		{
			Harness harness = new Harness();
			IHeadlessClient client = harness.borrow(SOLUTION, "alice-uid", "acme");

			harness.runtime.drain();
			verify(client, never()).shutDown(anyBoolean());

			harness.giveBack(SOLUTION, "alice-uid", "acme", client);
			verify(client).shutDown(true);
		}

		@Test
		@DisplayName("a drained runtime refuses with a closed pool, not with a pool timeout")
		void aDrainedRuntimeIsClosed() throws Exception
		{
			Harness harness = new Harness(1, 1, -1, Duration.ofMillis(200));
			harness.runtime.drain();

			assertThrows(IllegalStateException.class, () -> harness.borrow(SOLUTION, "alice-uid", "acme"));
		}

		@Test
		@DisplayName("a call waiting for a client is released by the drain, long before its timeout")
		@Timeout(30)
		void aWaiterIsReleasedByTheDrain() throws Exception
		{
			Harness harness = new Harness(1, 1, -1, Duration.ofSeconds(20));
			harness.borrow(SOLUTION, "alice-uid", "acme");

			Borrower waiter = new Borrower(harness, "alice-uid");
			long started = System.nanoTime();
			waiter.start();
			awaitBlocked(waiter);

			harness.runtime.drain();
			waiter.join(10_000);

			long waited = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
			assertFalse(waiter.isAlive(), "the waiter is still stuck after the drain");
			assertTrue(waited < 10_000, "it should not have waited out its 20 s: " + waited + " ms");
			assertNotNull(waiter.failure, "a released waiter has no client to show for it");
			assertFalse(waiter.failure instanceof McpClientPoolTimeoutException,
				"a drain is not a busy server: " + waiter.failure);
		}
	}

	@Nested
	class ServletLifecycle
	{
		@Test
		@DisplayName("destroying the servlet drains the shared runtime and the next use gets a fresh one")
		void destroyDrainsTheRuntime() throws Exception
		{
			McpRuntime before = McpRuntime.getInstance();

			new McpServlet().destroy();

			McpRuntime after = McpRuntime.getInstance();
			assertNotSame(before, after);
			assertThrows(IllegalStateException.class, () -> before.getClient(new McpClientKey(SOLUTION, "alice-uid", "acme")),
				"the old runtime's pool is closed");
		}

		@Test
		@DisplayName("destroying the servlet before anything used the runtime is harmless")
		void destroyWithoutARuntime()
		{
			McpRuntime.shutDown();

			new McpServlet().destroy();
		}
	}
}
