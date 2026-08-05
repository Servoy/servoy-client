package com.servoy.j2db.server.ngclient.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

@SuppressWarnings("nls")
public class LoginResultTest
{
	@Test
	public void testNeedsLogin()
	{
		LoginResult result = LoginResult.needsLogin();
		assertFalse(result.isAuthenticated());
		assertNull(result.getToken());
		assertNull(result.getCustomHtml());
		assertNull(result.getReturnValue());
		assertFalse(result.isResponseHandled());
	}

	@Test
	public void testNeedsLoginWithCustomHtml()
	{
		LoginResult result = LoginResult.needsLogin("<html><body>Cloud Login</body></html>");
		assertFalse(result.isAuthenticated());
		assertNull(result.getToken());
		assertEquals("<html><body>Cloud Login</body></html>", result.getCustomHtml());
		assertNull(result.getReturnValue());
		assertFalse(result.isResponseHandled());
	}

	@Test
	public void testNeedsLoginWithNullCustomHtml()
	{
		LoginResult result = LoginResult.needsLogin(null);
		assertFalse(result.isAuthenticated());
		assertNull(result.getCustomHtml());
	}

	@Test
	public void testAuthenticated()
	{
		LoginResult result = LoginResult.authenticated("jwt.token.here");
		assertTrue(result.isAuthenticated());
		assertEquals("jwt.token.here", result.getToken());
		assertNull(result.getCustomHtml());
		assertNull(result.getReturnValue());
		assertFalse(result.isResponseHandled());
	}

	@Test
	public void testAuthenticatedWithReturnValue()
	{
		LoginResult result = LoginResult.authenticated("jwt.token.here", "{\"errorMessage\":\"OK\"}");
		assertTrue(result.isAuthenticated());
		assertEquals("jwt.token.here", result.getToken());
		assertEquals("{\"errorMessage\":\"OK\"}", result.getReturnValue());
		assertNull(result.getCustomHtml());
		assertFalse(result.isResponseHandled());
	}

	@Test
	public void testAuthenticatedWithNullToken()
	{
		LoginResult result = LoginResult.authenticated(null);
		assertTrue(result.isAuthenticated());
		assertNull(result.getToken());
	}

	@Test
	public void testAuthenticatedWithNullReturnValue()
	{
		LoginResult result = LoginResult.authenticated("token", null);
		assertTrue(result.isAuthenticated());
		assertEquals("token", result.getToken());
		assertNull(result.getReturnValue());
	}

	@Test
	public void testHandled()
	{
		LoginResult result = LoginResult.handled();
		assertFalse(result.isAuthenticated());
		assertNull(result.getToken());
		assertNull(result.getCustomHtml());
		assertNull(result.getReturnValue());
		assertTrue(result.isResponseHandled());
	}

	@Test
	public void testSetAuthenticated()
	{
		LoginResult result = LoginResult.needsLogin();
		assertFalse(result.isAuthenticated());
		result.setAuthenticated(true);
		assertTrue(result.isAuthenticated());
		result.setAuthenticated(false);
		assertFalse(result.isAuthenticated());
	}

	@Test
	public void testSetToken()
	{
		LoginResult result = LoginResult.needsLogin();
		assertNull(result.getToken());
		result.setToken("new.token");
		assertEquals("new.token", result.getToken());
		result.setToken(null);
		assertNull(result.getToken());
	}

	@Test
	public void testSetCustomHtml()
	{
		LoginResult result = LoginResult.needsLogin();
		assertNull(result.getCustomHtml());
		result.setCustomHtml("<html>custom</html>");
		assertEquals("<html>custom</html>", result.getCustomHtml());
		result.setCustomHtml(null);
		assertNull(result.getCustomHtml());
	}

	@Test
	public void testSetReturnValue()
	{
		LoginResult result = LoginResult.needsLogin();
		assertNull(result.getReturnValue());
		result.setReturnValue("{\"key\":\"value\"}");
		assertEquals("{\"key\":\"value\"}", result.getReturnValue());
		result.setReturnValue(null);
		assertNull(result.getReturnValue());
	}

	@Test
	public void testSetResponseHandled()
	{
		LoginResult result = LoginResult.needsLogin();
		assertFalse(result.isResponseHandled());
		result.setResponseHandled(true);
		assertTrue(result.isResponseHandled());
		result.setResponseHandled(false);
		assertFalse(result.isResponseHandled());
	}

	@Test
	public void testFieldCombinationAuthenticatedWithCustomHtml()
	{
		LoginResult result = LoginResult.authenticated("token");
		result.setCustomHtml("<html>override</html>");
		assertTrue(result.isAuthenticated());
		assertEquals("token", result.getToken());
		assertEquals("<html>override</html>", result.getCustomHtml());
	}

	@Test
	public void testFieldCombinationNeedsLoginWithReturnValue()
	{
		LoginResult result = LoginResult.needsLogin();
		result.setReturnValue("{\"error\":\"bad password\"}");
		assertFalse(result.isAuthenticated());
		assertEquals("{\"error\":\"bad password\"}", result.getReturnValue());
	}

	@Test
	public void testFieldCombinationAllFieldsSet()
	{
		LoginResult result = LoginResult.authenticated("token", "{\"msg\":\"hello\"}");
		result.setCustomHtml("<html/>");
		result.setResponseHandled(true);
		assertTrue(result.isAuthenticated());
		assertEquals("token", result.getToken());
		assertEquals("{\"msg\":\"hello\"}", result.getReturnValue());
		assertEquals("<html/>", result.getCustomHtml());
		assertTrue(result.isResponseHandled());
	}
}
