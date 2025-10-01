/*
 * Copyright (C) 2014 Servoy BV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.servoy.j2db.server.ngclient.property;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.sablo.InMemPackageReader;
import org.sablo.WebComponent;
import org.sablo.specification.Package.IPackageReader;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectSpecification.PushToServerEnum;
import org.sablo.specification.property.ArrayOperation;
import org.sablo.specification.property.BrowserConverterContext;
import org.sablo.specification.property.ChangeAwareList;
import org.sablo.specification.property.ChangeAwareMap;
import org.sablo.specification.property.ChangeAwareMap.Changes;
import org.sablo.specification.property.CustomJSONArrayType;
import org.sablo.specification.property.CustomJSONObjectType;
import org.sablo.specification.property.CustomJSONPropertyType;
import org.sablo.websocket.TypedData;
import org.sablo.websocket.utils.JSONUtils;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import com.servoy.j2db.server.ngclient.INGApplication;
import com.servoy.j2db.server.ngclient.property.DataProviderDateTest.ServiceProvider;
import com.servoy.j2db.server.ngclient.property.types.NGConversions;
import com.servoy.j2db.server.ngclient.property.types.Types;

/**
 * @author acostescu
 */
@SuppressWarnings("nls")
public class CustomArrayAndObjectPropertyRhinoTest extends Log4JToConsoleTest
{

	@Before
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
	}

	@After
	public void tearDown()
	{
		WebComponentSpecProvider.disposeInstance();
	}

	@Test
	public void testCustomTypeJavaBasedRhinoChanges() throws JSONException
	{
		Context.enter();
		try
		{
			WebComponent component = new WebComponent("mycomponent", "testComponentName");
			BrowserConverterContext allowingBrowserConverterContext = new BrowserConverterContext(component, PushToServerEnum.allow);
			PropertyDescription arrayTPD = component.getSpecification().getProperty("arrayT");

			// just some initial checks and setting a java value
			assertNull(component.getProperty("arrayT"));
			List<Map<String, Object>> javaV = new ArrayList<>();
			HashMap<String, Object> hm = new HashMap<String, Object>();
			javaV.add(hm);
			hm.put("text", "Just some text");
			component.setProperty("arrayT", javaV);

			// set should turn it into a change-aware list
			Object wrapped = component.getProperty("arrayT");
			assertTrue(wrapped instanceof ChangeAwareList< ? , ? >);
			ChangeAwareList<Map<String, Object>, Map<String, Object>> cal = (ChangeAwareList)wrapped;
			assertEquals("Just some text", cal.get(0).get("text"));
			assertTrue(cal.get(0) instanceof ChangeAwareMap< ? , ? >);
			ChangeAwareMap cam = ((ChangeAwareMap< ? , ? >)cal.get(0));
			Changes chMap = cam.getChanges();
			ChangeAwareList<Map<String, Object>, Map<String, Object>>.Changes chList = cal.getChanges();

			// TODO I guess this kind of reference changes should be treated in the BaseWebObject directly when we have separate methods for changesToJSON and fullToJSON
			// so for now the change aware things do not report as being changed...
			assertTrue(!chList.mustSendAll());
			assertTrue(!chMap.mustSendAll());

			// still the component has to see them as changed!
			TypedData<Map<String, Object>> changes = component.getAndClearChanges();
			assertTrue(changes.content.get("arrayT") != null);
			assertTrue(changes.contentType.getProperty("arrayT").getType() instanceof CustomJSONArrayType);
			Object arrayCh = changes.content.get("arrayT");
			assertTrue(arrayCh != null);
			assertTrue(changes.contentType.getProperty("arrayT").getType() instanceof CustomJSONArrayType);
			Object mapCh = ((ChangeAwareList)arrayCh).get(0);
			assertTrue(mapCh != null);
			assertTrue(((CustomJSONPropertyType< ? >)changes.contentType.getProperty(
				"arrayT").getType()).getCustomJSONTypeDefinition().getType() instanceof CustomJSONObjectType);

			JSONUtils.writeDataAsFullToJSON(changes.content, changes.contentType, allowingBrowserConverterContext); // just to see it doesn't err. out
			// ok now that we called component.getChanges() no changes should be present any more

			assertTrue(!chList.mustSendAll());
			assertTrue(!chMap.mustSendAll());
			assertEquals(0, component.getAndClearChanges().content.size());
			assertFalse(chList.getGranularUpdatesKeeper().hasChanges());

			// check changing java => change reflected in Rhino
			ScriptableObject topLevel = new ScriptableObject()
			{

				@Override
				public String getClassName()
				{
					return "test_top_level_scope";
				}
			};
			Scriptable rhinoVal = (Scriptable)NGConversions.INSTANCE.convertSabloComponentToRhinoValue(component.getProperty("arrayT"), arrayTPD, component,
				topLevel);
			assertEquals("Just some text", ((Scriptable)rhinoVal.get(0, rhinoVal)).get("text", rhinoVal));
			cam.put("text", "Just some text 2");
			assertEquals(1, chMap.getKeysChangedByRef().size());
			assertEquals("text", chMap.getKeysChangedByRef().iterator().next());
			ArrayOperation[] opSeq = chList.getGranularUpdatesKeeper().getEquivalentSequenceOfOperations();
			assertEquals(1, opSeq.length);
			assertGranularOpIs(0, 0, ArrayOperation.CHANGE, ChangeAwareList.GRANULAR_UPDATE_OP, opSeq[0]);
			assertTrue(!chList.mustSendAll());
			assertTrue(!chMap.mustSendAll());
			assertEquals("Just some text 2", ((Scriptable)rhinoVal.get(0, rhinoVal)).get("text", rhinoVal));
			cam.put("active", new ArrayList());
			assertTrue(!chList.mustSendAll());
			assertTrue(!chMap.mustSendAll());
			assertEquals(2, chMap.getKeysChangedByRef().size());
			assertTrue(chMap.getKeysChangedByRef().contains("text"));
			assertTrue(chMap.getKeysChangedByRef().contains("active"));
			cam.remove("active");
			assertTrue(!chList.mustSendAll());
			assertTrue(chMap.mustSendAll());
			cal.add(new HashMap<String, Object>());
			ChangeAwareMap cam1 = ((ChangeAwareMap< ? , ? >)cal.get(1));
			assertFalse(chList.mustSendAll());
			opSeq = chList.getGranularUpdatesKeeper().getEquivalentSequenceOfOperations();
			assertEquals(2, opSeq.length);
			assertGranularOpIs(0, 0, ArrayOperation.CHANGE, ChangeAwareList.GRANULAR_UPDATE_OP, opSeq[0]);
			assertGranularOpIs(1, 1, ArrayOperation.INSERT, null, opSeq[1]);
			assertTrue(chMap.mustSendAll());

			// ok clear changes
			changes = component.getAndClearChanges();
			JSONUtils.writeDataAsFullToJSON(changes.content, changes.contentType, allowingBrowserConverterContext); // just to see it doesn't err. out
			assertEquals(1, changes.content.size());
			assertEquals(0, component.getAndClearChanges().content.size());
			assertTrue(!chList.mustSendAll());
			assertTrue(!chMap.mustSendAll());
			assertFalse(chList.getGranularUpdatesKeeper().hasChanges());
			assertEquals(0, chMap.getKeysChangedByRef().size());
			assertEquals(0, chMap.getKeysWithUpdates().size());

			// assign some native values
			Scriptable oneO = Context.getCurrentContext().newObject(topLevel);
			Scriptable activeA1 = Context.getCurrentContext().newArray(topLevel, 0);
			Scriptable activeA1Obj = Context.getCurrentContext().newObject(topLevel);
			rhinoVal.put(0, rhinoVal, oneO);
			Scriptable oneOScriptable = (Scriptable)rhinoVal.get(0, rhinoVal); // same as 'cam' but in it's Rhino representation
			assertTrue(!chList.mustSendAll());
			opSeq = chList.getGranularUpdatesKeeper().getEquivalentSequenceOfOperations();
			assertEquals(1, opSeq.length);
			assertGranularOpIs(0, 0, ArrayOperation.CHANGE, null, opSeq[0]);
			cam = ((ChangeAwareMap< ? , ? >)cal.get(0));
			chMap = cam.getChanges();
			activeA1Obj.put("field", activeA1Obj, 11);
			activeA1.put(0, activeA1, activeA1Obj);
			oneOScriptable.put("active", oneOScriptable, activeA1);
			assertEquals(11, ((Map)((List)cal.get(0).get("active")).get(0)).get("field"));
			((Map)((List)cal.get(0).get("active")).get(0)).put("percent", 0.22);

			assertEquals(1, chMap.getKeysChangedByRef().size());
			assertEquals(0, chMap.getKeysWithUpdates().size());
			assertTrue(chMap.getKeysChangedByRef().contains("active"));
			assertTrue(!chList.mustSendAll());
			opSeq = chList.getGranularUpdatesKeeper().getEquivalentSequenceOfOperations();
			assertEquals(1, opSeq.length);
			assertGranularOpIs(0, 0, ArrayOperation.CHANGE, null, opSeq[0]);

			// now change the native values using initial ref to see if it changed in java; this is no longer supported after case SVY-11027
//		activeA1Obj.put("field", activeA1Obj, 98);
//		assertEquals(98, ((Map)((List)((Map)cal.get(0)).get("active")).get(0)).get("field"));
//		activeA1.put(1, activeA1, activeA2Obj);
//		activeA2Obj.put("field", activeA2Obj, 45);
//		assertEquals(45, ((Map)((List)((Map)cal.get(0)).get("active")).get(1)).get("field"));

			changes = component.getAndClearChanges();
			JSONAssert.assertEquals(
				"{\"arrayT\":{\"vEr\":3,\"g\":[{\"op\":[0,0,0],\"d\":[{\"vEr\":5,\"v\":{\"active\":{\"vEr\":2,\"v\":[{\"vEr\":2,\"v\":{\"field\":11,\"percent\":0.22}}]}}}]}]}}",
				JSONUtils.writeChanges(changes.content, changes.contentType, allowingBrowserConverterContext), JSONCompareMode.NON_EXTENSIBLE);

			((Map)((List)cal.get(0).get("active")).get(0)).put("percent", 0.33);

			assertEquals(0, chMap.getKeysChangedByRef().size());
			assertEquals(1, chMap.getKeysWithUpdates().size());

			changes = component.getAndClearChanges();
			JSONAssert.assertEquals(
				"{\"arrayT\":{\"vEr\":3,\"g\":[{\"op\":[0,0,0],\"d\":[{\"vEr\":5,\"u\":[{\"k\":\"active\",\"v\":{\"vEr\":2,\"g\":[{\"op\":[0,0,0],\"d\":[{\"vEr\":2,\"u\":[{\"k\":\"percent\",\"v\":0.33}]}]}]}}]}]}]}}",
				JSONUtils.writeChanges(changes.content, changes.contentType, allowingBrowserConverterContext), JSONCompareMode.NON_EXTENSIBLE);

			((List)cal.get(0).get("active")).add(new HashMap<String, Object>());
			((Map)((List)cal.get(0).get("active")).get(1)).put("percent", 0.99);
			component.getAndClearChanges();
			// now simulate another request cycle that makes some change to the property from javascript
			rhinoVal = (Scriptable)NGConversions.INSTANCE.convertSabloComponentToRhinoValue(component.getProperty("arrayT"), arrayTPD, component, topLevel);
			Scriptable v = ((Scriptable)rhinoVal.get(0, rhinoVal));
			v = (Scriptable)v.get("active", v);
			v = (Scriptable)v.get(1, v);
			assertEquals(0.99, v.get("percent", v));
			v.put("percent", v, 0.56);
			assertEquals(0.56, ((Map)((List)cal.get(0).get("active")).get(1)).get("percent"));
			assertTrue(!chMap.mustSendAll());
			assertTrue(!chList.mustSendAll());
			opSeq = chList.getGranularUpdatesKeeper().getEquivalentSequenceOfOperations();
			assertEquals(1, opSeq.length);
			assertGranularOpIs(0, 0, ArrayOperation.CHANGE, ChangeAwareList.GRANULAR_UPDATE_OP, opSeq[0]);
			assertEquals(1, chMap.getKeysWithUpdates().size());
			assertEquals("active", chMap.getKeysWithUpdates().iterator().next());
		}
		finally
		{
			Context.exit();
		}
	}

	@Test
	public void testSkipNullAndSetToNullIfKeys() throws JSONException
	{
		WebComponent component = new WebComponent("mycomponent", "testComponentName");
		PropertyDescription arraySkipNullsAtRuntimePD = component.getSpecification().getProperty("arraySkipNullsAtRuntime");

		assertNull(component.getProperty("arraySkipNullsAtRuntime"));
		List<Map<String, Object>> javaV = new ArrayList<>();
		HashMap<String, Object> hm = new HashMap<String, Object>();
		hm.put("a", "Just some text 1");
		hm.put("b", Integer.valueOf(1));
		hm.put("c", new Date());
		javaV.add(hm);
		javaV.add(null); // to sablo conversion should skip this index completely
		hm = new HashMap<String, Object>();
		hm.put("a", "Just some text 2");
		hm.put("b", Integer.valueOf(2));
		hm.put("c", new Date());
		javaV.add(hm);
		hm = new HashMap<String, Object>();
		hm.put("a", null); // to sablo conversion should make the whole object null because "a" key is null then make then because whole thing is null skip the whole array index
		hm.put("b", Integer.valueOf(3));
		hm.put("c", new Date());
		javaV.add(hm);
		hm = new HashMap<String, Object>();
		hm.put("a", "Just some text 4");
		hm.put("b", Integer.valueOf(4));
		hm.put("c", new Date());
		javaV.add(hm);
		hm = new HashMap<String, Object>();
		hm.put("a", "Just some text 5");
		hm.put("b", Integer.valueOf(5));
		hm.put("c", null); // to sablo conversion should make the whole object null because "c" key is null then make then because whole thing is null skip the whole array index
		javaV.add(hm);

		// conversion should turn it into a change-aware list
		Object sabloBeforeWrap = NGConversions.INSTANCE.convertFormElementToSabloComponentValue(javaV.toArray(), arraySkipNullsAtRuntimePD, null, null, null);
		assertTrue(sabloBeforeWrap instanceof List< ? >);

		@SuppressWarnings("unchecked")
		List<Map< ? , ? >> sbr = (List<Map< ? , ? >>)sabloBeforeWrap;
		assertEquals("Just some text 1", sbr.get(0).get("a"));
		assertEquals("Just some text 2", sbr.get(1).get("a"));
		assertEquals("Just some text 4", sbr.get(2).get("a"));
		assertEquals(3, sbr.size());
	}

	@Test
	public void seeThatNormalArraysAndCustomObjectsKeepNullsAsExpected() throws JSONException
	{
		WebComponent component = new WebComponent("mycomponent", "testComponentName");
		PropertyDescription normalArrayPD = component.getSpecification().getProperty("normalArray");

		checkNormalArraysAndCustomObjectsWithNullValues(component, normalArrayPD);
	}

	@Test
	public void seeThatNormalArraysAndCustomObjectsWithIrrelevantConfigKeepNullsAsExpected() throws JSONException
	{
		WebComponent component = new WebComponent("mycomponent", "testComponentName");
		PropertyDescription normalArrayPD = component.getSpecification().getProperty("normalArrayWithConfig");

		checkNormalArraysAndCustomObjectsWithNullValues(component, normalArrayPD);
	}

	@Test
	public void shouldCorrectlytoJSONDatesAndFunctionsforObjectType() throws Exception
	{
		WebComponent component = new WebComponent("mycomponent", "test");
		BrowserConverterContext allowDataConverterContext = new BrowserConverterContext(component, PushToServerEnum.allow);

		// custom types are always a Map of values..
		Map<String, Object> customType1 = new HashMap<>();
		customType1.put("key1", "aaa");
		customType1.put("key2", 123);
		customType1.put("key3", true);

		Map<String, Object> customType2 = new HashMap<>();
		customType2.put("myproperty", customType1);

		component.setProperty("unknownvalue", customType2);

		TypedData<Map<String, Object>> properties = component.getProperties();

		String msg = JSONUtils.writeDataAsFullToJSON(properties.content, properties.contentType, allowDataConverterContext);
		JSONObject actual1 = new JSONObject(msg);
		JSONObject expected1 = new JSONObject(
			"{\"name\":\"test\",\"unknownvalue\":{\"vEr\":2,\"v\":{\"myproperty\":{\"key1\":\"aaa\",\"key2\":123,\"key3\":true}}}}");
		JSONAssert.assertEquals(expected1, actual1, JSONCompareMode.NON_EXTENSIBLE);

		customType1 = new HashMap<>();
		customType1.put("key1", "aaa");
		customType1.put("key2", null);
		customType1.put("key3", new Object[] { "bbb", "ccc", null });
		customType2.put("myproperty", customType1);

		component.setProperty("unknownvalue", customType2);

		properties = component.getProperties();

		msg = JSONUtils.writeDataAsFullToJSON(properties.content, properties.contentType, allowDataConverterContext);
		JSONObject actual2 = new JSONObject(msg);
		JSONObject expected2 = new JSONObject(
			"{\"name\":\"test\",\"unknownvalue\":{\"vEr\":4,\"v\":{\"myproperty\":{\"key1\":\"aaa\",\"key2\":null,\"key3\":[\"bbb\",\"ccc\",null]}}}}");
		JSONAssert.assertEquals(expected2, actual2, JSONCompareMode.NON_EXTENSIBLE);

		TimeZone default1 = TimeZone.getDefault();
		try
		{
			TimeZone.setDefault(TimeZone.getTimeZone("Europe/Bucharest"));
			customType1 = new HashMap<>();
			customType1.put("key1", "aaa");
			customType1.put("key2", new Date(90, 1, 1));
			customType1.put("key3", new Object[] { "bbb", "ccc", new Date(100, 10, 10) });

			customType2.put("myproperty", customType1);

			component.setProperty("unknownvalue", customType2);
			properties = component.getProperties();

			msg = JSONUtils.writeDataAsFullToJSON(properties.content, properties.contentType, allowDataConverterContext);
		}
		finally
		{
			TimeZone.setDefault(default1);
		}

		JSONObject actual3 = new JSONObject(msg);
		JSONObject expected3 = new JSONObject(
			"{\"name\":\"test\",\"unknownvalue\":{\"vEr\":6,\"v\":{\"myproperty\":{\"_T\":\"object\",\"_V\":{\"key1\":\"aaa\",\"key2\":{\"_T\":\"svy_date\",\"_V\":\"1990-02-01T00:00+02:00\"},\"key3\":{\"_T\":\"object\",\"_V\":[\"bbb\",\"ccc\",{\"_T\":\"svy_date\",\"_V\":\"2000-11-10T00:00+02:00\"}]}}}}}}}");
		JSONAssert.assertEquals(expected3, actual3, JSONCompareMode.NON_EXTENSIBLE);

		INGApplication application = new ServiceProvider();
		customType1 = new HashMap();
		customType1.put("key1", "aaa");
		customType1.put("key2", new BrowserFunction("func1", application));
		customType1.put("key3", new Object[] { "bbb", "ccc", new BrowserFunction("func2", application) });

		customType2.put("myproperty", customType1);

		component.setProperty("unknownvalue", customType2);

		properties = component.getProperties();

		msg = JSONUtils.writeDataAsFullToJSON(properties.content, properties.contentType, allowDataConverterContext);

		JSONObject actual4 = new JSONObject(msg);
		JSONObject expected4 = new JSONObject(
			"{\"name\":\"test\",\"unknownvalue\":{\"vEr\":8,\"v\":{\"myproperty\":{\"_T\":\"object\",\"_V\":{\"key1\":\"aaa\",\"key2\":{\"_T\":\"clientfunction\",\"_V\":\"func1\"},\"key3\":{\"_T\":\"object\",\"_V\":[\"bbb\",\"ccc\",{\"_T\":\"clientfunction\",\"_V\":\"func2\"}]}}}}}}}");
		JSONAssert.assertEquals(expected4, actual4, JSONCompareMode.NON_EXTENSIBLE);

		customType1 = new HashMap();
		customType1.put("key1", "aaa");
		customType1.put("key2", new BaseFunction());
		customType1.put("key3", new Object[] { "bbb", "ccc", new BaseFunction() });

		customType2.put("myproperty", customType1);

		component.setProperty("unknownvalue", customType2);

		properties = component.getProperties();

		msg = JSONUtils.writeDataAsFullToJSON(properties.content, properties.contentType, allowDataConverterContext);

		JSONObject json = new JSONObject(msg);
		JSONObject firstHash = json.getJSONObject("unknownvalue").getJSONObject("v").getJSONObject("myproperty").getJSONObject("_V").getJSONObject("key2")
			.getJSONObject("_V");
		firstHash.put("functionhash", "dummyhash");
		JSONObject secondHash = json.getJSONObject("unknownvalue").getJSONObject("v").getJSONObject("myproperty").getJSONObject("_V").getJSONObject("key3")
			.getJSONArray("_V").getJSONObject(2)
			.getJSONObject("_V");
		secondHash.put("functionhash", "dummyhash");
		JSONObject expected5 = new JSONObject(
			"{\"name\":\"test\",\"unknownvalue\":{\"vEr\":10,\"v\":{\"myproperty\":{\"_T\":\"object\",\"_V\":{\"key1\":\"aaa\",\"key2\":{\"_T\":\"NativeFunction\",\"_V\":{\"functionhash\":\"dummyhash\",\"svyType\":\"NativeFunction\"}},\"key3\":{\"_T\":\"object\",\"_V\":[\"bbb\",\"ccc\",{\"_T\":\"NativeFunction\",\"_V\":{\"functionhash\":\"dummyhash\",\"svyType\":\"NativeFunction\"}}]}}}}}}}");
		JSONAssert.assertEquals(expected5, json, JSONCompareMode.NON_EXTENSIBLE);

	}

	@Test
	public void shouldCorrectlytoJSONDatesAndFunctionsforObjectArrayType() throws Exception
	{
		WebComponent component = new WebComponent("mycomponent", "test");
		BrowserConverterContext allowDataConverterContext = new BrowserConverterContext(component, PushToServerEnum.allow);

		// custom types are always a Map of values..
		Map<String, Object> customType1 = new HashMap<>();
		customType1.put("key1", "aaa");
		customType1.put("key2", 123);
		customType1.put("key3", true);

		Map<String, Object> customType2 = new HashMap<>();
		customType2.put("myproperty", customType1);

		component.setProperty("unknownvaluearray", new Object[] { customType2 });

		TypedData<Map<String, Object>> properties = component.getProperties();

		String msg = JSONUtils.writeDataAsFullToJSON(properties.content, properties.contentType, allowDataConverterContext);
		JSONObject actual = new JSONObject(msg);
		JSONObject expected = new JSONObject(
			"{\"unknownvaluearray\":{\"vEr\":2,\"v\":[{\"vEr\":2,\"v\":{\"myproperty\":{\"key1\":\"aaa\",\"key2\":123,\"key3\":true}}}]},\"name\":\"test\"}");
		JSONAssert.assertEquals(expected, actual, JSONCompareMode.NON_EXTENSIBLE);

		customType1 = new HashMap<>();
		customType1.put("key1", "aaa");
		customType1.put("key2", null);
		customType1.put("key3", new Object[] { "bbb", "ccc", null });
		customType2.put("myproperty", customType1);

		component.setProperty("unknownvaluearray", new Object[] { customType2 });

		properties = component.getProperties();

		msg = JSONUtils.writeDataAsFullToJSON(properties.content, properties.contentType, allowDataConverterContext);
		JSONObject actual2 = new JSONObject(msg);
		JSONObject expected2 = new JSONObject(
			"{\"unknownvaluearray\":{\"vEr\":4,\"v\":[{\"vEr\":2,\"v\":{\"myproperty\":{\"key1\":\"aaa\",\"key2\":null,\"key3\":[\"bbb\",\"ccc\",null]}}}]},\"name\":\"test\"}");
		JSONAssert.assertEquals(expected2, actual2, JSONCompareMode.NON_EXTENSIBLE);

		TimeZone default1 = TimeZone.getDefault();
		try
		{
			TimeZone.setDefault(TimeZone.getTimeZone("Europe/Bucharest"));
			customType1 = new HashMap<>();
			customType1.put("key1", "aaa");
			customType1.put("key2", new Date(90, 1, 1));
			customType1.put("key3", new Object[] { "bbb", "ccc", new Date(100, 10, 10) });

			customType2.put("myproperty", customType1);

			component.setProperty("unknownvaluearray", new Object[] { customType2 });
			properties = component.getProperties();

			msg = JSONUtils.writeDataAsFullToJSON(properties.content, properties.contentType, allowDataConverterContext);
		}
		finally
		{
			TimeZone.setDefault(default1);
		}

		JSONObject actual3 = new JSONObject(msg);
		JSONObject expected3 = new JSONObject(
			"{\"unknownvaluearray\":{\"vEr\":6,\"v\":[{\"vEr\":2,\"v\":{\"myproperty\":{\"_T\":\"object\",\"_V\":{\"key1\":\"aaa\",\"key2\":{\"_T\":\"svy_date\",\"_V\":\"1990-02-01T00:00+02:00\"},\"key3\":{\"_T\":\"object\",\"_V\":[\"bbb\",\"ccc\",{\"_T\":\"svy_date\",\"_V\":\"2000-11-10T00:00+02:00\"}]}}}}}]},\"name\":\"test\"}");
		JSONAssert.assertEquals(expected3, actual3, JSONCompareMode.NON_EXTENSIBLE);

		INGApplication application = new ServiceProvider();
		customType1 = new HashMap();
		customType1.put("key1", "aaa");
		customType1.put("key2", new BrowserFunction("func1", application));
		customType1.put("key3", new Object[] { "bbb", "ccc", new BrowserFunction("func2", application) });

		customType2.put("myproperty", customType1);

		component.setProperty("unknownvaluearray", new Object[] { customType2 });

		properties = component.getProperties();

		msg = JSONUtils.writeDataAsFullToJSON(properties.content, properties.contentType, allowDataConverterContext);

		JSONObject actual4 = new JSONObject(msg);
		JSONObject expected4 = new JSONObject(
			"{\"unknownvaluearray\":{\"vEr\":8,\"v\":[{\"vEr\":2,\"v\":{\"myproperty\":{\"_T\":\"object\",\"_V\":{\"key1\":\"aaa\",\"key2\":{\"_T\":\"clientfunction\",\"_V\":\"func1\"},\"key3\":{\"_T\":\"object\",\"_V\":[\"bbb\",\"ccc\",{\"_T\":\"clientfunction\",\"_V\":\"func2\"}]}}}}}]},\"name\":\"test\"}");
		JSONAssert.assertEquals(expected4, actual4, JSONCompareMode.NON_EXTENSIBLE);

		customType1 = new HashMap();
		customType1.put("key1", "aaa");
		customType1.put("key2", new BaseFunction());
		customType1.put("key3", new Object[] { "bbb", "ccc", new BaseFunction() });

		customType2.put("myproperty", customType1);

		component.setProperty("unknownvaluearray", new Object[] { customType2 });

		properties = component.getProperties();

		msg = JSONUtils.writeDataAsFullToJSON(properties.content, properties.contentType, allowDataConverterContext);

		JSONObject json = new JSONObject(msg);
		JSONObject firstHash = json.getJSONObject("unknownvaluearray").getJSONArray("v").getJSONObject(0).getJSONObject("v").getJSONObject("myproperty")
			.getJSONObject("_V").getJSONObject("key2")
			.getJSONObject("_V");
		firstHash.put("functionhash", "dummyhash");
		JSONObject secondHash = json.getJSONObject("unknownvaluearray").getJSONArray("v").getJSONObject(0).getJSONObject("v").getJSONObject("myproperty")
			.getJSONObject("_V").getJSONObject("key3")
			.getJSONArray("_V").getJSONObject(2)
			.getJSONObject("_V");
		secondHash.put("functionhash", "dummyhash");
		JSONObject expected5 = new JSONObject(
			"{\"unknownvaluearray\":{\"vEr\":10,\"v\":[{\"vEr\":2,\"v\":{\"myproperty\":{\"_T\":\"object\",\"_V\":{\"key1\":\"aaa\",\"key2\":{\"_T\":\"NativeFunction\",\"_V\":{\"functionhash\":\"dummyhash\",\"svyType\":\"NativeFunction\"}},\"key3\":{\"_T\":\"object\",\"_V\":[\"bbb\",\"ccc\",{\"_T\":\"NativeFunction\",\"_V\":{\"functionhash\":\"dummyhash\",\"svyType\":\"NativeFunction\"}}]}}}}}]},\"name\":\"test\"}");
		JSONAssert.assertEquals(expected5, json, JSONCompareMode.NON_EXTENSIBLE);

	}

	private void checkNormalArraysAndCustomObjectsWithNullValues(WebComponent component, PropertyDescription normalArrayPD)
	{
		assertNull(component.getProperty("normalArray"));
		List<Map<String, Object>> javaV = new ArrayList<>();
		HashMap<String, Object> hm = new HashMap<String, Object>();
		hm.put("a", "Just some text 1");
		hm.put("b", Integer.valueOf(1));
		hm.put("c", new Date());
		javaV.add(hm);
		javaV.add(null);
		hm = new HashMap<String, Object>();
		hm.put("a", "Just some text 2");
		hm.put("b", Integer.valueOf(2));
		hm.put("c", new Date());
		javaV.add(hm);
		hm = new HashMap<String, Object>();
		hm.put("a", null);
		hm.put("b", Integer.valueOf(3));
		hm.put("c", new Date());
		javaV.add(hm);
		hm = new HashMap<String, Object>();
		hm.put("a", "Just some text 4");
		hm.put("b", Integer.valueOf(4));
		hm.put("c", new Date());
		javaV.add(hm);
		hm = new HashMap<String, Object>();
		hm.put("a", "Just some text 5");
		hm.put("b", Integer.valueOf(5));
		hm.put("c", null);
		javaV.add(hm);

		// conversion should turn it into a change-aware list
		Object sabloBeforeWrap = NGConversions.INSTANCE.convertFormElementToSabloComponentValue(javaV.toArray(), normalArrayPD, null, null, null);
		assertTrue(sabloBeforeWrap instanceof List< ? >);

		@SuppressWarnings("unchecked")
		List<Map< ? , ? >> sbr = (List<Map< ? , ? >>)sabloBeforeWrap;
		assertEquals("Just some text 1", sbr.get(0).get("a"));
		assertEquals(null, sbr.get(1));
		assertEquals("Just some text 2", sbr.get(2).get("a"));
		assertEquals(null, sbr.get(3).get("a"));
		assertEquals("Just some text 4", sbr.get(4).get("a"));
		assertEquals(null, sbr.get(5).get("c"));
		assertEquals(6, sbr.size());
	}

	public static void assertGranularOpIs(int startIndex, int endIndex, int opType, Set<String> columnNames, ArrayOperation opSeq)
	{
		assertEquals("startIndex check", startIndex, opSeq.startIndex);
		assertEquals("endIndex check", endIndex, opSeq.endIndex);
		assertEquals("opType check", opType, opSeq.type);
		assertEquals("columnName check", columnNames, opSeq.cellNames);
	}

}
