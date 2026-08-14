# Triage Report — SVY-21282

**Verdict:** PROCEED

## Reported problem

`solutionModel.cloneForm()` throws a `NullPointerException` when the source form contains a web component whose custom type properties (e.g. `columns` on `aggrid-groupingtable`) were set from script without explicit `svyUUID` values.

A previous fix (commit `f7733d3178a1e9a00b9ed62cd510da9b5cbc321b`) addressed the NPE but introduced a **severe regression**: the `fillClone` overrides in `WebComponent` and `WebCustomType` omit `propertiesMap` initialization, causing the clone to share the same `HashMap` instance with the source. Any property write on either object mutates both, corrupting the original form persist.

## Root-cause assessment

### The regression (commit f7733d3)

The previous fix added `fillClone` overrides in `WebComponent` (line 397) and `WebCustomType` (line 374) that skip the `allobjects` child-cloning step (correct intent — children are rebuilt from JSON via `initCustomTypes`). However, both overrides omit the critical `propertiesMap` initialization that `AbstractBase.fillClone` performs at line 796:

```java
cloned.propertiesMap = new HashMap<String, Object>();  // MISSING in overrides
```

`clonePersist` (line 770) uses `Object.clone()` — a shallow copy. After clone, `cloned.propertiesMap` and `this.propertiesMap` reference the **same HashMap instance**. Without creating a new map, `cloned.copyPropertiesMap(getPropertiesMap(), true)` operates on the shared map, and all subsequent property writes on the clone mutate the original.

**Why the override author missed this:** `propertiesMap` is `private` in `AbstractBase` (line 90). The base `fillClone` can access it (same-class access), but subclass overrides in `WebComponent`/`WebCustomType` **cannot** — there is no protected accessor to create a fresh map. The previous implementation simply had no way to include this step without modifying `AbstractBase`.

### The original NPE (still relevant)

During cloning, `setJson()` is called on the clone via `copyPropertiesMap → setProperty → setJson`. If `customTypesInitialized` is `true` (inherited from shallow clone), `setJson` enters the else branch (line 257) which calls `arg.opt(propertyName)` at line 268. Two NPE paths exist:

1. **Null UUID in JSON entries:** When columns lack `svyUUID`, `getChild(null)` is called → `ConcurrentHashMap.get(null)` throws NPE. Fixed by the null guard added in `AbstractBase.getChild` (line 661) — this part of the commit is correct.

2. **Null arg in setJson else branch:** If `setJson(null)` is called (via `clearProperty` from other paths like `JSBase.getOverridePersistIfNeeded`), line 268 dereferences `arg` without a null check. This path is not exercised during `fillClone` (because the new empty propertiesMap means no clearProperty calls happen), but remains unguarded for other callers.

## Ticket premise check

The ticket correctly identifies the NPE bug. The previous fix (commit f7733d3) correctly identified that the `fillClone` override is needed to prevent duplicate children, and correctly added the null guard in `getChild` and UUID stamping in `initCustomTypes`. However, it failed to account for the `private propertiesMap` field that subclass overrides cannot reinitialize, introducing the shared-map regression.

## Approaches considered

1. **Add a protected helper method in `AbstractBase` to reset `propertiesMap` for clone** — Introduce `protected void initClonedPropertiesMap(AbstractBase cloned) { cloned.propertiesMap = new HashMap<>(); }`. Both overrides call this before `copyPropertiesMap`. Also add null guard in `setJson` else branch.
   - Pros: Minimal change; clear intent; no visibility changes to the field; follows existing pattern of AbstractBase managing its own private state.
   - Cons: Adds a new protected method to AbstractBase API.

2. **Change `propertiesMap` visibility from `private` to package-private** — Allows same-package subclasses to assign directly.
   - Pros: Simplest code change; no new methods.
   - Cons: Widens field access beyond what's necessary; any class in the persistence package could write to it; violates existing encapsulation pattern.

3. **Revert the fillClone overrides and fix only `setJson` + `getChild`** — Remove the overrides entirely; rely on the null guard in `getChild` and the UUID stamping to prevent NPE; let the base `fillClone` clone `allobjects` normally.
   - Pros: No regression; simpler code.
   - Cons: The base fillClone would clone allobjects children AND initCustomTypes would recreate them from JSON (triggered by setJson in copyPropertiesMap path), resulting in duplicate children OR broken JSON references (children not pointing at clone's JSON entries). This is the original design problem the override was meant to solve.

4. **No code change** — Not viable; regression confirmed by reporter, customer had to revert.

## Recommendation

**Approach 1** — Add a protected helper method in AbstractBase:

1. **`AbstractBase.java`** — Add protected method:
   ```java
   protected void initClonedPropertiesMap(AbstractBase cloned)
   {
       cloned.propertiesMap = new HashMap<String, Object>();
   }
   ```

2. **`WebComponent.fillClone`** — Call the helper before `copyPropertiesMap`:
   ```java
   @Override
   protected void fillClone(AbstractBase cloned)
   {
       if (cloned instanceof WebComponent wc)
       {
           wc.customTypesInitialized = false;
       }
       cloned.internalClearAllObjects();
       initClonedPropertiesMap(cloned);
       cloned.copyPropertiesMap(getPropertiesMap(), true);
   }
   ```

3. **`WebCustomType.fillClone`** — Same pattern:
   ```java
   @Override
   protected void fillClone(AbstractBase cloned)
   {
       cloned.internalClearAllObjects();
       initClonedPropertiesMap(cloned);
       cloned.copyPropertiesMap(getPropertiesMap(), true);
   }
   ```

4. **`WebComponent.setJson`** — Add null guard for `arg` in the else branch (line 257):
   ```java
   else if (arg != null)
   {
       // existing else-block body
   }
   ```

This fixes both the regression (shared propertiesMap) and hardens `setJson` against NPE from other callers. The existing null guard in `getChild` and UUID stamping in `initCustomTypes` remain as correct defensive measures.

## Git history findings

- **Commit `f7733d3178a1e9a00b9ed62cd510da9b5cbc321b`** (2026-08-05, by lvostinar/opencode): "SVY-21282 fix cloneForm NPE when custom type properties lack svyUUID [ai]" — introduced the `fillClone` overrides that caused the regression by omitting `propertiesMap` initialization. The commit correctly added the null guard in `getChild` and UUID stamping in `initCustomTypes`.
- **`AbstractBase.fillClone` (line 796):** The `propertiesMap = new HashMap<>()` pattern has been in place since the original design. It is essential because `clonePersist` uses `Object.clone()` (shallow copy) at line 770.
- **`BaseComponent.fillClone` (line 352):** All other fillClone overrides in the codebase (`BaseComponent`, `Form`, `Tab`, `AbstractRootObject`) call `super.fillClone()`, preserving the propertiesMap initialization chain. The `WebComponent`/`WebCustomType` overrides are the only ones that break this chain.
