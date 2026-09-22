# Spec: SVY-21423 — Navbar shows empty dropdown caret for menu items without sub-items

## 1. Goal
Menu items on the `bootstrapextracomponents` navbar that have no configured sub-menu items must render as plain links (no dropdown caret, no empty dropdown `<div>`), matching the component's documented, pre-existing contract. Currently all three items in the reporter's test case show the dropdown-toggle markup, because an *unset* `subMenuItems` array property now serializes to the client as `[]` instead of `null`. The fix restores the platform's `null` contract for unset array-of-custom-object properties and additionally hardens the navbar template so it no longer breaks if a genuinely empty array is ever pushed to it.

## 2. Background

### 2.1 Reported symptom
A navbar with 3 menu items (`item1`/Mars, `item2`/Venus, `item3`/Earth), where only `item3` has sub-menu items configured (Moon/Sat2/Satt3). All three items render the dropdown-caret + `dropdown-menu` markup; for `item1`/`item2` the `dropdown-menu` is empty. Expected: `item1`/`item2` render as plain links, `item3` renders the dropdown as before.

### 2.2 Root cause (confirmed in triage, `docs/SVY-21423-triage.md`)
Two contributing factors, both already correctly identified by the reporter:

1. **Platform regression (`servoy-client` repo, `servoy_shared`)**: `PersistHelper.getWebComponentProperty(AbstractBase, String)` (`servoy_shared/src/com/servoy/j2db/util/PersistHelper.java:1569-1602`) handles "persist-mapped" properties — i.e. properties whose value lives as real child persists/`WebCustomType`s under the parent component (the same mechanism used for tab panels, portal columns, etc.). `subMenuItems` is typed `subMenuItem[]` where `subMenuItem` is itself a custom JSON object type, so `PersistHelper.isArrayOfCustomJSONObject(childPd.getType())` is `true` and the property is treated as persist-mapped. When no sub-menu-item child persists exist (the ordinary "unset" case), the method collects an empty `List<IPersist> customTypes` and unconditionally returns `customTypes.toArray(new IChildWebObject[customTypes.size()])` — an empty Java array, **never `null`**. This empty array serializes to `[]` in the client JSON payload.

   Before commit `b7aa82b0b` ("SVY-20784 refactor the flattened stuff in for the WebCustomType", 2026-05-08), the legacy `WebObjectImpl.getProperty(...)` read the value straight from the persist's JSON (`json.opt(propertyName)`), which is `null` when the key is absent. The refactor changed this without preserving the null-vs-empty-array distinction for the persist-mapped/array branch specifically — the `else` branch (plain JSON properties) still correctly distinguishes "key absent" from "key present".

2. **Component fragility (`bootstrapextracomponents` repo)**: `navbar.html` branches on strict `=== null` / `!== null` equality against `subMenuItems` in 12 places (4 duplicated display-type blocks × LEFT/RIGHT layout duplication), e.g.:
   ```html
   @if (menuItem.displayType === 'MENU_ITEM' && menuItem.subMenuItems === null) { <!-- plain link --> }
   @if (menuItem.displayType === 'MENU_ITEM' && menuItem.subMenuItems !== null) { <!-- dropdown --> }
   ```
   An empty array `[]` is `!== null`, so it always takes the dropdown branch. This check predates the current investigation (going back years) and was only reformatted for ESLint strict-equality style in commit `dbd10ce` — semantically neutral for a real `null`. The template's contract has always been "unset array → `null`", so this was not a bug in isolation, but it is fragile: it silently breaks the moment the platform (or any future scripting API call, e.g. `navbar.setMenuItems(...)` with an explicit empty array) sends `[]` instead of `null`.

The Angular 22 upgrade (`19aa4e5`) was investigated and ruled out — it only touched an unrelated `$safeNavigationMigration` shim for image `src` bindings.

### 2.3 Git history
- `servoy-client` repo, `b7aa82b0b` ("SVY-20784 refactor the flattened stuff in for the WebCustomType", 2026-05-08, lvostinar) — introduced the current `PersistHelper.getWebComponentProperty`; this is the regression-introducing commit for the null→`[]` behavior change.
- `servoy-client` repo, `581aea8f8` ("SVY-21271 fix nested custom type creation not persisting JSON in properties view") — a later, related fix to the same refactor area (added `WebCustomTypeAddChildTest`), but does not touch the null-vs-empty-array return path. This test file establishes the harness pattern (`TestableWebComponent extends WebComponent`, `DummySolution`, `CustomJSONObjectType`) that the new regression test should follow.
- `bootstrapextracomponents` repo, `19aa4e5` ("upgrade to angular 22 (first cut)") — unrelated to this issue; ruled out.
- `bootstrapextracomponents` repo, `dbd10ce` ("migrate ESLint to flat config and fix all lint warnings") — reformatted `==`/`!=` to `===`/`!==` throughout `navbar.html`; semantically neutral, not the root cause.

## 3. Design

### 3.1 Platform fix — restore `null` contract for unset array-of-custom-object properties
In `PersistHelper.getWebComponentProperty` (`servoy_shared/src/com/servoy/j2db/util/PersistHelper.java`), in the branch that currently does:
```java
if (PersistHelper.isArrayOfCustomJSONObject(childPd.getType()))
{
    return customTypes.toArray(new IChildWebObject[customTypes.size()]);
}
```
change it to return `null` when `customTypes` is empty:
```java
if (PersistHelper.isArrayOfCustomJSONObject(childPd.getType()))
{
    return customTypes.isEmpty() ? null : customTypes.toArray(new IChildWebObject[customTypes.size()]);
}
```
This restores the pre-SVY-20784 contract: an unset array-of-custom-object property returns `null` (and therefore serializes as `null` to the client), while a property with one or more configured child persists still returns the populated array exactly as before. The non-empty path and the scalar (`else if (customTypes.size() > 0)`) path are unchanged.

### 3.2 Component fix — length-based checks in navbar template
In `navbar.html` (bootstrapextracomponents repo), change all 12 occurrences of the strict null-equality checks on `subMenuItems` from:
```html
menuItem.subMenuItems === null   →   !subMenuItems?.length   (or equivalent length-based falsy check)
menuItem.subMenuItems !== null   →   subMenuItems?.length     (or equivalent length-based truthy check)
```
applied consistently to `menuItem.subMenuItems`, at every occurrence (both `displayType === 'IMAGE'` and `displayType === 'MENU_ITEM'` blocks, duplicated across the LEFT and RIGHT layout `<ng-template>` sections). This makes the component correct independent of the platform fix — it also handles a genuinely empty array pushed via scripting (e.g. `navbar.setMenuItems(...)` with sub-items explicitly set to `[]`), which the platform's `null` contract does not cover.

The existing truthy check on line 33/280 (`{{menuItem.subMenuItems ? 'dropdown' : ''}}`) is already correct — `[]` and `null` are both falsy there — and needs no change.

## 4. Implementation plan

**Repository 1 — `servoy-client` (this repo, `servoy_shared` project, path `D:\Eclipse\ReleaseWorkspace\servoy-client\servoy_shared`):**

1. Edit `servoy_shared/src/com/servoy/j2db/util/PersistHelper.java`, method `getWebComponentProperty(AbstractBase, String)` (around line 1569-1602): change the `isArrayOfCustomJSONObject` branch to return `null` instead of an empty array when `customTypes.isEmpty()`.
2. Add a regression test in `servoy_ngclient.tests` (project at `D:\Eclipse\ReleaseWorkspace\servoy-client\servoy_ngclient.tests`), package `com.servoy.j2db.persistence`, following the existing harness pattern from `WebCustomTypeAddChildTest`/`WebComponentCloneMapIsolationTest` (`TestableWebComponent extends WebComponent`, `DummySolution`, `CustomJSONObjectType` for the array-of-custom-object property). The test must assert that `PersistHelper.getWebComponentProperty(webComponent, "someArrayOfCustomObjectProp")` returns `null` when the `WebCustomType` array property has zero configured children (no child persists added), and still returns the correct populated `IChildWebObject[]` when children exist (non-regression check for the existing behavior).
3. Compile and run the new/affected tests (`servoy_ngclient.tests`) plus any existing tests in the same package to confirm no other code depends on getting `[]` back from this branch (per the triage's noted risk).

**Repository 2 — `bootstrapextracomponents` (separate repo at `D:\GitSourcesComponents\bootstrapextracomponents\bootstrapextracomponents`):**

4. Edit `projects/bootstrapextracomponents/src/navbar/navbar.html`: change all 12 `subMenuItems === null` / `subMenuItems !== null` occurrences to length-based checks (`!subMenuItems?.length` / `subMenuItems?.length`), across both the LEFT (`@if (menuItem.displayType === 'IMAGE' ...)`, `@if (menuItem.displayType === 'MENU_ITEM' ...)`) and RIGHT duplicated layout blocks.
5. Add tests alongside the component in `navbar.spec.ts` (Vitest) covering: `subMenuItems === null` → plain link, no caret, no dropdown-menu; `subMenuItems === []` → same (the regression case); `subMenuItems` populated → dropdown-toggle, caret and populated dropdown-menu render.
6. Verify with the user's reproduction case: test solution `E:\ServoyInstalls\Svy2026_09_18august2026\workspace\respSmp`, form `testnavbar`. Confirm Mars/Venus (no sub-items) render as plain links and Earth (has sub-items: Moon/Sat2/Satt3) still renders its dropdown correctly, both before the platform fix ships (component-only hardening) and after (both fixes together).
7. Run the `bootstrapextracomponents` project's lint and test suite (`npm run lint`, `npm test`).

**Coordination note:** these two changes ship from two independent git repositories with independent release cadences. The component fix (steps 4-5) is self-contained and can ship on its own. The platform fix (steps 1-3) requires a `servoy-client` release before navbar users benefit from the broader contract restoration. Commits happen separately per-repo, each following that repo's own commit conventions.

## 5. Acceptance criteria
- [x] `PersistHelper.getWebComponentProperty` returns `null` (not an empty array) for an array-of-custom-object property with zero configured children.
- [x] `PersistHelper.getWebComponentProperty` still returns the correct populated array for an array-of-custom-object property with one or more children (no regression).
- [x] New JUnit test in `servoy_ngclient.tests` (`com.servoy.j2db.persistence` package) passes, covering both the empty and non-empty cases — `PersistHelperGetWebComponentPropertyTest`.
- [x] All 12 `subMenuItems === null` / `!== null` occurrences in `navbar.html` are replaced with length-based checks.
- [x] New Vitest tests in `navbar.spec.ts` covering null, empty-array and populated-array cases for `subMenuItems` — all passing (108/108 test suite, lint clean).
- [x] In the reproduction solution (`respSmp`, form `testnavbar`), Mars/Venus render as plain links (no caret, no empty dropdown) and Earth still renders its populated dropdown — confirmed by the user, both in the form editor and at runtime.
- [x] No other `servoy_shared`/`servoy_ngclient` code that reads this property regresses — traced all in-workspace callers of `getWebComponentProperty`/`WebComponent.getProperty`/`WebCustomType.getProperty`; all handle `null` defensively (`instanceof` checks or explicit null guards). One caller (`CustomArrayTypePropertyController.addChildPropertyDescriptors`) already expected `null` for this case, corroborating that `null` was the original contract.

## 6. Out of scope
- Auditing other marketplace/spec components for the same `=== null` pattern against array-of-custom-object properties (the triage notes this is a broader latent risk class, but fixing other components is not part of this ticket).
- Any change to the Angular 22 upgrade or its `$safeNavigationMigration` shim — confirmed unrelated.
- Changing the `else` (plain JSON property) branch of `getWebComponentProperty` — it already correctly distinguishes null/absent from present.
- Multi-level nested sub-menus (e.g. a `subMenuItem` with its own `subMenuItems`) — the navbar template only ever renders one level of dropdown; this was confirmed to be a pre-existing limitation unrelated to this fix, not a regression.

## 7. Open questions
None outstanding — both fixes implemented, tested (Java + Vitest), and verified against the user's reproduction solution.
