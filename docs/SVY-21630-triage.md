# Triage Report — SVY-21630

**Verdict:** PROCEED

## Reported problem

Follow-up to SVY-21326 (MCP server), raised from its peer review. Two symptoms:

1. The headless-client pool in `McpRuntime` is described as "deliberately unbounded" (`maxTotalPerKey=-1`, `maxIdlePerKey=-1`, `blockWhenExhausted=false`), with no idle eviction. The pool key is `solution|userUid|tenant`, driven by whatever identities the authenticator admits. The ticket sees a resource-exhaustion / DoS surface on a public endpoint.
2. `McpRuntime.shutDown()` has no production caller, so pooled (licensed) headless clients are never drained when the server or servlet stops.

## Ticket proposes

1. A soft safety cap on the pool (about 100 per key), with a prominent WARNING in the server log when it is hit instead of failing silently.
2. Calling `McpRuntime.shutDown()` from the `McpServlet` lifecycle (`destroy()`).

## Root-cause assessment

### Finding A: the ticket's description of the pool is wrong. The "unbounded" config is never applied.

`servoy_mcp/src/com/servoy/mcp/McpRuntime.java:112-154`:

```java
GenericKeyedObjectPoolConfig<IHeadlessClient> config = new GenericKeyedObjectPoolConfig<>();
config.setMaxTotalPerKey(-1);
config.setMaxIdlePerKey(-1);
config.setBlockWhenExhausted(false);

clientPool = new GenericKeyedObjectPool<>(new BaseKeyedPooledObjectFactory<...>() { ... });   // <-- config NOT passed
clientPool.setTestOnBorrow(true);
```

`config` is built and then dropped; the `GenericKeyedObjectPool` constructor is called with the factory only. A grep of `servoy_mcp/src` shows no other use of `config`. So the pool runs on commons-pool2 defaults. I read these from the `commons-pool2-2.4.2` sources in the repo. The target platform uses 2.13.1 (`servoy-eclipse/launch_targets/com.servoy.eclipse.target.target`). I did not read the 2.13.1 sources, and I believe these defaults are unchanged:

| Setting | Actual value | Class Javadoc / ticket claim |
|---|---|---|
| `maxTotalPerKey` | **8** | -1 (unbounded) |
| `maxIdlePerKey` | 8 | -1 |
| `blockWhenExhausted` | **true** | false |
| `maxWait` | **-1 (wait forever)** | n/a |
| `maxTotal` (across keys) | -1 (unbounded) | n/a |
| eviction (`timeBetweenEvictionRuns`) | disabled | none (matches) |

Real behaviour today:

- The pool is **not** unbounded per key. The 9th concurrent call for the same `solution|user|tenant` **blocks indefinitely**. The servlet thread hangs, with no log line and no timeout. That is a different and arguably worse DoS (thread exhaustion) than the one the ticket describes.
- The number of distinct keys is still unbounded (`maxTotal=-1`). Each key can hold up to 8 idle licensed headless clients, and with no eviction they stay forever.
- The existing tests (`McpRuntimePoolTest`) never exceed one or two clients per key, so nothing caught this.

Consequence: the "deliberately unbounded" design comment (class Javadoc, `McpRuntime.java:57-59`, "The client pool is unlimited on purpose") has never matched runtime behaviour. Any fix has to start by actually applying a config. A simple "add a cap of 100" on top of the current code would be a no-op.

### Finding B: the shutdown gap is real.

- `McpRuntime.shutDown()` (`McpRuntime.java:175`) is static and public. `grep McpRuntime.shutDown` finds no caller in `src`.
- `McpServlet` (`@WebServlet("/mcp/*")`) has no `destroy()`.
- The servlet is registered by `startup/ServiceProvider` (root context, developer Tomcat via the `org.apache.tomcat.serviceprovider` extension point) and by `@WebServlet` annotation scanning in a WAR/server deployment (commit `66e372621`).
- Servlet `destroy()` is therefore the natural hook. The bundle has no other lifecycle object: no `Activator`, no `ServletContextListener`.
- `shutDown()` closes the pool, which destroys the idle clients through `destroyObject()` → `shutDown(true)`, then clears `solutionServers` and nulls `instance`.
- A later `getInstance()` recreates the runtime, so a servlet destroy followed by a context reload is safe.
- Caveat, not verified: pool2 `close()` only destroys idle objects. Clients out on loan at that moment are destroyed when returned to a closed pool. I believe that is how `returnObject` behaves, but I did not confirm it in the 2.13.1 sources.

## Ticket premise check

- **Item 2 (wire shutdown into the servlet lifecycle):** holds up. It is minimal and correct.
- **Item 1 (soft cap with WARNING):** the intent is right, but the premise is wrong (see Finding A: config unused, actual limit is 8 and blocking). Two further problems with the proposal as written:
  - A **per-key** cap of 100 does not address the issue the description names. The key includes user and tenant, so the attack or runaway surface is key cardinality: many distinct identities, each with its own pool. `maxTotalPerKey` bounds nothing there. A bound across keys (`maxTotal`) and/or idle eviction is what limits total clients.
  - "Do not fail silently, emit a WARNING" is ambiguous. With `blockWhenExhausted=false`, pool2 throws `NoSuchElementException("Pool exhausted")` from `borrowObject` when the cap is hit. The WARNING has to be logged by us around `getClient()`, because pool2 does not log it.
- **Design-intent conflict:** the author deliberately documented "unlimited on purpose" in the `McpRuntime` Javadoc (commit `9f650f6ca`, SVY-21326, `[ai]`). The ticket reverses that decision, which is fine because the ticket is the follow-up. The Javadoc must be rewritten as part of the fix; otherwise it would contradict the code.
- `AGENTS.md` §5 ("rate limiting is an infrastructure concern") targets auth endpoints (`StatelessLoginHandler`, `DefaultLoginManager`, `OAuthHandler`). A resource safety cap on a client pool is not request rate limiting or an attempt counter, so it does not conflict. A reviewer should still see this stated explicitly.

## Approaches considered

1. **Apply a real pool config with a high soft cap, plus WARNING on exhaustion, plus servlet `destroy()` → `McpRuntime.shutDown()`. (recommended)**
   - Pass `config` to the `GenericKeyedObjectPool` constructor with `maxTotalPerKey=100` and `blockWhenExhausted=false`. Choose `maxIdlePerKey` deliberately; the ticket does not specify one.
   - Catch `NoSuchElementException` in `McpRuntime.getClient()`, log a prominent WARN (solution, key, cap) and rethrow. The tool call then fails with the existing generic error from `McpSolutionServer.call`.
   - Pros: small, matches the ticket, removes the silent infinite-block behaviour, simple to test.
   - Cons: on its own it does not bound key cardinality (see approach 2).
2. **Approach 1 plus a global bound and idle eviction** (`maxTotal` of a few hundred, `timeBetweenEvictionRuns` plus a min idle time so abandoned per-user clients are released).
   - Pros: it actually addresses the "identities drive the key" and "no idle eviction" concerns in the description. It also releases licences held by idle users.
   - Cons: goes beyond the two literal asks. `maxTotal` needs a number the deployment may want to tune, which touches the "licence cost is the deployment's decision" intent.
   - Needs a human call on whether to include it.
3. **Warn-only soft threshold, never fail.** Keep it unbounded and log WARN when `getNumActive(key)` crosses a threshold, for example in the factory's `create()`.
   - Pros: no behaviour change for legitimate heavy users.
   - Cons: not a safety cap, and it does not stop the resource exhaustion the ticket is about. It reads the ticket's "ceiling" too loosely.
4. **No code change.**
   - Pros: none useful.
   - Cons: the shutdown leak is real. Config is silently ignored, so calls block forever once 8 are concurrent. Not acceptable.

## Recommendation

**PROCEED with approach 1.** Include the idle-eviction part of approach 2 if the human agrees, because without it the cap is mostly cosmetic for the distinct-identity scenario.

Concrete shape:

1. `McpRuntime`: pass `config` into the pool constructor. Set `maxTotalPerKey` to 100 (a named constant), `blockWhenExhausted=false`, and an explicit `maxIdlePerKey`. Rewrite the "unlimited on purpose" Javadoc. In `getClient()`, catch `NoSuchElementException`, log a WARN naming the cap and the key, and rethrow. Keep the cap overridable through the package-private constructor so a test does not need 100 clients.
2. `McpServlet`: add `destroy()` calling `McpRuntime.shutDown()`.
3. Tests, in `servoy_mcp_tests` beside `McpRuntimePoolTest`:
   - The cap is enforced: the borrow after the cap throws, and the WARN is logged.
   - After `shutDown()` all pooled clients get `shutDown(true)`.
   - Regression for the dropped-config bug: the pool reports the configured `maxTotalPerKey`, not 8.

Questions for the human decision-maker (they do not block PROCEED):

- Include `maxTotal` and idle eviction in this ticket, or open a separate one?
- On hitting the cap, fail the call (as proposed) or just warn?

## Git history findings

- `McpRuntime.java` is entirely from `9f650f6ca` (2026-08-28, "SVY-21326 MCP server for Servoy solutions [ai]"), the initial implementation. The `config` object has been built-but-unused since the first commit. `blame` on lines 55-62, 108-118 and 172-192 returns that single commit.
- `66e372621` (2026-08-31) changed the servlet to `@WebFilter`/`@WebServlet`-style auto-loading under `/mcp` and added the `org.apache.tomcat.serviceprovider` extension (`ServiceProvider`, `plugin.xml`). Its message says the server-side registration that would have carried a lifecycle hook was removed. That is how `shutDown()` lost its only caller.
- No prior spec for SVY-21630 or SVY-21326 under `docs/`.
- The target platform has commons-pool2 2.13.1. Defaults above come from the 2.4.2 sources in the repo.
