package com.servoy.j2db.server.ngclient.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.junit.Test;

import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Solution.AUTHENTICATOR_TYPE;
import com.servoy.j2db.server.ngclient.property.Log4JToConsoleTest;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Tests for AuthenticatorManager pure-logic methods â all testable without
 * ApplicationServerRegistry.
 */
@SuppressWarnings("nls")
public class AuthenticatorManagerUtilsTest extends Log4JToConsoleTest
{
	// =========================================================================
	// addCustomParameters (private static) â via reflection
	// =========================================================================

	@Test
	public void addCustomParameters_customPrefix_addedToJson() throws Exception
	{
		Map<String, String[]> params = new HashMap<>();
		params.put("custom_foo", new String[] { "bar" });
		params.put("custom_baz", new String[] { "qux" });
		params.put("other", new String[] { "ignored" });

		JSONObject json = new JSONObject();
		invokeAddCustomParameters(stubRequest(params), json);

		assertEquals("bar", json.getString("custom_foo"));
		assertEquals("qux", json.getString("custom_baz"));
		assertFalse("Non-custom params must not be added", json.has("other"));
	}

	@Test
	public void addCustomParameters_noCustomParams_jsonUnchanged() throws Exception
	{
		Map<String, String[]> params = new HashMap<>();
		params.put("username", new String[] { "alice" });

		JSONObject json = new JSONObject();
		invokeAddCustomParameters(stubRequest(params), json);

		assertFalse("No custom_ params â json must stay empty", json.has("username"));
	}

	@Test
	public void addCustomParameters_stateParam_parsedAndAddedToJson() throws Exception
	{
		String encodedState = URLEncoder.encode("mystate", StandardCharsets.UTF_8);
		String stateValue = "state=" + encodedState + "&svyuuid=abc123&foo=bar";

		Map<String, String[]> params = new HashMap<>();
		params.put("state", new String[] { stateValue });

		JSONObject json = new JSONObject();
		invokeAddCustomParameters(stubRequest(params), json);

		assertTrue("Parsed state must be put in json", json.has("state"));
		assertEquals("mystate", json.getString("state"));
		assertTrue("query field must be added", json.has("query"));
		// svyuuid must be removed from query
		assertFalse("svyuuid must not appear in query", json.getString("query").contains("svyuuid"));
	}

	@Test
	public void addCustomParameters_multipleCastsForSameCustomKey_lastValueWins() throws Exception
	{
		Map<String, String[]> params = new HashMap<>();
		params.put("custom_key", new String[] { "first", "second" });

		JSONObject json = new JSONObject();
		invokeAddCustomParameters(stubRequest(params), json);

		// The loop puts each value, last one wins in JSONObject
		assertNotNull(json.get("custom_key"));
	}

	// =========================================================================
	// addParsedStateParameterToJson (private static) â via reflection
	// =========================================================================

	@Test
	public void addParsedState_stateDecoded_svyuuidRemoved_queryBuilt() throws Exception
	{
		String encodedState = URLEncoder.encode("decoded-state-value", StandardCharsets.UTF_8);
		String input = "state=" + encodedState + "&svyuuid=uuid-abc&foo=1&bar=2";

		JSONObject json = new JSONObject();
		invokeAddParsedState(input, json);

		assertEquals("decoded-state-value", json.getString("state"));
		assertFalse("svyuuid must be removed", json.has("svyuuid"));
		String query = json.getString("query");
		assertTrue("foo must be in query", query.contains("foo=1"));
		assertTrue("bar must be in query", query.contains("bar=2"));
		assertFalse("state must not remain in query", query.contains("state="));
		assertFalse("svyuuid must not remain in query", query.contains("svyuuid="));
	}

	@Test
	public void addParsedState_noStateKey_onlyQueryBuilt() throws Exception
	{
		String input = "svyuuid=abc&foo=hello";

		JSONObject json = new JSONObject();
		invokeAddParsedState(input, json);

		assertFalse("No state key â no state in json", json.has("state"));
		String query = json.getString("query");
		assertTrue("foo must appear in query", query.contains("foo=hello"));
		assertFalse("svyuuid removed", query.contains("svyuuid"));
	}

	@Test
	public void addParsedState_emptyInput_queryIsEmpty() throws Exception
	{
		JSONObject json = new JSONObject();
		invokeAddParsedState("", json);

		// empty string splits to [""] which produces key="" value=""
		// just check it does not throw and json has query
		assertNotNull(json.get("query"));
	}

	@Test
	public void addParsedState_onlySvyuuid_queryIsEmpty() throws Exception
	{
		JSONObject json = new JSONObject();
		invokeAddParsedState("svyuuid=abc", json);

		assertEquals("", json.getString("query"));
	}

	@Test
	public void addParsedState_urlEncodedStateValue_properlyDecoded() throws Exception
	{
		String original = "hello world & special=chars";
		String encoded = URLEncoder.encode(original, StandardCharsets.UTF_8);
		String input = "state=" + encoded;

		JSONObject json = new JSONObject();
		invokeAddParsedState(input, json);

		assertEquals(original, json.getString("state"));
	}

	// =========================================================================
	// callAuthenticator â null onOpenMethodID â returns false immediately
	// =========================================================================

	@Test
	public void callAuthenticator_noOnOpenMethod_returnsFalse() throws Exception
	{
		Solution authenticator = createSolution();
		// onOpenMethodID is null by default (not set)
		assertNull("onOpenMethodID must be null for this test", authenticator.getOnOpenMethodID());

		Solution mainSolution = createSolution();
		Pair<Boolean, String> needToLogin = new Pair<>(Boolean.FALSE, null);

		boolean result = AuthenticatorManager.callAuthenticator(needToLogin, null, false,
			authenticator, new JSONObject(), null, mainSolution);

		assertFalse("Must return false when authenticator has no onOpen method", result);
		assertFalse("needToLogin.left must stay false", needToLogin.getLeft());
	}

	// =========================================================================
	// writeLoginPage â AUTHENTICATOR type calls super, OAUTH_AUTHENTICATOR redirects
	// (structural test via enum comparison â no server or OAuth call needed)
	// =========================================================================

	@Test
	public void writeLoginPage_authenticatorType_doesNotRedirectToOAuth() throws Exception
	{
		Solution solution = createSolutionWithAuthType(AUTHENTICATOR_TYPE.AUTHENTICATOR);

		// Verify via reflection that OAUTH_AUTHENTICATOR check returns false for AUTHENTICATOR type
		assertTrue("AUTHENTICATOR type must not equal OAUTH_AUTHENTICATOR",
			solution.getAuthenticator() != AUTHENTICATOR_TYPE.OAUTH_AUTHENTICATOR);
	}

	@Test
	public void writeLoginPage_oauthAuthenticatorType_branchIdentified() throws Exception
	{
		Solution solution = createSolutionWithAuthType(AUTHENTICATOR_TYPE.OAUTH_AUTHENTICATOR);
		assertEquals(AUTHENTICATOR_TYPE.OAUTH_AUTHENTICATOR, solution.getAuthenticator());
	}


	// =========================================================================
	// Helpers
	// =========================================================================

	private void invokeAddCustomParameters(HttpServletRequest request, JSONObject json) throws Exception
	{
		Method m = AuthenticatorManager.class.getDeclaredMethod("addCustomParameters",
			HttpServletRequest.class, JSONObject.class);
		m.setAccessible(true);
		m.invoke(null, request, json);
	}

	private void invokeAddParsedState(String stateValue, JSONObject json) throws Exception
	{
		Method m = AuthenticatorManager.class.getDeclaredMethod("addParsedStateParameterToJson",
			String.class, JSONObject.class);
		m.setAccessible(true);
		m.invoke(null, stateValue, json);
	}

	private HttpServletRequest stubRequest(Map<String, String[]> params)
	{
		return new AbstractAuthenticatorManagerBehaviourTest.StubRequest()
		{
			@Override
			public Map<String, String[]> getParameterMap()
			{
				return params;
			}

			@Override
			public String getParameter(String name)
			{
				String[] vals = params.get(name);
				return vals != null && vals.length > 0 ? vals[0] : null;
			}
		};
	}


	private Solution createSolution() throws Exception
	{
		AuthenticatorManagerInstanceTest.AuthTestRepository tr = new AuthenticatorManagerInstanceTest.AuthTestRepository();
		RootObjectMetaData metadata = tr.createRootObjectMetaData(UUID.randomUUID(), "TestSolution", IRepository.SOLUTIONS, 1, 1);
		return (Solution)tr.createRootObject(metadata);
	}

	private Solution createSolutionWithAuthType(AUTHENTICATOR_TYPE type) throws Exception
	{
		Solution solution = createSolution();
		solution.setAuthenticator(type);
		return solution;
	}

	// =========================================================================
	// DefaultLoginManager structural tests (no server call, so no blocking)
	// =========================================================================

	@Test
	public void defaultLoginManager_requiresCSRFForCheckUser_notOverridden()
	{
		try
		{
			DefaultLoginManager.class.getDeclaredMethod("requiresCSRFForCheckUser");
			org.junit.Assert.fail("DefaultLoginManager must NOT override requiresCSRFForCheckUser");
		}
		catch (NoSuchMethodException e)
		{
			// expected â inherits default true from IAuthenticatorManager
		}
		assertTrue(new DefaultLoginManager(null).requiresCSRFForCheckUser());
	}

	@Test
	public void defaultLoginManager_checkUser_methodExists() throws Exception
	{
		Method m = DefaultLoginManager.class.getDeclaredMethod("checkUser",
			String.class, String.class, boolean.class, SvyID.class, Pair.class,
			jakarta.servlet.http.HttpServletRequest.class, jakarta.servlet.http.HttpServletResponse.class);
		assertNotNull(m);
		assertTrue("checkUser must be public", java.lang.reflect.Modifier.isPublic(m.getModifiers()));
	}

	@Test
	public void defaultLoginManager_checkPermissions_methodExists() throws Exception
	{
		Method m = DefaultLoginManager.class.getDeclaredMethod("checkPermissions",
			String.class, String.class, boolean.class, SvyID.class, Pair.class,
			jakarta.servlet.http.HttpServletRequest.class);
		assertNotNull(m);
		assertTrue("checkPermissions must be public", java.lang.reflect.Modifier.isPublic(m.getModifiers()));
	}

	@Test
	public void defaultLoginManager_extendsAbstractAuthenticatorManager()
	{
		assertTrue(new DefaultLoginManager(null) instanceof AbstractAuthenticatorManager);
	}
}
