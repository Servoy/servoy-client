# Triage Report — SVY-21306

**Verdict:** PROCEED

## Reported problem
`String.replaceAll()` is not shown in code completion in Solex (Servoy Developer) and produces a warning, even though it works correctly at runtime.

## Root-cause assessment
The runtime Rhino engine (`C:\Users\vosti\git_master\rhino\rhino\src\main\java\org\mozilla\javascript\NativeString.java:970`) fully implements `replaceAll` as an ES2021 method. However, the DLTK type system and Servoy documentation model were never updated to declare it:

1. **`native-references.xml`** (`org.eclipse.dltk.javascript/plugins/org.eclipse.dltk.javascript.core/resources/native-references.xml`) — defines the String type for DLTK's type checker and code completion. It has `replace` (line 850) but no `replaceAll` entry. This causes the warning and missing completion.

2. **`String.java`** (`servoy-client/servoy_shared/src/com/servoy/j2db/documentation/scripting/docs/String.java`) — the Servoy documentation model. It defines `js_replace` (4 overloads, lines 562–627) but no `js_replaceAll`. This means the method won't appear in Servoy API docs.

This was likely an oversight when Rhino was updated to 1.8.0 (commit `2cbf85c8b`, SVY-19946). That commit added `matchAll` to both files but missed `replaceAll`.

## Ticket premise check
The ticket reports the problem without proposing a solution. The premise is correct — this is a genuine gap in the type/documentation metadata.

## Approaches considered
1. **Add `replaceAll` to `native-references.xml` and `String.java`** — directly fixes both code completion and the warning. Mirrors how `replace` is already defined (with String|RegExp first parameter and String|Function second parameter). Minimal, safe change.
2. **Also update the DLTK Rhino `NativeString.java`** — the older Rhino copy in `org.eclipse.dltk.javascript/plugins/org.eclipse.dltk.javascript.rhino/` could also get `replaceAll` added. However, DLTK relies on `native-references.xml` for type info, not on NativeString.java's method table, so this is not strictly necessary for fixing the reported issue.
3. **No code change** — not appropriate; this is a clear metadata omission that causes a user-visible warning and broken completion.

## Recommendation
Approach 1: Add `replaceAll` to both `native-references.xml` and `String.java`.

In `native-references.xml`, add a new `<members>` entry after the existing `replace` method (line 871), modelling `replaceAll` with the same parameter types (String|RegExp pattern, String|Function replacement), returning String.

In `String.java`, add `js_replaceAll` overloads mirroring the existing `js_replace` overloads (regexp+string, regexp+function, string+string, string+function).

Both changes are in separate repositories:
- `native-references.xml` → `C:\Users\vosti\git_master\org.eclipse.dltk.javascript`
- `String.java` → `C:\Users\vosti\git_master\servoy-client`

## Git history findings
- `2cbf85c8b` (SVY-19946, 2025-03-03): Added `matchAll` to `String.java` but missed `replaceAll`.
- The runtime Rhino at `C:\Users\vosti\git_master\rhino` has `js_replaceAll` fully implemented (line 970), confirming the method works at runtime.
- `native-references.xml` was last touched for SVY-21221 (Array.sort type support) — no String changes in recent history.
