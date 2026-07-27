package com.servoy.j2db.server.ngclient.auth;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.junit.Test;
import org.sablo.security.ContentSecurityPolicyConfig;

import com.servoy.j2db.server.ngclient.property.Log4JToConsoleTest;
import com.servoy.j2db.util.Pair;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@SuppressWarnings("nls")
public class AuthenticatorManagerCSRFTest extends Log4JToConsoleTest
{
	@Test
	public void defaultLoginManager_requiresCSRFForCheckUser()
	{
		DefaultLoginManager manager = new DefaultLoginManager(null);
		assertTrue(manager.requiresCSRFForCheckUser());
	}

	@Test
	public void authenticatorManager_requiresCSRFForCheckUser()
	{
		AuthenticatorManager manager = new AuthenticatorManager(null);
		assertTrue(manager.requiresCSRFForCheckUser());
	}

	@Test
	public void cloudStatelessAccessManager_requiresCSRFForCheckUser()
	{
		CloudStatelessAccessManager manager = new CloudStatelessAccessManager(null);
		assertTrue(manager.requiresCSRFForCheckUser());
	}

	@Test
	public void oauthHandler_doesNotRequireCSRFForCheckUser()
	{
		OAuthHandler handler = new OAuthHandler(null);
		assertFalse(handler.requiresCSRFForCheckUser());
	}

	@Test
	public void interfaceDefault_requiresCSRFForCheckUser()
	{
		IAuthenticatorManager manager = new IAuthenticatorManager()
		{
			@Override
			public void writeLoginPage(HttpServletRequest request, HttpServletResponse response, String customHTML)
				throws ServletException, UnsupportedEncodingException, IOException
			{
			}

			@Override
			public String getLoginScripts(HttpServletRequest request, long csrfToken)
			{
				return null;
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
		};
		assertTrue("Interface default should require CSRF", manager.requiresCSRFForCheckUser());
	}

	@Test
	public void writeSecuredHtmlResponse_appliesNonceAndCsrfCookie() throws IOException
	{
		StringWriter output = new StringWriter();
		List<Cookie> cookies = new ArrayList<>();
		StubHttpServletRequest request = new StubHttpServletRequest(true);
		StubHttpServletResponse response = new StubHttpServletResponse(output, cookies);

		String html = "<html><head><script type='text/javascript'>alert('test')</script></head><body></body></html>";
		long csrfToken = 12345L;
		ContentSecurityPolicyConfig cspConfig = new ContentSecurityPolicyConfig("test-nonce-123");

		AbstractAuthenticatorManager.writeSecuredHtmlResponse(request, response, html, csrfToken, cspConfig);

		String result = output.toString();
		assertTrue("Nonce must be injected into script tags", result.contains("nonce='test-nonce-123'"));
		assertTrue("Response must contain the HTML", result.contains("alert('test')"));

		boolean hasCsrfCookie = cookies.stream().anyMatch(c -> "csrf_token".equals(c.getName()) && "12345".equals(c.getValue()));
		assertTrue("CSRF cookie must be set", hasCsrfCookie);
	}

	@Test
	public void writeSecuredHtmlResponse_noCspConfig_noNonceInjected() throws IOException
	{
		StringWriter output = new StringWriter();
		List<Cookie> cookies = new ArrayList<>();
		StubHttpServletRequest request = new StubHttpServletRequest(false);
		StubHttpServletResponse response = new StubHttpServletResponse(output, cookies);

		String html = "<html><head><script type='text/javascript'>alert('test')</script></head><body></body></html>";
		long csrfToken = 67890L;

		AbstractAuthenticatorManager.writeSecuredHtmlResponse(request, response, html, csrfToken, null);

		String result = output.toString();
		assertFalse("No nonce should be injected when CSP config is null", result.contains("nonce="));
		assertTrue("Response must contain the original HTML", result.contains("alert('test')"));

		boolean hasCsrfCookie = cookies.stream().anyMatch(c -> "csrf_token".equals(c.getName()) && "67890".equals(c.getValue()));
		assertTrue("CSRF cookie must still be set", hasCsrfCookie);
	}

	@Test
	public void writeSecuredHtmlResponse_csrfCookie_isHttpOnlyAndSecure() throws IOException
	{
		StringWriter output = new StringWriter();
		List<Cookie> cookies = new ArrayList<>();
		StubHttpServletRequest request = new StubHttpServletRequest(true);
		StubHttpServletResponse response = new StubHttpServletResponse(output, cookies);

		String html = "<html><body></body></html>";
		AbstractAuthenticatorManager.writeSecuredHtmlResponse(request, response, html, 99999L, null);

		Cookie csrfCookie = cookies.stream().filter(c -> "csrf_token".equals(c.getName())).findFirst().orElse(null);
		assertTrue("CSRF cookie must be HttpOnly", csrfCookie.isHttpOnly());
		assertTrue("CSRF cookie must be Secure when request is secure", csrfCookie.getSecure());
	}

	private static class StubHttpServletRequest implements HttpServletRequest
	{
		private final boolean secure;

		StubHttpServletRequest(boolean secure)
		{
			this.secure = secure;
		}

		@Override
		public boolean isSecure()
		{
			return secure;
		}

		// Minimal stubs for compilation â unused methods return defaults
		@Override public Object getAttribute(String name) { return null; }
		@Override public java.util.Enumeration<String> getAttributeNames() { return null; }
		@Override public String getCharacterEncoding() { return null; }
		@Override public void setCharacterEncoding(String env) {}
		@Override public int getContentLength() { return 0; }
		@Override public long getContentLengthLong() { return 0; }
		@Override public String getContentType() { return null; }
		@Override public jakarta.servlet.ServletInputStream getInputStream() { return null; }
		@Override public String getParameter(String name) { return null; }
		@Override public java.util.Enumeration<String> getParameterNames() { return null; }
		@Override public String[] getParameterValues(String name) { return null; }
		@Override public java.util.Map<String, String[]> getParameterMap() { return null; }
		@Override public String getProtocol() { return null; }
		@Override public String getScheme() { return null; }
		@Override public String getServerName() { return null; }
		@Override public int getServerPort() { return 0; }
		@Override public java.io.BufferedReader getReader() { return null; }
		@Override public String getRemoteAddr() { return null; }
		@Override public String getRemoteHost() { return null; }
		@Override public void setAttribute(String name, Object o) {}
		@Override public void removeAttribute(String name) {}
		@Override public Locale getLocale() { return Locale.ENGLISH; }
		@Override public java.util.Enumeration<Locale> getLocales() { return null; }
		@Override public jakarta.servlet.RequestDispatcher getRequestDispatcher(String path) { return null; }
		@Override public int getRemotePort() { return 0; }
		@Override public String getLocalName() { return null; }
		@Override public String getLocalAddr() { return null; }
		@Override public int getLocalPort() { return 0; }
		@Override public jakarta.servlet.ServletContext getServletContext() { return null; }
		@Override public jakarta.servlet.AsyncContext startAsync() { return null; }
		@Override public jakarta.servlet.AsyncContext startAsync(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse resp) { return null; }
		@Override public boolean isAsyncStarted() { return false; }
		@Override public boolean isAsyncSupported() { return false; }
		@Override public jakarta.servlet.AsyncContext getAsyncContext() { return null; }
		@Override public jakarta.servlet.DispatcherType getDispatcherType() { return null; }
		@Override public String getRequestId() { return null; }
		@Override public String getProtocolRequestId() { return null; }
		@Override public jakarta.servlet.ServletConnection getServletConnection() { return null; }
		@Override public String getAuthType() { return null; }
		@Override public Cookie[] getCookies() { return null; }
		@Override public long getDateHeader(String name) { return 0; }
		@Override public String getHeader(String name) { return null; }
		@Override public java.util.Enumeration<String> getHeaders(String name) { return null; }
		@Override public java.util.Enumeration<String> getHeaderNames() { return null; }
		@Override public int getIntHeader(String name) { return 0; }
		@Override public String getMethod() { return null; }
		@Override public String getPathInfo() { return null; }
		@Override public String getPathTranslated() { return null; }
		@Override public String getContextPath() { return null; }
		@Override public String getQueryString() { return null; }
		@Override public String getRemoteUser() { return null; }
		@Override public boolean isUserInRole(String role) { return false; }
		@Override public java.security.Principal getUserPrincipal() { return null; }
		@Override public String getRequestedSessionId() { return null; }
		@Override public String getRequestURI() { return null; }
		@Override public StringBuffer getRequestURL() { return null; }
		@Override public String getServletPath() { return null; }
		@Override public jakarta.servlet.http.HttpSession getSession(boolean create) { return null; }
		@Override public jakarta.servlet.http.HttpSession getSession() { return null; }
		@Override public String changeSessionId() { return null; }
		@Override public boolean isRequestedSessionIdValid() { return false; }
		@Override public boolean isRequestedSessionIdFromCookie() { return false; }
		@Override public boolean isRequestedSessionIdFromURL() { return false; }
		@Override public boolean authenticate(HttpServletResponse response) { return false; }
		@Override public void login(String username, String password) {}
		@Override public void logout() {}
		@Override public Collection<jakarta.servlet.http.Part> getParts() { return null; }
		@Override public jakarta.servlet.http.Part getPart(String name) { return null; }
		@Override public <T extends jakarta.servlet.http.HttpUpgradeHandler> T upgrade(Class<T> handlerClass) { return null; }
	}

	private static class StubHttpServletResponse implements HttpServletResponse
	{
		private final StringWriter output;
		private final List<Cookie> cookies;

		StubHttpServletResponse(StringWriter output, List<Cookie> cookies)
		{
			this.output = output;
			this.cookies = cookies;
		}

		@Override
		public PrintWriter getWriter()
		{
			return new PrintWriter(output);
		}

		@Override
		public void addCookie(Cookie cookie)
		{
			cookies.add(cookie);
		}

		@Override public void setCharacterEncoding(String charset) {}
		@Override public void setContentType(String type) {}
		@Override public void setContentLengthLong(long len) {}

		// Minimal stubs for compilation
		@Override public String getCharacterEncoding() { return null; }
		@Override public String getContentType() { return null; }
		@Override public jakarta.servlet.ServletOutputStream getOutputStream() { return null; }
		@Override public void setContentLength(int len) {}
		@Override public void setBufferSize(int size) {}
		@Override public int getBufferSize() { return 0; }
		@Override public void flushBuffer() {}
		@Override public void resetBuffer() {}
		@Override public boolean isCommitted() { return false; }
		@Override public void reset() {}
		@Override public void setLocale(Locale loc) {}
		@Override public Locale getLocale() { return null; }
		@Override public void addHeader(String name, String value) {}
		@Override public void setHeader(String name, String value) {}
		@Override public void addIntHeader(String name, int value) {}
		@Override public void setIntHeader(String name, int value) {}
		@Override public void addDateHeader(String name, long date) {}
		@Override public void setDateHeader(String name, long date) {}
		@Override public boolean containsHeader(String name) { return false; }
		@Override public String encodeURL(String url) { return null; }
		@Override public String encodeRedirectURL(String url) { return null; }
		@Override public void sendError(int sc, String msg) {}
		@Override public void sendError(int sc) {}
		@Override public void sendRedirect(String location) {}
		@Override public void setStatus(int sc) {}
		@Override public int getStatus() { return 0; }
		@Override public String getHeader(String name) { return null; }
		@Override public Collection<String> getHeaders(String name) { return null; }
		@Override public Collection<String> getHeaderNames() { return null; }

		/* (non-Javadoc)
		 * @see jakarta.servlet.http.HttpServletResponse#sendRedirect(java.lang.String, int, boolean)
		 */
		@Override
		public void sendRedirect(String location, int sc, boolean clearBuffer) throws IOException
		{
		}
	}
}
