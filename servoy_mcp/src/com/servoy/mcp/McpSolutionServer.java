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

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Debug;
import com.servoy.mcp.McpIdentity.McpAuthenticationException;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.schema.JsonSchemaValidator;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServer.StatelessSyncSpecification;
import io.modelcontextprotocol.server.transport.HttpServletStatelessServerTransport;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * The MCP server for one solution.
 *
 * <p>The protocol is the official SDK's, the same one the Developer MCP servers run
 * (<code>servoy-developer-mcp</code>): a streamable HTTP transport wrapped by an
 * {@link McpSyncServer}. What is Servoy's own is everything either side of it - finding the tools in
 * the solution, and running one in a pooled client as the right user.</p>
 *
 * <p>Tools are registered dynamically rather than declared, because they are not known until a
 * solution has been scanned. The SDK supports that through {@link McpSyncServer#addTool}, which the
 * annotation-driven framework in the Developer bundle does not - its tools are Java methods known at
 * compile time.</p>
 *
 * <p>The bearer token travels in <code>Authorization: Bearer</code> and is lifted into the transport
 * context by an extractor, so a tool handler can read it without having to see the request. What it
 * means is not decided here - see {@link McpIdentity}.</p>
 *
 * @author Servoy
 */
@SuppressWarnings("nls")
public class McpSolutionServer
{
	/** Key under which the bearer token is put into the transport context. */
	static final String BEARER_TOKEN = "servoy.mcp.bearerToken";

	private static final String BEARER_PREFIX = "Bearer ";

	private static final String SERVER_NAME = "servoy-mcp";
	private static final String SERVER_VERSION = "1.0.0";

	/**
	 * The schema validator the SDK insists on having.
	 *
	 * <p>It validates <b>structured output</b> - a typed JSON result declared by an output schema.
	 * Tools here return text, so nothing ever reaches it; the SDK simply refuses to build a server
	 * without one, and resolves it through {@link java.util.ServiceLoader} from
	 * <code>mcp-json-jackson2</code> when none is supplied. Supplying this instead is what lets the
	 * server run without embedding that artifact, which is not an OSGi bundle.</p>
	 *
	 * <p>If tools ever publish an output schema, this has to be replaced by something that really
	 * validates - accepting everything would then be a silent lie.</p>
	 */
	private static final JsonSchemaValidator PERMISSIVE_VALIDATOR = (schema, structuredContent) -> JsonSchemaValidator.ValidationResponse
		.asValid(structuredContent == null ? null : String.valueOf(structuredContent));

	/**
	 * Who a bearer token belongs to.
	 *
	 * <p>Normally the solution's own authenticator module, reached through {@link McpIdentity}.
	 * Naming it lets a test stand in for that module, which is the only way to drive a real
	 * JSON-RPC call through this server without a solution being deployed anywhere.</p>
	 */
	interface Authenticator
	{
		McpIdentity authenticate(String solutionName, String bearerToken) throws McpAuthenticationException;
	}

	private final McpRuntime runtime;
	private final String solutionName;
	private final McpToolRegistry registry;
	private final McpToolExecutor executor;
	private final Authenticator authenticator;

	private volatile HttpServletStatelessServerTransport transport;

	McpSolutionServer(McpRuntime runtime, String solutionName)
	{
		this(runtime, solutionName, new McpToolRegistry(runtime, solutionName), McpIdentity::authenticate);
	}

	McpSolutionServer(McpRuntime runtime, String solutionName, McpToolRegistry registry, Authenticator authenticator)
	{
		this.runtime = runtime;
		this.solutionName = solutionName;
		this.registry = registry;
		this.executor = new McpToolExecutor(runtime);
		this.authenticator = authenticator;
	}

	public String getSolutionName()
	{
		return solutionName;
	}

	public McpToolRegistry getRegistry()
	{
		return registry;
	}

	/**
	 * Hands the request to the SDK transport, building the server on first use.
	 */
	public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		try
		{
			transport().service(request, response);
		}
		catch (Exception e)
		{
			throw new ServletException(e.getMessage(), e);
		}
	}

	private HttpServletStatelessServerTransport transport() throws Exception
	{
		HttpServletStatelessServerTransport existing = transport;
		if (existing != null) return existing;

		synchronized (this)
		{
			if (transport != null) return transport;

			McpJsonMapper jsonMapper = new McpJacksonMapper(new ObjectMapper());

			// The transport matches on the request URI ending with this, so each solution's server
			// answers only its own path: /servoy-service/mcp/<solution>
			String endpoint = "/" + McpRuntime.WEBSERVICE_NAME + "/" + solutionName;

			HttpServletStatelessServerTransport built = HttpServletStatelessServerTransport.builder()
				.jsonMapper(jsonMapper)
				.messageEndpoint(endpoint)
				.contextExtractor(McpSolutionServer::extractContext)
				.build();

			// Both of these are otherwise resolved through ServiceLoader, from mcp-json-jackson2 -
			// which is not an OSGi bundle. Supplying them is what lets the server run without
			// embedding that artifact.
			StatelessSyncSpecification specification = McpServer.sync(built)
				.serverInfo(SERVER_NAME, SERVER_VERSION)
				.jsonMapper(jsonMapper)
				.jsonSchemaValidator(PERMISSIVE_VALIDATOR);

			int published = 0;
			for (McpTool tool : registry.getTools())
			{
				try
				{
					specification.toolCall(describe(jsonMapper, tool),
						(context, request) -> call(tool, context, request));
					published++;
				}
				catch (Exception e)
				{
					// one tool that cannot be published must not take the rest of the solution down
					McpRuntime.log.warn("mcp: could not publish '" + tool.getName() + "'", e);
				}
			}

			specification.build();

			McpRuntime.log.info("mcp: solution '{}' publishes {} tool(s)", solutionName, Integer.valueOf(published));

			transport = built;
			return built;
		}
	}

	/**
	 * Lifts the bearer token out of the request, so a tool handler can read it from the exchange.
	 */
	static McpTransportContext extractContext(HttpServletRequest request)
	{
		String header = request.getHeader("Authorization");
		if (header == null) return McpTransportContext.EMPTY;

		String trimmed = header.trim();
		if (trimmed.length() <= BEARER_PREFIX.length() ||
			!trimmed.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length()))
		{
			return McpTransportContext.EMPTY;
		}

		String token = trimmed.substring(BEARER_PREFIX.length()).trim();
		if (token.length() == 0) return McpTransportContext.EMPTY;

		Map<String, Object> metadata = new HashMap<>();
		metadata.put(BEARER_TOKEN, token);
		return McpTransportContext.create(metadata);
	}

	/**
	 * What a published tool looks like to an agent: its name, what it is for, and the shape of its
	 * arguments - all read out of the documentation block in the solution.
	 */
	private static McpSchema.Tool describe(McpJsonMapper jsonMapper, McpTool tool) throws Exception
	{
		return McpSchema.Tool.builder()
			.name(tool.getName())
			.description(tool.getDescription())
			.inputSchema(jsonMapper, McpToolSchema.buildInputSchema(tool))
			.build();
	}

	/**
	 * Runs one tool.
	 *
	 * <p>A tool that throws answers a result carrying <code>isError</code> rather than a protocol
	 * error: a failing tool is a normal outcome the agent should be able to read and react to,
	 * whereas a protocol error means the call itself was malformed. A refused token is the
	 * exception - there is nobody to run as, so that is reported as an error result too, but with
	 * the reason spelled out.</p>
	 */
	private McpSchema.CallToolResult call(McpTool tool, McpTransportContext context, McpSchema.CallToolRequest request)
	{
		McpIdentity identity;
		try
		{
			Object bearer = context == null ? null : context.get(BEARER_TOKEN);
			identity = authenticator.authenticate(solutionName, bearer == null ? null : String.valueOf(bearer));
		}
		catch (McpAuthenticationException e)
		{
			return error(e.getMessage());
		}

		try
		{
			Object[] arguments = toPositionalArguments(tool, request.arguments());
			Object returned = executor.execute(identity.toClientKey(solutionName), identity, tool.getScopeName(),
				tool.getFunctionName(), arguments);

			return McpSchema.CallToolResult.builder()
				.addTextContent(McpResults.describe(returned))
				.isError(Boolean.FALSE)
				.build();
		}
		catch (Exception e)
		{
			McpRuntime.log.warn("mcp: tool '" + tool.getName() + "' failed", e);
			return error(e.getMessage() == null ? e.getClass().getName() : e.getMessage());
		}
	}

	private static McpSchema.CallToolResult error(String message)
	{
		return McpSchema.CallToolResult.builder().addTextContent(message).isError(Boolean.TRUE).build();
	}

	/**
	 * Maps the named arguments of the protocol onto the positional arguments a scope function takes,
	 * in the order the documentation block declares. A missing optional argument becomes
	 * <code>null</code>, which is what a Servoy function sees for an argument that was not passed.
	 */
	static Object[] toPositionalArguments(McpTool tool, Map<String, Object> arguments)
	{
		List<McpTool.Parameter> parameters = tool.getParameters();
		Object[] positional = new Object[parameters.size()];

		Map<String, Object> supplied = arguments == null ? Collections.emptyMap() : arguments;
		for (int i = 0; i < parameters.size(); i++)
		{
			positional[i] = supplied.get(parameters.get(i).getName());
		}

		return positional;
	}

	/**
	 * Drops the built server, so the next request scans the solution again.
	 */
	public synchronized void invalidate()
	{
		registry.invalidate();
		transport = null;
	}

	static
	{
		// touched so that a missing application server fails loudly at class load rather than on the
		// first request, where it would look like a protocol problem
		if (!ApplicationServerRegistry.exists()) Debug.warn("mcp: no application server registered yet");
	}
}
