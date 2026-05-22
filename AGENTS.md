# Agent Guidelines for Servoy Runtime Codebase

Welcome, AI Agent! This repository contains the core **Servoy runtime code** (including Servoy Developer, Eclipse integration, plugins, extensions, and various web/smart/headless clients). To ensure safety, consistency, and proper integration with the Eclipse workspace environment, you must adhere strictly to the following developer and automation workflows.

---

## 1. Repository Projects Analysis

This Git repository contains **8 core projects/plugins** forming the Servoy runtime. Understanding their roles and relationships is crucial for making architectural and design-compliant edits:

### 1. `servoy_base`
- **Type:** Eclipse Plugin / OSGi Bundle (`eclipse-plugin`)
- **Main Role:** Base persistence, querying, and the Solution Model APIs.
- **Key Focus:** Defines the foundational data processing structures, querying frameworks (`com.servoy.base.query`), persistent object mappings, and solution models (`com.servoy.base.solutionmodel`).
- **Crucial Detail:** Does not contain any UI or platform-specific libraries. It forms the lowest-level layer of the application.

### 2. `servoy_shared`
- **Type:** Eclipse Plugin / OSGi Bundle (`eclipse-plugin`)
- **Main Role:** Shared runtime logic, database connectivity, and JavaScript scripting engine.
- **Key Focus:** Serves as the central engine shared between different clients/servers. Implements the script execution interface using Mozilla Rhino (`org.eclipse.dltk.javascript.rhino`), core database processing (`com.servoy.j2db.dataprocessing`), serialization, i18n, and plugin APIs.
- **Crucial Detail:** Re-exports `servoy_base`. It acts as the backbone of both headless, desktop (smart), and web (NG) clients.

### 3. `servoy_smart_client`
- **Type:** Eclipse Plugin / OSGi Bundle (Manifest-first)
- **Main Role:** Java Swing-based Desktop client (Smart Client).
- **Key Focus:** Contains the complete GUI desktop framework, layout managers, wizards, UI preferences, and desktop-specific plug-in classes (`com.servoy.j2db.smart.*`).
- **Crucial Detail:** Inherits from `servoy_shared` and depends heavily on Java Swing packages.

### 4. `servoy_headless_client`
- **Type:** Eclipse Plugin / OSGi Bundle (Manifest-first)
- **Main Role:** Headless client execution for server-side processing.
- **Key Focus:** Allows executing Servoy logic and calculations on the server without any graphical user interface. Contains servlet integrations and HTML/web-adapter structures.
- **Crucial Detail:** Integrates with `jsoup` for HTML parsing and depends on the `jakarta.servlet` specification to run inside servlet containers.

### 5. `servoy_ngclient`
- **Type:** Eclipse Plugin / OSGi Bundle (`eclipse-plugin`)
- **Main Role:** Next-Generation (NG) Web Client (HTML5 / Angular / WebSockets).
- **Key Focus:** The modern, main web client engine. Integrates with the Sablo framework for WebSocket communication, Tomcat server, auth0 JWT/OAuth APIs, freemarker template engines, and handle client components, properties, styles (LESS compiling), and client event loop.
- **Crucial Detail:** This is highly complex with rich external dependencies (e.g., Tomcat, ScribeJava, Tus upload, auth0).

### 6. `servoy_ngclient.tests`
- **Type:** OSGi Fragment Bundle
- **Main Role:** Unit tests for `servoy_ngclient`.
- **Key Focus:** Contains JUnit tests specifically verifying WebSocket messaging, properties, component specifications, and behaviour of the NG client.
- **Crucial Detail:** Set as `Fragment-Host: servoy_ngclient`, allowing direct access to package-private members in `servoy_ngclient`.

### 7. `servoy_debug`
- **Type:** Eclipse Plugin / OSGi Bundle (Manifest-first)
- **Main Role:** Debugger capabilities for Servoy developers.
- **Key Focus:** Implements debugging interfaces, layout extensions, and hooks integrated into the developer workspace environment.
- **Crucial Detail:** Specifically uses Eclipse SWT (`org.eclipse.swt`) and runtime (`org.eclipse.core.runtime`) to tie debugger UI with the Eclipse workspace.

### 8. `servoy_doc`
- **Type:** Standalone Maven Build Project (`jar`)
- **Main Role:** Documentation XML generator from source code.
- **Key Focus:** Aggregates source code across `servoy_base`, `servoy_shared`, `servoy_headless_client`, and `servoy_ngclient` to generate Servoy API documentation (such as `servoydoc.xml` and `servoydoc_jslib.xml`) using a headless Tycho Eclipse application.
- **Crucial Detail:** Not part of the runtime client, but part of the build-time SDK generation process.

---

## 2. Prioritize Eclipse MCP Tools Over Standard Tools

Since this workspace is a complex, multi-project Eclipse environment, **always prioritize Eclipse-specific MCP/PDE tools** over standard, general-purpose command-line or filesystem tools. This ensures that the Eclipse index, builder, and classpath are kept in sync.

- **File Reading:** Use `eclipse-ide_readProjectResource` instead of the generic `read` tool.
- **File Writing & Creating:** Use `eclipse-coder_createFile` or `eclipse-coder_replaceFileContent` instead of the generic `write` tool.
- **File Editing:** Use `eclipse-coder_applyPatch`, `eclipse-coder_insertIntoFile`, `eclipse-coder_replaceString`, or `eclipse-coder_deleteLinesInFile` instead of the generic `edit` tool.
- **File / Class Searching:** Use `eclipse-ide_fileSearch`, `eclipse-ide_fileSearchRegExp`, or `eclipse-ide_findFiles` instead of generic `grep` or `glob`.
- **Git Operations:** Use `eclipse-git_*` tools instead of standard shell `git` commands in `bash`.
- **Testing:** Prefer `eclipse-ide_runAllTests`, `eclipse-ide_runClassTests`, `eclipse-ide_runTestMethod`, or `eclipse-pde_runJUnitPluginTests` over generic shell test commands.

---

## 3. Commit Message Convention `[ai]`

To maintain clarity and transparency about the origin of codebase changes, any Git commit consisting primarily of AI-generated or AI-assisted changes must follow this rule:
- **The commit subject line must end with ` [ai]`** (case-insensitive, space followed by bracketed `ai`). Examples: `Fix NullPointerException during client initialization [ai]` or `Implement support for modern TLS protocols in server connection [ai]`
- **Commit messages for cases:** When a commit is related to a Jira case, the case number (e.g. `SVY-123`, `SVYX-456`, `SERVOY-293`) must be included in the commit subject line. Example: `SERVOY-293 fix NPE in WAR export copyRequiredBundles [ai]`

---

## 4. Post-Modification Compilation & Quick-Fix Loop

After making any code modifications or creating files using the Eclipse MCP tools, you must execute a self-verification compile loop:

1. **Check for errors:** Call `eclipse-ide_getCompilationErrors()` immediately to check the build state.
2. **Review quick fixes:** If any compilation errors are introduced or identified, look at the returned quick fixes list.
3. **Apply quick fixes:** If a quick fix is applicable and safe, immediately apply it using `eclipse-ide_executeQuickFix` by passing the corresponding `markerId` and `proposalIndex`.
4. **Re-check:** Verify compilation again to ensure the workspace is clean.

---

## 5. Spotbugs Error Resolution

Spotbugs is used to find bugs in Java code. You must pay special attention to Spotbugs issues:
- **Identify Spotbugs Errors:** Spotbugs errors of the **two highest severity levels** are treated as blocking errors.
- **Proactive Fixing:** Always try to fix these Spotbugs errors in any new or modified code to keep the codebase robust and clean.

---

*Thank you for keeping the Servoy Runtime codebase healthy, compilation-error free, and highly consistent!*
