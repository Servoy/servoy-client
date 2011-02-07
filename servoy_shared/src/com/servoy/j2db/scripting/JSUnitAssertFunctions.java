/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.j2db.scripting;

import com.servoy.j2db.documentation.ServoyDocumented;

/**
 * This class is only used to provide code completion and jsunit node in developer's javascript editor/solution explorer.
 * 
 * @author acostescu
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "JSUnit", scriptingName = "jsunit")
public class JSUnitAssertFunctions implements IReturnedTypesProvider
{

	public static final JSUnitAssertFunctions DEVELOPER_JS_INSTANCE = new JSUnitAssertFunctions();

	static
	{
		ScriptObjectRegistry.registerReturnedTypesProviderForClass(JSUnitAssertFunctions.class, DEVELOPER_JS_INSTANCE);
	}

	private JSUnitAssertFunctions()
	{
	}

	public Class< ? >[] getAllReturnedTypes()
	{
		return null;
	}

	// dummy functions follow (they are only used for code completion and sol.ex. node)
	/**
	 * Asserts that two values are equal. AssertionFailedError is thrown if the actual value does not match the regular expression.
	 *
	 * @sample
	 * // Asserts that two values are equal. AssertionFailedError is thrown if the actual value does not match the regular expression.
	 * jsunit.assertEquals("Solution name test", "someSolution", application.getSolutionName());
	 * jsunit.assertEquals("Simple math test", 2, 1 + 1);
	 *
	 * @param message The test description/message.
	 *
	 * @param expected the expected value.
	 *
	 * @param actual the actual value.
	 */
	public void js_assertEquals(String message, Object expected, Object actual)
	{
	}

	/**
	 * Asserts that two values are equal. AssertionFailedError is thrown if the actual value does not match the regular expression.
	 *
	 * @sample
	 * // Asserts that two values are equal. AssertionFailedError is thrown if the actual value does not match the regular expression.
	 * jsunit.assertEquals("Solution name test", "someSolution", application.getSolutionName());
	 * jsunit.assertEquals("Simple math test", 2, 1 + 1);
	 *
	 * @param expected the expected value.
	 *
	 * @param actual the actual value.
	 */
	public void js_assertEquals(Object expected, Object actual)
	{
	}

	/**
	 * Asserts that a regular expression matches a string. AssertionFailedError is thrown if the expected value is not the actual one.
	 *
	 * @sample
	 * // Asserts that a regular expression matches a string. AssertionFailedError is thrown if the expected value is not the actual one.
	 * jsunit.assertMatches("Match test", new RegExp("gr(a|e)y"), "gray");
	 *
	 * @param message The test description/message.
	 *
	 * @param regularExpression the regular expression used for matching.
	 *
	 * @param actualString  the actual value to be matched.
	 */
	public void js_assertMatches(String message, Object regularExpression, String actualString)
	{
	}

	/**
	 * Asserts that a regular expression matches a string. AssertionFailedError is thrown if the expected value is not the actual one.
	 *
	 * @sample
	 * // Asserts that a regular expression matches a string. AssertionFailedError is thrown if the expected value is not the actual one.
	 * jsunit.assertMatches(new RegExp("gr(a|e)y"), "gray");
	 *
	 * @param regularExpression the regular expression used for matching.
	 *
	 * @param actualString  the actual value to be matched.
	 */
	public void js_assertMatches(Object regularExpression, String actualString)
	{
	}

	/**
	 * Asserts that a condition is false. AssertionFailedError is thrown if the evaluation was not false.
	 *
	 * @sample
	 * // Asserts that a condition is false. AssertionFailedError is thrown if the evaluation was not false.
	 * jsunit.assertFalse("False test", application.isLastPrintPreviewPrinted());
	 *
	 * @param message The test description/message.
	 *
	 * @param boolean_condition the actual value.
	 */
	public void js_assertFalse(String message, boolean boolean_condition)
	{
	}

	/**
	 * Asserts that a condition is false. AssertionFailedError is thrown if the evaluation was not false.
	 *
	 * @sample
	 * // Asserts that a condition is false. AssertionFailedError is thrown if the evaluation was not false.
	 * jsunit.assertFalse("False test", application.isLastPrintPreviewPrinted());
	 *
	 * @param boolean_condition the actual value.
	 */
	public void js_assertFalse(boolean boolean_condition)
	{
	}

	/**
	 * Asserts that two floating point values are equal to within a given tolerance. AssertionFailedError is thrown if the expected value is not within the tolerance of the actual one.
	 *
	 * @sample
	 * // Asserts that two floating point values are equal to within a given tolerance. AssertionFailedError is thrown if the expected value is not within the tolerance of the actual one.
	 * jsunit.assertFloatEquals("Float equals test", 3.12, 3.121, 0.0015);
	 *
	 * @param message The test description/message.
	 *
	 * @param expectedFloat the expected value.
	 *
	 * @param actualFloat the actual value.
	 *
	 * @param tolerance float tolerance when comparing.
	 */
	public void js_assertFloatEquals(String message, float expectedFloat, float actualFloat, float tolerance)
	{
	}

	/**
	 * Asserts that two floating point values are equal to within a given tolerance. AssertionFailedError is thrown if the expected value is not within the tolerance of the actual one.
	 *
	 * @sample
	 * // Asserts that two floating point values are equal to within a given tolerance. AssertionFailedError is thrown if the expected value is not within the tolerance of the actual one.
	 * jsunit.assertFloatEquals("Float equals test", 3.12, 3.121, 0.0015);
	 *
	 * @param expectedFloat the expected value.
	 *
	 * @param actualFloat the actual value.
	 *
	 * @param tolerance float tolerance when comparing.
	 */
	public void js_assertFloatEquals(float expectedFloat, float actualFloat, float tolerance)
	{
	}

	/**
	 * Asserts that an object is not null. AssertionFailedError is thrown if the object is not null.
	 *
	 * @sample
	 * // Asserts that an object is not null. AssertionFailedError is thrown if the object is not null.
	 * var a; // this is undefined, not null
	 * jsunit.assertNotNull("Not null test", a);
	 *
	 * @param message The test description/message.
	 *
	 * @param object the actual value.
	 */
	public void js_assertNotNull(String message, Object object)
	{
	}

	/**
	 * Asserts that an object is not null. AssertionFailedError is thrown if the object is not null.
	 *
	 * @sample
	 * // Asserts that an object is not null. AssertionFailedError is thrown if the object is not null.
	 * var a; // this is undefined, not null
	 * jsunit.assertNotNull(a);
	 *
	 * @param object the actual value.
	 */
	public void js_assertNotNull(Object object)
	{
	}

	/**
	 * Asserts that two values are not the same. AssertionFailedError is thrown if the expected value is the actual one.
	 *
	 * @sample
	 * // Asserts that two values are not the same. AssertionFailedError is thrown if the expected value is the actual one.
	 * var a = new Date(1990, 1, 1);
	 * var b = new Date(1990, 1, 1);
	 * jsunit.assertNotSame("Not same test", a, b);
	 * jsunit.assertEquals("But equals", a, b);
	 *
	 * @param message The test description/message.
	 *
	 * @param notExpected the value that is not expected.
	 *
	 * @param actual the actual value.
	 */
	public void js_assertNotSame(String message, Object notExpected, Object actual)
	{
	}

	/**
	 * Asserts that two values are not the same. AssertionFailedError is thrown if the expected value is the actual one.
	 *
	 * @sample
	 * // Asserts that two values are not the same. AssertionFailedError is thrown if the expected value is the actual one.
	 * var a = new Date(1990, 1, 1);
	 * var b = new Date(1990, 1, 1);
	 * jsunit.assertNotSame(a, b);
	 * jsunit.assertEquals("But equals", a, b);
	 *
	 * @param notExpected the value that is not expected.
	 *
	 * @param actual the actual value.
	 */
	public void js_assertNotSame(Object notExpected, Object actual)
	{
	}

	/**
	 * Asserts that an object is not undefined. AssertionFailedError is thrown if the object is undefined.
	 *
	 * @sample
	 * // Asserts that an object is not undefined. AssertionFailedError is thrown if the object is undefined.
	 * var a = 0;
	 * jsunit.assertNotUndefined(a);
	 *
	 * @param definedObject the actual value.
	 */
	public void js_assertNotUndefined(Object definedObject)
	{
	}

	/**
	 * Asserts that an object is not undefined. AssertionFailedError is thrown if the object is undefined.
	 *
	 * @sample
	 * // Asserts that an object is not undefined. AssertionFailedError is thrown if the object is undefined.
	 * var a = 0;
	 * jsunit.assertNotUndefined("Not undefined test", a);
	 *
	 * @param message The test description/message.
	 *
	 * @param definedObject the actual value.
	 */
	public void js_assertNotUndefined(String message, Object definedObject)
	{
	}

	/**
	 * Asserts that an object is null. AssertionFailedError is thrown if the object is not null.
	 *
	 * @sample
	 * // Asserts that an object is null. AssertionFailedError is thrown if the object is not null.
	 * jsunit.assertNull("Null test", null);
	 *
	 * @param nullValue the actual value.
	 */
	public void js_assertNull(Object nullValue)
	{
	}

	/**
	 * Asserts that an object is null. AssertionFailedError is thrown if the object is not null.
	 *
	 * @sample
	 * // Asserts that an object is null. AssertionFailedError is thrown if the object is not null.
	 * jsunit.assertNull("Null test", null);
	 *
	 * @param message The test description/message.
	 *
	 * @param nullValue the actual value.
	 */
	public void js_assertNull(String message, Object nullValue)
	{
	}

	/**
	 * Asserts that two values are the same. AssertionFailedError is thrown if the expected value is not the actual one.
	 *
	 * @sample
	 * // Asserts that two values are the same. AssertionFailedError is thrown if the expected value is not the actual one.
	 * var a = new Date(1990, 1, 1);
	 * var b = a;
	 * jsunit.assertSame(a, b);
	 *
	 * @param expected the expected value.
	 *
	 * @param actual the actual value.
	 */
	public void js_assertSame(Object expected, Object actual)
	{
	}

	/**
	 * Asserts that two values are the same. AssertionFailedError is thrown if the expected value is not the actual one.
	 *
	 * @sample
	 * // Asserts that two values are the same. AssertionFailedError is thrown if the expected value is not the actual one.
	 * var a = new Date(1990, 1, 1);
	 * var b = a;
	 * jsunit.assertSame("Same test", a, b);
	 *
	 * @param message The test description/message.
	 *
	 * @param expected the expected value.
	 *
	 * @param actual the actual value.
	 */
	public void js_assertSame(String message, Object expected, Object actual)
	{
	}

	/**
	 * Asserts that a condition is true. AssertionFailedError is thrown if the evaluation was not true.
	 *
	 * @sample
	 * // Asserts that a condition is true. AssertionFailedError is thrown if the evaluation was not true.
	 * jsunit.assertTrue("True test", application.isLastPrintPreviewPrinted());
	 *
	 * @param boolean_condition the actual value.
	 */
	public void js_assertTrue(boolean boolean_condition)
	{
	}

	/**
	 * Asserts that a condition is true. AssertionFailedError is thrown if the evaluation was not true.
	 *
	 * @sample
	 * // Asserts that a condition is true. AssertionFailedError is thrown if the evaluation was not true.
	 * jsunit.assertTrue("True test", application.isLastPrintPreviewPrinted());
	 *
	 * @param message The test description/message.
	 *
	 * @param boolean_condition the actual value.
	 */
	public void js_assertTrue(String message, boolean boolean_condition)
	{
	}

	/**
	 * Asserts that an object is undefined. AssertionFailedError is thrown if the object is defined.
	 *
	 * @sample
	 * // Asserts that an object is undefined. AssertionFailedError is thrown if the object is defined.
	 * jsunit.assertUndefined("Undefined test", thisIsUndefined);
	 *
	 * @param undefinedValue the actual value.
	 */
	public void js_assertUndefined(Object undefinedValue)
	{
	}

	/**
	 * Asserts that an object is undefined. AssertionFailedError is thrown if the object is defined.
	 *
	 * @sample
	 * // Asserts that an object is undefined. AssertionFailedError is thrown if the object is defined.
	 * jsunit.assertUndefined(thisIsUndefined);
	 *
	 * @param message The test description/message.
	 *
	 * @param undefinedValue the actual value.
	 */
	public void js_assertUndefined(String message, Object undefinedValue)
	{
	}

	/**
	 * Fails a test. AssertionFailedError is always thrown.
	 *
	 * @sample
	 * // Fails a test. AssertionFailedError is always thrown.
	 * jsunit.fail("Fail test");
	 * jsunit.fail("test", null, "Fail"); // 2nd param is not used in Servoy, params 3 and 1 get merged to form a message. The result is the same as in the line above.
	 *
	 * @param message The test description/message. This is usually the only parameter specified when calling this method.
	 *
	 */
	public void js_fail(String message)
	{
	}

	/**
	 * Fails a test. AssertionFailedError is always thrown.
	 *
	 * @sample
	 * // Fails a test. AssertionFailedError is always thrown.
	 * jsunit.fail("Fail test");
	 * jsunit.fail("test", null, "Fail"); // 2nd param is not used in Servoy, params 3 and 1 get merged to form a message. The result is the same as in the line above.
	 *
	 * @param message The test description/message. This is usually the only parameter specified when calling this method.
	 * 
	 * @param instanceOfCallStack an internal JSUnit call stack. Use null for this if you want to get to the next optional parameter. Usually not specified.
	 *
	 */
	public void js_fail(String message, Object instanceOfCallStack)
	{
	}

	/**
	 * Fails a test. AssertionFailedError is always thrown.
	 *
	 * @sample
	 * // Fails a test. AssertionFailedError is always thrown.
	 * jsunit.fail("Fail test");
	 * jsunit.fail("test", null, "Fail"); // 2nd param is not used in Servoy, params 3 and 1 get merged to form a message. The result is the same as in the line above.
	 *
	 * @param message The test description/message. This is usually the only parameter specified when calling this method.
	 * 
	 * @param instanceOfCallStack an internal JSUnit call stack. Use null for this if you want to get to the next optional parameter. Usually not specified.
	 *
	 * @param userMessage optional an user message. Usually not specified.
	 */
	public void js_fail(String message, Object instanceOfCallStack, String userMessage)
	{
	}


	@Override
	public String toString()
	{
		return "JSUnit[Standard assert functions]"; //$NON-NLS-1$
	}

}
