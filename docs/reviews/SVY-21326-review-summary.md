# SVY-21326 — Peer review summary

**Risk: MODERATE.** No blocker. The feature (an MCP server exposing a solution's `@Tool` scope functions over HTTP, with authentication delegated to the solution's own authenticator) is implemented soundly and the user/tenant isolation holds up; the rating is driven by operational gaps around a public endpoint — an unbounded, never-evicted client pool with no production shutdown drain — now tracked as a follow-up.

Reviewed scope (net landed state): servoy-client `05700dd9..HEAD` (the `servoy_mcp` bundle + `servoy_mcp_tests` + `MCP_SERVICE` solution type + `SessionClient` filter), server `309373af..HEAD` (import-engine note + commented-out `invalidateAll()`), servoy-eclipse `06bae79b..HEAD` (`Activator` save-listener, `ModelUtils.isTestRunning()` rewrite, feature/launch/target wiring, WAR export). All three on `release`; cross-repo coupled — **servoy-client must merge forward first or together** (servoy-eclipse `com.servoy.eclipse.core` requires bundle `servoy_mcp`).

## Outcome of the review

- **Confirmed intended, no action:** import-time `invalidateAll()` is deliberately skipped (legacy import path; the WAR export carries the solution as a bundle). `ModelUtils.isTestRunning()` rewrite is a deliberate fix — detect the real running application, not the bundle presence.
- **Follow-up raised — SVY-21630:** add a soft DDoS cap on the client pool (e.g. 100 per key) that logs a prominent warning when hit, and wire `McpRuntime.shutDown()` into the `McpServlet` lifecycle so the pool is actually drained.
- **Separate fix to do (not yet filed):** on a tool failure, `McpSolutionServer.call` returns the raw exception message (or the Java class name when null) to the agent — `McpSolutionServer.java:269`. Return a generic descriptive message to the agent and log the real detail server-side so no solution/DB-specific info leaks.

## Manual test plan

Also posted on SVY-21326 for whoever verifies the case.

**Verifying the feature**
1. Point a developer at `servoy_mcp_tests/testresources/workspace` (the `mcp_sample` solution, its `mcp_sample_auth` module, the `resources` project with the `mcp_user` permission); run `testresources/create_mcp_tenant_demo.sql`.
2. `POST` a JSON-RPC `tools/list` to `/servoy-service/mcp/mcp_sample` with `Accept: application/json, text/event-stream` and `Authorization: Bearer mcp-demo-token-alice`; expect the `@Tool` functions, and expect any unsupported-parameter-type tool to be absent (logged reason).
3. `tools/call salesTools_listMyItems` as alice (tenant acme) and as `mcp-demo-token-bob` (tenant globex); each sees only its tenant's rows (`QBSelect` filtered; `countRows` raw SQL deliberately not).
4. Call with no `Authorization` and with a rejected token; expect refusals, with the authenticator's own message for the bad token.

**Regression checks**
1. Non-MCP WAR: export a plain NG solution, deploy, confirm it starts, existing `/servoy-service/*` resolve, `GET /<ctx>/servoy-service/mcp/anything` → 404 with no mapping conflict.
2. New Solution wizard / Solution Explorer: create an MCP Service solution; usable and labelled "MCP Service" (generic tree icon — cosmetic).
3. Developer save with MCP never used → negligible cost; after one `/mcp` request, a save flushes only the solution + its modules, not the whole repository.
4. `isTestRunning()`: normal launch → `false` (Node/titanium extraction and the postgres prompt run); PDE plugin test → `true`.
5. Pool lifecycle: exercise with several distinct tokens, observe client/memory growth and whether app-server stop drains the pool (SVY-21630).

**Automated:** `servoy_mcp_tests` (~120 JUnit, unit + integration tags, ~1s, no OSGi); a Spotbugs pass on `servoy_mcp` before release (not run during review).

**Surfaces:** headless/WAR runtime and the developer. No coverage needed for smart client, NG client runtime or mobile — untouched, no UI in this change.

## Possible improvements / follow-ups

- **SVY-21630** — pool cap + warning log, and `shutDown()` wired into the servlet lifecycle.
- **Error-message generalisation** (`McpSolutionServer.java:269`) — generic message to the agent, real detail to the log. To be done as a separate task.
- **Documentation drift** in `docs/SVY-21326.md` (predates the final shape): SDK is in fact added to the target platform (doc says kept out); the servlet is wired via `plugin.xml`/`ServiceProvider` + `web-fragment.xml`, not `registerWebService(...)` in `ApplicationServer.startServices()`; import-time `invalidateAll()` is commented out; class count ("Thirteen") trails the ~18 that landed.
- **CVE/advisory check** on the new pinned deps `mcp-core 2.0.1`, `reactor-core 3.8.7`, `reactive-streams 1.0.4` — not performable during the review.

## Scope reviewed

servoy-client `05700dd9..HEAD`, server `309373af..HEAD`, servoy-eclipse `06bae79b..HEAD` (net landed state of SVY-21326 across the three repos).
