# Spec: SVY-21482 — Migrate servoy_ngclient.tests from JUnit 4 to JUnit 5 (Jupiter)

## 1. Goal

The `servoy_ngclient.tests` bundle still runs on JUnit 4 (`org.junit.Test`,
`org.junit.Assert`, `@Before`/`@After`), while newer test bundles in the same repo — notably
`servoy_mcp_tests` — already run on JUnit 5 (Jupiter). Standardize `servoy_ngclient.tests` on
JUnit 5 so the repository uses a single, modern test stack, using `servoy_mcp_tests` as the
reference configuration. This is a test-only change: no production code and no test *intent*
changes — only the test framework APIs and the bundle's test wiring.

Split out from SVY-21467 (which fixed the popup-menu `size(height)` warning and merely noticed
this project was still on JUnit 4).

## 2. Background

### 2.1 Current state of `servoy_ngclient.tests`

- ~57 `*.java` files under `src/test/java`; 42 import JUnit 4 APIs.
- `META-INF/MANIFEST.MF` declares `Require-Bundle: org.junit, ...` (JUnit 4).
- `.classpath` has no JUnit container; it relies on `org.eclipse.pde.core.requiredPlugins`.
- `pom.xml` is a bare `eclipse-test-plugin` (no special surefire/target config).
- No JUnit 3 (`junit.framework`), no `@Rule`/`@ClassRule`/`@Ignore`, no `@RunWith(Parameterized)`,
  no `Assume.*`, no `TestName` rule.
- One JUnit 4 test **suite**: `AllAuthenticatorTypesTestSuite`
  (`@RunWith(Suite.class)` + `@Suite.SuiteClasses`).
- One **Hamcrest** usage: `DataAdapterListTest` (`assertThat(..., greaterThan(...))`, 3 call
  sites at lines ~501/504/506).
- Base class `AbstractSolutionTest` (extended by ~12 heavy OSGi/solution tests) uses
  `@Before`/`@After`; its own base `Log4JToConsoleTest` has no lifecycle annotations.

### 2.2 Reference configuration — `servoy_mcp_tests`

`servoy_mcp_tests` already resolves JUnit 5 correctly in both Eclipse and the Tycho build:

- **MANIFEST.MF** `Import-Package` (versioned):
  ```
  org.junit.jupiter.api;version="[5.0.0,7.0.0)",
  org.junit.jupiter.api.function;version="[5.0.0,7.0.0)",
  org.junit.jupiter.params;version="[5.0.0,7.0.0)",
  org.junit.jupiter.params.provider;version="[5.0.0,7.0.0)",
  org.junit.platform.commons;version="[1.0.0,7.0.0)",
  org.junit.platform.commons.annotation;version="[1.0.0,7.0.0)"
  ```
  (plus mockito / log4j imports it needs — NOT required here unless a test needs them).
- **.classpath** adds the JUnit 5 container BEFORE `requiredPlugins`, with a comment explaining
  why (single JUnit 5 generation avoids `IllegalAccessError`):
  ```xml
  <classpathentry kind="con" path="org.eclipse.jdt.junit.JUNIT_CONTAINER/5"/>
  ```
  Tycho ignores `.classpath` and resolves from `Import-Package`; the container is for the Eclipse
  IDE / JDT launcher only.

The platform therefore already provides JUnit 5 — no target-platform change is required.

### 2.3 The message-argument hazard (the one non-mechanical part)

JUnit 4 and JUnit 5 order the optional failure **message** differently:

| API | JUnit 4 (`org.junit.Assert`) | JUnit 5 (`org.junit.jupiter.api.Assertions`) |
|-----|------------------------------|----------------------------------------------|
| `assertEquals` | `(message, expected, actual)` | `(expected, actual, message)` |
| `assertTrue`/`assertFalse` | `(message, condition)` | `(condition, message)` |
| `assertNull`/`assertNotNull` | `(message, object)` | `(object, message)` |
| `assertSame`/`assertNotSame` | `(message, expected, actual)` | `(expected, actual, message)` |
| `assertArrayEquals` | `(message, expected, actual)` | `(expected, actual, message)` |

In this project there are roughly:
- **~337** `assertTrue/assertFalse/assertNull/assertNotNull` calls with a **leading String
  message** (`assertTrue("msg", cond)` → must become `assertTrue(cond, "msg")`).
- **~100** `assertEquals`/`assertSame`/`assertArrayEquals` calls with a leading String message
  (`assertEquals("msg", exp, act)` → `assertEquals(exp, act, "msg")`).

**Critical distinction:** a 2-arg `assertEquals("literal", actual)` where the first arg is the
**expected value** (not a message) must be left untouched. Only reorder when the leading String
is genuinely a *message* — i.e. `assertEquals` with **three** args, or
`assertTrue/False/Null/NotNull` with **two** args whose first is a String. A naive regex that
moves every leading string literal silently corrupts expected-value assertions, so this step
must be verified by compiling AND running the suite.

## 3. Design

### 3.1 Bundle wiring (do first)

1. **`META-INF/MANIFEST.MF`** — remove `org.junit` from `Require-Bundle`; add the Jupiter/platform
   `Import-Package` block from §2.2. Keep the existing non-JUnit requires
   (`org.skyscreamer.jsonassert`, log4j) and the existing second `Import-Package` block
   (`com.servoy.j2db.util`, `jakarta.servlet.descriptor`, `org.sablo.websocket.utils`) — merge
   into one `Import-Package` header. Preserve `Export-Package` and `Fragment-Host: servoy_ngclient`.
2. **`.classpath`** — add `<classpathentry kind="con" path="org.eclipse.jdt.junit.JUNIT_CONTAINER/5"/>`
   before `org.eclipse.pde.core.requiredPlugins`, mirroring `servoy_mcp_tests` (keep the existing
   `test="true"` attribute on the `src/test/java` entry).
3. **`pom.xml`** — no change expected (Tycho resolves JUnit 5 from `Import-Package`, same as
   `servoy_mcp_tests` whose extra surefire/spifly config was SLF4J-specific, not JUnit-related).
   Only revisit if the Tycho test run cannot find the Jupiter engine.

### 3.2 Per-file source migration (mechanical, but message-order aware)

For each of the 42 JUnit 4 files (and the new `RuntimeLegacyComponentCssPositionTest` added by
SVY-21467, which is also JUnit 4):

- Imports:
  - `import org.junit.Test;` → `import org.junit.jupiter.api.Test;`
  - `import org.junit.Before;` → `import org.junit.jupiter.api.BeforeEach;`
  - `import org.junit.After;` → `import org.junit.jupiter.api.AfterEach;`
  - `import org.junit.BeforeClass;` → `import org.junit.jupiter.api.BeforeAll;`
  - `import org.junit.AfterClass;` → `import org.junit.jupiter.api.AfterAll;`
  - `import static org.junit.Assert.*;` → `import static org.junit.jupiter.api.Assertions.*;`
  - `import static org.junit.Assert.assertX;` → `import static org.junit.jupiter.api.Assertions.assertX;`
  - `import org.junit.Assert;` → `import org.junit.jupiter.api.Assertions;` and rewrite
    `Assert.assertX(...)` → `Assertions.assertX(...)` (494 FQ call sites) — OR replace with static
    imports; pick one style per file consistently.
- Annotations: `@Before`→`@BeforeEach`, `@After`→`@AfterEach`, `@BeforeClass`→`@BeforeAll`,
  `@AfterClass`→`@AfterAll`. `@BeforeAll`/`@AfterAll` methods must be `static` (the one
  `@BeforeClass` here already is; verify).
- **Message reorder** per §2.3 — the only non-mechanical step. Do this carefully and let the
  compile + test run catch mistakes.

### 3.3 Special cases

1. **`AllAuthenticatorTypesTestSuite`** — replace JUnit 4 suite with JUnit Platform Suite:
   ```java
   import org.junit.platform.suite.api.SelectClasses;
   import org.junit.platform.suite.api.Suite;

   @Suite
   @SelectClasses({ DefaultLoginManagerTest.class, AuthenticatorManagerTest.class,
       OAuthHandlerTest.class, CloudStatelessAccessManagerTest.class, LoginResultTest.class })
   class AllAuthenticatorTypesTestSuite { }
   ```
   Requires `org.junit.platform.suite.api` on the bundle classpath — add its `Import-Package`
   (`org.junit.platform.suite.api;version="[1.0.0,7.0.0)"`) and JUnit-5 container. If the suite
   API is not in the target platform, the fallback is to delete the suite class (the five tests
   still run individually; the suite was only a coverage-grouping convenience per its Javadoc).
   Confirm availability before choosing.
2. **`DataAdapterListTest` Hamcrest** — rewrite the 3 `assertThat(x, greaterThan(y))` to
   `assertTrue(x > y, "message")` (Jupiter has no Hamcrest), and drop the
   `org.hamcrest.*` static imports. This removes the only Hamcrest dependency.

### 3.4 Test-authoring convention for this project going forward

After migration, `servoy_ngclient.tests` follows the repo JUnit 5 convention (as
`servoy_mcp_tests`): Jupiter `@Test`, `Assertions.*`, `@BeforeEach`/`@AfterEach`,
`@DisplayName` only on `@Test` methods (never on classes/`@Nested`), no `public` modifier
required.

## 4. Implementation plan

1. Update `META-INF/MANIFEST.MF` (§3.1.1) and `.classpath` (§3.1.2).
2. Confirm `org.junit.platform.suite.api` availability (for the suite). Decide keep-vs-delete
   for `AllAuthenticatorTypesTestSuite`.
3. Migrate files in batches (group by directory: `auth/`, `property/`, root). For each file:
   imports → annotations → `Assert.`→`Assertions.` → **message reorder** → `organizeImports` →
   `formatFile` → `getCompilationErrors` (zero errors).
4. Handle the two special cases (§3.3).
5. Migrate the SVY-21467 `RuntimeLegacyComponentCssPositionTest` to Jupiter as part of this
   change (it currently uses JUnit 4 `@BeforeClass`/`Assert`).
6. Run the full `servoy_ngclient.tests` suite and confirm the pass count matches the pre-migration
   baseline (record the baseline first by running on the JUnit 4 version). Pay special attention to
   any newly-passing/failing assertion that would indicate a botched message reorder (e.g. an
   assertion that used to compare values now comparing a value against a message string).

## 5. Acceptance criteria

- [ ] No source file in `servoy_ngclient.tests` imports `org.junit.Test`, `org.junit.Assert`,
      `org.junit.Before`, `org.junit.After`, `org.junit.BeforeClass`, `org.junit.AfterClass`,
      `org.junit.runner.*`, `org.junit.runners.*`, or `org.hamcrest.*`.
- [ ] `MANIFEST.MF` no longer has `org.junit` in `Require-Bundle`; it imports the Jupiter/platform
      packages (versioned) as `servoy_mcp_tests` does.
- [ ] `.classpath` has the JUnit 5 container before `requiredPlugins`.
- [ ] `AllAuthenticatorTypesTestSuite` either runs as a JUnit Platform `@Suite` or is intentionally
      removed (documented in the commit).
- [ ] `DataAdapterListTest` no longer depends on Hamcrest; its 3 former `assertThat` checks assert
      the same conditions.
- [ ] `eclipse-ide_getCompilationErrors` on `servoy_ngclient.tests` → zero errors.
- [ ] The full `servoy_ngclient.tests` suite passes, and the pass/total count matches the recorded
      pre-migration baseline (no test silently dropped or inverted by a message-order mistake).
- [ ] Every `assertEquals`/`assertTrue`/... that had a JUnit 4 leading message now has that message
      as the LAST argument, and no genuine 2-arg `assertEquals(expected, actual)` was altered.

## 6. Out of scope

- Any production code change (this is test-only).
- Changing what the tests verify, adding/removing test cases, or restructuring into `@Nested`.
- Migrating other test bundles.
- Target-platform changes (JUnit 5 is already available).
- Adopting `@ParameterizedTest`/`@MethodSource` rewrites of existing loop-based tests (nice, but
  not part of a framework migration).

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Is `org.junit.platform.suite.api` present in the active target platform (needed to keep `AllAuthenticatorTypesTestSuite` as a `@Suite`)? If not, delete the suite class (the 5 tests still run standalone). | Dev | Open — resolve in step 2 |
| Prefer static-imported `assertX` or `Assertions.assertX` for files that currently use `org.junit.Assert.assertX` fully-qualified (494 sites)? Recommend static import per file for brevity and to match most existing files. | Dev | Open |
| Record the exact pre-migration pass/total baseline (some tests need the OSGi/solution harness via `AbstractSolutionTest`) so the post-migration run can be compared. | Dev | Open — capture in step 6 |
