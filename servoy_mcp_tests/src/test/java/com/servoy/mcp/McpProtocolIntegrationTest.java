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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.server.shared.IHeadlessClient;
import com.servoy.mcp.McpIdentity.McpAuthenticationException;

/**
 * A real MCP conversation with a solution's server, from JSON-RPC in to JSON-RPC out.
 *
 * <p>Everything between is the production code: the SDK transport, the tool catalogue, the
 * authentication step, the argument mapping, the rendering of what a function returned. Two things
 * are stood in for, and only two - the authenticator module, because there is no solution deployed
 * to hold one, and the client the function runs in, because there is no database behind it. That is
 * the boundary: fake who the user is and what the function returns, and let the protocol be
 * genuine.</p>
 *
 * <p>The tools are not invented either. They are parsed out of the {@code mcp_sample} scope files
 * that ship with these tests, through the same {@link McpJsDoc} the scanner uses, so what is
 * published here is what the sample declares.</p>
 *
 * @author Servoy
 */
@Tag("integration")
@SuppressWarnings("nls")
public class McpProtocolIntegrationTest
{
	private static final String SOLUTION = McpTestFixture.SOLUTION;

	private static final String ALICE_TOKEN = "mcp-demo-token-alice";

	/**
	 * A server for the sample solution, wired to stand-ins it can record.
	 */
	private static final class Harness
	{
		final McpFakeClient fake = new McpFakeClient();

		final McpRuntime runtime;

		final McpSolutionServer server;

		/** What the last tool call was asked to run, and with what. */
		final AtomicReference<String> ranContext = new AtomicReference<String>();

		final AtomicReference<Object[]> ranArguments = new AtomicReference<Object[]>();

		/** What the stood-in function hands back. */
		final AtomicReference<Object> returns = new AtomicReference<Object>("ok");

		Harness() throws Exception
		{
			this(McpProtocolIntegrationTest::publishableSampleTools, (solutionName, token) -> {
				if (token == null) throw new McpAuthenticationException("No bearer token was sent");
				if (!ALICE_TOKEN.equals(token)) throw new McpAuthenticationException("This token was refused");
				return new McpIdentity("alice-uid", "alice", new String[] { "mcp_user" }, new String[] { "acme" });
			});
		}

		Harness(McpToolRegistry.ToolSource tools, McpSolutionServer.Authenticator authenticator) throws Exception
		{
			runtime = new McpRuntime(solutionName -> fake.client);

			IClientPluginAccess access = fake.client.getPluginAccess();
			when(access.executeMethod(any(), any(), any(), anyBoolean())).thenAnswer(invocation -> {
				ranContext.set((String)invocation.getArgument(0));
				ranArguments.set((Object[])invocation.getArgument(2));
				return returns.get();
			});

			server = new McpSolutionServer(runtime, SOLUTION, new McpToolRegistry(runtime, SOLUTION, tools), authenticator);
		}
	}

	/**
	 * The tools the sample solution declares, read from its scope files.
	 */
	static List<McpTool> sampleTools() throws Exception
	{
		List<McpTool> tools = new ArrayList<McpTool>();

		for (String scopeName : new String[] { "myScope", "salesTools" })
		{
			String source = McpTestFixture.scopeSource(scopeName);
			for (String declaration : McpTestFixture.declarations(source))
			{
				if (!McpJsDoc.isTool(declaration)) continue;

				McpTool tool = McpJsDoc.parse(scopeName, McpTestFixture.functionName(declaration), declaration);
				if (tool != null) tools.add(tool);
			}
		}

		return tools;
	}

	/**
	 * The same, without the one the sample declares on purpose as unpublishable.
	 *
	 * <p>What every test but {@link #anUnsupportedToolIsNotPublished()} wants. Refusing that tool is
	 * correct and is logged at error level, which is also correct - but a server built once per test
	 * would print it once per test, and a console full of an expected error is where an unexpected
	 * one goes unnoticed.</p>
	 */
	static List<McpTool> publishableSampleTools() throws Exception
	{
		List<McpTool> tools = new ArrayList<McpTool>();

		for (McpTool tool : sampleTools())
		{
			if (!"myScope_unsupportedParameterType".equals(tool.getName())) tools.add(tool);
		}

		return tools;
	}

	@Test
	@DisplayName("tools/list answers with the solution's tools")
	void toolsListNamesTheTools() throws Exception
	{
		Harness harness = new Harness();

		McpCall call = McpCall.toolsList(harness.server);

		assertEquals(200, call.status());
		assertTrue(call.contains("\"myScope_add\""), "the catalogue should name add: " + call.body());
		assertTrue(call.contains("\"salesTools_findCustomers\""), "and the sales tools: " + call.body());
	}

	@Test
	@DisplayName("tools/list carries the description and the schema out of the JSDoc")
	void toolsListCarriesTheSchema() throws Exception
	{
		Harness harness = new Harness();

		McpCall call = McpCall.toolsList(harness.server);

		// what the agent decides on: the sentence from the documentation block, and the argument names
		assertTrue(call.contains("\"first\""), "the parameter names should be published: " + call.body());
		assertTrue(call.contains("\"second\""));
		assertTrue(call.contains("integer") || call.contains("number"), "typed as a number: " + call.body());
	}

	@Test
	@DisplayName("a tool whose parameter type is unsupported is not published")
	void anUnsupportedToolIsNotPublished() throws Exception
	{
		// the only test given the full sample, bad tool and all - the others are spared the error
		// this deliberately provokes
		Harness harness = new Harness(McpProtocolIntegrationTest::sampleTools, (solutionName, token) -> {
			throw new McpAuthenticationException("not needed here");
		});

		try (McpLogCapture log = McpLogCapture.of("servoy.mcp"))
		{
			McpCall call = McpCall.toolsList(harness.server);

			// mcp_sample declares one on purpose, taking a Date - dropping it is what keeps a
			// solution with one bad tool serving the rest
			assertFalse(call.contains("unsupportedParameterType"), "it should have been dropped: " + call.body());
			assertTrue(call.contains("myScope_add"), "and the good ones still published: " + call.body());

			// and the drop is announced. Nothing else marks it - there is no error in the Developer,
			// which Johan was content with precisely because this line exists, so it is the whole of
			// what the author of that tool ever hears
			assertTrue(log.contains("not publishing a tool"), "the drop should be logged: " + log.lines());
			assertTrue(log.contains("unsupportedParameterType"), "and should name the tool: " + log.lines());
			assertTrue(log.contains("must be String, Number or JSON"), "and say why: " + log.lines());
		}
	}

	@Test
	@DisplayName("tools/call reaches the function and returns what it returned")
	void aToolCallReachesTheFunction() throws Exception
	{
		Harness harness = new Harness();
		harness.returns.set(Double.valueOf(42));

		McpCall call = McpCall.callTool(harness.server, "myScope_add", "{\"first\":2,\"second\":40}", ALICE_TOKEN);

		assertEquals(200, call.status());
		assertTrue(call.contains("42"), call.body());
		assertFalse(call.contains("\"isError\":true"), call.body());

		// the scope prefix matters: executeMethod resolves a bare name as a form first
		assertEquals("scopes.myScope", harness.ranContext.get());
	}

	@Test
	@DisplayName("a whole number comes back as 42, not 42.0")
	void wholeNumbersAreNotRenderedAsDecimals() throws Exception
	{
		Harness harness = new Harness();
		harness.returns.set(Double.valueOf(42));

		McpCall call = McpCall.callTool(harness.server, "myScope_add", "{\"first\":2,\"second\":40}", ALICE_TOKEN);

		// every number crosses from Rhino as a Double; an agent reading "42.0" as a count is a
		// avoidable confusion
		assertFalse(call.contains("42.0"), call.body());
	}

	@Test
	@DisplayName("named arguments arrive in the order the function declares")
	void argumentsArriveInDeclaredOrder() throws Exception
	{
		Harness harness = new Harness();

		McpCall.callTool(harness.server, "myScope_add", "{\"second\":40,\"first\":2}", ALICE_TOKEN);

		Object[] arguments = harness.ranArguments.get();
		assertEquals(2, arguments.length);
		assertEquals("2", String.valueOf(((Number)arguments[0]).intValue()));
		assertEquals("40", String.valueOf(((Number)arguments[1]).intValue()));
	}

	@Test
	@DisplayName("an argument left out arrives as null")
	void anOmittedOptionalArgumentIsNull() throws Exception
	{
		Harness harness = new Harness();

		McpCall.callTool(harness.server, "myScope_echo", "{\"payload\":{\"a\":1}}", ALICE_TOKEN);

		Object[] arguments = harness.ranArguments.get();
		assertEquals(2, arguments.length, "the optional note still takes a slot");
		assertEquals(null, arguments[1], "which a Servoy function sees as null");
	}

	@Test
	@DisplayName("the caller ends up on the session the tool runs in")
	void theCallerReachesTheSession() throws Exception
	{
		Harness harness = new Harness();

		McpCall.callTool(harness.server, "myScope_whoAmI", null, ALICE_TOKEN);

		assertEquals("alice-uid", harness.fake.clientInfo.getUserUid());
		assertEquals("alice", harness.fake.clientInfo.getUserName());
	}

	@Test
	@DisplayName("no token means the tool does not run")
	void aCallWithoutATokenIsRefused() throws Exception
	{
		Harness harness = new Harness();

		McpCall call = McpCall.callTool(harness.server, "myScope_add", "{\"first\":2,\"second\":40}", null);

		assertTrue(call.contains("\"isError\":true"), call.body());
		assertEquals(null, harness.ranContext.get(), "nothing should have been run");
	}

	@Test
	@DisplayName("a refused token is reported in the authenticator's own words")
	void aRefusedTokenKeepsItsMessage() throws Exception
	{
		Harness harness = new Harness();

		McpCall call = McpCall.callTool(harness.server, "myScope_add", "{\"first\":2,\"second\":40}", "not-a-token");

		assertTrue(call.contains("\"isError\":true"), call.body());
		assertTrue(call.contains("This token was refused"),
			"the solution decides why, and the agent should be told: " + call.body());
		assertEquals(null, harness.ranContext.get());
	}

	@Test
	@DisplayName("listing the tools needs no token")
	void theCatalogueIsOpen() throws Exception
	{
		Harness harness = new Harness();

		// discovery is not privileged: an agent has to see what is on offer before it can be told
		// it may not use it. The token is checked when a tool is actually run.
		McpCall call = McpCall.toolsList(harness.server);

		assertEquals(200, call.status());
		assertTrue(call.contains("myScope_add"), call.body());
	}

	@Test
	@DisplayName("an unknown tool is an error, not a crash")
	void anUnknownToolIsReported() throws Exception
	{
		Harness harness = new Harness();

		McpCall call = McpCall.callTool(harness.server, "myScope_noSuchTool", null, ALICE_TOKEN);

		assertEquals(200, call.status(), "a JSON-RPC error still travels as a 200");
		assertTrue(call.contains("error") || call.contains("isError"), call.body());
	}

	@Test
	@DisplayName("a tool that throws answers an error result, not a protocol error")
	void aFailingToolIsAnErrorResult() throws Exception
	{
		Harness harness = new Harness();

		IClientPluginAccess access = harness.fake.client.getPluginAccess();
		when(access.executeMethod(any(), eq("add"), any(), anyBoolean()))
			.thenThrow(new RuntimeException("the table is not there"));

		McpCall call = McpCall.callTool(harness.server, "myScope_add", "{\"first\":2,\"second\":40}", ALICE_TOKEN);

		// a failing tool is a normal outcome the agent should be able to read and react to; a
		// protocol error would mean the call itself was malformed
		assertTrue(call.contains("\"isError\":true"), call.body());
		assertTrue(call.contains("the table is not there"), call.body());
	}

	@Test
	@DisplayName("a solution with no tools still answers")
	void anEmptySolutionAnswers() throws Exception
	{
		Harness harness = new Harness(() -> new ArrayList<McpTool>(), (solutionName, token) -> {
			throw new McpAuthenticationException("nobody");
		});

		McpCall call = McpCall.toolsList(harness.server);

		assertEquals(200, call.status());
		assertTrue(call.contains("tools"), "an empty catalogue is still a catalogue: " + call.body());
	}

	@Test
	@DisplayName("the client the tool ran in is given back to the pool")
	void theClientIsReturned() throws Exception
	{
		Harness harness = new Harness();

		McpCall.callTool(harness.server, "myScope_add", "{\"first\":2,\"second\":40}", ALICE_TOKEN);

		IHeadlessClient borrowed = harness.runtime.getClient(new McpClientKey(SOLUTION, "alice-uid", "acme"));

		// leaking a client per call would exhaust the pool under any real agent traffic
		assertEquals(harness.fake.client, borrowed, "the same client should still be poolable");
	}

	@Test
	@DisplayName("a malformed payload is refused without reaching a tool")
	void aMalformedPayloadIsRefused() throws Exception
	{
		Harness harness = new Harness();

		McpCall call = McpCall.post(harness.server, "{ this is not json", ALICE_TOKEN);

		assertEquals(null, harness.ranContext.get(), "nothing should have been run");
		assertTrue(call.status() >= 400 || call.contains("error"), "status " + call.status() + ", body " + call.body());
	}

	/** Kept out of the harness so the unused-mock warning does not hide a real one. */
	static IHeadlessClient unusedClient()
	{
		return mock(IHeadlessClient.class);
	}
}
