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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * One JSON-RPC call, put through a solution's MCP server the way an agent would send it.
 *
 * <p>No socket is involved, and no server is deployed: the call is handed to an
 * {@link McpSolutionServer} directly. What is on trial is the protocol - that a real
 * <code>tools/list</code> comes back listing real tools, that a <code>tools/call</code> reaches the
 * function and returns what it returned - and none of that becomes more true for having travelled
 * over TCP.</p>
 *
 * <p>The request and response are dynamic proxies rather than hand-written stubs. The servlet API is
 * wide and the SDK's transport touches a small part of it; a proxy answers what it is asked and
 * returns nothing for the rest, which is both shorter and less likely to lie than several hundred
 * lines of generated overrides.</p>
 *
 * @author Servoy
 */
final class McpCall
{
	private final String body;
	private final int status;

	private McpCall(String body, int status)
	{
		this.body = body;
		this.status = status;
	}

	/** The response body, as the agent would receive it. */
	String body()
	{
		return body;
	}

	/** The HTTP status the server set. */
	int status()
	{
		return status;
	}

	/** Whether the body mentions the given text - enough for what these tests assert. */
	boolean contains(String text)
	{
		return body != null && body.contains(text);
	}

	/**
	 * Asks the solution for its catalogue of tools.
	 */
	static McpCall toolsList(McpSolutionServer server) throws Exception
	{
		return post(server, "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}", null); //$NON-NLS-1$
	}

	/**
	 * Calls one tool, on behalf of whoever the token belongs to.
	 *
	 * @param token the bearer token, or <code>null</code> to send none at all
	 */
	static McpCall callTool(McpSolutionServer server, String toolName, String argumentsJson, String token) throws Exception
	{
		String payload = "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/call\",\"params\":{\"name\":\"" + //$NON-NLS-1$
			toolName + "\",\"arguments\":" + (argumentsJson == null ? "{}" : argumentsJson) + "}}"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		return post(server, payload, token);
	}

	/**
	 * Sends a raw JSON-RPC payload to one solution's server.
	 */
	@SuppressWarnings("nls")
	static McpCall post(McpSolutionServer server, String payload, String token) throws Exception
	{
		Map<String, String> headers = new HashMap<String, String>();
		// SDK 2.0.1 refuses anything that does not ask for both; 1.1.2 did not
		headers.put("accept", "application/json, text/event-stream");
		headers.put("content-type", "application/json");
		if (token != null) headers.put("authorization", "Bearer " + token);

		StringWriter written = new StringWriter();
		int[] status = new int[] { HttpServletResponse.SC_OK };

		// the transport matches on the URI ending with the solution's endpoint, so a call meant for
		// one solution cannot be answered by another
		String uri = "/servoy-service/" + McpRuntime.WEBSERVICE_NAME + "/" + server.getSolutionName();

		HttpServletRequest request = request(uri, payload, headers);
		HttpServletResponse response = response(written, status);

		server.service(request, response);

		return new McpCall(written.toString(), status[0]);
	}

	@SuppressWarnings("nls")
	private static HttpServletRequest request(final String uri, final String payload, final Map<String, String> headers)
	{
		final BufferedReader reader = new BufferedReader(new StringReader(payload));

		// the SDK's transport reads the body as a stream, not as a reader; offering only getReader()
		// leaves it with a null stream, and every call dies before it is even parsed
		final ByteArrayInputStream body = new ByteArrayInputStream(payload.getBytes(StandardCharsets.UTF_8));
		final ServletInputStream stream = new ServletInputStream()
		{
			@Override
			public int read()
			{
				return body.read();
			}

			@Override
			public boolean isFinished()
			{
				return body.available() == 0;
			}

			@Override
			public boolean isReady()
			{
				return true;
			}

			@Override
			public void setReadListener(ReadListener listener)
			{
				// read synchronously; there is nothing to notify
			}
		};

		InvocationHandler handler = new InvocationHandler()
		{
			@Override
			public Object invoke(Object proxy, Method method, Object[] arguments) throws Throwable
			{
				switch (method.getName())
				{
					case "getMethod" :
						return "POST";
					case "getPathInfo" :
					case "getRequestURI" :
					case "getServletPath" :
						return uri;
					case "getHeader" :
						return headers.get(String.valueOf(arguments[0]).toLowerCase());
					case "getHeaders" :
					{
						String value = headers.get(String.valueOf(arguments[0]).toLowerCase());
						return value == null ? Collections.emptyEnumeration() : Collections.enumeration(Collections.singletonList(value));
					}
					case "getHeaderNames" :
						return Collections.enumeration(headers.keySet());
					case "getContentType" :
						return headers.get("content-type");
					case "getCharacterEncoding" :
						return "UTF-8";
					case "getReader" :
						return reader;
					case "getInputStream" :
						return stream;
					case "getProtocol" :
						return "HTTP/1.1";
					case "isAsyncSupported" :
						return Boolean.FALSE;
					case "getContentLength" :
						return Integer.valueOf(payload.length());
					case "getContentLengthLong" :
						return Long.valueOf(payload.length());
					case "getAttributeNames" :
						return Collections.<String> emptyEnumeration();
					case "toString" :
						return "McpCall request " + uri;
					default :
						return emptyFor(method);
				}
			}
		};

		return (HttpServletRequest)Proxy.newProxyInstance(McpCall.class.getClassLoader(),
			new Class[] { HttpServletRequest.class }, handler);
	}

	@SuppressWarnings("nls")
	private static HttpServletResponse response(final StringWriter written, final int[] status)
	{
		final PrintWriter writer = new PrintWriter(written);

		InvocationHandler handler = new InvocationHandler()
		{
			@Override
			public Object invoke(Object proxy, Method method, Object[] arguments) throws Throwable
			{
				switch (method.getName())
				{
					case "getWriter" :
						return writer;
					case "setStatus" :
						status[0] = ((Integer)arguments[0]).intValue();
						writer.flush();
						return null;
					case "sendError" :
						status[0] = ((Integer)arguments[0]).intValue();
						if (arguments.length > 1 && arguments[1] != null) writer.write(String.valueOf(arguments[1]));
						writer.flush();
						return null;
					case "getStatus" :
						return Integer.valueOf(status[0]);
					case "isCommitted" :
						return Boolean.FALSE;
					case "flushBuffer" :
						writer.flush();
						return null;
					case "toString" :
						return "McpCall response";
					default :
						return emptyFor(method);
				}
			}
		};

		return (HttpServletResponse)Proxy.newProxyInstance(McpCall.class.getClassLoader(),
			new Class[] { HttpServletResponse.class }, handler);
	}

	/**
	 * What an unasked-for method returns: nothing, in whatever shape the signature needs.
	 */
	private static Object emptyFor(Method method)
	{
		Class< ? > type = method.getReturnType();

		if (!type.isPrimitive()) return null;
		if (type == boolean.class) return Boolean.FALSE;
		if (type == int.class) return Integer.valueOf(0);
		if (type == long.class) return Long.valueOf(0L);
		if (type == void.class) return null;

		return Integer.valueOf(0);
	}
}
