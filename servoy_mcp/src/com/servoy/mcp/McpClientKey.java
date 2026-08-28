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

/**
 * Identifies a pooled headless client for the MCP plugin.
 *
 * <p>Unlike the rest_ws plugin, which keys its pool on the solution name alone, an MCP client is
 * logged in as a specific user with a specific tenant. Reusing such a client for a different user
 * would leak that user's identity, permissions and tenant filtering into the next request, so the
 * user and tenant are part of the key.</p>
 *
 * @author Servoy
 */
public final class McpClientKey
{
	private static final String SEPARATOR = "|"; //$NON-NLS-1$

	private final String solutionName;
	private final String userUid;
	private final String tenant;

	public McpClientKey(String solutionName, String userUid, String tenant)
	{
		if (solutionName == null) throw new IllegalArgumentException("solutionName cannot be null"); //$NON-NLS-1$
		this.solutionName = solutionName;
		this.userUid = userUid == null ? "" : userUid; //$NON-NLS-1$
		this.tenant = tenant == null ? "" : tenant; //$NON-NLS-1$
	}

	public String getSolutionName()
	{
		return solutionName;
	}

	public String getUserUid()
	{
		return userUid;
	}

	public String getTenant()
	{
		return tenant;
	}

	/**
	 * The key as used by the client pool. The solution name is the first segment so that it can be
	 * recovered when the pool needs to (re)open the solution on a client.
	 */
	public String toPoolKey()
	{
		return solutionName + SEPARATOR + userUid + SEPARATOR + tenant;
	}

	/**
	 * Recovers the solution name from a pool key produced by {@link #toPoolKey()}.
	 */
	public static String solutionNameFromPoolKey(String poolKey)
	{
		int idx = poolKey.indexOf(SEPARATOR);
		return idx < 0 ? poolKey : poolKey.substring(0, idx);
	}

	@Override
	public String toString()
	{
		return toPoolKey();
	}

	@Override
	public int hashCode()
	{
		return toPoolKey().hashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (!(obj instanceof McpClientKey)) return false;
		return toPoolKey().equals(((McpClientKey)obj).toPoolKey());
	}
}
