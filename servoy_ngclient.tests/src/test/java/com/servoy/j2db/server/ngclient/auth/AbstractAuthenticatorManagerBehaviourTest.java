package com.servoy.j2db.server.ngclient.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
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
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.server.ngclient.StatelessLoginHandler;
import com.servoy.j2db.server.ngclient.property.Log4JToConsoleTest;
import com.servoy.j2db.server.ngclient.property.TestRepository;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.UUID;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Tests for AbstractAuthenticatorManager.getLoginScripts() branches
 * and getLoginHTML() media/classpath fallback.
 */
@SuppressWarnings("nls")
public class AbstractAuthenticatorManagerBehaviourTest extends Log4JToConsoleTest
{
	// =========================================================================
	// getLoginScripts â branch 1: no id_token param, no username param
	// â localStorage check block emitted
	// =========================================================================

	@Test
	public void getLoginScripts_noTokenNoUser_emitsLocalStorageCheck()
	{
		StubRequest request = new StubRequest();
		DefaultLoginManager manager = new DefaultLoginManager(null);
		String scripts = manager.getLoginScripts(request, 12345L);

		assertTrue("Should contain localStorage.getItem('servoy_id_token')",
			scripts.contains("localStorage.getItem('servoy_id_token')"));
		assertFalse("Should NOT contain removeItem", scripts.contains("removeItem"));
		// The localStorage branch reads username from localStorage (not from request param)
		assertFalse("Should NOT show errorlabel (no failed login)", scripts.contains("errorlabel"));
	}

	@Test
	public void getLoginScripts_noTokenNoUser_csrfTokenEmbeddedInScript()
	{
		StubRequest request = new StubRequest();
		DefaultLoginManager manager = new DefaultLoginManager(null);
		String scripts = manager.getLoginScripts(request, 99999L);

		assertTrue("CSRF token value must appear in the script for auto-submit",
			scripts.contains("'99999'"));
	}

	@Test
	public void getLoginScripts_noTokenNoUser_localStorageUsernameCheckPresent()
	{
		StubRequest request = new StubRequest();
		DefaultLoginManager manager = new DefaultLoginManager(null);
		String scripts = manager.getLoginScripts(request, 1L);

		assertTrue("Should also check servoy_username in localStorage",
			scripts.contains("localStorage.getItem('servoy_username')"));
	}

	// =========================================================================
	// getLoginScripts â branch 2: id_token in cookie (getExistingIdToken)
	// â removeItem block emitted
	// =========================================================================

	@Test
	public void getLoginScripts_idTokenInCookie_emitsRemoveItem()
	{
		StubRequest request = new StubRequest();
		// id_token param is null but a cookie provides the token (HTMLWriter.getExistingIdToken reads from request)
		// We need ID_TOKEN param to trigger branch 2: id_token != null after getExistingIdToken
		// getExistingIdToken reads request parameter "id_token"
		request.setParameter(StatelessLoginHandler.ID_TOKEN, "some.jwt.token");

		DefaultLoginManager manager = new DefaultLoginManager(null);
		String scripts = manager.getLoginScripts(request, 1L);

		assertTrue("Should emit removeItem for rejected token",
			scripts.contains("removeItem('servoy_id_token')"));
		assertFalse("Should NOT emit localStorage check", scripts.contains("localStorage.getItem('servoy_id_token')"));
	}

	// =========================================================================
	// getLoginScripts â branch 3: username param present (login failed)
	// â username pre-filled, errorlabel shown
	// =========================================================================

	@Test
	public void getLoginScripts_usernamePresent_prefillsUsernameAndShowsError()
	{
		StubRequest request = new StubRequest();
		request.setParameter(StatelessLoginHandler.USERNAME, "testuser");

		DefaultLoginManager manager = new DefaultLoginManager(null);
		String scripts = manager.getLoginScripts(request, 1L);

		assertTrue("Should pre-fill username", scripts.contains("login_form.username.value = 'testuser'"));
		assertTrue("Should show error label", scripts.contains("errorlabel"));
		assertFalse("Should NOT check localStorage", scripts.contains("localStorage.getItem('servoy_id_token')"));
	}

	@Test
	public void getLoginScripts_usernameWithXss_isEcmaScriptEscaped()
	{
		StubRequest request = new StubRequest();
		request.setParameter(StatelessLoginHandler.USERNAME, "user';<script>alert(1)</script>");

		DefaultLoginManager manager = new DefaultLoginManager(null);
		String scripts = manager.getLoginScripts(request, 1L);

		assertFalse("Raw single quote must be escaped in script", scripts.contains("user';"));
		// escapeEcmaScript turns ' into \'
		assertTrue("Escaped value must appear", scripts.contains("\\'"));
	}

	// =========================================================================
	// getLoginScripts â branch 4: id_token param present but blank, username blank
	// â show() called (fallback)
	// =========================================================================

	@Test
	public void getLoginScripts_blankIdTokenAndNoUsername_emitsShowCall()
	{
		StubRequest request = new StubRequest();
		request.setParameter(StatelessLoginHandler.ID_TOKEN, "   "); // blank, not null

		DefaultLoginManager manager = new DefaultLoginManager(null);
		String scripts = manager.getLoginScripts(request, 1L);

		assertTrue("Fallback branch must call show()", scripts.contains("show()"));
		assertFalse("Should NOT removeItem", scripts.contains("removeItem"));
	}

	// =========================================================================
	// getLoginScripts â always emits base <script> boilerplate
	// =========================================================================

	@Test
	public void getLoginScripts_alwaysContainsScriptTag()
	{
		StubRequest request = new StubRequest();
		DefaultLoginManager manager = new DefaultLoginManager(null);
		String scripts = manager.getLoginScripts(request, 1L);

		assertTrue("Scripts must contain a <script> tag", scripts.contains("<script"));
		assertTrue("Scripts must contain window.addEventListener", scripts.contains("addEventListener('load'"));
	}

	@Test
	public void getLoginScripts_alwaysContainsStyleTag()
	{
		StubRequest request = new StubRequest();
		DefaultLoginManager manager = new DefaultLoginManager(null);
		String scripts = manager.getLoginScripts(request, 1L);

		assertTrue("Scripts must contain a <style> block with loader CSS", scripts.contains("servoy_loader"));
	}

	// =========================================================================
	// getLoginHTML â no solution â classpath fallback
	// =========================================================================

	@Test
	public void getLoginHTML_noSolution_returnsClasspathResource() throws IOException
	{
		DefaultLoginManager manager = new DefaultLoginManager(null);
		String html = manager.getLoginHTML(null, null);

		assertNotNull("getLoginHTML must return content from classpath", html);
		assertFalse("Classpath login.html must not be empty", html.isEmpty());
		assertTrue("Classpath login.html must be HTML", html.contains("<html") || html.contains("<!DOCTYPE"));
	}

	// =========================================================================
	// getLoginHTML â solution has login.html media â uses media content
	// =========================================================================

	@Test
	public void getLoginHTML_solutionHasLoginMedia_returnsMediaContent() throws Exception
	{
		Solution solution = createSolution();
		IValidateName validator = (nameToCheck, skip, ctx, sqlRelated) -> { };
		solution.createNewMedia(validator, "login.html").setPermMediaData("<html><body>Custom Login</body></html>".getBytes("UTF-8"));

		DefaultLoginManager manager = new DefaultLoginManager(solution);
		String html = manager.getLoginHTML(null, null);

		assertTrue("Must return the media content, not the classpath resource",
			html.contains("Custom Login"));
	}

	@Test
	public void getLoginHTML_solutionWithoutLoginMedia_returnsClasspathFallback() throws Exception
	{
		Solution solution = createSolution();
		// no media created

		DefaultLoginManager manager = new DefaultLoginManager(solution);
		String html = manager.getLoginHTML(null, null);

		assertNotNull("Must fall back to classpath login.html", html);
		assertFalse("Must not be empty", html.isEmpty());
		// classpath resource won't contain "Custom Login"
		assertFalse(html.contains("Custom Login"));
	}

	// =========================================================================
	// writeLoginPage â loader div injected into body
	// =========================================================================

	@Test
	public void writeLoginPage_injectsLoaderDiv() throws Exception
	{
		StringWriter output = new StringWriter();
		List<Cookie> cookies = new ArrayList<>();
		StubRequest request = new StubRequest();
		StubResponse response = new StubResponse(output, cookies);

		TestManager manager = new TestManager(
			"<html><head><base href=\"/\"></head><body><form></form></body></html>");
		manager.writeLoginPage(request, response, null);

		assertTrue("Loader div must be injected into body",
			output.toString().contains("servoy_loader"));
	}

	@Test
	public void writeLoginPage_loaderDivAppearsBeforeForm() throws Exception
	{
		StringWriter output = new StringWriter();
		List<Cookie> cookies = new ArrayList<>();
		StubRequest request = new StubRequest();
		StubResponse response = new StubResponse(output, cookies);

		TestManager manager = new TestManager(
			"<html><head><base href=\"/\"></head><body><form></form></body></html>");
		manager.writeLoginPage(request, response, null);

		String result = output.toString();
		int loaderPos = result.indexOf("servoy_loader");
		int formPos = result.indexOf("<form");
		assertTrue("Loader must appear before the form", loaderPos < formPos);
	}

	@Test
	public void writeLoginPage_multipleForms_allGetCsrfField() throws Exception
	{
		StringWriter output = new StringWriter();
		List<Cookie> cookies = new ArrayList<>();
		StubRequest request = new StubRequest();
		StubResponse response = new StubResponse(output, cookies);

		TestManager manager = new TestManager(
			"<html><head><base href=\"/\"></head><body><form id='a'></form><form id='b'></form></body></html>");
		manager.writeLoginPage(request, response, null);

		String result = output.toString();
		// replaceAll("(?i)</form>", ...) replaces every </form> occurrence
		int csrfFieldCount = result.split("name='csrf_token'", -1).length - 1;
		assertEquals("Each </form> must get its own CSRF field â 2 forms â 2 fields", 2, csrfFieldCount);
	}

	// =========================================================================
	// Helpers
	// =========================================================================

	private Solution createSolution() throws Exception
	{
		AuthenticatorManagerInstanceTest.AuthTestRepository tr = new AuthenticatorManagerInstanceTest.AuthTestRepository();
		RootObjectMetaData metadata = tr.createRootObjectMetaData(UUID.randomUUID(), "TestSolution", IRepository.SOLUTIONS, 1, 1);
		Solution solution = (Solution)tr.createRootObject(metadata);
		solution.setChangeHandler(new com.servoy.j2db.persistence.ChangeHandler(tr));
		return solution;
	}

	static class StubRequest implements HttpServletRequest
	{
		private final Map<String, String> params = new HashMap<>();
		private final Map<String, String> headers = new HashMap<>();
		private Cookie[] cookies;
		private Locale locale = Locale.ENGLISH;

		void setParameter(String name, String value) { params.put(name, value); }
		void setHeader(String name, String value) { headers.put(name, value); }
		void setLocale(Locale l) { locale = l; }
		void setCookies(Cookie[] c) { cookies = c; }

		@Override public String getParameter(String name) { return params.get(name); }
		@Override public String getHeader(String name) { return headers.get(name); }
		@Override public Cookie[] getCookies() { return cookies; }
		@Override public Locale getLocale() { return locale; }
		@Override public boolean isSecure() { return false; }
		@Override public String getCharacterEncoding() { return "UTF-8"; }
		@Override public String getRemoteAddr() { return "127.0.0.1"; }
		@Override public String getRequestURI() { return "/solution/test/index.html"; }
		@Override public StringBuffer getRequestURL() { return new StringBuffer("http://localhost/solution/test/index.html"); }

		@Override public Object getAttribute(String n) { return null; }
		@Override public java.util.Enumeration<String> getAttributeNames() { return Collections.emptyEnumeration(); }
		@Override public void setCharacterEncoding(String e) {}
		@Override public int getContentLength() { return 0; }
		@Override public long getContentLengthLong() { return 0; }
		@Override public String getContentType() { return null; }
		@Override public jakarta.servlet.ServletInputStream getInputStream() { return null; }
		@Override public java.util.Enumeration<String> getParameterNames() { return Collections.emptyEnumeration(); }
		@Override public String[] getParameterValues(String n) { return null; }
		@Override public Map<String, String[]> getParameterMap() { return Collections.emptyMap(); }
		@Override public String getProtocol() { return null; }
		@Override public String getScheme() { return "http"; }
		@Override public String getServerName() { return "localhost"; }
		@Override public int getServerPort() { return 80; }
		@Override public java.io.BufferedReader getReader() { return null; }
		@Override public String getRemoteHost() { return null; }
		@Override public void setAttribute(String n, Object o) {}
		@Override public void removeAttribute(String n) {}
		@Override public java.util.Enumeration<Locale> getLocales() { return null; }
		@Override public jakarta.servlet.RequestDispatcher getRequestDispatcher(String p) { return null; }
		@Override public int getRemotePort() { return 0; }
		@Override public String getLocalName() { return null; }
		@Override public String getLocalAddr() { return null; }
		@Override public int getLocalPort() { return 0; }
		@Override public jakarta.servlet.ServletContext getServletContext() { return null; }
		@Override public jakarta.servlet.AsyncContext startAsync() { return null; }
		@Override public jakarta.servlet.AsyncContext startAsync(jakarta.servlet.ServletRequest rq, jakarta.servlet.ServletResponse rs) { return null; }
		@Override public boolean isAsyncStarted() { return false; }
		@Override public boolean isAsyncSupported() { return false; }
		@Override public jakarta.servlet.AsyncContext getAsyncContext() { return null; }
		@Override public jakarta.servlet.DispatcherType getDispatcherType() { return null; }
		@Override public String getRequestId() { return null; }
		@Override public String getProtocolRequestId() { return null; }
		@Override public jakarta.servlet.ServletConnection getServletConnection() { return null; }
		@Override public String getAuthType() { return null; }
		@Override public long getDateHeader(String n) { return 0; }
		@Override public java.util.Enumeration<String> getHeaders(String n) { return Collections.emptyEnumeration(); }
		@Override public java.util.Enumeration<String> getHeaderNames() { return Collections.emptyEnumeration(); }
		@Override public int getIntHeader(String n) { return 0; }
		@Override public String getMethod() { return "GET"; }
		@Override public String getPathInfo() { return null; }
		@Override public String getPathTranslated() { return null; }
		@Override public String getContextPath() { return ""; }
		@Override public String getQueryString() { return null; }
		@Override public String getRemoteUser() { return null; }
		@Override public boolean isUserInRole(String r) { return false; }
		@Override public java.security.Principal getUserPrincipal() { return null; }
		@Override public String getRequestedSessionId() { return null; }
		@Override public String getServletPath() { return ""; }
		@Override public jakarta.servlet.http.HttpSession getSession(boolean c) { return null; }
		@Override public jakarta.servlet.http.HttpSession getSession() { return null; }
		@Override public String changeSessionId() { return null; }
		@Override public boolean isRequestedSessionIdValid() { return false; }
		@Override public boolean isRequestedSessionIdFromCookie() { return false; }
		@Override public boolean isRequestedSessionIdFromURL() { return false; }
		@Override public boolean authenticate(HttpServletResponse r) { return false; }
		@Override public void login(String u, String p) {}
		@Override public void logout() {}
		@Override public Collection<jakarta.servlet.http.Part> getParts() { return null; }
		@Override public jakarta.servlet.http.Part getPart(String n) { return null; }
		@Override public <T extends jakarta.servlet.http.HttpUpgradeHandler> T upgrade(Class<T> c) { return null; }
	}

	static class StubResponse implements HttpServletResponse
	{
		private final StringWriter output;
		private final List<Cookie> cookies;
		private final Map<String, String> headers = new HashMap<>();

		StubResponse(StringWriter output, List<Cookie> cookies)
		{
			this.output = output;
			this.cookies = cookies;
		}

		@Override public PrintWriter getWriter() { return new PrintWriter(output, true); }
		@Override public void addCookie(Cookie c) { cookies.add(c); }
		@Override public void setHeader(String n, String v) { headers.put(n, v); }
		@Override public void addHeader(String n, String v) { headers.put(n, v); }
		@Override public void setCharacterEncoding(String c) {}
		@Override public void setContentType(String t) {}
		@Override public void setContentLengthLong(long l) {}
		@Override public String getCharacterEncoding() { return "UTF-8"; }
		@Override public String getContentType() { return null; }
		@Override public jakarta.servlet.ServletOutputStream getOutputStream() { return null; }
		@Override public void setContentLength(int l) {}
		@Override public void setBufferSize(int s) {}
		@Override public int getBufferSize() { return 0; }
		@Override public void flushBuffer() {}
		@Override public void resetBuffer() {}
		@Override public boolean isCommitted() { return false; }
		@Override public void reset() {}
		@Override public void setLocale(Locale l) {}
		@Override public Locale getLocale() { return null; }
		@Override public void addIntHeader(String n, int v) {}
		@Override public void setIntHeader(String n, int v) {}
		@Override public void addDateHeader(String n, long d) {}
		@Override public void setDateHeader(String n, long d) {}
		@Override public boolean containsHeader(String n) { return headers.containsKey(n); }
		@Override public String encodeURL(String u) { return u; }
		@Override public String encodeRedirectURL(String u) { return u; }
		@Override public void sendError(int sc, String msg) {}
		@Override public void sendError(int sc) {}
		@Override public void sendRedirect(String l) {}
		@Override public void sendRedirect(String l, int sc, boolean c) {}
		@Override public void setStatus(int sc) {}
		@Override public int getStatus() { return 200; }
		@Override public String getHeader(String n) { return headers.get(n); }
		@Override public Collection<String> getHeaders(String n) { return Collections.emptyList(); }
		@Override public Collection<String> getHeaderNames() { return headers.keySet(); }
	}

	private static class TestManager extends AbstractAuthenticatorManager
	{
		private final String html;

		TestManager(String html) { super(null); this.html = html; }

		@Override protected String getLoginHTML(HttpServletRequest request, String customHTML) { return html; }

		@Override
		public String getLoginScripts(HttpServletRequest request, long csrfToken)
		{
			return "<base href=\"/\">";
		}

		@Override
		public boolean checkPermissions(String u, String p, boolean r, SvyID t,
			Pair<Boolean, String> n, HttpServletRequest req) { return false; }

		@Override
		public boolean checkUser(String u, String p, boolean r, SvyID t,
			Pair<Boolean, String> n, HttpServletRequest req, HttpServletResponse res) { return false; }

		@Override
		protected ContentSecurityPolicyConfig addContentSecurityPolicyHeader(
			HttpServletRequest request, HttpServletResponse response) { return null; }
	}
}
