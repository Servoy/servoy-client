# Spec: SVY-21677 — Developer getting unresponsive because of Servoy AI

## 1. Goal

Stop Servoy Developer from hard-hanging (one `ForkJoinPool` worker pegging a core
indefinitely while holding the Eclipse workspace resource lock) when a web component's
persist-mapped custom-object array is re-read with array elements that have no `svyUUID`.
The fix breaks an unbounded loop in `WebComponent.setJson(...)` at its source: when a
custom-type array element lacks a `svyUUID`, mint a stable `UUID.randomUUID()`, write it
back into the element's JSON, and pass that UUID to `WebCustomType.createNewInstance(...)`
— mirroring the null-UUID back-fill that `WebComponent.initCustomTypes()` already performs
on the cold (`!customTypesInitialized`) path. This makes the array self-heal with stable
ids instead of growing by one element per loop pass, so the deserialize terminates and the
IDE no longer wedges. The Servoy AI (MCP) patch flow is the first producer that reliably
writes such UUID-less `.frm` content, but the defect is generic to any custom-object array
element missing a `svyUUID`.

## 2. Background

### 2.1 Symptom and root cause

A thread dump (SVY-21675, linked by subject) shows a single
`ForkJoinPool.commonPool-worker` RUNNABLE, having burned ~9,000,000 ms of CPU (≈ 2.5 h,
matching uptime), spinning inside
`WebComponent.initCustomTypes` → `WebCustomType.createNewInstance` →
`WebCustomType.<init>` → `initCustomTypes` → … . It was triggered by an MCP `applyPatch`
on a web component while the `.frm` was re-deserialized. Because the spinning thread holds
the Eclipse workspace resource lock, every other MCP write (`applyPatch` / `replaceString`
/ `insertIntoFile`) parks `WAITING` on `Workspace.prepareOperation` / `OrderedLock.acquire`
— the "IDE lock" the Developer reported. The deadlock is a secondary symptom; the lock is a
victim, not the cause. Fixing the loop fixes the hang.

The reproducing component in the trace is a `bootstrapcomponents-tabpanel` whose `tabs`
(a `tab[]` custom-object array) the agent wrote with plain tab objects and **no `svyUUID`**.
The ticket's attribution to AG Grid is too narrow: the defect fires for any persist-mapped
custom-object array element lacking a `svyUUID` (tabs, aggrid columns, anything).

### 2.2 The unbounded-loop mechanics (verified in current `master`)

`servoy_shared/src/com/servoy/j2db/persistence/WebComponent.java`,
`setJson(...)` — the `customTypesInitialized` (`else if (arg != null)`) branch, array case
(lines 286–306):

```java
else if (customTypesJSON instanceof JSONArray arr)
{
    for (int i = 0; i < arr.length(); i++)          // <-- live array, length re-read each pass
    {
        if (arr.opt(i) instanceof JSONObject jsonObject)
        {
            UUID childUUID = Utils.getAsUUID(jsonObject.optString(IChildWebObject.UUID_KEY, null), false);
            WebCustomType existingCustomType = (WebCustomType)getChild(childUUID);
            if (existingCustomType != null)
            {
                existingCustomType.setJson(jsonObject);
            }
            else
            {
                WebCustomType.createNewInstance(this, ..., propertyName, i,
                    Utils.getAsUUID(jsonObject.optString(IChildWebObject.UUID_KEY, null), false)); // <-- null uuid
            }
        }
    }
}
```

1. `arg` was just stored as the component's own json via `setTypedProperty` (line 252), so
   `arr` **is** the component's live json array.
2. The tab objects have no `svyUUID`, so `childUUID == null` every iteration.
   `getChild(null)` returns `null`, so `existingCustomType` is always `null` and the loop
   calls `createNewInstance(..., i, null)`.
3. `WebCustomType.createNewInstance` with a `null` uuid allocates a **fresh random UUID**
   (`WebCustomType.java:86-88`).
4. The `WebCustomType` constructor reads the parent's json (the same `arr`), scans it for an
   element whose `svyUUID` equals the brand-new random UUID (`WebCustomType.java:120-132`)
   — never found — then at the in-range insert branch (`index == i`, pointing at a
   populated slot) takes the **shift-and-insert** path (lines 142–150), which **grows the
   array by one** and stamps the new random UUID onto the inserted object.
5. Back in the `setJson` loop, `arr.length()` has increased by one, so `i` can never reach
   the end. Each pass inserts one more element and allocates one more `ServoyJSONObject` — a
   true unbounded loop, pegging a core and growing the heap, exactly matching the dump.

The symmetric object branch (lines 271–285) has the same null-UUID shape but, being a
single object, does not grow; it is corrected for consistency and stable ids.

### 2.3 Why the existing guards do not cover this

- **SVY-21574** (`e78a4f623`) only guards `WebCustomType.<init>` against an *out-of-range*
  index (`index > customTypesArray.length()`) to stop org.json padding/OOM. Here `i` is
  always a *valid in-range* slot, so that guard is never taken; the growth comes from the
  shift-insert branch (lines 142–150), not from padding.
- **SERVOY-295** (`f7ee10f25`) fixed a *different* recursion (the `getProperty`
  fall-through cycle), which does not appear in this dump.
- The `getChild(childUUID)` reuse short-circuit in `setJson` (SVY-20784, `d7422689f`) is
  defeated the moment `childUUID` is `null` — precisely the agent-written case.

### 2.4 Prior art in the same file

`WebComponent.initCustomTypes()` (the cold `!customTypesInitialized` path) already does the
exact back-fill this spec applies to the hot path (`WebComponent.java:514-527`):

```java
int arrLength = arr.length();                       // length snapshot
for (int i = 0; i < arrLength; i++)
{
    if (arr.opt(i) instanceof JSONObject jsonObject)
    {
        UUID childUUID = Utils.getAsUUID(jsonObject.optString(IChildWebObject.UUID_KEY, null), false);
        if (childUUID == null)
        {
            childUUID = UUID.randomUUID();
            jsonObject.put(IChildWebObject.UUID_KEY, childUUID.toString());
        }
        WebCustomType.createNewInstance(this, ..., propertyName, i, childUUID);
    }
}
```

The fix brings the `setJson` array branch in line with this proven pattern.

## 3. Design

### 3.1 Back-fill a stable UUID in `WebComponent.setJson`'s array branch

In `WebComponent.setJson(...)`, `customTypesInitialized` branch, array case: compute
`childUUID` once; when it is `null`, mint `UUID.randomUUID()` and write it back into
`jsonObject` under `IChildWebObject.UUID_KEY`; use that non-null `childUUID` for the
`getChild(...)` lookup **and** pass it to `createNewInstance(...)`. With a non-null, stable
uuid:

- the `WebCustomType` constructor's UUID scan finds/keeps a single element instead of
  inserting a new one every pass, so the array stops growing and the loop terminates; and
- `getChild(childUUID)` resolves on the next reload, so children are reused rather than
  duplicated.

This also removes the duplicate `Utils.getAsUUID(...)` call currently passed as the last
argument to `createNewInstance` (line 302) — the already-computed `childUUID` is reused.

### 3.2 Apply the same back-fill to the object branch

In the symmetric object case (lines 271–285), when `childUUID == null`, mint and write back
a UUID the same way before the `getChild` / `createNewInstance` calls. The object branch
cannot grow the array, but the back-fill gives it stable ids and keeps the two branches
consistent.

### 3.3 Belt-and-braces: snapshot `arr.length()` before the loop

Snapshot the array length into a local (`int arrLength = arr.length();`) and iterate against
that local, exactly as `initCustomTypes` already does (line 514). With §3.1 in place the
array no longer grows, so this is defence-in-depth: even if some future caller re-introduces
growth, a stale length can never be re-read mid-loop to drive an infinite spin.

### 3.4 Do not touch `WebCustomType.<init>`

The constructor's shift-insert (lines 136–168) and the SVY-21574 out-of-range guard are
left unchanged. The loop is fixed at its source in `setJson` (and is already correct in
`initCustomTypes`). Changing the constructor risks regressing SVY-21574 and the
`WebCustomTypeAddChildTest` insert/append behaviour, and is unnecessary once the caller
always passes a stable uuid.

### 3.5 Out-of-repo producer fix (not done here)

The MCP `CodeEditingService` / agent should also write a `svyUUID` on generated
custom-object array elements (triage Approach 3). That code lives in the `servoy-eclipse`
repo. It is defence-in-depth at the producer, not a substitute for the runtime fix here, and
is tracked as a separate linked ticket. **Out of scope for this repository.**

## 4. Implementation plan

1. Edit `servoy_shared/src/com/servoy/j2db/persistence/WebComponent.java`, method
   `setJson(JSONObject arg)`, `else if (arg != null)` branch:
   - **Array case (lines 286–306):** snapshot `int arrLength = arr.length();` and loop
     `for (int i = 0; i < arrLength; i++)`. Inside, compute
     `UUID childUUID = Utils.getAsUUID(jsonObject.optString(IChildWebObject.UUID_KEY, null), false);`
     once; if `childUUID == null`, set `childUUID = UUID.randomUUID();` and
     `jsonObject.put(IChildWebObject.UUID_KEY, childUUID.toString());`. Use `childUUID` in
     the `getChild(childUUID)` lookup and as the final argument to
     `WebCustomType.createNewInstance(..., i, childUUID)` (remove the duplicate
     `Utils.getAsUUID(...)` call).
   - **Object case (lines 271–285):** same null-UUID back-fill before the `getChild` /
     `createNewInstance` calls (index `-1`, as today).
2. Run the post-edit compile loop: `eclipse-ide` `getCompilationErrors` on `servoy_shared`;
   apply safe quick fixes; re-check clean. (`UUID`, `Utils`, `IChildWebObject` are already
   imported — no new imports or MANIFEST changes expected.)
3. Add a regression test
   `servoy_ngclient.tests/src/test/java/com/servoy/j2db/persistence/WebComponentSetJsonMissingUuidTest.java`
   — plain `*Test`, JUnit 5, no OSGi — modelled on `WebCustomTypeOutOfRangeIndexTest`
   (reuse its `DummySolution` / `AbstractPersistFactory` / `TestableWebComponent` scaffolding
   and a `columns` custom-object array `PropertyDescription`, or a tab-style one). The test:
   - builds a component whose custom-object array JSON has several elements with **no
     `svyUUID`**;
   - drives the hot `setJson` path with `customTypesInitialized == true` (set the flag true,
     then call `setJson(json)` with the array present) under
     `assertTimeoutPreemptively(Duration.ofSeconds(10), ...)`;
   - asserts the call terminates, the stored array length is **unchanged** (no growth), every
     element now carries a stable `svyUUID`, and the component has exactly one child per
     array element (no duplicates);
   - add a reload/idempotency check: calling the deserialize path a second time keeps the
     same count and the same UUIDs (children reused, not re-created).
4. Run the new test with `eclipse-ide` `runJUnitTests` (`projectName=servoy_ngclient.tests`).
   Confirm it fails on the pre-fix code and passes after.
5. Re-run the existing tests to confirm no regression: `WebComponentCloneTest`,
   `WebComponentCloneMapIsolationTest`, `WebCustomTypeAddChildTest`,
   `WebCustomTypeNamePropertyTest`, `WebCustomTypeOutOfRangeIndexTest`.
6. Commit with subject ending in `[ai]` and the case number, e.g.
   `SVY-21677 fix unbounded WebCustomType loop on custom-type array without svyUUID [ai]`.

## 5. Acceptance criteria

- [ ] In `WebComponent.setJson`'s `customTypesInitialized` array branch, a custom-type array
      element with no `svyUUID` gets a freshly-minted `UUID.randomUUID()` written back into its
      JSON, and that UUID is passed to `createNewInstance` and used for the `getChild` lookup.
- [ ] The same null-UUID back-fill is applied in the object branch of that path.
- [ ] `arr.length()` is snapshotted into a local before the array loop.
- [ ] Deserializing a component whose custom-object array elements lack `svyUUID` terminates
      (no infinite loop); the array length is unchanged (no growth); every element ends with a
      stable `svyUUID`; one child is created per element (no duplicates).
- [ ] A second deserialize of the same JSON reuses the existing children (same count, same
      UUIDs) rather than duplicating them.
- [ ] A new plain `*Test` in `servoy_ngclient.tests` covers the above, fails on pre-fix code
      and passes after, and runs well under its preemptive timeout.
- [ ] `WebComponentCloneTest`, `WebComponentCloneMapIsolationTest`,
      `WebCustomTypeAddChildTest`, `WebCustomTypeNamePropertyTest` and
      `WebCustomTypeOutOfRangeIndexTest` still pass unchanged.
- [ ] `servoy_shared` compiles with no new errors.

## 6. Out of scope

- The MCP `CodeEditingService` / Servoy AI producer-side fix to emit a `svyUUID` on
  generated custom-object array elements (triage Approach 3) — lives in the `servoy-eclipse`
  repo; file/track as a separate linked ticket.
- Any change to `WebCustomType.<init>` (the shift-insert path and the SVY-21574 out-of-range
  OOM guard) — left untouched.
- The OOM already fixed under SVY-21574 and the `getProperty` recursion fixed under
  SERVOY-295 — distinct, already-resolved issues.
- The Eclipse workspace-lock / `OrderedLock` behaviour — a secondary symptom that clears once
  the loop is fixed; no locking change is made.

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| File the separate `servoy-eclipse` ticket for the MCP `CodeEditingService` producer-side `svyUUID` back-fill (Approach 3). | Dev lead | open |
| Confirm the regression test should model the exact reproducing shape (`bootstrapcomponents-tabpanel` `tabs`) or the generic `columns` custom-object array reused from `WebCustomTypeOutOfRangeIndexTest`; either exercises the same code path. | Implementer | open (assumption: reuse the generic `columns` scaffolding) |
