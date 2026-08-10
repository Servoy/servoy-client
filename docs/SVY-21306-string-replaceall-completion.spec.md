# Spec: SVY-21306 — String.replaceAll() is not shown in Solex and gives a warning

## 1. Goal
Add `replaceAll` to the DLTK type definitions and the Servoy documentation model so that it appears in code completion, produces no warnings, and is included in the Servoy API docs. The method already works at runtime (Rhino 1.8.0+); only the metadata is missing.

## 2. Background

### 2.1 Runtime support
The Rhino engine (`org.mozilla.javascript.NativeString`, line 970 in the `rhino` repository) fully implements `replaceAll` as an ES2021 method. Users can call it and it works — but Solex does not know about it.

### 2.2 DLTK type system
`native-references.xml` in `org.eclipse.dltk.javascript.core/resources/` is the authoritative type definition file for DLTK's type checker and code-completion engine. It already declares `replace` (lines 850–871) with `String|RegExp` first parameter and `String|Function` second parameter, returning `String`. There is no `replaceAll` entry.

### 2.3 Servoy documentation model
`String.java` in `servoy_shared/src/com/servoy/j2db/documentation/scripting/docs/` defines Java stubs (`js_*` methods) that are picked up by the documentation generator. It has four `js_replace` overloads (lines 562–628) but no `js_replaceAll`.

### 2.4 Git history
Commit `2cbf85c8b` (SVY-19946, 2025-03-03) added `matchAll` to both files but missed `replaceAll`. This spec fills that gap.

## 3. Design

### 3.1 `native-references.xml` addition
Add a new `<members>` element immediately after the existing `replace` member (after line 871), with:
- `name="replaceAll"`
- `directType="String"`
- `description="Returns a new string with all matches of a pattern replaced by a replacement. When the pattern is a string, every occurrence is replaced (unlike replace which only replaces the first)."`
- Two parameters identical to `replace`:
  - `findStringOrRegexp` — UnionType of String | RegExp
  - `newStringOrFunction` — UnionType of String | Function

### 3.2 `String.java` addition
Add four `js_replaceAll` overloads after the existing `js_replace` block (after line 628), mirroring the signatures:
1. `js_replaceAll(RegExp regexp, String newSubStr)`
2. `js_replaceAll(RegExp regexp, Function function)`
3. `js_replaceAll(String substr, String newSubStr)`
4. `js_replaceAll(String substr, Function function)`

Each overload gets a Javadoc block with:
- Description emphasising "all matches" semantics
- `@sample` tag with a usage example
- `@param` tags
- `@link` to MDN `String/replaceAll`

## 4. Implementation plan

1. **`native-references.xml`** (`C:\Users\vosti\git_master\org.eclipse.dltk.javascript\plugins\org.eclipse.dltk.javascript.core\resources\native-references.xml`)
   — Insert a new `<members>` block after line 871 (end of `replace`) defining `replaceAll` with the same parameter structure.

2. **`String.java`** (`C:\Users\vosti\git_master\servoy-client\servoy_shared\src\com\servoy\j2db\documentation\scripting\docs\String.java`)
   — Insert four `js_replaceAll` method stubs after line 628 (end of `js_replace` block), with Javadoc.

3. **Verify compilation** — Run `eclipse-ide_getCompilationErrors` to confirm no errors are introduced in `servoy_shared`.

## 5. Acceptance criteria
- [ ] `replaceAll` appears in DLTK code completion for String values in Solex
- [ ] No warning is produced when calling `String.replaceAll()` in the script editor
- [ ] `replaceAll` appears in generated Servoy API documentation with correct parameter descriptions
- [ ] The method signature accepts (String|RegExp, String|Function), returning String
- [ ] No compilation errors in `servoy_shared` or `org.eclipse.dltk.javascript.core`

## 6. Out of scope
- Updating the DLTK Rhino `NativeString.java` copy (not needed for type resolution)
- Adding `replaceAll` to other built-in types
- Runtime behaviour changes (already works via the Rhino engine)

## 7. Open questions
| Question | Owner | Status |
|----------|-------|--------|
| None     | —     | —      |
