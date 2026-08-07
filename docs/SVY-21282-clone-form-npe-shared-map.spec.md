# Spec: SVY-21282 — cloneForm NPE and shared-map regression

## 1. Goal

Fix `solutionModel.cloneForm()` so that cloning a form with web components whose custom type properties lack explicit `svyUUID` values neither throws a NullPointerException nor corrupts the source form's properties through a shared `propertiesMap` instance. The previous fix (commit `f7733d3`) correctly prevented duplicate children during clone but introduced a severe regression: the clone and source share the same `HashMap`, so property writes on either mutate both.

## 2. Background

### 2.1 Clone mechanism

`AbstractBase.clonePersist()` uses `Object.clone()` (shallow copy). After clone, all reference-type fields — including `propertiesMap` — point to the same object. The base `fillClone()` at line 796 creates a fresh `HashMap` for the clone before calling `copyPropertiesMap`. This is the critical isolation step.

### 2.2 The `fillClone` overrides

`WebComponent.fillClone` and `WebCustomType.fillClone` override the base method to skip child-object cloning (children are rebuilt from JSON via `initCustomTypes` when `setJson` is called during `copyPropertiesMap`). However, `propertiesMap` is `private` in `AbstractBase` — the overrides cannot assign it directly, so the critical `new HashMap<>()` step was omitted, leaving the clone sharing the source's map.

### 2.3 The `setJson` null-arg NPE

`WebComponent.setJson()` line 268 dereferences `arg` (the JSONObject parameter) without a null guard when `customTypesInitialized` is true. While the current `fillClone` sets `customTypesInitialized = false` (avoiding this path during clone), other callers such as `clearProperty` can pass `null` to `setJson`, triggering an NPE at `arg.opt(propertyName)`.

## 3. Design

### 3.1 Protected helper in AbstractBase

Add a `protected` method to `AbstractBase` that reinitializes the `propertiesMap` field on a clone:

```java
protected void internalClearPropertiesMap(AbstractBase cloned)
{
    cloned.propertiesMap = new HashMap<String, Object>();
}
```

This preserves `private` field encapsulation (only `AbstractBase` can write to `propertiesMap`) while exposing a safe, intent-revealing operation to subclass `fillClone` overrides.

### 3.2 WebComponent.fillClone

Call the helper before `copyPropertiesMap`:

```java
@Override
protected void fillClone(AbstractBase cloned)
{
    if (cloned instanceof WebComponent wc)
    {
        wc.customTypesInitialized = false;
    }
    cloned.internalClearAllObjects();
    internalClearPropertiesMap(cloned);
    cloned.copyPropertiesMap(getPropertiesMap(), true);
}
```

### 3.3 WebCustomType.fillClone

Same pattern:

```java
@Override
protected void fillClone(AbstractBase cloned)
{
    cloned.internalClearAllObjects();
    internalClearPropertiesMap(cloned);
    cloned.copyPropertiesMap(getPropertiesMap(), true);
}
```

### 3.4 Null guard in WebComponent.setJson

Wrap the else branch (line 257) with a null check on `arg`:

```java
else if (arg != null)
{
    // existing else-block body unchanged
}
```

This prevents NPE when `setJson(null)` is called from `clearProperty` or other paths while `customTypesInitialized` is already true.

## 4. Implementation plan

1. **`AbstractBase.java`** — Add `protected void internalClearPropertiesMap(AbstractBase cloned)` method that assigns `cloned.propertiesMap = new HashMap<String, Object>()`. Place it adjacent to the existing `fillClone` method (after line 818).

2. **`WebComponent.java` line 404** — Insert `internalClearPropertiesMap(cloned);` between `cloned.internalClearAllObjects()` and `cloned.copyPropertiesMap(...)` in the `fillClone` override.

3. **`WebCustomType.java` line 377** — Insert `internalClearPropertiesMap(cloned);` between `cloned.internalClearAllObjects()` and `cloned.copyPropertiesMap(...)` in the `fillClone` override.

4. **`WebComponent.java` line 257** — Change `else` to `else if (arg != null)` in `setJson`.

5. **Verify** — Run `eclipse-ide_getCompilationErrors` to confirm no regressions. Run `servoy_ngclient.tests` to verify existing tests pass.

## 5. Acceptance criteria

- [ ] `solutionModel.cloneForm()` succeeds without NPE when the source form has web components with custom type properties lacking `svyUUID`.
- [ ] After cloning, modifying properties on the clone does not mutate the source form's properties (maps are independent).
- [ ] After cloning, modifying properties on the source does not mutate the clone.
- [ ] `setJson(null)` does not throw NPE when `customTypesInitialized` is true.
- [ ] Existing `servoy_ngclient.tests` pass without failures.
- [ ] No new compilation errors or SpotBugs high-severity findings.

## 6. Out of scope

- Refactoring `propertiesMap` visibility from `private` to `protected` (rejected approach — widens access unnecessarily).
- Reverting the `fillClone` overrides entirely (would reintroduce duplicate-children problem).
- Adding automated integration tests for `cloneForm` (would require a full Servoy runtime environment).

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| None — approach is fully specified and approved | — | closed |
