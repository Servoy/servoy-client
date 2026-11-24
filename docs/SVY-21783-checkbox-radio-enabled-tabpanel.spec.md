# Spec: SVY-21783 — Default Checkboxes and Radio Buttons Do Not Update Correctly When Dynamically Enabled in a Tab Panel (TiNG)

Status: In Progress · Priority: Critical · Repo: servoy-client (`servoy_ngclient`, `servoy_ngclient.tests`)
Triage: `docs/SVY-21783-triage.md` (verdict PROCEED, approach 1 approved by the user).

## 1. Goal
Make the server re-send the derived `readOnly` property of NG components whenever the property it mirrors (`enabled` or `editable`, via `oppositeOf`) changes. After a form shown in a tab panel has its checkbox/radio disabled in `onShow` and later re-enabled, clicking them must again update the dataprovider and fire events, exactly like a form opened directly.

## 2. Background

### 2.1 Reported behaviour and reproduction evidence
- Reported on 2024.3.12 LTS, TiNG. Reproduced live on master (`http://localhost:8183/solution/double_window_test#sdds`, fresh browser tab).
- Setup: form `sdds` with a tab panel holding 1 tab. The embedded form has `chk_test` (servoydefault check) and `rad_test` (servoydefault radio) bound to the same dataprovider, and a button that sets both `enabled = true`. The embedded form's `onShow` sets both `enabled = false`.
- Result:
  - After load both controls are disabled and checked.
  - After the button, the DOM `disabled` attribute is gone from both.
  - Clicking the checkbox toggles it visually, but the radio does not follow: nothing reached the server, no events, no JS errors.
- Without the `onShow` disable it works. An old tab loaded before the `onShow` code did not show the bug; it needs a fresh form send.
- Jira: one customer comment (ETA request), no attachments, no linked issues.

### 2.2 Root cause
A stale `readOnly` on the client, caused by `ReadonlySabloValue` never marking itself changed.

1. servoydefault `check.spec:22` / `radio.spec:22` (`servoy_ngclient/war/servoydefault/...`) declare `"readOnly": { "type": "readOnly", "oppositeOf": "enabled" }`. `combobox` also uses `oppositeOf: "enabled"`. Eight other servoydefault components use `oppositeOf: "editable"`.
2. `ReadonlySabloValue.toJSON` / `getValue` (`servoy_ngclient/src/com/servoy/j2db/server/ngclient/property/types/ReadonlySabloValue.java:131-147`) write `!oppositeValue()`, i.e. the current `!enabled`. The value sent depends on when it is serialized.
3. `attachToBaseObject(IChangeListener changeMonitor, IWebObjectContext)` (`:63-85`) ignores `changeMonitor`. The listener added in `addOppositeOfListener()` (`:87-102`) only updates `savedOppositeOfValue`. It never marks `readOnly` changed, so an `enabled` change never re-sends `readOnly`.
4. Only forms in a container hit this. `WebFormUI.setParentContainer(WebFormComponent)` (`WebFormUI.java:650-666`) calls `getController().pushParentReadOnly(...)`. That goes through `WebFormController.applyReadOnly` and `WebFormUI.setReadOnly`, then `propagatePropertyToAllComponents(READONLY, …)` (`WebFormUI.java:607-630`), which does `component.setProperty("readOnly", new ReadonlySabloValue(...))` via `ReadonlyPropertyType.toSabloComponentValue(Object rhinoValue, ...)` (`ReadonlyPropertyType.java:124-132`).
   - sablo's `BaseWebObject.setProperty` "new property" branch marks the value changed. `readOnly` is then serialized with the form data, after `onShow` set `enabled=false`, so `readOnly=true` reaches the client.
   - A directly opened form keeps the template value (`ReadonlyPropertyType.toTemplateJSONValue`, computed from design-time `enabled=true`, so `false`) and never sends a stale one.
5. The button sets `enabled=true`. The client gets `enabled=true` and the DOM `disabled` goes away, but `readOnly` stays `true`.
6. The client click guard `(!this.readOnly() && this.enabled()) || this.findmode()` (servoy-eclipse `check.ts:54`; `radio.ts:53` has the same guard without findmode) blocks `pushUpdate` and the action handler. The native input still toggles visually. This matches the evidence exactly.

### 2.3 How bootstrapcomponents avoid it, and why we do not copy that
- bootstrapcomponents' checkbox declares `readOnly` as `{"type":"protected","blockingOn":true,"default":false,...}` (19323c1, 2017, SVY-11009).
  - That value is not derived from `enabled`, so it cannot go stale.
  - It is set only by `controller.readOnly` / the parent readOnly.
- The user explicitly chose the server fix instead of changing the servoydefault specs, for three reasons:
  - Changing `check`/`radio`/`combobox` specs from `readOnly` type to `protected` changes semantics: the `oppositeOf` save/restore of `enabled` on controller readOnly toggles, and the find-mode integration.
  - It would fix only the components whose spec is edited. The `editable`-based components (textarea, password, spinner, …) would stay broken.
  - The defect is in the property type, so the fix belongs there.

## 3. Design

### 3.1 Change in `ReadonlySabloValue`
- Add a field `private IChangeListener changeMonitor;`.
- In `attachToBaseObject(IChangeListener changeMonitor, IWebObjectContext webObjectCntxt)`, store `this.changeMonitor = changeMonitor;` first, before any other logic.
- In the `oppositeOfListener.propertyChange`:
  - keep the existing `findModeChange` early return for the `savedOppositeOfValue` update;
  - call `changeMonitor.valueChanged()` (null-guarded), so `readOnly` is serialized again with its current `!oppositeValue()`.

  Recommended shape:
  ```java
  public void propertyChange(PropertyChangeEvent evt)
  {
      if (changeMonitor != null) changeMonitor.valueChanged();
      // ignore the change when this is a find mode change.
      if (findModeChange) return;
      savedOppositeOfValue = (boolean)webObjectContext.getProperty(configuration.getOppositeOf());
  }
  ```
  Marking changed before the find-mode return is deliberate (see 3.2).
- In `detach()`:
  - after removing the listener, set `oppositeOfListener = null;`, `changeMonitor = null;`, `webObjectContext = null;`;
  - this matches `FindModeSabloValue.detach()` (`FindModeSabloValue.java:91-93`);
  - after detach, `oppositeValue()` already falls back to `true` when `webObjectContext == null`.
- Keep `toJSON`, `getValue`, `getOldOppositeOfValue`, the constructors and `setFindModeChange` unchanged.
- This is the same smart-value contract other types in `property/types` follow: store the monitor on attach, call `valueChanged()` when the client-visible value changes, clear it on detach. Examples: `FormatTypeSabloValue.java:248` (`if (changeMonitor != null) changeMonitor.valueChanged();`), `FindModeSabloValue.java:152`, `DataproviderTypeSabloValue`.

### 3.1b Hook in `NGEnabledSabloValue.flagChanged` (added after code review)
The `ReadonlySabloValue` listener does **not** fire for `enabled` changes. `NGEnabledPropertyType.wrap(newValue, oldValue, …)` mutates the existing `EnabledSabloValue` (`setEnabled`) and returns the same reference. So sablo `BaseWebObject.setProperty` sees `oldWrappedValue == wrappedValue` and never calls `firePropertyChange`. A runtime probe confirmed this: after enable the change set was only `{"enabled":true}`. The listener does fire for `editable` (`protected` type, not a wrapper), so the 3.1 change stays for that case.

Every `enabled` change is marked through `EnabledSabloValue.setEnabled` → `flagChanged(comp, propName)`, which `NGEnabledSabloValue` overrides. The override recurses into container children (sablo) and into visible forms of a `WebFormComponent`. The extra marking goes in that override:
- In `NGEnabledSabloValue.flagChanged(BaseWebObject comp, String propName)`, after `super.flagChanged(...)`:
  - for each `PropertyDescription` of `comp` whose type is `ReadonlyPropertyType`;
  - whose config is a `ReadonlyConfig` with `getOppositeOf()` equal to `propName`;
  - call `comp.markPropertyAsChangedByRef(pd.getName())`.
- Only do this when the readOnly property is actually in the component's properties map (`comp.getProperties().content.containsKey(name)`). A default/template-only value was never sent with a stale value, so there is nothing to re-send.
- Do not change the recursion. Children reached through `super.flagChanged` and visible forms go through this same overridden method, so parent and tab-panel `enabled` changes are covered too (`ReadonlySabloValue.oppositeValue()` reads `getProperty("enabled")`, which includes the parent state).

### 3.2 Find mode and edge cases
- **Find mode.** `FindModeSabloValue.findModeChanged` (`FindModeSabloValue.java:97-154`) wraps its `setProperty` calls in `setFindModeChange(true/false)`, so `savedOppositeOfValue` is not overwritten by the find-mode values (SVY-8464 behaviour, kept).
  - Marking `readOnly` changed during that window is harmless and correct: `toJSON` reflects the current `enabled`/`editable`, which is what the client must see.
  - The find-mode property itself already calls `changeMonitor.valueChanged()`, so the extra key costs nothing in round trips.
- **Null changeMonitor.** All calls are guarded. A value that has been created but not yet attached, or that is already detached, is a no-op.
- **Infinite loops / re-entrancy.** In `attachToBaseObject`, `setOppositeValue(...)` (`:75`, `:80`) runs before `addOppositeOfListener()` (`:69`, `:83`), so the new listener is not yet registered and does not fire for those writes.
  - `valueChanged()` only flags the property in sablo's change set. It does not set `enabled`/`editable`, so no feedback loop is possible.
  - Even if a listener from an older, already-detached value were still registered, it would only flag a change. That old value removes its listener in `detach()`, which sablo calls when the property is replaced.
- **Detach / re-attach.** `WebFormUI.propagatePropertyToAllComponents` replaces the value with a new `ReadonlySabloValue`. sablo detaches the old one (listener removed, monitor cleared) and attaches the new one (monitor stored, listener added). The new instance carries `savedOppositeOfValue` through `previousComponentValue.getOldOppositeOfValue()` (`ReadonlyPropertyType.java:131`). There is no double listener.
- **`controller.readOnly` / parent readOnly.** Unchanged:
  - `readOnly=true` sets `enabled=false` and saves the previous value;
  - `readOnly=false` restores `savedOppositeOfValue`.
  - Both go through `setProperty` of a new value, which sablo already marks changed.
  - Later `enabled` changes made by user code now also re-send `readOnly`, which keeps client and server consistent.
- **`RuntimeWebGroup` / `ComponentTypeSabloValue`.** These also create `ReadonlySabloValue` instances (`RuntimeWebGroup.java:240-242`, `ComponentTypeSabloValue.java:444-452`). They get the fix for free through the same attach path. No changes are needed there.
- **Traffic.** On components with a `readOnly` property, each `enabled`/`editable` change now sends one extra boolean key. This is negligible.
- **Spotbugs.** The new field is a plain instance field, accessed on the event thread like the existing ones. No new high-severity findings are expected. Verify after the build.

### 3.3 Affected components
- servoydefault `check`, `radio`, `combobox` (`oppositeOf: "enabled"`).
- servoydefault components with `oppositeOf: "editable"` (textarea, password, spinner, checkgroup, radiogroup, …; see `SpecGenerator.java:500-593` for the mapping). They also had a stale `readOnly` after `editable` toggles.
- Any third-party/custom component spec that uses the `readOnly` type.
- Not affected: bootstrapcomponents and other packages that use `protected` for `readOnly`.

### 3.4 Git history
`git blame -L 63,102` on `ReadonlySabloValue.java` (confirmed):
- 5d31ea5b5 (georgeganea, 2015-03-31, SVY-8024 "implemented readOnly property type"): the attach logic, the listener and `toJSON(!oppositeValue())`. The listener has never marked the value changed.
- 24df2e004 (2015-04-01): rename to the generic `oppositeOf` (`:67`, `:75`, `:80`).
- 391aac8a3 (jcompagner, 2015-07-27, SVY-8464): the `findModeChange` early return (`:95-96`). Kept as is.
- ba3801981 (Andrei Costescu, 2017-06-19): moved to the `IWebObjectContext` attach signature (`:64`, `:66`, `:101`). The `changeMonitor` parameter was ignored then too.
- 778ef2c04 (Johan Compagner, 2025-07-07, "fix the class cast exception"): the listener reads `webObjectContext.getProperty(oppositeOf)` instead of `evt.getNewValue()` (`:97`). This proves the listener fires on `enabled` changes. Keep this read.

From triage (not re-verified):
- 14193d31a (2025-08-01, SVY-20451) removed `equals`/`hashCode`. It is not causal.
- c1b932b76 (2015-09, SVY-8398) added the `ComponentTypeSabloValue` propagation.
- cfb0c685d (2015-10, SVY-8771) added `setParentContainer` → `pushParentReadOnly` and the enabled `flagChanged`.
- The client guard in servoy-eclipse `check.ts`:
  - dates from 3bb3d900c7 (2020, SVY-15556);
  - e68e95c9c5 (2023, SVY-18673) added `|| findmode`;
  - 1d58324bc0 (2026-08) only converted it to signals.

The bug is not a regression: 2024.3 is equally affected.

## 4. Implementation plan
1. `servoy_ngclient/src/com/servoy/j2db/server/ngclient/property/types/ReadonlySabloValue.java`. Use eclipse-coder tools only.
   1. Add the field `private IChangeListener changeMonitor;`. `IChangeListener` is already imported.
   2. In `attachToBaseObject`, assign `this.changeMonitor = changeMonitor;` as the first statement.
   3. In the listener's `propertyChange`, add `if (changeMonitor != null) changeMonitor.valueChanged();` before the `findModeChange` return.
   4. In `detach()`, inside or after the existing removal, set `oppositeOfListener = null; changeMonitor = null; webObjectContext = null;`.
   5. Do not add code comments beyond the existing one (repo convention).
1b. `servoy_ngclient/src/com/servoy/j2db/server/ngclient/property/types/NGEnabledSabloValue.java`: implement 3.1b in `flagChanged`. Keep it minimal, using eclipse-coder tools only.
2. Run `tools["eclipse-ide"].getCompilationErrors` for `servoy_ngclient` and apply quick fixes if needed. Check Spotbugs on the file.
3. Test phase (test-gen): add `ReadonlySabloValueTest` in `servoy_ngclient.tests/src/test/java/com/servoy/j2db/server/ngclient/property/` (package `com.servoy.j2db.server.ngclient.property`).
   - Extend `AbstractSolutionTest`. It loads the `war/servoydefault/` specs, so the real `check.spec` with `readOnly`/`oppositeOf: enabled` is available.
   - Follow the pattern in `PersistFieldInstanceTest.testSettingTextOfTabInTabpanel` (`:160-191`):
     - `solution.getForm("test")`;
     - `DataAdapterList dal = new DataAdapterList(new TestFormController(form, client))`;
     - create a `Field` with `field.setDisplayType(Field.CHECKS)` (see `DataAdapterListTest.java:105-106` for `createNewField`);
     - `FormElementHelper.INSTANCE.getFormElements(form.getAllObjects(), new ServoyDataConverterContext(client))`;
     - `WebFormComponent wc = ComponentFactory.createComponent(client, dal, formElements.get(0), null, form)`.
   - Scenario A (the bug):
     - put a `ReadonlySabloValue` in the properties map the same way `WebFormUI.propagatePropertyToAllComponents` does: `wc.setProperty("readOnly", ReadonlyPropertyType.INSTANCE.toSabloComponentValue(Boolean.FALSE, (ReadonlySabloValue)wc.getProperty("readOnly"), pd, wc))`, with `pd = wc.getFormElement().getWebComponentSpec().getProperty("readOnly")`;
     - `wc.setProperty("enabled", false)`, then `wc.getAndClearChanges()`;
     - `wc.setProperty("enabled", true)`, then `changes = wc.getAndClearChanges()`;
     - serialize with `JSONUtils.writeChanges(changes.content, changes.contentType, new BrowserConverterContext(wc, PushToServerEnum.allow))`;
     - assert the JSON contains `"readOnly":false` (use `JSONObject` lookup or `JSONAssert` non-strict, since `enabled` is in the change set too).
     - Before the fix this fails because `readOnly` is absent.
   - Scenario B: same flow with `enabled` going `true` → `false`. Assert `"readOnly":true` is in the changes.
   - Scenario C (find mode flag): call `((ReadonlySabloValue)wc.getProperty("readOnly")).setFindModeChange(true)`, change `enabled`, reset the flag. Assert:
     - `readOnly` is in the changes;
     - `getOldOppositeOfValue()` is unchanged (find-mode skip preserved).
   - Scenario D (detach): replace `readOnly` with a new value, which detaches the old one. Change `enabled` and assert there is no exception, and that the old instance's listener no longer fires (its `getOldOppositeOfValue()` is unchanged).
   - Scenario E (controller readOnly toggle still works):
     - set `readOnly` via the rhino conversion to `true`; assert `enabled` is `false`;
     - then to `false`; assert `enabled` is restored to its prior value.
   - If `enabled` on a servoydefault component goes through `NGEnabledSabloValue` and needs a parent/form context, set it through the same API the runtime uses (`wc.setProperty("enabled", Boolean.valueOf(...))`) and assert via `wc.getProperty`. Adjust only the setup, not the production code.
   - Run with `tools["eclipse-ide"].runClassTests` (plain unit test, no OSGi). Also run the existing `PersistFieldInstanceTest` and `DataAdapterListTest` for regressions.
4. Optional manual check (reviewer/QA):
   - re-run the live repro in a fresh tab: `onShow` disable, enable button, then click the checkbox; the radio must follow and `onAction`/`onDataChange` must fire;
   - in the websocket frames, `readOnly:false` arrives with `enabled:true`.
5. Commit: `SVY-21783 re-send readOnly when its oppositeOf property changes [ai]`.

## 5. Acceptance criteria
- [ ] `ReadonlySabloValue` stores the `IChangeListener` from `attachToBaseObject`. It calls `valueChanged()` (null-guarded) whenever the `oppositeOf` property changes, and clears the monitor, listener and context on `detach()`.
- [ ] After `enabled` changes on a component whose `readOnly` value is in the properties map, the next change set contains `readOnly` with the value `!enabled`.
- [ ] Same for `oppositeOf: "editable"` components.
- [ ] Find-mode toggles still do not overwrite `savedOppositeOfValue`, and leaving find mode restores the previous `enabled`/`editable`.
- [ ] `controller.readOnly = true/false` and the parent-container readOnly push behave exactly as before.
- [ ] No infinite loop or `StackOverflowError` during attach. No exception after detach.
- [ ] Live repro: a form in a tab panel with `onShow` disabling `chk_test`/`rad_test`, then re-enabled by a button. Clicking the checkbox updates the dataprovider (the radio follows) and fires events.
- [ ] New `ReadonlySabloValueTest` passes, and fails without the production change (scenario A).
- [ ] `servoy_ngclient` compiles cleanly with no new high-severity Spotbugs findings. Existing `servoy_ngclient.tests` stay green.

## 6. Out of scope
- Changing the servoydefault `check`/`radio`/`combobox` (or any other) `.spec` files, for example switching `readOnly` to `protected` as bootstrapcomponents do.
- Client-side TypeScript changes in servoy-eclipse (`check.ts`, `radio.ts`, `basefield.ts`, `basecomponent.ts`).
- bootstrapcomponents or other component packages.
- LTS / release backports (2024.3.x): to be decided separately after the master fix is reviewed.
- Changes to `WebFormUI.propagatePropertyToAllComponents`, `setParentContainer` or `FindModeSabloValue`. `NGEnabledSabloValue.flagChanged` is in scope, limited to 3.1b.
- Changes in the sablo repo.
- The pre-existing stale `savedOppositeOfValue` for `enabled` (the listener not firing means `controller.readOnly` restore uses an older value). Track it separately if needed.

## 7. Open questions
| # | Question | Proposed default | Owner |
|---|----------|------------------|-------|
| 1 | Should `readOnly` also be marked changed while `findModeChange` is true, or should the whole listener stay skipped in find mode? | Mark changed (the value reflects the current state; it is harmless and avoids a stale client value after find mode). Only the `savedOppositeOfValue` update is skipped. | Dev / reviewer |
| 2 | Does sablo add a `readOnly` value that lives only as a default (not in the properties map) to the change set when `valueChanged()` is called? | Not required for this bug: the stale value only exists once `readOnly` is in the properties map. Verify in the unit test that a default-only value does not throw. | Dev |
| 3 | Backport to 2024.3 LTS (customer version)? | Decide after review; the fix is small and self-contained. | Product / user |
| 4 | Is websocket-frame confirmation of the root cause needed before merge? | The unit test (scenario A fails before, passes after) plus the live repro is enough. | Reviewer |
