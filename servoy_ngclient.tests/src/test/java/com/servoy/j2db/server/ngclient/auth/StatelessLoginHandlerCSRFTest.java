package com.servoy.j2db.server.ngclient.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;
import org.sablo.security.ContentSecurityPolicyConfig;

import com.servoy.j2db.server.ngclient.StatelessLoginHandler;
import com.servoy.j2db.server.ngclient.property.Log4JToConsoleTest;
import com.servoy.j2db.util.Pair;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@SuppressWarnings("nls")
public class StatelessLoginHandlerCSRFTest extends Log4JToConsoleTest
{
	// =========================================================================
	// checkCSRFToken tests (via reflection on private static method)
	// =========================================================================

	@Test
	public void checkCSRFToken_matchingTokens_returnsTrue() throws Exception
	{
		Cookie csrfCookie = new Cookie("csrf_token", "abc123");
		StubHttpServletRequest request = new StubHttpServletRequest(false);
		request.setParameter("csrf_token", "abc123");
		request.setCookies(new Cookie[] { csrfCookie });

		assertTrue(invokeCheckCSRFToken(request));
	}

	@Test
	public void checkCSRFToken_mismatchedTokens_returnsFalse() throws Exception
	{
		Cookie csrfCookie = new Cookie("csrf_token", "abc123");
		StubHttpServletRequest request = new StubHttpServletRequest(false);
		request.setParameter("csrf_token", "different_value");
		request.setCookies(new Cookie[] { csrfCookie });

		assertFalse(invokeCheckCSRFToken(request));
	}

	@Test
	public void checkCSRFToken_noCookies_returnsFalse() throws Exception
	{
		StubHttpServletRequest request = new StubHttpServletRequest(false);
		request.setParameter("csrf_token", "abc123");
		request.setCookies(null);

		assertFalse(invokeCheckCSRFToken(request));
	}

	@Test
	public void checkCSRFToken_noFieldToken_returnsFalse() throws Exception
	{
		Cookie csrfCookie = new Cookie("csrf_token", "abc123");
		StubHttpServletRequest request = new StubHttpServletRequest(false);
		request.setCookies(new Cookie[] { csrfCookie });

		assertFalse(invokeCheckCSRFToken(request));
	}

	@Test
	public void checkCSRFToken_noCsrfCookieAmongOthers_returnsFalse() throws Exception
	{
		Cookie otherCookie = new Cookie("session_id", "xyz");
		StubHttpServletRequest request = new StubHttpServletRequest(false);
		request.setParameter("csrf_token", "abc123");
		request.setCookies(new Cookie[] { otherCookie });

		assertFalse(invokeCheckCSRFToken(request));
	}

	@Test
	public void checkCSRFToken_multipleCookies_findsCorrectOne() throws Exception
	{
		Cookie otherCookie = new Cookie("session_id", "xyz");
		Cookie csrfCookie = new Cookie("csrf_token", "correct_token");
		StubHttpServletRequest request = new StubHttpServletRequest(false);
		request.setParameter("csrf_token", "correct_token");
		request.setCookies(new Cookie[] { otherCookie, csrfCookie });

		assertTrue(invokeCheckCSRFToken(request));
	}

	// =========================================================================
	// writeSecuredHtmlResponse additional edge cases
	// =========================================================================

	@Test
	public void writeSecuredHtmlResponse_injectsNonceIntoStyleTags() throws IOException
	{
		StringWriter output = new StringWriter();
		List<Cookie> cookies = new ArrayList<>();
		StubHttpServletRequest request = new StubHttpServletRequest(true);
		StubHttpServletResponse response = new StubHttpServletResponse(output, cookies);

		String html = "<html><head><style>body { color: red; }</style></head></html>";
		ContentSecurityPolicyConfig cspConfig = new ContentSecurityPolicyConfig("nonce-xyz");

		AbstractAuthenticatorManager.writeSecuredHtmlResponse(request, response, html, 111L, cspConfig);

		String result = output.toString();
		assertTrue("Nonce must be injected into style tags", result.contains("<style nonce='nonce-xyz'"));
	}

	@Test
	public void writeSecuredHtmlResponse_csrfCookie_pathIsRoot() throws IOException
	{
		StringWriter output = new StringWriter();
		List<Cookie> cookies = new ArrayList<>();
		StubHttpServletRequest request = new StubHttpServletRequest(false);
		StubHttpServletResponse response = new StubHttpServletResponse(output, cookies);

		AbstractAuthenticatorManager.writeSecuredHtmlResponse(request, response, "<html></html>", 555L, null);

		Cookie csrfCookie = cookies.stream().filter(c -> "csrf_token".equals(c.getName())).findFirst().orElse(null);
		assertEquals("CSRF cookie path must be /", "/", csrfCookie.getPath());
	}

	@Test
	public void writeSecuredHtmlResponse_nonSecureRequest_cookieNotSecure() throws IOException
	{
		StringWriter output = new StringWriter();
		List<Cookie> cookies = new ArrayList<>();
		StubHttpServletRequest request = new StubHttpServletRequest(false);
		StubHttpServletResponse response = new StubHttpServletResponse(output, cookies);

		AbstractAuthenticatorManager.writeSecuredHtmlResponse(request, response, "<html></html>", 777L, null);

		Cookie csrfCookie = cookies.stream().filter(c -> "csrf_token".equals(c.getName())).findFirst().orElse(null);
		assertFalse("CSRF cookie must NOT be Secure on non-secure requests", csrfCookie.getSecure());
	}

	@Test
	public void writeSecuredHtmlResponse_multipleScriptTags_allGetNonce() throws IOException
	{
		StringWriter output = new StringWriter();
		List<Cookie> cookies = new ArrayList<>();
		StubHttpServletRequest request = new StubHttpServletRequest(true);
		StubHttpServletResponse response = new StubHttpServletResponse(output, cookies);

		String html = "<html><script type='a'>1</script><script type='b'>2</script></html>";
		ContentSecurityPolicyConfig cspConfig = new ContentSecurityPolicyConfig("my-nonce");

		AbstractAuthenticatorManager.writeSecuredHtmlResponse(request, response, html, 123L, cspConfig);

		String result = output.toString();
		int count = result.split("nonce='my-nonce'", -1).length - 1;
		assertEquals("All script tags must get nonce", 2, count);
	}

	@Test
	public void writeSecuredHtmlResponse_csrfTokenValueInCookie() throws IOException
	{
		StringWriter output = new StringWriter();
		List<Cookie> cookies = new ArrayList<>();
		StubHttpServletRequest request = new StubHttpServletRequest(true);
		StubHttpServletResponse response = new StubHttpServletResponse(output, cookies);

		long token = Long.MAX_VALUE;
		AbstractAuthenticatorManager.writeSecuredHtmlResponse(request, response, "<html></html>", token, null);

		Cookie csrfCookie = cookies.stream().filter(c -> "csrf_token".equals(c.getName())).findFirst().orElse(null);
		assertEquals("CSRF cookie value must match token", Long.toString(token), csrfCookie.getValue());
	}

	// =========================================================================
	// writeLoginPage integration tests (using a test subclass)
	// =========================================================================

	@Test
	public void writeLoginPage_injectsCsrfHiddenFieldIntoForms() throws Exception
	{
		StringWriter output = new StringWriter();
		List<Cookie> cookies = new ArrayList<>();
		StubHttpServletRequest request = new StubHttpServletRequest(true);
		request.setHeader("accept-language", "en");
		StubHttpServletResponse response = new StubHttpServletResponse(output, cookies);

		TestAuthenticatorManager manager = new TestAuthenticatorManager(
			"<html><head><base href=\"/\"></head><body><form method='post'></form></body></html>");
		manager.writeLoginPage(request, response, null);

		String result = output.toString();
		assertTrue("CSRF hidden field must be injected before </form>",
			result.contains("<input type='hidden' name='csrf_token'") && result.contains("</form>"));
	}

	@Test
	public void writeLoginPage_setsNoCacheHeaders() throws Exception
	{
		StringWriter output = new StringWriter();
		List<Cookie> cookies = new ArrayList<>();
		StubHttpServletRequest request = new StubHttpServletRequest(true);
		StubHttpServletResponse response = new StubHttpServletResponse(output, cookies);

		TestAuthenticatorManager manager = new TestAuthenticatorManager(
			"<html><head><base href=\"/\"></head><body></body></html>");
		manager.writeLoginPage(request, response, null);

		assertTrue("No-cache headers must be set", response.getSetHeaders().containsKey("Cache-Control"));
	}

	@Test
	public void writeLoginPage_replacesLangAttribute() throws Exception
	{
		StringWriter output = new StringWriter();
		List<Cookie> cookies = new ArrayList<>();
		StubHttpServletRequest request = new StubHttpServletRequest(true);
		request.setHeader("accept-language", "nl");
		request.setLocale(new Locale("nl"));
		StubHttpServletResponse response = new StubHttpServletResponse(output, cookies);

		TestAuthenticatorManager manager = new TestAuthenticatorManager(
			"<html lang=\"en\"><head><base href=\"/\"></head><body></body></html>");
		manager.writeLoginPage(request, response, null);

		String result = output.toString();
		assertTrue("Language must be replaced to match request locale", result.contains("lang=\"nl\""));
		assertFalse("Original lang=en must be gone", result.contains("lang=\"en\""));
	}

	@Test
	public void writeLoginPage_nullHtml_doesNotWrite() throws Exception
	{
		StringWriter output = new StringWriter();
		List<Cookie> cookies = new ArrayList<>();
		StubHttpServletRequest request = new StubHttpServletRequest(true);
		StubHttpServletResponse response = new StubHttpServletResponse(output, cookies);

		TestAuthenticatorManager manager = new TestAuthenticatorManager(null);
		manager.writeLoginPage(request, response, null);

		assertEquals("Nothing should be written when HTML is null", "", output.toString());
		assertTrue("No cookies should be set when HTML is null", cookies.isEmpty());
	}

	@Test
	public void writeLoginPage_csrfCookieAndHiddenFieldHaveSameValue() throws Exception
	{
		StringWriter output = new StringWriter();
		List<Cookie> cookies = new ArrayList<>();
		StubHttpServletRequest request = new StubHttpServletRequest(true);
		StubHttpServletResponse response = new StubHttpServletResponse(output, cookies);

		TestAuthenticatorManager manager = new TestAuthenticatorManager(
			"<html><head><base href=\"/\"></head><body><form></form></body></html>");
		manager.writeLoginPage(request, response, null);

		String result = output.toString();
		Cookie csrfCookie = cookies.stream().filter(c -> "csrf_token".equals(c.getName())).findFirst().orElse(null);
		assertTrue("Hidden field must contain same value as cookie",
			result.contains("value='" + csrfCookie.getValue() + "'"));
	}

	// =========================================================================
	// Helper: invoke private static checkCSRFToken via reflection
	// =========================================================================

	private boolean invokeCheckCSRFToken(HttpServletRequest request) throws Exception
	{
		Method method = StatelessLoginHandler.class.getDeclaredMethod("checkCSRFToken", HttpServletRequest.class);
		method.setAccessible(true);
		return (boolean)method.invoke(null, request);
	}

	// =========================================================================
	// Test subclass of AbstractAuthenticatorManager for writeLoginPage tests
	// =========================================================================

	private static class TestAuthenticatorManager extends AbstractAuthenticatorManager
	{
		private final String html;

		TestAuthenticatorManager(String html)
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
	// Stub implementations
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

		// Minimal stubs
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

		// Minimal stubs
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

		@Override
		public void sendRedirect(String location, int sc, boolean clearBuffer)
		{
		}
	}
}
