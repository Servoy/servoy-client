# Triage Report — SVY-21470

**Verdict:** PROCEED

## Reported problem
Form components (form-component-component / list-form-component containers) that
contain child elements — e.g. labels — which have a per-instance override of
`visible = false` (set on the form where the form component is placed) still render
those child elements at runtime. The `visible` override "does not do anything".

The attached screenshot is a running NG client (Nightly Release 2026.9.0), not the
form editor, so this is a runtime rendering defect, not a designer-only display
issue.

The ticket reports only the symptom; it proposes no solution.

## Root-cause assessment
The override values for form-component child elements are applied in
`FormElementHelper.generateFormComponentPersists(...)`
(`servoy_ngclient/src/com/servoy/j2db/server/ngclient/FormElementHelper.java:350-414`).
For each child, the per-instance override JSON (`formElementValue.optJSONObject(childName)`)
is walked key-by-key. The loop looks up the persist's setter for the key
(`RepositoryHelper.getSetters(...)`) and uses the setter's parameter type only to
decide whether the JSON value is type-compatible; the value is then actually stored
via `setProperty` / `setCustomProperties` / merge branches at lines 388-413.

Commit `c446933c1` ("Skip incompatible legacy properties in form component persist
generation", 2026-08-25) added a guard (now at
`FormElementHelper.java:378-385`) that `continue`s — skipping the property entirely —
when, after all conversion attempts, the value still does not fit the setter's
parameter type. The intent was to drop stale legacy `location`/`size`/`anchors`
values that no longer exist in the spec after the Angular 22 migration.

That guard is too broad for boolean properties:

- `visible` is a plain boolean in the content spec (see
  `StaticContentSpecLoader.java:803-895`, and `BaseComponent.setVisible(boolean)` at
  `servoy_shared/.../BaseComponent.java:186`). Its setter parameter type is the
  primitive `boolean`.
- An override value of `visible = false` arrives as a `java.lang.Boolean`.
- `boolean.class.isAssignableFrom(Boolean.class)` is `false`.
- The Number exemption `paramType.isPrimitive() && val instanceof Number` is also
  `false`, because `Boolean` is not a `Number`.
- `visible` has no `IDesignValueConverter`, so the conversion branch at line 365 does
  not apply, and `fs.searchPersist("false")` returns null.
- Therefore the final guard at line 378 is satisfied and the code executes
  `continue` at line 384, dropping the `visible = false` override.

With the override dropped, the cloned child persist keeps its default `visible = true`
and the element renders. Before `c446933c1` there was no skip, so the value fell
through to `setProperty("visible", Boolean.FALSE)` at line 413 and the element was
correctly hidden.

This is the same class of false-positive skip that already required one exception:
commit `0c8752d63` (SVY-21469, 2026-09-17) had to exempt a legacy String
`customProperties` from the very same guard (lines 379-381). Boolean overrides are the
next false positive from the same guard.

## Ticket premise check
The ticket only describes the symptom and offers no proposed fix, so there is no
proposed approach to challenge. The premise that this is a real Servoy defect holds:
it is a runtime regression introduced by `c446933c1`, reproducible from the code path
without any user misconfiguration. It is not expected behaviour, not user-side, and
not a third-party issue.

## Approaches considered
1. **Make the type-compatibility guard boxing-aware (recommended).** Treat a wrapper
   value as compatible with its primitive setter type (`Boolean`↔`boolean`,
   `Character`↔`char`, and the numeric wrappers), instead of only exempting
   `Number`. This fixes `visible` and any other primitive-boolean/char override in one
   place and removes a whole family of latent false positives.
   - Pros: minimal, general, addresses the root cause rather than one property; keeps
     the original guard's intent (still skips genuinely stale `location`/`size`/
     `anchors` that have no setter or an object-typed setter).
   - Cons: must be careful that legitimately-incompatible legacy values (the ones
     `c446933c1` was meant to drop) are still skipped — those do not box to the
     setter's primitive type, so they remain caught.

2. **Add a targeted exemption for `visible` (and other known boolean props),** mirroring
   the `customProperties` exemption from `0c8752d63`.
   - Pros: very small, low risk to the specific reported case.
   - Cons: treats a symptom; the next boolean/char override will hit the same guard.
     Accretes special-cases (this would be the second patch on the same guard).

3. **Narrow the guard to only skip the known stale keys** (`location`, `size`,
   `anchors`) rather than skipping anything that fails the type check.
   - Pros: most conservative about what gets dropped; makes intent explicit.
   - Cons: a denylist can miss other genuinely-stale legacy keys the original guard
     was catching generically; slightly larger behavioural change to the guard.

4. **No code change.**
   - Pros: none.
   - Cons: leaves a clear, reproducible runtime regression (fixVersion 2026.9.0) in
     which `visible = false` on form-component children is silently ignored. Not
     acceptable — the property is documented and expected to work.

## Recommendation
PROCEED with **Approach 1**: make the incompatible-legacy-property skip guard in
`FormElementHelper.generateFormComponentPersists` boxing-aware so a wrapper value
(notably `Boolean`) is considered compatible with its primitive setter parameter type,
instead of only exempting `Number`. This directly restores the `visible = false`
override behaviour and eliminates the same class of false-positive skip for other
primitive-typed properties, while still dropping the truly stale legacy values that
`c446933c1` targeted. Approach 2 is an acceptable smaller fallback if a maximally
narrow change is preferred, but it only defers the underlying problem. A regression
test in the form-component persist-generation harness (as already exists for
SVY-21469) should pin `visible = false` propagating to the cloned child persist.

## Git history findings
- `c446933c1` (Johan Compagner, 2026-08-25) — introduced the incompatible-legacy skip
  guard in `FormElementHelper.generateFormComponentPersists`; this is the regression
  source.
- `0c8752d63` (SVY-21469, 2026-09-17) — already had to add one exemption to this same
  guard (legacy String `customProperties`), confirming the guard is prone to
  false positives; the current bug is another instance.
- `b4252b26b` (SVY-21344, 2026-09-01) and the Angular 22 form-component migration work
  are the surrounding context in which absolute-positioned legacy forms inside form
  components began carrying spec-less legacy values.
- `4191a9e60` (SVY-19985) touched `FormElement.initTemplateProperties` visible
  handling, but that path is designer/visible-dataprovider specific and is not the
  cause of this runtime override loss.
