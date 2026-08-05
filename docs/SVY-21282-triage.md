# Triage Report — SVY-21282

**Verdict:** PROCEED

## Reported problem

`solutionModel.cloneForm()` throws a `NullPointerException` (logged, not raised to client) when the source form contains a web component whose custom type array properties (e.g. `columns` on `aggrid-groupingtable`) were set from script via `setJSONProperty` without explicit `svyUUID` values. Additionally, phantom JSON array entries containing only `{"svyUUID":"..."}` are inserted into the cloned component's JSON, corrupting the data structure.

## Root-cause assessment

The NPE occurs in `AbstractBase.getChild(UUID)` (`AbstractBase.java:672`) when called with a `null` argument. `ConcurrentHashMap.get(null)` throws NPE by contract.

**Full call chain during cloning:**

1. `AbstractBase.clonePersist()` shallow-clones the WebComponent (copying `customTypesInitialized = true` from the source).
2. `AbstractBase.fillClone()` clears `propertiesMap` and calls `copyPropertiesMap(getPropertiesMap(), true)`.
3. `copyPropertiesMap` iterates properties and calls `setProperty("json", jsonValue)` on the clone.
4. `WebComponent.setProperty` identifies `"json"` as a persist property → invokes `setJson(JSONObject)` via reflection.
5. In `setJson()` (`WebComponent.java:253`), because `customTypesInitialized == true` (inherited from shallow clone), the **else branch** (lines 257–311) is taken instead of `initCustomTypes()`.
6. The else branch iterates the JSON array entries for persist-mapped properties (e.g. `columns`). For each entry it reads the `svyUUID` key (`WebComponent.java:292`).
7. When the user set columns from script without providing `svyUUID`, `jsonObject.optString(UUID_KEY, null)` returns `null` → `Utils.getAsUUID(null, false)` returns `null` → `getChild(null)` is called.
8. `AbstractBase.getChild(null)` at line 672: if `allobjectsMap` is non-null (populated from a previous iteration that created a child), `allobjectsMap.get(null)` throws **NullPointerException** because `ConcurrentHashMap` forbids null keys.

**Why the source form doesn't throw during `setJSONProperty`:**

When `setJSONProperty('columns', [...])` is called from script, the flow goes through `PersistHelper.setWebComponentProperty` (line 1659) which does `json.put(propertyName, value)` — a direct JSON mutation that does NOT call `setJson()` and does NOT create `WebCustomType` children. However, if the JSON property didn't exist yet, `setJson(new ServoyJSONObject())` is called on the empty json first (line 1624), which sets `customTypesInitialized = true` without processing any columns. The result: the source has `customTypesInitialized = true`, columns in the JSON without `svyUUID`, and no child `WebCustomType` objects.

**Secondary issue — phantom array entries:**

In the `setJson` else branch (and also in `initCustomTypes`), when a UUID is not found in the JSON, `WebCustomType.createNewInstance` is called. The `WebCustomType` constructor (`WebCustomType.java:124–147`) creates a new empty `ServoyJSONObject` and **inserts it at the target index, shifting existing array elements**. This produces ghost entries like `{"svyUUID":"..."}` that have no actual column data.

## Ticket premise check

The ticket correctly identifies the bug as a regression in 2026.6, correctly identifies the triggering condition (custom type properties set from script without explicit `svyUUID`), and correctly notes that adding `svyUUID` explicitly avoids the error. No incorrect solution is proposed — it's a pure bug report.

## Approaches considered

1. **Null-guard in `AbstractBase.getChild(UUID)` + fix `setJson` else branch** — Add `if (childUuid == null) return null;` in `getChild`. In `setJson` else branch, when `childUUID` is null, skip `getChild` call and go directly to `createNewInstance`. Additionally fix the `WebCustomType` constructor to reuse the existing JSON entry at the given index rather than creating a phantom and shifting.
   - Pros: Fixes the NPE, fixes the phantom entries, defensive against any other caller passing null to `getChild`.
   - Cons: Requires changes in 3 locations.

2. **Override `fillClone` in WebComponent to reset `customTypesInitialized = false`** — Forces the clone to use `initCustomTypes()` (the fresh-initialization path) instead of the update path.
   - Pros: Simple one-line fix for the clone NPE.
   - Cons: Does NOT fix the phantom entry problem in `initCustomTypes` itself (same constructor issue exists there). Also doesn't protect other callers of `getChild(null)`.

3. **Fix only `WebCustomType` constructor to use existing array entry at index** — When `fullJSONInFrmFile` is null after UUID search, check if `customTypesArray.opt(index)` is a valid JSONObject and use it directly instead of creating+inserting a phantom.
   - Pros: Fixes the root of the phantom entry corruption for both `initCustomTypes` and `setJson` else branch paths.
   - Cons: Doesn't fix the NPE independently (still needs null-guard or the fillClone reset).

4. **No code change** — Not viable; this is a confirmed NPE regression in a supported release.

## Recommendation

**Approach 1** — multi-layered fix covering all failure modes:

1. **`AbstractBase.getChild(UUID)`**: Add null guard (`if (childUuid == null) return null;`) as a defensive measure.
2. **`WebComponent.setJson()` else branch** (lines 292–293): When `childUUID` is null, treat as "no existing child" (skip `getChild`, proceed to `createNewInstance`).
3. **`WebCustomType` constructor** (lines 124–147): When `fullJSONInFrmFile == null` and `index >= 0` and `customTypesArray.opt(index)` is a JSONObject, **use that existing entry** rather than creating a new empty one and shifting. Just put the UUID into the existing entry.

This addresses the NPE, the phantom entries, and is safe for both the `initCustomTypes` path and the `setJson` update path.

## Git history findings

- Git CLI not available in this environment; no blame data could be retrieved.
- The `setJson` else branch (lines 257–311) that processes custom types when `customTypesInitialized == true` is the likely 2026.6 introduction point — prior versions may have always re-initialized via `initCustomTypes()`.
- Commit `581aea8f` (2026-07-28, SVY-21271) modified `WebComponent.addChild` for nested custom types but did not touch `setJson` or `fillClone`.
