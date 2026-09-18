package com.servoy.j2db.server.ngclient.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.servoy.j2db.server.ngclient.property.Log4JToConsoleTest;

/**
 * Framework-free test class colocated with QuerySanitizer for ad-hoc runs in Eclipse/Tycho.
 * This contains a main() to execute basic assertions without requiring JUnit on the classpath.
 */
@SuppressWarnings("nls")
public class QuerySanitizerTest extends Log4JToConsoleTest
{
	@Test
	public void testBuildQueryString_basicEncoding()
	{
		Map<String, String[]> params = new HashMap<>();
		params.put("key", new String[] { "hello world" });
		String qs = QuerySanitizer.buildQueryString(params, java.util.Collections.emptySet());
		// URLEncoder encodes space as + for application/x-www-form-urlencoded
		assertTrue(qs.contains("key=hello+world"), "basic encoding failed: " + qs);
	}

	@Test
	public void testSkipKeyBehavior()
	{
		Map<String, String[]> params = new HashMap<>();
		params.put("svyRedirect", new String[] { "shouldBeSkipped" });
		params.put("keep", new String[] { "value" });
		String qs = QuerySanitizer.buildQueryString(params, java.util.Collections.singleton("svyRedirect"));
		assertTrue(!qs.contains("svyRedirect"), "skipKey not applied: " + qs);
		assertTrue(qs.contains("keep=value"), "missing keep param: " + qs);
	}

	@Test
	public void testControlCharsRemoved()
	{
		String input = "hello\nworld\r\t!";
		String sanitized = QuerySanitizer.sanitizeParamValue(input);
		// control characters \n \r \t should be removed
		assertEquals("helloworld!", sanitized, "control chars removal failed");
	}

	@Test
	public void testParamNameSanitization()
	{
		String name = "weird name?*#";
		String sanitized = QuerySanitizer.sanitizeParamName(name);
		// spaces become underscores and other illegal chars replaced by '_'
		assertEquals("weird_name___", sanitized, "param name sanitization failed");
	}

	@Test
	public void testPerParamLengthRejection()
	{
		int len = QuerySanitizer.MAX_PARAM_ENCODED_LENGTH + 10;
		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; i++)
			sb.append('a');
		Map<String, String[]> params = new HashMap<>();
		params.put("k", new String[] { sb.toString() });
		try
		{
			QuerySanitizer.buildQueryString(params, java.util.Collections.emptySet());
			throw new AssertionError("Expected IllegalArgumentException for oversized parameter");
		}
		catch (IllegalArgumentException e)
		{
			// expected
		}
	}

	@Test
	public void testTotalLengthRejection()
	{
		Map<String, String[]> params = new HashMap<>();
		int count = QuerySanitizer.MAX_TOTAL_QUERY_LENGTH / 4 + 10;
		for (int i = 0; i < count; i++)
		{
			params.put("k" + i, new String[] { "val" + i });
		}
		try
		{
			QuerySanitizer.buildQueryString(params, java.util.Collections.emptySet());
			throw new AssertionError("Expected IllegalArgumentException for oversized total query");
		}
		catch (IllegalArgumentException e)
		{
			// expected
		}
	}
}
