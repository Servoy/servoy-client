# SVY-21478 — QueryBuilder support for array columns (contains / containsAll)

## Summary

Add Query Builder conditions for native array columns (PostgreSQL, HSQLDB):

- `QBArrayColumn.contains(value)` → `QBCondition` — true when the array column contains the single element `value`.
- `QBArrayColumn.containsAll(values)` → `QBCondition` — true when the array column contains all elements of `values`.

These complement the pre-existing `cardinality()` on `QBArrayColumnBase`. Array columns themselves (storage/foundset) were delivered in SVY-20207; this issue only adds the query-condition surface.

## Background / investigation

- Array column storage already exists (SVY-20207, 2025.6.0): Postgres + HSQLDB, one level deep, created outside Servoy, not supported in relation conditions.
- Query Builder already exposes array columns as `QBArrayColumn` (`TypeCreator.determineColumnClass` returns it for `columnType.isArray()`), but the only array-specific member was `cardinality()`.
- The existing `isin(Object[])` / `eq(...)` are **not** equivalent: `isin` generates `col IN (v1, v2, …)` (scalar column equals one of several values), which is the inverse of "array contains element/sub-array". So the feature genuinely needed new methods.

## Design history: two approaches were tried

### Attempt 1 (reverted) — route through Hibernate's `array_contains`/`array_includes` functions

Hibernate 7.4.5 ships `array_contains`/`array_includes` function descriptors, registered per-dialect (Postgres renders `@>`; HSQLDB renders an `unnest()`-based form). The first implementation added `QueryFunctionType.array_contains`/`array_includes`, had `QBColumnImpl.contains`/`containsAll` build a `QueryFunction` wrapped in `CompareCondition(EQUALS, function, Boolean.TRUE)`, and registered an "unsupported" fallback in `GenericDialect` for dialects without the functions.

**This failed at runtime** against a live PostgreSQL database with:

```
org.hibernate.query.sqm.produce.function.FunctionArgumentException:
Parameter 0 of function 'array_contains()' requires an array type, but argument is of type 'java.lang.Object'
```

Root cause: Hibernate's `ArrayContainsArgumentValidator` requires argument 0 to resolve to a Hibernate **plural (array) type** (a `BasicPluralType`/`BasicArrayType`). Servoy's `HibernateTypeResolver.resolve(TypeInfo, ...)` has no array branch — an array column's `TypeInfo` (sql type `ARRAY`, no registered native type name) falls through to a plain scalar `BasicType` via `resolve(Object.class, sqlTypeCode)`. The column reference therefore never carries plural-type information, so the validator rejects it. This is a real, pre-existing gap in the type resolver, not something fixable from the QB layer alone.

Fixing the resolver itself was rejected: it is shared by *every* column in *every* query Servoy generates, so changing it is a wide-blast-radius change for a feature that only two dialects support.

### Attempt 2 (shipped) — dedicated self-rendered condition, dialect-specific SQL

Bypass Hibernate's function validator entirely. Add a Servoy-owned `ISQLCondition` and let each dialect render its own SQL for it, the same way `CustomCondition` is rendered via `QueryCustomExpression`/`SelfRenderingPredicate`, and the same way `DialectExtensionArraySupport` already lets each dialect plug in its own array behaviour (`getArrayTypename`, `getArrayColumntype`, `supportsArrayExpressions`).

This is the shipped design.

## Shipped design

### 1. Query model — `com.servoy.j2db.query.ArrayContainsCondition`

A new `ISQLCondition` (servoy_shared), parallel to `CompareCondition`/`ExistsCondition`:

```java
public final class ArrayContainsCondition implements ISQLCondition
{
    private IQuerySelectValue arrayColumn;
    private Object argument;       // single element (contains) or Object[]/array value (containsAll)
    private final boolean all;     // false = contains, true = containsAll
    private final boolean negate;
    ...
}
```

Registered in `AbstractBaseQuery`'s serialization `classMapping` (key `35`) so it round-trips through the same `ReplacedObject`/XML mechanism as every other condition.

### 2. QB API — `QBArrayColumnBase` + `QBColumnImpl`

`QBArrayColumnBase` gains:

```java
@JSFunction QBCondition contains(Object value);
@JSFunction QBCondition containsAll(Object[] values);
```

`QBColumnImpl` implements them directly against the new condition type:

```java
@Override
public QBCondition contains(Object value)
{
    return createCondition(new ArrayContainsCondition(getQuerySelectValue(), createOperand(value), false));
}

@Override
public QBCondition containsAll(Object[] values)
{
    return createCondition(new ArrayContainsCondition(getQuerySelectValue(), createOperand(values), true));
}
```

`createCondition` is the existing `QBColumnImpl` helper that applies `.not` negation — so `.not.contains(...)` works for free. `createOperand` converts against the column's own `TypeInfo` as before: a scalar element converts against the element type, an `Object[]` is converted element-wise into one array operand.

### 3. SQL generation — `QueryGenerator` + `DialectExtensionArrayContains`

`QueryGenerator.getHibernatePredicate` dispatches `ArrayContainsCondition` to a new private method:

```java
private Predicate getHibernateArrayContainsPredicate(...)
{
    if (!(dialect instanceof DialectExtensionArrayContains dialectExtensionArrayContains))
    {
        throw new IllegalArgumentException("Current dialect does not support array conditions (contains/containsAll): " + dialect.getClass());
    }

    // render the array column and the operand the same way every other condition does
    Expression arrayColumnExpression = getHibernateExpression(...);     // the array column itself
    Expression operandExpression = getHibernateExpression(...);        // element (scalar) or ValueFactory.ArrayValue (containsAll)

    Predicate predicate = new SelfRenderingPredicate(
        new ArrayContainsExpression(dialectExtensionArrayContains, arrayContainsCondition.isAll(), arrayColumn.getColumnType(),
            arrayColumnExpression, operandExpression));

    return arrayContainsCondition.isNegate() ? new NegatedPredicate(predicate) : predicate;
}
```

The `containsAll` operand is bound via `ValueFactory.createArrayValue(elements, columnType)` — the exact same array-binding path (`SQLEngine.setArray`/`Connection.createArrayOf`) already used by native array-column storage and the `=ANY(?)` path. No new JDBC binding code.

**New extension point** — `com.servoy.j2db.dialect.extensions.DialectExtensionArrayContains`, modeled directly on the existing `DialectExtensionArraySupport`:

```java
public interface DialectExtensionArrayContains
{
    void renderArrayContains(ArrayContainsRenderer renderer, BaseColumnType arrayColumnType);
    void renderArrayContainsAll(ArrayContainsRenderer renderer, BaseColumnType arrayColumnType);

    interface ArrayContainsRenderer
    {
        void sql(String fragment);
        void renderArrayColumn();      // renders the already-built column expression
        void renderElementOperand();   // renders the already-built scalar operand
        void renderArrayOperand();     // renders the already-built array operand
    }
}
```

A dialect controls *only* the surrounding SQL text and the order the two pieces appear in — it never touches parameter binding or column resolution. This split exists because the two implemented dialects need genuinely different argument order:

- **PostgreSQL** (`PostgreSQLDialectExtension`): `arraycolumn @> ARRAY[operand]` for `contains`, `arraycolumn @> operand` for `containsAll` — column first.
- **HSQLDB** (`HSQLDialect`): `CAST(operand AS elementtype) IN (UNNEST(arraycolumn))` for `contains`, `NOT EXISTS (SELECT * FROM UNNEST(CAST(operand AS elementtype ARRAY)) AS v(x) WHERE v.x NOT IN (UNNEST(arraycolumn)))` for `containsAll` — operand first, and the element type name (via the existing `DialectExtensionArraySupport.getArrayTypename`) is needed for the `CAST`.

`ArrayContainsExpression` (`j2db_server`, `dblayer/hibernate`) is the Hibernate `SelfRenderingExpression` that bridges the two: it implements `ArrayContainsRenderer` by delegating to the pre-built Hibernate `Expression`s via `.accept(walker)`, and calls `dialectExtension.renderArrayContains(...)`/`renderArrayContainsAll(...)`. This is the same shape as `QueryCustomExpression`/`CustomCondition`.

On a dialect that does **not** implement `DialectExtensionArrayContains`, `getHibernateArrayContainsPredicate` throws a clear `IllegalArgumentException` up front — no attempt is made to generate invalid SQL.

### Extending to a new database

Adding array-contains support for a future dialect is a two-method job, the same shape as adding `DialectExtensionArraySupport`:

1. Have the dialect `implements DialectExtensionArrayContains`.
2. Implement `renderArrayContains`/`renderArrayContainsAll` using that database's array-containment syntax, calling the renderer's `sql(...)`/`renderArrayColumn()`/`renderElementOperand()`/`renderArrayOperand()` in whatever order the syntax needs.

No changes to `QueryGenerator`, `ArrayContainsCondition`, `HibernateTypeResolver`, or the QB layer are needed.

### IDE

`TypeCreator.determineColumnClass()` already returns `QBArrayColumn` for `columnType.isArray()`, so code completion picks up `contains`/`containsAll` automatically. No change needed.

## Files changed

- `servoy-client/servoy_shared/.../query/ArrayContainsCondition.java` — new condition type.
- `servoy-client/servoy_shared/.../query/AbstractBaseQuery.java` — serialization class-mapping entry.
- `servoy-client/servoy_shared/.../querybuilder/impl/QBArrayColumnBase.java` — `contains`/`containsAll` declarations.
- `servoy-client/servoy_shared/.../querybuilder/impl/QBColumnImpl.java` — implementation against `ArrayContainsCondition`.
- `server/j2db_server/.../dialect/extensions/DialectExtensionArrayContains.java` — new extension point.
- `server/j2db_server/.../dblayer/hibernate/ArrayContainsExpression.java` — Hibernate self-rendering bridge.
- `server/j2db_server/.../dblayer/QueryGenerator.java` — `ArrayContainsCondition` dispatch + predicate builder.
- `server/j2db_server/.../dialect/PostgreSQLDialectExtension.java` — `@>` rendering.
- `server/j2db_server/.../dialect/HSQLDialect.java` — `unnest()`-based rendering.
- `test/j2db_test/.../querybuilder/impl/QBArrayColumnConditionTest.java` — model-level unit test (JUnit 5, no DB).
- `test/j2db_test/.../HibernateIT.java` — `testArrayColumnContains` (end-to-end, Postgres+HSQLDB) and `testArrayColumnContainsUnsupportedDialect` (negative case), plus a `supportsArrayContains()` flag on the test config classes.

`QueryFunction`/`GenericDialect` were touched during Attempt 1 and then reverted back to their original state — no `QueryFunctionType.array_contains`/`array_includes` values exist in the shipped code.

## Verification performed

- **Model-level unit tests** — `QBArrayColumnConditionTest`: 4/4 passing. Asserts `contains`/`containsAll` build an `ArrayContainsCondition` with the right `isAll()`/`isNegate()`/argument, and that `.not.contains(...)` negates.
- **Regression check** — `QueryTest` (pre-existing, untouched): 12/12 passing.
- **Direct SQL verification against live databases** (bypassing the full Servoy stack, to confirm the exact SQL shape each dialect renders):
  - PostgreSQL 17 (bundled with the 2026.9 app server): `tags @> ARRAY[?]` and `tags @> ?` (bound `java.sql.Array`) both return the expected rows for `contains`/`containsAll`.
  - HSQLDB (in-memory, same driver version as bundled): `CAST(? AS INTEGER) IN (UNNEST(a.tags))` and `NOT EXISTS (SELECT * FROM UNNEST(CAST(? AS INTEGER ARRAY)) AS v(x) WHERE v.x NOT IN (UNNEST(a.tags)))` both return the expected rows, including with a qualified/quoted table alias (matching real generated SQL).
- **Not verified**: a full `HibernateIT#testArrayColumnContains` run through the actual Servoy stack. The `HibernateIT` JUnit launcher in this workspace fails to resolve *any* server from `servoy_test.properties` for *every* test in the class (including pre-existing, unmodified tests like `testSumCast`/`testCustomJoin`), so this is a pre-existing environment/classpath issue in the Eclipse test launcher, not a defect in this change. The new tests compile cleanly and are structured identically to the surrounding ones; they should pass once the launcher issue is resolved separately.

## Constraints carried from SVY-20207

Arrays stay one level deep; array columns are not supported in relation conditions — the new methods stay out of that path.
