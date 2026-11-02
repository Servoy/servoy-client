# Triage Report — SVY-21677

**Verdict:** PROCEED

## Reported problem

Servoy Developer becomes unresponsive while Servoy AI (the MCP agent) edits a form.
A thread dump shows one `ForkJoinPool.commonPool-worker` RUNNABLE and burning a full
core (~9,000,000 ms ≈ 2.5 h of CPU, matching uptime) spinning inside
`WebCustomType.<init>` → `createNewInstance` → `WebCustomType.<init>` …, triggered by an
`applyPatch` on a web component. The spinning thread holds the Eclipse workspace resource
lock, so every other MCP edit (`applyPatch` / `replaceString` / `insertIntoFile`) parks
`WAITING` on `Workspace.prepareOperation` / `OrderedLock.acquire`. Writes to the workspace
never complete; the IDE is wedged. The ticket's own description calls it "a genuine
runaway/deadlock … triggered by editing an AG Grid component's custom types via the MCP
patch path."

This is the same defect as **SVY-21675** (linked by subject), whose attached thread dumps
and agent trace I used as the primary evidence (`threaddump-1-1210-50.txt`,
`threaddump-5-1647-52.txt`, `opencode-agent-trace-2026-10-23.md`). The ticket proposes no
specific code fix — it only diagnoses the symptom and concludes "don't retry, the thread
won't clear."

## Root-cause assessment

The runaway is **not** an AG Grid problem and **not** the OOM already fixed under
SVY-21574. It is an unbounded loop in `WebComponent.setJson(...)` combined with the
`WebCustomType` constructor, hit when a persist-mapped custom-type array element is written
**without a `svyUUID`**. In SVY-21675's trace the component is a `bootstrapcomponents-tabpanel`
whose `tabs` (a `tab[]` custom-object array, `tabpanel.spec` line 25/234) the agent wrote
with plain tab objects and **no `svyUUID` key** (verified in the trace: 13 writes of the
`tabs` array, zero `svyUUID`/`_id` on any tab object).

Mechanics, all verified in the current `master` source
(`8da91326b`, 2026-10-30):

1. `AbstractRepository.updatePersistWithValueMap` → `AbstractBase.copyPropertiesMap`
   (line 173) → `WebComponent.setProperty("json", …)` (line 121) →
   `AbstractBase.setProperty` reflects into `WebComponent.setJson(...)`
   (`WebComponent.java:300` in the dump). This is the deserialize path when the `.frm` is
   re-read after the patch (`SolutionDeserializer.updateSolution`).
2. In `WebComponent.setJson` the `customTypesInitialized` branch (lines 257-310) iterates
   the `tabs` array `arr = arg.opt("tabs")`. `arg` was just stored as the component's own
   json via `setTypedProperty` (line 252), so `arr` **is** the component's live json array.
3. For each element it reads `childUUID = Utils.getAsUUID(jsonObject.optString(UUID_KEY,
   null), false)`. The tab objects have no `svyUUID`, so `childUUID == null` on every
   iteration. `getChild(null)` returns `null` (`AbstractBase.java:661`), so
   `existingCustomType` is always `null` and it calls
   `WebCustomType.createNewInstance(this, …, i, null)` (lines 300-302).
4. `createNewInstance` with a `null` uuid allocates a **fresh random UUID**
   (`WebCustomType.java:87`: `uuid != null ? uuid : UUID.randomUUID()`).
5. The `WebCustomType` constructor reads the parent's json
   (`getOwnProperty("json")` — the same `arr` array), scans it for an element whose
   `svyUUID` equals the brand-new random UUID (`WebCustomType.java:120-131`) — never found —
   then at the insert branch (`index == i`, which is `>= 0` and `< customTypesArray.length()`
   pointing at a non-null slot) takes the **shift-and-insert** path (lines 142-150), which
   **grows the array by one** and stamps the new random UUID onto the inserted object.
6. Control returns to the `setJson` loop. `arr.length()` has now increased by one, so the
   loop counter `i` can never reach the end. Each pass inserts one more element and
   allocates one more `ServoyJSONObject` — a true unbounded loop, pegging a core and
   growing the heap, exactly matching the dump (`setJson:300 → createNewInstance:86 →
   <init>:120`, single RUNNABLE worker, 19 others WAITING on the workspace lock).

The deadlock symptom is secondary: the loop runs under the Eclipse workspace lock held by
`CodeEditingService.applyPatch` → `File.setContents`, so no other MCP write can proceed.
The lock is a victim, not the cause; fixing the loop fixes the hang.

### Why the existing guards don't cover this

- **SVY-21574** (`e78a4f623`) only guards `WebCustomType.<init>` against an *out-of-range*
  index (`index > customTypesArray.length()`) to stop org.json padding/OOM. Here the index
  `i` is always a *valid* in-range slot, so that guard is never taken; the growth comes from
  the shift-insert branch, not from padding.
- **SERVOY-295** (`f7ee10f25`) fixed a *different* recursion (the `getProperty` fallthrough
  cycle). That path does not appear in this dump.
- The `getChild(childUUID)` short-circuit in `setJson` (SVY-20784, `d7422689f`) was meant to
  reuse existing children, but it is defeated the moment `childUUID` is `null`, which is
  precisely the agent-written-without-svyUUID case.

So the loop is a latent bug in `WebComponent.setJson` / `WebCustomType.<init>` that any
custom-object-array element lacking a `svyUUID` can trigger; the AI agent is just the first
producer that reliably writes such `.frm` content (a human using the designer always gets a
developer-assigned `svyUUID`).

## Ticket premise check

The ticket (and SVY-21675) correctly identify **where** the process is stuck and correctly
rule out "flaky tool / just retry." Their framing as an *AG Grid* issue is too narrow — the
reproducing trace is a **tabpanel `tabs` array**, and the defect is generic to any
persist-mapped custom-object array written without `svyUUID`. The ticket proposes no code
fix, so there is nothing to over-trust; the one inference to correct is the "AG Grid
custom types" attribution — the trigger is missing-`svyUUID` array elements in general, not
AG Grid specifically.

## Approaches considered

1. **Break the unbounded loop at the source — generate a svyUUID and reuse children by
   index when the UUID is missing.** In `WebComponent.setJson` (and the symmetric
   `initCustomTypes` / `WebComponent.java:290-305` array branch), when
   `childUUID == null`, mint a `UUID.randomUUID()`, write it back into the `jsonObject`
   (as `WebComponent.initCustomTypes` already does at lines 519-524 for the
   `!customTypesInitialized` path), and pass that uuid to `createNewInstance`. This both
   stops the fresh-random-UUID-per-pass growth and makes the array self-heal with stable
   ids.
   - Pros: fixes the actual cause; mirrors the existing `initCustomTypes` null-UUID
     back-fill already in the file, so it is a consistent, well-understood change;
     protects every custom-object array (tabs, aggrid columns, anything), not just the one
     component in the trace; small and testable with a plain `*Test` in
     `servoy_ngclient.tests`.
   - Cons: touches the shared custom-type deserialize path used by every web component —
     needs a regression test and a careful check that the existing
     `WebComponentCloneTest` / `WebCustomTypeAddChildTest` / `WebCustomTypeNamePropertyTest`
     still pass.

2. **Harden `WebCustomType.<init>` so it never grows the array it is scanning / add a
   loop-invariant guard in `setJson` (snapshot `arr.length()` before the loop, or iterate a
   defensive copy).** Snapshot the length once (`int n = arr.length();`) and/or make the
   constructor's insert path not re-grow an array during a bulk initialize.
   - Pros: defends against the same shape even if some other caller forgets a svyUUID;
     cheap.
   - Cons: a snapshot alone still creates N children with N fresh random UUIDs that don't
     match the json and get re-created next reload — it stops the *infinite* spin but leaves
     duplicate/unstable ids. Best combined with approach 1, not used alone.

3. **Fix it upstream in the MCP `CodeEditingService` / agent guidance so generated `.frm`
   custom-type array elements always carry a `svyUUID`.** (That code lives in the
   `servoy-eclipse` repo, not here.)
   - Pros: removes the trigger at the producer.
   - Cons: does not fix the latent runtime bug — any other producer (import, hand-edited
     `.frm`, older solution) can still wedge the IDE. Defense-in-depth at best, and out of
     scope for this repository. Should be a separate linked ticket, not the fix for this one.

4. **No code change.**
   - Pros: none beyond zero effort.
   - Cons: unacceptable — the IDE hard-hangs on a single core for hours, holding the
     workspace lock, with no recovery short of killing Developer, and the trigger recurs on
     every reload of the bad `.frm`. This is a reproducible runaway in product code, not a
     user misconfiguration. Rejected.

## Recommendation

**PROCEED** with **Approach 1**, optionally hardened with the length-snapshot from
Approach 2 as a belt-and-braces guard.

Fix the loop where it originates: in `WebComponent.setJson`'s `customTypesInitialized`
array branch (and the matching object branch), when the element has no `svyUUID`, mint a
`UUID.randomUUID()`, **write it back into the `jsonObject`**, and pass that uuid to
`createNewInstance` — exactly as `WebComponent.initCustomTypes` already does on the cold
path (`WebComponent.java:519-524`). With a non-null, stable uuid, (a) the constructor's
UUID scan finds/keeps a single element instead of inserting a new one every pass, so the
array stops growing and the loop terminates, and (b) `getChild(childUUID)` resolves on the
next reload so children are reused rather than duplicated. Additionally snapshot
`arr.length()` into a local before the loop so a stale length can never be re-read mid-loop.

Add a `servoy_ngclient.tests` regression test (plain `*Test`, no OSGi — alongside
`WebCustomTypeAddChildTest`) that builds a tabpanel-style component whose `tabs` array
elements have **no `svyUUID`**, runs it through the `setJson` deserialize path under a short
preemptive timeout (as SVY-21574's `WebCustomTypeOutOfRangeIndexTest` does), and asserts it
terminates, the array length is unchanged, and every element ends up with a stable
`svyUUID`. Re-run `WebComponentCloneTest`, `WebComponentCloneMapIsolationTest`,
`WebCustomTypeAddChildTest`, `WebCustomTypeNamePropertyTest` and
`WebCustomTypeOutOfRangeIndexTest` to confirm no regression.

Recommend also filing a **separate linked ticket in `servoy-eclipse`** so the MCP
`CodeEditingService` / agent writes a `svyUUID` on generated custom-object array elements
(Approach 3) — defense-in-depth at the producer, but not a substitute for the runtime fix
here.

Alternatives if Approach 1 is rejected: Approach 2 (snapshot/guard only) stops the infinite
spin but leaves unstable ids and is weaker; Approach 3 alone leaves the latent runtime bug
in place.

## Git history findings

- `WebComponent.setJson` array/object custom-type reconciliation (the loop,
  lines 273-305): **SVY-20784 `d7422689f`** (lvostinar, 2026-05-12) "refactor the flattened
  stuff … (fix e2e tests)". The `getChild(childUUID)`-reuse optimization that the null UUID
  defeats was introduced here.
- `WebCustomType.<init>` array index handling (the shift-insert that grows the array,
  lines 136-166): base **SVY-20784 merge `f98f7e5c7`** (2026-06-06), with the out-of-range
  OOM guard added later by **SVY-21574 `e78a4f623`** (Johan Compagner, 2026-10-12). That
  guard only covers `index > length`, not the valid-in-range insert that drives this loop.
- `WebCustomType.getProperty` recursion fix: **SERVOY-295 `f7ee10f25`** (2026-08-27) — a
  different recursion, not the one in this dump.
- No prior `docs/SVY-21677-*` or `docs/SVY-21675-*` spec/triage exists. Related existing
  docs: `docs/SVY-21475-webcustomtype-name-property.spec.md`.
- No dependency/target-platform version bump is implicated; this is pure product logic.
