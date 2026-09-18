# Triage Report — SVY-21475

**Verdict:** PROCEED

## Reported problem

"Tab names in a tab panel are not shown in the properties panel" and "on restart, they
are gone from the view. Yet the file seems to still have them." The ticket explicitly
frames this as a tab panel bug: tab `name` is stored in the `.frm` file but is not
rendered/restored after the IDE is restarted.

## Root-cause assessment

This is **not** a bug in either tab panel component's own code (neither
`bootstrapcomponents-tabpanel` nor `servoydefault-tabpanel`). It is a generic defect in
the shared persistence model class `WebCustomType`
(`servoy-client/servoy_shared/src/com/servoy/j2db/persistence/WebCustomType.java`),
which both tabpanel implementations use to represent each `tab` array item.

`WebCustomType` maintains a static set `purePersistPropertyNames`
(WebCustomType.java:49-63), computed via bean-introspection of `WebCustomType`'s own
setter methods. Because `WebCustomType extends AbstractBase` and (through
`IChildWebObject`) legacy persistence infra expects an `ISupportName`-style `setName`/
`getName()` pair, `WebCustomType` declares its own `setName(String)`/`getName()`
(WebCustomType.java:284-294) that store/read the name via the *old* `propertiesMap`
mechanism (`setTypedProperty`/`getTypedProperty` on `AbstractBase`), **not** via the new
component-JSON mechanism (`PersistHelper.getWebComponentProperty`/
`setWebComponentProperty`) that all other, spec-defined sub-properties use.

Because `setName`/`getName` exist, introspection picks up `"name"` as a
`purePersistProperty`. `WebCustomType.setProperty`/`getProperty` special-case
`purePersistPropertyNames`:

```java
// setProperty
if (purePersistPropertyNames.contains(propertyName)) super.setProperty(propertyName, val);
PersistHelper.setWebComponentProperty(this, propertyName, val);   // ALSO always runs

// getProperty (current, since commit f7ee10f25)
if (purePersistPropertyNames.contains(propertyName)) return super.getProperty(propertyName);
return PersistHelper.getWebComponentProperty(this, propertyName);
```

So **setting** `"name"` actually writes it twice: once into the legacy
`propertiesMap` (via `super.setProperty` → `setName`) and once into the JSON blob (via
`PersistHelper.setWebComponentProperty`, which always runs regardless of the `if`). Both
copies are correct at that point, and the `.frm` file (which is generated from the JSON
side, see `SolutionSerializer`) correctly contains `"name": "..."` for the tab. This
matches the ticket's observation that the file "still seems to have them."

But **getting** `"name"` returns `super.getProperty("name")`, i.e. `AbstractBase`'s
`propertiesMap`, and *never* looks at the JSON blob for that property. `propertiesMap`
is populated only by explicit calls made in the current JVM session (`setName`
originally called through `setProperty`). When Eclipse restarts (or a form/solution is
reloaded from disk), `WebComponent.initCustomTypes()` /
`WebCustomType.initCustomTypes()` reconstruct fresh `WebCustomType` instances purely
from the JSON stored on the parent (`WebCustomType.java:382-425`, called from the
constructor at line 165). The constructor never repopulates `propertiesMap["name"]` from
the reloaded JSON — it only explicitly re-syncs `typeName` (`setTypeName(...)`, line
164) and `json` itself (`setTypedProperty(PROPERTY_JSON, ...)`, line 163).
`extendsID` is special-cased with its own JSON-backed getter/setter
(`getExtendsID`/`setExtendsID`, lines 341-359) so it survives too. `name` has no such
special-casing, so after reconstruction `getProperty("name")` returns `null`/default even
though the underlying JSON, and therefore the `.frm` file, still has the value.

**This was verified empirically.** A JUnit 5 test was written against the existing
`WebCustomTypeAddChildTest` test harness (in `servoy_ngclient.tests`) using a synthetic
`tab`-like custom type with a `"name"` sub-property (mirroring both real tab specs):
- Setting `name` and reading it back on the *same, live* `WebCustomType` instance works.
- Simulating a reload (constructing a fresh `WebCustomType` from the exact same JSON
  that was persisted, the way `initCustomTypes()` does) reproduces the bug exactly:
  `getProperty("name")` returns `null` even though the JSON passed in still has
  `"name":"myTabName"`.

Both real-world tab specs are affected because **both name their tab-identifier
sub-property literally `name`**, colliding with `WebCustomType`'s own legacy `name`:
- `bootstrapcomponents/components/tabpanel/tabpanel.spec` — `tab.name: {"type":"string"}`
- `servoy-client/servoy_ngclient/war/servoydefault/tabpanel/tabpanel.spec` —
  `tab.name: {"type":"string","tags":{"useAsCaptionInDeveloper":true,"captionPriority":1}}`
  (this one is additionally used as the developer-caption for the tab in the Properties
  view outline label — `com.servoy.eclipse.ui.util.DeveloperUtils.
  getCustomObjectTypeCaptionFromTaggedSubproperties`, which explains why the
  servoydefault tab's *displayed caption* in the outline/properties view is specifically
  affected, on top of the raw property value being lost).

Neither `com.servoy.eclipse.designer.rfb` (Angular designer frontend), nor
`com.servoy.eclipse.designer` (Java palette/creation code), nor either component's own
`.js`/`.ts` runtime code is involved. The bug lives entirely in
`servoy-client/servoy_shared`, specifically `WebCustomType`.

## Ticket premise check

The ticket proposes no specific code-level solution — it only describes symptom and
expected behaviour ("Tab names ... should be shown ... and remain visible after
restart"). There is nothing to challenge about a *proposed fix*, but there is a premise
worth correcting: the ticket implicitly treats this as a tab-panel-specific display bug.
It is actually a general `WebCustomType` persistence bug that happens to surface via the
`tab` custom type's `name` sub-property, and could equally affect **any** custom type
(from any component, not just tab panels) that defines a spec-level property literally
called `name`. The report's "steps to reproduce" (set tab names, restart, check
properties panel) do correctly exercise the actual defect.

## Approaches considered

1. **Fix `WebCustomType` to route `"name"` reads/writes through the JSON-backed
   `PersistHelper` mechanism like every other component-JSON property, instead of
   through the legacy `propertiesMap`/`AbstractBase` `ISupportName` implementation.**
   This mirrors the pattern already used for `extendsID` (`WebCustomType.getExtendsID`/
   `setExtendsID`, WebCustomType.java:341-359), which reads/writes directly against
   `getFullJsonInFrmFile()`. Concretely: remove `setName`/`getName` from `WebCustomType`
   (or reimplement them to delegate to `PersistHelper.getWebComponentProperty`/
   `setWebComponentProperty`), so `"name"` is no longer picked up as a
   `purePersistProperty` via introspection, or is special-cased to read from JSON.
   - Pros: fixes the root cause for `name` and for any future spec property with the
     same name collision; small, targeted change; consistent with the existing
     `extendsID` precedent; testable with the reproduction test already written.
   - Cons: `WebCustomType.setName`/`getName` exist because `WebCustomType` implements
     interfaces (transitively, `ICommonWebComponent`/legacy code) that may expect
     `ISupportName`-like behavior in some code paths outside the component-JSON world;
     needs care to make sure no other caller relies on the old `propertiesMap`-backed
     `name` (a workspace-wide `findReferences` on `WebCustomType.getName`/`setName`
     should be run before changing this).

2. **Explicitly re-sync `"name"` from JSON into `propertiesMap` in the `WebCustomType`
   constructor / `initCustomTypes()`, the same way `typeName` and `json` are
   resynced.** i.e. add something like
   `if (json.has("name")) setTypedProperty(PROPERTY_NAME, json.getString("name"));`
   in the constructor.
   - Pros: minimal, localized change, no interface/behavioral changes elsewhere.
   - Cons: papers over the deeper design inconsistency (two storage mechanisms for the
     same conceptual "properties" of a `WebCustomType`); the next spec property that
     happens to be named the same as a `WebCustomType`/`AbstractBase`
     purePersistProperty (there aren't many risk candidates besides `name`/`extendsID`/
     `typeName`/`json`, but it's a landmine pattern) would reproduce the same class of
     bug; less consistent with the `extendsID` precedent already in the code.

3. **Rename the `name` sub-property in both tab specs to something else (e.g.
   `tabName`).**
   - Pros: trivially avoids the collision for tab panels specifically.
   - Cons: breaking change to two public component specs (would break existing
     solutions' `.frm` files referencing `tabs[].name` via SolutionModel API,
     `getTabAt().getName()`/`setName()`-style scripting, and both tab panels' developer
     experience). Does not fix the underlying `WebCustomType` bug for any other
     component that might define a `name` sub-property today or in future. Explicitly
     the *wrong* layer to fix this at — the ticket even says the name is legitimately
     wanted as `name`, matching normal component-property naming.

4. **No code change** — treat as a documentation/expected-behavior issue.
   - Pros: zero risk.
   - Cons: not viable. The reproduction test proves this is a genuine data-loss/read
     bug independent of any single component's implementation; users lose real, stored
     configuration from the properties view and (per the ticket) from any place that
     reads the tab's `name` after a fresh solution/session load. This is a defect, not
     expected behavior.

## Recommendation

**PROCEED with Approach 1**: fix `WebCustomType`'s `name` handling to be backed by the
JSON/`PersistHelper` mechanism (matching the existing `extendsID` pattern), rather than
by the legacy `AbstractBase`/`propertiesMap`-only `setName`/`getName`. This addresses
the root cause in the one shared class both tab panel implementations (and any other
component with a `name` sub-property) depend on, rather than patching two independent
component packages or masking the symptom.

Approach 2 (re-sync in constructor) is an acceptable, lower-risk fallback if Approach 1
turns out to have wider ripple effects on `ISupportName`-dependent code paths elsewhere
in the workspace — worth a `findReferences` check on `WebCustomType` before committing
to Approach 1's interface-level change. Either way, the fix belongs in
`servoy-client` (`servoy_shared`), not in `bootstrapcomponents` or in
`com.servoy.eclipse.*` — no change is needed in either tab panel's own `.spec`/`.js`/
`.ts`/`.java` files.

The failing JUnit 5 test (`WebCustomTypeNamePropertyTest`, using the existing
`WebCustomTypeAddChildTest` scaffolding) that reproduces this exactly should be added
to `servoy_ngclient.tests` as a regression test when the fix is implemented (it was
written and run during this triage, confirmed to fail against current code, then
removed so as not to leave a failing test in the tree).

## Git history findings

- `servoy-client` commit `b7aa82b0b` ("SVY-20784 refactor the flattened stuff in for the
  WebCustomType", 2026-05-08) is the commit that introduced the current
  constructor-based JSON initialization and `initCustomTypes()` machinery, replacing an
  older `WebObjectImpl`-based implementation. The `extendsID` JSON-backed special case
  was added here too. `name` was *not* given the same special-casing at this point —
  this refactor is where the current shape of the bug was introduced (though a similar
  issue likely existed with the older `WebObjectImpl` design as well, given the
  `getProperty` fallthrough pattern visible in the pre-refactor version).
- `servoy-client` commit `f7ee10f25` ("SERVOY-295 fix StackOverflowError and NPE in
  WebCustomType for missing component specs", 2026-08-27) tightened `getProperty` from
  an if/if-fallthrough back to if-return/return (to fix an infinite recursion via
  `getFlattenedJson`), which is unrelated to this bug but confirms `purePersistProperty`
  handling in `getProperty` is an actively-maintained, sensitive area of this class.
- `servoy-client` commits `0fbc08baa`, `f7733d317`, `736b039ad` (SVY-21271, SVY-21282,
  all recent, all `[ai]`) show this exact class (`WebCustomType`'s JSON/persist
  duality) has been the source of several recent, related bugs (nested custom type
  JSON not persisting, clone NPEs, shared-map regressions) — reinforcing that the
  `purePersistProperty`/JSON dual-storage design in `WebCustomType` is a recurring
  source of defects, of which SVY-21475 is one more instance.
- Neither `bootstrapcomponents` nor the `servoy-eclipse` `com.servoy.eclipse.ui`
  `DeveloperUtils`/`FormOutlineLabelprovider` caption logic show any recent changes
  relevant to this bug; they consume `WebCustomType.getProperty("name")` (or the
  tagged-caption mechanism, for servoydefault) correctly — they are downstream victims
  of the root-cause bug, not the cause.
