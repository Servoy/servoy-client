# SVY-21325 — Peer Review Summary

**Risk: LOW.** The change in `servoy-client@51e0f4b93` only widens existing catch blocks in
`PluginManager.java` and makes converter/validator manager construction unconditional — no
happy-path change, inherited uniformly by all three client types (smart/headless/NG),
consistent with the file's own established `catch (Throwable)` style. The companion
`build@fcc3ea84` commit is a text-only `RELEASE-NOTES` fix.

## Manual test plan

1. **Plugin resilience:** deploy two plugins into the server's `plugins/` directory — one
   normal, well-behaved plugin, and one deliberately broken (e.g. compiled against
   `javax.servlet` instead of `jakarta.servlet`, or any `IPlugin` provider that throws during
   `ServiceLoader` instantiation). Start a client (smart, headless, or NG) and confirm:
   - the server log shows a `Debug.error` entry for the broken plugin rather than a hard
     failure;
   - the well-behaved plugin still loads and is usable;
   - no NPE occurs when touching column converters/validators (e.g. open a form with a
     converted column).
2. **Full plugin-load failure:** if feasible, make every plugin in the directory fail to
   load, and confirm the client still starts (managers end up empty but non-null) rather
   than crashing.
3. **RELEASE-NOTES accuracy:** build a full distribution and diff the bundled
   `RELEASE-NOTES` against the actual Tomcat jar version shipped, confirming both report
   the same version with no mismatch.

## Possible improvements / follow-ups

- Catching `Throwable` (not `ServiceConfigurationError`/`Exception`) at both
  `PluginManager.java:211` and the outer `loadClientPlugins()` wrapper also swallows
  `OutOfMemoryError`/`StackOverflowError`. Consistent with this file's pre-existing style
  and plugins are trusted local JARs, so non-blocking — but worth confirming with the
  author whether it's intentional, and syncing the spec (which describes
  `ServiceConfigurationError`) to match.
- No automated test exists for "one plugin fails to load via `ServiceLoader`, a healthy one
  still loads, and all three managers end up non-null" — the exact scenario this fix
  targets. Adding one (a failing + a healthy `IPlugin` provider) would lock in the fix.
- `docs/SVY-21325-triage.md` still recommends "No code change is needed," which the shipped
  diff then contradicts with no reconciling note. Worth a one-line update so a future
  reader of the triage doc isn't surprised by the diff that followed it.

## Scope reviewed

- `servoy-client` branch `lts_2026`, commit `51e0f4b93` —
  `servoy_shared/src/com/servoy/j2db/plugins/PluginManager.java` + 2 new docs.
- `build` branch `lts_2026`, commit `fcc3ea84` —
  `eclipse_build/build/server/RELEASE-NOTES`.
