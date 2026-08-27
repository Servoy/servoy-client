# Spec: SVY-21382 — Tenant propagation and ClientIdentityApplicator

## 1. Goal

Extract and centralize user identity application (user UID, name, permissions, tenants) into a shared utility `ClientIdentityApplicator` and unify the tenant value pipeline so it works consistently across all client types (NG, headless, REST, MCP).

## 2. Background

### 2.1 What was implemented

SVY-21382 introduced the following changes:

1. **`ClientLogin`** (`servoy_shared`) — Added a 6-argument constructor that accepts `String[] tenantValues`. The legacy 5-arg constructor delegates with `null` tenants.

2. **`FoundSetManager.getRawTenantValue()` / `setRawTenantValue(Object)`** (`servoy_shared`) — New field `rawTenantValue` stored by `setRawTenantValue()` (just stores, no filter application) and exposed via `getRawTenantValue()`. Also stored by `setTenantValue(Solution, Object)` when filters are applied.

3. **`IFoundSetManagerInternal`** (`servoy_shared`) — Added `setRawTenantValue(Object)` and `getRawTenantValue()` to the interface.

4. **`ApplicationServer.login()`** (`j2db_server`) — Extracts `getRawTenantValue()` from the authenticator's FoundSetManager, converts it to `String[]`, and passes it into the `ClientLogin` 6-arg constructor.

5. **`AuthenticatorManager.callAuthenticator()`** (`servoy_ngclient`) — If `login.getTenantValues() != null`, calls `builder.withTenants(login.getTenantValues())` to embed tenant values as a JWT array claim.

6. **`SvyTokenBuilder.withTenants()`** (`servoy_ngclient`) — Stores the tenant array in the JWT via `builder.withArrayClaim(SvyID.TENANTS, tenantsValue)`.

7. **`ClientIdentityApplicator`** (`servoy_shared`) — New shared utility class with a static `void applyIdentity(IApplication, String userUid, String userName, String[] permissions, Object[] tenants)` method. Sets user info on ClientInfo, stores raw tenant value on FoundSetManager via `setRawTenantValue()`.

8. **`BasicFormManager.applyTenantValue(Solution)`** (`servoy_shared`) — New protected method that reads `getRawTenantValue()` from FoundSetManager and calls `setTenantValue(solution, value)` to apply filters. Skips authenticator solutions. Called from both `FormManager.makeSolutionSettings()` and `NGFormManager.makeSolutionSettings()`.

9. **`NGClientWebsocketSession.setUserId()`** (`servoy_ngclient`) — Refactored to delegate to `ClientIdentityApplicator.applyIdentity()`. No inline tenant handling.

10. **`NGFormManager`** — Removed `tenantValue` field and `setTenantValue(Object)` method. Tenant state now lives on FoundSetManager.

11. **`INGFormManager`** — Removed `setTenantValue(Object)` from the interface.

12. **`FormManager.makeSolutionSettings()`** — Added `applyTenantValue(solution)` call so headless clients also get tenant support.

### 2.2 Design principles

- **Single responsibility:** `ClientIdentityApplicator` sets identity + stores raw tenant. `BasicFormManager.applyTenantValue()` applies tenants when the solution loads.
- **No duplication:** Tenant application logic lives in one place (`BasicFormManager.applyTenantValue`), called by both `FormManager` and `NGFormManager`.
- **Tenant state on FoundSetManager:** Raw tenant value is stored on FoundSetManager (where it belongs). The form manager has no tenant state — only a helper method to apply it when the solution loads.
- **Authenticator guard:** `applyTenantValue` skips authenticator solutions to avoid applying tenants during login.
- **`ClientState.authenticate()` is NOT changed:** It continues to set `userName`, `userUid`, and `userGroups` directly on `ClientInfo` — it does NOT call `ClientIdentityApplicator`. This is intentional: `authenticate()` is called on all client types (NGClient extends ClientState), and the NG stateless flow already calls `applyIdentity()` via `setUserId()`. Having `authenticate()` also call `applyIdentity()` would cause double application for any client type that validates a token and then triggers authenticate on the same client instance (e.g., future MCP or REST flows).
- **Explicit caller contract:** Any client type that needs tenant support must call `ClientIdentityApplicator.applyIdentity()` explicitly after token validation, **before the solution loads** (i.e., before `makeSolutionSettings()` runs). The call must happen only once per authentication cycle.

### 2.3 Integration guidance for future client types (REST, MCP)

To add tenant support for REST WS or MCP clients:

1. **Validate the JWT token** — extract `userUid`, `userName`, `permissions`, and `tenants` from the token claims.
2. **Call `ClientIdentityApplicator.applyIdentity(app, uid, name, perms, tenants)`** — this stores identity on `ClientInfo` and raw tenant on `FoundSetManager`.
3. **Load the solution** — `makeSolutionSettings()` will call `applyTenantValue(solution)` which reads the raw tenant and applies filters.

The critical constraint is **ordering**: `applyIdentity()` must be called before `makeSolutionSettings()`. If the solution is already loaded (e.g., a reused SessionClient), tenant filters will not be applied automatically — the solution would need to be reloaded, or `applyTenantValue()` would need to be triggered manually.

Do NOT call `ClientIdentityApplicator.applyIdentity()` from `ClientState.authenticate()` — this would cause double application for clients that also call `applyIdentity()` from a token path (NG `setUserId()`, or future MCP/REST token validation).

### 2.4 What is NOT in scope

- REST WS bearer token validation — future follow-up.
- MCP client authentication — future follow-up.
- `SvyTokenVerifier` utility — future follow-up.
- OAuth token endpoint — future follow-up.

## 3. Test Design

### 3.1 Test: `ClientLoginTest`

Location: `j2db_test/src/com/servoy/j2db/ClientLoginTest.java`

Verifies:
- 6-arg constructor stores tenant values correctly via `getTenantValues()`.
- 5-arg constructor sets tenants to `null` (backward compatibility).
- All other getters work correctly with both constructors.
- Empty array tenants are preserved.

### 3.2 Test: `FoundSetManagerRawTenantTest`

Location: `j2db_test/src/com/servoy/j2db/dataprocessing/FoundSetManagerRawTenantTest.java`

Verifies:
- `setRawTenantValue(value)` / `getRawTenantValue()` roundtrip.
- `setTenantValue(solution, value)` also stores `rawTenantValue`.
- Null, scalar, array, and overwrite scenarios.

### 3.3 Test: `ClientIdentityApplicatorTest`

Location: `j2db_test/src/com/servoy/j2db/ClientIdentityApplicatorTest.java`

Verifies:
- `applyIdentity()` sets `userUid`, `userName`, and `permissions` on `ClientInfo`.
- `applyIdentity()` calls `setRawTenantValue()` on FoundSetManager.
- Null permissions does not overwrite existing groups.
- Null tenants still calls `setRawTenantValue(null)`.

### 3.4 Test: `TenantPipelineIntegrationTest`

Location: `j2db_test/src/com/servoy/j2db/TenantPipelineIntegrationTest.java`

Verifies the full pipeline via `BasicFormManager.applyTenantValue()` — the shared base method inherited by both `FormManager` and `NGFormManager` (5 tests):
- `applyIdentity()` → `setRawTenantValue` → `BasicFormManager.applyTenantValue(solution)` → `setTenantValue(solution, value)` on FoundSetManager.
- Last tenant wins (multiple applyIdentity calls).
- Null tenant → no `setTenantValue` call.
- Tenant set then cleared → no application.
- Multi-value tenant arrays passed through intact.

Verifies the explicit caller contract for headless/REST/MCP (4 tests):
- Explicit `applyIdentity()` with tenants → `makeSolutionSettings()` applies them.
- Explicit `applyIdentity()` with null tenants (legacy 5-arg ClientLogin) → no tenant application.
- Authenticator solution loads → tenants skipped; then real solution loads → tenants applied.
- Re-authentication with different tenant → old tenant replaced, new one applied on next solution load.

### 3.5 Test: `AuthenticatorManagerTest` (extended)

Location: `servoy_ngclient.tests/src/test/java/com/servoy/j2db/server/ngclient/auth/AuthenticatorManagerTest.java`

Added tests:
- Tenant values in JWT when `loginResponse` has tenantValues.
- No tenants → no claim.
- Multi-value tenants serialized as JSON array.
- Single tenant value stored as single-element array.

## 4. Files Changed

### servoy_shared
| File | Change |
|------|--------|
| `src/com/servoy/j2db/ClientIdentityApplicator.java` | **NEW** — shared utility |
| `src/com/servoy/j2db/BasicFormManager.java` | Added `applyTenantValue(Solution)`, import for `SolutionMetaData` |
| `src/com/servoy/j2db/FormManager.java` | Added `applyTenantValue(solution)` call in `makeSolutionSettings()` |
| `src/com/servoy/j2db/IBasicFormManager.java` | No change (setTenantValue removed then re-removed) |
| `src/com/servoy/j2db/dataprocessing/IFoundSetManagerInternal.java` | Added `setRawTenantValue(Object)` |
| `src/com/servoy/j2db/dataprocessing/FoundSetManager.java` | Added `setRawTenantValue(Object)` implementation |

### servoy_ngclient
| File | Change |
|------|--------|
| `src/com/servoy/j2db/server/ngclient/NGClientWebsocketSession.java` | Simplified `setUserId()` to use `ClientIdentityApplicator` |
| `src/com/servoy/j2db/server/ngclient/NGFormManager.java` | Removed `tenantValue` field, removed `setTenantValue()`, replaced inline tenant logic with `applyTenantValue(solution)` |
| `src/com/servoy/j2db/server/ngclient/INGFormManager.java` | Removed `setTenantValue(Object)` |

### j2db_test
| File | Change |
|------|--------|
| `src/com/servoy/j2db/ClientIdentityApplicatorTest.java` | **NEW** |
| `src/com/servoy/j2db/TenantPipelineIntegrationTest.java` | **NEW** |
| `src/com/servoy/j2db/dataprocessing/FoundSetManagerRawTenantTest.java` | **NEW** |
| `src/com/servoy/j2db/ClientLoginTest.java` | **NEW** |

### servoy_ngclient.tests
| File | Change |
|------|--------|
| `src/test/java/com/servoy/j2db/server/ngclient/auth/AuthenticatorManagerTest.java` | Added tenant-related test methods |

## 5. Commit Messages

### Commit 1: servoy_shared + servoy_ngclient (production code)

```
SVY-21382: Extract ClientIdentityApplicator and unify tenant pipeline

- New ClientIdentityApplicator utility in servoy_shared: applies user identity
  (uid, name, permissions) to ClientInfo and stores raw tenant on FoundSetManager.
- Added setRawTenantValue()/getRawTenantValue() on IFoundSetManagerInternal and
  FoundSetManager for tenant storage without immediate filter application.
- Added BasicFormManager.applyTenantValue(Solution) — shared method called by
  both FormManager and NGFormManager in makeSolutionSettings(). Skips
  authenticator solutions.
- FormManager.makeSolutionSettings() now calls applyTenantValue() so headless
  clients also support tenants.
- NGFormManager: removed tenantValue field and setTenantValue(), uses inherited
  applyTenantValue() instead.
- INGFormManager: removed setTenantValue(Object).
- NGClientWebsocketSession.setUserId(): simplified to single
  ClientIdentityApplicator.applyIdentity() call.
```

### Commit 2: j2db_test + servoy_ngclient.tests (tests)

```
SVY-21382: Add tests for ClientIdentityApplicator and tenant pipeline

- ClientIdentityApplicatorTest: verifies identity fields set on ClientInfo
  and rawTenantValue stored on FoundSetManager.
- TenantPipelineIntegrationTest: integration test for full pipeline from
  applyIdentity → applyTenantValue → setTenantValue on FoundSetManager.
  Covers multi-tenant, null, cleared, and multi-value scenarios.
- FoundSetManagerRawTenantTest: verifies setRawTenantValue/getRawTenantValue
  roundtrip and setTenantValue also stores rawTenantValue.
- ClientLoginTest: verifies 6-arg constructor tenant support.
- AuthenticatorManagerTest: added tenant propagation tests (withTenants,
  nullTenants, singleTenant, multiTenant, tenantWithJsReturn).
```

## 6. Acceptance Criteria

- [x] `ClientLoginTest` passes
- [x] `FoundSetManagerRawTenantTest` passes
- [x] `ClientIdentityApplicatorTest` passes
- [x] `TenantPipelineIntegrationTest` passes
- [x] `AuthenticatorManagerTest` passes (19 tests)
- [x] Zero compilation errors in servoy_shared, servoy_ngclient, servoy_headless_client, j2db_test
- [x] Authenticator solution does not get tenant filters applied
- [x] Both FormManager and NGFormManager apply tenants via shared applyTenantValue()
