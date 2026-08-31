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

import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.ClientInfo;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.plugins.ClientPluginAccessProvider;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.server.shared.IHeadlessClient;

/**
 * Runs a scope function in a pooled headless client, as the user the agent is acting for.
 *
 * <p>This is where the identity the authenticator returned stops being metadata and starts having
 * effect. A borrowed client is anonymous until told otherwise: permission checks would pass that
 * should not, <code>security.getUserUID()</code> would answer nothing, and - the dangerous one -
 * queries would come back unfiltered by tenant. So the identity is applied before every call.</p>
 *
 * <p>What is applied mirrors <code>NGClientWebsocketSession.setUserId()</code>, which does the same
 * thing when an NG client connects:</p>
 *
 * <pre>
 * ClientInfo ci = client.getClientInfo();
 * ci.setUserUid(identity.getUserUid());
 * ci.setUserName(identity.getUserName());
 * ci.setUserGroups(identity.getPermissions());
 * setTenantValue(identity.getTenants());
 * </pre>
 *
 * <p>It is applied on every call rather than once at creation. Setting four fields is cheap, it is
 * idempotent, and it removes a whole class of question about what state a pooled client came back
 * in.</p>
 *
 * @author Servoy
 */
public class McpToolExecutor
{
	private final McpRuntime runtime;

	public McpToolExecutor(McpRuntime runtime)
	{
		this.runtime = runtime;
	}

	/**
	 * Executes <code>scopes.&lt;scopeName&gt;.&lt;functionName&gt;(arguments)</code> as the given user.
	 *
	 * @param key identifies which client to use - solution, user and tenants
	 * @param identity the user the authenticator said this call is for
	 * @param scopeName the scope the function lives in, <code>null</code> means <code>globals</code>
	 * @param functionName the scope function to call
	 * @param arguments the arguments to pass, may be <code>null</code>
	 * @return whatever the scope function returned
	 */
	public Object execute(McpClientKey key, McpIdentity identity, String scopeName, String functionName, Object[] arguments)
		throws Exception
	{
		if (functionName == null) throw new IllegalArgumentException("functionName cannot be null"); //$NON-NLS-1$

		String context = toContext(scopeName);

		IHeadlessClient client = runtime.getClient(key);
		try
		{
			applyIdentity(client, identity);

			if (McpRuntime.log.isDebugEnabled())
			{
				McpRuntime.log.debug("executing {}.{} for {}", context, functionName, key); //$NON-NLS-1$
			}
			return client.getPluginAccess().executeMethod(context, functionName, arguments, false);
		}
		finally
		{
			runtime.releaseClient(key, client);
		}
	}

	/**
	 * Puts the user, the permissions and the tenants onto the client session.
	 *
	 * <p>Touching the client is done inside {@link IApplication#invokeAndWait(Runnable)}, the same
	 * rule that applies to reading the solution model - see {@link McpToolScanner}.</p>
	 */
	static void applyIdentity(IHeadlessClient client, McpIdentity identity) throws Exception
	{
		IClientPluginAccess access = client.getPluginAccess();
		if (!(access instanceof ClientPluginAccessProvider))
		{
			throw new IllegalStateException("Cannot apply the identity: unexpected plugin access implementation " + //$NON-NLS-1$
				(access == null ? "null" : access.getClass().getName())); //$NON-NLS-1$
		}

		final IApplication application = ((ClientPluginAccessProvider)access).getApplication();
		final Exception[] failure = new Exception[1];

		application.invokeAndWait(new Runnable()
		{
			public void run()
			{
				try
				{
					ClientInfo clientInfo = application.getClientInfo();
					clientInfo.setUserUid(identity.getUserUid());
					clientInfo.setUserName(identity.getUserName());

					String[] permissions = identity.getPermissions();
					if (permissions != null) clientInfo.setUserGroups(permissions);

					Object[] tenants = identity.getTenants();
					if (tenants != null && tenants.length > 0)
					{
						// the whole array: a user may belong to several tenants and the filter covers
						// all of them, which is what the NG client does with the same token.
						// The solution comes from the flattened solution, the way JSSecurity does it.
						application.getFoundSetManager().setTenantValue(
							application.getFlattenedSolution().getSolution(), tenants);
					}
				}
				catch (Exception e)
				{
					failure[0] = e;
				}
			}
		});

		if (failure[0] != null) throw failure[0];
	}

	/**
	 * Turns a scope name into the execution context {@link IClientPluginAccess#executeMethod} wants.
	 *
	 * <p>The context is resolved as a <b>form</b> name first; a scope has to be named explicitly with
	 * the <code>scopes.</code> prefix, otherwise the call fails with "did not resolve to a form".
	 * <code>rest_ws</code> deals with the same thing in <code>RestWSServlet.getContext()</code>,
	 * where it looks the name up as a form and falls back to the prefixed scope. Here the name is
	 * always a scope, because that is where tools are discovered, so the prefix is unconditional.</p>
	 *
	 * @param scopeName the scope, <code>null</code> meaning <code>globals</code>
	 */
	static String toContext(String scopeName)
	{
		String scope = scopeName == null || scopeName.trim().length() == 0 ? ScriptVariable.GLOBAL_SCOPE : scopeName.trim();

		if (scope.startsWith(ScriptVariable.SCOPES_DOT_PREFIX)) return scope;

		return ScriptVariable.SCOPES_DOT_PREFIX + scope;
	}
}
