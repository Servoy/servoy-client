# Triage Report — SVY-21783

**Verdict:** PROCEED

## Reported problem
Servoy 2024.3.12 LTS, TiNG. A form is shown in a tab panel. Its `onShow` sets `elements.chk_test.enabled = false` and `elements.rad_test.enabled = false`. A button later sets them back to enabled. After that, clicking the checkbox or radio changes it visually, but the dataprovider is not updated and no events fire. Opening the form directly (not in a tab panel) works. Jira has one new comment, a customer ping asking for an ETA. There is still no sample or log attached.

## Reproduction evidence
The bug was reproduced live on the master build (`http://localhost:8183/solution/double_window_test#sdds`). Form `sdds` has a tab panel with 1 tab. The embedded form has `chk_test` and `rad_test` on the same dataprovider, plus an enable button.
- Without the `onShow` disable: one checkbox click toggles both checkbox and radio. The value goes through the server.
- With the `onShow` disable, in a fresh tab:
  - After load, both controls are disabled and checked.
  - After the enable button, the DOM `disabled` attribute is removed from both.
  - The first checkbox click unchecks the checkbox only. The radio stays checked, so nothing reached the server.
  - No JS errors.
- An old tab opened before the `onShow` code was added did not show the bug. The bug needs a fresh form send.

I tried to re-verify in the browser during this pass. The dev server currently answers "TiNG resources are not generated correctly yet", so no new browser evidence was added. The analysis below is from code and git history. It explains every observation above.

## Root-cause assessment
**Root cause (high confidence): a stale `readOnly` property on the client.** It is caused by the server-side `ReadonlySabloValue`, which never marks itself changed when the property it mirrors (`enabled`) changes.

1. The servoydefault `check` and `radio` specs declare `"readOnly" : { "type" : "readOnly", "oppositeOf" : "enabled"}` (`servoy_ngclient/war/servoydefault/check/check.spec:22`, `radio/radio.spec:22`). So for these two components, the client's `readOnly` is a derived value meaning `!enabled`. `combobox` also uses `oppositeOf: "enabled"`. Eight others use `oppositeOf: "editable"`.
2. `ReadonlySabloValue.toJSON` / `getValue` return `!oppositeValue()`, which reads the current `enabled` (`ReadonlySabloValue.java:131-147`). The value sent depends on when it is serialized.
3. When `enabled` changes, the listener only updates `savedOppositeOfValue` (`ReadonlySabloValue.java:87-102`). It never calls the `IChangeListener` received in `attachToBaseObject` (which it ignores) and never marks `readOnly` changed. So an `enabled` change never re-sends `readOnly`.
4. Why only in a tab panel: `WebFormUI.setParentContainer(WebFormComponent)` (`WebFormUI.java:650-666`) runs only for forms shown inside a container (`WebFormComponent.updateVisibleForm`). It calls `getController().pushParentReadOnly(...)`, then `WebFormController.applyReadOnly`, then `WebFormUI.setReadOnly`, then `propagatePropertyToAllComponents(READONLY, …)` (`WebFormUI.java:607-630`). That does `component.setProperty("readOnly", new ReadonlySabloValue(...))` on every component.
   - The value moves from the template/defaults into the component's `properties` map. sablo's `setProperty` "new property" branch marks it changed (`BaseWebObject.java:681-689`).
   - So `readOnly` is serialized with the form's component data. That send happens after `onShow` has set `enabled=false`, so `readOnly=true` goes to the client.
   - A form opened directly never goes through `setParentContainer(WebFormComponent)`. `readOnly` stays at the template value (`false`, from `ReadonlyPropertyType.toTemplateJSONValue`, computed from design-time `enabled=true`) and is never sent with the stale value. That is why the direct case works.
5. The button sets `enabled=true`. The client receives `enabled=true`:
   - the template binding `[disabled]="!enabled() || !editable()"` (`check.html`) removes the DOM attribute;
   - `basecomponent.ts` `svyOnChanges` also removes it through the renderer;
   - `readOnly` is never re-sent and stays `true`.
6. The click guard `(!this.readOnly() && this.enabled()) || this.findmode()` (`check.ts:54`, and `radio.ts:53` without findmode) is false, so `itemClicked`, `pushUpdate` and `onActionMethodID` are skipped. The native checkbox still toggles visually because it is no longer DOM-disabled. This matches the evidence exactly: visual toggle, no dataprovider change, no events, no JS error.
7. Old tab not affected: it had received `readOnly=false` from a load without the `onShow` disable, and nothing re-sends it.

Hypotheses ruled out:
- **Client signal/DOM desync on `enabled`.** `enabled()` is correct. The stale signal is `readOnly()`, and it is stale because the server never sent an update. Not a client-side signal or buffering bug in `ServoyBaseComponent.ngOnChanges`.
- **Server rejecting the push.** No push is ever sent, because the client guard blocks it first. A push would in any case pass `checkForProtectedPropertiesThatMightBlockUpdatesOn`, since `enabled` is true on the server by then.
- **`NGEnabledSabloValue` / `flagChanged` ordering.** `enabled` itself reaches the client correctly (the DOM attribute is removed).

The confidence is high, but not yet confirmed by watching websocket frames. That check, or a unit test asserting `readOnly` is re-sent after `enabled` changes, is the first step of implementation.

## Ticket premise check
The premise (checkbox/radio don't update after being re-enabled in a tab panel) is correct. The real scope is broader:
- It is not specific to checkbox/radio UI code. It is a server property-sync bug in the `readOnly` (`oppositeOf`) property type.
- It affects every component with a `readOnly` type property whose `oppositeOf` prop changes after the form was sent with `readOnly` in its properties map:
  - `combobox` stays `[disabled]` because its template uses `readOnly()`;
  - `editable`-based fields (textarea, password, spinner, …) can stay `readonly` after `editable` is toggled, because `basefield.ts` recomputes the `readonly` attribute from the stale `readOnly()`.
- It is not a regression from the 2026 signal migration. The `readOnly` guard has been in `check.ts` since 2020 (3bb3d900c7). The missing change-marking has been in `ReadonlySabloValue` since 2015 (5d31ea5b5). 2024.3 is equally affected.

## Approaches considered
1. **Server fix in `ReadonlySabloValue` (recommended).** Keep the `IChangeListener changeMonitor` passed to `attachToBaseObject`. Call `changeMonitor.valueChanged()` from the `oppositeOf` property-change listener, so `readOnly` is re-sent whenever `enabled`/`editable` changes.
   - Pros: fixes the root cause for all `readOnly`-type components and both `oppositeOf` variants. Small and local.
   - Cons: an extra `readOnly` key goes out with each `enabled`/`editable` change on those components (negligible). It must still work correctly during find-mode toggles (`findModeChange`). Marking changed there is harmless, because `toJSON` reflects the current state.
2. **Client-only workaround.** Drop `readOnly()` from the click guards in `check.ts` / `radio.ts` (for these components `readOnly` is just `!enabled`).
   - Pros: trivial.
   - Cons: masks the server bug. Combobox and the `editable`-based components stay broken. It also diverges from the intended controller-readOnly semantics.
3. **Avoid putting `readOnly` into the properties map in `propagatePropertyToAllComponents` when the value doesn't actually change.**
   - Cons: narrows the trigger but leaves the underlying staleness. Any real `controller.readOnly` toggle followed by an `enabled` change would still desync.
4. **No code change needed.**
   - Rejected. The behaviour is a genuine defect with a legitimate user flow. The only user workaround is avoiding `enabled` toggles in `onShow` of tab-panel forms, which is not acceptable for a Critical customer case.

## Recommendation
Fix in this repo: `servoy_ngclient/src/com/servoy/j2db/server/ngclient/property/types/ReadonlySabloValue.java`.
- In `attachToBaseObject`, store `changeMonitor`.
- In the `oppositeOf` `PropertyChangeListener`, call `changeMonitor.valueChanged()` (guard null) in addition to updating `savedOppositeOfValue`, so `readOnly` is re-sent after `enabled`/`editable` changes.
- In `detach`, clear it.
- Add a unit test in `servoy_ngclient.tests`:
  - set up a component with `readOnly` (`oppositeOf` `enabled`) in its properties map;
  - set `enabled=false`, write changes, then set `enabled=true`;
  - assert the change JSON contains `readOnly:false`.
- Re-verify the live repro (fresh tab, `onShow` disable, enable button, click propagates to radio).

No change is needed in servoy-eclipse `check.ts` / `radio.ts`.

## Git history findings
- `ReadonlySabloValue` was introduced in 5d31ea5b5 (2015-03-31, SVY-8024 "implemented readOnly property type that works like in the other clients"). The listener and `toJSON(!oppositeValue())` date from then (plus 24df2e004 for the rename to generic `oppositeOf`). It has never marked itself changed.
- 391aac8a3 (2015-07, SVY-8464) added the `findModeChange` skip to the listener.
- c1b932b76 (2015-09, SVY-8398) added propagation through `ComponentTypeSabloValue`.
- 778ef2c04 (2025-07-07, Johan): "fix the class cast exception". The listener now reads `webObjectContext.getProperty(oppositeOf)` instead of `evt.getNewValue()`, because `EnabledPropertyType` delivers an `EnabledSabloValue`. This shows the listener does fire for `enabled` changes. It still only updates the saved value.
- 14193d31a (2025-08-01, SVY-20451) removed `equals`/`hashCode` from `ReadonlySabloValue`. Not causal: the "new property" branch of `setProperty` marks it changed regardless, so 2024.3 is affected too.
- `oppositeOf: "enabled"` on check/radio has been in place since 2015 (`git log -G`: e31911343, 1d46f51c8, 5d31ea5b5). Nothing changed there in 2024–2026 apart from EOL normalization (ebcb17651).
- `WebFormUI.setParentContainer` → `pushParentReadOnly` and `NGEnabledSabloValue.flagChanged`: cfb0c685d (2015-10, SVY-8771 "tabs in tab panel doesnt get disabled, flag enabled changed when form is added to the parent container"). 2024+ changes to `NGEnabledSabloValue` (8d1d2f07f SVY-19985, 9ebb7458e SVY-20387, 8d9cc6a8d/f131e9823 SVY-20257) concern the enabled dataprovider and attach order, not `readOnly`.
- Client, servoy-eclipse `check.ts:54`:
  - the `!readOnly && enabled` guard dates from 3bb3d900c7 (2020-12-22, SVY-15556);
  - e68e95c9c5 (2023-11-16, SVY-18673) added `|| findmode`;
  - 1d58324bc0 (2026-08-05) only converted to signals.
  - `radio.ts` has the same guard without findmode.
