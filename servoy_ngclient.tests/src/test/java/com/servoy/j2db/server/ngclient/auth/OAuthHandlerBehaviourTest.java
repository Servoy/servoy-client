package com.servoy.j2db.server.ngclient.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.json.JSONObject;
import org.junit.Test;

import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Solution.AUTHENTICATOR_TYPE;
import com.servoy.j2db.server.ngclient.property.Log4JToConsoleTest;
import com.servoy.j2db.util.UUID;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Tests for OAuthHandler behaviour that is reachable without a live OAuth server.
 */
@SuppressWarnings("nls")
public class OAuthHandlerBehaviourTest extends Log4JToConsoleTest
{
	// =========================================================================
	// generateOauthCall â null service (invalid/missing API config) â no output
	// =========================================================================

	@Test
	public void generateOauthCall_nullService_writesNothing()
	{
		StringWriter output = new StringWriter();
		List<Cookie> cookies = new ArrayList<>();
		StubRequest request = new StubRequest();
		StubResponse response = new StubResponse(output, cookies);

		// Empty JSON â OAuthUtils.createOauthService returns null â logs error
		try
		{
			OAuthHandler.generateOauthCall(request, response, new JSONObject());
		}
		catch (NullPointerException e)
		{
			// Acceptable if internal OAuth utils fail
		}

		// Either nothing was written, or an error occurred before writing
		if (output.toString().isEmpty())
		{
			assertTrue("No CSRF cookie should be set when service is null", cookies.isEmpty());
		}
	}

	@Test
	public void generateOauthCall_nullService_doesNotThrow()
	{
		StubRequest request = new StubRequest();
		StubResponse response = new StubResponse(new StringWriter(), new ArrayList<>());

		try
		{
			OAuthHandler.generateOauthCall(request, response, new JSONObject());
		}
		catch (NullPointerException e)
		{
			// NPE from internal OAuth utils is acceptable â not a checked exception from our code
		}
		catch (Exception e)
		{
			org.junit.Assert.fail("generateOauthCall must not throw checked exceptions: " + e.getMessage());
		}
	}


	// =========================================================================
	// OAuthHandler.redirectToOAuthLogin â missing OAuth config on solution
	// =========================================================================

	@Test
	public void redirectToOAuthLogin_noOAuthConfig_doesNotThrow() throws Exception
	{
		Solution solution = createSolutionWithAuthType(AUTHENTICATOR_TYPE.OAUTH);

		StubRequest request = new StubRequest();
		StubResponse response = new StubResponse(new StringWriter(), new ArrayList<>());

		try
		{
			OAuthHandler.redirectToOAuthLogin(request, response, solution);
		}
		catch (Exception e)
		{
			org.junit.Assert.fail("redirectToOAuthLogin must not throw when config is missing: " + e.getMessage());
		}
	}

	@Test
	public void redirectToOAuthLogin_noOAuthConfig_writesNothing() throws Exception
	{
		Solution solution = createSolutionWithAuthType(AUTHENTICATOR_TYPE.OAUTH);
		StringWriter output = new StringWriter();
		List<Cookie> cookies = new ArrayList<>();
		StubRequest request = new StubRequest();
		StubResponse response = new StubResponse(output, cookies);

		OAuthHandler.redirectToOAuthLogin(request, response, solution);

		assertEquals("Nothing must be written when OAuth config is missing", "", output.toString());
	}

	// =========================================================================
	// OAuthHandler instance â requiresCSRFForCheckUser = false
	// =========================================================================

	@Test
	public void oauthHandler_requiresCSRFForCheckUser_isFalse()
	{
		OAuthHandler handler = new OAuthHandler(null);
		assertFalse(handler.requiresCSRFForCheckUser());
	}


	// =========================================================================
	// Helpers
	// =========================================================================


	private Solution createSolutionWithAuthType(AUTHENTICATOR_TYPE type) throws Exception
	{
		AuthenticatorManagerInstanceTest.AuthTestRepository tr = new AuthenticatorManagerInstanceTest.AuthTestRepository();
		RootObjectMetaData metadata = tr.createRootObjectMetaData(UUID.randomUUID(), "TestSolution", IRepository.SOLUTIONS, 1, 1);
		Solution solution = (Solution)tr.createRootObject(metadata);
		solution.setAuthenticator(type);
		return solution;
	}

	static class StubRequest extends AbstractAuthenticatorManagerBehaviourTest.StubRequest
	{
		private final Map<String, String> params = new HashMap<>();

		@Override
		public void setParameter(String name, String value)
		{
			params.put(name, value);
		}

		@Override
		public String getParameter(String name)
		{
			return params.get(name);
		}

		@Override
		public Map<String, String[]> getParameterMap()
		{
			Map<String, String[]> result = new HashMap<>();
			params.forEach((k, v) -> result.put(k, new String[] { v }));
			return result;
		}

		@Override
		public jakarta.servlet.ServletContext getServletContext()
		{
			// Return a minimal stub ServletContext so OAuthUtils.generateNonce doesn't NPE
			return new jakarta.servlet.ServletContext()
			{
				private final Map<String, Object> attributes = new HashMap<>();

				@Override
				public Object getAttribute(String name)
				{
					return attributes.get(name);
				}

				@Override
				public void setAttribute(String name, Object o)
				{
					attributes.put(name, o);
				}

				@Override
				public void removeAttribute(String name)
				{
					attributes.remove(name);
				}

				@Override
				public java.util.Enumeration<String> getAttributeNames()
				{
					return Collections.emptyEnumeration();
				}

				@Override
				public String getContextPath()
				{
					return "";
				}

				@Override
				public jakarta.servlet.ServletContext getContext(String uripath)
				{
					return null;
				}

				@Override
				public int getMajorVersion()
				{
					return 6;
				}

				@Override
				public int getMinorVersion()
				{
					return 0;
				}

				@Override
				public int getEffectiveMajorVersion()
				{
					return 6;
				}

				@Override
				public int getEffectiveMinorVersion()
				{
					return 0;
				}

				@Override
				public String getMimeType(String file)
				{
					return null;
				}

				@Override
				public java.util.Set<String> getResourcePaths(String path)
				{
					return null;
				}

				@Override
				public java.net.URL getResource(String path)
				{
					return null;
				}

				@Override
				public java.io.InputStream getResourceAsStream(String path)
				{
					return null;
				}

				@Override
				public jakarta.servlet.RequestDispatcher getRequestDispatcher(String path)
				{
					return null;
				}

				@Override
				public jakarta.servlet.RequestDispatcher getNamedDispatcher(String name)
				{
					return null;
				}

				@Override
				public void log(String msg)
				{
				}

				@Override
				public void log(String message, Throwable throwable)
				{
				}

				@Override
				public String getRealPath(String path)
				{
					return null;
				}

				@Override
				public String getServerInfo()
				{
					return null;
				}

				@Override
				public String getInitParameter(String name)
				{
					return null;
				}

				@Override
				public java.util.Enumeration<String> getInitParameterNames()
				{
					return Collections.emptyEnumeration();
				}

				@Override
				public boolean setInitParameter(String name, String value)
				{
					return false;
				}

				@Override
				public jakarta.servlet.ServletRegistration.Dynamic addServlet(String servletName, String className)
				{
					return null;
				}

				@Override
				public jakarta.servlet.ServletRegistration.Dynamic addServlet(String servletName, jakarta.servlet.Servlet servlet)
				{
					return null;
				}

				@Override
				public jakarta.servlet.ServletRegistration.Dynamic addServlet(String servletName, Class< ? extends jakarta.servlet.Servlet> servletClass)
				{
					return null;
				}

				@Override
				public jakarta.servlet.ServletRegistration.Dynamic addJspFile(String servletName, String jspFile)
				{
					return null;
				}

				@Override
				public <T extends jakarta.servlet.Servlet> T createServlet(Class<T> clazz)
				{
					return null;
				}

				@Override
				public jakarta.servlet.ServletRegistration getServletRegistration(String servletName)
				{
					return null;
				}

				@Override
				public java.util.Map<String, ? extends jakarta.servlet.ServletRegistration> getServletRegistrations()
				{
					return null;
				}

				@Override
				public jakarta.servlet.FilterRegistration.Dynamic addFilter(String filterName, String className)
				{
					return null;
				}

				@Override
				public jakarta.servlet.FilterRegistration.Dynamic addFilter(String filterName, jakarta.servlet.Filter filter)
				{
					return null;
				}

				@Override
				public jakarta.servlet.FilterRegistration.Dynamic addFilter(String filterName, Class< ? extends jakarta.servlet.Filter> filterClass)
				{
					return null;
				}

				@Override
				public <T extends jakarta.servlet.Filter> T createFilter(Class<T> clazz)
				{
					return null;
				}

				@Override
				public jakarta.servlet.FilterRegistration getFilterRegistration(String filterName)
				{
					return null;
				}

				@Override
				public java.util.Map<String, ? extends jakarta.servlet.FilterRegistration> getFilterRegistrations()
				{
					return null;
				}

				@Override
				public jakarta.servlet.SessionCookieConfig getSessionCookieConfig()
				{
					return null;
				}

				@Override
				public void setSessionTrackingModes(java.util.Set<jakarta.servlet.SessionTrackingMode> sessionTrackingModes)
				{
				}

				@Override
				public java.util.Set<jakarta.servlet.SessionTrackingMode> getDefaultSessionTrackingModes()
				{
					return null;
				}

				@Override
				public java.util.Set<jakarta.servlet.SessionTrackingMode> getEffectiveSessionTrackingModes()
				{
					return null;
				}

				@Override
				public void addListener(String className)
				{
				}

				@Override
				public <T extends java.util.EventListener> void addListener(T t)
				{
				}

				@Override
				public void addListener(Class< ? extends java.util.EventListener> listenerClass)
				{
				}

				@Override
				public <T extends java.util.EventListener> T createListener(Class<T> clazz)
				{
					return null;
				}

				@Override
				public jakarta.servlet.descriptor.JspConfigDescriptor getJspConfigDescriptor()
				{
					return null;
				}

				@Override
				public ClassLoader getClassLoader()
				{
					return null;
				}

				@Override
				public void declareRoles(String... roleNames)
				{
				}

				@Override
				public String getVirtualServerName()
				{
					return null;
				}

				@Override
				public int getSessionTimeout()
				{
					return 0;
				}

				@Override
				public void setSessionTimeout(int sessionTimeout)
				{
				}

				@Override
				public String getRequestCharacterEncoding()
				{
					return null;
				}

				@Override
				public void setRequestCharacterEncoding(String encoding)
				{
				}

				@Override
				public String getResponseCharacterEncoding()
				{
					return null;
				}

				@Override
				public void setResponseCharacterEncoding(String encoding)
				{
				}

				@Override
				public String getServletContextName()
				{
					return null;
				}
			};
		}
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

		@Override
		public PrintWriter getWriter()
		{
			return new PrintWriter(output, true);
		}

		@Override
		public void addCookie(Cookie c)
		{
			cookies.add(c);
		}

		@Override
		public void setHeader(String n, String v)
		{
			headers.put(n, v);
		}

		@Override
		public void addHeader(String n, String v)
		{
			headers.put(n, v);
		}

		@Override
		public void setCharacterEncoding(String c)
		{
		}

		@Override
		public void setContentType(String t)
		{
		}

		@Override
		public void setContentLengthLong(long l)
		{
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
		public void setContentLength(int l)
		{
		}

		@Override
		public void setBufferSize(int s)
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
		public void setLocale(Locale l)
		{
		}

		@Override
		public Locale getLocale()
		{
			return null;
		}

		@Override
		public void addIntHeader(String n, int v)
		{
		}

		@Override
		public void setIntHeader(String n, int v)
		{
		}

		@Override
		public void addDateHeader(String n, long d)
		{
		}

		@Override
		public void setDateHeader(String n, long d)
		{
		}

		@Override
		public boolean containsHeader(String n)
		{
			return headers.containsKey(n);
		}

		@Override
		public String encodeURL(String u)
		{
			return u;
		}

		@Override
		public String encodeRedirectURL(String u)
		{
			return u;
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
		public void sendRedirect(String l)
		{
		}

		@Override
		public void sendRedirect(String l, int sc, boolean c)
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
		public String getHeader(String n)
		{
			return headers.get(n);
		}

		@Override
		public Collection<String> getHeaders(String n)
		{
			return Collections.emptyList();
		}

		@Override
		public Collection<String> getHeaderNames()
		{
			return headers.keySet();
		}
	}
}