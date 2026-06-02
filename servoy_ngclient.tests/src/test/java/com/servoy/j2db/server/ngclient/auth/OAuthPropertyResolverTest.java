/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2024 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
*/

package com.servoy.j2db.server.ngclient.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Properties;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import com.servoy.j2db.server.ngclient.property.Log4JToConsoleTest;

/**
 * Unit tests for {@link OAuthPropertyResolver}.
 * Covers all spec-mandated test cases from SVY-21140 §7
 * (TC1–TC7, TC9, TC10; there is no TC8 in the spec).
 * Additional edge-case tests are included beyond the spec requirements.
 */
@SuppressWarnings("nls")
public class OAuthPropertyResolverTest extends Log4JToConsoleTest
{
	private Properties settings;

	@Before
	public void setUp()
	{
		settings = new Properties();
	}

	// -------------------------------------------------------------------------
	// Test 1 — resolve_singlePlaceholder_replaced
	// AC: A clientId value of %%oauth.microsoft.clientId%% is resolved to the
	//     matching servoy.properties value before the OAuth service is constructed.
	// -------------------------------------------------------------------------
	@Test
	public void resolve_singlePlaceholder_replaced()
	{
		settings.setProperty("oauth.microsoft.clientId", "real-client-id");

		JSONObject json = new JSONObject();
		json.put("clientId", "%%oauth.microsoft.clientId%%");

		JSONObject result = OAuthPropertyResolver.resolve(json, settings);

		assertEquals("real-client-id", result.getString("clientId"));
	}

	// -------------------------------------------------------------------------
	// Test 2 — resolve_multiplePlaceholders_allReplaced
	// AC: Both clientId and apiSecret placeholders are resolved in one call.
	//     All other string fields in the OAuth JSON support the same substitution.
	// -------------------------------------------------------------------------
	@Test
	public void resolve_multiplePlaceholders_allReplaced()
	{
		settings.setProperty("oauth.microsoft.clientId", "real-client-id");
		settings.setProperty("oauth.microsoft.apiSecret", "real-secret");

		JSONObject json = new JSONObject();
		json.put("api", "Microsoft");
		json.put("clientId", "%%oauth.microsoft.clientId%%");
		json.put("apiSecret", "%%oauth.microsoft.apiSecret%%");
		json.put("defaultScope", "openid email profile");

		JSONObject result = OAuthPropertyResolver.resolve(json, settings);

		assertEquals("real-client-id", result.getString("clientId"));
		assertEquals("real-secret", result.getString("apiSecret"));
		// non-placeholder fields are preserved unchanged
		assertEquals("Microsoft", result.getString("api"));
		assertEquals("openid email profile", result.getString("defaultScope"));
	}

	// -------------------------------------------------------------------------
	// Test 3 — resolve_unknownKey_leftAsIs
	// AC: If a placeholder key has no matching property, the %%key%% token is
	//     left as-is (not silently replaced with empty string).
	// -------------------------------------------------------------------------
	@Test
	public void resolve_unknownKey_leftAsIs()
	{
		// settings intentionally empty — no matching property

		JSONObject json = new JSONObject();
		json.put("clientId", "%%missing.key%%");

		JSONObject result = OAuthPropertyResolver.resolve(json, settings);

		assertEquals("%%missing.key%%", result.getString("clientId"));
	}

	// -------------------------------------------------------------------------
	// Test 4 — resolve_noPlaceholders_unchanged
	// AC: Non-placeholder configs (literal values) continue to work unchanged.
	// -------------------------------------------------------------------------
	@Test
	public void resolve_noPlaceholders_unchanged()
	{
		JSONObject json = new JSONObject();
		json.put("api", "Microsoft");
		json.put("clientId", "literal-client-id");
		json.put("apiSecret", "literal-secret");

		JSONObject result = OAuthPropertyResolver.resolve(json, settings);

		assertEquals("literal-client-id", result.getString("clientId"));
		assertEquals("literal-secret", result.getString("apiSecret"));
		assertEquals("Microsoft", result.getString("api"));
	}

	// -------------------------------------------------------------------------
	// Test 5 — resolve_doesNotMutateInput
	// AC: The original JSONObject passed to resolve() is not modified.
	// -------------------------------------------------------------------------
	@Test
	public void resolve_doesNotMutateInput()
	{
		settings.setProperty("oauth.microsoft.clientId", "real-client-id");

		JSONObject json = new JSONObject();
		json.put("clientId", "%%oauth.microsoft.clientId%%");

		// Capture original value BEFORE the call so the assertion is meaningful
		// even if resolve() were to mutate and return the same object.
		String originalValue = json.getString("clientId");

		JSONObject result = OAuthPropertyResolver.resolve(json, settings);

		// result must be a distinct object (not the same reference as input)
		assertNotSame("resolve() must return a new object, not the input", json, result);
		// result must contain the resolved value
		assertEquals("real-client-id", result.getString("clientId"));
		// original captured value must still be the placeholder
		assertEquals("%%oauth.microsoft.clientId%%", originalValue);
		// live input object must also be unchanged
		assertEquals("%%oauth.microsoft.clientId%%", json.getString("clientId"));
	}

	// -------------------------------------------------------------------------
	// Test 6 — containsPlaceholder_true
	// AC: Returns true when any string value contains %%...%%.
	// -------------------------------------------------------------------------
	@Test
	public void containsPlaceholder_true()
	{
		JSONObject json = new JSONObject();
		json.put("api", "Microsoft");
		json.put("clientId", "%%oauth.microsoft.clientId%%");

		assertTrue(OAuthPropertyResolver.containsPlaceholder(json));
	}

	// -------------------------------------------------------------------------
	// Test 7 — containsPlaceholder_false
	// AC: Returns false for a JSON with only literal values.
	// -------------------------------------------------------------------------
	@Test
	public void containsPlaceholder_false()
	{
		JSONObject json = new JSONObject();
		json.put("api", "Microsoft");
		json.put("clientId", "literal-client-id");
		json.put("apiSecret", "literal-secret");

		assertFalse(OAuthPropertyResolver.containsPlaceholder(json));
	}

	// -------------------------------------------------------------------------
	// Test 8 — resolvePlaceholders_mixedContent
	// AC: A string that mixes literal text and a placeholder has only the
	//     placeholder token substituted; surrounding text is preserved.
	// -------------------------------------------------------------------------
	@Test
	public void resolvePlaceholders_mixedContent()
	{
		settings.setProperty("tenant.id", "my-tenant");

		String value = "https://login.microsoftonline.com/%%tenant.id%%/v2.0";
		String result = OAuthPropertyResolver.resolvePlaceholders(value, settings);

		assertEquals("https://login.microsoftonline.com/my-tenant/v2.0", result);
	}

	// -------------------------------------------------------------------------
	// Test 9 — findUnresolved_missingKeys_returned
	// AC: Returns the property key names for all %%key%% tokens with no
	//     matching entry in the supplied Properties.
	// -------------------------------------------------------------------------
	@Test
	public void findUnresolved_missingKeys_returned()
	{
		// settings has clientId but NOT apiSecret
		settings.setProperty("oauth.microsoft.clientId", "real-client-id");

		JSONObject json = new JSONObject();
		json.put("clientId", "%%oauth.microsoft.clientId%%");
		json.put("apiSecret", "%%oauth.microsoft.apiSecret%%");

		List<String> missing = OAuthPropertyResolver.findUnresolved(json, settings);

		assertEquals(1, missing.size());
		assertTrue(missing.contains("oauth.microsoft.apiSecret"));
		// resolved key must NOT appear in the missing list
		assertFalse(missing.contains("oauth.microsoft.clientId"));
	}

	// -------------------------------------------------------------------------
	// Test 10 — findUnresolved_allResolved_emptyList
	// AC: Returns an empty list when all placeholder keys have matching
	//     properties.
	// -------------------------------------------------------------------------
	@Test
	public void findUnresolved_allResolved_emptyList()
	{
		settings.setProperty("oauth.microsoft.clientId", "real-client-id");
		settings.setProperty("oauth.microsoft.apiSecret", "real-secret");

		JSONObject json = new JSONObject();
		json.put("clientId", "%%oauth.microsoft.clientId%%");
		json.put("apiSecret", "%%oauth.microsoft.apiSecret%%");

		List<String> missing = OAuthPropertyResolver.findUnresolved(json, settings);

		assertTrue("Expected empty list when all placeholders resolve", missing.isEmpty());
	}

	// -------------------------------------------------------------------------
	// Edge cases
	// -------------------------------------------------------------------------

	@Test
	public void resolvePlaceholders_emptyString_returnsEmpty()
	{
		String result = OAuthPropertyResolver.resolvePlaceholders("", settings);
		assertEquals("", result);
	}

	@Test
	public void resolvePlaceholders_noPlaceholderTokens_returnsUnchanged()
	{
		String value = "literal-value-with-no-tokens";
		String result = OAuthPropertyResolver.resolvePlaceholders(value, settings);
		assertEquals(value, result);
	}

	@Test
	public void resolvePlaceholders_singlePercentSigns_notTreatedAsPlaceholder()
	{
		// %single% (one percent each side) must NOT be treated as a placeholder
		String value = "%not.a.placeholder%";
		String result = OAuthPropertyResolver.resolvePlaceholders(value, settings);
		assertEquals("%not.a.placeholder%", result);
	}

	@Test
	public void findUnresolved_duplicatePlaceholderAcrossFields_reportedOnce()
	{
		// Same %%key%% appears in two fields — must appear only once in the missing list
		JSONObject json = new JSONObject();
		json.put("clientId", "%%shared.key%%");
		json.put("apiSecret", "%%shared.key%%");

		List<String> missing = OAuthPropertyResolver.findUnresolved(json, settings);

		assertEquals("Duplicate missing key must be reported only once", 1, missing.size());
		assertTrue(missing.contains("shared.key"));
	}

	@Test
	public void containsPlaceholder_emptyJson_returnsFalse()
	{
		assertFalse(OAuthPropertyResolver.containsPlaceholder(new JSONObject()));
	}
}
