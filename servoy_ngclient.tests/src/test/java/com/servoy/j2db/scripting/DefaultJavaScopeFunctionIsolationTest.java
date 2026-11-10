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

package com.servoy.j2db.scripting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import com.servoy.j2db.dataprocessing.FindState;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.JSDataSet;
import com.servoy.j2db.dataprocessing.MenuItemRecord;
import com.servoy.j2db.dataprocessing.Record;
import com.servoy.j2db.dataprocessing.RelatedFoundSet;
import com.servoy.j2db.dataprocessing.ViewRecord;
import com.servoy.j2db.dataprocessing.datasource.DBDataSource;
import com.servoy.j2db.dataprocessing.datasource.DBDataSourceServer;
import com.servoy.j2db.dataprocessing.datasource.MemDataSource;
import com.servoy.j2db.dataprocessing.datasource.MenuDataSource;
import com.servoy.j2db.dataprocessing.datasource.SPDataSource;
import com.servoy.j2db.dataprocessing.datasource.SPDataSourceServer;
import com.servoy.j2db.dataprocessing.datasource.ViewDataSource;
import com.servoy.j2db.querybuilder.impl.QBColumns;
import com.servoy.j2db.querybuilder.impl.QBJoins;
import com.servoy.j2db.scripting.DefaultJavaScope.JsMethod;
import com.servoy.j2db.testutils.JsFunctionCacheProbe;

/**
 * SVY-21334: the js functions of a {@link DefaultJavaScope} must not be shared between owners (clients). The static cache only holds
 * immutable {@link JsMethod} entries, the (parent/prototype holding) {@link NativeJavaMethod} instances are kept per owner.
 * <p>
 * Without the fix a single {@link NativeJavaMethod} per class was shared by all owners and its parent scope was overwritten by the
 * last owner that did a get, so {@link #ticketReproductionFirstFunctionKeepsItsOwnParent()} and the other cross owner assertions fail.
 *
 * @author Servoy
 */
@SuppressWarnings("nls")
class DefaultJavaScopeFunctionIsolationTest
{
	private Context cx;

	@BeforeEach
	void enterContext()
	{
		cx = Context.enter();
		// as in the servoy runtime: java primitives (strings) returned by java methods are not wrapped
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

	private String eval(ScriptableObject top, String script)
	{
		return Context.toString(cx.evaluateString(top, script, "test", 1, null));
	}

	private static NativeJavaMethod function(Object value)
	{
		return assertInstanceOf(NativeJavaMethod.class, value, "expected a js function");
	}

	/** js function holders that must never be static state of an owner class (statics are shared by all clients). */
	static Stream<Class< ? >> ownerClasses()
	{
		return Stream.of(FoundSet.class, RelatedFoundSet.class, Record.class, FindState.class, JSDataSet.class, MenuItemRecord.class, ViewRecord.class,
			DefaultJavaScope.class, DBDataSource.class, DBDataSourceServer.class, MemDataSource.class, MenuDataSource.class, SPDataSource.class,
			SPDataSourceServer.class, ViewDataSource.class, QBColumns.class, QBJoins.class, IsolationTestScope.class);
	}

	private static List<String> functionTypedStaticFields(Class< ? > clazz)
	{
		List<String> offending = new ArrayList<>();
		for (Field field : clazz.getDeclaredFields())
		{
			if (Modifier.isStatic(field.getModifiers()) && mentionsFunction(field.getGenericType()))
			{
				offending.add(clazz.getSimpleName() + "." + field.getName() + " : " + field.getGenericType());
			}
		}
		return offending;
	}

	/** true if the type is a rhino function (NativeJavaMethod, BaseFunction, Function) or has one as (nested) generic argument or array component. */
	private static boolean mentionsFunction(Type type)
	{
		if (type instanceof Class< ? > clazz)
		{
			return org.mozilla.javascript.Function.class.isAssignableFrom(clazz) || (clazz.isArray() && mentionsFunction(clazz.getComponentType()));
		}
		if (type instanceof ParameterizedType parameterized)
		{
			for (Type argument : parameterized.getActualTypeArguments())
			{
				if (mentionsFunction(argument)) return true;
			}
			return mentionsFunction(parameterized.getRawType());
		}
		if (type instanceof WildcardType wildcard)
		{
			for (Type bound : wildcard.getUpperBounds())
			{
				if (mentionsFunction(bound)) return true;
			}
			for (Type bound : wildcard.getLowerBounds())
			{
				if (mentionsFunction(bound)) return true;
			}
			return false;
		}
		if (type instanceof GenericArrayType array)
		{
			return mentionsFunction(array.getGenericComponentType());
		}
		return false;
	}

	@SuppressWarnings("unused")
	private static class FunctionHoldingFixture
	{
		private static NativeJavaMethod direct;
		private static Map<String, NativeJavaMethod> map;
		private static ConcurrentMap<Class< ? >, Map<String, BaseFunction>> nested;
		private static Map<String, JsMethod> fine;
		private static String alsoFine;
	}

	@Nested
	class SameOwner
	{
		@Test
		@DisplayName("repeated get of the same name on one owner returns the same function instance")
		void repeatedGetReturnsSameInstance()
		{
			ScriptableObject top = newTopLevel();
			IsolationTestScope owner = new IsolationTestScope("a", top);

			Object first = owner.get("hello", owner);
			Object second = owner.get("hello", owner);

			assertSame(function(first), second);
		}

		@Test
		@DisplayName("different function names on one owner are different function instances")
		void differentNamesGiveDifferentFunctions()
		{
			ScriptableObject top = newTopLevel();
			IsolationTestScope owner = new IsolationTestScope("a", top);

			assertNotSame(function(owner.get("hello", owner)), function(owner.get("describe", owner)));
		}

		@Test
		@DisplayName("function has the start as parent and the Function.prototype of the start's top level scope as prototype")
		void parentAndPrototypeComeFromStart()
		{
			ScriptableObject top = newTopLevel();
			IsolationTestScope owner = new IsolationTestScope("a", top);

			NativeJavaMethod fn = function(owner.get("hello", top));

			assertSame(top, fn.getParentScope());
			assertSame(ScriptableObject.getFunctionPrototype(top), fn.getPrototype());
		}

		@Test
		@DisplayName("start is refreshed on every access of the same owner, still the same instance")
		void startIsRefreshedOnEachAccess()
		{
			ScriptableObject topA = newTopLevel();
			ScriptableObject topB = newTopLevel();
			IsolationTestScope owner = new IsolationTestScope("a", topA);

			NativeJavaMethod first = function(owner.get("hello", topA));
			NativeJavaMethod second = function(owner.get("hello", topB));

			assertSame(first, second);
			assertSame(topB, second.getParentScope());
			assertSame(ScriptableObject.getFunctionPrototype(topB), second.getPrototype());
		}

		@Test
		@DisplayName("owner's function cache field is null on a new scope, stays null for a non function get and is created by the first function access")
		void functionCacheIsLazy() throws Exception
		{
			ScriptableObject top = newTopLevel();
			IsolationTestScope owner = new IsolationTestScope("a", top);

			assertNull(JsFunctionCacheProbe.readCache(owner), "no cache allocated up front");

			assertSame(Scriptable.NOT_FOUND, owner.get("nope", owner));
			assertNull(JsFunctionCacheProbe.readCache(owner), "a non function lookup must not allocate the cache");

			function(owner.get("hello", owner));
			Object cache = JsFunctionCacheProbe.readCache(owner);
			assertNotNull(cache, "first function access creates the cache");

			function(owner.get("describe", owner));
			assertSame(cache, JsFunctionCacheProbe.readCache(owner), "the same cache is kept for further functions");
		}

		@Test
		@Timeout(60)
		@DisplayName("concurrent first access of one owner: every caller gets a function with that owner as parent, afterwards one stable instance")
		void concurrentFirstAccessOnOneOwner() throws Exception
		{
			int threads = 8;
			int rounds = 50;
			ExecutorService executor = Executors.newFixedThreadPool(threads);
			try
			{
				for (int round = 0; round < rounds; round++)
				{
					ScriptableObject top = newTopLevel();
					IsolationTestScope owner = new IsolationTestScope("r" + round, top);
					CyclicBarrier barrier = new CyclicBarrier(threads);
					List<Future<Object>> futures = new ArrayList<>();
					for (int t = 0; t < threads; t++)
					{
						futures.add(executor.submit(() -> {
							Context.enter();
							try
							{
								barrier.await(20, TimeUnit.SECONDS);
								return owner.get("hello", owner);
							}
							finally
							{
								Context.exit();
							}
						}));
					}

					// the first access race is benign: callers may get different instances, but each must be a correctly parented function
					for (Future<Object> future : futures)
					{
						NativeJavaMethod racing = function(future.get(30, TimeUnit.SECONDS));
						assertSame(owner, racing.getParentScope(), "round " + round);
						assertSame(ScriptableObject.getFunctionPrototype(top), racing.getPrototype(), "round " + round);
					}

					NativeJavaMethod stable = function(owner.get("hello", owner));
					assertSame(owner, stable.getParentScope());
					for (int i = 0; i < 3; i++)
					{
						assertSame(stable, owner.get("hello", owner), "round " + round + ": after the race the owner has one stable instance");
					}
				}
			}
			finally
			{
				executor.shutdownNow();
			}
		}
	}

	@Nested
	class CrossOwnerIsolation
	{
		@Test
		@DisplayName("ticket reproduction: first function keeps its own owner as parent after a second owner got the same name")
		void ticketReproductionFirstFunctionKeepsItsOwnParent()
		{
			ScriptableObject topA = newTopLevel();
			ScriptableObject topB = newTopLevel();
			IsolationTestScope ownerA = new IsolationTestScope("A", topA);
			IsolationTestScope ownerB = new IsolationTestScope("B", topB);

			NativeJavaMethod fncA = function(ownerA.get("hello", ownerA));
			NativeJavaMethod fncB = function(ownerB.get("hello", ownerB));

			assertSame(ownerA, fncA.getParentScope(), "function of client A must still point to its own owner");
			assertSame(ownerB, fncB.getParentScope(), "function of client B must point to its own owner");
			assertNotSame(fncA, fncB, "functions of different owners must not be shared");
			assertSame(ScriptableObject.getFunctionPrototype(topA), fncA.getPrototype());
			assertSame(ScriptableObject.getFunctionPrototype(topB), fncB.getPrototype());
			assertNotSame(fncA.getPrototype(), fncB.getPrototype(), "prototype of A must not be overwritten by the one of B");
		}

		@Test
		@DisplayName("many owners keep their own parent for all function names")
		void manyOwnersKeepTheirOwnParents()
		{
			int count = 20;
			List<IsolationTestScope> owners = new ArrayList<>();
			List<NativeJavaMethod> hellos = new ArrayList<>();
			List<NativeJavaMethod> describes = new ArrayList<>();
			for (int i = 0; i < count; i++)
			{
				IsolationTestScope owner = new IsolationTestScope("o" + i, newTopLevel());
				owners.add(owner);
				hellos.add(function(owner.get("hello", owner)));
				describes.add(function(owner.get("describe", owner)));
			}

			for (int i = 0; i < count; i++)
			{
				assertSame(owners.get(i), hellos.get(i).getParentScope(), "hello of owner " + i);
				assertSame(owners.get(i), describes.get(i).getParentScope(), "describe of owner " + i);
				for (int j = i + 1; j < count; j++)
				{
					assertNotSame(hellos.get(i), hellos.get(j));
					assertNotSame(describes.get(i), describes.get(j));
				}
			}
		}

		@Test
		@DisplayName("a function retrieved from owner A still runs against A after owner B was accessed")
		void scriptedFunctionOfFirstOwnerStillWorks()
		{
			ScriptableObject topA = newTopLevel();
			ScriptableObject topB = newTopLevel();
			IsolationTestScope ownerA = new IsolationTestScope("A", topA);
			IsolationTestScope ownerB = new IsolationTestScope("B", topB);
			ScriptableObject.putProperty(topA, "obj", ownerA);
			ScriptableObject.putProperty(topB, "obj", ownerB);

			cx.evaluateString(topA, "var fnc = obj.hello;", "test", 1, null);
			assertEquals("hello from B", eval(topB, "obj.hello()"));

			assertEquals("hello from A", eval(topA, "fnc.call(obj)"));
			assertSame(ownerA, function(ScriptableObject.getProperty(topA, "fnc")).getParentScope());
		}

		@Test
		@DisplayName("JsMethod, the only thing cached statically per class, holds no scriptable or function field")
		void jsMethodHoldsNoScriptables()
		{
			for (Field field : JsMethod.class.getDeclaredFields())
			{
				assertFalse(Scriptable.class.isAssignableFrom(field.getType()), "JsMethod must not hold scriptables: " + field);
			}
		}

		@Test
		@Timeout(60)
		@DisplayName("concurrent owners never see another owner as parent of their function")
		void concurrentOwnersKeepTheirOwnParents() throws Exception
		{
			int threads = 8;
			int iterations = 500;
			List<IsolationTestScope> owners = new ArrayList<>();
			for (int i = 0; i < threads; i++)
			{
				owners.add(new IsolationTestScope("t" + i, newTopLevel()));
			}
			CountDownLatch startGate = new CountDownLatch(1);
			ExecutorService executor = Executors.newFixedThreadPool(threads);
			try
			{
				List<Future<String>> futures = new ArrayList<>();
				for (IsolationTestScope owner : owners)
				{
					Callable<String> task = () -> {
						startGate.await();
						for (int i = 0; i < iterations; i++)
						{
							Object fn = owner.get("hello", owner);
							if (!(fn instanceof NativeJavaMethod method)) return "not a function: " + fn;
							if (method.getParentScope() != owner) return "wrong parent in iteration " + i;
						}
						return null;
					};
					futures.add(executor.submit(task));
				}
				startGate.countDown();
				for (Future<String> future : futures)
				{
					assertEquals(null, future.get(30, TimeUnit.SECONDS));
				}
			}
			finally
			{
				executor.shutdownNow();
			}
		}
	}

	@Nested
	class StaticStateScan
	{
		@Test
		@DisplayName("the scan flags static function fields, also inside generic maps (guards against a vacuous scan)")
		void scanDetectsFunctionTypedStaticFields()
		{
			List<String> offending = functionTypedStaticFields(FunctionHoldingFixture.class);

			assertEquals(3, offending.size(), "offending: " + offending);
			assertTrue(offending.stream().anyMatch(s -> s.contains(".direct ")), offending.toString());
			assertTrue(offending.stream().anyMatch(s -> s.contains(".map ")), offending.toString());
			assertTrue(offending.stream().anyMatch(s -> s.contains(".nested ")), offending.toString());
		}

		@ParameterizedTest(name = "{0}")
		@MethodSource("com.servoy.j2db.scripting.DefaultJavaScopeFunctionIsolationTest#ownerClasses")
		@DisplayName("owner classes have no static field of function type and no static map with function values")
		void ownersHaveNoStaticFunctions(Class< ? > ownerClass)
		{
			List<String> offending = new ArrayList<>();
			for (Class< ? > c = ownerClass; c != null && c != Object.class; c = c.getSuperclass())
			{
				offending.addAll(functionTypedStaticFields(c));
			}

			assertTrue(offending.isEmpty(), ownerClass.getSimpleName() + " statically holds js functions: " + offending);
		}
	}

	@Nested
	class ScriptInterop
	{
		@ParameterizedTest
		@ValueSource(strings = { "obj.hello()", "obj.hello.call(obj)", "obj.hello.apply(obj, [])", "obj.hello.bind(obj)()", "var f = obj.hello; f.call(obj)" })
		@DisplayName("function can be invoked directly and through call/apply/bind")
		void functionCanBeInvoked(String script)
		{
			ScriptableObject top = newTopLevel();
			ScriptableObject.putProperty(top, "obj", new IsolationTestScope("a", top));

			assertEquals("hello from a", eval(top, script));
		}

		@Test
		@DisplayName("returned function is a real js Function")
		void functionIsInstanceOfFunction()
		{
			ScriptableObject top = newTopLevel();
			ScriptableObject.putProperty(top, "obj", new IsolationTestScope("a", top));

			assertAll4(eval(top, "typeof obj.hello"), eval(top, "String(obj.hello instanceof Function)"),
				eval(top, "String(Object.getPrototypeOf(obj.hello) === Function.prototype)"), eval(top, "String(typeof obj.hello.call)"));
		}

		private void assertAll4(String typeOf, String instanceOfFunction, String protoIsFunctionPrototype, String callType)
		{
			assertEquals("function", typeOf);
			assertEquals("true", instanceOfFunction);
			assertEquals("true", protoIsFunctionPrototype);
			assertEquals("function", callType);
		}

		@Test
		@DisplayName("overloads resolve by argument types, also when alternating (per function overload cache)")
		void overloadsResolveCorrectly()
		{
			ScriptableObject top = newTopLevel();
			ScriptableObject.putProperty(top, "obj", new IsolationTestScope("a", top));

			for (int i = 0; i < 5; i++)
			{
				assertEquals("string:x", eval(top, "obj.describe('x')"));
				assertEquals("int:7", eval(top, "obj.describe(7)"));
				assertEquals("pair:a,b", eval(top, "obj.describe('a', 'b')"));
			}
		}

		@Test
		@DisplayName("overloads resolve on every owner independently")
		void overloadsResolveOnEveryOwner()
		{
			ScriptableObject topA = newTopLevel();
			ScriptableObject topB = newTopLevel();
			ScriptableObject.putProperty(topA, "obj", new IsolationTestScope("A", topA));
			ScriptableObject.putProperty(topB, "obj", new IsolationTestScope("B", topB));

			assertEquals("int:1", eval(topA, "obj.describe(1)"));
			assertEquals("string:y", eval(topB, "obj.describe('y')"));
			assertEquals("string:z", eval(topA, "obj.describe('z')"));
			assertEquals("int:2", eval(topB, "obj.describe(2)"));
		}
	}

	@Nested
	class HasAndIds
	{
		@ParameterizedTest
		@ValueSource(strings = { "hello", "describe", "ownerId" })
		@DisplayName("has reports the js function names")
		void hasReportsFunctionNames(String name)
		{
			ScriptableObject top = newTopLevel();
			IsolationTestScope owner = new IsolationTestScope("a", top);

			assertTrue(owner.has(name, owner));
		}

		@Test
		@DisplayName("has is false and get is NOT_FOUND for unknown names")
		void unknownNames()
		{
			ScriptableObject top = newTopLevel();
			IsolationTestScope owner = new IsolationTestScope("a", top);

			assertFalse(owner.has("nope", owner));
			assertSame(Scriptable.NOT_FOUND, owner.get("nope", owner));
		}

		@Test
		@DisplayName("plain variables are stored and returned; a variable put under a function name is not returned by get, the function wins")
		void functionWinsOverVariableWithSameNameOnGet()
		{
			ScriptableObject top = newTopLevel();
			IsolationTestScope owner = new IsolationTestScope("a", top);

			owner.put("myVar", owner, "value");
			owner.put("hello", owner, "shadow");

			assertEquals("value", owner.get("myVar", owner));
			assertInstanceOf(NativeJavaMethod.class, owner.get("hello", owner), "function wins over a variable with the same name");
		}

		@Test
		@DisplayName("accessing functions does not add them to the ids of the scope")
		void idsAreNotAffectedByFunctionAccess()
		{
			ScriptableObject top = newTopLevel();
			IsolationTestScope owner = new IsolationTestScope("a", top);
			owner.put("myVar", owner, "value");

			List<Object> before = Arrays.asList(owner.getIds());
			owner.get("hello", owner);
			owner.get("describe", owner);
			List<Object> after = Arrays.asList(owner.getIds());

			assertEquals(before, after);
			assertTrue(after.contains("myVar"));
			assertTrue(after.contains("allnames"));
			assertTrue(after.contains("length"));
		}

		@Test
		@DisplayName("static map has an entry per js function name, is cached per class and names match the keys")
		void staticMapContents()
		{
			Map<String, JsMethod> functions = DefaultJavaScope.getJsFunctions(IsolationTestScope.class);

			assertSame(IsolationTestScope.FUNCTIONS, functions, "map is cached per class");
			assertEquals(Set.of("hello", "describe", "ownerId"), functions.keySet());
			functions.forEach((key, method) -> assertEquals(key, method.getName()));
		}

		@Test
		@DisplayName("overloads of a name are merged into one function")
		void overloadsAreMerged()
		{
			ScriptableObject top = newTopLevel();
			IsolationTestScope owner = new IsolationTestScope("a", top);

			assertEquals(3, function(owner.get("describe", owner)).getMethods().length);
		}
	}
}
