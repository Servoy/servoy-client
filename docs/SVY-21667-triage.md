# Triage Report — SVY-21667

**Verdict:** PROCEED

## Reported problem

When a form's `.frm` file contains a List Form Component (`servoycore-listformcomponent`)
whose `containedForm` property is a plain string (a form name such as `"fc_customer_row"`
or a form UUID such as `"DD0AECC8-..."`) instead of the expected object
`{ "svy_form": "<form UUID>" }`, opening the form throws:

```
java.lang.ClassCastException: class java.lang.String cannot be cast to class org.json.JSONObject
  at com.servoy.j2db.server.ngclient.FormElementHelper.addFormComponentProperties(FormElementHelper.java:1024)
  at ...FormElementHelper.getControlledTabSeqReplacementFor(FormElementHelper.java:935)
  at ...NGTabSeqPropertyType.toFormElementValue(NGTabSeqPropertyType.java:46)
  ... FormElement.initTemplateProperties ... WebFormUI.init
```

The exception names no form, no element and no property, so neither a developer nor an AI
agent can tell what is wrong. It appears in the NG client log **and** in the IDE. The form
still partly renders, so the exception reads as noise. The reporter wants a clear,
actionable error (form, element, property, expected shape) — not a different feature.

## Root-cause assessment

The symptom is a real, reproducible NPE-class defect in the NG-client code
(`servoy_ngclient`, located at `D:\Eclipse\202603Workspace\servoy-client\servoy_ngclient`),
not user error or a misunderstanding. The malformed `.frm` is the trigger, but the raw
`ClassCastException` with no context is the project's own code failing to guard a cast.

The line numbers in the ticket (from Servoy 2026.9.0.4202) have shifted slightly in the
current `master`, but the faulty code is unchanged in shape. The exact failure is in
`FormElementHelper.addFormComponentProperties(...)`:

```java
// FormElementHelper.java
981:  Object frmValue = formComponentEl.getPropertyValue(property.getName());
982:  Form frm = FormComponentPropertyType.INSTANCE.getForm(frmValue, flattenedSolution);
983:  if (frm == null) continue;
...
992:  List<IFormElement> formComponentPersistsForFCProp =
993:      generateFormComponentPersists(formComponentEl, property, (JSONObject)frmValue, frm, flattenedSolution);
```

The decisive detail is that `getForm(frmValue, fs)`
(`FormComponentPropertyType.java:214`) is **tolerant of a String**:

```java
public Form getForm(Object formElementValue, FlattenedSolution fs) {
    Object formId = formElementValue;
    if (formId instanceof JSONObject) formId = ((JSONObject)formId).optString(SVY_FORM);
    Form form = null;
    if (formId instanceof String || formId instanceof UUID) {
        form = fs.getForm(formId.toString());           // resolves "fc_customer_row" or the UUID
        if (form == null) form = (Form)fs.searchPersist(formId.toString());
    }
    ...
    return form;
}
```

So when `containedForm` is a bare string that happens to name/UUID a real form, `getForm`
**succeeds** (returns a non-null `Form`), the `if (frm == null) continue;` guard at line 983
is passed, and then line 992/993 unconditionally does `(JSONObject)frmValue` on the very
same value that is still a `String`. That cast is what throws. The guard and the cast
disagree about the shape of `frmValue`: the guard accepts a String, the cast does not.

This is the single root cause. The same latent pattern — `getForm(...)` accepts a String
while the follow-up code casts the value to `JSONObject` — recurs at several sibling call
sites, all of which would throw the same unhelpful `ClassCastException` for the same bad
input:

- `FormComponentPropertyType.java:157` — `(JSONObject)webComponentValue` in `getFormComponentElements`
- `FormComponentPropertyType.java:198` — `(JSONObject)formElementValue`
- `FormComponentPropertyType.java:248` / `:252` — `(JSONObject)formElementValue` in `toSabloComponentValue`
- `FormComponentPropertyType.java:272` — `(JSONObject)currentValue`

None of these carry any form/element/property context in the failure.

The `addFormComponentProperties` loop does already have the identifying information needed
for a good message in scope: `formComponentEl` (the element — `formComponentEl.getName()`),
`property.getName()` (the property, e.g. `containedForm`), and the containing form via
`formComponentEl.getForm()`. Nothing new needs to be plumbed through to produce the
actionable error the ticket asks for.

## Ticket premise check

The ticket's "Expected / suggested" section proposes exactly the right thing and does not
over-reach:

1. Validate the `containedForm` value when the element is loaded; if it is a string,
   either accept it (resolve name/UUID) or log one clear error naming form, element,
   property and the expected `{svy_form: <UUID>}` shape.
2. If strings are not meant to be supported, surface it as a form-load problem rather than
   a tab-sequence stack trace.

The lead developer's comment (Johan Compagner) agrees and sets the scope: *"we should be
able to catch this error and then report a nicer error in the log (and back to the ai) that
we got X and we expect a json object with structure Y."* That is a guard-and-report fix,
not a new public API. The premise holds up. The only decision the implementer should settle
(and it is already implied by the comment) is **reject-with-clear-error vs. silently
accept the string** — see Recommendation.

## Approaches considered

1. **Guard the cast and report a clear, actionable error (recommended).**
   At the point where `frmValue` is used as a `JSONObject`, check `instanceof JSONObject`
   first. If it is not, emit one clear `Debug.error` naming the form
   (`formComponentEl.getForm().getName()`), the element (`formComponentEl.getName()`), the
   property (`property.getName()`), the actual value, and the expected shape
   `{ "svy_form": "<form UUID>" }`, then `continue` / skip that property instead of throwing.
   Apply the same guard at the sibling cast sites listed above (or centralize a small
   `asFormComponentValue(...)` helper). — *Pros:* directly fixes the reported symptom, is
   low-risk and local, matches the dev's stated intent, keeps the form rendering. *Cons:*
   touches a handful of call sites; must be careful to log once, not per render, to avoid
   log spam.

2. **Make the string shape actually supported (coerce `"name"`/`"uuid"` →
   `{ svy_form: <uuid> }`) and proceed.** Since `getForm` already resolves the string, wrap
   it into a `JSONObject` so the LFC renders fully. — *Pros:* most forgiving; the form works
   even with the hand-written shorthand. *Cons:* blesses a shape the designer never writes;
   risks masking genuinely malformed `.frm` files and diverging from the canonical format;
   more behavioural change and test surface than the ticket asks for; the dev comment leans
   toward *reporting*, not silently accepting.

3. **Validate at `.frm` load time and raise an IDE Problems-view marker** (the ticket's
   fallback option 2). — *Pros:* catches the bad shape earlier, in the IDE, before the NG
   client. *Cons:* larger change spanning the persist/validation layer; does not by itself
   fix the runtime `ClassCastException` in the deployed NG client, so it would likely be
   *in addition to*, not instead of, approach 1. Out of proportion for this ticket on its
   own.

4. **No code change.** — *Pros:* none. *Cons:* the defect is real and reproducible with a
   plain `.frm` edit; the raw `ClassCastException` from tab-sequence code with no form or
   element name is exactly the actionable-error gap the ticket and the lead developer want
   closed. Rejected.

## Recommendation

**PROCEED with approach 1** — guard the `(JSONObject)` cast on the form-component value and
replace the raw `ClassCastException` with a single clear, actionable `Debug.error` that
names the form, element, property, the actual value received, and the expected
`{ "svy_form": "<form UUID>" }` shape, then skip that property instead of aborting
tab-sequence processing. Fix the primary site
(`FormElementHelper.addFormComponentProperties`, around line 992) and the sibling unchecked
casts in `FormComponentPropertyType` (lines 157, 198, 248, 252, 272) so the same bad input
cannot resurface the context-free exception through another path; a small shared helper
(`JSONObject asFormComponentValue(Object value, context...)`) is the clean way to do this
once.

This matches the lead developer's comment ("catch this error and then report a nicer error
... that we got X and we expect a json object with structure Y") and the ticket's expected
behaviour, is local and low-risk, and keeps the rest of the form rendering.

Implementer's one judgement call: whether to **reject-and-report** (recommended — log the
clear error and skip) or additionally **coerce** the string to the object form so the LFC
also renders (approach 2). The dev comment favours reporting; coercion can be added later if
product decides the shorthand should be supported. Suggest the implementer go
reject-and-report unless told otherwise.

## Git history findings

- The faulty call site (`generateFormComponentPersists(..., (JSONObject)frmValue, frm, ...)`)
  was last reshaped by commit `168704add` (*SVY-19051: When formcomponent contains a $ in the
  name is start throwing warnings*, Andrei Costescu, 2025-09-15), merged via `95adbd84d`
  (2025-10-01). That commit renamed the local variable and reflowed the lines but did **not**
  introduce or remove the unchecked cast — the `(JSONObject)frmValue` cast predates it. So
  the fix does not revert an intentional recent decision; the guard was simply never added.
- `FormComponentPropertyType.getForm(...)` has long been intentionally tolerant of a
  `String`/`UUID` (resolve-by-name or -UUID), which is precisely why the `frm == null` guard
  fails to protect the downstream cast. This is a mismatch between a lenient resolver and a
  strict cast, not a regression from a version/dependency bump.
- No prior spec for SVY-21667 exists in `docs/`. Related tickets referenced by the reporter:
  SVY-21670 (getFormLayout reports a failed form as a timeout), SVY-21668 (agent skill
  describes the wrong shape) — separate concerns, not blockers for this fix.

## Postscript — findings from implementation and manual testing (added after the PROCEED gate)

This section records what the implementation and testing phases found beyond what this triage
identified. It does not change the verdict or recommendation above, which stand as the
record of what was known and decided at the PROCEED gate. Full detail is in
`docs/SVY-21667-form-component-cast-guard.spec.md` §2.3, §3.3b and §7.

- **A sixth unguarded cast site existed beyond the five listed above**, in
  `FormWrapper.checkFormComponents` (`FormWrapper.java`) — the FreeMarker template-data builder
  for a form's generated JS, not any of the five render/scripting paths this triage identified
  in `FormComponentPropertyType`/`FormElementHelper`. It has the identical shape (a tolerant
  `getForm(...)` lookup followed by an unguarded `(JSONObject)` cast) and was found only once a
  dedicated integration test opened a real form with the malformed value in a running test
  client, triggering template generation. It has since been fixed the same way as the other
  five sites. **This triage's "recurs at several sibling call sites" list (the four bullet
  points above) was not exhaustive** — a systematic search across `servoy_ngclient/src`
  afterwards found three more sites in that same repo (`ChildrenJSONGenerator`,
  `FormComponentUtils`, `FormComponentSabloValue`), deliberately left unfixed by explicit user
  decision; see spec §3.3c.
- **A tenth site exists in a different repository entirely — `servoy-eclipse`, not
  `servoy-client`.** This triage's root-cause assessment and git-history scope was written
  against the `servoy_ngclient` checkout only (per its root-cause-assessment paragraph above:
  "a real, reproducible NPE-class defect in the NG-client code (`servoy_ngclient`...)"). It did
  not consider, and could not have been expected to consider without a cross-repository search,
  that the Servoy Developer IDE's own project builder
  (`com.servoy.eclipse.model`'s `ServoyFormBuilder.addFormMarkers`) has the identical defect.
  That builder runs continuously — on every save and every incremental/full build — so once a
  workspace contains a form with the malformed `containedForm` value (such as the one
  hand-authored for manual testing against this very ticket), it throws the same
  `ClassCastException` repeatedly into the Eclipse `.log`, far more disruptively than any of
  the nine `servoy-client`-side sites. It was found 2026-11-03 by that live symptom, fixed the
  same way (reusing the already-committed `asFormComponentValue` helper, which
  `com.servoy.eclipse.model` already resolves from `servoy_ngclient`'s workspace source), and
  requires its **own, separate commit in the `servoy-eclipse` repository** — see spec §3.3d.
  No systematic search of `servoy-eclipse` (or any repository beyond `servoy_ngclient`) for
  this same pattern has been performed; this one occurrence was found by symptom, not by a
  deliberate search as was done for the ninth-site search above.
- **The run-011 ticket attachments were analyzed** (not available/reviewed at the time this
  triage was written) and confirm, rather than challenge, this triage's reject-and-report
  recommendation: an AI-agent-authored `.frm` reproduced the exact defect (bare form-name
  string), and the attachments' own human-fixed version of that `.frm` corrects the string to
  the canonical `{ "svy_form": "<uuid>" }` shape rather than leaving the shorthand in place —
  evidence against approach 2 (coercion), consistent with this triage's recommendation of
  approach 1.
- **A log-volume gap was found and accepted, not fixed:** because the (now six) guarded call
  sites are independent consumers of the same raw value rather than a single choke point, one
  malformed property can produce more than one log line for a single operation (e.g. opening a
  form), each from a different site that independently rediscovers the same already-rejected
  value. This triage's approach-1 risk note ("must be careful to log once, not per render, to
  avoid log spam") was addressed only for the `toSabloComponentValue`/`toRhinoValue` pair (spec
  §3.3a); it was not achieved across all six independent sites. A low-priority follow-up for
  this was drafted but, per the user, not filed as a ticket.
