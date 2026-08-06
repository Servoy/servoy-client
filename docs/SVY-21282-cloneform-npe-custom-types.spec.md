# Spec: SVY-21282 — solutionModel.cloneForm() throws NullPointerException when custom type properties were set from script

## 1. Goal

Fix the `NullPointerException` thrown during `solutionModel.cloneForm()` when the source form contains a web component whose custom type array properties (e.g. `columns` on `aggrid-groupingtable`) were set from script via `setJSONProperty` without explicit `svyUUID` values.

## 2. Background

### 2.1 How custom types work in WebComponent

Web components can declare "custom type" properties (e.g. `columns` in a data grid). These are persisted as JSON arrays/objects within the component's `json` property and are mirrored as `WebCustomType` child persist objects. Each `WebCustomType` child has a UUID that is stored as `svyUUID` in the corresponding JSON entry. The child's own `json` property is a **direct reference** to the entry in the parent's JSON array — they share the same object.

### 2.2 The cloning flow (`clonePersist` path)

`solutionModel.cloneForm()` → `FlattenedSolution.clonePersist()` → `AbstractBase.clonePersist()` which uses Java `Object.clone()` (shallow copy) and then calls `fillClone()`. The shallow clone copies `customTypesInitialized = true` from the source.

In `fillClone()`, `copyPropertiesMap()` is called which triggers `setProperty("json", ...)` → `setJson()`. Since `customTypesInitialized` is `true` on the clone, `setJson()` takes the **else branch** (line 257) that tries to match existing children by UUID — this leads to the NPE.

### 2.3 The NPE trigger

When columns are set from script via `setJSONProperty('columns', [...])`, the JSON entries have no `svyUUID` key. During cloning, `setJson()` reads `jsonObject.optString(UUID_KEY, null)` → `null` → `Utils.getAsUUID(null, false)` → `null` → `getChild(null)` is called. `AbstractBase.getChild()` at line 672 calls `allobjectsMap.get(null)` which throws `NullPointerException` because `ConcurrentHashMap` forbids null keys.

### 2.4 Why cloning children via allobjects breaks JSON references

`AbstractBase.fillClone()` has two mechanisms for initializing a clone's children:
1. `copyPropertiesMap` → `setJson` → `initCustomTypes()` — creates children that bind directly to entries in the clone's JSON (correct references).
2. The `allobjects` cloning block — calls `clonePersist` on each source child, which copies the child's own properties (including its own `json` property from source). This creates a SECOND copy of the JSON data that is disconnected from the parent's JSON array entries — **breaking the reference link**.

For WebComponent/WebCustomType, only path 1 is correct: children must be created from the parent's JSON so they share the same JSON object references.

### 2.5 Git history

The existing `cloneObj` method (line 362) already resets `customTypesInitialized = false` — but this happens AFTER `super.cloneObj()` returns and only applies to the `cloneObj` path. The `clonePersist` path (used when Form's `fillClone` clones its children) does not have this reset.

## 3. Design

### 3.1 Override `fillClone` in WebComponent

Override `fillClone` to reset `customTypesInitialized = false` and only copy properties — skip the allobjects cloning. `initCustomTypes()` (triggered by `setJson` during property copying) will create the correct children bound to the clone's JSON entries.

```java
@Override
protected void fillClone(AbstractBase cloned)
{
    if (cloned instanceof WebComponent wc)
    {
        wc.customTypesInitialized = false;
    }
    cloned.internalClearAllObjects();
    cloned.copyPropertiesMap(getPropertiesMap(), true);
}
```

### 3.2 Override `fillClone` in WebCustomType

Same pattern — WebCustomType can also have nested custom type children. Skip allobjects cloning so nested children are created from JSON with correct references.

```java
@Override
protected void fillClone(AbstractBase cloned)
{
    if (cloned instanceof WebCustomType wct)
    {
        wct.customTypesInitialized = false;
    }
    cloned.internalClearAllObjects();
    cloned.copyPropertiesMap(getPropertiesMap(), true);
}
```

### 3.3 Defensive null guard in `AbstractBase.getChild(UUID)`

Add `if (childUuid == null) return null;` as the first statement in `getChild(UUID)`. This prevents the `ConcurrentHashMap` NPE regardless of which caller passes null — a safety net for any remaining code path.

## 4. Implementation plan

1. **`servoy_shared/src/com/servoy/j2db/persistence/WebComponent.java`** — Add `fillClone` override: reset flag, clear children, copy properties only.

2. **`servoy_shared/src/com/servoy/j2db/persistence/WebCustomType.java`** — Add `fillClone` override: same pattern.

3. **`servoy_shared/src/com/servoy/j2db/persistence/AbstractBase.java`** — Add null guard in `getChild(UUID)`.

4. **Verify** — Run `eclipse-ide_getCompilationErrors` to confirm zero compilation errors.

## 5. Acceptance criteria

- [ ] `solutionModel.cloneForm()` does not throw or log a `NullPointerException` when the source form has web component custom type array properties set from script without `svyUUID`.
- [ ] The cloned form's web component children have JSON references that point to entries within the parent's JSON (not disconnected copies).
- [ ] Existing forms that already have `svyUUID` in their JSON continue to clone correctly (regression-safe).
- [ ] `AbstractBase.getChild(null)` returns `null` instead of throwing NPE.
- [ ] Nested custom types (custom types within custom types) clone correctly.

## 6. Out of scope

- Adding `svyUUID` automatically when `setJSONProperty` is called from script.
- Changing the `cloneObj` path (line 362 already handles it).
- Fixing phantom entry creation in `WebCustomType` constructor (separate concern).

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| None | | |
