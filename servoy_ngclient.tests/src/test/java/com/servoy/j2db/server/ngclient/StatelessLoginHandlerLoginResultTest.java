package com.servoy.j2db.server.ngclient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.Map;

import org.json.JSONObject;
import org.junit.Test;

import com.servoy.base.util.I18NProvider;
import com.servoy.base.util.ITagResolver;
import com.servoy.base.util.TagParser;
import com.servoy.j2db.server.ngclient.auth.AbstractAuthenticatorManager;
import com.servoy.j2db.server.ngclient.auth.LoginResult;

@SuppressWarnings("nls")
public class StatelessLoginHandlerLoginResultTest
{
	private Map<String, String> invokeConvertReturnValueToMap(String returnValue) throws Exception
	{
		Method method = AbstractAuthenticatorManager.class.getDeclaredMethod("convertReturnValueToMap", String.class);
		method.setAccessible(true);
		@SuppressWarnings("unchecked")
		Map<String, String> result = (Map<String, String>)method.invoke(null, returnValue);
		return result;
	}

	@Test
	public void testConvertReturnValueToMap_validJsonObject() throws Exception
	{
		Map<String, String> map = invokeConvertReturnValueToMap("{\"errorMessage\":\"Invalid credentials\",\"redirectUrl\":\"/retry\"}");
		assertNotNull(map);
		assertEquals("Invalid credentials", map.get("errorMessage"));
		assertEquals("/retry", map.get("redirectUrl"));
		assertEquals(2, map.size());
	}

	@Test
	public void testConvertReturnValueToMap_emptyJsonObject() throws Exception
	{
		Map<String, String> map = invokeConvertReturnValueToMap("{}");
		assertNotNull(map);
		assertTrue(map.isEmpty());
	}

	@Test
	public void testConvertReturnValueToMap_nestedObject() throws Exception
	{
		Map<String, String> map = invokeConvertReturnValueToMap("{\"user\":{\"name\":\"john\",\"age\":30},\"status\":\"ok\"}");
		assertNotNull(map);
		assertEquals("ok", map.get("status"));
		JSONObject nested = new JSONObject(map.get("user"));
		assertEquals("john", nested.getString("name"));
		assertEquals(30, nested.getInt("age"));
		assertEquals(2, nested.length());
		assertEquals(2, map.size());
	}

	@Test
	public void testConvertReturnValueToMap_invalidJson() throws Exception
	{
		Map<String, String> map = invokeConvertReturnValueToMap("not json at all");
		assertNotNull(map);
		assertEquals("not json at all", map.get("returnValue"));
		assertEquals(1, map.size());
	}

	@Test
	public void testConvertReturnValueToMap_plainString() throws Exception
	{
		Map<String, String> map = invokeConvertReturnValueToMap("simple error message");
		assertNotNull(map);
		assertEquals("simple error message", map.get("returnValue"));
	}

	@Test
	public void testConvertReturnValueToMap_emptyString() throws Exception
	{
		Map<String, String> map = invokeConvertReturnValueToMap("");
		assertNotNull(map);
		assertEquals("", map.get("returnValue"));
	}

	@Test
	public void testConvertReturnValueToMap_jsonArray() throws Exception
	{
		Map<String, String> map = invokeConvertReturnValueToMap("[1,2,3]");
		assertNotNull(map);
		assertEquals("[1,2,3]", map.get("returnValue"));
	}

	@Test
	public void testConvertReturnValueToMap_numericValues() throws Exception
	{
		Map<String, String> map = invokeConvertReturnValueToMap("{\"count\":42,\"rate\":3.14}");
		assertNotNull(map);
		assertEquals("42", map.get("count"));
		assertEquals("3.14", map.get("rate"));
	}

	@Test
	public void testConvertReturnValueToMap_booleanValues() throws Exception
	{
		Map<String, String> map = invokeConvertReturnValueToMap("{\"success\":true,\"retry\":false}");
		assertNotNull(map);
		assertEquals("true", map.get("success"));
		assertEquals("false", map.get("retry"));
	}

	@Test
	public void testConvertReturnValueToMap_nullValue() throws Exception
	{
		Map<String, String> map = invokeConvertReturnValueToMap("{\"key\":null}");
		assertNotNull(map);
		assertEquals("null", map.get("key"));
	}

	@Test
	public void testTagResolver_resolvesKeysFromReturnValueMap()
	{
		LoginResult loginResult = LoginResult.authenticated("token", "{\"errorMessage\":\"Bad password\",\"code\":\"401\"}");
		Map<String, String> returnValueMap = safeConvertReturnValueToMap(loginResult.getReturnValue());
		final Map<String, String> resolvedMap = returnValueMap;

		ITagResolver resolver = new ITagResolver()
		{
			@Override
			public String getStringValue(String name)
			{
				if ("solutionTitle".equals(name))
				{
					return "My Solution";
				}
				if (resolvedMap != null && resolvedMap.containsKey(name))
				{
					return resolvedMap.get(name);
				}
				return "";
			}
		};

		String result = TagParser.processTags("Error: %%errorMessage%% (%%code%%)", resolver, null);
		assertEquals("Error: Bad password (401)", result);
	}

	@Test
	public void testTagResolver_resolvesSolutionTitle()
	{
		ITagResolver resolver = new ITagResolver()
		{
			@Override
			public String getStringValue(String name)
			{
				if ("solutionTitle".equals(name))
				{
					return "Test App";
				}
				return "";
			}
		};

		String result = TagParser.processTags("Welcome to %%solutionTitle%%", resolver, null);
		assertEquals("Welcome to Test App", result);
	}

	@Test
	public void testTagResolver_returnsEmptyForUnknownTags()
	{
		LoginResult loginResult = LoginResult.authenticated("token", "{\"msg\":\"hello\"}");
		Map<String, String> returnValueMap = safeConvertReturnValueToMap(loginResult.getReturnValue());
		final Map<String, String> resolvedMap = returnValueMap;

		ITagResolver resolver = new ITagResolver()
		{
			@Override
			public String getStringValue(String name)
			{
				if ("solutionTitle".equals(name))
				{
					return "App";
				}
				if (resolvedMap != null && resolvedMap.containsKey(name))
				{
					return resolvedMap.get(name);
				}
				return "";
			}
		};

		String result = TagParser.processTags("Value: %%unknownKey%%", resolver, null);
		assertEquals("Value: ", result);
	}

	@Test
	public void testTagResolver_handlesNullReturnValue()
	{
		LoginResult loginResult = LoginResult.needsLogin();
		Map<String, String> returnValueMap = null;
		if (loginResult.getReturnValue() != null)
		{
			returnValueMap = safeConvertReturnValueToMap(loginResult.getReturnValue());
		}
		final Map<String, String> resolvedMap = returnValueMap;

		ITagResolver resolver = new ITagResolver()
		{
			@Override
			public String getStringValue(String name)
			{
				if ("solutionTitle".equals(name))
				{
					return "App";
				}
				if (resolvedMap != null && resolvedMap.containsKey(name))
				{
					return resolvedMap.get(name);
				}
				return "";
			}
		};

		String result = TagParser.processTags("Title: %%solutionTitle%%, Error: %%error%%", resolver, null);
		assertEquals("Title: App, Error: ", result);
	}

	@Test
	public void testTagResolver_handlesNullLoginResult()
	{
		LoginResult loginResult = null;
		Map<String, String> returnValueMap = null;
		if (loginResult != null && loginResult.getReturnValue() != null)
		{
			returnValueMap = safeConvertReturnValueToMap(loginResult.getReturnValue());
		}
		final Map<String, String> resolvedMap = returnValueMap;

		ITagResolver resolver = new ITagResolver()
		{
			@Override
			public String getStringValue(String name)
			{
				if ("solutionTitle".equals(name))
				{
					return "App";
				}
				if (resolvedMap != null && resolvedMap.containsKey(name))
				{
					return resolvedMap.get(name);
				}
				return "";
			}
		};

		String result = TagParser.processTags("%%solutionTitle%% %%anything%%", resolver, null);
		assertEquals("App ", result);
	}

	@Test
	public void testTagResolver_i18nResolution()
	{
		I18NProvider i18nProvider = new I18NProvider()
		{
			@Override
			public String getI18NMessage(String i18nKey)
			{
				if ("login.error".equals(i18nKey)) return "Login failed";
				if ("login.title".equals(i18nKey)) return "Welcome";
				return i18nKey;
			}

			@Override
			public String getI18NMessage(String i18nKey, String language, String country)
			{
				return getI18NMessage(i18nKey);
			}

			@Override
			public String getI18NMessage(String i18nKey, Object[] array)
			{
				return getI18NMessage(i18nKey);
			}

			@Override
			public String getI18NMessage(String i18nKey, Object[] array, String language, String country)
			{
				return getI18NMessage(i18nKey);
			}

			@Override
			public String getI18NMessageIfPrefixed(String key)
			{
				if (key != null && key.startsWith("i18n:"))
				{
					return getI18NMessage(key.substring(5));
				}
				return key;
			}

			@Override
			public void setI18NMessage(String i18nKey, String value)
			{
			}
		};

		ITagResolver resolver = name -> "";

		String result = TagParser.processTags("%%i18n:login.error%% - %%i18n:login.title%%", resolver, i18nProvider);
		assertEquals("Login failed - Welcome", result);
	}

	@Test
	public void testTagResolver_i18nPrefixedStringAsFullInput()
	{
		I18NProvider i18nProvider = new I18NProvider()
		{
			@Override
			public String getI18NMessage(String i18nKey)
			{
				if ("app.title".equals(i18nKey)) return "My Application";
				return i18nKey;
			}

			@Override
			public String getI18NMessage(String i18nKey, String language, String country)
			{
				return getI18NMessage(i18nKey);
			}

			@Override
			public String getI18NMessage(String i18nKey, Object[] array)
			{
				return getI18NMessage(i18nKey);
			}

			@Override
			public String getI18NMessage(String i18nKey, Object[] array, String language, String country)
			{
				return getI18NMessage(i18nKey);
			}

			@Override
			public String getI18NMessageIfPrefixed(String key)
			{
				if (key != null && key.startsWith("i18n:"))
				{
					return getI18NMessage(key.substring(5));
				}
				return key;
			}

			@Override
			public void setI18NMessage(String i18nKey, String value)
			{
			}
		};

		String result = TagParser.processTags("i18n:app.title", null, i18nProvider);
		assertEquals("My Application", result);
	}

	@Test
	public void testIntegration_authenticatedWithTokenFlow()
	{
		LoginResult result = LoginResult.authenticated("jwt.token.value");
		assertTrue(result.isAuthenticated());
		assertNotNull(result.getToken());
		assertNull(result.getCustomHtml());
		assertFalse(result.isResponseHandled());
	}

	@Test
	public void testIntegration_needsLoginFlow()
	{
		LoginResult result = LoginResult.needsLogin();
		assertFalse(result.isAuthenticated());
		assertNull(result.getToken());
		assertNull(result.getCustomHtml());
		assertFalse(result.isResponseHandled());
	}

	@Test
	public void testIntegration_needsLoginWithCloudCustomHtml()
	{
		String cloudHtml = "<html><head></head><body>Cloud SSO page</body></html>";
		LoginResult result = LoginResult.needsLogin(cloudHtml);
		assertFalse(result.isAuthenticated());
		assertNotNull(result.getCustomHtml());
		assertTrue(result.getCustomHtml().startsWith("<"));
	}

	@Test
	public void testIntegration_handledForOAuthExtractFromFragment()
	{
		LoginResult result = LoginResult.handled();
		assertFalse(result.isAuthenticated());
		assertTrue(result.isResponseHandled());
		assertNull(result.getToken());
	}

	@Test
	public void testIntegration_authenticatedWithReturnValueResolvedInTemplate()
	{
		LoginResult result = LoginResult.authenticated("token123", "{\"welcomeMsg\":\"Hello John\",\"role\":\"admin\"}");
		assertTrue(result.isAuthenticated());

		Map<String, String> returnValueMap = safeConvertReturnValueToMap(result.getReturnValue());
		final Map<String, String> resolvedMap = returnValueMap;

		ITagResolver resolver = new ITagResolver()
		{
			@Override
			public String getStringValue(String name)
			{
				if ("solutionTitle".equals(name))
				{
					return "MyApp";
				}
				if (resolvedMap != null && resolvedMap.containsKey(name))
				{
					return resolvedMap.get(name);
				}
				return "";
			}
		};

		String template = "<h1>%%solutionTitle%%</h1><p>%%welcomeMsg%%</p><span>%%role%%</span>";
		String rendered = TagParser.processTags(template, resolver, null);
		assertEquals("<h1>MyApp</h1><p>Hello John</p><span>admin</span>", rendered);
	}

	@Test
	public void testIntegration_returnValueNonJsonFallsBackToReturnValueKey()
	{
		LoginResult result = LoginResult.authenticated("token", "plain error text");
		Map<String, String> returnValueMap = safeConvertReturnValueToMap(result.getReturnValue());
		final Map<String, String> resolvedMap = returnValueMap;

		ITagResolver resolver = new ITagResolver()
		{
			@Override
			public String getStringValue(String name)
			{
				if (resolvedMap != null && resolvedMap.containsKey(name))
				{
					return resolvedMap.get(name);
				}
				return "";
			}
		};

		String rendered = TagParser.processTags("%%returnValue%%", resolver, null);
		assertEquals("plain error text", rendered);
	}

	private Map<String, String> safeConvertReturnValueToMap(String returnValue)
	{
		try
		{
			return invokeConvertReturnValueToMap(returnValue);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
}
