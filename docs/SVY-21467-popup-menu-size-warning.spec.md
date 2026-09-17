# Spec: SVY-21467 — Showing popup menu relative to an element throws warning

## 1. Goal

Calling `plugins.window.createPopupMenu()` and then `menu.show(component, ...)` against a
modern (bootstrap / NG2) component logs a spurious server warning:

```
WARN com.servoy.j2db.util.Debug - Warn: Trying to get a property: size(height)
  that is a not in the spec bootstrapcomponents-button property on component settings
```

The popup still shows in the right place, but the warning is noise on a normal, documented
API call and recurs for any customer using the standard bootstrap components. The window
plugin's server script (`window_server.js`) legitimately asks the runtime component for its
`height`/`width`/`locationX`/`locationY`; the problem is that `RuntimeLegacyComponent` — the
server-side Rhino wrapper — only knows how to satisfy those reads from the legacy `size` /
`location` spec properties, which modern CSS-position based (bootstrap / NG2) components do
not declare. This fix teaches `RuntimeLegacyComponent` to fall back to the component's
`cssPosition` property when `size` / `location` are absent, and to return nothing quietly
(no warning) when neither is available. `window_server.js` and the deprecated NG1
`window.js` are left unchanged.

## 2. Background

### 2.1 How `menu.show(...)` positions the popup today

The window plugin splits the work between a server-side Rhino script and a client-side
Angular directive:

- **Server** — `servoy-client/servoy_ngclient/war/servoydefaultservices/window/window_server.js`,
  `PopupMenu.show(component, x, y, positionTop)` (`window_server.js:215`). For every overload
  that passes a component or event it builds a `command` object and reads the component's
  height:

  ```js
  command.elementId  = component.svyMarkupId;
  command.height     = component.height;   // <-- the read that trips the warning
  command.positionTop = false;
  ```
  (`window_server.js:227-229`, `:233-235`, `:242-245`)

  The command is assigned to `$scope.model.popupMenuShowCommand` (`window_server.js:252`) and
  pushed to the client.

- **Client** — `servoy-client/servoy_ngclient/war/servoydefaultservices/window/window.js`.
  The watcher on `popupMenuShowCommand` locates the target DOM element by id
  (`window.js:590-592`) and computes the *actual* on-screen rectangle from the live DOM:

  ```js
  var jsCompReg = YAHOO.util.Dom.getRegion(element); // element region relative to viewport
  var roomAbove = jsCompReg.top - 1;
  var roomBelow = document.documentElement.clientHeight - jsCompReg.top - newvalue.popupMenuShowCommand.height;
  ...
  oMenu.moveTo(jsCompReg.left + x, jsCompReg.top + y ...); // window.js:610-614
  ```
  (`window.js:597-615`)

### 2.2 Answering the user's questions — where size/location is actually used

> "where does the popup form stuff then still use the size/location? shouldn't it just use
> the css positioning? but what about if that is in a responsive form? how are we then
> getting the actual X and Y where we want to show that context menu or popup menu?"

The menu's **X and Y do not come from the server-side size/location at all.** They come from
`YAHOO.util.Dom.getRegion(element)` on the *rendered* element (`window.js:597`), which returns
the element's true viewport rectangle. Because the browser has already laid the element out,
this is correct identically for:

- **CSS-position (absolute) forms** — the element has an explicit laid-out box, and
  `getRegion` returns it.
- **Responsive forms** — the element's box is whatever the responsive flow produced, and
  `getRegion` returns that too.

So the client already "just uses the CSS positioning" via the DOM region. The only thing the
server-provided `command.height` contributes is a **fallback vertical offset**:

- `roomBelow` space check (`window.js:599`).
- The drop-down offset applied *only* when no explicit `x/y` was given —
  `xyReceived ? 0 : command.height` (`window.js:612`).

In the ticket's exact call `menu.show(elements.bootstrapbutton, 1, 40)` an explicit `x/y` is
passed, so `xyReceived` is true and `command.height` is **not even used** for the offset —
only for the `roomBelow` check. The read that raises the warning is therefore near-vestigial
for the reported call. Even where it *is* used, the same height is trivially available on the
client from the already-measured `jsCompReg` region.

### 2.3 Why the warning is bootstrap/NG2-specific

Reading `component.height` in server-side script routes through
`RuntimeLegacyComponent.get(...)`
(`servoy-client/servoy_ngclient/src/com/servoy/j2db/server/ngclient/component/RuntimeLegacyComponent.java`).
`"height"` is mapped in `ScriptNameToSpecName` to the spec property `size`
(`RuntimeLegacyComponent.java:94-95`), and `convertValue` extracts `Dimension.height`.

When the converted spec property (`size`) is **not declared** in the component's `.spec` and
the current value is null, `get(...)` logs the warning
(`RuntimeLegacyComponent.java:353-361`):

```java
if (webComponentSpec.getProperty(convertName) == null && (value == null || value == Scriptable.NOT_FOUND))
{
    value = Scriptable.NOT_FOUND;
    if (!inServerSideScript() && !getPrototype().has(name, start))
    {
        component.getDataAdapterList().getApplication().reportJSWarning(
            "Warn: Trying to get a property: " + convertName + ... +
                "  that is a not in the spec " + webComponentSpec.getName() + " property on component " + component.getName());
    }
}
```

- The **legacy servoydefault** button declares a `size` property
  (`servoy_ngclient/war/servoydefault/button/button.spec` → `"size": {"type":"dimension", ...}`),
  so the lookup succeeds and no warning is logged.
- The **bootstrap** button declares only a server-only `designsize` and uses `cssPosition`
  for its size/location — it has no `size` property — so the `size`→`height` read falls
  through to the warning branch.

The root cause is a legacy assumption in `window_server.js` that every runtime component
exposes a `size` property, which is false for CSS-position based bootstrap/NG2 components.

### 2.4 Correction to the initial approach (authoritative)

The initial triage leaned toward changing the deprecated NG1 client (`window.js`). That path is
**rejected**: `window.js` is NG1 (Dojo/YUI) and deprecated. The `*_server.js` scripts, however,
run server-side in Rhino and are **still used by the modern TiNG runtime** — the TiNG client
side lives in this workspace at
`com.servoy.eclipse.ngclient.ui/node/projects/window/src/lib/window_service/`
(`popupmenu.service.ts`, `window.service.ts`). Because `window_server.js` is shared and correct
in intent (asking the component for its `height`), the fix must live **below** it, in
`RuntimeLegacyComponent`, so that both the NG1 and TiNG clients benefit and no client code needs
to change.

`window_server.js` legitimately does `command.height = component.height;`. The only defect is
that `RuntimeLegacyComponent` cannot answer that read for a CSS-position component and logs a
warning instead of falling back gracefully.

## 3. Design

### 3.1 Chosen approach — teach `RuntimeLegacyComponent` to fall back to `cssPosition`

Leave `window_server.js` and the NG1 `window.js` unchanged. Fix the read path inside
`RuntimeLegacyComponent.get(...)`
(`servoy_ngclient/src/com/servoy/j2db/server/ngclient/component/RuntimeLegacyComponent.java`)
so that when the legacy geometry properties (`width`/`height`/`locationX`/`locationY`, mapped to
the spec properties `size`/`location`) are requested but the component does **not** declare
`size`/`location`, it:

1. **Falls back to the component's `cssPosition` property** if the component declares one, and
   derives the requested value from it:
   - `height` → `cssPosition.height`
   - `width`  → `cssPosition.width`
   - `locationX` → `cssPosition.left`
   - `locationY` → `cssPosition.top`
2. If `cssPosition` is also absent (or the specific side is not a resolvable numeric pixel
   value), **returns nothing** — i.e. `Scriptable.NOT_FOUND` — **without** logging the
   "not in the spec" warning for these known legacy geometry aliases.

This keeps `menu.show(...)` working: `window_server.js` still gets a usable `component.height`
for a bootstrap/NG2 component (from `cssPosition.height`) when one is available, and gets a
quiet `undefined` (no warning) when it is not. The client already tolerates a missing height
(it re-measures the rendered element), so returning nothing is safe.

### 3.2 Where the fallback goes in `RuntimeLegacyComponent`

The geometry aliases are declared in `ScriptNameToSpecName`
(`RuntimeLegacyComponent.java:94-97`): `width`/`height` → `size`, `locationX`/`locationY` →
`location`. The read is served in `get(String name, Scriptable start)`:

- `convertName(name)` maps `height` → `size` (`:345`).
- `convertValue(...)` currently extracts the value only when it is a `java.awt.Dimension` /
  `java.awt.Point` (`:540-563`); for a CSS-position component `component.getProperty("size")`
  is null, so `value` stays null.
- The warning branch at `:353-361` then fires because `webComponentSpec.getProperty("size")`
  is null and the value is null.

The fix adds, **before** the warning branch, a fallback that runs only for the geometry aliases
(`width`/`height`/`locationX`/`locationY`) when the mapped spec property (`size`/`location`) is
not declared. The `CSSPosition` is obtained from the design persist via
`BaseComponent.getCssPosition()` (`servoy_shared/.../persistence/BaseComponent.java:341` →
`getTypedProperty(StaticContentSpecLoader.PROPERTY_CSS_POSITION)`), reached from the runtime
component with `component.getFormElement().getPersistIfAvailable()`:

```java
// legacy geometry read (width/height/locationX/locationY) on a CSS-position based
// component that does not declare size/location: derive it from cssPosition instead of
// warning that size/location is not in the spec.
if ((value == null || value == Scriptable.NOT_FOUND) &&
    webComponentSpec.getProperty(convertName) == null &&
    needsValueConversion(name)) // name is one of width/height/locationX/locationY
{
    IPersist persist = component.getFormElement().getPersistIfAvailable();
    if (persist instanceof BaseComponent)
    {
        CSSPosition cssPosition = ((BaseComponent)persist).getCssPosition();
        if (cssPosition != null)
        {
            Integer derived = deriveFromCssPosition(name, cssPosition);
            if (derived != null) return derived;
        }
    }
    // no size/location and no usable cssPosition side -> return nothing, but do NOT warn:
    // this is a known legacy geometry alias that simply does not apply to this component.
    return Scriptable.NOT_FOUND;
}
```

`deriveFromCssPosition` parses the relevant side of the `CSSPosition`
(`servoy_shared/src/com/servoy/j2db/persistence/CSSPosition.java`, public `String` fields
`top`/`left`/`width`/`height`) into an `int` pixel value:

- `height`   → `CSSPosition.height`
- `width`    → `CSSPosition.width`
- `locationX`→ `CSSPosition.left`
- `locationY`→ `CSSPosition.top`

Values like `"40"`, `"40px"` parse to `40`; values that are not resolvable to a plain pixel
number (empty, `-1`, percentages, `calc(...)`, `auto`, or an anchored side such as a
`right`/`bottom`-only position where `width`/`height` is not given) yield `null`, causing the
method to return nothing (no warning). Per the case discussion, when the side cannot be a real
pixel value the legacy `size`/`location` read would have returned nothing too, so returning
nothing here matches the prior behaviour — just without the spurious warning.

### 3.3 Scope of the guard — do not suppress unrelated warnings

The no-warning fallback is deliberately narrow: it only applies when **all** of these hold —
the requested name is one of the four geometry aliases, the mapped spec property
(`size`/`location`) is genuinely absent from the spec, and the current value is null. Any other
"not in the spec" read (a typo'd property, a real missing property) still logs its warning
exactly as before. This is **not** the broad triage Approach 3 (blanket suppression); it targets
only the known legacy geometry aliases.

### 3.4 What is explicitly NOT changed

- `window_server.js` — unchanged; it keeps reading `component.height` etc.
- NG1 `window.js` — unchanged (deprecated).
- TiNG `popupmenu.service.ts` / `window.service.ts` — unchanged; they already position from the
  rendered DOM.
- The bootstrap/NG2 component specs — unchanged; no `size` property is added.

## 4. Implementation plan

The change is a single file in the **servoy-client** repository:
`servoy_ngclient/src/com/servoy/j2db/server/ngclient/component/RuntimeLegacyComponent.java`.
(`BaseComponent` and `IPersist` are already imported; add only `CSSPosition`.)

1. Add an import for `com.servoy.j2db.persistence.CSSPosition`.

2. In `get(String name, Scriptable start)`, **before** the existing warning branch
   (`:353-361`), insert the geometry fallback described in §3.2: for a geometry alias whose
   mapped spec property (`size`/`location`) is not declared and whose value is null, obtain the
   design persist via `component.getFormElement().getPersistIfAvailable()`, and if it is a
   `BaseComponent`, derive the value from `((BaseComponent)persist).getCssPosition()` when
   present; otherwise return `Scriptable.NOT_FOUND` without warning.

3. Add a private helper `Integer deriveFromCssPosition(String name, CSSPosition cssPosition)`
   that maps `height`/`width`/`locationX`/`locationY` to the `CSSPosition` side and parses it to
   a pixel `int`, returning `null` when the side is missing or not a plain pixel value. Reuse the
   existing `needsValueConversion(String)` predicate (`:565-572`) to detect the four aliases.

4. Build the `servoy_ngclient` project. Verify no `size(height) ... not in the spec` warning is
   logged on `menu.show` against a bootstrap component, and that positioning is unchanged for
   both NG1 and TiNG clients.

## 5. Acceptance criteria

- [ ] The warning `Trying to get a property: size(height) that is a not in the spec ...` is
      no longer logged when calling `menu.show` against a bootstrap / NG2 component.
- [ ] No `size`/`height`/`location` warning for the exact ticket repro:
      `plugins.window.createPopupMenu(); menu.show(elements.bootstrapbutton, 1, 40)`.
- [ ] For a bootstrap/NG2 component that declares `cssPosition` with a numeric `height`,
      reading `component.height` server-side returns that pixel value (derived from
      `cssPosition.height`); likewise `width`→`cssPosition.width`, `locationX`→`cssPosition.left`,
      `locationY`→`cssPosition.top`.
- [ ] When neither `size`/`location` nor a usable `cssPosition` side is available, the read
      returns nothing (`undefined` in script) and logs **no** warning.
- [ ] Popup / context menu still shows at the correct position for: servoydefault components,
      bootstrap components, a CSS-position (absolute) form, and a responsive form.
- [ ] Verified across the `menu.show` overloads: `show()`, `show(component)`, `show(event)`,
      `show(component, positionTop)`, `show(x, y)`, and `show(component, x, y[, positionTop])`.
- [ ] No warning regression for servoydefault components (which do declare `size`), and no
      change to the warning behaviour for genuinely unknown/typo'd property reads.

## 6. Out of scope

- Blanket/generic suppression of "not in the spec" warnings in `RuntimeLegacyComponent`
  (triage Approach 3). The fallback here is narrowly scoped to the four legacy geometry
  aliases (`width`/`height`/`locationX`/`locationY`).
- Any change to `window_server.js` or the NG1 `window.js` client.
- Any change to the TiNG client (`popupmenu.service.ts` / `window.service.ts`) — it already
  positions from the rendered DOM.
- Adding a `size` property to the bootstrap / NG2 component specs.

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| For CSS-position sides that are not plain pixel values (percentages, `calc(...)`, `auto`, anchored `right`/`bottom` only), the helper returns null and the read yields nothing. | Dev | Resolved — return nothing; the legacy `size`/`location` read would also have returned nothing, so behaviour matches, just without the warning. Client re-measures the rendered element for actual positioning. |
| How to obtain the `CSSPosition` for the component. | Dev | Resolved — via `((BaseComponent)component.getFormElement().getPersistIfAvailable()).getCssPosition()` (`BaseComponent.getCssPosition()` → `getTypedProperty(PROPERTY_CSS_POSITION)`). |
