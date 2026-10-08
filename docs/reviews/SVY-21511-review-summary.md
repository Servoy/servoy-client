# Peer-review summary — SVY-21511: 2026.09 build shows errors that are not in the IDE

**Risk: LOW.** A tightly-gated widening of one design-time validator (`Relation.checkKeyTypes`)
plus an additive `ArgumentType.ArrayUUID` singleton; the only open items are two
correctness/design judgements (breadth of acceptance; a runtime join path asserted-but-not-run),
not an active defect. Blast radius CONTAINED, security relevance NONE.

**Reviewed scope:** servoy-client @ `ca1d6ca61` (core fix: `Relation.java` + `ArgumentType.java`
+ `RelationUuidArrayKeyTypeTest`), `e68de972c` (docs: triage + spec), `077703981` (test-only
OSGi factory fixup). All three are on `release` and already merged forward to `master`.

**What it does:** SVY-21356 escalated the `RELATION_ITEM_TYPE_PROBLEM` builder marker from
WARNING to ERROR, and WAR export blocks only on ERROR — turning a long-tolerated warning into a
hard blocker. This change closes a pre-existing gap in `checkKeyTypes` rather than reverting the
severity: for a native-UUID foreign column (MEDIA + `UUID_COLUMN` flag) the array branch now also
accepts `Array<UUID>`, plain `Array`, and `Array<String>` for the `=`/`!`/`in` operators, gated on
`isUUID(foreign[i])`. `Array<Number>`, plain-binary-MEDIA-without-the-flag, and scalar-TEXT-vs-UUID
all stay rejected. The second reported error (`mem_assign_users_to_users`, `user_uuid TEXT` vs
native UUID) is intentionally left failing as a genuine data-model mismatch — the fix is partial by
design.

## Manual test plan

### Verifying the fix
1. Create a solution with a global scope var `scopes.jobs.applicationJobs` typed `Array<String>`.
2. Add a DB table with a native-UUID column `job_uuid` (the `UUID_COLUMN` flag set, mapping to MEDIA).
3. Define a relation joining `scopes.jobs.applicationJobs` to `job_uuid` with the `in` operator (also try `=` and `!`).
4. Confirm in the IDE: no `RELATION_ITEM_TYPE_PROBLEM` error marker on the relation, and the inline relation-editor error is gone.
5. Run a WAR export of the solution — it must now complete without the key-type error that blocked it on 2026.09.
6. Repeat steps 1–5 with the var retyped `Array<UUID>` — must also pass (this is the retype that previously did NOT help the reporter).

### Regression checks
7. `Array<Number>` scope var vs the native-UUID `job_uuid` column → must STILL error (export still blocked).
8. A plain binary MEDIA column WITHOUT the UUID flag vs any array key → must STILL error.
9. Scalar `TEXT` provider (e.g. `user_uuid TEXT`) vs a native-UUID column → must STILL error — this is the `mem_assign_users_to_users` shape, intentionally left failing.
10. Existing `Array<String>` → `TEXT` column join → must still be accepted (unchanged).
11. Existing `Array<Number>` → `NUMBER`/`INTEGER` column join → must still be accepted (unchanged).

### Automated checks
12. `RelationUuidArrayKeyTypeTest` in `servoy_ngclient.tests` (14 JUnit 5 tests; the PDE launcher is slow in this workspace) and the servoy-eclipse `FormCssPositionAndRelationSeverityIntegrationTest` (`com.servoy.eclipse.ui.tests`), which exercises the marker path end-to-end through the builder.

### Surfaces to cover
- Developer IDE builder (`ServoyRelationBuilder`), relation editor (`RelationEditor`), and WAR
  exporter all pick up the change transitively via `servoy_shared` — no servoy-eclipse code change
  was needed, so verify all three reflect the new acceptance.
- The three runtime clients (NG, smart, headless) touch these shapes only at join execution, not
  `checkKeyTypes` — so steps 5/6 actually loading related data are the only way to cover the query path.

## Possible improvements / follow-ups
1. **Breadth of acceptance** — accepting plain `Array` and `Array<String>` (not only `Array<UUID>`)
   against a native-UUID column is a deliberate widening to unblock the reporter's existing
   `Array<String>` var without a retype. Decide whether that breadth is intended long-term or should
   be tightened to `Array<UUID>` once the reporter can retype.
2. **Runtime join not exercised** — the uuid-array `in`/`=` join is argued safe by analogy to the
   scalar uuid→media path (`Column.getAsRightType`/`Utils.getAsUUID`), traced by reading but not run.
   The 14 tests cover only the design-time `checkKeyTypes` decision; a live-solution check of the
   query path would remove the last unverified link.
3. **Partial fix** — communicate back to the reporter that the second error
   (`mem_assign_users_to_users`) is left failing by design, so it is not mistaken for an incomplete fix.

---
*-- peer review (case-review): change narrative, regression/blast-radius, security assessment, manual test plan.*
