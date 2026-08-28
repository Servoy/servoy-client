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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.servoy.j2db.server.shared.IHeadlessClient;

/**
 * The tools one solution publishes.
 *
 * <p>Scanned once, on the first request for that solution, and kept. Scanning needs a client with
 * the solution loaded, so doing it at server startup would mean opening a client for every deployed
 * MCP solution whether or not anyone ever asks for it - and reporting any failure far away from
 * whoever caused it. On first use the failure is answered straight to the caller.</p>
 *
 * <p>A solution that changes needs a restart for its tools to change with it. That is deliberate:
 * the alternative is watching for redeploys and rebuilding servers underneath live sessions, which
 * buys little and can surprise a running agent.</p>
 *
 * <p>Scanning borrows a client under {@link #SCAN_USER}, a key belonging to no real user. That
 * client only reads the solution model, never runs a tool, so it needs no login and cannot leak into
 * anyone's session.</p>
 *
 * @author Servoy
 */
public class McpToolRegistry
{
	/**
	 * The pseudo user the scanning client is pooled under. Real users are identified by the uid in
	 * their token, and a uid can never take this shape, so the scanning client is never handed out.
	 */
	static final String SCAN_USER = "$scan"; //$NON-NLS-1$

	/**
	 * Where the raw list of marked functions comes from.
	 *
	 * <p>Normally a client with the solution loaded. Naming it lets a test hand over a list directly,
	 * so what this class does with that list - the schema check, the name clash, the caching - can be
	 * exercised without a solution being open anywhere.</p>
	 */
	interface ToolSource
	{
		List<McpTool> scan() throws Exception;
	}

	private final McpRuntime runtime;
	private final String solutionName;
	private final ToolSource toolSource;

	/** Tool name to tool. Null until the first successful scan. */
	private volatile Map<String, McpTool> tools;

	public McpToolRegistry(McpRuntime runtime, String solutionName)
	{
		this.runtime = runtime;
		this.solutionName = solutionName;
		this.toolSource = this::scanThroughClient;
	}

	McpToolRegistry(McpRuntime runtime, String solutionName, ToolSource toolSource)
	{
		this.runtime = runtime;
		this.solutionName = solutionName;
		this.toolSource = toolSource;
	}

	/**
	 * All published tools, scanning first if that has not happened yet.
	 */
	public Collection<McpTool> getTools() throws Exception
	{
		return load().values();
	}

	/**
	 * Looks a tool up by its published name.
	 *
	 * @return the tool, or <code>null</code> when no tool goes by that name
	 */
	public McpTool find(String toolName) throws Exception
	{
		if (toolName == null) return null;
		return load().get(toolName);
	}

	/**
	 * Drops the cached tools, so the next request scans again.
	 */
	public void invalidate()
	{
		tools = null;
	}

	private Map<String, McpTool> load() throws Exception
	{
		Map<String, McpTool> loaded = tools;
		if (loaded != null) return loaded;

		synchronized (this)
		{
			if (tools != null) return tools;

			tools = scan();
			return tools;
		}
	}

	/**
	 * Reads the marked functions out of a client with the solution loaded.
	 */
	private List<McpTool> scanThroughClient() throws Exception
	{
		McpClientKey key = new McpClientKey(solutionName, SCAN_USER, null);

		IHeadlessClient client = runtime.getClient(key);
		try
		{
			return McpToolScanner.scan(client);
		}
		finally
		{
			runtime.releaseClient(key, client);
		}
	}

	private Map<String, McpTool> scan() throws Exception
	{
		List<McpTool> found = toolSource.scan();

		Map<String, McpTool> byName = new LinkedHashMap<String, McpTool>();
		for (McpTool tool : found)
		{
			try
			{
				// building the schema now means an unpublishable tool is reported once, here,
				// rather than on every listing
				McpToolSchema.buildInputSchema(tool);
			}
			catch (McpToolSchema.UnsupportedTypeException e)
			{
				McpRuntime.log.error("mcp: not publishing a tool - {}", e.getMessage()); //$NON-NLS-1$
				continue;
			}

			McpTool clash = byName.put(tool.getName(), tool);
			if (clash != null)
			{
				// the tool name carries the scope, so this needs two identical scope and function
				// names in one solution - report it rather than silently dropping one
				McpRuntime.log.warn("mcp: two functions map onto tool name '{}', keeping the last one", //$NON-NLS-1$
					tool.getName());
			}
		}

		return Collections.unmodifiableMap(byName);
	}
}
