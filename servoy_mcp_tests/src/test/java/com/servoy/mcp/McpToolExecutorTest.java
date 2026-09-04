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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.server.shared.IHeadlessClient;

/**
 * How the identity the authenticator handed back is put onto the client that will run the tool.
 *
 * <p>This is the step that makes a tool run <em>as somebody</em>. If it were skipped the tool would
 * still run, and would quietly see everything - which is why it is worth a test of its own rather
 * than being inferred from a call that happened to return the right rows.</p>
 *
 * @author Servoy
 */
@Tag("integration")
public class McpToolExecutorTest
{
	/** An identity as the authenticator would have handed it back. */
	private static McpIdentity identity(String uid, String name, String[] permissions, String[] tenants)
	{
		return new McpIdentity(uid, name, permissions, tenants);
	}

	@Test
	@DisplayName("the user reaches the client session")
	void theUserIsAppliedToTheSession() throws Exception
	{
		McpFakeClient fake = new McpFakeClient();

		McpToolExecutor.applyIdentity(fake.client,
			identity("alice-uid", "alice", new String[] { "mcp_user" }, new String[] { "acme" }));

		assertEquals("alice-uid", fake.clientInfo.getUserUid());
		assertEquals("alice", fake.clientInfo.getUserName());
		assertArrayEquals(new String[] { "mcp_user" }, fake.clientInfo.getUserGroups());
	}

	@Test
	@DisplayName("every tenant is set, not just the first")
	void allTenantsAreApplied() throws Exception
	{
		McpFakeClient fake = new McpFakeClient();

		McpToolExecutor.applyIdentity(fake.client,
			identity("alice-uid", "alice", null, new String[] { "acme", "globex" }));

		ArgumentCaptor<Object> value = ArgumentCaptor.forClass(Object.class);
		verify(fake.foundSetManager).setTenantValue(eq(fake.solution), value.capture());

		// a user may belong to several tenants and the filter covers all of them, the way the NG
		// client does with the same token
		assertTrue(value.getValue() instanceof Object[], "the whole array goes in, not one element");
		assertEquals(2, ((Object[])value.getValue()).length);
	}

	@Test
	@DisplayName("no tenant means no filter, not an empty one")
	void withoutTenantsNoFilterIsSet() throws Exception
	{
		McpFakeClient fake = new McpFakeClient();

		McpToolExecutor.applyIdentity(fake.client, identity("alice-uid", "alice", null, new String[0]));

		// setting an empty filter would be a different thing from setting none, and would hide rows
		verify(fake.foundSetManager, never()).setTenantValue(any(Solution.class), any());
	}

	@Test
	@DisplayName("missing permissions are left alone rather than cleared")
	void nullPermissionsAreNotApplied() throws Exception
	{
		McpFakeClient fake = new McpFakeClient();

		McpToolExecutor.applyIdentity(fake.client, identity("alice-uid", "alice", null, null));

		assertEquals("alice-uid", fake.clientInfo.getUserUid());
		assertNull(fake.clientInfo.getUserGroups(), "no permissions is not the same as no permissions at all");
	}

	@Test
	@DisplayName("the identity is applied on the client's own thread")
	void theWorkGoesThroughInvokeAndWait() throws Exception
	{
		McpFakeClient fake = new McpFakeClient();

		McpToolExecutor.applyIdentity(fake.client, identity("alice-uid", "alice", null, null));

		// touching a client's session from any other thread is how a client gets corrupted
		verify(fake.application).invokeAndWait(any(Runnable.class));
	}

	@Test
	@DisplayName("a failure inside the client's thread is not swallowed")
	void aFailureInsideIsRethrown()
	{
		McpFakeClient fake = new McpFakeClient();
		when(fake.application.getClientInfo()).thenThrow(new IllegalStateException("no session"));

		IllegalStateException thrown = assertThrows(IllegalStateException.class,
			() -> McpToolExecutor.applyIdentity(fake.client, identity("alice-uid", "alice", null, null)));

		// running a tool as nobody, silently, would be worse than refusing the call
		assertEquals("no session", thrown.getMessage());
	}

	@Test
	@DisplayName("an unexpected client refuses to be used rather than being half configured")
	void anUnexpectedPluginAccessIsRefused()
	{
		IHeadlessClient client = mock(IHeadlessClient.class);
		when(client.getPluginAccess()).thenReturn(mock(IClientPluginAccess.class));

		IllegalStateException thrown = assertThrows(IllegalStateException.class,
			() -> McpToolExecutor.applyIdentity(client, identity("alice-uid", "alice", null, null)));

		assertTrue(thrown.getMessage().contains("Cannot apply the identity"), thrown.getMessage());
	}

	@Test
	@DisplayName("a scope is named as a scope, or it is taken for a form")
	void theContextNamesTheScope()
	{
		// executeMethod resolves the context as a form name first, so the prefix is not optional
		assertEquals("scopes.myScope", McpToolExecutor.toContext("myScope"));
		assertEquals("scopes.salesTools", McpToolExecutor.toContext("salesTools"));
	}

	@Test
	@DisplayName("no scope means globals")
	void theDefaultContextIsGlobals()
	{
		assertEquals("scopes.globals", McpToolExecutor.toContext(null));
	}
}
