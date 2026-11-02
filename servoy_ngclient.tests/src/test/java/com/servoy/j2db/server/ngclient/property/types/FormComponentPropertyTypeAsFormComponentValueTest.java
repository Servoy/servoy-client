package com.servoy.j2db.server.ngclient.property.types;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.stream.Stream;

import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;

/**
 * SVY-21667: a form-component property value is expected to have the canonical object shape
 * <code>{ "svy_form": "&lt;form UUID&gt;" }</code>. Before the fix, five call sites cast the raw
 * value straight to {@link org.json.JSONObject}; a bare form-name/UUID string hand-written into the
 * .frm therefore threw a context-free {@code ClassCastException} instead of being reported and skipped.
 * <p>
 * These tests pin the single guard all five call sites now delegate to,
 * {@link FormComponentPropertyType#asFormComponentValue(Object, String, String, String)}: a real
 * {@code JSONObject} must be returned unchanged (the exact same instance), and every other value -
 * a bare form name, a bare UUID string, {@code null}, or any other non-{@code JSONObject} type - must
 * return {@code null} (reject-and-report) rather than being coerced or throwing.
 * <p>
 * The guard's other observable effect, a single {@code Debug.error} log line, is deliberately not
 * asserted: {@code Debug} is a static Servoy logger and the acceptance criteria hinge on the return
 * value (passthrough vs {@code null}), which is what is verified here. The message is only exercised
 * for its null-safety (a {@code null} value must not make it throw).
 */
class FormComponentPropertyTypeAsFormComponentValueTest
{
	private static final String SOME_FORM_UUID = "A1B2C3D4-0000-1111-2222-3344556677AA";

	@Nested
	class WellFormedValue
	{
		/**
		 * Acceptance criterion "well-formed value behaves exactly as before": a canonical
		 * { "svy_form": "<uuid>" } object is returned as the very same instance, with its content
		 * left completely untouched.
		 */
		@Test
		@DisplayName("a canonical { svy_form: <uuid> } JSONObject is returned unchanged (same instance, same content)")
		void canonicalObjectIsReturnedUnchanged()
		{
			JSONObject value = new JSONObject().put(FormComponentPropertyType.SVY_FORM, SOME_FORM_UUID);

			JSONObject result = FormComponentPropertyType.asFormComponentValue(value, "fc_customer", "myElement",
				"containedForm");

			assertAll(
				() -> assertSame(value, result, "the exact same JSONObject instance must be returned"),
				() -> assertEquals(SOME_FORM_UUID, result.optString(FormComponentPropertyType.SVY_FORM, null),
					"svy_form must be left exactly as it was"),
				() -> assertEquals(1, result.length(), "no keys may be added or removed"));
		}

		/** Even an empty JSONObject is a valid shape and must pass through as the same instance. */
		@Test
		@DisplayName("an empty JSONObject still passes through as the same instance")
		void emptyObjectPassesThrough()
		{
			JSONObject value = new JSONObject();

			JSONObject result = FormComponentPropertyType.asFormComponentValue(value, null, null, "containedForm");

			assertSame(value, result);
		}
	}

	@Nested
	class RejectedValues
	{
		/**
		 * A value that is not a JSONObject (bare form name, bare form-UUID string, a non-String/
		 * non-JSONObject type) must be rejected with {@code null} - not coerced into an object - so the
		 * caller skips the property instead of throwing a ClassCastException. (SVY-21667)
		 */
		@ParameterizedTest(name = "{0} -> null")
		@MethodSource("badValues")
		@DisplayName("a non-JSONObject value is rejected with null (not coerced, not thrown)")
		void nonJsonObjectValueReturnsNull(String caseName, Object value)
		{
			JSONObject result = FormComponentPropertyType.asFormComponentValue(value, "fc_customer_row", "myElement",
				"containedForm");

			assertNull(result, caseName + " must be rejected with null");
		}

		static Stream<Arguments> badValues()
		{
			return Stream.of(
				Arguments.of("a bare form-name String", "fc_customer_row"),
				Arguments.of("a bare form-UUID String", SOME_FORM_UUID),
				Arguments.of("an Integer", Integer.valueOf(42)),
				Arguments.of("a plain Object", new Object()));
		}

		/**
		 * A {@code null} value is the one case that would also NPE inside the error-message building if
		 * the guard were not null-safe; it must return {@code null} without throwing.
		 */
		@ParameterizedTest
		@NullSource
		@DisplayName("a null value returns null without throwing (the message is null-safe)")
		void nullValueReturnsNull(Object value)
		{
			JSONObject result = FormComponentPropertyType.asFormComponentValue(value, null, null, "containedForm");

			assertNull(result, "a null value must be rejected with null and must not throw");
		}
	}
}
