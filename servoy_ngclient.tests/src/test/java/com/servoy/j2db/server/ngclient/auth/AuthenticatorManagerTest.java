package com.servoy.j2db.server.ngclient.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.servoy.j2db.ClientLogin;
import com.servoy.j2db.Credentials;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RootObjectReference;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Solution.AUTHENTICATOR_TYPE;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.ngclient.StatelessLoginHandler;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServer;
import com.servoy.j2db.server.shared.IApplicationServerSingleton;
import com.servoy.j2db.server.shared.IServiceRegistry;
import com.servoy.j2db.util.UUID;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Integration tests for the AUTHENTICATOR login flow via {@link AuthenticatorManager}.
 * Tests checkAuthenticatorPermissions with a mocked ApplicationServerRegistry and IApplicationServer.
 *
 * @author emera
 */
@SuppressWarnings("nls")
public class AuthenticatorManagerTest
{
	private static final String TEST_JWT_PASSWORD = "test-jwt-secret-key-for-authenticator-tests";
	private static final String MAIN_SOLUTION_NAME = "mainSolution";
	private static final String AUTHENTICATOR_MODULE_NAME = "myAuthenticator";
	private static final String TEST_USER_UID = "auth-user-uid-456";
	private static final String TEST_USERNAME = "authuser";
	private static final String TEST_PASSWORD = "authpass";
	private static final String[] TEST_PERMISSIONS = { "Editors", "Viewers" };

	private ClientLogin loginResponse;
	private Credentials lastCredentials;
	private Solution mainSolution;
	private Solution authenticatorModule;

	@Before
	public void setUp() throws Exception
	{
		loginResponse = new ClientLogin(null, TEST_USER_UID, TEST_USERNAME, TEST_PERMISSIONS, null);
		lastCredentials = null;

		IApplicationServerSingleton appServer = createMockApplicationServer();
		ApplicationServerRegistry.setApplicationServerSingleton(appServer);

		// Now create SolutionMetaData and Solution objects (must happen AFTER registry is set up)
		authenticatorModule = createSolution(AUTHENTICATOR_MODULE_NAME, SolutionMetaData.AUTHENTICATOR, null);
		authenticatorModule.setOnOpenMethodID("fake-onopen-method-uuid"); // required for callAuthenticator to proceed
		mainSolution = createSolution(MAIN_SOLUTION_NAME, SolutionMetaData.SOLUTION, AUTHENTICATOR_MODULE_NAME);
		// Set authenticator type to AUTHENTICATOR
		mainSolution.setAuthenticator(AUTHENTICATOR_TYPE.AUTHENTICATOR);
	}

	@After
	public void tearDown()
	{
		ApplicationServerRegistry.destroy();
	}

	// ===== Login success tests =====

	@Test
	public void testLogin_authenticatorReturnsValidUser_authenticated() throws Exception
	{
		LoginResult result = LoginResult.needsLogin();
		HttpServletRequest request = createMockRequest(Collections.emptyMap());

		boolean verified = AuthenticatorManager.checkAuthenticatorPermissions(
			TEST_USERNAME, TEST_PASSWORD, false, null, result, mainSolution, request);

		assertTrue("Should be verified", verified);
		assertTrue("Should be authenticated", result.isAuthenticated());
		assertNotNull("Token should not be null", result.getToken());

		// Verify the token claims
		DecodedJWT decoded = JWT.decode(result.getToken());
		assertEquals(TEST_USERNAME, decoded.getClaim(SvyID.USERNAME).asString());
		assertEquals(TEST_USER_UID, decoded.getClaim(SvyID.UID).asString());
		String[] permissions = decoded.getClaim(SvyID.PERMISSIONS).asArray(String.class);
		assertEquals(2, permissions.length);
		assertEquals("Editors", permissions[0]);
		assertEquals("Viewers", permissions[1]);
	}

	@Test
	public void testLogin_authenticatorPassesCorrectCredentials() throws Exception
	{
		LoginResult result = LoginResult.needsLogin();
		HttpServletRequest request = createMockRequest(Collections.emptyMap());

		AuthenticatorManager.checkAuthenticatorPermissions(
			TEST_USERNAME, TEST_PASSWORD, false, null, result, mainSolution, request);

		assertNotNull("Credentials should have been captured", lastCredentials);
		assertEquals(AUTHENTICATOR_MODULE_NAME, lastCredentials.getAuthenticatorType());
		assertNull("Method should be null", lastCredentials.getMethod());

		// Verify JSON credentials contain username and password
		JSONObject json = new JSONObject(lastCredentials.getJscredentials());
		assertEquals(TEST_USERNAME, json.getString(SvyID.USERNAME));
		assertEquals(TEST_PASSWORD, json.getString(StatelessLoginHandler.PASSWORD));
	}

	@Test
	public void testLogin_withRememberUser_tokenHasRememberClaim() throws Exception
	{
		LoginResult result = LoginResult.needsLogin();
		HttpServletRequest request = createMockRequest(Collections.emptyMap());

		boolean verified = AuthenticatorManager.checkAuthenticatorPermissions(
			TEST_USERNAME, TEST_PASSWORD, true, null, result, mainSolution, request);

		assertTrue("Should be verified", verified);
		DecodedJWT decoded = JWT.decode(result.getToken());
		Boolean remember = decoded.getClaim(SvyID.REMEMBER).asBoolean();
		assertNotNull("Remember claim should exist", remember);
		assertTrue("Remember should be true", remember.booleanValue());
	}

	// ===== Login failure tests =====

	@Test
	public void testLogin_authenticatorReturnsNullUserUid_notAuthenticated() throws Exception
	{
		// Authenticator returns login but with null userUid (authentication failed on the JS side)
		loginResponse = new ClientLogin(null, null, TEST_USERNAME, null, "{\"error\":\"Invalid credentials\"}");

		LoginResult result = LoginResult.needsLogin();
		HttpServletRequest request = createMockRequest(Collections.emptyMap());

		boolean verified = AuthenticatorManager.checkAuthenticatorPermissions(
			TEST_USERNAME, TEST_PASSWORD, false, null, result, mainSolution, request);

		assertFalse("Should not be verified", verified);
		assertFalse("Should not be authenticated", result.isAuthenticated());
	}

	@Test
	public void testLogin_authenticatorReturnsNull_notAuthenticated() throws Exception
	{
		loginResponse = null; // simulate complete failure

		LoginResult result = LoginResult.needsLogin();
		HttpServletRequest request = createMockRequest(Collections.emptyMap());

		boolean verified = AuthenticatorManager.checkAuthenticatorPermissions(
			TEST_USERNAME, TEST_PASSWORD, false, null, result, mainSolution, request);

		assertFalse("Should not be verified", verified);
	}

	@Test
	public void testLogin_noAuthenticatorModuleFound_notAuthenticated() throws Exception
	{
		// Main solution with no modules
		Solution solutionWithoutModules = createSolution("noModules", SolutionMetaData.SOLUTION, null);
		solutionWithoutModules.setAuthenticator(AUTHENTICATOR_TYPE.AUTHENTICATOR);

		LoginResult result = LoginResult.needsLogin();
		HttpServletRequest request = createMockRequest(Collections.emptyMap());

		boolean verified = AuthenticatorManager.checkAuthenticatorPermissions(
			TEST_USERNAME, TEST_PASSWORD, false, null, result, solutionWithoutModules, request);

		assertFalse("Should not be verified when no authenticator module", verified);
	}

	// ===== ReturnValue tests =====

	@Test
	public void testLogin_authenticatorReturnsJsReturn_setAsReturnValue() throws Exception
	{
		String jsReturn = "{\"welcomeMessage\":\"Hello!\",\"redirectUrl\":\"/dashboard\"}";
		loginResponse = new ClientLogin(null, TEST_USER_UID, TEST_USERNAME, TEST_PERMISSIONS, jsReturn);

		LoginResult result = LoginResult.needsLogin();
		HttpServletRequest request = createMockRequest(Collections.emptyMap());

		boolean verified = AuthenticatorManager.checkAuthenticatorPermissions(
			TEST_USERNAME, TEST_PASSWORD, false, null, result, mainSolution, request);

		assertTrue("Should be verified", verified);
		assertEquals("ReturnValue should be the jsReturn", jsReturn, result.getReturnValue());
	}

	@Test
	public void testLogin_failedWithReturnValue_returnValueStillSet() throws Exception
	{
		String jsReturn = "{\"errorMessage\":\"Account locked\"}";
		loginResponse = new ClientLogin(null, null, TEST_USERNAME, null, jsReturn);

		LoginResult result = LoginResult.needsLogin();
		HttpServletRequest request = createMockRequest(Collections.emptyMap());

		boolean verified = AuthenticatorManager.checkAuthenticatorPermissions(
			TEST_USERNAME, TEST_PASSWORD, false, null, result, mainSolution, request);

		assertFalse("Should not be verified", verified);
		assertEquals("ReturnValue should still be set even on failure", jsReturn, result.getReturnValue());
	}

	// ===== Token refresh tests =====

	@Test
	public void testTokenRefresh_oldTokenPassedToAuthenticator() throws Exception
	{
		// First, do a normal login to get a valid token
		LoginResult firstLogin = LoginResult.needsLogin();
		HttpServletRequest request = createMockRequest(Collections.emptyMap());
		AuthenticatorManager.checkAuthenticatorPermissions(
			TEST_USERNAME, TEST_PASSWORD, false, null, firstLogin, mainSolution, request);
		assertNotNull("First login should produce a token", firstLogin.getToken());

		// Now simulate refresh with old token
		SvyID oldToken = new SvyID(firstLogin.getToken());
		LoginResult refreshResult = LoginResult.needsLogin();
		AuthenticatorManager.checkAuthenticatorPermissions(
			TEST_USERNAME, null, false, oldToken, refreshResult, mainSolution, request);

		// Verify the authenticator received the old token's payload as last_login
		assertNotNull("Credentials should have been captured", lastCredentials);
		JSONObject json = new JSONObject(lastCredentials.getJscredentials());
		assertTrue("Should contain last_login from old token", json.has(SvyID.LAST_LOGIN));
		assertEquals("Username should come from old token", TEST_USERNAME, json.getString(SvyID.USERNAME));
	}

	// ===== Custom parameters tests =====

	@Test
	public void testLogin_customParametersForwarded() throws Exception
	{
		Map<String, String[]> params = new HashMap<>();
		params.put("custom_tenant", new String[]{ "acme" });
		params.put("custom_language", new String[]{ "en" });
		params.put("username", new String[]{ TEST_USERNAME }); // should NOT be forwarded

		LoginResult result = LoginResult.needsLogin();
		HttpServletRequest request = createMockRequest(params);

		AuthenticatorManager.checkAuthenticatorPermissions(
			TEST_USERNAME, TEST_PASSWORD, false, null, result, mainSolution, request);

		assertNotNull("Credentials should have been captured", lastCredentials);
		JSONObject json = new JSONObject(lastCredentials.getJscredentials());
		assertEquals("Custom param should be forwarded", "acme", json.getString("custom_tenant"));
		assertEquals("Custom param should be forwarded", "en", json.getString("custom_language"));
	}

	// ===== Full-flow tests via StatelessLoginHandler.mustAuthenticate =====

	@Test
	public void testFullFlow_authenticator_validCredentials_svyTokenCreated() throws Exception
	{
		String csrfToken = "test-csrf-token-123";
		Map<String, String[]> params = new HashMap<>();
		params.put("username", new String[]{ TEST_USERNAME });
		params.put("password", new String[]{ TEST_PASSWORD });
		params.put("csrf_token", new String[]{ csrfToken });

		HttpServletRequest request = createFullFlowMockRequest(params, csrfToken);
		HttpServletResponse response = createProxy(HttpServletResponse.class, (proxy, method, args) -> getDefaultReturnValue(method));

		LoginResult result = StatelessLoginHandler.mustAuthenticate(request, response, MAIN_SOLUTION_NAME);

		assertTrue("Should be authenticated via full flow", result.isAuthenticated());
		assertNotNull("Token should be created", result.getToken());

		DecodedJWT decoded = JWT.decode(result.getToken());
		assertEquals(TEST_USERNAME, decoded.getClaim(SvyID.USERNAME).asString());
		assertEquals(TEST_USER_UID, decoded.getClaim(SvyID.UID).asString());
		String[] permissions = decoded.getClaim(SvyID.PERMISSIONS).asArray(String.class);
		assertEquals(2, permissions.length);
		assertEquals("Editors", permissions[0]);
		assertEquals("Viewers", permissions[1]);
	}

	@Test
	public void testFullFlow_authenticator_checkPermissions_existingToken_revalidates() throws Exception
	{
		String csrfToken = "123456";
		String svyToken = new SvyTokenBuilder(TEST_USERNAME, TEST_USER_UID, TEST_PERMISSIONS).sign();

		Map<String, String[]> params = new HashMap<>();
		params.put("id_token", new String[]{ svyToken });
		params.put("csrf_token", new String[]{ csrfToken });

		HttpServletRequest request = createFullFlowMockRequest(params, csrfToken);
		HttpServletResponse response = createProxy(HttpServletResponse.class, (proxy, method, args) -> getDefaultReturnValue(method));

		LoginResult result = StatelessLoginHandler.mustAuthenticate(request, response, MAIN_SOLUTION_NAME);

		assertTrue("Should be authenticated via checkPermissions path", result.isAuthenticated());
		assertNotNull("Token should be present", result.getToken());

		DecodedJWT decoded = JWT.decode(result.getToken());
		assertEquals(TEST_USERNAME, decoded.getClaim(SvyID.USERNAME).asString());
		assertEquals(TEST_USER_UID, decoded.getClaim(SvyID.UID).asString());
	}

	// ===== Full-flow tests: existing token validation failures =====

	@Test
	public void testFullFlow_authenticator_existingToken_expired_triggersRefresh() throws Exception
	{
		java.util.Properties settings = ApplicationServerRegistry.get().getServerAccess().getSettings();
		String jwtPassword = settings.getProperty(StatelessLoginUtils.JWT_Password);
		String expiredToken = JWT.create()
			.withIssuer("svy")
			.withClaim(SvyID.USERNAME, TEST_USERNAME)
			.withClaim(SvyID.UID, TEST_USER_UID)
			.withArrayClaim(SvyID.PERMISSIONS, TEST_PERMISSIONS)
			.withIssuedAt(new java.util.Date(System.currentTimeMillis() - 7200000))
			.withExpiresAt(new java.util.Date(System.currentTimeMillis() - 1000))
			.sign(Algorithm.HMAC256(jwtPassword));

		String csrfToken = "123456";
		Map<String, String[]> params = new HashMap<>();
		params.put("id_token", new String[]{ expiredToken });
		params.put("csrf_token", new String[]{ csrfToken });

		HttpServletRequest request = createFullFlowMockRequest(params, csrfToken);
		HttpServletResponse response = createProxy(HttpServletResponse.class, (proxy, method, args) -> getDefaultReturnValue(method));

		LoginResult result = StatelessLoginHandler.mustAuthenticate(request, response, MAIN_SOLUTION_NAME);

		assertTrue("Should be authenticated after expired token refresh", result.isAuthenticated());
		assertNotNull("Should have a new token", result.getToken());
	}

	@Test
	public void testFullFlow_authenticator_existingToken_expired_refreshFails_writesLoginPage() throws Exception
	{
		// Authenticator returns a jsReturn with error info that should be resolved in the login page tags
		String jsReturn = "{\"errorMessage\":\"Invalid credentials\",\"errorTitle\":\"Login Failed\"}";
		loginResponse = new ClientLogin(null, null, TEST_USERNAME, null, jsReturn);

		java.util.Properties settings = ApplicationServerRegistry.get().getServerAccess().getSettings();
		String jwtPassword = settings.getProperty(StatelessLoginUtils.JWT_Password);
		String expiredToken = JWT.create()
			.withIssuer("svy")
			.withClaim(SvyID.USERNAME, TEST_USERNAME)
			.withClaim(SvyID.UID, TEST_USER_UID)
			.withArrayClaim(SvyID.PERMISSIONS, TEST_PERMISSIONS)
			.withIssuedAt(new java.util.Date(System.currentTimeMillis() - 7200000))
			.withExpiresAt(new java.util.Date(System.currentTimeMillis() - 1000))
			.sign(Algorithm.HMAC256(jwtPassword));

		String csrfToken = "123456";
		Map<String, String[]> params = new HashMap<>();
		params.put("id_token", new String[]{ expiredToken });
		params.put("csrf_token", new String[]{ csrfToken });

		HttpServletRequest request = createFullFlowMockRequest(params, csrfToken);
		HttpServletResponse response = createProxy(HttpServletResponse.class, (proxy, method, args) -> getDefaultReturnValue(method));

		LoginResult result = StatelessLoginHandler.mustAuthenticate(request, response, MAIN_SOLUTION_NAME);

		assertFalse("Should NOT be authenticated when authenticator refresh fails", result.isAuthenticated());

		// Add custom login.html with %% tags to the solution
		String customLoginHtml = "<html><body><h1>%%solutionTitle%%</h1><p>%%errorMessage%%</p><p>%%errorTitle%%</p>" +
			"<form name=\"login_form\"><input name=\"username\"><input name=\"password\"></form></body></html>";
		addLoginHtmlMedia(mainSolution, customLoginHtml);

		java.io.StringWriter pageOutput = new java.io.StringWriter();
		java.io.PrintWriter printWriter = new java.io.PrintWriter(pageOutput);
		HttpServletResponse writeResponse = createProxy(HttpServletResponse.class, (proxy, method, args) -> {
			if ("getWriter".equals(method.getName())) return printWriter;
			if ("setCharacterEncoding".equals(method.getName())) return null;
			if ("setContentType".equals(method.getName())) return null;
			if ("setContentLengthLong".equals(method.getName())) return null;
			if ("addHeader".equals(method.getName())) return null;
			if ("setHeader".equals(method.getName())) return null;
			if ("setStatus".equals(method.getName())) return null;
			if ("sendRedirect".equals(method.getName())) return null;
			if ("getCharacterEncoding".equals(method.getName())) return "UTF-8";
			if ("addCookie".equals(method.getName())) return null;
			return getDefaultReturnValue(method);
		});
		StatelessLoginHandler.writeLoginPage(request, writeResponse, MAIN_SOLUTION_NAME, result);
		String page = pageOutput.toString();

		// Verify that convertReturnValueToMap + tag resolution worked
		assertTrue("Should contain resolved errorMessage", page.contains("Invalid credentials"));
		assertTrue("Should contain resolved errorTitle", page.contains("Login Failed"));
		assertTrue("Should contain solution title", page.contains(MAIN_SOLUTION_NAME));
	}

	// ===== Helper methods =====

	private void addLoginHtmlMedia(Solution solution, String htmlContent) throws Exception
	{
		Constructor<com.servoy.j2db.persistence.Media> mediaCtor =
			com.servoy.j2db.persistence.Media.class.getDeclaredConstructor(
				com.servoy.j2db.persistence.ISupportChilds.class, UUID.class);
		mediaCtor.setAccessible(true);
		com.servoy.j2db.persistence.Media media = mediaCtor.newInstance(solution, UUID.randomUUID());
		media.setName("login.html");
		media.setPermMediaData(htmlContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
		solution.addChild(media);
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
			new Class<?>[]{ IRepository.class },
			(proxy, method, args) -> {
				if ("getActiveRootObject".equals(method.getName()))
				{
					if (args.length == 2)
					{
						String name = (String)args[0];
						if (AUTHENTICATOR_MODULE_NAME.equals(name))
						{
							return authenticatorModule;
						}
						if (MAIN_SOLUTION_NAME.equals(name))
						{
							return mainSolution;
						}
					}
					if (args.length == 1 && args[0] instanceof UUID)
					{
						UUID uuid = (UUID)args[0];
						if (mainSolution != null && uuid.equals(mainSolution.getSolutionMetaData().getRootObjectUuid()))
						{
							return mainSolution;
						}
						if (authenticatorModule != null && uuid.equals(authenticatorModule.getSolutionMetaData().getRootObjectUuid()))
						{
							return authenticatorModule;
						}
					}
				}
				if ("getRootObjectMetaData".equals(method.getName()) && args.length == 2)
				{
					String name = (String)args[0];
					if (MAIN_SOLUTION_NAME.equals(name))
					{
						return mainSolution.getSolutionMetaData();
					}
				}
				if ("getActiveSolutionModuleMetaDatas".equals(method.getName()) && args.length == 1)
				{
					SolutionMetaData metaData = mainSolution.getSolutionMetaData();
					RootObjectReference ref = new RootObjectReference(metaData, -1);
					return Collections.singletonList(ref);
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
				if ("getSettings".equals(method.getName()))
				{
					return settings;
				}
				return getDefaultReturnValue(method);
			});

		IRepository repository = createMockRepository();

		IApplicationServer applicationServer = (IApplicationServer)Proxy.newProxyInstance(getClass().getClassLoader(),
			new Class<?>[]{ IApplicationServer.class },
			(proxy, method, args) -> {
				if ("login".equals(method.getName()))
				{
					lastCredentials = (Credentials)args[0];
					return loginResponse;
				}
				if ("getLoginSolutionDefinitions".equals(method.getName()))
				{
					return new SolutionMetaData[0];
				}
				return getDefaultReturnValue(method);
			});

		IServiceRegistry serviceRegistry = createProxy(IServiceRegistry.class, (proxy, method, args) -> {
			if ("getService".equals(method.getName()) && args != null && args.length == 1)
			{
				if (args[0] == IApplicationServer.class)
				{
					return applicationServer;
				}
			}
			return getDefaultReturnValue(method);
		});
		ApplicationServerRegistry.setServiceRegistry(serviceRegistry);

		return createProxy(IApplicationServerSingleton.class, (proxy, method, args) -> {
			switch (method.getName())
			{
				case "getLocalRepository" :
					return repository;
				case "getServerAccess" :
					return serverAccess;
				case "calculateProtectionPassword" :
					return null;
				case "getService" :
					if (args != null && args.length == 1 && args[0] == IApplicationServer.class)
					{
						return applicationServer;
					}
					return null;
				default :
					return getDefaultReturnValue(method);
			}
		});
	}

	@SuppressWarnings("unchecked")
	private HttpServletRequest createMockRequest(Map<String, String[]> parameters)
	{
		return createProxy(HttpServletRequest.class, (proxy, method, args) -> {
			switch (method.getName())
			{
				case "getParameterMap" :
					return parameters;
				case "getParameter" :
					String[] values = parameters.get(args[0]);
					return values != null && values.length > 0 ? values[0] : null;
				case "getCharacterEncoding" :
					return "UTF-8";
				default :
					return getDefaultReturnValue(method);
			}
		});
	}

	private HttpServletRequest createFullFlowMockRequest(Map<String, String[]> parameters, String csrfToken)
	{
		Cookie csrfCookie = new Cookie("csrf_token", csrfToken);
		return createProxy(HttpServletRequest.class, (proxy, method, args) -> {
			switch (method.getName())
			{
				case "getParameterMap" :
					return parameters;
				case "getParameter" :
					String[] values = parameters.get(args[0]);
					return values != null && values.length > 0 ? values[0] : null;
				case "getCharacterEncoding" :
					return "UTF-8";
				case "setCharacterEncoding" :
					return null;
				case "getRequestURI" :
					return "/solution/" + MAIN_SOLUTION_NAME + "/index.html";
				case "getServletPath" :
					return "/solution/" + MAIN_SOLUTION_NAME + "/index.html";
				case "getContextPath" :
					return "";
				case "getScheme" :
					return "https";
				case "getServerName" :
					return "localhost";
				case "getServerPort" :
					return Integer.valueOf(8080);
				case "getCookies" :
					return new Cookie[]{ csrfCookie };
				case "getSession" :
					return null;
				case "getLocale" :
					return java.util.Locale.ENGLISH;
				case "getHeader" :
					if ("accept-language".equals(args[0])) return "en";
					return null;
				case "getRemoteAddr" :
					return "127.0.0.1";
				default :
					return getDefaultReturnValue(method);
			}
		});
	}

	@SuppressWarnings("unchecked")
	private <T> T createProxy(Class<T> iface, InvocationHandler handler)
	{
		return (T)Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ iface }, handler);
	}

	private static Class<?>[] getAllInterfaces(Class<?> iface)
	{
		java.util.Set<Class<?>> interfaces = new java.util.LinkedHashSet<>();
		collectInterfaces(iface, interfaces);
		return interfaces.toArray(new Class<?>[0]);
	}

	private static void collectInterfaces(Class<?> iface, java.util.Set<Class<?>> interfaces)
	{
		interfaces.add(iface);
		for (Class<?> superIface : iface.getInterfaces())
		{
			collectInterfaces(superIface, interfaces);
		}
	}

	private static Object getDefaultReturnValue(Method method)
	{
		Class<?> returnType = method.getReturnType();
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

	// ===== Tenant value propagation tests (SVY-21382) =====

	@Test
	public void testLogin_withTenantValues_tokenContainsTenantsClaim() throws Exception
	{
		String[] tenants = { "acme-corp", "beta-inc" };
		loginResponse = new ClientLogin(null, TEST_USER_UID, TEST_USERNAME, TEST_PERMISSIONS, null, tenants);

		LoginResult result = LoginResult.needsLogin();
		HttpServletRequest request = createMockRequest(Collections.emptyMap());

		boolean verified = AuthenticatorManager.checkAuthenticatorPermissions(
			TEST_USERNAME, TEST_PASSWORD, false, null, result, mainSolution, request);

		assertTrue("Should be verified", verified);
		assertNotNull("Token should not be null", result.getToken());

		DecodedJWT decoded = JWT.decode(result.getToken());
		String[] tokenTenants = decoded.getClaim(SvyID.TENANTS).asArray(String.class);
		assertNotNull("Tenants claim should exist in token", tokenTenants);
		assertEquals(2, tokenTenants.length);
		assertEquals("acme-corp", tokenTenants[0]);
		assertEquals("beta-inc", tokenTenants[1]);
	}

	@Test
	public void testLogin_nullTenantValues_tokenHasNoTenantsClaim() throws Exception
	{
		loginResponse = new ClientLogin(null, TEST_USER_UID, TEST_USERNAME, TEST_PERMISSIONS, null);

		LoginResult result = LoginResult.needsLogin();
		HttpServletRequest request = createMockRequest(Collections.emptyMap());

		boolean verified = AuthenticatorManager.checkAuthenticatorPermissions(
			TEST_USERNAME, TEST_PASSWORD, false, null, result, mainSolution, request);

		assertTrue("Should be verified", verified);
		assertNotNull("Token should not be null", result.getToken());

		DecodedJWT decoded = JWT.decode(result.getToken());
		assertTrue("Tenants claim should be null/missing when no tenants provided",
			decoded.getClaim(SvyID.TENANTS).isNull() || decoded.getClaim(SvyID.TENANTS).isMissing());
	}

	@Test
	public void testLogin_tenantValuesWithJsReturn_allPresent() throws Exception
	{
		String[] tenants = { "my-tenant" };
		String jsReturn = "{\"welcome\":true}";
		loginResponse = new ClientLogin(null, TEST_USER_UID, TEST_USERNAME, TEST_PERMISSIONS, jsReturn, tenants);

		LoginResult result = LoginResult.needsLogin();
		HttpServletRequest request = createMockRequest(Collections.emptyMap());

		AuthenticatorManager.checkAuthenticatorPermissions(
			TEST_USERNAME, TEST_PASSWORD, false, null, result, mainSolution, request);

		DecodedJWT decoded = JWT.decode(result.getToken());
		String[] tokenTenants = decoded.getClaim(SvyID.TENANTS).asArray(String.class);
		assertNotNull("Tenants should be present", tokenTenants);
		assertEquals("my-tenant", tokenTenants[0]);
		assertEquals(TEST_USERNAME, decoded.getClaim(SvyID.USERNAME).asString());
		assertEquals(TEST_USER_UID, decoded.getClaim(SvyID.UID).asString());
		assertEquals(jsReturn, result.getReturnValue());
	}
}
