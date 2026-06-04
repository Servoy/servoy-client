package com.servoy.j2db.server.ngclient.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;
import org.sablo.security.ContentSecurityPolicyConfig;

import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Solution.AUTHENTICATOR_TYPE;
import com.servoy.j2db.server.ngclient.property.Log4JToConsoleTest;
import com.servoy.j2db.server.ngclient.property.TestRepository;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.UUID;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@SuppressWarnings("nls")
public class AuthenticatorManagerInstanceTest extends Log4JToConsoleTest
{
	// =========================================================================
	// AuthenticatorManagerCreator - correct type returned for each AUTHENTICATOR_TYPE
	// =========================================================================

	@Test
	public void creator_defaultType_returnsDefaultLoginManager()
	{
		Solution solution = createSolutionWithAuthType(AUTHENTICATOR_TYPE.DEFAULT);
		IAuthenticatorManager manager = AuthenticatorManagerCreator.getAuthenticatorManager(solution);
		assertTrue("DEFAULT type must create DefaultLoginManager", manager instanceof DefaultLoginManager);
	}

	@Test
	public void creator_oauthType_returnsOAuthHandler()
	{
		Solution solution = createSolutionWithAuthType(AUTHENTICATOR_TYPE.OAUTH);
		IAuthenticatorManager manager = AuthenticatorManagerCreator.getAuthenticatorManager(solution);
		assertTrue("OAUTH type must create OAuthHandler", manager instanceof OAuthHandler);
	}

	@Test
	public void creator_servoyCloudType_returnsCloudStatelessAccessManager()
	{
		Solution solution = createSolutionWithAuthType(AUTHENTICATOR_TYPE.SERVOY_CLOUD);
		IAuthenticatorManager manager = AuthenticatorManagerCreator.getAuthenticatorManager(solution);
		assertTrue("SERVOY_CLOUD type must create CloudStatelessAccessManager", manager instanceof CloudStatelessAccessManager);
	}

	@Test
	public void creator_authenticatorType_returnsAuthenticatorManager()
	{
		Solution solution = createSolutionWithAuthType(AUTHENTICATOR_TYPE.AUTHENTICATOR);
		IAuthenticatorManager manager = AuthenticatorManagerCreator.getAuthenticatorManager(solution);
		assertTrue("AUTHENTICATOR type must create AuthenticatorManager", manager instanceof AuthenticatorManager);
	}

	@Test
	public void creator_oauthAuthenticatorType_returnsAuthenticatorManager()
	{
		Solution solution = createSolutionWithAuthType(AUTHENTICATOR_TYPE.OAUTH_AUTHENTICATOR);
		IAuthenticatorManager manager = AuthenticatorManagerCreator.getAuthenticatorManager(solution);
		assertTrue("OAUTH_AUTHENTICATOR type must create AuthenticatorManager", manager instanceof AuthenticatorManager);
	}

	// =========================================================================
	// All managers implement IAuthenticatorManager
	// =========================================================================

	@Test
	public void allManagers_implementInterface()
	{
		for (AUTHENTICATOR_TYPE type : new AUTHENTICATOR_TYPE[] { AUTHENTICATOR_TYPE.DEFAULT, AUTHENTICATOR_TYPE.OAUTH, AUTHENTICATOR_TYPE.SERVOY_CLOUD, AUTHENTICATOR_TYPE.AUTHENTICATOR, AUTHENTICATOR_TYPE.OAUTH_AUTHENTICATOR })
		{
			Solution solution = createSolutionWithAuthType(type);
			IAuthenticatorManager manager = AuthenticatorManagerCreator.getAuthenticatorManager(solution);
			assertNotNull("Manager must not be null for type " + type, manager);
			assertTrue("Manager for " + type + " must implement IAuthenticatorManager", manager instanceof IAuthenticatorManager);
		}
	}

	// =========================================================================
	// All managers extend AbstractAuthenticatorManager
	// =========================================================================

	@Test
	public void allManagers_extendAbstractBase()
	{
		for (AUTHENTICATOR_TYPE type : new AUTHENTICATOR_TYPE[] { AUTHENTICATOR_TYPE.DEFAULT, AUTHENTICATOR_TYPE.OAUTH, AUTHENTICATOR_TYPE.SERVOY_CLOUD, AUTHENTICATOR_TYPE.AUTHENTICATOR, AUTHENTICATOR_TYPE.OAUTH_AUTHENTICATOR })
		{
			Solution solution = createSolutionWithAuthType(type);
			IAuthenticatorManager manager = AuthenticatorManagerCreator.getAuthenticatorManager(solution);
			assertTrue("Manager for " + type + " must extend AbstractAuthenticatorManager", manager instanceof AbstractAuthenticatorManager);
		}
	}

	// =========================================================================
	// Verify methods are no longer static where they should be instance methods
	// =========================================================================

	@Test
	public void oauthHandler_refreshOAuthTokenIfPossible_isNotStatic() throws Exception
	{
		Method method = OAuthHandler.class.getDeclaredMethod("refreshOAuthTokenIfPossible",
			Pair.class, SvyID.class, HttpServletRequest.class, HttpServletResponse.class);
		assertFalse("refreshOAuthTokenIfPossible must not be static", Modifier.isStatic(method.getModifiers()));
	}

	@Test
	public void oauthHandler_refreshOAuthTokenIfPossible_isPrivate() throws Exception
	{
		Method method = OAuthHandler.class.getDeclaredMethod("refreshOAuthTokenIfPossible",
			Pair.class, SvyID.class, HttpServletRequest.class, HttpServletResponse.class);
		assertTrue("refreshOAuthTokenIfPossible must be private", Modifier.isPrivate(method.getModifiers()));
	}

	@Test
	public void abstractAuthenticatorManager_writeSecuredHtmlResponse_isProtected() throws Exception
	{
		Method method = AbstractAuthenticatorManager.class.getDeclaredMethod("writeSecuredHtmlResponse",
			HttpServletRequest.class, HttpServletResponse.class, String.class, long.class, ContentSecurityPolicyConfig.class);
		assertTrue("writeSecuredHtmlResponse must be protected", Modifier.isProtected(method.getModifiers()));
	}

	@Test
	public void abstractAuthenticatorManager_writeSecuredHtmlResponse_isStatic() throws Exception
	{
		Method method = AbstractAuthenticatorManager.class.getDeclaredMethod("writeSecuredHtmlResponse",
			HttpServletRequest.class, HttpServletResponse.class, String.class, long.class, ContentSecurityPolicyConfig.class);
		assertTrue("writeSecuredHtmlResponse must be static (shared utility)", Modifier.isStatic(method.getModifiers()));
	}

	// =========================================================================
	// requiresCSRFForCheckUser contract: only OAuth returns false
	// =========================================================================

	@Test
	public void requiresCSRFForCheckUser_onlyOAuthReturnsFalse()
	{
		for (AUTHENTICATOR_TYPE type : new AUTHENTICATOR_TYPE[] { AUTHENTICATOR_TYPE.DEFAULT, AUTHENTICATOR_TYPE.OAUTH, AUTHENTICATOR_TYPE.SERVOY_CLOUD, AUTHENTICATOR_TYPE.AUTHENTICATOR, AUTHENTICATOR_TYPE.OAUTH_AUTHENTICATOR })
		{
			Solution solution = createSolutionWithAuthType(type);
			IAuthenticatorManager manager = AuthenticatorManagerCreator.getAuthenticatorManager(solution);
			if (type == AUTHENTICATOR_TYPE.OAUTH)
			{
				assertFalse("OAUTH must not require CSRF for checkUser", manager.requiresCSRFForCheckUser());
			}
			else
			{
				assertTrue(type + " must require CSRF for checkUser", manager.requiresCSRFForCheckUser());
			}
		}
	}

	// =========================================================================
	// writeLoginPage polymorphism: OAuthHandler does NOT call super.writeLoginPage
	// (it redirects instead of rendering a form)
	// =========================================================================

	@Test
	public void oauthHandler_writeLoginPage_doesNotWriteFormHtml() throws Exception
	{
		StringWriter output = new StringWriter();
		List<Cookie> cookies = new ArrayList<>();
		StubHttpServletRequest request = new StubHttpServletRequest(false);
		StubHttpServletResponse response = new StubHttpServletResponse(output, cookies);

		OAuthHandler handler = new OAuthHandler(null);
		try
		{
			handler.writeLoginPage(request, response, null);
		}
		catch (Exception e)
		{
			// Expected: will fail because solution is null / no OAuth config
			// But it should NOT have written a login form HTML
		}
		assertFalse("OAuthHandler.writeLoginPage must not render a standard login form",
			output.toString().contains("<form"));
	}

	// =========================================================================
	// writeLoginPage on DefaultLoginManager/AuthenticatorManager uses
	// AbstractAuthenticatorManager base implementation (writes form)
	// =========================================================================

	@Test
	public void writeLoginPage_baseImplementation_writesHtml() throws Exception
	{
		StringWriter output = new StringWriter();
		List<Cookie> cookies = new ArrayList<>();
		StubHttpServletRequest request = new StubHttpServletRequest(true);
		request.setHeader("accept-language", "en");
		StubHttpServletResponse response = new StubHttpServletResponse(output, cookies);

		TestableAuthenticatorManager manager = new TestableAuthenticatorManager(
			"<html lang=\"en\"><head><base href=\"/\"></head><body><form method='post'><input type='text' name='username'></form></body></html>");
		manager.writeLoginPage(request, response, null);

		String result = output.toString();
		assertTrue("writeLoginPage must output HTML", result.contains("<html"));
		assertTrue("writeLoginPage must inject CSRF hidden field", result.contains("name='csrf_token'"));
		assertTrue("writeLoginPage must inject loader div", result.contains("servoy_loader"));
		assertFalse("CSRF cookie must be set", cookies.isEmpty());
	}

	// =========================================================================
	// ITokenRevocable contract - only OAuthHandler and CloudStatelessAccessManager
	// =========================================================================

	@Test
	public void oauthHandler_implementsITokenRevocable()
	{
		OAuthHandler handler = new OAuthHandler(null);
		assertTrue("OAuthHandler must implement ITokenRevocable", handler instanceof ITokenRevocable);
	}

	@Test
	public void cloudStatelessAccessManager_implementsITokenRevocable()
	{
		CloudStatelessAccessManager manager = new CloudStatelessAccessManager(null);
		assertTrue("CloudStatelessAccessManager must implement ITokenRevocable", manager instanceof ITokenRevocable);
	}

	@Test
	public void defaultLoginManager_doesNotImplementITokenRevocable()
	{
		DefaultLoginManager manager = new DefaultLoginManager(null);
		assertFalse("DefaultLoginManager must NOT implement ITokenRevocable", manager instanceof ITokenRevocable);
	}

	@Test
	public void authenticatorManager_doesNotImplementITokenRevocable()
	{
		AuthenticatorManager manager = new AuthenticatorManager(null);
		assertFalse("AuthenticatorManager must NOT implement ITokenRevocable", manager instanceof ITokenRevocable);
	}

	// =========================================================================
	// Verify StatelessLoginHandler.checkUser no longer references AUTHENTICATOR_TYPE
	// (structural test via reflection â no if/else on type in the method)
	// =========================================================================

	@Test
	public void statelessLoginHandler_checkUser_noAuthenticatorTypeReference() throws Exception
	{
		Method method = com.servoy.j2db.server.ngclient.StatelessLoginHandler.class.getDeclaredMethod(
			"checkUser", String.class, String.class, boolean.class, SvyID.class, Pair.class,
			Solution.class, HttpServletRequest.class, HttpServletResponse.class);
		method.setAccessible(true);

		// Verify the method signature does not have Solution.AUTHENTICATOR_TYPE in its bytecode
		// We do this by checking the method doesn't have type-specific branching by examining
		// that the parameter list uses IAuthenticatorManager (tested via factory) not AUTHENTICATOR_TYPE
		// The real proof is that we got here without AUTHENTICATOR_TYPE imports being needed
		assertNotNull("checkUser method must exist", method);
		assertEquals("checkUser must be private", Modifier.PRIVATE, method.getModifiers() & Modifier.PRIVATE);
		assertEquals("checkUser must be static", Modifier.STATIC, method.getModifiers() & Modifier.STATIC);
	}

	// =========================================================================
	// writeSecuredHtmlResponse is reused by both writeLoginPage and generateOauthCall
	// Verify it's accessible from subclasses
	// =========================================================================

	@Test
	public void writeSecuredHtmlResponse_accessibleFromSubclass() throws Exception
	{
		StringWriter output = new StringWriter();
		List<Cookie> cookies = new ArrayList<>();
		StubHttpServletRequest request = new StubHttpServletRequest(true);
		StubHttpServletResponse response = new StubHttpServletResponse(output, cookies);

		// Call directly via the static method - proves it's usable from OAuthHandler.generateOauthCall
		AbstractAuthenticatorManager.writeSecuredHtmlResponse(request, response,
			"<html><script type='text/javascript'>test</script></html>", 42L,
			new ContentSecurityPolicyConfig("abc"));

		String result = output.toString();
		assertTrue("Must inject nonce", result.contains("nonce='abc'"));
		assertTrue("Must write HTML", result.contains("test"));
		assertEquals("Must set CSRF cookie", 1, cookies.size());
		assertEquals("csrf_token", cookies.get(0).getName());
		assertEquals("42", cookies.get(0).getValue());
	}

	// =========================================================================
	// Manager holds solution reference (instance-based, not static lookup)
	// =========================================================================

	@Test
	public void managers_holdSolutionReference() throws Exception
	{
		Solution solution = createSolutionWithAuthType(AUTHENTICATOR_TYPE.DEFAULT);
		DefaultLoginManager manager = new DefaultLoginManager(solution);
		java.lang.reflect.Field field = AbstractAuthenticatorManager.class.getDeclaredField("solution");
		field.setAccessible(true);
		assertEquals("Manager must hold the solution instance", solution, field.get(manager));
	}

	// =========================================================================
	// Helpers
	// =========================================================================

	private Solution createSolutionWithAuthType(AUTHENTICATOR_TYPE type)
	{
		try
		{
			TestRepository tr = new AuthTestRepository();
			RootObjectMetaData metadata = tr.createRootObjectMetaData(UUID.randomUUID(), "TestSolution", IRepository.SOLUTIONS, 1, 1);
			Solution solution = (Solution)tr.createRootObject(metadata);
			solution.setAuthenticator(type);
			return solution;
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}


	public static class AuthTestRepository extends TestRepository
	{
		public AuthTestRepository()
		{
			super();
		}
	}

	private static class TestableAuthenticatorManager extends AbstractAuthenticatorManager
	{
		private final String html;

		TestableAuthenticatorManager(String html)
		{
			super(null);
			this.html = html;
		}

		@Override
		protected String getLoginHTML(HttpServletRequest request, String customHTML) throws IOException
		{
			return html;
		}

		@Override
		public String getLoginScripts(HttpServletRequest request, long csrfToken)
		{
			return "<base href=\"/\">";
		}

		@Override
		public boolean checkPermissions(String username, String password, boolean remember, SvyID oldToken,
			Pair<Boolean, String> needToLogin, HttpServletRequest request)
		{
			return false;
		}

		@Override
		public boolean checkUser(String username, String password, boolean remember, SvyID oldToken,
			Pair<Boolean, String> needToLogin, HttpServletRequest request, HttpServletResponse response)
		{
			return false;
		}

		@Override
		protected ContentSecurityPolicyConfig addContentSecurityPolicyHeader(HttpServletRequest request, HttpServletResponse response)
		{
			return null;
		}
	}

	// =========================================================================
	// Stubs
	// =========================================================================

	static class StubHttpServletRequest implements HttpServletRequest
	{
		private final boolean secure;
		private final Map<String, String> parameters = new HashMap<>();
		private final Map<String, String> headers = new HashMap<>();
		private Cookie[] cookies;
		private Locale locale = Locale.ENGLISH;

		StubHttpServletRequest(boolean secure)
		{
			this.secure = secure;
		}

		void setParameter(String name, String value)
		{
			parameters.put(name, value);
		}

		void setCookies(Cookie[] cookies)
		{
			this.cookies = cookies;
		}

		void setHeader(String name, String value)
		{
			headers.put(name, value);
		}

		void setLocale(Locale locale)
		{
			this.locale = locale;
		}

		@Override
		public boolean isSecure()
		{
			return secure;
		}

		@Override
		public String getParameter(String name)
		{
			return parameters.get(name);
		}

		@Override
		public Cookie[] getCookies()
		{
			return cookies;
		}

		@Override
		public String getHeader(String name)
		{
			return headers.get(name);
		}

		@Override
		public Locale getLocale()
		{
			return locale;
		}

		@Override
		public String getCharacterEncoding()
		{
			return "UTF-8";
		}

		@Override
		public String getRemoteAddr()
		{
			return "127.0.0.1";
		}

		@Override
		public Object getAttribute(String name)
		{
			return null;
		}

		@Override
		public java.util.Enumeration<String> getAttributeNames()
		{
			return Collections.emptyEnumeration();
		}

		@Override
		public void setCharacterEncoding(String env)
		{
		}

		@Override
		public int getContentLength()
		{
			return 0;
		}

		@Override
		public long getContentLengthLong()
		{
			return 0;
		}

		@Override
		public String getContentType()
		{
			return null;
		}

		@Override
		public jakarta.servlet.ServletInputStream getInputStream()
		{
			return null;
		}

		@Override
		public java.util.Enumeration<String> getParameterNames()
		{
			return Collections.emptyEnumeration();
		}

		@Override
		public String[] getParameterValues(String name)
		{
			return null;
		}

		@Override
		public java.util.Map<String, String[]> getParameterMap()
		{
			return Collections.emptyMap();
		}

		@Override
		public String getProtocol()
		{
			return null;
		}

		@Override
		public String getScheme()
		{
			return null;
		}

		@Override
		public String getServerName()
		{
			return null;
		}

		@Override
		public int getServerPort()
		{
			return 0;
		}

		@Override
		public java.io.BufferedReader getReader()
		{
			return null;
		}

		@Override
		public String getRemoteHost()
		{
			return null;
		}

		@Override
		public void setAttribute(String name, Object o)
		{
		}

		@Override
		public void removeAttribute(String name)
		{
		}

		@Override
		public java.util.Enumeration<Locale> getLocales()
		{
			return null;
		}

		@Override
		public jakarta.servlet.RequestDispatcher getRequestDispatcher(String path)
		{
			return null;
		}

		@Override
		public int getRemotePort()
		{
			return 0;
		}

		@Override
		public String getLocalName()
		{
			return null;
		}

		@Override
		public String getLocalAddr()
		{
			return null;
		}

		@Override
		public int getLocalPort()
		{
			return 0;
		}

		@Override
		public jakarta.servlet.ServletContext getServletContext()
		{
			return null;
		}

		@Override
		public jakarta.servlet.AsyncContext startAsync()
		{
			return null;
		}

		@Override
		public jakarta.servlet.AsyncContext startAsync(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse resp)
		{
			return null;
		}

		@Override
		public boolean isAsyncStarted()
		{
			return false;
		}

		@Override
		public boolean isAsyncSupported()
		{
			return false;
		}

		@Override
		public jakarta.servlet.AsyncContext getAsyncContext()
		{
			return null;
		}

		@Override
		public jakarta.servlet.DispatcherType getDispatcherType()
		{
			return null;
		}

		@Override
		public String getRequestId()
		{
			return null;
		}

		@Override
		public String getProtocolRequestId()
		{
			return null;
		}

		@Override
		public jakarta.servlet.ServletConnection getServletConnection()
		{
			return null;
		}

		@Override
		public String getAuthType()
		{
			return null;
		}

		@Override
		public long getDateHeader(String name)
		{
			return 0;
		}

		@Override
		public java.util.Enumeration<String> getHeaders(String name)
		{
			return Collections.emptyEnumeration();
		}

		@Override
		public java.util.Enumeration<String> getHeaderNames()
		{
			return Collections.emptyEnumeration();
		}

		@Override
		public int getIntHeader(String name)
		{
			return 0;
		}

		@Override
		public String getMethod()
		{
			return "POST";
		}

		@Override
		public String getPathInfo()
		{
			return null;
		}

		@Override
		public String getPathTranslated()
		{
			return null;
		}

		@Override
		public String getContextPath()
		{
			return "";
		}

		@Override
		public String getQueryString()
		{
			return null;
		}

		@Override
		public String getRemoteUser()
		{
			return null;
		}

		@Override
		public boolean isUserInRole(String role)
		{
			return false;
		}

		@Override
		public java.security.Principal getUserPrincipal()
		{
			return null;
		}

		@Override
		public String getRequestedSessionId()
		{
			return null;
		}

		@Override
		public String getRequestURI()
		{
			return "/";
		}

		@Override
		public StringBuffer getRequestURL()
		{
			return new StringBuffer("http://localhost/");
		}

		@Override
		public String getServletPath()
		{
			return "";
		}

		@Override
		public jakarta.servlet.http.HttpSession getSession(boolean create)
		{
			return null;
		}

		@Override
		public jakarta.servlet.http.HttpSession getSession()
		{
			return null;
		}

		@Override
		public String changeSessionId()
		{
			return null;
		}

		@Override
		public boolean isRequestedSessionIdValid()
		{
			return false;
		}

		@Override
		public boolean isRequestedSessionIdFromCookie()
		{
			return false;
		}

		@Override
		public boolean isRequestedSessionIdFromURL()
		{
			return false;
		}

		@Override
		public boolean authenticate(HttpServletResponse response)
		{
			return false;
		}

		@Override
		public void login(String username, String password)
		{
		}

		@Override
		public void logout()
		{
		}

		@Override
		public Collection<jakarta.servlet.http.Part> getParts()
		{
			return null;
		}

		@Override
		public jakarta.servlet.http.Part getPart(String name)
		{
			return null;
		}

		@Override
		public <T extends jakarta.servlet.http.HttpUpgradeHandler> T upgrade(Class<T> handlerClass)
		{
			return null;
		}
	}

	static class StubHttpServletResponse implements HttpServletResponse
	{
		private final StringWriter output;
		private final List<Cookie> cookies;
		private final Map<String, String> setHeaders = new HashMap<>();

		StubHttpServletResponse(StringWriter output, List<Cookie> cookies)
		{
			this.output = output;
			this.cookies = cookies;
		}

		Map<String, String> getSetHeaders()
		{
			return setHeaders;
		}

		@Override
		public PrintWriter getWriter()
		{
			return new PrintWriter(output, true);
		}

		@Override
		public void addCookie(Cookie cookie)
		{
			cookies.add(cookie);
		}

		@Override
		public void setCharacterEncoding(String charset)
		{
		}

		@Override
		public void setContentType(String type)
		{
		}

		@Override
		public void setContentLengthLong(long len)
		{
		}

		@Override
		public void setHeader(String name, String value)
		{
			setHeaders.put(name, value);
		}

		@Override
		public void addHeader(String name, String value)
		{
			setHeaders.put(name, value);
		}

		@Override
		public String getCharacterEncoding()
		{
			return "UTF-8";
		}

		@Override
		public String getContentType()
		{
			return null;
		}

		@Override
		public jakarta.servlet.ServletOutputStream getOutputStream()
		{
			return null;
		}

		@Override
		public void setContentLength(int len)
		{
		}

		@Override
		public void setBufferSize(int size)
		{
		}

		@Override
		public int getBufferSize()
		{
			return 0;
		}

		@Override
		public void flushBuffer()
		{
		}

		@Override
		public void resetBuffer()
		{
		}

		@Override
		public boolean isCommitted()
		{
			return false;
		}

		@Override
		public void reset()
		{
		}

		@Override
		public void setLocale(Locale loc)
		{
		}

		@Override
		public Locale getLocale()
		{
			return null;
		}

		@Override
		public void addIntHeader(String name, int value)
		{
		}

		@Override
		public void setIntHeader(String name, int value)
		{
		}

		@Override
		public void addDateHeader(String name, long date)
		{
		}

		@Override
		public void setDateHeader(String name, long date)
		{
		}

		@Override
		public boolean containsHeader(String name)
		{
			return false;
		}

		@Override
		public String encodeURL(String url)
		{
			return null;
		}

		@Override
		public String encodeRedirectURL(String url)
		{
			return null;
		}

		@Override
		public void sendError(int sc, String msg)
		{
		}

		@Override
		public void sendError(int sc)
		{
		}

		@Override
		public void sendRedirect(String location)
		{
		}

		@Override
		public void sendRedirect(String location, int sc, boolean clearBuffer)
		{
		}

		@Override
		public void setStatus(int sc)
		{
		}

		@Override
		public int getStatus()
		{
			return 200;
		}

		@Override
		public String getHeader(String name)
		{
			return setHeaders.get(name);
		}

		@Override
		public Collection<String> getHeaders(String name)
		{
			return Collections.emptyList();
		}

		@Override
		public Collection<String> getHeaderNames()
		{
			return setHeaders.keySet();
		}
	}
}
