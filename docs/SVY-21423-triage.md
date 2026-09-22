# Triage Report — SVY-21423

**Verdict:** PROCEED

## Reported problem
Navbar component (bootstrapextracomponents) with 3 menu items, where only one item (`Earth`/`item3`) has actual sub-menu items configured. All three items now render the dropdown-caret/dropdown-toggle markup and an (empty, for the two items with no sub-items) `dropdown-menu` `<div>`, instead of the two items without sub-items rendering as plain links. The reporter's own AI-assisted DOM inspection (quoted in the ticket) already narrowed this to: the JSON serialization of an *unset* `subMenuItems` array property changed from `null` to `[]`, and the navbar template's `menuItem.subMenuItems === null` / `!== null` checks then take the wrong branch for an empty (but non-null) array.

The user-supplied hypothesis ("recent Angular 22 upgrade may be the root cause") is a plausible-looking but incorrect lead — investigated and ruled out below.

## Root-cause assessment

### 1. The component template does distinguish `null` vs `[]` explicitly
`projects/bootstrapextracomponents/src/navbar/navbar.html` (repo `bootstrapextracomponents`) branches on strict null-checks in several places, e.g.:

```html
@if (menuItem.displayType === 'MENU_ITEM' && menuItem.subMenuItems === null) {
  <!-- plain link, no dropdown -->
}
@if (menuItem.displayType === 'MENU_ITEM' && menuItem.subMenuItems !== null) {
  <!-- dropdown-toggle + <span class="caret"> + populated/empty <div class="dropdown-menu"> -->
}
```//and similarly for `displayType === 'IMAGE'`, at 4 duplicated nesting levels (LEFT/RIGHT × top-level/button-wrapped).

An empty array `[]` is `!== null`, so it takes the dropdown branch and renders the caret + an empty `dropdown-menu`. This exactly matches the screenshot: Venus (`item2`) and Mars (`item1`) — which have no configured sub-items — show the broken/boxed caret with an empty dropdown, while Earth (`item3`, which does have sub-items: Moon/Sat2/Satt3) renders correctly.

`git blame` on this file shows the `===`/`!==` null-check style itself is *not new* — it predates the Angular 22 bump (introduced incrementally over many navbar commits going back years) and was only reformatted for stricter equality by the ESLint flat-config migration commit `dbd10ce` (`== null` → `=== null`), which is a semantically-neutral change for a real `null` vs `[]` value. **The template's contract has always been "server sends `null` for an unset array, sends a real array (possibly with items) when there are sub-items"** — it was never written to treat `[]` as "no dropdown".

### 2. Angular 22 upgrade (commit `19aa4e5`) is not implicated
`git show 19aa4e5 --stat` for the `bootstrapextracomponents` repo shows the Angular 22 upgrade touched only:
- `package.json` / `package-lock.json` / `tsconfig*.json` (build tooling)
- `navbar.html`: two lines, both `[src]="menuItem.dataProvider?.url"` → `[src]="$safeNavigationMigration(menuItem.dataProvider?.url)"` — an Angular-22-required safe-navigation shim, unrelated to `subMenuItems`.

No other navbar/dropdown-related lines were touched by the Angular 22 commit. This rules out the Angular version bump as the cause.

### 3. Actual root cause: platform-side serialization of unset array properties (servoy-client repo)
Traced through the property-value pipeline for `subMenuItems` (declared in `navbar.spec` as `"subMenuItems": {"type": "subMenuItem[]", ...}`, with no `"default"` — i.e. genuinely unset when the developer never configured sub-items):

- **Before** the refactor in commit `b7aa82b0b` ("SVY-20784 refactor the flattened stuff in for the WebCustomType", 2026-05-08, in `servoy-client`), the legacy `WebObjectImpl.getProperty(...)` read the value straight from the persist's JSON (`json.opt(propertyName)`), which is `null` when the key is absent, and passed that through `convertToJavaType`. An absent/unset array property therefore reached the client as `null`.

- **After** the refactor, the equivalent logic lives in `PersistHelper.getWebComponentProperty(AbstractBase, String)` (`servoy_shared/src/com/servoy/j2db/util/PersistHelper.java:1569-1602`):

  ```java
  if (PersistHelper.isPersistMappedProperty(childPd))
  {
      List<IPersist> customTypes = new ArrayList<IPersist>();
      webComponent.getAllObjects().forEachRemaining(customType -> {
          if (customType instanceof IChildWebObject childWebObject && Utils.equalObjects(childWebObject.getJsonKey(), propertyName))
              customTypes.add(customType);
      });
      if (PersistHelper.isArrayOfCustomJSONObject(childPd.getType()))
      {
          return customTypes.toArray(new IChildWebObject[customTypes.size()]);   // <-- always an array, never null
      }
      ...
  }
  ```

  `subMenuItems` is typed `subMenuItem[]`, and `subMenuItem` is itself a `JSON_obj` (custom) type, so `PersistHelper.isArrayOfCustomJSONObject(...)` is `true` for this property — it is treated as "persist-mapped" (i.e. a property whose values live as real child persists/`WebCustomType`s under the parent component, the same mechanism used for things like tab panels or portal columns). When no sub-menu-item child persists exist (the normal, unset case), `customTypes` is an empty `ArrayList`, and the method unconditionally returns `customTypes.toArray(...)`, i.e. **an empty array `IChildWebObject[0]`**, never `null`. This empty Java array subsequently serializes to `[]` in the browser JSON payload — matching the ticket's own diagnosis precisely.

  The `else` branch (plain, non-persist-mapped JSON properties) still correctly distinguishes "key absent" (`!json.has(propertyName)` → returns `null`) from "key present" — the bug is specific to the persist-mapped/array-of-custom-object branch.

This is a **platform regression in `servoy-client`** (the SVY-20784 refactor), not a bug in the `bootstrapextracomponents` navbar component and not related to any Angular version.

## Ticket premise check
The ticket (via the reporter's own AI-assisted investigation) already correctly identifies the mechanism and proposes two candidate fixes:
- **Platform**: restore `null` for unset array properties, or
- **Component**: harden the condition to `subMenuItems?.length > 0`.

Both are technically valid and this triage confirms the platform-side root cause exactly (`PersistHelper.getWebComponentProperty`, not the navbar template). The ticket does not misattribute the fix location — it leaves the choice open and asks for the component team's call, which this triage should settle.

Between the two:
- Fixing only the **component** (`subMenuItems?.length > 0` instead of `=== null` / `!== null`) fixes the *visible* symptom for this component immediately, and is strictly more robust (survives an explicitly-emptied array too, e.g. via `removeMenuItem`/API calls that could leave a `[]`). It requires touching every `=== null`/`!== null` occurrence in `navbar.html` (12 occurrences across 2 duplicated LEFT/RIGHT layout blocks) plus the truthy check on line 33/280 (`menuItem.subMenuItems ? 'dropdown' : ''`, which is *already* correct since `[]` and `null` are both falsy/effectively-no-dropdown for that specific check — only the strict-equality checks are wrong).
- Fixing only the **platform** (`servoy-client`, restore `null`) fixes this class of regression for *all* components across the whole component ecosystem that may have written `=== null` checks against the old (correct) `null` contract, without requiring every third-party/marketplace component to be patched. This is the same class of implicit API contract ("unset array property is `null`, not `[]`") that other spec/marketplace components likely rely on.

## Approaches considered
1. **Platform fix in `servoy-client`** — restore `null` for unset array-of-custom-object properties in `PersistHelper.getWebComponentProperty` (return `null` instead of an empty array when `customTypes.isEmpty()`). Pros: fixes the regression at its source for every component relying on the old null-contract, minimal code change (one guard), matches documented behavior prior to SVY-20784. Cons: need to verify no other code now depends on getting `[]` back for this branch (e.g. `.length` calls without null-checks elsewhere in servoy-client/ngclient conversion code) — requires a compile + test pass across `servoy-client`/`servoy_ngclient`.
2. **Component fix in `bootstrapextracomponents`** — change the 12 `=== null`/`!== null` checks in `navbar.html` to `.length`-based checks (`subMenuItems?.length` / `!subMenuItems?.length`). Pros: self-contained, ships independently of a platform release, also hardens against an explicitly-emptied array. Cons: does not fix any other component (in this or other component libraries) that has the same `=== null` pattern against array properties; treats a platform contract change as "the new normal" instead of restoring the documented behavior.
3. **Both** — fix the platform (restores the documented contract broadly) and additionally harden the navbar component's checks (defensive, handles a future explicitly-set empty array from scripting, e.g. `navbar.setMenuItems(...)` with an empty `subMenuItems: []` set deliberately by a Servoy developer). This is the most robust option and matches exactly what the ticket's own "AI" analysis recommended as the two complementary fixes.
4. **No code change** — not viable; this is a confirmed, reproducible visual regression with a screenshot and a traced root cause; leaving it as-is breaks any navbar-derived component (and potentially other spec components using unset array properties) that relies on the `null` contract.

## Recommendation
**PROCEED** with **Approach 3 (both fixes)**:

1. **Platform (`servoy-client`, primary fix)**: in `PersistHelper.getWebComponentProperty`, when `PersistHelper.isArrayOfCustomJSONObject(childPd.getType())` is true and `customTypes` is empty, return `null` instead of an empty array — restoring the pre-SVY-20784 contract that an unset array-of-custom-object property serializes as `null`. This is a one-branch change; must be paired with a regression test (e.g. extending `com.servoy.eclipse.model.tests` or an equivalent servoy-client/ngclient plugin test) asserting that a `WebCustomType` array property with zero children returns `null` from `getProperty(...)`, and that this is what reaches the client JSON (`null`, not `[]`).
2. **Component (`bootstrapextracomponents`, defensive hardening)**: change the navbar's `subMenuItems === null` / `!== null` checks to `subMenuItems?.length` / `!subMenuItems?.length` (or equivalently `(subMenuItems?.length ?? 0) > 0`) in `navbar.html`, so the component also renders correctly if a genuinely empty array is ever pushed to it (scripted or otherwise), independent of the platform fix.

This matches the ticket's own two proposed candidates and applies both, since they solve different (but related) problems: the platform fix restores a broad, previously-relied-upon contract; the component fix removes a latent fragility that made the navbar component sensitive to that contract in the first place.

## Git history findings
- `bootstrapextracomponents` repo, commit `19aa4e5` ("upgrade to angular 22 (first cut)", 2026-08-04, Johan Compagner): only touched `navbar.html` for `$safeNavigationMigration` around `menuItem.dataProvider?.url` (image `src` bindings) — unrelated to `subMenuItems`/dropdown rendering. Rules out the Angular 22 bump as a cause.
- `bootstrapextracomponents` repo, commit `dbd10ce` ("migrate ESLint to flat config and fix all lint warnings"): reformatted `==`/`!=` to `===`/`!==` throughout `navbar.html`, including the `subMenuItems` null-checks — semantically neutral for real `null`, but this is the style the current code already had; not the root cause, just makes the pre-existing pattern visible in diffs.
- `servoy-client` repo, commit `b7aa82b0b` ("SVY-20784 refactor the flattened stuff in for the WebCustomType", 2026-05-08, lvostinar): introduced the current `PersistHelper.getWebComponentProperty`, replacing `WebObjectImpl.getProperty`. This is where the null→`[]` behavior change was introduced for array-of-custom-JSON-object properties with zero children. This is the actual regression-introducing commit.
- `servoy-client` repo, commit `581aea8f8` ("SVY-21271 fix nested custom type creation not persisting JSON in properties view"): a later, related fix to the same refactor area, but does not touch the null-vs-empty-array return path.
