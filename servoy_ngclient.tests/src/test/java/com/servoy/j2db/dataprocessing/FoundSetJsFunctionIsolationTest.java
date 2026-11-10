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

import static com.servoy.base.query.IQueryConstants.LEFT_OUTER_JOIN;
import static com.servoy.j2db.testutils.JsFunctionCacheProbe.readCache;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.sablo.InMemPackageReader;

import com.servoy.base.query.IBaseSQLCondition;
import com.servoy.base.solutionmodel.IBaseSMPart;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.server.ngclient.property.AbstractSolutionTest;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.WrappedObjectReference;

/**
 * SVY-21334: the js functions of the real client side owners (foundset, record, find state, related foundset) are kept per owner instead of in
 * a JVM wide static, so that one client can never reach the owner of another client through the parent scope of a cached function.
 * <p>
 * Without the fix, one function instance per class was shared and its parent scope was overwritten by the last owner that did a get, so the
 * isolation assertions below fail.
 *
 * @author Servoy
 */
@SuppressWarnings("nls")
class FoundSetJsFunctionIsolationTest extends AbstractSolutionTest
{
	private static final String RELATION = "test_to_relatedtest";
	/** a js function name of Record (and FoundSet) that is also used as column name in the 'puttest' datasource (in-mem column names are lower case). */
	private static final String RECORD_PUT_COLUMN = "save";
	/** a js function name of FoundSet only (not of Record) used as column name in 'puttest': a put that reaches the selected record would be stored. */
	private static final String FOUNDSET_PUT_COLUMN = "clear";

	private Context cx;

	@Override
	protected InMemPackageReader getTestComponents() throws IOException
	{
		return null;
	}

	@Override
	protected void fillTestSolution() throws ServoyException
	{
		// a plain dummy first form so the solution does not auto-initialise a form that needs data too early
		Form firstForm = solution.createNewForm(validator, null, "dummyFirstForm", null, false, new Dimension(600, 400));
		firstForm.setNavigatorID(Form.NAVIGATOR_NONE);
		firstForm.createNewPart(IBaseSMPart.BODY, 5);
		solution.setFirstFormID(firstForm.getUUID().toString());
	}

	@Override
	protected void setupData() throws ServoyException
	{
		BufferedDataSet ds = new BufferedDataSet(new String[] { "pk", "test1", "test2" },
			new int[] { IColumnTypes.INTEGER, IColumnTypes.TEXT, IColumnTypes.TEXT });
		ds.addRow(new Object[] { Integer.valueOf(1), "value1", "value2" });
		ds.addRow(new Object[] { Integer.valueOf(2), "value3", "value4" });
		ds.addRow(new Object[] { Integer.valueOf(3), "value5", "value6" });
		client.getFoundSetManager().insertToDataSource("test", ds, null, new WrappedObjectReference<String[]>(new String[] { "pk" }), true, false);

		BufferedDataSet relatedDS = new BufferedDataSet(new String[] { "relatedtestpk", "testpk", "relatedtest1" },
			new int[] { IColumnTypes.INTEGER, IColumnTypes.INTEGER, IColumnTypes.TEXT });
		relatedDS.addRow(new Object[] { Integer.valueOf(1), Integer.valueOf(1), "relatedvalue11" });
		relatedDS.addRow(new Object[] { Integer.valueOf(2), Integer.valueOf(1), "relatedvalue12" });
		relatedDS.addRow(new Object[] { Integer.valueOf(3), Integer.valueOf(2), "relatedvalue21" });
		client.getFoundSetManager().insertToDataSource("relatedtest", relatedDS, null, new WrappedObjectReference<String[]>(new String[] { "relatedtestpk" }),
			true, false);

		// a datasource with columns that are named like a js function of record (and foundset) and like a js function of foundset only, so that a put
		// on those names has an observable effect when the guard of the owner is missing
		BufferedDataSet putDS = new BufferedDataSet(new String[] { "pk", RECORD_PUT_COLUMN, FOUNDSET_PUT_COLUMN, "plain" },
			new int[] { IColumnTypes.INTEGER, IColumnTypes.TEXT, IColumnTypes.TEXT, IColumnTypes.TEXT });
		putDS.addRow(new Object[] { Integer.valueOf(1), "original", "originalFoundSet", "plain1" });
		client.getFoundSetManager().insertToDataSource("puttest", putDS, null, new WrappedObjectReference<String[]>(new String[] { "pk" }), true, false);

		ConcurrentHashMap<String, IServer> serverProxies = new ConcurrentHashMap<String, IServer>();
		serverProxies.put("_sv_inmem", DUMMY_ISERVER);
		solution.setServerProxies(serverProxies);

		Relation relation = solution.createNewRelation(validator, RELATION, "mem:test", "mem:relatedtest", LEFT_OUTER_JOIN);
		Column primaryColumn = ((Table)client.getFoundSetManager().getTable(relation.getPrimaryDataSource())).getColumn("pk");
		Column foreignColumn = ((Table)client.getFoundSetManager().getTable(relation.getForeignDataSource())).getColumn("testpk");
		relation.createNewRelationItem(client.getFoundSetManager(), primaryColumn, IBaseSQLCondition.EQUALS_OPERATOR, foreignColumn);
	}

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

	private FoundSet newFoundSet() throws ServoyException
	{
		FoundSet foundSet = assertInstanceOf(FoundSet.class, client.getFoundSetManager().getNewFoundSet("mem:test"));
		foundSet.loadAllRecords();
		return foundSet;
	}

	private ScriptableObject newTopLevel()
	{
		return cx.initStandardObjects();
	}

	private static NativeJavaMethod function(Object value)
	{
		return assertInstanceOf(NativeJavaMethod.class, value);
	}

	private FoundSet newPutFoundSet() throws ServoyException
	{
		FoundSet foundSet = assertInstanceOf(FoundSet.class, client.getFoundSetManager().getNewFoundSet("mem:puttest"));
		foundSet.loadAllRecords();
		return foundSet;
	}

	@Nested
	class HasIdsAndPut
	{
		@Test
		@DisplayName("test setup: the puttest datasource has columns named like a record js function and like a foundset only js function, stored in the record")
		void setupHasFunctionNamedColumn() throws Exception
		{
			FoundSet foundSet = newPutFoundSet();

			assertTrue(Record.jsFunctions.containsKey(RECORD_PUT_COLUMN), "must be a js function of record");
			assertTrue(FoundSet.jsFunctions.containsKey(FOUNDSET_PUT_COLUMN), "must be a js function of foundset");
			assertFalse(Record.jsFunctions.containsKey(FOUNDSET_PUT_COLUMN),
				"must not be a js function of record, else the record guard hides a missing foundset guard");
			assertTrue(foundSet.getSQLSheet().getColumnIndex(RECORD_PUT_COLUMN) != -1,
				"column named like the record js function must exist for the put tests to be meaningful");
			assertTrue(foundSet.getSQLSheet().getColumnIndex(FOUNDSET_PUT_COLUMN) != -1,
				"column named like the foundset js function must exist for the put tests to be meaningful");
			assertEquals("original", foundSet.getRecord(0).getValue(RECORD_PUT_COLUMN));
			assertEquals("originalFoundSet", foundSet.getRecord(0).getValue(FOUNDSET_PUT_COLUMN));
		}

		@Test
		@DisplayName("FoundSet: has reports every js function, getIds lists them")
		void foundSetHasAndIds() throws Exception
		{
			FoundSet foundSet = newFoundSet();

			assertFalse(FoundSet.jsFunctions.isEmpty());
			for (String name : FoundSet.jsFunctions.keySet())
			{
				assertTrue(foundSet.has(name, foundSet), "has " + name);
			}
			assertFalse(foundSet.has("notAFunctionOrDataProvider", foundSet));
			assertTrue(Arrays.asList(foundSet.getIds()).containsAll(FoundSet.jsFunctions.keySet()));
		}

		@Test
		@DisplayName("FoundSet: put of a name that is a foundset js function (not a record one) and a column does not change the column of the selected record")
		void foundSetPutIgnoresFunctionNames() throws Exception
		{
			FoundSet foundSet = newPutFoundSet();
			foundSet.setSelectedIndex(1);
			Record record = (Record)foundSet.getRecord(0);
			ScriptableObject top = newTopLevel();

			foundSet.put("plain", top, "changedViaFoundSet");
			assertEquals("changedViaFoundSet", record.getValue("plain"), "control: put of a plain column goes to the selected record");

			// the name is not a js function of record, so only the guard in FoundSet.put keeps this away from the column of the selected record
			foundSet.put(FOUNDSET_PUT_COLUMN, top, "shadow");

			assertEquals("originalFoundSet", record.getValue(FOUNDSET_PUT_COLUMN));
			function(foundSet.get(FOUNDSET_PUT_COLUMN, top));
		}

		@Test
		@DisplayName("Record: has reports every js function, getIds lists them")
		void recordHasAndIds() throws Exception
		{
			Record record = (Record)newFoundSet().getRecord(0);

			assertFalse(Record.jsFunctions.isEmpty());
			for (String name : Record.jsFunctions.keySet())
			{
				assertTrue(record.has(name, record), "has " + name);
				assertTrue(record.has(name), "has(dataprovider) " + name);
			}
			assertFalse(record.has("notAFunctionOrDataProvider", record));
			assertTrue(Arrays.asList(record.getIds()).containsAll(Record.jsFunctions.keySet()));
		}

		@Test
		@DisplayName("Record: put of a name that is a js function and a column does not change the column value")
		void recordPutIgnoresFunctionNames() throws Exception
		{
			Record record = (Record)newPutFoundSet().getRecord(0);
			ScriptableObject top = newTopLevel();

			record.put("plain", top, "changedViaRecord");
			assertEquals("changedViaRecord", record.getValue("plain"), "control: put of a plain column is stored");

			record.put(RECORD_PUT_COLUMN, top, "shadow");

			assertEquals("original", record.getValue(RECORD_PUT_COLUMN));
			function(record.get(RECORD_PUT_COLUMN, top));
		}

		@Test
		@DisplayName("FindState: has reports every js function, getIds does not list them (unchanged behaviour)")
		void findStateHasAndIds() throws Exception
		{
			FoundSet foundSet = newFoundSet();
			foundSet.setFindMode();
			FindState state = assertInstanceOf(FindState.class, foundSet.getRecord(0));

			assertFalse(FindState.jsFunctions.isEmpty());
			for (String name : FindState.jsFunctions.keySet())
			{
				assertTrue(state.has(name, state), "has " + name);
				assertTrue(state.has(name), "has(dataprovider) " + name);
			}
			assertFalse(state.has("notAFunctionOrDataProvider", state));
			List<Object> ids = Arrays.asList(state.getIds());
			assertTrue(ids.contains("test1"), "ids are the column names: " + ids);
			assertTrue(Collections.disjoint(ids, FindState.jsFunctions.keySet()), "FindState.getIds only lists the columns: " + ids);
		}

		@Test
		@DisplayName("FindState: put of a js function name is ignored, it does not reach setValue (no modification event)")
		void findStatePutIgnoresFunctionNames() throws Exception
		{
			FoundSet foundSet = newFoundSet();
			foundSet.setFindMode();
			FindState state = assertInstanceOf(FindState.class, foundSet.getRecord(0));
			ScriptableObject top = newTopLevel();
			List<String> changedNames = new ArrayList<>();
			state.addModificationListener(e -> changedNames.add(e.getName()));

			// without the guard the name would go to setValue, which (even for an unknown data provider) fires a modification event
			state.put("test1", top, "searchValue");
			assertEquals(List.of("test1"), changedNames, "control: put of a plain column is stored and reported");
			assertEquals("searchValue", state.getValue("test1"));

			for (String functionName : FindState.jsFunctions.keySet())
			{
				state.put(functionName, top, "shadow");
			}

			assertEquals(List.of("test1"), changedNames, "put of a js function name must not reach setValue");
			function(state.get("getDataSource", top));
		}
	}

	@Nested
	class FoundSetOwner
	{
		@Test
		@DisplayName("cache field is null on a new foundset and on a non function lookup, created on the first function access")
		void cacheIsLazy() throws Exception
		{
			FoundSet foundSet = newFoundSet();
			ScriptableObject top = newTopLevel();

			assertNull(readCache(foundSet));
			foundSet.get("recordIndex", top);
			assertNull(readCache(foundSet), "a non function lookup must not allocate the cache");

			function(foundSet.get("getDataSource", top));

			assertNotNull(readCache(foundSet));
		}

		@Test
		@DisplayName("ticket reproduction: two foundsets (clients) keep their own parent for the same function name")
		void twoFoundSetsAreIsolated() throws Exception
		{
			FoundSet foundSetA = newFoundSet();
			FoundSet foundSetB = newFoundSet();
			ScriptableObject topA = newTopLevel();
			ScriptableObject topB = newTopLevel();

			NativeJavaMethod fncA = function(foundSetA.get("getDataSource", topA));
			NativeJavaMethod fncB = function(foundSetB.get("getDataSource", topB));

			assertNotSame(fncA, fncB);
			assertSame(topA, fncA.getParentScope(), "function of A must not be re-parented by B");
			assertSame(topB, fncB.getParentScope());
			assertSame(ScriptableObject.getFunctionPrototype(topA), fncA.getPrototype());
			assertSame(ScriptableObject.getFunctionPrototype(topB), fncB.getPrototype());
			assertSame(fncA, foundSetA.get("getDataSource", topA), "repeated access of the same foundset returns the same function");
		}

		@Test
		@DisplayName("FoundSet.clone() does not share the function cache with the original")
		void cloneDoesNotShareCache() throws Exception
		{
			FoundSet original = newFoundSet();
			ScriptableObject top = newTopLevel();
			NativeJavaMethod originalFunction = function(original.get("getDataSource", top));
			assertNotNull(readCache(original));

			Method cloneMethod = FoundSet.class.getDeclaredMethod("clone");
			cloneMethod.setAccessible(true);
			FoundSet copy = (FoundSet)cloneMethod.invoke(original);

			assertNull(readCache(copy), "clone must start with an empty function cache");
			assertNotNull(readCache(original), "clone must not reset the cache of the original");
			NativeJavaMethod copyFunction = function(copy.get("getDataSource", top));
			assertNotSame(originalFunction, copyFunction);
			assertSame(originalFunction, original.get("getDataSource", top));
		}

		@Test
		@DisplayName("foundset functions work through script: call, apply, bind and instanceof Function")
		void scriptInterop() throws Exception
		{
			FoundSet foundSet = newFoundSet();
			// the function prototype comes from the top level scope of the owner (the client's scope), so evaluate in that one
			Scriptable top = ScriptableObject.getTopLevelScope(foundSet);
			ScriptableObject.putProperty(top, "fs", foundSet);

			assertEquals("mem:test", Context.toString(cx.evaluateString(top, "fs.getDataSource()", "test", 1, null)));
			assertEquals("mem:test", Context.toString(cx.evaluateString(top, "fs.getDataSource.call(fs)", "test", 1, null)));
			assertEquals("mem:test", Context.toString(cx.evaluateString(top, "fs.getDataSource.apply(fs, [])", "test", 1, null)));
			assertEquals("mem:test", Context.toString(cx.evaluateString(top, "fs.getDataSource.bind(fs)()", "test", 1, null)));
			assertEquals("true", Context.toString(cx.evaluateString(top, "String(fs.getDataSource instanceof Function)", "test", 1, null)));
		}
	}

	@Nested
	class RecordOwner
	{
		@Test
		@DisplayName("cache field is null on a new record and created on the first function access")
		void cacheIsLazy() throws Exception
		{
			Record record = (Record)newFoundSet().getRecord(0);
			ScriptableObject top = newTopLevel();

			assertNull(readCache(record));
			function(record.get("getDataSource", top));

			assertNotNull(readCache(record));
		}

		@Test
		@DisplayName("two records get different functions and the first keeps its own parent")
		void twoRecordsAreIsolated() throws Exception
		{
			FoundSet foundSet = newFoundSet();
			Record recordA = (Record)foundSet.getRecord(0);
			Record recordB = (Record)foundSet.getRecord(1);
			ScriptableObject topA = newTopLevel();
			ScriptableObject topB = newTopLevel();

			NativeJavaMethod fncA = function(recordA.get("getDataSource", topA));
			NativeJavaMethod fncB = function(recordB.get("getDataSource", topB));

			assertNotSame(fncA, fncB);
			assertSame(topA, fncA.getParentScope());
			assertSame(topB, fncB.getParentScope());
			assertSame(fncA, recordA.get("getDataSource", topA));
		}

		@Test
		@DisplayName("record functions work through script: call and instanceof Function")
		void scriptInterop() throws Exception
		{
			Record record = (Record)newFoundSet().getRecord(0);
			Scriptable top = ScriptableObject.getTopLevelScope(record);
			ScriptableObject.putProperty(top, "rec", record);

			assertEquals("mem:test", Context.toString(cx.evaluateString(top, "rec.getDataSource()", "test", 1, null)));
			assertEquals("mem:test", Context.toString(cx.evaluateString(top, "rec.getDataSource.call(rec)", "test", 1, null)));
			assertEquals("true", Context.toString(cx.evaluateString(top, "String(rec.getDataSource instanceof Function)", "test", 1, null)));
		}
	}

	@Nested
	class FindStateOwner
	{
		@Test
		@DisplayName("cache field is null on a new find state, and two find states are isolated")
		void lazyAndIsolated() throws Exception
		{
			FoundSet foundSetA = newFoundSet();
			FoundSet foundSetB = newFoundSet();
			foundSetA.setFindMode();
			foundSetB.setFindMode();
			FindState stateA = assertInstanceOf(FindState.class, foundSetA.getRecord(0));
			FindState stateB = assertInstanceOf(FindState.class, foundSetB.getRecord(0));
			ScriptableObject topA = newTopLevel();
			ScriptableObject topB = newTopLevel();
			assertNull(readCache(stateA));

			NativeJavaMethod fncA = function(stateA.get("getDataSource", topA));
			NativeJavaMethod fncB = function(stateB.get("getDataSource", topB));

			assertNotNull(readCache(stateA));
			assertNotSame(fncA, fncB);
			assertSame(topA, fncA.getParentScope());
			assertSame(topB, fncB.getParentScope());
		}
	}

	@Nested
	class RelatedFoundSetOwner
	{
		private RelatedFoundSet relatedOf(int recordIndex) throws ServoyException
		{
			IRecordInternal record = newFoundSet().getRecord(recordIndex);
			return assertInstanceOf(RelatedFoundSet.class, record.getRelatedFoundSet(RELATION));
		}

		@Test
		@DisplayName("getMaxRecordIndex is still reported by has() and is a function with parent and Function.prototype")
		void getMaxRecordIndexHasParentAndPrototype() throws Exception
		{
			RelatedFoundSet related = relatedOf(0);
			ScriptableObject top = newTopLevel();

			assertTrue(related.has("getMaxRecordIndex", related));
			NativeJavaMethod fn = function(related.get("getMaxRecordIndex", top));

			assertSame(top, fn.getParentScope(), "formerly shared static had no parent at all");
			assertSame(ScriptableObject.getFunctionPrototype(top), fn.getPrototype(), "formerly shared static had no prototype");
			assertSame(fn, related.get("getMaxRecordIndex", top), "same related foundset returns the same function");
		}

		@Test
		@DisplayName("getMaxRecordIndex functions of two related foundsets are not shared")
		void getMaxRecordIndexIsNotShared() throws Exception
		{
			RelatedFoundSet relatedA = relatedOf(0);
			RelatedFoundSet relatedB = relatedOf(1);
			ScriptableObject topA = newTopLevel();
			ScriptableObject topB = newTopLevel();

			NativeJavaMethod fncA = function(relatedA.get("getMaxRecordIndex", topA));
			NativeJavaMethod fncB = function(relatedB.get("getMaxRecordIndex", topB));

			assertNotSame(relatedA, relatedB);
			assertNotSame(fncA, fncB);
			assertSame(topA, fncA.getParentScope());
			assertSame(topB, fncB.getParentScope());
		}

		@Test
		@DisplayName("put of getMaxRecordIndex is ignored: no record edited, selection and size unchanged, function still served")
		void putOfGetMaxRecordIndexIsIgnored() throws Exception
		{
			RelatedFoundSet related = relatedOf(0);
			ScriptableObject top = newTopLevel();
			NativeJavaMethod before = function(related.get("getMaxRecordIndex", top));
			int size = related.getSize();
			int selectedIndex = related.getSelectedIndex();
			int maxRecordIndex = related.js_getMaxRecordIndex();
			assertEquals(0, related.getEditingRecords().length);

			related.put("getMaxRecordIndex", top, "shadow");

			assertEquals(0, related.getEditingRecords().length, "put must not be forwarded to the selected record");
			assertEquals(size, related.getSize());
			assertEquals(selectedIndex, related.getSelectedIndex());
			assertEquals(maxRecordIndex, related.js_getMaxRecordIndex());
			assertSame(before, related.get("getMaxRecordIndex", top));
		}

		@Test
		@DisplayName("getMaxRecordIndex works through script: direct call, call, apply, instanceof Function")
		void getMaxRecordIndexScriptInterop() throws Exception
		{
			RelatedFoundSet related = relatedOf(0);
			Scriptable top = ScriptableObject.getTopLevelScope(related);
			ScriptableObject.putProperty(top, "rfs", related);
			int expected = related.js_getMaxRecordIndex();
			assertEquals(2, expected, "record 1 has two related rows");

			assertEquals(expected, ((Number)cx.evaluateString(top, "rfs.getMaxRecordIndex()", "test", 1, null)).intValue());
			assertEquals(expected, ((Number)cx.evaluateString(top, "rfs.getMaxRecordIndex.call(rfs)", "test", 1, null)).intValue());
			assertEquals(expected, ((Number)cx.evaluateString(top, "rfs.getMaxRecordIndex.apply(rfs, [])", "test", 1, null)).intValue());
			assertEquals("true", Context.toString(cx.evaluateString(top, "String(rfs.getMaxRecordIndex instanceof Function)", "test", 1, null)));
		}
	}
}
