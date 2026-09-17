# Triage Report — SVY-21467

**Verdict:** PROCEED

## Reported problem

Calling `plugins.window.createPopupMenu()` and then `menu.show(elements.bootstrapbutton, 1, 40)`
against a **bootstrap** component logs a warning to the server log:

```
WARN com.servoy.j2db.util.Debug - Warn: Trying to get a property: size(height)
  that is a not in the spec bootstrapcomponents-button property on component settings
```

The popup menu still shows (functionally it works); the ticket is about the spurious warning.
No solution is proposed in the ticket.

## Root-cause assessment

The warning originates entirely from the **window plugin's server-side script**, not from
component or CSS-position code.

1. `menu.show(...)` is implemented in `window_server.js` — this runs server-side in Rhino
   (`servoy-client/servoy_ngclient/war/servoydefaultservices/window/window_server.js:215`).
   In every branch where a component/event is passed it does:

   ```js
   command.elementId = component.svyMarkupId;
   command.height    = component.height;   // <-- reads .height on the runtime component
   command.positionTop = false;
   ```
   (lines 227-228, 233-234, 242-245)

2. Reading `component.height` in server-side script goes through
   `RuntimeLegacyComponent.get(...)`
   (`servoy-client/servoy_ngclient/src/com/servoy/j2db/server/ngclient/component/RuntimeLegacyComponent.java`).
   `"height"` is mapped in `ScriptNameToSpecName` to the spec property `size`
   (lines 94-95: `ScriptNameToSpecName.put("height", StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName())`),
   then `convertValue` extracts `Dimension.height`.

3. The warning is raised in `RuntimeLegacyComponent.get(...)` at line 353-361: when the
   converted spec property (`size`) is **not declared in the component's .spec** and the
   current value is null, it logs *"Trying to get a property: size(height) that is a not in
   the spec ..."*.

4. This is exactly the bootstrap case. The **legacy servoydefault** button declares a
   `size` property
   (`servoy-client/servoy_ngclient/war/servoydefault/button/button.spec:21` →
   `"size": {"type":"dimension", ...}`), so the lookup succeeds there and no warning is
   logged. The **bootstrap** button (`servoy_test/bootstrapcomponents/button/button.spec`,
   and the shipped bootstrapcomponents package) does **not** declare `size` — it declares
   only a server-only `designsize` and uses `cssPosition` for its actual size/location.
   So on bootstrap components the `size`→`height` read falls through to the warning branch.

**Why the size read exists at all / where the real positioning comes from** (directly
answering the user's questions):

- The `command.height` sent to the client is **only a fallback vertical offset**. The
  **actual X/Y** where the menu is drawn is computed on the **client** from the live DOM,
  not from the design size/location:
  `window.js:597` → `var jsCompReg = YAHOO.util.Dom.getRegion(element)` reads the element's
  real rendered rectangle relative to the viewport. The menu is then positioned with
  `oMenu.moveTo(jsCompReg.left + x, jsCompReg.top + y ...)` (lines 610-614).
- Because the client reads the *rendered* element region, this works identically for
  CSS-position anchored forms and for responsive forms — in both cases the browser has laid
  the element out and `getRegion` returns its true on-screen position. The server-provided
  `height` is used only in `roomBelow` (line 599) and as the drop-down offset when no
  explicit `x/y` was passed (line 612, `xyReceived ? 0 : command.height`).
- In the ticket's call `menu.show(elements.bootstrapbutton, 1, 40)` an explicit `x=1, y=40`
  is passed, so `xyReceived` is true and `command.height` is **not even used** for the
  offset — it is only consumed by the `roomBelow` space check. The read that triggers the
  warning is therefore near-vestigial for the reported call.

So: the popup does **not** genuinely need the server-side `component.height`/`size` to
position itself relative to a CSS-positioned or responsive element — the client already has
the true geometry. The server read is a legacy carry-over from the absolute-layout
servodefault era.

## Ticket premise check

The ticket proposes no solution, only reports the warning. The premise ("this is a real
defect, the warning should not happen") holds: it is a genuine Servoy-side issue — a
legacy assumption in `window_server.js` that every runtime component exposes a `size`
property, which is false for bootstrap/NG2 components that are CSS-position based. It is
not user misuse and not third-party.

## Approaches considered

1. **Guard the height read in `window_server.js`** — only read `component.height` when it is
   actually defined/available, e.g. `if (component.height != undefined) command.height =
   component.height;` (or read via a safe accessor). The client already tolerates a missing
   `height` (it is only used in `roomBelow`/fallback offset, and the client re-measures the
   real menu height from the DOM via `getRegion`).
   - Pros: smallest change, kills the warning at the source, no spec changes, no behavior
     change for the common (explicit x/y or DOM-measured) cases. Aligns with the user's
     observation that CSS positioning already provides the real coordinates client-side.
   - Cons: the `roomBelow` computation loses the component-height term when height is absent;
     needs a sensible fallback (e.g. use the DOM region height client-side, which is already
     available as `jsCompReg` — arguably the client should derive the component height from
     `jsCompReg` instead of trusting the server value).

2. **Move the height determination entirely to the client** — drop `command.height` from the
   server command and compute the component height on the client from
   `YAHOO.util.Dom.getRegion(element)` (already fetched at `window.js:597`).
   - Pros: most correct; removes the server-side dependency on a legacy `size` property
     completely; single source of truth (the rendered DOM) for both size and location; fixes
     it for all component types (bootstrap, NG2, responsive) uniformly.
   - Cons: slightly larger change touching both `window_server.js` and `window.js`; needs
     testing across the `show()` overloads (component, event, x/y, positionTop) and the
     top/bottom placement logic.

3. **Suppress the warning generically in `RuntimeLegacyComponent`** for `size`/`location`
   when the component has a `cssPosition` property instead.
   - Pros: fixes this and any similar legacy reads across all plugins.
   - Cons: broad blast radius; risks hiding genuinely wrong property reads elsewhere; treats
     the symptom (the log line) rather than the cause (window plugin reading a property that
     doesn't exist on modern components). Not recommended.

4. **No code change** — accept the warning as cosmetic.
   - Pros: zero risk.
   - Cons: it is a WARN in the server log on a normal, documented API call against the
     standard bootstrap components, so it will recur for many customers and generate noise /
     support questions. Given the ticket is a Major bug with a fixVersion (2026.9.0), doing
     nothing is not appropriate.

## Recommendation

**PROCEED** with **Approach 2** (client-side height determination) as the preferred fix,
falling back to **Approach 1** if a minimal change is wanted for the release.

Rationale, tying to the user's questions:
- The popup positioning does not, and should not, rely on the server-side legacy
  `size`/`location` of the component. The **real** X/Y already comes from the client's
  `YAHOO.util.Dom.getRegion(element)` on the rendered element, which is correct for both
  CSS-position and responsive forms.
- The only thing the server currently contributes is `command.height`, used as a fallback
  drop offset and in the `roomBelow` check. That same height is trivially available on the
  client from `jsCompReg` (the region already measured at `window.js:597`).
- Therefore the clean fix is to stop reading `component.height` on the server (which is what
  trips the "size not in spec" warning on bootstrap/NG2 components) and derive it from the
  DOM region on the client. This removes the warning at its root and removes an obsolete
  dependency on the absolute-layout `size` property.

Approach 1 is the low-risk subset: simply guard the server read so it does not fault on
components without a `size` property, keeping the existing fallback semantics.

Any fix should be verified against the `menu.show(...)` overloads:
`show()`, `show(component)`, `show(event)`, `show(component, positionTop)`, `show(x, y)`,
and `show(component, x, y[, positionTop])`, for both a CSS-position (absolute) form and a
responsive form, and for both servodefault (has `size`) and bootstrap (no `size`)
components.

## Git history findings

- `window_server.js`'s current `show(...)` implementation and the `command.height =
  component.height` reads are attributed to commit `ebcb176513` ("Introduce end-of-line
  normalization", Johan Compagner, 2026-06-06). That commit was a repo-wide EOL/normalization
  pass, so blame points at it mechanically — the logic itself predates it and was only
  re-touched by the normalization. There is no dedicated prior spec in `docs/` for this
  behavior; the height-read is legacy window-plugin code carried over from the
  absolute-layout servodefault era.
- The relevant contrast is in the spec files, not the git history: servodefault `button.spec`
  declares `"size"` while the bootstrap `button.spec` declares only `designsize`
  (server-only) + `cssPosition`, which is why the warning is bootstrap/NG2-specific.
