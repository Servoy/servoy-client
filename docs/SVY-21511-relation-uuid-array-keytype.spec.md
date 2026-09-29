# Spec: SVY-21511 — Accept a UUID array key type against a native-UUID (MEDIA) column in relation key-type validation

## 1. Goal

Extend `Relation.checkKeyTypes` (in the **servoy-client** repo) so that a typed UUID
array (`Array<UUID>`, and/or a plain untyped `Array`) is accepted as a valid primary
key type against a native-UUID foreign **column** (which surfaces as `MEDIA`) for the
`=`, `in`, and `!` relation operators.

This removes the false-positive relation error for
`globals_to_build$buildingorqueued$application` (scope var
`scopes.jobs.applicationJobs`, declared `Array<String>` / experimentally `Array<UUID>`,
joined `in` against the native-UUID column `job_uuid`), which currently blocks a WAR
build on 2026.09 and which the reporter **cannot work around** from their solution.

The `RELATION_ITEM_TYPE_PROBLEM` builder marker stays at **ERROR** severity (the
SVY-21356 intent). This spec does **not** revert that severity.

## 2. Background

### The reported symptom

A WAR build on 2026.09 (nightly_release) now fails with two relation errors that did
not appear on 2026.06, reported by the customer as "not in my IDE":

```
-Relation "globals_to_build$buildingorqueued$application" has a relation item with mismatched keys:
   Key type from scopes.jobs.applicationJobs (Array<String>) does not match with type from job_uuid (MEDIA).
-Relation "mem_assign_users_to_users" has a relation item with mismatched keys:
   Key type from user_uuid (TEXT) does not match with type from user_uuid (MEDIA).
```

Jira: SVY-21511 (Blocker, fixVersion 2026.9.0, caused-by/linked to SCCC-3502 "Upgrade /
Test 2026.09"). `job_uuid` is a database-native UUID. The reporter's own attempt to
change the scope var from `Array<String>` to `Array<UUID>` did **not** fix the build.

### Why the WAR export blocks (and the "not in the IDE" confusion)

Per the triage report (`docs/SVY-21511-triage.md`), this is **not** a DLTK problem and
**not** export-specific validation. The chain is:

1. `Relation.checkKeyTypes(IDataProviderHandler)` returns a non-null "…does not match
   with type from…" message when a relation item's primary/foreign types are considered
   incompatible (`servoy-client` `Relation.java`, method at line 802, message emitted at
   lines 880-891 / 889-891).
2. The **builder** — `ServoyRelationBuilder.checkRelation(...)` (line ~406) — turns a
   non-null result into the `RELATION_ITEM_TYPE_PROBLEM` marker (message
   `Relation "{0}" has a relation item with mismatched keys: {1}.`,
   `MarkerMessages.RelationItemTypeProblem`). The **editor**
   (`RelationEditor.createAndcheck`) uses the same predicate to block saving.
3. The marker severity is declared in `ServoyBuilder.java` (~line 537) as
   `RELATION_ITEM_TYPE_PROBLEM = new Pair<>("relationItemTypeProblem", ProblemSeverity.ERROR)`.
   On **2026.06 this was `WARNING`.**
4. The WAR export gate (`ExportWarWizard.java:155`) blocks with "Cannot export solution
   with errors" only when `BuilderUtils.getMarkers(...) == HAS_ERROR_MARKERS`
   (`BuilderUtils.java:71-73`, ERROR severity only). A WARNING did not block export.

So the regression from 26.06→26.09 is the **SVY-21356 severity escalation** of
`RELATION_ITEM_TYPE_PROBLEM` from WARNING → ERROR (commit `64f62bba7f`, "SVY-21356 Add
builder error markers for invalid artifacts that bypass UI wizard validation [ai]",
contained in `2026.9_RC1`/`2026.9_RC2`). The detection logic itself did not change. The
marker is produced in the IDE too — export just treats it as fatal, whereas in the IDE
Problems view it is easy to overlook (or the `.rel` files had not been rebuilt).

This ticket is exactly the fallout the SVY-21356 commit message predicted: "raising
`RELATION_ITEM_TYPE_PROBLEM` … to ERROR may surface new build errors in existing
projects that already contain these conditions (previously a warning)."

### The pre-existing checkKeyTypes gap (the real fix target)

`checkKeyTypes` already handles a **scalar** UUID on both sides: if `isUUID(primary) &&
isUUID(foreign)` it continues (Relation.java:835-838, "allow uuid to media mapping").
`isUUID` recognizes a Column via the `UUID_COLUMN` flag and a scripting data provider
via its persisted `@type {UUID}` runtime property (`IScriptProvider.TYPE`).

The **array** case is handled separately (Relation.java:867-886). When the primary
maps to `MEDIA` and its `@type` runtime property is `Array` or `Array<…>`, and the
operator is `=`/`!`/`in`, the code accepts the array only for these component types:

```java
ArgumentType componentType = ArgumentType.valueOf(typeProperty);
ok = (componentType == ArgumentType.ArrayString && foreignType == IColumnTypes.TEXT) ||
     (componentType == ArgumentType.ArrayNumber && (foreignType == IColumnTypes.NUMBER || foreignType == IColumnTypes.INTEGER));
```

`ArgumentType` (also in `servoy-client`) defines `ArrayString` (`"Array<String>"`) and
`ArrayNumber` (`"Array<Number>"`) but **no `Array<UUID>` case** — `valueOf("Array<UUID>")`
returns an anonymous, non-matching `ArgumentType`. A native-UUID foreign column maps to
`MEDIA` (via `Column.mapToDefaultType`), never `TEXT`. Therefore:

- `Array<String>` vs native-UUID `MEDIA` → `ok = false` (needs foreign `TEXT`).
- `Array<UUID>` vs native-UUID `MEDIA` → `ok = false` (component type is neither
  `ArrayString` nor `ArrayNumber`).

That is precisely why the reporter's `Array<UUID>` experiment did not help. An array of
UUID values joined `in`/`=` against a native-UUID column is a legitimate pattern that
the current code simply does not recognize. This is the gap Approach 2 closes.

### The two flagged relations are different problems

- **`globals_to_build$buildingorqueued$application`** (`scopes.jobs.applicationJobs`
  `Array<String>`/`Array<UUID>` vs `job_uuid` native-UUID `MEDIA`): the **false
  positive this fix targets**. Legitimate array-of-uuid-to-uuid-column join. The user
  cannot work around it.
- **`mem_assign_users_to_users`** (`user_uuid` **TEXT** vs `user_uuid` native-UUID
  **MEDIA**): a **genuine data-model mismatch** — the reporter already concedes it is
  "sort of valid". `checkKeyTypes` correctly rejects a scalar `TEXT` provider against a
  native-UUID `MEDIA` column. This is **out of scope** for the code fix; the reporter
  must correct that relation (align the column type) in their own solution.

## 3. Design

### 3.1 Where the change lives (cross-repo)

The code change is entirely in the **servoy-client** repo (`servoy_shared`), not in
this `servoy-eclipse` repo:

- `C:\Users\jcomp\git\servoy_release\servoy-client\servoy_shared\src\com\servoy\j2db\persistence\Relation.java`
  — method `checkKeyTypes` (line 802), array branch at lines 867-886.
- `C:\Users\jcomp\git\servoy_release\servoy-client\servoy_shared\src\com\servoy\j2db\persistence\ArgumentType.java`
  — add an `Array<UUID>` component type (see 3.3).

`servoy-eclipse` only **consumes** the result of `checkKeyTypes` via the builder
(`ServoyRelationBuilder` / `ServoyBuilder`) and the WAR export gate. **No severity
change and no code change is needed in servoy-eclipse.** The spec doc lives in this
repo per convention (`docs/`).

### 3.2 How a native-UUID column surfaces, and how the array type is read

- **Foreign column type**: `foreign[i]` is a `Column`. `foreign[i].getDataProviderType()`
  → `mapToDefaultType(...)` maps a binary/UUID storage type to `IColumnTypes.MEDIA`
  (`Column.java:199`). A native-UUID column additionally carries the `UUID_COLUMN` flag
  (`Column.hasFlag(IBaseColumn.UUID_COLUMN)` / `isUUID()` at `Column.java:1241`). This
  flag is what the existing scalar `isUUID(foreign)` helper (Relation.java:902-914)
  already uses to distinguish a genuine native-UUID column from a plain binary/MEDIA
  column.
- **Primary array component type**: the primary side is a scope/global data provider.
  Its declared `@type {Array<…>}` is read from the persisted `IScriptProvider.TYPE`
  runtime property: `((AbstractBase)primary[i]).getSerializableRuntimeProperty(IScriptProvider.TYPE)`
  → e.g. the string `"Array<UUID>"`, `"Array<String>"`, or `"Array"`. This is the
  `typeProperty` local already used in the branch. `ArgumentType.valueOf(typeProperty)`
  converts it to an `ArgumentType`.

### 3.3 The accept rule to add

Inside the existing `primaryType == MEDIA && (typeProperty is Array / Array<…>)` block
(Relation.java:867), for the `=`/`!`/`in` operators, add a UUID-array acceptance branch
alongside the existing `ArrayString`/`ArrayNumber` cases, gated on the foreign column
being a **native-UUID** column:

- Accept when the primary component type is `Array<UUID>` **and** the foreign column is
  a native-UUID column (`isUUID(foreign[i])`, i.e. `MEDIA` + `UUID_COLUMN`).
- Decision point (Open Question 7.1): also accept a plain untyped `Array` and/or
  `Array<String>` against a native-UUID column, since the reporter's original var was
  `Array<String>` and a UUID is transported as a String at the scripting layer. The
  safest customer-unblocking rule is: **for a native-UUID foreign column, accept
  `Array<UUID>`, plain `Array`, and `Array<String>`** (a uuid is representable as a
  36-char string); keep `Array<Number>` rejected against a UUID column.

Concretely, the `ok` computation becomes (illustrative — coder to finalize once the
`ArgumentType.ArrayUUID` decision is made):

```java
boolean foreignIsUuidColumn = isUUID(foreign[i]);   // MEDIA + UUID_COLUMN
ArgumentType componentType = ArgumentType.valueOf(typeProperty);
ok = (componentType == ArgumentType.ArrayString && foreignType == IColumnTypes.TEXT) ||
     (componentType == ArgumentType.ArrayNumber && (foreignType == IColumnTypes.NUMBER || foreignType == IColumnTypes.INTEGER)) ||
     (foreignIsUuidColumn && (componentType == ArgumentType.ArrayUUID
                              || componentType == ArgumentType.Array
                              || componentType == ArgumentType.ArrayString));
```

Add `ArgumentType.ArrayUUID` (`"Array<UUID>"`) as a new constant and a `valueOf(...)`
case in `ArgumentType.java` so `valueOf("Array<UUID>")` resolves to it rather than an
anonymous instance. (Without this, the `componentType == ArgumentType.ArrayUUID` check
can never be true; alternatively compare on `typeProperty` strings, but adding the
constant matches the existing `ArrayString`/`ArrayNumber` pattern and is cleaner.)

The plain-`Array` case (`typeProperty` == `"Array"`, no component) has no
`Array<...>` prefix, so it currently falls straight to `ok = true` (the `if
(typeProperty.startsWith("Array<"))` guard is skipped). That already-permissive plain
`Array` behavior must be preserved — do not tighten it.

### 3.4 Why the runtime join is safe

Both a native-UUID column and a Servoy `UUID`/String uuid value round-trip through the
same MEDIA/UUID handling the scalar `isUUID`&&`isUUID` branch already relies on
(`Column.getAsRightType` handles `UUID`/`String`→UUID for a `UUID_COLUMN`). An `in`/`=`
join with an array of uuid values against a native-UUID column is the array analogue of
the already-supported scalar uuid-to-media mapping, so accepting it does not create a
join the runtime cannot execute.

### 3.5 Git history (from triage — not re-dug)

- `64f62bba7f` — "SVY-21356 Add builder error markers …[ai]" (Diana Bunaciu,
  2026-09-01): flips `RELATION_ITEM_TYPE_PROBLEM` WARNING→ERROR (and PRIORITY_LOW→
  PRIORITY_NORMAL) in `com.servoy.eclipse.model/.../ServoyBuilder.java`. In
  `2026.9_RC1`/`2026.9_RC2`, on `release`. **Root cause of the regression.**
- `checkKeyTypes` array branch last materially changed in `3f2676e78`
  (SVY-17894/SVY-17431, 2023-02-21): added the `ArrayString`/`ArrayNumber`-vs-column
  branch but **no UUID-array case** — the gap this ticket closes. No change to the
  method between 26.06 and 26.09 (detection did not regress, only severity did).
- No DLTK version bump / target-platform / MANIFEST range change is implicated.
- Prior SVY-21356 docs in this repo confirm the above:
  `docs/SVY-21356-triage.md`, `docs/SVY-21356-investigation.md`,
  `docs/SVY-21356-css-form-body-relation-severity.spec.md`.

## 4. Implementation plan

All code changes are in the **servoy-client** repo (`servoy_shared`). Commit there with
the `SVY-21511 … [ai]` convention. This `servoy-eclipse` repo needs no code change.

1. **`ArgumentType.java`** — add a new constant
   `public static final ArgumentType ArrayUUID = new ArgumentType("Array<UUID>");` and a
   corresponding case in `valueOf(...)`:
   `if (ArrayUUID.getName().equalsIgnoreCase(type)) return ArrayUUID;` (place it next to
   the existing `ArrayString`/`ArrayNumber` cases). Consider whether
   `convertFromColumnType` should also emit `ArrayUUID` for a `MEDIA`+UUID column — only
   if needed to keep var-type suggestions consistent (verify before changing; not
   required for the fix).
2. **`Relation.java` `checkKeyTypes`** — in the array branch (lines 867-886), compute
   `boolean foreignIsUuidColumn = isUUID(foreign[i]);` and extend the `ok` expression to
   accept `Array<UUID>` (and per 7.1 also plain `Array` / `Array<String>`) when
   `foreignIsUuidColumn` is true, alongside the existing `ArrayString`/`ArrayNumber`
   branches. Keep the existing behavior for TEXT/NUMBER foreign columns unchanged. Keep
   the `unsupportedKindForOperator` return for non-`=`/`!`/`in` operators unchanged.
3. Do **not** touch `ServoyBuilder` severity, `ServoyRelationBuilder`,
   `ExportWarWizard`, or `BuilderUtils` — `RELATION_ITEM_TYPE_PROBLEM` stays ERROR.
4. Rebuild `servoy_shared`; verify no compilation errors in `Relation.java` /
   `ArgumentType.java`.
5. Add/extend a unit test for `checkKeyTypes` (see acceptance criteria) covering the
   accept and reject cases.
6. Re-check the WAR export path in `servoy-eclipse` against a solution reproducing the
   `applicationJobs` relation to confirm the export no longer blocks (see 5).

## 5. Acceptance criteria

- [ ] A relation whose primary is a scope var declared `Array<UUID>` joined with `in`
      (or `=`, `!`) against a **native-UUID** (`MEDIA` + `UUID_COLUMN`) foreign column
      passes `checkKeyTypes` (returns `null`, no error).
- [ ] The same relation with the primary declared `Array<String>` (the reporter's
      original type) against a native-UUID foreign column also passes `checkKeyTypes`
      (per the 7.1 decision to accept `Array<String>` against a UUID column).
- [ ] A relation with a plain untyped `Array` scope var against a native-UUID foreign
      column continues to pass (no regression to the already-permissive plain-`Array`
      path).
- [ ] `Array<Number>` against a native-UUID (`MEDIA`) foreign column still **fails**
      (`checkKeyTypes` returns the mismatch message) — the fix is scoped to UUID/string
      arrays, not numeric arrays.
- [ ] The genuine scalar mismatch `user_uuid TEXT` vs `user_uuid` native-UUID `MEDIA`
      (the `mem_assign_users_to_users` shape) still **fails** `checkKeyTypes` — this fix
      must not weaken scalar TEXT-vs-UUID detection.
- [ ] Existing `Array<String>`→`TEXT` and `Array<Number>`→`NUMBER`/`INTEGER`
      acceptances are unchanged.
- [ ] `ArgumentType.valueOf("Array<UUID>")` returns the new `ArgumentType.ArrayUUID`
      singleton (not a fresh anonymous instance).
- [ ] With the fix applied, a WAR export of a solution containing the
      `globals_to_build$buildingorqueued$application`-shaped relation no longer produces
      an ERROR `RELATION_ITEM_TYPE_PROBLEM` marker for that relation and no longer blocks
      export at `ExportWarWizard.java:155`.
- [ ] `RELATION_ITEM_TYPE_PROBLEM` remains `ProblemSeverity.ERROR` (SVY-21356 intent
      preserved; no severity revert).

## 6. Out of scope

- Reverting the `RELATION_ITEM_TYPE_PROBLEM` severity to WARNING (Approach 1). Severity
  stays ERROR.
- The WAR-export-only ignore/warn-and-continue special-casing (Approach 3).
- Fixing the `mem_assign_users_to_users` relation (`user_uuid TEXT` vs native-UUID
  `MEDIA`): a genuine data-model mismatch the reporter must correct in their own
  solution (align the column/scope type). No Servoy code change addresses it.
- Any `servoy-eclipse` builder/marker/export code change.
- DLTK / target-platform / MANIFEST changes (not implicated).
- The CSS-position-no-body marker from the same SVY-21356 commit (unrelated).

## 7. Open questions (resolved at spec approval)

The following decisions were made by the user at the Phase 1 approval gate and are now
**binding on the implementation**:

| Question | Decision |
|----------|----------|
| Which array component types are accepted against a native-UUID (`MEDIA`) foreign column? | **Accept `Array<UUID>`, plain untyped `Array`, AND `Array<String>`.** A uuid is representable as a String and the reporter's original var was `Array<String>`, so this unblocks the existing var without a retype. `Array<Number>` stays **rejected** against a UUID column. |
| Must the acceptance be gated on the foreign column being a genuine native-UUID column? | **Yes.** Gate on the existing `isUUID(foreign[i])` helper (`MEDIA` + `UUID_COLUMN` flag) so a plain binary `MEDIA` column is NOT silently accepted. |
| Does `RELATION_ITEM_TYPE_PROBLEM` severity stay ERROR? | **Yes — stays `ProblemSeverity.ERROR`.** No severity revert (SVY-21356 intent preserved). |
| Should `ArgumentType.convertFromColumnType` also emit `Array<UUID>` for a `MEDIA`+UUID column? | Only if required for the fix to compile/behave; verify before changing. Adding the `ArrayUUID` constant + `valueOf` case is the required part. Not an acceptance criterion. |
