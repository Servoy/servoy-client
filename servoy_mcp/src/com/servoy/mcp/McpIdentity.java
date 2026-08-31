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

import java.util.Arrays;
import java.util.Comparator;

import org.json.JSONObject;

import com.servoy.j2db.ClientLogin;
import com.servoy.j2db.Credentials;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServer;
import com.servoy.j2db.util.Utils;

/**
 * Who a tool call runs as.
 *
 * <p><b>There is no authentication code here.</b> The bearer token the agent presents is never read,
 * decoded or verified by this server - it is passed through, untouched, to the solution's own
 * authenticator module, which answers the only question that matters: which user is this, what may
 * they do, and which tenant are they in. A deployment that mints its tokens in Servoy Cloud, one
 * that uses an external identity provider and one that keeps a table of API keys all work the same
 * way, because none of that is our business.</p>
 *
 * <p>The token is carried in <code>Authorization: Bearer</code> rather than as a tool argument. That
 * keeps it out of the tool schemas the agent sees, and out of whatever the agent logs.</p>
 *
 * <p>The authenticator is reached the way the login page reaches it: an
 * {@link IApplicationServer#login} with <b>no client id</b>. That is deliberate and not incidental -
 * on that path, and only on that path, the server runs the authenticator in a throw-away client and
 * hands back the tenant along with the user (see <code>ApplicationServer.login</code>, where
 * <code>getRawTenantValue()</code> is read). Passing a client id instead would apply the identity to
 * that client and return nothing, which is no use here: the tenant has to be known <i>before</i> a
 * client is borrowed, because it is part of the pool key.</p>
 *
 * @author Servoy
 */
@SuppressWarnings("nls")
public final class McpIdentity
{
	/**
	 * The property the bearer token is handed to the authenticator under.
	 *
	 * <p>The authenticator's <code>onOpen</code> receives it as the second argument, the same shape the
	 * NG login page uses: <code>onOpen(arg, parameters)</code>, so a solution reads it as
	 * <code>parameters.userToken</code>.</p>
	 */
	static final String USER_TOKEN = "userToken";

	private final String userUid;
	private final String userName;
	private final String[] permissions;
	private final String[] tenants;

	McpIdentity(String userUid, String userName, String[] permissions, String[] tenants)
	{
		this.userUid = userUid;
		this.userName = userName;
		this.permissions = permissions;
		this.tenants = tenants;
	}

	/**
	 * Asks the solution's authenticator who the bearer of this token is.
	 *
	 * @param solutionName the MCP solution being called
	 * @param bearerToken the token the agent presented, passed on as-is
	 * @return the identity to run the tool as
	 * @throws McpAuthenticationException when no token was presented, the solution has no
	 *         authenticator, or the authenticator declined the token
	 */
	public static McpIdentity authenticate(String solutionName, String bearerToken) throws McpAuthenticationException
	{
		return authenticate(solutionName, bearerToken, ApplicationServerRegistry.get().getLocalRepository(),
			ApplicationServerRegistry.getService(IApplicationServer.class));
	}

	/**
	 * The same, told where to look instead of reaching for the running server.
	 *
	 * <p>The two collaborators are handed in rather than fetched from
	 * {@link ApplicationServerRegistry} so that this can be exercised without one. That is the only
	 * reason the seam exists, and it is worth it: what happens to a bearer token is the part of this
	 * server most worth testing, and it is otherwise reachable only by starting a developer.</p>
	 */
	static McpIdentity authenticate(String solutionName, String bearerToken, IRepository repository, IApplicationServer server)
		throws McpAuthenticationException
	{
		if (bearerToken == null || bearerToken.trim().length() == 0)
		{
			throw new McpAuthenticationException("No bearer token was presented");
		}

		Solution authenticator = findAuthenticator(solutionName, repository);
		if (authenticator == null)
		{
			// not the caller's fault, but there is no way to answer them either
			throw new McpAuthenticationException(
				"Solution '" + solutionName + "' has no authenticator module, so no tool call can be authorized");
		}

		ClientLogin login;
		try
		{
			JSONObject credentials = new JSONObject();
			credentials.put(USER_TOKEN, bearerToken.trim());

			// no client id: see the class comment - this is what makes the tenant come back
			Credentials request = new Credentials(null, authenticator.getName(), null, credentials.toString());

			login = server.login(request);
		}
		catch (Exception e)
		{
			McpRuntime.log.error("mcp: the authenticator '" + authenticator.getName() + "' failed", e);
			throw new McpAuthenticationException("The authenticator could not be reached");
		}

		if (login == null || login.getUserUid() == null)
		{
			// the authenticator looked at the token and said no; its own message, if it set one, is the
			// only explanation available and is worth passing on
			String reason = login == null ? null : login.getJsReturn();
			throw new McpAuthenticationException(
				"The authenticator did not accept the bearer token" + (reason == null ? "" : ": " + reason));
		}

		return new McpIdentity(login.getUserUid(), login.getUserName(), login.getUserGroups(), login.getTenantValues());
	}

	/**
	 * The authenticator module of a solution, or <code>null</code> when it has none.
	 *
	 * <p>The same walk <code>AuthenticatorManager.findAuthenticator</code> does for the login page. It
	 * is repeated here rather than shared because that one lives in <code>servoy_ngclient</code>, and
	 * the server has no business depending on a client.</p>
	 */
	static Solution findAuthenticator(String solutionName, IRepository repository)
	{
		try
		{
			Solution solution = (Solution)repository.getActiveRootObject(solutionName, IRepository.SOLUTIONS);
			if (solution == null) return null;

			for (String moduleName : Utils.getTokenElements(solution.getModulesNames(), ",", true))
			{
				Solution module = (Solution)repository.getActiveRootObject(moduleName, IRepository.SOLUTIONS);
				if (module != null && module.getSolutionType() == SolutionMetaData.AUTHENTICATOR)
				{
					return module;
				}
			}
		}
		catch (Exception e)
		{
			McpRuntime.log.error("mcp: could not look for the authenticator of '" + solutionName + "'", e);
		}

		return null;
	}

	public String getUserUid()
	{
		return userUid;
	}

	public String getUserName()
	{
		return userName;
	}

	public String[] getPermissions()
	{
		return permissions;
	}

	/**
	 * Every tenant the authenticator put the user in, or <code>null</code> when it named none.
	 *
	 * <p>All of them, not one: <code>setTenantValue</code> takes the whole array and filters on the
	 * lot, which is what an NG client does with the same values.</p>
	 */
	public String[] getTenants()
	{
		return tenants;
	}

	/**
	 * The pool key this identity maps onto. Two calls by the same user against the same solution and
	 * tenants reuse one client; two different users never do.
	 */
	public McpClientKey toClientKey(String solutionName)
	{
		return new McpClientKey(solutionName, userUid, tenantKey());
	}

	/**
	 * A stable rendering of the tenants for use in the pool key. Two identities with the same tenants
	 * in a different order must land on the same client, so the values are sorted.
	 */
	String tenantKey()
	{
		if (tenants == null || tenants.length == 0) return null;

		String[] sorted = tenants.clone();
		Arrays.sort(sorted, Comparator.nullsFirst(Comparator.naturalOrder()));

		return String.join(",", sorted);
	}

	@Override
	public String toString()
	{
		return "McpIdentity[" + userUid + (tenants == null ? "" : ", tenants " + tenantKey()) + "]";
	}

	/**
	 * Raised when a tool call cannot be attributed to a user.
	 */
	public static class McpAuthenticationException extends Exception
	{
		public McpAuthenticationException(String message)
		{
			super(message);
		}
	}
}
