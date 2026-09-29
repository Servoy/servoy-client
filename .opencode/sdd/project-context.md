# Project Context — Servoy Runtime (servoy-client)

This project is the **Servoy Runtime** — the core engine code including persistence,
shared runtime logic, NG web client, smart client, headless client, and debugger support.
It is built as a multi-project Eclipse workspace consisting of 8 OSGi plugin bundles.

## SDD variant

This repo uses the **sdd-java-eclipse** shared skill (Java / Eclipse-OSGi pipeline).

## Technology stack

| Aspect | Value |
|--------|-------|
| Java version | 17 |
| Build system | Maven with Eclipse Tycho (eclipse-plugin packaging) |
| Module system | OSGi (each project is a bundle with MANIFEST.MF) |
| UI framework | Java Swing (smart client), Sablo/WebSocket (NG client) |
| Web framework | Angular (NG client frontend), Tomcat (embedded server) |

## Project structure

| Module | Purpose |
|--------|---------|
| `servoy_base` | Base persistence, querying, and Solution Model APIs |
| `servoy_shared` | Shared runtime logic, database connectivity, Rhino scripting engine |
| `servoy_smart_client` | Java Swing desktop client |
| `servoy_headless_client` | Headless/server-side execution, servlet integration |
| `servoy_ngclient` | NG Web Client (HTML5/Angular/WebSockets via Sablo) |
| `servoy_ngclient.tests` | Unit tests for servoy_ngclient (Fragment-Host) |
| `servoy_debug` | Debugger capabilities for Servoy Developer |
| `servoy_doc` | Documentation XML generator (standalone Maven build) |

## Key architectural layers

- `servoy_base` → lowest layer, no UI, no platform-specific code
- `servoy_shared` → re-exports `servoy_base`, central engine for all clients
- `servoy_smart_client` / `servoy_headless_client` / `servoy_ngclient` → client implementations built on `servoy_shared`
- `servoy_debug` → developer tooling, depends on Eclipse SWT

## Eclipse plugin development essentials

When writing code for this project, you are writing **OSGi bundles**:

### Dependencies
- Declare in `META-INF/MANIFEST.MF` under `Require-Bundle` or `Import-Package`
- Use `eclipse-pde_getActiveTarget` to check what target is currently active
- If a dependency is already in the target platform, just add it to MANIFEST.MF

### Packages & visibility
- Export public API packages in MANIFEST.MF `Export-Package`
- Keep internal packages unexported
- Never reference another plugin's internal packages

## Code conventions

- Follow existing patterns in neighboring files — consistency over personal preference
- Use try-with-resources for all `Closeable` resources
- Use `volatile` or proper synchronization for shared mutable state
- Log via `Debug` class or SLF4J (check what the module uses)
- No `System.out.println` — use proper logging
- Prefer existing utility classes (check `com.servoy.j2db.util`)
- Servoy has its own `com.servoy.j2db.util.UUID` — do NOT use `java.util.UUID` where the codebase expects the Servoy type

## Testing

- Unit tests (pure logic, no OSGi): live in a `<plugin>.tests` fragment (e.g. `servoy_ngclient.tests`),
  run with `eclipse-ide_runClassTests`. Class suffix `*Test`.
- Integration tests (needs OSGi/workspace): `eclipse-test-plugin` packaging, run with
  `eclipse-pde_runJUnitPluginTestClass`. Class suffix `*IntegrationTest`.
- `servoy_ngclient.tests` is a `Fragment-Host: servoy_ngclient` fragment, giving package-private access.
- See `AGENTS.md` `## Testing` for the catalogue of existing feature test classes.

## Known design decisions (DO NOT CHANGE)

Read `AGENTS.md` section 5 for critical design decisions that must not be modified:
- SecuritySupport DESede with hardcoded passphrase (`background.gif`) — intentional obfuscation
- OAuthHandler `sendRedirect(loginFailedUrl)` — NOT an open redirect (false positive)
- Refresh token embedded in Servoy JWT — intentional stateless design trade-off
- Rate limiting is an infrastructure concern, not application-layer

## AGENTS.md

Always read `AGENTS.md` at the start of your work — it contains the full tool usage policy,
workflow requirements, post-edit checklist, and the `[ai]` commit-subject convention (with
the Jira case number) that you must follow.

## Gotchas

- **MANIFEST.MF formatting:** Strict 72-byte line-length limits. Use eclipse-coder tools to
  edit, or let `eclipse-coder_formatFile` handle it. A broken MANIFEST.MF = bundle won't load.
- **Plugin pom.xml is NOT for dependencies:** The `pom.xml` in a Tycho plugin project is only
  for build configuration. Runtime dependencies come from MANIFEST.MF + target platform.
- **Require-Bundle vs Import-Package:** Prefer `Require-Bundle` for Servoy internal bundles.
  Use `Import-Package` for third-party libraries.
- **build.properties matters:** New folders must be in `bin.includes` or they won't be in the JAR.
- **No JUnit in production MANIFEST:** Test dependencies belong only in test project bundles.
