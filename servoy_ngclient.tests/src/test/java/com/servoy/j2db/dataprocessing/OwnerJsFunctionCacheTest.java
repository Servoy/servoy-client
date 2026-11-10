/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2026 Servoy BV

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

package com.servoy.j2db.dataprocessing;

import static com.servoy.j2db.testutils.JsFunctionCacheProbe.findField;
import static com.servoy.j2db.testutils.JsFunctionCacheProbe.readCache;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import com.servoy.j2db.scripting.DefaultJavaScope.JsMethod;
import com.servoy.j2db.scripting.JSMenuItem;

/**
 * SVY-21334: the hand written owners of js functions that can be created without a client (dataset, menu item record, view record) keep
 * their function instances per owner, in a field that is <code>null</code> until the first function access.
 * <p>
 * Without the fix the functions were cached in a JVM wide static map and their parent was overwritten by the last owner that did a get,
 * so the cross owner assertions of {@link PerOwnerCache} fail.
 *
 * @author Servoy
 */
@SuppressWarnings("nls")
class OwnerJsFunctionCacheTest
{
	private Context cx;

	@BeforeEach
	void enterContext()
	{
		cx = Context.enter();
		cx.getWrapFactory().setJavaPrimitiveWrap(false);
	}

	@AfterEach
	void exitContext()
	{
		Context.exit();
	}

	private ScriptableObject newTopLevel()
	{
		return cx.initStandardObjects();
	}

	private static JsMethod anyMethod(Map<String, JsMethod> jsFunctions)
	{
		assertFalse(jsFunctions.isEmpty(), "owner class must have js functions");
		return jsFunctions.values().iterator().next();
	}

	static Stream<Arguments> owners()
	{
		Supplier<Scriptable> dataset = () -> new JSDataSet();
		Supplier<Scriptable> menuItemRecord = () -> new MenuItemRecord(new JSMenuItem(null, "item", null), new HashMap<String, Object>(), null);
		Supplier<Scriptable> viewRecord = () -> new ViewRecord(null, new String[0], new Object[0], null);
		return Stream.of(Arguments.of("JSDataSet", dataset, JSDataSet.jsFunctions), //
			Arguments.of("MenuItemRecord", menuItemRecord, MenuItemRecord.jsFunctions), //
			Arguments.of("ViewRecord", viewRecord, ViewRecord.jsFunctions));
	}

	@Nested
	class PerOwnerCache
	{
		@ParameterizedTest(name = "{0}")
		@MethodSource("com.servoy.j2db.dataprocessing.OwnerJsFunctionCacheTest#owners")
		@DisplayName("cache field is null on a new owner and stays null for non function lookups")
		void cacheIsNullInitially(String ownerName, Supplier<Scriptable> factory, @SuppressWarnings("unused") Map<String, JsMethod> jsFunctions)
			throws Exception
		{
			Scriptable owner = factory.get();

			assertNull(readCache(owner), ownerName + " must not allocate a function cache up front");
		}

		@Test
		@DisplayName("JSDataSet: cache field stays null for a lookup that is not a js function")
		void cacheStaysNullForNonFunctionLookup() throws Exception
		{
			JSDataSet ds = new JSDataSet();

			ds.get("notAJsFunctionName", ds);

			assertNull(readCache(ds));
		}

		@ParameterizedTest(name = "{0}")
		@MethodSource("com.servoy.j2db.dataprocessing.OwnerJsFunctionCacheTest#owners")
		@DisplayName("cache field is created on first function access and then kept")
		void cacheIsCreatedOnFirstFunctionAccess(String ownerName, Supplier<Scriptable> factory, Map<String, JsMethod> jsFunctions) throws Exception
		{
			Scriptable owner = factory.get();
			ScriptableObject top = newTopLevel();
			String name = anyMethod(jsFunctions).getName();

			Object function = owner.get(name, top);
			Object cache = readCache(owner);
			owner.get(name, top);

			assertInstanceOf(NativeJavaMethod.class, function);
			assertNotNull(cache, ownerName + " must have a function cache after the first function access");
			assertSame(cache, readCache(owner), "cache must be kept");
		}

		@ParameterizedTest(name = "{0}")
		@MethodSource("com.servoy.j2db.dataprocessing.OwnerJsFunctionCacheTest#owners")
		@DisplayName("same owner returns the same function with start as parent and Function.prototype of start as prototype")
		void sameOwnerSameFunction(String ownerName, Supplier<Scriptable> factory, Map<String, JsMethod> jsFunctions)
		{
			Scriptable owner = factory.get();
			ScriptableObject top = newTopLevel();
			String name = anyMethod(jsFunctions).getName();

			NativeJavaMethod first = assertInstanceOf(NativeJavaMethod.class, owner.get(name, top));
			NativeJavaMethod second = assertInstanceOf(NativeJavaMethod.class, owner.get(name, top));

			assertSame(first, second, ownerName + ": repeated access must return the same function");
			assertSame(top, first.getParentScope());
			assertSame(ScriptableObject.getFunctionPrototype(top), first.getPrototype());
		}

		@ParameterizedTest(name = "{0}")
		@MethodSource("com.servoy.j2db.dataprocessing.OwnerJsFunctionCacheTest#owners")
		@DisplayName("two owners get different functions and the first keeps its own parent (ticket reproduction)")
		void twoOwnersAreIsolated(String ownerName, Supplier<Scriptable> factory, Map<String, JsMethod> jsFunctions)
		{
			Scriptable ownerA = factory.get();
			Scriptable ownerB = factory.get();
			ScriptableObject topA = newTopLevel();
			ScriptableObject topB = newTopLevel();
			String name = anyMethod(jsFunctions).getName();

			NativeJavaMethod fncA = assertInstanceOf(NativeJavaMethod.class, ownerA.get(name, topA));
			NativeJavaMethod fncB = assertInstanceOf(NativeJavaMethod.class, ownerB.get(name, topB));
			assertSame(fncA, ownerA.get(name, topA), "owner A must keep returning its own function");
			assertNotSame(fncA, fncB, ownerName + ": functions of different owners must not be shared");
			assertSame(topA, fncA.getParentScope(), ownerName + ": parent of the first function must not be overwritten by the second owner");
			assertSame(topB, fncB.getParentScope());
			assertNotSame(fncA.getPrototype(), fncB.getPrototype());
		}
	}

	@Nested
	class DataSetScripting
	{
		@Test
		@DisplayName("dataset functions work through call/apply/bind/instanceof Function and keep working after a second dataset was accessed")
		void datasetFunctionsWork()
		{
			ScriptableObject top = newTopLevel();
			JSDataSet ds1 = new JSDataSet(null, 3, new String[] { "a", "b" });
			JSDataSet ds2 = new JSDataSet(null, 5, new String[] { "a", "b" });
			ds1.setParentScope(top);
			ds2.setParentScope(top);
			ScriptableObject.putProperty(top, "ds1", ds1);
			ScriptableObject.putProperty(top, "ds2", ds2);

			cx.evaluateString(top, "var fnc = ds1.getMaxRowIndex;", "test", 1, null);
			assertEquals(5, ((Number)cx.evaluateString(top, "ds2.getMaxRowIndex()", "test", 1, null)).intValue());

			assertEquals(3, ((Number)cx.evaluateString(top, "fnc.call(ds1)", "test", 1, null)).intValue());
			assertEquals(5, ((Number)cx.evaluateString(top, "fnc.call(ds2)", "test", 1, null)).intValue());
			assertEquals(3, ((Number)cx.evaluateString(top, "fnc.apply(ds1, [])", "test", 1, null)).intValue());
			assertEquals(3, ((Number)cx.evaluateString(top, "fnc.bind(ds1)()", "test", 1, null)).intValue());
			assertEquals(Boolean.TRUE, cx.evaluateString(top, "fnc instanceof Function", "test", 1, null));
			assertEquals("function", cx.evaluateString(top, "typeof ds1.getMaxRowIndex", "test", 1, null));
			assertSame(ds1, ((NativeJavaMethod)ScriptableObject.getProperty(top, "fnc")).getParentScope());
		}

		@Test
		@DisplayName("overloaded addRow resolves both overloads")
		void overloadedAddRowResolves()
		{
			ScriptableObject top = newTopLevel();
			JSDataSet ds = new JSDataSet(null, 0, new String[] { "a", "b" });
			ds.setParentScope(top);
			ScriptableObject.putProperty(top, "ds", ds);

			cx.evaluateString(top, "ds.addRow(['x', 'y']); ds.addRow(1, ['p', 'q']);", "test", 1, null);

			assertEquals(2, ds.js_getMaxRowIndex());
			assertEquals("p", ds.getValue(0, 0), "row inserted at index 1 by the (int, Object[]) overload");
			assertEquals("x", ds.getValue(1, 0), "row added at the end by the (Object[]) overload");
		}

		private JSDataSet datasetWithColumns(String... columns)
		{
			BufferedDataSet data = new BufferedDataSet(columns, new int[columns.length]);
			data.addRow(new Object[columns.length]);
			JSDataSet ds = new JSDataSet(data);
			ds.js_setRowIndex(1);
			assertEquals(1, ds.js_getRowIndex(), "a current row is needed for put to be able to store a value");
			return ds;
		}

		@Test
		@DisplayName("getIds lists the function names of the dataset")
		void idsContainFunctions()
		{
			JSDataSet ds = datasetWithColumns("a");

			List<Object> ids = Arrays.asList(ds.getIds());

			assertTrue(ids.containsAll(JSDataSet.jsFunctions.keySet()), "ids: " + ids);
			assertTrue(ids.contains("rowIndex"));
		}

		@Test
		@DisplayName("put of a column that is named like a js function is ignored, put of a normal column is stored")
		void putOfFunctionNamedColumnIsIgnored()
		{
			// the column names are lower cased by the dataset, so use a (lower case) function name for a column that put could really overwrite
			assertTrue(JSDataSet.jsFunctions.containsKey("sort"), "test assumes a sort js function");
			JSDataSet ds = datasetWithColumns("sort", "other");

			ds.put("other", ds, "changed");
			assertEquals("changed", ds.getValue(0, 1), "control: a put on a plain column is stored (so the test could detect a broken guard)");

			ds.put("sort", ds, "shadow");
			assertNull(ds.getValue(0, 0), "put of a js function name must not store a value in the column of that name");
			assertEquals("changed", ds.getValue(0, 1));
			assertInstanceOf(NativeJavaMethod.class, ds.get("sort", ds));
		}
	}

	@Nested
	class HasOnRecords
	{
		static Stream<Arguments> recordOwners()
		{
			Supplier<IRecordInternal> menuItemRecord = () -> new MenuItemRecord(new JSMenuItem(null, "item", null), new HashMap<String, Object>(), null);
			Supplier<IRecordInternal> viewRecord = () -> new ViewRecord(null, new String[0], new Object[0], null);
			return Stream.of(Arguments.of("MenuItemRecord", menuItemRecord, MenuItemRecord.jsFunctions),
				Arguments.of("ViewRecord", viewRecord, ViewRecord.jsFunctions));
		}

		@ParameterizedTest(name = "{0}")
		@MethodSource("recordOwners")
		@DisplayName("has(dataprovider) reports every js function name and not an unknown name")
		void hasReportsAllFunctionNames(String ownerName, Supplier<IRecordInternal> factory, Map<String, JsMethod> jsFunctions)
		{
			IRecordInternal owner = factory.get();

			assertFalse(jsFunctions.isEmpty());
			for (String name : jsFunctions.keySet())
			{
				assertTrue(owner.has(name), ownerName + ".has(" + name + ")");
			}
			assertFalse(owner.has("notAFunctionOrDataProvider"));
		}
	}

	@Nested
	class Serialization
	{
		@ParameterizedTest(name = "{0}")
		@MethodSource("transientOwners")
		@DisplayName("function cache field of a Serializable owner is transient")
		void cacheFieldIsTransient(String ownerName, Class< ? > ownerClass) throws Exception
		{
			assertTrue(Modifier.isTransient(findField(ownerClass).getModifiers()), ownerName + ".jsFunctionCache must be transient");
		}

		static Stream<Arguments> transientOwners()
		{
			return Stream.of(Arguments.of("JSDataSet", JSDataSet.class), Arguments.of("FindState", FindState.class));
		}

		@Test
		@DisplayName("a dataset that had a function accessed serializes and comes back with an empty cache that works")
		void datasetSerializesWithoutFunctionCache() throws Exception
		{
			JSDataSet ds = new JSDataSet(null, 2, new String[] { "a", "b" });
			ds.get("getMaxRowIndex", ds);
			assertNotNull(readCache(ds));

			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			try (ObjectOutputStream out = new ObjectOutputStream(bytes))
			{
				out.writeObject(ds);
			}
			JSDataSet copy;
			try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray())))
			{
				copy = (JSDataSet)in.readObject();
			}

			assertNull(readCache(copy), "cache must not be serialized");
			NativeJavaMethod fn = assertInstanceOf(NativeJavaMethod.class, copy.get("getMaxRowIndex", copy));
			assertSame(copy, fn.getParentScope());
			assertNotSame(ds.get("getMaxRowIndex", ds), fn);
		}
	}
}
