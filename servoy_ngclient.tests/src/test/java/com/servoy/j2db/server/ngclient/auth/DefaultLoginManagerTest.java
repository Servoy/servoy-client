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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
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
 * Integration tests for the DEFAULT authenticator login flow via {@link DefaultLoginManager}.
 * Tests the checkDefaultLoginPermissions method with a mocked ApplicationServerRegistry.
 *
 * @author emera
 */
@SuppressWarnings("nls")
public class DefaultLoginManagerTest
{
	private static final String TEST_CLIENT_ID = "test-client-id";
	private static final String TEST_JWT_PASSWORD = "test-jwt-secret-key-for-unit-tests-only";
	private static final String TEST_USER_UID = "user-uid-123";
	private static final String TEST_USERNAME = "testuser";
	private static final String TEST_PASSWORD = "testpass";
	private static final String[] TEST_PERMISSIONS = { "Administrators", "Users" };
	private static final String MAIN_SOLUTION_NAME = "defaultLoginSolution";

	private String validUserForAuth;
	private String[] permissionsForUser;
	private long passwordLastSetTime;
	private Solution mainSolution;

	@Before
	public void setUp() throws Exception
	{
		validUserForAuth = TEST_USER_UID;
		permissionsForUser = TEST_PERMISSIONS;
		passwordLastSetTime = 0;

		IApplicationServerSingleton appServer = createMockApplicationServer();
		ApplicationServerRegistry.setApplicationServerSingleton(appServer);

		mainSolution = createSolution(MAIN_SOLUTION_NAME, SolutionMetaData.SOLUTION);
		mainSolution.setAuthenticator(AUTHENTICATOR_TYPE.DEFAULT);
	}

	@After
	public void tearDown()
	{
		ApplicationServerRegistry.destroy();
	}

	// ===== Fresh login tests =====

	@Test
	public void testFreshLogin_validCredentials_authenticates() throws Exception
	{
		LoginResult result = LoginResult.needsLogin();
		boolean verified = DefaultLoginManager.checkDefaultLoginPermissions(TEST_USERNAME, TEST_PASSWORD, false, null, result);

		assertTrue("Should be verified", verified);
		assertTrue("Should be authenticated", result.isAuthenticated());
		assertNotNull("Token should not be null", result.getToken());

		// Verify the token contains expected claims
		DecodedJWT decoded = JWT.decode(result.getToken());
		assertEquals(TEST_USERNAME, decoded.getClaim(SvyID.USERNAME).asString());
		assertEquals(TEST_USER_UID, decoded.getClaim(SvyID.UID).asString());
		String[] permissions = decoded.getClaim(SvyID.PERMISSIONS).asArray(String.class);
		assertNotNull(permissions);
		assertEquals(2, permissions.length);
		assertEquals("Administrators", permissions[0]);
		assertEquals("Users", permissions[1]);
	}

	@Test
	public void testFreshLogin_invalidCredentials_notAuthenticated() throws Exception
	{
		validUserForAuth = null; // simulate invalid credentials

		LoginResult result = LoginResult.needsLogin();
		boolean verified = DefaultLoginManager.checkDefaultLoginPermissions(TEST_USERNAME, "wrong-password", false, null, result);

		assertFalse("Should not be verified", verified);
		assertNull("Token should be null", result.getToken());
	}

	@Test
	public void testFreshLogin_validCredentials_noPermissions() throws Exception
	{
		permissionsForUser = new String[0]; // user has no groups

		LoginResult result = LoginResult.needsLogin();
		boolean verified = DefaultLoginManager.checkDefaultLoginPermissions(TEST_USERNAME, TEST_PASSWORD, false, null, result);

		assertFalse("Should not be verified when no permissions", verified);
		assertNull("Token should be null", result.getToken());
	}

	@Test
	public void testFreshLogin_withRememberUser_tokenHasRememberClaim() throws Exception
	{
		LoginResult result = LoginResult.needsLogin();
		boolean verified = DefaultLoginManager.checkDefaultLoginPermissions(TEST_USERNAME, TEST_PASSWORD, true, null, result);

		assertTrue("Should be verified", verified);
		assertNotNull("Token should not be null", result.getToken());

		DecodedJWT decoded = JWT.decode(result.getToken());
		Boolean remember = decoded.getClaim(SvyID.REMEMBER).asBoolean();
		assertNotNull("Remember claim should exist", remember);
		assertTrue("Remember should be true", remember.booleanValue());
	}

	@Test
	public void testFreshLogin_withoutRememberUser_tokenHasNoRememberClaim() throws Exception
	{
		LoginResult result = LoginResult.needsLogin();
		boolean verified = DefaultLoginManager.checkDefaultLoginPermissions(TEST_USERNAME, TEST_PASSWORD, false, null, result);

		assertTrue("Should be verified", verified);
		assertNotNull("Token should not be null", result.getToken());

		DecodedJWT decoded = JWT.decode(result.getToken());
		assertNull("Remember claim should be null", decoded.getClaim(SvyID.REMEMBER).asBoolean());
	}

	// ===== Token refresh tests =====

	@Test
	public void testTokenRefresh_validToken_permissionsUnchanged_authenticates() throws Exception
	{
		// First, login to get a valid token
		LoginResult firstLogin = LoginResult.needsLogin();
		DefaultLoginManager.checkDefaultLoginPermissions(TEST_USERNAME, TEST_PASSWORD, false, null, firstLogin);
		assertNotNull("First login should produce a token", firstLogin.getToken());

		// Now simulate a token refresh with the same permissions
		SvyID oldToken = new SvyID(firstLogin.getToken());
		LoginResult refreshResult = LoginResult.needsLogin();
		boolean verified = DefaultLoginManager.checkDefaultLoginPermissions(TEST_USERNAME, null, false, oldToken, refreshResult);

		assertTrue("Should be verified on refresh", verified);
		assertTrue("Should be authenticated", refreshResult.isAuthenticated());
		assertNotNull("Should get a new token", refreshResult.getToken());
	}

	@Test
	public void testTokenRefresh_permissionsChanged_notAuthenticated() throws Exception
	{
		// First, login to get a valid token
		LoginResult firstLogin = LoginResult.needsLogin();
		DefaultLoginManager.checkDefaultLoginPermissions(TEST_USERNAME, TEST_PASSWORD, false, null, firstLogin);
		assertNotNull("First login should produce a token", firstLogin.getToken());

		// Now change the permissions on the server
		permissionsForUser = new String[]{ "Users" }; // changed from ["Administrators", "Users"]

		SvyID oldToken = new SvyID(firstLogin.getToken());
		LoginResult refreshResult = LoginResult.needsLogin();
		boolean verified = DefaultLoginManager.checkDefaultLoginPermissions(TEST_USERNAME, null, false, oldToken, refreshResult);

		assertFalse("Should not be verified when permissions changed", verified);
	}

	@Test
	public void testTokenRefresh_passwordChanged_notAuthenticated() throws Exception
	{
		// First, login to get a valid token
		LoginResult firstLogin = LoginResult.needsLogin();
		DefaultLoginManager.checkDefaultLoginPermissions(TEST_USERNAME, TEST_PASSWORD, false, null, firstLogin);
		assertNotNull("First login should produce a token", firstLogin.getToken());

		// Simulate password change after login
		passwordLastSetTime = System.currentTimeMillis() + 1000; // password changed after token was issued

		SvyID oldToken = new SvyID(firstLogin.getToken());
		LoginResult refreshResult = LoginResult.needsLogin();
		boolean verified = DefaultLoginManager.checkDefaultLoginPermissions(TEST_USERNAME, null, false, oldToken, refreshResult);

		assertFalse("Should not be verified when password changed", verified);
		assertFalse("Should not be authenticated", refreshResult.isAuthenticated());
		assertNull("Token should be null", refreshResult.getToken());
	}

	@Test
	public void testTokenRefresh_tokenHasValidJwtStructure() throws Exception
	{
		LoginResult result = LoginResult.needsLogin();
		DefaultLoginManager.checkDefaultLoginPermissions(TEST_USERNAME, TEST_PASSWORD, false, null, result);

		assertNotNull("Token should not be null", result.getToken());

		// Verify the JWT is properly signed and verifiable
		Algorithm algorithm = Algorithm.HMAC256(TEST_JWT_PASSWORD);
		DecodedJWT verified = JWT.require(algorithm).withIssuer("svy").build().verify(result.getToken());
		assertNotNull("JWT should be verifiable", verified);
		assertNotNull("Should have expiry", verified.getExpiresAt());
		assertNotNull("Should have last_login claim", verified.getClaim(SvyID.LAST_LOGIN).asLong());
	}

	// ===== Full-flow tests via StatelessLoginHandler.mustAuthenticate =====

	@Test
	public void testFullFlow_default_validCredentials_svyTokenCreated() throws Exception
	{
		String csrfToken = "test-csrf-token-456";
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
		assertNotNull(permissions);
		assertEquals(2, permissions.length);
		assertEquals("Administrators", permissions[0]);
		assertEquals("Users", permissions[1]);
	}

	@Test
	public void testFullFlow_default_checkPermissions_existingToken_revalidates() throws Exception
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

		// The token is re-created by DefaultLoginManager.checkDefaultLoginPermissions
		// username comes from request.getParameter("username") which is null in this flow
		// but uid and permissions are from the old token
		DecodedJWT decoded = JWT.decode(result.getToken());
		assertEquals(TEST_USER_UID, decoded.getClaim(SvyID.UID).asString());
		String[] permissions = decoded.getClaim(SvyID.PERMISSIONS).asArray(String.class);
		assertNotNull(permissions);
		assertEquals(2, permissions.length);
	}

	// ===== Full-flow tests: existing token validation failures =====

	@Test
	public void testFullFlow_default_existingToken_expired_triggersRefresh() throws Exception
	{
		String expiredToken = JWT.create()
			.withIssuer("svy")
			.withClaim(SvyID.USERNAME, TEST_USERNAME)
			.withClaim(SvyID.UID, TEST_USER_UID)
			.withArrayClaim(SvyID.PERMISSIONS, TEST_PERMISSIONS)
			.withClaim(SvyID.LAST_LOGIN, Long.valueOf(System.currentTimeMillis() - 3600000))
			.withIssuedAt(new java.util.Date(System.currentTimeMillis() - 7200000))
			.withExpiresAt(new java.util.Date(System.currentTimeMillis() - 1000))
			.sign(Algorithm.HMAC256(TEST_JWT_PASSWORD));

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
	public void testFullFlow_default_existingToken_expired_refreshFails_writesLoginPage() throws Exception
	{
		permissionsForUser = new String[]{ "Users" };

		String expiredToken = JWT.create()
			.withIssuer("svy")
			.withClaim(SvyID.USERNAME, TEST_USERNAME)
			.withClaim(SvyID.UID, TEST_USER_UID)
			.withArrayClaim(SvyID.PERMISSIONS, TEST_PERMISSIONS)
			.withIssuedAt(new java.util.Date(System.currentTimeMillis() - 7200000))
			.withExpiresAt(new java.util.Date(System.currentTimeMillis() - 1000))
			.sign(Algorithm.HMAC256(TEST_JWT_PASSWORD));

		String csrfToken = "123456";
		Map<String, String[]> params = new HashMap<>();
		params.put("id_token", new String[]{ expiredToken });
		params.put("csrf_token", new String[]{ csrfToken });

		HttpServletRequest request = createFullFlowMockRequest(params, csrfToken);
		HttpServletResponse response = createProxy(HttpServletResponse.class, (proxy, method, args) -> getDefaultReturnValue(method));

		LoginResult result = StatelessLoginHandler.mustAuthenticate(request, response, MAIN_SOLUTION_NAME);

		assertFalse("Should NOT be authenticated when permissions changed on refresh", result.isAuthenticated());

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
			return getDefaultReturnValue(method);
		});
		StatelessLoginHandler.writeLoginPage(request, writeResponse, MAIN_SOLUTION_NAME, result);
		String page = pageOutput.toString();
		assertTrue("Should write login page HTML", page.contains("login") || page.length() > 0);
	}

	// ===== Helper methods =====

	private Solution createSolution(String name, int solutionType) throws Exception
	{
		SolutionMetaData metaData = new SolutionMetaData(UUID.randomUUID(), name, IRepository.SOLUTIONS, 1, 1);
		metaData.setSolutionType(solutionType);

		Constructor<Solution> ctor = Solution.class.getDeclaredConstructor(IRepository.class, SolutionMetaData.class);
		ctor.setAccessible(true);
		return ctor.newInstance(createMockRepository(), metaData);
	}

	private IRepository createMockRepository()
	{
		return (IRepository)Proxy.newProxyInstance(getClass().getClassLoader(),
			new Class<?>[]{ IRepository.class },
			(proxy, method, args) -> {
				if ("getRootObjectMetaData".equals(method.getName()) && args.length == 2)
				{
					String name = (String)args[0];
					if (MAIN_SOLUTION_NAME.equals(name) && mainSolution != null)
					{
						return mainSolution.getSolutionMetaData();
					}
				}
				if ("getActiveSolutionModuleMetaDatas".equals(method.getName()) && args.length == 1)
				{
					if (mainSolution != null)
					{
						SolutionMetaData metaData = mainSolution.getSolutionMetaData();
						RootObjectReference ref = new RootObjectReference(metaData, -1);
						return Collections.singletonList(ref);
					}
				}
				if ("getActiveRootObject".equals(method.getName()))
				{
					if (args.length == 1 && args[0] instanceof UUID && mainSolution != null)
					{
						UUID uuid = (UUID)args[0];
						if (uuid.equals(mainSolution.getSolutionMetaData().getRootObjectUuid()))
						{
							return mainSolution;
						}
					}
					if (args.length == 2 && MAIN_SOLUTION_NAME.equals(args[0]) && mainSolution != null)
					{
						return mainSolution;
					}
				}
				return getDefaultReturnValue(method);
			});
	}

	@SuppressWarnings("unchecked")
	private <T> T createProxy(Class<T> iface, InvocationHandler handler)
	{
		return (T)Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ iface }, handler);
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

		Object userManager = Proxy.newProxyInstance(getClass().getClassLoader(),
			getAllInterfaces(com.servoy.j2db.server.shared.IUserManager.class),
			(proxy, method, args) -> {
				if ("getUserGroups".equals(method.getName()) && args.length == 2 && args[1] instanceof String)
				{
					return permissionsForUser;
				}
				if ("getPasswordLastSet".equals(method.getName()))
				{
					return Long.valueOf(passwordLastSetTime);
				}
				return getDefaultReturnValue(method);
			});

		IRepository repository = createMockRepository();

		IApplicationServer applicationServer = (IApplicationServer)Proxy.newProxyInstance(getClass().getClassLoader(),
			new Class<?>[]{ IApplicationServer.class },
			(proxy, method, args) -> {
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
				case "getClientId" :
					return TEST_CLIENT_ID;
				case "getUserManager" :
					return userManager;
				case "getServerAccess" :
					return serverAccess;
				case "getLocalRepository" :
					return repository;
				case "checkDefaultServoyAuthorisation" :
					if (args != null && TEST_USERNAME.equals(args[0]) && TEST_PASSWORD.equals(args[1]))
					{
						return validUserForAuth;
					}
					return null;
				default :
					return getDefaultReturnValue(method);
			}
		});
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
}
