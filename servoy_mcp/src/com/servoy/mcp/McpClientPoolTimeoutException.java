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

import java.time.Duration;

/**
 * Thrown by {@link McpRuntime#getClient(McpClientKey)} when the client pool stayed exhausted for the
 * whole configured wait: the per-key limit or the overall client max was reached and no client was
 * returned in time.
 *
 * <p>The message is meant for the agent that made the call, so it carries no pool key, user, tenant
 * or limit. Those are in the server log.</p>
 *
 * @author Servoy
 */
@SuppressWarnings("nls")
public class McpClientPoolTimeoutException extends Exception
{
	private static final long serialVersionUID = 1L;

	private final Duration timeout;

	public McpClientPoolTimeoutException(Duration timeout, Throwable cause)
	{
		super("Timed out after " + describe(timeout) + " waiting for an available Servoy client from the pool. " +
			"The server is busy; please retry.", cause);
		this.timeout = timeout;
	}

	/**
	 * How long the caller waited before giving up.
	 */
	public Duration getTimeout()
	{
		return timeout;
	}

	private static String describe(Duration timeout)
	{
		long millis = timeout.toMillis();
		if (millis % 1000 == 0)
		{
			long seconds = millis / 1000;
			return seconds + (seconds == 1 ? " second" : " seconds");
		}
		return millis + " milliseconds";
	}
}
