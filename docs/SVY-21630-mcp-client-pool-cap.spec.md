# Spec: SVY-21630 — MCP server: bound the client pool, make it configurable, and wire a shutdown drain

## 1. Goal
Make the headless-client pool in `McpRuntime` actually bounded, configurable and drained when the servlet stops.

Today the pool config is built and then dropped, so the pool runs on commons-pool2 defaults (8 per key, callers block forever). This ticket:

- Applies a real pool config. Four settings are read from `servoy.properties` through `com.servoy.j2db.util.Settings`. Each one is a static member of `McpRuntime`. Two of them are limits (per solution/user/tenant, and the overall client max across all keys); one is the number of idle clients kept; one is the configurable wait timeout:

  | Property | Static member in `McpRuntime` | Default |
  |---|---|---|
  | `servoy.mcpClientPool.maxClientsPerSolUsrTenant` | `MAX_CLIENTS_PER_KEY` | 100 |
  | `servoy.mcpClientPool.maxIdleClientsPerSolUsrTenant` | `MAX_IDLE_PER_KEY` | 8 |
  | `servoy.mcpClientPool.maxTotalClients` | `TOTAL_MAX_CLIENTS` | -1 (unlimited) |
  | `servoy.mcpClientPool.waitForAvailableClientTimeout` | `WAIT_FOR_AVAILABLE_CLIENT_TIMEOUT` | 10 (seconds) |

- Makes a caller that finds the pool exhausted (per-key limit or overall client max reached) wait up to the configurable timeout (`blockWhenExhausted=true` plus `maxWait`). It no longer hangs forever, and it does not fail at once.
- Reports a pool timeout to the MCP caller with a specific message ("Timed out after 10 seconds waiting for an available Servoy client from the pool. The server is busy; please retry.", where the number is the configured timeout) instead of the generic "The tool '<name>' failed. See the server log for details.". A detailed WARN stays in the server log. What exactly the caller receives in each case, and how that was verified against the code, is in section 3.0.
- Exposes the four properties on the server admin page (`ConfigServlet`). That class is in another repository and is specified and implemented there (section 3.8, companion spec).
- Adds `McpServlet.destroy()` so pooled, licensed headless clients are released when the servlet or server stops.

## 2. Background

### 2.1 Current pool behaviour (the ticket's premise is wrong)
`servoy_mcp/src/com/servoy/mcp/McpRuntime.java:112-154`:

```java
GenericKeyedObjectPoolConfig<IHeadlessClient> config = new GenericKeyedObjectPoolConfig<>();
config.setMaxTotalPerKey(-1);
config.setMaxIdlePerKey(-1);
config.setBlockWhenExhausted(false);

clientPool = new GenericKeyedObjectPool<>(new BaseKeyedPooledObjectFactory<...>() { ... });   // config NOT passed
clientPool.setTestOnBorrow(true);
```

`config` is never used (unchanged since the initial commit `9f650f6ca`, SVY-21326). The pool therefore runs on the commons-pool2 defaults.

Sources I read: the 2.4.2 sources (`servoy-extensions/servoy_shared/libsrc/commons-pool2-2.4.2-src.jar`) and a `javap` listing of the built 2.12.0 jar (`servoy_doc/target/repository/plugins/org.apache.commons.commons-pool2_2.12.0.jar`). The target platform uses 2.13.1 (`servoy-eclipse/launch_targets/com.servoy.eclipse.target.target:455-457`). The 2.13.1 sources were not read. Everything below about pool2 behaviour is from 2.4.2 and assumed unchanged; the tests assert it (open question 1).

| Setting | Actual value today | Javadoc / ticket claim | After this ticket |
|---|---|---|---|
| `maxTotalPerKey` | 8 | -1 (unbounded) | `servoy.mcpClientPool.maxClientsPerSolUsrTenant`, default 100 |
| `maxIdlePerKey` | 8 | -1 | `servoy.mcpClientPool.maxIdleClientsPerSolUsrTenant`, default 8 |
| `maxTotal` (overall client max, all keys) | -1 | n/a | `servoy.mcpClientPool.maxTotalClients`, default -1 (unlimited) |
| `blockWhenExhausted` | true | false | true (fixed) |
| `maxWait` (wait timeout) | -1 (wait forever) | n/a | `servoy.mcpClientPool.waitForAvailableClientTimeout`, default 10 seconds |
| idle eviction | disabled | none | unchanged, disabled (out of scope, section 6) |

So the 9th concurrent call for one `solution|userUid|tenant` key blocks indefinitely, with no log line. Adding a cap of 100 on top of the current code would be a no-op. The fix has to start by passing `config` to the pool constructor.

### 2.2 Pool key, call sites and how failures reach the agent
- Pool key: `McpClientKey.toPoolKey()` = `solution|userUid|tenant` (`McpClientKey.java:64-67`).
- `McpRuntime.getClient(McpClientKey)` (`McpRuntime.java:241-244`) has two callers. Both call it before their `try`, so a failed borrow needs no release:
  - `McpToolExecutor.execute` (`McpToolExecutor.java:70-92`): `getClient` at line 77, `try` at 78, releasing `finally` at 88-91. It is declared `throws Exception`, has no `catch`, and runs no thread or `Future`, so an exception from `getClient` propagates unwrapped (verified, section 3.0).
  - `McpToolRegistry.scanThroughClient` (`McpToolRegistry.java:130-143`): `getClient` at line 134, fixed scan user (`SCAN_USER = "$scan"`, line 51). It is reached from `scan()` (145-147, `toolSource.scan()`) and `load()` (113-125), which assigns `tools` only after a successful scan. A failed scan is not cached, so the next request scans again.
- Tool call path: `McpSolutionServer.call` (`McpSolutionServer.java:242-274`).
  - An authentication failure becomes an `isError` result with its own message (250-253).
  - The execution block (255-273) calls `executor.execute(...)`. Its `catch (Exception e)` (266-273) logs `WARN "mcp: tool '<name>' failed"` with the stack trace and returns the generic `isError` result through `error(...)` (276-279).
  - So today any pool failure reaches the agent as the generic message.
- Scan path: `McpSolutionServer.service` (`McpSolutionServer.java:131-141`) calls `transport()` (143-195). On the first request `transport()` calls `registry.getTools()` (line 173) inside `synchronized (this)`; `transport` stays `null` if that throws. `service()` wraps every `Exception` in `new ServletException(e.getMessage(), e)` (137-140), which the container turns into a 500 page.
- `McpServlet.respond` (`McpServlet.java:94-100`) is the existing way to answer with a status and a JSON body `{"error":"..."}`. The new 503 answer follows it.

### 2.3 Shutdown gap
- `McpRuntime.shutDown()` (`McpRuntime.java:175-192`) is static and public. It closes the pool, clears `solutionServers` and nulls `instance`. It has no production caller in this checkout. A grep over the whole `E:\GitHome_master\git` tree finds only `McpRuntime.invalidateAll()` in `servoy-eclipse/com.servoy.eclipse.core/src/com/servoy/eclipse/core/Activator.java:929` and a commented-out one in `server/j2db_server/src/com/servoy/j2db/server/xmlxport/XMLInMemoryImportEngineVersions11AndHigher.java:793`.
- `McpServlet` (`@WebServlet("/mcp/*")`, `McpServlet.java:41-42`) has no `destroy()`. The bundle has no `Activator` or `ServletContextListener`, so servlet `destroy()` is the natural hook.
- `66e372621` changed the servlet registration (`ServiceProvider` plus `@WebServlet` scanning) and removed the only hook that carried a lifecycle call.
- `getInstance()` recreates the runtime lazily (`McpRuntime.java:160-170`), so a destroy followed by a context reload is safe.

### 2.4 Settings, the admin page and the bundle graph (what I found)
- `Settings` is `servoy_shared/src/com/servoy/j2db/util/Settings.java` (`public final class Settings extends SortedProperties`, line 47). Setting names are `public static final String` constants at the top of the class (lines 53-82), for example `TESTING_MODE = "servoy.ngclient.testingMode"` (82) and `ALLOW_CLIENT_REPOSITORY_ACCESS_SETTING` (70). Where a default exists it is a sibling constant, for example `ALLOW_CLIENT_REPOSITORY_ACCESS_DEFAULT = false` (71) and `START_AS_TEAMPROVIDER_DEFAULT` (63).
- Reading: `Settings.getInstance()` (496-499) returns the singleton, then `getProperty(String)` (overridden at 814-843, handles variable substitution) or the inherited `getProperty(String, String)`. Existing numeric reads: `Utils.getAsInteger(Settings.getInstance().getProperty("servoy.max.webforms.loaded", "128"), false)` (`servoy_headless_client/src/com/servoy/j2db/server/headlessclient/WebFormManager.java:58`) and, with explicit `NumberFormatException` handling, `Integer.parseInt(settings.getProperty(KEY, "-1").trim())` (`servoy-extensions/.../rest_ws/RestWSPlugin.java:229`). `Utils.getAsInteger(String, boolean)` (`servoy_shared/.../util/Utils.java:619-638`) returns 0 for an unparsable value, which is not usable here (0 is not a valid cap), so the new code parses with `Integer.parseInt` like `RestWSPlugin`.
- Bundle visibility, no `MANIFEST.MF` change needed: `servoy_mcp/META-INF/MANIFEST.MF:7` has `Require-Bundle: servoy_shared`, and `servoy_shared/META-INF/MANIFEST.MF:46` exports `com.servoy.j2db.util;version="2026.12.0"`. `McpRuntime` already imports `com.servoy.j2db.util.Debug` (line 41) from that package. `McpClientPoolTimeoutException` goes in `com.servoy.mcp`, which is already exported (`MANIFEST.MF:12`).
- Admin page (different repository, companion spec in 3.8): `ConfigServlet` is in the sibling repository `server`: `E:\GitHome_master\git\server\j2db_server\src\com\servoy\j2db\server\servlets\ConfigServlet.java`. Its `j2db_server` bundle requires `servoy_shared` (`server/j2db_server/META-INF/MANIFEST.MF:9`) but not `servoy_mcp`, so it can reference `Settings` constants and cannot reference `McpRuntime` constants. That decides where the name and default constants live (section 3.1). The admin page list is the `generalProperties` array (`ConfigServlet.java:148`); the same array also feeds the REST admin API (`AdminSettingsServlet`, which would store the text `"null"` for a row whose default is `null`, `AdminSettingsServlet.java:146`, hence the `"null"` rule in 3.2) and the documentation generator. Timeout settings in that list are in seconds by convention (for example `sablo.window.timeout.secs`, line 206), which matches the new timeout.

### 2.5 Design-intent conflict and AGENTS.md
- The class Javadoc (`McpRuntime.java:57-59`) says "The client pool is unlimited on purpose... Imposing a ceiling here would only turn a decision someone else should make into a failure they cannot see." The ticket reverses that. The new design answers the "cannot see" part twice: the limits (per key and overall) and the wait timeout are configurable by whoever owns the deployment, and a refused call is visible (WARN in the server log, a specific message to the agent). The Javadoc must be rewritten (3.6).
- `AGENTS.md` section 5 says rate limiting is an infrastructure concern. That entry covers the authentication endpoints (`StatelessLoginHandler`, `DefaultLoginManager`, `OAuthHandler`). A resource safety cap on an internal client pool is not request rate limiting or an attempt counter, so it does not conflict. Reviewers should see this stated in the PR description (section 5).

### 2.6 What pool2 does when the pool is exhausted and blocking (2.4.2 sources, `GenericKeyedObjectPool.java`)
- `borrowObject(key, maxWait)` (338-440). With `blockWhenExhausted=true` it polls the idle deque, tries `create(key)`, and if both give nothing waits on the key's idle deque: `takeFirst()` when `maxWait < 0`, otherwise `pollFirst(maxWait, MILLISECONDS)` (362-369). If that returns `null` it throws `NoSuchElementException("Timeout waiting for idle object")` (370-373). With `blockWhenExhausted=false` the same situation throws `NoSuchElementException("Pool exhausted")` at once (385-387).
- The per-key limit and the overall client max (`maxTotal`) both surface the same way: `create(key)` (1006-1051) returns `null` when over `maxTotalPerKey` (1031-1036) or over `maxTotal` with no idle object anywhere to evict (1014-1020). The caller then waits and times out with the same message. The messages differ between the blocking and non-blocking modes, so the code must not parse messages.
- `NoSuchElementException` is not only the timeout. `borrowObject` also throws it for a freshly created object that fails activation ("Unable to activate object", 403-408) or validation ("Unable to validate object", 427-432). `testOnBorrow` is on (`McpRuntime.java:154`), so a new client whose `isValid()` is false takes the second path. Wrapping only `borrowObject` is therefore not enough to recognise a timeout; section 3.4 describes how `getClient()` tells them apart.
- Waking waiters. `returnObject` calls `reuseCapacity()` when there are borrow waiters (561-563; 809-843). It creates an instance for the most loaded key that is under `maxTotalPerKey`, and `create()` evicts the oldest idle object of another key when `maxTotal` is reached (1017-1021). So a call waiting on key B is served when a client of key A is returned under a `maxTotal` limit.
- `close()` (713-742): sets `closed`, clears idle objects, then "Release any threads that were waiting for an object" by calling `interuptTakeWaiters()` on every idle deque (733-737), and clears again. A later `borrowObject` fails in `assertOpen()` (339). `returnObject` destroys the object when `isClosed()` (541-546).

## 3. Design

### 3.0 What the MCP caller sees
A pool timeout reaches the MCP caller as a readable message in all cases where the caller can still receive an answer. There are three caller-visible outcomes:

| | Situation | HTTP level | What the caller receives |
|---|---|---|---|
| (a) | A `tools/call` waits the full timeout for a client (per-key limit or overall client max reached) | **200** (a normal JSON-RPC response) | `result.isError = true` with one text content: "Timed out after 10 seconds waiting for an available Servoy client from the pool. The server is busy; please retry." (the number is the configured timeout). No stack trace, no pool key, user uid, tenant or limit numbers. Not a JSON-RPC protocol `error`. |
| (b) | The first scan of the solution's tools waits the full timeout. The scan runs on the first request the solution's server receives, normally `tools/list`, but also a `tools/call` that arrives first (for example after a server restart or an `invalidate()`) | **503 Service Unavailable**, header `Retry-After: <timeout seconds, rounded up, at least 1>`, `Content-Type: application/json` | Body `{"error":"Timed out after 10 seconds waiting for an available Servoy client from the pool. The server is busy; please retry."}`. The failed scan is not cached, the next request scans again. |
| (c) | Any other failure of a tool call (the tool throws, licence or solution-open failure in `create()`, pool closed, new client failed validation, interrupted wait) | 200, `isError = true` | Unchanged generic message: "The tool '<name>' failed. See the server log for details.", plus the existing stack-trace WARN. Other scan failures are unchanged too (`ServletException`, 500). |

Example of the actual response body for (a), a `tools/call` with id 2 (field order and any extra fields are up to the SDK serialiser; the existing integration test already asserts the substring `"isError":true`, `McpProtocolIntegrationTest.java:340`):

```json
{"jsonrpc":"2.0","id":2,"result":{"content":[{"type":"text","text":"Timed out after 10 seconds waiting for an available Servoy client from the pool. The server is busy; please retry."}],"isError":true}}
```

Example for (b):

```
HTTP/1.1 503 Service Unavailable
Retry-After: 10
Content-Type: application/json

{"error":"Timed out after 10 seconds waiting for an available Servoy client from the pool. The server is busy; please retry."}
```

Verified end to end (code read, not assumed):

1. **Throw site.** `McpRuntime.getClient` (`McpRuntime.java:241`) will throw `McpClientPoolTimeoutException` (new, 3.4).
2. **`McpToolExecutor.execute`** (`McpToolExecutor.java:70-92`). `runtime.getClient(key)` is a direct call at line 77, outside the `try` that starts at 78. The method has no `catch`, only the releasing `finally` (88-91), does not submit work to another thread and uses no `Future`. A grep of `servoy_mcp/src` for `ExecutionException`, `CompletionException`, `Future` and `submit(` finds nothing. The `invokeAndWait` calls (`McpToolExecutor.java:112`, `McpToolScanner.java:82`) come after `getClient` and are not on its path. So the exception leaves `execute` unwrapped and unchanged.
3. **`McpSolutionServer.call`** (`McpSolutionServer.java:242-274`). `executor.execute(...)` is called at 258, inside the `try` of 255-273, on the same thread. The new `catch (McpClientPoolTimeoutException e)` placed before the generic `catch (Exception e)` (266) therefore sees the exception directly, with no unwrapping by cause chain. `error(...)` (276-279) builds `CallToolResult.builder().addTextContent(message).isError(Boolean.TRUE).build()`, which is exactly the result described in (a).
4. **SDK layer.** `call` is registered as the tool handler with `specification.toolCall(describe(...), (context, request) -> call(tool, context, request))` (`McpSolutionServer.java:177-178`) on a `StatelessSyncSpecification` (167). From the SDK sources on GitHub (main branch, read via web): a sync handler is adapted as `Mono.fromCallable(() -> syncToolSpec.callHandler().apply(ctx, req))`, subscribed on `Schedulers.boundedElastic()` unless `immediateExecution` is set (`McpStatelessServerFeatures.java:229-230`; default `false`, `McpServer.java:2044`; this code does not set it). The handler therefore runs on an SDK worker thread and not on the servlet thread, but the whole `call` method, `getClient` and the catch included, runs inside that one callable, so no thread or `Future` boundary sits between `getClient` and the catch. The `tools/call` request handler returns the handler's `Mono<CallToolResult>` as is (`McpStatelessAsyncServer.java:449-474`), and the servlet transport writes the response as HTTP 200 JSON (`HttpServletStatelessServerTransport.java:198-210`). I found no place that rewrites an `isError` result. A `CallToolResult` value is not an exception, so the SDK's error branch (`catch` at 211-214, 500) is not involved.
5. **Scan path.** The call chain is plain synchronous method calls on the servlet thread: `McpSolutionServer.service` (135) calls `transport()` (143), which calls `registry.getTools()` (173), then `McpToolRegistry.getTools` (89-92), `load()` (113-125), `scan()` (145-147), `toolSource.scan()`, `scanThroughClient` (130-143), `runtime.getClient` (134). No catch or wrapper in between; `service()` catches `Exception` at 137 and wraps it in a `ServletException` (139), which is why the new 503 catch must go in front of it (3.5). The 503 is written before the SDK transport handles anything (`transport` is still `null`), so the response is not yet committed.

What I could not verify by reading: the SDK version bundled at run time is a 2.x jar (the manifest imports `io.modelcontextprotocol.*` in `[2.0.0,3.0.0)`, `servoy_mcp/META-INF/MANIFEST.MF:15-20`; `McpCall.java:110` mentions 2.0.1), which is not on disk in this checkout. The SDK code above is from the GitHub main branch, and I did not read the structured-output path (`McpStatelessAsyncServer.java:242-254`, 283); the tools here have no output schema (`McpSolutionServer.java:71-85`). The integration tests in 4.3 drive the real bundled SDK transport through `McpCall`, and assert status 200, `isError` and the message text for (a) and status 503 for (b), so they prove the pass-through for the shipped SDK version.

Rule that follows: no cause-chain unwrapping is needed today. If a future change moves `executor.execute` to another thread or future, the catch in `call` must unwrap by cause chain (`ExecutionException`, `CompletionException`); the (a) integration test would fail and show it.

One more consequence for (b): while the first request scans, other first requests wait on `synchronized (this)` in `transport()` (148) until the scan finishes or fails, then each scans again if it failed. Each of them can therefore also wait up to one timeout, and all get 503.

### 3.1 Setting names and defaults live in `Settings`
In `servoy_shared/src/com/servoy/j2db/util/Settings.java`, next to the other name constants (after `TESTING_MODE`, line 82), following the name-plus-`_DEFAULT` pattern of `ALLOW_CLIENT_REPOSITORY_ACCESS_SETTING` / `ALLOW_CLIENT_REPOSITORY_ACCESS_DEFAULT` (70-71):

```java
public static final String MCP_CLIENT_POOL_MAX_CLIENTS_PER_KEY = "servoy.mcpClientPool.maxClientsPerSolUsrTenant";
public static final int MCP_CLIENT_POOL_MAX_CLIENTS_PER_KEY_DEFAULT = 100;
public static final String MCP_CLIENT_POOL_MAX_IDLE_PER_KEY = "servoy.mcpClientPool.maxIdleClientsPerSolUsrTenant";
public static final int MCP_CLIENT_POOL_MAX_IDLE_PER_KEY_DEFAULT = 8;
public static final String MCP_CLIENT_POOL_MAX_TOTAL_CLIENTS = "servoy.mcpClientPool.maxTotalClients";
public static final int MCP_CLIENT_POOL_MAX_TOTAL_CLIENTS_DEFAULT = -1;
public static final String MCP_CLIENT_POOL_WAIT_TIMEOUT = "servoy.mcpClientPool.waitForAvailableClientTimeout";
public static final int MCP_CLIENT_POOL_WAIT_TIMEOUT_DEFAULT = 10; // seconds
```

Why the defaults are in `Settings` and not only in `McpRuntime`: `ConfigServlet` (a different repository whose bundle does not depend on `servoy_mcp`, section 2.4) must show the same default text for all four settings, and it can only see `Settings`. `McpRuntime` takes its defaults from these constants, so there is one source of truth.

### 3.2 Static members in `McpRuntime`
```java
static final int MAX_CLIENTS_PER_KEY = readInt(McpRuntime::lookup, Settings.MCP_CLIENT_POOL_MAX_CLIENTS_PER_KEY,
    Settings.MCP_CLIENT_POOL_MAX_CLIENTS_PER_KEY_DEFAULT, value -> value > 0);
static final int MAX_IDLE_PER_KEY = readInt(McpRuntime::lookup, Settings.MCP_CLIENT_POOL_MAX_IDLE_PER_KEY,
    Settings.MCP_CLIENT_POOL_MAX_IDLE_PER_KEY_DEFAULT, value -> value >= 0);
static final int TOTAL_MAX_CLIENTS = readInt(McpRuntime::lookup, Settings.MCP_CLIENT_POOL_MAX_TOTAL_CLIENTS,
    Settings.MCP_CLIENT_POOL_MAX_TOTAL_CLIENTS_DEFAULT, value -> value == -1 || value > 0);
static final int WAIT_FOR_AVAILABLE_CLIENT_TIMEOUT = readInt(McpRuntime::lookup, Settings.MCP_CLIENT_POOL_WAIT_TIMEOUT,
    Settings.MCP_CLIENT_POOL_WAIT_TIMEOUT_DEFAULT, value -> value > 0); // seconds

private static String lookup(String key)
{
    return Settings.getInstance().getProperty(key);
}
```

- They are `static final`, initialised once when `McpRuntime` is loaded, after `log` (line 69), so `log` is usable in the initialiser. A changed property needs a server restart (open question 4). `ConfigServlet` already tells the user so (`ConfigServlet.java:3581`, `3586`).
- `Settings.getInstance().getProperty(String)` is the existing read path (2.4). Use the lambda/method above rather than `Settings.getInstance()::getProperty`, because `getProperty` is overloaded.
- New package-private static helper, so the parsing is testable without touching global `Settings` state:

```java
static int readInt(java.util.function.Function<String, String> lookup, String key, int defaultValue,
    java.util.function.IntPredicate valid)
```

  Rules (apply to all four properties, including `maxTotalClients` and `waitForAvailableClientTimeout`):
  - A missing value, a blank value, or the literal `null` (case-insensitive) means "not set": return `defaultValue`, log nothing. (`AdminSettingsServlet.constructSettingsMap` returns the text `"null"` for rows whose default is `null`, `AdminSettingsServlet.java:146`, so a round trip through the REST admin API can store that text.)
  - Otherwise `Integer.parseInt(value.trim())`, as `RestWSPlugin.java:229` does. A `NumberFormatException`, or a value for which `valid` is false, returns `defaultValue` and logs **one** WARN (`mcp: ignoring invalid value '<v>' for <key>, using <default>`). One line per bad property at class load, not per call.
- Validity per property:

  | Property | Valid | Notes |
  |---|---|---|
  | `maxClientsPerSolUsrTenant` | integer > 0 | -1 (unlimited) is not accepted: the whole point is a ceiling. 0 would refuse every call. |
  | `maxIdleClientsPerSolUsrTenant` | integer >= 0 | 0 means every returned client is shut down at once. |
  | `maxTotalClients` | -1 or integer > 0 | The overall client max over all keys. -1 is unlimited. 0 would refuse every call, so it falls back to -1. |
  | `waitForAvailableClientTimeout` | integer > 0 | The configurable wait timeout, in seconds. 0 would mean "do not wait" in pool2 and a negative value "wait forever", which is the hang this ticket removes, so neither is accepted. |

- Choosing the timeout default (client-timeout interplay). The caller's HTTP request is held open for up to the timeout, plus the tool's own run time when a client is obtained. Many MCP clients time out on the order of tens of seconds to minutes, and reverse proxies and load balancers have idle timeouts of their own; I have not checked any specific client or proxy. If the server's wait is longer than the caller's own request timeout, the caller gives up first and never sees the "server is busy" message, so the configured wait should stay shorter than the timeouts of the clients and proxies in front of the server. The default of 10 seconds is chosen to be short enough for that. Anyone who raises `waitForAvailableClientTimeout` has to check it against those timeouts. The admin page text (companion spec) states the default but cannot know the client side.
- `McpRuntime` imports `com.servoy.j2db.util.Settings`. No `MANIFEST.MF` change is needed (2.4).

### 3.3 Pool configuration
`McpRuntime(ClientSource)` delegates to a new package-private constructor that takes explicit values (per-key limit, idle limit, overall client max and wait timeout), so tests depend neither on `Settings` nor on the static initialisers:

```java
McpRuntime(ClientSource clientSource)
{
    this(clientSource, MAX_CLIENTS_PER_KEY, MAX_IDLE_PER_KEY, TOTAL_MAX_CLIENTS,
        Duration.ofSeconds(WAIT_FOR_AVAILABLE_CLIENT_TIMEOUT));
}

McpRuntime(ClientSource clientSource, int maxClientsPerKey, int maxIdlePerKey, int totalMaxClients, Duration waitTimeout)
```

The private no-arg constructor (line 103) is unchanged and uses the single-argument one. The constructor body:

- `config.setMaxTotalPerKey(maxClientsPerKey)`, `config.setMaxIdlePerKey(maxIdlePerKey)`, `config.setMaxTotal(totalMaxClients)`.
- `config.setBlockWhenExhausted(true)` and `config.setMaxWait(waitTimeout)`. Pool API: the built 2.12.0 jar offers both `setMaxWait(java.time.Duration)` and `setMaxWaitMillis(long)` (`javap`); 2.4.2 only has `setMaxWaitMillis`. Use `setMaxWait(Duration)` for the 2.13.1 target. I did not confirm that `setMaxWaitMillis` is deprecated in 2.13.1, and the implementer must confirm which one compiles against the bundle in the workspace target; `setMaxWaitMillis(waitTimeout.toMillis())` is the fallback.
- Construct with `new GenericKeyedObjectPool<>(factory, config)` and keep `clientPool.setTestOnBorrow(true)`.
- Delete the stale comment `// no ceiling: see the class comment`.
- Keep the resolved values in `final` fields (`maxClientsPerKey`, `totalMaxClients`, `waitTimeout`) for the WARN and the exception, so that logging does not depend on which getters pool2 declares.
- Package-private getters for the regression test: `getMaxClientsPerKey()` returns `clientPool.getMaxTotalPerKey()`, `getMaxIdleClientsPerKey()` returns `clientPool.getMaxIdlePerKey()` (both declared on the pool class, `javap`). For `maxTotal`, blocking and `maxWait`, the `javap` listing shows `getMaxTotal()`, `getBlockWhenExhausted()` and `getMaxWaitDuration()` on the config classes, but I did not see them declared on `GenericKeyedObjectPool` itself (they are probably inherited from `BaseGenericObjectPool`). The implementer reads them from the pool if they compile and otherwise asserts through behaviour only (the tests in 4.3 do both).

### 3.4 Timeout handling in `getClient()`
New exception, `servoy_mcp/src/com/servoy/mcp/McpClientPoolTimeoutException.java` (package `com.servoy.mcp`, already exported by the bundle):

```java
public class McpClientPoolTimeoutException extends Exception
{
    private final Duration timeout;

    public McpClientPoolTimeoutException(Duration timeout, Throwable cause)
    {
        super("Timed out after " + describe(timeout) + " waiting for an available Servoy client from the pool. " +
            "The server is busy; please retry.", cause);
        this.timeout = timeout;
    }

    public Duration getTimeout() { return timeout; }
}
```

- `describe(Duration)`: whole seconds give "10 seconds" ("1 second" for one), anything else gives milliseconds ("200 milliseconds"). The duration is the configured `waitForAvailableClientTimeout`, so the text follows the property.
- It is a checked exception. `getClient()` already declares `throws Exception`, and both callers (`McpToolExecutor.execute`, `McpToolRegistry.scanThroughClient`) are declared `throws Exception`, so no signature changes.
- The message is meant for the agent. It contains no pool key, user uid, tenant, limit or class name.

`getClient()`:

```java
public IHeadlessClient getClient(McpClientKey key) throws Exception
{
    String poolKey = key.toPoolKey();
    long started = System.nanoTime();
    try
    {
        return clientPool.borrowObject(poolKey);
    }
    catch (NoSuchElementException e)
    {
        long waited = System.nanoTime() - started;
        if (waited < waitTimeout.toNanos()) throw e;   // not a wait timeout, e.g. a new client failed validation

        log.warn("mcp: timed out after {} waiting for an available client for '{}': {} in use for this key (limit {} per solution/user/tenant), " +
            "{} in use in total (limit {}). The call is refused.", waitTimeout, poolKey, Integer.valueOf(clientPool.getNumActive(poolKey)),
            Integer.valueOf(maxClientsPerKey), Integer.valueOf(clientPool.getNumActive()), Integer.valueOf(totalMaxClients));
        throw new McpClientPoolTimeoutException(waitTimeout, e);
    }
}
```

- Only `borrowObject` is wrapped, and only `NoSuchElementException` is caught. `IllegalStateException` (pool closed), `InterruptedException` (a waiter released by `close()`) and failures from `create()` (licence, solution open) are untouched and keep their current generic handling.
- Deviation from the review note "getClient() wraps only borrowObject, so a `NoSuchElementException` is a timeout": section 2.6 shows pool2 also throws `NoSuchElementException` for a new client that fails validation or activation. The elapsed time is the discriminator. A wait timeout takes at least `maxWait` (`pollFirst(timeout)`, 366-367), while a validation failure of a new client comes back right after `create()`. The residual risk is a `create()` that itself takes longer than the whole timeout and is then rejected by validation, which would be reported as a timeout. That is acceptable (the server is busy either way), and the log line still shows the numbers. A test pins the normal case (3.4 tests in 4.3).
- The WARN is logged once, at the source, on logger `servoy.mcp` (`McpRuntime.log`). It names the pool key, the timeout, the active count for the key, the per-key limit, the active total and the overall client max (-1 is unlimited). The pool key holds the user uid; it was already DEBUG-logged by the factory, and uids are internal identifiers, so it is acceptable in a WARN that an operator acts on. It is never in the message that goes to the agent.
- The number of "in use" clients is read from the pool at the time of the log and can have changed in the meantime. It is diagnostic only.

### 3.5 Where the exception is turned into a result
(What the caller finally sees is summarised in 3.0.)

- **Tool call.** `McpToolExecutor.execute` is unchanged: `getClient` is outside its `try` (77-78), so the exception leaves the method without a release being needed. In `McpSolutionServer.call` (`McpSolutionServer.java:255-273`) add a catch **before** the generic `catch (Exception e)` (266):

```java
catch (McpClientPoolTimeoutException e)
{
    // McpRuntime.getClient already logged the WARN with the pool key and the limits
    return error(e.getMessage());
}
```

  The result is the usual `isError` result (`error(...)`, 276-279) with the specific message, and no stack trace is logged. Every other exception keeps the generic message and the stack-trace WARN (266-273), unchanged. The exception arrives unwrapped (3.0, points 2 to 4), so a plain catch of this type is enough.
- **Scan (first request, `McpToolRegistry.scanThroughClient` via `getTools()`).** The exception propagates out of `registry.getTools()` (`McpSolutionServer.java:173`) and `transport()` unchanged (3.0, point 5). In `service()` (131-141) add a catch before the generic one that answers **503 Service Unavailable** with a `Retry-After` header (the timeout in seconds, rounded up, at least 1) and the JSON body `{"error":"<message>"}`, the same shape as `McpServlet.respond` (94-100). A small private `respond` helper in `McpSolutionServer` does that (the `McpServlet` one is private). Reasons: the pool is busy, not broken, so a 5xx that clients treat as retryable is correct; the generic `ServletException` path gives a 500 error page and a stack trace for a normal overload. Nothing has been written to the response at that point, because the scan runs before the transport handles the request, and `transport` stays `null`, so the next request scans again (2.2).
- A tool call that is already inside the SDK transport never reaches the `service()` catch: `call()` handles its own exceptions.

### 3.6 Javadoc rewrite
Replace the "unlimited on purpose" paragraph in the `McpRuntime` class Javadoc with text to this effect:

> The client pool is bounded per solution/user/tenant, and optionally across all of them. Both limits (the per-key limit and the overall client max), the number of idle clients kept and how long a caller waits (the wait timeout) are configurable through the four <code>servoy.mcpClientPool.*</code> properties (see {@link Settings}); they are read once, when this class is loaded. A caller that finds the pool exhausted waits up to the configured timeout for a client to be returned, and then fails with a {@link McpClientPoolTimeoutException} and a WARN in the log. The defaults are a safety ceiling, not a tuning limit: a deployment's normal concurrency stays far below them, and licences usually run out first. The wait timeout should stay shorter than the request timeouts of the MCP clients in front of the server. Idle clients above the idle limit are released when returned.

### 3.7 Shutdown drain
- `McpServlet`: add

```java
@Override
public void destroy()
{
    McpRuntime.shutDown();
    super.destroy();
}
```

- Refactor `McpRuntime.shutDown()` so the closing logic lives in a package-private instance method, `void drain()` (close the pool, clear `solutionServers`). The static `shutDown()` keeps its signature and synchronization and calls `instance.drain()` before nulling `instance`. Tests can then exercise the drain on a harness instance without touching the singleton. The behaviour of the static method does not change.
- `clientPool.close()` destroys the idle clients through the factory's `destroyObject`, which calls `shutDown(true)` on each client. Clients on loan are not reached by `close()`; `returnObject` destroys them when it finds the pool closed (2.4.2, 541-546). This was read in 2.4.2 only and the tests assert it for 2.13.1.
- With blocking enabled, threads can be waiting in `borrowObject` when the servlet is destroyed. `close()` releases them (2.4.2, 733-737: `interuptTakeWaiters()`), so they fail promptly, and the wait timeout bounds them in any case. The exact exception such a waiter sees was not read (it is not a `NoSuchElementException` wait timeout as far as I can tell), so the test asserts that the waiter returns well before its timeout, not which exception it gets. `close()` itself does not wait for waiters and cannot hang on them. Such a failure is not a pool timeout, so it takes the generic path.
- If the servlet is registered twice (developer Tomcat via `ServiceProvider` plus annotation scanning), either instance's `destroy()` drains the shared runtime. `getInstance()` recreates it on the next request, so this is safe.

### 3.8 Admin page (`ConfigServlet`), different repository: companion spec
The admin-page change is not part of this checkout. It is specified in full, and will be implemented, in the `server` repository:

`E:\GitHome_master\git\server\docs\SVY-21630-mcp-pool-admin-settings.spec.md`

What is needed there, in short:
- File: `server/j2db_server/src/com/servoy/j2db/server/servlets/ConfigServlet.java`, the `generalProperties` array (`ConfigServlet.java:148`). Add four rows, one per property of section 1 (including `maxTotalClients` and `waitForAvailableClientTimeout`), under a new `MCP Server Settings` section header.
- The default column of each row is `null`, and the description states the default (100, 8, -1 unlimited, 10 seconds), built from the `Settings` `_DEFAULT` constants of 3.1. An empty field then removes the key, which means "use the default", and a save does not pin today's default into `servoy.properties`.
- Dependency: the rows use the property-name and default constants added to `Settings` in 3.1 (`Settings.MCP_CLIENT_POOL_*`). `j2db_server` requires `servoy_shared` but not `servoy_mcp` (2.4), so it cannot use the `McpRuntime` constants. This repository's change (4.1) therefore has to land first, or in the same build; the commit in the `server` repository follows.
- The row texts, the placement in the array, the effect on `AdminSettingsServlet` and the documentation generator, and the checks are in the companion spec and are not repeated here.

### 3.9 Backward compatibility
- Today the effective behaviour is 8 clients per key, the 9th caller waits forever (2.1). With the defaults it becomes 100 per key and a wait of at most 10 seconds (the configurable timeout). A deployment that relied on the accidental limit of 8 as a licence guard sets `servoy.mcpClientPool.maxClientsPerSolUsrTenant=8` (and gets a 10 second timeout instead of an unbounded wait). A deployment whose callers legitimately queue for longer raises `servoy.mcpClientPool.waitForAvailableClientTimeout`, keeping in mind the client timeout caveat of 3.2.
- `maxTotalClients` (the overall client max) defaults to -1, as today. Existing installations see no new global limit unless they set one.
- `servoy.properties` files without the new keys behave as the defaults. Unparsable or out-of-range values (for any of the four, including `maxTotalClients` and `waitForAvailableClientTimeout`) fall back to the defaults with one WARN at startup.
- No public API of `McpRuntime` changes. `getClient` gains one more (checked) exception subtype that its `throws Exception` already allows.

### 3.10 Git history
- `McpRuntime.java` is entirely from `9f650f6ca` (2026-08-28, SVY-21326, `[ai]`). The `config` object has been dropped since the first commit.
- `66e372621` (2026-08-31) removed the lifecycle registration that carried `shutDown()`.
- No earlier spec exists for SVY-21326 or SVY-21630 under `docs/`.

## 4. Implementation plan

### 4.1 `servoy-client` checkout (first commit)
1. `servoy_shared/src/com/servoy/j2db/util/Settings.java`: add the four name constants and four `_DEFAULT` constants (3.1). No other change.
2. `servoy_mcp/src/com/servoy/mcp/McpClientPoolTimeoutException.java`: new checked exception (3.4).
3. `servoy_mcp/src/com/servoy/mcp/McpRuntime.java`
   1. Add the four static members (per-key limit, idle limit, overall client max `TOTAL_MAX_CLIENTS`, wait timeout `WAIT_FOR_AVAILABLE_CLIENT_TIMEOUT`) and `readInt` / `lookup` (3.2), and the `Settings` and `java.time.Duration` imports. Add `java.util.NoSuchElementException` for the catch.
   2. Add the five-argument package-private constructor, delegate from `McpRuntime(ClientSource)`, pass `config` to the pool constructor (`maxTotalPerKey`, `maxIdlePerKey`, `maxTotal`, `blockWhenExhausted=true`, `maxWait`), remove the stale comment (3.3).
   3. Keep the resolved values in fields; add the package-private getters (3.3).
   4. Rewrite `getClient()` as in 3.4.
   5. Rewrite the class Javadoc paragraph (3.6).
   6. Extract the package-private instance `drain()` from the static `shutDown()` (3.7).
4. `servoy_mcp/src/com/servoy/mcp/McpSolutionServer.java`: catch `McpClientPoolTimeoutException` before the generic catch in `call` (255-273) and before the generic catch in `service` (133-140), with the 503 answer and a private `respond` helper (3.0, 3.5).
5. `servoy_mcp/src/com/servoy/mcp/McpServlet.java`: add `destroy()` calling `McpRuntime.shutDown()` (3.7).
6. `servoy_mcp/META-INF/MANIFEST.MF`: no change (2.4). Do not edit it.
7. Tests (4.3).
8. Build `servoy_mcp`, run `servoy_mcp_tests`, then the post-edit checklist in `AGENTS.md` (section 4: `getCompilationErrors`, quick fixes, re-check; section 6: Spotbugs on the new and changed code). Use the `eclipse-coder` tools for edits to files inside Eclipse projects (`AGENTS.md` section 2).
9. Commit subject: `SVY-21630 bound and configure the MCP client pool, report pool timeouts, drain it on servlet destroy [ai]` (`AGENTS.md` section 3).

### 4.2 `server` repository: see the companion spec (second commit, after step 4.1 has landed)
The `ConfigServlet.generalProperties` change (four rows under a `MCP Server Settings` header) is planned, implemented and verified in the companion spec `E:\GitHome_master\git\server\docs\SVY-21630-mcp-pool-admin-settings.spec.md` (3.8). It needs the `Settings` constants from step 4.1.1, so it follows this repository's commit. Its commit subject is defined there and follows `AGENTS.md`-style `[ai]` convention with the Jira key `SVY-21630`.

### 4.3 Tests (`servoy_mcp_tests`, a fragment of `servoy_mcp`, so package-private access works)
All use JUnit 5 and Mockito, in the style of the existing classes. Use `@Timeout` on every test that can block.

**`McpRuntimePoolTest`** (`servoy_mcp_tests/src/test/java/com/servoy/mcp/McpRuntimePoolTest.java`). Keep the current `Harness()` and tests untouched. Add a `Harness(int perKey, int idle, int total, Duration wait)` constructor that calls the five-argument `McpRuntime` constructor. Use `McpLogCapture.of("servoy.mcp")` for log assertions (it records `"<LEVEL> <message>"`, `McpLogCapture.java:76`).
- Timeout after the cap: perKey 2, idle 2, total -1, wait 200 ms. Borrow two clients on one key without returning them. The third `getClient` throws `McpClientPoolTimeoutException` after at least 200 ms (measure with `System.nanoTime()`; assert `>= 200 ms` and, for sanity, far below a few seconds). `harness.opened.size() == 2`. The message contains "Timed out after 200 milliseconds" and "available Servoy client" and does not contain the pool key, the user uid or the limit numbers. The log contains `WARN`, the pool key, and the limit (`@Timeout` 10 s).
- A blocked borrow succeeds when a client is returned in time: perKey 1, wait 5 s. Hold the only client, start a second borrow on another thread, return the first client after about 100 ms, join. The second borrow got a client, no exception, and `opened.size() == 1` (the same client is reused). `@Timeout` 15 s.
- Cap is per key: perKey 1, wait 200 ms. A second key still gets a client while the first key is at the cap.
- `maxTotalClients` (overall client max) across keys: total 2, perKey 100, wait 200 ms. Borrow on key A and key B without returning. A borrow on a third key C throws `McpClientPoolTimeoutException`. After returning A's client, a borrow on key C succeeds (pool2 evicts A's idle client to make room, section 2.6) and A's client received `shutDown(true)`. This pins the cross-key behaviour assumed from 2.4.2.
- The wait timeout is the configured one: with wait 300 ms the exception reports 300 milliseconds and the refused call returns after at least 300 ms, with wait 1 s the message says "1 second". (Pins that the constructor's `waitTimeout` reaches both `maxWait` and the message.)
- A timeout only counts as a pool timeout: use a client source whose `isValid()` returns false. With wait 5 s the borrow throws `NoSuchElementException` (not `McpClientPoolTimeoutException`) in well under 5 s, and no pool-timeout WARN is logged. This covers the elapsed-time discriminator in 3.4.
- Dropped-config regression, adapted for blocking: `new Harness(20, 8, 7, Duration.ofSeconds(3)).runtime` reports `getMaxClientsPerKey() == 20` and `getMaxIdleClientsPerKey() == 8`, and if the getters compile also `getMaxTotal() == 7`, `getBlockWhenExhausted()` true and `getMaxWaitDuration() == 3 s`. Behavioural half, which does not depend on getters: borrow 9 clients on one key (perKey 20) without returning them. With the dropped config the 9th call would wait forever; under `@Timeout` 5 s this fails the test. Also with perKey 2, wait 200 ms the third call must time out, which the dropped config (default 8, wait forever) would not do within the `@Timeout`.
- Idle limit: perKey 100, idle 8. Borrow 10 clients on one key, return all 10, and verify exactly 2 of them received `shutDown(true)`.
- Drain: borrow and return a client, call `harness.runtime.drain()`, `verify(client).shutDown(true)`. Also borrow a client, call `drain()`, then `releaseClient`, and verify it is shut down (loan-then-return). After `drain()` a new `getClient` throws `IllegalStateException`, not `McpClientPoolTimeoutException`.
- Waiters during drain: perKey 1, wait 20 s. Hold the client, start a second borrow on another thread, call `drain()`. The waiting thread finishes within a few seconds (not 20) with some exception, and not with `McpClientPoolTimeoutException`. This asserts the `close()` behaviour described in 3.7 for the pool version in use. `@Timeout` 30 s.
- Servlet lifecycle: `McpRuntime before = McpRuntime.getInstance(); new McpServlet().destroy(); assertNotSame(before, McpRuntime.getInstance());`, followed by `McpRuntime.shutDown()` in `@AfterEach`. The private constructor opens no client, so no application server is needed.

**Static defaults and setting overrides** (new `McpRuntimeSettingsTest`, same package). `readInt` takes the lookup as a function, so no global `Settings` state is touched.
- Defaults: `Settings.MCP_CLIENT_POOL_MAX_CLIENTS_PER_KEY_DEFAULT == 100`, `..._MAX_IDLE_PER_KEY_DEFAULT == 8`, `..._MAX_TOTAL_CLIENTS_DEFAULT == -1`, `..._WAIT_TIMEOUT_DEFAULT == 10`, and the four property-name constants equal the literal strings in section 1.
- The live static members (`MAX_CLIENTS_PER_KEY`, `MAX_IDLE_PER_KEY`, `TOTAL_MAX_CLIENTS`, `WAIT_FOR_AVAILABLE_CLIENT_TIMEOUT`) equal the defaults when the properties are not set. `Assumptions.assumeTrue(Settings.getInstance().getProperty(key) == null)` per property first, so a developer with a local override does not get a false failure. `Settings.getInstance()` is only read, never written.
- `readInt` table test with a `Map`-backed lookup: missing, blank and `"null"` give the default and log nothing; `"25"`, `" 25 "` give 25; `"abc"` gives the default and one WARN; `"0"` and `"-5"` give the default for the per-key cap and the timeout; `"0"` gives the default for `maxTotalClients` while `"-1"` and `"3"` are accepted; `"0"` is accepted for the idle limit and `"-1"` is not.
- Not tested: that `Settings` really delivers `servoy.properties` values to `McpRuntime` (that is `Settings` behaviour already used everywhere, and the static initialisers run once per JVM).

**`McpProtocolIntegrationTest`** (tool-call and scan path; these two tests prove 3.0 (a) and (b) through the real SDK transport). Its `Harness` builds `new McpRuntime(solutionName -> fake.client)` (line 94). Add a second constructor, or a `Harness` factory, that builds the runtime with perKey 1, idle 1, total -1 and wait 200 ms. Both tests are `@Timeout` 10 s.
- Tool call (3.0 a): borrow the only client for `new McpClientKey(SOLUTION, "alice-uid", "acme")` (the key `ALICE_TOKEN` maps to, see the existing `theClientIsReturned` test at 358-370), then `McpCall.callTool(harness.server, "myScope_add", "{\"first\":2,\"second\":40}", ALICE_TOKEN)`. Assert `call.status() == 200`, `call.contains("\"result\"")`, `call.contains("\"isError\":true")`, `call.contains("Timed out after 200 milliseconds waiting for an available Servoy client")`, `!call.contains("See the server log for details")` and `!call.contains("alice-uid")`. This is the end-to-end proof that the specific message survives the executor, the `call` catch and the SDK. Compare the existing generic-failure test at 326-342, which asserts `isError` (340) and, at 341, the text of the thrown exception (see open question 7). Nothing was run (`ranContext.get() == null`).
- Scan (3.0 b): build the server with the registry that scans through the client (`new McpSolutionServer(runtime, SOLUTION, new McpToolRegistry(runtime, SOLUTION), authenticator)`), hold the scan client with `new McpClientKey(SOLUTION, McpToolRegistry.SCAN_USER, null)` (`SCAN_USER` is package-private, `McpToolRegistry.java:51`), then `McpCall.toolsList(server)`. Assert `call.status() == 503` and the body contains the timeout message. If `McpCall` does not record response headers (its `response(...)` stand-in, `McpCall.java:218-257`, handles only `getWriter`, `setStatus`, `sendError`, `getStatus`, `isCommitted`, `flushBuffer` and `toString` and returns nothing for `setHeader`), extend it minimally to record `setHeader`/`addHeader`, expose `Retry-After` and assert it is `"1"`. After releasing the held client, a second `toolsList` must not answer 503 (the failed scan was not cached, 2.2); it may fail for another reason in the harness because there is no real solution behind the client, so assert only `status() != 503`.
- An ordinary tool failure still gets the generic message (3.0 c): the existing test at 326-342 is unchanged and keeps passing (see open question 7).

Existing `McpRuntimePoolTest`, `McpProtocolIntegrationTest` and `McpToolExecutorTest` cases must pass unchanged.

## 5. Acceptance criteria
- [ ] The pool is created with the config object: `maxTotalPerKey`, `maxIdlePerKey`, `maxTotal` and `maxWait` come from the four settings, and `blockWhenExhausted` is true.
- [ ] `MAX_CLIENTS_PER_KEY`, `MAX_IDLE_PER_KEY`, `TOTAL_MAX_CLIENTS` and `WAIT_FOR_AVAILABLE_CLIENT_TIMEOUT` are static members of `McpRuntime` with the defaults 100, 8, -1 and 10 (seconds). Each is read from its own property (`servoy.mcpClientPool.maxClientsPerSolUsrTenant`, `...maxIdleClientsPerSolUsrTenant`, `...maxTotalClients`, `...waitForAvailableClientTimeout`) through `Settings.getInstance().getProperty(...)`, and an unset, blank or invalid value gives the default.
- [ ] Validity: `maxTotalClients` accepts -1 or a value > 0 (0 falls back to -1); `waitForAvailableClientTimeout` accepts only a value > 0 (0 and negative values fall back to 10); an invalid value of any of the four logs one WARN at class load.
- [ ] The property names and defaults are constants in `Settings`. `McpRuntime` uses those constants (and the admin page in the `server` repository uses them too, see the last criteria block). `MANIFEST.MF` of `servoy_mcp` is unchanged.
- [ ] The package-private `McpRuntime` constructor takes the limits (per key, idle, overall client max) and the wait timeout explicitly, so tests do not depend on `Settings` or on the static initialisers.
- [ ] A borrow beyond the per-key limit, or beyond `maxTotalClients` across keys, waits up to the configured timeout and then fails with `McpClientPoolTimeoutException`. A borrow succeeds if a client is returned within the timeout.
- [ ] A WARN on logger `servoy.mcp` is written on a pool timeout, naming the pool key, the timeout, the active count, the per-key limit and the overall client max. Its text is not shown to the agent.
- [ ] A `tools/call` that times out on the pool answers HTTP 200 with a JSON-RPC `result` that has `isError: true` and the text "Timed out after N seconds waiting for an available Servoy client from the pool. The server is busy; please retry." (N is the configured timeout), and no pool key, user uid or limits (3.0 a). The exception reaches `McpSolutionServer.call` unwrapped. All other tool failures keep "The tool '<name>' failed. See the server log for details." (3.0 c).
- [ ] A first-request scan (`tools/list`, or a `tools/call` that arrives first) that times out on the pool answers HTTP 503 with a `Retry-After` header and a JSON body `{"error":"<same message>"}`, and the next request scans again (3.0 b).
- [ ] A new client that fails validation is not reported as a pool timeout.
- [ ] The cap applies per `solution|user|tenant` key. A key under the cap is unaffected by another key at it.
- [ ] A test fails if the pool config is dropped again (configured limits and wait are asserted, and 9 clients with limit 20 do not block).
- [ ] The "unlimited on purpose" Javadoc and the stale code comment are gone, replaced with text that matches the behaviour, including the overall client max, the configurable wait timeout and the client-timeout caveat (3.6).
- [ ] The wait timeout default and its interplay with MCP client request timeouts are documented in the spec (3.2) and in the class Javadoc (3.6).
- [ ] `McpServlet.destroy()` calls `McpRuntime.shutDown()`. After it, pooled idle clients have had `shutDown(true)` called, clients on loan are shut down when returned, and a thread waiting for a client is released before its timeout.
- [ ] After `destroy()`, `McpRuntime.getInstance()` returns a fresh, usable runtime.
- [ ] Existing tests in `servoy_mcp_tests` pass unchanged (subject to open question 7).
- [ ] The PR description states that this is a resource safety cap and not request rate limiting (`AGENTS.md` section 5), and notes the behaviour change in 3.9 (limit 8 and unbounded wait become 100 and 10 seconds, with the overall client max and the timeout configurable).

Admin page criteria, verified in the companion spec / `server` repository (not in this checkout; `E:\GitHome_master\git\server\docs\SVY-21630-mcp-pool-admin-settings.spec.md`):
- [ ] The four properties appear in `ConfigServlet.generalProperties` under "MCP Server Settings", with a `null` default column and descriptions that state the defaults, and are therefore also in `AdminSettingsServlet` and the generated configuration documentation.
- [ ] `ConfigServlet` uses the `Settings` constants from 3.1 for the keys and the default numbers, and an empty field removes the key.

## 6. Out of scope
- Idle EVICTION only (`timeBetweenEvictionRuns`, `minEvictableIdleDuration`). Abandoned per-user clients still stay pooled, up to the idle limit per key, until invalidated or shut down. `maxTotalClients` does bound their number when set, because pool2 evicts idle clients of other keys to make room (2.6).
- Explicitly **in scope**, not deferred: the overall client max (`servoy.mcpClientPool.maxTotalClients`, `TOTAL_MAX_CLIENTS`, default -1) and the configurable wait timeout (`servoy.mcpClientPool.waitForAvailableClientTimeout`, `WAIT_FOR_AVAILABLE_CLIENT_TIMEOUT`, default 10 s). Both are implemented in this ticket, covered by tests (4.3) and by the acceptance criteria (5).
- Reloading the properties at runtime. They are read once (open question 4).
- Request rate limiting, authentication changes, and anything under `AGENTS.md` section 5.
- A per-solution limit, or different limits for the scan user.
- Throttling or de-duplicating the pool-timeout WARN.
- A `ServletContextListener` or bundle `Activator` lifecycle.
- Anything inside the `server` repository (`ConfigServlet` rows, the Angular admin UI): specified in the companion spec (3.8), not changed from this repository.
- Warn-only behaviour. At the limit the call waits and then fails, as the review asked.

## 7. Open questions
| Question | Owner | Status |
|----------|-------|--------|
| commons-pool2 2.13.1 behaviour is assumed from the 2.4.2 sources and the 2.12.0 API listing: `NoSuchElementException` on a wait timeout and for a failed validation, `reuseCapacity`/`clearOldest` across keys under `maxTotal`, `close()` releasing waiters, `returnObject` destroying on a closed pool, and which of `setMaxWait(Duration)` / `setMaxWaitMillis` and which getters exist on the pool class. The tests in 4.3 assert the behaviour; the implementer confirms the API at compile time. | Implementer | open |
| The timeout is told apart from a failed validation by elapsed time (3.4), because pool2 uses `NoSuchElementException` for both. Is that acceptable, or should the factory record validation failures explicitly? | Reviewer | open |
| Under sustained exhaustion every refused call writes one WARN (after waiting up to the timeout, which already limits the rate per thread) and no stack trace. Is that volume acceptable, or should it be throttled? The spec keeps it unthrottled. Servlet threads are still held for up to the timeout, so the thread-exhaustion exposure is reduced, not removed. | Reviewer | open |
| The properties are read once, when `McpRuntime` is first loaded, because they are `static final` as requested. A change needs a server restart, and the admin page already says so. If the class can be loaded before `servoy.properties` is loaded into `Settings` in some startup path (for example the developer), it would silently use the defaults. I did not trace the startup order. | Implementer | open |
| Ordering of the two repositories: the `ConfigServlet` change (companion spec, `server` repository) needs the `Settings` constants from this repository, so this commit must land first or in the same build. Who merges the `server` change and when? | Assignee | open |
| Is the default of 100 clients per key still right now that it is the first limit ever applied (today it is effectively 8)? Each client uses a licence. The spec keeps the value from the ticket and documents the way back to 8 (3.9). | Product owner | open |
| Is a default wait of 10 s right relative to the request timeouts of the MCP clients and proxies that will call this server (3.2)? I could not check any specific client. If a client gives up earlier than 10 s, the caller never sees the message (3.0 a) and the setting has to be lowered. | Product owner | open |
| `McpProtocolIntegrationTest.aFailingToolIsAnErrorResult` (326-342) asserts at line 341 that the response contains "the table is not there", the text of the thrown exception, while `McpSolutionServer.call` (266-273) returns only the generic message and a comment (269-272) says the raw message must not leak. I did not run the tests, so I do not know whether that test passes today. The implementer runs it first: if it already fails, that is outside this ticket; if it passes, the generic path leaks more than the spec assumes and the (c) rows of 3.0 need rechecking. | Implementer | open |
