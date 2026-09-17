/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2020 Servoy BV

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

package com.servoy.j2db.server.ngclient.property;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeDate;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.sablo.InMemPackageReader;
import org.sablo.WebComponent;
import org.sablo.specification.Package.IPackageReader;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;

import com.servoy.j2db.server.ngclient.component.RhinoConversion;
import com.servoy.j2db.server.ngclient.property.types.Types;

/**
 * @author Diana
 *
 */
public class RhinoDefaultConversionsTest extends Log4JToConsoleTest
{

	private static final String DEFAULT_CONVERSIONS_PROP = "defaultConversionsProp";

	private ScriptableObject someRhinoScope;
	private Context rhinoContext;
	private WebComponent component;
	private PropertyDescription objectPD;

	@BeforeEach
	public void setUp() throws Exception
	{
		Types.getTypesInstance().registerTypes();

		InputStream is = getClass().getResourceAsStream("PropertyTests.manifest");
		byte[] bytes = new byte[is.available()];
		is.read(bytes);
		String manifest = new String(bytes);
		is.close();

		is = getClass().getResourceAsStream("mycomponent.spec");
		bytes = new byte[is.available()];
		is.read(bytes);
		String comp = new String(bytes);
		is.close();

		HashMap<String, String> components = new HashMap<>();
		components.put("mycomponent.spec", comp);
		WebComponentSpecProvider.init(new IPackageReader[] { new InMemPackageReader(manifest, components) }, null);

		rhinoContext = Context.enter();
		rhinoContext.setOptimizationLevel(-1);
		someRhinoScope = rhinoContext.initStandardObjects();

		component = new WebComponent("mycomponent", "testComponentName");
		objectPD = component.getSpecification().getProperty(DEFAULT_CONVERSIONS_PROP);
	}

	@AfterEach
	public void tearDown()
	{
		WebComponentSpecProvider.disposeInstance();
		Context.exit();
	}

	@Test
	public void testSimpleValuesDefaultConversions()
	{
		Object rhinoVal, javaVal;

		Date date = new Date();

		// DATE -----------------------

		// toRhino
		rhinoVal = RhinoConversion.defaultToRhino(date, objectPD, component, someRhinoScope);
		someRhinoScope.put("a", someRhinoScope, rhinoVal);
		assertSame(date, rhinoVal, "Date does not get translated to native rhino date by conversion as Rhino will do that automatically later");
		rhinoVal = ScriptRuntime.toObject(rhinoContext, someRhinoScope, rhinoVal);
		assertSame(NativeDate.class, rhinoVal.getClass(), "Now date - gets translated to native rhino date by internal Rhino code");
		assertEquals(Double.valueOf(date.getTime()), rhinoContext.evaluateString(someRhinoScope, "a.getTime()", "dummy js file name from junit tests", 0, null),
			"NativeDate and Date should represent the same moment in time");

		// fromRhino
		javaVal = RhinoConversion.defaultFromRhino(rhinoVal);
		assertSame(Date.class, javaVal.getClass(), "Converted to and from date is again a java date");
		assertEquals(date, javaVal, "Converted to and from date is equal to original");


		// Nulls / undefined  -----------------------

		// toRhino
		assertNull(RhinoConversion.defaultToRhino(null, objectPD, component, someRhinoScope), "Null to rhino null");
		assertNull(RhinoConversion.defaultToRhino(JSONObject.NULL, objectPD, component, someRhinoScope), "JSONObject.NULL to rhino null");

		// fromRhino
		assertNull(RhinoConversion.defaultFromRhino(Scriptable.NOT_FOUND), "Scriptable.NOT_FOUND to java null");
		assertNull(RhinoConversion.defaultFromRhino(Undefined.instance), "Undefined.instance to java null");
		assertNull(RhinoConversion.defaultFromRhino(Undefined.SCRIPTABLE_UNDEFINED), "Undefined.SCRIPTABLE_UNDEFINED to java null");
	}

	private Object toRhinoPlusRhinoInternalThing(Object sabloVal)
	{
		return ScriptRuntime.toObject(rhinoContext, someRhinoScope, RhinoConversion.defaultToRhino(sabloVal, objectPD, component, someRhinoScope));
	}

	@Test
	public void testSimpleObjectAndMap()
	{
		Object rhinoVal, javaVal;

		HashMap<String, Object> map = new HashMap<>();
		map.put("key1", "Just some text");
		map.put("key2", 456);
		Date date = new Date();
		map.put("key3", date);

		rhinoVal = toRhinoPlusRhinoInternalThing(map);
		someRhinoScope.put("a", someRhinoScope, rhinoVal);
		assertTrue(NativeObject.class.isAssignableFrom(rhinoVal.getClass()), "Map ends up as a native object in js");
		assertEquals("Just some text", rhinoContext.evaluateString(someRhinoScope, "a.key1", "dummy js file name from junit tests", 0, null),
			"Check key1 in Rhino");
		assertEquals(Integer.valueOf(456), rhinoContext.evaluateString(someRhinoScope, "a.key2", "dummy js file name from junit tests", 0, null),
			"Check key2 in Rhino");
		assertEquals(date, rhinoContext.evaluateString(someRhinoScope, "a.key3", "dummy js file name from junit tests", 0, null), "Check key3 in Rhino");
		assertEquals(Undefined.instance, rhinoContext.evaluateString(someRhinoScope, "a.inexistent", "dummy js file name from junit tests", 0, null),
			"Check inexistent key in Rhino");

		// from Rhino

		// from for what was previously to
		javaVal = RhinoConversion.defaultFromRhino(rhinoVal);
		assertTrue(javaVal instanceof Map, "From rhino should be a Map");
		assertEquals("Just some text", ((Map)javaVal).get("key1"), "Check key1 in java");
		assertEquals(Integer.valueOf(456), ((Map)javaVal).get("key2"), "Check key2 in java");
		assertEquals(date, ((Map)javaVal).get("key3"), "Check key3 in java");
		assertNull(((Map)javaVal).get("inexistent"), "Check inexistent in java");


		// from for new native object in Rhino
		javaVal = RhinoConversion.defaultFromRhino(
			rhinoContext.evaluateString(someRhinoScope, "(function z() { return { key5: 'aha', key6: 475, key7: false, key8: new Date() } }) ()",
				"dummy js file name from junit tests", 0, null));
		assertTrue(javaVal instanceof Map, "From rhino should be a Map");
		assertEquals("aha", ((Map)javaVal).get("key5"), "Check key5 in java");
		assertEquals(Double.valueOf(475), ((Map)javaVal).get("key6"), "Check key6 in java");
		assertEquals(false, ((Map)javaVal).get("key7"), "Check key7 in java");
		assertTrue(((Map)javaVal).get("key8") instanceof Date, "Check key8 in java");
		assertNull(((Map)javaVal).get("inexistent"), "Check inexistent in java");
	}

	@Test
	public void testSimpleArray()
	{
		Object rhinoVal, javaVal;

		ArrayList<Object> array = new ArrayList<>();
		array.add("Just some text");
		array.add(456);
		Date date = new Date();
		array.add(date);
		array.add(false);

		rhinoVal = toRhinoPlusRhinoInternalThing(array);
		someRhinoScope.put("a", someRhinoScope, rhinoVal);
		assertTrue(NativeArray.class.isAssignableFrom(rhinoVal.getClass()), "List ends up as a native array in js");
		assertEquals("Just some text", rhinoContext.evaluateString(someRhinoScope, "a[0]", "dummy js file name from junit tests", 0, null),
			"Check index 0 in Rhino");
		assertEquals(Integer.valueOf(456), rhinoContext.evaluateString(someRhinoScope, "a[1]", "dummy js file name from junit tests", 0, null),
			"Check index 1 in Rhino");
		assertEquals(date, rhinoContext.evaluateString(someRhinoScope, "a[2]", "dummy js file name from junit tests", 0, null), "Check index 2 in Rhino");
		assertEquals(false, rhinoContext.evaluateString(someRhinoScope, "a[3]", "dummy js file name from junit tests", 0, null), "Check index 3 in Rhino");
		assertEquals(Undefined.instance, rhinoContext.evaluateString(someRhinoScope, "a[4]", "dummy js file name from junit tests", 0, null),
			"Check inexistent index in Rhino");
		assertEquals(Double.valueOf(4), rhinoContext.evaluateString(someRhinoScope, "a.length", "dummy js file name from junit tests", 0, null),
			"Check length in Rhino");
		assertEquals("[\"Just some text\",456,false]",
			rhinoContext.evaluateString(someRhinoScope, "removed = a.splice(2, 1); JSON.stringify(a)", "dummy js file name from junit tests", 0, null),
			"Check splice to delete one item in Rhino - make sure it's a correct Rhino array");
		rhinoContext.evaluateString(someRhinoScope, "a.splice(2, 0, removed[0])", "dummy js file name from junit tests", 0, null);

		// from Rhino

		// from for what was previously to
		javaVal = RhinoConversion.defaultFromRhino(rhinoVal);
		assertTrue(javaVal instanceof List, "From rhino should be a List");
		assertEquals("Just some text", ((List)javaVal).get(0), "Check index 0 in java");
		assertEquals(Integer.valueOf(456), ((List)javaVal).get(1), "Check index 1 in java");
		assertEquals(date, ((List)javaVal).get(2), "Check index 2 in java");
		assertEquals(false, ((List)javaVal).get(3), "Check index 3 in java");
		assertEquals(4, ((List)javaVal).size(), "Check length in java");


		// from for new native object in Rhino
		javaVal = RhinoConversion
			.defaultFromRhino(rhinoContext.evaluateString(someRhinoScope,
				"(function z() { var g = new Array(); g.push('aha'); g.push(475); g.push(removed[0]); g.push(true); return g; }) ()",
				"dummy js file name from junit tests", 0,
				null));
		assertTrue(javaVal instanceof List, "From rhino should be a List");
		assertEquals("aha", ((List)javaVal).get(0), "Check index 0 in java");
		assertEquals(Double.valueOf(475), ((List)javaVal).get(1), "Check index 1 in java");
		assertEquals(date, ((List)javaVal).get(2), "Check index 2 in java");
		assertEquals(true, ((List)javaVal).get(3), "Check index 3 in java");
		assertEquals(4, ((List)javaVal).size(), "Check length in java");

		javaVal = RhinoConversion
			.defaultFromRhino(rhinoContext.evaluateString(someRhinoScope,
				"(function z() { return ['aha', 475, removed[0], true]; }) ()",
				"dummy js file name from junit tests", 0,
				null));
		assertTrue(javaVal instanceof List, "From rhino should be a List");
		assertEquals("aha", ((List)javaVal).get(0), "Check index 0 in java");
		assertEquals(Double.valueOf(475), ((List)javaVal).get(1), "Check index 1 in java");
		assertEquals(date, ((List)javaVal).get(2), "Check index 2 in java");
		assertEquals(true, ((List)javaVal).get(3), "Check index 3 in java");
		assertEquals(4, ((List)javaVal).size(), "Check length in java");
	}

	@Test
	public void testArrayAddValueNewValue()
	{
		Object rhinoVal, javaVal;

		ArrayList<Object> array = new ArrayList<>(10);
		array.add("Just some text");
		array.add(456);
		Date date = new Date();
		array.add(date);
		array.add(false);

		rhinoVal = toRhinoPlusRhinoInternalThing(array);
		someRhinoScope.put("a", someRhinoScope, rhinoVal);
		assertTrue(NativeArray.class.isAssignableFrom(rhinoVal.getClass()), "List ends up as a native array in js");
		assertEquals("Just some text", rhinoContext.evaluateString(someRhinoScope, "a[0]", "dummy js file name from junit tests", 0, null),
			"Check index 0 in Rhino");
		assertEquals(Integer.valueOf(456), rhinoContext.evaluateString(someRhinoScope, "a[1]", "dummy js file name from junit tests", 0, null),
			"Check index 1 in Rhino");
		assertEquals(date, rhinoContext.evaluateString(someRhinoScope, "a[2]", "dummy js file name from junit tests", 0, null), "Check index 2 in Rhino");
		assertEquals(false, rhinoContext.evaluateString(someRhinoScope, "a[3]", "dummy js file name from junit tests", 0, null), "Check index 3 in Rhino");
		assertEquals(Undefined.instance, rhinoContext.evaluateString(someRhinoScope, "a[4]", "dummy js file name from junit tests", 0, null),
			"Check inexistent index in Rhino");
		assertEquals(Double.valueOf(4), rhinoContext.evaluateString(someRhinoScope, "a.length", "dummy js file name from junit tests", 0, null),
			"Check length in Rhino");

		rhinoContext.evaluateString(someRhinoScope, "a[8] = \"are you ok?\"; a[15] = 2589; ", "dummy js file name from junit tests", 0, null);

		// from for what was previously to
		javaVal = RhinoConversion.defaultFromRhino(rhinoVal);
		assertTrue(javaVal instanceof List, "From rhino should be a List");
		assertEquals("Just some text", ((List)javaVal).get(0), "Check index 0 in java");
		assertEquals(Integer.valueOf(456), ((List)javaVal).get(1), "Check index 1 in java");
		assertEquals(date, ((List)javaVal).get(2), "Check index 2 in java");
		assertEquals(false, ((List)javaVal).get(3), "Check index 3 in java");
		assertEquals(null, ((List)javaVal).get(4), "Check index 4 in java");
		assertEquals(null, ((List)javaVal).get(5), "Check index 5 in java");
		assertEquals(null, ((List)javaVal).get(6), "Check index 6 in java");
		assertEquals(null, ((List)javaVal).get(7), "Check index 7 in java");
		assertEquals("are you ok?", ((List)javaVal).get(8), "Check index 8 in java");
		assertEquals(null, ((List)javaVal).get(9), "Check index 9 in java");
		assertEquals(null, ((List)javaVal).get(10), "Check index 10 in java");
		assertEquals(null, ((List)javaVal).get(11), "Check index 11 in java");
		assertEquals(null, ((List)javaVal).get(12), "Check index 12 in java");
		assertEquals(null, ((List)javaVal).get(13), "Check index 13 in java");
		assertEquals(null, ((List)javaVal).get(14), "Check index 14 in java");
		assertEquals(2589.0, ((List)javaVal).get(15), "Check index 15 in java");
		assertEquals(16, ((List)javaVal).size(), "Check length in java");
	}


	@Test
	public void testMapWithNumberKeys()
	{
		Object[] array = new Object[] { "Hello World", 4, 3.5, true, "Dis is a longe string", 35.983564, false };
		Map<Object, Object> map = new HashMap<Object, Object>();
		int arrayLength = array.length;
		for (int index = 0; index < arrayLength; index++)
		{
			map.put(index, array[index]);
		}

		final Object convertedNativeObject = toRhinoPlusRhinoInternalThing(map);
		assertTrue(convertedNativeObject instanceof NativeObject);
		assertEquals("[object Object]", ((NativeObject)convertedNativeObject).toString());
		assertEquals("[0=Hello World, 1=4, 2=3.5, 3=true, 4=Dis is a longe string, 5=35.983564, 6=false]",
			((NativeObject)convertedNativeObject).entrySet().toString());

		final Object convertedMap = RhinoConversion.defaultFromRhino(convertedNativeObject);
		assertTrue(convertedMap instanceof Map);
		assertEquals("{0=Hello World, 1=4, 2=3.5, 3=true, 4=Dis is a longe string, 5=35.983564, 6=false}", convertedMap.toString());
		assertEquals(map.toString(), convertedMap.toString());
	}

	@Test
	public void testSimpleJSONObjectAndObject()
	{
		Object rhinoVal, javaVal;

		JSONObject jsonObj = new JSONObject();
		jsonObj.put("key1", "Just some text");
		jsonObj.put("key2", 456);
		jsonObj.put("key3", false);

		rhinoVal = toRhinoPlusRhinoInternalThing(jsonObj);
		someRhinoScope.put("a", someRhinoScope, rhinoVal);
		assertTrue(NativeObject.class.isAssignableFrom(rhinoVal.getClass()), "Map ends up as a native object in js");
		assertEquals("Just some text", rhinoContext.evaluateString(someRhinoScope, "a.key1", "dummy js file name from junit tests", 0, null),
			"Check key1 in Rhino");
		assertEquals(Integer.valueOf(456), rhinoContext.evaluateString(someRhinoScope, "a.key2", "dummy js file name from junit tests", 0, null),
			"Check key2 in Rhino");
		assertEquals(false, rhinoContext.evaluateString(someRhinoScope, "a.key3", "dummy js file name from junit tests", 0, null), "Check key3 in Rhino");
		assertEquals(Undefined.instance, rhinoContext.evaluateString(someRhinoScope, "a.inexistent", "dummy js file name from junit tests", 0, null),
			"Check inexistent key in Rhino");

		// from Rhino

		// from for what was previously to
		javaVal = RhinoConversion.defaultFromRhino(rhinoVal);
		assertTrue(javaVal instanceof Map, "From rhino should be a Map");
		assertEquals("Just some text", ((Map)javaVal).get("key1"), "Check key1 in java");
		assertEquals(Integer.valueOf(456), ((Map)javaVal).get("key2"), "Check key2 in java");
		assertEquals(false, ((Map)javaVal).get("key3"), "Check key3 in java");
		assertNull(((Map)javaVal).get("inexistent"), "Check inexistent in java");
	}

	@Test
	public void testSimpleJSONArray()
	{
		Object rhinoVal, javaVal;

		JSONArray jsonArray = new JSONArray();
		jsonArray.put("Just some text");
		jsonArray.put(456);
		jsonArray.put(false);

		rhinoVal = toRhinoPlusRhinoInternalThing(jsonArray);
		someRhinoScope.put("a", someRhinoScope, rhinoVal);
		assertTrue(NativeArray.class.isAssignableFrom(rhinoVal.getClass()), "List ends up as a native array in js");
		assertEquals("Just some text", rhinoContext.evaluateString(someRhinoScope, "a[0]", "dummy js file name from junit tests", 0, null),
			"Check index 0 in Rhino");
		assertEquals(Integer.valueOf(456), rhinoContext.evaluateString(someRhinoScope, "a[1]", "dummy js file name from junit tests", 0, null),
			"Check index 1 in Rhino");
		assertEquals(false, rhinoContext.evaluateString(someRhinoScope, "a[2]", "dummy js file name from junit tests", 0, null), "Check index 2 in Rhino");
		assertEquals(Undefined.instance, rhinoContext.evaluateString(someRhinoScope, "a[3]", "dummy js file name from junit tests", 0, null),
			"Check inexistent index in Rhino");
		assertEquals(Double.valueOf(3), rhinoContext.evaluateString(someRhinoScope, "a.length", "dummy js file name from junit tests", 0, null),
			"Check length in Rhino");
		assertEquals("[\"Just some text\",456]",
			rhinoContext.evaluateString(someRhinoScope, "removed = a.splice(2, 1); JSON.stringify(a)", "dummy js file name from junit tests", 0, null),
			"Check splice to delete one item in Rhino - make sure it's a correct Rhino array");
		rhinoContext.evaluateString(someRhinoScope, "a.splice(2, 0, removed[0])", "dummy js file name from junit tests", 0, null);

		// from Rhino

		// from for what was previously to
		javaVal = RhinoConversion.defaultFromRhino(rhinoVal);
		assertTrue(javaVal instanceof List, "From rhino should be a List");
		assertEquals("Just some text", ((List)javaVal).get(0), "Check index 0 in java");
		assertEquals(Integer.valueOf(456), ((List)javaVal).get(1), "Check index 1 in java");
		assertEquals(false, ((List)javaVal).get(2), "Check index 2 in java");
		assertEquals(3, ((List)javaVal).size(), "Check length in java");
	}


	@Test
	public void testMapWithArrayAndDateAndTestChanges()
	{
		Date date = new Date();
		List array = new ArrayList<Object>(Arrays.asList("Hello World", 4, 3.5, true, "Dis is a longe string", 35.983564, false, date));
		Map<Object, Object> map = new HashMap<Object, Object>();
		map.put("childArray", array);
		map.put("childDate", date);
		map.put("childString", "Some string");

		component.setProperty(DEFAULT_CONVERSIONS_PROP, map);
		Object rhinoVal = toRhinoPlusRhinoInternalThing(map);

		someRhinoScope.put("a", someRhinoScope, rhinoVal);
		assertTrue(NativeObject.class.isAssignableFrom(rhinoVal.getClass()), "Map ends up as a native object in js");
		Object ra = rhinoContext.evaluateString(someRhinoScope, "a.childArray", "dummy js file name from junit tests", 0, null);
		assertTrue(NativeArray.class
			.isAssignableFrom(ra.getClass()), "List ends up as a native array in js");
		for (int i = 0; i < array.size(); i++)
		{
			assertEquals(array.get(i), rhinoContext.evaluateString(someRhinoScope, "a.childArray[" + i + "]", "dummy js file name from junit tests", 0, null),
				"Check a.childArray[" + i + "] in Rhino");
		}
		assertEquals(date, rhinoContext.evaluateString(someRhinoScope, "a.childDate", "dummy js file name from junit tests", 0, null),
			"Check childDate in Rhino");
		assertEquals("Some string", rhinoContext.evaluateString(someRhinoScope, "a.childString", "dummy js file name from junit tests", 0, null),
			"Check childString in Rhino");

		// ok now change the date from rhino obj. and see that we get notified and the sablo value gets updated
		rhinoContext.evaluateString(someRhinoScope, "a.childDate = new Date(1990, 6, 15);", "dummy js file name from junit tests", 0, null);
		assertTrue(date.getTime() > ((Date)map.get("childDate")).getTime(), "Check childDate change from Rhino");
		assertTrue(component.hasChanges(), "We changed the date just now in the map; component should know it has changed");
		assertTrue(component.getAndClearChanges().content.containsKey(DEFAULT_CONVERSIONS_PROP), "Component should know map changed"); // also clears changes
		assertFalse(component.hasChanges(), "Now it no longer has changes");

		// ok now delete the string from rhino obj. and see that we get notified and the sablo value gets updated
		rhinoContext.evaluateString(someRhinoScope, "delete a.childString;", "dummy js file name from junit tests", 0, null);
		assertFalse(map.containsKey("childString"), "Check childString delete from Rhino");
		assertTrue(component.hasChanges(), "We changed the date just now in the map; component should know it has changed");
		assertTrue(component.getAndClearChanges().content.containsKey(DEFAULT_CONVERSIONS_PROP), "Component should know map changed"); // also clears changes
		assertFalse(component.hasChanges(), "Now it no longer has changes");

		// ok now change the date inside the array inside the object (rhino) and see if it is reflected in sablo
		rhinoContext.evaluateString(someRhinoScope, "a.childArray[7] = new Date(1990, 6, 15);", "dummy js file name from junit tests", 0, null);
		assertTrue(date.getTime() > ((Date)((List)map.get("childArray")).get(7)).getTime(), "Check date from array change from Rhino");
		assertTrue(component.hasChanges(), "We changed the date just now in the array; component should know it has changed");
		assertTrue(component.getAndClearChanges().content.containsKey(DEFAULT_CONVERSIONS_PROP), "Component should know map changed"); // also clears changes
		assertFalse(component.hasChanges(), "Now it no longer has changes");

		// ok now add/remove (splice) from array inside the object (rhino) and see if it is reflected in sablo
		rhinoContext.evaluateString(someRhinoScope, "a.childArray.splice(2, 2, \"aha\")", "dummy js file name from junit tests", 0, null);
		assertEquals("aha", ((List)map.get("childArray")).get(2), "Check childDate of array change from Rhino");
		assertEquals(7, ((List)map.get("childArray")).size(), "New length of array");
		assertTrue(component.hasChanges(), "We changed the date just now in the array; component should know it has changed");
		assertTrue(component.getAndClearChanges().content.containsKey(DEFAULT_CONVERSIONS_PROP), "Component should know map changed"); // also clears changes
		assertFalse(component.hasChanges(), "Now it no longer has changes");
	}

	@Test
	public void testJSONObjectWithJSONArrayAndDateAndTestChanges()
	{
		JSONArray jsonArray = new JSONArray();
		jsonArray.put("Hello World");
		jsonArray.put(4);
		jsonArray.put(3.5);
		jsonArray.put(true);
		jsonArray.put("Dis is a longe string");
		jsonArray.put(35.983564);
		jsonArray.put(false);

		JSONObject jsonObject = new JSONObject();
		jsonObject.put("childArray", jsonArray);
		jsonObject.put("childNumber", 14);
		jsonObject.put("childString", "some string");

		component.setProperty(DEFAULT_CONVERSIONS_PROP, jsonObject);
		Object rhinoVal = toRhinoPlusRhinoInternalThing(jsonObject);

		someRhinoScope.put("a", someRhinoScope, rhinoVal);
		assertTrue(NativeObject.class.isAssignableFrom(rhinoVal.getClass()), "Map ends up as a native object in js");
		Object ra = rhinoContext.evaluateString(someRhinoScope, "a.childArray", "dummy js file name from junit tests", 0, null);
		assertTrue(NativeArray.class
			.isAssignableFrom(ra.getClass()), "List ends up as a native array in js");
		for (int i = 0; i < jsonArray.length(); i++)
		{
			assertEquals(jsonArray.get(i),
				rhinoContext.evaluateString(someRhinoScope, "a.childArray[" + i + "]", "dummy js file name from junit tests", 0, null),
				"Check a.childArray[" + i + "] in Rhino");
		}
		assertEquals(14, rhinoContext.evaluateString(someRhinoScope, "a.childNumber", "dummy js file name from junit tests", 0, null),
			"Check childNumber in Rhino");
		assertEquals("some string", rhinoContext.evaluateString(someRhinoScope, "a.childString", "dummy js file name from junit tests", 0, null),
			"Check childString in Rhino");

		// ok now change the date from rhino and see that we get notified and the sablo value gets updated
		rhinoContext.evaluateString(someRhinoScope, "a.childNumber = 543;", "dummy js file name from junit tests", 0, null);
		assertEquals(543.0, jsonObject.get("childNumber"), "Check childNumber change from Rhino");
		assertTrue(component.hasChanges(), "We changed the date just now in the map; component should know it has changed");
		assertTrue(component.getAndClearChanges().content.containsKey(DEFAULT_CONVERSIONS_PROP), "Component should know map changed"); // also clears changes
		assertFalse(component.hasChanges(), "Now it no longer has changes");

		// ok now delete the string from rhino obj. and see that we get notified and the sablo value gets updated
		rhinoContext.evaluateString(someRhinoScope, "delete a.childString;", "dummy js file name from junit tests", 0, null);
		assertFalse(jsonObject.has("childString"), "Check childString delete from Rhino");
		assertTrue(component.hasChanges(), "We changed the date just now in the map; component should know it has changed");
		assertTrue(component.getAndClearChanges().content.containsKey(DEFAULT_CONVERSIONS_PROP), "Component should know map changed"); // also clears changes
		assertFalse(component.hasChanges(), "Now it no longer has changes");


		// ok now change something in the array and see that it does get updated and that it is marked as changed
		rhinoContext.evaluateString(someRhinoScope, "a.childArray[5] = 987;", "dummy js file name from junit tests", 0, null);
		assertEquals(987.0, ((JSONArray)jsonObject.get("childArray")).get(5), "Check childNumber of array change from Rhino");
		assertTrue(component.hasChanges(), "We changed the date just now in the map; component should know it has changed");
		assertTrue(component.getAndClearChanges().content.containsKey(DEFAULT_CONVERSIONS_PROP), "Component should know map changed"); // also clears changes
		assertFalse(component.hasChanges(), "Now it no longer has changes");

		// ok now add/remove (splice) from array inside the object (rhino) and see if it is reflected in sablo
		rhinoContext.evaluateString(someRhinoScope, "a.childArray.splice(2, 2, \"aha\")", "dummy js file name from junit tests", 0, null);
		assertEquals("\"aha\"", ((JSONArray)jsonObject.get("childArray")).get(2), "Check childDate of array change from Rhino");
		assertEquals(6, ((JSONArray)jsonObject.get("childArray")).length(), "New length of array");
		assertTrue(component.hasChanges(), "We changed the date just now in the array; component should know it has changed");
		assertTrue(component.getAndClearChanges().content.containsKey(DEFAULT_CONVERSIONS_PROP), "Component should know map changed"); // also clears changes
		assertFalse(component.hasChanges(), "Now it no longer has changes");
	}

	@Test
	public void testChangesFromRhinoWhenMapObjectPropIsNotDirectlyOnComponentButInCustomObjOrArray()
	{
		PropertyDescription mytype007PD = component.getSpecification().getProperty("objectT");
		Map objectTValue = new HashMap();
		Map defaultConversionsSubPropValue = new HashMap();
		objectTValue.put("defaultConversionsSubProp", defaultConversionsSubPropValue);
		defaultConversionsSubPropValue.put("someSubKey", 12);

		component.setProperty("objectT", objectTValue); // this should directly convert it into a ChangeAwareMap
		objectTValue = (Map)component.getRawPropertyValue("objectT"); // so get the change aware map
		component.getAndClearChanges();

		Object rhinoVal = ScriptRuntime.toObject(rhinoContext, someRhinoScope,
			RhinoConversion.defaultToRhino(objectTValue, mytype007PD, component, someRhinoScope));
		someRhinoScope.put("a", someRhinoScope, rhinoVal);

		assertEquals(12, rhinoContext.evaluateString(someRhinoScope, "a.defaultConversionsSubProp.someSubKey;", "dummy js file name from junit tests", 0, null),
			"Check to see that customType.objectType.someSubKey is correct");

		// change sub-key of 'object' type inside custom type
		rhinoContext.evaluateString(someRhinoScope, "a.defaultConversionsSubProp.someSubKey = 15;", "dummy js file name from junit tests", 0, null);
		assertEquals(15.0,
			rhinoContext.evaluateString(someRhinoScope, "a.defaultConversionsSubProp.someSubKey;", "dummy js file name from junit tests", 0, null),
			"Check to see that customType.objectType.someSubKey is correct");

		// now the component should be aware that it's objectT/defaultConversionsSubProp has changes
		assertTrue(component.hasChanges(), "We changed 'object' prop; component should know it has changed");
		assertTrue(component.getAndClearChanges().content.containsKey("objectT"), "Component should know 'object' prop changed"); // also clears changes
		assertFalse(component.hasChanges(), "Now it no longer has changes");
	}


	@Test
	public void testChangesFromRhinoWhenJSONObjectObjectPropIsNotDirectlyOnComponentButInCustomObjOrArray()
	{
		PropertyDescription mytype007PD = component.getSpecification().getProperty("objectT");
		Map objectTValue = new HashMap();
		JSONObject defaultConversionsSubPropValue = new JSONObject();
		objectTValue.put("defaultConversionsSubProp", defaultConversionsSubPropValue);
		defaultConversionsSubPropValue.put("someSubKey", 12);

		component.setProperty("objectT", objectTValue); // this should directly convert it into a ChangeAwareMap
		objectTValue = (Map)component.getRawPropertyValue("objectT"); // so get the change aware map
		component.getAndClearChanges();

		Object rhinoVal = ScriptRuntime.toObject(rhinoContext, someRhinoScope,
			RhinoConversion.defaultToRhino(objectTValue, mytype007PD, component, someRhinoScope));
		someRhinoScope.put("a", someRhinoScope, rhinoVal);

		assertEquals(12, rhinoContext.evaluateString(someRhinoScope, "a.defaultConversionsSubProp.someSubKey;", "dummy js file name from junit tests", 0, null),
			"Check to see that customType.objectType.someSubKey is correct");

		// change sub-key of 'object' type inside custom type
		rhinoContext.evaluateString(someRhinoScope, "a.defaultConversionsSubProp.someSubKey = 15;", "dummy js file name from junit tests", 0, null);
		assertEquals(15.0,
			rhinoContext.evaluateString(someRhinoScope, "a.defaultConversionsSubProp.someSubKey;", "dummy js file name from junit tests", 0, null),
			"Check to see that customType.objectType.someSubKey is correct");

		// now the component should be aware that it's objectT/defaultConversionsSubProp has changes
		assertTrue(component.hasChanges(), "We changed the 'object' prop; component should know it has changed");
		assertTrue(component.getAndClearChanges().content.containsKey("objectT"), "Component should know 'object' prop changed"); // also clears changes
		assertFalse(component.hasChanges(), "Now it no longer has changes");
	}

	@Test
	public void testChangesFromRhinoWhenListObjectPropIsNotDirectlyOnComponentButInCustomObjOrArray()
	{
		PropertyDescription mytype007PD = component.getSpecification().getProperty("objectT");
		Map objectTValue = new HashMap();
		List defaultConversionsSubPropValue = new ArrayList();
		objectTValue.put("defaultConversionsSubProp", defaultConversionsSubPropValue);
		defaultConversionsSubPropValue.add(12);

		component.setProperty("objectT", objectTValue); // this should directly convert it into a ChangeAwareMap
		objectTValue = (Map)component.getRawPropertyValue("objectT"); // so get the change aware map
		component.getAndClearChanges();

		Object rhinoVal = ScriptRuntime.toObject(rhinoContext, someRhinoScope,
			RhinoConversion.defaultToRhino(objectTValue, mytype007PD, component, someRhinoScope));
		someRhinoScope.put("a", someRhinoScope, rhinoVal);

		assertEquals(12, rhinoContext.evaluateString(someRhinoScope, "a.defaultConversionsSubProp[0];", "dummy js file name from junit tests", 0, null),
			"Check to see that customType.objectType.someSubKey is correct");

		// change sub-key of 'object' type inside custom type
		rhinoContext.evaluateString(someRhinoScope, "a.defaultConversionsSubProp[0] = 15;", "dummy js file name from junit tests", 0, null);
		assertEquals(15.0, rhinoContext.evaluateString(someRhinoScope, "a.defaultConversionsSubProp[0];", "dummy js file name from junit tests", 0, null),
			"Check to see that customType.objectType.someSubKey is correct");

		// now the component should be aware that it's objectT/defaultConversionsSubProp has changes
		assertTrue(component.hasChanges(), "We changed the 'object' prop; component should know it has changed");
		assertTrue(component.getAndClearChanges().content.containsKey("objectT"), "Component should know 'object' prop changed"); // also clears changes
		assertFalse(component.hasChanges(), "Now it no longer has changes");
	}

	@Test
	public void testChangesFromRhinoWhenJSONArrayObjectPropIsNotDirectlyOnComponentButInCustomObjOrArray()
	{
		PropertyDescription mytype007PD = component.getSpecification().getProperty("objectT");
		Map objectTValue = new HashMap();
		JSONArray defaultConversionsSubPropValue = new JSONArray();
		objectTValue.put("defaultConversionsSubProp", defaultConversionsSubPropValue);
		defaultConversionsSubPropValue.put(12);

		component.setProperty("objectT", objectTValue); // this should directly convert it into a ChangeAwareMap
		objectTValue = (Map)component.getRawPropertyValue("objectT"); // so get the change aware map
		component.getAndClearChanges();

		Object rhinoVal = ScriptRuntime.toObject(rhinoContext, someRhinoScope,
			RhinoConversion.defaultToRhino(objectTValue, mytype007PD, component, someRhinoScope));
		someRhinoScope.put("a", someRhinoScope, rhinoVal);

		assertEquals(12, rhinoContext.evaluateString(someRhinoScope, "a.defaultConversionsSubProp[0];", "dummy js file name from junit tests", 0, null),
			"Check to see that customType.objectType.someSubKey is correct");

		// change sub-key of 'object' type inside custom type
		rhinoContext.evaluateString(someRhinoScope, "a.defaultConversionsSubProp[0] = 15;", "dummy js file name from junit tests", 0, null);
		assertEquals(15.0, rhinoContext.evaluateString(someRhinoScope, "a.defaultConversionsSubProp[0];", "dummy js file name from junit tests", 0, null),
			"Check to see that customType.objectType.someSubKey is correct");

		// now the component should be aware that it's objectT/defaultConversionsSubProp has changes
		assertTrue(component.hasChanges(), "We changed something in the 'object' prop");
		assertTrue(component.getAndClearChanges().content.containsKey("objectT"), "Component should know 'object' prop changed"); // also clears changes
		assertFalse(component.hasChanges(), "Now it no longer has changes");
	}

	@Test
	public void testSetFromRhinoThenChangesFromRhinoOnSameInstanceWithoutGettingItBackFromProp()
	{
		Map sabloVal;
		// from for new native object in Rhino
		sabloVal = (Map)RhinoConversion.defaultFromRhino(
			rhinoContext.evaluateString(someRhinoScope, "var a; (function z() { a = { key5: 'aha', key6: 475 }; return a; }) ()",
				"dummy js file name from junit tests", 0,
				null));
		component.setProperty(DEFAULT_CONVERSIONS_PROP, sabloVal);

		assertEquals("aha", sabloVal.get("key5"), "Check key5 in java");
		assertEquals(Double.valueOf(475), sabloVal.get("key6"), "Check key6 in java");

		// TODO this is not currently supported (so it will fail similar to how custom object props or custom array props. fails as well in a similar scenario); see if it can be improved in the future; if it;s improved add similar tests for List, JSONArray, JSONObject as sablo values for 'object' typed property
		// change original rhino NativeObject and see if sablo value is updated and component is aware of changes
		rhinoContext.evaluateString(someRhinoScope, "a.key5 = 'changedVal';",
			"dummy js file name from junit tests", 0,
			null);
		//		assertEquals("changedVal", sabloVal.get("key5"), "Check key5 in java");
		//		assertTrue(component.hasChanges(), "We changed the date just now in the array; component should know it has changed");
		//		assertTrue(component.getAndClearChanges().content.containsKey(DEFAULT_CONVERSIONS_PROP), "Component should know map changed"); // also clears changes
		//		assertFalse(component.hasChanges(), "Now it no longer has changes");
	}

}
