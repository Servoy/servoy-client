# Triage Report — SVY-21511

**Verdict:** PROCEED

## Reported problem

A WAR build on 2026.09 (nightly_release) now **fails** with two relation errors that
were not raised in 2026.06 and, per the reporter, are "not in my IDE":

```
-Relation "globals_to_build$buildingorqueued$application" has a relation item with mismatched keys:
   Key type from scopes.jobs.applicationJobs (Array<String>) does not match with type from job_uuid (MEDIA).
-Relation "mem_assign_users_to_users" has a relation item with mismatched keys:
   Key type from user_uuid (TEXT) does not match with type from user_uuid (MEDIA).
```

The reporter concedes the second (`TEXT` scope/column vs a native-UUID `MEDIA` column)
is "sort of valid" data-wise, and notes that changing the scope var from
`Array<String>` to `Array<UUID>` did not fix the first one. The reporter's three
questions (why only at export, why a regression from 26.06, is DLTK responsible) are
answered below.

The **symptom** is: a WAR build that succeeded on 26.06 is now blocked on 26.09. The
**proposed solution** in the ticket (changing the scope var type to `Array<UUID>`) is
a workaround attempt, not a fix, and it does not work.

## Root-cause assessment

This is **not** a DLTK issue and **not** a WAR-exporter issue. It is a deliberate
**severity change** in this repo (`servoy-eclipse`) to the relation key-type builder
marker, made under **SVY-21356** and shipped in 2026.09.

### The mechanism (all in this repo + `servoy-client`)

1. The relation key-type check is `Relation.checkKeyTypes(IDataProviderHandler)`
   (`servoy-client/servoy_shared/.../persistence/Relation.java:802`). It returns a
   non-null "…does not match with type from…" message string when a relation item's
   primary/foreign types are incompatible. This is the exact text in the ticket
   (`servoy.relation.error.typeDoesntMatch`, emitted at `Relation.java:880-891`).

2. The **same predicate** is invoked from two places:
   - The **editor** — `RelationEditor.java:895` (`createAndcheck`): a non-null result
     blocks saving the relation with an error dialog.
   - The **builder** — `ServoyRelationBuilder.checkRelation(...)` at line 406, which
     on a non-null result raises the `RELATION_ITEM_TYPE_PROBLEM` marker
     (`ServoyRelationBuilder.java:406-413`, `MarkerMessages.RelationItemTypeProblem`,
     text `Relation "{0}" has a relation item with mismatched keys: {1}.` at
     `MarkerMessages.java:661`).

3. The marker's **severity** is declared in
   `ServoyBuilder.java:537` as
   `RELATION_ITEM_TYPE_PROBLEM = new Pair<>("relationItemTypeProblem", ProblemSeverity.ERROR)`.
   **On 2026.06 this was `ProblemSeverity.WARNING`.**

4. The WAR export gate is severity-sensitive: `ExportWarWizard.java:155` blocks with
   "Cannot export solution with errors" only when
   `BuilderUtils.getMarkers(activeProject) == HAS_ERROR_MARKERS`
   (`BuilderUtils.java:71-73` returns `HAS_ERROR_MARKERS` only for
   `IMarker.SEVERITY_ERROR`). A `WARNING`-severity marker does **not** block export.

### Answering the reporter's three questions

**(1) Why only at WAR export and not in the IDE?**
It is *not* only at export — the marker is produced by the incremental `ServoyBuilder`
in the IDE too, so it is present in the Problems view of 26.09. What is
export-*specific* is the **blocking**: `ExportWarWizard`/the command-line WAR export
refuse to build when any ERROR-severity marker exists. The reporter almost certainly
has these as warnings-or-unnoticed entries in the IDE Problems view (easy to overlook
among many), whereas the export hard-stops on them. There is no separate export-time
validator; export just reads the markers the builder already produced and treats
ERRORs as fatal. (Two secondary reasons a developer may "not see it in the IDE": the
relation `.rel` files may not have been rebuilt since the upgrade, or Problems-view
filters hide the project/severity — but the code path is the same builder.)

**(2) Why is it a regression from 26.06?**
Because of commit **`64f62bba7f`** — "SVY-21356 Add builder error markers for invalid
artifacts that bypass UI wizard validation [ai]" (Diana Bunaciu, 2026-09-01),
contained in `2026.9_RC1` and `2026.9_RC2` and **not** in the 26.06 line. That commit
flips `RELATION_ITEM_TYPE_PROBLEM` from `ProblemSeverity.WARNING` → `ProblemSeverity.ERROR`
(and priority `PRIORITY_LOW` → `PRIORITY_NORMAL`) to make the builder's strictness
match the editor's save-blocking behavior. The detection logic itself did **not**
change between 26.06 and 26.09 — only the severity. Once it became ERROR, the WAR
export gate (`ExportWarWizard.java:155`) started blocking on it. The commit message
even predicted exactly this: *"raising RELATION_ITEM_TYPE_PROBLEM … to ERROR may
surface new build errors in existing projects that already contain these conditions
(previously a warning)."* This ticket is that predicted fallout.

**(3) Is DLTK responsible?**
No. `checkKeyTypes`, the marker, the severity constant, and the export gate are all
Servoy code in `servoy-eclipse` and `servoy-client`. There is no DLTK JavaScript
validation in this path (`ServoyRelationBuilder` has no DLTK reference). The scope
variable's `@type {Array<...>}` is read from the persisted `IScriptProvider.TYPE`
runtime property (set in `SolutionDeserializer.java:2033`), not evaluated by DLTK for
this check. A DLTK version bump is not the cause.

### Are the two flagged relations genuinely wrong?

- **`mem_assign_users_to_users`** (`user_uuid TEXT` = `user_uuid MEDIA/native-UUID`):
  a genuine type mismatch. `checkKeyTypes` correctly reports it; the reporter agrees
  it is "sort of valid". This one is arguably a real data-model issue to fix in the
  solution, independent of Servoy code.
- **`globals_to_build$…$application`** (`scopes.jobs.applicationJobs Array<String>` =
  `job_uuid MEDIA/native-UUID`): here `checkKeyTypes` (Relation.java:867-882) accepts
  `MEDIA` on the foreign side only when the primary array component type is
  `Array<String>` against a `TEXT` foreign column or `Array<Number>` against
  `NUMBER/INTEGER`. A native-UUID column maps to `MEDIA`, not `TEXT`, so both
  `Array<String>` **and** `Array<UUID>` fail — which is exactly why the reporter's
  `Array<UUID>` experiment did not help (`ArgumentType.valueOf("Array<UUID>")` is
  neither `ArrayString` nor `ArrayNumber`, so `ok=false`). This is a **real
  content-spec gap**: array-of-scope-values joined by `in`/`=` against a native-UUID
  (`MEDIA`) column is not recognized as valid even though it is a legitimate pattern
  (an array of uuid values matched against a uuid column).

So the mismatch detection is behaving as written; what changed is that a previously
non-blocking WARNING is now a hard, export-blocking ERROR.

## Ticket premise check

The ticket's implicit premise ("the developer/IDE doesn't show these, so the export is
wrong / something regressed in export or DLTK") does not hold. The export is faithfully
reporting builder ERRORs. The regression is a *known, intentional* severity escalation
(SVY-21356). The reporter's own workaround (`Array<String>` → `Array<UUID>`) cannot
work because `checkKeyTypes` does not accept any typed array against a `MEDIA` (native
UUID) column. So the fix is not "change the scope var type"; the decision is about the
severity escalation and/or extending `checkKeyTypes` to accept UUID-array-to-MEDIA.

## Approaches considered

1. **Revert the severity of `RELATION_ITEM_TYPE_PROBLEM` back to `WARNING`** (undo the
   SVY-21356 escalation for the relation marker; the new CSS-position-no-body marker
   from the same commit is unrelated and can stay). — Pros: restores 26.06 behavior,
   unblocks the build immediately, low-risk one-line change, matches the fact that
   this condition was tolerated for years as a warning. Cons: reopens the
   editor-vs-builder severity gap SVY-21356 deliberately closed; a genuinely broken
   relation (like `mem_assign_users_to_users`) would again only warn.

2. **Keep ERROR but extend `Relation.checkKeyTypes` to accept a typed UUID array
   (`Array<UUID>`, or a plain `Array`) against a native-UUID `MEDIA` foreign column**
   (in `servoy-client` Relation.java:867-882, add a UUID-array branch alongside the
   `ArrayString`/`ArrayNumber` cases; `ArgumentType` may need an `ArrayUUID`). —
   Pros: fixes the *actual* false positive for the `applicationJobs` relation while
   keeping strictness for truly wrong mismatches; makes the reporter's `Array<UUID>`
   intent valid. Cons: touches shared runtime `servoy-client`; needs care that the
   runtime join actually works for uuid-array-to-MEDIA; does not by itself fix the
   `TEXT`-vs-`MEDIA` relation (that one is a real data issue). Best combined with a
   decision on severity for the remaining genuine mismatches.

3. **Make the WAR export gate treat `RELATION_ITEM_TYPE_PROBLEM` (and similar
   newly-escalated markers) as non-blocking / warn-and-continue**, or add it to an
   ignorable set like the db-down markers (`ExportConfirmationPage` path,
   `ExportWarWizard.java:157`). — Pros: unblocks export without changing validation
   severity in the IDE. Cons: special-casing export vs IDE reintroduces exactly the
   parity gap SVY-21356 removed, in a more confusing place; least clean.

4. **No code change** (tell the reporter to fix the two relations in the solution). —
   Pros: the `TEXT`-vs-`MEDIA` relation is a real data-model problem worth fixing;
   zero Servoy-code risk. Cons: does not address the legitimate
   `Array<UUID>`-to-`MEDIA` case (approach 2), which is a genuine `checkKeyTypes` gap
   and cannot be worked around by the user at all; leaves a Blocker-priority build
   regression standing for a pattern that is arguably valid.

## Recommendation

**PROCEED.** Recommended: **approach 2 combined with a severity decision** — extend
`Relation.checkKeyTypes` so a typed UUID array (and/or plain `Array`) is accepted
against a native-UUID (`MEDIA`) foreign column for the `=`/`in`/`!` operators (fixing
the `globals_to_build$…$application` false positive that the reporter cannot work
around), and get release-owner sign-off on whether `RELATION_ITEM_TYPE_PROBLEM` should
remain ERROR (SVY-21356 intent) or be reverted to WARNING for the 26.09 line to avoid
blocking existing projects mid-release (approach 1).

Fastest unblock if release timing is tight: **approach 1** (revert just this marker's
severity to WARNING on the release line), then do approach 2 properly for a later
build. Either way, the `mem_assign_users_to_users` (`TEXT` vs native-UUID) relation is
a real data-model mismatch the reporter should correct in the solution regardless.

This is squarely in Servoy code (`servoy-eclipse` marker severity + `servoy-client`
`checkKeyTypes`); DLTK is not involved.

## Git history findings

- **`64f62bba7f`** — "SVY-21356 Add builder error markers for invalid artifacts that
  bypass UI wizard validation [ai]" (Diana Bunaciu, 2026-09-01). Flips
  `RELATION_ITEM_TYPE_PROBLEM` from `ProblemSeverity.WARNING` → `ERROR` and priority
  `PRIORITY_LOW` → `PRIORITY_NORMAL` in
  `com.servoy.eclipse.model/.../ServoyBuilder.java`. Contained in tags `2026.9_RC1`
  and `2026.9_RC2`; on the `release` branch. This is the regression cause. The commit
  message explicitly warns it "may surface new build errors in existing projects that
  already contain these conditions (previously a warning)."
- Prior triage/spec for that change already exist in this repo:
  `docs/SVY-21356-triage.md`, `docs/SVY-21356-investigation.md`,
  `docs/SVY-21356-css-form-body-relation-severity.spec.md`. They confirm the
  detection logic was unchanged and only severity was escalated.
- **`checkKeyTypes` logic** (`servoy-client/.../Relation.java:802`) last materially
  changed in **`3f2676e78`** (SVY-17894/SVY-17431, 2023-02-21): "checkTypes should be
  able to use `Array<string>` or any typed array" — this added the
  `ArrayString`/`ArrayNumber`-vs-column branch but did **not** add a UUID-array case,
  which is the gap behind the `Array<UUID>`-to-`MEDIA` false positive. No change to
  this method between 26.06 and 26.09, confirming detection did not regress — only the
  marker severity did.
- No DLTK version bump, target-platform (`launch_targets/*.target`), or MANIFEST
  version-range change is implicated in this path.
