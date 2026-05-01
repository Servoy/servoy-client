package com.servoy.j2db.server.ngclient.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

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
		assertTrue("basic encoding failed: " + qs, qs.contains("key=hello+world"));
	}

	@Test
	public void testSkipKeyBehavior()
	{
		Map<String, String[]> params = new HashMap<>();
		params.put("svyRedirect", new String[] { "shouldBeSkipped" });
		params.put("keep", new String[] { "value" });
		String qs = QuerySanitizer.buildQueryString(params, java.util.Collections.singleton("svyRedirect"));
		assertTrue("skipKey not applied: " + qs, !qs.contains("svyRedirect"));
		assertTrue("missing keep param: " + qs, qs.contains("keep=value"));
	}

	@Test
	public void testControlCharsRemoved()
	{
		String input = "hello\nworld\r\t!";
		String sanitized = QuerySanitizer.sanitizeParamValue(input);
		// control characters \n \r \t should be removed
		assertEquals("control chars removal failed", "helloworld!", sanitized);
	}

	@Test
	public void testParamNameSanitization()
	{
		String name = "weird name?*#";
		String sanitized = QuerySanitizer.sanitizeParamName(name);
		// spaces become underscores and other illegal chars replaced by '_'
		assertEquals("param name sanitization failed", "weird_name___", sanitized);
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
