# Spec: SVY-21475 — Tab names in tab panels are lost after Developer restart

## 1. Goal

When a developer sets the `name` of a tab in a tab panel (either `bootstrapcomponents-tabpanel`
or `servoydefault-tabpanel`), the value is written correctly to the `.frm` file, but after
restarting Servoy Developer (or reloading the solution), the Properties view shows `-none-` for
that tab's `name` even though the persisted JSON still contains it. This is a generic,
shared-code defect in `WebCustomType` (used by both tab panel implementations, and by any other
component that defines a `name` sub-property on a custom JSON object type) — not a bug in either
tab panel's own `.spec`/`.js` code. Fixing it makes `WebCustomType`'s `name` property survive a
Developer restart the same way `extendsID` already does, for both tab panel components and any
future custom type with a `name` sub-property.

## 2. Background

### 2.1 Triage summary

A full triage (`docs/SVY-21475-triage.md`) already root-caused this to
`servoy-client/servoy_shared/src/com/servoy/j2db/persistence/WebCustomType.java`:

- `WebCustomType` maintains a static `purePersistPropertyNames` set (WebCustomType.java:49-63),
  computed via bean-introspection of `WebCustomType`'s **own** setter methods.
- `WebCustomType` is required (by `IBasicWebObject`, `IBasicWebObject.java:31,33`) to implement
  `setName(String)` / `getName()`. Because those methods exist, introspection always includes
  `"name"` in `purePersistPropertyNames` — currently, those two methods are implemented as legacy
  `AbstractBase`/`propertiesMap`-backed accessors (`setTypedProperty`/`getTypedProperty` on
  `StaticContentSpecLoader.PROPERTY_NAME`), not JSON-backed.
- `WebCustomType.getProperty(String)` / `setProperty(String, Object)` (the **generic** property
  access path used by the Properties view and any other code that goes through `IPersist`) special-case
  `purePersistPropertyNames`:
  ```java
  // setProperty
  if (purePersistPropertyNames.contains(propertyName)) super.setProperty(propertyName, val);
  PersistHelper.setWebComponentProperty(this, propertyName, val);   // ALSO always runs

  // getProperty
  if (purePersistPropertyNames.contains(propertyName)) return super.getProperty(propertyName);
  return PersistHelper.getWebComponentProperty(this, propertyName);
  ```
  Because `"name"` is in `purePersistPropertyNames`, **setting** writes twice (once into the legacy
  `propertiesMap` via `super.setProperty`, once into the JSON blob via `PersistHelper`, which is
  why the `.frm` file correctly retains the value), but **getting** returns
  `super.getProperty("name")`, i.e. `AbstractBase`'s `propertiesMap`, which is populated only by
  explicit calls made during the current session. After a restart, `WebComponent`/
  `WebCustomType.initCustomTypes()` reconstruct fresh `WebCustomType` instances purely from JSON
  (`WebCustomType.java:382-425`, invoked from the constructor at line 165, and from `setJson`
  at line 234), and the constructor never repopulates `propertiesMap["name"]` from the reloaded
  JSON. Result: `getProperty("name")` returns `null` after reload even though the JSON (and the
  `.frm` file) still has the value.
- `extendsID` avoids this because `WebCustomType` has its own **typed**
  `getExtendsID()`/`setExtendsID()` (`WebCustomType.java:341-359`) that read/write
  `getFullJsonInFrmFile()` directly, and all real callers of extendsID use those typed methods,
  never the generic `getProperty("extendsID")`/`setProperty("extendsID", …)` path. `name` has no
  such typed JSON-backed accessor today, and — critically — `name` **is** exercised through the
  generic path (the Properties view reads/writes custom-type sub-properties generically via
  `IPersist.getProperty(propertyName)`/`setProperty(propertyName, val)`, e.g. see
  `com.servoy.eclipse.ui.util.DeveloperUtils.getCustomObjectTypeCaptionFromTaggedSubproperties`,
  which calls `webCustomType.getProperty(captionPD.getName())`).

Both real tab specs are affected because both declare a `tab.name` sub-property:
- `bootstrapcomponents/components/tabpanel/tabpanel.spec` (line 144): `"name": { "type": "string" }`
- `servoy-client/servoy_ngclient/war/servoydefault/tabpanel/tabpanel.spec` (line 237):
  `"name": { "type": "string", "tags": { "useAsCaptionInDeveloper": true, "captionPriority": 1 } }`
  — this one additionally drives the tab's outline/Properties-view caption via
  `DeveloperUtils.getCustomObjectTypeCaptionFromTaggedSubproperties`, so the servoydefault tab's
  *displayed caption* is affected on top of the raw property value being lost.

### 2.2 Why "mirror the extendsID pattern" needs one extra step

The triage's approved approach describes mirroring the `extendsID` pattern: give `WebCustomType`
a JSON-backed typed `getName()`/`setName()`. That part alone is **necessary but not sufficient**.
Because `"name"` will always be picked up by introspection as a `purePersistPropertyName` (the
interface-mandated `setName`/`getName` methods must exist no matter how their bodies are
implemented), the generic `getProperty("name")`/`setProperty("name", …)` path — which is what the
Properties view and `DeveloperUtils` actually use — will keep hitting the `super.getProperty`/
`super.setProperty` short-circuit and stay broken unless `"name"` is also removed from the
`purePersistPropertyNames` set used by that branching. Both parts of the fix are required together;
see §3.1 below.

### 2.3 `findReferences` check (per approved approach)

Run before implementing, to check for other callers relying on the old `propertiesMap`-backed
behavior:

- **`WebCustomType.setName`**: 1 real reference —
  `com.servoy.eclipse.designer/src/com/servoy/eclipse/designer/editor/commands/AddContainerCommand.java:664`
  (`customType.setName(compName);`, called once right after `WebCustomType.createNewInstance(...)`
  when a new custom type child — e.g. a new tab — is created interactively). This is a typed call
  made once at creation time in the same session; it already "worked" before the fix (same-session
  read-after-write always worked, per the triage) and will keep working identically after the fix,
  since the new implementation still ends up writing the name into the type's own JSON.
- **`WebCustomType.getName`**: the `findReferences` query on this method timed out / was truncated
  because `getName()` is inherited from `IBasicWebObject`/`ISupportName` and implemented by dozens
  of unrelated persist types (`Form`, `Relation`, `ScriptMethod`, `Media`, …), so JDT's search
  returns ~2000 hits across the workspace, the vast majority of which resolve to **other**
  `getName()` overrides, not `WebCustomType`'s. The distinguishable hits that could plausibly be
  calling `WebCustomType.getName()` specifically (i.e. call sites operating on a `WebCustomType`/
  custom-type-child value, not a plain `WebComponent`) were:
  - `DuplicateGhostsHandler.duplicateCustomType` (`original.getName()`, where `original` is a
    `WebCustomType`) — wants the *persisted* name to build a `"<name>_copy"` suggestion; today this
    can silently return `null` after a restart even though the `.frm` file has a name, which is
    exactly the bug being fixed. Fixing `getName()` only improves this call site's correctness.
  - `WebFormComponentChildType.toString()` (`((IBasicWebObject)getParent()).getName()`) — the
    parent here is generally a `WebComponent`, not a `WebCustomType`; unaffected.
  - The remaining plausible hits (`GhostHandler`, `DesignerPropertyAdapterFactory`,
    `ServoyFormBuilder`, `SolutionExplorerListContentProvider`) all call `getName()` on variables
    typed/named as `webcomponent`/`wc`/`basicWebComponent`, i.e. `WebComponent` instances, not
    `WebCustomType`; unaffected by this change.
  - No call site was found that depends on `WebCustomType.getName()` returning the stale/legacy
    `propertiesMap` value instead of the persisted JSON value — every real call site wants the
    persisted value and is currently either broken by this bug or coincidentally unaffected
    because it only reads within the same session as the write.

No caller depends on the current broken behavior; the fix is safe to make from a
caller-compatibility standpoint.

## 3. Design

### 3.1 `WebCustomType` changes (servoy_shared)

File: `servoy-client/servoy_shared/src/com/servoy/j2db/persistence/WebCustomType.java`

**Step 1 — stop treating `"name"` as a pure-persist property for the generic property path.**
Change the static initializer (currently WebCustomType.java:53-64) so that, after computing the
introspected setter names, `"name"` (`StaticContentSpecLoader.PROPERTY_NAME.getPropertyName()`) is
explicitly removed from the resulting set:

```java
static
{
    try
    {
        Set<String> introspectedNames = new HashSet<>(RepositoryHelper.getSettersViaIntrospection(WebCustomType.class).keySet());
        // "name" is required by the IBasicWebObject/ISupportName contract (so setName/getName must
        // exist), but it is a regular spec-declared JSON sub-property like any other on a custom
        // type (see tab.name in tabpanel specs) and must be read/written through the JSON-backed
        // PersistHelper mechanism, not through the legacy AbstractBase propertiesMap. Excluding it
        // here is what makes getProperty("name")/setProperty("name", ...) route to PersistHelper.
        introspectedNames.remove(StaticContentSpecLoader.PROPERTY_NAME.getPropertyName());
        purePersistPropertyNames = introspectedNames;
    }
    catch (IntrospectionException e)
    {
        purePersistPropertyNames = new HashSet<String>();
        Debug.error(e);
    }
}
```

This is the essential part of the fix: `getProperty("name")`/`setProperty("name", val)` will now
fall through to the existing `PersistHelper.getWebComponentProperty`/`setWebComponentProperty`
calls (already present in `WebCustomType.getProperty`/`setProperty`, unchanged), which — for a
plain, non-persist-mapped spec property like `name` — read/write the value directly against the
`WebCustomType`'s own JSON (via `getFlattenedJson()`/`getOwnProperty(PROPERTY_JSON)`), exactly the
same mechanism already used correctly today for ordinary sub-properties such as `text` (see
`WebCustomTypeAddChildTest.testSetPropertyOnNestedCustomTypePersistsInJson`). It also removes the
"write it twice" side effect the triage flagged (currently `setProperty("name", …)` wrote to both
`propertiesMap` and JSON).

**Step 2 — make the typed `setName`/`getName` delegate to the (now-fixed) generic property path**,
so direct typed callers (`AddContainerCommand`, `DuplicateGhostsHandler`) get the same JSON-backed
behavior with no duplicated JSON-manipulation code:

```java
@Override
public void setName(String arg)
{
    setProperty(StaticContentSpecLoader.PROPERTY_NAME.getPropertyName(), arg);
}

@Override
public String getName()
{
    Object value = getProperty(StaticContentSpecLoader.PROPERTY_NAME.getPropertyName());
    return value != null ? value.toString() : null;
}
```

This differs slightly from literally copying `getExtendsID`/`setExtendsID` (which manipulate
`getFullJsonInFrmFile()` directly): delegating to `setProperty`/`getProperty` is simpler, avoids
duplicating JSON-mutation logic, and is consistent with how every other ordinary sub-property
(`text`, `containedForm`, etc.) is already implemented on `WebCustomType`. It is safe specifically
*because* of Step 1 — without Step 1, this delegation would just call back into the
`purePersistPropertyNames`-guarded generic path and still hit the legacy `propertiesMap`.

**No other methods need to change.** `hasProperty`, `setProperty`, `getProperty`,
`getFullJsonInFrmFile`, `getExtendsID`/`setExtendsID`, and `initCustomTypes` are unaffected.

### 3.2 No changes needed outside `servoy_shared`

Per the approved approach and the triage's root-cause analysis:
- **`bootstrapcomponents`** (`components/tabpanel/tabpanel.spec`): no change. The `tab.name`
  sub-property definition (`"name": { "type": "string" }`) is correct as-is; it is only a
  consumer of the fixed `WebCustomType` behavior.
- **`servoy-eclipse`** (`com.servoy.eclipse.designer`, `com.servoy.eclipse.ui`, RFB Angular
  frontend): no code change. `AddContainerCommand.addCustomType`'s `customType.setName(compName)`
  call, `DuplicateGhostsHandler.duplicateCustomType`'s `original.getName()` call, and
  `DeveloperUtils.getCustomObjectTypeCaptionFromTaggedSubproperties`'s
  `webCustomType.getProperty(captionPD.getName())` call all become correct automatically once the
  shared `WebCustomType` fix lands; none of them need to change.
- **`servoy-client/servoy_ngclient/war/servoydefault/tabpanel/tabpanel.spec`**: no change, same
  reasoning as bootstrapcomponents.

### 3.3 Git history context (carried over from triage)

- `servoy-client` commit `b7aa82b0b` ("SVY-20784 refactor the flattened stuff in for the
  WebCustomType") introduced the current constructor-based JSON initialization and
  `initCustomTypes()` machinery, and added the `extendsID` JSON-backed special case, but did not
  give `name` the same treatment — this is where the current shape of the bug was introduced.
- `servoy-client` commit `f7ee10f25` ("SERVOY-295 fix StackOverflowError and NPE in WebCustomType
  for missing component specs") tightened `getProperty` to be strict if/return, confirming this is
  an actively-maintained, sensitive area — the Step 1/Step 2 changes above must keep that
  if/return structure intact (they do; they only change the *contents* of the set being tested,
  and the set is tested with the same `.contains(propertyName)` check as before).
- `servoy-client` commits `0fbc08baa`, `f7733d317`, `736b039ad` (SVY-21271, SVY-21282) show this
  same `WebCustomType` JSON/persist duality is a recurring source of bugs; this fix reduces that
  surface area by one more property.

## 4. Implementation plan

1. In `servoy-client/servoy_shared/src/com/servoy/j2db/persistence/WebCustomType.java`:
   - Update the `purePersistPropertyNames` static initializer to exclude
     `StaticContentSpecLoader.PROPERTY_NAME.getPropertyName()` from the introspected setter-name
     set (Step 1 in §3.1).
   - Reimplement `setName(String)` and `getName()` to delegate to `setProperty`/`getProperty`
     (Step 2 in §3.1).
2. Organize imports / format the file; verify no other method in the class needs adjustment.
3. Add a regression test in `servoy_ngclient.tests`, mirroring the existing
   `WebCustomTypeAddChildTest` scaffolding (`DummySolution`, `TestableWebComponent`,
   `PropertyDescriptionBuilder`-built custom types). Recommended shape — either as a new test
   class (e.g. `WebCustomTypeNamePropertyTest`) or additional `@Test` methods appended to
   `WebCustomTypeAddChildTest` — using only public API, no reflection required:
   - Build a `tab`-like `PropertyDescription` with a `"name"` string sub-property (plus e.g. a
     `"text"` sub-property for realism, matching both real tab specs).
   - Create a `WebCustomType` child via `WebCustomType.createNewInstance(wc, tabPd, "tabs", 0)`.
   - Call `child.setProperty("name", "myTabName")` (and separately, in another test,
     `child.setName("myTabName")`) and assert `child.getProperty("name")` /
     `child.getName()` return `"myTabName"` on the same live instance (sanity — should already
     pass today).
   - **Simulate a Developer restart without reflection**: capture the parent's own JSON
     (`wc.getOwnProperty(StaticContentSpecLoader.PROPERTY_JSON.getPropertyName())`) and call
     `wc.setJson(thatSameJsonObject)` again. `setJson` is public and internally calls
     `initCustomTypes()` (WebCustomType.java:234), which clears all existing child persists and
     reconstructs brand new `WebCustomType` instances purely from the JSON — exactly what happens
     when Eclipse restarts and reloads the solution from disk.
   - Fetch the freshly-reconstructed tab child from `wc.getAllObjectsAsList()` and assert both
     `reloadedChild.getProperty("name")` and `reloadedChild.getName()` return `"myTabName"` — this
     is the assertion that fails today (returns `null`) and must pass after the fix.
   - Add one more assertion that `reloadedChild.getProperty("text")` (or another ordinary
     sub-property) still round-trips correctly, as a no-regression guard on the rest of
     `getProperty`/`setProperty`.
4. Run the new/updated test class via `eclipse-pde_runJUnitPluginTests` (or the appropriate JUnit
   runner for `servoy_ngclient.tests`) and confirm it fails on the pre-fix code and passes after
   the fix (or at minimum passes cleanly after the fix, since the pre-fix state was already
   verified failing during triage).
5. Manual verification in **both** real components, per explicit user request, since the fix is in
   shared code and no component-specific code changes are made:
   - **bootstrapcomponents `tabpanel`**: in Servoy Developer, add a tab, set its `name` in the
     Properties view, save, restart Eclipse (or close/reopen the solution), confirm the tab's
     `name` still shows the set value (not `-none-`).
   - **servoydefault `tabpanel`**: same steps; additionally confirm the tab's outline/Properties
     caption (driven by `useAsCaptionInDeveloper`/`captionPriority` on `name`/`text`) still
     displays correctly after restart.
6. Run `eclipse-ide_getCompilationErrors` on `servoy_shared` and `servoy_ngclient.tests` after the
   change and fix anything reported.

## 5. Acceptance criteria

- [ ] `WebCustomType.purePersistPropertyNames` no longer contains `"name"`.
- [ ] `WebCustomType.getName()`/`setName(String)` delegate to `getProperty`/`setProperty` instead
      of the legacy `getTypedProperty`/`setTypedProperty(PROPERTY_NAME, …)`.
- [ ] New/updated JUnit test in `servoy_ngclient.tests` reproduces the "restart" scenario via
      `setJson()`-triggered `initCustomTypes()` re-construction (no reflection) and asserts
      `getProperty("name")` / `getName()` correctly return the persisted value on the
      freshly-reconstructed instance.
- [ ] Existing `WebCustomTypeAddChildTest` tests continue to pass unmodified.
- [ ] Manual verification confirms tab `name` survives a Developer restart in both
      bootstrapcomponents `tabpanel` and servoydefault `tabpanel`.
- [ ] No compilation errors in `servoy_shared` or `servoy_ngclient.tests`.

## 6. Out of scope

- Any change to `bootstrapcomponents/components/tabpanel/tabpanel.spec` or
  `servoy-client/servoy_ngclient/war/servoydefault/tabpanel/tabpanel.spec`.
- Any change to `com.servoy.eclipse.designer`, `com.servoy.eclipse.designer.rfb`, or
  `com.servoy.eclipse.ui` Java/TypeScript code.
- Broader refactoring of the `WebCustomType`/`AbstractBase` JSON-vs-propertiesMap duality beyond
  the `name` property (flagged in the triage as a recurring source of bugs, but out of scope for
  this fix).
- Adding JSON-backed re-sync for `name` in the constructor (triage's fallback Approach 2) — not
  needed since Approach 1 (this spec) fully addresses both the typed and generic property paths.
- Adding `useAsCaptionInDeveloper`/`captionPriority` tags to `bootstrapcomponents-tabpanel`'s
  `tab.name` (a UX alignment with `servoydefault-tabpanel`, not part of this bug fix).
- Any change to `AbstractBase`'s `checkForNameChange`/`NameChangeProperty` mechanism. It stops
  firing for `WebCustomType.name` as a side effect of removing `"name"` from
  `purePersistPropertyNames`, but that mechanism is only used for table/datasource rename
  bookkeeping, never for custom type children, so no behavior change is expected or in scope.

## 7. Open questions

None. Scope is limited to the `WebCustomType` persistence fix described in §3.1/§4; no
spec-tag alignment or other follow-on work is included in this change.

