# Spec: SVY-21667 — Guard the form-component value cast and report a clear, actionable error

> **Where the code lives:** The defect and all code changes in this spec are in the
> **`servoy_ngclient`** project, which is a **separate git repository** checked out at
> `D:\Eclipse\202603Workspace\servoy-client\servoy_ngclient`. This spec document itself
> lives in the `servoy-eclipse` repo's `docs/` only because that is where this SDD
> pipeline keeps its artifacts (the triage report is at `docs/SVY-21667-triage.md`).
> The two files to edit are:
>
> - `D:\Eclipse\202603Workspace\servoy-client\servoy_ngclient\src\com\servoy\j2db\server\ngclient\FormElementHelper.java`
> - `D:\Eclipse\202603Workspace\servoy-client\servoy_ngclient\src\com\servoy\j2db\server\ngclient\property\types\FormComponentPropertyType.java`
>
> The coding agent must edit the files in the `servoy_ngclient` checkout, **not** in
> `servoy-eclipse`.

## 1. Goal

When a form's `.frm` file contains a (List) Form Component whose `containedForm`-style
property is a bare string (a form name such as `"fc_customer_row"` or a form UUID such as
`"DD0AECC8-…"`) instead of the canonical object `{ "svy_form": "<form UUID>" }`, opening
the form throws a raw `java.lang.ClassCastException: class java.lang.String cannot be cast
to class org.json.JSONObject` from deep inside tab-sequence processing. The exception names
no form, no element and no property, so neither a developer nor an AI agent can tell what is
actually wrong — and because the form still partly renders, the exception reads as noise.
The goal is to **guard the `(JSONObject)` cast on the form-component value and replace the
unhelpful `ClassCastException` with a single clear, actionable `Debug.error`** that names the
form, the element, the property, the actual value received, and the expected
`{ "svy_form": "<form UUID>" }` shape, then **skip that property** instead of aborting
tab-sequence processing. This is a reject-and-report fix (log and skip), not a coercion of
the string into the object form, and not a new public API.

## 2. Background

### 2.1 The failing path

The crash originates in `FormElementHelper.addFormComponentProperties(...)`
(`FormElementHelper.java`). The ticket's stack trace is from Servoy 2026.9.0.4202, where the
cast was at line 1024; in the current checkout the same code is at **line 992–993**:

```java
// FormElementHelper.java
981:  Object frmValue = formComponentEl.getPropertyValue(property.getName());
982:  Form frm = FormComponentPropertyType.INSTANCE.getForm(frmValue, flattenedSolution);
983:  if (frm == null) continue;
...
992:  List<IFormElement> formComponentPersistsForFCProp =
993:      generateFormComponentPersists(formComponentEl, property, (JSONObject)frmValue, frm, flattenedSolution);
```

`addFormComponentProperties` is reached from `getControlledTabSeqReplacementFor(...)` →
`NGTabSeqPropertyType.toFormElementValue(...)` → `FormElement.initTemplateProperties` →
`WebFormUI.init`, which is why the symptom surfaces as a tab-sequence stack trace far from the
real (malformed-`.frm`) cause.

### 2.2 Why the existing `frm == null` guard does not protect the cast

`FormComponentPropertyType.getForm(frmValue, fs)` (`FormComponentPropertyType.java:214`) is
deliberately **tolerant of a bare String or UUID** — it has long resolved a form by name or
UUID, which other call paths rely on:

```java
public Form getForm(Object formElementValue, FlattenedSolution fs) {
    Object formId = formElementValue;
    if (formId instanceof JSONObject) formId = ((JSONObject)formId).optString(SVY_FORM);
    Form form = null;
    if (formId instanceof String || formId instanceof UUID) {
        form = fs.getForm(formId.toString());               // resolves "fc_customer_row" or the UUID
        if (form == null) form = (Form)fs.searchPersist(formId.toString());
    } else if (formId instanceof JSForm) {
        return ((JSForm)formId).getSupportChild();
    }
    return form;
}
```

So when `containedForm` is a bare string that happens to name/UUID a real form, `getForm`
**succeeds** (returns a non-null `Form`), the `if (frm == null) continue;` guard at line 983
is passed, and line 992/993 then unconditionally does `(JSONObject)frmValue` on a value that
is still a `String`. The guard and the cast disagree about the shape of `frmValue`: the guard
accepts a String, the cast does not. This is a mismatch between a lenient resolver and a
strict cast — not a regression from a dependency or platform bump.

### 2.3 The same latent pattern at sibling call sites

The identical "`getForm(...)` accepts a String while follow-up code casts to `JSONObject`"
pattern recurs at five more call sites — four in `FormComponentPropertyType.java`, plus a
**sixth site found only during implementation/testing**, in `FormWrapper.java`'s FreeMarker
template-data builder. Each would throw the same context-free `ClassCastException` for the
same bad input, reached through a different code path (Rhino value, template JSON, sablo
value, Rhino converter, FreeMarker template generation):

| File | Method | Cast |
|------|--------|------|
| `FormComponentPropertyType.java` | `toRhinoValue` | `(JSONObject)webComponentValue` |
| `FormComponentPropertyType.java` | `toTemplateJSONValue` | `(JSONObject)formElementValue` |
| `FormComponentPropertyType.java` | `toSabloComponentValue` | `(JSONObject)formElementValue` |
| `FormComponentPropertyType.java` | `getFormComponentRhinoConverter` | `(JSONObject)currentValue` |
| `FormWrapper.java` | `checkFormComponents` | `(JSONObject)propertyValue` |
| `ChildrenJSONGenerator.java:241` | children-JSON payload builder | `(JSONObject)propertyValue` — **found 2026-11-02, NOT fixed, see §3.3c** |
| `FormComponentUtils.java:93` | `addFormComponentComponentChildrenInternal` | `(JSONObject)rawPropertyValueOfFEsFCProperty` — **found 2026-11-02, NOT fixed, see §3.3c** |
| `FormComponentSabloValue.java:130` | `getComponents()` (foundset-linked sablo children) | `(JSONObject)propertyValue` — **found 2026-11-02, NOT fixed, see §3.3c** |

At every site except `getFormComponentRhinoConverter` the cast is guarded by a preceding
`getForm(...) != null` check (same lenient-resolver trap as the primary site).
`getFormComponentRhinoConverter` casts `currentValue` directly with no `getForm` guard at all.

**How the sixth site (`FormWrapper.checkFormComponents`) was found:** it was missed by the
original triage, this spec's first draft, and the first code-review pass, because it sits in
FreeMarker template generation (`FormTemplateGenerator.generate` → `form_recordview_js.ftl`'s
`#list baseComponents`), not in any of the five render/scripting paths this spec originally
enumerated. It surfaced only once a dedicated integration test
(`FormComponentPropertyTypeSabloRhinoGuardTest`, added in the test-gen phase) called
`showFormInCurrentContainer` on a form holding the malformed value, which triggers template
generation and hit the unguarded cast — producing a `freemarker.core._TemplateModelException`
wrapping the same `ClassCastException`. It has the identical shape as the primary site: `Form
frm = FormComponentPropertyType.INSTANCE.getForm(propertyValue, context.getSolution()); if
(frm == null) continue;` followed by an unguarded `(JSONObject)propertyValue` cast, including
the same `recursiveCheck.add(frm.getName())`/`recursiveCheck.remove(frm.getName())`
bookkeeping pattern as the primary site's `recursionCheck`.

**How the seventh/eighth/ninth sites were found, and why they are listed but not fixed:**
after fixing the sixth site, a systematic `grep` for `.getForm(` paired with `(JSONObject)`
casts was run across the whole `servoy_ngclient/src` tree to answer the open question "could
there be a seventh site" (this was previously unresolved — see §7). It found three more sites
with the identical defect shape, detailed in §3.3c. **The user was asked explicitly whether to
fix these three now or defer, and chose to defer** ("Document only, fix later"). They are
listed here, and described in full in §3.3c, as a known, documented gap — not fixed in this
pass.

### 2.4 Context already in scope at the primary site

The `addFormComponentProperties` loop already has everything needed for a good message:

- element name — `formComponentEl.getName()`
- property name — `property.getName()`
- containing form — `formComponentEl.getForm().getName()` (the form that owns the element)
- the actual bad value — `frmValue`
- the resolved target form — `frm.getName()` (useful extra context: the string *did* resolve)

Nothing new has to be plumbed through. `FormElementHelper.java` already imports
`com.servoy.j2db.util.Debug` (line 79), and `FormComponentPropertyType.java` already imports
`Debug` (line 72) and exposes the public constant `SVY_FORM = "svy_form"` (line 86), so the
expected-shape text can be built from `SVY_FORM` rather than a hardcoded literal.

### 2.5 Lead developer's scope

Johan Compagner's comment on the ticket sets the scope explicitly: *"we should be able to
catch this error and then report a nicer error in the log (and back to the ai) that we got X
and we expect a json object with structure Y."* That is a guard-and-report fix. The approved
approach is **reject-and-report only** — do **not** coerce the string into the object form.

## 3. Design

### 3.1 A small shared helper on `FormComponentPropertyType`

Introduce one private-logic helper that both files call, so the guard and the error text are
written once. Put it on `FormComponentPropertyType` (where `SVY_FORM`, `getForm`, and the
canonical shape knowledge already live) as a `public` method so `FormElementHelper` (a
different package) can call it:

```java
/**
 * Validates that a form-component property value has the canonical object shape
 * { "svy_form": "<form UUID>" } and returns it as a JSONObject.
 *
 * Returns null (after logging one clear, actionable Debug.error) when the value is not a
 * JSONObject — e.g. a bare form-name/UUID string written by hand into the .frm. Callers
 * must treat null as "skip this property" rather than casting and throwing a context-free
 * ClassCastException. See SVY-21667.
 *
 * @param value   the raw form-component property value (may be a String, JSONObject, …)
 * @param formName the form that owns the element (for the message), or null if unknown
 * @param elementName the element name (for the message), or null if unknown
 * @param propertyName the property name, e.g. "containedForm" (for the message)
 */
public static JSONObject asFormComponentValue(Object value, String formName, String elementName, String propertyName)
{
    if (value instanceof JSONObject) return (JSONObject)value;
    Debug.error("Form component property '" + propertyName + "' on element '" + elementName +
        "' of form '" + formName + "' has an unexpected value: " +
        (value == null ? "null" : value.getClass().getSimpleName() + " " + value) +
        ". Expected a JSON object of the form { \"" + SVY_FORM + "\": \"<form UUID>\" }. " +
        "Skipping this form-component property. (SVY-21667)");
    return null;
}
```

Design points:

- **Reject-and-report, not coerce.** The helper returns `null` for a non-`JSONObject`; it does
  **not** wrap the string into `{ svy_form: <uuid> }`. Coercion (triage approach 2) is
  explicitly out of scope.
- **Message content** must name: the property, the element, the owning form, the actual value
  (class + value, null-safe), and the expected `{ "svy_form": "<form UUID>" }` shape built from
  the `SVY_FORM` constant. Include `(SVY-21667)` so the log line is greppable.
- **Log once, not per render.** `Debug.error` here fires on the form-load / tab-sequence
  computation path; that path is already cached per form (`formTabSequences` /
  `FORM_TAB_SEQUENCE` runtime property in `FormElementHelper`), so a single malformed property
  logs essentially once per form load rather than on every paint. Do **not** add a new
  per-value de-dup cache — rely on the existing caching of the computed tab sequence. If, in
  testing, a given path is found to re-enter per render, demote that specific site's log to
  `Debug.warn`/first-time-only, but do not over-engineer a cache up front.
- **`static`** so it can be called from `FormComponentPropertyType.INSTANCE`-less contexts and
  from `FormElementHelper`; it needs no instance state.
- Some sibling call sites lack a nearby element/form/property in scope (e.g.
  `getFormComponentRhinoConverter` has `property` but no form/element). For those, pass what is
  available and `null` for the rest — the message degrades gracefully (`element 'null'`), still
  far better than a context-free `ClassCastException`. Prefer passing real context wherever the
  method already has it (the primary site has all of it).

### 3.2 Primary site — `FormElementHelper.addFormComponentProperties` (~line 992)

Replace the unguarded cast with a guard that skips the property on a bad value:

```java
// SVY-21667: guard the cast; a malformed .frm can hold a bare string here even though
// getForm(...) resolved it, which would otherwise throw a context-free ClassCastException.
JSONObject frmJSON = FormComponentPropertyType.asFormComponentValue(frmValue,
    formComponentEl.getForm() != null ? formComponentEl.getForm().getName() : null,
    formComponentEl.getName(), property.getName());
if (frmJSON == null)
{
    recursionCheck.remove(frm.getName()); // undo the add() done a few lines above
    continue;
}
List<IFormElement> formComponentPersistsForFCProp = generateFormComponentPersists(
    formComponentEl, property, frmJSON, frm, flattenedSolution);
```

> **Important — balance the `recursionCheck` set.** At line 985 the code does
> `if (!recursionCheck.add(frm.getName())) { … continue; }`, i.e. it has *already added*
> `frm.getName()` to `recursionCheck` by the time we reach the cast at 992. When we now
> `continue` on a bad value, we must first `recursionCheck.remove(frm.getName())` so the set
> stays consistent for the rest of the loop (otherwise a later legitimate reference to the same
> form name could be wrongly treated as recursive). Confirm the exact variable name and the
> add site when implementing (currently line 985).

### 3.3 Sibling sites — `FormComponentPropertyType.java`

Apply the same guard at each sibling cast. The behaviour on a bad value must match the method's
existing "form not resolvable" behaviour so nothing downstream NPEs:

- **Line 157, `toRhinoValue`** — it already does `if (form == null) return null;`. Add: after
  resolving `form`, guard the value; if `asFormComponentValue(webComponentValue, …)` returns
  `null`, `return null;` (same as the form-null path).
- **Line 198, `toTemplateJSONValue`** — inside `if (form != null) { … }`. Guard
  `formElementValue`; if `null`, skip the write (do not enter the block / `return writer;`), as
  if the form were not resolvable.
- **Lines 248 & 252, `toSabloComponentValue`** — inside `if (form != null) { … }`. Guard
  `formElementValue` once near the top of that block; on rejection **return `null`**, not
  `formElementValue` (the raw, still-unvalidated bad value). `null` is this type's own "no
  value" convention (`defaultValue()` already returns `null`), every caller of
  `toSabloComponentValue`/`convertFormElementToSabloComponentValue` already treats a `null`
  result as "nothing to set" (`ComponentFactory.fillProperty`'s `if (propertyValue != null)`,
  and `FoundsetLinkedPropertyType`'s `toJSON`/`changesToJSON`/`toRhinoValue`, which all
  explicitly null-check the wrapped sablo value before use — `containedForm` on the LFC is
  `forFoundset`-linked and so is wrapped by `FoundsetLinkedTypeSabloValue`/
  `FoundsetLinkedPropertyType` at runtime). Returning the raw value here instead of `null` would
  make `toRhinoValue` (below) re-run the same guard and re-log on every later scripting access
  to the property — see "Single log per rejected value" below.
- **Line 272, `getFormComponentRhinoConverter`** — has `property` but no form/element in scope.
  Guard `currentValue`; on rejection, construct `FormComponentValue` with the resolved (possibly
  `null`) value anyway: its constructor already does
  `this.currentValue = currentValue == null ? new JSONObject() : currentValue;`, so passing the
  guard's `null` result is safe as-is — confirmed by reading the constructor; no change needed
  at this site beyond adding the guard call for its log line.

Where a site already resolves `form` via `getForm(...)`, keep that call; the new guard is an
*additional* shape check on the raw value, not a replacement for the form lookup.

**Pass real form/element context wherever it is actually available, not just at the primary
site.** The first implementation pass left `toRhinoValue`, `toTemplateJSONValue` and
`toSabloComponentValue` passing `null, null` for form/element even though each one has access
to an `INGFormElement`/`FormElement` it can resolve both names from:

- `toSabloComponentValue(..., INGFormElement formElement, ...)` — pass
  `formElement.getForm() != null ? formElement.getForm().getName() : null` for the form name
  (it already passed `formElement.getName()` for the element).
- `toTemplateJSONValue(..., FormElementContext formElementContext)` — resolve
  `formElementContext.getFormElement()`, then that element's name and `.getForm().getName()`.
- `toRhinoValue(..., IWebObjectContext componentOrService, ...)` — in the `else` branch that
  calls `getForm`/`asFormComponentValue` directly (the fallback for a webComponentValue that
  isn't already a `FormComponentSabloValue` and isn't `null`), resolve the owning
  `WebFormComponent`'s `.getFormElement()` for both names.

Only `getFormComponentRhinoConverter` keeps `null, null` — it genuinely has nothing better
than `property` in scope. This was found and fixed after manual testing showed log lines
reading `on element 'fc1' of form 'null'` instead of naming the real form.

### 3.3a Single log per rejected value (`toSabloComponentValue` / `toRhinoValue`)

`toSabloComponentValue` and `toRhinoValue` are not independent consumers of the same raw
value — they are two stages of the *same* property's lifecycle: `toSabloComponentValue` runs
once when the component is built from the persisted `FormElement` value, producing the sablo
(runtime) value; `toRhinoValue` runs on every later scripting (Rhino) read of that property,
and its `else` branch (the fallback for "not already a `FormComponentSabloValue`") re-derives
the form/guarded value from scratch via `getForm`/`asFormComponentValue`.

If `toSabloComponentValue` returned the raw unvalidated value on rejection (as an earlier draft
of this fix did), every later scripting read would re-enter `toRhinoValue`'s `else` branch,
re-run `getForm` + `asFormComponentValue` on the identical already-rejected value, and log the
same "got X, expected Y" message again — for every read, not once per form load. That breaks
the "logged essentially once" acceptance criterion for this specific pair of call sites (it
still holds for the primary `FormElementHelper` site, which is cached).

**Fix:** `toSabloComponentValue` returns `null` (not `formElementValue`) when the guard rejects
the value. `toRhinoValue` gets an added branch, checked before its existing `else`:

```java
if (webComponentValue instanceof FormComponentSabloValue)
{
    cache = ((FormComponentSabloValue)webComponentValue).getCache();
}
else if (webComponentValue == null)
{
    // SVY-21667: toSabloComponentValue() already rejected and logged this property (it
    // returns null, never the raw value, on a non-JSONObject form-component value); do not
    // re-validate and re-log the same already-rejected value on every scripting access.
    return null;
}
else
{
    Form form = getForm(webComponentValue, fs);
    if (form == null) return null;
    JSONObject fcValue = asFormComponentValue(webComponentValue, null, null, pd.getName());
    if (fcValue == null) return null;
    ...
}
```

This makes the log fire once, from `toSabloComponentValue`, when the component is first built;
every subsequent scripting read of the rejected property short-circuits on `null` without
re-logging.

### 3.3b Sixth site — `FormWrapper.checkFormComponents` (FreeMarker template data)

`FormWrapper` builds the list of base components (and the form-component template cache) that
FreeMarker's `form_recordview_js.ftl` iterates when generating a form's JS. Its
`checkFormComponents(List<IFormElement> components, FormElement formComponentFormElement,
Set<String> recursiveCheck)` has the identical unguarded-cast shape as the primary
`FormElementHelper` site, including the same `recursiveCheck` add/remove bookkeeping:

```java
Object propertyValue = formComponentFormElement.getPropertyValue(pd.getName());
Form frm = FormComponentPropertyType.INSTANCE.getForm(propertyValue, context.getSolution());
if (frm == null) continue;
if (!recursiveCheck.add(frm.getName()))
{
    Debug.error("recursive reference found between (List)FormComponents: " + recursiveCheck);
    continue;
}
// SVY-21667: guard the cast; getForm(...) tolerates a bare String/UUID that this cast does not.
// Use FormWrapper's own owning "form" field for the message, not
// formComponentFormElement.getForm() - the latter resolves to null/something else at this stage
// of template generation, while this.form is reliably the form being rendered.
JSONObject fcValue = FormComponentPropertyType.asFormComponentValue(propertyValue, form.getName(), formComponentFormElement.getName(),
    pd.getName());
if (fcValue == null)
{
    recursiveCheck.remove(frm.getName()); // undo the add() done a few lines above
    continue;
}
FormComponentCache cache = FormElementHelper.INSTANCE.getFormComponentCache(formComponentFormElement, pd, fcValue, frm,
    context.getSolution());
```

Two things specific to this site, confirmed during implementation:

- **Use `FormWrapper`'s own `form` field for the form name in the message, not
  `formComponentFormElement.getForm()`.** The latter returned `null` here during template
  generation (confirmed by a `form 'null'` log line in manual testing); `FormWrapper.form` is
  the field already set in its constructor and reliably names the form actually being
  rendered.
- **`recursiveCheck.remove(frm.getName())` on rejection is required**, exactly as at the
  primary site (§3.2): `recursiveCheck.add(frm.getName())` has already run by the time the
  cast guard rejects the value, so without the matching `remove`, a later legitimate reference
  to the same form name elsewhere in the same traversal would be wrongly flagged as
  "recursive reference found" and skipped, even though there is no actual recursion.

### 3.3c Three further unguarded sites found, left unfixed by explicit decision (2026-11-02)

After fixing the sixth site, a systematic `grep` for `.getForm(` paired with `(JSONObject)`
casts was run across the whole `servoy_ngclient/src` tree specifically to answer the open
question "could there be a seventh site" (§7, previously unresolved). It found **three more**
sites with the exact same defect shape — a lenient `getForm(...)` lookup, an `if (frm == null)
continue;` guard that the cast does not respect, then an unguarded `(JSONObject)` cast on the
same raw value:

| File | Context | Current (unguarded) code |
|------|---------|---------------------------|
| `ChildrenJSONGenerator.java:229-241` | Building the children-JSON payload sent to the browser for a component's form-component children | `Form frm = FormComponentPropertyType.INSTANCE.getForm(propertyValue, context.getSolution()); if (frm == null) continue; ... FormComponentCache fccc = FormElementHelper.INSTANCE.getFormComponentCache(fe, pd, (JSONObject)propertyValue, frm, context.getSolution());` |
| `FormComponentUtils.java:89-93` | `addFormComponentComponentChildrenInternal` — walking form-component children for things like security/visibility checks | `Form formComponent = FormComponentPropertyType.INSTANCE.getForm(rawPropertyValueOfFEsFCProperty, fs); if (formComponent == null) continue; FormComponentCache cache = FormElementHelper.INSTANCE.getFormComponentCache(elementThatUsesAFormComponentInIt, pd, (JSONObject)rawPropertyValueOfFEsFCProperty, formComponent, fs);` |
| `FormComponentSabloValue.java:126-131` | `getComponents()` — building the live sablo child components of a foundset-linked form component (the runtime path for list form components) | `Form frm = FormComponentPropertyType.INSTANCE.getForm(propertyValue, dal.getApplication().getFlattenedSolution()); if (frm == null) continue; FormComponentCache innerCache = FormElementHelper.INSTANCE.getFormComponentCache(element, pd, (JSONObject)propertyValue, frm, dal.getApplication().getFlattenedSolution());` |

A fourth near-miss, `ChildrenJSONGenerator.java:511-517`, has the same `getForm`/`if (frm ==
null) continue;` pattern but does **not** go on to cast the value afterwards — it only reads
`frm.isResponsiveLayout()` — so it is not actually vulnerable to this `ClassCastException` and
needs no fix.

**These three are deliberately left unfixed.** The user was asked explicitly (via a direct
question) whether to fix all three now or document-only-and-decide-later, and chose
**"Document only, fix later."** No code changes were made to these three files in this
session. Each would need the same treatment as the six already-fixed sites: guard the raw
value with `FormComponentPropertyType.asFormComponentValue(value, formName, elementName,
propertyName)` before the `(JSONObject)` cast, using whatever form/element context each site
has in scope (all three have at least an `IFormElement`/`FormElement` in scope that can supply
both names, similar to `FormWrapper`'s fix), and `continue` instead of proceeding when the
guard returns `null`. None of these three has a `recursionCheck`/`recursiveCheck`-style set to
rebalance (that bookkeeping is specific to the primary `FormElementHelper` and `FormWrapper`
sites), so the fix at these three would be simpler: no matching `remove()` call is needed on
rejection.

### 3.4 What must not change

- `FormComponentPropertyType.getForm(...)` stays tolerant of String/UUID/JSForm — other code
  depends on name/UUID resolution. The fix is at the cast sites, not in `getForm`.
- No coercion of the string to `{ svy_form: <uuid> }`.
- No new validation marker in the IDE Problems view (triage approach 3 — out of scope here).
- No new public API beyond the single `asFormComponentValue` helper.

## 4. Implementation plan

All edits are in the **`servoy_ngclient`** checkout at
`D:\Eclipse\202603Workspace\servoy-client\servoy_ngclient`.

1. **Add the helper** `public static JSONObject asFormComponentValue(Object value, String
   formName, String elementName, String propertyName)` to
   `…\property\types\FormComponentPropertyType.java` (§3.1). Build the expected-shape text from
   the existing `SVY_FORM` constant; use the already-imported `Debug`.
2. **Primary site** — in `…\FormElementHelper.java`, `addFormComponentProperties`, replace the
   `(JSONObject)frmValue` cast at ~line 992–993 with the guarded version in §3.2, including the
   `recursionCheck.remove(frm.getName())` before `continue`. Re-confirm the current line numbers
   and the `recursionCheck` add site (currently ~985) before editing — the ticket's line numbers
   came from a released build.
3. **Sibling site — `toRhinoValue`** (~line 157): guard `webComponentValue`; `return null;` on
   a bad value, and in the `else` branch pass the owning `WebFormComponent`'s form/element names
   (not `null, null`) — see the "pass real context" note in §3.3.
4. **Sibling site — `toTemplateJSONValue`** (~line 198): guard `formElementValue`; skip the
   write / `return writer;` on a bad value; resolve form/element names from
   `formElementContext.getFormElement()` (§3.3).
5. **Sibling site — `toSabloComponentValue`** (~lines 248 & 252): guard `formElementValue` once
   near the top of the `if (form != null)` block; on rejection **return `null`**, not
   `formElementValue` (§3.3a); pass `formElement.getForm().getName()` for the form name.
6. **Sibling site — `getFormComponentRhinoConverter`** (~line 272): read `FormComponentValue`'s
   constructor first, then guard `currentValue` — the constructor already null-coalesces to an
   empty `JSONObject`, so no change beyond adding the guard call itself (§3.3). `null, null`
   stays for form/element here — no better context is available.
7. **Sixth site — `FormWrapper.checkFormComponents`** (found and fixed during testing, not in
   the original five-site plan): guard `propertyValue` with the same helper, name the form from
   `FormWrapper`'s own `form` field (not `formComponentFormElement.getForm()`, which is `null`
   at this stage), and rebalance `recursiveCheck.remove(frm.getName())` on rejection, exactly as
   at the primary site (§3.3b).
8. **Verify, per AGENTS.md:** after each edit run `tools["eclipse-ide"].getCompilationErrors`
   on the three changed files; run `tools["eclipse-coder"].organizeImports` (no new imports are
   expected — `Debug` and `JSONObject` are already imported everywhere they're used); fix the
   two highest-severity SpotBugs levels in the changed code.
9. **Confirm line numbers by reading, not trusting this spec:** use the `read` tool on the
   absolute paths above and `grep` for `(JSONObject)` within `FormComponentPropertyType.java`
   and `FormWrapper.java` before editing, since the current master has shifted from the ticket's
   2026.9.0.4202 lines — and because the sixth site is not in any released-version line-number
   reference at all.
10. **Run a real integration test, not just the unit test for the helper.** The unit test for
    `asFormComponentValue` alone (`FormComponentPropertyTypeAsFormComponentValueTest`) does not
    exercise `showFormInCurrentContainer`/template generation and therefore cannot catch the
    sixth site. Build a real form with a `servoycore-formcomponent`/LFC whose `containedForm` is
    a bare resolvable string and open it in a real (test) client
    (`FormComponentPropertyTypeSabloRhinoGuardTest` does this) — that is what actually surfaced
    both the sixth site and the `form 'null'` context-threading gaps.

## 5. Acceptance criteria

- [ ] Opening a form whose List Form Component `containedForm` is a bare form-name string (that
      resolves to a real form) no longer throws `ClassCastException: String cannot be cast to
      JSONObject`; the form loads with the rest of its tab-sequence processing intact.
- [ ] The same holds when `containedForm` is a bare form-UUID string.
- [ ] A single `Debug.error` is logged (in both the NG client log and the IDE) naming: the
      property (e.g. `containedForm`), the element, the owning form, the actual value received,
      and the expected `{ "svy_form": "<form UUID>" }` shape, and referencing `SVY-21667`.
- [ ] The error is **not** logged repeatedly on every render/paint of the form (it fires on the
      cached tab-sequence computation path, essentially once per form load).
- [x] The error is **not** logged repeatedly on every scripting (Rhino) read of the rejected
      property: `toSabloComponentValue` returns `null` on rejection (not the raw value), and
      `toRhinoValue` short-circuits on a `null` `webComponentValue` without re-running the guard
      (§3.3a). Only the `toSabloComponentValue` call (component build time) logs. — Verified by
      `FormComponentPropertyTypeSabloRhinoGuardTest`.
- [x] The malformed property is **skipped** (reject-and-report); the string is **not** coerced
      into the object form, and no other tab-sequence entries are lost because of it.
- [x] All six reachable paths (`toRhinoValue`, `toTemplateJSONValue`, `toSabloComponentValue`,
      `getFormComponentRhinoConverter`, the primary `FormElementHelper` site, and the sixth site
      `FormWrapper.checkFormComponents`) handle the same bad value with a clear logged message
      and a safe return instead of a context-free `ClassCastException` / FreeMarker
      `_TemplateModelException`.
- [x] Every guard that has real form/element context in scope uses it in its log message — no
      stray `on element 'fc1' of form 'null'` lines. Confirmed via manual testing in a running
      client (console output reviewed directly) after fixing `toSabloComponentValue`,
      `toTemplateJSONValue`, `toRhinoValue` and `FormWrapper.checkFormComponents` to resolve and
      pass the real owning-form name instead of `null`. Only `getFormComponentRhinoConverter`
      still logs `null` for form/element, because it genuinely has no better context available.
- [ ] A well-formed `{ "svy_form": "<form UUID>" }` value behaves exactly as before (no
      behavioural change, no new log noise). — Not independently re-verified after the sixth-site
      and context-threading fixes; the original five-site behavior for a well-formed value was
      unchanged by those edits (they only touch the rejection branch), but this criterion has not
      been re-run with a fresh well-formed-value test since the last round of fixes.
- [x] `tools["eclipse-ide"].getCompilationErrors` reports no errors in the three changed files
      (`FormElementHelper.java`, `FormComponentPropertyType.java`, `FormWrapper.java`); SpotBugs
      not separately re-run after the sixth-site fix — see Open questions.
- [ ] **Known, accepted gap (not blocking):** for a single malformed property, multiple
      independent call sites (`toSabloComponentValue`, `FormWrapper.checkFormComponents`, and
      whichever sibling site a given operation also touches) each log their own line when they
      independently encounter the same already-rejected value during one operation (e.g. one
      `showFormInCurrentContainer` call can produce 2-3 log lines for one bad property, observed
      in manual testing). Every line is individually accurate (correct form/element context),
      just repeated across sites that don't call each other. Deduplicating across these
      structurally independent sites would need a shared cache (keyed by
      form/element/property) with its own lifetime/scope design — out of scope for this fix,
      tracked as a separate low-priority follow-up (drafted, not filed: "Form-component
      cast-guard logs once per independent consumer, not once per malformed property").

## 6. Out of scope

- Coercing a bare string `containedForm` into `{ svy_form: <uuid> }` so the LFC renders fully
  (triage approach 2) — may be revisited if product decides the shorthand should be supported.
- Validating the `.frm` at load time and raising an IDE Problems-view marker (triage approach 3,
  the ticket's fallback option 2) — a larger persist/validation-layer change.
- SVY-21670 (getFormLayout reports a failed form as a timeout) and SVY-21668 (agent skill
  describes the wrong shape) — separate tickets, not blockers.
- Any change to `getForm(...)`'s String/UUID tolerance.

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| At `getFormComponentRhinoConverter` (line 272), what is the correct safe return when `currentValue` is not a JSONObject? | implementer | **resolved** — `FormComponentValue`'s constructor already null-coalesces `currentValue == null ? new JSONObject() : currentValue`, so passing the guard's `null` result as-is is safe; no change needed at this site beyond adding the guard call. |
| Should the log level be `Debug.error` (per the lead dev's "report a nicer error") for all sites, or `Debug.warn` for the read-only/template paths? | implementer / Johan Compagner | **resolved as shipped** — `Debug.error` used uniformly at all six sites; not revisited with Johan Compagner. |
| Is there an existing malformed sample `.frm` to reproduce against, or should one be hand-authored for the test? | implementer | **resolved** — hand-authored: `FormComponentPropertyTypeSabloRhinoGuardTest` builds a real solution/form/component with a bare form-name string at test setup time (not from an attached sample `.frm`). |
| Were the run-011 ticket attachments (an AI-agent-authored `.frm` with the same bare-form-name-string defect) analyzed to confirm the chosen reject-and-report design (vs. coercion) is right? | n/a | **resolved** — yes; see below. |
| **Was the sixth call site (`FormWrapper.checkFormComponents`) the only one missed, or could there be a seventh/eighth?** | implementer | **resolved — no, there are at least three more, found but deliberately left unfixed.** A systematic `grep` for `.getForm(` combined with `(JSONObject)` casts across `servoy_ngclient/src` was run after the sixth site was fixed. It found three further sites with the identical unguarded shape (lenient `getForm(...)` lookup, `if (frm == null) continue;`, then an unguarded `(JSONObject)` cast on the same raw value), all reachable with the same malformed `containedForm` value. **Per the user's explicit decision, these three are documented here but not fixed in this pass** — see the table below. |
| **The three newly found, unfixed sites — are they fixed?** | user (deferred) | **No — found 2026-11-02, left unfixed by explicit user decision ("Document only, fix later").** See the dedicated subsection below for the exact locations and what each would need. |
| **Should the duplicate-logging-across-independent-sites gap be fixed now or deferred?** | user | **deferred, by user decision.** A follow-up ticket was drafted ("Form-component cast-guard logs once per independent consumer, not once per malformed property") but the user judged it low priority and has not filed it yet. |

### Analysis of the ticket's run-011 attachments (added after initial implementation)

The ticket's attachments (`run-011-inputs-and-built.zip`, `run-011-built-artifacts.zip`,
`run-011-full-agent-trace.zip`, `run-011-agent-traces-full.zip`) were unzipped and reviewed
after the initial five-site fix was implemented, in response to a question challenging whether
reject-and-report (vs. coercing the bare string into the canonical shape) was really the right
design.

**What the attachments show:** an AI agent (Orchestrator → Developer → Reviewer loop) built
three forms (`lf_list`, `lf_cards`, `lf_related`) each containing a
`servoycore-listformcomponent` whose `containedForm` was written as a bare form-name string
(`"fc_customer_row"`, `"fc_customer_card"`, `"fc_order_row"`) — exactly the defect this ticket
is about, confirmed in `built/forms/lf_list.frm` etc. The agent's own Reviewer ran multiple
passes (catching and fixing a real CRITICAL XSS issue, a paging bug, an avatar bug) and never
caught this defect — the headless renderer used for review could not render LFC-container
forms at all, so the defect was invisible to automated review and only surfaced live, as the
raw `ClassCastException`.

**The decisive evidence:** `run-011-inputs-and-built/fixed-by-hand/lf_list.frm` (and others)
show a human fixing the same form by **correcting the string to the canonical
`{ "svy_form": "<uuid>" }` shape**, not by leaving the shorthand string in place. A from-scratch
reference form (`test_lfc_reference.frm`) in the same folder was also built using the canonical
object shape from the start. This confirms the canonical shape is load-bearing elsewhere in the
platform (property editors, designer, serialization) and that coercion (accepting the shorthand
permanently) would diverge from what the rest of the platform expects — reinforcing, not
undermining, the reject-and-report design already in this spec. The real gap the attachments
expose is **upstream**, in whatever produces/documents the shape for AI agents authoring
`.frm`s (tracked separately as SVY-21668, out of scope here).
