# Triage Report — SVY-21675

**Verdict:** NO_ACTION

## Reported problem

Servoy Developer **2026.9.1, build 4203** (builddate 2026-10-22) hangs: the IDE is stuck
constantly "Refreshing workspace" and performance is badly degraded while an AI/MCP agent
is issuing file edits (`com.servoy.eclipse.developer.mcp.*` MCP server on port 8183).

Four thread dumps (first set 12:10–12:13, second set 16:47–16:48, same build) all show one
`ForkJoinPool.commonPool-worker` thread in a **CPU-bound loop** (~95–100 % of one core,
~1,970 s CPU by 16:48, heap churn up to ~2 GB) with an identical leaf frame:

```
com.servoy.j2db.persistence.WebCustomType.<init>(WebCustomType.java:120)
com.servoy.j2db.persistence.WebCustomType.createNewInstance(WebCustomType.java:86)
com.servoy.j2db.persistence.WebComponent.setJson(WebComponent.java:300)
... AbstractRepository.updatePersistWithValueMap / SolutionDeserializer.updateSolution
... ServoyModel$26.resourceChanged -> Workspace.broadcastPostChange -> Workspace.endOperation
... File.setContents -> CodeEditingService.applyPatch <- ServoyCoderServer.applyPatch
... ToolExecutor -> ForkJoinPool
```

Because that worker holds the Eclipse **workspace lock** (it runs inside
`File.setContents → endOperation → broadcastPostChange`, which fires resource-change
listeners synchronously), every other MCP tool call, the "Refreshing workspace" job and
the "Compacting resource model" job all block behind it. The agent just sees ~60 s tool
latency and keeps firing edits into a locked workspace.

The **proposed solutions** in the ticket (noted, not assumed correct) are five: (1) guard
`WebCustomType` line 120 against bad/cyclic input; (2) move `SolutionDeserializer`
reparsing out of `resourceChanged` so it does not run under the workspace lock; (3) serialise
parallel MCP `File.setContents` calls / batch them; (4) give `ToolExecutor` its own executor
instead of the common pool; (5) add a timeout + cancellation to the `future.get()` in
`McpServerFactory.java:258`. The ticket itself flags the root cause as "preliminary … not
yet verified against source" and the trigger as "hypothesis only".

## Root-cause assessment

The reported engine-side defect — a `WebCustomType` custom-type array driving the loader
into a multi-GB allocation / CPU spin from a corrupt array index — **is already fixed in
the engine source**, and the fix is **already on the same release line as the build that
would carry this forward**. The ticket is reported against an older build that predates the
fix, which matches the user's own hypothesis ("maybe this is already fixed in master").

Evidence:

1. **The spin leaf is the custom-type array handling in `WebCustomType`.** The shipped
   build-4203 line numbers map exactly: `WebCustomType.java:120` is the linear scan
   `for (int i = 0; i < customTypesArray.length(); i++)` that walks the custom-type JSON
   array looking for the matching `svyUUID`, and `:86` is `createNewInstance`. The caller
   `initCustomTypes()` iterates `arr.length()` and, for each element, the constructor
   re-scans the whole array — i.e. **O(n²)** over the array. (Verified against the 2026.9.1
   version-bump tree `4624e973d`, which has the identical source structure and line numbers
   as the reported stack.)

2. **The trigger is a custom-type array with a corrupt/oversized index.** Before the fix,
   `WebCustomType.<init>` fed the array index straight into
   `org.json.JSONArray.put(index, value)`. `org.json` **pads the backing `ArrayList` with
   nulls up to `index`**, so a damaged `.frm` whose custom-type element carried a corrupt
   index (hundreds of millions) allocated gigabytes (the heap churn up to ~2 GB in this
   ticket) and the subsequent O(n²) rescan over the padded array is exactly the CPU spin
   observed. This is the **same failure signature** as the already-resolved **SVY-21574**
   ("columns array size ~1.8 billion, OOM in
   `WebComponent.initCustomTypes → WebCustomType.createNewInstance → JSONArray.put`",
   `globis_logistics` heap dump). SVY-21675's trigger is also an agent-generated form with a
   duplicated custom-type array element (the ticket comments point at `order_tabs.frm` with
   duplicate `containedForm` / `tabs` array entries) — the same class of damaged array.

3. **The fix exists and is on the release line.** Commit
   `e78a4f623 "SVY-21574 guard custom-type array index against OOM on solution load [ai]"`
   (Johan Compagner, authored & committed 2026-10-12) changes `WebCustomType.<init>` to only
   call `put(index, value)` when `index <= customTypesArray.length()` and to **append**
   (`put(value)`) with a `Debug.error` log for any out-of-range index — so a corrupt index
   no longer pads/allocates, and the array stays small (no gigabyte allocation, no
   billion-element O(n²) rescan). It is present on `origin/release` and `master`, and the
   **`2026.9.1` version bump `4624e973d` (2026-10-12 15:32) sits directly after it** — the
   fix is an ancestor of the bump (`git merge-base --is-ancestor e78a4f623 4624e973d` → yes).

4. **The reported build is from a divergent/older snapshot that misses it.** Build 4203
   (builddate 2026-10-22 11:58) corresponds to release commits around 2026-10-22; the
   nearest `WebCustomType`-structure-compatible snapshot on that path (`7fc62768`,
   2026-10-22 11:42) does **not** contain the SVY-21574 guard
   (`git merge-base --is-ancestor e78a4f623 7fc62768` → no; grep for the `SVY-21574` guard
   comment in that tree → empty). In other words the engine at the user's build still had
   the vulnerable `put(index, value)` padding path. The guard lands on the release/master
   line that carries 2026.9.1 forward.

The second, infrastructure-flavoured half of the ticket (MCP calls block on the workspace
lock; `future.get()` has no timeout; the common pool is saturated; `boundedElastic` /
Tomcat threads all wait) is **not in this repository**. `CodeEditingService`,
`ServoyCoderServer`, `ToolExecutor`, `McpServerFactory`,
`HttpServletStreamableServerTransportProvider` are all in
`com.servoy.eclipse.developer.mcp.*`, which lives in the **servoy-eclipse** (Developer)
repository, not in **servoy-client** (the runtime engine). Those symbols do not exist here.

## Ticket premise check

- The ticket's framing is honest: it labels the root cause "preliminary … not yet verified
  against source" and the trigger "hypothesis only". Verified against source, the engine-side
  spin **is** the custom-type-array loader, and it **is already fixed** by SVY-21574 on the
  release line.
- **Proposed fix (1) "guard WebCustomType line 120 against bad/cyclic input" — already done.**
  SVY-21574 guards precisely this path (the corrupt-index allocation that feeds the
  line-120 rescan). No new engine change is warranted in servoy-client for the spin.
- **Proposed fixes (2)–(5) are real but belong to a different repository.** They target the
  MCP server's threading/lock/timeout behaviour in `com.servoy.eclipse.developer.mcp.*`
  (servoy-eclipse). They are legitimate resilience improvements — the MCP server should not
  silently stall for 60 s behind the workspace lock, and `future.get()` without a timeout is
  a genuine robustness gap — but they cannot and should not be implemented here. They need a
  separate case against the Developer/MCP codebase.

## Approaches considered

1. **Re-fix the engine (guard `WebCustomType` again) in servoy-client** — pros: directly
   addresses the stack leaf. Cons: **redundant** — SVY-21574 (`e78a4f623`) already guards
   this exact corrupt-index path and is on the release/master line; re-doing it would
   duplicate or conflict with existing, tested code (`WebCustomTypeOutOfRangeIndexTest`).

2. **Harden the MCP server (lock handling, dedicated executor, `future.get()` timeout) —
   proposed fixes 2–5** — pros: removes the "whole IDE stalls / agent can't tell" class of
   problem and would have surfaced this fast instead of a 55-minute silent hang. Cons: **out
   of scope for servoy-client** — all those classes live in `servoy-eclipse`
   (`com.servoy.eclipse.developer.mcp.*`); needs its own case against that repo.

3. **No code change in servoy-client** — pros: the engine defect is already fixed
   (SVY-21574) and shipping on the release line; the remaining work is a Developer/MCP
   concern tracked elsewhere. Cons: the reporter still needs to (a) upgrade to a build that
   contains SVY-21574 and (b) get the MCP-resilience work tracked separately — neither is a
   servoy-client change.

## Recommendation

**NO_ACTION in servoy-client.** The engine-side root cause (corrupt custom-type array index
padding `JSONArray` and driving the O(n²) rescan at `WebCustomType.<init>`) is **already
fixed by SVY-21574 (`e78a4f623`)**, which is on `origin/release` and `master` and sits
directly after the 2026.9.1 version bump. The ticket was filed against build 4203, an older
snapshot that predates the guard — consistent with the reporter's own "maybe this is already
fixed in master" note. Verifying against a build that includes SVY-21574 should make the
spin/OOM go away.

Two follow-ups, neither of which is a servoy-client code change:

- **Verify / upgrade:** confirm the hang reproduces only on builds without SVY-21574, and
  retest on a current `release`/`master` build. If it still reproduces there with the same
  `order_tabs.frm`, reopen with the `.frm` attached (the ticket already asks the reporter to
  preserve `demo/forms/order_tabs.frm`) so the specific array shape can be added as a
  regression case alongside `WebCustomTypeOutOfRangeIndexTest`.
- **Separate MCP case (servoy-eclipse):** the "whole IDE stalls behind the workspace lock /
  no timeout on `future.get()` / common-pool saturation" resilience work (ticket's suggested
  fixes 2–5) should be tracked against the **Developer / MCP** codebase
  (`com.servoy.eclipse.developer.mcp.*`), which is not in this repository. That hardening is
  independent of the engine fix and would have turned a 55-minute silent hang into a fast,
  visible timeout.

## Git history findings

- `e78a4f623` — "SVY-21574 guard custom-type array index against OOM on solution load [ai]",
  Johan Compagner, authored/committed **2026-10-12 15:22**. Changes `WebCustomType.<init>`
  from `if (index >= 0) { if (index >= customTypesArray.length() || …) put(index,…) }` to
  `if (index >= 0 && index <= customTypesArray.length()) { if (index == length || …)
  put(index,…) }` with an `else` branch that **appends** (`put(value)`) and logs
  `Debug.error("Out-of-range custom-type index … appending instead of padding to avoid OOM
  (SVY-21574)")`. Adds `WebCustomTypeOutOfRangeIndexTest` (servoy_ngclient.tests, 5 tests,
  each under a 10 s timeout so a padding regression fails fast).
- `git merge-base --is-ancestor e78a4f623 4624e973d` → **yes**: the fix precedes the
  `2026.9.1` version bump (`4624e973d`, 2026-10-12 15:32) on the release line.
- `git merge-base --is-ancestor e78a4f623 7fc62768` → **no**, and the `SVY-21574` guard
  comment is absent from `7fc62768`'s `WebCustomType.java` — the build-4203-era snapshot
  (builddate 2026-10-22) on that path does not have the guard, so the reported build is
  vulnerable.
- The spin leaf itself (`WebCustomType.java:120`, the linear `svyUUID` scan loop) was
  introduced by `f98f7e5c7` (2026-06-06, "Merge branch '2026.03' into release") and was **not**
  modified by SVY-21574 — the fix removes the gigantic-array *cause* upstream of the loop
  rather than the loop, which is the correct place to fix it.
- `CodeEditingService`, `ServoyCoderServer`, `ToolExecutor`, `McpServerFactory`,
  `HttpServletStreamableServerTransportProvider` — not present in servoy-client; they belong
  to `com.servoy.eclipse.developer.mcp.*` in the servoy-eclipse repository.
