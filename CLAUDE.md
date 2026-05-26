# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Eclipse MCP Tools — Use These Instead of Generic Tools

This is a multi-project Eclipse/OSGi workspace. Always prefer Eclipse MCP tools to keep the Eclipse index, builder, and classpath in sync:

| Task | Eclipse MCP Tool |
|---|---|
| Read file | `eclipse-ide_readProjectResource` |
| Create/overwrite file | `eclipse-coder_createFile` or `eclipse-coder_replaceFileContent` |
| Edit file | `eclipse-coder_applyPatch`, `eclipse-coder_insertIntoFile`, `eclipse-coder_replaceString`, `eclipse-coder_deleteLinesInFile` |
| Search files/classes | `eclipse-ide_fileSearch`, `eclipse-ide_fileSearchRegExp`, `eclipse-ide_findFiles` |
| Git operations | `eclipse-git_*` tools |
| Run tests | `eclipse-ide_runAllTests`, `eclipse-ide_runClassTests`, `eclipse-ide_runTestMethod`, `eclipse-pde_runJUnitPluginTests` |

## After Any Code Modification

Always run the post-modification compile loop:
1. `eclipse-ide_getCompilationErrors()` — check build state
2. If errors exist, review the returned quick fixes
3. `eclipse-ide_executeQuickFix(markerId, proposalIndex)` — apply safe fixes
4. Re-verify compilation is clean

## Commit Message Convention

All AI-assisted commits must end with ` [ai]` in the subject line. If related to a Jira case, include the case number:
- `Fix NullPointerException during client initialization [ai]`
- `SERVOY-293 fix NPE in WAR export copyRequiredBundles [ai]`

## Build Commands

**Java (Maven/Tycho):**
```bash
# Build all runtime modules
mvn clean install

# Skip web build (faster)
mvn clean install -Dskip-web=true

# Generate API documentation
mvn clean install -Pdoc,design_doc

# Update OSGi version across all modules
mvn org.eclipse.tycho:tycho-versions-plugin:update-pom -DnewOSGIVersion=2026.7.0
```

**TypeScript/NG Client frontend:**
```bash
cd servoy_ngclient
npm install
npm run build          # compile TypeScript
npm test               # Karma/Jasmine tests (Chrome headless)
npm run test_dev       # watch mode (Chrome)
npm run test_dev_ff    # Firefox
npm run test_edge_nowatch  # Edge headless
```

**Java unit tests only:**
```bash
mvn test -DskipTests=false -f servoy_ngclient.tests/pom.xml
```

## Module Architecture

The repository contains 8 OSGi plugin modules with strict unidirectional dependencies:

```
servoy_ngclient     servoy_smart_client     servoy_headless_client     servoy_debug
        ↓                    ↓                          ↓                    ↓
        └──────────────── servoy_shared ────────────────┘
                                ↓
                          servoy_base
```

| Module | Role |
|---|---|
| `servoy_base` | Foundation: persistence, querying, Solution Model APIs. No UI dependencies. |
| `servoy_shared` | Central engine: Mozilla Rhino JS execution, database connectivity, serialization. Re-exports `servoy_base`. |
| `servoy_smart_client` | Java Swing desktop client (`com.servoy.j2db.smart.*`) |
| `servoy_headless_client` | Server-side execution without GUI; servlet integration; depends on `jsoup` + Jakarta Servlet |
| `servoy_ngclient` | Modern HTML5/Angular/WebSocket web client. Uses Sablo framework, Tomcat, Auth0/OAuth2, FreeMarker templates, LESS compilation. Has both Java backend (`com.servoy.j2db.server.ngclient.*`) and TypeScript/Angular frontend in `war/`. |
| `servoy_ngclient.tests` | OSGi Fragment bound to `servoy_ngclient`; JUnit tests + Karma/Jasmine frontend tests in `client_tests/` |
| `servoy_debug` | Eclipse SWT-based debugger integration for Servoy Developer |
| `servoy_doc` | Build-time API documentation generator (not runtime) |

**Package name conventions:**
- `com.servoy.base.*` → `servoy_base`
- `com.servoy.j2db.*` → `servoy_shared`
- `com.servoy.j2db.smart.*` → `servoy_smart_client`
- `com.servoy.j2db.server.headlessclient.*` → `servoy_headless_client`
- `com.servoy.j2db.server.ngclient.*` → `servoy_ngclient`

## Key Technical Details

- **Build system:** Maven + Tycho 4.0.12 (Eclipse plugin build); requires custom Maven repo at `https://developer.servoy.com/mvn_repository/`
- **Java version:** JavaSE-21
- **JavaScript engine:** Mozilla Rhino (server-side JS execution in `servoy_shared`)
- **WebSocket layer:** Sablo framework (`servoy_ngclient`)
- **OSGi bundle metadata:** Each module's `META-INF/MANIFEST.MF` defines exports, imports, and dependencies — changes here affect classpath resolution across the whole workspace
- **Test isolation:** `servoy_ngclient.tests` is a Fragment bundle (not a standalone plugin), giving it direct access to package-private members of `servoy_ngclient`

## Known Design Decisions — Do Not Change

### SecuritySupport: DESede with hardcoded passphrase (`background.gif`)

`servoy_shared/src/com/servoy/j2db/util/SecuritySupport.java` loads a DESede key from a JCEKS keystore embedded in the JAR as `background.gif`, using the hardcoded passphrase `"passphrase"`.

**This is intentional obfuscation, not a security vulnerability. Do not "fix" it.**

- The sole purpose is to prevent plain-text passwords from sitting visibly in `servoy.properties`. It is not intended to be cryptographically strong — the key being recoverable from the JAR is an accepted trade-off.
- The passphrase cannot be made external or configurable: `servoy.properties` is decrypted at startup before any external configuration is available, so there is no safe place to store a separate key.
- Changing the algorithm or key would silently break decryption for every existing customer installation whose `servoy.properties` contains passwords encrypted with the current scheme. There is no migration path that doesn't require every customer to re-enter all their credentials.
- Security scanners and audit tools (including automated ones) will flag this. That finding should be dismissed/suppressed — it has been reviewed and accepted.

Affected files: `SecuritySupport.java`, `background.gif` (both in `servoy_shared`).

### CryptUtils: legacy `createCipher` kept for backward compatibility

`servoy_shared/src/com/servoy/j2db/util/CryptUtils.java` contains a `@Deprecated` method `createCipher()` that uses MD5 key derivation and AES/ECB. It is intentionally kept because `fileDecryption()` falls back to it when decrypting solution files encrypted before the AES-256-GCM migration (format detected by the `SENC` magic prefix).

**Do not remove `createCipher()` until all legacy-format encrypted solution files in the field have been re-encrypted.** Once that migration is complete, the `decryptLegacy()` private method and `createCipher()` can both be deleted. The new encryption path (`fileEncryption()` → AES-256-GCM + PBKDF2) should be used for all new encryptions.

## SpotBugs

SpotBugs errors at the **two highest severity levels** are blocking. Proactively fix SpotBugs issues in any new or modified code.

## Licensing

All code must be compatible with GNU AGPL v3. Third-party code must use an open-source license other than GPL.
