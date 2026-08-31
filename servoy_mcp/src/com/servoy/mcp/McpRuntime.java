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

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.servoy.j2db.persistence.IDeveloperRepository;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.headlessclient.HeadlessClientFactory;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IHeadlessClient;
import com.servoy.j2db.util.Debug;

/**
 * The MCP server, for the lifetime of the application server.
 *
 * <p>It lives in core rather than in a plugin because the tools it publishes are the solution's own
 * business logic, and reaching that needs the application server itself: the solution model, the
 * client pool, the security context. It starts and stops with the server, roughly the way a batch
 * processor does.</p>
 *
 * <p>One endpoint serves every MCP solution the server hosts, addressed by name:
 * <code>/servoy-service/mcp/&lt;solution&gt;</code>. There is no configuration - a solution declares
 * itself by being of type {@link SolutionMetaData#MCP_SERVICE}, so nothing has to be set up twice,
 * and one server can host as many as it likes. <code>rest_ws</code> takes the same approach: the
 * solution comes from the URL and the plugin has no solution setting at all.</p>
 *
 * <p><b>The client pool is unlimited on purpose.</b> How many concurrent tool calls a deployment
 * wants to run, and what that costs in licences, is the deployment's business. Imposing a ceiling
 * here would only turn a decision someone else should make into a failure they cannot see.</p>
 *
 * @author Servoy
 */
@SuppressWarnings("nls")
public class McpRuntime
{
	/** The alias the endpoint is registered under: /servoy-service/mcp/... */
	public static final String WEBSERVICE_NAME = "mcp";

	static final Logger log = LoggerFactory.getLogger("servoy.mcp");

	private static final String[] SOLUTION_OPEN_METHOD_ARGS = new String[] { "mcp_server" };

	/**
	 * Appended to the solution open arguments in developer.
	 *
	 * <p>Without it the developer hands out a single shared debug headless client: it can only load
	 * the active solution or one of its modules, and creating a second shuts the first down - which
	 * makes the per-user pool impossible to exercise. With it, any solution in the workspace can be
	 * opened and several clients can live at once, at the cost of not being able to break inside a
	 * tool.</p>
	 */
	private static final String NO_DEBUG = "nodebug";

	private static volatile McpRuntime instance;

	private final GenericKeyedObjectPool<String, IHeadlessClient> clientPool;
	private final Map<String, McpSolutionServer> solutionServers = new ConcurrentHashMap<>();

	/**
	 * Where a client comes from.
	 *
	 * <p>Handed to the runtime rather than called directly so that the pool can be exercised without
	 * an application server behind it. In production there is one implementation and it is the line
	 * below; in a test it is whatever the test needs a client to be.</p>
	 */
	interface ClientSource
	{
		IHeadlessClient open(String solutionName) throws Exception;
	}

	private final ClientSource clientSource;

	private McpRuntime()
	{
		this(solutionName -> HeadlessClientFactory.createHeadlessClient(solutionName, solutionOpenArguments()));
	}

	McpRuntime(ClientSource clientSource)
	{
		this.clientSource = clientSource;

		GenericKeyedObjectPoolConfig<IHeadlessClient> config = new GenericKeyedObjectPoolConfig<>();
		// no ceiling: see the class comment
		config.setMaxTotalPerKey(-1);
		config.setMaxIdlePerKey(-1);
		config.setBlockWhenExhausted(false);

		clientPool = new GenericKeyedObjectPool<>(new BaseKeyedPooledObjectFactory<String, IHeadlessClient>()
		{
			@Override
			public IHeadlessClient create(String key) throws Exception
			{
				String solutionName = McpClientKey.solutionNameFromPoolKey(key);
				if (log.isDebugEnabled()) log.debug("creating a client for '{}'", key);
				return McpRuntime.this.clientSource.open(solutionName);
			}

			@Override
			public PooledObject<IHeadlessClient> wrap(IHeadlessClient value)
			{
				return new DefaultPooledObject<>(value);
			}

			@Override
			public boolean validateObject(String key, PooledObject<IHeadlessClient> pooledObject)
			{
				return pooledObject.getObject().isValid();
			}

			@Override
			public void destroyObject(String key, PooledObject<IHeadlessClient> pooledObject) throws Exception
			{
				if (log.isDebugEnabled()) log.debug("destroying the client for '{}'", key);
				try
				{
					pooledObject.getObject().shutDown(true);
				}
				catch (Exception e)
				{
					Debug.error(e);
				}
			}
		});
		clientPool.setTestOnBorrow(true);
	}

	/**
	 * The running instance, created on first use.
	 */
	public static McpRuntime getInstance()
	{
		McpRuntime running = instance;
		if (running != null) return running;

		synchronized (McpRuntime.class)
		{
			if (instance == null) instance = new McpRuntime();
			return instance;
		}
	}

	/**
	 * Shuts down every pooled client. Called when the application server stops.
	 */
	public static void shutDown()
	{
		synchronized (McpRuntime.class)
		{
			if (instance == null) return;

			try
			{
				instance.clientPool.close();
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
			instance.solutionServers.clear();
			instance = null;
		}
	}

	/**
	 * The MCP server for one solution, created on first request for it.
	 *
	 * @param solutionName the solution named in the request path
	 * @return the server, or <code>null</code> when no such solution is deployed or it is not of
	 *         type {@link SolutionMetaData#MCP_SERVICE}
	 */
	public McpSolutionServer getSolutionServer(String solutionName)
	{
		if (solutionName == null || solutionName.trim().length() == 0) return null;

		String name = solutionName.trim();

		McpSolutionServer existing = solutionServers.get(name);
		if (existing != null) return existing;

		if (!isMcpSolution(name)) return null;

		return solutionServers.computeIfAbsent(name, key -> new McpSolutionServer(this, key));
	}

	/**
	 * Whether a deployed solution of that name declares itself as an MCP service.
	 *
	 * <p>The type is the declaration: nothing else marks a solution as reachable over MCP, so a
	 * solution that was never meant to be exposed cannot be reached by guessing its name.</p>
	 */
	boolean isMcpSolution(String solutionName)
	{
		try
		{
			RootObjectMetaData metaData = ApplicationServerRegistry.get().getLocalRepository().getRootObjectMetaData(
				solutionName, IRepository.SOLUTIONS);

			return metaData instanceof SolutionMetaData &&
				(((SolutionMetaData)metaData).getSolutionType() & SolutionMetaData.MCP_SERVICE) != 0;
		}
		catch (Exception e)
		{
			log.warn("mcp: could not look up solution '" + solutionName + "'", e);
			return false;
		}
	}

	/**
	 * Borrows a client for the given key, creating one when the pool has none available.
	 */
	public IHeadlessClient getClient(McpClientKey key) throws Exception
	{
		return clientPool.borrowObject(key.toPoolKey());
	}

	/**
	 * Returns a client to the pool.
	 *
	 * <p>The solution is not reloaded, unlike <code>rest_ws</code>. Its pool is keyed on solution
	 * alone, so a client could be handed to a different user and the reload is what keeps that safe.
	 * Here the key carries the user and the tenants, so a client can only ever go back to the same
	 * user - which is both safe and much faster for an agent making several calls in a row.</p>
	 */
	public void releaseClient(McpClientKey key, IHeadlessClient client)
	{
		try
		{
			clientPool.returnObject(key.toPoolKey(), client);
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
	}

	/**
	 * Forgets everything cached for one solution, so the next request scans it again.
	 *
	 * <p>Called whenever the solution changes underneath a running server: on import, and in the
	 * developer whenever a scope function is saved. A no-op when no MCP server has been asked for
	 * yet - callers are on paths that have nothing to do with MCP, and must not bring the runtime
	 * to life just to tell it something.</p>
	 *
	 * <p>The server is dropped rather than reset, which re-checks the solution type as well: a
	 * solution that stopped being an MCP service stops answering. Pooled clients go too, since each
	 * one still has the previous version of the solution open. Clients that are out on loan at this
	 * moment are not reached - they finish the call they are in, and are stale until returned.</p>
	 *
	 * @param solutionName the solution that changed
	 */
	public static void invalidateSolution(String solutionName)
	{
		McpRuntime running = instance;
		if (running != null) running.invalidate(solutionName);
	}

	/**
	 * The same, on one runtime rather than on whichever one is running.
	 */
	void invalidate(String solutionName)
	{
		if (solutionName == null) return;

		String name = solutionName.trim();
		if (name.length() == 0) return;

		McpSolutionServer server = solutionServers.remove(name);
		flushCachedSolution(name);

		int discarded = 0;
		for (String poolKey : clientPool.getKeys())
		{
			if (!name.equals(McpClientKey.solutionNameFromPoolKey(poolKey))) continue;

			try
			{
				clientPool.clear(poolKey);
				discarded++;
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}

		if (server != null || discarded > 0)
		{
			log.info("mcp: '{}' invalidated, it will be scanned again on the next request", name);
		}
	}

	/**
	 * Drops the repository's own copy of one solution, so it is read again from where it lives.
	 *
	 * <p>Dropping the tool list is not enough on its own. The scan reads the solution model through a
	 * client, and the repository hands every new client the same loaded solution until that copy is
	 * flushed - so a brand new client still sees the version that was current when the first one was
	 * made. An import flushes the whole repository anyway; a scope function saved in the developer
	 * never reaches it, which is what this is for.</p>
	 *
	 * <p>The solution's modules go too. A module is a root object of its own, cached separately, but
	 * it is edited like any other part of the solution and its scopes carry tools just the same, so
	 * flushing only the solution named in the URL would leave half the tools stale.</p>
	 *
	 * <p>Named root objects rather than {@code flushAllCachedData()}: this runs on every save of a
	 * script method in the developer, and emptying the repository each time would be felt across the
	 * IDE. The module list is read before anything is flushed, so it is the list as last loaded - a
	 * module added and not yet seen is missed, but adding one is not a quiet event.</p>
	 */
	private static void flushCachedSolution(String solutionName)
	{
		// asking the registry for the server blocks until one is registered, and there is nothing to
		// flush before that anyway
		if (!ApplicationServerRegistry.exists()) return;

		try
		{
			IRepository repository = ApplicationServerRegistry.get().getLocalRepository();
			if (!(repository instanceof IDeveloperRepository)) return;

			IDeveloperRepository developerRepository = (IDeveloperRepository)repository;

			Set<String> names = new LinkedHashSet<>();
			names.add(solutionName);

			IRootObject rootObject = repository.getActiveRootObject(solutionName, IRepository.SOLUTIONS);
			if (rootObject instanceof Solution)
			{
				String modules = ((Solution)rootObject).getModulesNames();
				if (modules != null)
				{
					for (String module : modules.split(",")) //$NON-NLS-1$
					{
						String trimmed = module.trim();
						if (trimmed.length() > 0) names.add(trimmed);
					}
				}
			}

			for (String name : names)
			{
				RootObjectMetaData metaData = repository.getRootObjectMetaData(name, IRepository.SOLUTIONS);
				if (metaData != null) developerRepository.flushRootObject(metaData.getRootObjectUuid());
			}
		}
		catch (Exception e)
		{
			log.warn("mcp: could not flush the cached solution '{}'", solutionName, e); //$NON-NLS-1$
		}
	}

	/**
	 * Forgets everything cached for every solution.
	 *
	 * <p>What an import calls. Being coarse is deliberate: an import brings in modules as well, and
	 * a module carries tools that belong to the solution that includes it, so invalidating only the
	 * names that arrived would leave the including solution serving a stale list. Imports are rare
	 * and the next request simply scans again.</p>
	 */
	public static void invalidateAll()
	{
		McpRuntime running = instance;
		if (running != null) running.invalidateEverything();
	}

	/**
	 * The same, on one runtime rather than on whichever one is running.
	 */
	void invalidateEverything()
	{
		for (String solutionName : solutionServers.keySet().toArray(new String[0]))
		{
			invalidate(solutionName);
		}
	}

	static String[] solutionOpenArguments()
	{
		if (!ApplicationServerRegistry.get().isDeveloperStartup()) return SOLUTION_OPEN_METHOD_ARGS;

		String[] arguments = new String[SOLUTION_OPEN_METHOD_ARGS.length + 1];
		System.arraycopy(SOLUTION_OPEN_METHOD_ARGS, 0, arguments, 0, SOLUTION_OPEN_METHOD_ARGS.length);
		arguments[arguments.length - 1] = NO_DEBUG;
		return arguments;
	}
}
