# Spec: SVY-21254 — LoginResult object and i18n support for stateless login

## 1. Goal

Replace the `Pair<Boolean, String>` return type used throughout the stateless login flow with a proper `LoginResult` object that carries structured authentication data (`customHtml`, `token`, `authenticated`, `returnValue`). Capture the authenticator's JS return value from `ClientLogin` into this result, and make it available in the `TagResolver` when rendering login page HTML so that i18n messages and custom tags can reference authenticator return values.

## 2. Background

### 2.0 Target branch and version context

This spec targets the **`lts_2026`** branch of `servoy-client`. On this branch (2026.3 LTS), there is **no `AbstractAuthenticatorManager`** class — all login logic (including `writeLoginPage()`, `checkUser()`, `checkPermissions()`, and `callAuthenticator()`) lives in `StatelessLoginHandler`. The `AbstractAuthenticatorManager` / `IAuthenticatorManager` refactoring was introduced in 2026.6 (master). When merging these changes forward to master (2026.6+), the `LoginResult` usage must be adapted to the `AbstractAuthenticatorManager` / `AuthenticatorManager` class hierarchy.

### 2.1 Current Pair<Boolean, String> usage

`StatelessLoginHandler.mustAuthenticate()` returns `Pair<Boolean, String>` where:
- **Left (Boolean):** `true` = user must log in (show login page), `false` = authenticated
- **Right (String):** the JWT token when authenticated, or `null`/custom HTML content when not

This pair is consumed by `AngularIndexPageFilter.doFilter()` and `IndexPageFilter.doFilter()`:
- If `left == true`: calls `StatelessLoginHandler.writeLoginPage(request, response, solutionName, showLogin.getRight())`
- If `left == false` and `right != null`: stores the token in the HTTP session as `ID_TOKEN`

The same `Pair<Boolean, String>` is passed as a mutable parameter (`needToLogin`) through `checkUser()`, `checkPermissions()`, `IAuthenticatorManager.checkUser()`, and `AuthenticatorManager.callAuthenticator()`.

### 2.2 ClientLogin and the ignored jsReturn

`ClientLogin` (in `servoy_shared`) already carries a `jsReturn` field — the value returned by the authenticator solution's `onOpen` method. However, `AuthenticatorManager.callAuthenticator()` (line 135–149) only extracts `userName`, `userUid`, and `userGroups` from the `ClientLogin` to build the JWT token. The `jsReturn` value is **completely ignored**.

### 2.3 TagResolver in login page writing

`StatelessLoginHandler.writeLoginPage()` (on `lts_2026`; becomes `AbstractAuthenticatorManager.writeLoginPage()` on 2026.6/master) processes login HTML using `TagParser.processTags()` with:
- An `ITagResolver` that only resolves `%%solutionTitle%%`
- An `I18NTagResolver` that resolves `i18n:key` prefixed strings

There is currently no way for the tag resolver to access authenticator return values or other authentication context.

### 2.4 i18n in the stateless login flow

The `I18NTagResolver` class delegates to `AngularIndexPageWriter.getSolutionDefaultMessage()` using the request locale. While i18n messages are resolved in the login HTML, the resolution is limited to static keys. There is no mechanism to pass dynamic context (e.g., error messages from the authenticator, custom return values) into the tag resolution.

## 3. Design

### 3.1 Create `LoginResult` class

Create a new class `com.servoy.j2db.server.ngclient.auth.LoginResult` to replace `Pair<Boolean, String>`:

```java
public class LoginResult
{
    private boolean authenticated;
    private String token;
    private String customHtml;
    private String returnValue;
    private boolean responseHandled;

    // Constructor, getters, setters, and builder/factory methods
    public static LoginResult needsLogin() { ... }
    public static LoginResult needsLogin(String customHtml) { ... }
    public static LoginResult authenticated(String token) { ... }
    public static LoginResult authenticated(String token, String returnValue) { ... }
    public static LoginResult handled() { ... }
}
```

Fields:
- **`authenticated`** — replaces `Pair.getLeft()` (inverted: `true` = user is authenticated)
- **`token`** — the JWT token string (replaces `Pair.getRight()` when authenticated)
- **`customHtml`** — full HTML page from the cloud login flow (`CloudStatelessAccessManager`). When present and starts with `<`, it is written directly to the response, bypassing the solution's `login.html` media. Only used for `SERVOY_CLOUD` authenticator type.
- **`returnValue`** — the JS return value from the authenticator (captured from `ClientLogin.getJsReturn()`)
- **`responseHandled`** — `true` when the response has already been written (e.g., OAuth `extractFromFragment()` page, tenant selection page). The filter must not write anything else to the response in this case.

> **Note:** The actual login page template is read from `solution.getMedia("login.html")` (or a built-in default fallback). The `customHtml` field does NOT represent the login template — it is a cloud-specific full page override.

### 3.2 Refactor `StatelessLoginHandler.mustAuthenticate()`

Change the return type from `Pair<Boolean, String>` to `LoginResult`:

```java
public static LoginResult mustAuthenticate(HttpServletRequest request, HttpServletResponse response, String solutionName)
```

Update internal logic to populate the `LoginResult` fields instead of mutating a `Pair`.

### 3.3 Refactor login methods in `StatelessLoginHandler`

On `lts_2026`, there is no `IAuthenticatorManager` interface — the `checkUser()`, `checkPermissions()`, and `callAuthenticator()` methods all live in `StatelessLoginHandler`. Update their signatures to accept and populate `LoginResult` instead of `Pair<Boolean, String>`:

```java
boolean checkUser(String username, String password, boolean remember, SvyID oldToken,
    LoginResult result, HttpServletRequest request, HttpServletResponse response);

boolean checkPermissions(String username, String password, boolean remember, SvyID oldToken,
    LoginResult result, HttpServletRequest request);
```

> **Note for 2026.6/master merge:** On master these methods are split across `IAuthenticatorManager`, `AbstractAuthenticatorManager`, `AuthenticatorManager`, `DefaultLoginManager`, and `OAuthHandler`. The same `LoginResult` pattern applies but the refactoring touches more files.

### 3.4 Capture `jsReturn` in `StatelessLoginHandler.callAuthenticator()`

After `applicationServer.login(credentials)` returns a `ClientLogin`, extract `login.getJsReturn()` and store it in the `LoginResult`:

```java
ClientLogin login = applicationServer.login(credentials);
if (login != null)
{
    // ... build token as before ...
    result.setAuthenticated(true);
    result.setToken(token);
    result.setReturnValue(login.getJsReturn());  // NEW
    return true;
}
```

> **Note for 2026.6/master merge:** On master this method is `AuthenticatorManager.callAuthenticator()`.

### 3.5 Enhance TagResolver with LoginResult context

In `StatelessLoginHandler.writeLoginPage()` (on `lts_2026`; `AbstractAuthenticatorManager.writeLoginPage()` on 2026.6/master), extend the `ITagResolver` to expose `LoginResult` fields as template tags:

```java
// Convert returnValue (JSON/JS object) to a Map of key-value pairs for tag resolution
Map<String, String> returnValueMap = null;
if (loginResult != null && loginResult.getReturnValue() != null)
{
    returnValueMap = convertToMap(loginResult.getReturnValue()); // parse JSON string to Map<String,String>
}
final Map<String, String> resolvedMap = returnValueMap;

loginHtml = TagParser.processTags(loginHtml, new ITagResolver()
{
    @Override
    public String getStringValue(String name)
    {
        if ("solutionTitle".equals(name))
        {
            String titleText = sol.getTitleText();
            if (titleText == null) titleText = sol.getName();
            return i18nProvider.getI18NMessageIfPrefixed(titleText);
        }
        if (resolvedMap != null && resolvedMap.containsKey(name))
        {
            return resolvedMap.get(name);
        }
        return "";
    }
}, i18nProvider);
```

This allows login page templates to use `%%anyKey%%` tags where `anyKey` is a key from the authenticator's return value map. If a tag is not found in the map, it resolves to empty string (not the tag name). For example, if the authenticator returns `{ "errorMessage": "Invalid credentials", "redirectUrl": "/retry" }`, the login template can use `%%errorMessage%%` and `%%redirectUrl%%`.

### 3.6 Pass LoginResult to `writeLoginPage()`

Change the `writeLoginPage()` signature to accept `LoginResult` so the tag resolver has access to it:

```java
void writeLoginPage(HttpServletRequest request, HttpServletResponse response, LoginResult loginResult)
    throws ServletException, UnsupportedEncodingException, IOException;
```

The `customHtml` parameter currently passed separately becomes part of `LoginResult`.

### 3.7 Update callers in filters

Update `AngularIndexPageFilter.doFilter()` and `IndexPageFilter.doFilter()` to work with `LoginResult` instead of `Pair<Boolean, String>`:

```java
LoginResult showLogin = StatelessLoginHandler.mustAuthenticate(request, response, solutionName);
if (!showLogin.isAuthenticated())
{
    StatelessLoginHandler.writeLoginPage(request, response, solutionName, showLogin);
    return;
}
if (showLogin.getToken() != null)
{
    session.setAttribute(StatelessLoginHandler.ID_TOKEN, showLogin.getToken());
}
```

### 3.8 Ensure i18n messages resolve correctly in stateless login

The existing `I18NTagResolver` correctly uses the request locale and solution to resolve i18n keys. Ensure that:
1. The `customHtml` and `returnValue` fields are also processed through `TagParser.processTags()` so i18n keys embedded within them are resolved.
2. Login page templates can use `i18n:` prefixed keys in any part of the HTML (labels, error messages, placeholders) and they resolve using the browser's `Accept-Language` header locale.
3. If the authenticator returns an i18n key as `returnValue`, it should be resolvable when used as `%%returnValue%%` in the template.

## 4. Implementation plan

> All changes target the `lts_2026` branch of `servoy-client`. On this branch, login logic is centralized in `StatelessLoginHandler` (no `AbstractAuthenticatorManager`/`IAuthenticatorManager` yet).

1. **Create `LoginResult` class** in `com.servoy.j2db.server.ngclient.auth` package (or alongside `StatelessLoginHandler`) with fields: `authenticated`, `token`, `customHtml`, `returnValue`. Add static factory methods.

2. **Refactor `StatelessLoginHandler`** — change `mustAuthenticate()` return type to `LoginResult`. Update `checkUser()`, `checkPermissions()`, `callAuthenticator()`, and `writeLoginPage()` to use `LoginResult` instead of `Pair<Boolean, String>`.

3. **Capture `jsReturn`** — in `StatelessLoginHandler.callAuthenticator()`, after successful login, capture `ClientLogin.getJsReturn()` into `LoginResult.returnValue`.

4. **Enhance `writeLoginPage()` TagResolver** — convert `LoginResult.returnValue` (JSON/JS object) to a `Map<String, String>` and resolve any `%%key%%` tag from that map. Return `""` for unresolved tags instead of the tag name.

5. **Update `AngularIndexPageFilter.doFilter()`** — use `LoginResult` API instead of `Pair` getLeft/getRight.

6. **Update `IndexPageFilter.doFilter()`** (in `com.servoy.eclipse.ngclient.ui`) — same as above.

7. **Process i18n in returnValue** — if `loginResult.getReturnValue()` starts with `i18n:`, resolve it through `I18NTagResolver` before injecting into the template.

8. **Update tests** — adapt all test classes in `servoy_ngclient.tests/` that reference `Pair<Boolean, String>` in the login context.

9. **Verify backward compatibility** — ensure OAuth and cloud login flows still work correctly with `LoginResult`.

> **Merge-forward note:** When merging to master (2026.6+), the `LoginResult` usage must be adapted across `IAuthenticatorManager`, `AbstractAuthenticatorManager`, `AuthenticatorManager`, `DefaultLoginManager`, `OAuthHandler`, and `CloudStatelessAccessManager`.

## 5. Acceptance criteria

- [ ] `Pair<Boolean, String>` is no longer used in the stateless login authentication flow (replaced by `LoginResult`)
- [ ] `LoginResult` class exists with fields: `customHtml`, `token`, `authenticated`, `returnValue`
- [ ] `AuthenticatorManager.callAuthenticator()` captures `ClientLogin.getJsReturn()` into `LoginResult.returnValue`
- [ ] The `ITagResolver` in `AbstractAuthenticatorManager.writeLoginPage()` resolves `%%returnValue%%` and `%%customHtml%%` tags from `LoginResult`
- [ ] i18n keys (`i18n:some.key`) in login page HTML are resolved using the request locale
- [ ] i18n keys embedded within `returnValue` are resolved when rendered through the tag system
- [ ] All callers (`AngularIndexPageFilter`, `IndexPageFilter`, `NGClient`, `NGClientWebsocketSession`) work correctly with `LoginResult`
- [ ] All existing tests in `servoy_ngclient.tests` pass after refactoring
- [ ] No regression in login flows for Default, OAuth, Cloud, and Authenticator types

## 6. Out of scope

- Changing the `ClientLogin` class itself (it already has `jsReturn`)
- Adding new i18n translation keys (this spec enables their use, not authoring them)
- Changing the login HTML templates (the templates remain backward compatible; new tags are optional)
- Refactoring `StatelessLoginHandler` into `IAuthenticatorManager` implementations (covered by SVY-20949)
- OAuth token refresh flow changes
- Adding new authenticator types

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Should `LoginResult` be immutable (builder pattern) or mutable (setters)? The mutable `Pair` pattern is used today but immutable is safer. | Edit Mera | open |
| Should `returnValue` be a raw `String` or parsed to `Object` (JSON)? If the authenticator returns a JSON object, should the tag resolver support dotted paths like `%%returnValue.message%%`? | Edit Mera | open |
| Is there a need to sanitize `returnValue` before injecting it into HTML (XSS prevention)? The authenticator is developer-controlled, but defense-in-depth may warrant HTML escaping. | Edit Mera | open |
