# Triage Report — SVY-21325

**Verdict:** NO_ACTION

## Reported problem
Loading a solution in 2026 LTS that works in 2025 LTS produces NPE:
`Cannot invoke "com.servoy.j2db.dataprocessing.IConverterManager.getConverter(String)" because the return value of "com.servoy.j2db.dataprocessing.FoundSetManager.getColumnConverterManager()" is null`

## Root-cause assessment
The reporter was using plugins compiled against the old `javax.servlet` API (Tomcat 8 era). In 2026 LTS the runtime uses Tomcat 11 which requires the `jakarta.servlet` namespace. When those incompatible plugins fail to load, the `PluginManager.getColumnConverterManager()` returns null, `FoundSetManager.setColumnManangers()` is never called with a valid converter manager, and subsequent calls to `getColumnConverterManager().getConverter(...)` throw an NPE.

The reporter confirmed this in comments: updating their plugins to use the correct `jakarta.servlet` / Velocity version resolved the issue entirely.

Johan Compagner (architect) confirmed the diagnosis from the log file — old `javax.servlet` plugin bindings were the cause.

## Ticket premise check
The ticket's implicit premise is that Servoy 2026 LTS introduced a regression. This does not hold up — the runtime is working correctly. The breakage was caused by user-side plugins compiled against an incompatible servlet API version.

The reporter's remaining ask is to update `application_server/server/RELEASE-NOTES` to correctly reference Tomcat 11 instead of Tomcat 8. This file does not exist in the `servoy-client` repository — it belongs to the application server repository.

## Approaches considered
1. **Add null-safety checks around `getColumnConverterManager()` calls** — Would mask configuration errors (broken plugins) rather than surfacing them clearly. Contra: the NPE is actually a useful signal that plugin initialization failed. Pros: slightly friendlier error. Cons: hides the real problem, adds defensive noise to ~12 call sites.

2. **No code change in servoy-client** — The system works correctly when properly configured. The reporter resolved their own issue. The remaining documentation fix is in a different repository. Pros: no risk of masking real problems, no unnecessary code churn. Cons: none.

3. **Improve error messaging when converterManager is null** — Wrap calls with a check that throws a descriptive exception ("Column converter manager not initialized — check plugin compatibility"). Moderate effort for marginal benefit since the NPE message already points to the exact problem location.

## Recommendation
No code change is needed in the `servoy-client` repository. The issue was entirely user-side (incompatible plugin versions using `javax.servlet` instead of `jakarta.servlet`). The reporter self-resolved.

The only remaining action is a documentation fix: update `application_server/server/RELEASE-NOTES` to reference Tomcat 11 instead of Tomcat 8. This file is in the application server repository, not this one.

## Git history findings
None relevant — the `getColumnConverterManager()` method and `setColumnManangers()` are stable, long-standing code with no recent changes that would introduce a regression.
