# Spec: SVY-21325 — Make plugin loading resilient to individual plugin failures

## 1. Goal

When a single plugin fails to load (e.g., due to `javax.servlet` incompatibility with Tomcat 11's `jakarta.servlet`), the system should log a clear error for that specific plugin but still successfully create and return the converter/validator managers populated with converters from the remaining working plugins. A failing plugin must NOT prevent the entire converter manager infrastructure from being created.

## 2. Background

### 2.1 Current initialization flow

1. `AbstractApplication.createPluginManager()` (or equivalent in `J2DBClient`) calls `pluginManager.initClientPlugins(application, app)`
2. `initClientPlugins()` calls `loadClientPlugins(application)` which:
   - Calls `loadClientPluginDefs()` → `getExtensionsForClass(IClientPlugin.class)` (line 195)
   - `getExtensionsForClass()` iterates a `ServiceLoader<IPlugin>` using a for-each loop (line 201)
   - After plugin loading completes, creates the managers at lines 453-455:
     ```java
     columnConverterManager = new ConverterManager<IColumnConverter>();
     uiConverterManager = new ConverterManager<IUIConverter>();
     columnValidatorManager = new ColumnValidatorManager();
     ```
   - Calls `checkAllPluginsForConvertersAndValidators()` to register converters from loaded plugins
3. The caller then passes these managers to `FoundSetManager.setColumnManangers(...)`

### 2.2 Root cause — ServiceLoader for-each loop does not catch errors from iterator advancement

In `getExtensionsForClass()` (line 195), the ServiceLoader is iterated with a for-each loop:

```java
ServiceLoader<IPlugin> pluginsLoader = ServiceLoader.load(IPlugin.class, getClassLoader());
for (IPlugin plugin : pluginsLoader)   // <-- iterator.next() happens HERE
{
    try
    {
        // ... process plugin ...
    }
    catch (ServiceConfigurationError e) // line 247
    {
        Debug.error("Cannot use a plugin contributed as a service:", e);
    }
}
```

The for-each loop desugars to:
```java
Iterator<IPlugin> it = pluginsLoader.iterator();
while (it.hasNext()) {
    IPlugin plugin = it.next();   // ServiceConfigurationError thrown HERE — OUTSIDE the try-catch
    try { ... } catch (ServiceConfigurationError e) { ... }
}
```

When a plugin's class cannot be instantiated (e.g., because it references `javax.servlet` which is not on the classpath), `ServiceLoader.Iterator.next()` throws `ServiceConfigurationError`. This error is thrown **outside** the try-catch block (which only wraps the loop body), so it propagates up through `getExtensionsForClass()` → `loadClientPluginDefs()` → `loadClientPlugins()`, preventing lines 453-456 from executing. The converter managers remain `null`.

Subsequently, `PluginManager.getColumnConverterManager()` returns `null`, `FoundSetManager.setColumnManangers()` is called with null, and any later call to `getColumnConverterManager().getConverter(...)` throws an NPE.

### 2.3 Secondary concern — `loadClientPlugins` has no outer protection

Even if the ServiceLoader issue is fixed, any unexpected `Throwable` from `loadClientPluginDefs()` or the fallback `getExtensions()` method (line 259) would similarly prevent the manager creation at lines 453-456. The manager creation should be resilient to any failure in the plugin discovery/loading phase.

## 3. Design

### 3.1 Fix 1: Resilient ServiceLoader iteration in `getExtensionsForClass()`

Replace the for-each loop over the `ServiceLoader` with an explicit iterator where both `hasNext()` and `next()` are wrapped in a try-catch for `ServiceConfigurationError`. This ensures that a single plugin failing to load does not abort iteration over the remaining plugins.

**File:** `servoy_shared/src/com/servoy/j2db/plugins/PluginManager.java`
**Method:** `getExtensionsForClass()` (line 195)
**Change:** Replace lines 200-251 (the for-each loop) with:

```java
ServiceLoader<IPlugin> pluginsLoader = ServiceLoader.load(IPlugin.class, getClassLoader());
Iterator<IPlugin> pluginIterator = pluginsLoader.iterator();
while (true)
{
    IPlugin plugin;
    try
    {
        if (!pluginIterator.hasNext()) break;
        plugin = pluginIterator.next();
    }
    catch (ServiceConfigurationError e)
    {
        Debug.error("Cannot load a plugin contributed as a service (skipping):", e);
        continue;
    }
    try
    {
        // ... existing plugin processing logic (lines 205-245) ...
    }
    catch (ServiceConfigurationError e)
    {
        Debug.error("Cannot use a plugin contributed as a service:", e);
    }
}
```

This ensures that if one plugin's class fails to load (e.g., `NoClassDefFoundError` for `javax.servlet` wrapped in `ServiceConfigurationError`), the iterator continues to discover and load remaining plugins.

### 3.2 Fix 2: Protect converter manager creation from plugin loading failures

Wrap the plugin loading phase in `loadClientPlugins()` with a try-catch so that even if an unexpected exception escapes from `loadClientPluginDefs()` or the plugin loading loop, the converter manager creation at lines 453-456 always executes.

**File:** `servoy_shared/src/com/servoy/j2db/plugins/PluginManager.java`
**Method:** `loadClientPlugins()` (line 401)
**Change:** Wrap the plugin loading block (lines 406-451) in a try-catch that logs the error but allows execution to continue to line 453:

```java
public void loadClientPlugins(IApplication application)
{
    synchronized (initLock)
    {
        loadedClientPlugins = new HashMap<String, IClientPlugin>();
        try
        {
            if (application == null || !application.isRunningRemote())
            {
                Extension<IClientPlugin>[] exts = loadClientPluginDefs();
                // ... existing loading logic ...
            }
            else
            {
                // ... existing settings-based loading ...
            }
        }
        catch (Throwable th)
        {
            Debug.error("Error occurred during plugin loading (some plugins may not be available):", th);
        }

        // Always create managers — even if some/all plugins failed to load
        columnConverterManager = new ConverterManager<IColumnConverter>();
        uiConverterManager = new ConverterManager<IUIConverter>();
        columnValidatorManager = new ColumnValidatorManager();
        checkAllPluginsForConvertersAndValidators();
    }
}
```

This guarantees that `getColumnConverterManager()`, `getUIConverterManager()`, and `getColumnValidatorManager()` will never return `null` — they will at minimum return empty managers.

### 3.3 Error logging

The error messages must:
- Clearly identify which plugin failed (class name or JAR if available)
- Include the root cause exception (so users see `javax.servlet` / `NoClassDefFoundError`)
- Indicate that the failing plugin is being skipped and the system continues

### 3.4 Fix 3: Update RELEASE-NOTES to reference Tomcat 11

The file `eclipse_build/build/server/RELEASE-NOTES` in the `build` repository (`c:\Users\vosti\git_2026.3\build`) still references Apache Tomcat 8.5.6. Since the actual bundled version is Tomcat 11.0.11 (per `eclipse_build/pom.xml`), the RELEASE-NOTES must be replaced with the correct Tomcat 11 release notes content. This is critical because the outdated file misleads users about which servlet API version (`jakarta.servlet` vs `javax.servlet`) their plugins must target.

## 4. Implementation plan

1. **Modify `PluginManager.getExtensionsForClass()`** (line 195 in `servoy_shared/src/com/servoy/j2db/plugins/PluginManager.java`): Replace the for-each ServiceLoader loop with an explicit iterator that catches `ServiceConfigurationError` around both `hasNext()` and `next()`, logging the error and continuing iteration.

2. **Modify `PluginManager.loadClientPlugins()`** (line 401): Wrap the plugin discovery/loading block in a try-catch(Throwable) so that lines 453-456 (manager creation) always execute regardless of plugin loading failures.

3. **Add `java.util.Iterator` import** if not already present.

4. **Replace `build/eclipse_build/build/server/RELEASE-NOTES`** with the Tomcat 11.0.11 release notes content (matching the version in `eclipse_build/pom.xml`).

5. **Verify compilation** — no new errors should be introduced.

6. **Verify existing tests pass** — `servoy_ngclient.tests` should continue to pass since the happy path is unchanged.

## 5. Acceptance criteria

- [ ] A single plugin failing to load via ServiceLoader does NOT prevent other plugins from loading
- [ ] `getColumnConverterManager()`, `getUIConverterManager()`, and `getColumnValidatorManager()` never return null — at minimum they return empty managers
- [ ] The error for a failing plugin is logged with the plugin's class/JAR name and root cause exception
- [ ] Successfully loaded plugins still contribute their converters/validators to the managers
- [ ] `build/eclipse_build/build/server/RELEASE-NOTES` references Apache Tomcat 11.0.11 (not 8.5.6)
- [ ] No compilation errors introduced
- [ ] Existing tests in `servoy_ngclient.tests` continue to pass
- [ ] When all plugins load correctly, behavior is identical to before (no functional change in the happy path)

## 6. Files modified

| File | Repository | Change |
|------|------------|--------|
| `servoy_shared/src/com/servoy/j2db/plugins/PluginManager.java` | `servoy-client` | Fix ServiceLoader iteration (Fix 1) and protect manager creation (Fix 2) |
| `eclipse_build/build/server/RELEASE-NOTES` | `build` | Replace Tomcat 8.5.6 release notes with Tomcat 11.0.11 release notes (Fix 3) |

## 7. Out of scope

- Adding null checks at `FoundSetManager.getColumnConverterManager()` call sites (unnecessary once managers are guaranteed non-null)
- Automatically retrying failed plugins or providing a plugin reload mechanism
- Migrating plugins from `javax.servlet` to `jakarta.servlet` (user responsibility)
