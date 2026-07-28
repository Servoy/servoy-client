# Spec: SVY-21271 — Nested custom type creation doesn't work in properties view

## 1. Goal

Fix the properties view so that adding a new element to a nested custom type array (a custom type array that is itself a property of another custom type) correctly persists the new element. Currently, the user can add a nested custom type and set its properties in the UI, but after saving the editor the nested type is lost.

## 2. Background

### 2.1 Custom type arrays in Servoy components

Servoy web components can define custom object types in their `.spec` files. These types can be arranged in arrays (e.g., a data grid's `columns` property). Each custom type can in turn define nested custom type array properties (e.g., each column might have a `menuItems` sub-array, or a sidenav component's `menuItems` each have a nested `items` sub-array).

### 2.2 Properties view creation flow

When the user clicks the "+" button to add a new element to a custom type array in the properties view, the following chain executes:

1. `CustomArrayTypePropertyController.createNewElement` (com.servoy.eclipse.ui) — determines the parent key and UUID, calls the handler
2. `EditorComponentActionHandlerImpl.createComponent` (com.servoy.eclipse.designer) — wraps in `CreateOverrideIfNeeededCommandWrapper`, builds `CreateComponentCommand`
3. `CreateComponentCommand.createComponent` (com.servoy.eclipse.designer) — finds the parent persist via `PersistFinder.INSTANCE.searchForPersist(form, PersistIdentifier.fromSimpleUUID(uuid))`, then calls `AddContainerCommand.addCustomType`
4. `AddContainerCommand.addCustomType` (com.servoy.eclipse.designer) — resolves the property description from the spec, creates the `WebCustomType` instance via `WebCustomType.createNewInstance`

### 2.3 Top-level vs nested creation

For a **top-level** custom type array (direct property of a WebComponent, e.g., `columns`):
- `persistContext.getPersist()` = the WebComponent
- `PersistFinder.searchForPersist` finds the WebComponent by UUID (always indexed)
- `AddContainerCommand.addCustomType` uses `componentSpec.getProperty(propertyName)` to resolve the target PD
- Works correctly

For a **nested** custom type array (property of a WebCustomType that is itself inside an array, e.g., `columns[0].items`):
- `persistContext.getPersist()` = the intermediate WebCustomType (e.g., the column)
- `PersistFinder.searchForPersist` must find this WebCustomType by UUID
- `AddContainerCommand.addCustomType` uses `componentSpec.getDeclaredCustomObjectTypes().get(parentWebObject.getTypeName())` to resolve the target PD from the intermediate type
- **Fails** — the nested type is not persisted on save

### 2.4 Key classes

| Class | Project | Role |
|-------|---------|------|
| `CustomArrayTypePropertyController` | com.servoy.eclipse.ui | Properties view controller for custom type arrays |
| `CustomArrayTypePropertyController.CustomArrayPropertySource` | com.servoy.eclipse.ui | Property source for array elements |
| `PDPropertySource` | com.servoy.eclipse.ui | Property source backed by a PropertyDescription (used for custom type children) |
| `EditorComponentActionHandlerImpl` | com.servoy.eclipse.designer | Handles create/delete component actions from properties view |
| `CreateComponentCommand` | com.servoy.eclipse.designer | GEF command that creates components/custom types |
| `AddContainerCommand` | com.servoy.eclipse.designer | Static `addCustomType` method that instantiates WebCustomType |
| `PersistFinder` | com.servoy.eclipse.model | Finds persists by UUID/PersistIdentifier |
| `WebCustomType` | servoy_shared | Persist model for custom type instances |
| `CreateOverrideIfNeeededCommandWrapper` | com.servoy.eclipse.designer | Wraps commands to handle inherited form overrides |

## 3. Design

### 3.1 Root cause analysis

The root cause is that **`WebCustomType` does not override `addChild()`**, while `WebComponent` does.

When a property is set on a nested custom type via the properties view, the flow is:

1. `WebComponentPropertyHandler.setValue()` calls `bean.setProperty(name, value)` where `bean` is a `WebCustomType`
2. `WebCustomType.setProperty()` delegates to `PersistHelper.setWebComponentProperty(this, propertyName, val)`
3. `PersistHelper.setWebComponentProperty()` (line 1596-1608) detects the value is an `IChildWebObject[]` or `IChildWebObject` and calls `webComponent.addChild(child)` — but `webComponent` is a **WebCustomType**
4. This calls `AbstractBase.addChild()` which only adds the child to the in-memory list and fires events — **it does NOT update the parent's JSON**

The critical contrast: `WebComponent.addChild()` (line 340-387 in `WebComponent.java`) overrides `addChild()` to sync the child's `getFullJsonInFrmFile()` into the parent's JSON structure (either as an array element or a single object property). `WebCustomType` inherits only `AbstractBase.addChild()`, which has no JSON syncing logic.

Result: the nested custom type persist exists in memory but its JSON is never written into the parent WebCustomType's JSON. When the form is saved, the parent WebCustomType's JSON is serialized without the nested child — so it's lost.

### 3.2 Proposed fix

Override `addChild(IPersist obj, int index)` in `WebCustomType` with logic analogous to `WebComponent.addChild()`:

1. Call `super.addChild(obj, index)` to handle the in-memory list + events
2. If `obj instanceof WebCustomType customType`:
   - Get the current JSON via `getOwnProperty(StaticContentSpecLoader.PROPERTY_JSON.getPropertyName())`
   - If the property is an array-of-custom-type, get/create the JSONArray for `customType.getJsonKey()` and insert `customType.getFullJsonInFrmFile()` at the correct index
   - If the property is a single custom object, put `customType.getFullJsonInFrmFile()` directly under `customType.getJsonKey()`

This mirrors exactly what `WebComponent.addChild()` does and ensures the nested child's JSON is persisted in the parent's JSON structure.

## 4. Implementation plan

1. **Add `addChild(IPersist obj, int index)` override to `WebCustomType`** in `servoy-client/servoy_shared/src/com/servoy/j2db/persistence/WebCustomType.java`:
   - Mirror the logic from `WebComponent.addChild()` (lines 340-387)
   - Get the WebCustomType's own JSON, create if null
   - Check if the property (identified by child's `jsonKey`) is an array type using the WebCustomType's `propertyDescription`
   - Insert/append the child's `getFullJsonInFrmFile()` into the appropriate JSON structure

2. **Verify `propertyDescription` resolution for nested types**: In the `addChild` override, use `this.propertyDescription.getProperty(customType.getJsonKey())` to resolve the child property description (since `WebCustomType`'s `propertyDescription` describes its own properties, not the parent component's).

3. **Test with sidenav component**: Use the attached `testsidenav.servoy` solution to verify:
   - Add a nested item to a menu item's sub-items array
   - Set properties (id, text) on the nested item
   - Save the editor
   - Verify the nested item persists after reloading

## 5. Acceptance criteria

- [ ] Adding a nested custom type element via the properties view "+" button creates the element and it persists after editor save
- [ ] The nested custom type's properties (e.g., id, text) can be set and are saved correctly
- [ ] Undo/redo works correctly for nested custom type creation
- [ ] Top-level custom type creation (non-nested) continues to work as before
- [ ] The fix works for components with multiple levels of nesting (e.g., sidenav menuItems → items)
- [ ] The fix works on both non-inherited and inherited forms

## 6. Out of scope

- Drag-and-drop creation of nested custom types in the form designer (separate mechanism)
- Reordering of nested custom types (covered by `ReorderCustomTypesCommand`)
- Deletion of nested custom types (separate `deleteComponent` handler)

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Should the `addChild` override also handle `internalRemoveChild` / `removeChild` for nested types (delete scenario)? | Developer | open |
| Does `WebCustomType.createNewInstance()` constructor (which already manipulates JSON) conflict with the new `addChild` override? Need to ensure no double-insertion. | Developer | open |
