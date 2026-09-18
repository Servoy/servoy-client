# Spec: SVY-21470 — Form components show elements which should not be visible

## 1. Goal
Restore per-instance `visible = false` overrides on the child elements (e.g. labels) of a form-component container in the NG runtime. A regression introduced in `c446933c1` causes boolean overrides to be silently dropped during form-component persist generation, so a child element the developer explicitly hid still renders. This spec makes the incompatible-legacy-property skip guard in `FormElementHelper.generateFormComponentPersists` boxing-aware so a wrapper value (notably `Boolean`) is treated as compatible with its primitive setter parameter type, fixing `visible = false` and the whole family of primitive-typed overrides in one place.

## 2. Background

### 2.1 How form-component child overrides are applied
When a form-component container (form-component-component / list-form-component) is instantiated on a form, `FormElementHelper.generateFormComponentPersists(...)` (`servoy_ngclient/src/com/servoy/j2db/server/ngclient/FormElementHelper.java:350-414`) clones each child persist of the referenced form-component form and applies the per-instance override JSON captured on the placing form. For each child, the override JSON (`formElementValue.optJSONObject(childName)`) is walked key-by-key: the setter for the key is looked up via `RepositoryHelper.getSetters(...)`, the setter's first parameter type is used to decide type-compatibility, and the value is then stored via the `setProperty` / `setCustomProperties` / merge branches at lines 388-413.

### 2.2 The regression (`c446933c1`)
Commit `c446933c1` ("Skip incompatible legacy properties in form component persist generation", 2026-08-25) added a guard — now at `FormElementHelper.java:378-385` — that `continue`s and skips a property entirely when, after all conversion attempts, the value still does not fit the setter's parameter type. Its intent was to drop genuinely stale legacy `location`/`size`/`anchors` values that no longer exist in the spec after the Angular 22 form-component migration.

The guard is too broad for primitive-typed properties. `visible` is a plain boolean in the content spec and its setter is `BaseComponent.setVisible(boolean)` (`servoy_shared/src/com/servoy/j2db/persistence/BaseComponent.java:186`). An override value of `visible = false` arrives as a `java.lang.Boolean`. The compatibility test is:

```java
!paramType.isAssignableFrom(val.getClass()) && !(paramType.isPrimitive() && val instanceof Number)
```

For `paramType = boolean.class` and `val = Boolean.FALSE`:
- `boolean.class.isAssignableFrom(Boolean.class)` is `false`.
- The `Number` exemption is `false` because `Boolean` is not a `Number`.
- `visible` has no `IDesignValueConverter`, and `fs.searchPersist("false")` returns `null`.

So the guard at line 378 is satisfied, `continue` runs at line 384, and the `visible = false` override is dropped. The cloned child keeps its default `visible = true` and renders. Before `c446933c1` there was no skip; the value fell through to `setProperty("visible", Boolean.FALSE)` at line 413 and the element was correctly hidden.

### 2.3 Prior false positive from the same guard
Commit `0c8752d63` (SVY-21469, 2026-09-17) already had to add one exemption to this same guard — a legacy String `customProperties` (lines 379-381) — because that value is merged via `setCustomProperties` further down and must not be skipped. Boolean overrides are the next false positive from the same guard. The two exemptions accreted so far indicate the guard's compatibility test is the wrong shape: it does not account for the fact that JSON scalar values are wrapper objects while many persist setters take primitives.

## 3. Design

### 3.1 Make the compatibility test boxing-aware
The core defect is that both the pre-conversion test (line 362) and the post-conversion guard (line 378) only exempt `Number` from the primitive mismatch, but a JSON scalar for a primitive setter is always a wrapper (`Boolean`, `Character`, or a numeric wrapper). The fix is to treat a wrapper value as compatible with its own primitive type.

Introduce a small helper that answers "is `val` assignable/boxing-compatible with `paramType`":

```java
private static boolean isBoxingCompatible(Class< ? > paramType, Object val)
{
    if (paramType.isAssignableFrom(val.getClass())) return true;
    if (!paramType.isPrimitive()) return false;
    if (paramType == boolean.class) return val instanceof Boolean;
    if (paramType == char.class) return val instanceof Character;
    // all remaining primitives (byte, short, int, long, float, double) are numeric
    return val instanceof Number;
}
```

This subsumes the existing `paramType.isPrimitive() && val instanceof Number` exemption (numeric wrappers stay compatible) and additionally makes `Boolean`↔`boolean` and `Character`↔`char` compatible. It deliberately does not widen anything else: a genuinely stale legacy value (e.g. a `String "not-a-dimension"` for a `Dimension`-typed `size` setter, or a UUID string for an int) still fails both `isAssignableFrom` and the primitive-wrapper checks, so it remains skipped — preserving `c446933c1`'s intent.

### 3.2 Apply the helper at both guard sites
Replace the two occurrences of the compatibility expression:

- Line 362 (the entry condition that decides whether conversion is even attempted):
  `if (!paramType.isAssignableFrom(val.getClass()) && !(paramType.isPrimitive() && val instanceof Number))`
  becomes `if (!isBoxingCompatible(paramType, val))`.

- Line 378 (the post-conversion skip guard), keeping the existing `customProperties` String exemption intact:
  `if (!paramType.isAssignableFrom(val.getClass()) && !(paramType.isPrimitive() && val instanceof Number) && !(val instanceof String && ...CUSTOMPROPERTIES...))`
  becomes `if (!isBoxingCompatible(paramType, val) && !(val instanceof String && ...CUSTOMPROPERTIES...))`.

Using the helper at line 362 means that for a well-typed `Boolean` `visible` override, the conversion block (lines 363-386) is skipped entirely and control flows straight to line 388, where the `else` at line 413 does `setProperty("visible", Boolean.FALSE)` — exactly the pre-regression behaviour.

### 3.3 Why not a targeted `visible` exemption
A targeted exemption (Approach 2 in triage) would only defer the problem: the next primitive-boolean or char override hits the same guard and would need yet another special-case, compounding the two exemptions already present. The boxing-aware helper addresses the root cause generically while keeping the skip behaviour for truly incompatible legacy values.

### 3.4 Git history (carried from triage)
- `c446933c1` (Johan Compagner, 2026-08-25) — introduced the incompatible-legacy skip guard; regression source.
- `0c8752d63` (SVY-21469, 2026-09-17) — added the first exemption (legacy String `customProperties`) to the same guard, confirming its proneness to false positives.
- `b4252b26b` (SVY-21344, 2026-09-01) and the Angular 22 form-component migration — surrounding context in which spec-less legacy values began appearing on form-component children.

## 4. Implementation plan

1. In `servoy_ngclient/src/com/servoy/j2db/server/ngclient/FormElementHelper.java`, add a `private static boolean isBoxingCompatible(Class<?> paramType, Object val)` helper as described in §3.1 (handles `isAssignableFrom`, `boolean`↔`Boolean`, `char`↔`Character`, and numeric primitives↔`Number`).
2. In `generateFormComponentPersists`, replace the compatibility expression at line 362 with `!isBoxingCompatible(paramType, val)`.
3. In the same method, replace the compatibility portion of the post-conversion guard at line 378 with `!isBoxingCompatible(paramType, val)`, preserving the existing `customProperties` String exemption unchanged.
4. Add a regression test in the form-component persist-generation harness, mirroring `FormElementHelperCustomPropertiesTest` (SVY-21469): create a form-component form with a child, invoke `generateFormComponentPersists` with a per-child JSON of `{ "visible": false }`, and assert the generated child clone reports `getVisible() == false`. Also assert the guard's intent is preserved — a genuinely incompatible legacy value alongside `visible = false` (e.g. a String `size`) is still skipped without preventing the `visible` override from being applied.
5. Run the post-modification compile loop (`eclipse-ide_getCompilationErrors`) and the new/affected tests; ensure the workspace is clean.

## 5. Acceptance criteria
- [ ] A form-component child with a per-instance override `visible = false` renders hidden at runtime in the NG client (the override is applied, not dropped).
- [ ] `generateFormComponentPersists` applies `Boolean` overrides for primitive-`boolean` setters (and, by construction, `Character` for `char` and numeric wrappers for numeric primitives).
- [ ] Genuinely incompatible legacy values (String for a `Dimension` `size`, UUID string for an int, stale `location`/`anchors`) are still skipped, i.e. `c446933c1`'s intent is preserved.
- [ ] The existing SVY-21469 legacy String `customProperties` merge behaviour is unchanged (still merged, not skipped).
- [ ] A regression test pins `visible = false` propagating to the cloned child persist and passes; the workspace compiles with no new errors.

## 6. Out of scope
- Rewriting or narrowing the guard to an explicit stale-key denylist (Approach 3).
- Any designer-side / visible-dataprovider handling in `FormElement.initTemplateProperties` (touched by SVY-19985) — not the cause of this runtime override loss.
- Changes to the content spec, `BaseComponent.setVisible`, or the form-component migration itself.

## 7. Open questions
| Question | Owner | Status |
|----------|-------|--------|
| None — approach, root cause, and test harness are fully determined by the triage report and existing SVY-21469 test. | — | closed |
