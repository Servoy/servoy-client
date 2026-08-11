# Spec: INTERNAL — JSFoundSetUpdater: use PK IN condition for bulk updates when all PKs are loaded

## 1. Goal

When `JSFoundSetUpdater.js_performUpdate()` performs a bulk SQL UPDATE and all foundset PKs are already loaded in memory (`allFoundsetRecordsLoaded == true`), the UPDATE's WHERE clause should use `pk IN (pk1, pk2, ...)` instead of the foundset's original WHERE clause. This eliminates a race condition where rows inserted or updated between foundset load and bulk update execution could inadvertently be affected, triggering "Update count check failed (continuing)" warnings and falling back to the slower `performLoopUpdate()`.

## 2. Background

### Current behaviour

In `JSFoundSetUpdater.java:233`, the bulk UPDATE always uses:

```java
sqlUpdate.setCondition(sqlParts.getWhereClone());
```

This copies the foundset's original SELECT WHERE clause (e.g. `WHERE status = 'open'`) as the UPDATE condition. When `allFoundsetRecordsLoaded == true`, the code sets `expectedUpdateCount` to `currentPKs.getRowCount()` (line 254). If another client inserts a row matching the WHERE clause between the foundset load and the UPDATE execution, the actual update count exceeds the expected count, producing `UNEXPECTED_UPDATE_COUNT`. The existing fallback (line 262-267) catches this and calls `performLoopUpdate()`, which updates records one-by-one — a correctness workaround but not a proper fix.

### Prior art in the codebase

`SQLGenerator.createSetConditionFromPKs()` (`SQLGenerator.java:659`) already constructs a `SetCondition` representing `pk IN (values...)` from PK column metadata and a dataset of PK values. It is used in:

- `FoundSet.replaceOmitCondition()` (line 6432-6436) — constructs `pk NOT IN (omittedPKs)`
- `SQLGenerator.createSelectStatement()` (line 236) — same pattern for omit conditions

The `QueryUpdate.setCondition(ISQLCondition)` method (`QueryUpdate.java:67`) accepts any `ISQLCondition`, including `SetCondition`.

### Why this is safe

When `allFoundsetRecordsLoaded == true`, the PK set is guaranteed complete and bounded (≤ `pkChunkSize`, no `hadMoreRows()`). Using these PKs as the WHERE clause makes the UPDATE deterministic — it can only affect the exact rows the caller intended.

## 3. Design

### 3.1 Conditional WHERE clause construction

In `js_performUpdate()`, after constructing `sqlUpdate` and populating its column/value pairs (line 213-231), branch on `allFoundsetRecordsLoaded`:

- **When `true`:** Build a `SetCondition` via `SQLGenerator.createSetConditionFromPKs(IBaseSQLCondition.EQUALS_OPERATOR, pkQueryColumns, pkColumns, currentPKs)` and set it as the update condition.
- **When `false`:** Keep existing behaviour: `sqlUpdate.setCondition(sqlParts.getWhereClone())`.

This requires computing `allFoundsetRecordsLoaded` **before** setting the condition (moving the boolean computation from line 236 to before line 233).

### 3.2 Obtaining PK query columns

The PK columns for the update table are obtained from `table.getRowIdentColumns()`. Each `Column` is mapped to a `QueryColumn` via `column.queryColumn(sqlParts.getTable())` — the same pattern used in `FoundSet.replaceOmitCondition()` and `SQLGenerator.createSelectStatement()`.

### 3.3 No change to SQLStatement pks parameter

The `pks` parameter passed to the `SQLStatement` constructor (line 250) remains unchanged. It serves cache-invalidation purposes and is independent of the SQL WHERE clause.

### 3.4 expectedUpdateCount remains set

When `allFoundsetRecordsLoaded == true`, `setExpectedUpdateCount(pks.getRowCount())` is still set (line 253-255). With a PK IN condition the count will always match (unless a row was deleted between load and update), so the `UNEXPECTED_UPDATE_COUNT` fallback becomes a safety net for concurrent deletes rather than a routine occurrence.

## 4. Implementation plan

1. In `JSFoundSetUpdater.js_performUpdate()`, move the `allFoundsetRecordsLoaded` boolean computation (currently line 236) to immediately after `currentPKs` is obtained (after the synchronized block, around line 204).

2. Replace the unconditional `sqlUpdate.setCondition(sqlParts.getWhereClone())` at line 233 with a conditional block:
   ```java
   if (allFoundsetRecordsLoaded)
   {
       List<Column> pkColumns = table.getRowIdentColumns();
       QueryColumn[] pkQueryColumns = pkColumns.stream()
           .map(c -> c.queryColumn(sqlParts.getTable()))
           .toArray(QueryColumn[]::new);
       sqlUpdate.setCondition(
           SQLGenerator.createSetConditionFromPKs(
               IBaseSQLCondition.EQUALS_OPERATOR, pkQueryColumns, pkColumns, currentPKs));
   }
   else
   {
       sqlUpdate.setCondition(sqlParts.getWhereClone());
   }
   ```

3. Remove the now-redundant second `allFoundsetRecordsLoaded` boolean declaration at line 236 (it was moved earlier).

4. Keep the `if (allFoundsetRecordsLoaded) { pks = currentPKs; }` block (lines 237-245) and `setExpectedUpdateCount` (lines 252-255) unchanged.

5. Retain the `UNEXPECTED_UPDATE_COUNT` → `performLoopUpdate()` fallback (lines 262-267) as a safety net for concurrent deletes.

## 5. Acceptance criteria

- [ ] When `allFoundsetRecordsLoaded == true`, the generated SQL UPDATE uses `WHERE pk IN (...)` with the exact PK values from `currentPKs`.
- [ ] When `allFoundsetRecordsLoaded == false`, the generated SQL UPDATE uses the foundset's WHERE clause (existing behaviour unchanged).
- [ ] Bulk updates no longer trigger `UNEXPECTED_UPDATE_COUNT` when new rows matching the foundset's WHERE clause are inserted concurrently (race condition eliminated).
- [ ] The `performLoopUpdate()` fallback still triggers if a row is deleted between load and update (expectedUpdateCount > actualUpdateCount).
- [ ] Composite (multi-column) primary keys are handled correctly via `createSetConditionFromPKs`.
- [ ] No compilation errors introduced; existing `JSFoundSetUpdater` tests continue to pass.

## 6. Out of scope

- Changing the `allFoundsetRecordsLoaded == false` path to also use PK IN (this would require loading all PKs first, which has performance implications for large foundsets).
- Removing the `performLoopUpdate()` fallback entirely (it remains useful for concurrent-delete scenarios and lock errors).
- Changes to `FoundSet.deleteAllInternal()` which has a similar pattern but different semantics and risk profile.

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Should table filters still be applied when using PK IN condition? (They are applied via `fsm.getTableFilterParams` on the SQLStatement, which is independent of the WHERE clause — so yes, no change needed.) | — | resolved |
| Is there a database-specific limit on IN clause size that could be hit given pkChunkSize? (pkChunkSize defaults to 200, well within all major DB limits.) | — | resolved |
