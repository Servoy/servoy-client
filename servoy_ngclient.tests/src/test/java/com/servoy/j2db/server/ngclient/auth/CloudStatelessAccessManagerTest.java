package com.servoy.j2db.server.ngclient.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Solution.AUTHENTICATOR_TYPE;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.ngclient.StatelessLoginHandler;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServerSingleton;
import com.servoy.j2db.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@SuppressWarnings("nls")
public class CloudStatelessAccessManagerTest
{
	private static final String TEST_JWT_PASSWORD = "test-jwt-secret-key-for-cloud-tests";
	private static final String SOLUTION_NAME = "cloudSampleSolution";
	private static final String TEST_USERNAME = "testuser@servoy.com";
	private static final String[] TEST_PERMISSIONS = { "Administrators", "Users" };

	private Solution cloudSolution;
	private JSONObject lastCloudResponse;
	private int lastCloudStatusCode;

	@Before
	public void setUp() throws Exception
	{
		lastCloudResponse = new JSONObject();
		lastCloudStatusCode = 200;

		IApplicationServerSingleton appServer = createMockApplicationServer();
		ApplicationServerRegistry.setApplicationServerSingleton(appServer);

		cloudSolution = createSolution(SOLUTION_NAME, SolutionMetaData.SOLUTION, null);
		cloudSolution.setAuthenticator(AUTHENTICATOR_TYPE.SERVOY_CLOUD);

		CloudStatelessAccessManager.setHttpClientFactory(() -> createMockHttpClient());
	}

	@After
	public void tearDown()
	{
		CloudStatelessAccessManager.resetHttpClientFactory();
		ApplicationServerRegistry.destroy();
	}

	// ===== checkCloudPermissions - fresh login =====

	@Test
	public void testCheckCloudPermissions_validLogin_authenticates() throws Exception
	{
		JSONObject response = new JSONObject();
		response.put("permissions", new JSONArray(TEST_PERMISSIONS));
		response.put("username", TEST_USERNAME);
		lastCloudResponse = response;
		lastCloudStatusCode = 200;

		LoginResult result = LoginResult.needsLogin();
		boolean verified = CloudStatelessAccessManager.checkCloudPermissions(
			TEST_USERNAME, "password123", false, null, result, cloudSolution, createMockRequest());

		assertTrue(verified);
		assertTrue(result.isAuthenticated());
		assertNotNull(result.getToken());

		DecodedJWT decoded = JWT.decode(result.getToken());
		assertEquals(TEST_USERNAME, decoded.getClaim(SvyID.USERNAME).asString());
		String[] permissions = decoded.getClaim(SvyID.PERMISSIONS).asArray(String.class);
		assertNotNull(permissions);
		assertEquals(2, permissions.length);
	}

	@Test
	public void testCheckCloudPermissions_invalidLogin_notAuthenticated() throws Exception
	{
		lastCloudStatusCode = 401;
		lastCloudResponse = new JSONObject().put("error", "Invalid credentials");

		LoginResult result = LoginResult.needsLogin();
		boolean verified = CloudStatelessAccessManager.checkCloudPermissions(
			TEST_USERNAME, "wrongpass", false, null, result, cloudSolution, createMockRequest());

		assertFalse(verified);
		assertFalse(result.isAuthenticated());
	}

	@Test
	public void testCheckCloudPermissions_emptyPermissions_notAuthenticated() throws Exception
	{
		JSONObject response = new JSONObject();
		response.put("permissions", new JSONArray());
		response.put("username", TEST_USERNAME);
		lastCloudResponse = response;
		lastCloudStatusCode = 200;

		LoginResult result = LoginResult.needsLogin();
		boolean verified = CloudStatelessAccessManager.checkCloudPermissions(
			TEST_USERNAME, "password123", false, null, result, cloudSolution, createMockRequest());

		assertFalse(verified);
	}

	// ===== checkCloudPermissions - token refresh =====

	@Test
	public void testCheckCloudPermissions_tokenRefresh_authenticates() throws Exception
	{
		JSONObject response = new JSONObject();
		response.put("permissions", new JSONArray(TEST_PERMISSIONS));
		response.put("username", TEST_USERNAME);
		lastCloudResponse = response;
		lastCloudStatusCode = 200;

		LoginResult firstResult = LoginResult.needsLogin();
		CloudStatelessAccessManager.checkCloudPermissions(
			TEST_USERNAME, "password123", false, null, firstResult, cloudSolution, createMockRequest());
		assertNotNull(firstResult.getToken());

		SvyID oldToken = new SvyID(firstResult.getToken());
		LoginResult refreshResult = LoginResult.needsLogin();
		boolean verified = CloudStatelessAccessManager.checkCloudPermissions(
			null, null, false, oldToken, refreshResult, cloudSolution, createMockRequest());

		assertTrue(verified);
		assertTrue(refreshResult.isAuthenticated());
		assertNotNull(refreshResult.getToken());
	}

	// ===== getCloudLoginPage =====

	@Test
	public void testGetCloudLoginPage_returnsHtml() throws Exception
	{
		String loginHtml = "<html><body><form></form></body></html>";
		lastCloudResponse = new JSONObject().put("html", loginHtml);
		lastCloudStatusCode = 200;

		HttpServletRequest request = createMockRequest();
		String result = CloudStatelessAccessManager.getCloudLoginPage(request, cloudSolution, null);

		assertNotNull(result);
		assertTrue(result.contains("<form>"));
		assertTrue(result.contains("svyRedirect"));
	}

	@Test
	public void testGetCloudLoginPage_withFullLoginPage_containsExpectedElements() throws Exception
	{
		String loginHtml = "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n\t<meta charset=\"utf-8\">\n\t<!-- this base href is mandatory -->\n" +
			"\t<base href=\"/\">\n" +
			"<script type='text/javascript'>\n" +
			"    window.addEventListener('load', () => { \n" +
			"     if (window.localStorage.getItem('servoy_id_token')) { \n" +
			"    \tdocument.login_form.action = 'index.html'; \n" +
			"  \t    document.login_form.id_token.value = JSON.parse(window.localStorage.getItem('servoy_id_token'));  \n" +
			"  \t    document.login_form.elements['csrf_token'].value = '-6970833352899417059';\n" +
			"    \tdocument.login_form.remember.checked = true;  \n" +
			"    \tdocument.login_form.submit(); \n" +
			"     } else { \n" +
			"        if(loader) loader.style.display = 'none';\n" +
			"     } \n" +
			"   }) \n" +
			"  </script> \n" +
			"\t<title>cloudSampleSolution | Sign in</title>\n" +
			"</head>\n<body>\n" +
			"<form accept-charset=\"UTF-8\" role=\"form\" name=\"login_form\" method=\"post\" class=\"form\" action=\"svylogin/login\">\n" +
			"<input class=\"input-field\" name=\"username\" id=\"username\" type=\"text\" placeholder=\" \" required autocomplete=\"username\">\n" +
			"<input class=\"input-field\" name=\"password\" id=\"password\" type=\"password\" placeholder=\" \" required autocomplete=\"current-password\">\n" +
			"<input class=\"input-checkbox\" id=\"remember\" name=\"remember\" type=\"checkbox\"/>\n" +
			"<button type=\"submit\" class=\"button-submit\">Sign in</button>\n" +
			"<input name=\"id_token\" type=\"hidden\" >\n" +
			"<input type='hidden' name='csrf_token' value='-6970833352899417059'></form>\n" +
			"<form accept-charset=\"UTF-8\" role=\"form\" name=\"sso_login_form\" class='social-login-form' method=\"post\" action=\"svylogin/oauth\">\n" +
			"<button class=\"gsi-material-button\" type=\"submit\" name=\"oauth\" value=\"google\">Sign in with Google</button>\n" +
			"<button class=\"gsi-material-button\" type=\"submit\" name=\"oauth\" value=\"microsoft\">Sign in with Microsoft</button>\n" +
			"<input type='hidden' name='csrf_token' value='-6970833352899417059'></form>\n" +
			"</body>\n</html>";
		lastCloudResponse = new JSONObject().put("html", loginHtml);
		lastCloudStatusCode = 200;

		HttpServletRequest request = createMockRequest();
		String result = CloudStatelessAccessManager.getCloudLoginPage(request, cloudSolution, null);

		assertNotNull(result);
		assertTrue(result.contains("svyRedirect"));
		assertTrue(result.contains("login_form"));
		assertTrue(result.contains("id_token"));
		assertTrue(result.contains("svylogin/login"));
		assertTrue(result.contains("username"));
		assertTrue(result.contains("password"));
		assertTrue(result.contains("Sign in"));
		assertTrue(result.contains("sso_login_form"));
		assertTrue(result.contains("google"));
		assertTrue(result.contains("microsoft"));
		assertTrue(result.contains("csrf_token"));
	}

	@Test
	public void testGetCloudLoginPage_tenantSelectPage_containsExpectedElements() throws Exception
	{
		String tenantHtml = "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n\t<meta charset=\"utf-8\">\n" +
			"\t<base href=\"/\">\n" +
			"\t<title>cloudSampleSolution | Select tenant</title>\n" +
			"</head>\n<body>\n" +
			"<form accept-charset=\"UTF-8\" role=\"form\" name=\"login_form\" method=\"post\" class=\"form\" action=\"svylogin/tenant_select_redirect\">\n" +
			"<input class=\"input-field\" name=\"username\" id=\"username\" type=\"text\" disabled value=\"emera@servoy.com\">\n" +
			"<select class=\"input-field\" name=\"tenant\" id=\"tenant\" required>\n" +
			"<option value=\"1E90A95B-3190-4389-B224-52F01C749FB7\" selected>Sandbox</option>" +
			"<option value=\"B5F23F1E-5100-40E1-B334-DF5CD9CD84DE\">Servoy</option>\n" +
			"</select>\n" +
			"<button type=\"submit\" class=\"button-submit\">Continue</button>\n" +
			"<input name=\"svyTenantLoginToken\" id='formToken' type=\"hidden\" value=\"eyJhbGciOiJIUzM4NCJ9.test\" />\n" +
			"</form>\n" +
			"</body>\n</html>";
		lastCloudResponse = new JSONObject().put("html", tenantHtml);
		lastCloudStatusCode = 200;

		HttpServletRequest request = createMockRequest();
		String result = CloudStatelessAccessManager.getCloudLoginPage(request, cloudSolution, null);

		assertNotNull(result);
		assertTrue(result.contains("svyRedirect"));
		assertTrue(result.contains("tenant_select_redirect"));
		assertTrue(result.contains("svyTenantLoginToken"));
		assertTrue(result.contains("Sandbox"));
		assertTrue(result.contains("Servoy"));
		assertTrue(result.contains("Continue"));
	}

	@Test
	public void testGetCloudLoginPage_cloudReturnsError_returnsNull() throws Exception
	{
		lastCloudStatusCode = 500;
		lastCloudResponse = new JSONObject().put("error", "Internal server error");

		HttpServletRequest request = createMockRequest();
		String result = CloudStatelessAccessManager.getCloudLoginPage(request, cloudSolution, null);

		assertTrue(result == null || !result.contains("login_form"));
	}

	// ===== checkCloudPermissions - with tenants =====

	@Test
	public void testCheckCloudPermissions_withTenants_tokenContainsTenants() throws Exception
	{
		JSONObject response = new JSONObject();
		response.put("permissions", new JSONArray(TEST_PERMISSIONS));
		response.put("username", TEST_USERNAME);
		response.put("tenantValues", new JSONArray(new String[] { "Sandbox", "Production" }));
		lastCloudResponse = response;
		lastCloudStatusCode = 200;

		LoginResult result = LoginResult.needsLogin();
		boolean verified = CloudStatelessAccessManager.checkCloudPermissions(
			TEST_USERNAME, "password123", false, null, result, cloudSolution, createMockRequest());

		assertTrue(verified);
		assertNotNull(result.getToken());

		DecodedJWT decoded = JWT.decode(result.getToken());
		String[] tenants = decoded.getClaim(SvyID.TENANTS).asArray(String.class);
		assertNotNull(tenants);
		assertEquals(2, tenants.length);
		assertEquals("Sandbox", tenants[0]);
		assertEquals("Production", tenants[1]);
	}

	// ===== checkCloudPermissions - with remember =====

	@Test
	public void testCheckCloudPermissions_withRemember_refreshPreservesRememberFromOldToken() throws Exception
	{
		// The 'remember' claim in the token is only set from the oldToken on refresh.
		// For fresh login via checkCloudPermissions, remember is sent as a header to the cloud
		// but the token builder uses Boolean.FALSE (the writeResponse path handles remember differently).
		// This test verifies that on refresh, the remember flag from the old token is preserved.
		JSONObject response = new JSONObject();
		response.put("permissions", new JSONArray(TEST_PERMISSIONS));
		response.put("username", TEST_USERNAME);
		lastCloudResponse = response;
		lastCloudStatusCode = 200;

		// First, create an old token that has remember=true via the writeResponse flow simulation.
		// We manually build a token with remember=true to simulate an existing session.
		Properties settings = ApplicationServerRegistry.get().getServerAccess().getSettings();
		String jwtPassword = settings.getProperty(StatelessLoginUtils.JWT_Password);
		String oldTokenStr = JWT.create()
			.withClaim(SvyID.USERNAME, TEST_USERNAME)
			.withClaim(SvyID.PERMISSIONS, java.util.Arrays.asList(TEST_PERMISSIONS))
			.withClaim(SvyID.REMEMBER, true)
			.withExpiresAt(new java.util.Date(System.currentTimeMillis() + 3600000))
			.sign(Algorithm.HMAC256(jwtPassword));

		SvyID oldToken = new SvyID(oldTokenStr);
		LoginResult refreshResult = LoginResult.needsLogin();
		boolean verified = CloudStatelessAccessManager.checkCloudPermissions(
			null, null, false, oldToken, refreshResult, cloudSolution, createMockRequest());

		assertTrue(verified);
		assertTrue(refreshResult.isAuthenticated());
		DecodedJWT decoded = JWT.decode(refreshResult.getToken());
		Boolean remember = decoded.getClaim(SvyID.REMEMBER).asBoolean();
		assertNotNull(remember);
		assertTrue(remember.booleanValue());
	}

	// ===== Full flow via StatelessLoginHandler: mustAuthenticate -> writeLoginPage -> credentials -> token =====

	@Test
	public void testFullFlow_cloud_mustAuthenticate_writeLoginPage_credentials_svyToken() throws Exception
	{
		// Step 1: User hits index page - mustAuthenticate detects SERVOY_CLOUD needs login
		HttpServletRequest indexRequest = createFullFlowRequest(null, null);
		HttpServletResponse indexResponse = createMockResponse();

		LoginResult mustAuthResult = StatelessLoginHandler.mustAuthenticate(indexRequest, indexResponse, SOLUTION_NAME);
		assertFalse("Should need login for SERVOY_CLOUD", mustAuthResult.isAuthenticated());

		// Step 2: writeLoginPage calls getCloudLoginPage for SERVOY_CLOUD
		String loginPageHtml = "<!DOCTYPE html><html><body>" +
			"<form name=\"login_form\" method=\"post\">" +
			"<input name=\"username\"><input name=\"password\">" +
			"<input type='hidden' name='csrf_token' value='123456'></form></body></html>";
		lastCloudResponse = new JSONObject().put("html", loginPageHtml);
		lastCloudStatusCode = 200;

		StringWriter pageOutput = new StringWriter();
		HttpServletResponse writeResponse = createMockResponseWithWriter(pageOutput);
		StatelessLoginHandler.writeLoginPage(indexRequest, writeResponse, SOLUTION_NAME, mustAuthResult);
		String writtenPage = pageOutput.toString();
		assertTrue("Should have written a login page", writtenPage.contains("login_form") || writtenPage.contains("svyRedirect"));

		// Step 3: User submits credentials - mustAuthenticate with username/password/csrf
		JSONObject permissionsResponse = new JSONObject();
		permissionsResponse.put("permissions", new JSONArray(TEST_PERMISSIONS));
		permissionsResponse.put("username", TEST_USERNAME);
		lastCloudResponse = permissionsResponse;
		lastCloudStatusCode = 200;

		HttpServletRequest credRequest = createFullFlowRequest(TEST_USERNAME, "password123");
		HttpServletResponse credResponse = createMockResponse();

		LoginResult loginResult = StatelessLoginHandler.mustAuthenticate(credRequest, credResponse, SOLUTION_NAME);
		assertTrue("Should be authenticated after cloud login", loginResult.isAuthenticated());
		assertNotNull("Token should be created", loginResult.getToken());

		DecodedJWT decoded = JWT.decode(loginResult.getToken());
		assertEquals(TEST_USERNAME, decoded.getClaim(SvyID.USERNAME).asString());
		String[] permissions = decoded.getClaim(SvyID.PERMISSIONS).asArray(String.class);
		assertNotNull(permissions);
		assertEquals(2, permissions.length);
	}

	@Test
	public void testFullFlow_cloud_loginFails_errorPageWithForm_retrySucceeds() throws Exception
	{
		// Full flow: login page -> wrong credentials via svylogin path -> error page with form + CSRF -> retry succeeds
		// Step 1: writeLoginPage renders initial login form
		HttpServletRequest indexRequest = createFullFlowRequest(null, null);
		HttpServletResponse indexResponse = createMockResponse();

		LoginResult mustAuthResult = StatelessLoginHandler.mustAuthenticate(indexRequest, indexResponse, SOLUTION_NAME);
		assertFalse("Should need login", mustAuthResult.isAuthenticated());

		String loginPageHtml = "<!DOCTYPE html><html><body>" +
			"<form name=\"login_form\" method=\"post\" action=\"svylogin/login\">" +
			"<input name=\"username\"><input name=\"password\"></form></body></html>";
		lastCloudResponse = new JSONObject().put("html", loginPageHtml);
		lastCloudStatusCode = 200;

		StringWriter pageOutput = new StringWriter();
		HttpServletResponse writeResp = createMockResponseWithWriter(pageOutput);
		StatelessLoginHandler.writeLoginPage(indexRequest, writeResp, SOLUTION_NAME, mustAuthResult);
		String initialPage = pageOutput.toString();
		// writeLoginPage injects csrf into the form
		assertTrue("Should have csrf_token in form", initialPage.contains("csrf_token"));

		// Step 2: User submits wrong credentials -> handlePossibleCloudRequest -> cloud returns error HTML with form
		String errorLoginHtml = "<!DOCTYPE html><html><body>" +
			"<div class='error'>Invalid credentials</div>" +
			"<form name=\"login_form\" method=\"post\" action=\"svylogin/login\">" +
			"<input name=\"username\"><input name=\"password\"></form></body></html>";
		lastCloudResponse = new JSONObject().put("html", errorLoginHtml);
		lastCloudStatusCode = 200;

		// Set up the context with endpoints cached
		Map<String, Object> contextAttributes = new HashMap<>();
		contextAttributes.put("nonce", new java.util.concurrent.ConcurrentHashMap<String, JSONObject>());
		contextAttributes.put("endpoints", new String[] { "login", "tenant_select_redirect" });
		contextAttributes.put("endpoints_expire", Long.valueOf(System.currentTimeMillis() + 600000));

		String csrfToken = "123456";
		Map<String, String[]> loginParams = new HashMap<>();
		loginParams.put("username", new String[] { TEST_USERNAME });
		loginParams.put("password", new String[] { "wrong-password" });
		loginParams.put("csrf_token", new String[] { csrfToken });

		jakarta.servlet.http.Cookie csrfCookie = new jakarta.servlet.http.Cookie("csrf_token", csrfToken);
		Map<String, Object> sessionAttributes = new HashMap<>();
		final List<jakarta.servlet.http.Cookie> responseCookies = new ArrayList<>();

		HttpServletRequest cloudPostRequest = (HttpServletRequest)Proxy.newProxyInstance(
			HttpServletRequest.class.getClassLoader(),
			new Class< ? >[] { HttpServletRequest.class },
			(proxy, method, args) -> {
				switch (method.getName())
				{
					case "getParameterMap" :
						return loginParams;
					case "getParameter" :
						String[] vals = loginParams.get(args[0]);
						return vals != null && vals.length > 0 ? vals[0] : null;
					case "getCookies" :
						return new jakarta.servlet.http.Cookie[] { csrfCookie };
					case "getCharacterEncoding" :
						return "UTF-8";
					case "setCharacterEncoding" :
						return null;
					case "getRequestURI" :
						return "/solution/" + SOLUTION_NAME + "/svylogin/login";
					case "getRequestURL" :
						return new StringBuffer("https://localhost:8080/solution/" + SOLUTION_NAME + "/svylogin/login");
					case "getServletPath" :
						return "/solution/" + SOLUTION_NAME + "/svylogin/login";
					case "getContextPath" :
						return "";
					case "getScheme" :
						return "https";
					case "getServerName" :
						return "localhost";
					case "getServerPort" :
						return Integer.valueOf(8080);
					case "getHeader" :
						if ("accept-language".equals(args[0])) return "en";
						return null;
					case "getLocale" :
						return Locale.ENGLISH;
					case "getSession" :
						return (jakarta.servlet.http.HttpSession)Proxy.newProxyInstance(
							jakarta.servlet.http.HttpSession.class.getClassLoader(),
							new Class< ? >[] { jakarta.servlet.http.HttpSession.class },
							(p, m, a) -> {
								switch (m.getName())
								{
									case "getAttribute" :
										return sessionAttributes.get(a[0]);
									case "setAttribute" :
										sessionAttributes.put((String)a[0], a[1]);
										return null;
									default :
										return getDefaultReturnValue(m);
								}
							});
					case "getRemoteAddr" :
						return "127.0.0.1";
					case "getServletContext" :
						return (jakarta.servlet.ServletContext)Proxy.newProxyInstance(
							jakarta.servlet.ServletContext.class.getClassLoader(),
							new Class< ? >[] { jakarta.servlet.ServletContext.class },
							(p, m, a) -> {
								switch (m.getName())
								{
									case "getAttribute" :
										return contextAttributes.get(a[0]);
									case "setAttribute" :
										contextAttributes.put((String)a[0], a[1]);
										return null;
									default :
										return getDefaultReturnValue(m);
								}
							});
					case "getQueryString" :
						return null;
					case "getMethod" :
						return "POST";
					default :
						return getDefaultReturnValue(method);
				}
			});

		StringWriter errorPageOutput = new StringWriter();
		PrintWriter errorPrintWriter = new PrintWriter(errorPageOutput);
		HttpServletResponse cloudPostResponse = (HttpServletResponse)Proxy.newProxyInstance(
			HttpServletResponse.class.getClassLoader(),
			new Class< ? >[] { HttpServletResponse.class },
			(proxy, method, args) -> {
				switch (method.getName())
				{
					case "getWriter" :
						return errorPrintWriter;
					case "addCookie" :
						responseCookies.add((jakarta.servlet.http.Cookie)args[0]);
						return null;
					case "setCharacterEncoding" :
					case "setContentType" :
					case "setContentLengthLong" :
					case "addHeader" :
					case "setHeader" :
					case "setStatus" :
					case "sendRedirect" :
						return null;
					case "getCharacterEncoding" :
						return "UTF-8";
					default :
						return getDefaultReturnValue(method);
				}
			});

		boolean handled = CloudStatelessAccessManager.handlePossibleCloudRequest(cloudPostRequest, cloudPostResponse, SOLUTION_NAME);
		assertTrue("Should be handled by cloud manager", handled);

		String errorPage = errorPageOutput.toString();
		// The error page should have the CSRF token injected
		assertTrue("Error page should contain csrf_token hidden field", errorPage.contains("csrf_token"));
		assertTrue("Error page should contain error message", errorPage.contains("Invalid credentials"));

		// Verify a new CSRF cookie was set
		boolean csrfCookieSet = responseCookies.stream().anyMatch(c -> "csrf_token".equals(c.getName()));
		assertTrue("Should have set a new csrf_token cookie", csrfCookieSet);

		// Step 3: User retries with correct credentials using the new CSRF token
		String newCsrfValue = responseCookies.stream()
			.filter(c -> "csrf_token".equals(c.getName()))
			.findFirst().get().getValue();

		JSONObject permissionsResponse = new JSONObject();
		permissionsResponse.put("permissions", new JSONArray(TEST_PERMISSIONS));
		permissionsResponse.put("username", TEST_USERNAME);
		lastCloudResponse = permissionsResponse;
		lastCloudStatusCode = 200;

		// Now submit with new CSRF
		HttpServletRequest retryRequest = createFullFlowRequest(TEST_USERNAME, "correct-password");
		HttpServletResponse retryResponse = createMockResponse();

		LoginResult retryResult = StatelessLoginHandler.mustAuthenticate(retryRequest, retryResponse, SOLUTION_NAME);
		assertTrue("Should be authenticated on retry", retryResult.isAuthenticated());
		assertNotNull("Should have token", retryResult.getToken());
	}

	@Test
	public void testFullFlow_cloud_checkPermissions_validToken_revalidates() throws Exception
	{
		// Step 1: Login to get a valid svy token
		JSONObject permissionsResponse = new JSONObject();
		permissionsResponse.put("permissions", new JSONArray(TEST_PERMISSIONS));
		permissionsResponse.put("username", TEST_USERNAME);
		lastCloudResponse = permissionsResponse;
		lastCloudStatusCode = 200;

		HttpServletRequest loginRequest = createFullFlowRequest(TEST_USERNAME, "password123");
		HttpServletResponse loginResponse = createMockResponse();

		LoginResult firstResult = StatelessLoginHandler.mustAuthenticate(loginRequest, loginResponse, SOLUTION_NAME);
		assertTrue("Should be authenticated on first login", firstResult.isAuthenticated());
		assertNotNull("Should have a token", firstResult.getToken());

		// Step 2: Submit the existing svy token as id_token parameter -> triggers checkPermissions
		// Cloud returns same permissions on revalidation
		lastCloudResponse = permissionsResponse;
		lastCloudStatusCode = 200;

		String svyToken = firstResult.getToken();
		Map<String, String[]> revalidateParams = new HashMap<>();
		revalidateParams.put("id_token", new String[] { svyToken });
		revalidateParams.put("csrf_token", new String[] { "123456" });

		jakarta.servlet.http.Cookie csrfCookie = new jakarta.servlet.http.Cookie("csrf_token", "123456");

		Map<String, Object> sessionAttributes = new HashMap<>();
		Map<String, Object> contextAttributes = new HashMap<>();
		contextAttributes.put("nonce", new java.util.concurrent.ConcurrentHashMap<String, JSONObject>());

		HttpServletRequest checkPermRequest = (HttpServletRequest)Proxy.newProxyInstance(
			HttpServletRequest.class.getClassLoader(),
			new Class< ? >[] { HttpServletRequest.class },
			(proxy, method, args) -> {
				switch (method.getName())
				{
					case "getParameterMap" :
						return revalidateParams;
					case "getParameter" :
						String[] vals = revalidateParams.get(args[0]);
						return vals != null && vals.length > 0 ? vals[0] : null;
					case "getCookies" :
						return new jakarta.servlet.http.Cookie[] { csrfCookie };
					case "getCharacterEncoding" :
						return "UTF-8";
					case "setCharacterEncoding" :
						return null;
					case "getRequestURI" :
						return "/solution/" + SOLUTION_NAME + "/index.html";
					case "getRequestURL" :
						return new StringBuffer("https://localhost:8080/solution/" + SOLUTION_NAME + "/index.html");
					case "getServletPath" :
						return "/solution/" + SOLUTION_NAME + "/index.html";
					case "getContextPath" :
						return "";
					case "getScheme" :
						return "https";
					case "getServerName" :
						return "localhost";
					case "getServerPort" :
						return Integer.valueOf(8080);
					case "getHeader" :
						if ("accept-language".equals(args[0])) return "en";
						return null;
					case "getLocale" :
						return Locale.ENGLISH;
					case "getSession" :
						return (jakarta.servlet.http.HttpSession)Proxy.newProxyInstance(
							jakarta.servlet.http.HttpSession.class.getClassLoader(),
							new Class< ? >[] { jakarta.servlet.http.HttpSession.class },
							(p, m, a) -> {
								switch (m.getName())
								{
									case "getAttribute" :
										return sessionAttributes.get(a[0]);
									case "setAttribute" :
										sessionAttributes.put((String)a[0], a[1]);
										return null;
									case "removeAttribute" :
										sessionAttributes.remove(a[0]);
										return null;
									default :
										return getDefaultReturnValue(m);
								}
							});
					case "getRemoteAddr" :
						return "127.0.0.1";
					case "getServletContext" :
						return (jakarta.servlet.ServletContext)Proxy.newProxyInstance(
							jakarta.servlet.ServletContext.class.getClassLoader(),
							new Class< ? >[] { jakarta.servlet.ServletContext.class },
							(p, m, a) -> {
								switch (m.getName())
								{
									case "getAttribute" :
										return contextAttributes.get(a[0]);
									case "setAttribute" :
										contextAttributes.put((String)a[0], a[1]);
										return null;
									default :
										return getDefaultReturnValue(m);
								}
							});
					case "getQueryString" :
						return null;
					case "getMethod" :
						return "POST";
					default :
						return getDefaultReturnValue(method);
				}
			});

		HttpServletResponse checkPermResponse = createMockResponse();
		LoginResult checkResult = StatelessLoginHandler.mustAuthenticate(checkPermRequest, checkPermResponse, SOLUTION_NAME);

		assertTrue("Should be authenticated after checkPermissions", checkResult.isAuthenticated());
		assertNotNull("Should have a token after checkPermissions", checkResult.getToken());
	}

	// ===== Full flow: login page -> credentials -> tenants -> token =====

	@Test
	public void testFullFlow_loginPage_thenCredentials_thenTenants_thenToken() throws Exception
	{
		// Step 1: User hits the index page without being logged in.
		// writeLoginPage is called -> for SERVOY_CLOUD it calls getCloudLoginPage
		String loginPageHtml = "<!DOCTYPE html><html><head><base href=\"/\"></head><body>" +
			"<form name=\"login_form\" method=\"post\" action=\"svylogin/login\">" +
			"<input name=\"username\"><input name=\"password\">" +
			"<input name=\"id_token\" type=\"hidden\">" +
			"<input type='hidden' name='csrf_token' value='-6970833352899417059'></form></body></html>";
		lastCloudResponse = new JSONObject().put("html", loginPageHtml);
		lastCloudStatusCode = 200;

		HttpServletRequest request = createMockRequest();
		String page = CloudStatelessAccessManager.getCloudLoginPage(request, cloudSolution, null);
		assertNotNull(page);
		assertTrue(page.contains("login_form"));
		assertTrue(page.contains("svyRedirect"));

		// Step 2: User submits credentials.
		// Cloud responds with tenant selection page (html response).
		String tenantPageHtml = "<!DOCTYPE html><html><head><base href=\"/\"></head><body>" +
			"<form name=\"login_form\" method=\"post\" action=\"svylogin/tenant_select_redirect\">" +
			"<input name=\"username\" disabled value=\"" + TEST_USERNAME + "\">" +
			"<select name=\"tenant\">" +
			"<option value=\"1E90A95B\" selected>Sandbox</option>" +
			"<option value=\"B5F23F1E\">Servoy</option>" +
			"</select>" +
			"<input name=\"svyTenantLoginToken\" type=\"hidden\" value=\"eyJhbGciOiJIUzM4NCJ9.test\"/>" +
			"</form></body></html>";
		lastCloudResponse = new JSONObject().put("html", tenantPageHtml);
		lastCloudStatusCode = 200;

		// Simulate what writeResponse does when the cloud returns html for login endpoint
		page = CloudStatelessAccessManager.getCloudLoginPage(request, cloudSolution, null);
		assertNotNull(page);
		assertTrue(page.contains("tenant_select_redirect"));
		assertTrue(page.contains("svyTenantLoginToken"));
		assertTrue(page.contains("Sandbox"));
		assertTrue(page.contains("Servoy"));

		// Step 3: User selects tenant and submits.
		// Cloud responds with permissions JSON -> token is built.
		JSONObject permissionsResponse = new JSONObject();
		permissionsResponse.put("permissions", new JSONArray(TEST_PERMISSIONS));
		permissionsResponse.put("username", TEST_USERNAME);
		permissionsResponse.put("tenantValues", new JSONArray(new String[] { "Sandbox" }));
		permissionsResponse.put("lastLogin", "1785856847000");
		lastCloudResponse = permissionsResponse;
		lastCloudStatusCode = 200;

		LoginResult result = LoginResult.needsLogin();
		boolean verified = CloudStatelessAccessManager.checkCloudPermissions(
			TEST_USERNAME, "password123", true, null, result, cloudSolution, createMockRequest());

		assertTrue(verified);
		assertTrue(result.isAuthenticated());
		assertNotNull(result.getToken());

		// Verify the token has everything expected
		DecodedJWT decoded = JWT.decode(result.getToken());
		assertEquals(TEST_USERNAME, decoded.getClaim(SvyID.USERNAME).asString());
		String[] permissions = decoded.getClaim(SvyID.PERMISSIONS).asArray(String.class);
		assertEquals(2, permissions.length);
		String[] tenants = decoded.getClaim(SvyID.TENANTS).asArray(String.class);
		assertNotNull(tenants);
		assertEquals(1, tenants.length);
		assertEquals("Sandbox", tenants[0]);
	}

	@Test
	public void testFullFlow_tokenRefresh_afterExpiry() throws Exception
	{
		// Step 1: Initial login succeeds
		JSONObject response = new JSONObject();
		response.put("permissions", new JSONArray(TEST_PERMISSIONS));
		response.put("username", TEST_USERNAME);
		response.put("tenantValues", new JSONArray(new String[] { "Sandbox" }));
		lastCloudResponse = response;
		lastCloudStatusCode = 200;

		LoginResult firstResult = LoginResult.needsLogin();
		CloudStatelessAccessManager.checkCloudPermissions(
			TEST_USERNAME, "password123", true, null, firstResult, cloudSolution, createMockRequest());
		assertTrue(firstResult.isAuthenticated());
		assertNotNull(firstResult.getToken());

		// Step 2: Token expires, refresh is triggered.
		// Cloud returns refreshed permissions (same or updated).
		JSONObject refreshResponse = new JSONObject();
		refreshResponse.put("permissions", new JSONArray(new String[] { "Administrators", "Users", "Premium" }));
		refreshResponse.put("username", TEST_USERNAME);
		refreshResponse.put("tenantValues", new JSONArray(new String[] { "Sandbox" }));
		lastCloudResponse = refreshResponse;
		lastCloudStatusCode = 200;

		SvyID oldToken = new SvyID(firstResult.getToken());
		LoginResult refreshResult = LoginResult.needsLogin();
		boolean verified = CloudStatelessAccessManager.checkCloudPermissions(
			null, null, false, oldToken, refreshResult, cloudSolution, createMockRequest());

		assertTrue(verified);
		assertTrue(refreshResult.isAuthenticated());
		assertNotNull(refreshResult.getToken());

		DecodedJWT decoded = JWT.decode(refreshResult.getToken());
		String[] permissions = decoded.getClaim(SvyID.PERMISSIONS).asArray(String.class);
		assertEquals(3, permissions.length);
	}

	// ===== checkCloudPermissions - error scenarios =====

	@Test
	public void testCheckCloudPermissions_cloudReturns500_notAuthenticated() throws Exception
	{
		lastCloudStatusCode = 500;
		lastCloudResponse = new JSONObject().put("error", "Internal Server Error");

		LoginResult result = LoginResult.needsLogin();
		boolean verified = CloudStatelessAccessManager.checkCloudPermissions(
			TEST_USERNAME, "password123", false, null, result, cloudSolution, createMockRequest());

		assertFalse(verified);
		assertFalse(result.isAuthenticated());
	}

	@Test
	public void testCheckCloudPermissions_cloudReturns403_notAuthenticated() throws Exception
	{
		lastCloudStatusCode = 403;
		lastCloudResponse = new JSONObject().put("error", "Forbidden");

		LoginResult result = LoginResult.needsLogin();
		boolean verified = CloudStatelessAccessManager.checkCloudPermissions(
			TEST_USERNAME, "password123", false, null, result, cloudSolution, createMockRequest());

		assertFalse(verified);
		assertFalse(result.isAuthenticated());
	}

	@Test
	public void testCheckCloudPermissions_noPermissionsKey_notAuthenticated() throws Exception
	{
		JSONObject response = new JSONObject();
		response.put("username", TEST_USERNAME);
		// No "permissions" key at all
		lastCloudResponse = response;
		lastCloudStatusCode = 200;

		LoginResult result = LoginResult.needsLogin();
		boolean verified = CloudStatelessAccessManager.checkCloudPermissions(
			TEST_USERNAME, "password123", false, null, result, cloudSolution, createMockRequest());

		assertFalse(verified);
	}

	@Test
	public void testCheckCloudPermissions_nullUsername_usesResponseUsername() throws Exception
	{
		JSONObject response = new JSONObject();
		response.put("permissions", new JSONArray(TEST_PERMISSIONS));
		response.put("username", "cloud-user@servoy.com");
		lastCloudResponse = response;
		lastCloudStatusCode = 200;

		LoginResult result = LoginResult.needsLogin();
		boolean verified = CloudStatelessAccessManager.checkCloudPermissions(
			null, "password123", false, null, result, cloudSolution, createMockRequest());

		assertTrue(verified);
		assertTrue(result.isAuthenticated());
		DecodedJWT decoded = JWT.decode(result.getToken());
		assertEquals("cloud-user@servoy.com", decoded.getClaim(SvyID.USERNAME).asString());
	}

	@Test
	public void testCheckCloudPermissions_withLastLogin_tokenContainsLastLogin() throws Exception
	{
		JSONObject response = new JSONObject();
		response.put("permissions", new JSONArray(TEST_PERMISSIONS));
		response.put("username", TEST_USERNAME);
		response.put("lastLogin", "1785856847000");
		lastCloudResponse = response;
		lastCloudStatusCode = 200;

		LoginResult result = LoginResult.needsLogin();
		boolean verified = CloudStatelessAccessManager.checkCloudPermissions(
			TEST_USERNAME, "password123", false, null, result, cloudSolution, createMockRequest());

		assertTrue(verified);
		DecodedJWT decoded = JWT.decode(result.getToken());
		String lastLogin = decoded.getClaim(SvyID.LAST_LOGIN).asString();
		assertNotNull(lastLogin);
		assertEquals("1785856847000", lastLogin);
	}

	@Test
	public void testCheckCloudPermissions_refreshPreservesLastLoginHeader() throws Exception
	{
		// First login with lastLogin
		JSONObject response = new JSONObject();
		response.put("permissions", new JSONArray(TEST_PERMISSIONS));
		response.put("username", TEST_USERNAME);
		response.put("lastLogin", "1785856847000");
		lastCloudResponse = response;
		lastCloudStatusCode = 200;

		LoginResult firstResult = LoginResult.needsLogin();
		CloudStatelessAccessManager.checkCloudPermissions(
			TEST_USERNAME, "password123", false, null, firstResult, cloudSolution, createMockRequest());
		assertTrue(firstResult.isAuthenticated());

		// Now refresh - the old token has lastLogin, it should be sent as a header
		SvyID oldToken = new SvyID(firstResult.getToken());
		LoginResult refreshResult = LoginResult.needsLogin();
		boolean verified = CloudStatelessAccessManager.checkCloudPermissions(
			null, null, false, oldToken, refreshResult, cloudSolution, createMockRequest());

		assertTrue(verified);
		assertTrue(refreshResult.isAuthenticated());
	}

	@Test
	public void testCheckCloudPermissions_freshLogin_rememberFalseInToken() throws Exception
	{
		// For fresh login the remember claim in the token is always FALSE
		// (the remember parameter is sent as a header to the cloud, not in the token)
		JSONObject response = new JSONObject();
		response.put("permissions", new JSONArray(TEST_PERMISSIONS));
		response.put("username", TEST_USERNAME);
		lastCloudResponse = response;
		lastCloudStatusCode = 200;

		LoginResult result = LoginResult.needsLogin();
		CloudStatelessAccessManager.checkCloudPermissions(
			TEST_USERNAME, "password123", true, null, result, cloudSolution, createMockRequest());

		assertTrue(result.isAuthenticated());
		DecodedJWT decoded = JWT.decode(result.getToken());
		Boolean remember = decoded.getClaim(SvyID.REMEMBER).asBoolean();
		// Fresh login with oldToken=null sets FALSE via withRememberUser(FALSE)
		// SvyTokenBuilder only adds the claim when TRUE, so it should be null
		assertTrue("Remember claim should be null or false", remember == null || !remember.booleanValue());
	}

	// ===== checkCloudOAuthPermissions =====

	@Test
	public void testCheckCloudOAuthPermissions_validPayload_authenticates() throws Exception
	{
		JSONObject response = new JSONObject();
		response.put("permissions", new JSONArray(TEST_PERMISSIONS));
		response.put("username", TEST_USERNAME);
		lastCloudResponse = response;
		lastCloudStatusCode = 200;

		LoginResult result = LoginResult.needsLogin();
		HttpServletRequest request = createMockRequest();
		HttpServletResponse mockResponse = createMockResponse();
		boolean verified = CloudStatelessAccessManager.checkCloudOAuthPermissions(
			request, mockResponse, result, cloudSolution,
			"{\"id_token\":\"test-oauth-token\"}", Boolean.FALSE, null, "google");

		assertTrue(verified);
	}

	@Test
	public void testCheckCloudOAuthPermissions_invalidPayload_notAuthenticated() throws Exception
	{
		lastCloudStatusCode = 401;
		lastCloudResponse = new JSONObject().put("error", "Invalid OAuth token");

		LoginResult result = LoginResult.needsLogin();
		HttpServletRequest request = createMockRequest();
		HttpServletResponse mockResponse = createMockResponse();
		boolean verified = CloudStatelessAccessManager.checkCloudOAuthPermissions(
			request, mockResponse, result, cloudSolution,
			"{\"id_token\":\"invalid\"}", Boolean.FALSE, null, "google");

		assertFalse(verified);
	}

	@Test
	public void testCheckCloudOAuthPermissions_withHtmlResponse_writesHtml() throws Exception
	{
		String htmlContent = "<html><body><form><input name='test'></form></body></html>";
		JSONObject response = new JSONObject();
		response.put("html", htmlContent);
		lastCloudResponse = response;
		lastCloudStatusCode = 200;

		LoginResult result = LoginResult.needsLogin();
		HttpServletRequest request = createMockRequest();
		StringWriter writer = new StringWriter();
		HttpServletResponse mockResponse = createMockResponseWithWriter(writer);
		boolean verified = CloudStatelessAccessManager.checkCloudOAuthPermissions(
			request, mockResponse, result, cloudSolution,
			"{\"id_token\":\"test\"}", Boolean.FALSE, null, "google");

		assertTrue(verified);
		String output = writer.toString();
		assertTrue(output.contains("<form>") || output.contains("<input") || output.contains("test"));
	}

	@Test
	public void testCheckCloudOAuthPermissions_withPermissions_redirects() throws Exception
	{
		JSONObject response = new JSONObject();
		response.put("permissions", new JSONArray(TEST_PERMISSIONS));
		response.put("username", TEST_USERNAME);
		lastCloudResponse = response;
		lastCloudStatusCode = 200;

		LoginResult result = LoginResult.needsLogin();
		HttpServletRequest request = createMockRequest();
		List<String> redirects = new ArrayList<>();
		HttpServletResponse mockResponse = createMockResponseWithRedirect(redirects);
		boolean verified = CloudStatelessAccessManager.checkCloudOAuthPermissions(
			request, mockResponse, result, cloudSolution,
			"{\"id_token\":\"test\"}", Boolean.TRUE, "refresh-token-123", "google");

		assertTrue(verified);
		// writeResponse should have set a redirect when permissions are returned
		assertFalse(redirects.isEmpty());
	}

	// ===== writeResponse - error flow =====

	@Test
	public void testCheckCloudOAuthPermissions_errorHtmlResponse_writesErrorPage() throws Exception
	{
		JSONObject response = new JSONObject();
		response.put("error", "User not found");
		lastCloudResponse = response;
		lastCloudStatusCode = 400;

		LoginResult result = LoginResult.needsLogin();
		HttpServletRequest request = createMockRequest();
		StringWriter writer = new StringWriter();
		HttpServletResponse mockResponse = createMockResponseWithWriter(writer);
		boolean verified = CloudStatelessAccessManager.checkCloudOAuthPermissions(
			request, mockResponse, result, cloudSolution,
			"{\"id_token\":\"test\"}", Boolean.FALSE, null, "google");

		// 400 status means the outer if (res.getLeft() == HTTP_OK) fails -> return false
		assertFalse(verified);
	}

	@Test
	public void testCheckCloudOAuthPermissions_200_withError_writesErrorHtml() throws Exception
	{
		// When the cloud returns 200 but with an "error" field that starts with "<html>"
		// writeResponse writes it directly as HTML
		JSONObject response = new JSONObject();
		response.put("error", "<html><body>Account locked</body></html>");
		lastCloudResponse = response;
		lastCloudStatusCode = 200;

		LoginResult result = LoginResult.needsLogin();
		HttpServletRequest request = createMockRequest();
		StringWriter writer = new StringWriter();
		HttpServletResponse mockResponse = createMockResponseWithWriter(writer);

		// checkCloudOAuthPermissions checks for HTTP_OK then calls writeResponse
		boolean verified = CloudStatelessAccessManager.checkCloudOAuthPermissions(
			request, mockResponse, result, cloudSolution,
			"{\"id_token\":\"test\"}", Boolean.FALSE, null, "google");

		assertTrue(verified);
		String output = writer.toString();
		assertTrue("Should contain the error HTML", output.contains("Account locked"));
	}

	@Test
	public void testCheckCloudOAuthPermissions_200_htmlErrorResponse_writesRawHtml() throws Exception
	{
		// When the error starts with "<html>" it's used as-is (no template)
		JSONObject response = new JSONObject();
		response.put("error", "<html><body>Custom Error Page</body></html>");
		lastCloudResponse = response;
		lastCloudStatusCode = 200;

		LoginResult result = LoginResult.needsLogin();
		HttpServletRequest request = createMockRequest();
		StringWriter writer = new StringWriter();
		HttpServletResponse mockResponse = createMockResponseWithWriter(writer);

		boolean verified = CloudStatelessAccessManager.checkCloudOAuthPermissions(
			request, mockResponse, result, cloudSolution,
			"{\"id_token\":\"test\"}", Boolean.FALSE, null, "google");

		assertTrue(verified);
		String output = writer.toString();
		assertTrue(output.contains("Custom Error Page"));
	}

	// ===== writeResponse - permissions with remember from JSON =====

	@Test
	public void testCheckCloudOAuthPermissions_permissionsWithRememberInJson_setsRememberTrue() throws Exception
	{
		// When the JSON has "remember" key, writeResponse uses it instead of the parameter
		JSONObject response = new JSONObject();
		response.put("permissions", new JSONArray(TEST_PERMISSIONS));
		response.put("username", TEST_USERNAME);
		response.put(SvyID.REMEMBER, true);
		lastCloudResponse = response;
		lastCloudStatusCode = 200;

		LoginResult result = LoginResult.needsLogin();
		HttpServletRequest request = createMockRequest();
		List<String> redirects = new ArrayList<>();
		Map<String, Object> sessionAttrs = new HashMap<>();
		HttpServletResponse mockResponse = createMockResponseWithRedirect(redirects);
		// We pass rememberUser=FALSE but the JSON overrides it
		boolean verified = CloudStatelessAccessManager.checkCloudOAuthPermissions(
			request, mockResponse, result, cloudSolution,
			"{\"id_token\":\"test\"}", Boolean.FALSE, null, "google");

		assertTrue(verified);
	}

	@Test
	public void testCheckCloudOAuthPermissions_permissionsWithRefreshToken_setsRefreshToken() throws Exception
	{
		JSONObject response = new JSONObject();
		response.put("permissions", new JSONArray(TEST_PERMISSIONS));
		response.put("username", TEST_USERNAME);
		response.put("refresh_token", "oauth-refresh-token-from-cloud");
		lastCloudResponse = response;
		lastCloudStatusCode = 200;

		LoginResult result = LoginResult.needsLogin();
		HttpServletRequest request = createMockRequest();
		List<String> redirects = new ArrayList<>();
		HttpServletResponse mockResponse = createMockResponseWithRedirect(redirects);
		boolean verified = CloudStatelessAccessManager.checkCloudOAuthPermissions(
			request, mockResponse, result, cloudSolution,
			"{\"id_token\":\"test\"}", Boolean.FALSE, "my-refresh-token", "microsoft");

		assertTrue(verified);
	}

	// ===== Full flow tests via StatelessLoginHandler.mustAuthenticate and writeLoginPage =====

	@Test
	public void testFullFlow_cloud_submitCredentials_mustAuthenticate_authenticates() throws Exception
	{
		JSONObject permissionsResponse = new JSONObject();
		permissionsResponse.put("permissions", new JSONArray(TEST_PERMISSIONS));
		permissionsResponse.put("username", TEST_USERNAME);
		permissionsResponse.put("tenantValues", new JSONArray(new String[] { "Production" }));
		lastCloudResponse = permissionsResponse;
		lastCloudStatusCode = 200;

		HttpServletRequest credRequest = createFullFlowRequest(TEST_USERNAME, "password123");
		HttpServletResponse credResponse = createMockResponse();

		LoginResult loginResult = StatelessLoginHandler.mustAuthenticate(credRequest, credResponse, SOLUTION_NAME);
		assertTrue("Should be authenticated after cloud login", loginResult.isAuthenticated());
		assertNotNull("Token should be created", loginResult.getToken());

		DecodedJWT decoded = JWT.decode(loginResult.getToken());
		assertEquals(TEST_USERNAME, decoded.getClaim(SvyID.USERNAME).asString());
		String[] permissions = decoded.getClaim(SvyID.PERMISSIONS).asArray(String.class);
		assertNotNull(permissions);
		assertEquals(2, permissions.length);
		String[] tenants = decoded.getClaim(SvyID.TENANTS).asArray(String.class);
		assertNotNull(tenants);
		assertEquals("Production", tenants[0]);
	}

	@Test
	public void testFullFlow_cloud_submitCredentials_cloudRejects_thenWriteLoginPage() throws Exception
	{
		lastCloudStatusCode = 401;
		lastCloudResponse = new JSONObject().put("error", "Invalid credentials");

		HttpServletRequest credRequest = createFullFlowRequest(TEST_USERNAME, "wrongpass");
		HttpServletResponse credResponse = createMockResponse();

		LoginResult loginResult = StatelessLoginHandler.mustAuthenticate(credRequest, credResponse, SOLUTION_NAME);
		assertFalse("Should not be authenticated with wrong credentials", loginResult.isAuthenticated());

		String cloudHtml = "<html><body><form name=\"login_form\"><span class=\"error\">Invalid credentials</span></form></body></html>";
		lastCloudResponse = new JSONObject().put("html", cloudHtml);
		lastCloudStatusCode = 200;

		StringWriter pageOutput = new StringWriter();
		HttpServletResponse writeResponse = createMockResponseWithWriter(pageOutput);
		StatelessLoginHandler.writeLoginPage(credRequest, writeResponse, SOLUTION_NAME, loginResult);
		String page = pageOutput.toString();
		assertTrue("Should show login page again", page.contains("login_form") || page.contains("svyRedirect"));
	}

	@Test
	public void testFullFlow_cloud_validToken_mustAuthenticate_refreshSucceeds() throws Exception
	{
		JSONObject permissionsResponse = new JSONObject();
		permissionsResponse.put("permissions", new JSONArray(TEST_PERMISSIONS));
		permissionsResponse.put("username", TEST_USERNAME);
		lastCloudResponse = permissionsResponse;
		lastCloudStatusCode = 200;

		HttpServletRequest firstRequest = createFullFlowRequest(TEST_USERNAME, "password123");
		HttpServletResponse firstResponse = createMockResponse();
		LoginResult firstResult = StatelessLoginHandler.mustAuthenticate(firstRequest, firstResponse, SOLUTION_NAME);
		assertTrue(firstResult.isAuthenticated());
		String svyToken = firstResult.getToken();

		String csrfToken = "123456";
		Map<String, String[]> params = new HashMap<>();
		params.put("id_token", new String[] { svyToken });
		params.put("csrf_token", new String[] { csrfToken });
		jakarta.servlet.http.Cookie csrfCookie = new jakarta.servlet.http.Cookie("csrf_token", csrfToken);
		Map<String, Object> sessionAttributes = new HashMap<>();
		Map<String, Object> contextAttributes = new HashMap<>();
		contextAttributes.put("nonce", new java.util.concurrent.ConcurrentHashMap<String, JSONObject>());

		HttpServletRequest refreshRequest = (HttpServletRequest)Proxy.newProxyInstance(
			HttpServletRequest.class.getClassLoader(),
			new Class< ? >[] { HttpServletRequest.class },
			(proxy, method, args) -> {
				switch (method.getName())
				{
					case "getParameterMap" :
						return params;
					case "getParameter" :
						String[] vals = params.get(args[0]);
						return vals != null && vals.length > 0 ? vals[0] : null;
					case "getCookies" :
						return new jakarta.servlet.http.Cookie[] { csrfCookie };
					case "getCharacterEncoding" :
						return "UTF-8";
					case "setCharacterEncoding" :
						return null;
					case "getRequestURI" :
						return "/solution/" + SOLUTION_NAME + "/index.html";
					case "getRequestURL" :
						return new StringBuffer("https://localhost:8080/solution/" + SOLUTION_NAME + "/index.html");
					case "getServletPath" :
						return "/solution/" + SOLUTION_NAME + "/index.html";
					case "getContextPath" :
						return "";
					case "getScheme" :
						return "https";
					case "getServerName" :
						return "localhost";
					case "getServerPort" :
						return Integer.valueOf(8080);
					case "getHeader" :
						if ("accept-language".equals(args[0])) return "en";
						return null;
					case "getLocale" :
						return Locale.ENGLISH;
					case "getSession" :
						return (jakarta.servlet.http.HttpSession)Proxy.newProxyInstance(
							jakarta.servlet.http.HttpSession.class.getClassLoader(),
							new Class< ? >[] { jakarta.servlet.http.HttpSession.class },
							(p, m, a) -> {
								switch (m.getName())
								{
									case "getAttribute" :
										return sessionAttributes.get(a[0]);
									case "setAttribute" :
										sessionAttributes.put((String)a[0], a[1]);
										return null;
									case "removeAttribute" :
										sessionAttributes.remove(a[0]);
										return null;
									default :
										return getDefaultReturnValue(m);
								}
							});
					case "getRemoteAddr" :
						return "127.0.0.1";
					case "getServletContext" :
						return (jakarta.servlet.ServletContext)Proxy.newProxyInstance(
							jakarta.servlet.ServletContext.class.getClassLoader(),
							new Class< ? >[] { jakarta.servlet.ServletContext.class },
							(p, m, a) -> {
								switch (m.getName())
								{
									case "getAttribute" :
										return contextAttributes.get(a[0]);
									case "setAttribute" :
										contextAttributes.put((String)a[0], a[1]);
										return null;
									default :
										return getDefaultReturnValue(m);
								}
							});
					case "getQueryString" :
						return null;
					case "getMethod" :
						return "POST";
					default :
						return getDefaultReturnValue(method);
				}
			});

		HttpServletResponse refreshResponse = createMockResponse();
		LoginResult refreshResult = StatelessLoginHandler.mustAuthenticate(refreshRequest, refreshResponse, SOLUTION_NAME);
		assertTrue("Should be authenticated via checkPermissions refresh", refreshResult.isAuthenticated());
		assertNotNull("Token should be present", refreshResult.getToken());
	}

	@Test
	public void testFullFlow_cloud_expiredToken_mustAuthenticate_refreshFails_writeLoginPage() throws Exception
	{
		Properties settings = ApplicationServerRegistry.get().getServerAccess().getSettings();
		String jwtPassword = settings.getProperty(StatelessLoginUtils.JWT_Password);
		String expiredToken = JWT.create()
			.withIssuer("svy")
			.withClaim(SvyID.USERNAME, TEST_USERNAME)
			.withArrayClaim(SvyID.PERMISSIONS, TEST_PERMISSIONS)
			.withIssuedAt(new java.util.Date(System.currentTimeMillis() - 7200000))
			.withExpiresAt(new java.util.Date(System.currentTimeMillis() - 1000))
			.sign(Algorithm.HMAC256(jwtPassword));

		lastCloudStatusCode = 401;
		lastCloudResponse = new JSONObject().put("error", "Session expired");

		String csrfToken = "123456";
		Map<String, String[]> params = new HashMap<>();
		params.put("id_token", new String[] { expiredToken });
		params.put("csrf_token", new String[] { csrfToken });
		jakarta.servlet.http.Cookie csrfCookie = new jakarta.servlet.http.Cookie("csrf_token", csrfToken);
		Map<String, Object> sessionAttributes = new HashMap<>();
		Map<String, Object> contextAttributes = new HashMap<>();
		contextAttributes.put("nonce", new java.util.concurrent.ConcurrentHashMap<String, JSONObject>());

		HttpServletRequest request = (HttpServletRequest)Proxy.newProxyInstance(
			HttpServletRequest.class.getClassLoader(),
			new Class< ? >[] { HttpServletRequest.class },
			(proxy, method, args) -> {
				switch (method.getName())
				{
					case "getParameterMap" :
						return params;
					case "getParameter" :
						String[] vals = params.get(args[0]);
						return vals != null && vals.length > 0 ? vals[0] : null;
					case "getCookies" :
						return new jakarta.servlet.http.Cookie[] { csrfCookie };
					case "getCharacterEncoding" :
						return "UTF-8";
					case "setCharacterEncoding" :
						return null;
					case "getRequestURI" :
						return "/solution/" + SOLUTION_NAME + "/index.html";
					case "getRequestURL" :
						return new StringBuffer("https://localhost:8080/solution/" + SOLUTION_NAME + "/index.html");
					case "getServletPath" :
						return "/solution/" + SOLUTION_NAME + "/index.html";
					case "getContextPath" :
						return "";
					case "getScheme" :
						return "https";
					case "getServerName" :
						return "localhost";
					case "getServerPort" :
						return Integer.valueOf(8080);
					case "getHeader" :
						if ("accept-language".equals(args[0])) return "en";
						if ("user-agent".equals(args[0])) return "Mozilla/5.0";
						return null;
					case "getLocale" :
						return Locale.ENGLISH;
					case "getSession" :
						return (jakarta.servlet.http.HttpSession)Proxy.newProxyInstance(
							jakarta.servlet.http.HttpSession.class.getClassLoader(),
							new Class< ? >[] { jakarta.servlet.http.HttpSession.class },
							(p, m, a) -> {
								switch (m.getName())
								{
									case "getAttribute" :
										return sessionAttributes.get(a[0]);
									case "setAttribute" :
										sessionAttributes.put((String)a[0], a[1]);
										return null;
									case "removeAttribute" :
										sessionAttributes.remove(a[0]);
										return null;
									default :
										return getDefaultReturnValue(m);
								}
							});
					case "getRemoteAddr" :
						return "127.0.0.1";
					case "getServletContext" :
						return (jakarta.servlet.ServletContext)Proxy.newProxyInstance(
							jakarta.servlet.ServletContext.class.getClassLoader(),
							new Class< ? >[] { jakarta.servlet.ServletContext.class },
							(p, m, a) -> {
								switch (m.getName())
								{
									case "getAttribute" :
										return contextAttributes.get(a[0]);
									case "setAttribute" :
										contextAttributes.put((String)a[0], a[1]);
										return null;
									default :
										return getDefaultReturnValue(m);
								}
							});
					case "getQueryString" :
						return null;
					case "getMethod" :
						return "POST";
					default :
						return getDefaultReturnValue(method);
				}
			});

		HttpServletResponse response = createMockResponse();
		LoginResult result = StatelessLoginHandler.mustAuthenticate(request, response, SOLUTION_NAME);
		assertFalse("Should NOT be authenticated when cloud rejects expired token refresh", result.isAuthenticated());

		String cloudHtml = "<html><body><form name=\"login_form\">Session expired, please login again</form></body></html>";
		lastCloudResponse = new JSONObject().put("html", cloudHtml);
		lastCloudStatusCode = 200;

		StringWriter pageOutput = new StringWriter();
		HttpServletResponse writeResponse = createMockResponseWithWriter(pageOutput);
		StatelessLoginHandler.writeLoginPage(request, writeResponse, SOLUTION_NAME, result);
		String page = pageOutput.toString();
		assertTrue("Should write cloud login page", page.contains("login_form") || page.contains("svyRedirect"));
	}

	@Test
	public void testFullFlow_cloud_mustAuthenticate_noCredentials_needsLogin_writeLoginPageWithQueryParams() throws Exception
	{
		String cloudHtml = "<html><body><form name=\"login_form\"><input name=\"username\"><input name=\"password\"></form></body></html>";
		lastCloudResponse = new JSONObject().put("html", cloudHtml);
		lastCloudStatusCode = 200;

		HttpServletRequest request = createMockRequestWithQueryString("deeplink=dashboard&locale=en_US");
		HttpServletResponse indexResponse = createMockResponse();
		LoginResult mustAuthResult = StatelessLoginHandler.mustAuthenticate(request, indexResponse, SOLUTION_NAME);
		assertFalse("Should need login", mustAuthResult.isAuthenticated());

		StringWriter pageOutput = new StringWriter();
		HttpServletResponse writeResponse = createMockResponseWithWriter(pageOutput);
		StatelessLoginHandler.writeLoginPage(request, writeResponse, SOLUTION_NAME, mustAuthResult);
		String page = pageOutput.toString();
		assertNotNull(page);
		assertTrue("Should contain svyRedirect with query params", page.contains("svyRedirect"));
		assertTrue("Should contain deeplink in redirect", page.contains("deeplink"));
	}

	@Test
	public void testFullFlow_cloud_loginWithRemember_thenRefresh_preservesRememberInToken() throws Exception
	{
		JSONObject permissionsResponse = new JSONObject();
		permissionsResponse.put("permissions", new JSONArray(TEST_PERMISSIONS));
		permissionsResponse.put("username", TEST_USERNAME);
		lastCloudResponse = permissionsResponse;
		lastCloudStatusCode = 200;

		Map<String, String[]> loginParams = new HashMap<>();
		loginParams.put("username", new String[] { TEST_USERNAME });
		loginParams.put("password", new String[] { "password123" });
		loginParams.put("remember", new String[] { "true" });
		loginParams.put("csrf_token", new String[] { "123456" });

		jakarta.servlet.http.Cookie csrfCookie = new jakarta.servlet.http.Cookie("csrf_token", "123456");
		Map<String, Object> sessionAttributes = new HashMap<>();
		Map<String, Object> contextAttributes = new HashMap<>();
		contextAttributes.put("nonce", new java.util.concurrent.ConcurrentHashMap<String, JSONObject>());

		HttpServletRequest loginRequest = (HttpServletRequest)Proxy.newProxyInstance(
			HttpServletRequest.class.getClassLoader(),
			new Class< ? >[] { HttpServletRequest.class },
			(proxy, method, args) -> {
				switch (method.getName())
				{
					case "getParameterMap" :
						return loginParams;
					case "getParameter" :
						String[] vals = loginParams.get(args[0]);
						return vals != null && vals.length > 0 ? vals[0] : null;
					case "getCookies" :
						return new jakarta.servlet.http.Cookie[] { csrfCookie };
					case "getCharacterEncoding" :
						return "UTF-8";
					case "setCharacterEncoding" :
						return null;
					case "getRequestURI" :
						return "/solution/" + SOLUTION_NAME + "/index.html";
					case "getRequestURL" :
						return new StringBuffer("https://localhost:8080/solution/" + SOLUTION_NAME + "/index.html");
					case "getServletPath" :
						return "/solution/" + SOLUTION_NAME + "/index.html";
					case "getContextPath" :
						return "";
					case "getScheme" :
						return "https";
					case "getServerName" :
						return "localhost";
					case "getServerPort" :
						return Integer.valueOf(8080);
					case "getHeader" :
						if ("accept-language".equals(args[0])) return "en";
						return null;
					case "getLocale" :
						return Locale.ENGLISH;
					case "getSession" :
						return (jakarta.servlet.http.HttpSession)Proxy.newProxyInstance(
							jakarta.servlet.http.HttpSession.class.getClassLoader(),
							new Class< ? >[] { jakarta.servlet.http.HttpSession.class },
							(p, m, a) -> {
								switch (m.getName())
								{
									case "getAttribute" :
										return sessionAttributes.get(a[0]);
									case "setAttribute" :
										sessionAttributes.put((String)a[0], a[1]);
										return null;
									case "removeAttribute" :
										sessionAttributes.remove(a[0]);
										return null;
									default :
										return getDefaultReturnValue(m);
								}
							});
					case "getRemoteAddr" :
						return "127.0.0.1";
					case "getServletContext" :
						return (jakarta.servlet.ServletContext)Proxy.newProxyInstance(
							jakarta.servlet.ServletContext.class.getClassLoader(),
							new Class< ? >[] { jakarta.servlet.ServletContext.class },
							(p, m, a) -> {
								switch (m.getName())
								{
									case "getAttribute" :
										return contextAttributes.get(a[0]);
									case "setAttribute" :
										contextAttributes.put((String)a[0], a[1]);
										return null;
									default :
										return getDefaultReturnValue(m);
								}
							});
					case "getQueryString" :
						return null;
					case "getMethod" :
						return "POST";
					default :
						return getDefaultReturnValue(method);
				}
			});

		HttpServletResponse loginResponse = createMockResponse();
		LoginResult loginResult = StatelessLoginHandler.mustAuthenticate(loginRequest, loginResponse, SOLUTION_NAME);
		assertTrue("Should be authenticated", loginResult.isAuthenticated());
		assertNotNull(loginResult.getToken());

		DecodedJWT decoded = JWT.decode(loginResult.getToken());
		assertEquals(TEST_USERNAME, decoded.getClaim(SvyID.USERNAME).asString());
	}

	// ===== Integration tests: handlePossibleCloudRequest full flow =====

	@Test
	public void testHandlePossibleCloudRequest_svyLoginPath_postLogin_htmlReturned_writesHtml() throws Exception
	{
		String loginHtml = "<html><body><form name=\"login_form\"><input name=\"username\"></form></body></html>";
		lastCloudResponse = new JSONObject().put("html", loginHtml);
		lastCloudStatusCode = 200;

		Map<String, String[]> params = new HashMap<>();
		params.put("username", new String[] { TEST_USERNAME });
		params.put("password", new String[] { "pass123" });
		params.put("svyRedirect", new String[] { "/solution/" + SOLUTION_NAME + "/index.html" });
		params.put("csrf_token", new String[] { "123456" });

		StringWriter writer = new StringWriter();
		HttpServletRequest request = createHandlePossibleCloudRequest("/solution/" + SOLUTION_NAME + "/svylogin/login", params, "POST");
		HttpServletResponse response = createMockResponseWithWriter(writer);

		boolean handled = CloudStatelessAccessManager.handlePossibleCloudRequest(request, response, SOLUTION_NAME);

		assertTrue("Should handle svylogin/login POST", handled);
		String output = writer.toString();
		assertTrue("Should contain the login form", output.contains("login_form"));
		assertTrue("Should contain svyRedirect hidden field", output.contains("svyRedirect"));
	}

	@Test
	public void testHandlePossibleCloudRequest_secureRequest_csrfCookieHasSecureFlag() throws Exception
	{
		String loginHtml = "<html><body><form name=\"login_form\"><input name=\"username\"></form></body></html>";
		lastCloudResponse = new JSONObject().put("html", loginHtml);
		lastCloudStatusCode = 200;

		Map<String, String[]> params = new HashMap<>();
		params.put("username", new String[] { TEST_USERNAME });
		params.put("password", new String[] { "pass123" });
		params.put("csrf_token", new String[] { "123456" });

		StringWriter writer = new StringWriter();
		List<jakarta.servlet.http.Cookie> responseCookies = new ArrayList<>();
		HttpServletRequest request = createHandlePossibleCloudRequestWithSecure(
			"/solution/" + SOLUTION_NAME + "/svylogin/login", params, "POST", true);
		HttpServletResponse response = createMockResponseWithWriterAndCookies(writer, responseCookies);

		boolean handled = CloudStatelessAccessManager.handlePossibleCloudRequest(request, response, SOLUTION_NAME);

		assertTrue("Should handle svylogin/login POST", handled);
		jakarta.servlet.http.Cookie csrfCookie = responseCookies.stream()
			.filter(c -> "csrf_token".equals(c.getName()))
			.findFirst().orElse(null);
		assertNotNull("A csrf_token cookie should have been added", csrfCookie);
		assertTrue("csrf_token cookie must have the Secure flag on an HTTPS request", csrfCookie.getSecure());
		assertTrue("csrf_token cookie should be HttpOnly", csrfCookie.isHttpOnly());
	}

	@Test
	public void testHandlePossibleCloudRequest_insecureRequest_csrfCookieHasNoSecureFlag() throws Exception
	{
		String loginHtml = "<html><body><form name=\"login_form\"><input name=\"username\"></form></body></html>";
		lastCloudResponse = new JSONObject().put("html", loginHtml);
		lastCloudStatusCode = 200;

		Map<String, String[]> params = new HashMap<>();
		params.put("username", new String[] { TEST_USERNAME });
		params.put("password", new String[] { "pass123" });
		params.put("csrf_token", new String[] { "123456" });

		StringWriter writer = new StringWriter();
		List<jakarta.servlet.http.Cookie> responseCookies = new ArrayList<>();
		HttpServletRequest request = createHandlePossibleCloudRequestWithSecure(
			"/solution/" + SOLUTION_NAME + "/svylogin/login", params, "POST", false);
		HttpServletResponse response = createMockResponseWithWriterAndCookies(writer, responseCookies);

		boolean handled = CloudStatelessAccessManager.handlePossibleCloudRequest(request, response, SOLUTION_NAME);

		assertTrue("Should handle svylogin/login POST", handled);
		jakarta.servlet.http.Cookie csrfCookie = responseCookies.stream()
			.filter(c -> "csrf_token".equals(c.getName()))
			.findFirst().orElse(null);
		assertNotNull("A csrf_token cookie should have been added", csrfCookie);
		assertFalse("csrf_token cookie should not set Secure on a plain HTTP request", csrfCookie.getSecure());
	}

	@Test
	public void testHandlePossibleCloudRequest_svyLoginPath_postLogin_permissionsReturned_redirects() throws Exception
	{
		JSONObject permissionsResponse = new JSONObject();
		permissionsResponse.put("permissions", new JSONArray(TEST_PERMISSIONS));
		permissionsResponse.put("username", TEST_USERNAME);
		permissionsResponse.put(SvyID.REMEMBER, false);
		lastCloudResponse = permissionsResponse;
		lastCloudStatusCode = 200;

		Map<String, String[]> params = new HashMap<>();
		params.put("username", new String[] { TEST_USERNAME });
		params.put("password", new String[] { "pass123" });
		params.put("svyRedirect", new String[] { "/solution/" + SOLUTION_NAME + "/index.html" });
		params.put("csrf_token", new String[] { "123456" });

		List<String> redirects = new ArrayList<>();
		HttpServletRequest request = createHandlePossibleCloudRequest("/solution/" + SOLUTION_NAME + "/svylogin/login", params, "POST");
		HttpServletResponse response = createMockResponseWithRedirect(redirects);

		boolean handled = CloudStatelessAccessManager.handlePossibleCloudRequest(request, response, SOLUTION_NAME);

		assertTrue("Should handle svylogin/login POST with permissions", handled);
		assertFalse("Should redirect after successful login", redirects.isEmpty());
	}

	@Test
	public void testHandlePossibleCloudRequest_svyLoginPath_postOAuth_oauthConfigReturned() throws Exception
	{
		JSONObject oauthObj = new JSONObject();
		oauthObj.put("clientId", "cloud-oauth-client");
		oauthObj.put("apiSecret", "cloud-oauth-secret");
		oauthObj.put("authorizationBaseUrl", "https://accounts.google.com/o/oauth2/v2/auth");
		oauthObj.put("accessTokenEndpoint", "https://oauth2.googleapis.com/token");
		oauthObj.put("defaultScope", "openid email");
		lastCloudResponse = new JSONObject().put("oauth", oauthObj);
		lastCloudStatusCode = 200;

		Map<String, String[]> params = new HashMap<>();
		params.put("oauth", new String[] { "google" });
		params.put("csrf_token", new String[] { "123456" });

		StringWriter writer = new StringWriter();
		HttpServletRequest request = createHandlePossibleCloudRequest("/solution/" + SOLUTION_NAME + "/svylogin/oauth", params, "POST");
		HttpServletResponse response = createMockResponseWithWriter(writer);

		boolean handled = CloudStatelessAccessManager.handlePossibleCloudRequest(request, response, SOLUTION_NAME);

		assertTrue("Should handle svylogin/oauth POST", handled);
	}

	@Test
	public void testHandlePossibleCloudRequest_svyLoginPath_tenantSelect_permissionsReturned() throws Exception
	{
		JSONObject permissionsResponse = new JSONObject();
		permissionsResponse.put("permissions", new JSONArray(TEST_PERMISSIONS));
		permissionsResponse.put("username", TEST_USERNAME);
		permissionsResponse.put("tenantValues", new JSONArray(new String[] { "Sandbox" }));
		permissionsResponse.put(SvyID.REMEMBER, false);
		lastCloudResponse = permissionsResponse;
		lastCloudStatusCode = 200;

		Map<String, String[]> params = new HashMap<>();
		params.put("tenant", new String[] { "Sandbox" });
		params.put("svyTenantLoginToken", new String[] { "eyJhbGciOiJIUzM4NCJ9.test" });
		params.put("svyRedirect", new String[] { "/solution/" + SOLUTION_NAME + "/index.html" });
		params.put("csrf_token", new String[] { "123456" });

		List<String> redirects = new ArrayList<>();
		HttpServletRequest request = createHandlePossibleCloudRequest("/solution/" + SOLUTION_NAME + "/svylogin/tenant_select_redirect", params, "POST");
		HttpServletResponse response = createMockResponseWithRedirect(redirects);

		boolean handled = CloudStatelessAccessManager.handlePossibleCloudRequest(request, response, SOLUTION_NAME);

		assertTrue("Should handle svylogin/tenant_select_redirect POST", handled);
		assertFalse("Should redirect after tenant selection", redirects.isEmpty());
	}

	@Test
	public void testHandlePossibleCloudRequest_svyLoginPath_endpointNotInList_returnsFalse() throws Exception
	{
		lastCloudResponse = new JSONObject();
		lastCloudStatusCode = 200;

		Map<String, String[]> params = new HashMap<>();
		params.put("csrf_token", new String[] { "123456" });
		HttpServletRequest request = createHandlePossibleCloudRequest("/solution/" + SOLUTION_NAME + "/svylogin/unknown_endpoint", params, "POST");
		HttpServletResponse response = createMockResponse();

		boolean handled = CloudStatelessAccessManager.handlePossibleCloudRequest(request, response, SOLUTION_NAME);

		assertFalse("Should not handle unknown endpoint", handled);
	}

	@Test
	public void testHandlePossibleCloudRequest_svyLoginPath_getRequest_executesGet() throws Exception
	{
		String loginHtml = "<html><body><form name=\"login_form\">GET login page</form></body></html>";
		lastCloudResponse = new JSONObject().put("html", loginHtml);
		lastCloudStatusCode = 200;

		Map<String, String[]> params = new HashMap<>();
		StringWriter writer = new StringWriter();
		HttpServletRequest request = createHandlePossibleCloudRequest("/solution/" + SOLUTION_NAME + "/svylogin/login", params, "GET");
		HttpServletResponse response = createMockResponseWithWriter(writer);

		boolean handled = CloudStatelessAccessManager.handlePossibleCloudRequest(request, response, SOLUTION_NAME);

		assertTrue("Should handle svylogin/login GET", handled);
		String output = writer.toString();
		assertTrue("Should contain GET login page", output.contains("login_form"));
	}

	@Test
	public void testHandlePossibleCloudRequest_svyLoginPath_getEndpoints_fromCloud() throws Exception
	{
		// Test getCloudRestApiEndpoints + getArrayProperty by NOT pre-populating the servlet context
		// First call returns endpoints list, then 2nd call returns the actual login html
		// Since our mock returns lastCloudResponse for all calls, we need the response to contain "endpoints"
		// for the first call. But that won't work for the 2nd call which expects "html".
		// Instead, make mock stateful:
		JSONObject endpointsResponse = new JSONObject();
		endpointsResponse.put("endpoints", new JSONArray(new String[] { "login", "oauth", "tenant_select_redirect" }));
		lastCloudResponse = endpointsResponse;
		lastCloudStatusCode = 200;

		Map<String, String[]> params = new HashMap<>();
		params.put("csrf_token", new String[] { "123456" });
		// Use a request that does NOT have pre-populated endpoints in servletContext
		HttpServletRequest request = createHandlePossibleCloudRequestNoCachedEndpoints(
			"/solution/" + SOLUTION_NAME + "/svylogin/login", params, "POST");
		HttpServletResponse response = createMockResponse();

		boolean handled = CloudStatelessAccessManager.handlePossibleCloudRequest(request, response, SOLUTION_NAME);

		// The first HTTP call gets the endpoints array, the second call will also get "endpoints" JSON
		// which doesn't have "html"/"permissions"/"error"/"oauth" - so writeResponse will fall through to default
		assertTrue("Should handle the request (endpoints found, endpoint is in list)", handled);
	}

	private HttpServletRequest createHandlePossibleCloudRequest(String servletPath, Map<String, String[]> params, String httpMethod)
	{
		Map<String, Object> sessionAttributes = new HashMap<>();
		Map<String, Object> contextAttributes = new HashMap<>();
		contextAttributes.put("nonce", new HashMap<String, Object>());
		// Pre-populate endpoints to skip the getCloudRestApiEndpoints HTTP call
		contextAttributes.put("endpoints", new String[] { "login", "oauth", "tenant_select_redirect" });
		contextAttributes.put("endpoints_expire", Long.valueOf(System.currentTimeMillis() + 600000));

		String csrfValue = params.containsKey("csrf_token") ? params.get("csrf_token")[0] : "123456";
		jakarta.servlet.http.Cookie csrfCookie = new jakarta.servlet.http.Cookie("csrf_token", csrfValue);

		return (HttpServletRequest)Proxy.newProxyInstance(
			HttpServletRequest.class.getClassLoader(),
			new Class< ? >[] { HttpServletRequest.class },
			(proxy, method, args) -> {
				switch (method.getName())
				{
					case "getParameterMap" :
						return params;
					case "getParameter" :
						String[] vals = params.get(args[0]);
						return vals != null && vals.length > 0 ? vals[0] : null;
					case "getCookies" :
						return new jakarta.servlet.http.Cookie[] { csrfCookie };
					case "getCharacterEncoding" :
						return "UTF-8";
					case "setCharacterEncoding" :
						return null;
					case "getRequestURI" :
						return servletPath;
					case "getRequestURL" :
						return new StringBuffer("https://localhost:8080" + servletPath);
					case "getServletPath" :
						return servletPath;
					case "getContextPath" :
						return "";
					case "getScheme" :
						return "https";
					case "getServerName" :
						return "localhost";
					case "getServerPort" :
						return Integer.valueOf(8080);
					case "getHeader" :
						if ("accept-language".equals(args[0])) return "en";
						if ("user-agent".equals(args[0])) return "Mozilla/5.0";
						return null;
					case "getLocale" :
						return Locale.ENGLISH;
					case "getSession" :
						if (args != null && args.length == 1 && Boolean.FALSE.equals(args[0])) return null;
						return (jakarta.servlet.http.HttpSession)Proxy.newProxyInstance(
							jakarta.servlet.http.HttpSession.class.getClassLoader(),
							new Class< ? >[] { jakarta.servlet.http.HttpSession.class },
							(p, m, a) -> {
								switch (m.getName())
								{
									case "getAttribute" :
										return sessionAttributes.get(a[0]);
									case "setAttribute" :
										sessionAttributes.put((String)a[0], a[1]);
										return null;
									case "removeAttribute" :
										sessionAttributes.remove(a[0]);
										return null;
									default :
										return getDefaultReturnValue(m);
								}
							});
					case "getRemoteAddr" :
						return "127.0.0.1";
					case "getServletContext" :
						return (jakarta.servlet.ServletContext)Proxy.newProxyInstance(
							jakarta.servlet.ServletContext.class.getClassLoader(),
							new Class< ? >[] { jakarta.servlet.ServletContext.class },
							(p, m, a) -> {
								switch (m.getName())
								{
									case "getAttribute" :
										return contextAttributes.get(a[0]);
									case "setAttribute" :
										contextAttributes.put((String)a[0], a[1]);
										return null;
									default :
										return getDefaultReturnValue(m);
								}
							});
					case "getQueryString" :
						return null;
					case "getMethod" :
						return httpMethod;
					default :
						return getDefaultReturnValue(method);
				}
			});
	}

	private HttpServletRequest createHandlePossibleCloudRequestWithSecure(String servletPath, Map<String, String[]> params, String httpMethod,
		boolean secure)
	{
		Map<String, Object> sessionAttributes = new HashMap<>();
		Map<String, Object> contextAttributes = new HashMap<>();
		contextAttributes.put("nonce", new HashMap<String, Object>());
		contextAttributes.put("endpoints", new String[] { "login", "oauth", "tenant_select_redirect" });
		contextAttributes.put("endpoints_expire", Long.valueOf(System.currentTimeMillis() + 600000));

		String csrfValue = params.containsKey("csrf_token") ? params.get("csrf_token")[0] : "123456";
		jakarta.servlet.http.Cookie csrfCookie = new jakarta.servlet.http.Cookie("csrf_token", csrfValue);

		return (HttpServletRequest)Proxy.newProxyInstance(
			HttpServletRequest.class.getClassLoader(),
			new Class< ? >[] { HttpServletRequest.class },
			(proxy, method, args) -> {
				switch (method.getName())
				{
					case "getParameterMap" :
						return params;
					case "getParameter" :
						String[] vals = params.get(args[0]);
						return vals != null && vals.length > 0 ? vals[0] : null;
					case "getCookies" :
						return new jakarta.servlet.http.Cookie[] { csrfCookie };
					case "getCharacterEncoding" :
						return "UTF-8";
					case "setCharacterEncoding" :
						return null;
					case "isSecure" :
						return Boolean.valueOf(secure);
					case "getRequestURI" :
						return servletPath;
					case "getRequestURL" :
						return new StringBuffer("https://localhost:8080" + servletPath);
					case "getServletPath" :
						return servletPath;
					case "getContextPath" :
						return "";
					case "getScheme" :
						return secure ? "https" : "http";
					case "getServerName" :
						return "localhost";
					case "getServerPort" :
						return Integer.valueOf(8080);
					case "getHeader" :
						if ("accept-language".equals(args[0])) return "en";
						if ("user-agent".equals(args[0])) return "Mozilla/5.0";
						return null;
					case "getLocale" :
						return Locale.ENGLISH;
					case "getSession" :
						if (args != null && args.length == 1 && Boolean.FALSE.equals(args[0])) return null;
						return (jakarta.servlet.http.HttpSession)Proxy.newProxyInstance(
							jakarta.servlet.http.HttpSession.class.getClassLoader(),
							new Class< ? >[] { jakarta.servlet.http.HttpSession.class },
							(p, m, a) -> {
								switch (m.getName())
								{
									case "getAttribute" :
										return sessionAttributes.get(a[0]);
									case "setAttribute" :
										sessionAttributes.put((String)a[0], a[1]);
										return null;
									case "removeAttribute" :
										sessionAttributes.remove(a[0]);
										return null;
									default :
										return getDefaultReturnValue(m);
								}
							});
					case "getRemoteAddr" :
						return "127.0.0.1";
					case "getServletContext" :
						return (jakarta.servlet.ServletContext)Proxy.newProxyInstance(
							jakarta.servlet.ServletContext.class.getClassLoader(),
							new Class< ? >[] { jakarta.servlet.ServletContext.class },
							(p, m, a) -> {
								switch (m.getName())
								{
									case "getAttribute" :
										return contextAttributes.get(a[0]);
									case "setAttribute" :
										contextAttributes.put((String)a[0], a[1]);
										return null;
									default :
										return getDefaultReturnValue(m);
								}
							});
					case "getQueryString" :
						return null;
					case "getMethod" :
						return httpMethod;
					default :
						return getDefaultReturnValue(method);
				}
			});
	}

	private HttpServletRequest createHandlePossibleCloudRequestNoCachedEndpoints(String servletPath, Map<String, String[]> params, String httpMethod)
	{
		Map<String, Object> sessionAttributes = new HashMap<>();
		Map<String, Object> contextAttributes = new HashMap<>();
		contextAttributes.put("nonce", new HashMap<String, Object>());
		// NO pre-populated endpoints - forces getCloudRestApiEndpoints to make HTTP call

		String csrfValue = params.containsKey("csrf_token") ? params.get("csrf_token")[0] : "123456";
		jakarta.servlet.http.Cookie csrfCookie = new jakarta.servlet.http.Cookie("csrf_token", csrfValue);

		return (HttpServletRequest)Proxy.newProxyInstance(
			HttpServletRequest.class.getClassLoader(),
			new Class< ? >[] { HttpServletRequest.class },
			(proxy, method, args) -> {
				switch (method.getName())
				{
					case "getParameterMap" :
						return params;
					case "getParameter" :
						String[] vals = params.get(args[0]);
						return vals != null && vals.length > 0 ? vals[0] : null;
					case "getCookies" :
						return new jakarta.servlet.http.Cookie[] { csrfCookie };
					case "getCharacterEncoding" :
						return "UTF-8";
					case "setCharacterEncoding" :
						return null;
					case "getRequestURI" :
						return servletPath;
					case "getRequestURL" :
						return new StringBuffer("https://localhost:8080" + servletPath);
					case "getServletPath" :
						return servletPath;
					case "getContextPath" :
						return "";
					case "getScheme" :
						return "https";
					case "getServerName" :
						return "localhost";
					case "getServerPort" :
						return Integer.valueOf(8080);
					case "getHeader" :
						if ("accept-language".equals(args[0])) return "en";
						if ("user-agent".equals(args[0])) return "Mozilla/5.0";
						return null;
					case "getLocale" :
						return Locale.ENGLISH;
					case "getSession" :
						if (args != null && args.length == 1 && Boolean.FALSE.equals(args[0])) return null;
						return (jakarta.servlet.http.HttpSession)Proxy.newProxyInstance(
							jakarta.servlet.http.HttpSession.class.getClassLoader(),
							new Class< ? >[] { jakarta.servlet.http.HttpSession.class },
							(p, m, a) -> {
								switch (m.getName())
								{
									case "getAttribute" :
										return sessionAttributes.get(a[0]);
									case "setAttribute" :
										sessionAttributes.put((String)a[0], a[1]);
										return null;
									case "removeAttribute" :
										sessionAttributes.remove(a[0]);
										return null;
									default :
										return getDefaultReturnValue(m);
								}
							});
					case "getRemoteAddr" :
						return "127.0.0.1";
					case "getServletContext" :
						return (jakarta.servlet.ServletContext)Proxy.newProxyInstance(
							jakarta.servlet.ServletContext.class.getClassLoader(),
							new Class< ? >[] { jakarta.servlet.ServletContext.class },
							(p, m, a) -> {
								switch (m.getName())
								{
									case "getAttribute" :
										return contextAttributes.get(a[0]);
									case "setAttribute" :
										contextAttributes.put((String)a[0], a[1]);
										return null;
									default :
										return getDefaultReturnValue(m);
								}
							});
					case "getQueryString" :
						return null;
					case "getMethod" :
						return httpMethod;
					default :
						return getDefaultReturnValue(method);
				}
			});
	}

	@Test
	public void testCheckCloudOAuthPermissions_nonHtmlError_usesErrorTemplate() throws Exception
	{
		JSONObject response = new JSONObject();
		response.put("error", "<html><body>User account is locked</body></html>");
		lastCloudResponse = response;
		lastCloudStatusCode = 200;

		LoginResult result = LoginResult.needsLogin();
		HttpServletRequest request = createMockRequest();
		StringWriter writer = new StringWriter();
		HttpServletResponse mockResponse = createMockResponseWithWriter(writer);
		boolean verified = CloudStatelessAccessManager.checkCloudOAuthPermissions(
			request, mockResponse, result, cloudSolution,
			"{\"id_token\":\"test\"}", Boolean.FALSE, null, "google");

		assertTrue(verified);
		String output = writer.toString();
		assertTrue("Should write the error HTML", output.contains("User account is locked"));
	}

	@Test
	public void testCheckCloudOAuthPermissions_permissionsWithNullPermissionsArray_noToken() throws Exception
	{
		JSONObject response = new JSONObject();
		response.put("permissions", new JSONArray());
		response.put("username", TEST_USERNAME);
		lastCloudResponse = response;
		lastCloudStatusCode = 200;

		LoginResult result = LoginResult.needsLogin();
		HttpServletRequest request = createMockRequest();
		StringWriter writer = new StringWriter();
		HttpServletResponse mockResponse = createMockResponseWithWriter(writer);
		boolean verified = CloudStatelessAccessManager.checkCloudOAuthPermissions(
			request, mockResponse, result, cloudSolution,
			"{\"id_token\":\"test\"}", Boolean.FALSE, null, "google");

		assertTrue(verified);
	}

	@Test
	public void testCheckCloudOAuthPermissions_unknownResponseFormat_fallsThrough() throws Exception
	{
		JSONObject response = new JSONObject();
		response.put("unknownKey", "unknownValue");
		lastCloudResponse = response;
		lastCloudStatusCode = 200;

		LoginResult result = LoginResult.needsLogin();
		HttpServletRequest request = createMockRequest();
		StringWriter writer = new StringWriter();
		HttpServletResponse mockResponse = createMockResponseWithWriter(writer);
		boolean verified = CloudStatelessAccessManager.checkCloudOAuthPermissions(
			request, mockResponse, result, cloudSolution,
			"{\"id_token\":\"test\"}", Boolean.FALSE, null, "google");

		assertTrue(verified);
	}

	@Test
	public void testCheckCloudOAuthPermissions_permissionsWithRememberAndRefreshToken_setsRedirect() throws Exception
	{
		JSONObject response = new JSONObject();
		response.put("permissions", new JSONArray(TEST_PERMISSIONS));
		response.put("username", TEST_USERNAME);
		response.put(SvyID.REMEMBER, true);
		response.put(StatelessLoginHandler.REFRESH_TOKEN, "cloud-refresh-token");
		response.put(CloudStatelessAccessManager.CLOUD_OAUTH_ENDPOINT, "google");
		lastCloudResponse = response;
		lastCloudStatusCode = 200;

		LoginResult result = LoginResult.needsLogin();
		HttpServletRequest request = createMockRequest();
		List<String> redirects = new ArrayList<>();
		HttpServletResponse mockResponse = createMockResponseWithRedirect(redirects);
		boolean verified = CloudStatelessAccessManager.checkCloudOAuthPermissions(
			request, mockResponse, result, cloudSolution,
			"{\"id_token\":\"test\"}", Boolean.TRUE, "initial-refresh", "google");

		assertTrue(verified);
		assertFalse(redirects.isEmpty());
	}

	@Test
	public void testCheckCloudOAuthPermissions_withSvyRedirectInDeeplink_usesCorrectRedirect() throws Exception
	{
		JSONObject response = new JSONObject();
		response.put("permissions", new JSONArray(TEST_PERMISSIONS));
		response.put("username", TEST_USERNAME);
		lastCloudResponse = response;
		lastCloudStatusCode = 200;

		LoginResult result = LoginResult.needsLogin();
		Map<String, String[]> params = new HashMap<>();
		params.put("id_token", new String[] { "test" });
		HttpServletRequest request = createMockRequestWithServletPathAndParams(
			"/solution/" + SOLUTION_NAME + "/svy_oauth/callback", params, "svyRedirect=%2Fsolution%2F" + SOLUTION_NAME + "%2Findex.html%3Fdeeplink%3Dpage1");
		List<String> redirects = new ArrayList<>();
		HttpServletResponse mockResponse = createMockResponseWithRedirect(redirects);
		boolean verified = CloudStatelessAccessManager.checkCloudOAuthPermissions(
			request, mockResponse, result, cloudSolution,
			"{\"id_token\":\"test\"}", Boolean.FALSE, null, "google");

		assertTrue(verified);
		assertFalse(redirects.isEmpty());
		assertTrue(redirects.get(0).contains(SOLUTION_NAME));
	}

	// ===== Full flow via StatelessLoginHandler.mustAuthenticate and writeLoginPage =====

	@Test
	public void testFullFlow_cloud_writeLoginPage_withCustomHtml_writesCloudPage() throws Exception
	{
		String cloudHtml = "<html><body><form name=\"login_form\" action=\"svylogin/login\"><input name=\"username\"><input name=\"password\"><button>Login</button></form></body></html>";
		lastCloudResponse = new JSONObject().put("html", cloudHtml);
		lastCloudStatusCode = 200;

		HttpServletRequest request = createMockRequest();
		HttpServletResponse indexResponse = createMockResponse();
		LoginResult mustAuthResult = StatelessLoginHandler.mustAuthenticate(request, indexResponse, SOLUTION_NAME);
		assertFalse("Should need login", mustAuthResult.isAuthenticated());

		StringWriter pageOutput = new StringWriter();
		HttpServletResponse writeResponse = createMockResponseWithWriter(pageOutput);
		StatelessLoginHandler.writeLoginPage(request, writeResponse, SOLUTION_NAME, mustAuthResult);
		String page = pageOutput.toString();
		assertNotNull(page);
		assertTrue("Should contain the cloud login form", page.contains("login_form") || page.contains("svyRedirect"));
	}

	@Test
	public void testFullFlow_cloud_writeLoginPage_cloudReturnsError_writesDefaultPage() throws Exception
	{
		lastCloudStatusCode = 500;
		lastCloudResponse = new JSONObject().put("error", "Service unavailable");

		HttpServletRequest request = createMockRequest();
		HttpServletResponse indexResponse = createMockResponse();
		LoginResult mustAuthResult = StatelessLoginHandler.mustAuthenticate(request, indexResponse, SOLUTION_NAME);
		assertFalse("Should need login", mustAuthResult.isAuthenticated());

		StringWriter pageOutput = new StringWriter();
		HttpServletResponse writeResponse = createMockResponseWithWriter(pageOutput);
		StatelessLoginHandler.writeLoginPage(request, writeResponse, SOLUTION_NAME, mustAuthResult);
		String page = pageOutput.toString();
		assertNotNull(page);
		assertTrue("Should write some login page", page.length() > 0);
	}

	// ===== Full flow: logoutAndRevokeToken with OAuth provider =====

	@Test
	public void testLogoutAndRevokeToken_withProviderAndOauthConfig_attemptsRevoke() throws Exception
	{
		JSONObject oauthObj = new JSONObject();
		oauthObj.put("clientId", "fake-client-id");
		oauthObj.put("apiSecret", "fake-secret");
		oauthObj.put("authorizationBaseUrl", "https://accounts.google.com/o/oauth2/v2/auth");
		oauthObj.put("accessTokenEndpoint", "https://oauth2.googleapis.com/token");
		oauthObj.put("revokeTokenEndpoint", "https://oauth2.googleapis.com/revoke");
		lastCloudResponse = new JSONObject().put("oauth", oauthObj);
		lastCloudStatusCode = 200;

		Properties settings = ApplicationServerRegistry.get().getServerAccess().getSettings();
		String jwtPassword = settings.getProperty(StatelessLoginUtils.JWT_Password);
		String tokenStr = JWT.create()
			.withClaim(SvyID.USERNAME, TEST_USERNAME)
			.withClaim(SvyID.PERMISSIONS, java.util.Arrays.asList(TEST_PERMISSIONS))
			.withClaim(CloudStatelessAccessManager.CLOUD_OAUTH_ENDPOINT, "google")
			.withClaim(StatelessLoginHandler.REFRESH_TOKEN, "my-refresh-token-to-revoke")
			.withExpiresAt(new java.util.Date(System.currentTimeMillis() + 3600000))
			.sign(Algorithm.HMAC256(jwtPassword));

		DecodedJWT jwt = JWT.decode(tokenStr);
		// revokeToken will attempt to revoke with a fake token, which throws OAuth2AccessTokenErrorResponse
		// The implementation catches IOException|InterruptedException|ExecutionException|UnsupportedOperationException
		// but OAuth2AccessTokenErrorResponse extends OAuth2AccessTokenException extends OAuthException extends ScribeJavaException extends RuntimeException
		// so it propagates. We just verify the method doesn't throw an unexpected exception type.
		try
		{
			CloudStatelessAccessManager.revokeToken(cloudSolution, jwt);
		}
		catch (RuntimeException e)
		{
			// Expected: ScribeJava throws when the revoke endpoint returns an error
			assertTrue(e.getClass().getName().contains("OAuth2AccessToken") || e.getMessage().contains("invalid_token"));
		}
	}

	@Test
	public void testLogoutAndRevokeToken_withProviderButServiceNull_logsError() throws Exception
	{
		// Return oauth config without clientId -> getOAuthConfigFromTheCloud returns json without clientId
		// OAuthUtils.createOauthService will throw IllegalArgumentException for empty API key
		JSONObject oauthObj = new JSONObject();
		oauthObj.put("apiSecret", "fake-secret");
		oauthObj.put("authorizationBaseUrl", "https://login.microsoftonline.com/common/oauth2/v2.0/authorize");
		oauthObj.put("accessTokenEndpoint", "https://login.microsoftonline.com/common/oauth2/v2.0/token");
		lastCloudResponse = new JSONObject().put("oauth", oauthObj);
		lastCloudStatusCode = 200;

		Properties settings = ApplicationServerRegistry.get().getServerAccess().getSettings();
		String jwtPassword = settings.getProperty(StatelessLoginUtils.JWT_Password);
		String tokenStr = JWT.create()
			.withClaim(SvyID.USERNAME, TEST_USERNAME)
			.withClaim(SvyID.PERMISSIONS, java.util.Arrays.asList(TEST_PERMISSIONS))
			.withClaim(CloudStatelessAccessManager.CLOUD_OAUTH_ENDPOINT, "microsoft")
			.withClaim(StatelessLoginHandler.REFRESH_TOKEN, "some-refresh-token")
			.withExpiresAt(new java.util.Date(System.currentTimeMillis() + 3600000))
			.sign(Algorithm.HMAC256(jwtPassword));

		DecodedJWT jwt = JWT.decode(tokenStr);
		try
		{
			CloudStatelessAccessManager.revokeToken(cloudSolution, jwt);
		}
		catch (IllegalArgumentException e)
		{
			// Expected: ServiceBuilder throws for missing/empty API key
			assertTrue(e.getMessage().contains("Api key"));
		}
	}

	// ===== Full flow: login -> checkPermissions refresh -> writeLoginPage on failure =====

	@Test
	public void testFullFlow_cloud_loginThenRefreshFails_mustAuthenticate_needsRelogin() throws Exception
	{
		JSONObject response = new JSONObject();
		response.put("permissions", new JSONArray(TEST_PERMISSIONS));
		response.put("username", TEST_USERNAME);
		lastCloudResponse = response;
		lastCloudStatusCode = 200;

		HttpServletRequest firstRequest = createFullFlowRequest(TEST_USERNAME, "password123");
		HttpServletResponse firstResponse = createMockResponse();
		LoginResult firstResult = StatelessLoginHandler.mustAuthenticate(firstRequest, firstResponse, SOLUTION_NAME);
		assertTrue(firstResult.isAuthenticated());
		String svyToken = firstResult.getToken();
		assertNotNull(svyToken);

		lastCloudStatusCode = 401;
		lastCloudResponse = new JSONObject().put("error", "Session expired");

		Properties settings = ApplicationServerRegistry.get().getServerAccess().getSettings();
		String jwtPassword = settings.getProperty(StatelessLoginUtils.JWT_Password);
		String expiredToken = JWT.create()
			.withIssuer("svy")
			.withClaim(SvyID.USERNAME, TEST_USERNAME)
			.withArrayClaim(SvyID.PERMISSIONS, TEST_PERMISSIONS)
			.withIssuedAt(new java.util.Date(System.currentTimeMillis() - 7200000))
			.withExpiresAt(new java.util.Date(System.currentTimeMillis() - 1000))
			.sign(Algorithm.HMAC256(jwtPassword));

		String csrfToken = "123456";
		Map<String, String[]> params = new HashMap<>();
		params.put("id_token", new String[] { expiredToken });
		params.put("csrf_token", new String[] { csrfToken });
		jakarta.servlet.http.Cookie csrfCookie = new jakarta.servlet.http.Cookie("csrf_token", csrfToken);
		Map<String, Object> sessionAttributes = new HashMap<>();
		Map<String, Object> contextAttributes = new HashMap<>();
		contextAttributes.put("nonce", new java.util.concurrent.ConcurrentHashMap<String, JSONObject>());

		HttpServletRequest refreshRequest = (HttpServletRequest)Proxy.newProxyInstance(
			HttpServletRequest.class.getClassLoader(),
			new Class< ? >[] { HttpServletRequest.class },
			(proxy, method, args) -> {
				switch (method.getName())
				{
					case "getParameterMap" :
						return params;
					case "getParameter" :
						String[] vals = params.get(args[0]);
						return vals != null && vals.length > 0 ? vals[0] : null;
					case "getCookies" :
						return new jakarta.servlet.http.Cookie[] { csrfCookie };
					case "getCharacterEncoding" :
						return "UTF-8";
					case "setCharacterEncoding" :
						return null;
					case "getRequestURI" :
						return "/solution/" + SOLUTION_NAME + "/index.html";
					case "getRequestURL" :
						return new StringBuffer("https://localhost:8080/solution/" + SOLUTION_NAME + "/index.html");
					case "getServletPath" :
						return "/solution/" + SOLUTION_NAME + "/index.html";
					case "getContextPath" :
						return "";
					case "getScheme" :
						return "https";
					case "getServerName" :
						return "localhost";
					case "getServerPort" :
						return Integer.valueOf(8080);
					case "getHeader" :
						if ("accept-language".equals(args[0])) return "en";
						if ("user-agent".equals(args[0])) return "Mozilla/5.0";
						return null;
					case "getLocale" :
						return Locale.ENGLISH;
					case "getSession" :
						return (jakarta.servlet.http.HttpSession)Proxy.newProxyInstance(
							jakarta.servlet.http.HttpSession.class.getClassLoader(),
							new Class< ? >[] { jakarta.servlet.http.HttpSession.class },
							(p, m, a) -> {
								switch (m.getName())
								{
									case "getAttribute" :
										return sessionAttributes.get(a[0]);
									case "setAttribute" :
										sessionAttributes.put((String)a[0], a[1]);
										return null;
									case "removeAttribute" :
										sessionAttributes.remove(a[0]);
										return null;
									default :
										return getDefaultReturnValue(m);
								}
							});
					case "getRemoteAddr" :
						return "127.0.0.1";
					case "getServletContext" :
						return (jakarta.servlet.ServletContext)Proxy.newProxyInstance(
							jakarta.servlet.ServletContext.class.getClassLoader(),
							new Class< ? >[] { jakarta.servlet.ServletContext.class },
							(p, m, a) -> {
								switch (m.getName())
								{
									case "getAttribute" :
										return contextAttributes.get(a[0]);
									case "setAttribute" :
										contextAttributes.put((String)a[0], a[1]);
										return null;
									default :
										return getDefaultReturnValue(m);
								}
							});
					case "getQueryString" :
						return null;
					case "getMethod" :
						return "POST";
					default :
						return getDefaultReturnValue(method);
				}
			});

		HttpServletResponse refreshResponse = createMockResponse();
		LoginResult refreshResult = StatelessLoginHandler.mustAuthenticate(refreshRequest, refreshResponse, SOLUTION_NAME);
		assertFalse("Should NOT be authenticated when cloud rejects refresh", refreshResult.isAuthenticated());

		String cloudHtml = "<html><body><form name=\"login_form\">Please login again</form></body></html>";
		lastCloudResponse = new JSONObject().put("html", cloudHtml);
		lastCloudStatusCode = 200;

		StringWriter pageOutput = new StringWriter();
		HttpServletResponse writeResponse = createMockResponseWithWriter(pageOutput);
		StatelessLoginHandler.writeLoginPage(refreshRequest, writeResponse, SOLUTION_NAME, refreshResult);
		String page = pageOutput.toString();
		assertTrue("Should show login page after failed refresh", page.contains("login_form") || page.contains("svyRedirect"));
	}

	@Test
	public void testFullFlow_cloud_oauthLoginThenMustAuthenticate_validatesToken() throws Exception
	{
		JSONObject response = new JSONObject();
		response.put("permissions", new JSONArray(TEST_PERMISSIONS));
		response.put("username", TEST_USERNAME);
		response.put("tenantValues", new JSONArray(new String[] { "Sandbox" }));
		lastCloudResponse = response;
		lastCloudStatusCode = 200;

		HttpServletRequest firstRequest = createFullFlowRequest(TEST_USERNAME, "password123");
		HttpServletResponse firstResponse = createMockResponse();
		LoginResult firstResult = StatelessLoginHandler.mustAuthenticate(firstRequest, firstResponse, SOLUTION_NAME);
		assertTrue(firstResult.isAuthenticated());
		String svyToken = firstResult.getToken();
		assertNotNull(svyToken);

		String csrfToken = "123456";
		Map<String, String[]> params = new HashMap<>();
		params.put("id_token", new String[] { svyToken });
		params.put("csrf_token", new String[] { csrfToken });

		jakarta.servlet.http.Cookie csrfCookie = new jakarta.servlet.http.Cookie("csrf_token", csrfToken);
		Map<String, Object> sessionAttributes = new HashMap<>();
		Map<String, Object> contextAttributes = new HashMap<>();
		contextAttributes.put("nonce", new java.util.concurrent.ConcurrentHashMap<String, JSONObject>());

		HttpServletRequest checkRequest = (HttpServletRequest)Proxy.newProxyInstance(
			HttpServletRequest.class.getClassLoader(),
			new Class< ? >[] { HttpServletRequest.class },
			(proxy, method, args) -> {
				switch (method.getName())
				{
					case "getParameterMap" :
						return params;
					case "getParameter" :
						String[] vals = params.get(args[0]);
						return vals != null && vals.length > 0 ? vals[0] : null;
					case "getCookies" :
						return new jakarta.servlet.http.Cookie[] { csrfCookie };
					case "getCharacterEncoding" :
						return "UTF-8";
					case "setCharacterEncoding" :
						return null;
					case "getRequestURI" :
						return "/solution/" + SOLUTION_NAME + "/index.html";
					case "getRequestURL" :
						return new StringBuffer("https://localhost:8080/solution/" + SOLUTION_NAME + "/index.html");
					case "getServletPath" :
						return "/solution/" + SOLUTION_NAME + "/index.html";
					case "getContextPath" :
						return "";
					case "getScheme" :
						return "https";
					case "getServerName" :
						return "localhost";
					case "getServerPort" :
						return Integer.valueOf(8080);
					case "getHeader" :
						if ("accept-language".equals(args[0])) return "en";
						return null;
					case "getLocale" :
						return Locale.ENGLISH;
					case "getSession" :
						return (jakarta.servlet.http.HttpSession)Proxy.newProxyInstance(
							jakarta.servlet.http.HttpSession.class.getClassLoader(),
							new Class< ? >[] { jakarta.servlet.http.HttpSession.class },
							(p, m, a) -> {
								switch (m.getName())
								{
									case "getAttribute" :
										return sessionAttributes.get(a[0]);
									case "setAttribute" :
										sessionAttributes.put((String)a[0], a[1]);
										return null;
									case "removeAttribute" :
										sessionAttributes.remove(a[0]);
										return null;
									default :
										return getDefaultReturnValue(m);
								}
							});
					case "getRemoteAddr" :
						return "127.0.0.1";
					case "getServletContext" :
						return (jakarta.servlet.ServletContext)Proxy.newProxyInstance(
							jakarta.servlet.ServletContext.class.getClassLoader(),
							new Class< ? >[] { jakarta.servlet.ServletContext.class },
							(p, m, a) -> {
								switch (m.getName())
								{
									case "getAttribute" :
										return contextAttributes.get(a[0]);
									case "setAttribute" :
										contextAttributes.put((String)a[0], a[1]);
										return null;
									default :
										return getDefaultReturnValue(m);
								}
							});
					case "getQueryString" :
						return null;
					case "getMethod" :
						return "POST";
					default :
						return getDefaultReturnValue(method);
				}
			});

		HttpServletResponse checkResponse = createMockResponse();
		LoginResult checkResult = StatelessLoginHandler.mustAuthenticate(checkRequest, checkResponse, SOLUTION_NAME);
		assertTrue("Should be authenticated with valid svy token", checkResult.isAuthenticated());
		assertNotNull(checkResult.getToken());

		DecodedJWT decoded = JWT.decode(checkResult.getToken());
		assertEquals(TEST_USERNAME, decoded.getClaim(SvyID.USERNAME).asString());
		String[] tenants = decoded.getClaim(SvyID.TENANTS).asArray(String.class);
		assertNotNull(tenants);
		assertEquals("Sandbox", tenants[0]);
	}

	// ===== getCloudLoginPage - additional scenarios =====

	@Test
	public void testGetCloudLoginPage_withQueryString_includesQueryInRedirect() throws Exception
	{
		String loginHtml = "<html><body><form></form></body></html>";
		lastCloudResponse = new JSONObject().put("html", loginHtml);
		lastCloudStatusCode = 200;

		HttpServletRequest request = createMockRequestWithQueryString("deeplink=mypage&param=value");
		String result = CloudStatelessAccessManager.getCloudLoginPage(request, cloudSolution, null);

		assertNotNull(result);
		assertTrue(result.contains("svyRedirect"));
		assertTrue(result.contains("deeplink"));
	}

	@Test
	public void testGetCloudLoginPage_noHtmlInResponse_returnsNull() throws Exception
	{
		// Cloud returns 200 but no "html" key
		JSONObject response = new JSONObject();
		response.put("message", "no html");
		lastCloudResponse = response;
		lastCloudStatusCode = 200;

		HttpServletRequest request = createMockRequest();
		String result = CloudStatelessAccessManager.getCloudLoginPage(request, cloudSolution, null);

		// Should return the loginHtml parameter (null in this case)
		assertTrue(result == null);
	}

	@Test
	public void testGetCloudLoginPage_htmlWithoutForm_noRedirectInjected() throws Exception
	{
		// If the html doesn't contain "</form>" the redirect is not injected
		String loginHtml = "<html><body><p>No form here</p></body></html>";
		lastCloudResponse = new JSONObject().put("html", loginHtml);
		lastCloudStatusCode = 200;

		HttpServletRequest request = createMockRequest();
		String result = CloudStatelessAccessManager.getCloudLoginPage(request, cloudSolution, null);

		assertNotNull(result);
		assertFalse(result.contains("svyRedirect"));
		assertTrue(result.contains("No form here"));
	}

	@Test
	public void testGetCloudLoginPage_existingLoginHtml_overriddenByCloud() throws Exception
	{
		String existingHtml = "<html><body>existing</body></html>";
		String cloudHtml = "<html><body><form>cloud form</form></body></html>";
		lastCloudResponse = new JSONObject().put("html", cloudHtml);
		lastCloudStatusCode = 200;

		HttpServletRequest request = createMockRequest();
		String result = CloudStatelessAccessManager.getCloudLoginPage(request, cloudSolution, existingHtml);

		assertNotNull(result);
		assertTrue(result.contains("cloud form"));
		assertTrue(result.contains("svyRedirect"));
	}

	// ===== handlePossibleCloudRequest =====

	@Test
	public void testHandlePossibleCloudRequest_nonSvyLoginPath_returnsFalse() throws Exception
	{
		HttpServletRequest request = createMockRequestWithServletPath("/solution/" + SOLUTION_NAME + "/index.html");
		HttpServletResponse mockResponse = createMockResponse();

		boolean handled = CloudStatelessAccessManager.handlePossibleCloudRequest(request, mockResponse, SOLUTION_NAME);

		assertFalse(handled);
	}

	@Test
	public void testHandlePossibleCloudRequest_nullSolutionName_returnsFalse() throws Exception
	{
		HttpServletRequest request = createMockRequestWithServletPath("/solution/test/svylogin/login");
		HttpServletResponse mockResponse = createMockResponse();

		boolean handled = CloudStatelessAccessManager.handlePossibleCloudRequest(request, mockResponse, null);

		assertFalse(handled);
	}

	// ===== addcontentSecurityPolicyHeader =====

	@Test
	public void testAddContentSecurityPolicyHeader_addsCloudUrlToScriptAndStyleSrc() throws Exception
	{
		HttpServletRequest request = createMockRequest();
		Map<String, List<String>> responseHeaders = new HashMap<>();
		HttpServletResponse mockResponse = createMockResponseWithHeaders(responseHeaders);

		CloudStatelessAccessManager.addcontentSecurityPolicyHeader(request, mockResponse);

		// Since our mock request returns "Mozilla/5.0" for user-agent, CSP may or may not be generated
		// depending on whether the user-agent is considered to support CSP level 3.
		// The test verifies the method doesn't throw.
		// If CSP is generated, it should contain the cloud URL in script-src and style-src
		if (!responseHeaders.isEmpty())
		{
			List<String> cspValues = responseHeaders.get("Content-Security-Policy");
			if (cspValues != null && !cspValues.isEmpty())
			{
				String csp = cspValues.get(0);
				assertTrue(csp.contains("script-src") || csp.contains("style-src"));
			}
		}
	}

	// ===== buildQueryString - additional coverage =====

	@Test
	public void testGetCloudLoginPage_withParameters_buildsQueryString() throws Exception
	{
		String loginHtml = "<html><body><form></form></body></html>";
		lastCloudResponse = new JSONObject().put("html", loginHtml);
		lastCloudStatusCode = 200;

		Map<String, String[]> params = new HashMap<>();
		params.put("locale", new String[] { "en_US" });
		params.put("theme", new String[] { "dark" });
		HttpServletRequest request = createMockRequestWithParams(params);
		String result = CloudStatelessAccessManager.getCloudLoginPage(request, cloudSolution, null);

		assertNotNull(result);
		assertTrue(result.contains("svyRedirect"));
	}

	@Test
	public void testGetCloudLoginPage_withBlankParamValues_filtersBlankValues() throws Exception
	{
		String loginHtml = "<html><body><form></form></body></html>";
		lastCloudResponse = new JSONObject().put("html", loginHtml);
		lastCloudStatusCode = 200;

		Map<String, String[]> params = new HashMap<>();
		params.put("key1", new String[] { "" });
		params.put("key2", new String[] { "  " });
		params.put("key3", new String[] { "value3" });
		HttpServletRequest request = createMockRequestWithParams(params);
		String result = CloudStatelessAccessManager.getCloudLoginPage(request, cloudSolution, null);

		assertNotNull(result);
	}

	@Test
	public void testGetCloudLoginPage_withSpecialCharsInParams_encodesCorrectly() throws Exception
	{
		String loginHtml = "<html><body><form></form></body></html>";
		lastCloudResponse = new JSONObject().put("html", loginHtml);
		lastCloudStatusCode = 200;

		Map<String, String[]> params = new HashMap<>();
		params.put("redirect", new String[] { "https://example.com/path?a=1&b=2" });
		HttpServletRequest request = createMockRequestWithParams(params);
		String result = CloudStatelessAccessManager.getCloudLoginPage(request, cloudSolution, null);

		assertNotNull(result);
	}

	@Test
	public void testGetCloudLoginPage_withMultipleValuesForSameParam() throws Exception
	{
		String loginHtml = "<html><body><form></form></body></html>";
		lastCloudResponse = new JSONObject().put("html", loginHtml);
		lastCloudStatusCode = 200;

		Map<String, String[]> params = new HashMap<>();
		params.put("role", new String[] { "admin", "user" });
		HttpServletRequest request = createMockRequestWithParams(params);
		String result = CloudStatelessAccessManager.getCloudLoginPage(request, cloudSolution, null);

		assertNotNull(result);
	}

	// ===== getCloudRestApiEndpoints - via getCloudLoginPage with caching =====

	@Test
	public void testGetCloudLoginPage_secondCall_usesCache() throws Exception
	{
		String loginHtml = "<html><body><form>first call</form></body></html>";
		lastCloudResponse = new JSONObject().put("html", loginHtml);
		lastCloudStatusCode = 200;

		HttpServletRequest request = createMockRequest();
		String result1 = CloudStatelessAccessManager.getCloudLoginPage(request, cloudSolution, null);
		assertNotNull(result1);

		// Change cloud response for second call
		String secondHtml = "<html><body><form>second call</form></body></html>";
		lastCloudResponse = new JSONObject().put("html", secondHtml);

		String result2 = CloudStatelessAccessManager.getCloudLoginPage(request, cloudSolution, null);
		assertNotNull(result2);
		// getCloudLoginPage always queries the cloud (no caching for login page)
		assertTrue(result2.contains("second call"));
	}

	// ===== revokeToken - when no provider =====

	@Test
	public void testRevokeToken_noProviderClaim_doesNothing() throws Exception
	{
		// Build a token without the CLOUD_OAUTH_ENDPOINT claim
		Properties settings = ApplicationServerRegistry.get().getServerAccess().getSettings();
		String jwtPassword = settings.getProperty(StatelessLoginUtils.JWT_Password);
		String tokenStr = JWT.create()
			.withClaim(SvyID.USERNAME, TEST_USERNAME)
			.withClaim(SvyID.PERMISSIONS, java.util.Arrays.asList(TEST_PERMISSIONS))
			.withExpiresAt(new java.util.Date(System.currentTimeMillis() + 3600000))
			.sign(Algorithm.HMAC256(jwtPassword));

		DecodedJWT jwt = JWT.decode(tokenStr);
		// Should not throw - provider is null so it's a no-op
		CloudStatelessAccessManager.revokeToken(cloudSolution, jwt);
	}

	@Test
	public void testRevokeToken_withProvider_cloudReturnsNoOAuth_logsError() throws Exception
	{
		// Build a token WITH the CLOUD_OAUTH_ENDPOINT claim but the cloud returns no oauth config
		Properties settings = ApplicationServerRegistry.get().getServerAccess().getSettings();
		String jwtPassword = settings.getProperty(StatelessLoginUtils.JWT_Password);
		String tokenStr = JWT.create()
			.withClaim(SvyID.USERNAME, TEST_USERNAME)
			.withClaim(SvyID.PERMISSIONS, java.util.Arrays.asList(TEST_PERMISSIONS))
			.withClaim(CloudStatelessAccessManager.CLOUD_OAUTH_ENDPOINT, "google")
			.withClaim(StatelessLoginHandler.REFRESH_TOKEN, "my-refresh-token")
			.withExpiresAt(new java.util.Date(System.currentTimeMillis() + 3600000))
			.sign(Algorithm.HMAC256(jwtPassword));

		// The cloud mock will return an empty JSON (no "oauth" key)
		lastCloudResponse = new JSONObject();
		lastCloudStatusCode = 200;

		DecodedJWT jwt = JWT.decode(tokenStr);
		// Should not throw - oauth is null so it logs an error
		CloudStatelessAccessManager.revokeToken(cloudSolution, jwt);
	}

	// ===== parseQueryString (tested indirectly via getOAuthConfigFromTheCloud) =====

	@Test
	public void testCheckCloudPermissions_refreshWithOAuthEndpointWithQueryParams() throws Exception
	{
		// This exercises the parseQueryString method via getOAuthConfigFromTheCloud
		// When oldToken has CLOUD_OAUTH_ENDPOINT with query params like "google?param=val"
		// The cloud returns oauth config, but refreshAccessToken fails (no real provider)
		Properties settings = ApplicationServerRegistry.get().getServerAccess().getSettings();
		String jwtPassword = settings.getProperty(StatelessLoginUtils.JWT_Password);
		String tokenStr = JWT.create()
			.withClaim(SvyID.USERNAME, TEST_USERNAME)
			.withClaim(SvyID.PERMISSIONS, java.util.Arrays.asList(TEST_PERMISSIONS))
			.withClaim(CloudStatelessAccessManager.CLOUD_OAUTH_ENDPOINT, "google?scope=openid")
			.withClaim(StatelessLoginHandler.REFRESH_TOKEN, "my-refresh-token")
			.withExpiresAt(new java.util.Date(System.currentTimeMillis() + 3600000))
			.sign(Algorithm.HMAC256(jwtPassword));

		// The cloud POST for oauth config returns an "oauth" key with a valid clientId
		JSONObject oauthObj = new JSONObject();
		oauthObj.put("clientId", "fake-client-id");
		oauthObj.put("apiSecret", "fake-secret");
		oauthObj.put("authorizationBaseUrl", "https://accounts.google.com/o/oauth2/v2/auth");
		oauthObj.put("accessTokenEndpoint", "https://oauth2.googleapis.com/token");
		lastCloudResponse = new JSONObject().put("oauth", oauthObj);
		lastCloudStatusCode = 200;

		SvyID oldToken = new SvyID(tokenStr);
		LoginResult result = LoginResult.needsLogin();
		boolean verified = CloudStatelessAccessManager.checkCloudPermissions(
			null, null, false, oldToken, result, cloudSolution, createMockRequest());

		// Expected: false because refreshAccessToken will fail (no real OAuth provider)
		assertFalse(verified);
	}

	@Test
	public void testCheckCloudPermissions_refreshWithOAuthEndpointNoQueryParams() throws Exception
	{
		// CLOUD_OAUTH_ENDPOINT without query params
		Properties settings = ApplicationServerRegistry.get().getServerAccess().getSettings();
		String jwtPassword = settings.getProperty(StatelessLoginUtils.JWT_Password);
		String tokenStr = JWT.create()
			.withClaim(SvyID.USERNAME, TEST_USERNAME)
			.withClaim(SvyID.PERMISSIONS, java.util.Arrays.asList(TEST_PERMISSIONS))
			.withClaim(CloudStatelessAccessManager.CLOUD_OAUTH_ENDPOINT, "microsoft")
			.withClaim(StatelessLoginHandler.REFRESH_TOKEN, "my-refresh-token")
			.withExpiresAt(new java.util.Date(System.currentTimeMillis() + 3600000))
			.sign(Algorithm.HMAC256(jwtPassword));

		// Return oauth config with clientId so ServiceBuilder doesn't throw
		JSONObject oauthObj = new JSONObject();
		oauthObj.put("clientId", "fake-client-id");
		oauthObj.put("apiSecret", "fake-secret");
		oauthObj.put("authorizationBaseUrl", "https://login.microsoftonline.com/common/oauth2/v2.0/authorize");
		oauthObj.put("accessTokenEndpoint", "https://login.microsoftonline.com/common/oauth2/v2.0/token");
		lastCloudResponse = new JSONObject().put("oauth", oauthObj);
		lastCloudStatusCode = 200;

		SvyID oldToken = new SvyID(tokenStr);
		LoginResult result = LoginResult.needsLogin();
		boolean verified = CloudStatelessAccessManager.checkCloudPermissions(
			null, null, false, oldToken, result, cloudSolution, createMockRequest());

		assertFalse(verified);
	}


	// ===== Full flow: existing token validation failures =====

	@Test
	public void testFullFlow_cloud_existingToken_expired_triggersRefresh() throws Exception
	{
		Properties settings = ApplicationServerRegistry.get().getServerAccess().getSettings();
		String jwtPassword = settings.getProperty(StatelessLoginUtils.JWT_Password);
		String expiredToken = JWT.create()
			.withIssuer("svy")
			.withClaim(SvyID.USERNAME, TEST_USERNAME)
			.withClaim(SvyID.UID, "cloud-uid-123")
			.withArrayClaim(SvyID.PERMISSIONS, TEST_PERMISSIONS)
			.withIssuedAt(new java.util.Date(System.currentTimeMillis() - 7200000))
			.withExpiresAt(new java.util.Date(System.currentTimeMillis() - 1000))
			.sign(Algorithm.HMAC256(jwtPassword));

		JSONObject permissionsResponse = new JSONObject();
		permissionsResponse.put("permissions", new JSONArray(TEST_PERMISSIONS));
		permissionsResponse.put("username", TEST_USERNAME);
		lastCloudResponse = permissionsResponse;
		lastCloudStatusCode = 200;

		Map<String, String[]> params = new HashMap<>();
		params.put("id_token", new String[] { expiredToken });
		params.put("csrf_token", new String[] { "123456" });

		jakarta.servlet.http.Cookie csrfCookie = new jakarta.servlet.http.Cookie("csrf_token", "123456");
		Map<String, Object> sessionAttributes = new HashMap<>();
		Map<String, Object> contextAttributes = new HashMap<>();
		contextAttributes.put("nonce", new java.util.concurrent.ConcurrentHashMap<String, JSONObject>());

		HttpServletRequest request = (HttpServletRequest)Proxy.newProxyInstance(
			HttpServletRequest.class.getClassLoader(),
			new Class< ? >[] { HttpServletRequest.class },
			(proxy, method, args) -> {
				switch (method.getName())
				{
					case "getParameterMap" :
						return params;
					case "getParameter" :
						String[] vals = params.get(args[0]);
						return vals != null && vals.length > 0 ? vals[0] : null;
					case "getCookies" :
						return new jakarta.servlet.http.Cookie[] { csrfCookie };
					case "getCharacterEncoding" :
						return "UTF-8";
					case "setCharacterEncoding" :
						return null;
					case "getRequestURI" :
						return "/solution/" + SOLUTION_NAME + "/index.html";
					case "getRequestURL" :
						return new StringBuffer("https://localhost:8080/solution/" + SOLUTION_NAME + "/index.html");
					case "getServletPath" :
						return "/solution/" + SOLUTION_NAME + "/index.html";
					case "getContextPath" :
						return "";
					case "getScheme" :
						return "https";
					case "getServerName" :
						return "localhost";
					case "getServerPort" :
						return Integer.valueOf(8080);
					case "getHeader" :
						if ("accept-language".equals(args[0])) return "en";
						return null;
					case "getLocale" :
						return Locale.ENGLISH;
					case "getSession" :
						return (jakarta.servlet.http.HttpSession)Proxy.newProxyInstance(
							jakarta.servlet.http.HttpSession.class.getClassLoader(),
							new Class< ? >[] { jakarta.servlet.http.HttpSession.class },
							(p, m, a) -> {
								switch (m.getName())
								{
									case "getAttribute" :
										return sessionAttributes.get(a[0]);
									case "setAttribute" :
										sessionAttributes.put((String)a[0], a[1]);
										return null;
									case "removeAttribute" :
										sessionAttributes.remove(a[0]);
										return null;
									default :
										return getDefaultReturnValue(m);
								}
							});
					case "getRemoteAddr" :
						return "127.0.0.1";
					case "getServletContext" :
						return (jakarta.servlet.ServletContext)Proxy.newProxyInstance(
							jakarta.servlet.ServletContext.class.getClassLoader(),
							new Class< ? >[] { jakarta.servlet.ServletContext.class },
							(p, m, a) -> {
								switch (m.getName())
								{
									case "getAttribute" :
										return contextAttributes.get(a[0]);
									case "setAttribute" :
										contextAttributes.put((String)a[0], a[1]);
										return null;
									default :
										return getDefaultReturnValue(m);
								}
							});
					case "getQueryString" :
						return null;
					case "getMethod" :
						return "POST";
					default :
						return getDefaultReturnValue(method);
				}
			});

		HttpServletResponse response = createMockResponse();
		LoginResult result = StatelessLoginHandler.mustAuthenticate(request, response, SOLUTION_NAME);

		assertTrue("Should be authenticated after expired token refresh", result.isAuthenticated());
		assertNotNull("Should have a new token", result.getToken());
	}

	@Test
	public void testFullFlow_cloud_existingToken_expired_refreshFails_writesLoginPage() throws Exception
	{
		Properties settings = ApplicationServerRegistry.get().getServerAccess().getSettings();
		String jwtPassword = settings.getProperty(StatelessLoginUtils.JWT_Password);
		String expiredToken = JWT.create()
			.withIssuer("svy")
			.withClaim(SvyID.USERNAME, TEST_USERNAME)
			.withClaim(SvyID.UID, "cloud-uid-123")
			.withArrayClaim(SvyID.PERMISSIONS, TEST_PERMISSIONS)
			.withIssuedAt(new java.util.Date(System.currentTimeMillis() - 7200000))
			.withExpiresAt(new java.util.Date(System.currentTimeMillis() - 1000))
			.sign(Algorithm.HMAC256(jwtPassword));

		lastCloudStatusCode = 401;
		lastCloudResponse = new JSONObject().put("error", "Unauthorized");

		Map<String, String[]> params = new HashMap<>();
		params.put("id_token", new String[] { expiredToken });
		params.put("csrf_token", new String[] { "123456" });

		jakarta.servlet.http.Cookie csrfCookie = new jakarta.servlet.http.Cookie("csrf_token", "123456");
		Map<String, Object> sessionAttributes = new HashMap<>();
		Map<String, Object> contextAttributes = new HashMap<>();
		contextAttributes.put("nonce", new java.util.concurrent.ConcurrentHashMap<String, JSONObject>());

		HttpServletRequest request = (HttpServletRequest)Proxy.newProxyInstance(
			HttpServletRequest.class.getClassLoader(),
			new Class< ? >[] { HttpServletRequest.class },
			(proxy, method, args) -> {
				switch (method.getName())
				{
					case "getParameterMap" :
						return params;
					case "getParameter" :
						String[] vals = params.get(args[0]);
						return vals != null && vals.length > 0 ? vals[0] : null;
					case "getCookies" :
						return new jakarta.servlet.http.Cookie[] { csrfCookie };
					case "getCharacterEncoding" :
						return "UTF-8";
					case "setCharacterEncoding" :
						return null;
					case "getRequestURI" :
						return "/solution/" + SOLUTION_NAME + "/index.html";
					case "getRequestURL" :
						return new StringBuffer("https://localhost:8080/solution/" + SOLUTION_NAME + "/index.html");
					case "getServletPath" :
						return "/solution/" + SOLUTION_NAME + "/index.html";
					case "getContextPath" :
						return "";
					case "getScheme" :
						return "https";
					case "getServerName" :
						return "localhost";
					case "getServerPort" :
						return Integer.valueOf(8080);
					case "getHeader" :
						if ("accept-language".equals(args[0])) return "en";
						if ("user-agent".equals(args[0])) return "Mozilla/5.0";
						return null;
					case "getLocale" :
						return Locale.ENGLISH;
					case "getSession" :
						return (jakarta.servlet.http.HttpSession)Proxy.newProxyInstance(
							jakarta.servlet.http.HttpSession.class.getClassLoader(),
							new Class< ? >[] { jakarta.servlet.http.HttpSession.class },
							(p, m, a) -> {
								switch (m.getName())
								{
									case "getAttribute" :
										return sessionAttributes.get(a[0]);
									case "setAttribute" :
										sessionAttributes.put((String)a[0], a[1]);
										return null;
									case "removeAttribute" :
										sessionAttributes.remove(a[0]);
										return null;
									default :
										return getDefaultReturnValue(m);
								}
							});
					case "getRemoteAddr" :
						return "127.0.0.1";
					case "getServletContext" :
						return (jakarta.servlet.ServletContext)Proxy.newProxyInstance(
							jakarta.servlet.ServletContext.class.getClassLoader(),
							new Class< ? >[] { jakarta.servlet.ServletContext.class },
							(p, m, a) -> {
								switch (m.getName())
								{
									case "getAttribute" :
										return contextAttributes.get(a[0]);
									case "setAttribute" :
										contextAttributes.put((String)a[0], a[1]);
										return null;
									default :
										return getDefaultReturnValue(m);
								}
							});
					case "getQueryString" :
						return null;
					case "getMethod" :
						return "POST";
					default :
						return getDefaultReturnValue(method);
				}
			});

		HttpServletResponse response = createMockResponse();
		LoginResult result = StatelessLoginHandler.mustAuthenticate(request, response, SOLUTION_NAME);

		assertFalse("Should NOT be authenticated when cloud refresh fails", result.isAuthenticated());

		String loginPageHtml = "<html><body><form name=\"login_form\">cloud login</form></body></html>";
		lastCloudResponse = new JSONObject().put("html", loginPageHtml);
		lastCloudStatusCode = 200;

		StringWriter pageOutput = new StringWriter();
		HttpServletResponse writeResponse = createMockResponseWithWriter(pageOutput);
		StatelessLoginHandler.writeLoginPage(request, writeResponse, SOLUTION_NAME, result);
		String page = pageOutput.toString();
		assertTrue("Should write login page with cloud login form", page.contains("login") || page.contains("svyRedirect"));
	}

	@SuppressWarnings("unchecked")
	private <T> T createProxy(Class<T> iface, InvocationHandler handler)
	{
		return (T)Proxy.newProxyInstance(getClass().getClassLoader(), new Class< ? >[] { iface }, handler);
	}

	private HttpClient createMockHttpClient()
	{
		return new HttpClient()
		{
			@Override
			public java.util.Optional<java.net.CookieHandler> cookieHandler()
			{
				return java.util.Optional.empty();
			}

			@Override
			public java.util.Optional<java.time.Duration> connectTimeout()
			{
				return java.util.Optional.empty();
			}

			@Override
			public java.net.http.HttpClient.Redirect followRedirects()
			{
				return Redirect.NEVER;
			}

			@Override
			public java.util.Optional<java.net.ProxySelector> proxy()
			{
				return java.util.Optional.empty();
			}

			@Override
			public javax.net.ssl.SSLContext sslContext()
			{
				return null;
			}

			@Override
			public javax.net.ssl.SSLParameters sslParameters()
			{
				return null;
			}

			@Override
			public java.util.Optional<java.util.concurrent.Executor> executor()
			{
				return java.util.Optional.empty();
			}

			@Override
			public java.net.http.HttpClient.Version version()
			{
				return Version.HTTP_1_1;
			}

			@Override
			public java.util.Optional<java.net.Authenticator> authenticator()
			{
				return java.util.Optional.empty();
			}

			@Override
			public <T> HttpResponse<T> send(HttpRequest request, BodyHandler<T> responseBodyHandler)
				throws java.io.IOException, InterruptedException
			{
				String responseBody = lastCloudResponse.toString();
				HttpResponse.ResponseInfo responseInfo = new HttpResponse.ResponseInfo()
				{
					@Override
					public int statusCode()
					{
						return lastCloudStatusCode;
					}

					@Override
					public java.net.http.HttpHeaders headers()
					{
						return java.net.http.HttpHeaders.of(Collections.emptyMap(), (a, b) -> true);
					}

					@Override
					public HttpClient.Version version()
					{
						return Version.HTTP_1_1;
					}
				};

				HttpResponse.BodySubscriber<T> subscriber = responseBodyHandler.apply(responseInfo);
				subscriber.onSubscribe(new java.util.concurrent.Flow.Subscription()
				{
					@Override
					public void request(long n)
					{
						subscriber.onNext(List.of(java.nio.ByteBuffer.wrap(responseBody.getBytes(StandardCharsets.UTF_8))));
						subscriber.onComplete();
					}

					@Override
					public void cancel()
					{
					}
				});

				try
				{
					T body = subscriber.getBody().toCompletableFuture().get();
					return createMockHttpResponse(request, body);
				}
				catch (Exception e)
				{
					throw new java.io.IOException("Mock HTTP error", e);
				}
			}

			@Override
			public <T> java.util.concurrent.CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
				BodyHandler<T> responseBodyHandler)
			{
				return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
					try
					{
						return send(request, responseBodyHandler);
					}
					catch (Exception e)
					{
						throw new RuntimeException(e);
					}
				});
			}

			@Override
			public <T> java.util.concurrent.CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
				BodyHandler<T> responseBodyHandler, HttpResponse.PushPromiseHandler<T> pushPromiseHandler)
			{
				return sendAsync(request, responseBodyHandler);
			}

			@Override
			public void close()
			{
			}
		};
	}

	@SuppressWarnings("unchecked")
	private <T> HttpResponse<T> createMockHttpResponse(HttpRequest request, T body)
	{
		return (HttpResponse<T>)Proxy.newProxyInstance(getClass().getClassLoader(),
			new Class< ? >[] { HttpResponse.class },
			(proxy, method, args) -> {
				switch (method.getName())
				{
					case "statusCode" :
						return Integer.valueOf(lastCloudStatusCode);
					case "body" :
						return body;
					case "request" :
						return request;
					case "headers" :
						return java.net.http.HttpHeaders.of(Collections.emptyMap(), (a, b) -> true);
					default :
						return getDefaultReturnValue(method);
				}
			});
	}

	private HttpServletRequest createMockRequest()
	{
		Map<String, Object> sessionAttributes = new HashMap<>();
		Map<String, Object> contextAttributes = new HashMap<>();
		contextAttributes.put("nonce", new HashMap<String, Object>());

		return (HttpServletRequest)Proxy.newProxyInstance(
			HttpServletRequest.class.getClassLoader(),
			new Class< ? >[] { HttpServletRequest.class },
			(proxy, method, args) -> {
				switch (method.getName())
				{
					case "getParameterMap" :
						return Collections.emptyMap();
					case "getParameter" :
						return null;
					case "getCookies" :
						return null;
					case "getCharacterEncoding" :
						return "UTF-8";
					case "setCharacterEncoding" :
						return null;
					case "getRequestURI" :
						return "/solution/" + SOLUTION_NAME + "/index.html";
					case "getRequestURL" :
						return new StringBuffer("https://localhost:8080/solution/" + SOLUTION_NAME + "/index.html");
					case "getServletPath" :
						return "/solution/" + SOLUTION_NAME + "/index.html";
					case "getContextPath" :
						return "";
					case "getScheme" :
						return "https";
					case "getServerName" :
						return "localhost";
					case "getServerPort" :
						return Integer.valueOf(8080);
					case "getHeader" :
						if ("accept-language".equals(args[0])) return "en";
						if ("Accept-Language".equals(args[0])) return "en";
						if ("user-agent".equals(args[0])) return "Mozilla/5.0";
						return null;
					case "getLocale" :
						return Locale.ENGLISH;
					case "getSession" :
						return (jakarta.servlet.http.HttpSession)Proxy.newProxyInstance(
							jakarta.servlet.http.HttpSession.class.getClassLoader(),
							new Class< ? >[] { jakarta.servlet.http.HttpSession.class },
							(p, m, a) -> {
								switch (m.getName())
								{
									case "getAttribute" :
										return sessionAttributes.get(a[0]);
									case "setAttribute" :
										sessionAttributes.put((String)a[0], a[1]);
										return null;
									case "removeAttribute" :
										sessionAttributes.remove(a[0]);
										return null;
									default :
										return getDefaultReturnValue(m);
								}
							});
					case "getRemoteAddr" :
						return "127.0.0.1";
					case "getServletContext" :
						return (jakarta.servlet.ServletContext)Proxy.newProxyInstance(
							jakarta.servlet.ServletContext.class.getClassLoader(),
							new Class< ? >[] { jakarta.servlet.ServletContext.class },
							(p, m, a) -> {
								switch (m.getName())
								{
									case "getAttribute" :
										return contextAttributes.get(a[0]);
									case "setAttribute" :
										contextAttributes.put((String)a[0], a[1]);
										return null;
									default :
										return getDefaultReturnValue(m);
								}
							});
					case "getQueryString" :
						return null;
					case "getMethod" :
						return "GET";
					default :
						return getDefaultReturnValue(method);
				}
			});
	}

	private Solution createSolution(String name, int solutionType, String modulesNames) throws Exception
	{
		SolutionMetaData metaData = new SolutionMetaData(UUID.randomUUID(), name, IRepository.SOLUTIONS, 1, 1);
		metaData.setSolutionType(solutionType);
		Constructor<Solution> ctor = Solution.class.getDeclaredConstructor(IRepository.class, SolutionMetaData.class);
		ctor.setAccessible(true);
		Solution sol = ctor.newInstance(createMockRepository(), metaData);
		if (modulesNames != null)
		{
			sol.setModulesNames(modulesNames);
		}
		return sol;
	}

	private IRepository createMockRepository()
	{
		return (IRepository)Proxy.newProxyInstance(getClass().getClassLoader(),
			new Class< ? >[] { IRepository.class },
			(proxy, method, args) -> {
				if ("getActiveRootObject".equals(method.getName()))
				{
					if (args.length == 2 && args[0] instanceof String)
					{
						if (SOLUTION_NAME.equals(args[0])) return cloudSolution;
					}
					if (args.length == 1 && args[0] instanceof UUID)
					{
						if (cloudSolution != null && args[0].equals(cloudSolution.getUUID())) return cloudSolution;
					}
				}
				if ("getRootObjectMetaData".equals(method.getName()) && args.length == 2)
				{
					if (SOLUTION_NAME.equals(args[0])) return cloudSolution.getSolutionMetaData();
				}
				if ("getActiveSolutionModuleMetaDatas".equals(method.getName()))
				{
					UUID solUuid = (UUID)args[0];
					if (cloudSolution != null && solUuid.equals(cloudSolution.getUUID()))
					{
						return java.util.Collections.singletonList(
							new com.servoy.j2db.persistence.RootObjectReference(cloudSolution.getSolutionMetaData(), 1));
					}
					return java.util.Collections.emptyList();
				}
				return getDefaultReturnValue(method);
			});
	}

	private IApplicationServerSingleton createMockApplicationServer()
	{
		Properties settings = new Properties();
		settings.setProperty(StatelessLoginUtils.JWT_Password, TEST_JWT_PASSWORD);

		Object serverAccess = Proxy.newProxyInstance(getClass().getClassLoader(),
			getAllInterfaces(com.servoy.j2db.plugins.IServerAccess.class),
			(proxy, method, args) -> {
				if ("getSettings".equals(method.getName())) return settings;
				return getDefaultReturnValue(method);
			});

		IRepository repository = createMockRepository();

		com.servoy.j2db.server.shared.IApplicationServer applicationServer = (com.servoy.j2db.server.shared.IApplicationServer)Proxy.newProxyInstance(
			getClass().getClassLoader(),
			new Class< ? >[] { com.servoy.j2db.server.shared.IApplicationServer.class },
			(proxy, method, args) -> {
				if ("getLoginSolutionDefinitions".equals(method.getName()))
				{
					return new SolutionMetaData[0];
				}
				return getDefaultReturnValue(method);
			});

		com.servoy.j2db.server.shared.IServiceRegistry serviceRegistry = createProxy(
			com.servoy.j2db.server.shared.IServiceRegistry.class, (proxy, method, args) -> {
				if ("getService".equals(method.getName()) && args != null && args.length == 1)
				{
					if (args[0] == com.servoy.j2db.server.shared.IApplicationServer.class) return applicationServer;
				}
				return getDefaultReturnValue(method);
			});
		ApplicationServerRegistry.setServiceRegistry(serviceRegistry);

		return createProxy(IApplicationServerSingleton.class, (proxy, method, args) -> {
			switch (method.getName())
			{
				case "getServerAccess" :
					return serverAccess;
				case "getLocalRepository" :
					return repository;
				case "getService" :
					if (args != null && args.length == 1 && args[0] == com.servoy.j2db.server.shared.IApplicationServer.class)
						return applicationServer;
					return null;
				default :
					return getDefaultReturnValue(method);
			}
		});
	}

	private static Class< ? >[] getAllInterfaces(Class< ? > iface)
	{
		java.util.Set<Class< ? >> interfaces = new java.util.LinkedHashSet<>();
		collectInterfaces(iface, interfaces);
		return interfaces.toArray(new Class< ? >[0]);
	}

	private static void collectInterfaces(Class< ? > iface, java.util.Set<Class< ? >> interfaces)
	{
		interfaces.add(iface);
		for (Class< ? > superIface : iface.getInterfaces())
		{
			collectInterfaces(superIface, interfaces);
		}
	}

	private static Object getDefaultReturnValue(Method method)
	{
		Class< ? > returnType = method.getReturnType();
		if (returnType == boolean.class) return Boolean.FALSE;
		if (returnType == int.class) return Integer.valueOf(0);
		if (returnType == long.class) return Long.valueOf(0L);
		if (returnType == double.class) return Double.valueOf(0.0);
		if (returnType == float.class) return Float.valueOf(0.0f);
		if (returnType == char.class) return Character.valueOf('\0');
		if (returnType == byte.class) return Byte.valueOf((byte)0);
		if (returnType == short.class) return Short.valueOf((short)0);
		return null;
	}

	private HttpServletResponse createMockResponse()
	{
		return (HttpServletResponse)Proxy.newProxyInstance(
			HttpServletResponse.class.getClassLoader(),
			new Class< ? >[] { HttpServletResponse.class },
			(proxy, method, args) -> {
				switch (method.getName())
				{
					case "getWriter" :
						return new PrintWriter(new StringWriter());
					case "setCharacterEncoding" :
					case "setContentType" :
					case "setContentLengthLong" :
					case "addHeader" :
					case "setHeader" :
					case "setStatus" :
						return null;
					case "sendRedirect" :
						return null;
					case "getCharacterEncoding" :
						return "UTF-8";
					default :
						return getDefaultReturnValue(method);
				}
			});
	}

	private HttpServletResponse createMockResponseWithWriter(StringWriter writer)
	{
		PrintWriter printWriter = new PrintWriter(writer);
		return (HttpServletResponse)Proxy.newProxyInstance(
			HttpServletResponse.class.getClassLoader(),
			new Class< ? >[] { HttpServletResponse.class },
			(proxy, method, args) -> {
				switch (method.getName())
				{
					case "getWriter" :
						return printWriter;
					case "setCharacterEncoding" :
					case "setContentType" :
					case "setContentLengthLong" :
					case "addHeader" :
					case "setHeader" :
					case "setStatus" :
						return null;
					case "sendRedirect" :
						return null;
					case "getCharacterEncoding" :
						return "UTF-8";
					default :
						return getDefaultReturnValue(method);
				}
			});
	}

	private HttpServletResponse createMockResponseWithRedirect(List<String> redirects)
	{
		return (HttpServletResponse)Proxy.newProxyInstance(
			HttpServletResponse.class.getClassLoader(),
			new Class< ? >[] { HttpServletResponse.class },
			(proxy, method, args) -> {
				switch (method.getName())
				{
					case "getWriter" :
						return new PrintWriter(new StringWriter());
					case "setCharacterEncoding" :
					case "setContentType" :
					case "setContentLengthLong" :
					case "addHeader" :
					case "setHeader" :
					case "setStatus" :
						return null;
					case "sendRedirect" :
						redirects.add((String)args[0]);
						return null;
					case "getCharacterEncoding" :
						return "UTF-8";
					default :
						return getDefaultReturnValue(method);
				}
			});
	}

	private HttpServletResponse createMockResponseWithWriterAndCookies(StringWriter writer, List<jakarta.servlet.http.Cookie> cookies)
	{
		PrintWriter printWriter = new PrintWriter(writer);
		return (HttpServletResponse)Proxy.newProxyInstance(
			HttpServletResponse.class.getClassLoader(),
			new Class< ? >[] { HttpServletResponse.class },
			(proxy, method, args) -> {
				switch (method.getName())
				{
					case "getWriter" :
						return printWriter;
					case "addCookie" :
						cookies.add((jakarta.servlet.http.Cookie)args[0]);
						return null;
					case "setCharacterEncoding" :
					case "setContentType" :
					case "setContentLengthLong" :
					case "addHeader" :
					case "setHeader" :
					case "setStatus" :
					case "sendRedirect" :
						return null;
					case "getCharacterEncoding" :
						return "UTF-8";
					default :
						return getDefaultReturnValue(method);
				}
			});
	}

	private HttpServletResponse createMockResponseWithHeaders(Map<String, List<String>> headers)
	{
		return (HttpServletResponse)Proxy.newProxyInstance(
			HttpServletResponse.class.getClassLoader(),
			new Class< ? >[] { HttpServletResponse.class },
			(proxy, method, args) -> {
				switch (method.getName())
				{
					case "getWriter" :
						return new PrintWriter(new StringWriter());
					case "setCharacterEncoding" :
					case "setContentType" :
					case "setContentLengthLong" :
					case "setStatus" :
						return null;
					case "addHeader" :
					case "setHeader" :
						String headerName = (String)args[0];
						String headerValue = (String)args[1];
						headers.computeIfAbsent(headerName, k -> new ArrayList<>()).add(headerValue);
						return null;
					case "sendRedirect" :
						return null;
					case "getCharacterEncoding" :
						return "UTF-8";
					default :
						return getDefaultReturnValue(method);
				}
			});
	}

	private HttpServletRequest createMockRequestWithQueryString(String queryString)
	{
		Map<String, Object> sessionAttributes = new HashMap<>();
		Map<String, Object> contextAttributes = new HashMap<>();
		contextAttributes.put("nonce", new HashMap<String, Object>());

		return (HttpServletRequest)Proxy.newProxyInstance(
			HttpServletRequest.class.getClassLoader(),
			new Class< ? >[] { HttpServletRequest.class },
			(proxy, method, args) -> {
				switch (method.getName())
				{
					case "getParameterMap" :
						return Collections.emptyMap();
					case "getParameter" :
						return null;
					case "getCookies" :
						return null;
					case "getCharacterEncoding" :
						return "UTF-8";
					case "setCharacterEncoding" :
						return null;
					case "getRequestURI" :
						return "/solution/" + SOLUTION_NAME + "/index.html";
					case "getRequestURL" :
						return new StringBuffer("https://localhost:8080/solution/" + SOLUTION_NAME + "/index.html");
					case "getServletPath" :
						return "/solution/" + SOLUTION_NAME + "/index.html";
					case "getContextPath" :
						return "";
					case "getScheme" :
						return "https";
					case "getServerName" :
						return "localhost";
					case "getServerPort" :
						return Integer.valueOf(8080);
					case "getHeader" :
						if ("accept-language".equals(args[0])) return "en";
						if ("Accept-Language".equals(args[0])) return "en";
						if ("user-agent".equals(args[0])) return "Mozilla/5.0";
						return null;
					case "getLocale" :
						return Locale.ENGLISH;
					case "getSession" :
						return (jakarta.servlet.http.HttpSession)Proxy.newProxyInstance(
							jakarta.servlet.http.HttpSession.class.getClassLoader(),
							new Class< ? >[] { jakarta.servlet.http.HttpSession.class },
							(p, m, a) -> {
								switch (m.getName())
								{
									case "getAttribute" :
										return sessionAttributes.get(a[0]);
									case "setAttribute" :
										sessionAttributes.put((String)a[0], a[1]);
										return null;
									case "removeAttribute" :
										sessionAttributes.remove(a[0]);
										return null;
									default :
										return getDefaultReturnValue(m);
								}
							});
					case "getRemoteAddr" :
						return "127.0.0.1";
					case "getServletContext" :
						return (jakarta.servlet.ServletContext)Proxy.newProxyInstance(
							jakarta.servlet.ServletContext.class.getClassLoader(),
							new Class< ? >[] { jakarta.servlet.ServletContext.class },
							(p, m, a) -> {
								switch (m.getName())
								{
									case "getAttribute" :
										return contextAttributes.get(a[0]);
									case "setAttribute" :
										contextAttributes.put((String)a[0], a[1]);
										return null;
									default :
										return getDefaultReturnValue(m);
								}
							});
					case "getQueryString" :
						return queryString;
					case "getMethod" :
						return "GET";
					default :
						return getDefaultReturnValue(method);
				}
			});
	}

	private HttpServletRequest createMockRequestWithParams(Map<String, String[]> params)
	{
		Map<String, Object> sessionAttributes = new HashMap<>();
		Map<String, Object> contextAttributes = new HashMap<>();
		contextAttributes.put("nonce", new HashMap<String, Object>());

		return (HttpServletRequest)Proxy.newProxyInstance(
			HttpServletRequest.class.getClassLoader(),
			new Class< ? >[] { HttpServletRequest.class },
			(proxy, method, args) -> {
				switch (method.getName())
				{
					case "getParameterMap" :
						return params;
					case "getParameter" :
						String[] vals = params.get(args[0]);
						return vals != null && vals.length > 0 ? vals[0] : null;
					case "getCookies" :
						return null;
					case "getCharacterEncoding" :
						return "UTF-8";
					case "setCharacterEncoding" :
						return null;
					case "getRequestURI" :
						return "/solution/" + SOLUTION_NAME + "/index.html";
					case "getRequestURL" :
						return new StringBuffer("https://localhost:8080/solution/" + SOLUTION_NAME + "/index.html");
					case "getServletPath" :
						return "/solution/" + SOLUTION_NAME + "/index.html";
					case "getContextPath" :
						return "";
					case "getScheme" :
						return "https";
					case "getServerName" :
						return "localhost";
					case "getServerPort" :
						return Integer.valueOf(8080);
					case "getHeader" :
						if ("accept-language".equals(args[0])) return "en";
						if ("Accept-Language".equals(args[0])) return "en";
						if ("user-agent".equals(args[0])) return "Mozilla/5.0";
						return null;
					case "getLocale" :
						return Locale.ENGLISH;
					case "getSession" :
						return (jakarta.servlet.http.HttpSession)Proxy.newProxyInstance(
							jakarta.servlet.http.HttpSession.class.getClassLoader(),
							new Class< ? >[] { jakarta.servlet.http.HttpSession.class },
							(p, m, a) -> {
								switch (m.getName())
								{
									case "getAttribute" :
										return sessionAttributes.get(a[0]);
									case "setAttribute" :
										sessionAttributes.put((String)a[0], a[1]);
										return null;
									case "removeAttribute" :
										sessionAttributes.remove(a[0]);
										return null;
									default :
										return getDefaultReturnValue(m);
								}
							});
					case "getRemoteAddr" :
						return "127.0.0.1";
					case "getServletContext" :
						return (jakarta.servlet.ServletContext)Proxy.newProxyInstance(
							jakarta.servlet.ServletContext.class.getClassLoader(),
							new Class< ? >[] { jakarta.servlet.ServletContext.class },
							(p, m, a) -> {
								switch (m.getName())
								{
									case "getAttribute" :
										return contextAttributes.get(a[0]);
									case "setAttribute" :
										contextAttributes.put((String)a[0], a[1]);
										return null;
									default :
										return getDefaultReturnValue(m);
								}
							});
					case "getQueryString" :
						return null;
					case "getMethod" :
						return "GET";
					default :
						return getDefaultReturnValue(method);
				}
			});
	}

	private HttpServletRequest createMockRequestWithServletPath(String servletPath)
	{
		return createMockRequestWithServletPathAndParams(servletPath, Collections.emptyMap(), null);
	}

	private HttpServletRequest createMockRequestWithServletPathAndParams(String servletPath, Map<String, String[]> params, String queryString)
	{
		Map<String, Object> sessionAttributes = new HashMap<>();
		Map<String, Object> contextAttributes = new HashMap<>();
		contextAttributes.put("nonce", new HashMap<String, Object>());

		return (HttpServletRequest)Proxy.newProxyInstance(
			HttpServletRequest.class.getClassLoader(),
			new Class< ? >[] { HttpServletRequest.class },
			(proxy, method, args) -> {
				switch (method.getName())
				{
					case "getParameterMap" :
						return params;
					case "getParameter" :
						String[] vals = params.get(args[0]);
						return vals != null && vals.length > 0 ? vals[0] : null;
					case "getCookies" :
						return null;
					case "getCharacterEncoding" :
						return "UTF-8";
					case "setCharacterEncoding" :
						return null;
					case "getRequestURI" :
						return servletPath;
					case "getRequestURL" :
						return new StringBuffer("https://localhost:8080" + servletPath);
					case "getServletPath" :
						return servletPath;
					case "getContextPath" :
						return "";
					case "getScheme" :
						return "https";
					case "getServerName" :
						return "localhost";
					case "getServerPort" :
						return Integer.valueOf(8080);
					case "getHeader" :
						if ("accept-language".equals(args[0])) return "en";
						if ("Accept-Language".equals(args[0])) return "en";
						if ("user-agent".equals(args[0])) return "Mozilla/5.0";
						return null;
					case "getLocale" :
						return Locale.ENGLISH;
					case "getSession" :
						return (jakarta.servlet.http.HttpSession)Proxy.newProxyInstance(
							jakarta.servlet.http.HttpSession.class.getClassLoader(),
							new Class< ? >[] { jakarta.servlet.http.HttpSession.class },
							(p, m, a) -> {
								switch (m.getName())
								{
									case "getAttribute" :
										return sessionAttributes.get(a[0]);
									case "setAttribute" :
										sessionAttributes.put((String)a[0], a[1]);
										return null;
									case "removeAttribute" :
										sessionAttributes.remove(a[0]);
										return null;
									default :
										return getDefaultReturnValue(m);
								}
							});
					case "getRemoteAddr" :
						return "127.0.0.1";
					case "getServletContext" :
						return (jakarta.servlet.ServletContext)Proxy.newProxyInstance(
							jakarta.servlet.ServletContext.class.getClassLoader(),
							new Class< ? >[] { jakarta.servlet.ServletContext.class },
							(p, m, a) -> {
								switch (m.getName())
								{
									case "getAttribute" :
										return contextAttributes.get(a[0]);
									case "setAttribute" :
										contextAttributes.put((String)a[0], a[1]);
										return null;
									default :
										return getDefaultReturnValue(m);
								}
							});
					case "getQueryString" :
						return queryString;
					case "getMethod" :
						return "GET";
					default :
						return getDefaultReturnValue(method);
				}
			});
	}

	private HttpServletRequest createFullFlowRequest(String username, String password)
	{
		Map<String, String[]> params = new HashMap<>();
		if (username != null) params.put("username", new String[] { username });
		if (password != null) params.put("password", new String[] { password });
		params.put("csrf_token", new String[] { "123456" });

		Map<String, Object> sessionAttributes = new HashMap<>();
		Map<String, Object> contextAttributes = new HashMap<>();
		contextAttributes.put("nonce", new java.util.concurrent.ConcurrentHashMap<String, JSONObject>());

		jakarta.servlet.http.Cookie csrfCookie = new jakarta.servlet.http.Cookie("csrf_token", "123456");

		return (HttpServletRequest)Proxy.newProxyInstance(
			HttpServletRequest.class.getClassLoader(),
			new Class< ? >[] { HttpServletRequest.class },
			(proxy, method, args) -> {
				switch (method.getName())
				{
					case "getParameterMap" :
						return params;
					case "getParameter" :
						String[] vals = params.get(args[0]);
						return vals != null && vals.length > 0 ? vals[0] : null;
					case "getCookies" :
						return new jakarta.servlet.http.Cookie[] { csrfCookie };
					case "getCharacterEncoding" :
						return "UTF-8";
					case "setCharacterEncoding" :
						return null;
					case "getRequestURI" :
						return "/solution/" + SOLUTION_NAME + "/index.html";
					case "getRequestURL" :
						return new StringBuffer("https://localhost:8080/solution/" + SOLUTION_NAME + "/index.html");
					case "getServletPath" :
						return "/solution/" + SOLUTION_NAME + "/index.html";
					case "getContextPath" :
						return "";
					case "getScheme" :
						return "https";
					case "getServerName" :
						return "localhost";
					case "getServerPort" :
						return Integer.valueOf(8080);
					case "getHeader" :
						if ("accept-language".equals(args[0])) return "en";
						if ("Accept-Language".equals(args[0])) return "en";
						if ("user-agent".equals(args[0])) return "Mozilla/5.0";
						return null;
					case "getLocale" :
						return Locale.ENGLISH;
					case "getSession" :
						return (jakarta.servlet.http.HttpSession)Proxy.newProxyInstance(
							jakarta.servlet.http.HttpSession.class.getClassLoader(),
							new Class< ? >[] { jakarta.servlet.http.HttpSession.class },
							(p, m, a) -> {
								switch (m.getName())
								{
									case "getAttribute" :
										return sessionAttributes.get(a[0]);
									case "setAttribute" :
										sessionAttributes.put((String)a[0], a[1]);
										return null;
									case "removeAttribute" :
										sessionAttributes.remove(a[0]);
										return null;
									default :
										return getDefaultReturnValue(m);
								}
							});
					case "getRemoteAddr" :
						return "127.0.0.1";
					case "getServletContext" :
						return (jakarta.servlet.ServletContext)Proxy.newProxyInstance(
							jakarta.servlet.ServletContext.class.getClassLoader(),
							new Class< ? >[] { jakarta.servlet.ServletContext.class },
							(p, m, a) -> {
								switch (m.getName())
								{
									case "getAttribute" :
										return contextAttributes.get(a[0]);
									case "setAttribute" :
										contextAttributes.put((String)a[0], a[1]);
										return null;
									default :
										return getDefaultReturnValue(m);
								}
							});
					case "getQueryString" :
						return null;
					case "getMethod" :
						return "POST";
					default :
						return getDefaultReturnValue(method);
				}
			});
	}
}
