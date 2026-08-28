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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.servoy.j2db.ClientLogin;
import com.servoy.j2db.Credentials;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.shared.IApplicationServer;

/**
 * What the server does with a bearer token.
 *
 * <p>The design says the server decides nothing: the token goes to the solution's authenticator
 * module and the answer comes back. These tests hold that line - the token arrives untouched, a
 * refusal is the authenticator's own refusal, and the identity is whatever it said.</p>
 *
 * <p>No developer, no database, no real solution. Whether a particular authenticator logs someone in
 * correctly is that solution's business to test; what is ours is that we ask it and believe it.</p>
 *
 * @author Servoy
 */
@Tag("integration")
public class McpIdentityTest
{
	private static final String SOLUTION = "mcp_sample";

	private static final String AUTHENTICATOR = "mcp_sample_auth";

	/**
	 * A solution with one authenticator module, as the repository would hand it over.
	 */
	private static IRepository repositoryWithAuthenticator() throws Exception
	{
		Solution solution = mock(Solution.class);
		when(solution.getModulesNames()).thenReturn(AUTHENTICATOR);

		Solution authenticator = mock(Solution.class);
		when(authenticator.getSolutionType()).thenReturn(SolutionMetaData.AUTHENTICATOR);
		when(authenticator.getName()).thenReturn(AUTHENTICATOR);

		IRepository repository = mock(IRepository.class);
		when(repository.getActiveRootObject(SOLUTION, IRepository.SOLUTIONS)).thenReturn(solution);
		when(repository.getActiveRootObject(AUTHENTICATOR, IRepository.SOLUTIONS)).thenReturn(authenticator);
		return repository;
	}

	/** What an authenticator returns when it accepts a token. */
	private static ClientLogin accepted(String uid, String name, String tenant)
	{
		return new ClientLogin(null, uid, name, new String[] { "mcp_user" }, null, new String[] { tenant });
	}

	/** What it returns when it declines one: no user uid, and a message of its own. */
	private static ClientLogin refused(String reason)
	{
		return new ClientLogin(null, null, null, null, reason);
	}

	private static Credentials captureLogin(IApplicationServer server) throws Exception
	{
		ArgumentCaptor<Credentials> sent = ArgumentCaptor.forClass(Credentials.class);
		verify(server).login(sent.capture());
		return sent.getValue();
	}

	@Test
	@DisplayName("the token reaches the authenticator untouched")
	void theTokenIsPassedThroughUnchanged() throws Exception
	{
		IApplicationServer server = mock(IApplicationServer.class);
		when(server.login(any(Credentials.class))).thenReturn(accepted("alice-uid", "alice", "acme"));

		McpIdentity.authenticate(SOLUTION, "  a-token-we-never-read  ", repositoryWithAuthenticator(), server);

		JSONObject credentials = new JSONObject(captureLogin(server).getJscredentials());
		assertEquals("a-token-we-never-read", credentials.getString(McpIdentity.USER_TOKEN),
			"the server must not read, decode or verify the token - only hand it over");
	}

	@Test
	@DisplayName("the authenticator is named, and no client id goes with the request")
	void theRequestNamesTheAuthenticatorAndNoClient() throws Exception
	{
		IApplicationServer server = mock(IApplicationServer.class);
		when(server.login(any(Credentials.class))).thenReturn(accepted("alice-uid", "alice", "acme"));

		McpIdentity.authenticate(SOLUTION, "a-token", repositoryWithAuthenticator(), server);

		Credentials sent = captureLogin(server);
		assertEquals(AUTHENTICATOR, sent.getAuthenticatorType(), "the authenticator module is named in the request");

		// load-bearing: with a client id the server applies the identity to that client and returns
		// nothing, and the tenant has to be known before a client is borrowed - it is part of the key
		assertNull(sent.getClientId(), "no client id, or the tenant does not come back");
	}

	@Test
	@DisplayName("the identity is whatever the authenticator said it is")
	void theIdentityComesBackFromTheAuthenticator() throws Exception
	{
		IApplicationServer server = mock(IApplicationServer.class);
		when(server.login(any(Credentials.class))).thenReturn(accepted("bob-uid", "bob", "globex"));

		McpIdentity identity = McpIdentity.authenticate(SOLUTION, "a-token", repositoryWithAuthenticator(), server);

		assertEquals("bob-uid", identity.getUserUid());
		assertEquals("bob", identity.getUserName());
		assertArrayEquals(new String[] { "globex" }, identity.getTenants());
		assertArrayEquals(new String[] { "mcp_user" }, identity.getPermissions());
	}

	@Test
	@DisplayName("two tokens are two identities, and two pool keys")
	void twoTokensAreTwoIdentities() throws Exception
	{
		IApplicationServer server = mock(IApplicationServer.class);
		when(server.login(any(Credentials.class)))
			.thenReturn(accepted("alice-uid", "alice", "acme"))
			.thenReturn(accepted("bob-uid", "bob", "globex"));

		IRepository repository = repositoryWithAuthenticator();
		McpIdentity alice = McpIdentity.authenticate(SOLUTION, "token-a", repository, server);
		McpIdentity bob = McpIdentity.authenticate(SOLUTION, "token-b", repository, server);

		// this is what keeps one agent's client away from another's
		assertEquals(false, alice.toClientKey(SOLUTION).equals(bob.toClientKey(SOLUTION)),
			"two users must not share a pooled client");
	}

	@Test
	@DisplayName("a refusal carries the authenticator's own message")
	void aRefusalCarriesTheAuthenticatorsMessage() throws Exception
	{
		IApplicationServer server = mock(IApplicationServer.class);
		when(server.login(any(Credentials.class))).thenReturn(refused("{\"error\":\"Unknown token\"}"));

		IRepository repository = repositoryWithAuthenticator();
		McpIdentity.McpAuthenticationException thrown = assertThrows(McpIdentity.McpAuthenticationException.class,
			() -> McpIdentity.authenticate(SOLUTION, "not-a-real-token", repository, server));

		assertTrue(thrown.getMessage().contains("Unknown token"),
			"we have no idea why a token was refused; the authenticator does: " + thrown.getMessage());
	}

	@Test
	@DisplayName("a null answer is a refusal too")
	void aNullLoginIsRefused() throws Exception
	{
		IApplicationServer server = mock(IApplicationServer.class);
		when(server.login(any(Credentials.class))).thenReturn(null);

		IRepository repository = repositoryWithAuthenticator();
		assertThrows(McpIdentity.McpAuthenticationException.class,
			() -> McpIdentity.authenticate(SOLUTION, "a-token", repository, server));
	}

	@Test
	@DisplayName("no token at all is refused before the authenticator is even asked")
	void noTokenIsRefusedWithoutCallingTheAuthenticator() throws Exception
	{
		IApplicationServer server = mock(IApplicationServer.class);
		IRepository repository = repositoryWithAuthenticator();

		assertThrows(McpIdentity.McpAuthenticationException.class,
			() -> McpIdentity.authenticate(SOLUTION, null, repository, server));
		assertThrows(McpIdentity.McpAuthenticationException.class,
			() -> McpIdentity.authenticate(SOLUTION, "   ", repository, server));

		verify(server, never()).login(any(Credentials.class));
	}

	@Test
	@DisplayName("a solution with no authenticator cannot authorise anything")
	void withoutAnAuthenticatorNothingIsAuthorised() throws Exception
	{
		Solution solution = mock(Solution.class);
		when(solution.getModulesNames()).thenReturn("");

		IRepository repository = mock(IRepository.class);
		when(repository.getActiveRootObject(SOLUTION, IRepository.SOLUTIONS)).thenReturn(solution);

		IApplicationServer server = mock(IApplicationServer.class);

		McpIdentity.McpAuthenticationException thrown = assertThrows(McpIdentity.McpAuthenticationException.class,
			() -> McpIdentity.authenticate(SOLUTION, "a-token", repository, server));

		assertTrue(thrown.getMessage().contains("authenticator"), thrown.getMessage());
		verify(server, never()).login(any(Credentials.class));
	}

	@Test
	@DisplayName("a module that is not an authenticator is not mistaken for one")
	void onlyAnAuthenticatorModuleCounts() throws Exception
	{
		Solution solution = mock(Solution.class);
		when(solution.getModulesNames()).thenReturn("some_module");

		Solution module = mock(Solution.class);
		when(module.getSolutionType()).thenReturn(SolutionMetaData.MODULE);

		IRepository repository = mock(IRepository.class);
		when(repository.getActiveRootObject(SOLUTION, IRepository.SOLUTIONS)).thenReturn(solution);
		when(repository.getActiveRootObject("some_module", IRepository.SOLUTIONS)).thenReturn(module);

		assertNull(McpIdentity.findAuthenticator(SOLUTION, repository));
	}

	@Test
	@DisplayName("an authenticator that blows up is reported, not passed on raw")
	void aFailingAuthenticatorIsReported() throws Exception
	{
		IApplicationServer server = mock(IApplicationServer.class);
		when(server.login(any(Credentials.class))).thenThrow(new Boom());

		IRepository repository = repositoryWithAuthenticator();

		try (McpLogCapture log = McpLogCapture.of("servoy.mcp"))
		{
			McpIdentity.McpAuthenticationException thrown = assertThrows(McpIdentity.McpAuthenticationException.class,
				() -> McpIdentity.authenticate(SOLUTION, "a-token", repository, server));

			// the agent gets something it can act on, and no more: why the authenticator broke is
			// the solution's business and could say anything at all
			assertTrue(thrown.getMessage().contains("could not be reached"), thrown.getMessage());

			// the cause is not lost, it goes to the log - which is where whoever runs the server
			// looks, and the only place the real reason is written down
			assertTrue(log.contains("the authenticator"), "the failure should be logged: " + log.lines());
			assertTrue(log.contains(AUTHENTICATOR), "and should name it: " + log.lines());
		}
	}

	/**
	 * The failure the authenticator is made to suffer, without a stack trace.
	 *
	 * <p>Logging the trace is the right thing for the production code to do - it is the only record
	 * of why a solution's authenticator broke. Here the failure is provoked on purpose, and eighty
	 * lines of JUnit frames in the console is where a real error would go unnoticed.</p>
	 */
	private static final class Boom extends RuntimeException
	{
		Boom()
		{
			super("boom", null, false, false); //$NON-NLS-1$
		}
	}
}
